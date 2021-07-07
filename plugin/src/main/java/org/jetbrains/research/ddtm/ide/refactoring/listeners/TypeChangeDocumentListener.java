package org.jetbrains.research.ddtm.ide.refactoring.listeners;

import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.Config;
import org.jetbrains.research.ddtm.data.TypeChangeRulesStorage;
import org.jetbrains.research.ddtm.ide.refactoring.ReactiveTypeChangeAvailabilityUpdater;
import org.jetbrains.research.ddtm.ide.refactoring.services.TypeChangeRefactoringProviderImpl;
import org.jetbrains.research.ddtm.utils.PsiRelatedUtils;

public class TypeChangeDocumentListener implements DocumentListener {
    private static final Logger LOG = Logger.getInstance(TypeChangeDocumentListener.class);

    private final Project project;
    private final PsiDocumentManager psiDocumentManager;

    public TypeChangeDocumentListener(Project project) {
        this.project = project;
        this.psiDocumentManager = PsiDocumentManager.getInstance(project);
    }

    @Override
    public void beforeDocumentChange(@NotNull DocumentEvent event) {
        if (UndoManager.getInstance(project).isUndoInProgress()) return;

        final var state = TypeChangeRefactoringProviderImpl.getInstance(project).getState();
        if (state.isInternalTypeChangeInProgress) return;

        final var document = event.getDocument();
        if (!psiDocumentManager.isCommitted(document) || psiDocumentManager.isDocumentBlockedByPsi(document)) return;

        var psiFile = psiDocumentManager.getCachedPsiFile(document);
        if (psiFile == null || shouldIgnoreFile(psiFile)) return;

        final int offset = event.getOffset();
        try {
            for (var rangeMarker : state.uncompletedTypeChanges.keySet()) {
                if (document.getLineNumber(rangeMarker.getStartOffset()) == document.getLineNumber(offset)) return;
            }
        } catch (IndexOutOfBoundsException ex) {
            LOG.warn("Wrong offset");
            state.uncompletedTypeChanges.clear();
            return;
        }

        final var oldElement = psiFile.findElementAt(offset);
        if (oldElement == null) return;

        final PsiTypeElement oldTypeElement = PsiRelatedUtils.getClosestPsiTypeElement(oldElement);
        if (oldTypeElement == null) return;
        final String sourceType = oldTypeElement.getType().getCanonicalText();

        if (TypeChangeRulesStorage.hasSourceType(sourceType)) {
            processSourceTypeChangeEvent(oldElement, sourceType, document);
        }
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        if (UndoManager.getInstance(project).isUndoInProgress()) return;

        final var state = TypeChangeRefactoringProviderImpl.getInstance(project).getState();
        if (state.isInternalTypeChangeInProgress) return;

        final var document = event.getDocument();
        try {
            psiDocumentManager.commitDocument(document);
        } catch (IllegalArgumentException ex) {
            LOG.warn("Can not commit document");
            return;
        }

        final PsiFile psiFile = psiDocumentManager.getCachedPsiFile(document);
        if (psiFile == null || shouldIgnoreFile(psiFile)) return;

        final int offset = event.getOffset();
        PsiElement newElement = psiFile.findElementAt(offset);
        if (newElement == null) return;

        final PsiTypeElement newTypeElement = PsiRelatedUtils.getClosestPsiTypeElement(newElement);
        if (newTypeElement == null) return;
        final String fqTargetType = newTypeElement.getType().getCanonicalText();
        final String fqTargetTypeWithoutGenerics =
                fqTargetType.contains("<")
                        ? fqTargetType.substring(0, fqTargetType.indexOf('<'))
                        : fqTargetType;
        final String shortenedTargetType = fqTargetTypeWithoutGenerics.substring(fqTargetTypeWithoutGenerics.lastIndexOf('.') + 1);

        if (TypeChangeRulesStorage.hasTargetType(fqTargetType) && newElement.getText().equals(shortenedTargetType)) {
            processTargetTypeChangeEvent(newElement, fqTargetType, document);

            final var updater = ReactiveTypeChangeAvailabilityUpdater.getInstance(project);
            updater.updateAllHighlighters(event.getDocument(), event.getOffset());
        }
    }

    private void processSourceTypeChangeEvent(PsiElement oldElement, String sourceType, Document document) {
        final TextRange range = TextRange.from(
                oldElement.getTextOffset(),
                oldElement.getTextLength()
        );
        final RangeMarker rangeMarker = document.createRangeMarker(range);

        final var state = TypeChangeRefactoringProviderImpl.getInstance(project).getState();
        state.uncompletedTypeChanges.put(rangeMarker, sourceType);
    }

    private void processTargetTypeChangeEvent(PsiElement newElement, String targetType, Document document) {
        final var newRange = TextRange.from(newElement.getTextOffset(), newElement.getTextLength());
        final var state = TypeChangeRefactoringProviderImpl.getInstance(project).getState();

        RangeMarker relevantOldRangeMarker = null;
        String relevantSourceType = null;
        for (var entry : state.uncompletedTypeChanges.entrySet()) {
            final RangeMarker oldRangeMarker = entry.getKey();
            final String sourceType = entry.getValue();
            if (oldRangeMarker.getDocument() != document) continue;

            final var oldRange = new TextRange(oldRangeMarker.getStartOffset(), oldRangeMarker.getEndOffset());
            if (oldRange.intersects(newRange) && TypeChangeRulesStorage.findPattern(sourceType, targetType).isPresent()) {
                relevantOldRangeMarker = oldRangeMarker;
                relevantSourceType = sourceType;
                break;
            }
        }
        if (relevantOldRangeMarker == null || relevantSourceType == null) return;
        state.uncompletedTypeChanges.remove(relevantOldRangeMarker);

        final var newRangeMarker = document.createRangeMarker(newRange);
        state.addCompletedTypeChange(relevantOldRangeMarker, newRangeMarker, relevantSourceType, targetType);
        state.refactoringEnabled = true;

        Thread disablerWaitThread = new Thread(() -> {
            try {
                Thread.sleep(Config.WAIT_UNTIL_DISABLE_INTENTION);
                state.refactoringEnabled = false;
                state.removeAllTypeChangesByRange(newRange);
                state.uncompletedTypeChanges.clear();
            } catch (InterruptedException e) {
                LOG.error(e);
            }
        });
        disablerWaitThread.start();
    }

    private Boolean shouldIgnoreFile(PsiFile file) {
        return !file.isPhysical() || file instanceof PsiBinaryFile || file instanceof PsiCodeFragment;
    }
}
