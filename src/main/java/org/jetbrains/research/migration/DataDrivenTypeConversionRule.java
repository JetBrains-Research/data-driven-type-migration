package org.jetbrains.research.migration;

import com.intellij.ide.highlighter.JavaFileType;
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
import org.jetbrains.research.migration.json.DataDrivenTypeMigrationRule;
import org.jetbrains.research.migration.json.DataDrivenTypeMigrationRulesDescriptor;

import java.util.List;

public class DataDrivenTypeConversionRule extends TypeConversionRule {
    private static final int MAX_PARENTS_TO_LIFT_UP = 2;

    private List<MatchResult> findMatches(PsiElement sourceElement, String pattern) {
        final MatchOptions options = new MatchOptions();
        options.setSearchPattern(pattern);
        options.setFileType(JavaFileType.INSTANCE);
        final Matcher matcher = new Matcher(sourceElement.getProject(), options);
        return matcher.testFindMatches(sourceElement.getText(), false, JavaFileType.INSTANCE, false);
    }

    @Override
    public @Nullable TypeConversionDescriptorBase findConversion(
            PsiType from, PsiType to, PsiMember member, PsiExpression context, TypeMigrationLabeler labeler
    ) {
        PsiElement current = context;
        if (context == null) {
            return null;
        }
        int parentsPassed = 0;
        DataDrivenTypeMigrationRule bestRule = null;
        while (parentsPassed < MAX_PARENTS_TO_LIFT_UP) {
            DataDrivenTypeMigrationRulesDescriptor descriptor =
                    DataDrivenRulesStorage.findDescriptor(from.getCanonicalText(), to.getCanonicalText());
            if (descriptor != null) {
                List<DataDrivenTypeMigrationRule> rules = descriptor.getRules();
                for (var rule : rules) {
                    List<MatchResult> matches = findMatches(current, rule.getExpressionBefore());
                    if (!matches.isEmpty()) {
                        if (bestRule == null
                                || bestRule.getExpressionBefore().length() < rule.getExpressionBefore().length()) {
                            // TODO: number of psi nodes
                            bestRule = rule;
                        }
                    }
                }
            }
            current = current.getParent();
            parentsPassed++;
        }
        if (bestRule != null) {
            return new TypeConversionDescriptor(bestRule.getExpressionBefore(), bestRule.getExpressionAfter());
        }
        return null;
    }
}
