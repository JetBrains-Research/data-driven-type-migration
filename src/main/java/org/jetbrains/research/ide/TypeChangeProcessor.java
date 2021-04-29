package org.jetbrains.research.ide;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.refactoring.typeMigration.TypeMigrationProcessor;
import com.intellij.refactoring.typeMigration.TypeMigrationRules;
import com.intellij.refactoring.typeMigration.rules.TypeConversionRule;
import com.intellij.ui.content.Content;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewContentManager;
import com.intellij.util.Functions;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.migration.HeuristicTypeConversionRule;
import org.jetbrains.research.migration.TypeChangeRulesStorage;
import org.jetbrains.research.migration.json.TypeChangeRulesDescriptor;
import org.jetbrains.research.utils.PsiUtils;
import org.jetbrains.research.utils.StringUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class TypeChangeProcessor {
    private static final Logger LOG = Logger.getInstance(TypeChangeRulesStorage.class);

    private final Project project;

    public TypeChangeProcessor(Project project) {
        this.project = project;
    }

    public void run(PsiElement element, TypeChangeRulesDescriptor descriptor) {
        final TypeMigrationProcessor builtInProcessor = createBuiltInTypeMigrationProcessor(element, descriptor);
        if (builtInProcessor == null) return;
        final UsageInfo[] usages = builtInProcessor.findUsages();

        if (builtInProcessor.hasFailedConversions()) {
            final var panel = new FailedTypeChangesPanel(usages);
            Content content = UsageViewContentManager.getInstance(project).addContent(
                    "Failed Type Conversions",
                    false,
                    panel,
                    true,
                    true
            );
            panel.setContent(content);
            ToolWindow toolWindow = Objects.requireNonNull(
                    ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.FIND)
            );
            toolWindow.activate(null);
        }

        builtInProcessor.performRefactoring(usages);
        addAndOptimizeImports(project, usages);
    }

    private @Nullable TypeMigrationProcessor createBuiltInTypeMigrationProcessor(
            PsiElement element,
            TypeChangeRulesDescriptor descriptor
    ) {
        PsiTypeElement rootType = PsiUtils.getHighestParentOfType(element, PsiTypeElement.class);
        PsiElement root;
        if (rootType != null) {
            root = rootType.getParent();
        } else {
            LOG.error("Type of migration root is null");
            return null;
        }

        String targetType = StringUtils.substituteTypeByPattern(
                Objects.requireNonNull(PsiUtils.getExpectedType(root)),
                descriptor.getSourceType(),
                descriptor.getTargetType()
        );

        PsiTypeCodeFragment typeCodeFragment = JavaCodeFragmentFactory
                .getInstance(project)
                .createTypeCodeFragment(targetType, root, true);
        SearchScope scope = new LocalSearchScope(element.getContainingFile());

        TypeMigrationRules rules = new TypeMigrationRules(project);
        rules.setBoundScope(scope);
        TypeConversionRule dataDrivenRule = new HeuristicTypeConversionRule();
        rules.addConversionDescriptor(dataDrivenRule);

        return new TypeMigrationProcessor(
                project,
                new PsiElement[]{root},
                Functions.constant(PsiUtils.getTypeOfCodeFragment(typeCodeFragment)),
                rules,
                true
        );
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
