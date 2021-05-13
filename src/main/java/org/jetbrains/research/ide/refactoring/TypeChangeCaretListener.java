package org.jetbrains.research.ide.refactoring;

import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TypeChangeCaretListener implements CaretListener {

    @Override
    public void caretPositionChanged(@NotNull CaretEvent event) {
        final var editor = event.getEditor();
        final var project = editor.getProject();
        final int offset = Objects.requireNonNull(event.getCaret()).getOffset();
        TypeChangeRefactoringAvailabilityUpdater.getInstance(project).updateHighlighter(editor, offset);
    }
}
