package org.jetbrains.research.ddtm.data;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.research.ddtm.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ddtm.data.specifications.SourceTypeSpecification;
import org.jetbrains.research.ddtm.data.specifications.TargetTypeSpecification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Service
public final class TypeChangeRulesStorage {
    private static final Logger LOG = Logger.getInstance(TypeChangeRulesStorage.class);

    private final Project project;
    private final Set<String> sourceTypesCache = new HashSet<>();
    private final Set<String> targetTypesCache = new HashSet<>();
    private List<TypeChangePatternDescriptor> patterns;
    private List<TypeChangePatternDescriptor> inspectionPatterns;

    public TypeChangeRulesStorage(Project project) {
        this.project = project;
        String json;
        try {
            json = getResourceFileAsString("/rules.json");
            Gson gson = new Gson();
            Type type = new TypeToken<List<TypeChangePatternDescriptor>>() {
            }.getType();

            this.patterns = gson.fromJson(json, type);
            this.inspectionPatterns = Objects.requireNonNull(patterns).stream()
                    .filter(TypeChangePatternDescriptor::shouldInspect)
                    .collect(Collectors.toList());
            initCache();
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private void initCache() {
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
        return patterns;
    }

    public List<TypeChangePatternDescriptor> getInspectionPatterns() {
        return inspectionPatterns;
    }

    public List<TypeChangePatternDescriptor> getPatternsBySourceType(String sourceType) {
        return patterns.stream()
                .filter(new SourceTypeSpecification(sourceType, project))
                .collect(Collectors.toList());
    }

    public List<TypeChangePatternDescriptor> getPatternsByTargetType(String targetType) {
        return patterns.stream()
                .filter(new TargetTypeSpecification(targetType, project))
                .collect(Collectors.toList());
    }

    public Optional<TypeChangePatternDescriptor> findPattern(String sourceType, String targetType) {
        return patterns.stream()
                .filter(new SourceTypeSpecification(sourceType, project).and(new TargetTypeSpecification(targetType, project)))
                .findFirst();
    }

    private String getResourceFileAsString(String fileName) throws IOException {
        try (InputStream stream = TypeChangeRulesStorage.class.getResourceAsStream(fileName)) {
            if (stream == null) return null;
            try (InputStreamReader isr = new InputStreamReader(stream);
                 BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }
}