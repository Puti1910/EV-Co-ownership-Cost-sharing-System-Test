package com.example.reservationadminservice.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {

    // 🔑 Khóa bí mật để ký JWT (tối thiểu 32 bytes)
    private static final SecretKey SECRET_KEY =
            Keys.hmacShaKeyFor("MySuperSecretKeyForJWTGeneration123456789".getBytes());

    // ⏰ Thời gian sống của token (1 ngày)
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000;

    // 🔹 Tạo token mới
    // 🔹 Tạo token mới (username + role)
    public static String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }


    // 🔹 Lấy username từ token
    public static String extractUsername(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    // 🔹 Kiểm tra token có hợp lệ hay không
    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
