// Admin Payments Tracking Page JavaScript

// Use window scope to avoid conflicts with admin-common.js
if (typeof window.currentPaymentsData === 'undefined') {
    window.currentPaymentsData = [];
}
let currentPaymentsData = window.currentPaymentsData;

// Function to initialize payments page
function initializePaymentsPage() {
    console.log('🔵 [admin-payments.js] Admin Payments page initializing...');
    const tbody = document.getElementById('payments-tbody');
    if (!tbody) {
        console.warn('⚠️ [admin-payments.js] payments-tbody not found, retrying...');
        setTimeout(initializePaymentsPage, 200);
        return;
    }
    
    console.log('✅ [admin-payments.js] Found payments-tbody, loading payments...');
    // Load payments immediately - call the function directly
    console.log('✅ [admin-payments.js] Calling loadPaymentsAdmin()...');
    loadPaymentsAdmin();
    initPaymentFilters();
    initExportButton();
}

// Try multiple ways to ensure initialization
(function() {
    if (document.readyState === 'loading') {
        // DOM is still loading
        document.addEventListener('DOMContentLoaded', initializePaymentsPage);
    } else {
        // DOM is already loaded
        console.log('DOM already loaded, initializing immediately...');
        // Use setTimeout to ensure all scripts are loaded
        setTimeout(initializePaymentsPage, 100);
    }
    
    // Also try immediately (in case script loads after DOM is ready)
    setTimeout(function() {
        const tbody = document.getElementById('payments-tbody');
        if (tbody && tbody.innerHTML.includes('Đang tải dữ liệu')) {
            console.log('Found loading state, initializing payments...');
            initializePaymentsPage();
        }
    }, 200);
})();

// Fallback: also try on window load
window.addEventListener('load', function() {
    console.log('Window loaded, checking if payments loaded...');
    const tbody = document.getElementById('payments-tbody');
    if (tbody) {
        const tbodyContent = tbody.innerHTML.trim();
        // Check if still showing loading or empty
            if (tbodyContent.includes('Đang tải dữ liệu') || 
            tbodyContent.includes('Đang tải...') ||
            tbodyContent === '' ||
            (tbodyContent.includes('<tr>') && tbodyContent.includes('colspan="9"'))) {
            console.log('Payments not loaded yet, loading now...');
            if (typeof window.loadPaymentsAdmin === 'function') {
                window.loadPaymentsAdmin();
            } else if (typeof loadPayments === 'function') {
                loadPayments();
            }
        }
    }
});

