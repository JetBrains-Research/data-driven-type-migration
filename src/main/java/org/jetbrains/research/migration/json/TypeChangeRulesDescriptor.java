package org.jetbrains.research.migration.json;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TypeChangeRulesDescriptor {
    @SerializedName("From")
    private String sourceType;

    @SerializedName("To")
    private String targetType;

    @SerializedName("Rules")
    private List<TypeChangeRule> rules;

    public String getTargetType() {
        return targetType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public List<TypeChangeRule> getRules() {
        return rules;
    }
}

