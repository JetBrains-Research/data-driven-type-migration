package org.jetbrains.research.ddtm.ide.migration.structuralsearch;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.Project;
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
import org.jetbrains.research.ddtm.Config;

import java.text.MessageFormat;
import java.util.List;

public class SSRUtils {
    public static List<MatchResult> matchRule(String source, String pattern, String currentRootName, Project project) {
        final MatchOptions options = new MatchOptions();
        patchMatchOptionsWithConstraints(options, pattern, currentRootName);
        final CompiledPattern compiledPattern = PatternCompiler.compilePattern(
                project, options, false, false
        );
        final Matcher matcher = new Matcher(project, options, compiledPattern);
        return matcher.testFindMatches(source, false, JavaFileType.INSTANCE, false);
    }

    public static void patchMatchOptionsWithConstraints(MatchOptions options, String pattern, String currentRootName) {
        options.fillSearchCriteria(pattern);
        options.setFileType(JavaFileType.INSTANCE);

        MatchVariableConstraint rootConstraint = new MatchVariableConstraint("1");
        rootConstraint.setRegExp(MessageFormat.format("{0}|{0}[(].*[)]", currentRootName));
        options.addVariableConstraint(rootConstraint);

        // There should be a regex retrieving all the variables between $$,
        // but I guess it's enough to only add constraints for 2, 3, 4, 5 with current templates
        for (int i = 2; i < 5; ++i) {
            MatchVariableConstraint nonRootConstraint = new MatchVariableConstraint(Integer.toString(i));
            nonRootConstraint.setRegExp(currentRootName);
            nonRootConstraint.setInvertRegExp(true);
            options.addVariableConstraint(nonRootConstraint);
        }
    }

    public static List<MatchResult> matchType(String source, String typePattern) {
        final MatchOptions options = new MatchOptions();
        options.setSearchPattern(typePattern);
        options.setFileType(JavaFileType.INSTANCE);
        final Matcher matcher = new Matcher(Config.project, options);
        return matcher.testFindMatches(source, false, JavaFileType.INSTANCE, false);
    }

    @NotNull
    public static String substituteTypeByPattern(@NotNull PsiType type,
                                                 String stringToSubstitute,
                                                 String substituteByString) {
        final Project project = Config.project;
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
