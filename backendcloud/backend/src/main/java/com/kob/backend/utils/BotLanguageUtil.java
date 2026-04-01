package com.kob.backend.utils;

import java.util.Set;

public final class BotLanguageUtil {
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("java", "python", "cpp", "javascript");

    private BotLanguageUtil() {
    }

    public static String normalize(String language) {
        if (language == null) return "java";
        return language.trim().toLowerCase();
    }

    public static boolean isSupported(String language) {
        return SUPPORTED_LANGUAGES.contains(normalize(language));
    }
}
