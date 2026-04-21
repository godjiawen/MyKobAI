package com.kob.backend.utils;

import java.util.Set;

/**
 * Utility for normalizing and validating bot programming language identifiers.
 * 机器人编程语言标识符的标准化与支持性校验工具类。
 */
public final class BotLanguageUtil {
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("java", "python", "cpp", "javascript");

    private BotLanguageUtil() {
    }

    /**
     * Normalizes the language string to lowercase; defaults to "java" if null.
     * 将语言字符串规范化为小写；若为null则默认返回"java"。
     */
    public static String normalize(String language) {
        if (language == null) return "java";
        return language.trim().toLowerCase();
    }

    /**
     * Returns true if the given language is in the supported languages set.
     * 若给定语言在支持的语言集合中则返回true。
     */
    public static boolean isSupported(String language) {
        return SUPPORTED_LANGUAGES.contains(normalize(language));
    }
}
