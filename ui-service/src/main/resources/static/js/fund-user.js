// ========================================
// FUND USER JS - Giao diện User
// ========================================

const API_BASE_URL = '/api/fund';
const CURRENT_USER_ID = 1; // TODO: Get from session

// ========================================
// INITIALIZATION
// ========================================

document.addEventListener('DOMContentLoaded', function() {
    loadGroups();
    loadFundStats();
    loadMyPendingRequests();
    loadPendingVoteRequests();
    loadRecentTransactions();
    loadTransactionHistory();
    
    // Event listeners
    document.getElementById('depositForm').addEventListener('submit', handleDeposit);
    document.getElementById('withdrawVoteForm').addEventListener('submit', handleWithdrawVote);
    document.getElementById('filterStatus').addEventListener('change', loadTransactionHistory);
    document.getElementById('filterType').addEventListener('change', loadTransactionHistory);
    
    // Auto refresh every 30s
    setInterval(() => {
        loadGroups(); // Refresh group list to update fund status
        loadFundStats();
        loadMyPendingRequests();
        loadPendingVoteRequests();
        loadRecentTransactions();
    }, 30000);
});

// ========================================
// LOAD DATA
// ========================================

// Load groups for dropdowns
async function loadGroups() {
    try {
        const response = await fetch('/groups/api/all');
        if (!response.ok) throw new Error('Failed to load groups');
        
        const groups = await response.json();
        console.log('📦 [USER] Loaded groups:', groups);
        
        // Fetch fundId for each group
        const groupsWithFunds = await Promise.all(
            groups.map(async (group) => {
                try {
                    const fundResponse = await fetch(`${API_BASE_URL}/group/${group.groupId}`);
                    if (fundResponse.ok) {
                        const fund = await fundResponse.json();
                        return {
                            ...group,
                            fundId: fund.fundId
                        };
                    } else {
                        // Fund không tồn tại, không có vấn đề gì
                        console.log(`ℹ️ No fund found for group ${group.groupId} (this is OK)`);
                    }
                } catch (e) {
                    // Fund không tồn tại, không có vấn đề gì
                    console.log(`ℹ️ No fund found for group ${group.groupId} (this is OK)`);
                }
                return group;
            })
        );
        
        console.log('💰 [USER] Groups with fund info:', groupsWithFunds);
        
        // Populate deposit dropdown - hiển thị tất cả nhóm (dùng groupId)
        const depositSelect = document.getElementById('depositGroup');
        if (depositSelect) {
            depositSelect.innerHTML = '<option value="">Chọn nhóm</option>' +
                groupsWithFunds
                    .map(g => `<option value="${g.groupId}" data-fund-id="${g.fundId || ''}">${g.groupName}${g.fundId ? '' : ' (chưa có quỹ)'}</option>`)
                    .join('');
            console.log('✅ Populated depositGroup dropdown');
        }
        
        // Populate withdraw dropdown - chỉ nhóm có quỹ mới rút được
        const withdrawSelect = document.getElementById('withdrawGroup');
        if (withdrawSelect) {
            withdrawSelect.innerHTML = '<option value="">Chọn nhóm</option>' +
                groupsWithFunds
                    .filter(g => g.fundId)
                    .map(g => `<option value="${g.groupId}" data-fund-id="${g.fundId}">${g.groupName}</option>`)
                    .join('');
            console.log('✅ Populated withdrawGroup dropdown');
        }
        
    } catch (error) {
        console.error('❌ Error loading groups:', error);
        
        // Restore empty state on error
        const depositSelect = document.getElementById('depositGroup');
        const withdrawSelect = document.getElementById('withdrawGroup');
        if (depositSelect) depositSelect.innerHTML = '<option value="">Không thể tải nhóm</option>';
        if (withdrawSelect) withdrawSelect.innerHTML = '<option value="">Không thể tải nhóm</option>';
    }
}

async function loadFundStats() {
    try {
        const response = await fetch(`${API_BASE_URL}/stats`);
        if (!response.ok) throw new Error('Failed to load stats');
        
        const stats = await response.json();
        
        // Update stats cards
        document.getElementById('totalBalance').textContent = formatCurrency(stats.totalBalance);
        document.getElementById('myDeposits').textContent = formatCurrency(stats.myDeposits || 0);
        document.getElementById('myWithdraws').textContent = formatCurrency(stats.myWithdraws || 0);
        document.getElementById('myPending').textContent = stats.myPendingCount || 0;
        
        // Update summary
        document.getElementById('summaryOpening').textContent = formatCurrency(stats.openingBalance);
        document.getElementById('summaryIncome').textContent = formatCurrency(stats.totalIncome);
        document.getElementById('summaryExpense').textContent = formatCurrency(stats.totalExpense);
        document.getElementById('summaryBalance').textContent = formatCurrency(stats.totalBalance);
        
    } catch (error) {
        console.error('Error loading stats:', error);
    }
}

