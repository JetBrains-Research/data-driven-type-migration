package org.jetbrains.research.ddtm.ide.inspections;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiTypeElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.DataDrivenTypeMigrationBundle;
import org.jetbrains.research.ddtm.data.TypeChangeRulesStorage;
import org.jetbrains.research.ddtm.data.models.TypeChangePatternDescriptor;

import java.util.List;
import java.util.stream.Collectors;

public class TypeChangeInspection extends AbstractBaseJavaLocalInspectionTool {
    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @NonNls
            private final String DESCRIPTION_TEMPLATE = DataDrivenTypeMigrationBundle.message("inspection.problem.descriptor");

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
                    holder.registerProblem(type, DESCRIPTION_TEMPLATE, new TypeChangeQuickFix(inspectionPatterns));
                }
            }
        };
    }
}
