package org.jetbrains.research.ddtm.data.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.util.xmlb.Converter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.jetbrains.research.ddtm.data.TypeChangeRulesProvider.SERIALIZATION_TYPE;

public class TypeChangePatternDescriptorConverter extends Converter<List<TypeChangePatternDescriptor>> {
    @Override
    public @Nullable List<TypeChangePatternDescriptor> fromString(@NotNull String value) {
        return new Gson().fromJson(value, SERIALIZATION_TYPE);
    }

    @Override
    public @Nullable String toString(@NotNull List<TypeChangePatternDescriptor> value) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(value, SERIALIZATION_TYPE);
    }
}
