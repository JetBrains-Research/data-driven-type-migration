package org.jetbrains.research.utils;

import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.util.TextRange;

import java.util.regex.Pattern;

public class EditorUtils {
    public static boolean intersects(RangeMarker marker1, RangeMarker marker2) {
        final var range1 = new TextRange(marker1.getStartOffset(), marker1.getEndOffset());
        final var range2 = new TextRange(marker2.getStartOffset(), marker2.getEndOffset());
        return range1.intersects(range2);
    }

    public static boolean intersects(RangeMarker marker, TextRange range) {
        final var rangeFromMarker = new TextRange(marker.getStartOffset(), marker.getEndOffset());
        return rangeFromMarker.intersects(range);
    }

    private static final Pattern[] ESCAPE_HTML_PATTERNS = new Pattern[]{
            Pattern.compile("&"),
            Pattern.compile("<"),
            Pattern.compile(">")
    };
    private static final String[] ESCAPE_HTML_REPLACEMENTS = {
            "&amp;",
            "&lt;",
            "&gt;"
    };

    // https://stackoverflow.com/a/30940100
    public static String escapeHTML(String input) {
        for (int i = 0; i < ESCAPE_HTML_PATTERNS.length; i++)
            input = ESCAPE_HTML_PATTERNS[i].matcher(input).replaceAll(ESCAPE_HTML_REPLACEMENTS[i]);
        return input;
    }
}
