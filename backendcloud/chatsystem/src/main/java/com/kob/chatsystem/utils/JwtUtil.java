package com.kob.chatsystem.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * 聊天服务使用的令牌工具类。
 * 密钥需与 `com.kob.backend.utils.JwtUtil.JWT_KEY` 保持一致。
 */
public class JwtUtil {

    public static final String JWT_KEY = "SDFGjhdsfqazwsxeddsjkdsfds121232131afasdfac";

    /**
     * 处理 generalKey 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of generalKey with controlled input and output handling.
     *
     * @return 返回 SecretKey 类型结果；Returns a result of type SecretKey.
     */
    public static SecretKey generalKey() {
        byte[] encodeKey = Base64.getDecoder().decode(JWT_KEY);
        return new SecretKeySpec(encodeKey, 0, encodeKey.length, "HmacSHA256");
    }

    /**
     * 构建或转换 parseJWT 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of parseJWT with controlled input and output handling.
     *
     * @param jwt 输入参数；Input parameter.
     * @return 返回 Claims 类型结果；Returns a result of type Claims.
     */
    public static Claims parseJWT(String jwt) throws Exception {
        SecretKey secretKey = generalKey();
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
    }

    /**
     * 查询并返回 getUserId 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getUserId with controlled input and output handling.
     *
     * @param token 令牌参数；Token parameter.
     * @return 返回数值结果；Returns a numeric result.
     */
    public static Integer getUserId(String token) {
        try {
            Claims claims = parseJWT(token);
            return Integer.parseInt(claims.getSubject());
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }
}
