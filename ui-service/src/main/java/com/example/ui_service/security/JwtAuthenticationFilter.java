package com.example.ui_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String token = resolveToken(request);

        // Debug logging for API requests
        if (requestUri.startsWith("/api/")) {
            System.out.println("=== JWT Filter Debug ===");
            System.out.println("Request URI: " + requestUri);
            System.out.println("Token found: " + (token != null && !token.isEmpty()));
            if (token != null && !token.isEmpty()) {
                System.out.println("Token length: " + token.length());
                System.out.println("Token valid: " + jwtService.isTokenValid(token));
            }
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                System.out.println("Cookies count: " + cookies.length);
                for (Cookie cookie : cookies) {
                    System.out.println("Cookie: " + cookie.getName() + " = " + 
                        (cookie.getName().equals("jwtToken") ? cookie.getValue().substring(0, Math.min(20, cookie.getValue().length())) + "..." : "***"));
                }
            } else {
                System.out.println("No cookies found");
            }
        }

        // Always try to authenticate if token is present, even if context already has authentication
        // This ensures token is validated on every request
        if (StringUtils.hasText(token)) {
            if (jwtService.isTokenValid(token)) {
                String email = jwtService.extractEmail(token).orElse(null);
                String role = jwtService.extractRole(token).orElse(null);
                Long userId = jwtService.extractUserId(token).orElse(null);

                if (email != null && role != null && userId != null) {
                    // Normalize role: convert to uppercase and ensure ROLE_ prefix
                    String normalizedRole = role.toUpperCase().trim();
                    if (!normalizedRole.startsWith("ROLE_")) {
                        normalizedRole = "ROLE_" + normalizedRole;
                    }
                    String authority = normalizedRole;
                    
                    if (requestUri.startsWith("/api/")) {
                        System.out.println("Original role from token: " + role);
                        System.out.println("Normalized role: " + normalizedRole);
                        System.out.println("Authority: " + authority);
                    }
                    
                    AuthenticatedUser principal = new AuthenticatedUser(
                            userId,
                            email,
                            role,
                            List.of(new SimpleGrantedAuthority(authority))
                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    principal.getAuthorities()
                            );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Set authentication in SecurityContext
                    var securityContext = SecurityContextHolder.getContext();
                    securityContext.setAuthentication(authentication);
                    
                    // Verify it was set correctly
                    var setAuth = securityContext.getAuthentication();
                    if (requestUri.startsWith("/api/")) {
                        System.out.println("Authentication successful for: " + email + " (role: " + role + ", userId: " + userId + ")");
                        System.out.println("Authorities: " + authentication.getAuthorities());
                        System.out.println("Authentication set in context: " + (setAuth != null));
                        System.out.println("Set authentication name: " + (setAuth != null ? setAuth.getName() : "null"));
                        System.out.println("Set authentication isAuthenticated: " + (setAuth != null && setAuth.isAuthenticated()));
                    }
                } else {
                    if (requestUri.startsWith("/api/")) {
                        System.out.println("Authentication failed: missing email, role, or userId");
                        System.out.println("  email: " + email + ", role: " + role + ", userId: " + userId);
                    }
                    // Clear authentication if token is invalid
                    SecurityContextHolder.clearContext();
                }
            } else {
                if (requestUri.startsWith("/api/")) {
                    System.out.println("Authentication failed: invalid token");
                }
                // Clear authentication if token is invalid
                SecurityContextHolder.clearContext();
            }
        } else {
            if (requestUri.startsWith("/api/")) {
                System.out.println("Authentication failed: no token found");
                // Check if there's existing authentication
                var existingAuth = SecurityContextHolder.getContext().getAuthentication();
                if (existingAuth != null) {
                    System.out.println("Existing authentication found: " + existingAuth.getName());
                } else {
                    System.out.println("No existing authentication in context");
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // First check Authorization header
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (request.getRequestURI().startsWith("/api/")) {
                System.out.println("Token found in Authorization header");
            }
            return token;
        }

        // Then check cookies
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwtToken".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    if (request.getRequestURI().startsWith("/api/")) {
                        System.out.println("Token found in cookie: jwtToken");
                    }
                    return cookie.getValue();
                }
            }
        }

        if (request.getRequestURI().startsWith("/api/")) {
            System.out.println("No token found in Authorization header or cookies");
        }
        return null;
    }
}

