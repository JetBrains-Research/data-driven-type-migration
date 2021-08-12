package org.jetbrains.research.ddtm.ide.refactoring;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.ddtm.DataDrivenTypeMigrationBundle;
import org.jetbrains.research.ddtm.data.enums.InvocationWorkflow;
import org.jetbrains.research.ddtm.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ddtm.ide.migration.TypeChangeProcessor;
import org.jetbrains.research.ddtm.ide.settings.TypeChangeSettingsState;
import org.jetbrains.research.ddtm.ide.ui.TypeChangeClassicModePanel;
import org.jetbrains.research.ddtm.utils.PsiRelatedUtils;

import javax.swing.*;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TypeChangeDialog extends DialogWrapper {
    private final InvocationWorkflow invocationWorkflow;
    private final Project project;
    private final PsiElement context;
    private final TypeChangeClassicModePanel panel;

    public TypeChangeDialog(List<TypeChangePatternDescriptor> rulesDescriptors,
                            InvocationWorkflow invocationWorkflow, PsiElement context, Project project) {
        super(true);
        this.project = project;
        this.invocationWorkflow = invocationWorkflow;
        this.context = context;
        PsiType sourceType = Objects.requireNonNull(PsiRelatedUtils.getClosestPsiTypeElement(context)).getType();

        this.panel = new TypeChangeClassicModePanel(
                rulesDescriptors.stream()
                        .sorted(Comparator.comparing(TypeChangePatternDescriptor::getRank).reversed())
                        .collect(Collectors.toList()),
                sourceType,
                project
        );
        setTitle(DataDrivenTypeMigrationBundle.message("settings.display.name"));
        setOKButtonText(DataDrivenTypeMigrationBundle.message("suggested.gutter.popup.button"));
        init();
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        final var processor = new TypeChangeProcessor(project, invocationWorkflow);
        TypeChangeSettingsState.getInstance().searchScope = panel.getSearchScopeOption();

        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
            processor.run(context, panel.getSelectedPatternDescriptor());
        }, DataDrivenTypeMigrationBundle.message("intention.family.name"), false, project);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return panel;
    }
}
