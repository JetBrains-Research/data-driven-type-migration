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
        return SSRUtils.hasMatch(sourceType, pattern.getSourceType(), project);
    }
}
