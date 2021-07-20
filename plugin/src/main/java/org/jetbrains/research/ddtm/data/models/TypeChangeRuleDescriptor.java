package org.jetbrains.research.ddtm.data.models;

import com.google.gson.annotations.SerializedName;

public class TypeChangeRuleDescriptor {
    @SerializedName("Before")
    private String expressionBefore;

    @SerializedName("After")
    private String expressionAfter;

    @SerializedName("RequiredImports")
    private String requiredImports;

    @SerializedName("ReturnType")
    private TypeChange returnType;

    public String getExpressionAfter() {
        return expressionAfter;
    }

    public String getExpressionBefore() {
        return expressionBefore;
    }

    public TypeChange getReturnType() {
        return returnType;
    }

    public String getRequiredImports() {
        return requiredImports;
    }

    @Override
    public String toString() {
        return expressionBefore.replaceAll("\\$.*?\\$", "")
                + " -> "
                + expressionAfter.replaceAll("\\$.*?\\$", "");
    }
}
