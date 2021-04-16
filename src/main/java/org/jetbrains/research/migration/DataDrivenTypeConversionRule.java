package org.jetbrains.research.migration;

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
import org.jetbrains.research.migration.json.DataDrivenTypeMigrationRule;
import org.jetbrains.research.utils.StringUtils;

import java.util.List;

public class DataDrivenTypeConversionRule extends TypeConversionRule {
    private static final int MAX_PARENTS_TO_LIFT_UP = 2;
    private static final Logger LOG = Logger.getInstance(DataDrivenRulesStorage.class);

    @Override
    public @Nullable TypeConversionDescriptorBase findConversion(
            PsiType from, PsiType to, PsiMember member, PsiExpression context, TypeMigrationLabeler labeler
    ) {
        if (context == null) {
            return null;
        }

        PsiElement currentContext = context;
        int parentsPassed = 0;
        DataDrivenTypeMigrationRule bestMatchedRule = null;

        while (parentsPassed < MAX_PARENTS_TO_LIFT_UP) {
            final var descriptor = DataDrivenRulesStorage.findDescriptor(
                    from.getCanonicalText(),
                    to.getCanonicalText()
            );
            if (descriptor != null) {
                final List<DataDrivenTypeMigrationRule> rules = descriptor.getRules();
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
            }
            currentContext = currentContext.getParent();
            parentsPassed++;
        }

        if (bestMatchedRule != null) {
            return new TypeConversionDescriptor(
                    bestMatchedRule.getExpressionBefore(),
                    bestMatchedRule.getExpressionAfter()
            );
        }

        return null;
    }
}
