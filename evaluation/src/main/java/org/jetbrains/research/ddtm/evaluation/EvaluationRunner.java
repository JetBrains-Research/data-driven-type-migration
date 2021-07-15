package org.jetbrains.research.ddtm.evaluation;

import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.usageView.UsageInfo;
import org.apache.commons.cli.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.SupportedSearchScope;
import org.jetbrains.research.ddtm.data.TypeChangeRulesStorage;
import org.jetbrains.research.ddtm.ide.migration.TypeChangeProcessor;
import org.jetbrains.research.ddtm.ide.migration.collectors.RequiredImportsCollector;
import org.jetbrains.research.ddtm.ide.migration.collectors.TypeChangesInfoCollector;
import org.jetbrains.research.ddtm.ide.settings.TypeChangeSettingsState;
import org.jetbrains.research.ddtm.utils.PsiRelatedUtils;

import java.io.File;
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


    @Override
    public void main(@NotNull List<String> args) {
        try {
            CommandLine parsedArgs = parseArgs(args.toArray(new String[0]));
            File pathToProjects = new File(parsedArgs.getOptionValue("src-projects-dir"));
            String pathToJdk = parsedArgs.getOptionValue("jdk-path");

            ApplicationManager.getApplication().runWriteAction(() -> {

                // Traverse all subdirectories in the specified directory and create project for each of them
                for (File projectDir : Objects.requireNonNull(pathToProjects.listFiles())) {
                    if (projectDir.isDirectory()) {
                        final Project project = ProjectUtil.openOrImport(projectDir.toPath(), null, false);
                        if (project == null) continue;

                        IntellijProjectUtils.loadModules(projectDir, project);
                        IntellijProjectUtils.setupJdk(project, pathToJdk);

                        TypeChangeSettingsState.getInstance().searchScope = SupportedSearchScope.PROJECT;

                        // Just for test: the types for Type Change should be specified / injected from outside
                        final String sourceType = "java.io.File";
                        final String targetType = "java.nio.file.Path";

                        // Traverse all the `.java` files in the project and build PSI for each of them
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

                                // Find the first `<caret>` in the program and repair the corresponding element:
                                // Like `F<caret>ile` to `File`
                                // TODO: check the next <caret> tags as well
                                String source = psi.getText();
                                int caretOffset = source.indexOf("<caret>");
                                if (caretOffset == -1) return true;
                                repairElementWithCaretTag(project, sourceType, psi, caretOffset);

                                WriteCommandAction.runWriteCommandAction(project, () -> {
                                    // Create built-in `TypeMigrationProcessor`
                                    final PsiElement context = psi.findElementAt(caretOffset);
                                    final var storage = project.getService(TypeChangeRulesStorage.class);
                                    final var descriptor = storage.findPattern(sourceType, targetType).get();
                                    final var typeChangeProcessor = new TypeChangeProcessor(project, false);
                                    final var builtInProcessor = typeChangeProcessor.createBuiltInTypeMigrationProcessor(context, descriptor);
                                    if (builtInProcessor == null) return;

                                    // Disable plugin's internal usages collectors and find migrating usages in the all project
                                    final var typeChangesCollector = TypeChangesInfoCollector.getInstance();
                                    final var requiredImportsCollector = RequiredImportsCollector.getInstance();
                                    typeChangesCollector.off();
                                    requiredImportsCollector.off();
                                    final UsageInfo[] usages = builtInProcessor.findUsages();

                                    // Traverse the usages, extract PsiElements and parents
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

    private void repairElementWithCaretTag(Project project, String sourceType, PsiFile psi, int caretOffset) {
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
