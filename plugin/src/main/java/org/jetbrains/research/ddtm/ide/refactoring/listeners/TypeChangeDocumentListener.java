package org.jetbrains.research.ddtm.ide.refactoring.listeners;

import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiTypeElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.data.TypeChangeRulesStorage;
import org.jetbrains.research.ddtm.ide.refactoring.ReactiveTypeChangeAvailabilityUpdater;
import org.jetbrains.research.ddtm.ide.refactoring.services.TypeChangeRefactoringProvider;
import org.jetbrains.research.ddtm.ide.settings.TypeChangeSettingsState;
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

        final var state = TypeChangeRefactoringProvider.getInstance(project).getState();
        if (state.isInternalTypeChangeInProgress) return;

        final var document = event.getDocument();
        if (!psiDocumentManager.isCommitted(document) || psiDocumentManager.isDocumentBlockedByPsi(document)) return;

        var psiFile = psiDocumentManager.getCachedPsiFile(document);
        if (psiFile == null || PsiRelatedUtils.shouldIgnoreFile(psiFile)) return;

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
        String sourceType;
        try {
            sourceType = oldTypeElement.getType().getCanonicalText();
        } catch (IndexNotReadyException e) {
            return;
        }

        final var storage = project.getService(TypeChangeRulesStorage.class);
        if (storage.hasSourceType(sourceType)) {
            processSourceTypeChangeEvent(oldElement, sourceType, document);
        }
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        if (UndoManager.getInstance(project).isUndoInProgress()) return;

        final var state = TypeChangeRefactoringProvider.getInstance(project).getState();
        if (state.isInternalTypeChangeInProgress) return;
        final var document = event.getDocument();

        psiDocumentManager.performForCommittedDocument(document, new Runnable() {
            final DocumentEvent e = event;

            @Override
            public void run() {
                documentChangedAndCommitted(e);
            }
        });
    }

    private void documentChangedAndCommitted(DocumentEvent event) {
        final Document document = event.getDocument();
        PsiFile psiFile = null;
        try {
            psiFile = psiDocumentManager.getPsiFile(document);
        } catch (Throwable e) {
            LOG.error(e);
        }
        if (psiFile == null || PsiRelatedUtils.shouldIgnoreFile(psiFile)) return;


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

        final var storage = project.getService(TypeChangeRulesStorage.class);
        if (storage.hasTargetType(fqTargetType) && newElement.getText().equals(shortenedTargetType)) {
            processTargetTypeChangeEvent(newElement, fqTargetType, document);

            final var updater = project.getService(ReactiveTypeChangeAvailabilityUpdater.class);
            updater.updateAllHighlighters(event.getDocument(), newElement.getTextRange().getEndOffset());
        }
    }

    private void processSourceTypeChangeEvent(PsiElement oldElement, String sourceType, Document document) {
        final TextRange range = TextRange.from(
                oldElement.getTextOffset(),
                oldElement.getTextLength()
        );
        final RangeMarker rangeMarker = document.createRangeMarker(range);

        final var state = TypeChangeRefactoringProvider.getInstance(project).getState();
        state.uncompletedTypeChanges.put(rangeMarker, sourceType);
    }

    private void processTargetTypeChangeEvent(PsiElement newElement, String targetType, Document document) {
        final var newRange = TextRange.from(
                newElement.getTextOffset(),
                newElement.getTextLength() + 1
        );
        final var state = TypeChangeRefactoringProvider.getInstance(project).getState();

        RangeMarker relevantOldRangeMarker = null;
        String relevantSourceType = null;
        for (var entry : state.uncompletedTypeChanges.entrySet()) {
            final RangeMarker oldRangeMarker = entry.getKey();
            final String sourceType = entry.getValue();
            if (oldRangeMarker.getDocument() != document) continue;

            final var storage = project.getService(TypeChangeRulesStorage.class);
            final var oldRange = new TextRange(oldRangeMarker.getStartOffset(), oldRangeMarker.getEndOffset());
            if (oldRange.intersects(newRange) && storage.findPattern(sourceType, targetType).isPresent()) {
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
                Thread.sleep(TypeChangeSettingsState.getInstance().disableIntentionTimeout);
                state.refactoringEnabled = false;
                state.completedTypeChanges.clear();
                state.uncompletedTypeChanges.clear();
            } catch (InterruptedException e) {
                LOG.error(e);
            }
        });
        disablerWaitThread.start();
    }
}
