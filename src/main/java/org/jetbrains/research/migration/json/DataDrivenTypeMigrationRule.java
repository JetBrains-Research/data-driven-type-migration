package org.jetbrains.research.migration.json;

import com.google.gson.annotations.SerializedName;

public class DataDrivenTypeMigrationRule {
    @SerializedName("Before")
    private String expressionBefore;

    @SerializedName("After")
    private String expressionAfter;

    public DataDrivenTypeMigrationRule(String expressionBefore, String expressionAfter) {
        this.expressionBefore = expressionBefore;
        this.expressionAfter = expressionAfter;
    }

    public String getExpressionAfter() {
        return expressionAfter;
    }

    public String getExpressionBefore() {
        return expressionBefore;
    }
}
