// Admin Groups Management Page JavaScript

let currentGroupsData = [];

// Helper function to get JWT token
function getJwtToken() {
    // Try localStorage first
    const token = localStorage.getItem('jwtToken');
    if (token) {
        return token;
    }
    
    // Try cookie
    const cookies = document.cookie.split(';');
    for (let cookie of cookies) {
        const [name, value] = cookie.trim().split('=');
        if (name === 'jwtToken' && value) {
            return value;
        }
    }
    
    return null;
}

// Helper function to get authenticated headers
function getAuthHeaders() {
    const headers = {
        'Content-Type': 'application/json'
    };
    
    const token = getJwtToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    
    return headers;
}

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', function() {
    console.log('Admin Groups page initializing...');
    loadGroups();
    initCreateGroupButton();
    initGroupForm();
});

// Load groups
async function loadGroups() {
    try {
        const response = await fetch('/api/groups');
        if (!response.ok) throw new Error('Failed to fetch groups');
        
        const groups = await response.json();
        currentGroupsData = groups || [];
        
        console.log('Groups loaded:', currentGroupsData);
        renderGroupsGrid(currentGroupsData);
        
    } catch (error) {
        console.error('Error loading groups:', error);
        const grid = document.getElementById('groups-grid');
        if (grid) {
            grid.innerHTML = `
                <div style="text-align: center; padding: 2rem; color: var(--danger);">
                    <i class="fas fa-exclamation-circle"></i> Lỗi khi tải dữ liệu: ${error.message}
                </div>
            `;
        }
    }
}

