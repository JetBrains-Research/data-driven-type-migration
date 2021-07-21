package org.jetbrains.research.ddtm.data.models;

import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.ide.intentions.SuggestedTypeChangeIntention;
import org.jetbrains.research.ddtm.ide.migration.structuralsearch.SSRUtils;

import java.util.List;

public class TypeChangePatternDescriptor {
    @SerializedName("ID")
    private int id;

    @SerializedName("From")
    private String sourceType;

    @SerializedName("To")
    private String targetType;

    @SerializedName("Inspect")
    private boolean inspect;

    @SerializedName("Kind")
    private String kind;

    @SerializedName("Rules")
    private List<TypeChangeRuleDescriptor> rules;

    public boolean shouldInspect() {
        return inspect;
    }

    public int getId() {
        return id;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public List<TypeChangeRuleDescriptor> getRules() {
        return rules;
    }

    @Override
    public String toString() {
        return sourceType + " to " + targetType;
    }

    /**
     * Actually, for simple types like File and Path we already have {@link TypeChangePatternDescriptor#getTargetType()}.
     * But the following method supposed to resolve more complicated situations including generics,
     * like in the pattern "from List<$1$> to Set<$1$>", where we should substitute resolved source type to $1$.
     */
    public @NotNull String resolveTargetType(PsiType resolvedSourceType, Project project) {
        if (targetType.contains("$")) {
            return SSRUtils.substituteTypeByPattern(resolvedSourceType, sourceType, targetType, project);
        }
        return targetType;
    }

    /**
     * This method is used for recovering original root type when applying {@link SuggestedTypeChangeIntention}
     */
    public String resolveSourceType(PsiType resolvedTargetType, Project project) {
        if (sourceType.contains("$")) {
            return SSRUtils.substituteTypeByPattern(resolvedTargetType, targetType, sourceType, project);
        }
        return sourceType;
    }
}

