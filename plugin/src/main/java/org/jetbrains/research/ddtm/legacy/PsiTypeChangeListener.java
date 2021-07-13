package org.jetbrains.research.ddtm.legacy;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.data.TypeChangeRulesStorage;
import org.jetbrains.research.ddtm.ide.refactoring.ReactiveTypeChangeAvailabilityUpdater;
import org.jetbrains.research.ddtm.ide.refactoring.services.TypeChangeRefactoringProvider;
import org.jetbrains.research.ddtm.ide.settings.TypeChangeSettingsState;
import org.jetbrains.research.ddtm.utils.PsiRelatedUtils;

public class PsiTypeChangeListener extends PsiTreeChangeAdapter {
    private static final Logger LOG = Logger.getInstance(PsiTypeChangeListener.class);

    private final Project project;
    private final TypeChangeRulesStorage storage;
    private final PsiDocumentManager documentManager;
    private final ReactiveTypeChangeAvailabilityUpdater updater;

    public PsiTypeChangeListener(Project project) {
        this.project = project;
        this.storage = project.getService(TypeChangeRulesStorage.class);
        this.updater = project.getService(ReactiveTypeChangeAvailabilityUpdater.class);
        this.documentManager = PsiDocumentManager.getInstance(project);
    }

    @Override
    public void beforeChildReplacement(@NotNull PsiTreeChangeEvent event) {
        super.beforeChildReplacement(event);
        final var state = TypeChangeRefactoringProvider.getInstance(project).getState();
        if (state.isInternalTypeChangeInProgress) return;

        final var psiFile = event.getFile();
        if (psiFile == null || PsiRelatedUtils.shouldIgnoreFile(psiFile)) return;

        final var document = documentManager.getDocument(psiFile);
        if (document == null) return;

        final var oldElement = event.getOldChild();
        if (oldElement == null) return;

        final PsiTypeElement oldTypeElement = PsiRelatedUtils.getClosestPsiTypeElement(oldElement);
        if (oldTypeElement == null) return;
        final String sourceType = oldTypeElement.getType().getCanonicalText();

        if (storage.hasSourceType(sourceType)) {
            processSourceTypeChangeEvent(oldElement, sourceType, document);
        }
    }

    @Override
    public void childReplaced(@NotNull PsiTreeChangeEvent event) {
        super.childReplaced(event);
        final var state = TypeChangeRefactoringProvider.getInstance(project).getState();
        if (state.isInternalTypeChangeInProgress) return;

        final PsiFile psiFile = event.getFile();
        if (psiFile == null || PsiRelatedUtils.shouldIgnoreFile(psiFile)) return;

        final var document = documentManager.getDocument(psiFile);
        if (document == null) return;

        PsiElement newElement = event.getNewChild();
        if (newElement == null) return;

        final PsiTypeElement newTypeElement = PsiRelatedUtils.getClosestPsiTypeElement(newElement);
        if (newTypeElement == null) return;

        final String fqTargetType = newTypeElement.getType().getCanonicalText();
        final String fqTargetTypeWithoutGenerics =
                fqTargetType.contains("<")
                        ? fqTargetType.substring(0, fqTargetType.indexOf('<'))
                        : fqTargetType;
        final String shortenedTargetType = fqTargetTypeWithoutGenerics.substring(fqTargetTypeWithoutGenerics.lastIndexOf('.') + 1);

        if (storage.hasTargetType(fqTargetType) && newElement.getText().equals(fqTargetType)) {
            processTargetTypeChangeEvent(newElement, fqTargetType, shortenedTargetType, document);
            updater.updateAllHighlighters(document, event.getNewChild().getTextRange().getEndOffset());
        }
    }

    private void processSourceTypeChangeEvent(PsiElement oldElement, String sourceType, Document document) {
        final TextRange range = TextRange.create(
                oldElement.getTextRange().getStartOffset(),
                oldElement.getTextRange().getEndOffset()
        );
        final RangeMarker rangeMarker = document.createRangeMarker(range);
        final var state = TypeChangeRefactoringProvider.getInstance(project).getState();
        state.uncompletedTypeChanges.put(rangeMarker, sourceType);
    }

    private void processTargetTypeChangeEvent(PsiElement newElement, String targetType, String shortenedTargetType, Document document) {
        final var newRange = TextRange.from(newElement.getTextRange().getStartOffset(), shortenedTargetType.length());
        final var state = TypeChangeRefactoringProvider.getInstance(project).getState();

        RangeMarker relevantOldRangeMarker = null;
        String relevantSourceType = null;
        for (var entry : state.uncompletedTypeChanges.entrySet()) {
            final RangeMarker oldRangeMarker = entry.getKey();
            final String sourceType = entry.getValue();
            if (oldRangeMarker.getDocument() != document) continue;
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
                state.removeAllTypeChangesByRange(newRange);
                state.uncompletedTypeChanges.clear();
            } catch (InterruptedException e) {
                LOG.error(e);
            }
        });
        disablerWaitThread.start();
    }
}
