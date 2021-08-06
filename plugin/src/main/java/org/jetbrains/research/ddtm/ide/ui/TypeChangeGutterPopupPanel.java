package org.jetbrains.research.ddtm.ide.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.DataDrivenTypeMigrationBundle;
import org.jetbrains.research.ddtm.data.enums.SupportedSearchScope;
import org.jetbrains.research.ddtm.ide.settings.TypeChangeSettingsState;

import javax.swing.*;
import java.awt.*;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;
import static org.jetbrains.research.ddtm.utils.StringUtils.escapeSSRTemplates;

public class TypeChangeGutterPopupPanel extends JPanel {
    public Runnable onRefactor;

    private final ComboBox<String> searchScopeOptionsComboBox =
            new ComboBox<>(DataDrivenTypeMigrationBundle.SEARCH_SCOPE_OPTIONS.values().toArray(String[]::new));

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

        final var textPanel = new JPanel();
        textPanel.setLayout(new BorderLayout());
        final var header = new JLabel(DataDrivenTypeMigrationBundle.message("suggested.gutter.popup.header"));
        final var content = new JLabel(DataDrivenTypeMigrationBundle.message(
                "suggested.gutter.popup.content",
                escapeHtml(escapeSSRTemplates(sourceType)),
                escapeHtml(escapeSSRTemplates(targetType))
        ));
        content.setBorder(JBUI.Borders.empty(15, 20, 25, 20));
        textPanel.add(header, BorderLayout.NORTH);
        textPanel.add(content, BorderLayout.SOUTH);

        this.setSearchScopeOption(TypeChangeSettingsState.getInstance().searchScope);
        final var searchScopePanel = FormBuilder.createFormBuilder()
                .setHorizontalGap(30)
                .addLabeledComponent(
                        new JBLabel(DataDrivenTypeMigrationBundle.message("suggested.gutter.popup.scope.combobox.label")),
                        searchScopeOptionsComboBox,
                        1,
                        false
                )
                .getPanel();
        searchScopePanel.setBorder(JBUI.Borders.emptyBottom(15));

        add(textPanel, BorderLayout.NORTH);
        add(searchScopePanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        this.setBorder(JBUI.Borders.empty(5, 2));
        button.addActionListener(actionEvent -> onRefactor.run());
    }

    public SupportedSearchScope getSearchScopeOption() {
        return DataDrivenTypeMigrationBundle.SEARCH_SCOPE_OPTIONS.inverse().get(searchScopeOptionsComboBox.getSelectedItem());
    }

    public void setSearchScopeOption(@NotNull SupportedSearchScope searchScope) {
        searchScopeOptionsComboBox.setSelectedItem(DataDrivenTypeMigrationBundle.SEARCH_SCOPE_OPTIONS.get(searchScope));
    }
}
