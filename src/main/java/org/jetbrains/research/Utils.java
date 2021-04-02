package org.jetbrains.research;

public class Utils {
    public static String[] splitByTokens(String source) {
        return source.split("[\\s.()]+");
    }
}