// Render groups grid
function renderGroupsGrid(groups) {
    const grid = document.getElementById('groups-grid');
    
    if (!grid) return;
    
    if (!groups || groups.length === 0) {
        grid.innerHTML = `
            <div style="text-align: center; padding: 2rem; color: var(--text-light); grid-column: 1 / -1;">
                <i class="fas fa-inbox"></i><br>Không có nhóm nào
            </div>
        `;
        return;
    }
    
    grid.innerHTML = groups.map(group => {
        // Backend trả về "Active" hoặc "Inactive", không phải "ACTIVE" hay "INACTIVE"
        const isActive = group.status === 'Active' || group.status === 'ACTIVE';
        const statusText = isActive ? 'Hoạt động' : 'Không hoạt động';
        const statusColor = isActive ? '#10B981' : '#F59E0B';
        
        return `
        <div class="group-card" style="background: white; border-radius: 8px; padding: 1.5rem; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
            <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 1rem;">
                <div>
                    <h3 style="margin: 0; color: var(--text-primary);">${escapeHtml(group.groupName || `Nhóm #${group.groupId}`)}</h3>
                    <p style="margin: 0.5rem 0 0 0; color: var(--text-light); font-size: 0.875rem;">
                        ID: ${group.groupId} | Xe: ${group.vehicleId || 'Chưa có'}
                    </p>
                </div>
                <span class="status-badge ${isActive ? 'paid' : 'pending'}" 
                      style="background: ${statusColor}; color: white; padding: 0.25rem 0.75rem; border-radius: 12px; font-size: 0.75rem; font-weight: 600;">
                    ${statusText}
                </span>
            </div>
            
            <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem; margin-bottom: 1rem;">
                <div>
                    <div style="color: var(--text-light); font-size: 0.875rem;">Thành viên</div>
                    <div style="font-size: 1.25rem; font-weight: bold; color: var(--text-primary);">
                        ${group.memberCount || 0}
                    </div>
                </div>
                <div>
                    <div style="color: var(--text-light); font-size: 0.875rem;">Tỷ lệ sở hữu</div>
                    <div style="font-size: 1.25rem; font-weight: bold; color: var(--text-primary);">
                        ${group.totalOwnershipPercentage || 0}%
                    </div>
                </div>
            </div>
            
            <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
                <button class="btn btn-sm" style="background: var(--info); color: white; padding: 0.5rem 0.75rem;" 
                        onclick="viewGroupDetail(${group.groupId})" title="Xem chi tiết">
                    <i class="fas fa-eye"></i> Chi tiết
                </button>
                <button class="btn btn-sm" style="background: var(--primary); color: white; padding: 0.5rem 0.75rem;" 
                        onclick="editGroup(${group.groupId})" title="Sửa">
                    <i class="fas fa-edit"></i> Sửa
                </button>
                <button class="btn btn-sm" style="background: var(--danger); color: white; padding: 0.5rem 0.75rem;" 
                        onclick="deleteGroup(${group.groupId})" title="Xóa">
                    <i class="fas fa-trash"></i> Xóa
                </button>
            </div>
        </div>
    `;
    }).join('');
}

// Escape HTML để tránh XSS
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Initialize create group button
function initCreateGroupButton() {
    const btnCreate = document.getElementById('btn-create-group');
    if (btnCreate) {
        btnCreate.addEventListener('click', function() {
            openGroupModal();
        });
    }
}

// Initialize group form
function initGroupForm() {
    const form = document.getElementById('group-form');
    if (form) {
        form.addEventListener('submit', async function(e) {
            e.preventDefault();
            await saveGroup();
        });
    }
}

// Open group modal for create/edit
function openGroupModal(groupId = null) {
    const modal = document.getElementById('group-modal');
    const title = document.getElementById('group-modal-title');
    const form = document.getElementById('group-form');
    
    if (!modal || !title || !form) {
        console.error('Group modal elements not found');
        return;
    }
    
    // Reset form
    form.reset();
    document.getElementById('group-id').value = '';
    
    // Show/hide vehicle fields based on create/edit mode
    const vehicleSection = document.getElementById('vehicle-section');
    const vehicleNumberField = document.getElementById('vehicle-number');
    const vehicleTypeField = document.getElementById('vehicle-type');
    const vehicleNumberRequired = document.getElementById('vehicle-number-required');
    const vehicleTypeRequired = document.getElementById('vehicle-type-required');
    const vehicleNumberHint = document.getElementById('vehicle-number-hint');
    const vehicleTypeHint = document.getElementById('vehicle-type-hint');
    
    const vehicleIdGroup = document.getElementById('vehicle-id-group');
    const vehicleIdDisplay = document.getElementById('vehicle-id-display');
    const vehicleIdRequired = document.getElementById('vehicle-id-required');
    const vehicleIdHint = document.getElementById('vehicle-id-hint');
    
    if (groupId) {
        title.textContent = 'Chỉnh sửa nhóm';
        // Show vehicle fields when editing (cho phép chỉnh sửa thông tin xe)
        if (vehicleSection) {
            vehicleSection.style.display = 'block';
        }
        // Vehicle fields không bắt buộc khi chỉnh sửa (nhưng có thể chỉnh sửa)
        if (vehicleNumberField) {
            vehicleNumberField.removeAttribute('required');
        }
        if (vehicleTypeField) {
            vehicleTypeField.removeAttribute('required');
        }
        if (vehicleIdDisplay) {
            vehicleIdDisplay.removeAttribute('required');
            vehicleIdDisplay.readOnly = false;
            vehicleIdDisplay.style.backgroundColor = '';
            vehicleIdDisplay.style.cursor = '';
        }
        // Ẩn dấu * và cập nhật hint text
        if (vehicleNumberRequired) vehicleNumberRequired.style.display = 'none';
        if (vehicleTypeRequired) vehicleTypeRequired.style.display = 'none';
        if (vehicleIdRequired) vehicleIdRequired.style.display = 'none';
        if (vehicleNumberHint) vehicleNumberHint.textContent = 'Biển số xe (tùy chọn khi chỉnh sửa)';
        if (vehicleTypeHint) vehicleTypeHint.textContent = 'Loại xe (tùy chọn khi chỉnh sửa)';
        if (vehicleIdHint) vehicleIdHint.textContent = 'Mã xe (có thể chỉnh sửa)';
        // Hiển thị vehicle ID group khi chỉnh sửa (sẽ load mã xe nếu có)
        if (vehicleIdGroup) vehicleIdGroup.style.display = 'block';
        if (vehicleIdDisplay) {
            vehicleIdDisplay.value = '';
            vehicleIdDisplay.placeholder = '';
        }
        loadGroupForEdit(groupId);
    } else {
        title.textContent = 'Tạo nhóm mới';
        // Show vehicle fields when creating
        if (vehicleSection) {
            vehicleSection.style.display = 'block';
        }
        // Vehicle fields bắt buộc khi tạo mới
        if (vehicleIdDisplay) {
            vehicleIdDisplay.setAttribute('required', 'required');
            vehicleIdDisplay.readOnly = false;
            vehicleIdDisplay.style.backgroundColor = '';
            vehicleIdDisplay.style.cursor = '';
            vehicleIdDisplay.value = '';
            vehicleIdDisplay.placeholder = '';
        }
        if (vehicleNumberField) {
            vehicleNumberField.setAttribute('required', 'required');
            vehicleNumberField.value = '';
        }
        if (vehicleTypeField) {
            vehicleTypeField.setAttribute('required', 'required');
            vehicleTypeField.value = '';
        }
        // Hiển thị dấu * và cập nhật hint text
        if (vehicleIdRequired) vehicleIdRequired.style.display = 'inline';
        if (vehicleNumberRequired) vehicleNumberRequired.style.display = 'inline';
        if (vehicleTypeRequired) vehicleTypeRequired.style.display = 'inline';
        if (vehicleIdHint) vehicleIdHint.textContent = 'Mã xe duy nhất (bắt buộc khi tạo mới)';
        if (vehicleNumberHint) vehicleNumberHint.textContent = 'Biển số xe duy nhất (bắt buộc khi tạo mới)';
        if (vehicleTypeHint) vehicleTypeHint.textContent = 'Loại xe (bắt buộc khi tạo mới)';
        // Hiển thị vehicle ID group khi tạo mới
        if (vehicleIdGroup) vehicleIdGroup.style.display = 'block';
    }
    
    modal.classList.add('active');
    document.getElementById('modal-overlay').classList.add('active');
}

// Load group data for editing
async function loadGroupForEdit(groupId) {
    try {
        const response = await fetch(`/api/groups/${groupId}`);
        if (!response.ok) throw new Error('Failed to fetch group');
        
        const group = await response.json();
        
        // Set group basic info
        document.getElementById('group-id').value = group.groupId || '';
        document.getElementById('group-name').value = group.groupName || '';
        
        // Admin ID (optional)
        const adminIdField = document.getElementById('group-admin');
        if (adminIdField) {
            adminIdField.value = group.adminId || '';
        }
        
        // Set status - backend trả về "Active" hoặc "Inactive"
        const statusSelect = document.getElementById('group-status');
        if (statusSelect) {
            const status = group.status || 'Active';
            statusSelect.value = status === 'Active' || status === 'ACTIVE' ? 'Active' : 'Inactive';
        }
        
        // Load vehicle information if available
        try {
            const vehiclesResponse = await fetch(`http://localhost:8084/api/vehicle-groups/${groupId}/vehicles`, {
                credentials: 'include'
            });
            
            const vehicleIdGroup = document.getElementById('vehicle-id-group');
            const vehicleIdDisplay = document.getElementById('vehicle-id-display');
            const vehicleNumberField = document.getElementById('vehicle-number');
            const vehicleTypeField = document.getElementById('vehicle-type');
            
            if (vehiclesResponse.ok) {
                const vehicles = await vehiclesResponse.json();
                
                // Load first vehicle's info (nếu có)
                if (Array.isArray(vehicles) && vehicles.length > 0) {
                    const firstVehicle = vehicles[0];
                    
                    // Hiển thị mã xe nếu có (có thể chỉnh sửa)
                    if (firstVehicle.vehicleId) {
                        if (vehicleIdGroup) vehicleIdGroup.style.display = 'block';
                        if (vehicleIdDisplay) {
                            vehicleIdDisplay.value = firstVehicle.vehicleId;
                            vehicleIdDisplay.placeholder = '';
                            vehicleIdDisplay.readOnly = false;
                            vehicleIdDisplay.style.backgroundColor = '';
                            vehicleIdDisplay.style.cursor = '';
                            // Lưu vehicleId cũ để so sánh khi lưu
                            vehicleIdDisplay.dataset.oldVehicleId = firstVehicle.vehicleId;
                        }
                    } else {
                        if (vehicleIdGroup) vehicleIdGroup.style.display = 'block';
                        if (vehicleIdDisplay) {
                            vehicleIdDisplay.value = '';
                            vehicleIdDisplay.placeholder = '';
                            vehicleIdDisplay.readOnly = false;
                            vehicleIdDisplay.style.backgroundColor = '';
                            vehicleIdDisplay.style.cursor = '';
                            vehicleIdDisplay.dataset.oldVehicleId = '';
                        }
                    }
                    
                    // Load thông tin xe khác
                    if (vehicleNumberField && firstVehicle.vehicleNumber) {
                        vehicleNumberField.value = firstVehicle.vehicleNumber || '';
                    }
                    if (vehicleTypeField && firstVehicle.vehicleType) {
                        vehicleTypeField.value = firstVehicle.vehicleType || '';
                    }
                } else {
                    // Không có xe, ẩn mã xe và để trống các trường khác
                    if (vehicleIdGroup) vehicleIdGroup.style.display = 'none';
                    if (vehicleIdDisplay) vehicleIdDisplay.value = '';
                    if (vehicleNumberField) vehicleNumberField.value = '';
                    if (vehicleTypeField) vehicleTypeField.value = '';
                }
            } else {
                // Không thể load thông tin xe, ẩn mã xe và để trống
                if (vehicleIdGroup) vehicleIdGroup.style.display = 'none';
                if (vehicleIdDisplay) vehicleIdDisplay.value = '';
                if (vehicleNumberField) vehicleNumberField.value = '';
                if (vehicleTypeField) vehicleTypeField.value = '';
            }
        } catch (vehicleError) {
            console.warn('Could not load vehicle info:', vehicleError);
            // Không bắt buộc phải có thông tin xe khi chỉnh sửa
            const vehicleIdGroup = document.getElementById('vehicle-id-group');
            const vehicleIdDisplay = document.getElementById('vehicle-id-display');
            const vehicleNumberField = document.getElementById('vehicle-number');
            const vehicleTypeField = document.getElementById('vehicle-type');
            if (vehicleIdGroup) vehicleIdGroup.style.display = 'none';
            if (vehicleIdDisplay) vehicleIdDisplay.value = '';
            if (vehicleNumberField) vehicleNumberField.value = '';
            if (vehicleTypeField) vehicleTypeField.value = '';
        }
        
    } catch (error) {
        console.error('Error loading group for edit:', error);
        alert('Lỗi khi tải thông tin nhóm: ' + error.message);
    }
}

