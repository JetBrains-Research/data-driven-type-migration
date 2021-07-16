package org.jetbrains.research.ddtm.ide.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.Config;
import org.jetbrains.research.ddtm.DataDrivenTypeMigrationBundle;
import org.jetbrains.research.ddtm.data.enums.SupportedSearchScope;

import javax.swing.*;

public class TypeChangeSettingsComponent {
    private final JPanel panel;
    private final JBIntSpinner disableIntentionTimeoutIntSpinner =
            new JBIntSpinner(Config.DISABLE_INTENTION_TIMEOUT_BY_DEFAULT, 1_000, 100_000, 1_000);
    private final ComboBox<String> searchScopeOptionsComboBox =
            new ComboBox<>(DataDrivenTypeMigrationBundle.SEARCH_SCOPE_OPTIONS.values().toArray(String[]::new));

    public TypeChangeSettingsComponent() {
        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(
                        new JBLabel(DataDrivenTypeMigrationBundle.message("settings.timeout.spinner.label")),
                        disableIntentionTimeoutIntSpinner,
                        1,
                        false
                )
                .addLabeledComponent(
                        new JBLabel(DataDrivenTypeMigrationBundle.message("settings.scope.combobox.label")),
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
        return DataDrivenTypeMigrationBundle.SEARCH_SCOPE_OPTIONS.inverse().get(searchScopeOptionsComboBox.getSelectedItem());
    }

    public void setSearchScopeOption(@NotNull SupportedSearchScope searchScope) {
        searchScopeOptionsComboBox.setSelectedItem(DataDrivenTypeMigrationBundle.SEARCH_SCOPE_OPTIONS.get(searchScope));
    }

    public int getDisableIntentionTimeout() {
        return disableIntentionTimeoutIntSpinner.getNumber();
    }

    public void setDisableIntentionTimeout(int timeout) {
        disableIntentionTimeoutIntSpinner.setNumber(timeout);
    }
}