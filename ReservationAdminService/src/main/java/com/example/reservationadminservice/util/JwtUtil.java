package com.example.reservationadminservice.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private SecretKey secretKey;

    // Inject secret từ application.properties
    // Dùng SHA-256 để tạo key 256-bit từ bất kỳ secret nào (tránh WeakKeyException)
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        try {
            // Decode Base64 của secret
            byte[] rawBytes = Base64.getDecoder().decode(secret);
            // Hash bằng SHA-256 để đảm bảo đủ 256 bits
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(rawBytes);
            this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            // Fallback: hash plain text bằng SHA-256
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] keyBytes = digest.digest(secret.getBytes());
                this.secretKey = Keys.hmacShaKeyFor(keyBytes);
            } catch (Exception ex) {
                throw new RuntimeException("Không thể khởi tạo JWT secret key", ex);
            }
        }
    }

    // ⏰ Thời gian sống của token (1 ngày)
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000;

    // 🔹 Tạo token mới (username + role) — dùng cho admin login riêng nếu cần
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // 🔹 Lấy username/email từ token
    public String extractUsername(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    // 🔹 Kiểm tra token có hợp lệ không
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
