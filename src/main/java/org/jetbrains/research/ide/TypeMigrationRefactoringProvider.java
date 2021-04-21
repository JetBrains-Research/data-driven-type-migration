package org.jetbrains.research.ide;

import com.intellij.openapi.project.Project;

public interface TypeMigrationRefactoringProvider {
    static TypeMigrationRefactoringProvider getInstance(Project project) {
        return project.getService(TypeMigrationRefactoringProvider.class);
    }
}
