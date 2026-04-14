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

    public static SecretKey generalKey() {
        byte[] encodeKey = Base64.getDecoder().decode(JWT_KEY);
        return new SecretKeySpec(encodeKey, 0, encodeKey.length, "HmacSHA256");
    }

    public static Claims parseJWT(String jwt) throws Exception {
        SecretKey secretKey = generalKey();
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
    }

    public static Integer getUserId(String token) {
        try {
            Claims claims = parseJWT(token);
            return Integer.parseInt(claims.getSubject());
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }
}

