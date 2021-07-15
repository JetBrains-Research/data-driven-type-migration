package org.jetbrains.research.ddtm.ide.migration;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.refactoring.typeMigration.TypeConversionDescriptor;
import com.intellij.refactoring.typeMigration.TypeEvaluator;
import com.intellij.structuralsearch.MatchOptions;
import com.intellij.structuralsearch.MatchResult;
import com.intellij.structuralsearch.plugin.replace.ReplaceOptions;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.Config;
import org.jetbrains.research.ddtm.ide.migration.structuralsearch.MyReplacer;
import org.jetbrains.research.ddtm.ide.migration.structuralsearch.SSRUtils;
import org.jetbrains.research.ddtm.utils.PsiRelatedUtils;

import java.util.List;

public class HeuristicTypeConversionDescriptor extends TypeConversionDescriptor {
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
        PsiElement currentExpression = expression;
        PsiElement bestMatchedExpression = expression;
        int parentsPassed = 0;

        while (parentsPassed < Config.MAX_PARENTS_TO_LIFT_UP) {
            if (currentExpression instanceof PsiExpression) {
                List<MatchResult> matches = SSRUtils.matchRule(getStringToReplace(), currentRootName, currentExpression, project);
                if (!matches.isEmpty() &&
                        matches.get(0).getChildren().stream()
                                .noneMatch(matchResult -> !matchResult.getName().equals("1")
                                        && PsiRelatedUtils.hasRootInside(matchResult.getMatch(), currentRootName))) {
                    bestMatchedExpression = currentExpression;
                }
            }
            currentExpression = currentExpression.getParent();
            parentsPassed++;
        }

        final ReplaceOptions options = new ReplaceOptions();
        final MatchOptions matchOptions = options.getMatchOptions();
        SSRUtils.patchMatchOptionsWithConstraints(matchOptions, getStringToReplace(), currentRootName, bestMatchedExpression);

        final String replacement = MyReplacer.testReplace(
                bestMatchedExpression.getText(), getStringToReplace(), getReplaceByString(), options, project
        );
        return (PsiExpression) JavaCodeStyleManager.getInstance(project).shortenClassReferences(
                bestMatchedExpression.replace(
                        JavaPsiFacade.getElementFactory(project).createExpressionFromText(replacement, bestMatchedExpression)
                ));
    }
}
