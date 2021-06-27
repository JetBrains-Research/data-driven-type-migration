package org.jetbrains.research.ddtc.evaluation;

import com.intellij.ide.impl.NewProjectUtil;
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
import com.intellij.testFramework.PsiTestUtil;
import org.apache.commons.cli.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class IntellijProjectUtils {
    public static void setupJdk(Project project, String jdkHomeDirectory) {
        Sdk jdk = JavaSdk.getInstance().createJdk(Path.of(jdkHomeDirectory).getFileName().toString(), jdkHomeDirectory, false);
        ProjectJdkTable.getInstance().addJdk(jdk);
        ProjectRootManager.getInstance(project).setProjectSdk(jdk);
        NewProjectUtil.applyJdkToProject(project, jdk);
        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            ModuleRootModificationUtil.setModuleSdk(module, jdk);
        }
    }

    public static void loadModules(File projectDir, Project project) {
        // FIXME: there is only one root module in the project (`src` folder) for current test project
        // it should resolve any number of modules potentially
        final var moduleType = JavaModuleType.getModuleType();

        Path moduleSrcPath = projectDir.toPath().resolve("src").resolve("main").resolve("java");
        if(!Files.exists(moduleSrcPath)){
            moduleSrcPath = projectDir.toPath().resolve("src");
        }
        if(!Files.exists(moduleSrcPath)){
            moduleSrcPath = projectDir.toPath().resolve("java");
        }
        Path moduleImlPath = projectDir.toPath().resolve(".iml");
        Module module = ModuleManager.getInstance(project).newModule(moduleImlPath, moduleType.getId());
        PsiTestUtil.addSourceContentToRoots(
                module,
                Objects.requireNonNull(LocalFileSystem.getInstance().findFileByPath(moduleSrcPath.toString())),
                false
        );


        Path moduleTestPath = projectDir.toPath().resolve("src").resolve("test").resolve("java");
        if(Files.exists(moduleTestPath)) {
            Module testModule = ModuleManager.getInstance(project).newModule(moduleImlPath, moduleType.getId());
            PsiTestUtil.addSourceContentToRoots(
                    testModule,
                    Objects.requireNonNull(LocalFileSystem.getInstance().findFileByPath(moduleTestPath.toString())),
                    true
            );
        }
    }

    static CommandLine parseArgs(String[] args) {
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

        Option output = new Option(
                "op",
                "output",
                true,
                "File where the migration sites should be collected"
        );
        output.setRequired(true);
        options.addOption(output);


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


//    public static boolean isParentOfSrcMAinJava(Path p){
//        return p.resolve("src")
//    }
}
