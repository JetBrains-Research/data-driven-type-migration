package org.jetbrains.research.ide;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.GlobalState;

import java.nio.file.Path;
import java.util.Arrays;

public class TypeChangeIntentionTest extends LightJavaCodeInsightFixtureTestCase {
    private static final String MOCK_JDK_1_8_NAME = "mockJDK-1.8";

    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

    @Override
    protected @NotNull LightProjectDescriptor getProjectDescriptor() {
        return new ProjectDescriptor(LanguageLevel.JDK_1_8) {
            @Override
            public Sdk getSdk() {
                final Path mockJdkPath = Path.of(getTestDataPath()).resolve(MOCK_JDK_1_8_NAME);
                return IdeaTestUtil.createMockJdk(MOCK_JDK_1_8_NAME, mockJdkPath.toString());
            }
        };
    }

    protected void doTest(String testProjectName, String intentionText) {
        final VirtualFile directory = myFixture.copyDirectoryToProject(testProjectName, testProjectName);
        final VirtualFile fileForEditor = Arrays.stream(directory.getChildren()).findFirst().get();
        myFixture.configureFromExistingVirtualFile(fileForEditor);
        GlobalState.project = getProject(); // TODO: eliminate

        final var action = myFixture.findSingleIntention(intentionText);
        assertNotNull(action);

        myFixture.launchAction(action);
        System.out.println(myFixture.getEditor().getDocument().getText());
    }

    public void testIntention() {
        doTest("junit5", "Data-driven type migration");
    }
}
