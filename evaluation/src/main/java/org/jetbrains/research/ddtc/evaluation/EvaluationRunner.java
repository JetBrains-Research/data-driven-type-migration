package org.jetbrains.research.ddtc.evaluation;

import com.google.gson.Gson;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.apache.commons.cli.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.GlobalState;
import org.jetbrains.research.ide.migration.TypeChangeProcessor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.copyOfRange;
import static java.util.stream.Collectors.toMap;

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
            File pathToProject = new File(parsedArgs.getOptionValue("src-projects-dir"));
            String pathToJdk = parsedArgs.getOptionValue("jdk-path");
            Path pathToOffsets = Paths.get(parsedArgs.getOptionValue("offset"));
//            int offsetOfTypeMigrationRoot = Integer.parseInt(parsedArgs.getOptionValue("offset"));
            // Also pass the name of the file and the fileOffsets where the type migration should be simulated
            // Report all the changes in a JSON or something
            // for (File projectDir : Objects.requireNonNull(pathToProjects.listFiles())) {
            // if (projectDir.isDirectory() && projectDir.toPath().toString().contains("example")) {
            Map<String, int[]> fileOffsets = Files.readAllLines(pathToOffsets).stream().map(x -> x.split(","))
                    .collect(toMap(x -> x[0], x -> Arrays.stream(copyOfRange(x, 1, x.length))
                            .mapToInt(Integer::parseInt).toArray()));
            ApplicationManager.getApplication().runWriteAction(() -> {
                        final Project project = ProjectUtil.openOrImport(pathToProject.toPath(), null, false);
                        if (project == null) return;
                        IntellijProjectUtils.loadModules(pathToProject, project);
                        IntellijProjectUtils.setupJdk(project, pathToJdk);
                        GlobalState.project = project;
                        VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentRoots();
                        final var typeChangeProcessor = new TypeChangeProcessor(project, false);
                        Set<String> alreadyAnalyzed = new HashSet<>();
                        for (var root : roots) {
                            VfsUtilCore.iterateChildrenRecursively(root, null, javaFile -> {
                                if (javaFile.getExtension() == null || javaFile.getCanonicalPath() == null || !javaFile.getExtension().equals("java")) {
                                    return true;
                                }
                                final var javaFilePsi = PsiManager.getInstance(project).findFile(javaFile);
                                if (javaFilePsi == null)
                                    return true;
                                String javaFileName = Paths.get(javaFile.getPath()).getFileName().toString();
                                int[] offsets = fileOffsets.get(javaFileName);
                                var b = Arrays.stream(offsets).boxed()
                                        .collect(toMap(x -> x,
                                                x -> WriteCommandAction.<TypeDependentCode>runWriteCommandAction(project, () -> getUsagesFor(javaFilePsi, x, typeChangeProcessor))));
                                Output o = new Output(b);
                                var json = new Gson().toJson(o, Output.class);
                                System.out.println(json);
                                return false;
                            });
                        }
                    }

            );
        } catch (Throwable e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            System.exit(0);
        }
    }

    @NotNull
    private EvaluationRunner.TypeDependentCode getUsagesFor(PsiFile psi, int offset, TypeChangeProcessor typeChangeProcessor) {
        // Create built-in `TypeMigrationProcessor`
        final @Nullable PsiElement context = psi.findElementAt(offset);
        if (context == null)
            return new TypeDependentCode(null, null);
        final var builtInProcessor = typeChangeProcessor.getTypeDependentReferences(context);
        if (builtInProcessor.isEmpty()) {
            return new TypeDependentCode(context.getTextRange(), new ArrayList<>());
        }
        // Disable plugin's internal usages collectors and find migrating usages in the all project
//                                                    final var typeChangesCollector = TypeChangesInfoCollector.getInstance();
//                                                    final var requiredImportsCollector = RequiredImportsCollector.getInstance();
//                                                    typeChangesCollector.off();
//                                                    requiredImportsCollector.off();
        var collect = Arrays.stream(builtInProcessor.get().findUsages())
                .map(usage -> getAncestors(usage.getElement(), 4))
                .peek(x -> System.out.println("---"))
                .collect(Collectors.toList());
        return new TypeDependentCode(context.getTextRange(), collect);


//                                                    for (List<PsiElement> psiElements : collect) {
//                                                        for(var p : psiElements){
//                                                            TextRange textRange = p.getTextRange();
//
//                                                        }
//                                                        psiElements.forEach(e -> {
//                                                            System.out.println(e.getText());
//                                                        });
//                                                    }
    }


    private static List<TextRange> getAncestors(PsiElement x, int n) {
//        if(!(x instanceof PsiClass))
        if (n == 0 || x instanceof PsiClass || x instanceof PsiJavaFile || x instanceof PsiCodeBlock || x instanceof PsiMethod)
            return new ArrayList<>();
        else {
            System.out.println(x.getText());
            return Stream.concat(Stream.of(x.getTextRange()), getAncestors(x.getParent(), n - 1).stream())
                    .collect(Collectors.toList());
        }
    }

//    private void repairElementWithCaretTag(Project project, String sourceType, PsiFile psi, int caretOffset) {
//        PsiElement targetElement = psi.findElementAt(caretOffset);
//        for (int i = 0; i < 4; ++i) {
//            targetElement = Objects.requireNonNull(targetElement).getParent();
//        }
//        PsiTypeCodeFragment sourcePsiType = JavaCodeFragmentFactory.getInstance(project)
//                .createTypeCodeFragment(sourceType, targetElement, true);
//        PsiTypeElement sourceTypeElement = JavaPsiFacade.getElementFactory(project)
//                .createTypeElement(Objects.requireNonNull(PsiRelatedUtils.getTypeOfCodeFragment(sourcePsiType)));
//        PsiElement finalTargetElement = targetElement;
//        WriteCommandAction.runWriteCommandAction(project, () -> {
//            finalTargetElement.replace(sourceTypeElement);
//        });
//    }

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

        Option offset = new Option(
                "o",
                "offset",
                true,
                "Offset of the element where the type migration should be invoked!"
        );
        offset.setRequired(true);
        options.addOption(offset);


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

    public static class Output {
        private Map<Integer, TypeDependentCode> rootsUsages;

        public Output(Map<Integer, TypeDependentCode> rootsUsages) {
            this.rootsUsages = rootsUsages;
        }
    }

    public static class TypeDependentCode {
        public TextRange element;
        public List<List<TextRange>> usages;

        public TypeDependentCode(TextRange element, List<List<TextRange>> usages) {
            this.element = element;
            this.usages = usages;
        }
    }
}
