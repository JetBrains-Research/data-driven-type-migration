package org.jetbrains.research.ddtm.utils;

import com.intellij.ide.highlighter.JavaFileType;
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
    public static PsiTypeElement getClosestPsiTypeElement(PsiElement element) {
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

        return correspondingTypeElement;
    }

    public static Boolean shouldIgnoreFile(PsiFile file) {
        return !file.isPhysical() || file instanceof PsiBinaryFile || file instanceof PsiCodeFragment
                || !(file.getFileType().equals(JavaFileType.INSTANCE));
    }

    public static Boolean hasRootInside(PsiElement context, String currentRootName) {
        return context.getText().equals(currentRootName) ||
                PsiTreeUtil.findChildrenOfType(context, PsiReferenceExpression.class).stream()
                        .anyMatch(element -> element.getText().equals(currentRootName));
    }

    public static PsiElement getContainingStatement(final PsiElement root) {
        final PsiStatement statement = PsiTreeUtil.getParentOfType(root, PsiStatement.class);
        PsiExpression condition = getContainingCondition(root, statement);
        if (condition != null) return condition;
        final PsiField field = PsiTreeUtil.getParentOfType(root, PsiField.class);
        return statement != null ? statement : field != null ? field : root;
    }

    public static PsiExpression getContainingCondition(PsiElement root, PsiStatement statement) {
        PsiExpression condition = null;
        if (statement instanceof PsiConditionalLoopStatement) {
            condition = ((PsiConditionalLoopStatement) statement).getCondition();
        } else if (statement instanceof PsiIfStatement) {
            condition = ((PsiIfStatement) statement).getCondition();
        }
        return PsiTreeUtil.isAncestor(condition, root, false) ? condition : null;
    }
}
