package org.jetbrains.research.data.models;

import com.google.gson.annotations.SerializedName;

public class TypeChangeDescriptor {
    @SerializedName("Before")
    private String sourceType;

    @SerializedName("After")
    private String targetType;

    public String getSourceType() {
        return sourceType;
    }

    public String getTargetType() {
        return targetType;
    }
}
