package org.jetbrains.research.ide;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.migration.DataDrivenRulesStorage;
import org.jetbrains.research.utils.PsiUtils;

class TypeChangeDocumentListener implements DocumentListener {
    private static final Logger LOG = Logger.getInstance(DataDrivenRulesStorage.class);

    private final Project project;
    private final PsiDocumentManager psiDocumentManager;

    public TypeChangeDocumentListener(Project project) {
        this.project = project;
        this.psiDocumentManager = PsiDocumentManager.getInstance(project);
    }

    @Override
    public void beforeDocumentChange(@NotNull DocumentEvent event) {
        try {
            final var state = TypeMigrationRefactoringProviderImpl.getInstance(project).getState();
            final var document = event.getDocument();
            var psiFile = psiDocumentManager.getCachedPsiFile(document);

            if (psiFile == null || shouldIgnoreFile(psiFile)) return;

            if (psiDocumentManager.isCommitted(document) && !psiDocumentManager.isDocumentBlockedByPsi(document)) {
                final int offset = event.getOffset();
                final var oldElement = psiFile.findElementAt(offset);
                if (oldElement == null) return;

                final var oldElementQualifiedName = getQualifiedName(oldElement);

                if (oldElementQualifiedName != null) {
                    if (!DataDrivenRulesStorage.getRulesDescriptorsBySourceType(oldElementQualifiedName).isEmpty()) {
                        final var range = TextRange.from(
                                oldElement.getTextOffset(),
                                oldElement.getTextLength()
                        );
                        state.affectedTextRangeToSourceTypeName.put(range, oldElementQualifiedName);
                    }
                }
            }

        } catch (Exception e) {
            LOG.error(e);
        }
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        try {
            if (event.getOldLength() == 0 && event.getNewLength() == 0) return;

            final var state = TypeMigrationRefactoringProviderImpl.getInstance(project).getState();
            final var document = event.getDocument();
            var psiFile = psiDocumentManager.getCachedPsiFile(document);
            if (psiFile == null) return;

            if (shouldIgnoreFile(psiFile)) return;

            psiDocumentManager.commitDocument(document);
            psiFile = psiDocumentManager.getCachedPsiFile(document);
            if (psiFile == null) return;

            final int offset = event.getOffset();
            final var newElement = psiFile.findElementAt(offset);
            if (newElement == null) return;

            final var newElementQualifiedName = getQualifiedName(newElement);

            if (newElementQualifiedName != null) {
                if (!DataDrivenRulesStorage.getRulesDescriptorsByTargetType(newElementQualifiedName).isEmpty()) {
                    final var newRange = TextRange.from(
                            newElement.getTextOffset(),
                            newElement.getTextLength()
                    );

                    final var relevantRange = state.affectedTextRangeToSourceTypeName.keySet().stream()
                            .filter(oldRange -> oldRange.intersects(newRange))
                            .findFirst();
                    if (relevantRange.isPresent()) {
                        String sourceType = state.affectedTextRangeToSourceTypeName.get(relevantRange.get());

                        state.affectedTextRangeToTargetTypeName.put(newRange, newElementQualifiedName);
                        state.showRefactoringOpportunity = true;
                    }
                }
            }

        } catch (Exception e) {
            LOG.error(e);
        }
    }

    private Boolean shouldIgnoreFile(PsiFile file) {
        return !file.isPhysical() || file instanceof PsiBinaryFile || file instanceof PsiCodeFragment;
    }

    private @Nullable String getQualifiedName(PsiElement element) {
        PsiTypeElement correspondingTypeElement = null;

        if (element instanceof PsiWhiteSpace) {
            if (element.getPrevSibling() instanceof PsiTypeElement) {
                correspondingTypeElement = (PsiTypeElement) element.getPrevSibling();
            } else if (element.getNextSibling() instanceof PsiTypeElement) {
                correspondingTypeElement = (PsiTypeElement) element.getNextSibling();
            }
        } else {
            correspondingTypeElement = PsiUtils.getHighestParentOfType(element, PsiTypeElement.class);
        }
        if (correspondingTypeElement == null) return null;

        return correspondingTypeElement.getType().getCanonicalText();
    }

}
