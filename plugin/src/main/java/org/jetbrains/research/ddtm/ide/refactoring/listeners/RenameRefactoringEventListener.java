package org.jetbrains.research.ddtm.ide.refactoring.listeners;

import com.intellij.psi.PsiElement;
import com.intellij.refactoring.listeners.RefactoringEventData;
import com.intellij.refactoring.listeners.RefactoringEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.ddtm.ide.fus.TypeChangeLogsCollector;

public class RenameRefactoringEventListener implements RefactoringEventListener {
    @Override
    public void refactoringStarted(@NotNull String refactoringId, @Nullable RefactoringEventData beforeData) {
    }

    @Override
    public void refactoringDone(@NotNull String refactoringId, @Nullable RefactoringEventData afterData) {
        if (afterData == null) return;
        if (refactoringId.contains("rename")) {
            final var elementKey = afterData.get().getKeys()[0];
            final PsiElement element = (PsiElement) afterData.get().get(elementKey);
            if (element == null) return;
            TypeChangeLogsCollector.getInstance().renamePerformed(element.getProject(), element.getClass().getCanonicalName());
        }
    }

    @Override
    public void conflictsDetected(@NotNull String refactoringId, @NotNull RefactoringEventData conflictsData) {

    }

    @Override
    public void undoRefactoring(@NotNull String refactoringId) {

    }
}
