package org.jetbrains.research.ddtm.ide.intentions;

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
import org.jetbrains.research.ddtm.DataDrivenTypeMigrationBundle;
import org.jetbrains.research.ddtm.data.TypeChangeRulesStorage;
import org.jetbrains.research.ddtm.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ddtm.data.models.TypeChangeRuleDescriptor;
import org.jetbrains.research.ddtm.ide.fus.TypeChangeLogsCollector;
import org.jetbrains.research.ddtm.ide.migration.collectors.TypeChangesInfoCollector;
import org.jetbrains.research.ddtm.utils.PsiRelatedUtils;

public class FailedTypeChangeRecoveringIntention extends PsiElementBaseIntentionAction implements PriorityAction {
    private final TypeChangeRuleDescriptor rule;

    public FailedTypeChangeRecoveringIntention(TypeChangeRuleDescriptor rule) {
        this.rule = rule;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return DataDrivenTypeMigrationBundle.message("intention.family.name");
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
        final var storage = project.getService(TypeChangeRulesStorage.class);
        final int patternId = storage.findPatternByRule(rule).map(TypeChangePatternDescriptor::getId).orElse(-1);
        TypeChangeLogsCollector.getInstance().recoveringIntentionApplied(project, patternId);
    }

    @Override
    public @NotNull Priority getPriority() {
        return PriorityAction.Priority.TOP;
    }
}