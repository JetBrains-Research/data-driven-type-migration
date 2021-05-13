package org.jetbrains.research.ide.refactoring;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ide.services.TypeChangeRefactoringProviderImpl;
import org.jetbrains.research.migration.TypeChangeRulesStorage;
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
            processSourceTypeChangeEvent(oldElement, oldElementQualifiedName);
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
            processTargetTypeChangeEvent(newElement, newElementQualifiedName, event);
        }
    }

    private void processSourceTypeChangeEvent(PsiElement oldElement, String sourceType) {
        final var range = TextRange.from(
                oldElement.getTextOffset(),
                oldElement.getTextLength()
        );

        final var state = TypeChangeRefactoringProviderImpl.getInstance(project).getState();
        state.affectedRangeToSourceTypeMappings.put(range, sourceType);
    }

    private void processTargetTypeChangeEvent(PsiElement newElement, String targetType, DocumentEvent event) {
        final var newRange = TextRange.from(newElement.getTextOffset(), newElement.getTextLength());
        final var state = TypeChangeRefactoringProviderImpl.getInstance(project).getState();

        TextRange relevantOldRange = null;
        String relevantSourceType = null;
        for (var entry : state.affectedRangeToSourceTypeMappings.entrySet()) {
            final var oldRange = entry.getKey();
            final var sourceType = entry.getValue();
            if (oldRange.intersects(newRange)) {
                relevantOldRange = oldRange;
                relevantSourceType = sourceType;
                break;
            }
        }
        if (relevantOldRange == null || relevantSourceType == null) return;

        state.addTypeChange(relevantOldRange, newRange, relevantSourceType, targetType);
        state.refactoringEnabled = true;

        EditorFactory.getInstance().editors(event.getDocument(), project).forEach(editor -> {
            TypeChangeRefactoringAvailabilityUpdater.getInstance(project)
                    .updateHighlighter(editor, event.getOffset());
        });
    }

    private Boolean shouldIgnoreFile(PsiFile file) {
        return !file.isPhysical() || file instanceof PsiBinaryFile || file instanceof PsiCodeFragment;
    }
}
