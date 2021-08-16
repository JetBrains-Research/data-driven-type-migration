package org.jetbrains.research.ddtm.legacy;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;

import java.util.*;

import static org.jetbrains.research.ddtm.utils.PsiRelatedUtils.getContainingStatement;

public class TypeRelevantUsagesProcessor {
    private final Project project;
    private LinkedList<PsiElement> roots;
    private final SearchScope globalSearchScope;
    private Set<PsiElement> allRelevantUsages;

    public TypeRelevantUsagesProcessor(Project project, PsiElement root, SearchScope globalSearchScope) {
        this.project = project;
        this.roots = new LinkedList<>(Collections.singletonList(root));
        this.globalSearchScope = globalSearchScope;
    }

    public Set<PsiElement> getAllRelevantUsages() {
        allRelevantUsages = new HashSet<>();
        findRootsAndUsages();
        return allRelevantUsages;
    }

    public void addMigrationRoot(PsiElement root) {
        roots.add(root);
    }

    private void saveUsage(PsiElement element) {
        allRelevantUsages.add(element);
    }

    private static PsiReference[] findLocalUsages(PsiElement element, SearchScope scope) {
        return ReferencesSearch
                .search(element, scope, false)
                .toArray(PsiReference.EMPTY_ARRAY);
    }

    private void findRootsAndUsages() {
        while (!roots.isEmpty()) {
            final List<PsiElement> currentRoots = new ArrayList<>(roots);
            roots = new LinkedList<>();
            for (final PsiElement root : currentRoots) {
                processRootElement(root);
                saveUsage(root);
                final Set<PsiElement> processed = new HashSet<>();
                final PsiReference[] rootUsages = findLocalUsages(root, globalSearchScope);
                for (PsiReference usage : rootUsages) {
                    processRootUsageExpression(usage, processed);
                }
            }
        }
    }

    private void processRootElement(PsiElement root) {
        if (root instanceof PsiVariable || root instanceof PsiExpression) {
            final PsiElement element = getContainingStatement(root);
            element.accept(new TypeRelevantUsagesVisitor(this));
        }
    }

    private void processRootUsageExpression(final PsiReference usage, final Set<? super PsiElement> processed) {
        final PsiElement ref = usage.getElement();
        if (ref.getLanguage() == JavaLanguage.INSTANCE) {
            saveUsage(ref);
            final PsiElement element = getContainingStatement(ref);
            if (element != null && !processed.contains(element)) {
                processed.add(element);
                ref.accept(new TypeRelevantUsagesVisitor(this));
            }
        }
    }
}
