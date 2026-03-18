function applyFilter(event) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }

    const searchQuery = document.querySelector('.filter-group input[type="text"]').value;
    const serviceFilter = document.getElementById('serviceFilter').value;
    const url = new URL(window.location.href);
    url.searchParams.set('searchQuery', searchQuery);
    url.searchParams.set('serviceFilter', serviceFilter);
    url.searchParams.delete('page');
    window.location.href = url.toString();
}

// Use event delegation for dynamically added buttons
document.addEventListener('click', function(e) {
    // Handle delete service button
    if (e.target.closest('.btn-delete-service')) {
        e.preventDefault();
        e.stopPropagation();
        const btn = e.target.closest('.btn-delete-service');
        const serviceId = btn.getAttribute('data-service-id');
        const vehicleId = btn.getAttribute('data-vehicle-id');
        if (serviceId && vehicleId) {
            console.log('Clicking delete service button:', serviceId, vehicleId);
            if (window.deleteVehicleServiceInline) {
                window.deleteVehicleServiceInline(serviceId, vehicleId);
            } else {
                console.error('deleteVehicleServiceInline not found');
                alert('Lỗi: Không tìm thấy hàm xóa dịch vụ');
            }
        } else {
            console.error('No service-id or vehicle-id found on delete button');
            alert('Lỗi: Không tìm thấy thông tin dịch vụ để xóa');
        }
    }
});

document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.querySelector('.filter-group input[type="text"]');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                applyFilter(e);
            }
        });
    }

    const filterBtn = document.getElementById('btnFilter');
    if (filterBtn) {
        filterBtn.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            applyFilter(e);
        });
    }

    // Also attach directly for delete buttons that exist on page load
    document.querySelectorAll('.btn-delete-service').forEach(btn => {
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            const serviceId = this.getAttribute('data-service-id');
            const vehicleId = this.getAttribute('data-vehicle-id');
            if (serviceId && vehicleId) {
                console.log('Clicking delete service button:', serviceId, vehicleId);
                if (window.deleteVehicleServiceInline) {
                    window.deleteVehicleServiceInline(serviceId, vehicleId);
                } else {
                    console.error('deleteVehicleServiceInline not found');
                    alert('Lỗi: Không tìm thấy hàm xóa dịch vụ');
                }
            } else {
                console.error('No service-id or vehicle-id found on delete button');
                alert('Lỗi: Không tìm thấy thông tin dịch vụ để xóa');
            }
        });
    });
});

let statusChanges = {};
let currentVehicleId = null;

function openVehicleDetailModal(vehicleId) {
    console.log('Mở modal chi tiết cho xe: ' + vehicleId);
    const modal = document.getElementById('vehicleDetailModal');
    if (modal) {
        modal.style.display = 'block';
    }
    currentVehicleId = vehicleId;
    statusChanges = {};
    loadVehicleDetail(vehicleId);
}

function closeVehicleDetailModal(skipCheck = false) {
    if (!skipCheck && Object.keys(statusChanges).length > 0) {
        if (confirm('Bạn có thay đổi chưa lưu. Bạn có muốn lưu trước khi đóng không?')) {
            saveChangesAndClose();
            return;
        }
    }
    const modal = document.getElementById('vehicleDetailModal');
    if (modal) {
        modal.style.display = 'none';
    }
    statusChanges = {};
    currentVehicleId = null;
}