async function loadMyPendingRequests() {
    try {
        console.log('🔍 Loading my pending withdrawal requests for user:', CURRENT_USER_ID);
        
        // Lấy tất cả transactions của user từ API
        const response = await fetch(`${API_BASE_URL}/transactions/user/${CURRENT_USER_ID}`);
        if (!response.ok) {
            console.error('❌ Failed to load user transactions:', response.status, response.statusText);
            throw new Error('Failed to load pending requests');
        }
        
        const transactions = await response.json();
        console.log('📋 All user transactions:', transactions);
        
        // Filter: chỉ các withdrawal requests với status Pending và của user này
        const myRequests = transactions.filter(t => {
            const transactionType = t.transactionType || t.transaction_type;
            const status = t.status || t.transaction_status;
            const userId = t.userId || t.user_id || t.createdBy;
            
            const isWithdraw = transactionType === 'Withdraw' || transactionType === 'WITHDRAW';
            const isPending = status === 'Pending' || status === 'PENDING';
            const isMyRequest = userId === CURRENT_USER_ID || userId === parseInt(CURRENT_USER_ID);
            
            console.log(`Checking transaction ${t.transactionId}: type=${transactionType}, status=${status}, userId=${userId}, isMyRequest=${isMyRequest}`);
            
            return isWithdraw && isPending && isMyRequest;
        });
        
        console.log('✅ My pending withdrawal requests:', myRequests);
        updateMyPendingDisplay(myRequests);
        
    } catch (error) {
        console.error('❌ Error loading my pending requests:', error);
        // Hiển thị empty state nếu có lỗi
        updateMyPendingDisplay([]);
    }
}

