package org.jetbrains.research.ide.refactoring;

import com.intellij.openapi.util.TextRange;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TypeChangeSuggestedRefactoringState {
    public final Map<TextRange, String> affectedTextRangeToSourceTypeName;
    public final Map<TextRange, String> affectedTextRangeToTargetTypeName;
    public volatile boolean showRefactoringOpportunity;

    public TypeChangeSuggestedRefactoringState() {
        this.showRefactoringOpportunity = false;
        this.affectedTextRangeToSourceTypeName = new HashMap<>();
        this.affectedTextRangeToTargetTypeName = new HashMap<>();
    }

    public Optional<String> getSourceTypeByOffset(int offset) {
        return affectedTextRangeToSourceTypeName.keySet().stream()
                .filter(it -> it.contains(offset))
                .findFirst()
                .map(affectedTextRangeToSourceTypeName::get);
    }

    public boolean shouldProvideRefactoring(int offset) {
        return affectedTextRangeToTargetTypeName
                .keySet().stream()
                .anyMatch(it -> it.contains(offset));
    }
}
