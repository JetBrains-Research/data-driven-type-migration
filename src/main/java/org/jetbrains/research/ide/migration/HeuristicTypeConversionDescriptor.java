package org.jetbrains.research.ide.migration;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.refactoring.typeMigration.TypeConversionDescriptor;
import com.intellij.refactoring.typeMigration.TypeEvaluator;
import com.intellij.structuralsearch.MatchOptions;
import com.intellij.structuralsearch.MatchResult;
import com.intellij.structuralsearch.MatchVariableConstraint;
import com.intellij.structuralsearch.impl.matcher.CompiledPattern;
import com.intellij.structuralsearch.impl.matcher.compiler.PatternCompiler;
import com.intellij.structuralsearch.plugin.replace.ReplaceOptions;
import com.intellij.structuralsearch.plugin.replace.impl.Replacer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.utils.StringUtils;

import java.util.List;

public class HeuristicTypeConversionDescriptor extends TypeConversionDescriptor {
    private static final int MAX_PARENTS_TO_LIFT_UP = 3;

    private final String currentRootName;

    public HeuristicTypeConversionDescriptor(@NonNls String stringToReplace,
                                             @NonNls String replaceByString,
                                             @NonNls String currentRootName) {
        super(stringToReplace, replaceByString);
        this.currentRootName = currentRootName;
    }

    @Override
    public PsiExpression replace(PsiExpression expression, @NotNull TypeEvaluator evaluator) {
        Project project = expression.getProject();
        final ReplaceOptions options = new ReplaceOptions();
        final MatchOptions matchOptions = options.getMatchOptions();
        matchOptions.setFileType(JavaFileType.INSTANCE);

        MatchVariableConstraint rootConstraint = new MatchVariableConstraint("1");
        rootConstraint.setRegExp(currentRootName);
        matchOptions.addVariableConstraint(rootConstraint);

        MatchVariableConstraint nonRootConstraint = new MatchVariableConstraint("2");
        nonRootConstraint.setRegExp(currentRootName);
        nonRootConstraint.setInvertRegExp(true);
        matchOptions.addVariableConstraint(nonRootConstraint);

        final CompiledPattern compiledPattern = PatternCompiler.compilePattern(
                project, matchOptions, false, false
        );

        PsiElement currentExpression = expression;
        PsiElement bestMatchedExpression = expression;
        int parentsPassed = 0;

        while (parentsPassed < MAX_PARENTS_TO_LIFT_UP) {
            List<MatchResult> matches = StringUtils.match(
                    currentExpression.getText(),
                    getStringToReplace(),
                    currentRootName,
                    expression.getProject()
            );
            if (!matches.isEmpty()) {
                bestMatchedExpression = currentExpression;
            }
            currentExpression = currentExpression.getParent();
            parentsPassed++;
        }

        final String replacement = Replacer.testReplace(
                bestMatchedExpression.getText(), getStringToReplace(), getReplaceByString(), options, project
        );
        return (PsiExpression) JavaCodeStyleManager.getInstance(project).shortenClassReferences(
                bestMatchedExpression.replace(
                        JavaPsiFacade.getElementFactory(project).createExpressionFromText(replacement, bestMatchedExpression)
                ));
    }
}
