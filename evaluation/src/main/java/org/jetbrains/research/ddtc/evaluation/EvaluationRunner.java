package org.jetbrains.research.ddtc.evaluation;

import com.intellij.ide.impl.NewProjectUtil;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.usageView.UsageInfo;
import org.apache.commons.cli.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.GlobalState;
import org.jetbrains.research.data.TypeChangeRulesStorage;
import org.jetbrains.research.ide.migration.TypeChangeProcessor;
import org.jetbrains.research.ide.migration.collectors.RequiredImportsCollector;
import org.jetbrains.research.ide.migration.collectors.TypeChangesInfoCollector;
import org.jetbrains.research.utils.PsiRelatedUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class EvaluationRunner implements ApplicationStarter {
    private static final Logger LOG = Logger.getInstance(EvaluationRunner.class);
    private static final String JDK_NAME = "jdk";

    @Override
    public @NonNls
    String getCommandName() {
        return "evaluation";
    }

    private static void setupJdk(Project project, String jdkHomeDirectory) {
        Sdk jdk = JavaSdk.getInstance().createJdk(JDK_NAME, jdkHomeDirectory, false);
        ProjectJdkTable.getInstance().addJdk(jdk);
        ProjectRootManager.getInstance(project).setProjectSdk(jdk);
        NewProjectUtil.applyJdkToProject(project, jdk);
        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            ModuleRootModificationUtil.setModuleSdk(module, jdk);
        }
    }

    @Override
    public void main(@NotNull List<String> args) {
        try {
            CommandLine parsedArgs = parseArgs(args.toArray(new String[0]));
            File pathToProjects = new File(parsedArgs.getOptionValue("src-projects-dir"));
            String pathToJdk = parsedArgs.getOptionValue("jdk-path");

            ApplicationManager.getApplication().runWriteAction(() -> {
                for (File projectDir : Objects.requireNonNull(pathToProjects.listFiles())) {
                    if (projectDir.isDirectory()) {
                        final Project project = ProjectUtil.openOrImport(projectDir.toPath(), null, false);
                        if (project == null) continue;

                        loadModules(projectDir, project);
                        setupJdk(project, pathToJdk);
                        GlobalState.project = project;

                        final String sourceType = "java.io.File";
                        final String targetType = "java.nio.file.Path";

                        VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentRoots();
                        for (var root : roots) {
                            VfsUtilCore.iterateChildrenRecursively(root, null, fileOrDir -> {
                                if (fileOrDir.getExtension() == null
                                        || fileOrDir.getCanonicalPath() == null
                                        || !fileOrDir.getExtension().equals("java")) {
                                    return true;
                                }
                                final var psi = PsiManager.getInstance(project).findFile(fileOrDir);
                                if (psi == null) return true;

                                String source = psi.getText();
                                int caretOffset = source.indexOf("<caret>");
                                if (caretOffset == -1) return true;

                                PsiElement targetElement = psi.findElementAt(caretOffset);
                                for (int i = 0; i < 4; ++i) {
                                    targetElement = Objects.requireNonNull(targetElement).getParent();
                                }
                                PsiTypeCodeFragment sourcePsiType = JavaCodeFragmentFactory.getInstance(project)
                                        .createTypeCodeFragment(sourceType, targetElement, true);
                                PsiTypeElement sourceTypeElement = JavaPsiFacade.getElementFactory(project)
                                        .createTypeElement(Objects.requireNonNull(PsiRelatedUtils.getTypeOfCodeFragment(sourcePsiType)));
                                PsiElement finalTargetElement = targetElement;
                                WriteCommandAction.runWriteCommandAction(project, () -> {
                                    finalTargetElement.replace(sourceTypeElement);
                                });

                                WriteCommandAction.runWriteCommandAction(project, () -> {
                                    final PsiElement context = psi.findElementAt(caretOffset);
                                    final var descriptor = TypeChangeRulesStorage.findPattern(sourceType, targetType);
                                    final var typeChangeProcessor = new TypeChangeProcessor(project, false);
                                    final var builtInProcessor = typeChangeProcessor.createBuiltInTypeMigrationProcessor(context, descriptor);
                                    if (builtInProcessor == null) return;

                                    final var typeChangesCollector = TypeChangesInfoCollector.getInstance();
                                    final var requiredImportsCollector = RequiredImportsCollector.getInstance();
                                    typeChangesCollector.off();
                                    requiredImportsCollector.off();
                                    final UsageInfo[] usages = builtInProcessor.findUsages();

                                    for (var usage : usages) {
                                        PsiElement element = usage.getElement();
                                        assert element != null;

                                        PsiElement[] parents = {
                                                element.getParent(),
                                                element.getParent().getParent(),
                                                element.getParent().getParent()
                                        };
                                        System.out.print(element + " - ");
                                        System.out.println(Arrays.toString(parents));
                                    }
                                });
                                return false;
                            });
                        }
                    }
                }
            });
        } catch (Throwable e) {
            System.out.println(e.getMessage());
            System.exit(1);
        } finally {
            System.exit(0);
        }
    }

    private void loadModules(File projectDir, Project project) {
        final var moduleType = JavaModuleType.getModuleType();
        Path moduleSrcPath = projectDir.toPath().resolve("src");
        Path moduleImlPath = projectDir.toPath().resolve(".iml");
        Module module = ModuleManager.getInstance(project).newModule(moduleImlPath, moduleType.getId());
        PsiTestUtil.addSourceContentToRoots(
                module,
                Objects.requireNonNull(LocalFileSystem.getInstance().findFileByPath(moduleSrcPath.toString())),
                false
        );
    }

    private CommandLine parseArgs(String[] args) {
        Options options = new Options();

        Option src = new Option(
                "s",
                "src-projects-dir",
                true,
                "path to the directory with projects for evaluation"
        );
        src.setRequired(true);
        options.addOption(src);

        Option jdk = new Option(
                "j",
                "jdk-path",
                true,
                "path to the JDK (such as /usr/lib/jvm/java-8-openjdk-amd64)"
        );
        jdk.setRequired(true);
        options.addOption(jdk);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("evaluation", options);
            System.exit(1);
        }
        return cmd;
    }
}
