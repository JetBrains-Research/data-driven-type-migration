package org.jetbrains.research.migration;

import com.intellij.java.JavaBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.refactoring.typeMigration.TypeMigrationProcessor;
import com.intellij.refactoring.typeMigration.TypeMigrationRules;
import com.intellij.refactoring.typeMigration.rules.TypeConversionRule;
import com.intellij.refactoring.typeMigration.ui.FailedConversionsDialog;
import com.intellij.refactoring.typeMigration.ui.MigrationPanel;
import com.intellij.refactoring.util.RefactoringUIUtil;
import com.intellij.ui.content.Content;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewContentManager;
import com.intellij.util.Functions;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.research.migration.json.DataDrivenTypeMigrationRulesDescriptor;
import org.jetbrains.research.utils.PsiUtils;
import org.jetbrains.research.utils.StringUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class DataDrivenTypeMigrationProcessor {
    private static final Logger LOG = Logger.getInstance(DataDrivenRulesStorage.class);

    private final Project project;
    private final PsiElement element;

    public DataDrivenTypeMigrationProcessor(PsiElement element, Project project) {
        this.element = element;
        this.project = project;
    }

    public void migrate(DataDrivenTypeMigrationRulesDescriptor descriptor) {
        PsiTypeElement rootType = PsiUtils.getHighestParentOfType(element, PsiTypeElement.class);
        PsiElement root;
        if (rootType != null) {
            root = rootType.getParent();
        } else {
            LOG.error("Type of migration root is null");
            return;
        }

        String targetType = StringUtils.substituteTypeByPattern(
                Objects.requireNonNull(PsiUtils.getExpectedType(root)),
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
                Functions.constant(PsiUtils.getTypeOfCodeFragment(myTypeCodeFragment)),
                rules,
                true
        );

        final UsageInfo[] usages = migrationProcessor.findUsages();

        if (migrationProcessor.hasFailedConversions()) {
            FailedConversionsDialog dialog = new FailedConversionsDialog(
                    migrationProcessor.getLabeler().getFailedConversionsReport(),
                    project
            );
            dialog.showAndGet();
        }

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
