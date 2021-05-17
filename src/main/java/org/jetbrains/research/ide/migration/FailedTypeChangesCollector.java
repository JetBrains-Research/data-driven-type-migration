package org.jetbrains.research.ide.migration;

import com.intellij.psi.PsiElement;
import com.intellij.refactoring.typeMigration.TypeMigrationProcessor;

import java.util.ArrayList;
import java.util.List;

public class FailedTypeChangesCollector {
    private static FailedTypeChangesCollector collector = null;
    private List<PsiElement> failedUsages;

    public static FailedTypeChangesCollector getInstance() {
        if (collector == null) {
            collector = new FailedTypeChangesCollector();
        }
        return collector;
    }

    public List<PsiElement> getFailedUsages() {
        return failedUsages;
    }

    public void addFailedTypeChange(PsiElement element) {
        failedUsages.add(element);
    }

    /**
     * Clears the list of usages failed to migrate, collected during previous type migration pass.
     * <p>
     * Call this method before calling {@link TypeMigrationProcessor#findUsages()}
     * </p>
     */
    public void clear() {
        failedUsages = new ArrayList<>();
    }

    public boolean hasFailedTypeChanges() {
        return !failedUsages.isEmpty();
    }
}
