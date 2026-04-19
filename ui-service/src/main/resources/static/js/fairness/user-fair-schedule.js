<<<<<<< HEAD
(function () {
=======
(function() {
>>>>>>> origin/main
    const API_BASE = typeof window.getApiBaseUrl === 'function'
        ? window.getApiBaseUrl()
        : 'http://localhost:8084';

    let currentVehicleId = null;
    let currentRange = 30;
    let currentUserId = null;
    let currentSummary = null;
    let userCalendar;
<<<<<<< HEAD
=======
    let signaturePad;
    let activeCheckpointId = null;
>>>>>>> origin/main

    // Helper function để decode JWT token và kiểm tra profileStatus
    function decodeJWT(token) {
        try {
            const base64Url = token.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
<<<<<<< HEAD
            const jsonPayload = decodeURIComponent(atob(base64).split('').map(function (c) {
=======
            const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
>>>>>>> origin/main
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
<<<<<<< HEAD

=======
        
>>>>>>> origin/main
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
<<<<<<< HEAD

        bindUserControls();
        loadUserVehicles().then(() => {
            loadUserFairness();
=======
        
        setupSignaturePad();
        bindUserControls();
        loadUserVehicles().then(() => {
            loadUserFairness();
            loadUserReservations();
>>>>>>> origin/main
        });
    });

    function bindUserControls() {
        document.getElementById('userVehicleSelect').addEventListener('change', e => {
            currentVehicleId = e.target.value ? Number(e.target.value) : null;
            loadUserFairness();
<<<<<<< HEAD
=======
            loadUserReservations();
>>>>>>> origin/main
        });
        document.getElementById('userRangeSelect').addEventListener('change', e => {
            currentRange = Number(e.target.value || 30);
            loadUserFairness();
        });
        document.getElementById('userRefreshBtn').addEventListener('click', () => {
            loadUserFairness();
<<<<<<< HEAD
        });
        document.getElementById('fairnessSuggestionForm').addEventListener('submit', handleSuggestionSubmit);
        document.getElementById('copySuggestionBtn').addEventListener('click', copySuggestion);
=======
            loadUserReservations();
        });
        document.getElementById('fairnessSuggestionForm').addEventListener('submit', handleSuggestionSubmit);
        document.getElementById('copySuggestionBtn').addEventListener('click', copySuggestion);
        document.getElementById('userGenerateCheckin').addEventListener('click', () => issueUserCheckpoint('CHECK_IN'));
        document.getElementById('userGenerateCheckout').addEventListener('click', () => issueUserCheckpoint('CHECK_OUT'));
        document.getElementById('clearSignatureBtn').addEventListener('click', () => signaturePad && signaturePad.clear());
        document.getElementById('submitSignatureBtn').addEventListener('click', submitSignature);
    }

    function setupSignaturePad() {
        const canvas = document.getElementById('signatureCanvas');
        signaturePad = new SignaturePad(canvas, {
            penColor: '#1d4ed8'
        });
>>>>>>> origin/main
    }

    async function loadUserVehicles() {
        const select = document.getElementById('userVehicleSelect');
        select.innerHTML = '<option value="">Đang tải...</option>';
        try {
            const client = typeof window.authenticatedFetch === 'function'
                ? window.authenticatedFetch
                : fetch;
            const res = await client(`${API_BASE}/api/users/${currentUserId}/vehicles`);
<<<<<<< HEAD

=======
            
>>>>>>> origin/main
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
<<<<<<< HEAD

=======
                
>>>>>>> origin/main
                // Thử refresh token để lấy profileStatus mới từ database
                if (typeof window.authenticatedFetch === 'function' && localStorage.getItem('refreshToken')) {
                    console.log('🔄 Attempting to refresh token to get updated profileStatus...');
                    try {
                        const refreshResponse = await fetch(`${API_BASE}/api/auth/users/refresh`, {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ refreshToken: localStorage.getItem('refreshToken') })
                        });
<<<<<<< HEAD

=======
                        
>>>>>>> origin/main
                        if (refreshResponse.ok) {
                            const newSession = await refreshResponse.json();
                            if (typeof window.saveAuthSession === 'function') {
                                window.saveAuthSession(newSession);
                            }
<<<<<<< HEAD

=======
                            
>>>>>>> origin/main
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
<<<<<<< HEAD

=======
                
>>>>>>> origin/main
                select.innerHTML = `<option value="">⚠️ ${errorMessage}</option>`;
                console.error('403 Forbidden:', errorMessage);
                return;
            }
<<<<<<< HEAD

            if (!res.ok) {
                throw new Error(`HTTP ${res.status}: ${res.statusText}`);
            }

=======
            
            if (!res.ok) {
                throw new Error(`HTTP ${res.status}: ${res.statusText}`);
            }
            
>>>>>>> origin/main
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
<<<<<<< HEAD
=======
        // Kiểm tra xem FullCalendar đã được load chưa
>>>>>>> origin/main
        if (typeof FullCalendar === 'undefined') {
            console.warn('FullCalendar chưa được load, đợi thêm...');
            setTimeout(() => renderUserCalendar(events), 100);
            return;
        }
<<<<<<< HEAD

=======
        
>>>>>>> origin/main
        const calendarEl = document.getElementById('userFairnessCalendar');
        if (!calendarEl) {
            console.warn('Calendar element không tồn tại');
            return;
        }
<<<<<<< HEAD

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
=======
        
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
>>>>>>> origin/main
            } catch (error) {
                console.error('Lỗi khi khởi tạo FullCalendar:', error);
                return;
            }
        }
        userCalendar.removeAllEvents();
        events.forEach(event => {
<<<<<<< HEAD
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
=======
            userCalendar.addEvent({
                id: event.reservationId,
                title: `${event.userName || 'Thành viên'} - ${event.status}`,
                start: event.start,
                end: event.end,
>>>>>>> origin/main
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
<<<<<<< HEAD
})();
=======

    async function loadUserReservations() {
        const select = document.getElementById('userReservationSelect');
        if (!currentVehicleId) {
            select.innerHTML = '<option value="">Chưa chọn xe</option>';
            return;
        }
        console.log('[Fairness] Loading reservations for vehicleId:', currentVehicleId, 'userId:', currentUserId);
        try {
            const client = typeof window.authenticatedFetch === 'function'
                ? window.authenticatedFetch
                : fetch;
            const res = await client(`${API_BASE}/api/vehicles/${currentVehicleId}/reservations`);
            if (!res.ok) {
                console.error('[Fairness] Reservation API error:', res.status, res.statusText);
                throw new Error(`HTTP ${res.status}`);
            }
            const reservations = await res.json();
            console.log('[Fairness] Reservations fetched for vehicle', currentVehicleId, reservations);
            const now = Date.now() - 3600000; // cho phép trễ 1h
            const upcoming = (reservations || [])
                .filter(r => {
                    const ownerId = r.userId ?? (r.user && (r.user.userId ?? r.user.id));
                    console.log('[Fairness] Checking reservation owner', {
                        reservationId: r.reservationId || r.id,
                        ownerId,
                        currentUserId
                    });
                    return Number(ownerId) === Number(currentUserId);
                })
                .filter(r => {
                    const startTime = new Date(r.startDatetime || r.start).getTime();
                    console.log('[Fairness] Reservation time', {
                        reservationId: r.reservationId || r.id,
                        start: r.startDatetime || r.start,
                        parsed: startTime,
                        now
                    });
                    return !Number.isNaN(startTime) && startTime >= now;
                })
                .sort((a, b) => new Date(a.startDatetime || a.start) - new Date(b.startDatetime || b.start));
            
            select.innerHTML = '';
            if (!upcoming.length) {
                console.warn('[Fairness] No upcoming reservations for user', currentUserId);
                select.innerHTML = '<option value="">Không có lịch sắp tới</option>';
                return;
            }
            upcoming.forEach(r => {
                const option = document.createElement('option');
                option.value = r.reservationId || r.id;
                const startLabel = new Date(r.startDatetime || r.start).toLocaleString('vi-VN');
                option.textContent = `${startLabel} (${r.status})`;
                select.appendChild(option);
            });
        } catch (error) {
            console.error('loadUserReservations', error);
            select.innerHTML = '<option value="">Không thể tải lịch sắp tới</option>';
        }
    }

    async function issueUserCheckpoint(type) {
        const select = document.getElementById('userReservationSelect');
        const reservationId = select.value;
        if (!reservationId) {
            alert('Bạn chưa chọn lịch nào.');
            return;
        }
        try {
            const checkpoint = await window.FairnessAPI.issueCheckpoint(Number(reservationId), {
                type,
                issuedBy: 'USER',
                notes: 'Self check-in/out'
            });
            activeCheckpointId = checkpoint.checkpointId;
            await window.FairnessAPI.scanCheckpoint({
                token: checkpoint.qrToken
            });
            showUserQr(checkpoint);
        } catch (error) {
            alert(error.message || 'Không thể tạo QR');
        }
    }

    function showUserQr(checkpoint) {
        const box = document.getElementById('userQrPreview');
        box.style.display = 'block';
        document.getElementById('userQrImage').src =
            `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(checkpoint.qrPayload)}`;
        document.getElementById('userQrExpire').textContent = checkpoint.expiresAt
            ? new Date(checkpoint.expiresAt).toLocaleString('vi-VN')
            : '—';
        document.getElementById('userQrToken').textContent = checkpoint.qrToken;
    }

    async function submitSignature() {
        if (!activeCheckpointId) {
            alert('Bạn cần tạo QR trước.');
            return;
        }
        if (signaturePad.isEmpty()) {
            alert('Vui lòng ký vào khung trước khi gửi.');
            return;
        }
        const signerName = localStorage.getItem('userName') || 'Co-owner';
        const signerId = prompt('Nhập số giấy tờ tùy thân để ký xác nhận:', '');
        if (!signerId) {
            alert('Bạn cần nhập số giấy tờ để ký.');
            return;
        }
        try {
            await window.FairnessAPI.signCheckpoint(activeCheckpointId, {
                signerName,
                signerIdNumber: signerId,
                signatureData: signaturePad.toDataURL('image/png')
            });
            alert('Đã gửi chữ ký thành công!');
            signaturePad.clear();
        } catch (error) {
            alert(error.message || 'Không thể gửi chữ ký');
        }
    }
})();

>>>>>>> origin/main
