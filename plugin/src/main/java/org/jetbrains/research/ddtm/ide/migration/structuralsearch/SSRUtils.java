package org.jetbrains.research.ddtm.ide.migration.structuralsearch;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.structuralsearch.MatchOptions;
import com.intellij.structuralsearch.MatchResult;
import com.intellij.structuralsearch.MatchVariableConstraint;
import com.intellij.structuralsearch.Matcher;
import com.intellij.structuralsearch.impl.matcher.CompiledPattern;
import com.intellij.structuralsearch.impl.matcher.compiler.PatternCompiler;
import com.intellij.structuralsearch.plugin.replace.ReplaceOptions;
import com.intellij.structuralsearch.plugin.replace.impl.Replacer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.utils.PsiRelatedUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.regex.Pattern;

public class SSRUtils {
    public static List<MatchResult> matchRule(String pattern, String currentRootName, PsiElement sourceExpression, Project project) {
        final MatchOptions options = new MatchOptions();
        patchMatchOptionsWithConstraints(options, pattern, currentRootName, sourceExpression);
        final CompiledPattern compiledPattern = PatternCompiler.compilePattern(
                project, options, false, false
        );
        final Matcher matcher = new Matcher(project, options, compiledPattern);
        return matcher.testFindMatches(sourceExpression.getText(), false, JavaFileType.INSTANCE, false);
    }

    public static void patchMatchOptionsWithConstraints(MatchOptions options, String pattern, String currentRootName, PsiElement sourceExpression) {
        options.fillSearchCriteria(pattern);
        options.setFileType(JavaFileType.INSTANCE);

        String src = sourceExpression.getText();
        String regexForRoot = currentRootName;
        int startIndex = src.indexOf(currentRootName);
        if (startIndex != -1) {
            int i = startIndex + currentRootName.length();
            int balance = 0;
            while (i < src.length()) {
                if (src.charAt(i) == '(') balance++;
                if (src.charAt(i) == ')') balance--;
                if (balance == 0) break;
                i++;
            }
            if (balance == 0) {
                // If the current root is a function or method, it will help to match it with a "full inclusion",
                // i.e., if the current root is `someFunc`, then it will match it correctly within call expression
                // `someFunc(someStr.toLowerCase()).toUpperCase()` because of the balanced bracket sequence.
                if (i < src.length() && src.charAt(i) == ')') i++;
                regexForRoot = Pattern.quote(src.substring(startIndex, i));
            }
        }

        MatchVariableConstraint rootConstraint = new MatchVariableConstraint("1");
        rootConstraint.setRegExp(regexForRoot);
        options.addVariableConstraint(rootConstraint);

        String regexForNonRoot =
                PsiRelatedUtils.hasRootInside(sourceExpression, currentRootName)
                        ? MessageFormat.format(".*{0}.*", regexForRoot)
                        : regexForRoot;
        // There should be a regex retrieving all the variables between $$,
        // but I guess it's enough to only add constraints for 2, 3, 4, 5 with current templates
        for (int i = 2; i < 5; ++i) {
            MatchVariableConstraint nonRootConstraint = new MatchVariableConstraint(Integer.toString(i));
            nonRootConstraint.setRegExp(regexForNonRoot);
            nonRootConstraint.setInvertRegExp(true);
            options.addVariableConstraint(nonRootConstraint);
        }
    }

    public static List<MatchResult> matchType(String source, String typePattern, Project project) {
        final MatchOptions options = new MatchOptions();
        options.setSearchPattern(typePattern);
        options.setFileType(JavaFileType.INSTANCE);
        final Matcher matcher = new Matcher(project, options);
        return matcher.testFindMatches(source, false, JavaFileType.INSTANCE, false);
    }

    @NotNull
    public static String substituteTypeByPattern(@NotNull PsiType type,
                                                 String stringToSubstitute,
                                                 String substituteByString,
                                                 Project project) {
        final ReplaceOptions options = new ReplaceOptions();
        final MatchOptions matchOptions = options.getMatchOptions();
        matchOptions.setFileType(JavaFileType.INSTANCE);
        String result = Replacer.testReplace(
                type.getCanonicalText() + " x;",
                stringToSubstitute + " x;",
                substituteByString + " x;",
                options, project
        );
        return result.substring(0, result.length() - 3);
    }
}
