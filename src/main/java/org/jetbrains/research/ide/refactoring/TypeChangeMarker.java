package org.jetbrains.research.ide.refactoring;

import com.intellij.openapi.editor.RangeMarker;

public class TypeChangeMarker {
    public RangeMarker oldRangeMarker;
    public RangeMarker newRangeMarker;
    public String sourceType;
    public String targetType;

    public TypeChangeMarker(RangeMarker oldRangeMarker, RangeMarker newRangeMarker, String sourceType, String targetType) {
        this.oldRangeMarker = oldRangeMarker;
        this.newRangeMarker = newRangeMarker;
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    @Override
    public int hashCode() {
        int result = oldRangeMarker.hashCode();
        result = 31 * result + newRangeMarker.hashCode();
        result = 31 * result + sourceType.hashCode();
        result = 31 * result + targetType.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TypeChangeMarker)) return false;
        final TypeChangeMarker other = (TypeChangeMarker) obj;
        if (other == this) return true;
        return other.sourceType.equals(this.sourceType)
                && other.targetType.equals(this.targetType)
                && other.oldRangeMarker.equals(this.oldRangeMarker)
                && other.newRangeMarker.equals(this.newRangeMarker);
    }
}