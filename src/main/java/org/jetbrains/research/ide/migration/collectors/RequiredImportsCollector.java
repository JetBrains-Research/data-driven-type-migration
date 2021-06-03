package org.jetbrains.research.ide.migration.collectors;

import java.util.HashSet;
import java.util.Set;

public class RequiredImportsCollector extends SwitchableCollector {
    private static RequiredImportsCollector collector = null;
    private final Set<String> requiredImports;

    private RequiredImportsCollector() {
        this.requiredImports = new HashSet<>();
    }

    public static RequiredImportsCollector getInstance() {
        if (collector == null) {
            collector = new RequiredImportsCollector();
        }
        return collector;
    }

    public void addRequiredImport(String requiredImport) {
        if (shouldCollect) {
            requiredImports.add(requiredImport);
        }
    }

    public Set<String> getRequiredImports() {
        return requiredImports;
    }

    public void clear() {
        requiredImports.clear();
        shouldCollect = true;
    }
}
