package org.jetbrains.research.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PsiRelatedUtils {
    private static final Logger LOG = Logger.getInstance(PsiRelatedUtils.class);

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

    @Nullable
    public static String getClosestFullyQualifiedName(PsiElement element) {
        PsiTypeElement correspondingTypeElement = null;

        if (element instanceof PsiWhiteSpace) {
            if (element.getPrevSibling() instanceof PsiTypeElement) {
                correspondingTypeElement = (PsiTypeElement) element.getPrevSibling();
            } else if (element.getNextSibling() instanceof PsiTypeElement) {
                correspondingTypeElement = (PsiTypeElement) element.getNextSibling();
            }
        } else {
            correspondingTypeElement = PsiRelatedUtils.getHighestParentOfType(element, PsiTypeElement.class);
        }
        if (correspondingTypeElement == null) return null;

        return correspondingTypeElement.getType().getCanonicalText();
    }
}
