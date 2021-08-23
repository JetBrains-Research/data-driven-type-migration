package org.jetbrains.research.ddtm.data;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.OptionTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ddtm.data.models.TypeChangePatternDescriptorConverter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Persistent application service, keeps track of the current set of type change rules.
 */
@Service
@State(
        name = "TypeChangeRulesProvider",
        storages = {@Storage("TypeChangeRulesProvider.xml")}
)
public final class TypeChangeRulesProvider implements PersistentStateComponent<TypeChangeRulesProvider> {
    public static final Type SERIALIZATION_TYPE = new TypeToken<List<TypeChangePatternDescriptor>>() {
    }.getType();
    private static final Logger LOG = Logger.getInstance(TypeChangeRulesProvider.class);
    @OptionTag(converter = TypeChangePatternDescriptorConverter.class)
    public List<TypeChangePatternDescriptor> patterns;

    public TypeChangeRulesProvider() {
        try {
            loadStateFromJson(getResourceFileAsString("/rules.json"));
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    public static TypeChangeRulesProvider getInstance() {
        return ApplicationManager.getApplication().getService(TypeChangeRulesProvider.class);
    }

    private String getResourceFileAsString(String fileName) throws IOException {
        try (InputStream stream = TypeChangeRulesProvider.class.getResourceAsStream(fileName)) {
            if (stream == null) return null;
            try (InputStreamReader isr = new InputStreamReader(stream);
                 BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }

    @Override
    public TypeChangeRulesProvider getState() {
        return this;
    }

    public String getStateJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(patterns, SERIALIZATION_TYPE);
    }

    @Override
    public void loadState(@NotNull TypeChangeRulesProvider state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public void loadStateFromJson(String sourceJson) {
        patterns = new Gson().fromJson(sourceJson, SERIALIZATION_TYPE);
    }
}
