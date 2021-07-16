package org.jetbrains.research.ddtm.ide.refactoring.services;

import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ProjectDisposeAwareDocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.Config;
import org.jetbrains.research.ddtm.ide.refactoring.TypeChangeSuggestedRefactoringState;
import org.jetbrains.research.ddtm.ide.refactoring.listeners.AfterTypeChangeCaretListener;
import org.jetbrains.research.ddtm.ide.refactoring.listeners.TypeChangeDocumentListener;
import org.jetbrains.research.ddtm.ide.settings.TypeChangeSettingsState;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class TypeChangeRefactoringProvider {
    private final TypeChangeSuggestedRefactoringState state = new TypeChangeSuggestedRefactoringState();
    public Project myProject;

    public TypeChangeRefactoringProvider(Project project) {
        this.myProject = project;
    }

    public static TypeChangeRefactoringProvider getInstance(Project project) {
        return project.getService(TypeChangeRefactoringProvider.class);
    }

    public TypeChangeSuggestedRefactoringState getState() {
        return state;
    }

    static class Startup implements StartupActivity.Background {
        private static final Logger LOG = Logger.getInstance(Startup.class);

        @Override
        public void runActivity(@NotNull Project project) {
            if (!ApplicationManager.getApplication().isUnitTestMode()) {

                EditorFactory.getInstance().getEventMulticaster().addDocumentListener(
                        ProjectDisposeAwareDocumentListener.create(project, new TypeChangeDocumentListener(project)),
                        project
                );

                Arrays.stream(EditorFactory.getInstance().getAllEditors()).forEach(editor ->
                        editor.getCaretModel().addCaretListener(new AfterTypeChangeCaretListener()));

                EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
                    @Override
                    public void editorCreated(@NotNull EditorFactoryEvent event) {
                        event.getEditor().getCaretModel().addCaretListener(new AfterTypeChangeCaretListener());
                    }

                    @Override
                    public void editorReleased(@NotNull EditorFactoryEvent event) {
                        event.getEditor().getCaretModel().removeCaretListener(new AfterTypeChangeCaretListener());
                    }
                }, project);

                TypeChangeSuggestedRefactoringState state = TypeChangeRefactoringProvider.getInstance(project).getState();
                final long delay = Config.GARBAGE_COLLECTOR_FACTOR * TypeChangeSettingsState.getInstance().disableIntentionTimeout;
                JobScheduler.getScheduler().scheduleWithFixedDelay(
                        state::clear,
                        delay,
                        delay,
                        TimeUnit.MILLISECONDS
                );
            }
        }
    }
}
