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
        Project project = context.getProject();
        final var collector = TypeChangesInfoCollector.getInstance();

        final var storage = project.getService(TypeChangeRulesStorage.class);
        final var pattern = storage.findPattern(
                from.getCanonicalText(),
                to.getCanonicalText()
        );

        final TypeMigrationUsageInfo currentRoot = extractCurrentRoot(labeler);
        if (pattern.isEmpty() || currentRoot == null) {
            collector.addFailedUsage(context);
            return null;
        }
        final String currentRootName = PsiUtil.getName(currentRoot.getElement());

        PsiElement currentContext = context;
        int parentsPassed = 0;
        TypeChangeRuleDescriptor bestMatchedRule = null;
        final List<TypeChangeRuleDescriptor> rules = pattern.get().getRules();

        while (parentsPassed < Config.MAX_PARENTS_TO_LIFT_UP) {
            // It means that we didn't lift up too much
            if (currentContext instanceof PsiExpression &&
                    PsiTreeUtil.findChildrenOfType(currentContext, PsiReferenceExpression.class).stream()
                            .filter(element -> element.getText().equals(currentRootName)).count() <= 1) {
                for (var rule : rules) {
                    if (rule.getExpressionBefore().contains("$1$") && !PsiRelatedUtils.hasRootInside(currentContext, currentRootName)) {
                        // To avoid cases when `currentRootName` appears in the `currentContext` as a part of some string literal,
                        // such as root reference `file` in the expression `new File("file.txt")`
                        continue;
                    }
                    List<MatchResult> matches = SSRUtils.matchRule(rule.getExpressionBefore(), currentRootName, currentContext, project);
                    if (!matches.isEmpty()) {
                        // To prevent cases like `UUID.fromString(System.out.println(s))`, where `s` is current root,
                        // and the rule $2$ -> UUId.fromString($2$) matches to all statement, which is wrong,
                        // because it should not contain the root inside
                        if (matches.get(0).getChildren().stream()
                                .anyMatch(matchResult -> !matchResult.getName().equals("1")
                                        && PsiRelatedUtils.hasRootInside(matchResult.getMatch(), currentRootName))) {
                            continue;
                        }

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
                RequiredImportsCollector.getInstance().addRequiredImport(bestMatchedRule.getRequiredImports());
            }
            // Will be successfully updated with a rule
            collector.addUpdatedUsage(context);
            collector.addUsedRule(bestMatchedRule);
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