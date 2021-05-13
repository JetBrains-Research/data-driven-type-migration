package org.jetbrains.research.ide.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ProjectDisposeAwareDocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.GlobalState;
import org.jetbrains.research.ide.refactoring.TypeChangeCaretListener;
import org.jetbrains.research.ide.refactoring.TypeChangeDocumentListener;
import org.jetbrains.research.ide.refactoring.TypeChangeSuggestedRefactoringState;

import java.util.Arrays;

public class TypeChangeRefactoringProviderImpl implements TypeChangeRefactoringProvider {
    private final TypeChangeSuggestedRefactoringState state = new TypeChangeSuggestedRefactoringState();
    public Project myProject;

    public TypeChangeRefactoringProviderImpl(Project project) {
        this.myProject = project;
    }

    public static TypeChangeRefactoringProviderImpl getInstance(Project project) {
        return (TypeChangeRefactoringProviderImpl) TypeChangeRefactoringProvider.getInstance(project);
    }

    public TypeChangeSuggestedRefactoringState getState() {
        return state;
    }

    static class Startup implements StartupActivity.DumbAware {
        @Override
        public void runActivity(@NotNull Project project) {
            if (!ApplicationManager.getApplication().isUnitTestMode()) {
                GlobalState.project = project;

                EditorFactory.getInstance().getEventMulticaster().addDocumentListener(
                        ProjectDisposeAwareDocumentListener.create(project, new TypeChangeDocumentListener(project)),
                        project
                );

                Arrays.stream(EditorFactory.getInstance().getAllEditors()).forEach(editor ->
                        editor.getCaretModel().addCaretListener(new TypeChangeCaretListener()));

                EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
                    @Override
                    public void editorCreated(@NotNull EditorFactoryEvent event) {
                        event.getEditor().getCaretModel().addCaretListener(new TypeChangeCaretListener());
                    }

                    @Override
                    public void editorReleased(@NotNull EditorFactoryEvent event) {
                        event.getEditor().getCaretModel().removeCaretListener(new TypeChangeCaretListener());
                    }
                }, project);

            }
        }
    }
}
