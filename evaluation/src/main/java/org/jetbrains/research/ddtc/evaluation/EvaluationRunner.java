package org.jetbrains.research.ddtc.evaluation;

import com.google.common.collect.Streams;
import com.google.gson.Gson;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.GlobalState;
import org.jetbrains.research.ide.migration.TypeChangeProcessor;
import org.jetbrains.research.ide.migration.collectors.RequiredImportsCollector;
import org.jetbrains.research.ide.migration.collectors.TypeChangesInfoCollector;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.copyOfRange;
import static java.util.stream.Collectors.toMap;
import static org.jetbrains.research.utils.PsiRelatedUtils.getHighestParentOfType;

public class EvaluationRunner implements ApplicationStarter {
//    private static final Logger LOG = Logger.getInstance(EvaluationRunner.class);
//    private static final String JDK_NAME = "jdk";

    @Override
    public @NonNls
    String getCommandName() {
        return "evaluation";
    }


    @Override
    public void main(@NotNull List<String> args) {
        try {
            CommandLine parsedArgs = IntellijProjectUtils.parseArgs(args.toArray(new String[0]));
            File pathToProject = new File(parsedArgs.getOptionValue("src-projects-dir"));
            String pathToJdk = parsedArgs.getOptionValue("jdk-path");
            Path pathToOffsets = Paths.get(parsedArgs.getOptionValue("offset"));
            Path pathToOutput = Paths.get(parsedArgs.getOptionValue("output"));
            Map<String, List<String>> fileOffsets = Files.readAllLines(pathToOffsets).stream().map(x -> x.split(","))
                    .collect(toMap(x -> x[0], x -> Streams.zip(Arrays.stream(copyOfRange(x, 1, x.length)),
                            Arrays.stream(copyOfRange(x, 2, x.length)), (a,b)->a+"-"+b)
                            .filter(z ->Character.isDigit(z.charAt(0))).collect(Collectors.toList())));
            Computable<Result> compute = getTypeMigrationSitesFor(pathToProject, pathToJdk, fileOffsets);
            Result result = ApplicationManager.getApplication().runWriteAction(compute);
            var jsonStr = new Gson().toJson(result, Result.class);
            Files.write(pathToOutput, jsonStr.getBytes(), StandardOpenOption.CREATE);
        } catch (Throwable e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            System.exit(0);
        }
    }

    @NotNull
    private Computable<Result> getTypeMigrationSitesFor(File pathToProject, String pathToJdk, Map<String, List<String>> fileOffsets) {
        return () -> {
            final Project project = ProjectUtil.openOrImport(pathToProject.toPath(), null, false);
            if (project == null) return new Result(new ArrayList<>());
            IntellijProjectUtils.loadModules(pathToProject, project);
            IntellijProjectUtils.setupJdk(project, pathToJdk);
            GlobalState.project = project;
            List<VirtualFile> roots = Arrays.stream(ProjectRootManager.getInstance(project).getContentRoots())
                    .filter(x -> !x.getPath().equals(pathToProject.getAbsolutePath()))
                    .collect(Collectors.toList());
            Set<String> alreadyAnalyzed = new HashSet<>();
            final var typeChangeProcessor = new TypeChangeProcessor(project, false);
            List<Output> outputs = new ArrayList<>();
            for (var root : roots) {
                VfsUtilCore.iterateChildrenRecursively(root, null, javaFile -> {
                    if(alreadyAnalyzed.contains(javaFile.getName()))
                        return true;
                    System.out.println(javaFile.getName());
                    final PsiFile javaFilePsi = getPsiJavaFile(project, javaFile);
                    if (javaFilePsi == null)
                        return true;
                    alreadyAnalyzed.add(javaFile.getName());
                    List<String> offsets = fileOffsets.get(Paths.get(javaFile.getPath()).getFileName().toString());
                    if(offsets == null)
                        return true;
                    var b = offsets.stream()
                            .collect(toMap(x -> x,
                                    x -> WriteCommandAction.<TypeDependentCode>runWriteCommandAction(project, () -> getUsagesFor(javaFilePsi, x, typeChangeProcessor, project))));
                    outputs.add(new Output(b, javaFile.getName(), pathToProject.toPath().getFileName().toString()));
                    return true;
                });
            }

            return new Result(outputs);

        };
    }

    @Nullable
    private PsiFile getPsiJavaFile(Project project, VirtualFile javaFile) {
        if (javaFile.getExtension() == null || javaFile.getCanonicalPath() == null || !javaFile.getExtension().equals("java")) {
            return null;
        }
        final var javaFilePsi = PsiManager.getInstance(project).findFile(javaFile);
        if (javaFilePsi == null)
            return null;
        return javaFilePsi;
    }

