package org.jetbrains.research.ide.services;

import com.intellij.openapi.project.Project;

public interface TypeChangeRefactoringProvider {
    static TypeChangeRefactoringProvider getInstance(Project project) {
        return project.getService(TypeChangeRefactoringProvider.class);
    }
}
