package org.jetbrains.research.ide.refactoring;

import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ide.services.TypeChangeRefactoringProviderImpl;

public class DisableRefactoringWatcher implements DocumentListener {
    private static DisableRefactoringWatcher INSTANCE = null;
    private final Project project;

    public DisableRefactoringWatcher(Project project) {
        this.project = project;
    }

    public static DisableRefactoringWatcher getInstance(Project project) {
        if (INSTANCE == null) {
            INSTANCE = new DisableRefactoringWatcher(project);
        }
        return INSTANCE;
    }

    @Override
    public void beforeDocumentChange(@NotNull DocumentEvent event) {
        final var state = TypeChangeRefactoringProviderImpl.getInstance(project).getState();
        if (state.refactoringEnabled) {
            final PsiFile psiFile = PsiDocumentManager.getInstance(project).getCachedPsiFile(event.getDocument());
            if (psiFile == null) return;

            final int offset = event.getOffset();
            final var newElement = psiFile.findElementAt(offset);

            // disableSuggestedRefactoring(state, event);
        }
    }

    private void disableSuggestedRefactoring(TypeChangeSuggestedRefactoringState state, DocumentEvent event) {
        state.refactoringEnabled = false;
        state.removeLastCompleteTypeChange();

        final var updater = TypeChangeRefactoringAvailabilityUpdater.getInstance(project);
        updater.updateAllHighlighters(event.getDocument(), event.getOffset());
        updater.removeDisableRefactoringWatcher();
    }
}
