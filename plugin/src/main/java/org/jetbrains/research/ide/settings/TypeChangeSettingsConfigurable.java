package org.jetbrains.research.ide.settings;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.DataDrivenTypeMigrationBundle;

import javax.swing.*;

/**
 * Provides controller functionality for application settings.
 */
public class TypeChangeSettingsConfigurable implements Configurable {
    private TypeChangeSettingsComponent settingsComponent;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return DataDrivenTypeMigrationBundle.message("settings.display.name");
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return settingsComponent.getPreferredFocusedComponent();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        settingsComponent = new TypeChangeSettingsComponent();
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        TypeChangeSettingsState settings = TypeChangeSettingsState.getInstance();
        return !settingsComponent.getSearchScopeOption().equals(settings.searchScope);
    }

    @Override
    public void apply() {
        TypeChangeSettingsState settings = TypeChangeSettingsState.getInstance();
        settings.searchScope = settingsComponent.getSearchScopeOption();
    }

    @Override
    public void reset() {
        TypeChangeSettingsState settings = TypeChangeSettingsState.getInstance();
        settingsComponent.setSearchScopeOption(settings.searchScope);
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}