package org.jetbrains.research.ddtm;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.intellij.openapi.project.Project;

import java.util.Map;

public class Config {
    public static final int MAX_PARENTS_TO_LIFT_UP = 3;
    public static final int WAIT_UNTIL_DISABLE_INTENTION = 10000;
    public static final int WAIT_UNTIL_COLLECT_GARBAGE = 40000;

    public static Project project;

    public static final BiMap<SupportedSearchScope, String> SEARCH_SCOPE_OPTIONS = HashBiMap.create(Map.of(
            SupportedSearchScope.FILE, "Current file",
            SupportedSearchScope.PROJECT, "All project"
    ));
}

