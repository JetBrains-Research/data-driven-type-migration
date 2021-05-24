package org.jetbrains.research.ide.migration;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.refactoring.typeMigration.TypeEvaluator;
import com.intellij.refactoring.typeMigration.TypeMigrationProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.data.models.TypeChangeRuleDescriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FailedTypeChangesCollector {
    private static final int MAX_PARENTS_TO_LIFT_UP = 4;

    private static FailedTypeChangesCollector collector = null;
    private TypeEvaluator typeEvaluator;
    private List<SmartPsiElementPointer<PsiElement>> failedUsages =
            new ArrayList<>();
    private Map<SmartPsiElementPointer<PsiElement>, TypeChangeRuleDescriptor> failedUsageToCorrespondingRule =
            new HashMap<>();

    public static FailedTypeChangesCollector getInstance() {
        if (collector == null) {
            collector = new FailedTypeChangesCollector();
        }
        return collector;
    }

    public List<PsiElement> getFailedUsages() {
        return failedUsages.stream()
                .map(SmartPsiElementPointer::getElement)
                .collect(Collectors.toList());
    }

    public void addFailedUsage(PsiElement element) {
        failedUsages.add(SmartPointerManager.createPointer(element));
    }

    public void addRuleForFailedUsage(PsiElement element, TypeChangeRuleDescriptor rule) {
        final var pointer = SmartPointerManager.createPointer(element);
        failedUsageToCorrespondingRule.put(pointer, rule);
    }

    public @Nullable TypeChangeRuleDescriptor getRuleForFailedUsage(@NotNull PsiElement element) {
        int parentsPassed = 0;
        PsiElement currentContext = element;
        final var psiManager = PsiManager.getInstance(element.getProject());

        while (parentsPassed < MAX_PARENTS_TO_LIFT_UP) {
            for (var entry : failedUsageToCorrespondingRule.entrySet()) {
                final boolean arePsiElementsEqual = psiManager.areElementsEquivalent(
                        entry.getKey().getElement(),
                        currentContext
                );
                if (arePsiElementsEqual) {
                    return entry.getValue();
                }
            }
            currentContext = currentContext.getParent();
            parentsPassed++;
        }
        return null;
    }

    /**
     * Clears the list of usages failed to migrate, collected during previous type migration pass.
     * <p>
     * Call this method before calling built-in  {@link TypeMigrationProcessor#findUsages()}
     * </p>
     */
    public void clear() {
        failedUsages = new ArrayList<>();
        failedUsageToCorrespondingRule = new HashMap<>();
    }

    public boolean hasFailedTypeChanges() {
        return !failedUsages.isEmpty();
    }

    public TypeEvaluator getTypeEvaluator() {
        return typeEvaluator;
    }

    public void setTypeEvaluator(TypeEvaluator typeEvaluator) {
        this.typeEvaluator = typeEvaluator;
    }
}
