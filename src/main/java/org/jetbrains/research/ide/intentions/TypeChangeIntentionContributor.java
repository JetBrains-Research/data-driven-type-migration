package org.jetbrains.research.ide.intentions;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.IntentionMenuContributor;
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ide.services.TypeChangeRefactoringProviderImpl;

import javax.swing.*;

class TypeChangeIntentionContributor implements IntentionMenuContributor {
    private final Icon icon = AllIcons.Actions.SuggestedRefactoringBulb;

    @Override
    public void collectActions(@NotNull Editor hostEditor,
                               @NotNull PsiFile hostFile,
                               @NotNull ShowIntentionsPass.IntentionsInfo intentions,
                               int passIdToShowIntentionsFor,
                               int offset) {
        final var state = TypeChangeRefactoringProviderImpl.getInstance(hostEditor.getProject()).getState();
        if (state.getRelevantTypeChangeForOffset(offset).isEmpty() || !state.refactoringEnabled) return;

        final var sourceType = state.getSourceTypeByOffset(offset);
        if (sourceType.isEmpty()) return;
        final var intention = new SuggestedTypeChangeIntention(sourceType.get());

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