// Load payments - explicit function name to avoid conflicts with admin-common.js
async function loadPaymentsAdmin(filters = {}) {
    console.log('🔵 [admin-payments.js] loadPaymentsAdmin() called with filters:', filters);
    
    // Expose function globally
window.loadPaymentsAdmin = loadPaymentsAdmin;
    const tbody = document.getElementById('payments-tbody');
    if (!tbody) {
        console.error('payments-tbody element not found');
        return;
    }
    
    try {
        // Show loading state
        tbody.innerHTML = `
            <tr>
                <td colspan="9" style="text-align: center; padding: 2rem;">
                    <i class="fas fa-spinner fa-spin"></i> Đang tải dữ liệu...
                </td>
            </tr>
        `;
        
        let url = '/api/payments/admin/tracking';
        const params = new URLSearchParams();
        if (filters.status) params.append('status', filters.status);
        if (filters.startDate) params.append('startDate', filters.startDate);
        if (filters.endDate) params.append('endDate', filters.endDate);
        if (filters.search) params.append('search', filters.search);
        
        if (params.toString()) {
            url += '?' + params.toString();
        }
        
        console.log('Fetching payments from:', url);
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        });
        
        if (!response.ok) {
            const errorText = await response.text();
            console.error('Response not OK:', response.status, errorText);
            throw new Error(`HTTP ${response.status}: ${errorText.substring(0, 100)}`);
        }
        
        const data = await response.json();
        console.log('Received data:', data);
        console.log('Payments count:', data.payments ? data.payments.length : 0);
        
        window.currentPaymentsData = data.payments || [];
        currentPaymentsData = window.currentPaymentsData;
        const stats = data.statistics || { total: 0, totalAmount: 0, paidCount: 0, pendingCount: 0 };
        
        console.log('Statistics:', stats);
        
        // Update statistics
        updatePaymentStats(stats);
        
        // Render table
        renderPaymentsTable(currentPaymentsData);
        
    } catch (error) {
        console.error('Error loading payments:', error);
        console.error('Error stack:', error.stack);
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="9" style="text-align: center; padding: 2rem; color: var(--danger);">
                        <i class="fas fa-exclamation-circle"></i> Lỗi khi tải dữ liệu: ${error.message}
                        <br><small style="color: var(--text-light); margin-top: 0.5rem; display: block;">
                            Vui lòng kiểm tra console để biết thêm chi tiết
                        </small>
                    </td>
                </tr>
            `;
        }
    }
}

// Update payment statistics
function updatePaymentStats(stats) {
    const totalPayments = document.getElementById('total-payments');
    const paidCount = document.getElementById('paid-count');
    const pendingCount = document.getElementById('pending-count');
    const totalAmount = document.getElementById('total-amount');
    
    if (totalPayments) totalPayments.textContent = stats.total || 0;
    if (paidCount) paidCount.textContent = stats.paidCount || 0;
    if (pendingCount) pendingCount.textContent = stats.pendingCount || 0;
    if (totalAmount) totalAmount.textContent = formatCurrency(stats.totalAmount || 0);
}

// Render payments table
function renderPaymentsTable(payments) {
    const tbody = document.getElementById('payments-tbody');
    
    if (!payments || payments.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="9" style="text-align: center; padding: 2rem; color: var(--text-light);">
                    <i class="fas fa-inbox"></i><br>Không có dữ liệu thanh toán
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = payments.map(payment => {
        const statusClass = getPaymentStatusClass(payment.status);
        const statusText = getPaymentStatusText(payment.status);
        
        // Handle paymentDate - could be string (ISO-8601) or LocalDateTime object from Java
        let paymentDate = '-';
        if (payment.paymentDate) {
            try {
                // If it's a LocalDateTime object from Java (has year, month, dayOfMonth, etc.)
                if (typeof payment.paymentDate === 'object' && payment.paymentDate.year) {
                    const date = payment.paymentDate;
                    paymentDate = new Date(
                        date.year, 
                        date.monthValue - 1, 
                        date.dayOfMonth,
                        date.hour || 0,
                        date.minute || 0,
                        date.second || 0
                    ).toLocaleDateString('vi-VN');
                } else if (typeof payment.paymentDate === 'string') {
                    // If it's a string (ISO-8601 format), parse it
                    paymentDate = new Date(payment.paymentDate).toLocaleDateString('vi-VN');
                } else {
                    paymentDate = String(payment.paymentDate);
                }
            } catch (e) {
                console.warn('Error parsing paymentDate:', payment.paymentDate, e);
                paymentDate = String(payment.paymentDate);
            }
        }
        
        const costType = payment.costType || '-';
        const method = payment.method || '-';
        const transactionCode = payment.transactionCode || '-';
        
        return `
            <tr>
                <td>${payment.paymentId}</td>
                <td>User #${payment.userId}</td>
                <td>${costType}</td>
                <td style="font-weight: bold; color: var(--primary);">${formatCurrency(payment.amount)}</td>
                <td>${method}</td>
                <td style="font-family: monospace; font-size: 0.85rem;">${transactionCode}</td>
                <td>${paymentDate}</td>
                <td><span class="status-badge ${statusClass}">${statusText}</span></td>
                <td>
                    <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
                        <button class="btn btn-sm" style="background: #10B981; color: white; padding: 0.5rem 0.75rem; border-radius: 6px; border: none; cursor: pointer; display: inline-flex; align-items: center; gap: 0.25rem; font-size: 0.875rem;" 
                                onclick="window.location.href='/admin/payments/edit/${payment.paymentId}'" title="Chỉnh sửa thanh toán">
                            <i class="fas fa-edit"></i>
                        </button>
                        ${payment.status === 'PENDING' ? `
                            <button class="btn btn-sm" style="background: #10B981; color: white; padding: 0.5rem 0.75rem;" 
                                    onclick="confirmPayment(${payment.paymentId})" title="Xác nhận thanh toán">
                                <i class="fas fa-check"></i>
                            </button>
                        ` : ''}
                        <button class="btn btn-sm" style="background: var(--danger); color: white; padding: 0.5rem 0.75rem;" 
                                onclick="deletePayment(${payment.paymentId})" title="Xóa">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

// Get payment status class
function getPaymentStatusClass(status) {
    const statusMap = {
        'PAID': 'paid',
        'PENDING': 'pending',
        'OVERDUE': 'overdue',
        'CANCELLED': 'cancelled'
    };
    return statusMap[status] || 'pending';
}

// Get payment status text
function getPaymentStatusText(status) {
    const statusMap = {
        'PAID': 'Đã thanh toán',
        'PENDING': 'Chờ thanh toán',
        'OVERDUE': 'Quá hạn',
        'CANCELLED': 'Đã hủy'
    };
    return statusMap[status] || status;
}

// Initialize payment filters
function initPaymentFilters() {
    const btnFilter = document.getElementById('btn-filter-payments');
    const btnReset = document.getElementById('btn-reset-filters');
    
    if (btnFilter) {
        btnFilter.addEventListener('click', function() {
            const filters = {
                status: document.getElementById('payment-status-filter')?.value || '',
                startDate: document.getElementById('payment-date-from')?.value || '',
                endDate: document.getElementById('payment-date-to')?.value || '',
                search: document.getElementById('payment-search')?.value || ''
            };
            if (typeof window.loadPaymentsAdmin === 'function') {
                window.loadPaymentsAdmin(filters);
            } else if (typeof loadPayments === 'function') {
                loadPayments(filters);
            }
        });
    }
    
    if (btnReset) {
        btnReset.addEventListener('click', function() {
            document.getElementById('payment-status-filter').value = '';
            document.getElementById('payment-date-from').value = '';
            document.getElementById('payment-date-to').value = '';
            document.getElementById('payment-search').value = '';
            if (typeof window.loadPaymentsAdmin === 'function') {
                window.loadPaymentsAdmin();
            } else if (typeof loadPayments === 'function') {
                loadPayments();
            }
        });
    }
}

// Initialize export button
function initExportButton() {
    const btnExport = document.getElementById('btn-export-payments');
    if (btnExport) {
        btnExport.addEventListener('click', exportPayments);
    }
}

// Export payments
function exportPayments() {
    alert('Tính năng xuất Excel đang được phát triển');
}


// Confirm payment
async function confirmPayment(paymentId) {
    if (!confirm('Xác nhận thanh toán này đã được thanh toán?')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/payments/${paymentId}/confirm`, {
            method: 'PUT'
        });
        
        if (response.ok) {
            alert('Xác nhận thanh toán thành công!');
            if (typeof window.loadPaymentsAdmin === 'function') {
                window.loadPaymentsAdmin();
            } else if (typeof loadPayments === 'function') {
                loadPayments();
            }
        } else {
            alert('Lỗi khi xác nhận thanh toán');
        }
    } catch (error) {
        console.error('Error confirming payment:', error);
        alert('Lỗi khi xác nhận thanh toán: ' + error.message);
    }
}

// Delete payment
async function deletePayment(paymentId) {
    if (!confirm(`Bạn có chắc chắn muốn xóa thanh toán #${paymentId}?`)) {
        return;
    }
    
    try {
        const response = await fetch(`/api/payments/${paymentId}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            alert('Xóa thanh toán thành công!');
            if (typeof window.loadPaymentsAdmin === 'function') {
                window.loadPaymentsAdmin();
            } else if (typeof loadPayments === 'function') {
                loadPayments();
            }
        } else {
            alert('Lỗi khi xóa thanh toán');
        }
    } catch (error) {
        console.error('Error deleting payment:', error);
        alert('Lỗi khi xóa thanh toán: ' + error.message);
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

