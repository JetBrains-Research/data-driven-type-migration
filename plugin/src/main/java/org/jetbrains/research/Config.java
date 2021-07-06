package org.jetbrains.research;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.SearchScope;

public class Config {
    public static final int MAX_PARENTS_TO_LIFT_UP = 3;
    public static final int WAIT_UNTIL_DISABLE_INTENTION = 10000;
    public static final int WAIT_UNTIL_COLLECT_GARBAGE = 40000;

    public static Project project;
    public static SearchScope searchScope;
}
