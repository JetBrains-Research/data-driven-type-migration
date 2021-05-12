package org.jetbrains.research.migration.json;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TypeChangePatternDescriptor {
    @SerializedName("From")
    private String sourceType;

    @SerializedName("To")
    private String targetType;

    @SerializedName("Rules")
    private List<TypeChangeRuleDescriptor> rules;

    public String getTargetType() {
        return targetType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public List<TypeChangeRuleDescriptor> getRules() {
        return rules;
    }
}