function updateMyPendingDisplay(requests) {
    const badge = document.getElementById('myPendingBadge');
    const tbody = document.getElementById('myPendingBody');
    
    if (!badge || !tbody) {
        console.error('❌ Missing DOM elements for my pending requests');
        return;
    }
    
    badge.textContent = requests.length;
    
    if (requests.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="empty-table">
                    <div class="empty-state">
                        <i class="fas fa-check-circle"></i>
                        <p>Không có phiếu nào đang chờ</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = requests.map(t => {
        const date = t.date || t.createdAt || t.created_at;
        const transactionId = t.transactionId || t.transaction_id;
        const amount = t.amount || 0;
        const purpose = t.purpose || '-';
        const status = t.status || t.transaction_status || 'Pending';
        const voteId = t.voteId || t.vote_id;
        const groupId = t.groupId || t.group_id;
        
        return `
        <tr>
            <td>${formatDate(date)}</td>
            <td class="amount negative">
                ${formatCurrency(amount)}
            </td>
            <td>${purpose}</td>
            <td>
                <span class="badge badge-${getStatusClass(status)}">
                    ${getStatusIcon(status)} ${getStatusText(status)}
                </span>
            </td>
            <td>
                ${(voteId && groupId && typeof window.viewDecisionDetail === 'function')
                    ? `<button class="btn btn-sm btn-outline" onclick="viewDecisionDetail(${groupId}, ${voteId})">
                         <i class="fas fa-poll"></i> Xem phiếu vote
                       </button>`
                    : (voteId && groupId
                        ? `<a href="/groups/${groupId}/votes?voteId=${voteId}" class="btn btn-sm btn-outline">
                             <i class="fas fa-poll"></i> Xem phiếu vote
                           </a>`
                        : '<span class="text-muted">Chưa có vote</span>')
                }
            </td>
            <td>
                <button class="btn btn-sm btn-outline" onclick="viewTransactionDetail(${transactionId})">
                    <i class="fas fa-eye"></i>
                </button>
                ${status === 'Pending' || status === 'PENDING'
                    ? `<button class="btn btn-sm btn-danger" onclick="cancelRequest(${transactionId})">
                         <i class="fas fa-times"></i>
                       </button>`
                    : ''
                }
            </td>
        </tr>
        `;
    }).join('');
}

/**
 * Load các withdrawal requests từ thành viên khác mà user cần vote
 */
async function loadPendingVoteRequests() {
    try {
        console.log('🔍 Loading pending vote requests for user:', CURRENT_USER_ID);
        
        // Lấy danh sách các nhóm mà user tham gia
        const groupsResponse = await fetch(`/api/groups/user/${CURRENT_USER_ID}`);
        if (!groupsResponse.ok) {
            console.error('❌ Failed to load user groups');
            updatePendingVoteDisplay([]);
            return;
        }
        
        const groups = await groupsResponse.json();
        console.log('📋 User groups:', groups);
        
        const allPendingRequests = [];
        
        // Với mỗi nhóm, lấy fund và pending requests
        for (const group of groups) {
            try {
                // Lấy fund của nhóm
                const fundResponse = await fetch(`${API_BASE_URL}/group/${group.groupId}`);
                if (!fundResponse.ok) continue;
                
                const fund = await fundResponse.json();
                if (!fund || !fund.fundId) continue;
                
                const fundId = fund.fundId;
                
                // Lấy pending requests của fund này
                const pendingUrl = `/api/funds/${fundId}/pending-requests`;
                console.log(`🔍 Fetching pending requests from: ${pendingUrl}`);
                const requestsResponse = await fetch(pendingUrl);
                if (!requestsResponse.ok) continue;
                
                const requests = await requestsResponse.json();
                if (!Array.isArray(requests)) continue;
                
                console.log(`📋 Found ${requests.length} pending requests for fund ${fundId}`);
                
                // Filter: chỉ các withdrawal requests không phải của user này
                requests.forEach(req => {
                    const transactionType = req.transactionType || req.transaction_type;
                    const status = req.status || req.transaction_status;
                    const userId = req.userId || req.user_id || req.createdBy;
                    
                    const isWithdraw = transactionType === 'Withdraw' || transactionType === 'WITHDRAW';
                    const isPending = status === 'Pending' || status === 'PENDING';
                    const isNotMyRequest = userId !== CURRENT_USER_ID && userId !== parseInt(CURRENT_USER_ID);
                    
                    if (isWithdraw && isPending && isNotMyRequest) {
                        allPendingRequests.push({
                            ...req,
                            groupName: group.groupName || group.group_name || `Nhóm ${group.groupId}`,
                            groupId: group.groupId,
                            fundId: fundId,
                            requesterId: userId
                        });
                    }
                });
            } catch (e) {
                console.warn(`Error loading requests for group ${group.groupId}:`, e);
            }
        }
        
        console.log('✅ Pending vote requests:', allPendingRequests);
        updatePendingVoteDisplay(allPendingRequests);
        
    } catch (error) {
        console.error('❌ Error loading pending vote requests:', error);
        updatePendingVoteDisplay([]);
    }
}

/**
 * Hiển thị danh sách các withdrawal requests cần vote
 */
function updatePendingVoteDisplay(requests) {
    // Tìm section để hiển thị, nếu không có thì sẽ tích hợp vào section hiện có
    let voteSection = document.getElementById('pendingVoteSection');
    let voteBadge = document.getElementById('pendingVoteBadge');
    let voteBody = document.getElementById('pendingVoteBody');
    
    // Nếu không có section riêng, có thể tích hợp vào section "Phiếu rút tiền của tôi"
    // hoặc tạo section mới động
    
    if (!voteSection && requests.length > 0) {
        // Tạo section mới sau section "Phiếu rút tiền của tôi"
        const myPendingCard = document.getElementById('myPendingCard');
        if (myPendingCard && myPendingCard.parentNode) {
            const newSection = document.createElement('div');
            newSection.className = 'card';
            newSection.id = 'pendingVoteSection';
            newSection.innerHTML = `
                <div class="card-header">
                    <h3>
                        <i class="fas fa-bell"></i>
                        Yêu cầu rút tiền cần bỏ phiếu
                    </h3>
                    <span class="badge badge-primary" id="pendingVoteBadge">${requests.length}</span>
                </div>
                <div class="card-content">
                    <div class="table-container">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Người yêu cầu</th>
                                    <th>Ngày tạo</th>
                                    <th>Số tiền</th>
                                    <th>Mục đích</th>
                                    <th>Nhóm</th>
                                    <th>Thao tác</th>
                                </tr>
                            </thead>
                            <tbody id="pendingVoteBody">
                            </tbody>
                        </table>
                    </div>
                </div>
            `;
            myPendingCard.parentNode.insertBefore(newSection, myPendingCard.nextSibling);
            voteSection = newSection;
            voteBadge = document.getElementById('pendingVoteBadge');
            voteBody = document.getElementById('pendingVoteBody');
        }
    }
    
    if (!voteBody) {
        // Nếu không thể tạo section, chỉ log
        console.log('📋 Pending vote requests (no display):', requests);
        return;
    }
    
    if (voteBadge) voteBadge.textContent = requests.length;
    
    // Cập nhật notification badge ở header
    const headerBadge = document.getElementById('pendingVoteBadgeHeader');
    if (headerBadge) {
        if (requests.length > 0) {
            headerBadge.textContent = requests.length;
            headerBadge.style.display = 'inline-block';
        } else {
            headerBadge.style.display = 'none';
        }
    }
    
    if (requests.length === 0) {
        if (voteSection) voteSection.style.display = 'none';
        return;
    }
    
    if (voteSection) voteSection.style.display = 'block';
    
    voteBody.innerHTML = requests.map(req => {
        const date = req.date || req.createdAt || req.created_at;
        const transactionId = req.transactionId || req.transaction_id;
        const amount = req.amount || 0;
        const purpose = req.purpose || '-';
        const requesterId = req.requesterId || req.userId || req.user_id;
        const groupName = req.groupName || `Nhóm ${req.groupId}`;
        const fundId = req.fundId;
        
        return `
        <tr>
            <td>
                <strong>User #${requesterId}</strong>
            </td>
            <td>${formatDate(date)}</td>
            <td class="amount negative">
                ${formatCurrency(amount)}
            </td>
            <td>${purpose}</td>
            <td>${groupName}</td>
            <td>
                <div style="display: flex; gap: 0.5rem;">
                    <button class="btn btn-sm btn-success" onclick="voteOnWithdrawRequest(${transactionId}, ${fundId}, true)" title="Đồng ý">
                        <i class="fas fa-check"></i> Đồng ý
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="voteOnWithdrawRequest(${transactionId}, ${fundId}, false)" title="Từ chối">
                        <i class="fas fa-times"></i> Từ chối
                    </button>
                </div>
            </td>
        </tr>
        `;
    }).join('');
}

/**
 * Vote cho withdrawal request (approve hoặc reject)
 */
async function voteOnWithdrawRequest(transactionId, fundId, approve) {
    if (!confirm(approve 
        ? 'Bạn có chắc chắn muốn đồng ý yêu cầu rút tiền này không?'
        : 'Bạn có chắc chắn muốn từ chối yêu cầu rút tiền này không?')) {
        return;
    }
    
    try {
        const url = `${API_BASE_URL}/transactions/${transactionId}/vote`;
        
        console.log(`🗳️ Voting ${approve ? 'approve' : 'reject'} for transaction ${transactionId}`);
        
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                transactionId: transactionId,
                userId: CURRENT_USER_ID,
                approve: approve
            })
        });
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ error: await response.text() }));
            throw new Error(errorData.error || 'Failed to vote');
        }
        
        const result = await response.json();
        console.log('✅ Vote result:', result);
        
        showNotification('success',
            result.message || (approve 
                ? '✅ Bạn đã đồng ý yêu cầu rút tiền này'
                : '❌ Bạn đã từ chối yêu cầu rút tiền này')
        );
        
        // Reload data
        loadPendingVoteRequests();
        loadFundStats();
        loadMyPendingRequests();
        loadTransactionHistory();
        
    } catch (error) {
        console.error('❌ Error voting on withdraw request:', error);
        showNotification('error', '❌ Lỗi: ' + error.message);
    }
}

