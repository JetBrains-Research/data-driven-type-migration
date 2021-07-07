package org.jetbrains.research.ide.ui;

import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

import static org.jetbrains.research.utils.EditorUtils.escapeHTML;

public class TypeChangeGutterPopupPanel extends JPanel {
    public Runnable onRefactor;

    public TypeChangeGutterPopupPanel(String sourceType, String targetType) {
        super.setLayout(new BorderLayout());

        final var buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());
        JButton button = new JButton("Apply") {
            @Override
            public boolean isDefaultButton() {
                return true;
            }
        };
        buttonPanel.add(button, BorderLayout.EAST);

        final var labelsPanel = new JPanel();
        labelsPanel.setLayout(new BorderLayout());
        final var header = new JLabel("Suggested type change:");
        final var content = new JLabel(String.format(
                "<html><pre>from:<code>  %s</code></pre><br><pre>       to:<code>  %s</code></pre></html>",
                escapeHTML(sourceType), escapeHTML(targetType)
        ));
        content.setBorder(JBUI.Borders.empty(15, 30));
        labelsPanel.add(header, BorderLayout.NORTH);
        labelsPanel.add(content, BorderLayout.SOUTH);

        add(labelsPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);

        this.setBorder(JBUI.Borders.empty(5, 2));

        button.addActionListener(actionEvent -> onRefactor.run());
    }
}
