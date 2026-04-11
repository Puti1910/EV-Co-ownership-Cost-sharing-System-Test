document.addEventListener('DOMContentLoaded', function () {
    console.log('Staff script loaded');
    
    const editModal = document.getElementById('editGroupModal');
    const editForm = document.getElementById('editGroupForm');
    const closeModal = editModal?.querySelector('.close-modal');
    const cancelBtn = document.getElementById('cancelEditBtn');
    const updateStatusMessage = document.getElementById('updateStatusMessage');

    // Kiểm tra xem các element có tồn tại không
    if (!editModal) {
        console.error('Modal không tìm thấy!');
        return;
    }

    console.log('Modal found:', editModal);
    console.log('Edit buttons found:', document.querySelectorAll('.btn-edit-group').length);

    // Biến lưu groupId hiện tại khi mở modal
    let currentGroupIdInModal = '';

    // Mở modal sửa khi click nút Sửa
    const editButtons = document.querySelectorAll('.btn-edit-group');
    console.log('Số lượng nút Sửa:', editButtons.length);
    
    editButtons.forEach((btn, index) => {
        console.log(`Đăng ký event cho nút Sửa ${index + 1}`);
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            console.log('Nút Sửa được click!');
            
            const groupId = this.getAttribute('data-group-id');
            const groupName = this.getAttribute('data-group-name');
            const active = this.getAttribute('data-active');
            const description = this.getAttribute('data-description') || '';
            
            console.log('Dữ liệu nhóm xe:', { groupId, groupName, active, description });
            
            // Lưu groupId
            currentGroupIdInModal = groupId || '';
            console.log('🔹 Lưu groupId:', currentGroupIdInModal);
            
            // Điền dữ liệu vào form
            const editGroupId = document.getElementById('editGroupId');
            const editGroupName = document.getElementById('editGroupName');
            const editActive = document.getElementById('editActive');
            const editDescription = document.getElementById('editDescription');
            
            if (editGroupId) editGroupId.value = groupId || '';
            if (editGroupName) editGroupName.value = groupName || '';
            if (editActive) editActive.value = active || 'active';
            if (editDescription) editDescription.value = description || '';
            
            if (editModal) {
                editModal.classList.add('show');
                editModal.style.display = 'block';
                console.log('Modal đã được mở');
            } else {
                console.error('Không thể mở modal - editModal không tồn tại');
            }
        });
    });

    // Hàm đóng modal
    function closeEditModal() {
        if (editModal) {
            editModal.classList.remove('show');
            editModal.style.display = 'none';
        }
    }

    // Đóng modal khi click nút X hoặc Hủy
    if (closeModal) {
        closeModal.addEventListener('click', function () {
            closeEditModal();
        });
    }

    if (cancelBtn) {
        cancelBtn.addEventListener('click', function () {
            closeEditModal();
        });
    }

    // Đóng modal khi click bên ngoài modal
    if (editModal) {
        window.addEventListener('click', function (event) {
            if (event.target === editModal) {
                closeEditModal();
            }
        });
    }

    // Xử lý submit form sửa
    if (editForm) {
        editForm.addEventListener('submit', function (e) {
            e.preventDefault();
            
            const groupId = document.getElementById('editGroupId').value;
            const groupName = document.getElementById('editGroupName').value.trim();
            const active = document.getElementById('editActive').value;
            const description = document.getElementById('editDescription').value.trim();
            
            // Tạo dữ liệu nhóm xe (không có vehicleCount nữa)
            const groupData = {
                name: groupName,
                active: active,
                description: description
            };

            // Gọi API để cập nhật nhóm xe
            fetch(`http://localhost:8084/api/vehicle-groups/${groupId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(groupData)
            })
            .then(response => {
                if (response.ok) {
                    return response.json();
                } else {
                    return response.text().then(text => {
                        throw new Error(text);
                    });
                }
            })
            .then(data => {
                // Hiển thị thông báo thành công
                showUpdateMessage('Nhóm xe đã được cập nhật thành công!', 'success');
                // Đóng modal
                if (editModal) {
                    editModal.classList.remove('show');
                    editModal.style.display = 'none';
                }
                // Reload trang sau 1 giây
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            })
            .catch(error => {
                // Hiển thị thông báo lỗi
                showUpdateMessage('Lỗi khi cập nhật nhóm xe: ' + error.message, 'error');
            });
        });
    }

    // Hàm hiển thị thông báo
    function showUpdateMessage(message, type) {
        updateStatusMessage.textContent = message;
        updateStatusMessage.className = type === 'success' ? 'alert alert-success' : 'alert alert-danger';
        updateStatusMessage.style.display = 'block';
        
        // Tự động ẩn sau 5 giây
        setTimeout(() => {
            updateStatusMessage.style.display = 'none';
        }, 5000);
    }

    // Xử lý modal thêm xe vào nhóm
    const addVehiclesModal = document.getElementById('addVehiclesModal');
    const addVehiclesForm = document.getElementById('addVehiclesForm');
    const cancelAddVehiclesBtn = document.getElementById('cancelAddVehiclesBtn');
    const addVehiclesModalClose = addVehiclesModal?.querySelector('.close-modal');

    // Hàm đóng modal thêm xe
    function closeAddVehiclesModal() {
        if (addVehiclesModal) {
            addVehiclesModal.classList.remove('show');
            addVehiclesModal.style.display = 'none';
        }
    }

    // Đóng modal khi click nút X hoặc Hủy
    if (addVehiclesModalClose) {
        addVehiclesModalClose.addEventListener('click', function() {
            closeAddVehiclesModal();
        });
    }

    if (cancelAddVehiclesBtn) {
        cancelAddVehiclesBtn.addEventListener('click', function() {
            closeAddVehiclesModal();
        });
    }

    // Đóng modal khi click bên ngoài modal
    if (addVehiclesModal) {
        window.addEventListener('click', function (event) {
            if (event.target === addVehiclesModal) {
                closeAddVehiclesModal();
            }
        });
    }

    // Xử lý submit form thêm xe
    if (addVehiclesForm) {
        addVehiclesForm.addEventListener('submit', function(e) {
            e.preventDefault();
            e.stopPropagation();
            
            console.log('🔹 Form submit thêm xe được trigger');
            
            const groupId = document.getElementById('addVehiclesGroupId').value;
            const vehicleRows = document.querySelectorAll('#addVehiclesContainer .vehicle-row');
            const vehicles = [];
            
            vehicleRows.forEach(function(row) {
                const vehicleId = row.querySelector('.vehicle-id-input')?.value.trim();
                const vehicleType = row.querySelector('.vehicle-type-input')?.value.trim();
                const vehicleNumber = row.querySelector('.vehicle-number-input')?.value.trim();
                const status = row.querySelector('.vehicle-status-input')?.value;
                
                if (vehicleId && vehicleType && vehicleNumber) {
                    vehicles.push({
                        vehicleId: vehicleId,
                        type: vehicleType,
                        vehicleNumber: vehicleNumber,
                        status: status || 'available'
                    });
                }
            });
            
            if (vehicles.length === 0) {
                alert('Vui lòng nhập ít nhất một xe!');
                return false;
            }
            
            // Thêm xe trực tiếp qua API
            const requestData = {
                groupId: groupId,
                vehicles: vehicles
            };
            
            // Kiểm tra số lượng xe (chỉ cho phép 1 xe)
            if (vehicles.length > 1) {
                alert('Mỗi nhóm chỉ được có đúng 1 xe! Vui lòng chỉ nhập 1 xe.');
                return false;
            }
            
            return fetch('http://localhost:8084/api/vehicles/batch', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestData)
            })
            .then(response => {
                if (response.ok) {
                    return response.json();
                } else {
                    return response.text().then(text => {
                        throw new Error(text);
                    });
                }
            })
            .then(data => {
                showUpdateMessage('Đã thêm ' + vehicles.length + ' xe vào nhóm thành công!', 'success');
                closeAddVehiclesModal();
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            })
            .catch(error => {
                console.error('❌ Lỗi khi thêm xe:', error);
                showUpdateMessage('Lỗi khi thêm xe: ' + error.message, 'error');
            });
            
            return false;
        });
    }

    // Xử lý modal chọn xe xóa
    const deleteVehiclesModal = document.getElementById('deleteVehiclesModal');
    const deleteVehiclesForm = document.getElementById('deleteVehiclesForm');
    const cancelDeleteVehiclesBtn = document.getElementById('cancelDeleteVehiclesBtn');
    const deleteVehiclesModalClose = deleteVehiclesModal?.querySelector('.close-modal');

    // Hàm đóng modal xóa xe
    function closeDeleteVehiclesModal() {
        if (deleteVehiclesModal) {
            deleteVehiclesModal.classList.remove('show');
            deleteVehiclesModal.style.display = 'none';
        }
    }

    // Đóng modal khi click nút X hoặc Hủy
    if (deleteVehiclesModalClose) {
        deleteVehiclesModalClose.addEventListener('click', function() {
            closeDeleteVehiclesModal();
        });
    }

    if (cancelDeleteVehiclesBtn) {
        cancelDeleteVehiclesBtn.addEventListener('click', function() {
            closeDeleteVehiclesModal();
        });
    }

    // Đóng modal khi click bên ngoài modal
    if (deleteVehiclesModal) {
        window.addEventListener('click', function (event) {
            if (event.target === deleteVehiclesModal) {
                closeDeleteVehiclesModal();
            }
        });
    }

    // Hàm xử lý xóa xe (để có thể gọi từ nhiều nơi)
    function handleDeleteVehicles() {
        console.log('🔹 handleDeleteVehicles được gọi');
        
        // Kiểm tra nếu đang xử lý, không cho phép click lại
        const deleteVehiclesSubmitBtn = document.getElementById('deleteVehiclesSubmitBtn');
        if (deleteVehiclesSubmitBtn && deleteVehiclesSubmitBtn.disabled) {
            console.log('⚠️ Đang xử lý, không cho phép click lại');
            return false;
        }
        
        // Disable nút để tránh multiple clicks
        if (deleteVehiclesSubmitBtn) {
            deleteVehiclesSubmitBtn.disabled = true;
            deleteVehiclesSubmitBtn.textContent = 'Đang xóa...';
            deleteVehiclesSubmitBtn.style.opacity = '0.6';
            deleteVehiclesSubmitBtn.style.cursor = 'not-allowed';
        }
        
        const groupId = document.getElementById('deleteVehiclesGroupId')?.value;
        const vehiclesToDelete = parseInt(document.getElementById('deleteVehiclesCount')?.value) || 0;
        const checkedBoxes = document.querySelectorAll('#deleteVehiclesContainer .vehicle-delete-checkbox:checked');
        
        console.log('🔹 GroupId:', groupId);
        console.log('🔹 Số lượng xe cần xóa:', vehiclesToDelete);
        console.log('🔹 Số lượng xe đã chọn:', checkedBoxes.length);
        
        if (!groupId) {
            alert('Lỗi: Không tìm thấy mã nhóm xe!');
            return false;
        }
        
        if (checkedBoxes.length === 0) {
            alert('Vui lòng chọn ít nhất 1 xe để xóa!');
            return false;
        }
        
        if (checkedBoxes.length !== vehiclesToDelete) {
            alert('Vui lòng chọn đúng ' + vehiclesToDelete + ' xe cần xóa! (Đã chọn: ' + checkedBoxes.length + ')');
            return false;
        }
        
        const vehicleIdsToDelete = Array.from(checkedBoxes).map(cb => cb.value);
        console.log('🔹 Các xe cần xóa:', vehicleIdsToDelete);
        
        // Biến để lưu số lượng xe đã xóa thành công
        let actualDeletedCount = vehicleIdsToDelete.length;
        
        // Xóa các xe được chọn - xử lý từng xe để theo dõi lỗi chi tiết
        const deletePromises = vehicleIdsToDelete.map(async (vehicleId) => {
            try {
                const response = await fetch(`http://localhost:8084/api/vehicles/${vehicleId}`, {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });
                
                const responseText = await response.text();
                
                if (response.ok) {
                    console.log(`✅ Đã xóa xe ${vehicleId} thành công`);
                    return { vehicleId, success: true, message: 'Xóa thành công' };
                } else if (response.status === 404) {
                    console.warn(`⚠️ Xe ${vehicleId} không tồn tại (có thể đã bị xóa trước đó)`);
                    return { vehicleId, success: true, message: 'Đã bị xóa trước đó', warning: true };
                } else {
                    console.error(`❌ Lỗi khi xóa xe ${vehicleId}: ${response.status} - ${responseText}`);
                    return { vehicleId, success: false, message: responseText || `Lỗi ${response.status}` };
                }
            } catch (error) {
                console.error(`❌ Lỗi khi xóa xe ${vehicleId}:`, error);
                return { vehicleId, success: false, message: error.message };
            }
        });
        
        Promise.all(deletePromises)
        .then(results => {
            const successCount = results.filter(r => r.success).length;
            const failCount = results.filter(r => !r.success).length;
            const warningCount = results.filter(r => r.warning).length;
            
            console.log(`✅ Đã xóa ${successCount}/${vehicleIdsToDelete.length} xe`);
            console.log('🔹 Chi tiết kết quả:', results);
            
            // Nếu có xe không xóa được
            if (failCount > 0) {
                const failedVehicles = results.filter(r => !r.success);
                const failedMessages = failedVehicles.map(r => `${r.vehicleId}: ${r.message}`).join('; ');
                throw new Error(`Không thể xóa ${failCount} xe: ${failedMessages}`);
            }
            
            // Nếu tất cả đều thành công (bao gồm cả các xe đã bị xóa trước đó)
            if (successCount === vehicleIdsToDelete.length) {
                const deletedCount = results.filter(r => r.success && !r.warning).length;
                actualDeletedCount = deletedCount; // Lưu số lượng xe thực sự đã xóa
                if (warningCount > 0) {
                    console.log(`⚠️ ${warningCount} xe đã bị xóa trước đó nhưng không ảnh hưởng`);
                }
                // Trả về số lượng xe thực sự đã xóa (không tính các xe đã bị xóa trước đó)
                return { deletedCount, warningCount };
            }
            
            // Trường hợp này không nên xảy ra nhưng để an toàn
            throw new Error('Có lỗi không xác định khi xóa xe');
        })
        .then((result) => {
            // Đợi một chút để đảm bảo database đã cập nhật
            return new Promise(resolve => setTimeout(resolve, 200));
        })
        .then(() => {
            // Không cần cập nhật vehicleCount nữa vì đã xóa cột này
            // Chỉ cần hiển thị thông báo và reload trang
                showUpdateMessage('Đã xóa ' + actualDeletedCount + ' xe khỏi nhóm thành công!', 'success');
                closeDeleteVehiclesModal();
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
        })
        .catch(error => {
            console.error('❌ Lỗi khi xóa xe:', error);
            showUpdateMessage('Lỗi khi xóa xe: ' + error.message, 'error');
        })
        .finally(() => {
            // Enable lại nút sau khi hoàn thành
            if (deleteVehiclesSubmitBtn) {
                deleteVehiclesSubmitBtn.disabled = false;
                deleteVehiclesSubmitBtn.textContent = 'Xóa Các Xe Đã Chọn';
                deleteVehiclesSubmitBtn.style.opacity = '1';
                deleteVehiclesSubmitBtn.style.cursor = 'pointer';
            }
        });
        
        return false;
    }
    
    // Xử lý submit form xóa xe
    if (deleteVehiclesForm) {
        console.log('✅ Đã đăng ký event listener cho form xóa xe');
        
        deleteVehiclesForm.addEventListener('submit', function(e) {
            e.preventDefault();
            e.stopPropagation();
            
            console.log('🔹 Form submit xóa xe được trigger');
            handleDeleteVehicles();
            return false;
        });
    }
    
    // Thêm event listener trực tiếp cho nút submit để đảm bảo hoạt động
    const deleteVehiclesSubmitBtn = document.getElementById('deleteVehiclesSubmitBtn');
    if (deleteVehiclesSubmitBtn) {
        console.log('✅ Đã đăng ký event listener trực tiếp cho nút xóa xe');
        deleteVehiclesSubmitBtn.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            
            console.log('🔹 Nút xóa xe được click trực tiếp');
            handleDeleteVehicles();
        });
    } else {
        console.warn('⚠️ Không tìm thấy nút deleteVehiclesSubmitBtn');
    }
    
    // Sử dụng event delegation để đảm bảo hoạt động ngay cả khi modal được tạo sau
    if (deleteVehiclesModal) {
        deleteVehiclesModal.addEventListener('click', function(e) {
            if (e.target && e.target.id === 'deleteVehiclesSubmitBtn') {
                e.preventDefault();
                e.stopPropagation();
                console.log('🔹 Nút xóa xe được click qua event delegation');
                handleDeleteVehicles();
            }
        });
    }
});
