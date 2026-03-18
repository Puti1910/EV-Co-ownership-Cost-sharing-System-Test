// login.js

document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('loginForm');
    const statusMessage = document.getElementById('error-message');
    const passwordInput = document.getElementById('password');
    const API_BASE_URL = typeof window.getApiBaseUrl === 'function'
        ? window.getApiBaseUrl()
        : 'http://localhost:8084';
    const LOGIN_API_URL = `${API_BASE_URL}/api/auth/users/login`;

    // QUAN TRỌNG: Nếu bạn dùng togglePassword, hãy đảm bảo gọi hàm đó
    function togglePassword(id) {
        // Logic togglePassword (có thể nằm trong auth-utils hoặc được định nghĩa riêng)
        const input = document.getElementById(id);
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

    // Áp dụng togglePassword cho các input password (ví dụ)
    document.querySelectorAll('.toggle-password').forEach(span => {
        span.addEventListener('click', () => togglePassword(span.previousElementSibling.id));
    });


    if (loginForm) {
        loginForm.addEventListener('submit', async function(event) {
            event.preventDefault();
            statusMessage.style.display = 'none';

            const email = document.getElementById('email').value;
            const password = passwordInput.value;

            try {
                const response = await fetch(LOGIN_API_URL, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, password })
                });

                const isJson = response.headers.get('content-type')?.includes('application/json');
                const result = isJson ? await response.json() : await response.text();

                if (response.ok) {
                    if (typeof window.saveAuthSession === 'function') {
                        window.saveAuthSession(result);
                    }
                    const role = result.role;
                    const profileStatus = result.profileStatus;

                    setTimeout(() => {
                        if (role === 'ROLE_ADMIN') {
                            window.location.href = "/admin/overview";
                        } else if (role === 'ROLE_USER') {
                            // Luôn chuyển đến trang Nhóm của tôi sau khi đăng nhập
                            window.location.href = "/user/groups";
                        } else {
                            window.location.href = "/";
                        }
                    }, 100);
                } else {
                    const errorText = typeof result === 'string'
                        ? result
                        : (result.message || "Email hoặc mật khẩu không đúng.");
                    statusMessage.textContent = errorText;
                    statusMessage.style.display = 'block';
                }
            } catch (error) {
                statusMessage.textContent = 'Lỗi kết nối đến dịch vụ: ' + error.message;
                statusMessage.style.display = 'block';
            }
        });
    }
});