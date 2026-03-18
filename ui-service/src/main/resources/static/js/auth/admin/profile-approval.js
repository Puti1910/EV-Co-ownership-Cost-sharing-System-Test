// profile-approval.js
// (File admin-guard.js đã được chèn vào <head> để bảo vệ)

document.addEventListener('DOMContentLoaded', function() {
    const userListBody = document.getElementById('user-list-body');
    if (!userListBody) {
        return;
    }
    const searchInput = document.getElementById('search-input');
    const refreshButton = document.getElementById('btn-refresh');
    const modal = document.getElementById('profileModal');
    const modalCloseButton = document.getElementById('modalCloseButton');
    const modalApproveBtn = document.getElementById('modalApproveBtn');
    const modalRejectBtn = document.getElementById('modalRejectBtn');
    const modalUserName = document.getElementById('modalUserName');
    const refreshButton2 = document.getElementById('btn-refresh-2');

    const statsElements = {
        pending: document.getElementById('pending-count'),
        approved: document.getElementById('approved-count'),
        rejected: document.getElementById('rejected-count'),
        missingDocs: document.getElementById('missing-docs-count')
    };

    const API_BASE_URL = (typeof window.getApiBaseUrl === 'function'
        ? window.getApiBaseUrl()
        : 'http://localhost:8084') + '/api/auth/admin';

    let pendingUsersList = [];
    let activeUserId = null;

    // --- (Lệnh gọi checkAuthAndLoadUser() đã được XÓA khỏi đây) ---
    // (File auth-utils.js sẽ tự động chạy)

    // 2. Hàm tải danh sách User đang chờ
    async function loadPendingUsers() {
        try {
            const response = await authenticatedFetch(`${API_BASE_URL}/pending-users`, {
                method: 'GET'
            });

            if (!response.ok) {
                if (response.status === 401 || response.status === 403) {
                    throw new Error('Bạn không có quyền tải danh sách này.');
                }
                throw new Error('Không thể tải danh sách.');
            }

            const users = await response.json();
            pendingUsersList = users;
            renderTable(pendingUsersList);
            updateStats(pendingUsersList);
            updateStatusSnapshots();

        } catch (error) {
            console.error('Lỗi tải danh sách chờ:', error);
            userListBody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: red;">${error.message}</td></tr>`;
        }
    }

    // 3. Hàm hiển thị dữ liệu ra bảng
    function renderTable(users) {
        userListBody.innerHTML = '';

        if (users.length === 0) {
            userListBody.innerHTML = `<tr><td colspan="6" style="text-align: center; padding: 32px;">
                <i class="fas fa-inbox me-2 text-muted"></i> Không có hồ sơ nào đang chờ duyệt.
            </td></tr>`;
            return;
        }

        users.forEach(user => {
            const row = document.createElement('tr');
            row.setAttribute('data-user-id', user.userId);
            const updatedAt = user.updatedAt ? new Date(user.updatedAt).toLocaleDateString() : '—';

            row.innerHTML = `
                <td>${user.fullName || 'N/A'}</td>
                <td>${user.email}</td>
                <td>${user.phoneNumber || 'N/A'}</td>
                <td>${updatedAt}</td>
                <td>
                    <span class="status-pill pending">
                        <i class="fas fa-clock"></i> ${user.profileStatus || 'PENDING'}
                    </span>
                </td>
                <td class="action-buttons">
                    <button class="btn-view" data-action="view" title="Xem chi tiết"><i class="fas fa-eye"></i></button>
                    <button class="btn-approve" data-action="approve" title="Duyệt"><i class="fas fa-check"></i></button>
                    <button class="btn-reject" data-action="reject" title="Từ chối"><i class="fas fa-times"></i></button>
                </td>
            `;
            userListBody.appendChild(row);
        });
    }

    // 4. Hàm xử lý hành động (Duyệt/Từ chối)
    async function handleUserAction(userId, action) {
        const url = `${API_BASE_URL}/${action}/${userId}`;

        try {
            const response = await authenticatedFetch(url, {
                method: 'PUT',
            });

            if (!response.ok) {
                throw new Error('Hành động thất bại.');
            }

            loadPendingUsers(); // Tải lại danh sách

        } catch (error) {
            console.error(`Lỗi khi ${action} user ${userId}:`, error);
            alert(`Đã xảy ra lỗi: ${error.message}`);
        }
    }

    // 5. Hàm Mở Modal và điền dữ liệu
    function openModal(userId) {
        const user = pendingUsersList.find(u => u.userId == userId);
        if (!user) return;
        activeUserId = user.userId;

        const modalBody = document.getElementById('modalBodyContent');
        if (modalUserName) {
            modalUserName.textContent = user.fullName || user.email || 'Người dùng';
        }

        modalBody.innerHTML = `
            <div class="detail-group">
                <h3><i class="fas fa-user"></i> Thông tin cá nhân</h3>
                <div class="detail-grid">
                    <div class="detail-item"><p>Họ và Tên:</p><span>${user.fullName || 'N/A'}</span></div>
                    <div class="detail-item"><p>Email:</p><span>${user.email || 'N/A'}</span></div>
                    <div class="detail-item"><p>Số điện thoại:</p><span>${user.phoneNumber || 'N/A'}</span></div>
                    <div class="detail-item"><p>Ngày sinh:</p><span>${user.dateOfBirth || 'N/A'}</span></div>
                </div>
            </div>
            <div class="detail-group">
                <h3><i class="fas fa-id-card"></i> Giấy tờ tùy thân</h3>
                <div class="detail-grid">
                    <div class="detail-item"><p>Số CMND/CCCD:</p><span>${user.idCardNumber || 'N/A'}</span></div>
                    <div class="detail-item"><p>Ngày cấp:</p><span>${user.idCardIssueDate || 'N/A'}</span></div>
                    <div class="detail-item"><p>Nơi cấp:</p><span>${user.idCardIssuePlace || 'N/A'}</span></div>
                </div>
            </div>
            <div class="detail-group">
                <h3><i class="fas fa-car-side"></i> Giấy phép lái xe</h3>
                <div class="detail-grid">
                    <div class="detail-item"><p>Số GPLX:</p><span>${user.licenseNumber || 'N/A'}</span></div>
                    <div class="detail-item"><p>Hạng:</p><span>${user.licenseClass || 'N/A'}</span></div>
                    <div class="detail-item"><p>Ngày cấp:</p><span>${user.licenseIssueDate || 'N/A'}</span></div>
                    <div class="detail-item"><p>Ngày hết hạn:</p><span>${user.licenseExpiryDate || 'N/A'}</span></div>
                </div>
            </div>
            <div class="detail-group">
                <h3><i class="fas fa-upload"></i> Hình ảnh (Click để xem)</h3>
                <div class="image-grid">
                    <div class="image-item">
                        <label>CCCD Mặt trước:</label>
                        ${user.idCardFrontUrl ? `<a href="${user.idCardFrontUrl}" target="_blank"><img src="${user.idCardFrontUrl}"></a>` : '<span>Chưa có</span>'}
                    </div>
                    <div class="image-item">
                        <label>CCCD Mặt sau:</label>
                        ${user.idCardBackUrl ? `<a href="${user.idCardBackUrl}" target="_blank"><img src="${user.idCardBackUrl}"></a>` : '<span>Chưa có</span>'}
                    </div>
                    <div class="image-item">
                        <label>Ảnh GPLX:</label>
                        ${user.licenseImageUrl ? `<a href="${user.licenseImageUrl}" target="_blank"><img src="${user.licenseImageUrl}"></a>` : '<span>Chưa có</span>'}
                    </div>
                    <div class="image-item">
                        <label>Ảnh Chân dung:</label>
                        ${user.portraitImageUrl ? `<a href="${user.portraitImageUrl}" target="_blank"><img src="${user.portraitImageUrl}"></a>` : '<span>Chưa có</span>'}
                    </div>
                </div>
            </div>
        `;

        if (modal) {
            modal.style.display = 'flex';
        }
    }

    // 6. Hàm Đóng Modal
    function closeModal() {
        if (modal) {
            modal.style.display = 'none';
        }
        activeUserId = null;
    }

    // 7. Thêm Event Listener cho các nút (Duyệt/Từ chối/Xem)
    userListBody.addEventListener('click', function(event) {
        const button = event.target.closest('button');
        if (!button) return;
        const action = button.dataset.action;
        const userId = button.closest('tr').dataset.userId;

        if (action === 'view') openModal(userId);
        else if (action === 'approve' || action === 'reject') {
            if (confirm(`Bạn có chắc muốn "${action === 'approve' ? 'Duyệt' : 'Từ chối'}" hồ sơ này?`)) {
                handleUserAction(userId, action);
            }
        }
    });

    // 8. Thêm Event Listener để Đóng Modal
    modalCloseButton?.addEventListener('click', closeModal);
    modal?.addEventListener('click', function(event) {
        if (event.target === modal) {
            closeModal();
        }
    });

    async function performModalAction(action) {
        if (!activeUserId) return;
        await handleUserAction(activeUserId, action);
        closeModal();
    }

    modalApproveBtn?.addEventListener('click', () => performModalAction('approve'));
    modalRejectBtn?.addEventListener('click', () => performModalAction('reject'));

    function updateStats(users) {
        const pending = users.length;
        const missingDocs = users.filter(user =>
            !user.idCardFrontUrl || !user.idCardBackUrl || !user.licenseImageUrl || !user.portraitImageUrl
        ).length;

        if (statsElements.pending) statsElements.pending.textContent = pending;
        if (statsElements.missingDocs) statsElements.missingDocs.textContent = missingDocs;
    }

    async function updateStatusSnapshots() {
        try {
            const [approvedResp, rejectedResp] = await Promise.all([
                authenticatedFetch(`${API_BASE_URL}/users?status=APPROVED`),
                authenticatedFetch(`${API_BASE_URL}/users?status=REJECTED`)
            ]);

            if (approvedResp.ok) {
                const approved = await approvedResp.json();
                if (statsElements.approved) statsElements.approved.textContent = approved.length;
            }

            if (rejectedResp.ok) {
                const rejected = await rejectedResp.json();
                if (statsElements.rejected) statsElements.rejected.textContent = rejected.length;
            }
        } catch (error) {
            console.warn('Không thể cập nhật thống kê:', error);
        }
    }

    function handleSearch(event) {
        const keyword = event.target.value.trim().toLowerCase();
        if (!keyword) {
            renderTable(pendingUsersList);
            return;
        }
        const filtered = pendingUsersList.filter(user => {
            const candidate = `${user.fullName || ''} ${user.email || ''} ${user.phoneNumber || ''}`.toLowerCase();
            return candidate.includes(keyword);
        });
        renderTable(filtered);
    }

    searchInput?.addEventListener('input', handleSearch);
    refreshButton?.addEventListener('click', () => loadPendingUsers());
    refreshButton2?.addEventListener('click', () => loadPendingUsers());

    loadPendingUsers();
});