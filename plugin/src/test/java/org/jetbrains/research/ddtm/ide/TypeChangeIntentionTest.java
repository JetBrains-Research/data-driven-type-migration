package org.jetbrains.research.ddtm.ide;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.data.TypeChangeRulesStorage;
import org.jetbrains.research.ddtm.data.enums.InvocationWorkflow;
import org.jetbrains.research.ddtm.ide.migration.TypeChangeProcessor;
import org.jetbrains.research.ddtm.utils.TypeReference;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class TypeChangeIntentionTest extends LightJavaCodeInsightFixtureTestCase {
    private static final String JDK_HOME_DIRECTORY = System.getProperty("jdk.home.path");
    private static final String MOCK_JDK_1_8_NAME = "mockJDK-1.8";
    private static final String INITIAL_FILE_NAME = "Initial.java";
    private static final String EXPECTED_FILE_NAME = "Expected.java";

    @Override
    protected String getTestDataPath() {
        return Path.of("src", "test", "resources", "testData").toString();
    }

    private String getMockJdkPath() {
        return Path.of("src", "test", "resources", MOCK_JDK_1_8_NAME).toString();
    }

    @Override
    protected @NotNull LightProjectDescriptor getProjectDescriptor() {
        return new ProjectDescriptor(LanguageLevel.JDK_1_8) {
            @Override
            public Sdk getSdk() {
                return JavaSdk.getInstance().createJdk(Path.of(JDK_HOME_DIRECTORY).getFileName().toString(), JDK_HOME_DIRECTORY, false);
            }
        };
    }

    protected void doTest(String testName, TypeReference<?> sourceType, TypeReference<?> targetType) {
        final VirtualFile directory = myFixture.copyDirectoryToProject(testName, testName);
        final VirtualFile initialFile = directory.findChild(INITIAL_FILE_NAME);
        final VirtualFile expectedFile = directory.findChild(EXPECTED_FILE_NAME);
        assertNotNull(initialFile);
        assertNotNull(expectedFile);

        myFixture.configureFromExistingVirtualFile(initialFile);
        final int offset = myFixture.getCaretOffset();
        PsiElement element = myFixture.getFile().findElementAt(offset);
        final var storage = getProject().getService(TypeChangeRulesStorage.class);

        final var result = storage.findPattern(sourceType.getType().getTypeName(), targetType.getType().getTypeName());
        assertTrue("No such pattern with specified types", result.isPresent());
        final var pattern = result.get();

        final var processor = (new TypeChangeProcessor(getProject(), InvocationWorkflow.PROACTIVE));
        WriteCommandAction.runWriteCommandAction(getProject(), () -> processor.run(element, pattern));

        PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(expectedFile);
        assertNotNull(psiFile);

        String expectedSrc = psiFile.getText().strip();
        String actualSrc = getFile().getText().strip();
        assertEquals(expectedSrc, actualSrc);
    }

    public void testFileToPath() {
        doTest("TestFileToPath",
                new TypeReference<File>() {
                },
                new TypeReference<Path>() {
                }
        );
    }

    public void testListToSet() {
        doTest("TestListToSet",
                new TypeReference<List<String>>() {
                },
                new TypeReference<Set<String>>() {
                }
        );
    }
}
