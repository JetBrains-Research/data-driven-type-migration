package org.jetbrains.research.legacy;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

public class TypeRelevantUsagesVisitor extends JavaRecursiveElementVisitor {
    private final TypeRelevantUsagesProcessor processor;

    TypeRelevantUsagesVisitor(TypeRelevantUsagesProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void visitReturnStatement(PsiReturnStatement statement) {
        super.visitReturnStatement(statement);

        final PsiElement method = PsiTreeUtil.getParentOfType(statement, PsiMethod.class);
        final PsiExpression value = statement.getReturnValue();

        if (method != null && value != null) {
            processor.addMigrationRoot(method);
        }
    }

    @Override
    public void visitLocalVariable(PsiLocalVariable variable) {
        super.visitLocalVariable(variable);
    }

    @Override
    public void visitLambdaExpression(PsiLambdaExpression expression) {
    }

    @Override
    public void visitClass(PsiClass aClass) {
    }
}
