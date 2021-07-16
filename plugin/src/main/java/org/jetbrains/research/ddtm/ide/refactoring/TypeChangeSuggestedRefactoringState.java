package org.jetbrains.research.ddtm.ide.refactoring;

import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.research.ddtm.utils.EditorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class TypeChangeSuggestedRefactoringState {
    // TODO: encapsulate
    public final Map<RangeMarker, String> uncompletedTypeChanges;
    public final List<TypeChangeMarker> completedTypeChanges;

    public volatile boolean refactoringEnabled;
    public boolean isInternalTypeChangeInProgress = false;

    public TypeChangeSuggestedRefactoringState() {
        this.refactoringEnabled = false;
        this.uncompletedTypeChanges = new ConcurrentHashMap<>();
        this.completedTypeChanges = new ArrayList<>();
    }

    public Optional<TypeChangeMarker> getCompletedTypeChangeForOffset(int offset) {
        return completedTypeChanges.stream()
                .filter(it -> it.newRangeMarker.getStartOffset() <= offset && offset < it.newRangeMarker.getEndOffset())
                .reduce((x, y) -> y); // ~ findLast()
    }

    public boolean hasUncompletedTypeChangeForOffset(int offset) {
        return uncompletedTypeChanges.keySet().stream()
                .anyMatch(it -> it.getStartOffset() <= offset && offset < it.getEndOffset());
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

    public void clear() {
        uncompletedTypeChanges.clear();
        completedTypeChanges.clear();
    }
}
