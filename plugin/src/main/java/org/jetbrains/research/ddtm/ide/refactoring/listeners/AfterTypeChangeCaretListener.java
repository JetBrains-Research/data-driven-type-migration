package org.jetbrains.research.ddtm.ide.refactoring.listeners;

import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.ide.refactoring.ReactiveTypeChangeAvailabilityUpdater;

public class AfterTypeChangeCaretListener implements CaretListener {

    @Override
    public void caretPositionChanged(@NotNull CaretEvent event) {
        final var editor = event.getEditor();
        final var caret = event.getCaret();
        final var project = editor.getProject();
        if (project == null || caret == null) return;

        final int offset = caret.getOffset();
        final var updater = project.getService(ReactiveTypeChangeAvailabilityUpdater.class);
        updater.updateHighlighter(editor, offset);
    }
}
