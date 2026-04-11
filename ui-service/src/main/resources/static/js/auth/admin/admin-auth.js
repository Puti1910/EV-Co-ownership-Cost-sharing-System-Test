// admin-auth.js
// File này sẽ được chèn vào MỌI trang Admin (ví dụ: group-management, profile-approval)

document.addEventListener('DOMContentLoaded', function() {
    const token = localStorage.getItem('jwtToken');
    const userRole = localStorage.getItem('userRole');

    // KIỂM TRA VAI TRÒ ADMIN
    if (!token || userRole !== 'ROLE_ADMIN') {

        // Nếu không phải Admin, hiển thị cảnh báo và đá về trang login
        alert("Bạn không có quyền truy cập trang này. Vui lòng đăng nhập với tài khoản Admin.");

        // Gọi logout() từ auth-utils.js (nếu đã tải)
        if (typeof logout === 'function') {
            logout();
        } else {
            // Xóa localStorage và chuyển hướng (dự phòng)
            localStorage.clear();
            window.location.href = '/login';
        }
        return;
    }

    // Nếu là Admin, tải tên người dùng lên header
    if (typeof checkAuthAndLoadUser === 'function') {
        checkAuthAndLoadUser();
    }
});