// Auto Split - Tự động chia chi phí
const AUTO_SPLIT_API = '/api/auto-split';
const GROUP_API = '/api/groups';
const COST_API = '/api/costs';

let groupsData = [];
let currentGroupId = null;

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', function() {
    initializeForm();
    loadVehicles();
    loadRecentCosts();
    setActiveNavItem();
});

// Initialize form
function initializeForm() {
    const costForm = document.getElementById('costForm');
    if (costForm) {
        costForm.addEventListener('submit', handleFormSubmit);
    }

    // Add event listeners for auto-update preview
    const vehicleSelect = document.getElementById('vehicleId');
    if (vehicleSelect) {
        vehicleSelect.addEventListener('change', updatePreview);
    }

    const amountInput = document.getElementById('amount');
    if (amountInput) {
        amountInput.addEventListener('input', updatePreview);
    }

    const splitMethodSelect = document.getElementById('splitMethod');
    if (splitMethodSelect) {
        splitMethodSelect.addEventListener('change', updatePreview);
    }
}

// Load vehicles (groups)
async function loadVehicles() {
    try {
        const response = await fetch(GROUP_API);
        if (!response.ok) throw new Error('Failed to fetch groups');
        
        groupsData = await response.json();
        const vehicleSelect = document.getElementById('vehicleId');
        
        if (!vehicleSelect) {
            console.error('Vehicle select element not found');
            return;
        }

        if (!groupsData || groupsData.length === 0) {
            vehicleSelect.innerHTML = '<option value="">-- Không có xe nào --</option>';
            showNotification('Không có nhóm/xe nào trong hệ thống', 'warning');
            return;
        }

        // Filter groups that have both groupId and vehicleId
        const validGroups = groupsData.filter(g => g.groupId && g.vehicleId);
        
        if (validGroups.length === 0) {
            vehicleSelect.innerHTML = '<option value="">-- Không có xe hợp lệ --</option>';
            showNotification('Không có xe nào có dữ liệu hợp lệ', 'warning');
            return;
        }

        vehicleSelect.innerHTML = '<option value="">-- Chọn xe --</option>' +
            validGroups.map(g => {
                const groupName = g.groupName || `Group ${g.groupId}`;
                return `<option value="${g.vehicleId}" data-group-id="${g.groupId}">${groupName}</option>`;
            }).join('');

        console.log(`Loaded ${validGroups.length} vehicles/groups`);
    } catch (error) {
        console.error('Error loading vehicles:', error);
        const vehicleSelect = document.getElementById('vehicleId');
        if (vehicleSelect) {
            vehicleSelect.innerHTML = '<option value="">-- Lỗi tải dữ liệu --</option>';
        }
        showNotification('Không thể tải danh sách xe: ' + error.message, 'error');
    }
}

// Load recent costs
async function loadRecentCosts() {
    try {
        const response = await fetch(COST_API);
        if (!response.ok) return;
        
        const costs = await response.json();
        const tbody = document.getElementById('recentCostsBody');
        
        if (!tbody) return;
        
        // Filter costs with splitMethod
        const autoSplitCosts = costs.filter(c => c.splitMethod).slice(0, 10);
        
        if (autoSplitCosts.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center">
                        <div class="empty-state">
                            <i class="fas fa-inbox"></i>
                            <p>Chưa có chi phí tự động chia nào</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }
        
        tbody.innerHTML = autoSplitCosts.map(cost => `
            <tr>
                <td><span class="badge badge-info">${getCostTypeIcon(cost.costType)} ${getCostTypeLabel(cost.costType)}</span></td>
                <td>${cost.description || '-'}</td>
                <td><strong>${formatMoney(cost.amount)}</strong></td>
                <td><span class="badge badge-success">${getSplitMethodLabel(cost.splitMethod)}</span></td>
                <td>${formatDate(cost.createdAt)}</td>
                <td>
                    <button class="btn btn-sm btn-outline" onclick="viewCostDetail(${cost.costId})">
                        <i class="fas fa-eye"></i>
                    </button>
                </td>
            </tr>
        `).join('');
        
    } catch (error) {
        console.error('Error loading recent costs:', error);
    }
}

// Suggest split method based on cost type
function suggestSplitMethod() {
    const costType = document.getElementById('costType').value;
    const splitMethodSelect = document.getElementById('splitMethod');
    const hint = document.getElementById('splitMethodHint');

    const suggestions = {
        'ElectricCharge': { method: 'BY_USAGE', text: '💡 Khuyến nghị: Chia theo km đã chạy' },
        'Maintenance': { method: 'BY_OWNERSHIP', text: '💡 Khuyến nghị: Chia theo sở hữu' },
        'Insurance': { method: 'BY_OWNERSHIP', text: '💡 Khuyến nghị: Chia theo sở hữu' },
        'Inspection': { method: 'BY_OWNERSHIP', text: '💡 Khuyến nghị: Chia theo sở hữu' },
        'Cleaning': { method: 'EQUAL', text: '💡 Khuyến nghị: Chia đều' },
        'Other': { method: 'EQUAL', text: '💡 Khuyến nghị: Chia đều' }
    };

    if (suggestions[costType]) {
        splitMethodSelect.value = suggestions[costType].method;
        hint.textContent = suggestions[costType].text;
        hint.style.color = '#10b981';
        updatePreview();
    }
}

