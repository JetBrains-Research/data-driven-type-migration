package org.jetbrains.research.utils;

import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.util.TextRange;

public class CommonUtils {
    public static boolean intersects(RangeMarker marker1, RangeMarker marker2) {
        final var range1 = new TextRange(marker1.getStartOffset(), marker1.getEndOffset());
        final var range2 = new TextRange(marker2.getStartOffset(), marker2.getEndOffset());
        return range1.intersects(range2);
    }

    public static boolean intersects(RangeMarker marker, TextRange range) {
        final var rangeFromMarker = new TextRange(marker.getStartOffset(), marker.getEndOffset());
        return rangeFromMarker.intersects(range);
    }
}
