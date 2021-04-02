package org.jetbrains.research;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.structuralsearch.MatchOptions;
import com.intellij.structuralsearch.MatchResult;
import com.intellij.structuralsearch.Matcher;
import com.intellij.structuralsearch.plugin.replace.ReplaceOptions;
import com.intellij.structuralsearch.plugin.replace.impl.Replacer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Utils {
    private static final Logger LOG = Logger.getInstance(Utils.class);

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

    @Nullable
    public static PsiType getType(PsiTypeCodeFragment fragment) {
        try {
            return fragment.getType();
        } catch (PsiTypeCodeFragment.TypeSyntaxException | PsiTypeCodeFragment.NoTypeException e) {
            LOG.debug(e);
            return null;
        }
    }
}
