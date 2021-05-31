package org.jetbrains.research.utils;

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
import org.jetbrains.research.GlobalState;

import java.util.List;

public class StringUtils {
    public static String[] splitByTokens(String source) {
        return source.split("[\\s.()<>]+");
    }

    public static List<MatchResult> match(String source, String pattern, String currentRoot, Project project) {
        final MatchOptions options = new MatchOptions();
        options.setRecursiveSearch(true);
        options.fillSearchCriteria(pattern);
        options.setFileType(JavaFileType.INSTANCE);

        MatchVariableConstraint rootConstraint = new MatchVariableConstraint("1");
        rootConstraint.setRegExp(currentRoot);
        options.addVariableConstraint(rootConstraint);

        MatchVariableConstraint nonRootConstraint = new MatchVariableConstraint("2");
        nonRootConstraint.setRegExp(currentRoot);
        nonRootConstraint.setInvertRegExp(true);
        options.addVariableConstraint(nonRootConstraint);

        final CompiledPattern compiledPattern = PatternCompiler.compilePattern(
                project, options, false, false
        );
        final Matcher matcher = new Matcher(project, options, compiledPattern);
        return matcher.testFindMatches(source, false, JavaFileType.INSTANCE, false);
    }

    public static List<MatchResult> findSimpleMatches(String source, String pattern) {
        final MatchOptions options = new MatchOptions();
        options.setSearchPattern(pattern);
        options.setFileType(JavaFileType.INSTANCE);
        final Matcher matcher = new Matcher(GlobalState.project, options);
        return matcher.testFindMatches(source, false, JavaFileType.INSTANCE, false);
    }

    @NotNull
    public static String substituteTypeByPattern(@NotNull PsiType type,
                                                 String stringToSubstitute,
                                                 String substituteByString) {
        final Project project = GlobalState.project;
        final ReplaceOptions options = new ReplaceOptions();
        final MatchOptions matchOptions = options.getMatchOptions();
        matchOptions.setFileType(JavaFileType.INSTANCE);
        return Replacer.testReplace(type.getCanonicalText(), stringToSubstitute, substituteByString, options, project);
    }
}
