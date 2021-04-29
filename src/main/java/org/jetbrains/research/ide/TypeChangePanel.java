package org.jetbrains.research.ide;

import com.intellij.java.refactoring.JavaRefactoringBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.content.Content;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

public class TypeChangePanel extends JPanel implements Disposable {
    public TypeChangePanel(UsageInfo[] usages) {
        super(new BorderLayout());
        add(createToolbar(), BorderLayout.SOUTH);
    }

    private JComponent createToolbar() {
        final JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints(
                GridBagConstraints.RELATIVE, 0, 1, 1, 0, 1, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, JBUI.insets(5, 10, 5, 0), 0, 0
        );

        final JButton performButton = new JButton(JavaRefactoringBundle.message("type.migration.migrate.button.text"));
        panel.add(performButton, gc);

        return panel;
    }

    public void setContent(final Content content) {
        Disposer.register(content, this);
    }

    @Override
    public void dispose() {
    }
}
