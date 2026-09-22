package com.example.mall.user.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {

    // 签名密钥：至少 32 个字符（HS256 要求 256 位）。生产环境要放配置文件，不能硬编码
    private static final String SECRET = "mall-jwt-secret-key-2026-0123456789-abcdefghijklmnopqrstuvwxyz";

    // 过期时间：30 分钟
    private static final long EXPIRE = 1000 * 60 * 30;

    // 把字符串密钥转成 JWT 需要的 SecretKey 对象
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    // 生成 token
    public static String generateToken(String username) {
        return Jwts.builder()
                .subject(username)                                          // payload：放用户名
                .issuedAt(new Date())                                       // 签发时间
                .expiration(new Date(System.currentTimeMillis() + EXPIRE))  // 过期时间
                .signWith(KEY)                                              // 用密钥签名
                .compact();                                                 // 拼成最终字符串
    }

    // 从 token 里解析出用户名
    public static String parseUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(KEY)          // 用同样的密钥验证签名（防篡改）
                .build()
                .parseSignedClaims(token) // 解析并验签
                .getPayload();            // 拿到 payload 部分
        return claims.getSubject();       // 取回用户名
    }
}
