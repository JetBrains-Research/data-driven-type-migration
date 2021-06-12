package org.jetbrains.research.ide.migration;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
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
import org.jetbrains.research.GlobalState;
import org.jetbrains.research.data.TypeChangeRulesStorage;
import org.jetbrains.research.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ide.fus.TypeChangeLogsCollector;
import org.jetbrains.research.ide.migration.collectors.RequiredImportsCollector;
import org.jetbrains.research.ide.migration.collectors.TypeChangesInfoCollector;
import org.jetbrains.research.ide.refactoring.TypeChangeRefactoringAvailabilityUpdater;
import org.jetbrains.research.ide.refactoring.services.TypeChangeRefactoringProviderImpl;
import org.jetbrains.research.ide.ui.FailedTypeChangesPanel;
import org.jetbrains.research.utils.PsiRelatedUtils;

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

        final var typeChangesCollector = TypeChangesInfoCollector.getInstance();
        final var requiredImportsCollector = RequiredImportsCollector.getInstance();
        typeChangesCollector.clear();
        requiredImportsCollector.clear();
        final UsageInfo[] usages = builtInProcessor.findUsages();

        if (typeChangesCollector.hasFailedTypeChanges()) {
            typeChangesCollector.setTypeEvaluator(builtInProcessor.getLabeler().getTypeEvaluator());
            final var panel = new FailedTypeChangesPanel(typeChangesCollector.getFailedUsages(), project);
            Content content = UsageViewContentManager.getInstance(project).addContent(
                    "Failed Type Changes",
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

        PsiElement root = Objects.requireNonNull(
                PsiRelatedUtils.getHighestParentOfType(element, PsiTypeElement.class)
        ).getParent();

        if (isRootTypeAlreadyChanged) {
            TypeChangeLogsCollector.getInstance().reactiveIntentionApplied(
                    project,
                    descriptor.getSourceType(),
                    descriptor.getTargetType(),
                    root,
                    typeChangesCollector.getUpdatedUsages().size(),
                    typeChangesCollector.getSuspiciousUsages().size(),
                    typeChangesCollector.getFailedUsages().size()
            );
            disableRefactoring(element);
        } else {
            TypeChangeLogsCollector.getInstance().proactiveIntentionApplied(
                    project,
                    descriptor.getSourceType(),
                    descriptor.getTargetType(),
                    root,
                    typeChangesCollector.getUpdatedUsages().size(),
                    typeChangesCollector.getSuspiciousUsages().size(),
                    typeChangesCollector.getFailedUsages().size()
            );
        }
    }

    private void disableRefactoring(PsiElement element) {
        final var state = TypeChangeRefactoringProviderImpl.getInstance(project).getState();
        state.refactoringEnabled = false;
        state.removeAllTypeChangesByRange(element.getTextRange());

        Document document;
        try {
            document = PsiDocumentManager.getInstance(project).getDocument(element.getContainingFile());
        } catch (PsiInvalidElementAccessException exception) {
            LOG.warn(exception);
            return;
        }
        if (document == null) return;

        TypeChangeRefactoringAvailabilityUpdater.getInstance(project)
                .updateAllHighlighters(document, element.getTextOffset());
    }

    public @Nullable TypeMigrationProcessor createBuiltInTypeMigrationProcessor(
            PsiElement element,
            TypeChangePatternDescriptor descriptor
    ) {
        PsiTypeElement rootTypeElement = PsiRelatedUtils.getHighestParentOfType(element, PsiTypeElement.class);
        PsiElement root;
        if (rootTypeElement != null) {
            root = rootTypeElement.getParent();
        } else {
            LOG.error("Type of migration root is null");
            return null;
        }

        // In case of suggested refactoring intention
        if (isRootTypeAlreadyChanged) {
            final PsiType currentRootType = Objects.requireNonNull(PsiRelatedUtils.getExpectedType(root));
            final String recoveredRootType = descriptor.resolveSourceType(currentRootType);
            final PsiTypeElement recoveredRootTypeElement = PsiElementFactory.getInstance(project)
                    .createTypeElementFromText(recoveredRootType, root);
            rootTypeElement.replace(recoveredRootTypeElement);
        }

        final PsiType expectedRootType = Objects.requireNonNull(PsiRelatedUtils.getExpectedType(root));
        String targetType = descriptor.resolveTargetType(expectedRootType);
        PsiTypeCodeFragment targetTypeCodeFragment = JavaCodeFragmentFactory.getInstance(project)
                .createTypeCodeFragment(targetType, root, true);

        TypeMigrationRules rules = new TypeMigrationRules(project);
        rules.setBoundScope(GlobalState.searchScope);
        rules.addConversionDescriptor(new HeuristicTypeConversionRule());

        return new TypeMigrationProcessor(
                project,
                new PsiElement[]{root},
                Functions.constant(PsiRelatedUtils.getTypeOfCodeFragment(targetTypeCodeFragment)),
                rules,
                true
        );
    }

    private void addAndOptimizeImports(Project project, UsageInfo[] usages) {
        final JavaCodeStyleManager javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);
        final Set<PsiFile> affectedFiles = getAffectedFiles(usages);
        for (PsiFile file : affectedFiles) {
            for (var requiredImport : RequiredImportsCollector.getInstance().getRequiredImports()) {
                final PsiClass importClass = JavaPsiFacade.getInstance(project)
                        .findClass(requiredImport, GlobalSearchScope.everythingScope(project));
                if (importClass == null) continue;
                javaCodeStyleManager.addImport((PsiJavaFile) file, importClass);
            }
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
