package org.jetbrains.research.ddtm;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.intellij.openapi.project.Project;

import java.util.Map;

public class Config {
    public static final int MAX_PARENTS_TO_LIFT_UP = 3;
    public static final int DISABLE_INTENTION_TIMEOUT_BY_DEFAULT = 10000;
    public static final long GARBAGE_COLLECTOR_FACTOR = 4L;

    public static Project project;

    public static final BiMap<SupportedSearchScope, String> SEARCH_SCOPE_OPTIONS = HashBiMap.create(Map.of(
            SupportedSearchScope.FILE, "Current file",
            SupportedSearchScope.PROJECT, "All project"
    ));
}

