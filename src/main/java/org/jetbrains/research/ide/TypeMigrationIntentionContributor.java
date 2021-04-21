package org.jetbrains.research.ide;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.IntentionMenuContributor;
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

class TypeMigrationIntentionContributor implements IntentionMenuContributor {
    private final Icon icon = AllIcons.Actions.SuggestedRefactoringBulb;

    @Override
    public void collectActions(@NotNull Editor hostEditor,
                               @NotNull PsiFile hostFile,
                               @NotNull ShowIntentionsPass.IntentionsInfo intentions,
                               int passIdToShowIntentionsFor,
                               int offset) {
        final var state = TypeMigrationRefactoringProviderImpl.getInstance(hostEditor.getProject()).getState();
        if (!state.shouldProvideRefactoring(offset)) return;

        final var intention = new DataDrivenTypeMigrationIntention();

        // we add it into 'errorFixesToShow' if it's not empty to always be at the top of the list
        // we don't add into it if it's empty to keep the color of the bulb
        final var collectionToAdd =
                (intentions.errorFixesToShow == null || intentions.errorFixesToShow.isEmpty())
                        ? intentions.inspectionFixesToShow
                        : intentions.errorFixesToShow;
        collectionToAdd.add(0, new HighlightInfo.IntentionActionDescriptor(
                intention, null, null, icon, null, null, null)
        );
    }
}
