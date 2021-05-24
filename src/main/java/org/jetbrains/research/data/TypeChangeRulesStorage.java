package org.jetbrains.research.data;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.utils.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeChangeRulesStorage {
    private static final Logger LOG = Logger.getInstance(TypeChangeRulesStorage.class);
    private static List<TypeChangePatternDescriptor> patterns;
    private static final Set<String> sourceTypesCache = new HashSet<>();
    private static final Set<String> targetTypesCache = new HashSet<>();

    static {
        String json;
        try {
            json = getResourceFileAsString("/new_rules.json");
            Gson gson = new Gson();
            Type type = new TypeToken<List<TypeChangePatternDescriptor>>() {
            }.getType();
            patterns = gson.fromJson(json, type);
            initCache();
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    public static Boolean hasSourceType(String sourceType) {
        return sourceTypesCache.contains(sourceType);
    }

    public static Boolean hasTargetType(String targetType) {
        return targetTypesCache.contains(targetType);
    }

    public static List<TypeChangePatternDescriptor> getPatterns() {
        return patterns;
    }

    public static List<TypeChangePatternDescriptor> getPatternsBySourceType(String sourceType) {
        return patterns.stream()
                .filter(pattern -> hasMatch(sourceType, pattern.getSourceType()))
                .collect(Collectors.toList());
    }

    public static List<TypeChangePatternDescriptor> getPatternsByTargetType(String targetType) {
        return patterns.stream()
                .filter(pattern -> hasMatch(targetType, pattern.getTargetType()))
                .collect(Collectors.toList());
    }

    @Nullable
    public static TypeChangePatternDescriptor findPattern(String sourceType, String targetType) {
        return patterns.stream()
                .filter(pattern ->
                        hasMatch(sourceType, pattern.getSourceType())
                                && hasMatch(targetType, pattern.getTargetType()))
                .findFirst()
                .orElse(null);
    }

    private static void initCache() {
        for (var pattern : patterns) {
            sourceTypesCache.add(pattern.getSourceType());
            targetTypesCache.add(pattern.getTargetType());
        }
    }

    private static String getResourceFileAsString(String fileName) throws IOException {
        try (InputStream stream = TypeChangeRulesStorage.class.getResourceAsStream(fileName)) {
            if (stream == null) return null;
            try (InputStreamReader isr = new InputStreamReader(stream);
                 BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }

    private static Boolean hasMatch(String source, String pattern) {
        // Full match
        if (pattern.equals(source)) return true;

        // Preventing incorrect matches for generic types, such as from List<String> to String
        if (source.contains(pattern)) return false;

        // Matching complicated cases with substitutions, such as List<String> to List<$1$>
        return !StringUtils.findMatches(source, pattern).isEmpty();
    }
}