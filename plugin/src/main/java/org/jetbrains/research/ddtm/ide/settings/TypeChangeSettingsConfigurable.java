package org.jetbrains.research.ddtm.ide.settings;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.ddtm.DataDrivenTypeMigrationBundle;

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
        return !settingsComponent.getSearchScopeOption().equals(settings.searchScope)
                || settingsComponent.getDisableIntentionTimeout() != settings.disableIntentionTimeout;
    }

    @Override
    public void apply() {
        TypeChangeSettingsState settings = TypeChangeSettingsState.getInstance();
        settings.searchScope = settingsComponent.getSearchScopeOption();
        settings.disableIntentionTimeout = settingsComponent.getDisableIntentionTimeout();
    }

    @Override
    public void reset() {
        TypeChangeSettingsState settings = TypeChangeSettingsState.getInstance();
        settingsComponent.setSearchScopeOption(settings.searchScope);
        settingsComponent.setDisableIntentionTimeout(settings.disableIntentionTimeout);
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}