    private @NotNull TypeDependentCode getUsagesFor(PsiFile psi, String lineNoName, TypeChangeProcessor typeChangeProcessor, Project project) {

        Document document = PsiDocumentManager.getInstance(project).getDocument(psi.getContainingFile());
        int lineNo = Integer.parseInt(lineNoName.split("-")[0]);
        String name= lineNoName.split("-")[1];

        String t = psi.getText().split("\n")[lineNo - 1];
        int offset = document.getLineStartOffset(lineNo - 1) + t.indexOf(name);


        PsiElement root = getRoot(psi.findElementAt(offset));
        if (root == null)
            return new TypeDependentCode(null, null);

        System.out.println("****************");
        System.out.println(root.getText());
        System.out.println("****************");

        final var builtInProcessor = typeChangeProcessor.getTypeDependentReferences(root);
        if (builtInProcessor.isEmpty()){
            return new TypeDependentCode(root.getTextRange(), new ArrayList<>());
        }
        // Disable plugin's internal usages collectors and find migrating usages in the all project
        final var typeChangesCollector = TypeChangesInfoCollector.getInstance();
        final var requiredImportsCollector = RequiredImportsCollector.getInstance();
        typeChangesCollector.off();
        requiredImportsCollector.off();

        var collect = Arrays.stream(builtInProcessor.get().findUsages())
                .map(usage -> getAncestors(usage.getElement(), 4, document))
                .peek(x -> System.out.println("---"))
                .collect(Collectors.toList());
        return new TypeDependentCode(root.getTextRange(), collect);
    }

    private PsiTypeElement getTypeNode(@Nullable PsiElement elementAtOffset) {
        PsiParameter parElem = PsiTreeUtil.getParentOfType(elementAtOffset, PsiParameter.class);
        if(parElem!=null){
            return parElem.getTypeElement();
        }
        PsiMethod mthdElem = PsiTreeUtil.getParentOfType(elementAtOffset, PsiMethod.class);
        if(mthdElem!=null){
            return mthdElem.getReturnTypeElement();
        }
        PsiField fldElem = PsiTreeUtil.getParentOfType(elementAtOffset, PsiField.class);
        if(fldElem!=null){
            return fldElem.getTypeElement();
        }
        PsiVariable varElem = PsiTreeUtil.getParentOfType(elementAtOffset, PsiVariable.class);
        if(varElem!=null){
            return varElem.getTypeElement();
        }
        return null;
    }

    private PsiElement getRoot(PsiElement elementAtOffset) {
        if (elementAtOffset == null) return null;
        PsiParameter parElem = getHighestParentOfType(elementAtOffset, PsiParameter.class);
        if(parElem!=null) return parElem;
        PsiField fldElem = getHighestParentOfType(elementAtOffset, PsiField.class);
        if(fldElem!=null) return fldElem;
        PsiVariable varElem = getHighestParentOfType(elementAtOffset, PsiVariable.class);
        if(varElem!=null) return varElem;
        return getHighestParentOfType(elementAtOffset, PsiMethod.class);
    }


    private static List<ImmutablePair<TextRange, ImmutablePair<String,Integer>>> getAncestors(PsiElement x, int n, Document document) {
//        if(!(x instanceof PsiClass))
        if (n == 0 || x instanceof PsiClass || x instanceof PsiJavaFile || x instanceof PsiCodeBlock || x instanceof PsiMethod || x instanceof PsiIfStatement)
            return new ArrayList<>();
        else {
            System.out.println(x.getText());
            return Stream.concat(Stream.of(ImmutablePair.of(x.getTextRange(),
                    ImmutablePair.of(x.getText(),document.getLineNumber(x.getTextOffset())))), getAncestors(x.getParent(), n - 1, document).stream())
                    .collect(Collectors.toList());
        }
    }


    public static class Output {
        private final String project;
        private String fileName;
        private Map<String, TypeDependentCode> rootsUsages;

        public Output(Map<String, TypeDependentCode> rootsUsages, String fileName, String project) {
            this.rootsUsages = rootsUsages;
            this.fileName = fileName;
            this.project = project;
        }
    }

    public static class Result {
        public List<Output> allOutputs;
        public Result(List<Output> allOutputs){
            this.allOutputs = allOutputs;
        }
    }

    public static class TypeDependentCode {
        public TextRange element;
        public List<List<ImmutablePair<TextRange, ImmutablePair<String, Integer>>>> usages;
        public TypeDependentCode(TextRange element, List<List<ImmutablePair<TextRange, ImmutablePair<String, Integer>>>> usages) {
            this.element = element;
            this.usages = usages;
        }
    }
}
