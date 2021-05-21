package org.jetbrains.research.ide.refactoring.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.data.TypeChangeRulesStorage;
import org.jetbrains.research.ide.refactoring.TypeChangeRefactoringAvailabilityUpdater;
import org.jetbrains.research.ide.refactoring.services.TypeChangeRefactoringProviderImpl;
import org.jetbrains.research.utils.PsiUtils;

public class TypeChangeDocumentListener implements DocumentListener {
    private static final Logger LOG = Logger.getInstance(TypeChangeRulesStorage.class);

    private final Project project;
    private final PsiDocumentManager psiDocumentManager;

    public TypeChangeDocumentListener(Project project) {
        this.project = project;
        this.psiDocumentManager = PsiDocumentManager.getInstance(project);
    }

    @Override
    public void beforeDocumentChange(@NotNull DocumentEvent event) {
        final var document = event.getDocument();
        if (!psiDocumentManager.isCommitted(document) || psiDocumentManager.isDocumentBlockedByPsi(document)) return;

        var psiFile = psiDocumentManager.getCachedPsiFile(document);
        if (psiFile == null || shouldIgnoreFile(psiFile)) return;

        final int offset = event.getOffset();
        final var oldElement = psiFile.findElementAt(offset);
        if (oldElement == null) return;

        final var oldElementQualifiedName = PsiUtils.getClosestFullyQualifiedName(oldElement);
        if (oldElementQualifiedName == null) return;

        if (TypeChangeRulesStorage.hasSourceType(oldElementQualifiedName)) {
            processSourceTypeChangeEvent(oldElement, oldElementQualifiedName, document);
        }
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        final var document = event.getDocument();
        psiDocumentManager.commitDocument(document);

        final PsiFile psiFile = psiDocumentManager.getCachedPsiFile(document);
        if (psiFile == null || shouldIgnoreFile(psiFile)) return;

        final int offset = event.getOffset();
        final var newElement = psiFile.findElementAt(offset);
        if (newElement == null) return;

        final var newElementQualifiedName = PsiUtils.getClosestFullyQualifiedName(newElement);
        if (newElementQualifiedName == null) return;

        if (TypeChangeRulesStorage.hasTargetType(newElementQualifiedName)) {
            processTargetTypeChangeEvent(newElement, newElementQualifiedName, document);

            final var updater = TypeChangeRefactoringAvailabilityUpdater.getInstance(project);
            updater.updateAllHighlighters(event.getDocument(), event.getOffset());
            ApplicationManager.getApplication().invokeLater(updater::addDisableRefactoringWatcher);
        }
    }

    private void processSourceTypeChangeEvent(PsiElement oldElement, String sourceType, Document document) {
        final var range = TextRange.from(
                oldElement.getTextOffset(),
                oldElement.getTextLength()
        );
        final var rangeMarker = document.createRangeMarker(range);

        final var state = TypeChangeRefactoringProviderImpl.getInstance(project).getState();
        state.initialMarkerToSourceTypeMappings.put(rangeMarker, sourceType);
    }

    private void processTargetTypeChangeEvent(PsiElement newElement, String targetType, Document document) {
        final var newRange = TextRange.from(newElement.getTextOffset(), newElement.getTextLength());
        final var state = TypeChangeRefactoringProviderImpl.getInstance(project).getState();

        RangeMarker relevantOldRangeMarker = null;
        String relevantSourceType = null;
        for (var entry : state.initialMarkerToSourceTypeMappings.entrySet()) {
            final var oldRangeMarker = entry.getKey();
            final var oldRange = new TextRange(oldRangeMarker.getStartOffset(), oldRangeMarker.getEndOffset());
            final var sourceType = entry.getValue();
            if (oldRange.intersects(newRange)) {
                relevantOldRangeMarker = oldRangeMarker;
                relevantSourceType = sourceType;
                break;
            }
        }
        if (relevantOldRangeMarker == null || relevantSourceType == null) return;

        final var newRangeMarker = document.createRangeMarker(newRange);
        state.addCompleteTypeChange(relevantOldRangeMarker, newRangeMarker, relevantSourceType, targetType);
        state.refactoringEnabled = true;
    }

    private Boolean shouldIgnoreFile(PsiFile file) {
        return !file.isPhysical() || file instanceof PsiBinaryFile || file instanceof PsiCodeFragment;
    }
}