async function loadVehicleDetail(vehicleId) {
    try {
        const row = document.querySelector(`tr[data-vehicle-id="${vehicleId}"]`);
        if (row) {
            const vehicleName = row.querySelector('.vehicle-name')?.textContent || '-';
            const plateNumber = row.cells[1]?.textContent || '-';
            const vehicleType = row.cells[2]?.textContent || '-';
            document.getElementById('modalVehicleId').textContent = vehicleId;
            document.getElementById('modalVehicleName').textContent = vehicleName;
            document.getElementById('modalPlateNumber').textContent = plateNumber;
            document.getElementById('modalVehicleType').textContent = vehicleType;
        }
        const response = await fetch(`/admin/vehicle-services/api/vehicle/${vehicleId}/services`);
        const data = await response.json();
        if (data.success && data.services) {
            displayServices(data.services);
            updateSaveButtonState();
        } else {
            document.getElementById('modalServicesList').innerHTML = '<div class="error-message">Không thể tải danh sách dịch vụ</div>';
            document.getElementById('modalServicesHistory').innerHTML = '<div class="no-data">Không có lịch sử dịch vụ</div>';
        }
    } catch (error) {
        console.error('Lỗi khi load chi tiết xe:', error);
        document.getElementById('modalServicesList').innerHTML = '<div class="error-message">Đã xảy ra lỗi khi tải dữ liệu</div>';
        document.getElementById('modalServicesHistory').innerHTML = '<div class="error-message">Đã xảy ra lỗi khi tải lịch sử</div>';
    }
}

function displayServices(services) {
    const servicesList = document.getElementById('modalServicesList');
    const servicesHistory = document.getElementById('modalServicesHistory');

    if (!services || services.length === 0) {
        servicesList.innerHTML = '<div class="no-data">Không có dịch vụ đang chờ</div>';
        servicesHistory.innerHTML = '<div class="no-data">Không có lịch sử dịch vụ</div>';
        return;
    }

    const pendingServices = [];
    const completedServices = [];

    services.forEach(service => {
        const status = (service.status || 'pending').toLowerCase().trim();
        if (status === 'completed' || status === 'complete') {
            completedServices.push(service);
        } else {
            pendingServices.push(service);
        }
    });

    console.log('📊 Phân tách dịch vụ từ bảng vehicleservice:');
    console.log('   - Dịch vụ đang chờ (pending/in_progress):', pendingServices.length);
    console.log('   - Lịch sử dịch vụ (completed):', completedServices.length);

    if (pendingServices.length === 0) {
        servicesList.innerHTML = '<div class="no-data">Không có dịch vụ đang chờ</div>';
    } else {
        let html = '<div class="service-items">';
        pendingServices.forEach(service => {
            html += buildServiceItem(service, false);
        });
        html += '</div>';
        servicesList.innerHTML = html;
    }

    if (completedServices.length === 0) {
        servicesHistory.innerHTML = '<div class="no-data">Không có lịch sử dịch vụ</div>';
    } else {
        let html = '<div class="service-items">';
        completedServices.forEach(service => {
            html += buildServiceItem(service, true);
        });
        html += '</div>';
        servicesHistory.innerHTML = html;
    }
}

function buildServiceItem(service, isHistory) {
    let id = '';
    let serviceId = '';
    let vehicleId = '';

    if (service.id !== undefined && service.id !== null) {
        if (typeof service.id === 'object') {
            id = '';
            serviceId = service.id.serviceId || '';
            vehicleId = service.id.vehicleId || '';
        } else {
            id = service.id;
            serviceId = service.serviceId || '';
            vehicleId = service.vehicleId || '';
        }
    } else {
        serviceId = service.serviceId || '';
        vehicleId = service.vehicleId || '';
    }

    const serviceName = service.serviceName || 'Dịch vụ không tên';
    const serviceType = service.serviceType || 'Không xác định';
    const serviceDescription = service.serviceDescription || '';
    const status = (service.status || 'pending').toLowerCase().trim();
    const requestDate = service.requestDate ? formatDate(service.requestDate) : '-';
    const completionDate = service.completionDate ? formatDate(service.completionDate) : null;
    const isCompleted = status === 'completed' || status === 'Completed' || isHistory;
    const disabledAttr = isCompleted ? 'disabled' : '';
    const readonlyClass = isCompleted ? 'status-readonly' : '';
    const historyClass = isHistory ? 'service-history-item' : '';

    return `<div class="service-item ${historyClass}" data-id="${id}" data-service-id="${serviceId}" data-vehicle-id="${vehicleId}">
        <div class="service-header">
            <h4>${serviceName}</h4>
            <select class="status-select ${readonlyClass}"
                    data-id="${id}"
                    data-service-id="${serviceId}"
                    data-vehicle-id="${vehicleId}"
                    data-original-status="${status}"
                    ${disabledAttr}
                    onchange="trackStatusChange(this)">
                <option value="pending" ${status === 'pending' ? 'selected' : ''}>Pending</option>
                <option value="in_progress" ${status === 'in_progress' || status === 'in progress' ? 'selected' : ''}>In Progress</option>
                <option value="completed" ${status === 'completed' ? 'selected' : ''}>Completed</option>
            </select>
            ${isCompleted ? '<span class="readonly-badge">Chỉ xem</span>' : ''}
        </div>
        <div class="service-details">
            <div class="detail-row"><label>Loại dịch vụ:</label><span>${serviceType}</span></div>
            <div class="detail-row"><label>Mô tả:</label><span>${serviceDescription}</span></div>
            <div class="detail-row"><label>Ngày yêu cầu:</label><span>${requestDate}</span></div>
            ${completionDate ? `<div class="detail-row"><label>Ngày hoàn thành:</label><span>${completionDate}</span></div>` : ''}
        </div>
    </div>`;
}

