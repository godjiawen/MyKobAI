package com.kob.backend.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for formatting Date objects to a standard string representation.
 * 将Date对象格式化为标准字符串表示的工具类。
 */
public class DateTimeUtil {
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 处理 DateTimeUtil 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of DateTimeUtil with controlled input and output handling.
     *
     */
    private DateTimeUtil() {
    }

    /**
     * 处理 format 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of format with controlled input and output handling.
     *
     * @param date 时间参数；Time parameter.
     * @return 返回字符串结果；Returns a string result.
     */
    public static String format(Date date) {
        if (date == null) return null;
        return new SimpleDateFormat(DEFAULT_PATTERN).format(date);
    }
}