// Update preview
async function updatePreview() {
    const vehicleSelect = document.getElementById('vehicleId');
    const amount = parseFloat(document.getElementById('amount').value) || 0;
    const splitMethod = document.getElementById('splitMethod').value;

    if (!vehicleSelect || !vehicleSelect.value || !amount || !splitMethod) {
        document.getElementById('previewSection').style.display = 'none';
        return;
    }

    // Get groupId from selected vehicle
    const selectedOption = vehicleSelect.options[vehicleSelect.selectedIndex];
    if (!selectedOption || !selectedOption.dataset || !selectedOption.dataset.groupId) {
        console.error('Không tìm thấy groupId cho xe đã chọn');
        document.getElementById('previewSection').style.display = 'none';
        showNotification('Vui lòng chọn xe hợp lệ', 'error');
        return;
    }

    const groupId = parseInt(selectedOption.dataset.groupId);
    if (isNaN(groupId) || groupId <= 0) {
        console.error('groupId không hợp lệ:', selectedOption.dataset.groupId);
        document.getElementById('previewSection').style.display = 'none';
        showNotification('Dữ liệu xe không hợp lệ. Vui lòng làm mới trang.', 'error');
        return;
    }

    currentGroupId = groupId;

    try {
        showLoading();
        
        // Get preview from backend
        const response = await fetch(`${AUTO_SPLIT_API}/preview`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                groupId: groupId,
                amount: amount,
                splitMethod: splitMethod,
                month: new Date().getMonth() + 1,
                year: new Date().getFullYear()
            })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `Lỗi ${response.status}: ${response.statusText}`);
        }

        const preview = await response.json();
        
        // Validate preview response
        if (!preview || !preview.shares || !Array.isArray(preview.shares)) {
            throw new Error('Dữ liệu preview không hợp lệ từ server');
        }

        if (preview.shares.length === 0) {
            displayEmptyPreview(amount, splitMethod);
            return;
        }

        displayPreview(preview, amount, splitMethod);

    } catch (error) {
        console.error('Error getting preview:', error);
        displayErrorPreview(amount, splitMethod, error.message);
    } finally {
        hideLoading();
    }
}

// Display preview
function displayPreview(preview, amount, splitMethod) {
    const previewSection = document.getElementById('previewSection');
    if (!previewSection) {
        console.error('Preview section not found');
        return;
    }

    previewSection.style.display = 'block';
    
    const previewTotal = document.getElementById('previewTotal');
    const previewMembers = document.getElementById('previewMembers');
    const previewMethod = document.getElementById('previewMethod');
    const tbody = document.getElementById('previewTableBody');

    if (previewTotal) previewTotal.textContent = formatMoney(amount);
    if (previewMembers) previewMembers.textContent = preview.shares ? preview.shares.length : 0;
    if (previewMethod) previewMethod.textContent = getSplitMethodLabel(splitMethod);

    if (tbody && preview.shares && preview.shares.length > 0) {
        tbody.innerHTML = preview.shares.map(share => {
            const percent = share.percent != null ? parseFloat(share.percent).toFixed(2) : '0.00';
            const amountShare = share.amountShare != null ? share.amountShare : 0;
            return `
                <tr>
                    <td><strong>User ${share.userId || 'N/A'}</strong></td>
                    <td><span class="badge badge-primary">${percent}%</span></td>
                    <td><strong>${formatMoney(amountShare)}</strong></td>
                </tr>
            `;
        }).join('');
    } else {
        tbody.innerHTML = `
            <tr>
                <td colspan="3" class="text-center">
                    Không có dữ liệu để hiển thị
                </td>
            </tr>
        `;
    }
}

// Display simple preview (fallback)
function displaySimplePreview(amount, splitMethod) {
    document.getElementById('previewSection').style.display = 'block';
    document.getElementById('previewTotal').textContent = formatMoney(amount);
    document.getElementById('previewMembers').textContent = '?';
    document.getElementById('previewMethod').textContent = getSplitMethodLabel(splitMethod);

    const tbody = document.getElementById('previewTableBody');
    tbody.innerHTML = `
        <tr>
            <td colspan="3" class="text-center">
                <i class="fas fa-spinner fa-spin"></i> Đang tải preview...
            </td>
        </tr>
    `;
}

