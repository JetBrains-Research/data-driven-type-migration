package org.jetbrains.research.ddtm.ide.refactoring.services;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.IntentionMenuContributor;
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.data.models.TypeChangeRuleDescriptor;
import org.jetbrains.research.ddtm.ide.intentions.FailedTypeChangeRecoveringIntention;
import org.jetbrains.research.ddtm.ide.intentions.SuggestedTypeChangeIntention;
import org.jetbrains.research.ddtm.ide.migration.collectors.TypeChangesInfoCollector;
import org.jetbrains.research.ddtm.ide.refactoring.TypeChangeMarker;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

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
        final Optional<TypeChangeRuleDescriptor> rule = failedTypeChangesCollector.getRuleForFailedUsage(context);

        IntentionAction intention;
        if (rule.isPresent()) {
            intention = new FailedTypeChangeRecoveringIntention(rule.get());
        } else {
            final var state = TypeChangeRefactoringProvider.getInstance(hostEditor.getProject()).getState();
            final var typeChangeMarker = state.getCompletedTypeChangeForOffset(offset);
            if (typeChangeMarker.isEmpty() || !state.refactoringEnabled) return;
            TypeChangeMarker marker = typeChangeMarker.get();
            if (Objects.equals(marker.sourceType, marker.targetType))
                return; // to prevent strange bugs with reactive intention

            intention = new SuggestedTypeChangeIntention(marker);
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