function formatDate(dateString) {
    if (!dateString) return '-';
    try {
        const date = new Date(dateString);
        const day = String(date.getDate()).padStart(2, '0');
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const year = date.getFullYear();
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        return `${day}/${month}/${year} ${hours}:${minutes}:${seconds}`;
    } catch (e) {
        return dateString;
    }
}

function trackStatusChange(selectElement) {
    const id = selectElement.getAttribute('data-id');
    const serviceId = selectElement.getAttribute('data-service-id');
    const vehicleId = selectElement.getAttribute('data-vehicle-id');
    const originalStatus = selectElement.getAttribute('data-original-status');
    const newStatus = selectElement.value;

    const changeKey = id ? id : `${serviceId}_${vehicleId}`;

    if (!id && (!serviceId || !vehicleId)) {
        console.error('Không tìm thấy id hoặc serviceId/vehicleId');
        return;
    }

    if (newStatus === originalStatus) {
        delete statusChanges[changeKey];
    } else {
        statusChanges[changeKey] = {
            id: id,
            serviceId: serviceId,
            vehicleId: vehicleId,
            newStatus: newStatus,
            originalStatus: originalStatus
        };
    }

    updateSaveButtonState();
}

function updateSaveButtonState() {
    const saveBtn = document.querySelector('.btn-save');
    if (saveBtn) {
        const hasChanges = Object.keys(statusChanges).length > 0;
        if (hasChanges) {
            saveBtn.style.opacity = '1';
            saveBtn.style.cursor = 'pointer';
            saveBtn.disabled = false;
            saveBtn.textContent = `Lưu và Đóng (${Object.keys(statusChanges).length} thay đổi)`;
        } else {
            saveBtn.style.opacity = '0.6';
            saveBtn.style.cursor = 'not-allowed';
            saveBtn.disabled = true;
            saveBtn.textContent = 'Lưu và Đóng';
        }
    }
}

