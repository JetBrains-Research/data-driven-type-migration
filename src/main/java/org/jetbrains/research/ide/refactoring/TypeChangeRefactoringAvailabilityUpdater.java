package org.jetbrains.research.ide.refactoring;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import org.jetbrains.research.ide.services.TypeChangeRefactoringProviderImpl;

import java.util.HashMap;
import java.util.Map;

public class TypeChangeRefactoringAvailabilityUpdater {
    private static TypeChangeRefactoringAvailabilityUpdater INSTANCE = null;

    private final Project project;
    private final Map<Editor, RangeHighlighter> editorsAndHighlighters = new HashMap<>();

    private TypeChangeRefactoringAvailabilityUpdater(Project project) {
        this.project = project;
    }

    public static TypeChangeRefactoringAvailabilityUpdater getInstance(Project project) {
        if (INSTANCE == null) {
            INSTANCE = new TypeChangeRefactoringAvailabilityUpdater(project);
        }
        return INSTANCE;
    }

    public void updateHighlighter(Editor editor, int caretOffset) {
        final var state = TypeChangeRefactoringProviderImpl.getInstance(project).getState();
        final var relevantTypeChangeForCurrentOffset = state.getRelevantTypeChangeForOffset(caretOffset);

        final var prevHighlighter = editorsAndHighlighters.get(editor);
        if (prevHighlighter != null) {
            editor.getMarkupModel().removeHighlighter(prevHighlighter);
            editorsAndHighlighters.remove(editor);
        }

        if (state.refactoringEnabled && relevantTypeChangeForCurrentOffset.isPresent()) {
            final var highlighterRange = relevantTypeChangeForCurrentOffset.get().newRange;
            final var highlighter = editor.getMarkupModel().addRangeHighlighter(
                    highlighterRange.getStartOffset(),
                    highlighterRange.getEndOffset(),
                    HighlighterLayer.LAST,
                    new TextAttributes(),
                    HighlighterTargetArea.EXACT_RANGE
            );
            highlighter.setGutterIconRenderer(new TypeChangeGutterIconRenderer(caretOffset));
            editorsAndHighlighters.put(editor, highlighter);
        }
    }
}
