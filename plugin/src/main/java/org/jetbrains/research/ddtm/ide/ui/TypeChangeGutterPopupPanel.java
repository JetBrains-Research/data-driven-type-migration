package org.jetbrains.research.ddtm.ide.ui;

import com.intellij.util.ui.JBUI;
import org.jetbrains.research.ddtm.DataDrivenTypeMigrationBundle;
import org.jetbrains.research.ddtm.utils.EditorUtils;

import javax.swing.*;
import java.awt.*;

public class TypeChangeGutterPopupPanel extends JPanel {
    public Runnable onRefactor;

    public TypeChangeGutterPopupPanel(String sourceType, String targetType) {
        super.setLayout(new BorderLayout());

        final var buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());
        JButton button = new JButton(DataDrivenTypeMigrationBundle.message("suggested.gutter.popup.button")) {
            @Override
            public boolean isDefaultButton() {
                return true;
            }
        };
        buttonPanel.add(button, BorderLayout.EAST);

        final var labelsPanel = new JPanel();
        labelsPanel.setLayout(new BorderLayout());
        final var header = new JLabel(DataDrivenTypeMigrationBundle.message("suggested.gutter.popup.header"));
        final var content = new JLabel(DataDrivenTypeMigrationBundle.message(
                "suggested.gutter.popup.content",
                EditorUtils.escapeHTML(sourceType),
                EditorUtils.escapeHTML(targetType))
        );
        content.setBorder(JBUI.Borders.empty(15, 30));
        labelsPanel.add(header, BorderLayout.NORTH);
        labelsPanel.add(content, BorderLayout.SOUTH);

        add(labelsPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);

        this.setBorder(JBUI.Borders.empty(5, 2));

        button.addActionListener(actionEvent -> onRefactor.run());
    }
}
