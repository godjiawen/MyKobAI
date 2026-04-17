package com.kob.backend.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeUtil {
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private DateTimeUtil() {
    }

    public static String format(Date date) {
        if (date == null) return null;
        return new SimpleDateFormat(DEFAULT_PATTERN).format(date);
    }
}
