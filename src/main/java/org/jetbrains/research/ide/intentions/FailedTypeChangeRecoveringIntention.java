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
import org.jetbrains.research.ide.fus.TypeChangeLogsCollector;
import org.jetbrains.research.ide.migration.TypeChangesInfoCollector;
import org.jetbrains.research.utils.PsiRelatedUtils;

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
        final var typeEvaluator = TypeChangesInfoCollector.getInstance().getTypeEvaluator();
        conversionDescriptor.replace(
                PsiRelatedUtils.getHighestParentOfType(element, PsiExpression.class),
                typeEvaluator
        );
        TypeChangeLogsCollector.getInstance().recoveringIntentionApplied(project, rule);
    }

    @Override
    public @NotNull Priority getPriority() {
        return PriorityAction.Priority.TOP;
    }
}