async function loadRecentTransactions() {
    try {
        // Lấy transactions của user từ tất cả funds
        const response = await fetch(`${API_BASE_URL}/transactions/user/${CURRENT_USER_ID}`);
        if (!response.ok) {
            console.error('❌ Failed to load user transactions:', response.status);
            throw new Error('Failed to load transactions');
        }
        
        const allTransactions = await response.json();
        
        // Filter: chỉ Completed transactions và lấy 5 giao dịch gần nhất
        const transactions = (allTransactions || [])
            .filter(t => {
                const status = t.status || t.transaction_status;
                return status === 'Completed' || status === 'COMPLETED';
            })
            .slice(0, 5);
        
        const container = document.getElementById('recentTransactions');
        if (!container) {
            console.warn('⚠️ Container #recentTransactions not found');
            return;
        }
        
        if (transactions.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-receipt"></i>
                    <p>Chưa có giao dịch nào</p>
                </div>
            `;
            return;
        }
        
        container.innerHTML = transactions.map(t => {
            const transactionType = t.transactionType || t.transaction_type || t.type;
            const date = t.date || t.createdAt || t.created_at;
            return `
            <div class="transaction-item">
                <div class="transaction-icon ${transactionType === 'Withdraw' ? 'expense' : 'income'}">
                    <i class="fas fa-${transactionType === 'Withdraw' ? 'arrow-down' : 'arrow-up'}"></i>
                </div>
                <div class="transaction-info">
                    <div class="transaction-title">${t.purpose || 'Không có mục đích'}</div>
                    <div class="transaction-date">${formatDate(date)}</div>
                </div>
                <div class="transaction-amount ${transactionType === 'Withdraw' ? 'negative' : 'positive'}">
                    ${transactionType === 'Withdraw' ? '-' : '+'} ${formatCurrency(t.amount)}
                </div>
            </div>
            `;
        }).join('');
        
    } catch (error) {
        console.error('Error loading recent transactions:', error);
    }
}

async function loadTransactionHistory() {
    try {
        const statusEl = document.getElementById('filterStatus');
        const typeEl = document.getElementById('filterType');
        const status = statusEl ? statusEl.value : '';
        const type = typeEl ? typeEl.value : '';
        
        // Lấy transactions của user từ tất cả funds
        const response = await fetch(`${API_BASE_URL}/transactions/user/${CURRENT_USER_ID}`);
        if (!response.ok) {
            console.error('❌ Failed to load user transactions:', response.status);
            throw new Error('Failed to load transactions');
        }
        
        let transactions = await response.json();
        if (!Array.isArray(transactions)) transactions = [];
        
        // Filter theo status và type ở client-side
        if (status) {
            transactions = transactions.filter(t => {
                const tStatus = t.status || t.transaction_status;
                return tStatus === status || tStatus === status.toUpperCase();
            });
        }
        
        if (type) {
            transactions = transactions.filter(t => {
                const tType = t.transactionType || t.transaction_type || t.type;
                return tType === type || tType === type.toUpperCase();
            });
        }
        
        updateTransactionTable(transactions);
        
    } catch (error) {
        console.error('Error loading transaction history:', error);
        updateTransactionTable([]);
    }
}

function updateTransactionTable(transactions) {
    const tbody = document.getElementById('transactionsTableBody');
    
    if (transactions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="empty-table">
                    <div class="empty-state">
                        <i class="fas fa-receipt"></i>
                        <p>Không có giao dịch nào</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = transactions.map(t => {
        const transactionType = t.transactionType || t.transaction_type || t.type;
        const date = t.date || t.createdAt || t.created_at;
        const status = t.status || t.transaction_status;
        const isDeposit = transactionType === 'Deposit' || transactionType === 'DEPOSIT';
        const isWithdraw = transactionType === 'Withdraw' || transactionType === 'WITHDRAW';
        
        return `
        <tr>
            <td>${formatDate(date)}</td>
            <td>
                <span class="badge ${isDeposit ? 'badge-success' : 'badge-warning'}">
                    ${isDeposit ? '📥 Nạp tiền' : '📤 Rút tiền'}
                </span>
            </td>
            <td>${t.purpose || '-'}</td>
            <td class="amount ${isWithdraw ? 'negative' : 'positive'}">
                ${formatCurrency(t.amount)}
            </td>
            <td>
                <span class="badge badge-${getStatusClass(status)}">
                    ${getStatusIcon(status)} ${getStatusText(status)}
                </span>
            </td>
            <td>${t.createdByName || t.userName || `User #${t.userId || t.user_id || t.createdBy || 'Unknown'}`}</td>
        </tr>
        `;
    }).join('');
}

// ========================================
// MODAL HANDLERS
// ========================================

// Deposit Modal
function openDepositModal() {
    document.getElementById('depositModal').classList.add('show');
    document.getElementById('depositForm').reset();
}

function closeDepositModal() {
    document.getElementById('depositModal').classList.remove('show');
}

async function handleDeposit(e) {
    e.preventDefault();
    
    const formData = new FormData(e.target);
    const groupId = parseInt(formData.get('groupId'));
    
    // Lấy fundId từ data attribute của option đã chọn
    const selectedOption = e.target.querySelector(`option[value="${groupId}"]`);
    let fundId = selectedOption ? selectedOption.getAttribute('data-fund-id') : null;
    
    try {
        // Nếu chưa có fund, tạo fund mới trước
        if (!fundId || fundId === '') {
            console.log(`🆕 Creating new fund for group ${groupId}...`);
            const createResponse = await fetch(`/api/fund/group/${groupId}/create`, {
                method: 'POST'
            });
            
            if (createResponse.ok) {
                const newFund = await createResponse.json();
                fundId = newFund.fundId;
                console.log(`✅ Created fund ${fundId} for group ${groupId}`);
            } else {
                throw new Error('Không thể tạo quỹ mới');
            }
        }
        
        const data = {
            fundId: parseInt(fundId),
            userId: CURRENT_USER_ID,
            amount: parseFloat(formData.get('amount')),
            purpose: formData.get('purpose')
        };
        
        const response = await fetch(`${API_BASE_URL}/deposit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        
        const result = await response.json();
        
        if (!response.ok) {
            throw new Error(result.error || result.message || 'Failed to deposit');
        }
        
        if (result.success) {
            showNotification('success', '✅ Nạp tiền thành công!');
            closeDepositModal();
            
            // Reload data
            loadGroups(); // Reload để cập nhật fundId mới
            loadFundStats();
            loadRecentTransactions();
            loadTransactionHistory();
        } else {
            throw new Error(result.message || 'Unknown error');
        }
        
    } catch (error) {
        console.error('Error depositing:', error);
        showNotification('error', '❌ Lỗi: ' + error.message);
    }
}

// Withdraw Vote Modal
function openWithdrawVoteModal() {
    document.getElementById('withdrawVoteModal').classList.add('show');
    document.getElementById('withdrawVoteForm').reset();
    
    // Load current balance
    loadAvailableBalance();
}

function closeWithdrawVoteModal() {
    document.getElementById('withdrawVoteModal').classList.remove('show');
}

async function loadAvailableBalance() {
    try {
        const response = await fetch(`${API_BASE_URL}/stats`);
        if (!response.ok) throw new Error('Failed to load balance');
        
        const stats = await response.json();
        document.getElementById('availableBalance').textContent = formatCurrency(stats.totalBalance);
    } catch (error) {
        console.error('Error loading balance:', error);
    }
}

async function handleWithdrawVote(e) {
    e.preventDefault();
    
    const formData = new FormData(e.target);
    const groupId = parseInt(formData.get('groupId'));
    
    // Lấy fundId từ data attribute của option đã chọn
    const selectedOption = e.target.querySelector(`option[value="${groupId}"]`);
    let fundId = selectedOption ? selectedOption.getAttribute('data-fund-id') : null;
    
    if (!fundId || fundId === '') {
        showNotification('error', '❌ Vui lòng chọn nhóm có quỹ');
        return;
    }
    
    const data = {
        fundId: parseInt(fundId),
        userId: CURRENT_USER_ID,
        amount: parseFloat(formData.get('amount')),
        purpose: formData.get('purpose'),
        receiptUrl: formData.get('receiptUrl') || null
    };
    
    try {
        const response = await fetch(`${API_BASE_URL}/withdraw/request`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || 'Failed to create withdrawal request');
        }
        
        const result = await response.json();
        
        if (result.success) {
            showNotification('success', '✅ Yêu cầu rút tiền đã được tạo! Các thành viên trong nhóm sẽ được thông báo để bỏ phiếu.');
            closeWithdrawVoteModal();
            
            // Reload data ngay lập tức
            loadFundStats();
            loadMyPendingRequests();
            // Load pending vote requests để các user khác thấy ngay
            setTimeout(() => {
                loadPendingVoteRequests();
            }, 500);
            loadTransactionHistory();
        } else {
            throw new Error(result.message || 'Unknown error');
        }
        
    } catch (error) {
        console.error('Error creating withdrawal request:', error);
        showNotification('error', '❌ Lỗi: ' + error.message);
    }
}

