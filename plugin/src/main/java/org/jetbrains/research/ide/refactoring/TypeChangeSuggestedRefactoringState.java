package org.jetbrains.research.ide.refactoring;

import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.research.utils.EditorUtils;

import java.util.*;

public class TypeChangeSuggestedRefactoringState {
    // TODO: encapsulate
    public final Map<RangeMarker, String> uncompletedTypeChanges;
    public final List<TypeChangeMarker> completedTypeChanges;

    public volatile boolean refactoringEnabled;
    public boolean isInternalTypeChangeInProgress = false;

    public TypeChangeSuggestedRefactoringState() {
        this.refactoringEnabled = false;
        this.uncompletedTypeChanges = new HashMap<>();
        this.completedTypeChanges = new ArrayList<>();
    }

    public Optional<TypeChangeMarker> getCompletedTypeChangeForOffset(int offset) {
        return completedTypeChanges.stream()
                .filter(it -> {
                    final var newRange = it.newRangeMarker;
                    final var greedyToRightRange = new TextRange(
                            newRange.getStartOffset(),
                            newRange.getEndOffset()
                    );
                    return greedyToRightRange.contains(offset);
                })
                .findFirst();
    }

    public void addCompletedTypeChange(RangeMarker relevantOldRange, RangeMarker newRange,
                                       String relevantSourceType, String targetType) {
        final var typeChange = new TypeChangeMarker(relevantOldRange, newRange, relevantSourceType, targetType);
        completedTypeChanges.add(typeChange);
    }

    public void removeAllTypeChangesByRange(TextRange range) {
        uncompletedTypeChanges.keySet().removeIf(oldMarker -> EditorUtils.intersects(oldMarker, range));
        completedTypeChanges.removeIf(typeChange -> EditorUtils.intersects(typeChange.newRangeMarker, range));
    }
}
