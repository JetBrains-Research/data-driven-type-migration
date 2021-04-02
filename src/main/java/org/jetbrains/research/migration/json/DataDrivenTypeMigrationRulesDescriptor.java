package org.jetbrains.research.migration.json;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DataDrivenTypeMigrationRulesDescriptor {
    @SerializedName("From")
    private String fromType;

    @SerializedName("To")
    private String toType;

    @SerializedName("Rules")
    private List<DataDrivenTypeMigrationRule> rules;

    public String getToType() {
        return toType;
    }

    public String getFromType() {
        return fromType;
    }

    public List<DataDrivenTypeMigrationRule> getRules() {
        return rules;
    }
}

