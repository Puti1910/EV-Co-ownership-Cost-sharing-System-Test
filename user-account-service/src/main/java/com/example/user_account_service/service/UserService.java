package com.example.user_account_service.service;

import com.example.user_account_service.dto.LoginRequest;
import com.example.user_account_service.dto.LoginResponse;
import com.example.user_account_service.dto.RegisterRequest;
import com.example.user_account_service.dto.UserProfileUpdateRequest;
import com.example.user_account_service.entity.RefreshToken;
import com.example.user_account_service.entity.User;
import com.example.user_account_service.enums.ProfileStatus;
import com.example.user_account_service.enums.Role;
import com.example.user_account_service.repository.RefreshTokenRepository;
import com.example.user_account_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private static final long REFRESH_TOKEN_TTL_MILLIS = 1000L * 60 * 60 * 24 * 7; // 7 ngày

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtService jwtService;

    // Tìm user bằng email (hỗ trợ Controller và Security)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Đăng ký người dùng mới và trả về bộ đôi token.
     */
    public LoginResponse registerUser(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống!");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .profileStatus(ProfileStatus.PENDING)
                .isVerified(false)
                .build();

        User saved = userRepository.save(user);
        return buildAuthResponse(saved, createRefreshToken(saved));
    }

    /**
     * Đăng nhập: xác thực, phát access token + refresh token.
     */
    public LoginResponse loginUser(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng sau khi đăng nhập"));

        RefreshToken refreshToken = createRefreshToken(user);
        return buildAuthResponse(user, refreshToken);
    }

    /**
     * Refresh access token dựa trên refresh token hợp lệ (rotation).
     */
    public LoginResponse refreshSession(String refreshTokenValue) {
        RefreshToken storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ hoặc đã bị thu hồi"));

        if (storedToken.isExpired()) {
            storedToken.revoke();
            refreshTokenRepository.save(storedToken);
            throw new RuntimeException("Refresh token đã hết hạn, vui lòng đăng nhập lại");
        }

        // Rotate token: thu hồi token cũ, phát token mới
        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        // Load lại user từ database để đảm bảo có profileStatus mới nhất
        // (không dùng storedToken.getUser() vì có thể bị cache)
        Long userId = storedToken.getUser().getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        RefreshToken newToken = createRefreshToken(user);
        return buildAuthResponse(user, newToken);
    }

    /**
     * Thu hồi refresh token (logout).
     */
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue).ifPresent(token -> {
            token.revoke();
            refreshTokenRepository.save(token);
        });
    }

    /**
     * Logic Cập nhật hồ sơ (Onboarding) + reset trạng thái.
     */
    public User updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng."));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());

        if (request.getIdCardNumber() != null) user.setIdCardNumber(request.getIdCardNumber());
        if (request.getIdCardIssueDate() != null) user.setIdCardIssueDate(request.getIdCardIssueDate());
        if (request.getIdCardIssuePlace() != null) user.setIdCardIssuePlace(request.getIdCardIssuePlace());

        if (request.getLicenseNumber() != null) user.setLicenseNumber(request.getLicenseNumber());
        if (request.getLicenseClass() != null) user.setLicenseClass(request.getLicenseClass());
        if (request.getLicenseIssueDate() != null) user.setLicenseIssueDate(request.getLicenseIssueDate());
        if (request.getLicenseExpiryDate() != null) user.setLicenseExpiryDate(request.getLicenseExpiryDate());

        if (request.getIdCardFrontUrl() != null) user.setIdCardFrontUrl(request.getIdCardFrontUrl());
        if (request.getIdCardBackUrl() != null) user.setIdCardBackUrl(request.getIdCardBackUrl());
        if (request.getLicenseImageUrl() != null) user.setLicenseImageUrl(request.getLicenseImageUrl());
        if (request.getPortraitImageUrl() != null) user.setPortraitImageUrl(request.getPortraitImageUrl());

        user.setProfileStatus(ProfileStatus.PENDING);
        user.setVerified(false);

        return userRepository.save(user);
    }

    /**
     * Cập nhật URL hồ sơ KYC (khi user upload ảnh).
     */
    public User updateKycDocuments(Long userId, Map<String, String> documentUrls) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng."));

        documentUrls.forEach((key, value) -> {
            switch (key) {
                case "idCardFrontUrl" -> user.setIdCardFrontUrl(value);
                case "idCardBackUrl" -> user.setIdCardBackUrl(value);
                case "licenseImageUrl" -> user.setLicenseImageUrl(value);
                case "portraitImageUrl" -> user.setPortraitImageUrl(value);
                default -> { }
            }
        });

        user.setProfileStatus(ProfileStatus.PENDING);
        user.setVerified(false);

        return userRepository.save(user);
    }

    // --- CÁC HÀM DÀNH CHO ADMIN ---

    public List<User> getProfilesByStatus(ProfileStatus status) {
        return userRepository.findByProfileStatus(status);
    }

    public User approveProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng."));

        user.setProfileStatus(ProfileStatus.APPROVED);
        user.setVerified(true);
        return userRepository.save(user);
    }

    public User rejectProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng."));

        user.setProfileStatus(ProfileStatus.REJECTED);
        user.setVerified(false);
        return userRepository.save(user);
    }

    public User updateUserRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng."));
        user.setRole(role);
        return userRepository.save(user);
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setIssuedAt(Instant.now());
        refreshToken.setExpiresAt(Instant.now().plusMillis(REFRESH_TOKEN_TTL_MILLIS));
        refreshToken.setRevoked(false);
        return refreshTokenRepository.save(refreshToken);
    }

    private LoginResponse buildAuthResponse(User user, RefreshToken refreshToken) {
        String accessToken = jwtService.generateToken(user);
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .profileStatus(user.getProfileStatus().name())
                .build();
    }
}