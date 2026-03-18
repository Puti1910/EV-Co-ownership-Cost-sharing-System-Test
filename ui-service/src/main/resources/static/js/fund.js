// Fund Management JavaScript

// Global variables
let fundStats = {};
let transactions = [];
let pendingTransactions = [];
let groups = [];
let isAdmin = true; // TODO: Get from session/auth

// Initialize page
document.addEventListener('DOMContentLoaded', function() {
    initializePage();
    loadFundStats();
    loadGroups();
    loadTransactions();
    loadPendingApprovals();
    setupEventListeners();
    checkAdminRole();
});

// Check admin role
function checkAdminRole() {
    const pendingCard = document.getElementById('pendingApprovalsCard');
    if (pendingCard) {
        pendingCard.style.display = isAdmin ? 'block' : 'none';
    }
}

// Initialize page elements
function initializePage() {
    // Set current date for transaction form
    const today = new Date().toISOString().split('T')[0];
    const fundDateInput = document.getElementById('fundDate');
    if (fundDateInput) {
        fundDateInput.value = today;
    }
    
    // Setup transaction type change listener
    const transactionType = document.getElementById('transactionType');
    if (transactionType) {
        transactionType.addEventListener('change', function() {
            const receiptGroup = document.getElementById('receiptGroup');
            if (receiptGroup) {
                receiptGroup.style.display = this.value === 'Withdraw' ? 'block' : 'none';
            }
        });
    }
}

// Load fund statistics
async function loadFundStats() {
    try {
        const response = await fetch('/api/fund/stats');
        if (response.ok) {
            fundStats = await response.json();
            updateFundStatsDisplay();
        }
    } catch (error) {
        console.error('Error loading fund stats:', error);
        showNotification('Lỗi khi tải thống kê quỹ', 'error');
    }
}

// Update fund statistics display
function updateFundStatsDisplay() {
    // Update stat cards
    const statCards = document.querySelectorAll('.stat-card .stat-value');
    if (statCards.length >= 4) {
        statCards[0].textContent = formatCurrency(fundStats.totalBalance || 0);
        statCards[1].textContent = formatCurrency(fundStats.totalIncome || 0);
        statCards[2].textContent = formatCurrency(fundStats.totalExpense || 0);
        statCards[3].textContent = (fundStats.pendingCount || 0).toString();
    }
    
    // Update fund summary
    const summaryItems = document.querySelectorAll('.fund-summary .value');
    if (summaryItems.length >= 4) {
        summaryItems[0].textContent = formatCurrency(fundStats.openingBalance || 0);
        summaryItems[1].textContent = formatCurrency(fundStats.totalIncome || 0);
        summaryItems[2].textContent = formatCurrency(fundStats.totalExpense || 0);
        summaryItems[3].textContent = formatCurrency(fundStats.totalBalance || 0);
    }
}

// Load groups
async function loadGroups() {
    try {
        const response = await fetch('/groups/api/all');
        if (response.ok) {
            groups = await response.json();
            updateGroupSelects();
        }
    } catch (error) {
        console.error('Error loading groups:', error);
    }
}

// Update group select elements
function updateGroupSelects() {
    const groupSelects = document.querySelectorAll('#filterGroup, #depositGroup, #withdrawGroup');
    groupSelects.forEach(select => {
        if (!select) return;
        
        // Clear existing options except the first one
        while (select.children.length > 1) {
            select.removeChild(select.lastChild);
        }
        
        // Update the first option text
        if (select.children.length > 0) {
            select.children[0].textContent = 'Chọn nhóm';
        }
        
        // Add group options
        groups.forEach(group => {
            const option = document.createElement('option');
            option.value = group.groupId;
            option.textContent = group.groupName;
            select.appendChild(option);
        });
    });
}

// Load pending approvals (Admin only)
async function loadPendingApprovals() {
    if (!isAdmin) return;
    
    try {
        const response = await fetch('/api/fund/transactions?status=Pending');
        if (response.ok) {
            pendingTransactions = await response.json();
            updatePendingApprovalsDisplay();
            
            // Update badge
            const badge = document.getElementById('pendingBadge');
            if (badge) {
                badge.textContent = pendingTransactions.length;
            }
        }
    } catch (error) {
        console.error('Error loading pending approvals:', error);
    }
}

