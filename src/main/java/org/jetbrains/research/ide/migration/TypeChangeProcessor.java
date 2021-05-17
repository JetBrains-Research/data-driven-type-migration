package org.jetbrains.research.ide.migration;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.typeMigration.TypeMigrationProcessor;
import com.intellij.refactoring.typeMigration.TypeMigrationRules;
import com.intellij.ui.content.Content;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewContentManager;
import com.intellij.util.Functions;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.data.TypeChangeRulesStorage;
import org.jetbrains.research.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ide.refactoring.TypeChangeRefactoringAvailabilityUpdater;
import org.jetbrains.research.ide.refactoring.services.TypeChangeRefactoringProviderImpl;
import org.jetbrains.research.ide.ui.FailedTypeChangesPanel;
import org.jetbrains.research.utils.PsiUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class TypeChangeProcessor {
    private static final Logger LOG = Logger.getInstance(TypeChangeRulesStorage.class);

    private final Project project;
    private final Boolean isRootTypeAlreadyChanged;

    public TypeChangeProcessor(Project project, Boolean isRootTypeAlreadyChanged) {
        this.project = project;
        this.isRootTypeAlreadyChanged = isRootTypeAlreadyChanged;
    }

    public void run(PsiElement element, TypeChangePatternDescriptor descriptor) {
        final TypeMigrationProcessor builtInProcessor = createBuiltInTypeMigrationProcessor(element, descriptor);
        if (builtInProcessor == null) return;

        final var failedUsagesCollector = FailedTypeChangesCollector.getInstance();
        failedUsagesCollector.clear();
        final UsageInfo[] usages = builtInProcessor.findUsages();

        if (failedUsagesCollector.hasFailedTypeChanges()) {
            final var panel = new FailedTypeChangesPanel(failedUsagesCollector.getFailedUsages(), project);
            Content content = UsageViewContentManager.getInstance(project).addContent(
                    "Failed Usages",
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
        disableRefactoring(element);
    }

    private void disableRefactoring(PsiElement element) {
        final var state = TypeChangeRefactoringProviderImpl.getInstance(project).getState();
        state.refactoringEnabled = false;
        state.removeAllTypeChangesByRange(element.getTextRange());

        final var document = PsiDocumentManager.getInstance(project).getDocument(element.getContainingFile());
        if (document == null) return;

        TypeChangeRefactoringAvailabilityUpdater.getInstance(project)
                .updateAllHighlighters(document, element.getTextOffset());
    }

    private @Nullable TypeMigrationProcessor createBuiltInTypeMigrationProcessor(
            PsiElement element,
            TypeChangePatternDescriptor descriptor
    ) {
        PsiTypeElement rootTypeElement = PsiUtils.getHighestParentOfType(element, PsiTypeElement.class);
        PsiElement root;
        if (rootTypeElement != null) {
            root = rootTypeElement.getParent();
        } else {
            LOG.error("Type of migration root is null");
            return null;
        }

        // In case of suggested refactoring intention
        if (isRootTypeAlreadyChanged) {
            final PsiType currentRootType = Objects.requireNonNull(PsiUtils.getExpectedType(root));
            final String recoveredRootType = descriptor.resolveSourceType(currentRootType);
            final PsiTypeElement recoveredRootTypeElement = PsiElementFactory.getInstance(project)
                    .createTypeElementFromText(recoveredRootType, root);
            rootTypeElement.replace(recoveredRootTypeElement);
        }

        final PsiType expectedRootType = Objects.requireNonNull(PsiUtils.getExpectedType(root));
        String targetType = descriptor.resolveTargetType(expectedRootType);
        PsiTypeCodeFragment targetTypeCodeFragment = JavaCodeFragmentFactory.getInstance(project)
                .createTypeCodeFragment(targetType, root, true);

        TypeMigrationRules rules = new TypeMigrationRules(project);
        rules.setBoundScope(GlobalSearchScope.projectScope(project));
        rules.addConversionDescriptor(new HeuristicTypeConversionRule());

        return new TypeMigrationProcessor(
                project,
                new PsiElement[]{root},
                Functions.constant(PsiUtils.getTypeOfCodeFragment(targetTypeCodeFragment)),
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