async function saveChangesAndClose() {
    const changes = Object.values(statusChanges);

    if (changes.length === 0) {
        closeVehicleDetailModal(true);
        setTimeout(() => {
            window.location.reload();
        }, 100);
        return;
    }

    const saveBtn = document.querySelector('.btn-save');
    if (saveBtn) {
        saveBtn.disabled = true;
        saveBtn.textContent = 'Đang lưu...';
    }

    try {
        const updatePromises = changes.map(change => {
            if (change.id) {
                return fetch(`/admin/vehicle-services/service/${change.id}/status`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({ status: change.newStatus })
                });
            }
            return fetch(`/admin/vehicle-services/service/${change.serviceId}/vehicle/${change.vehicleId}/status`, {
                    method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ status: change.newStatus })
            });
        });

        const responses = await Promise.all(updatePromises);
        const errorResponses = responses.filter(r => !r.ok);
        if (errorResponses.length > 0) {
            const errorTexts = await Promise.all(errorResponses.map(r => r.text()));
            console.error('Lỗi khi lưu:', errorTexts);
            alert('Có lỗi xảy ra khi lưu một số thay đổi. Vui lòng thử lại.\n' + errorTexts.join('\n'));
            if (saveBtn) {
                saveBtn.disabled = false;
                updateSaveButtonState();
            }
            return;
        }

        const results = await Promise.all(responses.map(r => r.json()));
        const failed = results.filter(r => !r.success);
        if (failed.length > 0) {
            const errorMessages = failed.map(r => r.message || 'Lỗi không xác định').join('\n');
            alert('Có lỗi xảy ra khi lưu một số thay đổi:\n' + errorMessages);
            if (saveBtn) {
                saveBtn.disabled = false;
                updateSaveButtonState();
            }
            return;
        }

        statusChanges = {};

        if (saveBtn) {
            saveBtn.textContent = 'Đã lưu thành công!';
            saveBtn.style.background = '#10B981';
            saveBtn.style.color = 'white';
        }

        closeVehicleDetailModal(true);

        console.log(`✅ Đã lưu thành công ${changes.length} thay đổi.`);

        setTimeout(() => {
            window.location.reload();
        }, 300);

    } catch (error) {
        console.error('Lỗi khi lưu thay đổi:', error);
        alert('Đã xảy ra lỗi khi lưu thay đổi: ' + error.message);
        if (saveBtn) {
            saveBtn.disabled = false;
            updateSaveButtonState();
        }
    }
}

function openAddNewServiceModal() {
    console.log('🔵 [openAddNewServiceModal] Function called');
    const modal = document.getElementById('addNewServiceModal');
    if (!modal) {
        console.error('❌ [openAddNewServiceModal] Modal element not found!');
        return;
    }
    
    console.log('🔵 [openAddNewServiceModal] Modal found, showing...');
    modal.style.display = 'block';
    modal.classList.add('show');
    document.body.style.overflow = 'hidden';
    
    // Reset form
    const form = document.getElementById('addNewServiceForm');
    if (form) {
        form.reset();
    }
    
    // Hide message
    const messageDiv = document.getElementById('addNewServiceMessage');
    if (messageDiv) {
        messageDiv.style.display = 'none';
        messageDiv.textContent = '';
    }
    
    console.log('✅ [openAddNewServiceModal] Modal should be visible now');
}

function closeAddNewServiceModal() {
    console.log('🔵 [closeAddNewServiceModal] Function called');
    const modal = document.getElementById('addNewServiceModal');
    if (modal) {
        modal.style.display = 'none';
        modal.classList.remove('show');
        document.body.style.overflow = '';
        
        const form = document.getElementById('addNewServiceForm');
        if (form) {
            form.reset();
        }
        
        const messageDiv = document.getElementById('addNewServiceMessage');
        if (messageDiv) {
            messageDiv.style.display = 'none';
            messageDiv.textContent = '';
        }
    }
}

