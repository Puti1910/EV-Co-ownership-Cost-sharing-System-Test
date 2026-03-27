// user-guard.js
// QUAN TRỌNG: File này phải được chèn vào <head>

(function() {
    const token = localStorage.getItem('jwtToken');
    const userRole = localStorage.getItem('userRole');
    const allowedRoles = ['ROLE_USER', 'ROLE_ADMIN'];

    if (!token || !allowedRoles.includes(userRole)) {
        alert("Bạn không có quyền truy cập trang này. Vui lòng đăng nhập với tài khoản hợp lệ.");
        localStorage.clear();
        window.location.href = '/auth/login';
        throw new Error("Access Denied: insufficient role");
    }
})();