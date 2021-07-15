package org.jetbrains.research.ddtm;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.intellij.AbstractBundle;
import com.intellij.reference.SoftReference;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.util.Map;
import java.util.ResourceBundle;

public class DataDrivenTypeMigrationBundle {
    public static final BiMap<SupportedSearchScope, String> SEARCH_SCOPE_OPTIONS = HashBiMap.create(Map.of(
            SupportedSearchScope.LOCAL, message("settings.scope.local"),
            SupportedSearchScope.FILE, message("settings.scope.file"),
            SupportedSearchScope.PROJECT, message("settings.scope.project")
    ));
    private static final String BUNDLE = "DataDrivenTypeMigrationBundle";
    private static Reference<ResourceBundle> INSTANCE;

    private DataDrivenTypeMigrationBundle() {
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return AbstractBundle.message(getBundle(), key, params);
    }

    private static ResourceBundle getBundle() {
        ResourceBundle bundle = SoftReference.dereference(INSTANCE);
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE);
            INSTANCE = new SoftReference<>(bundle);
        }
        return bundle;
    }
}