async function submitAddNewService() {
    const serviceId = document.getElementById('newServiceId').value.trim();
    const serviceName = document.getElementById('newServiceName').value.trim();
    const serviceType = document.getElementById('newServiceType').value;

    if (!serviceId) {
        showAddNewServiceMessage('Vui lòng nhập mã dịch vụ', 'error');
        return;
    }

    if (!serviceName) {
        showAddNewServiceMessage('Vui lòng nhập tên dịch vụ', 'error');
        return;
    }

    if (!serviceType) {
        showAddNewServiceMessage('Vui lòng chọn loại dịch vụ', 'error');
        return;
    }

    const submitBtn = document.querySelector('#addNewServiceModal .btn-save');
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Đang thêm...';
    }

    try {
        const requestData = {
            serviceId: serviceId,
            serviceName: serviceName,
            serviceType: serviceType
        };

        console.log('📡 [ADD NEW SERVICE] Gửi request thêm dịch vụ mới:', requestData);

        const response = await fetch('/admin/vehicle-services/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestData)
        });

        const data = await response.json();

        if (data.success) {
            console.log('✅ [ADD NEW SERVICE] Đã thêm dịch vụ mới thành công');
            showAddNewServiceMessage('Đã thêm dịch vụ mới vào hệ thống thành công!', 'success');

            setTimeout(() => {
                closeAddNewServiceModal();
                window.location.reload();
            }, 1500);
        } else {
            console.error('❌ [ADD NEW SERVICE] Lỗi khi thêm dịch vụ:', data.message);
            showAddNewServiceMessage(data.message || 'Lỗi khi thêm dịch vụ', 'error');
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.textContent = 'Thêm Dịch Vụ';
            }
        }
    } catch (error) {
        console.error('❌ [ADD NEW SERVICE] Lỗi khi thêm dịch vụ:', error);
        showAddNewServiceMessage('Lỗi khi thêm dịch vụ: ' + error.message, 'error');
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Thêm Dịch Vụ';
        }
    }
}

function showAddNewServiceMessage(message, type) {
    const messageDiv = document.getElementById('addNewServiceMessage');
    if (messageDiv) {
        messageDiv.textContent = message;
        messageDiv.className = type === 'success' ? 'alert alert-success' : 'alert alert-danger';
        messageDiv.style.display = 'block';

        if (type === 'success') {
            setTimeout(() => {
                messageDiv.style.display = 'none';
            }, 3000);
        }
    }
}

window.addEventListener('click', function(event) {
    const vehicleDetailModal = document.getElementById('vehicleDetailModal');
    const addNewServiceModal = document.getElementById('addNewServiceModal');

    if (event.target === vehicleDetailModal) {
        closeVehicleDetailModal();
    }

    if (event.target === addNewServiceModal) {
        closeAddNewServiceModal();
    }
});

// Đảm bảo modal hiển thị khi click nút
document.addEventListener('DOMContentLoaded', function() {
    const btnAddNewService = document.getElementById('btnAddNewService');
    if (btnAddNewService) {
        btnAddNewService.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            console.log('🔵 [BUTTON CLICK] Add new service button clicked');
            openAddNewServiceModal();
        });
    }
});

/**
 * Inline status change handler for table select (onchange attribute)
 */
async function handleStatusChangeInline(selectElement) {
    if (!selectElement) {
        return;
    }
    const serviceId = selectElement.getAttribute('data-service-id');
    const vehicleId = selectElement.getAttribute('data-vehicle-id');
    const originalStatus = selectElement.getAttribute('data-original-status');
    const newStatus = selectElement.value;

    if (!serviceId || !vehicleId || serviceId === 'NO_ID') {
        alert('Không tìm thấy thông tin dịch vụ để cập nhật.');
        selectElement.value = originalStatus;
        return;
    }
    if (newStatus === originalStatus) {
        return;
    }
    if (!confirm(`Bạn có chắc muốn đổi trạng thái từ "${originalStatus}" sang "${newStatus}"?`)) {
        selectElement.value = originalStatus;
        return;
    }

    selectElement.disabled = true;
    try {
        const response = await fetch(`/admin/vehicle-services/service/${serviceId}/vehicle/${vehicleId}/status`, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ status: newStatus })
        });
        const data = await response.json().catch(() => ({}));
        if (response.ok && data.success) {
            selectElement.setAttribute('data-original-status', newStatus);
            alert('Đã cập nhật trạng thái thành công!');
            setTimeout(() => window.location.reload(), 800);
        } else {
            alert('Lỗi: ' + (data.message || 'Không thể cập nhật trạng thái'));
            selectElement.value = originalStatus;
        }
    } catch (error) {
        console.error('Lỗi khi cập nhật trạng thái:', error);
        alert('Đã xảy ra lỗi khi cập nhật trạng thái: ' + error.message);
        selectElement.value = originalStatus;
    } finally {
        selectElement.disabled = false;
    }
}

