package org.jetbrains.research.ddtm.data.specifications;

import com.intellij.openapi.project.Project;
import org.jetbrains.research.ddtm.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ddtm.ide.migration.structuralsearch.SSRUtils;

public class SourceTypeSpecification extends AbstractPatternSpecification<TypeChangePatternDescriptor> {
    private final Project project;
    private final String sourceType;

    public SourceTypeSpecification(String sourceType, Project project) {
        this.project = project;
        this.sourceType = sourceType;
    }

    @Override
    public boolean test(TypeChangePatternDescriptor pattern) {
        // Heuristics to prevent suggestions like `Migrate to Optional<Optional<Integer>>`, etc.
        if (sourceType.startsWith("java.util.List") && pattern.getTargetType().startsWith("java.util.List"))
            return false;
        if (sourceType.startsWith("java.util.Optional") && pattern.getTargetType().startsWith("java.util.Optional"))
            return false;
        return SSRUtils.hasMatch(sourceType, pattern.getSourceType(), project);
    }
}
