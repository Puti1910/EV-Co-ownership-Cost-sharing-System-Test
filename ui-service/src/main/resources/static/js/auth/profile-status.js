// profile-status.js
// (File user-guard.js đã được chèn vào <head> để bảo vệ)

document.addEventListener('DOMContentLoaded', function() {
    const API_BASE_URL = typeof window.getApiBaseUrl === 'function'
        ? window.getApiBaseUrl()
        : 'http://localhost:8084';
    const PROFILE_API_URL = `${API_BASE_URL}/api/auth/users/profile`;

    // --- (Lệnh gọi checkAuthAndLoadUser() đã được XÓA khỏi đây) ---
    // (File auth-utils.js sẽ tự động chạy)

    // Hàm hiển thị ảnh (nếu là URL) hoặc văn bản (nếu chưa có)
    function displayImage(elementId, url) {
        const element = document.getElementById(elementId);
        if (!element) {
            console.warn(`Element với id "${elementId}" không tồn tại`);
            return;
        }

        // Nếu không có URL hợp lệ -> hiển thị "Chưa tải lên"
        if (!url || url.trim() === '' || url === 'null' || url === 'undefined') {
            console.log(`Không có URL cho ${elementId}, URL nhận được:`, url);
            element.textContent = "Chưa tải lên";
            element.classList.remove('has-image');
            return;
        }

        // Tạo URL đầy đủ nếu là relative path
        let imageUrl = url.trim();

        if (imageUrl.startsWith('/uploads/') || (imageUrl.startsWith('/') && !imageUrl.startsWith('http'))) {
            if (imageUrl.startsWith('/')) {
                imageUrl = imageUrl.substring(1);
            }
            imageUrl = `${API_BASE_URL}/${imageUrl}`;
        } else if (!imageUrl.startsWith('http://') && !imageUrl.startsWith('https://')) {
            imageUrl = `${API_BASE_URL}/uploads/${imageUrl}`;
        }

        console.log(`Hiển thị ảnh cho ${elementId}:`, imageUrl);

        // Xóa nội dung cũ và tạo thẻ img an toàn (không dùng inline onerror)
        element.innerHTML = '';

        const img = document.createElement('img');
        img.src = imageUrl;
        img.alt = 'Giấy tờ đã tải lên';
        img.style.maxWidth = '100%';
        img.style.maxHeight = '200px';
        img.style.objectFit = 'contain';
        img.style.borderRadius = '6px';

        img.onerror = function () {
            // Bảo vệ: kiểm tra parentNode tồn tại trước khi thao tác
            const parent = img.parentElement;
            if (parent) {
                parent.textContent = 'Lỗi tải ảnh';
                parent.classList.remove('has-image');
            }
        };

        element.appendChild(img);
        element.classList.add('has-image');
    }

    // Tải dữ liệu hồ sơ
    async function loadProfileStatus() {
        try {
            const response = await authenticatedFetch(PROFILE_API_URL, {
                method: 'GET'
            });

            if (!response.ok) {
                // Đọc response body một lần duy nhất
                const responseText = await response.text();
                let errorData = {};
                
                try {
                    errorData = JSON.parse(responseText);
                } catch (e) {
                    // Nếu không parse được JSON, dùng text thuần
                    errorData = { message: responseText || 'Lỗi không xác định' };
                }

                console.error('Lỗi tải hồ sơ:', response.status, errorData);

                if (response.status === 401) {
                    // Lỗi xác thực - yêu cầu đăng nhập lại
                    if (typeof logout === 'function') {
                        alert(errorData.message || 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
                        setTimeout(() => {
                            logout({ skipRemote: true });
                        }, 1000);
                    } else {
                        window.location.href = '/auth/login';
                    }
                    return;
                } else if (response.status === 400) {
                    // Lỗi yêu cầu không hợp lệ
                    alert(errorData.message || 'Yêu cầu không hợp lệ. Vui lòng thử lại.');
                    return;
                } else {
                    throw new Error(errorData.message || 'Không thể tải dữ liệu hồ sơ.');
                }
            }

            // Đọc response body một lần duy nhất
            const responseText = await response.text();
            let user = {};
            
            try {
                user = JSON.parse(responseText);
            } catch (e) {
                console.error('Lỗi parse JSON từ profile response:', e);
                throw new Error('Phản hồi từ server không hợp lệ');
            }

            console.log('Dữ liệu user nhận được:', user);
            console.log('URL ảnh:', {
                idCardFrontUrl: user.idCardFrontUrl,
                idCardBackUrl: user.idCardBackUrl,
                licenseImageUrl: user.licenseImageUrl,
                portraitImageUrl: user.portraitImageUrl
            });

            if (typeof window.updateStoredProfileStatus === 'function') {
                window.updateStoredProfileStatus(user.profileStatus || 'PENDING');
            }

            // Cập nhật thẻ Trạng thái (Status Tag)
            const statusTag = document.getElementById('status-tag');
            if (user.profileStatus) {
                statusTag.classList.remove('pending', 'approved', 'rejected');

                if (user.profileStatus === 'APPROVED') {
                    statusTag.textContent = 'Đã duyệt';
                    statusTag.classList.add('approved');
                } else if (user.profileStatus === 'REJECTED') {
                    statusTag.textContent = 'Bị từ chối';
                    statusTag.classList.add('rejected');
                } else { // PENDING
                    statusTag.textContent = 'Đang chờ duyệt';
                    statusTag.classList.add('pending');
                }
            }

            // Điền dữ liệu vào các thẻ span (read-only)
            document.getElementById('fullName-view').textContent = user.fullName || 'N/A';
            document.getElementById('phoneNumber-view').textContent = user.phoneNumber || 'N/A';
            document.getElementById('email-view').textContent = user.email || 'N/A';
            document.getElementById('dateOfBirth-view').textContent = user.dateOfBirth || 'N/A';
            document.getElementById('idCardNumber-view').textContent = user.idCardNumber || 'N/A';
            document.getElementById('idCardIssueDate-view').textContent = user.idCardIssueDate || 'N/A';
            document.getElementById('idCardIssuePlace-view').textContent = user.idCardIssuePlace || 'N/A';
            document.getElementById('licenseNumber-view').textContent = user.licenseNumber || 'N/A';
            document.getElementById('licenseClass-view').textContent = user.licenseClass || 'N/A';
            document.getElementById('licenseIssueDate-view').textContent = user.licenseIssueDate || 'N/A';
            document.getElementById('licenseExpiryDate-view').textContent = user.licenseExpiryDate || 'N/A';

            // Hiển thị ảnh (nếu có URL)
            displayImage('idCardFrontUrl-view', user.idCardFrontUrl);
            displayImage('idCardBackUrl-view', user.idCardBackUrl);
            displayImage('licenseImageUrl-view', user.licenseImageUrl);
            displayImage('portraitImageUrl-view', user.portraitImageUrl);

        } catch (error) {
            console.error('Lỗi tải trang Tình trạng Hồ sơ:', error);
        }
    }

    // Tải dữ liệu chính
    loadProfileStatus();
});