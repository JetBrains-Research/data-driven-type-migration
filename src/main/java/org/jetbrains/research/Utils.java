package org.jetbrains.research;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Utils {
    public static String[] splitByTokens(String source) {
        return source.split("[\\s.()]+");
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
}
