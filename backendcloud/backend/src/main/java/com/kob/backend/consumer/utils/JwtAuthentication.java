package com.kob.backend.consumer.utils;

import com.kob.backend.utils.JwtUtil;
import io.jsonwebtoken.Claims;

/**
 * Utility for extracting user identity from a JWT token in the WebSocket context.
 * 用于在WebSocket上下文中从JWT令牌中提取用户身份的工具类。
 */
public class JwtAuthentication {
    /**
     * Parses the given JWT token and returns the user ID encoded as its subject.
     * 解析给定的JWT令牌并返回其主题中编码的用户ID。
     */
    public static Integer getUserId(String token) {
        int userId = -1;
        try {
            Claims claims = JwtUtil.parseJWT(token);
            userId = Integer.parseInt(claims.getSubject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return userId;
    }
}
