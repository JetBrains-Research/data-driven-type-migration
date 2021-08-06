package org.jetbrains.research.ddtm.ide.intentions;

import com.intellij.codeInsight.intention.PriorityAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.DataDrivenTypeMigrationBundle;
import org.jetbrains.research.ddtm.data.TypeChangeRulesStorage;
import org.jetbrains.research.ddtm.ide.refactoring.TypeChangeGutterIconRenderer;
import org.jetbrains.research.ddtm.ide.refactoring.TypeChangeMarker;

public class SuggestedTypeChangeIntention extends PsiElementBaseIntentionAction implements PriorityAction {
    private final String sourceType;
    private final String targetType;

    public SuggestedTypeChangeIntention(TypeChangeMarker typeChangeMarker) {
        this.sourceType = typeChangeMarker.sourceType;
        this.targetType = typeChangeMarker.targetType;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return DataDrivenTypeMigrationBundle.message("intention.family.name");
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return DataDrivenTypeMigrationBundle.message("intention.suggested.text");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
            throws IncorrectOperationException {
        final var storage = project.getService(TypeChangeRulesStorage.class);
        final var pattern = storage.findPattern(sourceType, targetType);
        if (pattern.isEmpty()) return;

        (new TypeChangeGutterIconRenderer(element.getTextOffset())).showRefactoringOpportunity(project, editor);

//        ListPopup suggestionsPopup = JBPopupFactory.getInstance().createListPopup(
//                new TypeChangesListPopupStep(
//                        DataDrivenTypeMigrationBundle.message("intention.list.caption"),
//                        Collections.singletonList(pattern.get()),
//                        element,
//                        project,
//                        InvocationWorkflow.REACTIVE
//                )
//        );
//        suggestionsPopup.showInBestPositionFor(editor);
    }

    @Override
    public @NotNull Priority getPriority() {
        return PriorityAction.Priority.TOP;
    }
}