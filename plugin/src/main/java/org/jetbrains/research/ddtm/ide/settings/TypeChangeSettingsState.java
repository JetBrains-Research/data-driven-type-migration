package org.jetbrains.research.ddtm.ide.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.ddtm.Config;
import org.jetbrains.research.ddtm.data.enums.SupportedSearchScope;

/**
 * Supports storing the application settings in a persistent way.
 * The {@link State} and {@link Storage} annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@State(
        name = "TypeChangeSettingsState",
        storages = {@Storage("TypeChangeSettingsPlugin.xml")}
)
public class TypeChangeSettingsState implements PersistentStateComponent<TypeChangeSettingsState> {
    public SupportedSearchScope searchScope = SupportedSearchScope.FILE;
    public int disableIntentionTimeout = Config.DISABLE_INTENTION_TIMEOUT_BY_DEFAULT;

    public static TypeChangeSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(TypeChangeSettingsState.class);
    }

    @Nullable
    @Override
    public TypeChangeSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull TypeChangeSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
