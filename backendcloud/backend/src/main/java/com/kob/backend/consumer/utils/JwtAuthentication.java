package com.kob.backend.consumer.utils;

import com.kob.backend.utils.JwtUtil;
import io.jsonwebtoken.Claims;

/**
 * Utility for extracting user identity from a JWT token in the WebSocket context.
 * 用于在WebSocket上下文中从JWT令牌中提取用户身份的工具类。
 */
public class JwtAuthentication {
    /**
     * 查询并返回 getUserId 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getUserId with controlled input and output handling.
     *
     * @param token 令牌参数；Token parameter.
     * @return 返回数值结果；Returns a numeric result.
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