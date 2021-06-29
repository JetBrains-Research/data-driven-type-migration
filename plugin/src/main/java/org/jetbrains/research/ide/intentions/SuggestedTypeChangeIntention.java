package org.jetbrains.research.ide.intentions;

import com.intellij.codeInsight.intention.PriorityAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.GlobalState;
import org.jetbrains.research.data.TypeChangeRulesStorage;
import org.jetbrains.research.ide.refactoring.TypeChangeMarker;

import java.util.Collections;

public class SuggestedTypeChangeIntention extends PsiElementBaseIntentionAction implements PriorityAction {
    private final String sourceType;
    private final String targetType;

    public SuggestedTypeChangeIntention(TypeChangeMarker typeChangeMarker) {
        this.sourceType = typeChangeMarker.sourceType;
        this.targetType = typeChangeMarker.targetType;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "Data-driven type migration";
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return this.getFamilyName();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
            throws IncorrectOperationException {
        GlobalState.searchScope = GlobalSearchScope.fileScope(element.getContainingFile());
        ListPopup suggestionsPopup = JBPopupFactory.getInstance().createListPopup(
                new TypeChangesListPopupStep(
                        "Type Migration Rules",
                        Collections.singletonList(TypeChangeRulesStorage.findPattern(sourceType, targetType)),
                        element,
                        project,
                        true
                )
        );
        suggestionsPopup.showInBestPositionFor(editor);
    }

    @Override
    public @NotNull Priority getPriority() {
        return PriorityAction.Priority.TOP;
    }
}