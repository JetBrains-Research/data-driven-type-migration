package org.jetbrains.research.ddtm.ide.intentions;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.ddtm.DataDrivenTypeMigrationBundle;
import org.jetbrains.research.ddtm.data.enums.InvocationWorkflow;
import org.jetbrains.research.ddtm.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ddtm.ide.migration.TypeChangeProcessor;
import org.jetbrains.research.ddtm.utils.PsiRelatedUtils;
import org.jetbrains.research.ddtm.utils.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TypeChangesListPopupStep extends BaseListPopupStep<TypeChangePatternDescriptor> {
    private final InvocationWorkflow invocationWorkflow;
    private final Project project;
    private final PsiElement context;
    private final PsiType sourceType;
    private TypeChangePatternDescriptor selectedPatternDescriptor = null;

    public TypeChangesListPopupStep(String caption,
                                    List<TypeChangePatternDescriptor> rulesDescriptors,
                                    PsiElement context,
                                    Project project,
                                    InvocationWorkflow invocationWorkflow) {
        super(caption, rulesDescriptors.stream()
                .sorted(Comparator.comparing(TypeChangePatternDescriptor::getRank).reversed())
                .collect(Collectors.toList())
        );
        this.sourceType = Objects.requireNonNull(PsiRelatedUtils.getClosestPsiTypeElement(context)).getType();
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
        return DataDrivenTypeMigrationBundle.message(
                "intention.list.item.text",
                StringUtils.escapeSSRTemplates(value.resolveTargetType(sourceType, project))
        );
    }

    @Override
    public @Nullable Runnable getFinalRunnable() {
        return () -> {
            final var processor = new TypeChangeProcessor(project, invocationWorkflow);
            ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                processor.run(context, selectedPatternDescriptor);
            }, DataDrivenTypeMigrationBundle.message("intention.family.name"), false, project);
        };
    }
}
