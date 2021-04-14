package org.jetbrains.research.migration;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.utils.StringUtils;
import org.jetbrains.research.migration.json.DataDrivenTypeMigrationRulesDescriptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

public class DataDrivenRulesStorage {
    private static final Logger LOG = Logger.getInstance(DataDrivenRulesStorage.class);
    private static List<DataDrivenTypeMigrationRulesDescriptor> rulesDescriptors;

    private static String getResourceFileAsString(String fileName) throws IOException {
        try (InputStream stream = DataDrivenRulesStorage.class.getResourceAsStream(fileName)) {
            if (stream == null) return null;
            try (InputStreamReader isr = new InputStreamReader(stream);
                 BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }

    static {
        String json = null;
        try {
            json = getResourceFileAsString("/rules.json");
            Gson gson = new Gson();
            Type type = new TypeToken<List<DataDrivenTypeMigrationRulesDescriptor>>() {
            }.getType();
            rulesDescriptors = gson.fromJson(json, type);
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    public static List<DataDrivenTypeMigrationRulesDescriptor> getRulesDescriptors() {
        return rulesDescriptors;
    }

    public static List<DataDrivenTypeMigrationRulesDescriptor> getRulesDescriptorsBySourceType(String sourceType) {
        return rulesDescriptors.stream()
                .filter(descriptor -> {

                    // Full match
                    if (descriptor.getSourceType().equals(sourceType)) {
                        return true;
                    }

                    // Preventing incorrect matches for generic types, such as from List<String> to String
                    if (sourceType.contains(descriptor.getSourceType())) {
                        return false;
                    }

                    // Matching complicated cases with substitutions, such as List<String> to List<$1$>
                    return !StringUtils.findMatches(sourceType, descriptor.getSourceType()).isEmpty();
                })
                .collect(Collectors.toList());
    }

    @Nullable
    public static DataDrivenTypeMigrationRulesDescriptor findDescriptor(String sourceType, String targetType) {
        for (var descriptor : rulesDescriptors) {

            // TODO: eliminate copy-paste

            // Full match
            if (descriptor.getSourceType().equals(sourceType) && descriptor.getTargetType().equals(targetType)) {
                return descriptor;
            }

            // Preventing incorrect matches for generic types, such as from List<String> to String
            if (sourceType.contains(descriptor.getSourceType())) {
                continue;
            }

            // Matching complicated cases with substitutions, such as List<String> to List<$1$>
            if (!StringUtils.findMatches(sourceType, descriptor.getSourceType()).isEmpty()) {
                return descriptor;
            }
        }
        return null;
    }
}
