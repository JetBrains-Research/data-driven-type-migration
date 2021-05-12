package org.jetbrains.research.migration.models;

import com.google.gson.annotations.SerializedName;

public class TypeChangeRuleDescriptor {
    @SerializedName("Before")
    private final String expressionBefore;

    @SerializedName("After")
    private final String expressionAfter;

    public TypeChangeRuleDescriptor(String expressionBefore, String expressionAfter) {
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
