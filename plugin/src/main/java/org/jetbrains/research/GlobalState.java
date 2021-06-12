package org.jetbrains.research;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.SearchScope;

public class GlobalState {
    // TODO: remove it and inject project everywhere manually
    public static Project project;
    public static SearchScope searchScope;
}
