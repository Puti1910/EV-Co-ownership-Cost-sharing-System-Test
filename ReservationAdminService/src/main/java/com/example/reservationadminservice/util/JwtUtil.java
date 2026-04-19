package com.example.reservationadminservice.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
<<<<<<< HEAD
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.util.Base64;
=======
import javax.crypto.SecretKey;
>>>>>>> origin/main
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

<<<<<<< HEAD
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
=======
public class JwtUtil {

    // 🔑 Khóa bí mật để ký JWT (tối thiểu 32 bytes)
    private static final SecretKey SECRET_KEY =
            Keys.hmacShaKeyFor("MySuperSecretKeyForJWTGeneration123456789".getBytes());
>>>>>>> origin/main

    // ⏰ Thời gian sống của token (1 ngày)
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000;

<<<<<<< HEAD
    // 🔹 Tạo token mới (username + role) — dùng cho admin login riêng nếu cần
    public String generateToken(String username, String role) {
=======
    // 🔹 Tạo token mới
    // 🔹 Tạo token mới (username + role)
    public static String generateToken(String username, String role) {
>>>>>>> origin/main
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
<<<<<<< HEAD
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // 🔹 Lấy username/email từ token
    public String extractUsername(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
=======
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }


    // 🔹 Lấy username từ token
    public static String extractUsername(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
>>>>>>> origin/main
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

<<<<<<< HEAD
    // 🔹 Kiểm tra token có hợp lệ không
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
=======
    // 🔹 Kiểm tra token có hợp lệ hay không
    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
>>>>>>> origin/main
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
