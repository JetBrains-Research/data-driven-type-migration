package org.jetbrains.research.ddtm.ide.migration.collectors;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.refactoring.typeMigration.TypeEvaluator;
import com.intellij.refactoring.typeMigration.TypeMigrationProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.data.models.TypeChangeRuleDescriptor;

import java.util.*;
import java.util.stream.Collectors;

public class TypeChangesInfoCollector extends SwitchableCollector {
    private static final int MAX_PARENTS_TO_LIFT_UP = 4;

    private static TypeChangesInfoCollector collector = null;
    private TypeEvaluator typeEvaluator;

    private List<SmartPsiElementPointer<PsiElement>> updatedUsages = new ArrayList<>();
    private List<SmartPsiElementPointer<PsiElement>> failedUsages = new ArrayList<>();
    private Map<SmartPsiElementPointer<PsiElement>, TypeChangeRuleDescriptor> failedUsageToCorrespondingRule = new HashMap<>();

    public static TypeChangesInfoCollector getInstance() {
        if (collector == null) {
            collector = new TypeChangesInfoCollector();
        }
        return collector;
    }

    public List<PsiElement> getUpdatedUsages() {
        return updatedUsages.stream()
                .map(SmartPsiElementPointer::getElement)
                .collect(Collectors.toList());
    }

    public List<PsiElement> getFailedUsages() {
        return failedUsages.stream()
                .map(SmartPsiElementPointer::getElement)
                .collect(Collectors.toList());
    }

    public List<PsiElement> getSuspiciousUsages() {
        return failedUsages.stream()
                .filter(usage -> failedUsageToCorrespondingRule.containsKey(usage))
                .map(SmartPsiElementPointer::getElement)
                .collect(Collectors.toList());
    }

    public void addFailedUsage(PsiElement element) {
        if (shouldCollect) {
            failedUsages.add(SmartPointerManager.createPointer(element));
        }
    }

    public void addUpdatedUsage(PsiElement element) {
        if (shouldCollect) {
            updatedUsages.add(SmartPointerManager.createPointer(element));
        }
    }

    public void addRuleForFailedUsage(PsiElement element, TypeChangeRuleDescriptor rule) {
        if (shouldCollect) {
            final var pointer = SmartPointerManager.createPointer(element);
            failedUsageToCorrespondingRule.put(pointer, rule);
        }
    }

    public Optional<TypeChangeRuleDescriptor> getRuleForFailedUsage(@NotNull PsiElement element) {
        int parentsPassed = 0;
        PsiElement currentContext = element;
        final var psiManager = PsiManager.getInstance(element.getProject());

        while (currentContext != null && parentsPassed < MAX_PARENTS_TO_LIFT_UP) {
            for (var entry : failedUsageToCorrespondingRule.entrySet()) {
                final boolean arePsiElementsEqual = psiManager.areElementsEquivalent(
                        entry.getKey().getElement(),
                        currentContext
                );
                if (arePsiElementsEqual) {
                    return Optional.of(entry.getValue());
                }
            }
            currentContext = currentContext.getParent();
            parentsPassed++;
        }
        return Optional.empty();
    }

    /**
     * Clears the list of usages failed to migrate, collected during previous type migration pass.
     * <p>
     * Call this method before calling built-in  {@link TypeMigrationProcessor#findUsages()}
     * </p>
     */
    public void clear() {
        failedUsages = new ArrayList<>();
        updatedUsages = new ArrayList<>();
        failedUsageToCorrespondingRule = new HashMap<>();
        shouldCollect = true;
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
