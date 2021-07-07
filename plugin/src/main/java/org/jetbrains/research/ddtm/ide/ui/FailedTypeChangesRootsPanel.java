package org.jetbrains.research.ddtm.ide.ui;

import com.intellij.openapi.project.Project;
import com.intellij.packageDependencies.ui.UsagesPanel;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class FailedTypeChangesRootsPanel extends UsagesPanel {
    public FailedTypeChangesRootsPanel(Project project) {
        super(project);
    }

    @Override
    public @Nls String getInitialPositionText() {
        return "No failed type changes found";
    }

    @Override
    public @Nls String getCodeUsagesString() {
        return "Found failed type changes";
    }

    @Override
    public void showUsages(final PsiElement @NotNull [] primaryElements, final UsageInfo @NotNull [] usageInfos) {
        super.showUsages(primaryElements, usageInfos);
    }
}
