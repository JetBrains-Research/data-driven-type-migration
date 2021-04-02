package org.jetbrains.research;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.structuralsearch.MatchOptions;
import com.intellij.structuralsearch.MatchResult;
import com.intellij.structuralsearch.Matcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Utils {
    public static String[] splitByTokens(String source) {
        return source.split("[\\s.()<>]+");
    }

    public static @Nullable <T extends PsiElement> T getHighestParentOfType(
            @Nullable PsiElement element, @NotNull Class<T> aClass
    ) {
        PsiElement currentContext = element;
        while (true) {
            T parent = PsiTreeUtil.getParentOfType(currentContext, aClass);
            if (parent == null) {
                if (aClass.isInstance(currentContext)) {
                    return aClass.cast(currentContext);
                } else {
                    return null;
                }
            }
            currentContext = parent;
        }
    }

    public static List<MatchResult> findMatches(String source, String pattern) {
        final MatchOptions options = new MatchOptions();
        options.setSearchPattern(pattern);
        options.setFileType(JavaFileType.INSTANCE);
        final Matcher matcher = new Matcher(GlobalState.project, options);
        return matcher.testFindMatches(source, false, JavaFileType.INSTANCE, false);
    }
}
