package org.jetbrains.research.ddtm.ide.refactoring.services;

import com.intellij.openapi.project.Project;

public interface TypeChangeRefactoringProvider {
    static TypeChangeRefactoringProvider getInstance(Project project) {
        return project.getService(TypeChangeRefactoringProvider.class);
    }
}
