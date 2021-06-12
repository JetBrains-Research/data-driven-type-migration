package org.jetbrains.research.ide.ui;

import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TypeChangeGutterPopupPanel extends JPanel {

    public TypeChangeGutterPopupPanel(Runnable onRefactor) {
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

        final var label = new JLabel("Data-driven type change");
        label.setBorder(JBUI.Borders.emptyRight(24));

        add(label, BorderLayout.NORTH);
        add(Box.createVerticalStrut(18));
        add(buttonPanel, BorderLayout.SOUTH);

        this.setBorder(JBUI.Borders.empty(5, 2));

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                onRefactor.run();
            }
        });
    }
}
