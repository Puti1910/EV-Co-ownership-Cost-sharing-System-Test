const maintenanceState = {
    options: [],
    selected: [], // Changed to array to support multiple selection
    loading: false
};

const API_BASE_MAINTENANCE = typeof window.getApiBaseUrl === 'function'
    ? window.getApiBaseUrl()
    : 'http://localhost:8084';

document.addEventListener('DOMContentLoaded', () => {
    const page = document.getElementById('maintenance-page');
    if (!page) {
        return;
    }
    initMaintenancePage();
});

async function initMaintenancePage() {
    updateSelectionSummary();
    await loadMaintenanceOptions();
    initMaintenanceForm();
    initResetButton();
}

function getCurrentUserName() {
    const container = document.querySelector('.user-container');
    return container?.dataset.userName || 'Thành viên CarShare';
}

function getCurrentUserId() {
    const container = document.querySelector('.user-container');
    if (!container) {
        console.warn('[Maintenance] user container not found');
    } else {
        console.log('[Maintenance] user id:', container.dataset.userId, 'name:', container.dataset.userName);
    }
    return container?.dataset.userId || null;
}

async function loadMaintenanceOptions() {
    const userId = getCurrentUserId();
    if (!userId) {
        showToast('Không xác định được người dùng. Vui lòng đăng nhập lại.', 'error');
        return;
    }

    maintenanceState.loading = true;
    toggleLoadingState(true);
    try {
        console.log('[Maintenance] fetching vehicles for user', userId);
        const url = `${API_BASE_MAINTENANCE}/api/users/${userId}/vehicles`;
        const response = await authenticatedRequest(url);
        if (!response.ok) {
            throw new Error('Không thể tải danh sách xe.');
        }
        const data = await response.json();
        console.log('[Maintenance] vehicles response', data);
        const normalized = Array.isArray(data) ? data.map((vehicle, index) => ({
            id: `${vehicle.vehicleType ?? vehicle.vehicleId ?? index}`,
            groupId: vehicle.groupId ?? vehicle.group?.groupId ?? null,
            groupName: vehicle.groupName || vehicle.group?.groupName || ('Group #' + (vehicle.groupId ?? '')),
            vehicleId: vehicle.vehicleId ?? vehicle.id ?? index,
            vehicleName: vehicle.vehicleName || vehicle.name || 'Xe đồng sở hữu',
            vehicleType: vehicle.vehicleType || 'N/A',
            licensePlate: vehicle.licensePlate || '',
            role: vehicle.role || vehicle.membershipRole || 'Thành viên'
        })) : [];

        maintenanceState.options = normalized;
        renderMaintenanceOptions(normalized);
    } catch (error) {
        console.error(error);
        showToast(error.message, 'error');
        renderMaintenanceOptions([]);
    } finally {
        maintenanceState.loading = false;
        toggleLoadingState(false);
    }
}

function toggleLoadingState(isLoading) {
    const loadingEl = document.getElementById('maintenance-options-loading');
    if (!loadingEl) return;
    loadingEl.style.display = isLoading ? 'flex' : 'none';
}

