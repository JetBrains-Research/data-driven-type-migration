package org.jetbrains.research.ddtm.ide.refactoring.listeners;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.components.Service;
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
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SlowOperations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.data.TypeChangeRulesStorage;
import org.jetbrains.research.ddtm.ide.refactoring.ReactiveTypeChangeAvailabilityUpdater;
import org.jetbrains.research.ddtm.ide.refactoring.services.TypeChangeRefactoringProvider;
import org.jetbrains.research.ddtm.ide.settings.TypeChangeSettingsState;
import org.jetbrains.research.ddtm.utils.PsiRelatedUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Service
public final class TypeChangeDocumentListener implements DocumentListener {
    private static final Logger LOG = Logger.getInstance(TypeChangeDocumentListener.class);

    private final Project project;

    public TypeChangeDocumentListener(Project project) {
        this.project = project;
    }

    @Override
    public void beforeDocumentChange(@NotNull DocumentEvent event) {
        if (UndoManager.getInstance(project).isUndoInProgress()) return;

        final var state = TypeChangeRefactoringProvider.getInstance(project).getState();
        if (state.isInternalTypeChangeInProgress) return;

        final var document = event.getDocument();
        final var psiDocumentManager = PsiDocumentManager.getInstance(project);
        if (!psiDocumentManager.isCommitted(document) || psiDocumentManager.isDocumentBlockedByPsi(document)) return;

        var psiFile = PsiDocumentManager.getInstance(project).getCachedPsiFile(document);
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
            final var storage = project.getService(TypeChangeRulesStorage.class);
            if (storage.hasSourceType(sourceType)) {
                processSourceTypeChangeEvent(oldElement, sourceType, document);
            }
        } catch (IndexNotReadyException | IncorrectOperationException e) {
            LOG.warn(e);
        }
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        if (UndoManager.getInstance(project).isUndoInProgress()) return;

        final var state = TypeChangeRefactoringProvider.getInstance(project).getState();
        if (state.isInternalTypeChangeInProgress) return;
        final var document = event.getDocument();

        PsiDocumentManager.getInstance(project).performForCommittedDocument(document, new Runnable() {
            final DocumentEvent e = event;

            @Override
            public void run() {
                try {
                    SlowOperations.allowSlowOperations(() -> documentChangedAndCommitted(e));
                } catch (Exception executionException) {
                    LOG.warn(executionException);
                }
            }
        });
    }

    private void documentChangedAndCommitted(DocumentEvent event) throws ExecutionException, TimeoutException {
        // FIXME: if I keep a reference to the Project in the field, then it throws an exception "Light files should have only one PSI"
        // But the similar approach is working in Suggested Refactoring...
        final var dataContext = DataManager.getInstance().getDataContextFromFocusAsync().blockingGet(500);
        if (dataContext == null) return;

        final var project = dataContext.getData(PlatformDataKeys.PROJECT);
        if (project == null || project.isDisposed()) return;

        final Document document = event.getDocument();
        PsiFile psiFile = null;
        try {
            psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        } catch (Throwable e) {
            LOG.warn(e);
        }
        if (psiFile == null || PsiRelatedUtils.shouldIgnoreFile(psiFile)) return;

        final int offset = event.getOffset();
        PsiElement newElement = psiFile.findElementAt(offset);
        if (newElement == null) return;

        final PsiTypeElement newTypeElement = PsiRelatedUtils.getClosestPsiTypeElement(newElement);
        if (newTypeElement == null) return;
        final String fqTargetType = newTypeElement.getType().getCanonicalText();

        final var storage = project.getService(TypeChangeRulesStorage.class);
        if (storage.hasTargetType(fqTargetType)) {
            processTargetTypeChangeEvent(newTypeElement, fqTargetType, document);

            final var updater = project.getService(ReactiveTypeChangeAvailabilityUpdater.class);
            updater.updateAllHighlighters(event.getDocument(), newTypeElement.getTextRange().getEndOffset());
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

    private void processTargetTypeChangeEvent(PsiTypeElement newTypeElement, String targetType, Document document) {
        final var newRange = TextRange.from(
                newTypeElement.getTextOffset(),
                newTypeElement.getTextLength() + 1
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
