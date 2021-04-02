package org.jetbrains.research.legacy;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.*;

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

    private static PsiElement getContainingStatement(final PsiElement root) {
        final PsiStatement statement = PsiTreeUtil.getParentOfType(root, PsiStatement.class);
        PsiExpression condition = getContainingCondition(root, statement);
        if (condition != null) return condition;
        final PsiField field = PsiTreeUtil.getParentOfType(root, PsiField.class);
        return statement != null ? statement : field != null ? field : root;
    }

    private static PsiExpression getContainingCondition(PsiElement root, PsiStatement statement) {
        PsiExpression condition = null;
        if (statement instanceof PsiConditionalLoopStatement) {
            condition = ((PsiConditionalLoopStatement) statement).getCondition();
        } else if (statement instanceof PsiIfStatement) {
            condition = ((PsiIfStatement) statement).getCondition();
        }
        return PsiTreeUtil.isAncestor(condition, root, false) ? condition : null;
    }
}