function renderMaintenanceOptions(options) {
    const checkboxContainer = document.getElementById('maintenance-vehicle-checkbox-container');
    const checkboxList = document.getElementById('maintenance-vehicle-checkbox-list');
    const emptyState = document.getElementById('maintenance-empty-state');
    const submitBtn = document.getElementById('maintenance-submit-btn');

    if (!checkboxContainer || !checkboxList || !emptyState || !submitBtn) {
        return;
    }

    checkboxList.innerHTML = '';
    if (!options.length) {
        checkboxContainer.style.display = 'none';
        emptyState.style.display = 'flex';
        submitBtn.disabled = true;
        maintenanceState.selected = [];
        updateSelectionSummary();
        document.getElementById('maintenance-total-vehicles').textContent = '0';
        return;
    }

    document.getElementById('maintenance-total-vehicles').textContent = options.length;
    checkboxContainer.style.display = 'block';
    emptyState.style.display = 'none';

    // Group vehicles by groupId
    const groupedByGroup = {};
    options.forEach(option => {
        const groupId = option.groupId || 'unknown';
        const groupName = option.groupName || ('Nhóm #' + groupId);
        if (!groupedByGroup[groupId]) {
            groupedByGroup[groupId] = {
                groupId: groupId,
                groupName: groupName,
                vehicles: []
            };
        }
        groupedByGroup[groupId].vehicles.push(option);
    });

    // Render vehicles grouped by group
    let checkboxIndex = 0;
    Object.keys(groupedByGroup).sort((a, b) => {
        const nameA = groupedByGroup[a].groupName.toLowerCase();
        const nameB = groupedByGroup[b].groupName.toLowerCase();
        return nameA.localeCompare(nameB);
    }).forEach(groupId => {
        const groupData = groupedByGroup[groupId];
        
        // Add group header
        const groupHeader = document.createElement('div');
        groupHeader.style.cssText = 'grid-column: 1 / -1; padding: 12px 16px; background: #e0e7ff; border-radius: 8px; margin-bottom: 12px; margin-top: 8px; font-weight: 600; color: #4F46E5; font-size: 15px; border-left: 4px solid #4F46E5;';
        groupHeader.innerHTML = `<i class="fas fa-users" style="margin-right: 8px;"></i>${groupData.groupName}`;
        checkboxList.appendChild(groupHeader);

        // Create checkbox for each vehicle in this group
        groupData.vehicles.forEach((option) => {
            const roleText = option.role === 'ADMIN' || option.role === 'Admin' ? 'Admin' : 'Member';
            const vehicleData = {
                groupId: option.groupId,
                groupName: option.groupName || ('Group #' + option.groupId),
                vehicleId: option.vehicleId,
                vehicleName: option.vehicleName || ('Vehicle #' + option.vehicleId),
                vehicleType: option.vehicleType || 'N/A',
                licensePlate: option.licensePlate || ''
            };
            const vehicleDataJson = JSON.stringify(vehicleData);

            const checkboxItem = document.createElement('div');
            checkboxItem.className = 'vehicle-checkbox-item';
            checkboxItem.style.cssText = 'display: flex; align-items: center; padding: 12px; border: 1px solid #e0e0e0; border-radius: 8px; background: #f9f9f9; cursor: pointer; transition: all 0.2s;';
            
            // Add hover effect
            checkboxItem.addEventListener('mouseenter', () => {
                if (!checkboxItem.querySelector('input[type="checkbox"]').checked) {
                    checkboxItem.style.borderColor = '#c7c7d1';
                    checkboxItem.style.background = '#f5f5f5';
                }
            });
            checkboxItem.addEventListener('mouseleave', () => {
                const checkbox = checkboxItem.querySelector('input[type="checkbox"]');
                if (!checkbox.checked) {
                    checkboxItem.style.borderColor = '#e0e0e0';
                    checkboxItem.style.background = '#f9f9f9';
                }
            });
            
            // Format status display
            const status = option.status || 'ready';
            const statusText = status === 'ready' ? 'Sẵn sàng' : 
                             status === 'maintenance' ? 'Bảo dưỡng' :
                             status === 'in_use' ? 'Đang sử dụng' :
                             status === 'repair' ? 'Sửa chữa' :
                             status === 'checking' ? 'Kiểm tra' : status;
            const statusColor = status === 'ready' ? '#10B981' : 
                               status === 'maintenance' ? '#F59E0B' :
                               status === 'in_use' ? '#3B82F6' :
                               status === 'repair' ? '#EF4444' :
                               status === 'checking' ? '#8B5CF6' : '#6B7280';
            
            checkboxItem.innerHTML = `
                <input type="checkbox" 
                       id="vehicle-checkbox-${checkboxIndex}" 
                       class="vehicle-checkbox" 
                       value="${vehicleDataJson.replace(/"/g, '&quot;')}"
                       style="width: 20px; height: 20px; margin-right: 12px; cursor: pointer;">
                <label for="vehicle-checkbox-${checkboxIndex}" style="flex: 1; cursor: pointer; margin: 0;">
                    <div style="padding: 1rem; background: white; border-radius: 4px; border-left: 4px solid #4F46E5;">
                        <div style="display: grid; gap: 0.5rem;">
                            <div style="display: flex; justify-content: space-between; align-items: center;">
                                <strong style="color: #374151;">Mã xe:</strong>
                                <span style="color: #6B7280;">${option.vehicleId || 'N/A'}</span>
                            </div>
                            <div style="display: flex; justify-content: space-between; align-items: center;">
                                <strong style="color: #374151;">Biển số xe:</strong>
                                <span style="color: #6B7280;">${option.licensePlate || 'N/A'}</span>
                            </div>
                            <div style="display: flex; justify-content: space-between; align-items: center;">
                                <strong style="color: #374151;">Loại xe:</strong>
                                <span style="color: #6B7280;">${option.vehicleType || 'N/A'}</span>
                            </div>
                            <div style="display: flex; justify-content: space-between; align-items: center;">
                                <strong style="color: #374151;">Trạng thái:</strong>
                                <span style="background: ${statusColor}; color: white; padding: 0.25rem 0.75rem; border-radius: 12px; font-size: 0.875rem;">
                                    ${statusText}
                                </span>
                            </div>
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 0.5rem; padding-top: 0.5rem; border-top: 1px solid #E5E7EB;">
                                <strong style="color: #374151; font-size: 0.875rem;">Nhóm:</strong>
                                <span style="color: #6B7280; font-size: 0.875rem;">${option.groupName || ('Nhóm #' + option.groupId)}</span>
                            </div>
                            <div style="display: flex; justify-content: space-between; align-items: center;">
                                <strong style="color: #374151; font-size: 0.875rem;">Vai trò:</strong>
                                <span style="color: #6B7280; font-size: 0.875rem;">${roleText}</span>
                            </div>
                        </div>
                    </div>
                </label>
            `;

            // Add click handler to the entire item
            checkboxItem.addEventListener('click', (e) => {
                if (e.target.type !== 'checkbox') {
                    const checkbox = checkboxItem.querySelector('input[type="checkbox"]');
                    checkbox.checked = !checkbox.checked;
                    checkbox.dispatchEvent(new Event('change'));
                }
            });

            // Add change handler to checkbox
            const checkbox = checkboxItem.querySelector('input[type="checkbox"]');
            checkbox.addEventListener('change', () => {
                updateSelectedVehicles();
                // Update visual state
                if (checkbox.checked) {
                    checkboxItem.style.borderColor = '#4F46E5';
                    checkboxItem.style.background = '#f0f0ff';
                } else {
                    checkboxItem.style.borderColor = '#e0e0e0';
                    checkboxItem.style.background = '#f9f9f9';
                }
            });

            checkboxList.appendChild(checkboxItem);
            checkboxIndex++;
        });
    });

    updateSelectedVehicles();
}

