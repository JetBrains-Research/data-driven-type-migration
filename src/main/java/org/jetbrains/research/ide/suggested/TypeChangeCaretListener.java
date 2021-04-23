package org.jetbrains.research.ide.suggested;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ide.services.TypeMigrationRefactoringProviderImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TypeChangeCaretListener implements CaretListener {
    private static TypeChangeCaretListener caretListener = null;
    private final Map<Editor, RangeHighlighter> editorsAndHighlighters = new HashMap<>();

    public static TypeChangeCaretListener getInstance() {
        if (caretListener == null) {
            caretListener = new TypeChangeCaretListener();
        }
        return caretListener;
    }

    @Override
    public void caretPositionChanged(@NotNull CaretEvent event) {
        final var editor = event.getEditor();
        final var project = editor.getProject();
        final int offset = Objects.requireNonNull(event.getCaret()).getOffset();

        final var state = TypeMigrationRefactoringProviderImpl.getInstance(project).getState();
        if (!state.shouldProvideRefactoring(offset)) return;

        final var prevHighlighter = editorsAndHighlighters.get(editor);
        if (prevHighlighter != null) {
            editor.getMarkupModel().removeHighlighter(prevHighlighter);
            editorsAndHighlighters.remove(editor);
        }

        final var highlighterRange = state.affectedTextRangeToTargetTypeName.keySet()
                .stream().filter(it -> it.contains(offset))
                .findFirst()
                .get();

        final var highlighter = editor.getMarkupModel().addRangeHighlighter(
                highlighterRange.getStartOffset(),
                highlighterRange.getEndOffset(),
                HighlighterLayer.LAST,
                new TextAttributes(),
                HighlighterTargetArea.EXACT_RANGE
        );

        highlighter.setGutterIconRenderer(new TypeMigrationGutterIconRenderer(offset));
        editorsAndHighlighters.put(editor, highlighter);
    }
}
