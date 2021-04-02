package org.jetbrains.research.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PsiUtils {
    private static final Logger LOG = Logger.getInstance(PsiUtils.class);

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
    public static PsiType getTypeOfCodeFragment(PsiTypeCodeFragment fragment) {
        try {
            return fragment.getType();
        } catch (PsiTypeCodeFragment.TypeSyntaxException | PsiTypeCodeFragment.NoTypeException e) {
            LOG.debug(e);
            return null;
        }
    }

    @Nullable
    public static PsiType getExpectedType(PsiElement element) {
        if (element instanceof PsiVariable) {
            return ((PsiVariable) element).getType();
        } else if (element instanceof PsiAssignmentExpression) {
            PsiType type = ((PsiAssignmentExpression) element).getLExpression().getType();
            return !PsiType.NULL.equals(type) ? type : null;
        } else if (element instanceof PsiMethod) {
            return ((PsiMethod) element).getReturnType();
        }
        return null;
    }
}
