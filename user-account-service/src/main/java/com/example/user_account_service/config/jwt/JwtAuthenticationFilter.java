package com.example.user_account_service.config.jwt;

import com.example.user_account_service.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // SỬ DỤNG CONSTRUCTOR INJECTION
    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String jwt = null;
        String userEmail = null;

        // 1. Thử lấy JWT từ Authorization header
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        } else {
            // 2. Nếu không có trong header, thử lấy từ cookie
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    if ("jwtToken".equals(cookie.getName())) {
                        jwt = cookie.getValue();
                        break;
                    }
                }
            }
            // 3. Nếu vẫn không có, thử lấy từ query parameter (cho môi trường phát triển)
            if (jwt == null) {
                String tokenParam = request.getParameter("token");
                if (tokenParam != null && !tokenParam.isEmpty()) {
                    jwt = tokenParam; // Spring đã tự động decode URL parameter
                    logger.info("Extracted JWT from query parameter, length: {}", jwt.length());
                }
            }
        }

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            logger.info("Processing JWT token, request URI: {}", request.getRequestURI());
            userEmail = jwtService.extractEmail(jwt);
            logger.info("Extracted email from JWT: {}", userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                logger.info("Loaded user details for: {}, authorities: {}", userEmail, userDetails.getAuthorities());

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("Authentication set successfully for user: {}", userEmail);
                } else {
                    logger.warn("JWT token is invalid for user: {}", userEmail);
                }
            } else {
                if (userEmail == null) {
                    logger.warn("Could not extract email from JWT token");
                } else {
                    logger.debug("User already authenticated: {}", userEmail);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing JWT token: {}", e.getMessage(), e);
            // Không set authentication, để Spring Security xử lý như request chưa được xác thực
        }
        
        filterChain.doFilter(request, response);
    }
}