// Save group (create or update)
async function saveGroup() {
    const groupId = document.getElementById('group-id').value;
    const groupName = document.getElementById('group-name').value;
    const adminId = document.getElementById('group-admin')?.value?.trim();
    const status = document.getElementById('group-status').value;
    
    // Vehicle fields (only required when creating new group)
    const vehicleId = document.getElementById('vehicle-id-display')?.value?.trim();
    const vehicleNumber = document.getElementById('vehicle-number')?.value?.trim();
    const vehicleType = document.getElementById('vehicle-type')?.value;
    
    if (!groupName) {
        alert('Vui lòng điền tên nhóm');
        return;
    }
    
    // When creating new group, vehicle ID, number and type are required
    if (!groupId) {
        if (!vehicleId || !vehicleNumber || !vehicleType) {
            alert('Vui lòng điền đầy đủ thông tin xe (Mã xe, Biển số xe và Loại xe)');
            return;
        }
    }
    
    const groupData = {
        groupName: groupName,
        status: status
    };
    
    // Chỉ thêm adminId nếu có giá trị
    if (adminId && adminId !== '') {
        groupData.adminId = parseInt(adminId);
    }
    
    try {
        let response;
        if (groupId) {
            // Update existing group
            response = await fetch(`/api/groups/${groupId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(groupData)
            });
            
            if (response.ok) {
                // Nếu có thông tin xe được nhập, cập nhật thông tin xe
                const vehicleIdDisplay = document.getElementById('vehicle-id-display');
                const newVehicleId = vehicleIdDisplay?.value?.trim();
                const oldVehicleId = vehicleIdDisplay?.dataset?.oldVehicleId;
                
                if (newVehicleId && (vehicleNumber || vehicleType)) {
                    try {
                        // Nếu vehicleId thay đổi, cần xử lý đặc biệt
                        if (oldVehicleId && oldVehicleId !== newVehicleId) {
                            // VehicleId đã thay đổi - cập nhật vehicle với vehicleId mới
                            // Nếu vehicle với vehicleId mới chưa tồn tại, tạo mới
                            const vehicleUpdateData = {
                                vehicleNumber: vehicleNumber || '',
                                vehicleType: vehicleType || '',
                                status: 'ready'
                            };
                            
                            // Thử cập nhật vehicle với vehicleId mới
                            const vehicleUpdateResponse = await fetch(`http://localhost:8084/api/vehicles/${newVehicleId}`, {
                                method: 'PUT',
                                headers: getAuthHeaders(),
                                credentials: 'include',
                                body: JSON.stringify(vehicleUpdateData)
                            });
                            
                            if (!vehicleUpdateResponse.ok) {
                                // Nếu không tìm thấy vehicle với vehicleId mới, tạo mới
                                const vehicleGroupData = {
                                    groupId: groupId.toString(),
                                    vehicles: [{
                                        vehicleId: newVehicleId,
                                        vehicleNumber: vehicleNumber || '',
                                        vehicleType: vehicleType || '',
                                        status: 'ready'
                                    }]
                                };
                                
                                const vehicleCreateResponse = await fetch('http://localhost:8084/api/vehicles/batch', {
                                    method: 'POST',
                                    headers: getAuthHeaders(),
                                    credentials: 'include',
                                    body: JSON.stringify(vehicleGroupData)
                                });
                                
                                if (vehicleCreateResponse.ok) {
                                    console.log('✅ New vehicle created with new vehicleId');
                                }
                            } else {
                                console.log('✅ Vehicle updated with new vehicleId');
                            }
                        } else {
                            // VehicleId không thay đổi hoặc chưa có vehicleId cũ, chỉ cập nhật thông tin
                            const vehicleUpdateData = {};
                            if (vehicleNumber) vehicleUpdateData.vehicleNumber = vehicleNumber;
                            if (vehicleType) vehicleUpdateData.vehicleType = vehicleType;
                            
                            const vehicleUpdateResponse = await fetch(`http://localhost:8084/api/vehicles/${newVehicleId}`, {
                                method: 'PUT',
                                headers: getAuthHeaders(),
                                credentials: 'include',
                                body: JSON.stringify(vehicleUpdateData)
                            });
                            
                            if (vehicleUpdateResponse.ok) {
                                console.log('✅ Vehicle updated successfully');
                            } else {
                                const errorText = await vehicleUpdateResponse.text();
                                console.warn('⚠️ Could not update vehicle:', errorText);
                            }
                        }
                    } catch (vehicleError) {
                        console.warn('⚠️ Error updating vehicle:', vehicleError);
                        // Không fail toàn bộ nếu cập nhật xe thất bại
                    }
                }
                
                alert('Cập nhật nhóm thành công!');
                closeGroupModal();
                loadGroups();
            } else {
                const errorData = await response.json().catch(() => ({}));
                alert('Lỗi: ' + (errorData.message || errorData.error || 'Không thể cập nhật nhóm'));
            }
        } else {
            // Create new group
            response = await fetch('/api/groups', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(groupData)
            });
            
            if (response.ok) {
                const createdGroup = await response.json();
                const newGroupId = createdGroup.groupId || createdGroup.id;
                
                console.log('✅ Group created with ID:', newGroupId);
                
                // Automatically create vehicle with the new group ID
                if (newGroupId && vehicleId && vehicleNumber && vehicleType) {
                    try {
                        // Step 1: Create Vehiclegroup in vehicle_management database
                        const vehicleGroupData = {
                            groupId: newGroupId.toString(),
                            name: groupName,
                            description: `Nhóm đồng sở hữu: ${groupName}`
                        };
                        
                        console.log('📡 Creating vehicle group with data:', vehicleGroupData);
                        
                        const vehicleGroupResponse = await fetch('http://localhost:8084/api/vehicle-groups', {
                            method: 'POST',
                            headers: getAuthHeaders(),
                            credentials: 'include',
                            body: JSON.stringify(vehicleGroupData)
                        });
                        
                        if (!vehicleGroupResponse.ok) {
                            const contentType = vehicleGroupResponse.headers.get('content-type');
                            let errorMessage = 'Không thể tạo nhóm xe';
                            
                            if (contentType && contentType.includes('application/json')) {
                                const error = await vehicleGroupResponse.json();
                                errorMessage = error.message || error.error || errorMessage;
                            } else {
                                const textResponse = await vehicleGroupResponse.text();
                                errorMessage = textResponse.substring(0, 200) || errorMessage;
                            }
                            
                            // If group already exists, continue (it's OK)
                            if (!errorMessage.includes('đã tồn tại') && !errorMessage.includes('already exists')) {
                                throw new Error(errorMessage);
                            } else {
                                console.log('⚠️ Vehicle group already exists, continuing...');
                            }
                        } else {
                            const createdVehicleGroup = await vehicleGroupResponse.json();
                            console.log('✅ Vehicle group created:', createdVehicleGroup);
                        }
                        
                        // Step 2: Create vehicle with the vehicle group ID (gửi vehicleId do người dùng nhập)
                        const vehicleData = {
                            groupId: newGroupId.toString(),
                            vehicles: [{
                                vehicleId: vehicleId, // Gửi vehicleId do người dùng nhập
                                vehicleNumber: vehicleNumber,
                                vehicleType: vehicleType,
                                status: 'ready' // Default status
                            }]
                        };
                        
                        console.log('📡 Creating vehicle with data:', vehicleData);
                        
                        const vehicleResponse = await fetch('http://localhost:8084/api/vehicles/batch', {
                            method: 'POST',
                            headers: getAuthHeaders(),
                            credentials: 'include',
                            body: JSON.stringify(vehicleData)
                        });
                        
                        console.log('📡 Vehicle API response status:', vehicleResponse.status);
                        
                        if (vehicleResponse.ok) {
                            let createdVehicleData;
                            const contentType = vehicleResponse.headers.get('content-type');
                            if (contentType && contentType.includes('application/json')) {
                                createdVehicleData = await vehicleResponse.json();
                                console.log('✅ Vehicle created:', createdVehicleData);
                            } else {
                                const textResponse = await vehicleResponse.text();
                                console.log('⚠️ Vehicle API returned non-JSON:', textResponse.substring(0, 200));
                            }
                            
                            // Step 3: Update Group to add vehicleId (auto-generated by backend)
                            if (createdVehicleData) {
                                const vehicles = Array.isArray(createdVehicleData) ? createdVehicleData : [createdVehicleData];
                                if (vehicles.length > 0) {
                                    const firstVehicle = vehicles[0];
                                    // Get vehicleId - could be vehicleId or vehicle_id
                                    const createdVehicleId = firstVehicle.vehicleId || firstVehicle.vehicle_id;
                                    
                                    if (createdVehicleId) {
                                        try {
                                            console.log('📡 Updating group with auto-generated vehicleId:', createdVehicleId);
                                            console.log('📡 Update URL: /api/groups/' + newGroupId);
                                            console.log('📡 Update payload:', { vehicleId: createdVehicleId });
                                            
                                            const updateGroupResponse = await fetch(`/api/groups/${newGroupId}`, {
                                                method: 'PUT',
                                                headers: getAuthHeaders(),
                                                credentials: 'include',
                                                body: JSON.stringify({ vehicleId: createdVehicleId })
                                            });
                                            
                                            console.log('📡 Update response status:', updateGroupResponse.status);
                                            console.log('📡 Update response headers:', updateGroupResponse.headers);
                                            
                                            if (updateGroupResponse.ok) {
                                                const updatedGroup = await updateGroupResponse.json();
                                                console.log('✅ Group updated with vehicleId:', createdVehicleId, updatedGroup);
                                                alert('Tạo nhóm và xe thành công!');
                                            } else {
                                                const statusText = updateGroupResponse.statusText || 'Unknown';
                                                const contentType = updateGroupResponse.headers.get('content-type');
                                                let errorMessage = `HTTP ${updateGroupResponse.status}: ${statusText}`;
                                                
                                                if (contentType && contentType.includes('application/json')) {
                                                    try {
                                                        const updateError = await updateGroupResponse.json();
                                                        errorMessage = updateError.message || updateError.error || errorMessage;
                                                    } catch (e) {
                                                        const textResponse = await updateGroupResponse.text();
                                                        errorMessage = textResponse.substring(0, 200) || errorMessage;
                                                    }
                                                } else {
                                                    const textResponse = await updateGroupResponse.text();
                                                    errorMessage = textResponse.substring(0, 200) || errorMessage;
                                                }
                                                
                                                console.error('❌ Error updating group with vehicleId:', {
                                                    status: updateGroupResponse.status,
                                                    statusText: statusText,
                                                    message: errorMessage
                                                });
                                                alert('Tạo nhóm và xe thành công nhưng không thể cập nhật nhóm với ID xe: ' + errorMessage);
                                            }
                                        } catch (updateError) {
                                            console.error('❌ Exception updating group with vehicleId:', updateError);
                                            alert('Tạo nhóm và xe thành công nhưng không thể cập nhật nhóm với ID xe: ' + (updateError.message || 'Lỗi không xác định'));
                                        }
                                    } else {
                                        console.warn('⚠️ Vehicle created but no vehicleId in response');
                                        alert('Tạo nhóm và xe thành công nhưng không thể lấy ID xe đã tạo.');
                                    }
                                } else {
                                    console.warn('⚠️ Vehicle created but response is empty array');
                                    alert('Tạo nhóm và xe thành công nhưng không có dữ liệu xe được trả về.');
                                }
                            } else {
                                console.warn('⚠️ Vehicle created but no data in response');
                                alert('Tạo nhóm và xe thành công nhưng không có dữ liệu xe được trả về.');
                            }
                        } else {
                            const contentType = vehicleResponse.headers.get('content-type');
                            let errorMessage = 'Không thể tạo xe';
                            
                            if (contentType && contentType.includes('application/json')) {
                                const vehicleError = await vehicleResponse.json();
                                errorMessage = vehicleError.message || vehicleError.error || errorMessage;
                            } else {
                                const textResponse = await vehicleResponse.text();
                                errorMessage = textResponse.substring(0, 200) || errorMessage;
                            }
                            
                            console.error('❌ Error creating vehicle:', errorMessage);
                            alert('Tạo nhóm thành công nhưng không thể tạo xe: ' + errorMessage);
                        }
                    } catch (vehicleError) {
                        console.error('❌ Error creating vehicle:', vehicleError);
                        alert('Tạo nhóm thành công nhưng không thể tạo xe: ' + vehicleError.message);
                    }
                } else {
                    alert('Tạo nhóm thành công!');
                }
                
            closeGroupModal();
            loadGroups();
        } else {
            const errorData = await response.json().catch(() => ({}));
                alert('Lỗi: ' + (errorData.message || errorData.error || 'Không thể tạo nhóm'));
            }
        }
    } catch (error) {
        console.error('Error saving group:', error);
        alert('Lỗi khi lưu nhóm: ' + error.message);
    }
}

