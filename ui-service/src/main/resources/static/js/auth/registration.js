// registration.js

// Hàm chuyển đổi ẩn/hiện mật khẩu (Đã cập nhật để phù hợp với HTML mới)
function togglePassword(id) {
    const input = document.getElementById(id);
    // Lấy icon (i tag) nằm trong span.toggle-password
    const icon = input.nextElementSibling.querySelector('i');

    if (input.type === "password") {
        input.type = "text";
        icon.classList.remove("fa-eye");
        icon.classList.add("fa-eye-slash");
    } else {
        input.type = "password";
        icon.classList.remove("fa-eye-slash");
        icon.classList.add("fa-eye");
    }
}

// Bắt sự kiện submit của form Đăng Ký
document.addEventListener("DOMContentLoaded", function() {
    const registerForm = document.getElementById("registerForm");
    if (!registerForm) return;

    const API_BASE_URL = typeof window.getApiBaseUrl === 'function'
        ? window.getApiBaseUrl()
        : 'http://localhost:8084';
    const REGISTER_API_URL = `${API_BASE_URL}/api/auth/users/register`;

    registerForm.addEventListener("submit", async function(event) {
        event.preventDefault(); // Ngăn form submit theo cách truyền thống

        const fullName = document.getElementById("fullName").value;
        const email = document.getElementById("email").value;
        const password = document.getElementById("password").value;
        const confirmPassword = document.getElementById("confirmPassword").value;
        const errorMessageDiv = document.getElementById("error-message");

        errorMessageDiv.style.display = "none"; // Ẩn thông báo lỗi cũ

        // 1. Kiểm tra mật khẩu trùng khớp (phía client)
        if (password !== confirmPassword) {
            errorMessageDiv.textContent = "Lỗi: Mật khẩu xác nhận không trùng khớp!";
            errorMessageDiv.style.display = "block";
            return;
        }

        // 2. Chuẩn bị dữ liệu gửi đi
        const data = {
            fullName: fullName,
            email: email,
            password: password
        };

        // 3. Gọi API backend (user-account-service)
        try {
            const response = await fetch(REGISTER_API_URL, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(data)
            });

            if (response.ok) {
                alert("Đăng ký tài khoản thành công! Vui lòng đăng nhập và hoàn tất hồ sơ KYC để tiếp tục.");
                window.location.href = "/auth/login";
            } else {
                const isJson = response.headers.get('content-type')?.includes('application/json');
                const errorText = isJson ? (await response.json()).message : await response.text();
                errorMessageDiv.textContent = errorText || "Đăng ký thất bại. Vui lòng thử lại.";
                errorMessageDiv.style.display = "block";
            }
        } catch (error) {
            // Lỗi mạng hoặc server không chạy
            errorMessageDiv.textContent = "Không thể kết nối đến máy chủ. Vui lòng thử lại sau.";
            errorMessageDiv.style.display = "block";
        }
    });
});