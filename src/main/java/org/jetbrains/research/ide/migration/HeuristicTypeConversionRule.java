package org.jetbrains.research.ide.migration;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiType;
import com.intellij.refactoring.typeMigration.TypeConversionDescriptor;
import com.intellij.refactoring.typeMigration.TypeConversionDescriptorBase;
import com.intellij.refactoring.typeMigration.TypeMigrationLabeler;
import com.intellij.refactoring.typeMigration.rules.TypeConversionRule;
import com.intellij.structuralsearch.MatchResult;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.data.TypeChangeRulesStorage;
import org.jetbrains.research.data.models.TypeChangeRuleDescriptor;
import org.jetbrains.research.utils.StringUtils;

import java.util.List;

public class HeuristicTypeConversionRule extends TypeConversionRule {
    private static final int MAX_PARENTS_TO_LIFT_UP = 2;
    private static final Logger LOG = Logger.getInstance(TypeChangeRulesStorage.class);

    @Override
    public @Nullable TypeConversionDescriptorBase findConversion(
            PsiType from, PsiType to, PsiMember member, PsiExpression context, TypeMigrationLabeler labeler
    ) {
        final var pattern = TypeChangeRulesStorage.findPattern(
                from.getCanonicalText(),
                to.getCanonicalText()
        );
        if (pattern == null || context == null) return null;

        PsiElement currentContext = context;
        int parentsPassed = 0;
        TypeChangeRuleDescriptor bestMatchedRule = null;

        while (parentsPassed < MAX_PARENTS_TO_LIFT_UP) {
            final List<TypeChangeRuleDescriptor> rules = pattern.getRules();
            for (var rule : rules) {
                List<MatchResult> matches = StringUtils.findMatches(
                        currentContext.getText(),
                        rule.getExpressionBefore()
                );
                if (!matches.isEmpty()) {
                    if (bestMatchedRule == null) {
                        bestMatchedRule = rule;
                        continue;
                    }

                    // Update bestMatchedRule iff it matches a larger number of tokens
                    final var ruleTokens = StringUtils.splitByTokens(rule.getExpressionBefore());
                    final var bestMatchedRuleTokens = StringUtils.splitByTokens(bestMatchedRule.getExpressionBefore());
                    if (bestMatchedRuleTokens.length < ruleTokens.length) {
                        bestMatchedRule = rule;
                    }
                }
            }
            currentContext = currentContext.getParent();
            parentsPassed++;
        }

        final var collector = TypeChangesInfoCollector.getInstance();
        if (bestMatchedRule != null) {
            if (bestMatchedRule.getReturnType() != null) {
                // Check if the rule is "suspicious", e.g. it changes the return type of the expression
                if (!bestMatchedRule.getReturnType().getSourceType()
                        .equals(bestMatchedRule.getReturnType().getTargetType())) {
                    collector.addFailedUsage(context);
                    collector.addRuleForFailedUsage(context, bestMatchedRule);
                    return null;
                }
            }
            // Collect required imports for this rule
            if (bestMatchedRule.getRequiredImports() != null) {
                RequiredImportsCollector.getInstance().addRequiredImport(
                        bestMatchedRule.getRequiredImports()
                );
            }
            // Will be successfully updated with a rule
            collector.addUpdatedUsage(context);
            return new TypeConversionDescriptor(
                    bestMatchedRule.getExpressionBefore(),
                    bestMatchedRule.getExpressionAfter()
            );
        }
        collector.addFailedUsage(context);
        return null;
    }
}