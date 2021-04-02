package org.jetbrains.research.migration;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.typeMigration.TypeMigrationProcessor;
import com.intellij.refactoring.typeMigration.TypeMigrationRules;
import com.intellij.refactoring.typeMigration.rules.TypeConversionRule;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Functions;
import org.jetbrains.research.Utils;
import org.jetbrains.research.migration.json.DataDrivenTypeMigrationRulesDescriptor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class DataDrivenTypeMigrationProcessor {

    private final Project project;
    private final PsiElement element;

    public DataDrivenTypeMigrationProcessor(PsiElement element, Project project) {
        this.element = element;
        this.project = project;
    }

    public void migrate(DataDrivenTypeMigrationRulesDescriptor descriptor) {
        PsiLocalVariable root = PsiTreeUtil.getParentOfType(element, PsiLocalVariable.class);

        String targetType = Utils.substituteTypeByPattern(
                Objects.requireNonNull(root).getType(),
                descriptor.getSourceType(),
                descriptor.getTargetType()
        );
        PsiTypeCodeFragment myTypeCodeFragment = JavaCodeFragmentFactory
                .getInstance(project)
                .createTypeCodeFragment(targetType, root, true);
        SearchScope scope = new LocalSearchScope(element.getContainingFile());

        TypeMigrationRules rules = new TypeMigrationRules(project);
        rules.setBoundScope(scope);
        TypeConversionRule dataDrivenRule = new DataDrivenTypeConversionRule();
        rules.addConversionDescriptor(dataDrivenRule);

        TypeMigrationProcessor migrationProcessor = new TypeMigrationProcessor(
                project,
                new PsiElement[]{root},
                Functions.constant(Utils.getType(myTypeCodeFragment)),
                rules,
                true
        );

        final UsageInfo[] usages = migrationProcessor.findUsages();
        migrationProcessor.performRefactoring(usages);
        addAndOptimizeImports(project, usages);
    }

    private void addAndOptimizeImports(Project project, UsageInfo[] usages) {
        // TODO: Add auto imports
        final JavaCodeStyleManager javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);
        final Set<PsiFile> affectedFiles = getAffectedFiles(usages);
        for (PsiFile file : affectedFiles) {
            javaCodeStyleManager.optimizeImports(file);
            javaCodeStyleManager.shortenClassReferences(file);
        }
    }

    private Set<PsiFile> getAffectedFiles(UsageInfo[] usages) {
        final Set<PsiFile> affectedFiles = new HashSet<>();
        for (UsageInfo usage : usages) {
            final PsiFile usageFile = usage.getFile();
            if (usageFile != null) {
                affectedFiles.add(usageFile);
            }
        }
        return affectedFiles;
    }
}
