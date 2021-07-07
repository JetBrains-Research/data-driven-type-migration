package org.jetbrains.research.ide.intentions;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.DataDrivenTypeMigrationBundle;
import org.jetbrains.research.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ide.migration.TypeChangeProcessor;

import java.util.List;

public class TypeChangesListPopupStep extends BaseListPopupStep<TypeChangePatternDescriptor> {
    private final Boolean isRootTypeAlreadyChanged;
    private final Project project;
    private final PsiElement context;
    private TypeChangePatternDescriptor selectedPatternDescriptor = null;

    public TypeChangesListPopupStep(String caption,
                                    List<TypeChangePatternDescriptor> rulesDescriptors,
                                    PsiElement context,
                                    Project project,
                                    Boolean isRootTypeAlreadyChanged) {
        super(caption, rulesDescriptors);
        this.context = context;
        this.project = project;
        this.isRootTypeAlreadyChanged = isRootTypeAlreadyChanged;
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
        return () -> WriteCommandAction.runWriteCommandAction(project, () -> {
            final var processor = new TypeChangeProcessor(project, isRootTypeAlreadyChanged);
            processor.run(context, selectedPatternDescriptor);
        });
    }
}
