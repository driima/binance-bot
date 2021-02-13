package com.driima.binance;

import java.util.HashMap;
import java.util.Map;

public final class TextUtils {

    private static final char ZWSP = '\u200B';
    private static final double SPACE_WIDTH = 3.075;

    private static final Map<Integer, Character> SIZED_SPACES = new HashMap<>();
    static {
        SIZED_SPACES.put(14, ' '); // EM
        SIZED_SPACES.put(7, ' '); // EM
        SIZED_SPACES.put(3, ' '); // EM
    }

    public static String asCode(String language, String code) {
        return "```" + language + "\n" + code + "```";
    }

    public static String asCode(String code) {
        return "```" + code + "```";
    }

    public static String asInlineCode(String code) {
        return "`" + code + "`";
    }
}
