package org.jetbrains.research.ide;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.GlobalState;

public class TypeMigrationStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        GlobalState.project = project;
        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            editor.getCaretModel().addCaretListener(TypeChangeCaretListener.getInstance());
        }
    }
}