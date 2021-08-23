package org.jetbrains.research.ddtm.ide.ui;

import com.intellij.ui.components.JBTextArea;

import javax.swing.*;
import java.awt.*;

public class TypeChangeRulesJsonEditorPanel extends JPanel {
    private final JBTextArea textArea;

    public TypeChangeRulesJsonEditorPanel(String text) {
        textArea = new JBTextArea(text);
        textArea.setPreferredSize(new Dimension(400, 400));
        add(textArea);
    }

    public String getText() {
        return textArea.getText();
    }

    public void setText(String text) {
        textArea.setText(text);
    }
}
