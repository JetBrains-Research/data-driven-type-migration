package org.jetbrains.research.ide.refactoring.services;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.IntentionMenuContributor;
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ide.intentions.FailedTypeChangeRecoveringIntention;
import org.jetbrains.research.ide.intentions.SuggestedTypeChangeIntention;
import org.jetbrains.research.ide.migration.collectors.TypeChangesInfoCollector;

import javax.swing.*;

class TypeChangeIntentionContributor implements IntentionMenuContributor {
    private final Icon icon = AllIcons.Actions.SuggestedRefactoringBulb;

    @Override
    public void collectActions(@NotNull Editor hostEditor,
                               @NotNull PsiFile hostFile,
                               @NotNull ShowIntentionsPass.IntentionsInfo intentions,
                               int passIdToShowIntentionsFor,
                               int offset) {
        final PsiElement context = hostFile.findElementAt(offset);
        if (context == null) return;
        final var failedTypeChangesCollector = TypeChangesInfoCollector.getInstance();
        final var rule = failedTypeChangesCollector.getRuleForFailedUsage(context);

        IntentionAction intention;
        if (rule.isPresent()) {
            intention = new FailedTypeChangeRecoveringIntention(rule.get());
        } else {
            final var state = TypeChangeRefactoringProviderImpl.getInstance(hostEditor.getProject()).getState();
            final var typeChangeMarker = state.getCompletedTypeChangeForOffset(offset);
            if (typeChangeMarker.isEmpty() || !state.refactoringEnabled) return;

            intention = new SuggestedTypeChangeIntention(typeChangeMarker.get().sourceType);
        }

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