function updateSelectedVehicles() {
    const checkboxes = document.querySelectorAll('.vehicle-checkbox:checked');
    maintenanceState.selected = Array.from(checkboxes).map(checkbox => JSON.parse(checkbox.value));
    
    const submitBtn = document.getElementById('maintenance-submit-btn');
    if (submitBtn) {
        submitBtn.disabled = maintenanceState.selected.length === 0;
    }
    
    updateSelectionSummary();
}

function updateSelectionSummary() {
    const summary = document.getElementById('maintenance-selection-summary');
    if (!summary) return;

    if (!maintenanceState.selected || maintenanceState.selected.length === 0) {
        summary.innerHTML = '<i class="fas fa-circle-info"></i><span>Hãy chọn một hoặc nhiều xe ở danh sách phía trên trước khi gửi yêu cầu.</span>';
        return;
    }

    const count = maintenanceState.selected.length;
    if (count === 1) {
        const vehicle = maintenanceState.selected[0];
        summary.innerHTML = `
            <i class="fas fa-check-circle" style="color: var(--success);"></i>
            <span>
                Đang chọn <strong>${vehicle.vehicleName}</strong> thuộc nhóm
                <strong>${vehicle.groupName}</strong>.
            </span>
        `;
    } else {
        const vehicleNames = maintenanceState.selected.map(v => v.vehicleName).join(', ');
        summary.innerHTML = `
            <i class="fas fa-check-circle" style="color: var(--success);"></i>
            <span>
                Đã chọn <strong>${count} xe</strong>: ${vehicleNames}
            </span>
        `;
    }
}

