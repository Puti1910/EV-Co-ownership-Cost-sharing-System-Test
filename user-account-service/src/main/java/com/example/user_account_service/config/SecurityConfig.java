package com.example.user_account_service.config;

import com.example.user_account_service.config.jwt.JwtAuthenticationFilter;
import com.example.user_account_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority; // <-- THÊM IMPORT NÀY
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List; // <-- THÊM IMPORT NÀY

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserRepository userRepository;

    // (Bean PasswordEncoder, AuthenticationProvider, AuthenticationManager giữ nguyên)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * CẬP NHẬT: "Dạy" Spring Security cách tìm user VÀ VAI TRÒ (ROLE)
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmail(email)
                .map(user -> new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPasswordHash(),
                        // Thêm vai trò (role) của user vào (Rất quan trọng)
                        List.of(new SimpleGrantedAuthority(user.getRole().name()))
                ))
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với email: " + email));
    }

    /**
     * CẬP NHẬT: Chuỗi lọc bảo mật (Thêm bảo vệ cho API Admin)
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration conf = new CorsConfiguration();
                    conf.setAllowedOrigins(List.of("http://localhost:8080"));
                    conf.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    conf.setAllowedHeaders(List.of("*"));
                    conf.setAllowCredentials(true);
                    return conf;
                }))
                .authorizeHttpRequests(auth -> auth
                        // API Public
                        .requestMatchers("/api/auth/users/register", "/api/auth/users/login", "/api/auth/users/refresh", "/api/auth/users/logout").permitAll()

                        // Endpoint trung gian để nhận JWT và set cookie (public)
                        .requestMatchers("/user/auth").permitAll()

                        // Trang UI User (Yêu cầu đăng nhập, cả USER và ADMIN đều có thể truy cập)
                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")

                        // API User (Yêu cầu đăng nhập, cả USER và ADMIN đều có thể gọi)
                        .requestMatchers("/api/auth/users/profile", "/api/auth/users/profile/**").hasAnyRole("USER", "ADMIN")

                        // API ADMIN (CHỈ ROLE_ADMIN MỚI ĐƯỢC VÀO)
                        .requestMatchers("/api/auth/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated() // Tất cả các API khác đều yêu cầu đăng nhập
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}