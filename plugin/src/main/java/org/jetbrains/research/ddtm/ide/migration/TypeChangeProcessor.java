package org.jetbrains.research.ddtm.ide.migration;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.typeMigration.TypeMigrationProcessor;
import com.intellij.refactoring.typeMigration.TypeMigrationRules;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewContentManager;
import com.intellij.util.Functions;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.ddtm.DataDrivenTypeMigrationBundle;
import org.jetbrains.research.ddtm.data.enums.InvocationWorkflow;
import org.jetbrains.research.ddtm.data.enums.SupportedSearchScope;
import org.jetbrains.research.ddtm.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ddtm.ide.fus.TypeChangeLogsCollector;
import org.jetbrains.research.ddtm.ide.migration.collectors.RequiredImportsCollector;
import org.jetbrains.research.ddtm.ide.migration.collectors.TypeChangesInfoCollector;
import org.jetbrains.research.ddtm.ide.refactoring.ReactiveTypeChangeAvailabilityUpdater;
import org.jetbrains.research.ddtm.ide.refactoring.services.TypeChangeRefactoringProvider;
import org.jetbrains.research.ddtm.ide.settings.TypeChangeSettingsState;
import org.jetbrains.research.ddtm.ide.ui.FailedTypeChangesPanel;
import org.jetbrains.research.ddtm.utils.PsiRelatedUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class TypeChangeProcessor {
    private static final Logger LOG = Logger.getInstance(TypeChangeProcessor.class);

    private final Project project;
    private final InvocationWorkflow invocationWorkflow;
    private PsiElement root;

    public TypeChangeProcessor(Project project, InvocationWorkflow invocationWorkflow) {
        this.project = project;
        this.invocationWorkflow = invocationWorkflow;
    }

    public void run(PsiElement element, TypeChangePatternDescriptor descriptor) {
        try {
            final var state = TypeChangeRefactoringProvider.getInstance(project).getState();
            state.isInternalTypeChangeInProgress = true;

            final TypeMigrationProcessor builtInProcessor = WriteCommandAction.writeCommandAction(project)
                    .withGroupId(DataDrivenTypeMigrationBundle.message("group.id"))
                    .compute((ThrowableComputable<TypeMigrationProcessor, Throwable>) () ->
                            createBuiltInTypeMigrationProcessor(element, descriptor)
                    );
            if (builtInProcessor == null) return;

            final TypeChangesInfoCollector typeChangesCollector = TypeChangesInfoCollector.getInstance();
            final var requiredImportsCollector = RequiredImportsCollector.getInstance();
            typeChangesCollector.clear();
            requiredImportsCollector.clear();
            final UsageInfo[] usages = ReadAction.compute(builtInProcessor::findUsages);

            ApplicationManager.getApplication().invokeLater(() ->
                    WriteCommandAction.writeCommandAction(project)
                            .withName(DataDrivenTypeMigrationBundle.message("group.id") + ": " + descriptor.toString())
                            .withGroupId(DataDrivenTypeMigrationBundle.message("group.id"))
                            .run(() -> {
                                builtInProcessor.performRefactoring(usages);
                                addAndOptimizeImports(project, usages);
                            }), project.getDisposed()
            );

            if (typeChangesCollector.hasFailedTypeChanges()) {
                typeChangesCollector.setTypeEvaluator(builtInProcessor.getLabeler().getTypeEvaluator());
                UsageInfo[] infos = typeChangesCollector.getFailedUsages().stream()
                        .map(pointer -> new UsageInfo(pointer, null, false, false))
                        .toArray(UsageInfo[]::new);

                ApplicationManager.getApplication().invokeLater(() -> {
                    final var panel = new FailedTypeChangesPanel(project);
                    Content content = UsageViewContentManager.getInstance(project).addContent(
                            DataDrivenTypeMigrationBundle.message("tool.window.content.name"),
                            true,
                            panel,
                            false,
                            false
                    );
                    content.setDisposer(panel);
                    panel.setContent(content);

                    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.FIND);
                    if (toolWindow == null) return;
                    ContentManager contentManager = toolWindow.getContentManager();
                    contentManager.addContent(content);
                    toolWindow.activate(null);

                    panel.updateLayout(infos);
                });
            }

            if (invocationWorkflow.equals(InvocationWorkflow.REACTIVE)) {
                WriteCommandAction.runWriteCommandAction(project, () -> disableRefactoring(element));
            }

            final String notificationContent = DataDrivenTypeMigrationBundle.message(
                    "notification.balloon.content",
                    typeChangesCollector.getUpdatedUsages().size(),
                    typeChangesCollector.getFailedUsages().size()
            );
            NotificationGroupManager.getInstance().getNotificationGroup("DDTM Notification Group")
                    .createNotification(notificationContent, NotificationType.INFORMATION)
                    .notify(project);

            TypeChangeLogsCollector.getInstance().refactoringIntentionApplied(
                    project,
                    descriptor.getId(),
                    root,
                    typeChangesCollector.getUsedRules().size(),
                    typeChangesCollector.getUpdatedUsages().size(),
                    typeChangesCollector.getSuspiciousUsages().size(),
                    typeChangesCollector.getFailedUsages().size(),
                    invocationWorkflow
            );
            state.isInternalTypeChangeInProgress = false;
        } catch (Throwable throwable) {
            LOG.error(throwable);
        }
    }

    private void disableRefactoring(PsiElement element) {
        final var state = TypeChangeRefactoringProvider.getInstance(project).getState();
        state.refactoringEnabled = false;
        state.removeAllTypeChangesByRange(element.getTextRange());
        Document document = PsiDocumentManager.getInstance(project).getDocument(root.getContainingFile());
        if (document == null) return;
        final var updater = project.getService(ReactiveTypeChangeAvailabilityUpdater.class);
        updater.updateAllHighlighters(document, element.getTextOffset());
    }

    public @Nullable TypeMigrationProcessor createBuiltInTypeMigrationProcessor(
            PsiElement element,
            TypeChangePatternDescriptor descriptor
    ) {
        PsiTypeElement rootTypeElement = PsiRelatedUtils.getClosestPsiTypeElement(element);
        PsiElement root;
        if (rootTypeElement != null) {
            root = rootTypeElement.getParent();
        } else {
            LOG.warn("Type of migration root is null");
            return null;
        }
        this.root = root;

        // In case of suggested refactoring intention
        if (invocationWorkflow.equals(InvocationWorkflow.REACTIVE)) {
            final PsiType currentRootType = Objects.requireNonNull(PsiRelatedUtils.getExpectedType(root));
            final String recoveredRootType = descriptor.resolveSourceType(currentRootType, project);
            final PsiTypeElement recoveredRootTypeElement = PsiElementFactory.getInstance(project)
                    .createTypeElementFromText(recoveredRootType, root);
            rootTypeElement.replace(recoveredRootTypeElement);
        }

        final PsiType expectedRootType = Objects.requireNonNull(PsiRelatedUtils.getExpectedType(root));
        String targetType = descriptor.resolveTargetType(expectedRootType, project);
        PsiTypeCodeFragment targetTypeCodeFragment = JavaCodeFragmentFactory.getInstance(project)
                .createTypeCodeFragment(targetType, root, true);

        TypeMigrationRules rules = new TypeMigrationRules(project);
        var chosenSearchScope = TypeChangeSettingsState.getInstance().searchScope;
        if (chosenSearchScope == null) chosenSearchScope = SupportedSearchScope.FILE; // by default
        switch (chosenSearchScope) {
            case LOCAL:
                rules.setBoundScope(root.getUseScope());
                break;
            case FILE:
                rules.setBoundScope(GlobalSearchScope.fileScope(root.getContainingFile()));
                break;
            case PROJECT:
                rules.setBoundScope(GlobalSearchScope.projectScope(project));
                break;
        }
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
