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

import java.io.File;
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
        Path moduleSrcPath = projectDir.toPath().resolve("src");
        Path moduleImlPath = projectDir.toPath().resolve(".iml");
        Module module = ModuleManager.getInstance(project).newModule(moduleImlPath, moduleType.getId());
        PsiTestUtil.addSourceContentToRoots(
                module,
                Objects.requireNonNull(LocalFileSystem.getInstance().findFileByPath(moduleSrcPath.toString())),
                false
        );
    }
}
