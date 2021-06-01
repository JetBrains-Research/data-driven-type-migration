package org.jetbrains.research.data.models;

import com.google.gson.annotations.SerializedName;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.utils.SSRUtils;

import java.util.List;

public class TypeChangePatternDescriptor {
    @SerializedName("From")
    private String sourceType;

    @SerializedName("To")
    private String targetType;

    @SerializedName("Kind")
    private String kind;

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

    /**
     * Actually, for simple types like File and Path we already have {@link TypeChangePatternDescriptor#getTargetType()}.
     * But the following method supposed to resolve more complicated situations including generics,
     * like in the pattern "from List<$1$> to Set<$1$>", where we should substitute resolved source type to $1$.
     */
    public @NotNull String resolveTargetType(PsiType resolvedSourceType) {
        return SSRUtils.substituteTypeByPattern(resolvedSourceType, sourceType, targetType);
    }

    /**
     * This method is used for recovering original root type when applying {@link org.jetbrains.research.ide.intentions.SuggestedTypeChangeIntention}
     */
    public String resolveSourceType(PsiType resolvedTargetType) {
        return SSRUtils.substituteTypeByPattern(resolvedTargetType, targetType, sourceType);
    }
}

