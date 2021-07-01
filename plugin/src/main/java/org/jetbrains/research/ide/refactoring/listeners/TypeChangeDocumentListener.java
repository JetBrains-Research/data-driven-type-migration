package org.jetbrains.research.ide.refactoring.listeners;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.Config;
import org.jetbrains.research.data.TypeChangeRulesStorage;
import org.jetbrains.research.ide.refactoring.ReactiveTypeChangeAvailabilityUpdater;
import org.jetbrains.research.ide.refactoring.services.TypeChangeRefactoringProviderImpl;
import org.jetbrains.research.utils.PsiRelatedUtils;

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
            LOG.error("Wrong offset");
            return;
        }

        final var oldElement = psiFile.findElementAt(offset);
        if (oldElement == null) return;

        final var oldElementQualifiedName = PsiRelatedUtils.getClosestFullyQualifiedName(oldElement);
        if (oldElementQualifiedName == null) return;

        if (TypeChangeRulesStorage.hasSourceType(oldElementQualifiedName)) {
            processSourceTypeChangeEvent(oldElement, oldElementQualifiedName, document);
        }
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        final var state = TypeChangeRefactoringProviderImpl.getInstance(project).getState();
        if (state.isInternalTypeChangeInProgress) return;

        final var document = event.getDocument();
        psiDocumentManager.commitDocument(document);

        final PsiFile psiFile = psiDocumentManager.getCachedPsiFile(document);
        if (psiFile == null || shouldIgnoreFile(psiFile)) return;

        final int offset = event.getOffset();
        final var newElement = psiFile.findElementAt(offset);
        if (newElement == null) return;

        final var newElementQualifiedName = PsiRelatedUtils.getClosestFullyQualifiedName(newElement);
        if (newElementQualifiedName == null) return;

        if (TypeChangeRulesStorage.hasTargetType(newElementQualifiedName)) {
            processTargetTypeChangeEvent(newElement, newElementQualifiedName, document);

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
            final var oldRangeMarker = entry.getKey();
            if (oldRangeMarker.getDocument() != document) continue;

            final var oldRange = new TextRange(oldRangeMarker.getStartOffset(), oldRangeMarker.getEndOffset());
            if (oldRange.intersects(newRange)) {
                relevantOldRangeMarker = oldRangeMarker;
                relevantSourceType = entry.getValue();
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
