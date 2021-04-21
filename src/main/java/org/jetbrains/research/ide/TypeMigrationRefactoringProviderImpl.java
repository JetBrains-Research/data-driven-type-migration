package org.jetbrains.research.ide;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ProjectDisposeAwareDocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class TypeMigrationRefactoringProviderImpl implements TypeMigrationRefactoringProvider {
    private final TypeMigrationSuggestedRefactoringState state = new TypeMigrationSuggestedRefactoringState();
    public Project project;

    public TypeMigrationRefactoringProviderImpl(Project project) {
        this.project = project;
    }

    public static TypeMigrationRefactoringProviderImpl getInstance(Project project) {
        return (TypeMigrationRefactoringProviderImpl) TypeMigrationRefactoringProvider.getInstance(project);
    }

    public TypeMigrationSuggestedRefactoringState getState() {
        return state;
    }

    static class Startup implements StartupActivity.DumbAware {
        @Override
        public void runActivity(@NotNull Project project) {
            if (!ApplicationManager.getApplication().isUnitTestMode()) {

                // Run listeners
                EditorFactory.getInstance().getEventMulticaster().addDocumentListener(
                        ProjectDisposeAwareDocumentListener.create(project, new TypeChangeDocumentListener(project)),
                        project
                );

                EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
                    @Override
                    public void editorCreated(@NotNull EditorFactoryEvent event) {
                        event.getEditor().getCaretModel().addCaretListener(TypeChangeCaretListener.getInstance());
                    }

                    @Override
                    public void editorReleased(@NotNull EditorFactoryEvent event) {
                        event.getEditor().getCaretModel().removeCaretListener(TypeChangeCaretListener.getInstance());
                    }
                }, project);

            }
        }
    }
}
