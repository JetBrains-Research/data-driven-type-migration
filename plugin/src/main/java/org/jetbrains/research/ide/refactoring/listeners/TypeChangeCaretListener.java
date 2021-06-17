package org.jetbrains.research.ide.refactoring.listeners;

import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ide.refactoring.ReactiveTypeChangeAvailabilityUpdater;

import java.util.Objects;

public class TypeChangeCaretListener implements CaretListener {

    @Override
    public void caretPositionChanged(@NotNull CaretEvent event) {
        final var editor = event.getEditor();
        final var project = editor.getProject();
        final int offset = Objects.requireNonNull(event.getCaret()).getOffset();
        ReactiveTypeChangeAvailabilityUpdater.getInstance(project).updateHighlighter(editor, offset);
    }
}
