// Admin Funds Management Page JavaScript

let currentFundsData = [];

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', function() {
    console.log('Admin Funds page initializing...');
    window.loadFundManagementData();
    initFundFilters();
    initExportButton();
    
    // Close modal when clicking overlay
    const overlay = document.getElementById('modal-overlay');
    if (overlay) {
        overlay.addEventListener('click', function(e) {
            if (e.target === overlay) {
                // Close all modals
                document.querySelectorAll('.modal.active').forEach(modal => {
                    modal.classList.remove('active');
                });
                overlay.classList.remove('active');
            }
        });
    }
    
    // Close modal when pressing ESC key
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            const activeModal = document.querySelector('.modal.active');
            if (activeModal) {
                activeModal.classList.remove('active');
                if (overlay) {
                    overlay.classList.remove('active');
                }
            }
        }
    });
});

// Load fund management data
window.loadFundManagementData = async function() {
    try {
        // Load groups
        const groupsResponse = await fetch('/api/groups');
        const groups = groupsResponse.ok ? await groupsResponse.json() : [];
        
        // Populate group filter
        const groupFilter = document.getElementById('fund-group-filter');
        if (groupFilter) {
            groupFilter.innerHTML = '<option value="">Tất cả nhóm</option>' +
                groups.map(g => `<option value="${g.groupId}">${g.groupName || `Nhóm #${g.groupId}`}</option>`).join('');
        }
        
        // Load all funds with statistics
        const fundsData = await Promise.all(
            groups.map(async (group) => {
                try {
                    const fundResponse = await fetch(`/api/fund/group/${group.groupId}`);
                    if (fundResponse.ok) {
                        const fund = await fundResponse.json();
                        
                        // Fetch statistics for this fund
                        let statistics = null;
                        try {
                            const statsResponse = await fetch(`/api/funds/${fund.fundId}/statistics`);
                            if (statsResponse.ok) {
                                statistics = await statsResponse.json();
                            }
                        } catch (e) {
                            console.error(`Error loading statistics for fund ${fund.fundId}:`, e);
                        }
                        
                        // Fetch transaction count
                        let transactionCount = 0;
                        try {
                            const transResponse = await fetch(`/api/funds/${fund.fundId}/transactions`);
                            if (transResponse.ok) {
                                const transactions = await transResponse.json();
                                transactionCount = transactions ? transactions.length : 0;
                            }
                        } catch (e) {
                            console.error(`Error loading transactions for fund ${fund.fundId}:`, e);
                        }
                        
                        // Get balance from statistics (currentBalance) or fund object, or calculate from deposits/withdraws
                        let balance = 0;
                        if (statistics && statistics.currentBalance !== undefined) {
                            balance = statistics.currentBalance || 0;
                        } else if (fund.currentBalance !== undefined) {
                            balance = fund.currentBalance || 0;
                        } else if (fund.balance !== undefined) {
                            balance = fund.balance || 0;
                        } else if (statistics) {
                            // Calculate from deposits - withdraws if balance not available
                            const deposits = statistics.totalDeposit || 0;
                            const withdraws = statistics.totalWithdraw || 0;
                            balance = deposits - withdraws;
                        }
                        
                        return { 
                            ...fund, 
                            groupName: group.groupName || `Nhóm #${group.groupId}`, 
                            groupId: group.groupId,
                            balance: balance,
                            currentBalance: balance,
                            totalDeposit: statistics ? (statistics.totalDeposit || 0) : 0,
                            totalWithdraw: statistics ? (statistics.totalWithdraw || 0) : 0,
                            transactionCount: transactionCount
                        };
                    }
                } catch (e) {
                    console.warn(`No fund for group ${group.groupId}:`, e);
                }
                return null;
            })
        );
        
        const funds = fundsData.filter(f => f !== null);
        currentFundsData = funds;
        
        // Apply filters
        const filteredFunds = applyFundFilters(funds);
        
        // Update stats
        updateFundStats(funds);
        
        // Render funds table
        renderFundsTable(filteredFunds);
        
        // Load pending requests
        loadPendingWithdrawRequests();
        
    } catch (error) {
        console.error('Error loading fund management data:', error);
        const tbody = document.getElementById('funds-tbody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align: center; padding: 2rem; color: var(--danger);">
                        <i class="fas fa-exclamation-circle"></i> Lỗi khi tải dữ liệu: ${error.message}
                    </td>
                </tr>
            `;
        }
    }
}

// Apply fund filters
function applyFundFilters(funds) {
    const groupFilter = document.getElementById('fund-group-filter')?.value;
    const statusFilter = document.getElementById('fund-status-filter')?.value;
    const searchInput = document.getElementById('fund-search-input')?.value.toLowerCase();
    
    let filtered = funds;
    
    if (groupFilter) {
        filtered = filtered.filter(f => f.groupId == groupFilter);
    }
    
    if (statusFilter) {
        filtered = filtered.filter(f => (f.status || 'ACTIVE') === statusFilter);
    }
    
    if (searchInput) {
        filtered = filtered.filter(f => 
            (f.groupName && f.groupName.toLowerCase().includes(searchInput)) ||
            (f.fundId && f.fundId.toString().includes(searchInput))
        );
    }
    
    return filtered;
}

// Update fund statistics
function updateFundStats(funds) {
    // Use currentBalance first, then balance, then calculate from deposits - withdraws
    const totalBalance = funds.reduce((sum, f) => {
        const balance = f.currentBalance !== undefined ? f.currentBalance : 
                       (f.balance !== undefined ? f.balance : 
                       ((f.totalDeposit || 0) - (f.totalWithdraw || 0)));
        return sum + (balance || 0);
    }, 0);
    const totalDeposit = funds.reduce((sum, f) => sum + (f.totalDeposit || 0), 0);
    const totalWithdraw = funds.reduce((sum, f) => sum + (f.totalWithdraw || 0), 0);
    const pendingRequests = 0; // TODO: Load from API
    
    const totalFundBalance = document.getElementById('total-fund-balance');
    const totalDeposits = document.getElementById('total-deposits');
    const totalWithdraws = document.getElementById('total-withdraws');
    const pendingRequestsCount = document.getElementById('pending-requests-count');
    const totalFundsCount = document.getElementById('total-funds-count');
    
    if (totalFundBalance) totalFundBalance.textContent = formatCurrency(totalBalance);
    if (totalDeposits) totalDeposits.textContent = formatCurrency(totalDeposit);
    if (totalWithdraws) totalWithdraws.textContent = formatCurrency(totalWithdraw);
    if (pendingRequestsCount) pendingRequestsCount.textContent = pendingRequests;
    if (totalFundsCount) totalFundsCount.textContent = funds.length;
}

// Render funds table
function renderFundsTable(funds) {
    const tbody = document.getElementById('funds-tbody');
    
    if (!tbody) return;
    
    if (!funds || funds.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align: center; padding: 2rem; color: var(--text-light);">
                    <i class="fas fa-inbox"></i><br>Không có dữ liệu quỹ
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = funds.map(fund => {
        // Get balance: prefer currentBalance, then balance, then calculate from deposits - withdraws
        const balance = fund.currentBalance !== undefined ? fund.currentBalance : 
                       (fund.balance !== undefined ? fund.balance : 
                       ((fund.totalDeposit || 0) - (fund.totalWithdraw || 0)));
        
        return `
        <tr>
            <td>${fund.fundId}</td>
            <td>${fund.groupName}</td>
            <td style="font-weight: bold; color: var(--primary);">${formatCurrency(balance)}</td>
            <td>${formatCurrency(fund.totalDeposit || 0)}</td>
            <td>${formatCurrency(fund.totalWithdraw || 0)}</td>
            <td>${fund.transactionCount || 0}</td>
            <td>
                <span class="status-badge ${(fund.status || 'ACTIVE') === 'ACTIVE' ? 'paid' : 'pending'}">
                    ${(fund.status || 'ACTIVE') === 'ACTIVE' ? 'Hoạt động' : 'Không hoạt động'}
                </span>
            </td>
            <td>
                <div class="action-buttons">
                    <button class="btn-icon btn-info" onclick="window.viewFundDetails(${fund.fundId}, ${fund.groupId})" 
                            title="Xem chi tiết">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button class="btn-icon btn-success" onclick="window.openDepositModal(${fund.fundId}, ${fund.groupId})" 
                            title="Nạp tiền">
                        <i class="fas fa-arrow-down"></i>
                    </button>
                    <button class="btn-icon btn-warning" onclick="window.openWithdrawModal(${fund.fundId}, ${fund.groupId})" 
                            title="Rút tiền">
                        <i class="fas fa-arrow-up"></i>
                    </button>
                    <button class="btn-icon btn-primary" onclick="window.viewPendingRequests(${fund.fundId})" 
                            title="Yêu cầu chờ duyệt">
                        <i class="fas fa-hourglass-half"></i>
                    </button>
                </div>
            </td>
        </tr>
    `;
    }).join('');
}

// Initialize fund filters
function initFundFilters() {
    const searchInput = document.getElementById('fund-search-input');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                const filtered = applyFundFilters(currentFundsData);
                renderFundsTable(filtered);
            }
        });
    }
}

// Initialize export button
function initExportButton() {
    const btnExport = document.querySelector('button[onclick="exportFundReport()"]');
    if (btnExport) {
        btnExport.addEventListener('click', exportFundReport);
    }
}

// Export fund report
window.exportFundReport = function() {
    alert('Tính năng xuất báo cáo đang được phát triển');
}

// Load pending withdraw requests
window.loadPendingWithdrawRequests = async function() {
    try {
        const pendingList = document.getElementById('pending-requests-list');
        if (!pendingList) return;
        
        // Load all pending requests from all funds
        const allFunds = currentFundsData || [];
        let allPendingRequests = [];
        
        for (const fund of allFunds) {
            try {
                const response = await fetch(`/api/funds/${fund.fundId}/pending-requests`);
                if (response.ok) {
                    const requests = await response.json();
                    if (requests && requests.length > 0) {
                        allPendingRequests = allPendingRequests.concat(requests.map(r => ({
                            ...r,
                            fundId: fund.fundId,
                            groupName: fund.groupName
                        })));
                    }
                }
            } catch (e) {
                console.warn(`Error loading pending requests for fund ${fund.fundId}:`, e);
            }
        }
        
        // Update pending count
        const pendingRequestsCount = document.getElementById('pending-requests-count');
        if (pendingRequestsCount) {
            pendingRequestsCount.textContent = allPendingRequests.length;
        }
        
        // Render pending requests
        if (allPendingRequests.length === 0) {
            pendingList.innerHTML = '<p style="text-align: center; color: var(--text-light); padding: 1rem;">Không có yêu cầu chờ duyệt</p>';
        } else {
            pendingList.innerHTML = allPendingRequests.map(req => `
                <div class="pending-request-item" style="background: var(--card-bg); padding: 1rem; border-radius: 8px; margin-bottom: 0.5rem; border-left: 4px solid var(--warning);">
                    <div style="display: flex; justify-content: space-between; align-items: start;">
                        <div style="flex: 1;">
                            <div style="font-weight: bold; margin-bottom: 0.5rem;">
                                ${req.groupName || `Nhóm #${req.fundId}`} - ${formatCurrency(req.amount || 0)}
                            </div>
                            <div style="color: var(--text-light); font-size: 0.9rem; margin-bottom: 0.5rem;">
                                <strong>User ID:</strong> ${req.userId || '-'} | 
                                <strong>Ngày:</strong> ${req.createdAt ? new Date(req.createdAt).toLocaleDateString('vi-VN') : '-'}
                            </div>
                            ${req.purpose ? `<div style="color: var(--text-light); font-size: 0.9rem;">${req.purpose}</div>` : ''}
                        </div>
                        <div style="display: flex; gap: 0.5rem;">
                            <button class="btn btn-sm btn-success" onclick="approveWithdrawRequest(${req.transactionId || req.id})" title="Duyệt">
                                <i class="fas fa-check"></i>
                            </button>
                            <button class="btn btn-sm btn-danger" onclick="rejectWithdrawRequest(${req.transactionId || req.id})" title="Từ chối">
                                <i class="fas fa-times"></i>
                            </button>
                        </div>
                    </div>
                </div>
            `).join('');
        }
    } catch (error) {
        console.error('Error loading pending requests:', error);
        const pendingList = document.getElementById('pending-requests-list');
        if (pendingList) {
            pendingList.innerHTML = '<p style="text-align: center; color: var(--danger); padding: 1rem;">Lỗi khi tải yêu cầu chờ duyệt</p>';
        }
    }
}

// View fund details and transaction history
window.viewFundDetails = async function(fundId, groupId) {
    try {
        // Load fund details
        const fundResponse = await fetch(`/api/fund/group/${groupId}`);
        if (!fundResponse.ok) {
            alert('Không thể tải thông tin quỹ');
            return;
        }
        const fund = await fundResponse.json();
        
        // Load statistics to get accurate balance
        let statistics = null;
        try {
            const statsResponse = await fetch(`/api/funds/${fund.fundId}/statistics`);
            if (statsResponse.ok) {
                statistics = await statsResponse.json();
            }
        } catch (e) {
            console.error(`Error loading statistics:`, e);
        }
        
        // Get accurate balance
        let balance = 0;
        if (statistics && statistics.currentBalance !== undefined) {
            balance = statistics.currentBalance || 0;
        } else if (fund.currentBalance !== undefined) {
            balance = fund.currentBalance || 0;
        } else if (fund.balance !== undefined) {
            balance = fund.balance || 0;
        }
        
        // Load transactions
        const transResponse = await fetch(`/api/funds/${fundId}/transactions`);
        const transactions = transResponse.ok ? await transResponse.json() : [];
        
        // Show in modal
        const modal = document.getElementById('fund-detail-modal');
        const content = document.getElementById('fund-detail-content');
        const overlay = document.getElementById('modal-overlay');
        
        if (modal && content && overlay) {
            content.innerHTML = `
                <div style="margin-bottom: 2rem;">
                    <h4 style="margin-bottom: 1rem; color: var(--primary);">Thông tin quỹ</h4>
                    <div class="info-box" style="background: var(--light); padding: 1rem; border-radius: 8px;">
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                            <div>
                                <strong>ID Quỹ:</strong> ${fundId}
                            </div>
                            <div>
                                <strong>Số dư:</strong> <span style="color: var(--primary); font-weight: bold;">${formatCurrency(balance)}</span>
                            </div>
                            <div>
                                <strong>Nhóm:</strong> ${fund?.groupId || groupId}
                            </div>
                            <div>
                                <strong>Trạng thái:</strong> 
                                <span class="status-badge ${(fund?.status || 'ACTIVE') === 'ACTIVE' ? 'paid' : 'pending'}">
                                    ${(fund?.status || 'ACTIVE') === 'ACTIVE' ? 'Hoạt động' : 'Không hoạt động'}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div>
                    <h4 style="margin-bottom: 1rem; color: var(--primary);">Lịch sử giao dịch</h4>
                    <button class="btn btn-primary btn-sm" onclick="window.viewFundTransactions(${fundId})" style="margin-bottom: 1rem;">
                        <i class="fas fa-list"></i> Xem chi tiết giao dịch
                    </button>
                    ${transactions.length > 0 ? `
                        <div class="data-table">
                            <table>
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>Loại</th>
                                        <th>Số tiền</th>
                                        <th>Ngày</th>
                                        <th>Trạng thái</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${transactions.slice(0, 5).map(t => `
                                        <tr>
                                            <td>${t.transactionId || t.id}</td>
                                            <td>${t.type === 'DEPOSIT' ? '<span style="color: green;">Nạp</span>' : '<span style="color: orange;">Rút</span>'}</td>
                                            <td>${formatCurrency(t.amount || 0)}</td>
                                            <td>${t.createdAt ? new Date(t.createdAt).toLocaleDateString('vi-VN') : '-'}</td>
                                            <td>
                                                <span class="status-badge ${t.status === 'APPROVED' ? 'paid' : t.status === 'PENDING' ? 'pending' : 'cancelled'}">
                                                    ${t.status === 'APPROVED' ? 'Đã duyệt' : t.status === 'PENDING' ? 'Chờ duyệt' : 'Từ chối'}
                                                </span>
                                            </td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                            ${transactions.length > 5 ? `<p style="text-align: center; margin-top: 1rem; color: var(--text-light);">Và ${transactions.length - 5} giao dịch khác...</p>` : ''}
                        </div>
                    ` : '<p style="text-align: center; color: var(--text-light); padding: 2rem;">Chưa có giao dịch nào</p>'}
                </div>
            `;
            
            modal.classList.add('active');
            overlay.classList.add('active');
        }
    } catch (error) {
        console.error('Error loading fund details:', error);
        alert('Lỗi khi tải thông tin quỹ: ' + error.message);
    }
}

// View fund transactions in modal
window.viewFundTransactions = async function(fundId) {
    try {
        const response = await fetch(`/api/funds/${fundId}/transactions`);
        const transactions = response.ok ? await response.json() : [];
        
        const modal = document.getElementById('fund-transactions-modal');
        const content = document.getElementById('fund-transactions-content');
        const overlay = document.getElementById('modal-overlay');
        
        if (modal && content && overlay) {
            // Store fundId for filter
            window.currentFundId = fundId;
            
            content.innerHTML = transactions.length > 0 ? `
                <div class="data-table">
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Loại</th>
                                <th>Số tiền</th>
                                <th>User ID</th>
                                <th>Mục đích</th>
                                <th>Ngày</th>
                                <th>Trạng thái</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${transactions.map(t => `
                                <tr>
                                    <td>${t.transactionId || t.id}</td>
                                    <td>${t.type === 'DEPOSIT' ? '<span style="color: green;"><i class="fas fa-arrow-down"></i> Nạp</span>' : '<span style="color: orange;"><i class="fas fa-arrow-up"></i> Rút</span>'}</td>
                                    <td style="font-weight: bold;">${formatCurrency(t.amount || 0)}</td>
                                    <td>${t.userId || '-'}</td>
                                    <td>${t.purpose || '-'}</td>
                                    <td>${t.createdAt ? new Date(t.createdAt).toLocaleDateString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '-'}</td>
                                    <td>
                                        <span class="status-badge ${t.status === 'APPROVED' ? 'paid' : t.status === 'PENDING' ? 'pending' : 'cancelled'}">
                                            ${t.status === 'APPROVED' ? 'Đã duyệt' : t.status === 'PENDING' ? 'Chờ duyệt' : 'Từ chối'}
                                        </span>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            ` : '<p style="text-align: center; color: var(--text-light); padding: 2rem;">Chưa có giao dịch nào</p>';
            
            // Close fund detail modal and open transactions modal
            window.closeFundDetailModal();
            modal.classList.add('active');
            overlay.classList.add('active');
        }
    } catch (error) {
        console.error('Error loading transactions:', error);
        alert('Lỗi khi tải lịch sử giao dịch: ' + error.message);
    }
}

// Open deposit modal
window.openDepositModal = async function(fundId, groupId) {
    try {
        const fundResponse = await fetch(`/api/fund/group/${groupId}`);
        const fund = fundResponse.ok ? await fundResponse.json() : null;
        
        // Load statistics to get accurate balance
        let balance = 0;
        if (fund) {
            try {
                const statsResponse = await fetch(`/api/funds/${fund.fundId}/statistics`);
                if (statsResponse.ok) {
                    const statistics = await statsResponse.json();
                    balance = statistics.currentBalance !== undefined ? statistics.currentBalance : 
                             (fund.currentBalance !== undefined ? fund.currentBalance : 
                             (fund.balance !== undefined ? fund.balance : 0));
                } else {
                    balance = fund.currentBalance !== undefined ? fund.currentBalance : 
                             (fund.balance !== undefined ? fund.balance : 0);
                }
            } catch (e) {
                balance = fund.currentBalance !== undefined ? fund.currentBalance : 
                         (fund.balance !== undefined ? fund.balance : 0);
            }
        }
        
        const fundData = currentFundsData.find(f => f.fundId === fundId);
        
        const modal = document.getElementById('deposit-fund-modal');
        const overlay = document.getElementById('modal-overlay');
        
        if (modal && overlay) {
            document.getElementById('deposit-fund-id').value = fundId;
            document.getElementById('deposit-group-id').value = groupId;
            document.getElementById('deposit-group-name').textContent = fundData?.groupName || `Nhóm #${groupId}`;
            document.getElementById('deposit-current-balance').textContent = formatCurrency(balance);
            
            // Reset form
            document.getElementById('deposit-fund-form').reset();
            document.getElementById('deposit-fund-id').value = fundId;
            document.getElementById('deposit-group-id').value = groupId;
            
            modal.classList.add('active');
            overlay.classList.add('active');
        }
    } catch (error) {
        console.error('Error opening deposit modal:', error);
        alert('Lỗi khi mở form nạp tiền');
    }
}

// Close deposit modal
window.closeDepositFundModal = function() {
    const modal = document.getElementById('deposit-fund-modal');
    const overlay = document.getElementById('modal-overlay');
    if (modal && overlay) {
        modal.classList.remove('active');
        overlay.classList.remove('active');
    }
}

// Open withdraw modal
window.openWithdrawModal = async function(fundId, groupId) {
    try {
        const fundResponse = await fetch(`/api/fund/group/${groupId}`);
        const fund = fundResponse.ok ? await fundResponse.json() : null;
        
        // Load statistics to get accurate balance
        let balance = 0;
        if (fund) {
            try {
                const statsResponse = await fetch(`/api/funds/${fund.fundId}/statistics`);
                if (statsResponse.ok) {
                    const statistics = await statsResponse.json();
                    balance = statistics.currentBalance !== undefined ? statistics.currentBalance : 
                             (fund.currentBalance !== undefined ? fund.currentBalance : 
                             (fund.balance !== undefined ? fund.balance : 0));
                } else {
                    balance = fund.currentBalance !== undefined ? fund.currentBalance : 
                             (fund.balance !== undefined ? fund.balance : 0);
                }
            } catch (e) {
                balance = fund.currentBalance !== undefined ? fund.currentBalance : 
                         (fund.balance !== undefined ? fund.balance : 0);
            }
        }
        
        const fundData = currentFundsData.find(f => f.fundId === fundId);
        
        const modal = document.getElementById('withdraw-fund-modal');
        const overlay = document.getElementById('modal-overlay');
        
        if (modal && overlay) {
            document.getElementById('withdraw-fund-id').value = fundId;
            document.getElementById('withdraw-group-id').value = groupId;
            document.getElementById('withdraw-group-name').textContent = fundData?.groupName || `Nhóm #${groupId}`;
            document.getElementById('withdraw-current-balance').textContent = formatCurrency(balance);
            
            // Reset form
            document.getElementById('withdraw-fund-form').reset();
            document.getElementById('withdraw-fund-id').value = fundId;
            document.getElementById('withdraw-group-id').value = groupId;
            
            modal.classList.add('active');
            overlay.classList.add('active');
        }
    } catch (error) {
        console.error('Error opening withdraw modal:', error);
        alert('Lỗi khi mở form rút tiền');
    }
}

// Close withdraw modal
window.closeWithdrawFundModal = function() {
    const modal = document.getElementById('withdraw-fund-modal');
    const overlay = document.getElementById('modal-overlay');
    if (modal && overlay) {
        modal.classList.remove('active');
        overlay.classList.remove('active');
    }
}

// Close fund detail modal
window.closeFundDetailModal = function() {
    const modal = document.getElementById('fund-detail-modal');
    const overlay = document.getElementById('modal-overlay');
    if (modal && overlay) {
        modal.classList.remove('active');
        overlay.classList.remove('active');
    }
}

// Close fund transactions modal
window.closeFundTransactionsModal = function() {
    const modal = document.getElementById('fund-transactions-modal');
    const overlay = document.getElementById('modal-overlay');
    if (modal && overlay) {
        modal.classList.remove('active');
        overlay.classList.remove('active');
    }
}

// Handle deposit form submit
document.addEventListener('DOMContentLoaded', function() {
    const depositForm = document.getElementById('deposit-fund-form');
    if (depositForm) {
        depositForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const fundId = document.getElementById('deposit-fund-id').value;
            const userId = document.getElementById('deposit-user-id').value;
            const amount = parseFloat(document.getElementById('deposit-amount').value);
            const purpose = document.getElementById('deposit-purpose').value;
            const receiptUrl = document.getElementById('deposit-receipt-url').value;
            
            if (!fundId || !userId || !amount || amount < 1000) {
                alert('Vui lòng điền đầy đủ thông tin. Số tiền tối thiểu là 1,000 ₫');
                return;
            }
            
            try {
                const response = await fetch('/api/fund/deposit', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        fundId: parseInt(fundId),
                        userId: parseInt(userId),
                        amount: amount,
                        purpose: purpose || null,
                        receiptUrl: receiptUrl || null
                    })
                });
                
                const result = await response.json();
                
                if (response.ok && result.success !== false) {
                    alert('✅ Nạp tiền thành công!');
                    window.closeDepositFundModal();
                    window.loadFundManagementData();
                } else {
                    alert('Lỗi: ' + (result.error || result.message || 'Không thể nạp tiền'));
                }
            } catch (error) {
                console.error('Error depositing:', error);
                alert('Lỗi khi nạp tiền: ' + error.message);
            }
        });
    }
    
    // Handle withdraw form submit
    const withdrawForm = document.getElementById('withdraw-fund-form');
    if (withdrawForm) {
        withdrawForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const fundId = document.getElementById('withdraw-fund-id').value;
            const userId = document.getElementById('withdraw-user-id').value;
            const amount = parseFloat(document.getElementById('withdraw-amount').value);
            const purpose = document.getElementById('withdraw-purpose').value;
            const receiptUrl = document.getElementById('withdraw-receipt-url').value;
            
            if (!fundId || !userId || !amount || amount < 1000 || !purpose) {
                alert('Vui lòng điền đầy đủ thông tin. Số tiền tối thiểu là 1,000 ₫');
                return;
            }
            
            if (!confirm(`Bạn có chắc chắn muốn rút ${formatCurrency(amount)} từ quỹ này?`)) {
                return;
            }
            
            try {
                const response = await fetch('/api/fund/withdraw/admin', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        fundId: parseInt(fundId),
                        userId: parseInt(userId),
                        amount: amount,
                        purpose: purpose,
                        receiptUrl: receiptUrl || null
                    })
                });
                
                const result = await response.json();
                
                if (response.ok && result.success !== false) {
                    alert('✅ Rút tiền thành công!');
                    window.closeWithdrawFundModal();
                    window.loadFundManagementData();
                } else {
                    alert('Lỗi: ' + (result.error || result.message || 'Không thể rút tiền'));
                }
            } catch (error) {
                console.error('Error withdrawing:', error);
                alert('Lỗi khi rút tiền: ' + error.message);
            }
        });
    }
});

