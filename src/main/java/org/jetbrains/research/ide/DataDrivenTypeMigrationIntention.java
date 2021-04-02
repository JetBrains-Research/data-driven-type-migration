package org.jetbrains.research.ide;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.utils.PsiUtils;
import org.jetbrains.research.migration.DataDrivenRulesStorage;

import java.util.Objects;

public class DataDrivenTypeMigrationIntention extends PsiElementBaseIntentionAction {
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
        PsiTypeElement parentType = PsiUtils.getHighestParentOfType(element, PsiTypeElement.class);
        if (parentType != null) {
            String parentTypeQualifiedName = parentType.getType().getCanonicalText();
            return !DataDrivenRulesStorage.getRulesDescriptorsBySourceType(parentTypeQualifiedName).isEmpty();
        }
        return false;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
            throws IncorrectOperationException {
        PsiType rootType = Objects.requireNonNull(PsiUtils.getHighestParentOfType(element, PsiTypeElement.class)).getType();
        ListPopup suggestionsPopup = JBPopupFactory.getInstance().createListPopup(
                new TypeMigrationsListPopupStep(
                        "Type Migration Rules",
                        DataDrivenRulesStorage.getRulesDescriptorsBySourceType(rootType.getCanonicalText()),
                        element, project
                )
        );
        suggestionsPopup.showInBestPositionFor(editor);
    }
}