// Update pending approvals display
function updatePendingApprovalsDisplay() {
    const tbody = document.getElementById('pendingApprovalsBody');
    if (!tbody) return;
    
    if (pendingTransactions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="empty-table">
                    <div class="empty-state">
                        <i class="fas fa-check-circle"></i>
                        <p>Không có yêu cầu chờ duyệt</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = pendingTransactions.map(tx => `
        <tr>
            <td>${formatDate(tx.date)}</td>
            <td>User ${tx.userId}</td>
            <td>
                <span class="status-badge status-${tx.transactionType.toLowerCase()}">
                    ${tx.transactionType === 'Deposit' ? 'Nạp tiền' : 'Rút tiền'}
                </span>
            </td>
            <td class="negative">${formatCurrency(tx.amount)}</td>
            <td>${tx.purpose || '-'}</td>
            <td>
                ${tx.receiptUrl ? 
                    `<a href="${tx.receiptUrl}" target="_blank" class="receipt-link">
                        <i class="fas fa-file-invoice"></i> Xem hóa đơn
                    </a>` : 
                    '<span style="color: #9ca3af;">Không có</span>'
                }
            </td>
            <td>
                <div class="action-buttons">
                    <button class="btn btn-sm btn-success" onclick="approveTransaction(${tx.transactionId})" title="Phê duyệt">
                        <i class="fas fa-check"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="rejectTransaction(${tx.transactionId})" title="Từ chối">
                        <i class="fas fa-times"></i>
                    </button>
                    <button class="btn btn-sm btn-info" onclick="viewTransactionDetails(${tx.transactionId})" title="Chi tiết">
                        <i class="fas fa-eye"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

// Load transactions
async function loadTransactions() {
    try {
        const response = await fetch('/api/fund/transactions');
        if (response.ok) {
            transactions = await response.json();
            updateTransactionsDisplay();
            updateRecentTransactions();
        }
    } catch (error) {
        console.error('Error loading transactions:', error);
        showNotification('Lỗi khi tải giao dịch', 'error');
    }
}

// Update transactions table display
function updateTransactionsDisplay() {
    const tbody = document.getElementById('transactionsTableBody');
    if (!tbody) return;
    
    if (transactions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="empty-table">
                    <div class="empty-state">
                        <i class="fas fa-receipt"></i>
                        <p>Chưa có giao dịch nào</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = transactions.map(transaction => `
        <tr>
            <td>${formatDate(transaction.date)}</td>
            <td>
                <span class="status-badge status-${transaction.transactionType.toLowerCase()}">
                    ${transaction.transactionType === 'Deposit' ? 'Nạp tiền' : 'Rút tiền'}
                </span>
            </td>
            <td>${transaction.purpose || '-'}</td>
            <td class="${transaction.transactionType === 'Deposit' ? 'positive' : 'negative'}">
                ${transaction.transactionType === 'Deposit' ? '+' : '-'}${formatCurrency(transaction.amount)}
            </td>
            <td>
                <span class="status-badge status-${transaction.status.toLowerCase()}">
                    <i class="fas fa-${getStatusIcon(transaction.status)}"></i>
                    ${getStatusText(transaction.status)}
                </span>
            </td>
            <td>User ${transaction.userId}</td>
            <td>
                <div class="action-buttons">
                    ${transaction.receiptUrl ? 
                        `<a href="${transaction.receiptUrl}" target="_blank" class="btn btn-sm btn-info" title="Hóa đơn">
                            <i class="fas fa-file-invoice"></i>
                        </a>` : ''
                    }
                    ${transaction.status === 'Completed' ? 
                        `<button class="btn btn-sm btn-outline" onclick="viewTransactionDetails(${transaction.transactionId})" title="Chi tiết">
                            <i class="fas fa-eye"></i>
                        </button>` : ''
                    }
                </div>
            </td>
        </tr>
    `).join('');
}

// Update recent transactions display
function updateRecentTransactions() {
    const recentContainer = document.getElementById('recentTransactions');
    if (!recentContainer) return;
    
    const recentTransactions = transactions.slice(0, 5);
    
    if (recentTransactions.length === 0) {
        recentContainer.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-receipt"></i>
                <p>Chưa có giao dịch nào</p>
            </div>
        `;
        return;
    }
    
    recentContainer.innerHTML = recentTransactions.map(transaction => `
        <div class="transaction-item">
            <div class="transaction-info">
                <div class="transaction-type ${transaction.type.toLowerCase()}">
                    <i class="fas fa-${transaction.type === 'INCOME' ? 'arrow-up' : 'arrow-down'}"></i>
                    ${transaction.type === 'INCOME' ? 'Thu nhập' : 'Chi phí'}
                </div>
                <div class="transaction-details">
                    <div class="transaction-description">${transaction.description || 'Không có mô tả'}</div>
                    <div class="transaction-meta">
                        ${getGroupName(transaction.groupId)} • ${formatDate(transaction.transactionDate)}
                    </div>
                </div>
            </div>
            <div class="transaction-amount ${transaction.type === 'INCOME' ? 'positive' : 'negative'}">
                ${transaction.type === 'INCOME' ? '+' : '-'}${formatCurrency(transaction.amount)}
            </div>
        </div>
    `).join('');
}

// Setup event listeners
function setupEventListeners() {
    // Filter change events
    const filterStatus = document.getElementById('filterStatus');
    const filterType = document.getElementById('filterType');
    const filterGroup = document.getElementById('filterGroup');
    
    if (filterStatus) {
        filterStatus.addEventListener('change', filterTransactions);
    }
    
    if (filterType) {
        filterType.addEventListener('change', filterTransactions);
    }
    
    if (filterGroup) {
        filterGroup.addEventListener('change', filterTransactions);
    }
    
    // Form submission
    const addFundForm = document.getElementById('addFundForm');
    if (addFundForm) {
        addFundForm.addEventListener('submit', handleAddFundTransaction);
    }
}

// Filter transactions
function filterTransactions() {
    const statusFilter = document.getElementById('filterStatus')?.value;
    const typeFilter = document.getElementById('filterType')?.value;
    const groupFilter = document.getElementById('filterGroup')?.value;
    
    let filteredTransactions = transactions;
    
    if (statusFilter) {
        filteredTransactions = filteredTransactions.filter(t => t.status === statusFilter);
    }
    
    if (typeFilter) {
        filteredTransactions = filteredTransactions.filter(t => t.transactionType === typeFilter);
    }
    
    if (groupFilter) {
        filteredTransactions = filteredTransactions.filter(t => t.fundId == groupFilter);
    }
    
    // Update display with filtered transactions
    updateTransactionsDisplayWithData(filteredTransactions);
}

// Update transactions display with specific data
function updateTransactionsDisplayWithData(transactionData) {
    const tbody = document.getElementById('transactionsTableBody');
    if (!tbody) return;
    
    if (transactionData.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="empty-table">
                    <div class="empty-state">
                        <i class="fas fa-search"></i>
                        <p>Không tìm thấy giao dịch phù hợp</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = transactionData.map(transaction => `
        <tr>
            <td>${formatDate(transaction.date)}</td>
            <td>
                <span class="status-badge status-${transaction.transactionType.toLowerCase()}">
                    ${transaction.transactionType === 'Deposit' ? 'Nạp tiền' : 'Rút tiền'}
                </span>
            </td>
            <td>${transaction.purpose || '-'}</td>
            <td class="${transaction.transactionType === 'Deposit' ? 'positive' : 'negative'}">
                ${transaction.transactionType === 'Deposit' ? '+' : '-'}${formatCurrency(transaction.amount)}
            </td>
            <td>
                <span class="status-badge status-${transaction.status.toLowerCase()}">
                    <i class="fas fa-${getStatusIcon(transaction.status)}"></i>
                    ${getStatusText(transaction.status)}
                </span>
            </td>
            <td>User ${transaction.userId}</td>
            <td>
                <div class="action-buttons">
                    ${transaction.receiptUrl ? 
                        `<a href="${transaction.receiptUrl}" target="_blank" class="btn btn-sm btn-info" title="Hóa đơn">
                            <i class="fas fa-file-invoice"></i>
                        </a>` : ''
                    }
                    ${transaction.status === 'Completed' ? 
                        `<button class="btn btn-sm btn-outline" onclick="viewTransactionDetails(${transaction.transactionId})" title="Chi tiết">
                            <i class="fas fa-eye"></i>
                        </button>` : ''
                    }
                </div>
            </td>
        </tr>
    `).join('');
}

// Modal functions
function openAddFundModal() {
    const modal = document.getElementById('addFundModal');
    if (modal) {
        modal.classList.add('show');
        modal.style.display = 'flex';
    }
}

function closeAddFundModal() {
    const modal = document.getElementById('addFundModal');
    if (modal) {
        modal.classList.remove('show');
        modal.style.display = 'none';
        // Reset form
        const form = document.getElementById('addFundForm');
        if (form) {
            form.reset();
        }
    }
}

// Handle add fund transaction
async function handleAddFundTransaction(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const transactionData = {
        type: formData.get('type'),
        groupId: parseInt(formData.get('groupId')),
        amount: parseFloat(formData.get('amount')),
        description: formData.get('description'),
        transactionDate: formData.get('transactionDate')
    };
    
    try {
        const response = await fetch('/api/fund/transactions', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(transactionData)
        });
        
        if (response.ok) {
            showNotification('Thêm giao dịch thành công!', 'success');
            closeAddFundModal();
            loadFundStats();
            loadTransactions();
        } else {
            const error = await response.json();
            showNotification(error.message || 'Lỗi khi thêm giao dịch', 'error');
        }
    } catch (error) {
        console.error('Error adding transaction:', error);
        showNotification('Lỗi khi thêm giao dịch', 'error');
    }
}

// Export fund report
function exportFundReport() {
    // Create CSV content
    const csvContent = createFundReportCSV();
    
    // Download CSV
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `fund_report_${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

// Create fund report CSV
function createFundReportCSV() {
    const headers = ['Ngày', 'Loại', 'Mô tả', 'Nhóm', 'Số tiền', 'Người thực hiện'];
    const rows = transactions.map(t => [
        formatDate(t.transactionDate),
        t.type === 'INCOME' ? 'Thu nhập' : 'Chi phí',
        t.description || '',
        getGroupName(t.groupId),
        t.amount,
        t.createdBy || ''
    ]);
    
    const csvContent = [headers, ...rows]
        .map(row => row.map(field => `"${field}"`).join(','))
        .join('\n');
    
    return '\uFEFF' + csvContent; // Add BOM for UTF-8
}

// View all transactions
function viewAllTransactions() {
    // Scroll to transactions table
    const table = document.querySelector('.content-card:last-child');
    if (table) {
        table.scrollIntoView({ behavior: 'smooth' });
    }
}

// Edit transaction
function editTransaction(transactionId) {
    const transaction = transactions.find(t => t.id === transactionId);
    if (!transaction) return;
    
    // Fill form with transaction data
    document.getElementById('transactionType').value = transaction.type;
    document.getElementById('fundGroup').value = transaction.groupId;
    document.getElementById('fundAmount').value = transaction.amount;
    document.getElementById('fundDescription').value = transaction.description || '';
    document.getElementById('fundDate').value = transaction.transactionDate;
    
    // Open modal
    openAddFundModal();
    
    // Change form title and submit behavior
    const modalTitle = document.querySelector('#addFundModal .modal-header h3');
    if (modalTitle) {
        modalTitle.textContent = 'Chỉnh sửa giao dịch';
    }
    
    // Store transaction ID for update
    const form = document.getElementById('addFundForm');
    form.dataset.transactionId = transactionId;
}

// Delete transaction
async function deleteTransaction(transactionId) {
    if (!confirm('Bạn có chắc chắn muốn xóa giao dịch này?')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/fund/transactions/${transactionId}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            showNotification('Xóa giao dịch thành công!', 'success');
            loadFundStats();
            loadTransactions();
        } else {
            showNotification('Lỗi khi xóa giao dịch', 'error');
        }
    } catch (error) {
        console.error('Error deleting transaction:', error);
        showNotification('Lỗi khi xóa giao dịch', 'error');
    }
}

// Approve transaction (Admin only)
async function approveTransaction(transactionId) {
    if (!confirm('Xác nhận phê duyệt yêu cầu rút tiền này?')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/fund/transactions/${transactionId}/approve`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            }
        });
        
        if (response.ok) {
            showNotification('Phê duyệt thành công!', 'success');
            loadFundStats();
            loadTransactions();
            loadPendingApprovals();
        } else {
            const error = await response.json();
            showNotification(error.message || 'Lỗi khi phê duyệt', 'error');
        }
    } catch (error) {
        console.error('Error approving transaction:', error);
        showNotification('Lỗi khi phê duyệt', 'error');
    }
}

