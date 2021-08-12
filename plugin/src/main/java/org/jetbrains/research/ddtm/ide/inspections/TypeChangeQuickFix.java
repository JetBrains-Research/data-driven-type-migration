package org.jetbrains.research.ddtm.ide.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.data.enums.InvocationWorkflow;
import org.jetbrains.research.ddtm.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ddtm.ide.refactoring.TypeChangeDialog;

import java.util.List;

public class TypeChangeQuickFix implements LocalQuickFix {
    List<TypeChangePatternDescriptor> inspectionPatterns;
    private final String familyName;

    public TypeChangeQuickFix(List<TypeChangePatternDescriptor> inspectionPatterns, String familyName) {
        this.inspectionPatterns = inspectionPatterns;
        this.familyName = familyName;
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return familyName;
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final var dialog = new TypeChangeDialog(
                    inspectionPatterns, InvocationWorkflow.PROACTIVE, descriptor.getPsiElement(), project
            );
            dialog.showAndGet();
        });
    }
}
