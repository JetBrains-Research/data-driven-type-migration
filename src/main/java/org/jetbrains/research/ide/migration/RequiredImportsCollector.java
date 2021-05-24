package org.jetbrains.research.ide.migration;

import java.util.HashSet;
import java.util.Set;

class RequiredImportsCollector {
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
        requiredImports.add(requiredImport);
    }

    public Set<String> getRequiredImports() {
        return requiredImports;
    }

    public void clear() {
        requiredImports.clear();
    }
}
