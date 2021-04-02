package org.jetbrains.research.migration.json;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DataDrivenTypeMigrationRulesDescriptor {
    @SerializedName("From")
    private String sourceType;

    @SerializedName("To")
    private String targetType;

    @SerializedName("Rules")
    private List<DataDrivenTypeMigrationRule> rules;

    public String getTargetType() {
        return targetType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public List<DataDrivenTypeMigrationRule> getRules() {
        return rules;
    }
}

