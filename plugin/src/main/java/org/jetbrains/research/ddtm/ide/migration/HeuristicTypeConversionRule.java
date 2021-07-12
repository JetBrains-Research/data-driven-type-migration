package org.jetbrains.research.ddtm.ide.migration;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.refactoring.typeMigration.TypeConversionDescriptorBase;
import com.intellij.refactoring.typeMigration.TypeMigrationLabeler;
import com.intellij.refactoring.typeMigration.rules.TypeConversionRule;
import com.intellij.refactoring.typeMigration.usageInfo.TypeMigrationUsageInfo;
import com.intellij.structuralsearch.MatchResult;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.ddtm.Config;
import org.jetbrains.research.ddtm.data.TypeChangeRulesStorage;
import org.jetbrains.research.ddtm.data.models.TypeChangeRuleDescriptor;
import org.jetbrains.research.ddtm.ide.migration.collectors.RequiredImportsCollector;
import org.jetbrains.research.ddtm.ide.migration.collectors.TypeChangesInfoCollector;
import org.jetbrains.research.ddtm.ide.migration.structuralsearch.SSRUtils;
import org.jetbrains.research.ddtm.utils.PsiRelatedUtils;

import java.lang.reflect.Field;
import java.util.List;

public class HeuristicTypeConversionRule extends TypeConversionRule {
    @Override
    public @Nullable TypeConversionDescriptorBase findConversion(
            PsiType from, PsiType to, PsiMember member, PsiExpression context, TypeMigrationLabeler labeler
    ) {
        if (from.getCanonicalText().equals(to.getCanonicalText()) || context == null) return null;
        final var pattern = TypeChangeRulesStorage.findPattern(
                from.getCanonicalText(),
                to.getCanonicalText()
        );

        final TypeMigrationUsageInfo currentRoot = extractCurrentRoot(labeler);
        if (pattern.isEmpty() || currentRoot == null) return null;
        final String currentRootName = PsiUtil.getName(currentRoot.getElement());

        Project project = context.getProject();
        PsiElement currentContext = context;
        int parentsPassed = 0;
        TypeChangeRuleDescriptor bestMatchedRule = null;
        final List<TypeChangeRuleDescriptor> rules = pattern.get().getRules();

        while (parentsPassed < Config.MAX_PARENTS_TO_LIFT_UP) {
            if (currentContext instanceof PsiExpression) {
                // It means that we lifted up to much and even touching full statement
                for (var rule : rules) {
                    if (rule.getExpressionBefore().contains("$1$") && !currentContext.getText().equals(currentRootName) &&
                            PsiTreeUtil.findChildrenOfType(currentContext, PsiReferenceExpression.class).stream()
                                    .noneMatch(element -> element.getText().equals(currentRootName))) {
                        // To avoid cases when `currentRootName` appears in the `currentContext` as a part of some string literal,
                        // such as root reference `file` in the expression `new File("file.txt")`
                        continue;
                    }
                    List<MatchResult> matches = SSRUtils.matchRule(rule.getExpressionBefore(), currentRootName, currentContext, project);
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
                            // The second condition is used to always prefer rules with a current root in the "before" part
                            bestMatchedRule = rule;
                        }
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
    private @Nullable TypeMigrationUsageInfo extractCurrentRoot(TypeMigrationLabeler labeler) {
        try {
            Field field = labeler.getClass().getDeclaredField("myCurrentRoot");
            field.setAccessible(true);
            return (TypeMigrationUsageInfo) field.get(labeler);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }
}