/**
 * Inline delete button handler (data attributes on button)
 */
async function deleteVehicleServiceInline(serviceId, vehicleId) {
    console.log('deleteVehicleServiceInline called with:', { serviceId, vehicleId });
    
    if (!serviceId || !vehicleId || serviceId === 'NO_ID') {
        alert('Không tìm thấy thông tin dịch vụ để xóa.');
        return;
    }
    
    if (!confirm(`Bạn có chắc chắn muốn xóa dịch vụ này không?\n\nService ID: ${serviceId}\nVehicle ID: ${vehicleId}`)) {
        return;
    }

    try {
        console.log('Sending DELETE request to:', `/admin/vehicle-services/service/${serviceId}/vehicle/${vehicleId}`);
        const response = await fetch(`/admin/vehicle-services/service/${encodeURIComponent(serviceId)}/vehicle/${encodeURIComponent(vehicleId)}`, {
            method: 'DELETE',
            headers: {'Content-Type': 'application/json'}
        });
        
        console.log('Response status:', response.status);
        const data = await response.json().catch(() => {
            // Nếu không parse được JSON, thử lấy text
            return response.text().then(text => {
                console.log('Response text:', text);
                return { message: text, success: false };
            });
        });
        
        if (response.ok) {
            // Kiểm tra nếu response là string (success message)
            if (typeof data === 'string') {
                alert('Đã xóa dịch vụ thành công!');
            } else if (data.success !== false) {
                alert('Đã xóa dịch vụ thành công!');
            } else {
                alert('Lỗi: ' + (data.message || 'Không thể xóa dịch vụ'));
                return;
            }
            setTimeout(() => window.location.reload(), 800);
        } else {
            const errorMessage = typeof data === 'string' ? data : (data.message || `HTTP ${response.status}: ${response.statusText}`);
            console.error('Delete failed:', errorMessage);
            alert('Lỗi khi xóa dịch vụ: ' + errorMessage);
        }
    } catch (error) {
        console.error('Lỗi khi xóa dịch vụ:', error);
        alert('Đã xảy ra lỗi khi xóa dịch vụ: ' + error.message);
    }
}

// Expose helpers for inline attributes in template
// Store original function references before assigning to window
const originalHandleStatusChangeInline = handleStatusChangeInline;
const originalDeleteVehicleServiceInline = deleteVehicleServiceInline;
const originalOpenVehicleDetailModal = openVehicleDetailModal;

window.handleStatusChangeInline = function(selectElement) {
    console.log('handleStatusChangeInline called', selectElement);
    return originalHandleStatusChangeInline(selectElement);
};

window.deleteVehicleServiceInline = function(serviceId, vehicleId) {
    console.log('deleteVehicleServiceInline called', serviceId, vehicleId);
    return originalDeleteVehicleServiceInline(serviceId, vehicleId);
};

// Also expose openVehicleDetailModal for debugging
window.openVehicleDetailModal = function(vehicleId) {
    console.log('openVehicleDetailModal called via window', vehicleId);
    return originalOpenVehicleDetailModal(vehicleId);
};

// Expose add new service functions
// Store original function references before assigning to window
const originalOpenAddNewServiceModal = openAddNewServiceModal;
const originalCloseAddNewServiceModal = closeAddNewServiceModal;
const originalSubmitAddNewService = submitAddNewService;

window.openAddNewServiceModal = function() {
    console.log('🔵 [window.openAddNewServiceModal] Called via window');
    try {
        return originalOpenAddNewServiceModal();
    } catch (error) {
        console.error('❌ [window.openAddNewServiceModal] Error:', error);
        throw error;
    }
};

window.closeAddNewServiceModal = function() {
    console.log('closeAddNewServiceModal called');
    return originalCloseAddNewServiceModal();
};

window.submitAddNewService = function() {
    console.log('submitAddNewService called');
    return originalSubmitAddNewService();
};

