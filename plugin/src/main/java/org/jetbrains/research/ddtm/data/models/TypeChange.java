package org.jetbrains.research.ddtm.data.models;

import com.google.gson.annotations.SerializedName;

public class TypeChange {
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
