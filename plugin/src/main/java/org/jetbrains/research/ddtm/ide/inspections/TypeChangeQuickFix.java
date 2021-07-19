package org.jetbrains.research.ddtm.ide.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.DataDrivenTypeMigrationBundle;
import org.jetbrains.research.ddtm.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ddtm.ide.intentions.TypeChangesListPopupStep;

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
        ListPopup suggestionsPopup = JBPopupFactory.getInstance().createListPopup(
                new TypeChangesListPopupStep(
                        DataDrivenTypeMigrationBundle.message("intention.list.caption"),
                        inspectionPatterns,
                        descriptor.getPsiElement(),
                        project,
                        false
                )
        );
        final Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) return;
        suggestionsPopup.showInBestPositionFor(editor);
    }
}
