package org.jetbrains.research.ide;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.migration.json.DataDrivenTypeMigrationRulesDescriptor;

import java.util.List;

public class TypeMigrationsListPopupStep extends BaseListPopupStep<DataDrivenTypeMigrationRulesDescriptor> {

    private DataDrivenTypeMigrationRulesDescriptor selectedDescriptor = null;
    private final Project project;
    private final PsiElement element;

    public TypeMigrationsListPopupStep(String caption,
                                       List<DataDrivenTypeMigrationRulesDescriptor> rulesDescriptors,
                                       PsiElement element,
                                       Project project) {
        super(caption, rulesDescriptors);
        this.element = element;
        this.project = project;
    }

    @Override
    public @Nullable PopupStep<?> onChosen(DataDrivenTypeMigrationRulesDescriptor selectedValue, boolean finalChoice) {
        selectedDescriptor = selectedValue;
        return super.onChosen(selectedValue, finalChoice);
    }

    @Override
    public @NotNull String getTextFor(DataDrivenTypeMigrationRulesDescriptor value) {
        return value.getSourceType() + " to " + value.getTargetType();
    }

    @Override
    public @Nullable Runnable getFinalRunnable() {
        return () -> WriteCommandAction.runWriteCommandAction(project, () -> {
            final var processor = new DataDrivenTypeMigrationProcessor(element, project);
            processor.migrate(selectedDescriptor);
        });
    }
}