// ========================================
// UTILITY FUNCTIONS
// ========================================

function scrollToPendingVoteSection() {
    const section = document.getElementById('pendingVoteSection');
    if (section) {
        section.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } else {
        // Nếu chưa có section, load lại và scroll sau
        loadPendingVoteRequests();
        setTimeout(() => {
            const newSection = document.getElementById('pendingVoteSection');
            if (newSection) {
                newSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        }, 500);
    }
}

function getStatusClass(status) {
    const map = {
        'Pending': 'warning',
        'Approved': 'info',
        'Rejected': 'danger',
        'Completed': 'success'
    };
    return map[status] || 'secondary';
}

function getStatusText(status) {
    const map = {
        'Pending': 'Chờ duyệt',
        'Approved': 'Đã duyệt',
        'Rejected': 'Từ chối',
        'Completed': 'Hoàn tất'
    };
    return map[status] || status;
}

function getStatusIcon(status) {
    const map = {
        'Pending': '⏳',
        'Approved': '✅',
        'Rejected': '❌',
        'Completed': '✔️'
    };
    return map[status] || '';
}

function formatCurrency(amount) {
    if (!amount) return '0 VNĐ';
    return new Intl.NumberFormat('vi-VN').format(amount) + ' VNĐ';
}

function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', { 
        year: 'numeric', 
        month: '2-digit', 
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function showNotification(type, message) {
    // Tạo notification element nếu chưa có
    let notificationContainer = document.getElementById('notificationContainer');
    if (!notificationContainer) {
        notificationContainer = document.createElement('div');
        notificationContainer.id = 'notificationContainer';
        notificationContainer.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 10000;
            max-width: 400px;
        `;
        document.body.appendChild(notificationContainer);
    }
    
    // Tạo notification
    const notification = document.createElement('div');
    notification.style.cssText = `
        background: ${type === 'success' ? '#4caf50' : '#f44336'};
        color: white;
        padding: 16px 20px;
        margin-bottom: 10px;
        border-radius: 4px;
        box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        display: flex;
        align-items: center;
        justify-content: space-between;
        animation: slideIn 0.3s ease-out;
        min-width: 300px;
    `;
    
    // Thêm animation
    if (!document.getElementById('notificationStyles')) {
        const style = document.createElement('style');
        style.id = 'notificationStyles';
        style.textContent = `
            @keyframes slideIn {
                from {
                    transform: translateX(400px);
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
                    transform: translateX(400px);
                    opacity: 0;
                }
            }
        `;
        document.head.appendChild(style);
    }
    
    notification.innerHTML = `
        <span>${message}</span>
        <button onclick="this.parentElement.remove()" style="
            background: transparent;
            border: none;
            color: white;
            font-size: 20px;
            cursor: pointer;
            margin-left: 10px;
            padding: 0 5px;
        ">&times;</button>
    `;
    
    notificationContainer.appendChild(notification);
    
    // Tự động đóng sau 5 giây
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease-out';
        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove();
            }
        }, 300);
    }, 5000);
}

function viewAllTransactions() {
    // Scroll to transaction table
    document.getElementById('transactionsTableBody').scrollIntoView({ behavior: 'smooth' });
}

function exportFundReport() {
    alert('📥 Chức năng xuất báo cáo đang được phát triển...');
}

function viewTransactionDetail(transactionId) {
    // TODO: Show modal with transaction details
    alert(`Xem chi tiết giao dịch #${transactionId}`);
}

async function cancelRequest(transactionId) {
    if (!confirm('Bạn có chắc muốn hủy yêu cầu này?')) return;
    
    try {
        const url = `${API_BASE_URL}/transactions/${transactionId}?userId=${CURRENT_USER_ID}`;
        console.log(`🗑️ Cancelling transaction ${transactionId}...`);
        
        const response = await fetch(url, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ error: await response.text() }));
            throw new Error(errorData.error || 'Failed to cancel request');
        }
        
        const result = await response.json();
        console.log('✅ Cancel result:', result);
        
        showNotification('success', result.message || '✅ Đã hủy yêu cầu');
        
        // Reload data ngay lập tức để cập nhật UI
        loadFundStats();
        loadMyPendingRequests();
        loadPendingVoteRequests();
        loadTransactionHistory();
        loadRecentTransactions();
        
    } catch (error) {
        console.error('❌ Error canceling request:', error);
        showNotification('error', '❌ Lỗi: ' + error.message);
    }
}

// Close modal when clicking outside
window.onclick = function(event) {
    if (event.target.id === 'depositModal') {
        closeDepositModal();
    }
    if (event.target.id === 'withdrawVoteModal') {
        closeWithdrawVoteModal();
    }
}

