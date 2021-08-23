package org.jetbrains.research.ddtm.data;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.research.ddtm.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ddtm.data.models.TypeChangeRuleDescriptor;
import org.jetbrains.research.ddtm.data.specifications.SourceTypeSpecification;
import org.jetbrains.research.ddtm.data.specifications.TargetTypeSpecification;
import org.jetbrains.research.ddtm.utils.PsiRelatedUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A DAO object for querying patterns, also a project service.
 */
@Service
public final class TypeChangeRulesStorage {
    private final Project project;
    private final Set<String> sourceTypesCache = new HashSet<>();
    private final Set<String> targetTypesCache = new HashSet<>();

    public TypeChangeRulesStorage(Project project) {
        this.project = project;
        initCache();
    }

    private void initCache() {
        final var patterns = TypeChangeRulesProvider.getInstance().getState().patterns;
        for (var pattern : patterns) {
            sourceTypesCache.add(pattern.getSourceType());
            targetTypesCache.add(pattern.getTargetType());
        }
    }

    public Boolean hasSourceType(String sourceType) {
        if (sourceTypesCache.contains(sourceType)) return true;
        return !getPatternsBySourceType(sourceType).isEmpty();
    }

    public Boolean hasTargetType(String targetType) {
        if (targetTypesCache.contains(targetType)) return true;
        return !getPatternsByTargetType(targetType).isEmpty();
    }

    public List<TypeChangePatternDescriptor> getPatterns() {
        return TypeChangeRulesProvider.getInstance().getState().patterns;
    }

    public List<TypeChangePatternDescriptor> getInspectionPatterns() {
        return TypeChangeRulesProvider.getInstance().getState().patterns.stream()
                .filter(TypeChangePatternDescriptor::shouldInspect)
                .collect(Collectors.toList());
    }

    public List<TypeChangePatternDescriptor> getPatternsBySourceType(String sourceType) {
        final var patterns = TypeChangeRulesProvider.getInstance().getState().patterns;
        return patterns.stream()
                .filter(new SourceTypeSpecification(sourceType, project))
                .collect(Collectors.toList());
    }

    public Optional<TypeChangePatternDescriptor> findPatternByRule(TypeChangeRuleDescriptor rule) {
        final var patterns = TypeChangeRulesProvider.getInstance().getState().patterns;
        return patterns.stream()
                .filter(pattern -> pattern.getRules().contains(rule))
                .findFirst();
    }

    public List<TypeChangePatternDescriptor> getPatternsByTargetType(String targetType) {
        final var patterns = TypeChangeRulesProvider.getInstance().getState().patterns;
        return patterns.stream()
                .filter(new TargetTypeSpecification(targetType, project))
                .collect(Collectors.toList());
    }

    public Optional<TypeChangePatternDescriptor> findPattern(String sourceType, String targetType) {
        final var patterns = TypeChangeRulesProvider.getInstance().getState().patterns;
        return patterns.stream()
                .filter(new SourceTypeSpecification(sourceType, project)
                        .and(new TargetTypeSpecification(targetType, project)))
                .max(Comparator.comparing(pattern -> PsiRelatedUtils.splitByTokens(pattern.toString()).length));
    }
}