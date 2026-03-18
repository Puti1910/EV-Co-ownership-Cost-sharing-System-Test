// admin-guard.js
// QUAN TRỌNG: File này phải được chèn vào <head>

(function() {
    const token = localStorage.getItem('jwtToken');
    const userRole = localStorage.getItem('userRole');

    // Kiểm tra ngay lập tức
    if (!token || userRole !== 'ROLE_ADMIN') {
        alert("Bạn không có quyền truy cập trang này. Vui lòng đăng nhập với tài khoản Admin.");
        localStorage.clear();
        window.location.href = '/auth/login';
        throw new Error("Access Denied: Not ROLE_ADMIN");
    }
})();