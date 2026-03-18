package com.example.ui_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret.key}")
    private String secretKey;

    public Optional<String> extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Optional<String> extractRole(String token) {
        return extractClaim(token, claims -> {
            Object role = claims.get("role");
            return role != null ? role.toString() : null;
        });
    }

    public Optional<Long> extractUserId(String token) {
        return extractClaim(token, claims -> {
            Object userId = claims.get("userId");
            if (userId instanceof Number number) {
                return number.longValue();
            }
            if (userId instanceof String stringValue) {
                try {
                    return Long.parseLong(stringValue);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        });
    }

    public boolean isTokenValid(String token) {
        return extractEmail(token).isPresent() && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Optional<Date> expiration = extractClaim(token, Claims::getExpiration);
        return expiration.map(date -> date.before(new Date())).orElse(true);
    }

    private <T> Optional<T> extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = extractAllClaims(token);
            return Optional.ofNullable(claimsResolver.apply(claims));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

