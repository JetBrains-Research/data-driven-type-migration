package org.jetbrains.research.ide;

import com.intellij.openapi.util.TextRange;

import java.util.HashMap;
import java.util.Map;

public class TypeMigrationSuggestedRefactoringState {
    public final Map<TextRange, String> affectedTextRangeToSourceTypeName;
    public final Map<TextRange, String> affectedTextRangeToTargetTypeName;
    public volatile boolean showRefactoringOpportunity;

    public TypeMigrationSuggestedRefactoringState() {
        this.showRefactoringOpportunity = false;
        this.affectedTextRangeToSourceTypeName = new HashMap<>();
        this.affectedTextRangeToTargetTypeName = new HashMap<>();
    }

    public boolean shouldProvideRefactoring(int offset) {
        return affectedTextRangeToTargetTypeName
                .keySet().stream()
                .anyMatch(it -> it.contains(offset));
    }
}
