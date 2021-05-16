package org.jetbrains.research.ide.refactoring;

import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.research.utils.CommonUtils;

import java.util.*;

public class TypeChangeSuggestedRefactoringState {
    // TODO: encapsulate
    public final Map<RangeMarker, String> initialMarkerToSourceTypeMappings;
    public final Queue<TypeChangeDescriptor> completeTypeChanges;

    public volatile boolean refactoringEnabled;

    public TypeChangeSuggestedRefactoringState() {
        this.refactoringEnabled = false;
        this.initialMarkerToSourceTypeMappings = new HashMap<>();
        this.completeTypeChanges = new ArrayDeque<>();
    }

    public Optional<String> getSourceTypeByOffset(int offset) {
        return initialMarkerToSourceTypeMappings.entrySet().stream()
                .filter(entry -> {
                    final var range = TextRange.from(
                            entry.getKey().getStartOffset(),
                            entry.getValue().length());
                    return range.contains(offset);
                })
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public Optional<TypeChangeDescriptor> getRelevantTypeChangeForOffset(int offset) {
        return completeTypeChanges.stream()
                .filter(it -> {
                    final var newRange = it.newRangeMarker;
                    // To cover whitespace on the right of the token and exclude symbol on the left
                    final var greedyToRightRange = new TextRange(
                            newRange.getStartOffset() + 1,
                            newRange.getEndOffset() + 1
                    );
                    return greedyToRightRange.contains(offset);
                })
                .findFirst();
    }

    public void addCompleteTypeChange(RangeMarker relevantOldRange, RangeMarker newRange,
                                      String relevantSourceType, String targetType) {
        final var typeChange = new TypeChangeDescriptor(relevantOldRange, newRange, relevantSourceType, targetType);
        completeTypeChanges.add(typeChange);
    }

    public void removeLastCompleteTypeChange() {
        if (!completeTypeChanges.isEmpty()) {
            completeTypeChanges.poll();
        }
    }

    public void removeAllTypeChangesByRange(TextRange range) {
        initialMarkerToSourceTypeMappings.keySet().removeIf(oldMarker -> CommonUtils.intersects(oldMarker, range));
        completeTypeChanges.removeIf(typeChange -> CommonUtils.intersects(typeChange.newRangeMarker, range));
    }
}
