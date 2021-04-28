package org.jetbrains.research.ide;

import com.intellij.dvcs.repo.Repository;
import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.intellij.openapi.vcs.impl.VcsInitObject;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.testFramework.HeavyPlatformTestCase;
import git4idea.checkout.GitCheckoutProvider;
import git4idea.commands.Git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

public class DataDrivenTypeMigrationIntentionTest extends HeavyPlatformTestCase {
    final String url = "https://github.com/JetBrains-Research/data-driven-type-migration.git";
    final String commitHashToCheckout = "9eaa67bb";

    Path myTestNioRoot;

    private VirtualFile getMyProjectRoot() {
        return getOrCreateProjectBaseDir();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.myTestNioRoot = getTempDir().createVirtualDir("test_root").toNioPath();
    }

    public void testCloningProject() throws IOException, InterruptedException {
        final String projectName = url.substring(url.lastIndexOf('/') + 1).replace(".git", "");
        final String parentDirectory = myTestNioRoot.toString();
        final Git git = Git.getInstance();

        final var indicator = new EmptyProgressIndicator();
        ProgressManager.getInstance().executeProcessUnderProgress(() -> {
            assertTrue(GitCheckoutProvider.doClone(getProject(), git, projectName, parentDirectory, url));
        }, indicator);

        VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
        VfsUtil.markDirtyAndRefresh(false, true, false, getMyProjectRoot());

        ((ProjectLevelVcsManagerImpl) ProjectLevelVcsManager.getInstance(myProject))
                .addInitializationRequest(VcsInitObject.AFTER_COMMON, () -> {
                    Collection<Repository> repositories = VcsRepositoryManager.getInstance(myProject).getRepositories();
                    System.out.println(repositories);
                });

        FileUtil.delete(myTestNioRoot);
    }

}
