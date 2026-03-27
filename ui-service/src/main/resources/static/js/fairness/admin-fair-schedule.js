(function() {
    const API_BASE = typeof window.getApiBaseUrl === 'function'
        ? window.getApiBaseUrl()
        : 'http://localhost:8084';

    let fairnessCalendar;
    let currentVehicleId = null;
    let currentRange = 30;
    let currentSummary = null;
    let reservationModal;

    document.addEventListener('DOMContentLoaded', () => {
        reservationModal = new bootstrap.Modal(document.getElementById('reservationModal'));
        bindControls();
        loadVehicleOptions().then(() => loadFairnessSummary());
    });

    function bindControls() {
        const vehicleSelect = document.getElementById('fairnessVehicleSelect');
        const rangeSelect = document.getElementById('fairnessRangeSelect');
        document.getElementById('refreshFairnessBtn').addEventListener('click', loadFairnessSummary);

        vehicleSelect.addEventListener('change', () => {
            currentVehicleId = vehicleSelect.value ? Number(vehicleSelect.value) : null;
            loadFairnessSummary();
        });
        rangeSelect.addEventListener('change', () => {
            currentRange = Number(rangeSelect.value || 30);
            loadFairnessSummary();
        });

        document.getElementById('issueCheckinBtn').addEventListener('click', () => handleIssueCheckpoint('CHECK_IN'));
        document.getElementById('issueCheckoutBtn').addEventListener('click', () => handleIssueCheckpoint('CHECK_OUT'));
    }

    async function loadVehicleOptions() {
        const select = document.getElementById('fairnessVehicleSelect');
        try {
            const client = typeof window.authenticatedFetch === 'function'
                ? window.authenticatedFetch
                : fetch;
            const res = await client(`${API_BASE}/api/vehicles`);
            const data = await res.json();
            select.innerHTML = '';

            const vehicles = Array.isArray(data) ? data : [];
            if (!vehicles.length) {
                select.innerHTML = '<option value="">Không tìm thấy xe</option>';
                return;
            }

            vehicles.forEach(vehicle => {
                const id = vehicle.vehicleId ?? vehicle.id;
                if (!id) return;
                const option = document.createElement('option');
                option.value = id;
                option.textContent = vehicle.vehicleName || vehicle.name || `Xe #${id}`;
                select.appendChild(option);
            });

            currentVehicleId = Number(select.value);
        } catch (error) {
            console.error('loadVehicleOptions', error);
            select.innerHTML = '<option value="">Không thể tải danh sách xe</option>';
        }
    }

    async function loadFairnessSummary() {
        if (!currentVehicleId) {
            return;
        }

        setLoadingState(true);
        try {
            currentSummary = await window.FairnessAPI.fetchFairnessSummary(currentVehicleId, currentRange);
            renderSummary(currentSummary);
            renderMembers(currentSummary.members || []);
            renderQueue(currentSummary);
            renderAvailability(currentSummary.availability || []);
            renderCalendar(currentSummary.reservations || []);
        } catch (error) {
            console.error('loadFairnessSummary', error);
            showToast(error.message || 'Không thể tải dữ liệu fairness');
        } finally {
            setLoadingState(false);
        }
    }

    function setLoadingState(isLoading) {
        const button = document.getElementById('refreshFairnessBtn');
        button.disabled = isLoading;
        button.innerHTML = isLoading
            ? '<span class="spinner-border spinner-border-sm me-2"></span>Đang tải...'
            : '<i class="bi bi-arrow-repeat"></i> Làm mới';
    }

    function renderSummary(summary) {
        document.getElementById('fairnessIndexValue').textContent =
            summary.fairnessIndex != null ? `${summary.fairnessIndex}%` : '-';
        document.getElementById('totalUsageHours').textContent =
            summary.totalUsageHours != null ? `${summary.totalUsageHours.toFixed(1)}h` : '-';

        const queue = summary.priorityQueue || [];
        const nextUser = queue.length
            ? (summary.members || []).find(m => m.userId === queue[0])
            : null;
        document.getElementById('nextPriorityMember').textContent =
            nextUser ? nextUser.fullName : 'Chưa xác định';

        const firstSlot = (summary.availability || []).find(slot => slot.durationHours >= 0.5);
        document.getElementById('nextAvailabilitySlot').textContent = firstSlot
            ? `${formatTime(firstSlot.start)}`
            : 'Không có';
    }

    function renderMembers(members) {
        const tbody = document.getElementById('membersTableBody');
        if (!members.length) {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted py-4">
                Chưa có dữ liệu sử dụng cho xe này.
            </td></tr>`;
            return;
        }

        tbody.innerHTML = '';
        members.forEach(member => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>
                    <div class="fw-semibold">${member.fullName}</div>
                    <small class="text-muted">${member.email || '—'}</small>
                </td>
                <td class="text-center">${member.ownershipPercentage ?? 0}%</td>
                <td class="text-center">${member.usagePercentage ?? 0}%</td>
                <td class="text-center ${member.difference > 0 ? 'text-danger' : 'text-success'}">
                    ${member.difference > 0 ? '+' : ''}${member.difference ?? 0}%
                </td>
                <td class="text-center">
                    <span class="priority-chip priority-${member.priority || 'NORMAL'}">
                        ${member.priority || 'NORMAL'}
                    </span>
                </td>
            `;
            row.addEventListener('click', () => renderMemberDetail(member));
            tbody.appendChild(row);
        });

        renderMemberDetail(members[0]);
    }

    function renderMemberDetail(member) {
        if (!member) {
            document.getElementById('memberDetailPanel').textContent = 'Chưa chọn thành viên.';
            return;
        }
        const html = `
            <div class="mb-2">
                <strong>${member.fullName}</strong>
                <div class="text-muted small">${member.email || ''}</div>
            </div>
            <ul class="list-unstyled small mb-0">
                <li>Quyền sở hữu: <strong>${member.ownershipPercentage ?? 0}%</strong></li>
                <li>Đã sử dụng: <strong>${member.usagePercentage ?? 0}%</strong> (${member.usageHours ?? 0}h)</li>
                <li>Chênh lệch: <strong class="${member.difference > 0 ? 'text-danger' : 'text-success'}">
                    ${member.difference > 0 ? '+' : ''}${member.difference ?? 0}%</strong></li>
                <li>Ưu tiên: <span class="priority-chip priority-${member.priority || 'NORMAL'}">
                    ${member.priority || 'NORMAL'}</span></li>
                <li>Lần sử dụng gần nhất: ${member.lastUsageEnd ? formatTime(member.lastUsageEnd) : 'Chưa có'}</li>
                <li>Lịch sắp tới: ${member.nextReservationStart ? formatTime(member.nextReservationStart) : 'Không có'}</li>
            </ul>
        `;
        document.getElementById('memberDetailPanel').innerHTML = html;
    }

    function renderQueue(summary) {
        const list = document.getElementById('priorityQueueList');
        const queue = summary.priorityQueue || [];
        list.innerHTML = '';

        if (!queue.length) {
            list.innerHTML = '<li class="text-muted">Chưa có dữ liệu ưu tiên.</li>';
            return;
        }

        queue.forEach((userId, index) => {
            const member = (summary.members || []).find(m => m.userId === userId);
            if (!member) return;
            const li = document.createElement('li');
            li.innerHTML = `
                <div class="d-flex align-items-center gap-3">
                    <div class="queue-rank">${index + 1}</div>
                    <div>
                        <div class="fw-semibold">${member.fullName}</div>
                        <small class="text-muted">Chênh lệch ${member.difference ?? 0}%</small>
                    </div>
                </div>
                <span class="priority-chip priority-${member.priority}">
                    ${member.priority}
                </span>
            `;
            list.appendChild(li);
        });
    }

    function renderAvailability(availability) {
        const container = document.getElementById('availabilityList');
        container.innerHTML = '';
        if (!availability.length) {
            container.innerHTML = '<span class="text-muted small">Không có khoảng trống phù hợp.</span>';
            return;
        }
        availability.slice(0, 6).forEach(slot => {
            const chip = document.createElement('span');
            chip.className = 'availability-chip';
            chip.innerHTML = `<i class="bi bi-clock-history"></i> ${formatTime(slot.start)} • ${slot.durationHours}h`;
            container.appendChild(chip);
        });
    }

    function renderCalendar(events) {
        const calendarEl = document.getElementById('fairnessCalendar');
        if (!fairnessCalendar) {
            fairnessCalendar = new FullCalendar.Calendar(calendarEl, {
                initialView: 'timeGridWeek',
                height: 650,
                locale: 'vi',
                headerToolbar: {
                    left: 'prev,next today',
                    center: 'title',
                    right: 'dayGridMonth,timeGridWeek,timeGridDay'
                },
                eventClick: info => openReservationModal(info.event.extendedProps)
            });
            fairnessCalendar.render();
        }

        fairnessCalendar.removeAllEvents();
        events.forEach(event => {
            fairnessCalendar.addEvent({
                id: event.reservationId,
                title: `${event.userName || 'Thành viên'} - ${event.status}`,
                start: event.start,
                end: event.end,
                backgroundColor: getStatusColor(event.status),
                borderColor: getStatusColor(event.status),
                extendedProps: event
            });
        });
    }

    function getStatusColor(status) {
        switch (status) {
            case 'COMPLETED':
                return '#22c55e';
            case 'CANCELLED':
                return '#ef4444';
            default:
                return '#6366f1';
        }
    }

    function formatTime(value) {
        if (!value) return '';
        const date = typeof value === 'string' ? new Date(value) : value;
        return date.toLocaleString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    function openReservationModal(reservation) {
        if (!reservation) return;
        document.getElementById('reservationModalTitle').textContent =
            reservation.vehicleName || `Xe #${reservation.vehicleId}`;
        document.getElementById('reservationModalSubtitle').textContent =
            `Người đặt: ${reservation.userName || 'Chưa rõ'}`;

        const infoList = document.getElementById('reservationInfoList');
        infoList.innerHTML = `
            <dt class="col-5">ID</dt><dd class="col-7">#${reservation.reservationId}</dd>
            <dt class="col-5">Thời gian</dt><dd class="col-7">${formatTime(reservation.start)} - ${formatTime(reservation.end)}</dd>
            <dt class="col-5">Trạng thái</dt><dd class="col-7">${reservation.status}</dd>
            <dt class="col-5">Ghi chú</dt><dd class="col-7">${reservation.purpose || '—'}</dd>
        `;

        document.getElementById('issueCheckinBtn').dataset.reservationId = reservation.reservationId;
        document.getElementById('issueCheckoutBtn').dataset.reservationId = reservation.reservationId;

        document.getElementById('checkpointPreview').style.display = 'none';
        document.getElementById('signaturePreview').classList.add('d-none');
        document.getElementById('signaturePlaceholder').classList.remove('d-none');

        loadCheckpointHistory(reservation.reservationId);
        reservationModal.show();
    }

    async function loadCheckpointHistory(reservationId) {
        const container = document.getElementById('checkpointHistory');
        container.innerHTML = '<div class="text-muted small">Đang tải...</div>';
        try {
            const checkpoints = await window.FairnessAPI.listCheckpoints(reservationId);
            if (!checkpoints.length) {
                container.innerHTML = '<div class="text-muted small">Chưa có mã nào.</div>';
                return;
            }
            container.innerHTML = checkpoints.map(cp => `
                <div class="border rounded-3 p-2 mb-2">
                    <div class="d-flex justify-content-between">
                        <strong>${cp.checkpointType === 'CHECK_IN' ? 'Check-in' : 'Check-out'}</strong>
                        <span class="priority-chip priority-${cp.status}">
                            ${cp.status}
                        </span>
                    </div>
                    <small class="text-muted d-block">Tạo lúc: ${formatTime(cp.issuedAt)}</small>
                    ${cp.scannedAt ? `<small class="text-muted d-block">QR quét lúc ${formatTime(cp.scannedAt)}</small>` : ''}
                    ${cp.signedAt ? `<small class="text-muted d-block">Ký lúc ${formatTime(cp.signedAt)}</small>` : ''}
                </div>
            `).join('');

            // show last signature if exist
            const signed = checkpoints.find(cp => !!cp.signatureData);
            if (signed) {
                const img = document.getElementById('signaturePreview');
                img.src = signed.signatureData;
                img.classList.remove('d-none');
                document.getElementById('signaturePlaceholder').classList.add('d-none');
            }
        } catch (error) {
            container.innerHTML = `<div class="text-danger small">${error.message}</div>`;
        }
    }

    async function handleIssueCheckpoint(type) {
        const btn = type === 'CHECK_IN'
            ? document.getElementById('issueCheckinBtn')
            : document.getElementById('issueCheckoutBtn');
        const reservationId = btn.dataset.reservationId;
        if (!reservationId) return;

        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang tạo...';

        try {
            const checkpoint = await window.FairnessAPI.issueCheckpoint(Number(reservationId), {
                type,
                issuedBy: 'ADMIN'
            });
            showCheckpointPreview(checkpoint);
            await loadCheckpointHistory(Number(reservationId));
        } catch (error) {
            showToast(error.message || 'Không thể tạo mã QR');
        } finally {
            btn.disabled = false;
            btn.innerHTML = type === 'CHECK_IN'
                ? '<i class="bi bi-qr-code-scan"></i> Tạo QR Check-in'
                : '<i class="bi bi-qr-code"></i> Tạo QR Check-out';
        }
    }

    function showCheckpointPreview(checkpoint) {
        const preview = document.getElementById('checkpointPreview');
        preview.style.display = 'block';
        const img = document.getElementById('checkpointQrImage');
        img.src = `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(checkpoint.qrPayload)}`;
        document.getElementById('checkpointToken').textContent = checkpoint.qrToken;
        document.getElementById('checkpointExpire').textContent =
            checkpoint.expiresAt ? formatTime(checkpoint.expiresAt) : '—';
    }

    function showToast(message) {
        if (!message) return;
        alert(message);
    }
})();