// Reject transaction (Admin only)
async function rejectTransaction(transactionId) {
    const reason = prompt('Lý do từ chối:');
    if (!reason) {
        return;
    }
    
    try {
        const response = await fetch(`/api/fund/transactions/${transactionId}/reject`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ reason })
        });
        
        if (response.ok) {
            showNotification('Đã từ chối yêu cầu', 'warning');
            loadFundStats();
            loadTransactions();
            loadPendingApprovals();
        } else {
            const error = await response.json();
            showNotification(error.message || 'Lỗi khi từ chối', 'error');
        }
    } catch (error) {
        console.error('Error rejecting transaction:', error);
        showNotification('Lỗi khi từ chối', 'error');
    }
}

// View transaction details
function viewTransactionDetails(transactionId) {
    const transaction = transactions.find(t => t.transactionId === transactionId) || 
                       pendingTransactions.find(t => t.transactionId === transactionId);
    if (!transaction) return;
    
    let detailsHtml = `
        <div style="padding: 1rem;">
            <h4 style="margin-bottom: 1rem;">Chi tiết giao dịch</h4>
            <div style="display: grid; gap: 0.75rem;">
                <div><strong>Mã giao dịch:</strong> #${transaction.transactionId}</div>
                <div><strong>Loại:</strong> ${transaction.transactionType === 'Deposit' ? 'Nạp tiền' : 'Rút tiền'}</div>
                <div><strong>Số tiền:</strong> ${formatCurrency(transaction.amount)}</div>
                <div><strong>Mục đích:</strong> ${transaction.purpose || '-'}</div>
                <div><strong>Trạng thái:</strong> ${getStatusText(transaction.status)}</div>
                <div><strong>Ngày tạo:</strong> ${formatDate(transaction.date)}</div>
                <div><strong>Người thực hiện:</strong> User ${transaction.userId}</div>
                ${transaction.receiptUrl ? 
                    `<div><strong>Hóa đơn:</strong> <a href="${transaction.receiptUrl}" target="_blank">Xem hóa đơn</a></div>` : ''
                }
                ${transaction.approvedBy ? 
                    `<div><strong>Người duyệt:</strong> Admin ${transaction.approvedBy}</div>` : ''
                }
                ${transaction.approvedAt ? 
                    `<div><strong>Ngày duyệt:</strong> ${formatDate(transaction.approvedAt)}</div>` : ''
                }
            </div>
        </div>
    `;
    
    // Create and show modal (simplified)
    alert(detailsHtml.replace(/<[^>]*>/g, '\n')); // For demo, replace with proper modal
}