// Display empty preview
function displayEmptyPreview(amount, splitMethod) {
    document.getElementById('previewSection').style.display = 'block';
    document.getElementById('previewTotal').textContent = formatMoney(amount);
    document.getElementById('previewMembers').textContent = '0';
    document.getElementById('previewMethod').textContent = getSplitMethodLabel(splitMethod);

    const tbody = document.getElementById('previewTableBody');
    tbody.innerHTML = `
        <tr>
            <td colspan="3" class="text-center" style="color: #ef4444;">
                <i class="fas fa-exclamation-triangle"></i> 
                Không có thành viên nào trong nhóm để chia chi phí
            </td>
        </tr>
    `;
}

// Display error preview
function displayErrorPreview(amount, splitMethod, errorMessage) {
    document.getElementById('previewSection').style.display = 'block';
    document.getElementById('previewTotal').textContent = formatMoney(amount);
    document.getElementById('previewMembers').textContent = '-';
    document.getElementById('previewMethod').textContent = getSplitMethodLabel(splitMethod);

    const tbody = document.getElementById('previewTableBody');
    tbody.innerHTML = `
        <tr>
            <td colspan="3" class="text-center" style="color: #ef4444; padding: 1rem;">
                <i class="fas fa-exclamation-circle"></i><br>
                <strong>Không thể tải preview</strong><br>
                <small style="color: #6b7280;">${errorMessage || 'Vui lòng thử lại sau'}</small>
            </td>
        </tr>
    `;
}

// Handle form submit
async function handleFormSubmit(e) {
    e.preventDefault();

    const vehicleSelect = document.getElementById('vehicleId');
    const selectedOption = vehicleSelect.options[vehicleSelect.selectedIndex];
    const groupId = selectedOption.dataset.groupId;
    
    const formData = {
        vehicleId: parseInt(vehicleSelect.value),
        costType: document.getElementById('costType').value,
        amount: parseFloat(document.getElementById('amount').value),
        description: document.getElementById('description').value,
        splitMethod: document.getElementById('splitMethod').value,
        groupId: parseInt(groupId),
        month: new Date().getMonth() + 1,
        year: new Date().getFullYear()
    };

    try {
        showLoading();
        
        const response = await fetch(`${AUTO_SPLIT_API}/create-and-split`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        if (!response.ok) throw new Error('Failed to create cost');

        const result = await response.json();
        
        showNotification(
            `✅ Thành công!\n\nĐã tạo chi phí ${formatMoney(result.cost.amount)}\nĐã chia cho ${result.shares.length} thành viên`,
            'success'
        );

        // Reset form and reload
        resetForm();
        loadRecentCosts();
        
        // Redirect to cost list after 2 seconds
        setTimeout(() => {
            window.location.href = '/costs';
        }, 2000);

    } catch (error) {
        console.error('Error creating cost:', error);
        showNotification('Có lỗi xảy ra khi tạo chi phí. Vui lòng thử lại.', 'error');
    } finally {
        hideLoading();
    }
}

// Reset form
function resetForm() {
    document.getElementById('costForm').reset();
    document.getElementById('previewSection').style.display = 'none';
    document.getElementById('splitMethodHint').textContent = '';
}

// View cost detail
function viewCostDetail(costId) {
    window.location.href = `/costs/${costId}/edit`;
}

// Helper functions
function getCostTypeIcon(costType) {
    const icons = {
        'ElectricCharge': '⚡',
        'Maintenance': '🔧',
        'Insurance': '🛡️',
        'Inspection': '📋',
        'Cleaning': '🧽',
        'Other': '📦'
    };
    return icons[costType] || '📦';
}

function getCostTypeLabel(costType) {
    const labels = {
        'ElectricCharge': 'Sạc điện',
        'Maintenance': 'Bảo dưỡng',
        'Insurance': 'Bảo hiểm',
        'Inspection': 'Đăng kiểm',
        'Cleaning': 'Vệ sinh',
        'Other': 'Khác'
    };
    return labels[costType] || costType;
}

function getSplitMethodLabel(method) {
    const labels = {
        'BY_OWNERSHIP': '📊 Theo sở hữu',
        'BY_USAGE': '🛣️ Theo km',
        'EQUAL': '➗ Chia đều',
        'CUSTOM': '✏️ Tùy chỉnh'
    };
    return labels[method] || method;
}

function formatMoney(value) {
    return new Intl.NumberFormat('vi-VN').format(value) + ' VNĐ';
}

function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN');
}

function showNotification(message, type = 'info') {
    alert(message);
}

function showLoading() {
    document.body.style.cursor = 'wait';
}

function hideLoading() {
    document.body.style.cursor = 'default';
}

