package org.jetbrains.research.ide.refactoring;

import com.intellij.openapi.util.TextRange;

public class TypeChangeDescriptor {
    public TextRange oldRange;
    public TextRange newRange;
    public String sourceType;
    public String targetType;

    public TypeChangeDescriptor(TextRange oldRange, TextRange newRange, String sourceType, String targetType) {
        this.oldRange = oldRange;
        this.newRange = newRange;
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    @Override
    public int hashCode() {
        int result = oldRange.hashCode();
        result = 31 * result + newRange.hashCode();
        result = 31 * result + sourceType.hashCode();
        result = 31 * result + targetType.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TypeChangeDescriptor)) return false;
        final TypeChangeDescriptor other = (TypeChangeDescriptor) obj;
        if (other == this) return true;
        return other.sourceType.equals(this.sourceType)
                && other.targetType.equals(this.targetType)
                && other.oldRange.equals(this.oldRange)
                && other.newRange.equals(this.newRange);
    }
}
