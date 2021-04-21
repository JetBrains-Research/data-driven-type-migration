package org.jetbrains.research.ide;

import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import org.jetbrains.annotations.NotNull;

public class TypeChangeCaretListener implements CaretListener {
    private static TypeChangeCaretListener caretListener = null;

    public static TypeChangeCaretListener getInstance() {
        if (caretListener == null) {
            caretListener = new TypeChangeCaretListener();
        }
        return caretListener;
    }

    @Override
    public void caretPositionChanged(@NotNull CaretEvent event) {

    }
}
