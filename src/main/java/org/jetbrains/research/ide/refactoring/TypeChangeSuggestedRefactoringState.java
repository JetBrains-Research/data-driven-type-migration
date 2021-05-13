package org.jetbrains.research.ide.refactoring;

import com.intellij.openapi.util.TextRange;

import java.util.*;

public class TypeChangeSuggestedRefactoringState {
    public final Map<TextRange, String> affectedRangeToSourceTypeMappings;
    public Set<TypeChangeDescriptor> typeChanges;

    public volatile boolean refactoringEnabled;

    public TypeChangeSuggestedRefactoringState() {
        this.refactoringEnabled = false;
        this.affectedRangeToSourceTypeMappings = new HashMap<>();
        this.typeChanges = new HashSet<>();
    }

    public Optional<String> getSourceTypeByOffset(int offset) {
        return affectedRangeToSourceTypeMappings.keySet().stream()
                .filter(it -> it.contains(offset))
                .findFirst()
                .map(affectedRangeToSourceTypeMappings::get);
    }

    public Optional<TypeChangeDescriptor> getRelevantTypeChangeForOffset(int offset) {
        return typeChanges.stream().filter(it -> it.newRange.contains(offset)).findFirst();
    }

    public void addTypeChange(TextRange relevantOldRange, TextRange newRange, String relevantSourceType, String targetType) {
        typeChanges.add(new TypeChangeDescriptor(relevantOldRange, newRange, relevantSourceType, targetType));
    }
}