// Close group modal
function closeGroupModal() {
    const modal = document.getElementById('group-modal');
    const overlay = document.getElementById('modal-overlay');
    
    if (modal) modal.classList.remove('active');
    if (overlay) overlay.classList.remove('active');
}

// View group detail
async function viewGroupDetail(groupId) {
    const modal = document.getElementById('group-detail-modal');
    const title = document.getElementById('group-detail-title');
    const content = document.getElementById('group-detail-content');
    
    if (!modal || !title || !content) {
        console.error('Group detail modal elements not found');
        return;
    }
    
    try {
        // Load group data
        const groupResponse = await fetch(`/api/groups/${groupId}`);
        if (!groupResponse.ok) throw new Error('Failed to fetch group');
        const group = await groupResponse.json();
        
        // Load members
        const membersResponse = await fetch(`/api/groups/${groupId}/members`);
        const members = membersResponse.ok ? await membersResponse.json() : [];
        
        // Load vehicle information
        let vehicles = [];
        try {
            const vehiclesResponse = await fetch(`http://localhost:8084/api/vehicle-groups/${groupId}/vehicles`, {
                credentials: 'include'
            });
            if (vehiclesResponse.ok) {
                vehicles = await vehiclesResponse.json();
            }
        } catch (vehicleError) {
            console.warn('Could not load vehicle info:', vehicleError);
        }
        
        const isActive = group.status === 'Active' || group.status === 'ACTIVE';
        const statusText = isActive ? 'Hoạt động' : 'Không hoạt động';
        const statusColor = isActive ? '#10B981' : '#F59E0B';
        
        // Calculate total ownership
        const totalOwnership = members.reduce((sum, m) => sum + (parseFloat(m.ownershipPercent) || 0), 0);
        
        // Render detail content
        content.innerHTML = `
            <div style="display: grid; gap: 1.5rem;">
                <div style="background: var(--light); padding: 1.5rem; border-radius: 8px;">
                    <h4 style="margin: 0 0 1rem 0; color: var(--primary);">
                        <i class="fas fa-info-circle"></i> Thông tin nhóm
                    </h4>
                    <div style="display: grid; gap: 1rem;">
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <strong>ID nhóm:</strong>
                            <span>${group.groupId}</span>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <strong>Tên nhóm:</strong>
                            <span>${escapeHtml(group.groupName || 'N/A')}</span>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <strong>Admin ID:</strong>
                            <span>${group.adminId || 'Chưa có'}</span>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <strong>Trạng thái:</strong>
                            <span style="background: ${statusColor}; color: white; padding: 0.25rem 0.75rem; border-radius: 12px; font-size: 0.875rem;">
                                ${statusText}
                            </span>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <strong>Ngày tạo:</strong>
                            <span>${group.createdAt ? new Date(group.createdAt).toLocaleString('vi-VN') : 'N/A'}</span>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <strong>Số thành viên:</strong>
                            <span>${members.length}</span>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <strong>Tỷ lệ sở hữu tổng:</strong>
                            <span>${totalOwnership.toFixed(1)}%</span>
                        </div>
                    </div>
                </div>
                
                <div style="background: var(--light); padding: 1.5rem; border-radius: 8px;">
                    <h4 style="margin: 0 0 1rem 0; color: var(--primary);">
                        <i class="fas fa-car"></i> Thông tin xe
                    </h4>
                    ${vehicles.length > 0 ? `
                        <div style="display: grid; gap: 0.75rem;">
                            ${vehicles.map(vehicle => `
                                <div style="padding: 1rem; background: white; border-radius: 4px; border-left: 4px solid var(--primary);">
                                    <div style="display: grid; gap: 0.5rem;">
                                        <div style="display: flex; justify-content: space-between; align-items: center;">
                                            <strong>Mã xe:</strong>
                                            <span>${vehicle.vehicleId || 'N/A'}</span>
                                        </div>
                                        <div style="display: flex; justify-content: space-between; align-items: center;">
                                            <strong>Biển số xe:</strong>
                                            <span>${escapeHtml(vehicle.vehicleNumber || 'N/A')}</span>
                                        </div>
                                        <div style="display: flex; justify-content: space-between; align-items: center;">
                                            <strong>Loại xe:</strong>
                                            <span>${escapeHtml(vehicle.vehicleType || 'N/A')}</span>
                                        </div>
                                        <div style="display: flex; justify-content: space-between; align-items: center;">
                                            <strong>Trạng thái:</strong>
                                            <span style="background: ${vehicle.status === 'ready' ? '#10B981' : '#F59E0B'}; color: white; padding: 0.25rem 0.75rem; border-radius: 12px; font-size: 0.875rem;">
                                                ${vehicle.status === 'ready' ? 'Sẵn sàng' : vehicle.status || 'N/A'}
                                            </span>
                                        </div>
                                    </div>
                                </div>
                            `).join('')}
                        </div>
                    ` : '<p style="color: var(--text-light);">Chưa có thông tin xe</p>'}
                </div>
                
                <div style="background: var(--light); padding: 1.5rem; border-radius: 8px;">
                    <h4 style="margin: 0 0 1rem 0; color: var(--primary);">
                        <i class="fas fa-users"></i> Danh sách thành viên
                    </h4>
                    ${members.length > 0 ? `
                        <div style="display: grid; gap: 0.75rem;">
                            ${members.map(member => `
                                <div style="display: flex; justify-content: space-between; align-items: center; padding: 0.75rem; background: white; border-radius: 4px;">
                                    <div>
                                        <strong>User ID: ${member.userId}</strong>
                                        <div style="font-size: 0.875rem; color: var(--text-light); margin-top: 0.25rem;">
                                            <span style="background: ${member.role === 'Admin' ? '#3B82F6' : '#6B7280'}; color: white; padding: 0.125rem 0.5rem; border-radius: 8px; font-size: 0.75rem; margin-right: 0.5rem;">
                                                ${member.role || 'Member'}
                                            </span>
                                            Sở hữu: <strong>${member.ownershipPercent || 0}%</strong>
                                        </div>
                                    </div>
                                </div>
                            `).join('')}
                        </div>
                    ` : '<p style="color: var(--text-light);">Chưa có thành viên nào</p>'}
                </div>
            </div>
        `;
        
        title.textContent = `Chi tiết nhóm: ${escapeHtml(group.groupName || `#${group.groupId}`)}`;
        modal.classList.add('active');
        document.getElementById('modal-overlay').classList.add('active');
        
    } catch (error) {
        console.error('Error loading group detail:', error);
        alert('Lỗi khi tải chi tiết nhóm: ' + error.message);
    }
}

// Close group detail modal
function closeGroupDetailModal() {
    const modal = document.getElementById('group-detail-modal');
    const overlay = document.getElementById('modal-overlay');
    
    if (modal) modal.classList.remove('active');
    if (overlay) overlay.classList.remove('active');
}

// Edit group
function editGroup(groupId) {
    openGroupModal(groupId);
}

// Delete group
async function deleteGroup(groupId) {
    if (!confirm(`Bạn có chắc chắn muốn xóa nhóm #${groupId}?`)) {
        return;
    }
    
    try {
        const response = await fetch(`/api/groups/${groupId}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            alert('Xóa nhóm thành công!');
            loadGroups();
        } else {
            alert('Lỗi khi xóa nhóm');
        }
    } catch (error) {
        console.error('Error deleting group:', error);
        alert('Lỗi khi xóa nhóm: ' + error.message);
    }
}

// Close modals when clicking overlay
document.addEventListener('DOMContentLoaded', function() {
    const overlay = document.getElementById('modal-overlay');
    if (overlay) {
        overlay.addEventListener('click', function() {
            closeGroupModal();
            closeGroupDetailModal();
        });
    }
});
