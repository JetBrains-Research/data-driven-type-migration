package org.jetbrains.research.ide.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.SupportedSearchScope;

import javax.swing.*;

import static org.jetbrains.research.Config.SEARCH_SCOPE_OPTIONS;

public class TypeChangeSettingsComponent {
    private final JPanel panel;
    private final ComboBox<String> searchScopeOptionsComboBox = new ComboBox<>(SEARCH_SCOPE_OPTIONS.values().toArray(String[]::new));

    public TypeChangeSettingsComponent() {
        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel(
                                "Choose search scope for migration: "),
                        searchScopeOptionsComboBox,
                        1,
                        false
                )
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() {
        return panel;
    }

    public JComponent getPreferredFocusedComponent() {
        return searchScopeOptionsComboBox;
    }

    public SupportedSearchScope getSearchScopeOption() {
        return SEARCH_SCOPE_OPTIONS.inverse().get(searchScopeOptionsComboBox.getSelectedItem());
    }

    public void setSearchScopeOption(@NotNull SupportedSearchScope searchScope) {
        searchScopeOptionsComboBox.setSelectedItem(SEARCH_SCOPE_OPTIONS.get(searchScope));
    }
}