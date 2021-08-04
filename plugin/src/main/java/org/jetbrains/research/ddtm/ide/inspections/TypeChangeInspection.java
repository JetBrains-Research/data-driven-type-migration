package org.jetbrains.research.ddtm.ide.inspections;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.DataDrivenTypeMigrationBundle;
import org.jetbrains.research.ddtm.data.TypeChangeRulesStorage;
import org.jetbrains.research.ddtm.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ddtm.utils.StringUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class TypeChangeInspection extends AbstractBaseJavaLocalInspectionTool {
    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitTypeElement(PsiTypeElement type) {
                super.visitTypeElement(type);
                final Project project = holder.getProject();
                final TypeChangeRulesStorage storage = project.getService(TypeChangeRulesStorage.class);
                final String sourceType = type.getType().getCanonicalText();
                if (sourceType.isEmpty() || sourceType.isBlank()) return;
                final List<TypeChangePatternDescriptor> inspectionPatterns = storage.getPatternsBySourceType(sourceType).stream()
                        .filter(TypeChangePatternDescriptor::shouldInspect)
                        .collect(Collectors.toList());
                if (!inspectionPatterns.isEmpty()) {
                    holder.registerProblem(
                            type,
                            DataDrivenTypeMigrationBundle.message("inspection.problem.descriptor", sourceType),
                            new TypeChangeQuickFix(
                                    inspectionPatterns,
                                    DataDrivenTypeMigrationBundle.message("inspection.simple.family.name")
                            )
                    );
                }
            }

            @Override
            public void visitDeclarationStatement(PsiDeclarationStatement statement) {
                super.visitDeclarationStatement(statement);
                if (statement.getDeclaredElements().length != 1) return;
                PsiElement decl = statement.getDeclaredElements()[0];

                PsiTypeElement sourceTypeElement = PsiTreeUtil.getChildOfType(decl, PsiTypeElement.class);
                if (sourceTypeElement == null) return;

                String sourceType = sourceTypeElement.getType().getCanonicalText();
                if (sourceType.equals(String.class.getCanonicalName())) {
                    PsiLiteral literal = PsiTreeUtil.getChildOfType(decl, PsiLiteral.class);
                    if (literal == null) return;

                    String value = (String) literal.getValue();
                    if (StringUtils.isSystemPath(value)) {
                        final Project project = holder.getProject();
                        final TypeChangeRulesStorage storage = project.getService(TypeChangeRulesStorage.class);
                        final var pattern = storage.findPattern(
                                String.class.getCanonicalName(),
                                Path.class.getCanonicalName()
                        ).orElseThrow(NoSuchElementException::new);

                        holder.registerProblem(
                                sourceTypeElement,
                                DataDrivenTypeMigrationBundle.message("inspection.problem.descriptor", sourceType),
                                new TypeChangeQuickFix(
                                        List.of(pattern),
                                        DataDrivenTypeMigrationBundle.message("inspection.smart.string.to.path")
                                )
                        );
                    }
                }
            }
        };
    }
}
