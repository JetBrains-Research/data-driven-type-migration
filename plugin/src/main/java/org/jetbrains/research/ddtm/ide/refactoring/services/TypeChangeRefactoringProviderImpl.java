package org.jetbrains.research.ddtm.ide.refactoring.services;

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

    static class Startup implements StartupActivity.Background {
        private static final Logger LOG = Logger.getInstance(Startup.class);

        @Override
        public void runActivity(@NotNull Project project) {
            if (!ApplicationManager.getApplication().isUnitTestMode()) {
                Config.project = project;

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

                TypeChangeSuggestedRefactoringState state = TypeChangeRefactoringProviderImpl.getInstance(project).getState();
                Thread uncompletedTypeChangesCollector = new Thread(() -> {
                    try {
                        // Probably, this is a busy waiting. But such way of "garbage collection" helps to avoid bugs
                        // with the reactive type-change intention after applying the undoable actions.
                        while (true) {
                            Thread.sleep(Config.GARBAGE_COLLECTOR_FACTOR * TypeChangeSettingsState.getInstance().disableIntentionTimeout);
                            state.uncompletedTypeChanges.clear();
                        }
                    } catch (InterruptedException e) {
                        LOG.error(e);
                    }
                });
                uncompletedTypeChangesCollector.start();
            }
        }
    }
}
