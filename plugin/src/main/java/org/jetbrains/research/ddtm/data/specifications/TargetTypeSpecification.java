package org.jetbrains.research.ddtm.data.specifications;

import com.intellij.openapi.project.Project;
import org.jetbrains.research.ddtm.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ddtm.ide.migration.structuralsearch.SSRUtils;

public class TargetTypeSpecification extends AbstractPatternSpecification<TypeChangePatternDescriptor> {
    private final Project project;
    private final String targetType;

    public TargetTypeSpecification(String targetType, Project project) {
        this.project = project;
        this.targetType = targetType;
    }

    @Override
    public boolean test(TypeChangePatternDescriptor pattern) {
        return SSRUtils.hasMatch(targetType, pattern.getTargetType(), project);
    }
}