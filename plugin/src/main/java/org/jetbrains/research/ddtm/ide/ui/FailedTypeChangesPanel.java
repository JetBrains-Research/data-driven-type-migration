package org.jetbrains.research.ddtm.ide.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiElement;
import com.intellij.ui.content.Content;
import com.intellij.usageView.UsageInfo;

import javax.swing.*;
import java.awt.*;

public class FailedTypeChangesPanel extends JPanel implements Disposable {
    private final FailedTypeChangesUsagesPanel usagesPanel;

    public FailedTypeChangesPanel(Project project) {
        super(new BorderLayout());
        usagesPanel = new FailedTypeChangesUsagesPanel(project);
        Disposer.register(this, usagesPanel);
        add(usagesPanel, BorderLayout.CENTER);
    }

    public void setContent(final Content content) {
        Disposer.register(content, this);
    }

    public void updateLayout(UsageInfo[] infos) {
        usagesPanel.showUsages(PsiElement.EMPTY_ARRAY, infos);
    }

    @Override
    public void dispose() {
        Disposer.dispose(usagesPanel);
    }
}
