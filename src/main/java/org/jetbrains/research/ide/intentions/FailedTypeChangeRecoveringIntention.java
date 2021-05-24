package org.jetbrains.research.ide.intentions;

import com.intellij.codeInsight.intention.PriorityAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.refactoring.typeMigration.TypeConversionDescriptor;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.data.models.TypeChangeRuleDescriptor;
import org.jetbrains.research.ide.migration.FailedTypeChangesCollector;
import org.jetbrains.research.utils.PsiUtils;

public class FailedTypeChangeRecoveringIntention extends PsiElementBaseIntentionAction implements PriorityAction {
    private final TypeChangeRuleDescriptor rule;

    public FailedTypeChangeRecoveringIntention(TypeChangeRuleDescriptor rule) {
        this.rule = rule;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "Data-driven type migration";
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return rule.toString();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
            throws IncorrectOperationException {
        final var conversionDescriptor = new TypeConversionDescriptor(
                rule.getExpressionBefore(),
                rule.getExpressionAfter()
        );
        final var typeEvaluator = FailedTypeChangesCollector.getInstance().getTypeEvaluator();
        conversionDescriptor.replace(
                PsiUtils.getHighestParentOfType(element, PsiExpression.class),
                typeEvaluator
        );
    }

    @Override
    public @NotNull Priority getPriority() {
        return PriorityAction.Priority.TOP;
    }
}