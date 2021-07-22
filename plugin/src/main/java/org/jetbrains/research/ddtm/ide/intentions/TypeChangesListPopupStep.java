package org.jetbrains.research.ddtm.ide.intentions;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.ddtm.DataDrivenTypeMigrationBundle;
import org.jetbrains.research.ddtm.data.enums.InvocationWorkflow;
import org.jetbrains.research.ddtm.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ddtm.ide.migration.TypeChangeProcessor;

import java.util.List;

public class TypeChangesListPopupStep extends BaseListPopupStep<TypeChangePatternDescriptor> {
    private final InvocationWorkflow invocationWorkflow;
    private final Project project;
    private final PsiElement context;
    private TypeChangePatternDescriptor selectedPatternDescriptor = null;

    public TypeChangesListPopupStep(String caption,
                                    List<TypeChangePatternDescriptor> rulesDescriptors,
                                    PsiElement context,
                                    Project project,
                                    InvocationWorkflow invocationWorkflow) {
        super(caption, rulesDescriptors);
        this.context = context;
        this.project = project;
        this.invocationWorkflow = invocationWorkflow;
    }

    @Override
    public @Nullable PopupStep<?> onChosen(TypeChangePatternDescriptor selectedValue, boolean finalChoice) {
        selectedPatternDescriptor = selectedValue;
        return super.onChosen(selectedValue, finalChoice);
    }

    @Override
    public @NotNull String getTextFor(TypeChangePatternDescriptor value) {
        return DataDrivenTypeMigrationBundle.message("intention.list.item.text", value.getSourceType(), value.getTargetType());
    }

    @Override
    public @Nullable Runnable getFinalRunnable() {
        return () -> {
            final var processor = new TypeChangeProcessor(project, invocationWorkflow);
            ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                WriteCommandAction.writeCommandAction(project)
                        .withName(DataDrivenTypeMigrationBundle.message("group.id") + ": " + selectedPatternDescriptor.toString())
                        .withGlobalUndo()
                        .run(() -> processor.run(context, selectedPatternDescriptor));
            }, DataDrivenTypeMigrationBundle.message("intention.family.name"), false, project);
        };
    }
}
