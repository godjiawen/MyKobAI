package com.kob.backend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Utility class for creating, signing and parsing JWT tokens.
 * JWT令牌的创建、签名和解析工具类。
 */
@Component
public class JwtUtil {
    public static final long JWT_TTL = 60 * 60 * 1000L * 24 * 14;  // 有效期14天
    public static final String JWT_KEY = "SDFGjhdsfqazwsxeddsjkdsfds121232131afasdfac";

    /**
     * 查询并返回 getUUID 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getUUID with controlled input and output handling.
     *
     * @return 返回字符串结果；Returns a string result.
     */
    public static String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 创建或保存 createJWT 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of createJWT with controlled input and output handling.
     *
     * @param subject 输入参数；Input parameter.
     * @return 返回字符串结果；Returns a string result.
     */
    public static String createJWT(String subject) {
        JwtBuilder builder = getJwtBuilder(subject, null, getUUID());
        return builder.compact();
    }

    /**
     * 查询并返回 getJwtBuilder 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getJwtBuilder with controlled input and output handling.
     *
     * @param subject 输入参数；Input parameter.
     * @param ttlMillis 输入参数；Input parameter.
     * @param uuid 标识参数；Identifier value.
     * @return 返回 JwtBuilder 类型结果；Returns a result of type JwtBuilder.
     */
    private static JwtBuilder getJwtBuilder(String subject, Long ttlMillis, String uuid) {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        SecretKey secretKey = generalKey();
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        if (ttlMillis == null) {
            ttlMillis = JwtUtil.JWT_TTL;
        }

        long expMillis = nowMillis + ttlMillis;
        Date expDate = new Date(expMillis);
        return Jwts.builder()
                .setId(uuid)
                .setSubject(subject)
                .setIssuer("sg")
                .setIssuedAt(now)
                .signWith(signatureAlgorithm, secretKey)
                .setExpiration(expDate);
    }

    /**
     * 处理 generalKey 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of generalKey with controlled input and output handling.
     *
     * @return 返回 SecretKey 类型结果；Returns a result of type SecretKey.
     */
    public static SecretKey generalKey() {
        byte[] encodeKey = Base64.getDecoder().decode(JwtUtil.JWT_KEY);
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
}