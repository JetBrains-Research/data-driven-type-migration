package org.jetbrains.research.ddtm.utils;

import java.util.Map;
import java.util.regex.Pattern;

public class StringUtils {
    private static final Map<Pattern, String> ESCAPE_HTML_REPLACEMENTS = Map.of(
            Pattern.compile("&"), "&amp;",
            Pattern.compile("<"), "&lt;",
            Pattern.compile(">"), "&gt;"
    );

    // Upgraded version of https://stackoverflow.com/a/30940100
    public static String escapeHTML(String input) {
        for (var entry : ESCAPE_HTML_REPLACEMENTS.entrySet()) {
            input = entry.getKey().matcher(input).replaceAll(entry.getValue());
        }
        return input;
    }

    // Linux or Windows absolute path
    private static final Pattern systemPathPattern =
            Pattern.compile("^/|(/[a-zA-Z0-9_.-]+)+$" + "|" + "^([a-zA-Z]:)?(\\\\[a-zA-Z0-9_.-]+)+\\\\?$");

    public static boolean isSystemPath(String source) {
        return source != null && !source.trim().isEmpty() && systemPathPattern.matcher(source).matches();
    }
}