package org.jetbrains.research.ddtm.ide.intentions;

import com.intellij.codeInsight.intention.PriorityAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SlowOperations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.DataDrivenTypeMigrationBundle;
import org.jetbrains.research.ddtm.data.TypeChangeRulesStorage;
import org.jetbrains.research.ddtm.data.enums.InvocationWorkflow;
import org.jetbrains.research.ddtm.ide.refactoring.TypeChangeDialog;
import org.jetbrains.research.ddtm.utils.PsiRelatedUtils;

import java.util.Objects;

public class ProactiveTypeChangeIntention extends PsiElementBaseIntentionAction implements PriorityAction {
    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return DataDrivenTypeMigrationBundle.message("intention.family.name");
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return this.getFamilyName();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        PsiTypeElement parentType = PsiRelatedUtils.getHighestParentOfType(element, PsiTypeElement.class);
        if (parentType != null) {
            String parentTypeQualifiedName = parentType.getType().getCanonicalText();
            final var storage = project.getService(TypeChangeRulesStorage.class);
            return !storage.getPatternsBySourceType(parentTypeQualifiedName).isEmpty();
        }
        return false;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
            throws IncorrectOperationException {
        PsiType rootType = Objects.requireNonNull(PsiRelatedUtils.getHighestParentOfType(element, PsiTypeElement.class)).getType();
        final var storage = project.getService(TypeChangeRulesStorage.class);

        ApplicationManager.getApplication().invokeLater(() -> {
            final var dialog = new TypeChangeDialog(
                    SlowOperations.allowSlowOperations(() -> storage.getPatternsBySourceType(rootType.getCanonicalText())),
                    InvocationWorkflow.PROACTIVE, element, project
            );
            dialog.showAndGet();
        });
    }

    @Override
    public @NotNull Priority getPriority() {
        return PriorityAction.Priority.TOP;
    }
}