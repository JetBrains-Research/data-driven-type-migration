package org.jetbrains.research.ddtm.ide.refactoring;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;
import org.jetbrains.research.ddtm.ide.refactoring.services.TypeChangeRefactoringProviderImpl;

import java.util.HashMap;
import java.util.Map;

public class ReactiveTypeChangeAvailabilityUpdater {
    private static ReactiveTypeChangeAvailabilityUpdater INSTANCE = null;

    private final Project project;
    private final Map<Editor, RangeHighlighter> editorsAndHighlighters = new HashMap<>();

    private ReactiveTypeChangeAvailabilityUpdater(Project project) {
        this.project = project;
    }

    public static ReactiveTypeChangeAvailabilityUpdater getInstance(Project project) {
        if (INSTANCE == null) {
            INSTANCE = new ReactiveTypeChangeAvailabilityUpdater(project);
        }
        return INSTANCE;
    }

    public void updateAllHighlighters(Document document, int caretOffset) {
        EditorFactory.getInstance().editors(document, project).forEach(editor ->
                updateHighlighter(editor, caretOffset)
        );
    }

    public void updateHighlighter(Editor editor, int caretOffset) {
        final var state = TypeChangeRefactoringProviderImpl.getInstance(project).getState();
        final var relevantTypeChangeForCurrentOffset = state.getCompletedTypeChangeForOffset(caretOffset);

        final var prevHighlighter = editorsAndHighlighters.get(editor);
        if (prevHighlighter != null) {
            editor.getMarkupModel().removeHighlighter(prevHighlighter);
            editorsAndHighlighters.remove(editor);
        }

        if (state.refactoringEnabled && relevantTypeChangeForCurrentOffset.isPresent()) {
            final var highlighterRangeMarker = relevantTypeChangeForCurrentOffset.get().newRangeMarker;
            final var highlighter = editor.getMarkupModel().addRangeHighlighter(
                    null,
                    highlighterRangeMarker.getStartOffset() + 1,
                    highlighterRangeMarker.getEndOffset() + 1,
                    HighlighterLayer.LAST,
                    HighlighterTargetArea.EXACT_RANGE
            );
            highlighter.setGutterIconRenderer(new TypeChangeGutterIconRenderer(caretOffset));
            editorsAndHighlighters.put(editor, highlighter);
        }
    }
}
