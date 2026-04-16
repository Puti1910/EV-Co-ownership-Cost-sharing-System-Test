(function () {
    const API_BASE = typeof window.getApiBaseUrl === 'function'
        ? window.getApiBaseUrl()
        : 'http://localhost:8084';

    let currentVehicleId = null;
    let currentRange = 30;
    let currentUserId = null;
    let currentSummary = null;
    let userCalendar;

    // Helper function để decode JWT token và kiểm tra profileStatus
    function decodeJWT(token) {
        try {
            const base64Url = token.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(atob(base64).split('').map(function (c) {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join(''));
            return JSON.parse(jsonPayload);
        } catch (e) {
            console.error('Error decoding JWT:', e);
            return null;
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        currentUserId = localStorage.getItem('userId');
        if (!currentUserId) {
            alert('Vui lòng đăng nhập lại để tiếp tục.');
            return;
        }

        // Debug: Kiểm tra profileStatus trong token
        const token = localStorage.getItem('jwtToken');
        if (token) {
            const decoded = decodeJWT(token);
            if (decoded) {
                console.log('🔍 Current JWT Token Info:');
                console.log('  - User ID:', decoded.userId);
                console.log('  - Email:', decoded.sub);
                console.log('  - Role:', decoded.role);
                console.log('  - ProfileStatus:', decoded.profileStatus);
                if (decoded.profileStatus !== 'APPROVED') {
                    console.warn('⚠️ Token có profileStatus =', decoded.profileStatus, '- Cần đăng nhập lại để có token mới!');
                }
            }
        }

        bindUserControls();
        loadUserVehicles().then(() => {
            loadUserFairness();
        });
    });

    function bindUserControls() {
        document.getElementById('userVehicleSelect').addEventListener('change', e => {
            currentVehicleId = e.target.value ? Number(e.target.value) : null;
            loadUserFairness();
        });
        document.getElementById('userRangeSelect').addEventListener('change', e => {
            currentRange = Number(e.target.value || 30);
            loadUserFairness();
        });
        document.getElementById('userRefreshBtn').addEventListener('click', () => {
            loadUserFairness();
        });
        document.getElementById('fairnessSuggestionForm').addEventListener('submit', handleSuggestionSubmit);
        document.getElementById('copySuggestionBtn').addEventListener('click', copySuggestion);
    }

    async function loadUserVehicles() {
        const select = document.getElementById('userVehicleSelect');
        select.innerHTML = '<option value="">Đang tải...</option>';
        try {
            const client = typeof window.authenticatedFetch === 'function'
                ? window.authenticatedFetch
                : fetch;
            const res = await client(`${API_BASE}/api/users/${currentUserId}/vehicles`);

            // Check for 403 error (profile not approved)
            if (res.status === 403) {
                let errorMessage = 'Hồ sơ chưa được duyệt. Vui lòng hoàn tất KYC.';
                try {
                    const errorData = await res.json().catch(() => ({}));
                    if (errorData.message) {
                        errorMessage = errorData.message;
                    }
                } catch (e) {
                    // Use default message
                }

                // Thử refresh token để lấy profileStatus mới từ database
                if (typeof window.authenticatedFetch === 'function' && localStorage.getItem('refreshToken')) {
                    console.log('🔄 Attempting to refresh token to get updated profileStatus...');
                    try {
                        const refreshResponse = await fetch(`${API_BASE}/api/auth/users/refresh`, {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ refreshToken: localStorage.getItem('refreshToken') })
                        });

                        if (refreshResponse.ok) {
                            const newSession = await refreshResponse.json();
                            if (typeof window.saveAuthSession === 'function') {
                                window.saveAuthSession(newSession);
                            }

                            // Thử lại request với token mới
                            console.log('✅ Token refreshed, retrying request...');
                            const retryRes = await client(`${API_BASE}/api/users/${currentUserId}/vehicles`);
                            if (retryRes.ok) {
                                const vehicles = await retryRes.json();
                                select.innerHTML = '';
                                if (!vehicles || !vehicles.length) {
                                    select.innerHTML = '<option value="">Bạn chưa có xe đồng sở hữu</option>';
                                    return;
                                }
                                vehicles.forEach(vehicle => {
                                    const id = vehicle.vehicleId ?? vehicle.id;
                                    const option = document.createElement('option');
                                    option.value = id;
                                    const vehicleName = vehicle.vehicleName || vehicle.name || `Xe #${id}`;
                                    const groupName = vehicle.groupName || `Nhóm #${vehicle.groupId || ''}`;
                                    const ownership = vehicle.ownershipPercentage || 0;
                                    option.textContent = `${vehicleName} - ${groupName} (${ownership}%)`;
                                    select.appendChild(option);
                                });
                                currentVehicleId = Number(select.value);
                                return;
                            }
                        }
                    } catch (refreshError) {
                        console.warn('Failed to refresh token:', refreshError);
                    }
                }

                select.innerHTML = `<option value="">⚠️ ${errorMessage}</option>`;
                console.error('403 Forbidden:', errorMessage);
                return;
            }

            if (!res.ok) {
                throw new Error(`HTTP ${res.status}: ${res.statusText}`);
            }

            const vehicles = await res.json();
            select.innerHTML = '';
            if (!vehicles || !vehicles.length) {
                select.innerHTML = '<option value="">Bạn chưa có xe đồng sở hữu</option>';
                return;
            }
            vehicles.forEach(vehicle => {
                const id = vehicle.vehicleId ?? vehicle.id;
                const option = document.createElement('option');
                option.value = id;
                const vehicleName = vehicle.vehicleName || vehicle.name || `Xe #${id}`;
                const groupName = vehicle.groupName || `Nhóm #${vehicle.groupId || ''}`;
                const ownership = vehicle.ownershipPercentage || 0;
                option.textContent = `${vehicleName} - ${groupName} (${ownership}%)`;
                select.appendChild(option);
            });
            currentVehicleId = Number(select.value);
        } catch (error) {
            console.error('loadUserVehicles error:', error);
            select.innerHTML = '<option value="">Không thể tải danh sách xe</option>';
        }
    }

    async function loadUserFairness() {
        if (!currentVehicleId) {
            return;
        }
        toggleUserLoading(true);
        try {
            currentSummary = await window.FairnessAPI.fetchFairnessSummary(currentVehicleId, currentRange);
            renderUserSummary();
            renderUserCalendar(currentSummary.reservations || []);
        } catch (error) {
            console.error('loadUserFairness', error);
            alert(error.message || 'Không thể tải dữ liệu fairness');
        } finally {
            toggleUserLoading(false);
        }
    }

    function toggleUserLoading(isLoading) {
        const btn = document.getElementById('userRefreshBtn');
        btn.disabled = isLoading;
        btn.innerHTML = isLoading
            ? '<span class="spinner-border spinner-border-sm me-2"></span>Đang tải...'
            : '<i class="bi bi-arrow-repeat"></i> Làm mới';
    }

    function renderUserSummary() {
        const indexValue = currentSummary?.fairnessIndex ?? '-';
        const totalHours = currentSummary?.totalUsageHours ?? 0;

        document.getElementById('userFairnessIndex').textContent =
            indexValue === '-' ? '-' : `${indexValue}%`;
        document.getElementById('userTotalHours').textContent =
            `${totalHours.toFixed ? totalHours.toFixed(1) : totalHours}h`;

        const queue = currentSummary?.priorityQueue || [];
        const position = queue.findIndex(id => id === Number(currentUserId));
        document.getElementById('userPriorityIndex').textContent =
            position >= 0 ? `#${position + 1}` : '—';
    }

    function renderUserCalendar(events) {
        if (typeof FullCalendar === 'undefined') {
            console.warn('FullCalendar chưa được load, đợi thêm...');
            setTimeout(() => renderUserCalendar(events), 100);
            return;
        }

        const calendarEl = document.getElementById('userFairnessCalendar');
        if (!calendarEl) {
            console.warn('Calendar element không tồn tại');
            return;
        }

        if (!userCalendar) {
            try {
                userCalendar = new FullCalendar.Calendar(calendarEl, {
                    initialView: 'dayGridMonth',
                    locale: 'vi',
                    height: 600,
                    eventColor: '#2563eb',
                    eventClick: info => alert(`Lịch của ${info.event.title}`)
                });
                userCalendar.render();
            } catch (error) {
                console.error('Lỗi khi khởi tạo FullCalendar:', error);
                return;
            }
        }
        userCalendar.removeAllEvents();
        events.forEach(event => {
            let startDate = event.start;
            let endDate = event.end;

            if (Array.isArray(startDate)) {
                const [y, m, d, h = 0, min = 0, s = 0] = startDate;
                startDate = new Date(y, m - 1, d, h, min, s);
            }
            if (Array.isArray(endDate)) {
                const [y, m, d, h = 0, min = 0, s = 0] = endDate;
                endDate = new Date(y, m - 1, d, h, min, s);
            }

            userCalendar.addEvent({
                id: event.reservationId,
                title: `${event.userName || 'Thành viên'} - ${event.status}`,
                start: startDate,
                end: endDate,
                backgroundColor: event.userId === Number(currentUserId) ? '#22c55e' : '#94a3b8',
                borderColor: event.userId === Number(currentUserId) ? '#16a34a' : '#94a3b8'
            });
        });
    }

    async function handleSuggestionSubmit(event) {
        event.preventDefault();
        if (!currentVehicleId) {
            alert('Vui lòng chọn xe trước.');
            return;
        }
        const desiredStart = document.getElementById('desiredStart').value;
        const durationHours = Number(document.getElementById('durationHours').value || 2);
        if (!desiredStart) {
            alert('Vui lòng chọn thời gian mong muốn.');
            return;
        }
        const payload = {
            userId: Number(currentUserId),
            desiredStart: desiredStart,
            durationHours
        };
        try {
            const result = await window.FairnessAPI.requestFairnessSuggestion(currentVehicleId, payload);
            renderSuggestionResult(result);
        } catch (error) {
            alert(error.message || 'Không thể xin gợi ý');
        }
    }

    function renderSuggestionResult(result) {
        const box = document.getElementById('suggestionResult');
        box.style.display = 'block';
        document.getElementById('suggestionTitle').textContent = result.approved
            ? 'Bạn được ưu tiên!'
            : 'Hãy cân nhắc thời điểm khác';
        const icon = document.getElementById('suggestionIcon');
        icon.classList.toggle('text-success', result.approved);
        icon.classList.toggle('text-danger', !result.approved);
        document.getElementById('suggestionReason').textContent = result.reason || '';

        const slotList = result.recommendations || [];
        document.getElementById('suggestionSlots').innerHTML = slotList.length
            ? slotList.map(slot => `• ${new Date(slot.start).toLocaleString('vi-VN')} (${slot.durationHours}h)`).join('<br>')
            : 'Chưa có khung giờ thay thế.';

        const bookingBtn = document.getElementById('goToBookingBtn');
        bookingBtn.href = `/reservations/book${currentVehicleId ? `/${currentVehicleId}` : ''}?userId=${currentUserId}`;
    }

    function copySuggestion() {
        const text = document.getElementById('suggestionReason').textContent;
        navigator.clipboard.writeText(text).then(() => {
            alert('Đã sao chép vào clipboard');
        });
    }
})();
