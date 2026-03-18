// Admin Costs Management Page JavaScript

// Mark that this script is loaded
window.adminCostsLoaded = true;

let currentCostsData = [];

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', function() {
    console.log('Admin Costs page initializing...');
    
    // Check if this is the active section before loading
    const costSection = document.getElementById('cost-management-section');
    if (costSection) {
        const style = window.getComputedStyle(costSection);
        if (style.display !== 'none') {
            console.log('Loading costs data...');
            loadCosts();
        } else {
            console.log('cost-management-section is hidden, skipping load');
        }
    } else {
        console.log('cost-management-section not found');
    }
    
    initCreateCostModal();
    initCostFilters();
});

// Load costs data
async function loadCosts() {
    try {
        const response = await fetch('/api/costs');
        if (!response.ok) throw new Error('Failed to fetch costs');
        
        const costs = await response.json();
        const initialCosts = costs || [];
        
        // Use status from database (PENDING or SHARED)
        // Map status to hasShares for backward compatibility with filter logic
        const costsWithStatus = initialCosts.map(cost => {
            // Ensure status is set (default to PENDING if not present)
            cost.status = cost.status || 'PENDING';
            // Map status to hasShares for filter compatibility
            cost.hasShares = cost.status === 'SHARED';
            return cost;
        });
        
        // Update currentCostsData
        currentCostsData = costsWithStatus;
        
        renderCostsTable(costsWithStatus);
        
    } catch (error) {
        console.error('Error loading costs:', error);
        document.getElementById('costs-tbody').innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 2rem; color: var(--danger);">
                    <i class="fas fa-exclamation-circle"></i> Lỗi khi tải dữ liệu: ${error.message}
                </td>
            </tr>
        `;
    }
}

// Render costs table
function renderCostsTable(costs) {
    const tbody = document.getElementById('costs-tbody');
    
    if (!costs || costs.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 2rem; color: var(--text-light);">
                    <i class="fas fa-inbox"></i><br>Không có dữ liệu chi phí
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = costs.map(cost => `
        <tr>
            <td>${cost.costId}</td>
            <td>${cost.vehicleId || '-'}</td>
            <td>${getCostTypeName(cost.costType)}</td>
            <td style="font-weight: bold; color: var(--primary);">${formatCurrency(cost.amount)}</td>
            <td>${formatDate(cost.createdAt)}</td>
            <td>
                ${cost.status === 'SHARED' ? 
                    `<span class="status-badge paid" style="background: #10B981;">Đã chia</span>` :
                    `<span class="status-badge pending">Chưa chia</span>`
                }
            </td>
            <td>
                <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
                    <button class="btn btn-sm" style="background: var(--info); color: white; padding: 0.5rem 0.75rem;" 
                            onclick="viewCostDetail(${cost.costId})" title="Xem chi tiết">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button class="btn btn-sm" style="background: #10B981; color: white; padding: 0.5rem 0.75rem;" 
                            onclick="openCostSplitModal(${cost.costId})" title="Chia chi phí">
                        <i class="fas fa-share-alt"></i>
                    </button>
                    <button class="btn btn-sm" style="background: var(--primary); color: white; padding: 0.5rem 0.75rem;" 
                            onclick="editCost(${cost.costId})" title="Sửa">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm" style="background: var(--danger); color: white; padding: 0.5rem 0.75rem;" 
                            onclick="deleteCost(${cost.costId})" title="Xóa">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

// Apply filters
function applyCostFilters() {
    const typeFilter = document.getElementById('cost-type-filter').value;
    const statusFilter = document.getElementById('cost-status-filter').value;
    const searchInput = document.getElementById('cost-search-input').value.toLowerCase();
    
    let filtered = [...currentCostsData];
    
    if (typeFilter) {
        filtered = filtered.filter(c => c.costType === typeFilter);
    }
    
    if (statusFilter === 'PENDING') {
        filtered = filtered.filter(c => (c.status || 'PENDING') === 'PENDING');
    } else if (statusFilter === 'SHARED') {
        filtered = filtered.filter(c => c.status === 'SHARED');
    }
    
    if (searchInput) {
        filtered = filtered.filter(c => 
            (c.costId && c.costId.toString().includes(searchInput)) ||
            (c.vehicleId && c.vehicleId.toString().includes(searchInput)) ||
            (c.description && c.description.toLowerCase().includes(searchInput))
        );
    }
    
    renderCostsTable(filtered);
}

// Initialize filters
function initCostFilters() {
    const searchInput = document.getElementById('cost-search-input');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                applyCostFilters();
            }
        });
    }
}

// Initialize create cost modal
function initCreateCostModal() {
    const btnCreate = document.getElementById('btn-create-cost');
    if (btnCreate) {
        btnCreate.addEventListener('click', openCreateCostModal);
    }
}

// Open create cost modal
function openCreateCostModal() {
    // Check if modal exists in admin-modals fragment
    const modal = document.getElementById('create-cost-modal');
    if (modal) {
        modal.style.display = 'flex';
    } else {
        // If modal doesn't exist, redirect to create page
        window.location.href = '/costs/create';
    }
}

// View cost detail
function viewCostDetail(costId) {
    window.location.href = `/costs/${costId}/splits`;
}

// Open cost split modal
function openCostSplitModal(costId) {
    window.location.href = `/costs/${costId}/splits`;
}

// Edit cost
function editCost(costId) {
    window.location.href = `/costs/${costId}/edit`;
}

// Delete cost
async function deleteCost(costId) {
    if (!confirm(`Bạn có chắc chắn muốn xóa chi phí #${costId}?`)) {
        return;
    }
    
    try {
        const response = await fetch(`/api/costs/${costId}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            alert('Xóa chi phí thành công!');
            loadCosts();
        } else {
            alert('Lỗi khi xóa chi phí');
        }
    } catch (error) {
        console.error('Error deleting cost:', error);
        alert('Lỗi khi xóa chi phí: ' + error.message);
    }
}

// Helper functions (if not available from admin-common.js)
function getCostTypeName(costType) {
    const types = {
        'ElectricCharge': 'Phí sạc điện',
        'Maintenance': 'Bảo dưỡng',
        'Insurance': 'Bảo hiểm',
        'Inspection': 'Đăng kiểm',
        'Cleaning': 'Vệ sinh',
        'Other': 'Khác'
    };
    return types[costType] || costType;
}

function formatCurrency(amount) {
    if (!amount) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    });
}

