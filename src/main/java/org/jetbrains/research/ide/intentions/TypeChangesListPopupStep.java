package org.jetbrains.research.ide.intentions;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.ide.TypeChangeProcessor;
import org.jetbrains.research.migration.models.TypeChangePatternDescriptor;

import java.util.List;

public class TypeChangesListPopupStep extends BaseListPopupStep<TypeChangePatternDescriptor> {
    private TypeChangePatternDescriptor selectedDescriptor = null;
    private final Project project;
    private final PsiElement context;

    public TypeChangesListPopupStep(String caption,
                                    List<TypeChangePatternDescriptor> rulesDescriptors,
                                    PsiElement context,
                                    Project project) {
        super(caption, rulesDescriptors);
        this.context = context;
        this.project = project;
    }

    @Override
    public @Nullable PopupStep<?> onChosen(TypeChangePatternDescriptor selectedValue, boolean finalChoice) {
        selectedDescriptor = selectedValue;
        return super.onChosen(selectedValue, finalChoice);
    }

    @Override
    public @NotNull String getTextFor(TypeChangePatternDescriptor value) {
        return value.getSourceType() + " to " + value.getTargetType();
    }

    @Override
    public @Nullable Runnable getFinalRunnable() {
        return () -> WriteCommandAction.runWriteCommandAction(project, () -> {
            final var processor = new TypeChangeProcessor(project);
            processor.run(context, selectedDescriptor);
        });
    }
}
