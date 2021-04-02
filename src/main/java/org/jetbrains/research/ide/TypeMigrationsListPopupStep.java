package org.jetbrains.research.ide;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.migration.DataDrivenTypeConversionRule;
import org.jetbrains.research.migration.json.DataDrivenTypeMigrationRulesDescriptor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TypeMigrationsListPopupStep extends BaseListPopupStep<DataDrivenTypeMigrationRulesDescriptor> {
    private static final Logger LOG = Logger.getInstance(DataDrivenTypeMigrationIntention.class);

    private DataDrivenTypeMigrationRulesDescriptor selectedDescriptor = null;
    private final Project project;
    private final PsiElement element;

    public TypeMigrationsListPopupStep(String caption,
                                       List<DataDrivenTypeMigrationRulesDescriptor> rulesDescriptors,
                                       PsiElement element,
                                       Project project) {
        super(caption, rulesDescriptors);
        this.element = element;
        this.project = project;
    }

    @Override
    public @Nullable PopupStep<?> onChosen(DataDrivenTypeMigrationRulesDescriptor selectedValue, boolean finalChoice) {
        selectedDescriptor = selectedValue;
        return super.onChosen(selectedValue, finalChoice);
    }

    @Override
    public @NotNull String getTextFor(DataDrivenTypeMigrationRulesDescriptor value) {
        return value.getFromType() + " to " + value.getToType();
    }

    @Override
    public @Nullable Runnable getFinalRunnable() {
        return () -> WriteCommandAction.runWriteCommandAction(project, () -> migrate(selectedDescriptor));
    }

    private void migrate(DataDrivenTypeMigrationRulesDescriptor descriptor) {
        PsiLocalVariable root = PsiTreeUtil.getParentOfType(element, PsiLocalVariable.class);

        PsiTypeCodeFragment myTypeCodeFragment = JavaCodeFragmentFactory
                .getInstance(project)
                .createTypeCodeFragment(descriptor.getToType(), root, true);
        SearchScope scope = new LocalSearchScope(element.getContainingFile());

        TypeMigrationRules rules = new TypeMigrationRules(project);
        rules.setBoundScope(scope);
        TypeConversionRule dataDrivenRule = new DataDrivenTypeConversionRule();
        rules.addConversionDescriptor(dataDrivenRule);

        TypeMigrationProcessor migrationProcessor = new TypeMigrationProcessor(
                project,
                new PsiElement[]{root},
                Functions.constant(getMigrationType(myTypeCodeFragment)),
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

    @Nullable
    public PsiType getMigrationType(PsiTypeCodeFragment fragment) {
        try {
            return fragment.getType();
        } catch (PsiTypeCodeFragment.TypeSyntaxException | PsiTypeCodeFragment.NoTypeException e) {
            LOG.debug(e);
            return null;
        }
    }
}
