package org.jetbrains.research.data.models;

import com.google.gson.annotations.SerializedName;

public class TypeChangeRuleDescriptor {
    @SerializedName("Before")
    private String expressionBefore;

    @SerializedName("After")
    private String expressionAfter;

    @SerializedName("RequiredImports")
    private String requiredImports;

    @SerializedName("ReturnType")
    private TypeChangeDescriptor returnType;

    public String getExpressionAfter() {
        return expressionAfter;
    }

    public String getExpressionBefore() {
        return expressionBefore;
    }

    public TypeChangeDescriptor getReturnType() {
        return returnType;
    }

    public String getRequiredImports() {
        return requiredImports;
    }
}
