package com.kob.backend.utils;

import java.util.Set;

/**
 * Utility for normalizing and validating bot programming language identifiers.
 * 机器人编程语言标识符的标准化与支持性校验工具类。
 */
public final class BotLanguageUtil {
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("java", "python", "cpp", "javascript");

    /**
     * 处理 BotLanguageUtil 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of BotLanguageUtil with controlled input and output handling.
     *
     */
    private BotLanguageUtil() {
    }

    /**
     * 处理 normalize 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of normalize with controlled input and output handling.
     *
     * @param language 输入参数；Input parameter.
     * @return 返回字符串结果；Returns a string result.
     */
    public static String normalize(String language) {
        if (language == null) return "java";
        return language.trim().toLowerCase();
    }

    /**
     * 校验或判断 isSupported 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of isSupported with controlled input and output handling.
     *
     * @param language 输入参数；Input parameter.
     * @return 返回判断结果；Returns a boolean decision result.
     */
    public static boolean isSupported(String language) {
        return SUPPORTED_LANGUAGES.contains(normalize(language));
    }
}