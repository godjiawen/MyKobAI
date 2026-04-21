package com.kob.backend.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for formatting Date objects to a standard string representation.
 * 将Date对象格式化为标准字符串表示的工具类。
 */
public class DateTimeUtil {
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private DateTimeUtil() {
    }

    /**
     * Formats a Date to "yyyy-MM-dd HH:mm:ss"; returns null if the date is null.
     * 将Date格式化为"yyyy-MM-dd HH:mm:ss"字符串；date为null时返回null。
     */
    public static String format(Date date) {
        if (date == null) return null;
        return new SimpleDateFormat(DEFAULT_PATTERN).format(date);
    }
}
