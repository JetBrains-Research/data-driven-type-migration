package org.jetbrains.research.ddtm.data;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiType;
import org.jetbrains.research.ddtm.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ddtm.ide.migration.structuralsearch.SSRUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public final class TypeChangeRulesStorage {
    private static final Logger LOG = Logger.getInstance(TypeChangeRulesStorage.class);

    private final Project project;
    private final Set<String> sourceTypesCache = new HashSet<>();
    private final Set<String> targetTypesCache = new HashSet<>();
    private List<TypeChangePatternDescriptor> patterns;

    public TypeChangeRulesStorage(Project project) {
        this.project = project;
        String json;
        try {
            json = getResourceFileAsString("/rules.json");
            Gson gson = new Gson();
            Type type = new TypeToken<List<TypeChangePatternDescriptor>>() {
            }.getType();
            this.patterns = gson.fromJson(json, type);
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

    public List<TypeChangePatternDescriptor> getPatternsBySourceType(String sourceType) {
        return patterns.stream()
                .filter(pattern -> hasMatch(sourceType, pattern.getSourceType()))
                .collect(Collectors.toList());
    }

    public List<TypeChangePatternDescriptor> getPatternsByTargetType(String targetType) {
        return patterns.stream()
                .filter(pattern -> hasMatch(targetType, pattern.getTargetType()))
                .collect(Collectors.toList());
    }

    public Optional<TypeChangePatternDescriptor> findPattern(String sourceType, String targetType) {
        return patterns.stream()
                .filter(pattern -> hasMatch(sourceType, pattern.getSourceType()) && hasMatch(targetType, pattern.getTargetType()))
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

    private Boolean hasMatch(String source, String typePattern) {
        // Full match
        if (typePattern.equals(source)) return true;

        // Preventing incorrect matches for generic types, such as from List<String> to String
        if (source.contains(typePattern)) return false;

        // Matching complicated cases with substitutions, such as List<String> to List<$1$>
        if (!SSRUtils.matchType(source, typePattern, project).isEmpty()) return true;

        // Match supertypes, such as ArrayList<> to List<>
        PsiType sourceType = JavaPsiFacade.getElementFactory(project).createTypeFromText(source, null);
        for (PsiType superType : sourceType.getSuperTypes()) {
            if (!SSRUtils.matchType(superType.getCanonicalText(), typePattern, project).isEmpty()) return true;
        }
        return false;
    }
}