function initMaintenanceForm() {
    const form = document.getElementById('maintenance-form');
    if (!form) return;

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!maintenanceState.selected || maintenanceState.selected.length === 0) {
            showToast('Bạn cần chọn ít nhất một xe trước khi đặt dịch vụ.', 'error');
            return;
        }

        const submitBtn = document.getElementById('maintenance-submit-btn');
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang gửi...';

        try {
            // Gửi yêu cầu cho từng xe đã chọn
            const promises = maintenanceState.selected.map(vehicle => {
                const payload = buildMaintenancePayload(vehicle);
                return authenticatedRequest('/api/vehicle-maintenance/book', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(payload)
                });
            });

            const responses = await Promise.all(promises);
            const errors = [];
            
            for (let i = 0; i < responses.length; i++) {
                if (!responses[i].ok) {
                    const message = await responses[i].json().catch(() => ({ message: 'Không thể đặt lịch.' }));
                    errors.push(`${maintenanceState.selected[i].vehicleName}: ${message.message || 'Lỗi không xác định'}`);
                }
            }

            if (errors.length > 0) {
                if (errors.length === responses.length) {
                    // Tất cả đều lỗi
                    throw new Error('Không thể đặt lịch cho bất kỳ xe nào. ' + errors.join('; '));
                } else {
                    // Một số thành công, một số lỗi
                    showToast(`Đã gửi yêu cầu cho ${responses.length - errors.length} xe. ${errors.length} xe gặp lỗi: ${errors.join('; ')}`, 'warning');
                }
            } else {
                // Tất cả thành công
                const count = maintenanceState.selected.length;
                showToast(`Đã gửi yêu cầu bảo dưỡng cho ${count} ${count === 1 ? 'xe' : 'xe'}!`, 'success');
            }

            form.reset();
            // Clear selection
            const checkboxes = document.querySelectorAll('.vehicle-checkbox');
            checkboxes.forEach(checkbox => {
                checkbox.checked = false;
                const item = checkbox.closest('.vehicle-checkbox-item');
                if (item) {
                    item.style.borderColor = '#e0e0e0';
                    item.style.background = '#f9f9f9';
                }
            });
            maintenanceState.selected = [];
            updateSelectionSummary();
        } catch (error) {
            console.error(error);
            showToast(error.message, 'error');
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<i class="fas fa-paper-plane"></i> Gửi yêu cầu';
        }
    });
}

function buildMaintenancePayload(vehicle) {
    const serviceSelect = document.getElementById('maintenance-service');
    const descriptionInput = document.getElementById('maintenance-description');
    const phoneInput = document.getElementById('maintenance-phone');
    const noteInput = document.getElementById('maintenance-note');
    const startInput = document.getElementById('maintenance-start');
    const endInput = document.getElementById('maintenance-end');

    const selectedOption = serviceSelect.options[serviceSelect.selectedIndex];
    const serviceName = selectedOption?.dataset.serviceName || selectedOption?.textContent || '';

    return {
        groupId: vehicle.groupId,
        vehicleId: vehicle.vehicleId,
        vehicleName: vehicle.vehicleName,
        vehicleType: vehicle.vehicleType || null,
        licensePlate: vehicle.licensePlate || null,
        serviceId: serviceSelect.value,
        serviceName: serviceName,
        serviceDescription: descriptionInput.value?.trim(),
        contactPhone: phoneInput.value?.trim(),
        note: noteInput.value?.trim(),
        preferredStartDatetime: toIsoString(startInput.value),
        preferredEndDatetime: toIsoString(endInput.value),
        requestedByName: getCurrentUserName()
    };
}

function toIsoString(value) {
    if (!value) return null;
    try {
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return null;
        }
        return date.toISOString();
    } catch (error) {
        return null;
    }
}

function initResetButton() {
    const resetBtn = document.getElementById('maintenance-reset-btn');
    const form = document.getElementById('maintenance-form');
    if (!resetBtn || !form) return;

    resetBtn.addEventListener('click', () => {
        form.reset();
        const checkboxes = document.querySelectorAll('.vehicle-checkbox');
        checkboxes.forEach(checkbox => {
            checkbox.checked = false;
            const item = checkbox.closest('.vehicle-checkbox-item');
            if (item) {
                item.style.borderColor = '#e0e0e0';
                item.style.background = '#f9f9f9';
            }
        });
        maintenanceState.selected = [];
        updateSelectionSummary();
    });
}

