package org.jetbrains.research.ddtm.utils;

import java.util.regex.Pattern;

public class StringUtils {
    public static String escapeSSRTemplates(String input) {
        String[] genericTypes = {"T", "U", "V", "S", "K"};
        for (int i = 1; i <= 5; ++i) {
            input = input.replace(String.format("$%d$", i), genericTypes[i - 1]);
        }
        return input.replace("java.lang.", "")
                .replace(", ", ",")
                .replace(",", ", ");
    }

    // Linux or Windows absolute path
    private static final Pattern systemPathPattern =
            Pattern.compile("^/|(/[a-zA-Z0-9_.-]+)+$" + "|" + "^([a-zA-Z]:)?(\\\\[a-zA-Z0-9_.-]+)+\\\\?$");

    public static boolean isSystemPath(String source) {
        return source != null && !source.trim().isEmpty() && systemPathPattern.matcher(source).matches();
    }
}
