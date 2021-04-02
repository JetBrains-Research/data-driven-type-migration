package org.jetbrains.research.migration;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiType;
import com.intellij.refactoring.typeMigration.TypeConversionDescriptor;
import com.intellij.refactoring.typeMigration.TypeConversionDescriptorBase;
import com.intellij.refactoring.typeMigration.TypeMigrationLabeler;
import com.intellij.refactoring.typeMigration.rules.TypeConversionRule;
import com.intellij.structuralsearch.MatchOptions;
import com.intellij.structuralsearch.MatchResult;
import com.intellij.structuralsearch.Matcher;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.Utils;
import org.jetbrains.research.migration.json.DataDrivenTypeMigrationRule;

import java.util.List;

public class DataDrivenTypeConversionRule extends TypeConversionRule {
    private static final int MAX_PARENTS_TO_LIFT_UP = 2;
    private Project project;

    @Override
    public @Nullable TypeConversionDescriptorBase findConversion(
            PsiType from, PsiType to, PsiMember member, PsiExpression context, TypeMigrationLabeler labeler
    ) {
        if (context == null) {
            return null;
        }

        this.project = context.getProject();
        PsiElement currentContext = context;

        int parentsPassed = 0;
        DataDrivenTypeMigrationRule bestMatchedRule = null;
        while (parentsPassed < MAX_PARENTS_TO_LIFT_UP) {
            final var descriptor = DataDrivenRulesStorage.findDescriptor(from.getCanonicalText(), to.getCanonicalText());
            if (descriptor != null) {
                final List<DataDrivenTypeMigrationRule> rules = descriptor.getRules();
                for (var rule : rules) {
                    List<MatchResult> matches = Utils.findMatches(currentContext.getText(), rule.getExpressionBefore());
                    if (!matches.isEmpty()) {
                        if (bestMatchedRule == null) {
                            bestMatchedRule = rule;
                            continue;
                        }
                        final var ruleTokens = Utils.splitByTokens(rule.getExpressionBefore());
                        final var bestMatchedRuleTokens = Utils.splitByTokens(bestMatchedRule.getExpressionBefore());
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
