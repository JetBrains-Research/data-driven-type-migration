package org.jetbrains.research.utils;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiType;
import com.intellij.structuralsearch.MatchOptions;
import com.intellij.structuralsearch.MatchResult;
import com.intellij.structuralsearch.Matcher;
import com.intellij.structuralsearch.plugin.replace.ReplaceOptions;
import com.intellij.structuralsearch.plugin.replace.impl.Replacer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.GlobalState;

import java.util.List;

public class StringUtils {
    public static String[] splitByTokens(String source) {
        return source.split("[\\s.()<>]+");
    }

    public static List<MatchResult> findMatches(String source, String pattern) {
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
