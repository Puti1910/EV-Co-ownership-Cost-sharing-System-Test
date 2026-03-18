// Admin Auto Split Page JavaScript

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', function() {
    console.log('Admin Auto Split page initializing...');
    loadGroupsForSplit();
    initAutoSplitForm();
    initSplitMethodToggle();
});

// Initialize split method toggle
function initSplitMethodToggle() {
    const splitMethod = document.getElementById('split-method');
    const usagePeriod = document.getElementById('usage-period');
    
    if (splitMethod && usagePeriod) {
        splitMethod.addEventListener('change', function() {
            if (this.value === 'BY_USAGE') {
                usagePeriod.style.display = 'block';
            } else {
                usagePeriod.style.display = 'none';
            }
        });
    }
}

// Initialize auto split form
function initAutoSplitForm() {
    const form = document.getElementById('auto-split-form');
    const btnPreview = document.getElementById('btn-preview');
    
    if (form) {
        form.addEventListener('submit', async function(e) {
            e.preventDefault();
            await createAndSplit();
        });
    }
    
    if (btnPreview) {
        btnPreview.addEventListener('click', async function() {
            await previewSplit();
        });
    }
}

// Get form data
function getFormData() {
    const groupSelect = document.getElementById('group-select');
    const costType = document.getElementById('cost-type').value;
    const amount = parseFloat(document.getElementById('amount').value);
    const splitMethod = document.getElementById('split-method').value;
    const month = document.getElementById('month')?.value;
    const year = document.getElementById('year')?.value;
    const description = document.getElementById('description').value;
    
    if (!groupSelect || !groupSelect.value) {
        throw new Error('Vui lòng chọn nhóm');
    }
    
    const groupId = parseInt(groupSelect.value);
    const vehicleId = parseInt(groupSelect.options[groupSelect.selectedIndex].getAttribute('data-vehicle-id'));
    
    if (!vehicleId) {
        throw new Error('Nhóm này chưa có xe được gán. Vui lòng chọn nhóm khác.');
    }
    
    if (!amount || amount <= 0) {
        throw new Error('Vui lòng nhập số tiền hợp lệ');
    }
    
    return {
        groupId,
        vehicleId,
        costType,
        amount,
        splitMethod,
        month: splitMethod === 'BY_USAGE' ? parseInt(month) : null,
        year: splitMethod === 'BY_USAGE' ? parseInt(year) : null,
        description
    };
}

// Preview split
async function previewSplit() {
    let data;
    
    try {
        data = getFormData();
    } catch (error) {
        alert(error.message || 'Lỗi khi lấy dữ liệu form');
        return;
    }
    
    try {
        const response = await fetch('/api/auto-split/preview', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            const errorMsg = errorData.error || errorData.message || `Lỗi ${response.status}: ${response.statusText}`;
            throw new Error(errorMsg);
        }
        
        const result = await response.json();
        
        // Check if result has error field
        if (result.error) {
            throw new Error(result.error);
        }
        
        showPreviewResult(result);
        
    } catch (error) {
        console.error('Error previewing split:', error);
        alert('Lỗi khi xem trước: ' + error.message);
    }
}

// Create and split
async function createAndSplit() {
    let data;
    
    try {
        data = getFormData();
    } catch (error) {
        alert(error.message || 'Lỗi khi lấy dữ liệu form');
        return;
    }
    
    try {
        const btnSubmit = document.querySelector('#auto-split-form button[type="submit"]');
        if (btnSubmit) {
            btnSubmit.disabled = true;
            btnSubmit.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang xử lý...';
        }
        
        const response = await fetch('/api/auto-split/create-and-split', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Lỗi khi tạo và chia chi phí');
        }
        
        const result = await response.json();
        alert('Tạo và chia chi phí thành công!');
        
        // Reset form
        document.getElementById('auto-split-form').reset();
        document.getElementById('preview-result').style.display = 'none';
        
        // Redirect to costs page
        window.location.href = '/admin/costs';
        
    } catch (error) {
        console.error('Error creating and splitting:', error);
        alert('Lỗi khi tạo và chia chi phí: ' + error.message);
    } finally {
        const btnSubmit = document.querySelector('#auto-split-form button[type="submit"]');
        if (btnSubmit) {
            btnSubmit.disabled = false;
            btnSubmit.innerHTML = '<i class="fas fa-save"></i> Tạo và chia tự động';
        }
    }
}

// Show preview result
function showPreviewResult(result) {
    const previewResult = document.getElementById('preview-result');
    const previewContent = document.getElementById('preview-content');
    
    if (!previewResult || !previewContent) return;
    
    const shares = result.shares || [];
    
    previewContent.innerHTML = `
        <div style="margin-bottom: 1rem;">
            <strong>Tổng số tiền:</strong> ${formatCurrency(result.totalAmount || 0)}
        </div>
        <div style="margin-bottom: 1rem;">
            <strong>Số người chia:</strong> ${shares.length}
        </div>
        <table style="width: 100%; border-collapse: collapse; margin-top: 1rem;">
            <thead>
                <tr style="background: var(--bg-secondary);">
                    <th style="padding: 0.75rem; text-align: left; border-bottom: 1px solid var(--border);">User ID</th>
                    <th style="padding: 0.75rem; text-align: right; border-bottom: 1px solid var(--border);">Số tiền</th>
                    <th style="padding: 0.75rem; text-align: right; border-bottom: 1px solid var(--border);">Tỷ lệ</th>
                </tr>
            </thead>
            <tbody>
                ${shares.map(share => `
                    <tr>
                        <td style="padding: 0.75rem; border-bottom: 1px solid var(--border);">User #${share.userId}</td>
                        <td style="padding: 0.75rem; text-align: right; border-bottom: 1px solid var(--border); font-weight: bold;">
                            ${formatCurrency(share.amount)}
                        </td>
                        <td style="padding: 0.75rem; text-align: right; border-bottom: 1px solid var(--border);">
                            ${((share.amount / (result.totalAmount || 1)) * 100).toFixed(2)}%
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
    
    previewResult.style.display = 'block';
}

// Load groups for split
async function loadGroupsForSplit() {
    try {
        const response = await fetch('/api/groups');
        if (!response.ok) throw new Error('Failed to fetch groups');
        
        const groups = await response.json();
        const select = document.getElementById('group-select');
        
        if (!select) return;
        
        if (!groups || groups.length === 0) {
            select.innerHTML = '<option value="">-- Không có nhóm nào --</option>';
            return;
        }
        
        select.innerHTML = '<option value="">-- Chọn nhóm --</option>';
        
        groups.forEach(g => {
            const option = document.createElement('option');
            const groupId = g.groupId || g.id;
            const groupName = g.groupName || g.name || `Nhóm #${groupId}`;
            const vehicleId = g.vehicleId || null;
            
            option.value = String(groupId);
            
            if (vehicleId) {
                option.setAttribute('data-vehicle-id', String(vehicleId));
                option.textContent = `${groupName} (Xe ID: ${vehicleId})`;
            } else {
                option.textContent = `${groupName} (⚠️ Chưa có xe)`;
            }
            
            select.appendChild(option);
        });
        
    } catch (error) {
        console.error('Error loading groups:', error);
        const select = document.getElementById('group-select');
        if (select) {
            select.innerHTML = '<option value="">-- Lỗi tải danh sách nhóm --</option>';
        }
        alert('Không thể tải danh sách nhóm. Vui lòng thử lại sau.');
    }
}

// Helper functions
function formatCurrency(amount) {
    if (!amount) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

