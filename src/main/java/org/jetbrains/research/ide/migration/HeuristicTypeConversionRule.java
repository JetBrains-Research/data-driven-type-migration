package org.jetbrains.research.ide.migration;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiUtil;
import com.intellij.refactoring.typeMigration.TypeConversionDescriptorBase;
import com.intellij.refactoring.typeMigration.TypeMigrationLabeler;
import com.intellij.refactoring.typeMigration.rules.TypeConversionRule;
import com.intellij.refactoring.typeMigration.usageInfo.TypeMigrationUsageInfo;
import com.intellij.structuralsearch.MatchResult;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.data.TypeChangeRulesStorage;
import org.jetbrains.research.data.models.TypeChangeRuleDescriptor;
import org.jetbrains.research.ide.migration.structuralsearch.SSRUtils;
import org.jetbrains.research.utils.PsiRelatedUtils;

import java.lang.reflect.Field;
import java.util.List;

public class HeuristicTypeConversionRule extends TypeConversionRule {
    private static final int MAX_PARENTS_TO_LIFT_UP = 3;
    private static final Logger LOG = Logger.getInstance(TypeChangeRulesStorage.class);

    @Override
    public @Nullable TypeConversionDescriptorBase findConversion(
            PsiType from, PsiType to, PsiMember member, PsiExpression context, TypeMigrationLabeler labeler
    ) {
        final var pattern = TypeChangeRulesStorage.findPattern(
                from.getCanonicalText(),
                to.getCanonicalText()
        );
        final String currentRootName = extractCurrentRootIdentName(labeler);
        if (pattern == null || context == null || currentRootName == null) return null;

        PsiElement currentContext = context;
        int parentsPassed = 0;
        TypeChangeRuleDescriptor bestMatchedRule = null;
        final List<TypeChangeRuleDescriptor> rules = pattern.getRules();

        while (parentsPassed < MAX_PARENTS_TO_LIFT_UP) {
            if (currentContext.getText().contains("=")) {
                // It means that we lifted up to much and even touching another usage from the same assignment
                break;
            }
            for (var rule : rules) {
                List<MatchResult> matches = SSRUtils.matchRule(
                        currentContext.getText(),
                        rule.getExpressionBefore(),
                        currentRootName,
                        context.getProject()
                );
                if (!matches.isEmpty()) {
                    if (bestMatchedRule == null) {
                        bestMatchedRule = rule;
                        continue;
                    }

                    // Update bestMatchedRule iff it matches a larger number of tokens
                    final var ruleTokens = PsiRelatedUtils.splitByTokens(rule.getExpressionBefore());
                    final var bestMatchedRuleTokens = PsiRelatedUtils.splitByTokens(bestMatchedRule.getExpressionBefore());
                    if (bestMatchedRuleTokens.length < ruleTokens.length
                            || bestMatchedRuleTokens.length == ruleTokens.length && rule.getExpressionBefore().contains("$1$")) {
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
                if (!bestMatchedRule.getReturnType().getSourceType().equals(bestMatchedRule.getReturnType().getTargetType())) {
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
            return new HeuristicTypeConversionDescriptor(
                    bestMatchedRule.getExpressionBefore(),
                    bestMatchedRule.getExpressionAfter(),
                    currentRootName
            );
        }
        collector.addFailedUsage(context);
        return null;
    }

    @ApiStatus.Internal
    private @Nullable String extractCurrentRootIdentName(TypeMigrationLabeler labeler) {
        try {
            Field field = labeler.getClass().getDeclaredField("myCurrentRoot");
            field.setAccessible(true);
            return PsiUtil.getName(((TypeMigrationUsageInfo) field.get(labeler)).getElement());
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }
}