// Helper: Get status text
function getStatusText(status) {
    const statusMap = {
        'Pending': 'Chờ duyệt',
        'Approved': 'Đã duyệt',
        'Rejected': 'Từ chối',
        'Completed': 'Hoàn tất'
    };
    return statusMap[status] || status;
}

// Helper: Get status icon
function getStatusIcon(status) {
    const iconMap = {
        'Pending': 'clock',
        'Approved': 'check-circle',
        'Rejected': 'times-circle',
        'Completed': 'check-double'
    };
    return iconMap[status] || 'circle';
}

// Utility functions
function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN');
}

function getGroupName(groupId) {
    const group = groups.find(g => g.id === groupId);
    return group ? group.name : 'Không xác định';
}

function showNotification(message, type = 'info') {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 1rem 1.5rem;
        border-radius: 0.5rem;
        color: white;
        font-weight: 500;
        z-index: 3000;
        animation: slideIn 0.3s ease;
        max-width: 400px;
    `;
    
    // Set background color based on type
    const colors = {
        success: '#10b981',
        error: '#ef4444',
        warning: '#f59e0b',
        info: '#3b82f6'
    };
    notification.style.backgroundColor = colors[type] || colors.info;
    
    notification.textContent = message;
    
    // Add to page
    document.body.appendChild(notification);
    
    // Remove after 3 seconds
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 300);
    }, 3000);
}

// Add CSS for notifications
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
    
    .transaction-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1rem;
        border-bottom: 1px solid #f1f5f9;
    }
    
    .transaction-item:last-child {
        border-bottom: none;
    }
    
    .transaction-info {
        display: flex;
        align-items: center;
        gap: 1rem;
    }
    
    .transaction-type {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.25rem 0.75rem;
        border-radius: 1rem;
        font-size: 0.8rem;
        font-weight: 500;
    }
    
    .transaction-type.income {
        background: #dcfce7;
        color: #166534;
    }
    
    .transaction-type.expense {
        background: #fef2f2;
        color: #991b1b;
    }
    
    .transaction-details {
        flex: 1;
    }
    
    .transaction-description {
        font-weight: 500;
        color: #1e293b;
        margin-bottom: 0.25rem;
    }
    
    .transaction-meta {
        font-size: 0.8rem;
        color: #64748b;
    }
    
    .transaction-amount {
        font-weight: 600;
        font-size: 1.1rem;
    }
    
    .transaction-amount.positive {
        color: #059669;
    }
    
    .transaction-amount.negative {
        color: #dc2626;
    }
    
    .type-badge {
        display: inline-flex;
        align-items: center;
        gap: 0.25rem;
        padding: 0.25rem 0.75rem;
        border-radius: 1rem;
        font-size: 0.8rem;
        font-weight: 500;
    }
    
    .type-badge.income {
        background: #dcfce7;
        color: #166534;
    }
    
    .type-badge.expense {
        background: #fef2f2;
        color: #991b1b;
    }
`;
document.head.appendChild(style);
