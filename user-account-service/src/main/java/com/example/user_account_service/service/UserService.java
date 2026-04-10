package com.example.user_account_service.service;

import com.example.user_account_service.dto.LoginRequest;
import com.example.user_account_service.dto.LoginResponse;
import com.example.user_account_service.dto.RegisterRequest;
import com.example.user_account_service.dto.UserProfileUpdateRequest;
import com.example.user_account_service.entity.RefreshToken;
import com.example.user_account_service.entity.User;
import com.example.user_account_service.enums.ProfileStatus;
import com.example.user_account_service.enums.Role;
import com.example.user_account_service.exception.ResourceNotFoundException;
import com.example.user_account_service.exception.TooManyRequestsException;
import com.example.user_account_service.repository.RefreshTokenRepository;
import com.example.user_account_service.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
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
    @Autowired
    private LoginAttemptService loginAttemptService;
    @Autowired
    private HttpServletRequest httpServletRequest;

    // Tìm user bằng email (hỗ trợ Controller và Security)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Đăng ký người dùng mới và trả về bộ đôi token.
     */
    public LoginResponse registerUser(RegisterRequest request) {
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Mật khẩu không được để trống!");
        }

        // Kiểm tra độ dài họ tên 2-50 ký tự
        if (request.getFullName() == null || request.getFullName().length() < 2 || request.getFullName().length() > 50) {
            throw new RuntimeException("Họ tên phải từ 2 đến 50 ký tự!");
        }

        // Kiểm tra độ dài mật khẩu 8-32 ký tự
        if (request.getPassword().length() < 8 || request.getPassword().length() > 32) {
            throw new RuntimeException("Mật khẩu phải từ 8 đến 32 ký tự!");
        }

        // Kiểm tra định dạng email (Regex đơn giản)
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (request.getEmail() == null || !request.getEmail().matches(emailRegex)) {
            throw new RuntimeException("Email không đúng định dạng!");
        }

        // Kiểm tra độ dài local part (trước @) <= 64 và domain part (sau @) <= 100
        String[] emailParts = request.getEmail().split("@");
        if (emailParts.length == 2) {
            if (emailParts[0].length() > 64) {
                throw new RuntimeException("Phần tên người dùng của email (trước @) không được vượt quá 64 ký tự!");
            }
            if (emailParts[1].length() > 100) {
                throw new RuntimeException("Phần tên miền của email (sau @) không được vượt quá 100 ký tự!");
            }
        }
        
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserService.class);

    /**
     * Đăng nhập: xác thực, phát access token + refresh token.
     */
    public LoginResponse loginUser(LoginRequest request) {
        // Lấy IP thật từ X-Forwarded-For hoặc RemoteAddr (ưu tiên IP từ Gateway)
        String clientIp = httpServletRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = httpServletRequest.getRemoteAddr();
        } else {
            // Lấy cái đầu tiên nếu có nhiều IP (do đi qua nhiều proxy)
            clientIp = clientIp.split(",")[0].trim();
        }

        // Tạo Key: IP + Email để chặn Brute-force
        String key = clientIp + ":" + (request.getEmail() != null ? request.getEmail() : "unknown");
        
        log.info(">>> LOGIN_CHECK: Key=[{}], IP=[{}]", key, clientIp);

        // PHẢI KIỂM TRA BLOCK ĐẦU TIÊN
        if (loginAttemptService.isBlocked(key)) {
            log.warn(">>> SECURITY_BLOCK: Key [{}] bị chặn do Brute-force.", key);
            throw new TooManyRequestsException("Tài khoản đã bị khóa tạm thời do nhập sai quá nhiều lần. Vui lòng thử lại sau 15 phút.");
        }

        if (request.getEmail() == null || request.getEmail().trim().isEmpty() ||
            request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Email và mật khẩu không được để trống!");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            log.info(">>> LOGIN_SUCCESS: Key=[{}]", key);
            loginAttemptService.loginSucceeded(key);
            
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng sau khi đăng nhập"));

            RefreshToken refreshToken = createRefreshToken(user);
            return buildAuthResponse(user, refreshToken);
        } catch (Exception e) {
            log.error("Đăng nhập THẤT BẠI cho Key: [{}]. Lỗi: {}", key, e.getMessage());
            loginAttemptService.loginFailed(key);
            throw e;
        }
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
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("ID người dùng không hợp lệ (ID phải lớn hơn 0)");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // 1. Kiểm tra tuổi (18-100)
        if (request.getDateOfBirth() != null) {
            Period ageCount = Period.between(request.getDateOfBirth(), LocalDate.now());
            if (ageCount.getYears() < 18 || ageCount.getYears() > 100) {
                throw new RuntimeException("Tuổi phải từ 18 đến 100 tuổi.");
            }
        }

        // 2. Kiểm tra logid ngày GPLX (Ngày hết hạn phải sau ngày cấp)
        if (request.getLicenseIssueDate() != null && request.getLicenseExpiryDate() != null) {
            if (request.getLicenseExpiryDate().isBefore(request.getLicenseIssueDate()) || 
                request.getLicenseExpiryDate().isEqual(request.getLicenseIssueDate())) {
                throw new RuntimeException("Ngày hết hạn GPLX phải sau ngày cấp.");
            }
        }

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
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("ID người dùng không hợp lệ (ID phải lớn hơn 0)");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

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
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("ID người dùng không hợp lệ (ID phải lớn hơn 0)");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // Nếu hồ sơ đã được duyệt rồi, không cần làm gì thêm (Idempotent)
        if (user.getProfileStatus() == ProfileStatus.APPROVED) {
            return user;
        }

        user.setProfileStatus(ProfileStatus.APPROVED);
        user.setVerified(true);
        return userRepository.save(user);
    }

    public User rejectProfile(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("ID người dùng không hợp lệ (ID phải lớn hơn 0)");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // Chặn logic: Không thể từ chối hồ sơ đã được duyệt (APPROVED) hoặc hồ sơ đang định chỉ (SUSPENDED)
        if (user.getProfileStatus() == ProfileStatus.APPROVED) {
            throw new RuntimeException("Chặn logic: Không thể từ chối hồ sơ đã ở trạng thái APPROVED.");
        }
        if (user.getProfileStatus() == ProfileStatus.SUSPENDED) {
            throw new RuntimeException("Chặn logic: Không thể từ chối hồ sơ đang bị đình chỉ (SUSPENDED).");
        }

        user.setProfileStatus(ProfileStatus.REJECTED);
        user.setVerified(false);
        return userRepository.save(user);
    }

    public User updateUserRole(Long userId, Role role) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("ID người dùng không hợp lệ (ID phải lớn hơn 0)");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));
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