// View pending requests for a specific fund
window.viewPendingRequests = async function(fundId) {
    try {
        const response = await fetch(`/api/funds/${fundId}/pending-requests`);
        const requests = response.ok ? await response.json() : [];
        
        if (requests.length === 0) {
            alert('Không có yêu cầu chờ duyệt cho quỹ này');
            return;
        }
        
        // Scroll to pending requests section
        const pendingSection = document.querySelector('.pending-requests-section');
        if (pendingSection) {
            pendingSection.scrollIntoView({ behavior: 'smooth' });
        }
        
        // Reload to show all pending requests
        window.loadPendingWithdrawRequests();
    } catch (error) {
        console.error('Error loading pending requests:', error);
        alert('Lỗi khi tải yêu cầu chờ duyệt');
    }
}

// Approve withdraw request
window.approveWithdrawRequest = async function(transactionId) {
    if (!confirm('Bạn có chắc chắn muốn duyệt yêu cầu rút tiền này?')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/fund/transactions/${transactionId}/approve`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        const result = await response.json();
        
        if (response.ok && result.success !== false) {
            alert('✅ Đã duyệt yêu cầu rút tiền!');
            window.loadFundManagementData();
        } else {
            alert('Lỗi: ' + (result.error || result.message || 'Không thể duyệt yêu cầu'));
        }
    } catch (error) {
        console.error('Error approving request:', error);
        alert('Lỗi khi duyệt yêu cầu: ' + error.message);
    }
}

// Reject withdraw request
window.rejectWithdrawRequest = async function(transactionId) {
    const note = prompt('Nhập lý do từ chối (tùy chọn):');
    
    if (!confirm('Bạn có chắc chắn muốn từ chối yêu cầu rút tiền này?')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/fund/transactions/${transactionId}/reject`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                note: note || null
            })
        });
        
        const result = await response.json();
        
        if (response.ok && result.success !== false) {
            alert('✅ Đã từ chối yêu cầu rút tiền!');
            window.loadFundManagementData();
        } else {
            alert('Lỗi: ' + (result.error || result.message || 'Không thể từ chối yêu cầu'));
        }
    } catch (error) {
        console.error('Error rejecting request:', error);
        alert('Lỗi khi từ chối yêu cầu: ' + error.message);
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

