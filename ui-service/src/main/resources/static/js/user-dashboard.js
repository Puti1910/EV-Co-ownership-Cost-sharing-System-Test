// User Dashboard JavaScript

// API Endpoints
const API = {
    GROUPS: '/api/groups',
    COSTS: '/api/costs',
    USAGE: '/api/usage-tracking',
    PAYMENTS: '/api/payments',
    COST_SHARES: '/api/cost-shares',
    FUND: '/api/fund'
};

// Current User - retrieved from server-injected dataset or local storage fallback
const dashboardContainer = document.querySelector('.user-container');
let CURRENT_USER_ID = dashboardContainer?.dataset.userId
    ? parseInt(dashboardContainer.dataset.userId, 10)
    : null;

if (!CURRENT_USER_ID) {
    const storedUserId = localStorage.getItem('userId');
    CURRENT_USER_ID = storedUserId ? parseInt(storedUserId, 10) : null;
}

if (!CURRENT_USER_ID || Number.isNaN(CURRENT_USER_ID)) {
    console.warn('Không tìm thấy userId trong dataset hoặc localStorage, sử dụng 0 làm mặc định.');
    CURRENT_USER_ID = 0;
}

// Helper function to get cookie value
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

// Helper function to get authenticated fetch (use authenticatedFetch if available, otherwise add headers manually)
async function authenticatedRequest(url, options = {}) {
    // Use authenticatedFetch if available (from token-manager.js)
    if (typeof window.authenticatedFetch === 'function') {
        return await window.authenticatedFetch(url, options);
    }
    
    // Fallback: manually add Authorization header
    const token = localStorage.getItem('jwtToken') || getCookie('jwtToken');
    const headers = new Headers(options.headers || {});
    if (token && !headers.has('Authorization')) {
        headers.set('Authorization', `Bearer ${token}`);
    }
    
    const response = await fetch(url, {
        ...options,
        headers: headers,
        credentials: 'include'
    });
    
    // Note: Browser will still log 404 errors in console, but this is expected behavior
    // for cases like groups without funds. The code handles 404s gracefully.
    return response;
}

async function safeParseJsonBody(response) {
    try {
        const text = await response.text();
        if (!text || text.trim().length === 0) {
            return {};
        }
        return JSON.parse(text);
    } catch (error) {
        console.warn('safeParseJsonBody: unable to parse response body as JSON:', error);
        return {};
    }
}

// Helper function to show profile not approved message
function showProfileNotApprovedMessage(container, errorMessage = 'Hồ sơ chưa được duyệt. Vui lòng hoàn tất KYC.') {
    if (!container) return;
    
    container.innerHTML = `
        <div style="text-align: center; padding: 40px;">
            <div style="background: #FEF3C7; border: 2px solid #F59E0B; border-radius: 12px; padding: 24px; max-width: 500px; margin: 0 auto;">
                <i class="fas fa-exclamation-triangle" style="font-size: 48px; color: #F59E0B; margin-bottom: 16px;"></i>
                <h3 style="color: #92400E; margin-bottom: 12px;">Hồ sơ chưa được duyệt</h3>
                <p style="color: #78350F; margin-bottom: 20px;">${errorMessage}</p>
                <button onclick="window.location.href='/auth/profile-status'" 
                        style="background: #F59E0B; color: white; border: none; padding: 12px 24px; border-radius: 8px; cursor: pointer; font-weight: 600;">
                    <i class="fas fa-user-check"></i> Kiểm tra trạng thái hồ sơ
                </button>
            </div>
        </div>
    `;
}

// Helper function to check if response is 403 and handle profile not approved
async function handle403Error(response, container) {
    if (response.status === 403) {
        let errorMessage = 'Hồ sơ chưa được duyệt. Vui lòng hoàn tất KYC.';
        try {
            const errorData = await response.json().catch(() => ({}));
            if (errorData.message) {
                errorMessage = errorData.message;
            }
        } catch (e) {
            // Use default message
        }
        showProfileNotApprovedMessage(container, errorMessage);
        return true; // Indicates 403 was handled
    }
    return false; // Not a 403 error
}

// Global State
let currentPage = 'home';
let fundAutoRefreshInterval = null;
let lastPendingVoteCount = 0;
let lastPendingLeaveRequestCount = {}; // Track pending leave requests per group
let leaveRequestAutoRefreshInterval = null; // Auto-refresh interval for leave requests

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', function() {
    initNavigation();
    initUsageForm();
    initPaymentMethods();
    initFundModals();
    initCostFilters();
    
    // Detect current page from URL and load appropriate page
    const path = window.location.pathname;
    let page = 'home';
    if (path.includes('/user/costs')) {
        page = 'costs';
    } else if (path.includes('/user/contracts')) {
        page = 'contracts';
    } else if (path.includes('/user/usage')) {
        page = 'usage';
    } else if (path.includes('/user/payments')) {
        page = 'payments';
    } else if (path.includes('/user/fund')) {
        page = 'fund';
    } else if (path.includes('/user/fair-schedule')) {
        page = 'fair-schedule';
    } else if (path.includes('/user/disputes')) {
        page = 'disputes';
    }
    
    // Load the appropriate page
    switch(page) {
        case 'home':
    loadHomePage();
            break;
        case 'costs':
            loadCostsPage();
            break;
        case 'usage':
            loadUsagePage();
            break;
        case 'payments':
            loadPaymentsPage();
            break;
        case 'fund':
            loadFundPage();
            break;
        case 'disputes':
            // data loaded server-side; nothing additional for now
            break;
        case 'fair-schedule':
            // Trang này sử dụng script riêng (user-fair-schedule.js)
            break;
        case 'contracts':
            if (typeof loadContractsPage === 'function') {
                loadContractsPage();
            }
            break;
    }
});

// ============ NAVIGATION ============
function initNavigation() {
    // Navigation links now use real URLs, so we don't need to preventDefault
    // The page will reload with the correct URL
    // We only need to handle programmatic navigation for internal links
    
    // Handle view-all links
    document.querySelectorAll('.view-all').forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const page = this.getAttribute('data-page');
            if (page) switchPage(page);
        });
    });
    
    // Handle stat-link clicks
    document.addEventListener('click', function(e) {
        if (e.target.closest('.stat-link')) {
            e.preventDefault();
            const link = e.target.closest('.stat-link');
            const page = link.getAttribute('data-page');
            if (page) switchPage(page);
        }
        
        // Handle quick-action-btn clicks
        if (e.target.closest('.quick-action-btn')) {
            e.preventDefault();
            const btn = e.target.closest('.quick-action-btn');
            const page = btn.getAttribute('data-page');
            if (page) switchPage(page);
        }
    });
}

function switchPage(page) {
    // Dừng auto-refresh nếu không ở trang Fund
    if (page !== 'fund') {
        stopFundAutoRefresh();
    }
    
    // Update nav
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
    });
    document.querySelector(`[data-page="${page}"]`)?.classList.add('active');
    
    // Update page content
    document.querySelectorAll('.page').forEach(p => {
        p.classList.remove('active');
    });
    document.getElementById(`${page}-page`)?.classList.add('active');
    
    currentPage = page;
    
    // Load page data
    switch(page) {
        case 'home':
            loadHomePage();
            break;
        case 'costs':
            loadCostsPage();
            break;
        case 'usage':
            loadUsagePage();
            break;
        case 'payments':
            loadPaymentsPage();
            break;
        case 'fund':
            loadFundPage();
            break;
        case 'disputes':
            // no extra client-side loading yet
            break;
    }
}

// ============ HOME PAGE (Nhóm của tôi) ============
async function loadHomePage() {
    try {
        // Chỉ load danh sách nhóm
        await loadMyGroups();
        
    } catch (error) {
        console.error('Error loading home page:', error);
    }
}

async function loadQuickStats() {
    try {
        // Get current month and year for filtering
        const now = new Date();
        const currentMonth = now.getMonth() + 1;
        const currentYear = now.getFullYear();
        const monthStart = new Date(currentYear, currentMonth - 1, 1);
        const monthEnd = new Date(currentYear, currentMonth, 0, 23, 59, 59);
        
        // Fetch pending cost shares
        let totalPending = 0;
        try {
            const token = localStorage.getItem('jwtToken') || getCookie('jwtToken');
            const headers = token ? { 'Authorization': `Bearer ${token}` } : {};
            const pendingResponse = await authenticatedRequest(`${API.COST_SHARES}/user/${CURRENT_USER_ID}/pending`, { 
                credentials: 'include'
            });
            if (pendingResponse.ok) {
                const pendingShares = await pendingResponse.json();
                if (Array.isArray(pendingShares)) {
                    totalPending = pendingShares.reduce((sum, share) => {
                        return sum + (share.amountShare || 0);
                    }, 0);
                }
            }
        } catch (e) {
            console.warn('Error fetching pending shares:', e);
        }
        
        // Fetch paid payments for current month
        let totalPaid = 0;
        try {
            const paymentsResponse = await authenticatedRequest(`${API.PAYMENTS}/user/${CURRENT_USER_ID}/history`);
            if (paymentsResponse.ok) {
                const payments = await paymentsResponse.json();
                if (Array.isArray(payments)) {
                    // Filter by current month based on paymentDate
                    const currentMonthPayments = payments.filter(payment => {
                        if (!payment.paymentDate) return false;
                        try {
                            const paymentDate = new Date(payment.paymentDate);
                            // Check if date is valid
                            if (isNaN(paymentDate.getTime())) return false;
                            // Normalize dates to start of day for comparison
                            const paymentDateOnly = new Date(paymentDate.getFullYear(), paymentDate.getMonth(), paymentDate.getDate());
                            const monthStartOnly = new Date(monthStart.getFullYear(), monthStart.getMonth(), monthStart.getDate());
                            const monthEndOnly = new Date(monthEnd.getFullYear(), monthEnd.getMonth(), monthEnd.getDate());
                            return paymentDateOnly >= monthStartOnly && paymentDateOnly <= monthEndOnly;
                        } catch (e) {
                            console.warn('Error parsing payment date:', payment.paymentDate, e);
                            return false;
                        }
                    });
                    totalPaid = currentMonthPayments.reduce((sum, payment) => {
                        return sum + (parseFloat(payment.amount) || 0);
                    }, 0);
                }
            }
        } catch (e) {
            console.warn('Error fetching paid payments:', e);
        }
        
        // Fetch usage tracking for current month
        let totalKm = 0;
        try {
            // Get all usage history for user
            const usageResponse = await authenticatedRequest(`${API.USAGE}/user/${CURRENT_USER_ID}/history`);
            if (usageResponse.ok) {
                const usageData = await usageResponse.json();
                if (Array.isArray(usageData)) {
                    // Filter by current month and year
                    const currentMonthUsage = usageData.filter(usage => {
                        // Handle both number and string month/year
                        const usageMonth = typeof usage.month === 'string' ? parseInt(usage.month) : usage.month;
                        const usageYear = typeof usage.year === 'string' ? parseInt(usage.year) : usage.year;
                        return usageMonth === currentMonth && usageYear === currentYear;
                    });
                    totalKm = currentMonthUsage.reduce((sum, usage) => {
                        const km = parseFloat(usage.kmDriven) || 0;
                        return sum + km;
                    }, 0);
                }
            }
        } catch (e) {
            console.warn('Error fetching usage data:', e);
        }
        
        // Fetch ownership percentage from groups
        let ownershipPercent = 0;
        try {
            const groupsResponse = await authenticatedRequest(`${API.GROUPS}/user/${CURRENT_USER_ID}`);
            if (groupsResponse.ok) {
                const groups = await groupsResponse.json();
                if (Array.isArray(groups) && groups.length > 0) {
                    // Get average ownership across all groups
                    let totalOwnership = 0;
                    let groupCount = 0;
                    
                    for (const group of groups) {
                        try {
                            const membersResponse = await authenticatedRequest(`${API.GROUPS}/${group.groupId}/members`);
                            if (membersResponse.ok) {
                                const members = await membersResponse.json();
                                const userMember = members.find(m => m.userId === CURRENT_USER_ID);
                                if (userMember && userMember.ownershipPercent) {
                                    totalOwnership += userMember.ownershipPercent;
                                    groupCount++;
                                }
                            }
                        } catch (e) {
                            console.warn(`Error fetching members for group ${group.groupId}:`, e);
                        }
                    }
                    
                    if (groupCount > 0) {
                        ownershipPercent = totalOwnership / groupCount;
                    }
                }
            }
        } catch (e) {
            console.warn('Error fetching ownership data:', e);
        }
        
        // Update UI (only if elements exist)
        const myPendingEl = document.getElementById('my-pending');
        const myPaidEl = document.getElementById('my-paid');
        const myKmEl = document.getElementById('my-km');
        const myOwnershipEl = document.getElementById('my-ownership');
        
        if (myPendingEl) myPendingEl.textContent = formatCurrency(totalPending);
        if (myPaidEl) myPaidEl.textContent = formatCurrency(totalPaid);
        if (myKmEl) myKmEl.textContent = `${Math.round(totalKm)} km`;
        if (myOwnershipEl) myOwnershipEl.textContent = `${ownershipPercent.toFixed(1)}%`;
        
    } catch (error) {
        console.error('Error loading stats:', error);
        // Set default values on error (only if elements exist)
        const myPendingEl = document.getElementById('my-pending');
        const myPaidEl = document.getElementById('my-paid');
        const myKmEl = document.getElementById('my-km');
        const myOwnershipEl = document.getElementById('my-ownership');
        
        if (myPendingEl) myPendingEl.textContent = formatCurrency(0);
        if (myPaidEl) myPaidEl.textContent = formatCurrency(0);
        if (myKmEl) myKmEl.textContent = '0 km';
        if (myOwnershipEl) myOwnershipEl.textContent = '0%';
    }
}

// Store user role for each group
let userGroupRoles = {}; // { groupId: 'Admin' | 'Member' }

async function loadMyGroups() {
    try {
        const response = await authenticatedRequest(`${API.GROUPS}/user/${CURRENT_USER_ID}`);
        if (!response.ok) throw new Error('Failed to load groups');
        
        const groups = await response.json();
        
        const container = document.getElementById('my-groups-list');
        if (!container) return;
        
        if (groups.length === 0) {
            container.innerHTML = '<p style="text-align: center; color: var(--text-light);">Bạn chưa tham gia nhóm nào</p>';
            return;
        }
        
        // Fetch user role for each group
        for (const group of groups) {
            try {
                const membersResponse = await authenticatedRequest(`${API.GROUPS}/${group.groupId}/members`);
                if (membersResponse.ok) {
                    const members = await membersResponse.json();
                    const userMember = members.find(m => m.userId === CURRENT_USER_ID);
                    if (userMember) {
                        userGroupRoles[group.groupId] = userMember.role || 'Member';
                    } else {
                        userGroupRoles[group.groupId] = 'Member';
                    }
                }
            } catch (e) {
                console.warn(`Failed to fetch role for group ${group.groupId}:`, e);
                userGroupRoles[group.groupId] = 'Member';
            }
        }
        
        container.innerHTML = groups.map(group => {
            const isAdmin = userGroupRoles[group.groupId] === 'Admin';
            return `
            <div class="group-item" data-group-id="${group.groupId}">
                <div class="group-item-header">
                    <h3>${escapeHtml(group.groupName)}</h3>
                    ${isAdmin ? '<span class="badge badge-admin"><i class="fas fa-crown"></i> Admin</span>' : ''}
                </div>
                <p>Quản lý bởi: User #${group.adminId}</p>
                <div class="group-stats">
                    <div class="group-stat">
                        <i class="fas fa-users"></i>
                        <span>${group.memberCount || 0} thành viên</span>
                    </div>
                    <div class="group-stat">
                        <i class="fas fa-car"></i>
                        <span>Xe #${group.vehicleId || 'N/A'}</span>
                    </div>
                </div>
                <div class="group-actions">
                    <button class="btn btn-info btn-sm view-group-btn" data-group-id="${group.groupId}" data-group-name="${escapeHtml(group.groupName)}">
                        <i class="fas fa-info-circle"></i> Xem chi tiết
                    </button>
                    ${isAdmin ? `
                    <button class="btn btn-primary btn-sm manage-group-btn" data-group-id="${group.groupId}" data-group-name="${escapeHtml(group.groupName)}">
                        <i class="fas fa-cog"></i> Quản lý nhóm
                    </button>
                ` : ''}
                </div>
            </div>
        `}).join('');
        
        // Bind click handlers for view group buttons
        document.querySelectorAll('.view-group-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                const groupId = parseInt(this.getAttribute('data-group-id'));
                const groupName = this.getAttribute('data-group-name');
                openViewGroupModal(groupId, groupName);
            });
        });
        
        // Bind click handlers for manage group buttons
        document.querySelectorAll('.manage-group-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                const groupId = parseInt(this.getAttribute('data-group-id'));
                const groupName = this.getAttribute('data-group-name');
                openManageGroupModal(groupId, groupName);
            });
        });
        
    } catch (error) {
        console.error('Error loading groups:', error);
        const container = document.getElementById('my-groups-list');
        if (container) {
            container.innerHTML = '<p style="text-align: center; color: var(--text-light);">Không có dữ liệu</p>';
        }
    }
}

async function loadRecentCosts() {
    try {
        const response = await authenticatedRequest(API.COSTS);
        const costs = await response.json();
        
        const recent = costs.slice(0, 5);
        const timeline = document.getElementById('recent-costs-timeline');
        
        timeline.innerHTML = recent.map(cost => `
            <div class="timeline-item">
                <div class="timeline-content">
                    <div class="timeline-header">
                        <div class="timeline-title">${getCostTypeName(cost.costType)}</div>
                        <div class="timeline-amount">${formatCurrency(cost.amount)}</div>
                    </div>
                    <div class="timeline-meta">
                        <i class="fas fa-calendar"></i> ${formatDate(cost.createdAt)}
                    </div>
                </div>
            </div>
        `).join('');
        
    } catch (error) {
        console.error('Error loading recent costs:', error);
    }
}

// ============ COSTS PAGE ============
async function loadCostsPage() {
    const grid = document.getElementById('user-costs-grid');
    
    try {
        console.log('Loading costs page for user:', CURRENT_USER_ID);
        
        // Load all cost shares for current user (both paid and pending)
        const pendingUrl = `${API.COST_SHARES}/user/${CURRENT_USER_ID}/pending`;
        const historyUrl = `${API.COST_SHARES}/user/${CURRENT_USER_ID}/history`;
        
        console.log('Fetching pending from:', pendingUrl);
        console.log('Fetching history from:', historyUrl);
        
        // Use authenticatedRequest which automatically handles JWT token
        console.log('Loading costs for user:', CURRENT_USER_ID);
        
        const [pendingResponse, historyResponse] = await Promise.all([
            authenticatedRequest(pendingUrl, { 
                credentials: 'include'
            }),
            authenticatedRequest(historyUrl, { 
                credentials: 'include'
            })
        ]);
        
        console.log('Pending response status:', pendingResponse.status);
        console.log('History response status:', historyResponse.status);
        
        // Check for 403 Forbidden (profile not approved)
        if (pendingResponse.status === 403 || historyResponse.status === 403) {
            let errorMessage = 'Hồ sơ chưa được duyệt. Vui lòng hoàn tất KYC.';
            try {
                const errorResponse = pendingResponse.status === 403 ? pendingResponse : historyResponse;
                const errorData = await errorResponse.json().catch(() => ({}));
                if (errorData.message) {
                    errorMessage = errorData.message;
                }
            } catch (e) {
                // Use default message
            }
            
            grid.innerHTML = `
                <div style="grid-column: 1/-1; text-align: center; padding: 40px;">
                    <div style="background: #FEF3C7; border: 2px solid #F59E0B; border-radius: 12px; padding: 24px; max-width: 500px; margin: 0 auto;">
                        <i class="fas fa-exclamation-triangle" style="font-size: 48px; color: #F59E0B; margin-bottom: 16px;"></i>
                        <h3 style="color: #92400E; margin-bottom: 12px;">Hồ sơ chưa được duyệt</h3>
                        <p style="color: #78350F; margin-bottom: 20px;">${errorMessage}</p>
                        <button onclick="window.location.href='/auth/profile-status'" 
                                style="background: #F59E0B; color: white; border: none; padding: 12px 24px; border-radius: 8px; cursor: pointer; font-weight: 600;">
                            <i class="fas fa-user-check"></i> Kiểm tra trạng thái hồ sơ
                        </button>
                    </div>
                </div>
            `;
            return;
        }
        
        if (!pendingResponse.ok && pendingResponse.status !== 403) {
            const errorText = await pendingResponse.text();
            console.error('Pending response error:', errorText);
        }
        
        if (!historyResponse.ok && historyResponse.status !== 403) {
            const errorText = await historyResponse.text();
            console.error('History response error:', errorText);
        }
        
        const pendingShares = pendingResponse.ok ? await pendingResponse.json() : [];
        const paidShares = historyResponse.ok ? await historyResponse.json() : [];
        
        console.log('Pending shares count:', pendingShares.length);
        console.log('Paid shares count:', paidShares.length);
        console.log('Pending shares:', pendingShares);
        console.log('Paid shares:', paidShares);
        
        // Validate that we got arrays
        if (!Array.isArray(pendingShares)) {
            console.warn('Pending shares is not an array:', pendingShares);
        }
        if (!Array.isArray(paidShares)) {
            console.warn('Paid shares is not an array:', paidShares);
        }
        
        // Combine and mark paid status
        const allShares = [
            ...(Array.isArray(pendingShares) ? pendingShares.map(s => ({...s, isPaid: false})) : []),
            ...(Array.isArray(paidShares) ? paidShares.map(s => ({...s, isPaid: true})) : [])
        ].sort((a, b) => {
            const dateA = a.calculatedAt ? new Date(a.calculatedAt) : new Date(0);
            const dateB = b.calculatedAt ? new Date(b.calculatedAt) : new Date(0);
            return dateB - dateA;
        });
        
        console.log('Total shares to display:', allShares.length);
        
        if (allShares.length === 0) {
            grid.innerHTML = `
                <div style="grid-column: 1/-1; text-align: center; padding: 40px; color: var(--text-light);">
                    <i class="fas fa-inbox" style="font-size: 48px; margin-bottom: 16px; opacity: 0.5;"></i>
                    <p>Bạn chưa có chi phí nào</p>
                    <small style="display: block; margin-top: 10px; opacity: 0.7;">User ID: ${CURRENT_USER_ID}</small>
                </div>
            `;
            return;
        }
        
        grid.innerHTML = allShares.map(share => {
            const description = share.description || `Chi phí #${share.costId || 'N/A'}`;
            const safeDescription = description.replace(/'/g, "\\'").replace(/"/g, '&quot;');
            const amount = share.amountShare || share.shareAmount || 0;
            const shareId = share.shareId || share.share_id || 'N/A';
            const totalAmount = share.totalAmount || amount;
            const percent = share.percent || 0;
            const costType = share.costType || 'Other';
            const costTypeDisplay = share.costTypeDisplay || 'Khác';
            const splitMethod = share.splitMethod || 'BY_OWNERSHIP';
            const splitMethodDisplay = share.splitMethodDisplay || 'Chia theo sở hữu';
            
            // Handle calculatedAt - could be string or already a date
            let calculatedDate = new Date().toISOString();
            if (share.calculatedAt) {
                try {
                    calculatedDate = share.calculatedAt;
                } catch (e) {
                    console.warn('Error parsing calculatedAt:', e);
                }
            }
            
            // Get icon and color for cost type
            const costTypeInfo = getCostTypeInfo(costType);
            
            return `
                <div class="cost-card" data-cost-type="${costType}" data-status="${share.isPaid ? 'paid' : 'pending'}">
                    <div class="cost-header">
                        <div class="cost-type-badge ${costTypeInfo.class}">
                            <i class="${costTypeInfo.icon}"></i>
                            <span>${costTypeDisplay}</span>
                        </div>
                        <div class="cost-status ${share.isPaid ? 'paid' : 'pending'}">
                            ${share.isPaid ? 'Đã thanh toán' : 'Chưa thanh toán'}
                        </div>
                    </div>
                    <div class="cost-amount">${formatCurrency(amount)}</div>
                    <div class="cost-details">
                        <div class="cost-description">${description}</div>
                        <div class="cost-meta">
                            <div class="meta-item">
                                <i class="fas fa-tag"></i>
                                <span>Tổng chi phí: ${formatCurrency(totalAmount)}</span>
                            </div>
                            <div class="meta-item">
                                <i class="fas fa-percentage"></i>
                                <span>Tỷ lệ của bạn: ${percent.toFixed(2)}%</span>
                            </div>
                            <div class="meta-item">
                                <i class="fas fa-balance-scale"></i>
                                <span>${splitMethodDisplay}</span>
                            </div>
                            ${share.kmDriven ? `
                                <div class="meta-item">
                                    <i class="fas fa-tachometer-alt"></i>
                                    <span>Km của bạn: ${share.kmDriven.toFixed(1)} km</span>
                                </div>
                            ` : ''}
                            ${share.ownershipPercent ? `
                                <div class="meta-item">
                                    <i class="fas fa-user-tag"></i>
                                    <span>Tỷ lệ sở hữu: ${share.ownershipPercent.toFixed(2)}%</span>
                                </div>
                            ` : ''}
                        </div>
                    </div>
                    <div class="cost-footer">
                        <div class="cost-date">
                            <i class="fas fa-calendar"></i> ${formatDate(calculatedDate)}
                        </div>
                        ${!share.isPaid ? `
                            <button class="btn btn-success" style="padding: 0.5rem 1rem;" 
                                    onclick="payCostShare(${shareId}, ${amount}, '${safeDescription}')">
                                <i class="fas fa-credit-card"></i> Thanh toán
                            </button>
                        ` : `
                            <span class="paid-badge">
                                <i class="fas fa-check-circle"></i> Đã thanh toán
                            </span>
                        `}
                    </div>
                </div>
            `;
        }).join('');
        
        // Apply filters
        applyCostFilters();
        
        console.log('Successfully rendered', allShares.length, 'cost shares');
        
    } catch (error) {
        console.error('Error loading costs:', error);
        console.error('Error stack:', error.stack);
        grid.innerHTML = `
            <div style="grid-column: 1/-1; text-align: center; padding: 40px; color: var(--danger);">
                <i class="fas fa-exclamation-triangle" style="font-size: 48px; margin-bottom: 16px;"></i>
                <p>Không thể tải dữ liệu chi phí</p>
                <small style="display: block; margin-top: 10px; opacity: 0.7;">${error.message}</small>
            </div>
        `;
    }
}

function payCostShare(shareId, amount, description) {
    // Store payment info for later use
    window.pendingCostPayment = {
        shareId: shareId,
        amount: amount,
        description: description
    };
    
    // Switch to payments page
    switchPage('payments');
    showToast('Đã chuyển đến trang thanh toán. Vui lòng chọn phương thức!', 'info');
    
    // After page loads, scroll to payment methods
    setTimeout(() => {
        const methodSection = document.querySelector('.payment-methods-grid');
        if (methodSection) {
            methodSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    }, 300);
}

// ============ COST FILTERS ============
function initCostFilters() {
    const statusFilter = document.getElementById('user-cost-filter');
    const typeFilter = document.getElementById('user-cost-type-filter');
    const monthFilter = document.getElementById('user-month-filter');
    
    if (statusFilter) {
        statusFilter.addEventListener('change', applyCostFilters);
    }
    if (typeFilter) {
        typeFilter.addEventListener('change', applyCostFilters);
    }
    if (monthFilter) {
        monthFilter.addEventListener('change', applyCostFilters);
    }
}

function applyCostFilters() {
    const statusFilter = document.getElementById('user-cost-filter')?.value || 'all';
    const typeFilter = document.getElementById('user-cost-type-filter')?.value || 'all';
    const monthFilter = document.getElementById('user-month-filter')?.value || '';
    
    const cards = document.querySelectorAll('.cost-card');
    let visibleCount = 0;
    
    cards.forEach(card => {
        const costType = card.getAttribute('data-cost-type') || '';
        const status = card.getAttribute('data-status') || '';
        const dateStr = card.querySelector('.cost-date')?.textContent || '';
        
        // Check status filter
        let showByStatus = statusFilter === 'all' || status === statusFilter;
        
        // Check type filter
        let showByType = typeFilter === 'all' || costType === typeFilter;
        
        // Check month filter
        let showByMonth = true;
        if (monthFilter) {
            const month = parseInt(monthFilter);
            const monthNames = ['Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6',
                               'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'];
            const monthName = monthNames[month - 1];
            showByMonth = dateStr.includes(monthName) || dateStr.includes(`/${month}/`);
        }
        
        const shouldShow = showByStatus && showByType && showByMonth;
        card.style.display = shouldShow ? 'block' : 'none';
        if (shouldShow) visibleCount++;
    });
    
    // Show message if no results
    const grid = document.getElementById('user-costs-grid');
    if (visibleCount === 0 && cards.length > 0) {
        const noResults = document.createElement('div');
        noResults.className = 'no-results';
        noResults.style.cssText = 'grid-column: 1/-1; text-align: center; padding: 40px; color: var(--text-light);';
        noResults.innerHTML = `
            <i class="fas fa-filter" style="font-size: 48px; margin-bottom: 16px; opacity: 0.5;"></i>
            <p>Không có chi phí nào phù hợp với bộ lọc</p>
        `;
        
        // Remove existing no-results if any
        const existing = grid.querySelector('.no-results');
        if (existing) existing.remove();
        
        grid.appendChild(noResults);
    } else {
        const existing = grid.querySelector('.no-results');
        if (existing) existing.remove();
    }
}

/**
 * Get icon and CSS class for cost type
 */
function getCostTypeInfo(costType) {
    const typeMap = {
        'ElectricCharge': {
            icon: 'fas fa-bolt',
            class: 'cost-type-electric'
        },
        'Maintenance': {
            icon: 'fas fa-wrench',
            class: 'cost-type-maintenance'
        },
        'Insurance': {
            icon: 'fas fa-shield-alt',
            class: 'cost-type-insurance'
        },
        'Inspection': {
            icon: 'fas fa-clipboard-check',
            class: 'cost-type-inspection'
        },
        'Cleaning': {
            icon: 'fas fa-spray-can',
            class: 'cost-type-cleaning'
        },
        'Other': {
            icon: 'fas fa-receipt',
            class: 'cost-type-other'
        }
    };
    
    return typeMap[costType] || typeMap['Other'];
}

// ============ USAGE PAGE ============
async function loadUsagePage() {
    try {
        // Load groups for selection (user's groups)
        const groups = await loadGroupsForUsage();
        
        // Auto-load data for first group if available
        if (groups && groups.length > 0) {
            const groupSelect = document.getElementById('usage-group');
            const monthSelect = document.getElementById('usage-month');
            const yearInput = document.getElementById('usage-year');
            
            // Set current month/year if not already set
            if (monthSelect && !monthSelect.value) {
                const now = new Date();
                monthSelect.value = now.getMonth() + 1;
            }
            if (yearInput && !yearInput.value) {
                const now = new Date();
                yearInput.value = now.getFullYear();
            }
            
            // Auto-select first group and load data
            if (groupSelect && !groupSelect.value && groups.length > 0) {
                groupSelect.value = groups[0].groupId;
                // Load usage data for selected group
                await loadGroupUsageInfo();
            } else if (groupSelect && groupSelect.value) {
                // If group is already selected, load data
                await loadGroupUsageInfo();
            }
        }
        
        // Load usage history
        await loadUsageHistory();
        
    } catch (error) {
        console.error('Error loading usage page:', error);
    }
}

let currentGroupUsageData = null;
let currentGroupMembers = null;

function initUsageForm() {
    const form = document.getElementById('usage-form');
    if (form) {
        form.addEventListener('submit', async function(e) {
            e.preventDefault();
            await saveUsage();
        });
    }
    
    // Set current month/year
    const now = new Date();
    const monthSelect = document.getElementById('usage-month');
    const yearInput = document.getElementById('usage-year');
    if (monthSelect) monthSelect.value = now.getMonth() + 1;
    if (yearInput) yearInput.value = now.getFullYear();
    
    // Note: Groups will be loaded by loadUsagePage() which also auto-loads data
}

async function loadGroupsForUsage() {
    try {
        const response = await authenticatedRequest(`${API.GROUPS}/user/${CURRENT_USER_ID}`);
        if (!response.ok) throw new Error('Failed to load groups');
        
        const groups = await response.json();
        const groupSelect = document.getElementById('usage-group');
        
        if (groupSelect && groups && groups.length > 0) {
            groupSelect.innerHTML = '<option value="">-- Chọn nhóm --</option>' +
                groups.map(g => `<option value="${g.groupId}">${g.groupName}</option>`).join('');
        }
        
        return groups || [];
    } catch (error) {
        console.error('Error loading groups:', error);
        return [];
    }
}

async function loadGroupUsageInfo() {
    const groupId = document.getElementById('usage-group').value;
    const month = document.getElementById('usage-month').value;
    const year = document.getElementById('usage-year').value;
    
    if (!groupId || !month || !year) {
        const statsCard = document.getElementById('group-usage-stats-card');
        if (statsCard) statsCard.style.display = 'none';
        document.getElementById('cost-preview').style.display = 'none';
        return;
    }
    
    try {
        // Load group members to get ownership percentage
        const membersResponse = await authenticatedRequest(`${API.GROUPS}/${groupId}/members`);
        if (!membersResponse.ok) throw new Error('Failed to load members');
        currentGroupMembers = await membersResponse.json();
        
        // Find current user's ownership
        const myMember = currentGroupMembers.find(m => m.userId === CURRENT_USER_ID);
        const myOwnership = myMember ? (myMember.ownershipPercent || 0) : 0;
        
        // Load group usage data
        const usageResponse = await authenticatedRequest(`${API.USAGE}/group/${groupId}?month=${month}&year=${year}`);
        let groupUsageData = [];
        if (usageResponse.ok) {
            groupUsageData = await usageResponse.json();
        }
        currentGroupUsageData = groupUsageData;
        
        // Calculate totals
        const totalKm = groupUsageData.reduce((sum, u) => sum + (u.kmDriven || 0), 0);
        // Find current user's usage - ensure type comparison is correct
        const myUsage = groupUsageData.find(u => parseInt(u.userId) === parseInt(CURRENT_USER_ID));
        const myKm = myUsage ? (myUsage.kmDriven || 0) : 0;
        const myUsagePercent = totalKm > 0 ? ((myKm / totalKm) * 100).toFixed(2) : 0;
        
        // Update statistics cards
        const monthNames = ['Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6', 
                           'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'];
        const monthName = monthNames[parseInt(month) - 1] || `Tháng ${month}`;
        
        const statCurrentKm = document.getElementById('usage-stat-current-km');
        const statGroupTotal = document.getElementById('usage-stat-group-total');
        const statPercent = document.getElementById('usage-stat-percent');
        const statOwnership = document.getElementById('usage-stat-ownership');
        const statPeriod = document.getElementById('usage-stat-period');
        
        if (statCurrentKm) statCurrentKm.textContent = myKm.toFixed(1) + ' km';
        if (statGroupTotal) statGroupTotal.textContent = totalKm.toFixed(1) + ' km';
        if (statPercent) statPercent.textContent = myUsagePercent + '%';
        if (statOwnership) statOwnership.textContent = myOwnership.toFixed(2) + '%';
        if (statPeriod) statPeriod.textContent = `${monthName}/${year}`;
        
        // Update detailed stats card
        document.getElementById('my-ownership-percent').textContent = myOwnership.toFixed(2) + '%';
        document.getElementById('group-total-km').textContent = totalKm.toFixed(1) + ' km';
        document.getElementById('my-current-km').textContent = myKm.toFixed(1) + ' km';
        document.getElementById('my-usage-percent').textContent = myUsagePercent + '%';
        
        // Update comparison bars
        document.getElementById('ownership-bar').style.width = myOwnership + '%';
        document.getElementById('usage-bar').style.width = myUsagePercent + '%';
        
        // Set current km input value
        const kmInput = document.getElementById('km-driven');
        if (myKm > 0 && !kmInput.value) {
            kmInput.value = myKm;
        }
        
        // Show stats card
        const statsCard = document.getElementById('group-usage-stats-card');
        if (statsCard) statsCard.style.display = 'block';
        
        // Update preview
        updateUsagePreview();
        
    } catch (error) {
        console.error('Error loading group usage info:', error);
        const statsCard = document.getElementById('group-usage-stats-card');
        if (statsCard) statsCard.style.display = 'none';
    }
}

function updateUsagePreview() {
    const groupId = document.getElementById('usage-group').value;
    const month = document.getElementById('usage-month').value;
    const year = document.getElementById('usage-year').value;
    const kmInput = document.getElementById('km-driven');
    const kmDriven = parseFloat(kmInput.value) || 0;
    
    if (!groupId || !month || !year || kmDriven <= 0) {
        document.getElementById('cost-preview').style.display = 'none';
        // Update statistics cards with current input
        const statCurrentKm = document.getElementById('usage-stat-current-km');
        if (statCurrentKm) statCurrentKm.textContent = kmDriven.toFixed(1) + ' km';
        return;
    }
    
    // Calculate total km including current input
    let totalKm = 0;
    if (currentGroupUsageData && currentGroupUsageData.length > 0) {
        totalKm = currentGroupUsageData.reduce((sum, u) => {
            if (u.userId === CURRENT_USER_ID) {
                return sum + kmDriven; // Use current input value
            }
            return sum + (u.kmDriven || 0);
        }, 0);
    } else {
        totalKm = kmDriven; // Only current user's input
    }
    
    // Calculate percentage
    const myPercent = totalKm > 0 ? ((kmDriven / totalKm) * 100).toFixed(2) : 0;
    
    // Update statistics cards
    const statCurrentKm = document.getElementById('usage-stat-current-km');
    const statGroupTotal = document.getElementById('usage-stat-group-total');
    const statPercent = document.getElementById('usage-stat-percent');
    
    if (statCurrentKm) statCurrentKm.textContent = kmDriven.toFixed(1) + ' km';
    if (statGroupTotal) statGroupTotal.textContent = totalKm.toFixed(1) + ' km';
    if (statPercent) statPercent.textContent = myPercent + '%';
    
    if (totalKm <= 0) {
        document.getElementById('cost-preview').style.display = 'none';
        return;
    }
    
    // Example cost: 1,000,000 VNĐ
    const exampleCost = 1000000;
    const myShare = (exampleCost * myPercent / 100).toFixed(0);
    
    // Update preview
    document.getElementById('preview-my-percent').textContent = myPercent + '%';
    document.getElementById('preview-my-share').textContent = formatCurrency(myShare);
    
    // Show preview
    document.getElementById('cost-preview').style.display = 'block';
}

async function saveUsage() {
    const groupId = parseInt(document.getElementById('usage-group').value);
    const month = parseInt(document.getElementById('usage-month').value);
    const year = parseInt(document.getElementById('usage-year').value);
    const kmDriven = parseFloat(document.getElementById('km-driven').value);
    const note = document.getElementById('usage-note').value;
    
    if (!groupId || !month || !year || isNaN(kmDriven) || kmDriven < 0) {
        showToast('Vui lòng điền đầy đủ thông tin', 'error');
        return;
    }
    
    const data = {
        groupId: groupId,
        userId: CURRENT_USER_ID,
        month: month,
        year: year,
        kmDriven: kmDriven,
        note: note || null
    };
    
    try {
        const response = await authenticatedRequest(`${API.USAGE}/update-km?groupId=${groupId}&userId=${CURRENT_USER_ID}&month=${month}&year=${year}&kmDriven=${kmDriven}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' }
        });
        
        if (response.ok) {
            showToast('Đã lưu thông tin sử dụng!', 'success');
            
            // Reload group usage info to update stats
            await loadGroupUsageInfo();
            
            // Reload history
            await loadUsageHistory();
        } else {
            const errorData = await response.json().catch(() => ({}));
            showToast(errorData.message || 'Lỗi khi lưu dữ liệu', 'error');
        }
        
    } catch (error) {
        console.error('Error saving usage:', error);
        showToast('Lỗi khi lưu dữ liệu', 'error');
    }
}

async function loadUsageHistory() {
    try {
        const response = await authenticatedRequest(`${API.USAGE}/user/${CURRENT_USER_ID}/history`);
        const history = await response.json();
        
        const container = document.getElementById('usage-history-list');
        
        if (history && history.length > 0) {
            container.innerHTML = history.map(item => `
                <div class="usage-item">
                    <div class="usage-item-left">
                        <div class="usage-period">Tháng ${item.month}/${item.year}</div>
                        <div class="usage-note">Nhóm #${item.groupId}</div>
                    </div>
                    <div class="usage-item-right">
                        <div class="usage-km">${item.kmDriven} km</div>
                        <div class="usage-percent">${item.percentage || 0}%</div>
                    </div>
                </div>
            `).join('');
        } else {
            container.innerHTML = '<p style="text-align: center; color: var(--text-light);">Chưa có dữ liệu</p>';
        }
        
    } catch (error) {
        console.error('Error loading usage history:', error);
        document.getElementById('usage-history-list').innerHTML = '<p style="text-align: center; color: var(--text-light);">Không thể tải dữ liệu</p>';
    }
}

// ============ PAYMENTS PAGE ============
async function loadPaymentsPage() {
    try {
        await loadPendingPayments();
        await loadPaymentHistory();
    } catch (error) {
        console.error('Error loading payments page:', error);
    }
}

function initPaymentMethods() {
    const methods = document.querySelectorAll('.method-card');
    methods.forEach(method => {
        method.addEventListener('click', function() {
            methods.forEach(m => m.classList.remove('active'));
            this.classList.add('active');
            
            // If there's a pending cost payment, show QR immediately
            if (window.pendingCostPayment) {
                const methodType = this.getAttribute('data-method');
                const { shareId, amount, description } = window.pendingCostPayment;
                
                // Show QR modal
                showQRCodeModalForCostShare(shareId, amount, description, methodType);
                
                // Clear pending payment
                window.pendingCostPayment = null;
            }
        });
    });
}

async function loadPendingPayments() {
    const container = document.getElementById('pending-payments-list');
    
    try {
        // Load pending cost shares (chưa thanh toán) from API
        const token = localStorage.getItem('jwtToken') || getCookie('jwtToken');
        const headers = token ? { 'Authorization': `Bearer ${token}` } : {};
        const response = await authenticatedRequest(`${API.COST_SHARES}/user/${CURRENT_USER_ID}/pending`, { 
            credentials: 'include',
            headers: headers
        });
        
        // Check for 403 error
        if (await handle403Error(response, container)) {
            return;
        }
        
        if (!response.ok) {
            throw new Error('Failed to load pending payments');
        }
        
        const pendingShares = await response.json();
        
        if (pendingShares && pendingShares.length > 0) {
            container.innerHTML = pendingShares.map(share => {
                // Get cost description or use default
                const description = share.costDescription || share.description || `Chi phí #${share.costId}`;
                const safeDescription = description.replace(/'/g, "\\'").replace(/"/g, '&quot;');
                const createdDate = share.calculatedAt || share.createdAt || new Date().toISOString();
                const amount = share.amountShare || share.shareAmount || 0;
                
                return `
                    <div class="payment-item">
                        <div class="payment-item-left">
                            <h4>${description}</h4>
                            <p><i class="fas fa-calendar"></i> ${formatDate(createdDate)}</p>
                            <small style="color: var(--text-light);">Phần chia của bạn</small>
                        </div>
                        <div class="payment-item-right">
                            <div class="payment-amount">${formatCurrency(amount)}</div>
                            <button class="btn btn-success" onclick="processCostSharePayment(${share.shareId}, ${amount}, '${safeDescription}')">
                                <i class="fas fa-credit-card"></i> Thanh toán
                            </button>
                        </div>
                    </div>
                `;
            }).join('');
        } else {
            container.innerHTML = `
                <div style="text-align: center; padding: 40px; color: var(--text-light);">
                    <i class="fas fa-check-circle" style="font-size: 48px; margin-bottom: 16px; color: var(--success);"></i>
                    <p>Bạn không có khoản thanh toán nào đang chờ</p>
                </div>
            `;
        }
    } catch (error) {
        console.error('Error loading pending payments:', error);
        container.innerHTML = `
            <div style="text-align: center; padding: 40px; color: var(--text-light);">
                <i class="fas fa-exclamation-circle" style="font-size: 48px; margin-bottom: 16px; color: var(--danger);"></i>
                <p>Không thể tải dữ liệu thanh toán</p>
            </div>
        `;
    }
}

async function loadPaymentHistory() {
    const container = document.getElementById('payment-history-list');
    
    try {
        // Load payment history from API
        const response = await authenticatedRequest(`${API.PAYMENTS}/user/${CURRENT_USER_ID}/history`);
        
        // Check for 403 error
        if (await handle403Error(response, container)) {
            return;
        }
        
        if (!response.ok) {
            throw new Error('Failed to load payment history');
        }
        
        const history = await response.json();
        
        if (history && history.length > 0) {
            container.innerHTML = history.map(item => {
                // Map payment method to Vietnamese
                const methodNames = {
                    'EWallet': 'Ví điện tử',
                    'Banking': 'Chuyển khoản',
                    'Cash': 'Tiền mặt'
                };
                const methodName = methodNames[item.method] || item.method;
                const description = item.description || `Thanh toán #${item.paymentId}`;
                
                return `
                    <div class="payment-history-item">
                        <div class="payment-history-left">
                            <div class="payment-title">${description}</div>
                            <div class="payment-date">
                                <i class="fas fa-calendar"></i> ${formatDate(item.paymentDate)}
                            </div>
                            <small style="color: var(--text-light);">Mã: ${item.transactionCode}</small>
                        </div>
                        <div class="payment-history-right">
                            <div class="payment-history-amount">${formatCurrency(item.amount)}</div>
                            <div class="payment-method">
                                <i class="fas ${getPaymentMethodIcon(item.method)}"></i>
                                ${methodName}
                            </div>
                        </div>
                    </div>
                `;
            }).join('');
        } else {
            container.innerHTML = `
                <div style="text-align: center; padding: 40px; color: var(--text-light);">
                    <i class="fas fa-history" style="font-size: 48px; margin-bottom: 16px;"></i>
                    <p>Chưa có lịch sử thanh toán</p>
                </div>
            `;
        }
    } catch (error) {
        console.error('Error loading payment history:', error);
        container.innerHTML = `
            <div style="text-align: center; padding: 40px; color: var(--text-light);">
                <i class="fas fa-exclamation-circle" style="font-size: 48px; margin-bottom: 16px; color: var(--danger);"></i>
                <p>Không thể tải lịch sử thanh toán</p>
            </div>
        `;
    }
}

// New function to handle cost share payment
async function processCostSharePayment(shareId, amount, description) {
    const selectedMethod = document.querySelector('.method-card.active');
    if (!selectedMethod) {
        showToast('Vui lòng chọn phương thức thanh toán', 'error');
        return;
    }
    
    const method = selectedMethod.getAttribute('data-method');
    
    // Show QR code modal directly with cost share info
    showQRCodeModalForCostShare(shareId, amount, description, method);
}

async function processPayment(paymentId) {
    const selectedMethod = document.querySelector('.method-card.active');
    if (!selectedMethod) {
        showToast('Vui lòng chọn phương thức thanh toán', 'error');
        return;
    }
    
    const method = selectedMethod.getAttribute('data-method');
    
    // Get payment details
    try {
        const response = await authenticatedRequest(`${API.PAYMENTS}/${paymentId}`);
        const payment = await response.json();
        
        // Show QR code modal
        showQRCodeModal(paymentId, payment, method);
        
    } catch (error) {
        console.error('Error loading payment details:', error);
        showToast('Không thể tải thông tin thanh toán', 'error');
    }
}

// New function to show QR modal for cost share payment
function showQRCodeModalForCostShare(shareId, amount, description, method) {
    // Get method info
    const methodInfo = {
        'ewallet': { name: 'Ví điện tử', bank: 'MoMo', account: '0123456789', accountName: 'NGUYEN VAN A' },
        'banking': { name: 'Chuyển khoản', bank: 'Vietcombank', account: '0987654321', accountName: 'NGUYEN VAN A' },
        'cash': { name: 'Tiền mặt', bank: 'Tiền mặt', account: 'Thanh toán trực tiếp', accountName: 'Admin' }
    };
    
    const info = methodInfo[method] || methodInfo['ewallet']; // Default to ewallet
    
    // Generate QR content (for demo - in production use real QR API)
    const qrContent = `Bank: ${info.bank}\nAccount: ${info.account}\nAmount: ${amount}\nContent: SHARE${shareId}`;
    const qrCodeUrl = `https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${encodeURIComponent(qrContent)}`;
    
    // Create modal HTML
    const modalHTML = `
        <div class="payment-modal-overlay" id="qr-modal">
            <div class="payment-modal">
                <div class="modal-header">
                    <h2><i class="fas fa-qrcode"></i> Thanh toán ${info.name}</h2>
                    <button class="close-modal" onclick="closeQRModal()">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                
                <div class="modal-body">
                    <!-- Payment Info Box with Gradient -->
                    <div class="payment-info-section">
                        <div class="info-row">
                            <span class="info-label">Số tiền:</span>
                            <span class="info-value amount">${formatCurrency(amount)}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Nội dung:</span>
                            <span class="info-value">SHARE${shareId}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Chi phí:</span>
                            <span class="info-value">${description}</span>
                        </div>
                    </div>
                    
                    <!-- Bank Info Box -->
                    <div class="bank-info-box">
                        <div class="bank-row">
                            <i class="fas fa-university"></i>
                            <span class="label">Ngân hàng:</span>
                            <span>${info.bank}</span>
                        </div>
                        <div class="bank-row">
                            <i class="fas fa-credit-card"></i>
                            <span class="label">Số TK:</span>
                            <span>${info.account}</span>
                        </div>
                        <div class="bank-row">
                            <i class="fas fa-user"></i>
                            <span class="label">Tên TK:</span>
                            <span>${info.accountName}</span>
                        </div>
                    </div>
                    
                    ${method !== 'cash' ? `
                        <div class="qr-code-section">
                            <div class="qr-code-display">
                                <h4>Quét mã QR để thanh toán</h4>
                                <div class="qr-code-img">
                                    <img src="${qrCodeUrl}" alt="QR Code">
                                </div>
                                <p class="qr-note">
                                    <i class="fas fa-info-circle"></i>
                                    Quét mã QR bằng app ${info.bank} của bạn
                                </p>
                            </div>
                        </div>
                    ` : `
                        <div class="cash-payment-section">
                            <div class="cash-notice">
                                <i class="fas fa-hand-holding-usd"></i>
                                <p>Vui lòng thanh toán tiền mặt trực tiếp cho admin</p>
                                <p class="cash-amount">${formatCurrency(amount)}</p>
                            </div>
                        </div>
                    `}
                    
                    <div class="payment-instructions">
                        <h4><i class="fas fa-clipboard-list"></i> Hướng dẫn thanh toán:</h4>
                        <ol>
                            ${method === 'ewallet' ? `
                                <li>Mở app MoMo trên điện thoại</li>
                                <li>Chọn "Quét QR" hoặc "Chuyển tiền"</li>
                                <li>Quét mã QR hoặc nhập số: ${info.account}</li>
                                <li>Kiểm tra số tiền: ${formatCurrency(amount)}</li>
                                <li>Kiểm tra nội dung: SHARE${shareId}</li>
                                <li>Xác nhận thanh toán trên app</li>
                                <li>Sau khi chuyển khoản thành công, bấm "Xác nhận thanh toán" bên dưới</li>
                            ` : method === 'banking' ? `
                                <li>Mở app ngân hàng trên điện thoại</li>
                                <li>Chọn "Chuyển khoản" hoặc "QR Pay"</li>
                                <li>Quét mã QR hoặc nhập số TK: ${info.account}</li>
                                <li>Chọn ngân hàng: ${info.bank}</li>
                                <li>Nhập số tiền: ${formatCurrency(amount)}</li>
                                <li>Nhập nội dung: SHARE${shareId}</li>
                                <li>Xác nhận và nhập OTP</li>
                                <li>Sau khi thành công, bấm "Xác nhận thanh toán" bên dưới</li>
                            ` : `
                                <li>Chuẩn bị số tiền: ${formatCurrency(amount)}</li>
                                <li>Liên hệ admin để thanh toán trực tiếp</li>
                                <li>Ghi nhớ mã: SHARE${shareId}</li>
                                <li>Sau khi thanh toán, bấm "Xác nhận thanh toán" bên dưới</li>
                            `}
                        </ol>
                    </div>
                </div>
                
                <div class="modal-footer">
                    <button class="btn btn-secondary" onclick="closeQRModal()">
                        <i class="fas fa-times"></i> Hủy
                    </button>
                    <button class="btn btn-success" onclick="confirmCostSharePayment(${shareId}, '${method}')">
                        <i class="fas fa-check-circle"></i> Xác nhận thanh toán
                    </button>
                </div>
            </div>
        </div>
    `;
    
    // Add modal to page
    document.body.insertAdjacentHTML('beforeend', modalHTML);
}

function showQRCodeModal(paymentId, payment, method) {
    // Get method info
    const methodInfo = {
        'ewallet': { name: 'Ví điện tử', bank: 'MoMo', account: '0123456789', accountName: 'NGUYEN VAN A' },
        'banking': { name: 'Chuyển khoản', bank: 'Vietcombank', account: '0987654321', accountName: 'NGUYEN VAN A' },
        'cash': { name: 'Tiền mặt', bank: 'Tiền mặt', account: 'Thanh toán trực tiếp', accountName: 'Admin' }
    };
    
    const info = methodInfo[method] || methodInfo['banking'];
    const amount = payment.amount || 0;
    
    // Generate QR content (for demo - in production use real QR API)
    const qrContent = `Bank: ${info.bank}\nAccount: ${info.account}\nAmount: ${amount}\nContent: PAY${paymentId}`;
    const qrCodeUrl = `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(qrContent)}`;
    
    // Create modal HTML
    const modalHTML = `
        <div class="payment-modal-overlay" id="qr-modal">
            <div class="payment-modal">
                <div class="modal-header">
                    <h2><i class="fas fa-qrcode"></i> Thanh toán ${info.name}</h2>
                    <button class="close-modal" onclick="closeQRModal()">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                
                <div class="modal-body">
                    <div class="payment-info-box">
                        <div class="info-row">
                            <span class="info-label">Ngân hàng/Ví:</span>
                            <span class="info-value">${info.bank}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Số tài khoản:</span>
                            <span class="info-value">${info.account}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Tên tài khoản:</span>
                            <span class="info-value">${info.accountName}</span>
                        </div>
                        <div class="info-row highlight">
                            <span class="info-label">Số tiền:</span>
                            <span class="info-value amount">${formatCurrency(amount)}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Nội dung:</span>
                            <span class="info-value">PAY${paymentId}</span>
                        </div>
                    </div>
                    
                    ${method !== 'cash' ? `
                        <div class="qr-code-container">
                            <h3>Quét mã QR để thanh toán</h3>
                            <div class="qr-code">
                                <img src="${qrCodeUrl}" alt="QR Code">
                            </div>
                            <p class="qr-note">
                                <i class="fas fa-info-circle"></i>
                                Quét mã QR bằng app ${info.bank} của bạn
                            </p>
                        </div>
                    ` : `
                        <div class="cash-payment-note">
                            <i class="fas fa-hand-holding-usd"></i>
                            <p>Vui lòng thanh toán tiền mặt trực tiếp cho admin</p>
                        </div>
                    `}
                    
                    <div class="payment-instructions">
                        <h4><i class="fas fa-clipboard-list"></i> Hướng dẫn:</h4>
                        <ol>
                            ${method !== 'cash' ? `
                                <li>Mở app ${info.bank} trên điện thoại</li>
                                <li>Quét mã QR hoặc nhập thông tin chuyển khoản</li>
                                <li>Kiểm tra số tiền và nội dung chuyển khoản</li>
                                <li>Xác nhận thanh toán trên app</li>
                                <li>Sau khi chuyển khoản thành công, bấm nút "Xác nhận đã thanh toán" bên dưới</li>
                            ` : `
                                <li>Chuẩn bị số tiền: ${formatCurrency(amount)}</li>
                                <li>Liên hệ admin để thanh toán</li>
                                <li>Sau khi thanh toán, bấm nút "Xác nhận đã thanh toán"</li>
                            `}
                        </ol>
                    </div>
                </div>
                
                <div class="modal-footer">
                    <button class="btn btn-secondary" onclick="closeQRModal()">
                        <i class="fas fa-times"></i> Hủy
                    </button>
                    <button class="btn btn-success" onclick="confirmPayment(${paymentId}, '${method}')">
                        <i class="fas fa-check-circle"></i> Xác nhận đã thanh toán
                    </button>
                </div>
            </div>
        </div>
    `;
    
    // Add modal to page
    document.body.insertAdjacentHTML('beforeend', modalHTML);
}

function closeQRModal() {
    const modal = document.getElementById('qr-modal');
    if (modal) {
        modal.remove();
    }
}

// New function to confirm cost share payment
async function confirmCostSharePayment(shareId, method) {
    // Show loading
    showToast('Đang xác nhận thanh toán...', 'info');
    
    try {
        // Generate transaction code
        const transactionCode = 'TXN' + Date.now() + Math.floor(Math.random() * 1000);
        
        // Call API to confirm payment for cost share
        const response = await authenticatedRequest(`${API.COST_SHARES}/${shareId}/payment`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                userId: CURRENT_USER_ID,
                paymentMethod: method,
                transactionCode: transactionCode,
                status: 'PAID'
            })
        });
        
        if (response.ok) {
            const result = await response.json();
            
            // Close modal
            closeQRModal();
            
            // Show success message
            showToast(`Thanh toán thành công! Mã GD: ${transactionCode}`, 'success');
            
            // Reload payment lists
            await loadPendingPayments();
            await loadPaymentHistory();
            
            // Reload costs page if it's visible
            const costsPage = document.getElementById('costs');
            if (costsPage && !costsPage.classList.contains('hidden')) {
                await loadCostsPage();
            }
            
            // Update quick stats
            await loadQuickStats();
        } else {
            const errorText = await response.text();
            console.error('Payment error:', errorText);
            showToast('Lỗi khi xác nhận thanh toán', 'error');
        }
    } catch (error) {
        console.error('Error confirming payment:', error);
        showToast('Có lỗi xảy ra. Vui lòng thử lại!', 'error');
    }
}

async function confirmPayment(paymentId, method) {
    // Show loading
    showToast('Đang xác nhận thanh toán...', 'info');
    
    try {
        // Generate transaction code
        const transactionCode = 'TXN' + Date.now() + Math.floor(Math.random() * 1000);
        
        // Call API to confirm payment
        const response = await authenticatedRequest(`${API.PAYMENTS}/${paymentId}/confirm`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                userId: CURRENT_USER_ID,
                method: method,
                transactionCode: transactionCode
            })
        });
        
        if (response.ok) {
            const result = await response.json();
            
            // Close modal
            closeQRModal();
            
            // Show success message
            showToast(`Thanh toán thành công! Mã GD: ${transactionCode}`, 'success');
            
            // Reload payment lists
            await loadPendingPayments();
            await loadPaymentHistory();
            
            // Update quick stats
            await loadQuickStats();
        } else {
            showToast('Lỗi khi xác nhận thanh toán', 'error');
        }
    } catch (error) {
        console.error('Error confirming payment:', error);
        showToast('Có lỗi xảy ra. Vui lòng thử lại!', 'error');
    }
}

// ============ UTILITY FUNCTIONS ============
function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN');
}

function getCostTypeName(type) {
    const types = {
        'ElectricCharge': 'Phí sạc điện',
        'Maintenance': 'Bảo dưỡng',
        'Insurance': 'Bảo hiểm',
        'Inspection': 'Đăng kiểm',
        'Cleaning': 'Vệ sinh',
        'Other': 'Khác'
    };
    return types[type] || type;
}

function getPaymentMethodIcon(method) {
    const icons = {
        'EWallet': 'fa-mobile-alt',
        'Banking': 'fa-university',
        'Cash': 'fa-money-bill'
    };
    return icons[method] || 'fa-credit-card';
}

function showToast(message, type) {
    const toast = document.getElementById('toast');
    const icon = toast.querySelector('.toast-icon');
    const messageEl = toast.querySelector('.toast-message');
    
    toast.className = `toast ${type}`;
    
    if (type === 'success') {
        icon.innerHTML = '<i class="fas fa-check-circle"></i>';
    } else {
        icon.innerHTML = '<i class="fas fa-exclamation-circle"></i>';
    }
    
    messageEl.textContent = message;
    
    toast.classList.add('show');
    
    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

// ============ FUND PAGE ============
async function loadFundPage() {
    try {
        await loadFundGroups();
        await loadFundStats();
        await loadMyPendingRequests();
        await loadPendingVoteRequests(); // Load các yêu cầu cần vote
        await loadRecentTransactions();
        await loadTransactionHistory();
        await loadGroupDecisions(); // Load quyết định chung
        
        // Bắt đầu auto-refresh mỗi 15 giây khi ở trang Fund
        startFundAutoRefresh();
    } catch (error) {
        console.error('Error loading fund page:', error);
    }
}

// Switch between fund tabs
function switchFundTab(tabName) {
    // Update tab buttons
    document.querySelectorAll('.fund-tab').forEach(tab => {
        tab.classList.remove('active');
    });
    const activeTab = document.querySelector(`.fund-tab[data-tab="${tabName}"]`);
    if (activeTab) {
        activeTab.classList.add('active');
    }
    
    // Update tab content
    document.querySelectorAll('.fund-tab-content').forEach(content => {
        content.classList.remove('active');
    });
    const activeContent = document.getElementById(`fund-${tabName}-tab`);
    if (activeContent) {
        activeContent.classList.add('active');
    }
}

// Auto-refresh cho trang Fund (kiểm tra yêu cầu mới mỗi 15 giây)
function startFundAutoRefresh() {
    // Dừng interval cũ nếu có
    if (fundAutoRefreshInterval) {
        clearInterval(fundAutoRefreshInterval);
    }
    
    // Chỉ auto-refresh khi đang ở trang Fund
    fundAutoRefreshInterval = setInterval(() => {
        if (currentPage === 'fund') {
            console.log('🔄 Auto-refreshing fund data...');
            loadPendingVoteRequests(); // Kiểm tra yêu cầu mới cần vote
            loadMyPendingRequests(); // Kiểm tra yêu cầu của mình
            loadFundStats(); // Cập nhật stats
        }
    }, 15000); // 15 giây
}

function stopFundAutoRefresh() {
    if (fundAutoRefreshInterval) {
        clearInterval(fundAutoRefreshInterval);
        fundAutoRefreshInterval = null;
    }
}

function initFundModals() {
    // Deposit form handler
    const depositForm = document.getElementById('depositForm');
    if (depositForm) {
        depositForm.addEventListener('submit', handleDeposit);
    }
    
    // Withdraw vote form handler
    const withdrawVoteForm = document.getElementById('withdrawVoteForm');
    if (withdrawVoteForm) {
        withdrawVoteForm.addEventListener('submit', handleWithdrawVote);
    }
    
    // Withdraw group dropdown - cập nhật số dư khi chọn nhóm
    const withdrawGroupSelect = document.getElementById('withdrawGroup');
    if (withdrawGroupSelect) {
        withdrawGroupSelect.addEventListener('change', function() {
            const selectedGroupId = this.value;
            if (selectedGroupId) {
                loadFundBalanceByGroupId(parseInt(selectedGroupId));
            } else {
                // Reset về 0 nếu không chọn nhóm
                const availableBalanceEl = document.getElementById('availableBalance');
                if (availableBalanceEl) {
                    availableBalanceEl.textContent = formatFundCurrency(0);
                }
            }
        });
    }
    
    // Filter handlers
    const filterStatus = document.getElementById('filterStatus');
    const filterType = document.getElementById('filterType');
    if (filterStatus) {
        filterStatus.addEventListener('change', loadTransactionHistory);
    }
    if (filterType) {
        filterType.addEventListener('change', loadTransactionHistory);
    }
    
    // Close modal when clicking outside
    window.addEventListener('click', function(event) {
        if (event.target.id === 'depositModal') {
            closeDepositModal();
        }
        if (event.target.id === 'withdrawVoteModal') {
            closeWithdrawVoteModal();
        }
    });
}

// Load groups for fund dropdowns (chỉ các nhóm mà user đã tham gia)
async function loadFundGroups() {
    try {
        // Chỉ load các nhóm mà user hiện tại đã tham gia
        const response = await authenticatedRequest(`/api/groups/user/${CURRENT_USER_ID}`);
        if (!response.ok) throw new Error('Failed to load groups');
        
        const groups = await response.json();
        console.log(`📦 [FUND] Loaded ${groups.length} groups for user ${CURRENT_USER_ID}:`, groups);
        
        // Fetch fundId for each group
        const groupsWithFunds = await Promise.all(
            groups.map(async (group) => {
                try {
                    const fundResponse = await authenticatedRequest(`${API.FUND}/group/${group.groupId}`);
                    if (fundResponse.ok) {
                        const fund = await fundResponse.json();
                        return {
                            ...group,
                            fundId: fund.fundId
                        };
                    } else if (fundResponse.status === 404) {
                        // Group chưa có fund là bình thường, không cần log warning
                        // console.debug(`Group ${group.groupId} chưa có fund`);
                    }
                } catch (e) {
                    // Ignore 404 errors (group chưa có fund)
                    if (e.message && !e.message.includes('404')) {
                        console.debug(`Error checking fund for group ${group.groupId}:`, e.message);
                    }
                }
                return group;
            })
        );
        
        // Populate deposit dropdown - chỉ các nhóm user đã tham gia
        const depositSelect = document.getElementById('depositGroup');
        if (depositSelect) {
            depositSelect.innerHTML = '<option value="">Chọn nhóm</option>' +
                groupsWithFunds
                    .map(g => `<option value="${g.groupId}" data-fund-id="${g.fundId || ''}">${g.groupName}${g.fundId ? '' : ' (chưa có quỹ)'}</option>`)
                    .join('');
        }
        
        // Populate withdraw dropdown - chỉ nhóm có quỹ và user đã tham gia mới rút được
        const withdrawSelect = document.getElementById('withdrawGroup');
        if (withdrawSelect) {
            const groupsWithFundsOnly = groupsWithFunds.filter(g => g.fundId);
            if (groupsWithFundsOnly.length === 0) {
                withdrawSelect.innerHTML = '<option value="">Bạn chưa tham gia nhóm nào có quỹ</option>';
            } else {
                withdrawSelect.innerHTML = '<option value="">Chọn nhóm</option>' +
                    groupsWithFundsOnly
                        .map(g => `<option value="${g.groupId}" data-fund-id="${g.fundId}">${g.groupName}</option>`)
                        .join('');
            }
        }
        
    } catch (error) {
        console.error('Error loading groups:', error);
        const depositSelect = document.getElementById('depositGroup');
        const withdrawSelect = document.getElementById('withdrawGroup');
        if (depositSelect) depositSelect.innerHTML = '<option value="">Không thể tải nhóm</option>';
        if (withdrawSelect) withdrawSelect.innerHTML = '<option value="">Không thể tải nhóm</option>';
    }
}

async function loadFundStats() {
    try {
        // Gọi API với userId để chỉ lấy số dư của các nhóm mà user tham gia
        const response = await authenticatedRequest(`${API.FUND}/stats?userId=${CURRENT_USER_ID}`);
        
        // Check for 403 error - find a suitable container to show the message
        const statsContainer = document.getElementById('fund-stats-container') || 
                              document.querySelector('.fund-stats') ||
                              document.getElementById('fund-page');
        if (await handle403Error(response, statsContainer)) {
            return;
        }
        
        // 404 means user has no funds yet, which is normal - show zero values
        if (response.status === 404) {
            const totalBalanceEl = document.getElementById('totalBalance');
            const myDepositsEl = document.getElementById('myDeposits');
            const myWithdrawsEl = document.getElementById('myWithdraws');
            const myPendingEl = document.getElementById('myPending');
            
            if (totalBalanceEl) totalBalanceEl.textContent = formatFundCurrency(0);
            if (myDepositsEl) myDepositsEl.textContent = formatFundCurrency(0);
            if (myWithdrawsEl) myWithdrawsEl.textContent = formatFundCurrency(0);
            if (myPendingEl) myPendingEl.textContent = '0';
            return;
        }
        
        if (!response.ok) throw new Error('Failed to load stats');
        
        const stats = await response.json();
        
        // Update stats cards
        const totalBalanceEl = document.getElementById('totalBalance');
        const myDepositsEl = document.getElementById('myDeposits');
        const myWithdrawsEl = document.getElementById('myWithdraws');
        const myPendingEl = document.getElementById('myPending');
        
        if (totalBalanceEl) totalBalanceEl.textContent = formatFundCurrency(stats.totalBalance);
        if (myDepositsEl) myDepositsEl.textContent = formatFundCurrency(stats.myDeposits || 0);
        if (myWithdrawsEl) myWithdrawsEl.textContent = formatFundCurrency(stats.myWithdraws || 0);
        if (myPendingEl) myPendingEl.textContent = stats.myPendingCount || 0;
        
        // Update summary
        const summaryOpeningEl = document.getElementById('summaryOpening');
        const summaryIncomeEl = document.getElementById('summaryIncome');
        const summaryExpenseEl = document.getElementById('summaryExpense');
        const summaryBalanceEl = document.getElementById('summaryBalance');
        
        if (summaryOpeningEl) summaryOpeningEl.textContent = formatFundCurrency(stats.openingBalance);
        if (summaryIncomeEl) summaryIncomeEl.textContent = formatFundCurrency(stats.totalIncome);
        if (summaryExpenseEl) summaryExpenseEl.textContent = formatFundCurrency(stats.totalExpense);
        if (summaryBalanceEl) summaryBalanceEl.textContent = formatFundCurrency(stats.totalBalance);
        
    } catch (error) {
        console.error('Error loading stats:', error);
    }
}

async function loadMyPendingRequests() {
    try {
        const response = await authenticatedRequest(`${API.FUND}/transactions?status=Pending&userId=${CURRENT_USER_ID}`);
        
        // 404 means no pending requests, which is normal
        if (response.status === 404) {
            const container = document.getElementById('my-pending-requests-list');
            if (container) {
                container.innerHTML = '<p style="text-align: center; color: var(--text-light); padding: 20px;">Bạn chưa có yêu cầu nào đang chờ</p>';
            }
            return;
        }
        
        if (!response.ok) throw new Error('Failed to load pending requests');
        
        const transactions = await response.json();
        if (!Array.isArray(transactions)) {
            console.warn('⚠️ Expected array but got:', transactions);
            updateMyPendingDisplay([]);
            return;
        }
        
        // Filter only my withdrawal requests (deposits don't need approval)
        const myRequests = transactions.filter(t => {
            const userId = t.userId || t.user_id || t.createdBy || t.created_by;
            const transactionType = t.transactionType || t.transaction_type || t.type;
            return userId === CURRENT_USER_ID && 
                   (transactionType === 'Withdraw' || transactionType === 'WITHDRAW');
        });
        
        updateMyPendingDisplay(myRequests);
        
    } catch (error) {
        console.error('Error loading my pending requests:', error);
        updateMyPendingDisplay([]);
    }
}

function updateMyPendingDisplay(requests) {
    const badge = document.getElementById('myPendingBadge');
    const tbody = document.getElementById('myPendingBody');
    const requestsTabBadge = document.getElementById('requestsTabBadge');
    
    if (badge) badge.textContent = requests.length;
    
    // Cập nhật badge trên tab (tổng số yêu cầu cần xử lý)
    if (requestsTabBadge) {
        // Tính tổng: yêu cầu của tôi + yêu cầu cần vote
        const pendingVoteCount = document.getElementById('pendingVoteBadge')?.textContent || 0;
        const totalRequests = requests.length + parseInt(pendingVoteCount);
        requestsTabBadge.textContent = totalRequests;
        requestsTabBadge.style.display = totalRequests > 0 ? 'inline-flex' : 'none';
    }
    
    if (!tbody) return;
    
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
        const groupId = t.groupId || t.group_id || (t.group && (t.group.groupId || t.group.id));
        const voteCount = t.voteCount || { approve: 0, reject: 0, total: 0 };
        const hasVotes = voteCount.total > 0;
        
        return `
        <tr>
            <td>${formatFundDate(t.date || t.createdAt)}</td>
            <td class="amount negative">
                ${formatFundCurrency(t.amount)}
            </td>
            <td>${t.purpose || '-'}</td>
            <td>
                <span class="badge badge-${getFundStatusClass(t.status)}">
                    ${getFundStatusIcon(t.status)} ${getFundStatusText(t.status)}
                </span>
            </td>
            <td>
                ${hasVotes
                    ? `<div style="font-size: 0.9em;">
                         <i class="fas fa-vote-yea"></i> 
                         Đồng ý: ${voteCount.approve || 0} | 
                         Từ chối: ${voteCount.reject || 0}
                       </div>`
                    : '<span class="text-muted">Chưa có vote</span>'
                }
            </td>
            <td>
                <button class="btn btn-sm btn-outline" onclick="viewTransactionDetail(${t.transactionId})">
                    <i class="fas fa-eye"></i>
                </button>
                ${t.status === 'Pending' 
                    ? `<button class="btn btn-sm btn-danger" onclick="cancelRequest(${t.transactionId})">
                         <i class="fas fa-times"></i>
                       </button>`
                    : ''
                }
            </td>
        </tr>
    `}).join('');
}

async function loadRecentTransactions() {
    try {
        const response = await authenticatedRequest(`${API.FUND}/transactions?status=Completed&userId=${CURRENT_USER_ID}`);
        
        // 404 means no transactions yet, which is normal
        if (response.status === 404) {
            updateRecentTransactionsDisplay([]);
            return;
        }
        
        if (!response.ok) throw new Error('Failed to load transactions');
        
        const transactions = await response.json();
        if (!Array.isArray(transactions)) {
            console.warn('⚠️ Expected array but got:', transactions);
            transactions = [];
        }
        
        // Take only last 5
        const recent = transactions.slice(0, 5);
        
        const container = document.getElementById('recentTransactions');
        if (!container) {
            console.warn('⚠️ Container #recentTransactions not found');
            return;
        }
        
        if (recent.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-receipt"></i>
                    <p>Chưa có giao dịch nào</p>
                </div>
            `;
            return;
        }
        
        container.innerHTML = recent.map(t => {
            const transactionType = t.transactionType || t.transaction_type || t.type;
            const date = t.date || t.createdAt || t.created_at;
            const isWithdraw = transactionType === 'Withdraw' || transactionType === 'WITHDRAW';
            
            return `
            <div class="transaction-item">
                <div class="transaction-icon ${isWithdraw ? 'expense' : 'income'}">
                    <i class="fas fa-${isWithdraw ? 'arrow-down' : 'arrow-up'}"></i>
                </div>
                <div class="transaction-info">
                    <div class="transaction-title">${t.purpose || 'Không có mục đích'}</div>
                    <div class="transaction-date">${formatFundDate(date)}</div>
                </div>
                <div class="transaction-amount ${isWithdraw ? 'negative' : 'positive'}">
                    ${isWithdraw ? '-' : '+'} ${formatFundCurrency(t.amount)}
                </div>
            </div>
            `;
        }).join('');
        
    } catch (error) {
        console.error('Error loading recent transactions:', error);
        const container = document.getElementById('recentTransactions');
        if (container) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-exclamation-triangle"></i>
                    <p>Không thể tải giao dịch</p>
                </div>
            `;
        }
    }
}

// Load các yêu cầu rút tiền cần vote (của các thành viên khác trong nhóm)
async function loadPendingVoteRequests() {
    try {
        console.log('🔍 Loading pending vote requests for user:', CURRENT_USER_ID);
        
        // Lấy danh sách các nhóm mà user tham gia
        const groupsResponse = await authenticatedRequest(`/api/groups/user/${CURRENT_USER_ID}`);
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
                const fundResponse = await authenticatedRequest(`${API.FUND}/group/${group.groupId}`);
                // 404 means group doesn't have a fund yet, which is normal - skip this group
                if (fundResponse.status === 404 || !fundResponse.ok) continue;
                
                const fund = await fundResponse.json();
                if (!fund || !fund.fundId) continue;
                
                const fundId = fund.fundId;
                
                // Lấy pending requests của fund này
                const pendingUrl = `/api/funds/${fundId}/pending-requests`;
                console.log(`🔍 Fetching pending requests from: ${pendingUrl} for group ${group.groupId} (${group.groupName || group.group_name || 'N/A'})`);
                const requestsResponse = await authenticatedRequest(pendingUrl);
                if (!requestsResponse.ok) {
                    console.warn(`⚠️ Failed to fetch pending requests for fund ${fundId}: ${requestsResponse.status}`);
                    continue;
                }
                
                let requests;
                try {
                    const responseText = await requestsResponse.text();
                    if (!responseText || responseText.trim() === '') {
                        console.warn(`⚠️ Empty response for fund ${fundId}`);
                        continue;
                    }
                    requests = JSON.parse(responseText);
                } catch (e) {
                    console.error(`❌ Error parsing JSON for fund ${fundId}:`, e);
                    continue;
                }
                
                if (!Array.isArray(requests)) {
                    console.warn(`⚠️ Response is not an array for fund ${fundId}:`, typeof requests, requests);
                    continue;
                }
                
                console.log(`📋 Found ${requests.length} pending requests for fund ${fundId} (group ${group.groupId})`);
                console.log(`📋 Current user ID: ${CURRENT_USER_ID} (type: ${typeof CURRENT_USER_ID})`);
                console.log(`📋 Raw requests data:`, JSON.stringify(requests, null, 2));
                
                // Filter: chỉ các withdrawal requests CỦA các user KHÁC (để user hiện tại bỏ phiếu)
                let addedCount = 0;
                let skippedCount = 0;
                requests.forEach((req, index) => {
                    const transactionType = req.transactionType || req.transaction_type || req.type;
                    const status = req.status || req.transaction_status || req.transactionStatus;
                    const userId = req.userId || req.user_id || req.createdBy || req.created_by;
                    const transactionId = req.transactionId || req.transaction_id;
                    
                    const isWithdraw = transactionType === 'Withdraw' || transactionType === 'WITHDRAW' || transactionType === 'withdraw';
                    const isPending = status === 'Pending' || status === 'PENDING' || status === 'pending';
                    
                    // So sánh userId với CURRENT_USER_ID (cả string và number)
                    const userIdNum = userId != null ? parseInt(userId) : null;
                    const currentUserIdNum = CURRENT_USER_ID != null ? parseInt(CURRENT_USER_ID) : null;
                    const isMyRequest = userId == CURRENT_USER_ID || 
                                       userIdNum === currentUserIdNum ||
                                       String(userId) === String(CURRENT_USER_ID);
                    const isNotMyRequest = !isMyRequest; // Yêu cầu của user KHÁC
                    
                    console.log(`  📝 [${index + 1}/${requests.length}] Request #${transactionId}: userId=${userId} (type: ${typeof userId}), type=${transactionType}, status=${status}, isWithdraw=${isWithdraw}, isPending=${isPending}, isNotMyRequest=${isNotMyRequest}`);
                    console.log(`    → userIdNum=${userIdNum}, currentUserIdNum=${currentUserIdNum}`);
                    
                    // Chỉ hiển thị yêu cầu của các user KHÁC để user hiện tại vote
                    if (isWithdraw && isPending && isNotMyRequest) {
                        console.log(`    ✅ Adding request #${transactionId} to pending vote list (user ${userId}'s request for user ${CURRENT_USER_ID} to vote)`);
                        allPendingRequests.push({
                            ...req,
                            groupName: group.groupName || group.group_name || `Nhóm ${group.groupId}`,
                            groupId: group.groupId,
                            fundId: fundId,
                            requesterId: userId
                        });
                        addedCount++;
                    } else {
                        if (isMyRequest) {
                            console.log(`    ⚠️ Skipping request #${transactionId}: This is YOUR request (user ${userId}), you cannot vote for your own request`);
                        } else {
                            console.log(`    ❌ Skipping request #${transactionId}: isWithdraw=${isWithdraw}, isPending=${isPending}, isNotMyRequest=${isNotMyRequest}`);
                        }
                        skippedCount++;
                    }
                });
                console.log(`📊 Summary for fund ${fundId}: Added ${addedCount}, Skipped ${skippedCount} out of ${requests.length} total requests`);
            } catch (e) {
                console.warn(`Error loading requests for group ${group.groupId}:`, e);
            }
        }
        
        console.log(`✅ Total pending vote requests found: ${allPendingRequests.length}`);
        console.log('✅ Pending vote requests:', allPendingRequests);
        
        // Kiểm tra xem có yêu cầu mới không (so với lần trước)
        // Chỉ hiển thị thông báo nếu:
        // 1. Có yêu cầu mới (số lượng tăng) và đã có yêu cầu trước đó - để tránh thông báo khi lần đầu load trang
        // HOẶC đang ở trang Fund và có yêu cầu (để user biết ngay khi vào trang)
        if (allPendingRequests.length > lastPendingVoteCount) {
            if (lastPendingVoteCount > 0) {
                // Có yêu cầu mới được tạo
                const newCount = allPendingRequests.length - lastPendingVoteCount;
                showToast(`🔔 Có ${newCount} yêu cầu rút tiền mới cần bạn bỏ phiếu!`, 'info');
            } else if (allPendingRequests.length > 0 && currentPage === 'fund') {
                // Lần đầu vào trang Fund và có yêu cầu đang chờ
                showToast(`🔔 Có ${allPendingRequests.length} yêu cầu rút tiền đang chờ bạn bỏ phiếu!`, 'info');
            }
        }
        lastPendingVoteCount = allPendingRequests.length;
        
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
    const voteSection = document.getElementById('pendingVoteSection');
    const voteBadge = document.getElementById('pendingVoteBadge');
    const voteBody = document.getElementById('pendingVoteBody');
    const requestsTabBadge = document.getElementById('requestsTabBadge');
    
    if (!voteSection || !voteBadge || !voteBody) {
        console.warn('⚠️ Pending vote section elements not found');
        return;
    }
    
    // Luôn hiển thị section để user biết có phần này
    voteSection.style.display = 'block';
    
    // Cập nhật badge
    voteBadge.textContent = requests.length;
    
    // Cập nhật badge trên tab
    if (requestsTabBadge) {
        requestsTabBadge.textContent = requests.length;
        requestsTabBadge.style.display = requests.length > 0 ? 'inline-flex' : 'none';
    }
    
    // Render danh sách yêu cầu hoặc message trống
    if (requests.length === 0) {
        voteBody.innerHTML = `
            <tr>
                <td colspan="6" class="empty-table">
                    <div class="empty-state">
                        <i class="fas fa-check-circle"></i>
                        <p>Không có yêu cầu nào cần bỏ phiếu</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }
    
    // Render danh sách yêu cầu với form chi tiết
    voteBody.innerHTML = requests.map(req => {
        const date = req.date || req.createdAt || req.created_at;
        const transactionId = req.transactionId || req.transaction_id;
        const amount = req.amount || 0;
        const purpose = req.purpose || '-';
        const requesterId = req.requesterId || req.userId || req.user_id;
        const requesterName = req.requesterName || req.userName || `User #${requesterId}`;
        const groupName = req.groupName || `Nhóm ${req.groupId}`;
        const fundId = req.fundId;
        const currentBalance = req.currentBalance || req.balance || 0;
        const voteCount = req.voteCount || { approve: 0, reject: 0, total: 0 };
        
        return `
        <tr class="withdraw-request-row" data-transaction-id="${transactionId}">
            <td>
                <div>
                    <strong>${requesterName}</strong>
                    <small style="display: block; color: var(--text-light);">ID: #${requesterId}</small>
                </div>
            </td>
            <td>
                <div>
                    <div>${formatFundDate(date)}</div>
                    <small style="color: var(--text-light);">${new Date(date).toLocaleTimeString('vi-VN')}</small>
                </div>
            </td>
            <td class="amount negative">
                <strong style="font-size: 1.1em;">${formatFundCurrency(amount)}</strong>
            </td>
            <td>
                <div style="max-width: 200px;">
                    <div style="font-weight: 500;">${purpose}</div>
                </div>
            </td>
            <td>
                <div>
                    <div>${groupName}</div>
                    <small style="color: var(--text-light);">Số dư: ${formatFundCurrency(currentBalance)}</small>
                </div>
            </td>
            <td>
                <div style="display: flex; flex-direction: column; gap: 0.5rem;">
                    <button class="btn btn-sm btn-info" onclick="openWithdrawRequestVoteModal(${transactionId}, ${fundId})" title="Xem chi tiết và bỏ phiếu">
                        <i class="fas fa-info-circle"></i> Xem chi tiết
                    </button>
                    <div style="display: flex; gap: 0.5rem;">
                        <button class="btn btn-sm btn-success" onclick="voteOnWithdrawRequest(${transactionId}, ${fundId}, true)" title="Đồng ý">
                            <i class="fas fa-check"></i> Đồng ý
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="voteOnWithdrawRequest(${transactionId}, ${fundId}, false)" title="Từ chối">
                            <i class="fas fa-times"></i> Từ chối
                        </button>
                    </div>
                    <div style="font-size: 0.85em; color: var(--text-light); text-align: center;">
                        <i class="fas fa-vote-yea"></i> 
                        Đồng ý: ${voteCount.approve || 0} | 
                        Từ chối: ${voteCount.reject || 0}
                    </div>
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
        const url = `${API.FUND}/transactions/${transactionId}/vote`;
        
        console.log(`🗳️ Voting ${approve ? 'approve' : 'reject'} for transaction ${transactionId}`);
        
        const response = await authenticatedRequest(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                transactionId: transactionId,
                userId: CURRENT_USER_ID,
                approve: approve
            })
        });
        
        if (!response.ok) {
            let errorData;
            try {
                errorData = await response.json();
            } catch (e) {
                const errorText = await response.text();
                errorData = { error: errorText };
            }
            throw new Error(errorData.error || 'Failed to vote');
        }
        
        // Một số endpoint trả về 204 No Content -> không có body JSON
        let result = {};
        try {
            const raw = await response.text();
            if (raw && raw.trim().length > 0) {
                result = JSON.parse(raw);
            }
        } catch (parseErr) {
            console.warn('Failed to parse removeMember response body, continuing anyway:', parseErr);
        }
        console.log('✅ Vote result:', result);
        
        showToast(approve 
            ? '✅ Bạn đã đồng ý yêu cầu rút tiền này'
            : '❌ Bạn đã từ chối yêu cầu rút tiền này', 'success');
        
        // Reload data
        loadPendingVoteRequests();
        loadFundStats();
        loadMyPendingRequests();
        loadTransactionHistory();
        
    } catch (error) {
        console.error('Error voting:', error);
        showToast('❌ Lỗi: ' + error.message, 'error');
    }
}

/**
 * Mở modal chi tiết yêu cầu rút tiền và form bỏ phiếu
 */
async function openWithdrawRequestVoteModal(transactionId, fundId) {
    try {
        const modal = document.getElementById('withdrawRequestVoteModal');
        const modalBody = document.getElementById('withdrawRequestVoteModalBody');
        const modalFooter = document.getElementById('withdrawRequestVoteModalFooter');
        
        if (!modal || !modalBody || !modalFooter) {
            console.error('Modal elements not found');
            return;
        }
        
        // Hiển thị loading
        modalBody.innerHTML = '<div style="text-align: center; padding: 2rem;"><i class="fas fa-spinner fa-spin"></i> Đang tải thông tin...</div>';
        modalFooter.innerHTML = '';
        modal.style.display = 'flex';
        
        // Lấy thông tin chi tiết về transaction
        const transactionResponse = await authenticatedRequest(`${API.FUND}/transactions/${transactionId}`);
        if (!transactionResponse.ok) {
            throw new Error('Không thể tải thông tin yêu cầu');
        }
        
        const transaction = await transactionResponse.json();
        
        // Lấy thông tin vote - ưu tiên từ transaction response (đã có voteCount)
        let voteInfo = { approve: 0, reject: 0, total: 0, votes: [] };
        
        // Nếu transaction có voteCount, sử dụng nó
        if (transaction.voteCount) {
            voteInfo = {
                approve: transaction.voteCount.approve || 0,
                reject: transaction.voteCount.reject || 0,
                total: transaction.voteCount.total || 0,
                votes: []
            };
        } else {
            // Nếu không có, thử gọi API riêng
            try {
                const voteResponse = await authenticatedRequest(`${API.FUND}/transactions/${transactionId}/votes`);
                if (voteResponse.ok) {
                    voteInfo = await voteResponse.json();
                }
            } catch (e) {
                console.warn('Could not load vote info:', e);
            }
        }
        
        // Lấy thông tin nhóm - ưu tiên từ transaction response
        const groupId = transaction.groupId || transaction.group_id;
        let groupName = 'N/A';
        if (groupId) {
            try {
                const groupResponse = await authenticatedRequest(`/api/groups/${groupId}`);
                if (groupResponse.ok) {
                    const group = await groupResponse.json();
                    groupName = group.groupName || group.group_name || `Nhóm ${groupId}`;
                }
            } catch (e) {
                console.warn('Could not load group info:', e);
                // Fallback: nếu không lấy được, dùng tên mặc định
                groupName = `Nhóm ${groupId}`;
            }
        }
        
        const requesterId = transaction.userId || transaction.user_id || transaction.createdBy;
        const requesterName = transaction.userName || transaction.user_name || `User #${requesterId}`;
        const amount = transaction.amount || 0;
        const purpose = transaction.purpose || '-';
        const date = transaction.createdAt || transaction.created_at || transaction.date;
        const status = transaction.status || transaction.transaction_status || 'Pending';
        // Lấy currentBalance từ transaction response (đã được thêm từ backend)
        const currentBalance = transaction.currentBalance || transaction.balance || 0;
        
        // Render modal body
        modalBody.innerHTML = `
            <div class="withdraw-request-detail">
                <div class="alert alert-info" style="margin-bottom: 1.5rem;">
                    <i class="fas fa-info-circle"></i>
                    <div>
                        <strong>Yêu cầu rút tiền đang chờ bỏ phiếu</strong>
                        <p style="margin: 0.5rem 0 0 0;">Tất cả thành viên trong nhóm có thể bỏ phiếu đồng ý hoặc từ chối yêu cầu này.</p>
                    </div>
                </div>
                
                <div class="detail-section">
                    <h4 style="margin-bottom: 1rem; color: var(--primary-color);">
                        <i class="fas fa-user"></i> Thông tin người yêu cầu
                    </h4>
                    <div class="detail-grid">
                        <div class="detail-item">
                            <label>Người yêu cầu:</label>
                            <div><strong>${requesterName}</strong> <small>(ID: #${requesterId})</small></div>
                        </div>
                        <div class="detail-item">
                            <label>Ngày tạo:</label>
                            <div>${formatFundDate(date)} ${new Date(date).toLocaleTimeString('vi-VN')}</div>
                        </div>
                    </div>
                </div>
                
                <div class="detail-section">
                    <h4 style="margin-bottom: 1rem; color: var(--primary-color);">
                        <i class="fas fa-money-bill-wave"></i> Thông tin yêu cầu
                    </h4>
                    <div class="detail-grid">
                        <div class="detail-item">
                            <label>Số tiền yêu cầu:</label>
                            <div class="amount-large negative"><strong>${formatFundCurrency(amount)}</strong></div>
                        </div>
                        <div class="detail-item">
                            <label>Nhóm:</label>
                            <div><strong>${groupName}</strong></div>
                        </div>
                        <div class="detail-item">
                            <label>Số dư hiện tại của nhóm:</label>
                            <div>${formatFundCurrency(currentBalance)}</div>
                        </div>
                        <div class="detail-item">
                            <label>Trạng thái:</label>
                            <div>
                                <span class="badge badge-warning">${status}</span>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="detail-section">
                    <h4 style="margin-bottom: 1rem; color: var(--primary-color);">
                        <i class="fas fa-file-alt"></i> Mục đích rút tiền
                    </h4>
                    <div class="purpose-box">
                        <p>${purpose}</p>
                    </div>
                </div>
                
                <div class="detail-section">
                    <h4 style="margin-bottom: 1rem; color: var(--primary-color);">
                        <i class="fas fa-vote-yea"></i> Kết quả bỏ phiếu
                    </h4>
                    <div class="vote-summary">
                        <div class="vote-stat">
                            <div class="vote-stat-label">
                                <i class="fas fa-check-circle" style="color: var(--success-color);"></i>
                                Đồng ý
                            </div>
                            <div class="vote-stat-value">${voteInfo.approve || 0}</div>
                        </div>
                        <div class="vote-stat">
                            <div class="vote-stat-label">
                                <i class="fas fa-times-circle" style="color: var(--danger-color);"></i>
                                Từ chối
                            </div>
                            <div class="vote-stat-value">${voteInfo.reject || 0}</div>
                        </div>
                        <div class="vote-stat">
                            <div class="vote-stat-label">
                                <i class="fas fa-users"></i>
                                Tổng số vote
                            </div>
                            <div class="vote-stat-value">${voteInfo.total || 0}</div>
                        </div>
                    </div>
                    ${voteInfo.votes && voteInfo.votes.length > 0 ? `
                        <div style="margin-top: 1rem;">
                            <h5 style="margin-bottom: 0.5rem;">Danh sách thành viên đã bỏ phiếu:</h5>
                            <div class="votes-list">
                                ${voteInfo.votes.map(vote => `
                                    <div class="vote-item">
                                        <span><strong>User #${vote.userId}</strong></span>
                                        <span class="badge ${vote.approve ? 'badge-success' : 'badge-danger'}">
                                            ${vote.approve ? 'Đồng ý' : 'Từ chối'}
                                        </span>
                                    </div>
                                `).join('')}
                            </div>
                        </div>
                    ` : ''}
                </div>
            </div>
        `;
        
        // Render modal footer với nút bỏ phiếu
        modalFooter.innerHTML = `
            <button type="button" class="btn btn-secondary" onclick="closeWithdrawRequestVoteModal()">
                <i class="fas fa-times"></i> Đóng
            </button>
            <button type="button" class="btn btn-success" onclick="voteOnWithdrawRequestFromModal(${transactionId}, ${fundId}, true)">
                <i class="fas fa-check"></i> Đồng ý
            </button>
            <button type="button" class="btn btn-danger" onclick="voteOnWithdrawRequestFromModal(${transactionId}, ${fundId}, false)">
                <i class="fas fa-times"></i> Từ chối
            </button>
        `;
        
    } catch (error) {
        console.error('Error opening withdraw request vote modal:', error);
        const modalBody = document.getElementById('withdrawRequestVoteModalBody');
        if (modalBody) {
            modalBody.innerHTML = `
                <div class="alert alert-danger">
                    <i class="fas fa-exclamation-triangle"></i>
                    <div>
                        <strong>Lỗi:</strong> ${error.message}
                    </div>
                </div>
            `;
        }
    }
}

/**
 * Đóng modal chi tiết yêu cầu rút tiền
 */
function closeWithdrawRequestVoteModal() {
    const modal = document.getElementById('withdrawRequestVoteModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

/**
 * Bỏ phiếu từ modal
 */
async function voteOnWithdrawRequestFromModal(transactionId, fundId, approve) {
    closeWithdrawRequestVoteModal();
    await voteOnWithdrawRequest(transactionId, fundId, approve);
}

async function loadTransactionHistory() {
    try {
        const statusEl = document.getElementById('filterStatus');
        const typeEl = document.getElementById('filterType');
        const categoryEl = document.getElementById('filterCategory');
        const status = statusEl ? statusEl.value : '';
        const type = typeEl ? typeEl.value : '';
        
        let url = `${API.FUND}/transactions?userId=${CURRENT_USER_ID}`;
        if (status) url += `&status=${status}`;
        if (type) url += `&type=${type}`;
        
        const response = await authenticatedRequest(url);
        
        // 404 means no transactions yet, which is normal
        if (response.status === 404) {
            updateTransactionTable([]);
            return;
        }
        
        if (!response.ok) throw new Error('Failed to load transactions');
        
        const transactions = await response.json();
        
        updateTransactionTable(transactions);
        
        // Update category filter event listener
        if (categoryEl && !categoryEl.hasAttribute('data-listener-added')) {
            categoryEl.setAttribute('data-listener-added', 'true');
            categoryEl.addEventListener('change', () => {
                loadTransactionHistory();
            });
        }
        
    } catch (error) {
        console.error('Error loading transaction history:', error);
    }
}

function updateTransactionTable(transactions) {
    const tbody = document.getElementById('transactionsTableBody');
    if (!tbody) return;
    
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
    
    // Phân loại giao dịch theo mục đích
    const categorizeTransaction = (purpose) => {
        if (!purpose) return 'other';
        const purposeLower = purpose.toLowerCase();
        if (purposeLower.includes('bảo dưỡng') || purposeLower.includes('maintenance') || 
            purposeLower.includes('sửa chữa') || purposeLower.includes('repair')) {
            return 'maintenance';
        }
        if (purposeLower.includes('dự phòng') || purposeLower.includes('reserve') || 
            purposeLower.includes('phòng ngừa') || purposeLower.includes('emergency')) {
            return 'reserve';
        }
        return 'other';
    };
    
    const categoryEl = document.getElementById('filterCategory');
    const selectedCategory = categoryEl ? categoryEl.value : '';
    
    let filteredTransactions = transactions;
    if (selectedCategory) {
        filteredTransactions = transactions.filter(t => {
            const category = categorizeTransaction(t.purpose);
            return category === selectedCategory;
        });
    }
    
    if (filteredTransactions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="empty-table">
                    <div class="empty-state">
                        <i class="fas fa-filter"></i>
                        <p>Không có giao dịch nào phù hợp với bộ lọc</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = filteredTransactions.map(t => {
        const category = categorizeTransaction(t.purpose);
        const categoryBadge = category === 'maintenance' ? '<span class="badge badge-info">🔧 Bảo Dưỡng</span>' :
                            category === 'reserve' ? '<span class="badge badge-warning">🛡️ Dự Phòng</span>' :
                            '<span class="badge badge-secondary">📋 Khác</span>';
        
        return `
        <tr>
            <td>${formatFundDate(t.date || t.createdAt)}</td>
            <td>
                <div style="display: flex; flex-direction: column; gap: 0.25rem;">
                    <span class="badge ${t.transactionType === 'Deposit' ? 'badge-success' : 'badge-warning'}">
                        ${t.transactionType === 'Deposit' ? '📥 Nạp tiền' : '📤 Rút tiền'}
                    </span>
                    ${categoryBadge}
                </div>
            </td>
            <td>${t.purpose || '-'}</td>
            <td class="amount ${t.transactionType === 'Withdraw' ? 'negative' : 'positive'}">
                ${formatFundCurrency(t.amount)}
            </td>
            <td>
                <span class="badge badge-${getFundStatusClass(t.status)}">
                    ${getFundStatusIcon(t.status)} ${getFundStatusText(t.status)}
                </span>
            </td>
            <td>User #${t.userId || 'Unknown'}</td>
        </tr>
    `;
    }).join('');
}

// Modal functions
function openDepositModal() {
    const modal = document.getElementById('depositModal');
    if (modal) {
        modal.classList.add('show');
        modal.style.display = 'flex';
        const form = document.getElementById('depositForm');
        if (form) form.reset();
        
        // Reload groups để đảm bảo chỉ hiển thị nhóm user đã tham gia
        loadFundGroups();
    }
}

function closeDepositModal() {
    const modal = document.getElementById('depositModal');
    if (modal) {
        modal.classList.remove('show');
        modal.style.display = 'none';
    }
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
            console.log(`💰 Creating fund for group ${groupId}...`);
            const createResponse = await authenticatedRequest(`${API.FUND}/group/${groupId}/create`, {
                method: 'POST'
            });
            
            if (createResponse.ok) {
                const newFund = await createResponse.json();
                fundId = newFund.fundId;
                console.log(`✅ Fund created successfully: fundId=${fundId}`);
            } else {
                // Lấy thông báo lỗi chi tiết từ response
                let errorMessage = 'Không thể tạo quỹ mới';
                try {
                    const errorData = await createResponse.json().catch(() => ({}));
                    if (errorData.error) {
                        errorMessage = errorData.error;
                    } else if (errorData.message) {
                        errorMessage = errorData.message;
                    }
                } catch (e) {
                    const errorText = await createResponse.text().catch(() => '');
                    if (errorText) {
                        errorMessage = errorText;
                    }
                }
                console.error(`❌ Failed to create fund: ${errorMessage} (Status: ${createResponse.status})`);
                throw new Error(errorMessage);
            }
        }
        
        const data = {
            fundId: parseInt(fundId),
            userId: CURRENT_USER_ID,
            amount: parseFloat(formData.get('amount')),
            purpose: formData.get('purpose')
        };
        
        const response = await authenticatedRequest(`${API.FUND}/deposit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        
        const result = await safeParseJsonBody(response);
        
        if (!response.ok) {
            throw new Error(result.error || result.message || 'Failed to deposit');
        }
        
        if (result.success) {
            showToast('✅ Nạp tiền thành công!', 'success');
            closeDepositModal();
            
            // Reload data
            loadFundGroups();
            loadFundStats();
            loadRecentTransactions();
            loadTransactionHistory();
        } else {
            throw new Error(result.message || 'Unknown error');
        }
        
    } catch (error) {
        console.error('Error depositing:', error);
        showToast('❌ Lỗi: ' + error.message, 'error');
    }
}

function openWithdrawVoteModal() {
    const modal = document.getElementById('withdrawVoteModal');
    if (modal) {
        modal.classList.add('show');
        modal.style.display = 'flex';
        const form = document.getElementById('withdrawVoteForm');
        if (form) form.reset();
        
        // Reset số dư về 0 khi mở modal
        const availableBalanceEl = document.getElementById('availableBalance');
        if (availableBalanceEl) {
            availableBalanceEl.textContent = formatFundCurrency(0);
        }
        
        // Reload groups để đảm bảo chỉ hiển thị nhóm user đã tham gia
        loadFundGroups();
    }
}

function closeWithdrawVoteModal() {
    const modal = document.getElementById('withdrawVoteModal');
    if (modal) {
        modal.classList.remove('show');
        modal.style.display = 'none';
    }
}

async function loadAvailableBalance() {
    try {
        // Gọi API với userId để chỉ lấy số dư của các nhóm mà user tham gia
        const response = await authenticatedRequest(`${API.FUND}/stats?userId=${CURRENT_USER_ID}`);
        if (!response.ok) throw new Error('Failed to load balance');
        
        const stats = await response.json();
        const availableBalanceEl = document.getElementById('availableBalance');
        if (availableBalanceEl) {
            availableBalanceEl.textContent = formatFundCurrency(stats.totalBalance);
        }
    } catch (error) {
        console.error('Error loading balance:', error);
    }
}

/**
 * Load số dư của một nhóm cụ thể khi chọn nhóm trong dropdown rút tiền
 */
async function loadFundBalanceByGroupId(groupId) {
    try {
        const response = await authenticatedRequest(`${API.FUND}/group/${groupId}`);
        
        // 404 means group doesn't have a fund yet, which is normal
        if (response.status === 404) {
            const availableBalanceEl = document.getElementById('availableBalance');
            if (availableBalanceEl) {
                availableBalanceEl.textContent = formatFundCurrency(0);
            }
            return;
        }
        
        if (!response.ok) throw new Error('Failed to load fund balance');
        
        const fund = await response.json();
        const availableBalanceEl = document.getElementById('availableBalance');
        if (availableBalanceEl) {
            const currentBalance = fund.currentBalance || 0;
            availableBalanceEl.textContent = formatFundCurrency(currentBalance);
        }
    } catch (error) {
        console.error('Error loading fund balance for groupId:', groupId, error);
        const availableBalanceEl = document.getElementById('availableBalance');
        if (availableBalanceEl) {
            availableBalanceEl.textContent = formatFundCurrency(0);
        }
    }
}

async function handleWithdrawVote(e) {
    e.preventDefault();
    
    const formData = new FormData(e.target);
    const groupId = parseInt(formData.get('groupId'));
    
    // Get fundId from selected option
    const selectedOption = e.target.querySelector(`option[value="${groupId}"]`);
    const fundId = selectedOption ? selectedOption.getAttribute('data-fund-id') : null;
    
    if (!fundId) {
        showToast('Nhóm này chưa có quỹ', 'error');
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
        const response = await authenticatedRequest(`${API.FUND}/withdraw/request`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        
        if (!response.ok) throw new Error('Failed to create withdrawal request');
        
        let result = {};
        try {
            const bodyText = await response.text();
            if (bodyText && bodyText.trim().length > 0) {
                result = JSON.parse(bodyText);
            }
        } catch (parseError) {
            console.warn('removeMember: response body is not JSON (likely empty):', parseError);
        }
        
        if (result.success) {
            showToast('🗳️ Phiếu bỏ phiếu đã được tạo! Các thành viên sẽ bỏ phiếu trong 3 ngày.', 'success');
            closeWithdrawVoteModal();
            
            // Reload data
            loadFundStats();
            loadMyPendingRequests();
            loadTransactionHistory();
        } else {
            throw new Error(result.message || 'Unknown error');
        }
        
    } catch (error) {
        console.error('Error creating withdrawal request:', error);
        showToast('❌ Lỗi: ' + error.message, 'error');
    }
}

function viewAllTransactions() {
    // Scroll to transaction table
    const table = document.getElementById('transactionsTableBody');
    if (table) {
        table.scrollIntoView({ behavior: 'smooth' });
    }
}

function viewTransactionDetail(transactionId) {
    showToast(`Xem chi tiết giao dịch #${transactionId}`, 'info');
}

async function cancelRequest(transactionId) {
    if (!confirm('Bạn có chắc muốn hủy yêu cầu này?')) return;
    
    try {
        // Đảm bảo userId luôn có giá trị
        const userId = CURRENT_USER_ID || 1;
        const url = `${API.FUND}/transactions/${transactionId}?userId=${userId}`;
        
        console.log('🗑️ Cancelling transaction:', { transactionId, userId, url });
        
        const response = await authenticatedRequest(url, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            const errorText = await response.text().catch(() => '');
            console.error('❌ Delete failed:', { status: response.status, errorData, errorText });
            throw new Error(errorData.error || errorData.message || errorText || 'Failed to cancel request');
        }
        
        const result = await safeParseJsonBody(response.clone());
        console.log('✅ Cancel success:', result);
        showToast('✅ Đã hủy yêu cầu', 'success');
        
        // Reload data
        loadFundStats();
        loadMyPendingRequests();
        loadTransactionHistory();
        
    } catch (error) {
        console.error('Error canceling request:', error);
        showToast('❌ Lỗi: ' + error.message, 'error');
    }
}

// Fund utility functions
function getFundStatusClass(status) {
    const map = {
        'Pending': 'warning',
        'Approved': 'info',
        'Rejected': 'danger',
        'Completed': 'success'
    };
    return map[status] || 'secondary';
}

function getFundStatusText(status) {
    const map = {
        'Pending': 'Chờ duyệt',
        'Approved': 'Đã duyệt',
        'Rejected': 'Từ chối',
        'Completed': 'Hoàn tất'
    };
    return map[status] || status;
}

function getFundStatusIcon(status) {
    const map = {
        'Pending': '⏳',
        'Approved': '✅',
        'Rejected': '❌',
        'Completed': '✔️'
    };
    return map[status] || '';
}

function formatFundCurrency(amount) {
    if (!amount) return '0 VNĐ';
    return new Intl.NumberFormat('vi-VN').format(amount) + ' VNĐ';
}

function formatFundDate(dateString) {
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

// ============ BROWSE GROUPS PAGE ============
let allGroups = [];
let myGroupIds = [];

// Toggle browse groups section
let browseGroupsLoaded = false;
async function toggleBrowseGroups() {
    const section = document.getElementById('browse-groups-section');
    const toggleBtn = document.getElementById('toggle-browse-groups');
    
    if (!section || !toggleBtn) return;
    
    const isVisible = section.style.display !== 'none';
    
    if (isVisible) {
        // Hide section
        section.style.display = 'none';
        toggleBtn.innerHTML = '<i class="fas fa-chevron-down"></i> Hiển thị';
    } else {
        // Show section
        section.style.display = 'block';
        toggleBtn.innerHTML = '<i class="fas fa-chevron-up"></i> Ẩn';
        
        // Load groups if not loaded yet
        if (!browseGroupsLoaded) {
            try {
                await loadBrowseGroupsData();
                browseGroupsLoaded = true;
            } catch (error) {
                console.error('Error loading browse groups:', error);
                showToast('Lỗi khi tải danh sách nhóm', 'error');
            }
        }
    }
}

async function loadBrowseGroupsData() {
    try {
        // Load all groups and user's groups
        await Promise.all([
            loadAllGroups(),
            loadUserGroups()
        ]);
        
        // Initialize search and filter
        initBrowseGroupsFilters();
        
        // Render groups
        renderBrowseGroups();
        
    } catch (error) {
        console.error('Error loading browse groups data:', error);
        throw error;
    }
}

async function loadAllGroups() {
    try {
        const response = await authenticatedRequest(API.GROUPS);
        if (!response.ok) throw new Error('Failed to load groups');
        
        allGroups = await response.json();
        console.log(`📦 Loaded ${allGroups.length} groups:`, allGroups);
        
    } catch (error) {
        console.error('Error loading all groups:', error);
        allGroups = [];
    }
}

async function loadUserGroups() {
    try {
        const response = await authenticatedRequest(`${API.GROUPS}/user/${CURRENT_USER_ID}`);
        if (!response.ok) throw new Error('Failed to load user groups');
        
        const userGroups = await response.json();
        myGroupIds = userGroups.map(g => g.groupId);
        console.log(`👤 User ${CURRENT_USER_ID} is member of groups:`, myGroupIds);
        
    } catch (error) {
        console.error('Error loading user groups:', error);
        myGroupIds = [];
    }
}

function initBrowseGroupsFilters() {
    const searchInput = document.getElementById('group-search');
    const statusFilter = document.getElementById('group-status-filter');
    
    if (searchInput) {
        searchInput.addEventListener('input', renderBrowseGroups);
    }
    
    if (statusFilter) {
        statusFilter.addEventListener('change', renderBrowseGroups);
    }
}

function renderBrowseGroups() {
    const container = document.getElementById('browse-groups-grid');
    if (!container) return;
    
    const searchTerm = document.getElementById('group-search')?.value.toLowerCase() || '';
    const statusFilter = document.getElementById('group-status-filter')?.value || 'all';
    
    // Filter groups
    let filteredGroups = allGroups.filter(group => {
        // Search filter
        const matchesSearch = !searchTerm || 
            group.groupName.toLowerCase().includes(searchTerm);
        
        // Status filter
        const matchesStatus = statusFilter === 'all' || 
            group.status === statusFilter;
        
        return matchesSearch && matchesStatus;
    });
    
    // Render groups
    if (filteredGroups.length === 0) {
        container.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1;">
                <i class="fas fa-search"></i>
                <p>Không tìm thấy nhóm nào</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = filteredGroups.map(group => {
        const isMember = myGroupIds.includes(group.groupId);
        const statusBadge = group.status === 'Active' 
            ? '<span class="badge badge-success">Đang hoạt động</span>'
            : '<span class="badge badge-warning">Tạm ngưng</span>';
        
        return `
            <div class="group-card">
                <div class="group-card-header">
                    <h3>${escapeHtml(group.groupName)}</h3>
                    ${statusBadge}
                </div>
                <div class="group-card-body">
                    <div class="group-info-item">
                        <i class="fas fa-user-shield"></i>
                        <span>Quản lý bởi: User #${group.adminId}</span>
                    </div>
                    <div class="group-info-item">
                        <i class="fas fa-users"></i>
                        <span>${group.memberCount || 0} thành viên</span>
                    </div>
                    <div class="group-info-item">
                        <i class="fas fa-car"></i>
                        <span>Xe #${group.vehicleId || 'N/A'}</span>
                    </div>
                    <div class="group-info-item">
                        <i class="fas fa-vote-yea"></i>
                        <span>${group.voteCount || 0} phiếu bỏ phiếu</span>
                    </div>
                    ${group.createdAt ? `
                    <div class="group-info-item">
                        <i class="fas fa-calendar"></i>
                        <span>Thành lập: ${formatDate(group.createdAt)}</span>
                    </div>
                    ` : ''}
                </div>
                <div class="group-card-footer">
                    ${isMember ? `
                        <button class="btn btn-success" disabled>
                            <i class="fas fa-check"></i> Đã tham gia
                        </button>
                    ` : `
                        <button class="btn btn-primary join-group-btn" data-group-id="${group.groupId}">
                            <i class="fas fa-user-plus"></i> Tham gia nhóm
                        </button>
                    `}
                </div>
            </div>
        `;
    }).join('');
}

async function openJoinGroupModal(groupId) {
    console.log('openJoinGroupModal called with groupId:', groupId, 'type:', typeof groupId);
    console.log('allGroups:', allGroups);
    
    // Convert to number if needed
    const id = typeof groupId === 'string' ? parseInt(groupId) : groupId;
    
    let group = allGroups.find(g => g.groupId === id || g.groupId === groupId);
    
    if (!group) {
        console.log('Group not found in cache. Fetching from API...');
        try {
            const response = await authenticatedRequest(`${API.GROUPS}/${id}`);
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || 'Không thể tải thông tin nhóm');
            }
            const fetchedGroup = await response.json();
            group = normalizeGroupData(fetchedGroup);
            
            if (group) {
                // Update cache to avoid refetching
                const existingIndex = allGroups.findIndex(g => g.groupId === group.groupId);
                if (existingIndex >= 0) {
                    allGroups[existingIndex] = group;
                } else {
                    allGroups.push(group);
                }
            }
        } catch (fetchError) {
            console.error('Failed to fetch group info:', fetchError);
            showToast('Không thể tải thông tin nhóm. Vui lòng thử lại.', 'error');
            return;
        }
    }
    
    if (!group) {
        console.error('Group not found. ID:', id, 'Available groups:', allGroups.map(g => g.groupId));
        showToast('Không tìm thấy thông tin nhóm', 'error');
        return;
    }
    
    console.log('Found group:', group);
    
    // Set hidden fields
    const groupIdInput = document.getElementById('join-group-id');
    const userIdInput = document.getElementById('join-user-id');
    
    if (!groupIdInput || !userIdInput) {
        console.error('Modal inputs not found');
        showToast('Lỗi: Không tìm thấy form', 'error');
        return;
    }
    
    groupIdInput.value = id;
    userIdInput.value = CURRENT_USER_ID;
    
    // Set group info
    const infoDiv = document.getElementById('join-group-info');
    if (infoDiv) {
        infoDiv.innerHTML = `
            <div><strong>Tên nhóm:</strong> ${escapeHtml(group.groupName)}</div>
            <div><strong>Thành viên hiện tại:</strong> ${group.memberCount || 0}</div>
            <div><strong>Trạng thái:</strong> ${group.status === 'Active' ? 'Đang hoạt động' : 'Tạm ngưng'}</div>
        `;
    }
    
    // Reset form
    const ownershipInput = document.getElementById('joinOwnershipPercent');
    if (ownershipInput) {
        ownershipInput.value = '';
    }
    
    // Show modal
    const modal = document.getElementById('joinGroupModal');
    if (modal) {
        modal.classList.add('active');
        console.log('Modal opened');
        
        // Ensure submit button handler is bound
        const submitBtn = document.getElementById('joinGroupSubmitBtn');
        if (submitBtn) {
            console.log('🔵 Binding submit button handler');
            
            // Remove all existing click listeners by replacing the button
            // Create a temporary marker to identify old listeners
            const oldBtn = submitBtn;
            const newBtn = oldBtn.cloneNode(true);
            
            // Replace the button
            oldBtn.parentNode.replaceChild(newBtn, oldBtn);
            
            // Get the new button reference
            const button = document.getElementById('joinGroupSubmitBtn');
            
            // Ensure button is enabled
            button.disabled = false;
            button.style.pointerEvents = 'auto';
            button.style.cursor = 'pointer';
            
            // Bind click handler with detailed logging
            // Handle both button click and icon click
            const clickHandler = function(e) {
                e.preventDefault();
                e.stopPropagation();
                console.log('🔵 Submit button clicked!');
                console.log('🔵 Event details:', {
                    type: e.type,
                    target: e.target,
                    currentTarget: e.currentTarget,
                    buttonId: e.currentTarget.id,
                    clickedElement: e.target.tagName,
                    clickedElementClass: e.target.className
                });
                
                // If clicked on icon, find the button parent
                let targetButton = e.target;
                if (targetButton.tagName === 'I' || targetButton.tagName === 'SPAN') {
                    targetButton = targetButton.closest('button');
                }
                
                if (!targetButton || targetButton.id !== 'joinGroupSubmitBtn') {
                    console.warn('⚠️ Click not on button, ignoring');
                    return;
                }
                
                console.log('🔵 Calling handleJoinGroup...');
                
                // Try to trigger form submit as primary method
                const form = document.getElementById('joinGroupForm');
                if (form) {
                    console.log('🔵 Triggering form submit...');
                    // Create and dispatch submit event
                    const submitEvent = new Event('submit', { bubbles: true, cancelable: true });
                    form.dispatchEvent(submitEvent);
                } else {
                    // Fallback to direct handler call
                    console.log('🔵 Form not found, calling handleJoinGroup directly...');
                    handleJoinGroup(e);
                }
            };
            
            // Set onclick attribute directly as primary method (most reliable)
            button.onclick = function(e) {
                console.log('🔵 onclick attribute triggered!');
                clickHandler(e);
            };
            
            // Add click listener to button as backup
            button.addEventListener('click', clickHandler, { once: false, capture: false });
            
            // Also add mousedown/pointerdown as backup
            button.addEventListener('mousedown', function(e) {
                console.log('🔵 Button mousedown event');
            });
            
            // Add click listener to icon if exists
            const icon = button.querySelector('i');
            if (icon) {
                console.log('🔵 Found icon, adding click handler to icon too');
                icon.addEventListener('click', function(e) {
                    console.log('🔵 Icon clicked!');
                    clickHandler(e);
                }, { once: false, capture: false });
                icon.style.pointerEvents = 'auto';
                icon.style.cursor = 'pointer';
            }
            
            // Also try to trigger via form submit as backup
            const form = document.getElementById('joinGroupForm');
            if (form) {
                console.log('🔵 Also binding form submit handler');
                const formSubmitHandler = function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    console.log('🔵 Form submit triggered');
                    handleJoinGroup(e);
                };
                form.addEventListener('submit', formSubmitHandler, { once: false });
            }
            
            console.log('✅ Submit button handler bound successfully');
            console.log('✅ Button state:', {
                id: button.id,
                disabled: button.disabled,
                type: button.type,
                hasOnclick: !!button.onclick
            });
            
            // Debug: Check button visibility and clickability
            setTimeout(() => {
                const btn = document.getElementById('joinGroupSubmitBtn');
                if (btn) {
                    const styles = window.getComputedStyle(btn);
                    const rect = btn.getBoundingClientRect();
                    console.log('🔍 Button debug info:', {
                        display: styles.display,
                        visibility: styles.visibility,
                        pointerEvents: styles.pointerEvents,
                        opacity: styles.opacity,
                        zIndex: styles.zIndex,
                        position: styles.position,
                        top: rect.top,
                        left: rect.left,
                        width: rect.width,
                        height: rect.height,
                        visible: rect.width > 0 && rect.height > 0
                    });
                    
                    // Check if button is covered by another element
                    const elementAtPoint = document.elementFromPoint(
                        rect.left + rect.width / 2,
                        rect.top + rect.height / 2
                    );
                    console.log('🔍 Element at button center:', {
                        tagName: elementAtPoint?.tagName,
                        id: elementAtPoint?.id,
                        className: elementAtPoint?.className,
                        isButton: elementAtPoint === btn || btn.contains(elementAtPoint)
                    });
                }
            }, 100);
            
            // Test click programmatically after a short delay
            setTimeout(() => {
                const btn = document.getElementById('joinGroupSubmitBtn');
                if (btn) {
                    console.log('🧪 Testing programmatic click...');
                    // Don't actually trigger, just log that we can access it
                    console.log('✅ Button accessible for programmatic click');
                }
            }, 200);
        } else {
            console.warn('⚠️ Submit button not found when opening modal');
        }
    } else {
        console.error('Modal not found');
    }
}

function closeJoinGroupModal() {
    document.getElementById('joinGroupModal').classList.remove('active');
    document.getElementById('joinGroupForm').reset();
}

// Initialize join group form handler
document.addEventListener('DOMContentLoaded', function() {
    console.log('🔵 DOMContentLoaded - Initializing join group form...');
    
    const joinGroupForm = document.getElementById('joinGroupForm');
    console.log('Form found:', joinGroupForm ? 'YES' : 'NO');
    
    if (joinGroupForm) {
        // Bind form submit handler
        joinGroupForm.addEventListener('submit', function(e) {
            e.preventDefault();
            e.stopPropagation();
            console.log('🔵 Form submit event triggered');
            handleJoinGroup(e);
        });
        console.log('✅ Form submit event listener added');
        
        // Also bind directly to submit button as backup
        const submitBtn = document.getElementById('joinGroupSubmitBtn');
        if (submitBtn) {
            console.log('✅ Submit button found');
            submitBtn.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                console.log('🔵 Submit button clicked (backup handler)');
                handleJoinGroup(e);
            });
        } else {
            console.warn('⚠️ Submit button not found');
        }
    } else {
        console.error('❌ joinGroupForm not found in DOM');
    }
    
    // Event delegation for join group buttons (handles dynamically created buttons)
    document.addEventListener('click', async function(event) {
        // Skip if clicking on submit button or inside modal footer
        const submitBtn = event.target.closest('#joinGroupSubmitBtn');
        const modalFooter = event.target.closest('.modal-footer');
        if (submitBtn || (modalFooter && event.target.closest('button'))) {
            // Let the button's own handlers handle this
            return;
        }
        
        // Check if clicked element is a join group button or inside one
        const joinBtn = event.target.closest('.join-group-btn');
        if (joinBtn) {
            event.preventDefault();
            const groupId = parseInt(joinBtn.getAttribute('data-group-id'));
            if (!isNaN(groupId)) {
                console.log('Join button clicked, groupId:', groupId);
                await handleJoinButtonClick(groupId);
            }
        }
        
        // Close modal when clicking outside
        if (event.target.id === 'joinGroupModal') {
            closeJoinGroupModal();
        }
    });
});

// Flag to prevent duplicate calls
let isJoiningGroup = false;
const CONTRACTS_CACHE_TTL = 30000; // 30 seconds
let contractsOverviewCache = null;
let contractsCacheTimestamp = 0;
let groupDecisionsCacheById = new Map();

async function handleJoinGroup(e) {
    e.preventDefault();
    e.stopPropagation();
    
    // Prevent duplicate calls
    if (isJoiningGroup) {
        console.log('⚠️ handleJoinGroup already in progress, ignoring duplicate call');
        return;
    }
    
    isJoiningGroup = true;
    console.log('🔵 handleJoinGroup called');
    
    const groupIdInput = document.getElementById('join-group-id');
    const userIdInput = document.getElementById('join-user-id');
    const ownershipInput = document.getElementById('joinOwnershipPercent');
    
    console.log('Form inputs:', {
        groupIdInput: groupIdInput ? groupIdInput.value : 'NOT FOUND',
        userIdInput: userIdInput ? userIdInput.value : 'NOT FOUND',
        ownershipInput: ownershipInput ? ownershipInput.value : 'NOT FOUND'
    });
    
    const groupId = parseInt(groupIdInput?.value);
    const userId = parseInt(userIdInput?.value);
    const ownershipPercent = parseFloat(ownershipInput?.value);
    
    console.log('Parsed values:', { groupId, userId, ownershipPercent });
    
    // Validation
    if (!groupId || isNaN(groupId) || groupId <= 0) {
        console.error('❌ Validation failed: Invalid groupId', { groupId });
        showToast('Lỗi: Không tìm thấy thông tin nhóm', 'error');
        isJoiningGroup = false;
        return;
    }
    
    if (!userId || isNaN(userId) || userId <= 0) {
        console.error('❌ Validation failed: Invalid userId', { userId });
        showToast('Lỗi: Không tìm thấy thông tin người dùng', 'error');
        isJoiningGroup = false;
        return;
    }
    
    if (!ownershipInput || !ownershipInput.value || isNaN(ownershipPercent)) {
        console.error('❌ Validation failed: Invalid ownershipPercent', { ownershipPercent, inputValue: ownershipInput?.value });
        showToast('Vui lòng nhập tỷ lệ sở hữu (từ 0.01% đến 100%)', 'error');
        isJoiningGroup = false;
        return;
    }
    
    if (ownershipPercent <= 0 || ownershipPercent > 100) {
        console.error('❌ Ownership percent out of range:', ownershipPercent);
        showToast('Tỷ lệ sở hữu phải từ 0.01% đến 100%', 'error');
        isJoiningGroup = false;
        return;
    }
    
    try {
        // ========== KIỂM TRA HỢP ĐỒNG TRƯỚC KHI THAM GIA NHÓM ==========
        console.log('📋 Checking contract status for group:', groupId);
        const groupContract = await getGroupContractInfo(groupId);
        
        if (groupContract) {
            console.log('📋 Found contract for group:', groupContract);
            const contractStatus = (groupContract.status || groupContract.contractStatus || '').toLowerCase();
            
            if (contractStatus !== 'signed' && contractStatus !== 'finished') {
                console.log('⚠️ Contract not signed yet. Status:', contractStatus);
                closeJoinGroupModal();
                const contractCode = groupContract.contractCode || `Hợp đồng #${groupContract.contractId}`;
                showContractRequirementModal(buildSignContractModalConfig(contractCode));
                isJoiningGroup = false;
                return;
            } else {
                console.log('✅ Contract is signed. Status:', contractStatus);
                await ensureContractSignatureSynced(groupContract.contractId, userId);
            }
        } else {
            console.log('⚠️ No contract found for this group.');
            closeJoinGroupModal();
            showContractRequirementModal(buildNoContractModalConfig());
            isJoiningGroup = false;
            return;
        }
        
        console.log('📡 Checking current group members...');
        // Check current ownership total
        const membersUrl = `${API.GROUPS}/${groupId}/members`;
        console.log('Fetching:', membersUrl);
        
        const membersResponse = await authenticatedRequest(membersUrl);
        console.log('Members response status:', membersResponse.status, membersResponse.ok);
        
        if (!membersResponse.ok) {
            const errorText = await membersResponse.text();
            console.error('❌ Failed to load group members:', errorText);
            throw new Error('Failed to load group members');
        }
        
        const currentMembers = await membersResponse.json();
        console.log('Current members:', currentMembers);
        
        // Check if user is already a member
        const existingMember = currentMembers.find(m => m.userId === userId);
        if (existingMember) {
            console.log('⚠️ User is already a member:', existingMember);
            console.log('⚠️ Existing ownership:', existingMember.ownershipPercent);
            console.log('⚠️ New ownership request:', ownershipPercent);
            
            // If user is already a member with same ownership, just show success
            if (existingMember.ownershipPercent === ownershipPercent) {
                console.log('✅ User already has same ownership, treating as success');
                showToast('Bạn đã là thành viên của nhóm này với tỷ lệ sở hữu này rồi', 'info');
                closeJoinGroupModal();
                // Reload groups
                await loadMyGroups();
                if (browseGroupsLoaded) {
                    await loadBrowseGroupsData();
                }
                isJoiningGroup = false;
                return;
            }
            
            // If user wants to update ownership, allow it (backend will handle)
            console.log('⚠️ User wants to update ownership, proceeding...');
        }
        
        // Calculate total ownership excluding current user (if already a member)
        const currentTotal = currentMembers
            .filter(m => m.userId !== userId) // Exclude current user's existing ownership
            .reduce((sum, m) => sum + (m.ownershipPercent || 0), 0);
        console.log('Current total ownership (excluding current user):', currentTotal);
        console.log('Requested ownership:', ownershipPercent);
        console.log('Total would be:', currentTotal + ownershipPercent);
        
        if (currentTotal + ownershipPercent > 100) {
            console.error('❌ Total ownership exceeds 100%');
            showToast(`Tổng tỷ lệ sở hữu không được vượt quá 100%. Hiện tại: ${currentTotal.toFixed(2)}%`, 'error');
            isJoiningGroup = false; // Reset flag
            return;
        }
        
        // Join group
        const joinData = {
            userId: userId,
            role: 'Member',
            ownershipPercent: ownershipPercent,
            currentUserId: CURRENT_USER_ID
        };
        
        console.log('📤 Sending join request:', joinData);
        const joinUrl = `${API.GROUPS}/${groupId}/members`;
        console.log('POST URL:', joinUrl);
        
        const response = await authenticatedRequest(joinUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(joinData)
        });
        
        console.log('Join response status:', response.status, response.ok);
        
        if (!response.ok) {
            const errorText = await response.text();
            console.error('❌ Join failed. Status:', response.status);
            console.error('❌ Response body:', errorText);
            
            let errorMessage = 'Không thể tham gia nhóm';
            
            // Try to parse error response as JSON
            try {
                const errorData = JSON.parse(errorText);
                // Check multiple possible error message fields
                errorMessage = errorData.message || errorData.error || errorData.details || errorMessage;
                console.error('Parsed error data:', errorData);
            } catch (e) {
                // If not JSON, use text directly
                if (errorText && errorText.trim().length > 0) {
                    errorMessage = errorText;
                }
            }
            
            // Map specific status codes to user-friendly messages
            if (response.status === 404) {
                errorMessage = errorMessage || 'Không tìm thấy nhóm';
            } else if (response.status === 400) {
                if (!errorMessage || errorMessage === 'Không thể tham gia nhóm') {
                    errorMessage = 'Bạn đã là thành viên của nhóm này rồi hoặc dữ liệu không hợp lệ';
                }
            } else if (response.status === 500) {
                errorMessage = errorMessage || 'Lỗi server, vui lòng thử lại sau';
            }
            
            throw new Error(errorMessage);
        }
        
        const result = await response.json();
        console.log('✅ Successfully joined group:', result);
        
        showToast('Tham gia nhóm thành công!', 'success');
        closeJoinGroupModal();
        
        // Reload groups
        await loadMyGroups();
        if (browseGroupsLoaded) {
            await loadBrowseGroupsData();
        }
        
    } catch (error) {
        console.error('❌ Error joining group:', error);
        console.error('Error stack:', error.stack);
        showToast(error.message || 'Có lỗi xảy ra khi tham gia nhóm', 'error');
    } finally {
        // Reset flag after completion
        isJoiningGroup = false;
    }
}

async function handleJoinButtonClick(groupId) {
    if (!groupId || isNaN(groupId)) {
        return;
    }
    
    try {
        const groupContract = await getGroupContractInfo(groupId, true);
        
        if (!groupContract) {
            console.log('⚠️ No contract found for group when opening modal');
            showContractRequirementModal(buildNoContractModalConfig());
            return;
        }
        
        const contractStatus = (groupContract.status || groupContract.contractStatus || '').toLowerCase();
        if (contractStatus !== 'signed' && contractStatus !== 'finished') {
            const contractCode = groupContract.contractCode || `Hợp đồng #${groupContract.contractId}`;
            showContractRequirementModal(buildSignContractModalConfig(contractCode));
            return;
        }
        
        openJoinGroupModal(groupId);
    } catch (error) {
        console.warn('⚠️ Error validating contract before opening join modal:', error);
        openJoinGroupModal(groupId);
    }
}

async function fetchContractsOverview(force = false) {
    const now = Date.now();
    if (!force && contractsOverviewCache && (now - contractsCacheTimestamp) < CONTRACTS_CACHE_TTL) {
        return contractsOverviewCache;
    }
    
    try {
        const response = await authenticatedRequest('/user/contracts/api');
        if (!response.ok) {
            console.warn('⚠️ Unable to fetch contracts overview. Status:', response.status);
            return null;
        }
        const data = await response.json();
        contractsOverviewCache = data;
        contractsCacheTimestamp = now;
        return data;
    } catch (error) {
        console.warn('⚠️ Error fetching contracts overview:', error);
        return null;
    }
}

function invalidateContractsCache() {
    contractsOverviewCache = null;
    contractsCacheTimestamp = 0;
}

async function getGroupContractInfo(groupId, force = false) {
    const overview = await fetchContractsOverview(force);
    if (!overview || !Array.isArray(overview.contracts)) {
        return null;
    }
    return overview.contracts.find(c => c.groupId === groupId);
}

async function ensureContractSignatureSynced(contractId, userId) {
    if (!contractId || !userId) {
        return;
    }
    
    try {
        const response = await authenticatedRequest(`${API.GROUPS}/contracts/${contractId}/sign`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                userId: userId,
                signatureMethod: 'ui-sync'
            })
        });
        
        if (!response.ok) {
            const errorText = await response.text();
            console.warn('⚠️ Không thể đồng bộ chữ ký với GroupManagementService:', errorText);
        } else {
            console.log('✅ Signature synced with GroupManagementService');
            invalidateContractsCache();
        }
    } catch (error) {
        console.warn('⚠️ Lỗi khi đồng bộ chữ ký với GroupManagementService:', error);
    }
}

function buildSignContractModalConfig(contractCode) {
    const safeCode = escapeHtml(contractCode || 'Hợp đồng chưa xác định');
    return {
        title: 'Yêu cầu ký hợp đồng',
        body: `
            <div style="text-align: center; padding: 12px 8px;">
                <i class="fas fa-file-contract" style="font-size: 48px; color: #ff9800; margin-bottom: 15px;"></i>
                <h3 style="margin-bottom: 15px;">Cần ký hợp đồng trước</h3>
                <p style="margin-bottom: 20px; color: #666;">
                    Bạn cần ký hợp đồng <strong>${safeCode}</strong> trước khi tham gia nhóm này.
                </p>
                <div style="display: flex; gap: 10px; flex-wrap: wrap; justify-content: center;">
                    <button class="btn btn-primary" onclick="closeContractRequiredModal(); window.location.href='/user/contracts';" style="min-width: 160px;">
                        <i class="fas fa-file-signature"></i> Đi đến trang Hợp đồng
                    </button>
                    <button class="btn btn-secondary" onclick="closeContractRequiredModal();" style="min-width: 140px;">
                        Hủy
                    </button>
                </div>
            </div>
        `
    };
}

function buildNoContractModalConfig() {
    return {
        title: 'Yêu cầu hợp đồng',
        body: `
            <div style="text-align: center; padding: 12px 8px;">
                <i class="fas fa-file-contract" style="font-size: 48px; color: #ff9800; margin-bottom: 15px;"></i>
                <h3 style="margin-bottom: 15px;">Nhóm này chưa có hợp đồng</h3>
                <p style="margin-bottom: 20px; color: #666;">
                    Vui lòng liên hệ quản trị viên để tạo hợp đồng nhóm trước khi tham gia.
                </p>
                <button class="btn btn-secondary" onclick="closeContractRequiredModal();" style="min-width: 140px;">
                    Đóng
                </button>
            </div>
        `
    };
}

function showContractRequirementModal(config) {
    if (!config) return;
    
    closeContractRequiredModal();
    
    const overlay = document.createElement('div');
    overlay.id = 'contractRequiredModal';
    overlay.className = 'contract-modal-overlay';
    overlay.innerHTML = `
        <div class="modal-content contract-modal-content">
            <div class="modal-header">
                <h2><i class="fas fa-exclamation-triangle" style="margin-right: 8px;"></i> ${config.title || 'Thông báo'}</h2>
                <button class="modal-close" onclick="closeContractRequiredModal()">&times;</button>
            </div>
            <div class="modal-body">
                ${config.body || ''}
            </div>
        </div>
    `;
    
    overlay.addEventListener('click', function(e) {
        if (e.target === overlay) {
            closeContractRequiredModal();
        }
    });
    
    document.body.appendChild(overlay);
}

function closeContractRequiredModal() {
    document.getElementById('contractRequiredModal')?.remove();
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

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ============ GROUP MANAGEMENT FUNCTIONS ============

let currentManagingGroupId = null;

function openManageGroupModal(groupId, groupName) {
    // Đảm bảo groupId là number
    const numGroupId = Number(groupId);
    if (isNaN(numGroupId)) {
        console.error('Invalid groupId:', groupId);
        showToast('Lỗi: ID nhóm không hợp lệ', 'error');
        return;
    }
    
    currentManagingGroupId = numGroupId;
    document.getElementById('manage-group-name').textContent = groupName;
    document.getElementById('manageGroupModal').classList.add('active');
    loadGroupMembers(numGroupId);
    
    // Load and update pending leave requests count
    updatePendingLeaveRequestsBadge(numGroupId);
    
    // Start auto-refresh for leave requests (check every 10 seconds)
    startLeaveRequestAutoRefresh(numGroupId);
    
    // Reset add member form
    document.getElementById('addMemberForm').reset();
}

function closeManageGroupModal() {
    document.getElementById('manageGroupModal').classList.remove('active');
    currentManagingGroupId = null;
    
    // Stop auto-refresh when modal is closed
    stopLeaveRequestAutoRefresh();
}

function normalizeGroupData(rawGroup) {
    if (!rawGroup) return null;
    
    const resolvedId = rawGroup.groupId ?? rawGroup.id ?? rawGroup.group_id;
    const numericId = typeof resolvedId === 'string' ? parseInt(resolvedId) : resolvedId;
    
    return {
        ...rawGroup,
        groupId: numericId,
        groupName: rawGroup.groupName || rawGroup.name || rawGroup.group_name || `Nhóm #${numericId ?? ''}`,
        status: rawGroup.status || rawGroup.groupStatus || rawGroup.group_status || 'Active',
        memberCount: rawGroup.memberCount ?? rawGroup.totalMembers ?? rawGroup.total_members ?? 0
    };
}

function normalizeDecisionData(rawDecision) {
    if (!rawDecision) return null;
    const groupInfo = rawDecision.group || {};
    const voteId = rawDecision.voteId ?? rawDecision.id ?? rawDecision.vote_id;
    const groupId = rawDecision.groupId ?? groupInfo.groupId ?? groupInfo.id ?? groupInfo.group_id;
    const topic = rawDecision.topic || rawDecision.title || rawDecision.subject;
    const optionA = rawDecision.optionA ?? rawDecision.optiona ?? rawDecision.option_a ?? 'Đồng ý';
    const optionB = rawDecision.optionB ?? rawDecision.optionb ?? rawDecision.option_b ?? 'Từ chối';
    const createdAt = rawDecision.createdAt ?? rawDecision.creationDate ?? rawDecision.created_at;
    const finalResult = rawDecision.finalResult ?? rawDecision.result ?? null;
    const totalVotes = rawDecision.totalVotes ?? rawDecision.total_votes ?? 0;
    
    return {
        ...rawDecision,
        voteId: typeof voteId === 'string' ? parseInt(voteId) : voteId,
        groupId: typeof groupId === 'string' ? parseInt(groupId) : groupId,
        groupName: rawDecision.groupName || groupInfo.groupName || `Nhóm #${groupId ?? '-'}`,
        topic,
        optionA,
        optionB,
        createdAt,
        finalResult,
        totalVotes
    };
}

function openLeaveRequestsModalFromManage() {
    if (currentManagingGroupId) {
        // Lưu groupId và groupName trước khi đóng modal (vì closeManageGroupModal sẽ set currentManagingGroupId = null)
        const groupId = currentManagingGroupId;
        const groupName = document.getElementById('manage-group-name').textContent;
        closeManageGroupModal();
        // Đảm bảo groupId là number, không phải string
        openLeaveRequestsModal(Number(groupId), groupName);
    } else {
        console.error('No group is currently being managed');
        showToast('Không tìm thấy thông tin nhóm', 'error');
    }
}

async function loadGroupMembers(groupId) {
    const container = document.getElementById('members-list-container');
    container.innerHTML = '<div class="loading-spinner"><i class="fas fa-spinner fa-spin"></i> Đang tải...</div>';
    
    try {
        const response = await authenticatedRequest(`${API.GROUPS}/${groupId}/members`);
        if (!response.ok) throw new Error('Failed to load members');
        
        const members = await response.json();
        
        if (members.length === 0) {
            container.innerHTML = '<p style="text-align: center; color: var(--text-light);">Chưa có thành viên nào</p>';
            return;
        }
        
        // Calculate total ownership
        const totalOwnership = members.reduce((sum, m) => sum + (m.ownershipPercent || 0), 0);
        
        container.innerHTML = `
            <div class="members-summary">
                <span><strong>Tổng thành viên:</strong> ${members.length}</span>
                <span><strong>Tổng tỷ lệ sở hữu:</strong> ${totalOwnership.toFixed(2)}%</span>
            </div>
            <div class="members-table">
                <table>
                    <thead>
                        <tr>
                            <th>User ID</th>
                            <th>Quyền</th>
                            <th>Tỷ lệ sở hữu</th>
                            <th>Ngày tham gia</th>
                            <th>Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${members.map(member => `
                            <tr>
                                <td>User #${member.userId}</td>
                                <td>
                                    <span class="badge ${member.role === 'Admin' ? 'badge-admin' : 'badge-member'}">
                                        ${member.role === 'Admin' ? '<i class="fas fa-crown"></i> Admin' : '<i class="fas fa-user"></i> Thành viên'}
                                    </span>
                                </td>
                                <td>${(member.ownershipPercent || 0).toFixed(2)}%</td>
                                <td>${member.joinedAt ? formatDate(member.joinedAt) : 'N/A'}</td>
                                <td>
                                    <div class="member-actions">
                                        ${member.userId !== CURRENT_USER_ID ? `
                                            <button class="btn btn-sm btn-danger" onclick="removeMember(${groupId}, ${member.memberId}, ${member.userId})" title="Xóa thành viên">
                                                <i class="fas fa-trash"></i> Xóa
                                            </button>
                                        ` : `
                                            <span class="text-muted">Bạn</span>
                                        `}
                                    </div>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
        
        // Update pending leave requests badge after loading members
        updatePendingLeaveRequestsBadge(groupId);
        
    } catch (error) {
        console.error('Error loading group members:', error);
        container.innerHTML = '<div class="alert alert-danger"><i class="fas fa-exclamation-circle"></i> Lỗi khi tải danh sách thành viên</div>';
    }
}

/**
 * Cập nhật badge hiển thị số lượng yêu cầu rời nhóm đang chờ
 */
async function updatePendingLeaveRequestsBadge(groupId) {
    try {
        const response = await authenticatedRequest(`${API.GROUPS}/${groupId}/leave-requests?currentUserId=${CURRENT_USER_ID}`);
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            console.warn('Failed to load leave requests for badge:', response.status, errorData);
            return;
        }
        
        const data = await response.json();
        console.log('Leave requests data:', data); // Debug log
        
        // Xử lý cả 2 format: Map (mới) và List (cũ - để tương thích)
        let pendingCount = 0;
        if (data.pending !== undefined) {
            // Format mới: { requests: [], total: X, pending: Y }
            pendingCount = data.pending || 0;
        } else if (Array.isArray(data)) {
            // Format cũ: List - đếm số pending
            pendingCount = data.filter(r => r.status === 'Pending').length;
        }
        
        // Tìm nút "Yêu cầu rời nhóm" và cập nhật badge
        const leaveRequestBtn = document.querySelector('button[onclick="openLeaveRequestsModalFromManage()"]');
        if (leaveRequestBtn) {
            // Tìm hoặc tạo badge
            let badge = leaveRequestBtn.querySelector('.leave-request-badge');
            if (!badge) {
                badge = document.createElement('span');
                badge.className = 'badge badge-warning leave-request-badge';
                badge.style.marginLeft = '8px';
                badge.style.display = 'inline-block';
                leaveRequestBtn.appendChild(badge);
            }
            
            if (pendingCount > 0) {
                badge.textContent = pendingCount;
                badge.style.display = 'inline-block';
            } else {
                badge.style.display = 'none';
            }
        } else {
            console.warn('Leave request button not found');
        }
        
        // Kiểm tra xem có yêu cầu mới không và hiển thị thông báo
        const lastCount = lastPendingLeaveRequestCount[groupId] || 0;
        if (pendingCount > lastCount && lastCount > 0) {
            // Có yêu cầu mới
            const newCount = pendingCount - lastCount;
            showToast(`🔔 Có ${newCount} yêu cầu rời nhóm mới cần bạn xử lý!`, 'info');
        }
        lastPendingLeaveRequestCount[groupId] = pendingCount;
        
    } catch (error) {
        console.error('Error updating pending leave requests badge:', error);
    }
}

/**
 * Bắt đầu auto-refresh để kiểm tra yêu cầu rời nhóm mới
 */
function startLeaveRequestAutoRefresh(groupId) {
    // Dừng interval cũ nếu có
    stopLeaveRequestAutoRefresh();
    
    // Kiểm tra mỗi 10 giây
    leaveRequestAutoRefreshInterval = setInterval(() => {
        if (currentManagingGroupId === groupId) {
            updatePendingLeaveRequestsBadge(groupId);
        } else {
            stopLeaveRequestAutoRefresh();
        }
    }, 10000); // 10 giây
}

/**
 * Dừng auto-refresh cho yêu cầu rời nhóm
 */
function stopLeaveRequestAutoRefresh() {
    if (leaveRequestAutoRefreshInterval) {
        clearInterval(leaveRequestAutoRefreshInterval);
        leaveRequestAutoRefreshInterval = null;
    }
}

// Initialize add member form handler
document.addEventListener('DOMContentLoaded', function() {
    const addMemberForm = document.getElementById('addMemberForm');
    if (addMemberForm) {
        addMemberForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            await addMember();
        });
    }
    
    // Close modal when clicking outside
    const manageGroupModal = document.getElementById('manageGroupModal');
    if (manageGroupModal) {
        manageGroupModal.addEventListener('click', function(e) {
            if (e.target === manageGroupModal) {
                closeManageGroupModal();
            }
        });
    }
});

async function addMember() {
    if (!currentManagingGroupId) return;
    
    const userId = parseInt(document.getElementById('newMemberUserId').value);
    const ownershipPercent = parseFloat(document.getElementById('newMemberOwnership').value);
    const role = document.getElementById('newMemberRole').value;
    
    if (!userId || isNaN(ownershipPercent)) {
        showToast('Vui lòng điền đầy đủ thông tin', 'error');
        return;
    }
    
    try {
        const response = await authenticatedRequest(`${API.GROUPS}/${currentManagingGroupId}/members`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                currentUserId: CURRENT_USER_ID, // Thêm currentUserId để kiểm tra quyền Admin
                userId: userId,
                ownershipPercent: ownershipPercent,
                role: role
            })
        });
        
        const result = await response.json();
        
        if (!response.ok) {
            throw new Error(result.message || result.error || 'Failed to add member');
        }
        
        showToast(`Đã thêm User #${userId} vào nhóm thành công`, 'success');
        document.getElementById('addMemberForm').reset();
        await loadGroupMembers(currentManagingGroupId);
        
        // Reload groups list to update member count
        await loadMyGroups();
        
    } catch (error) {
        console.error('Error adding member:', error);
        showToast(error.message || 'Lỗi khi thêm thành viên', 'error');
    }
}

async function removeMember(groupId, memberId, userId) {
    if (!confirm(`Bạn có chắc chắn muốn xóa User #${userId} khỏi nhóm này?`)) {
        return;
    }
    
    try {
        // Thêm currentUserId vào query parameter để kiểm tra quyền Admin
        const response = await authenticatedRequest(
            `${API.GROUPS}/${groupId}/members/${memberId}?currentUserId=${CURRENT_USER_ID}`,
            { method: 'DELETE' }
        );
        
        const result = await safeParseJsonBody(response.clone());
        
        if (!response.ok) {
            throw new Error(result.message || result.error || `Failed to remove member (HTTP ${response.status})`);
        }
        
        showToast(`Đã xóa User #${userId} khỏi nhóm`, 'success');
        await loadGroupMembers(groupId);
        
        // Reload groups list to update member count
        await loadMyGroups();
        
    } catch (error) {
        console.error('Error removing member:', error);
        showToast(error.message || 'Lỗi khi xóa thành viên', 'error');
    }
}

async function changeMemberRole(groupId, memberId, newRole) {
    const roleText = newRole === 'Admin' ? 'thăng làm Admin' : 'hạ xuống thành viên';
    if (!confirm(`Bạn có chắc chắn muốn ${roleText}?`)) {
        return;
    }
    
    try {
        // First, get current member data
        const membersResponse = await authenticatedRequest(`${API.GROUPS}/${groupId}/members`);
        if (!membersResponse.ok) throw new Error('Failed to fetch members');
        
        const members = await membersResponse.json();
        const member = members.find(m => m.memberId === memberId);
        
        if (!member) {
            throw new Error('Member not found');
        }
        
        // Update member with new role
        const response = await authenticatedRequest(`${API.GROUPS}/${groupId}/members/${memberId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                currentUserId: CURRENT_USER_ID, // Thêm currentUserId để kiểm tra quyền Admin
                userId: member.userId,
                role: newRole,
                ownershipPercent: member.ownershipPercent
            })
        });
        
        const result = await response.json();
        
        if (!response.ok) {
            throw new Error(result.message || result.error || 'Failed to update member role');
        }
        
        showToast(`Đã ${newRole === 'Admin' ? 'thăng' : 'hạ'} quyền thành công`, 'success');
        await loadGroupMembers(groupId);
        
        // Update user role cache if it's current user
        if (member.userId === CURRENT_USER_ID) {
            userGroupRoles[groupId] = newRole;
        }
        
        // Reload groups list to update UI
        await loadMyGroups();
        
    } catch (error) {
        console.error('Error changing member role:', error);
        showToast('Lỗi khi thay đổi quyền', 'error');
    }
}

// ============ CREATE GROUP FUNCTIONS ============

function openCreateGroupModal() {
    // Reset form
    document.getElementById('createGroupForm').reset();
    document.getElementById('createGroupStatus').value = 'Active';
    
    // Show modal
    document.getElementById('createGroupModal').classList.add('active');
}

function closeCreateGroupModal() {
    document.getElementById('createGroupModal').classList.remove('active');
    document.getElementById('createGroupForm').reset();
}

// Initialize create group form submit handler
document.addEventListener('DOMContentLoaded', function() {
    const createGroupSubmitBtn = document.getElementById('createGroupSubmitBtn');
    const createGroupForm = document.getElementById('createGroupForm');
    
    if (createGroupSubmitBtn) {
        createGroupSubmitBtn.addEventListener('click', async function() {
            // Validate form
            if (!createGroupForm.checkValidity()) {
                createGroupForm.reportValidity();
                return;
            }
            
            const groupName = document.getElementById('createGroupName').value.trim();
            const vehicleId = document.getElementById('createGroupVehicleId').value;
            const ownershipPercent = document.getElementById('createGroupOwnershipPercent').value;
            const status = document.getElementById('createGroupStatus').value;
            
            if (!groupName) {
                showToast('Vui lòng nhập tên nhóm', 'error');
                return;
            }
            
            // Validate ownershipPercent nếu có nhập
            if (ownershipPercent) {
                const ownershipValue = parseFloat(ownershipPercent);
                if (isNaN(ownershipValue) || ownershipValue < 0 || ownershipValue > 100) {
                    showToast('Tỷ lệ sở hữu phải là số từ 0 đến 100', 'error');
                    return;
                }
            }
            
            try {
                // Tự động set adminId = CURRENT_USER_ID (người tạo nhóm)
                const groupData = {
                    groupName: groupName,
                    adminId: CURRENT_USER_ID, // ⭐ QUAN TRỌNG: User tạo nhóm tự động trở thành Admin
                    vehicleId: vehicleId ? parseInt(vehicleId) : null,
                    ownershipPercent: ownershipPercent ? parseFloat(ownershipPercent) : null, // Tỷ lệ sở hữu của Admin
                    status: status
                };
                
                const response = await authenticatedRequest(API.GROUPS, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(groupData)
                });
                
                const result = await response.json();
                
                if (!response.ok) {
                    throw new Error(result.message || result.error || 'Failed to create group');
                }
                
                showToast(`Đã tạo nhóm "${groupName}" thành công! Bạn đã trở thành Admin của nhóm này.`, 'success');
                closeCreateGroupModal();
                
                // Reload groups list để hiển thị nhóm mới
                await loadMyGroups();
                
            } catch (error) {
                console.error('Error creating group:', error);
                showToast(error.message || 'Lỗi khi tạo nhóm', 'error');
            }
        });
    }
    
    // Close modal when clicking outside
    const createGroupModal = document.getElementById('createGroupModal');
    if (createGroupModal) {
        createGroupModal.addEventListener('click', function(e) {
            if (e.target === createGroupModal) {
                closeCreateGroupModal();
            }
        });
    }
    
    // Initialize Create Decision Modal
    const createDecisionForm = document.getElementById('createDecisionForm');
    if (createDecisionForm) {
        createDecisionForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const groupId = document.getElementById('decisionGroup').value;
            const decisionType = document.getElementById('decisionType').value;
            const topic = document.getElementById('decisionTopic').value;
            const optionA = document.getElementById('decisionOptionA').value;
            const optionB = document.getElementById('decisionOptionB').value;
            
            if (!groupId || !decisionType || !topic || !optionA || !optionB) {
                showToast('Vui lòng điền đầy đủ thông tin', 'error');
                return;
            }
            
            try {
                const voteData = {
                    topic: topic,
                    optionA: optionA,
                    optionB: optionB
                };
                
                const response = await authenticatedRequest(`${API.GROUPS}/${groupId}/votes`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(voteData)
                });
                
                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || errorData.error || 'Failed to create decision');
                }
                
                const result = await response.json();
                showToast('✅ Đã tạo quyết định chung thành công!', 'success');
                closeCreateDecisionModal();
                await loadGroupDecisions();
                
            } catch (error) {
                console.error('Error creating decision:', error);
                showToast('❌ Lỗi: ' + error.message, 'error');
            }
        });
    }
    
    // Load groups for decision modal
    const decisionGroupSelect = document.getElementById('decisionGroup');
    if (decisionGroupSelect) {
        // Load groups when modal opens
        window.openCreateDecisionModal = function() {
            const modal = document.getElementById('createDecisionModal');
            if (modal) {
                modal.classList.add('show');
                modal.style.display = 'flex';
                const form = document.getElementById('createDecisionForm');
                if (form) form.reset();
                loadFundGroupsForDecision();
            }
        };
        
        window.closeCreateDecisionModal = function() {
            const modal = document.getElementById('createDecisionModal');
            if (modal) {
                modal.classList.remove('show');
                modal.style.display = 'none';
            }
        };
    }
    
    // Close decision modal when clicking outside
    const createDecisionModal = document.getElementById('createDecisionModal');
    if (createDecisionModal) {
        createDecisionModal.addEventListener('click', function(e) {
            if (e.target === createDecisionModal) {
                closeCreateDecisionModal();
            }
        });
    }
});

// Load fund categories (Quỹ Bảo Dưỡng, Phí Dự Phòng)
// Load group decisions (Quyết định chung)
async function loadGroupDecisions() {
    try {
        // Get user's groups
        const groupsResponse = await authenticatedRequest(`/api/groups/user/${CURRENT_USER_ID}`);
        if (!groupsResponse.ok) {
            console.error('Failed to load user groups');
            return;
        }
        
        const groups = await groupsResponse.json();
        const allDecisions = [];
        
        // Load votes from all groups
        for (const group of groups) {
            try {
                const votesResponse = await authenticatedRequest(`${API.GROUPS}/${group.groupId}/votes`);
                if (votesResponse.ok) {
                    const votes = await votesResponse.json();
                    votes.forEach(vote => {
                        const normalized = normalizeDecisionData({
                            ...vote,
                            groupId: group.groupId,
                            groupName: group.groupName || `Nhóm ${group.groupId}`
                        });
                        if (normalized) {
                            allDecisions.push(normalized);
                        }
                    });
                }
            } catch (e) {
                console.warn(`Error loading votes for group ${group.groupId}:`, e);
            }
        }
        
        groupDecisionsCacheById = new Map();
        allDecisions.forEach(decision => {
            if (decision.voteId != null) {
                groupDecisionsCacheById.set(decision.voteId, decision);
            }
        });
        
        // Categorize decisions by type
        const categorizeDecision = (topic) => {
            if (!topic) return 'other';
            const topicLower = topic.toLowerCase();
            if (topicLower.includes('pin') || topicLower.includes('battery')) return 'battery_upgrade';
            if (topicLower.includes('bảo hiểm') || topicLower.includes('insurance')) return 'insurance';
            if (topicLower.includes('bán') || topicLower.includes('sell')) return 'sell_vehicle';
            if (topicLower.includes('bảo dưỡng') || topicLower.includes('maintenance')) return 'maintenance';
            return 'other';
        };
        
        const getDecisionTypeLabel = (type) => {
            const labels = {
                'battery_upgrade': '🔋 Nâng cấp pin',
                'insurance': '🛡️ Bảo hiểm',
                'sell_vehicle': '🚗 Bán xe',
                'maintenance': '🔧 Bảo dưỡng lớn',
                'upgrade': '⚡ Nâng cấp khác',
                'other': '📋 Khác'
            };
            return labels[type] || '📋 Khác';
        };
        
        // Separate decisions
        const pendingDecisions = allDecisions.filter(d => !d.finalResult);
        const myDecisions = allDecisions.filter(d => {
            // Assuming creator info might be in the vote or we need to check differently
            // For now, show all decisions
            return true;
        });
        
        // Update badges
        const pendingBadge = document.getElementById('pendingDecisionsBadge');
        if (pendingBadge) pendingBadge.textContent = pendingDecisions.length;
        
        const decisionsTabBadge = document.getElementById('decisionsTabBadge');
        if (decisionsTabBadge) {
            decisionsTabBadge.textContent = pendingDecisions.length;
            decisionsTabBadge.style.display = pendingDecisions.length > 0 ? 'inline-flex' : 'none';
        }
        
        // Update tables
        updateAllDecisionsTable(allDecisions, categorizeDecision, getDecisionTypeLabel);
        updatePendingDecisionsTable(pendingDecisions, categorizeDecision, getDecisionTypeLabel);
        updateMyDecisionsTable(myDecisions, categorizeDecision, getDecisionTypeLabel);
        
    } catch (error) {
        console.error('Error loading group decisions:', error);
    }
}

function updateAllDecisionsTable(decisions, categorizeDecision, getDecisionTypeLabel) {
    const tbody = document.getElementById('allDecisionsBody');
    if (!tbody) return;
    
    if (decisions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="empty-table">
                    <div class="empty-state">
                        <i class="fas fa-vote-yea"></i>
                        <p>Chưa có quyết định nào</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = decisions.map(d => {
        const type = categorizeDecision(d.topic);
        const typeLabel = getDecisionTypeLabel(type);
        const status = d.finalResult ? 'completed' : 'pending';
        const statusBadge = status === 'completed' 
            ? '<span class="badge badge-success">✅ Hoàn tất</span>'
            : '<span class="badge badge-warning">⏳ Đang chờ</span>';
        
        return `
        <tr>
            <td>${typeLabel}</td>
            <td><strong>${d.topic || '-'}</strong></td>
            <td>User #${d.groupId || 'Unknown'}</td>
            <td>${formatFundDate(d.createdAt)}</td>
            <td>${statusBadge}</td>
            <td>${d.finalResult || '<span class="text-muted">Chưa có kết quả</span>'}</td>
            <td>
                <button class="btn btn-sm btn-outline" onclick="viewDecisionDetail(${d.groupId}, ${d.voteId})">
                    <i class="fas fa-eye"></i> Xem
                </button>
                ${!d.finalResult ? `
                    <button class="btn btn-sm btn-success" onclick="voteOnDecision(${d.voteId}, 'A')">
                        <i class="fas fa-check"></i> Đồng ý
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="voteOnDecision(${d.voteId}, 'B')">
                        <i class="fas fa-times"></i> Từ chối
                    </button>
                ` : ''}
            </td>
        </tr>
    `;
    }).join('');
}

function updatePendingDecisionsTable(decisions, categorizeDecision, getDecisionTypeLabel) {
    const tbody = document.getElementById('pendingDecisionsBody');
    if (!tbody) return;
    
    if (decisions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="empty-table">
                    <div class="empty-state">
                        <i class="fas fa-check-circle"></i>
                        <p>Không có quyết định nào đang chờ vote</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = decisions.map(d => {
        const type = categorizeDecision(d.topic);
        const typeLabel = getDecisionTypeLabel(type);
        const progress = `${d.totalVotes || 0} phiếu`;
        
        return `
        <tr>
            <td>${typeLabel}</td>
            <td><strong>${d.topic || '-'}</strong></td>
            <td>User #${d.groupId || 'Unknown'}</td>
            <td>${formatFundDate(d.createdAt)}</td>
            <td>${progress}</td>
            <td>
                <button class="btn btn-sm btn-success" onclick="voteOnDecision(${d.voteId}, 'A')">
                    <i class="fas fa-check"></i> Đồng ý
                </button>
                <button class="btn btn-sm btn-danger" onclick="voteOnDecision(${d.voteId}, 'B')">
                    <i class="fas fa-times"></i> Từ chối
                </button>
            </td>
        </tr>
    `;
    }).join('');
}

function updateMyDecisionsTable(decisions, categorizeDecision, getDecisionTypeLabel) {
    const tbody = document.getElementById('myDecisionsBody');
    if (!tbody) return;
    
    if (decisions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="empty-table">
                    <div class="empty-state">
                        <i class="fas fa-user"></i>
                        <p>Bạn chưa tạo quyết định nào</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = decisions.map(d => {
        const type = categorizeDecision(d.topic);
        const typeLabel = getDecisionTypeLabel(type);
        const status = d.finalResult ? 'completed' : 'pending';
        const statusBadge = status === 'completed' 
            ? '<span class="badge badge-success">✅ Hoàn tất</span>'
            : '<span class="badge badge-warning">⏳ Đang chờ</span>';
        
        return `
        <tr>
            <td>${typeLabel}</td>
            <td><strong>${d.topic || '-'}</strong></td>
            <td>${formatFundDate(d.createdAt)}</td>
            <td>${statusBadge}</td>
            <td>${d.finalResult || '<span class="text-muted">Chưa có kết quả</span>'}</td>
            <td>
                <button class="btn btn-sm btn-outline" onclick="viewDecisionDetail(${d.groupId}, ${d.voteId})">
                    <i class="fas fa-eye"></i> Xem
                </button>
            </td>
        </tr>
    `;
    }).join('');
}

// Switch decision tab
function switchDecisionTab(tabName) {
    // Update tab buttons
    document.querySelectorAll('.decision-tabs .tab-btn').forEach(btn => {
        btn.classList.remove('active');
        if (btn.getAttribute('data-tab') === tabName) {
            btn.classList.add('active');
        }
    });
    
    // Update tab content
    document.querySelectorAll('.decision-tab-content').forEach(content => {
        content.classList.remove('active');
        if (content.id === `${tabName}-tab`) {
            content.classList.add('active');
        }
    });
}

// Load groups for decision modal
async function loadFundGroupsForDecision() {
    try {
        const response = await authenticatedRequest(`/api/groups/user/${CURRENT_USER_ID}`);
        if (!response.ok) throw new Error('Failed to load groups');
        
        const groups = await response.json();
        const select = document.getElementById('decisionGroup');
        if (select) {
            select.innerHTML = '<option value="">Chọn nhóm</option>' +
                groups.map(g => `<option value="${g.groupId}">${g.groupName}</option>`).join('');
        }
    } catch (error) {
        console.error('Error loading groups for decision:', error);
        const select = document.getElementById('decisionGroup');
        if (select) select.innerHTML = '<option value="">Không thể tải nhóm</option>';
    }
}

// Vote on decision
async function voteOnDecision(voteId, choice) {
    if (!confirm(`Bạn có chắc chắn muốn chọn "${choice === 'A' ? 'Đồng ý' : 'Từ chối'}" không?`)) {
        return;
    }
    
    try {
        const response = await authenticatedRequest(`${API.GROUPS}/votes/${voteId}/results`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                userId: CURRENT_USER_ID,
                choice: choice
            })
        });
        
        const responseData = await response.json();
        
        if (!response.ok) {
            const errorMsg = responseData.error || responseData.message || 'Failed to vote';
            throw new Error(errorMsg);
        }
        
        showToast(`✅ Bạn đã bỏ phiếu "${choice === 'A' ? 'Đồng ý' : 'Từ chối'}" thành công!`, 'success');
        
        // Reload decisions to show updated status
        await loadGroupDecisions();
        
    } catch (error) {
        console.error('Error voting on decision:', error);
        showToast('❌ Lỗi: ' + error.message, 'error');
    }
}

// View decision detail
async function viewDecisionDetail(groupId, voteId) {
    try {
        const numericVoteId = Number(voteId);
        if (Number.isNaN(numericVoteId)) {
            showToast('Không xác định được quyết định cần xem', 'error');
            return;
        }
        
        let decision = groupDecisionsCacheById.get(numericVoteId);
        
        if (!decision) {
            const resolvedGroupId = Number(groupId) || undefined;
            if (!resolvedGroupId) {
                showToast('Không xác định được nhóm của quyết định này', 'error');
                return;
            }
            const response = await authenticatedRequest(`${API.GROUPS}/${resolvedGroupId}/votes`);
            if (!response.ok) {
                throw new Error('Không thể tải thông tin quyết định');
            }
            const votes = await response.json();
            votes
                .map(v => normalizeDecisionData({ ...v, groupId: resolvedGroupId }))
                .filter(Boolean)
                .forEach(v => groupDecisionsCacheById.set(v.voteId, v));
            decision = groupDecisionsCacheById.get(numericVoteId);
        }
        
        if (!decision) {
            showToast('Không tìm thấy thông tin quyết định', 'error');
            return;
        }
        
        showDecisionDetailModal(decision);
    } catch (error) {
        console.error('Error loading decision detail:', error);
        showToast(error.message || 'Không thể tải chi tiết quyết định', 'error');
    }
}

function showDecisionDetailModal(decision) {
    closeDecisionDetailModal();
    
    const totalVotes = decision.totalVotes || 0;
    const finalResult = decision.finalResult || 'Chưa có kết quả';
    const createdAt = formatFundDate(decision.createdAt);
    const optionA = decision.optionA || 'Đồng ý';
    const optionB = decision.optionB || 'Từ chối';
    
    const overlay = document.createElement('div');
    overlay.id = 'decisionDetailModal';
    overlay.className = 'contract-modal-overlay';
    overlay.innerHTML = `
        <div class="modal-content contract-modal-content">
            <div class="modal-header">
                <h2><i class="fas fa-hand-paper"></i> Chi tiết quyết định</h2>
                <button class="modal-close" onclick="closeDecisionDetailModal()">&times;</button>
            </div>
            <div class="modal-body decision-detail-body">
                <div class="decision-detail-row">
                    <span class="detail-label">Nhóm:</span>
                    <span class="detail-value">${escapeHtml(decision.groupName || `Nhóm #${decision.groupId}`)}</span>
                </div>
                <div class="decision-detail-row">
                    <span class="detail-label">Chủ đề:</span>
                    <span class="detail-value"><strong>${escapeHtml(decision.topic || '-')}</strong></span>
                </div>
                <div class="decision-detail-row">
                    <span class="detail-label">Ngày tạo:</span>
                    <span class="detail-value">${createdAt}</span>
                </div>
                <div class="decision-detail-row">
                    <span class="detail-label">Kết quả:</span>
                    <span class="detail-value">${finalResult}</span>
                </div>
                <div class="decision-options">
                    <div class="option-card">
                        <h4><i class="fas fa-check text-success"></i> Phương án A</h4>
                        <p>${escapeHtml(optionA)}</p>
                    </div>
                    <div class="option-card">
                        <h4><i class="fas fa-times text-danger"></i> Phương án B</h4>
                        <p>${escapeHtml(optionB)}</p>
                    </div>
                </div>
                <div class="decision-summary">
                    <div><strong>Tổng phiếu:</strong> ${totalVotes}</div>
                    <div><strong>Mã vote:</strong> #${decision.voteId}</div>
                </div>
                <div class="decision-actions">
                    ${!decision.finalResult ? `
                        <button class="btn btn-success" onclick="closeDecisionDetailModal(); voteOnDecision(${decision.voteId}, 'A')">
                            <i class="fas fa-check"></i> Đồng ý
                        </button>
                        <button class="btn btn-danger" onclick="closeDecisionDetailModal(); voteOnDecision(${decision.voteId}, 'B')">
                            <i class="fas fa-times"></i> Từ chối
                        </button>
                    ` : ''}
                    <button class="btn btn-secondary" onclick="closeDecisionDetailModal()">Đóng</button>
                </div>
            </div>
        </div>
    `;
    
    overlay.addEventListener('click', (event) => {
        if (event.target === overlay) {
            closeDecisionDetailModal();
        }
    });
    
    document.body.appendChild(overlay);
}

function closeDecisionDetailModal() {
    document.getElementById('decisionDetailModal')?.remove();
}

// ========================================
// VIEW GROUP MODAL FUNCTIONS
// ========================================

let currentViewingGroupId = null;

async function openViewGroupModal(groupId, groupName) {
    currentViewingGroupId = groupId;
    document.getElementById('view-group-name').textContent = groupName;
    document.getElementById('viewGroupModal').classList.add('active');
    
    // Load membership info
    await loadMyMembershipInfo(groupId);
    
    // Load members list
    await loadViewGroupMembers(groupId);
    
    // Load leave request status
    await loadMyLeaveRequestStatus(groupId);
    
    // Setup form handler
    const form = document.getElementById('leaveGroupForm');
    form.onsubmit = async (e) => {
        e.preventDefault();
        await submitLeaveRequest(groupId);
    };
}

function closeViewGroupModal() {
    document.getElementById('viewGroupModal').classList.remove('active');
    currentViewingGroupId = null;
    document.getElementById('leaveGroupForm').reset();
}

async function loadMyMembershipInfo(groupId) {
    const container = document.getElementById('my-membership-info');
    container.innerHTML = '<div class="loading-spinner"><i class="fas fa-spinner fa-spin"></i> Đang tải...</div>';
    
    try {
        const response = await authenticatedRequest(`${API.GROUPS}/${groupId}/members/me/${CURRENT_USER_ID}`);
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to load membership info');
        }
        
        const data = await response.json();
        
        container.innerHTML = `
            <div class="membership-details">
                <div class="detail-row">
                    <span class="detail-label"><i class="fas fa-id-card"></i> Member ID:</span>
                    <span class="detail-value">#${data.memberId}</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label"><i class="fas fa-user-shield"></i> Quyền:</span>
                    <span class="detail-value">
                        <span class="badge ${data.role === 'Admin' ? 'badge-admin' : 'badge-member'}">
                            ${data.role === 'Admin' ? '<i class="fas fa-crown"></i> Admin' : '<i class="fas fa-user"></i> Thành viên'}
                        </span>
                    </span>
                </div>
                <div class="detail-row">
                    <span class="detail-label"><i class="fas fa-percent"></i> Tỷ lệ sở hữu:</span>
                    <span class="detail-value"><strong>${(data.ownershipPercent || 0).toFixed(2)}%</strong></span>
                </div>
                <div class="detail-row">
                    <span class="detail-label"><i class="fas fa-calendar"></i> Ngày tham gia:</span>
                    <span class="detail-value">${data.joinedAt ? formatDate(data.joinedAt) : 'N/A'}</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label"><i class="fas fa-users"></i> Tổng thành viên:</span>
                    <span class="detail-value">${data.totalMembers || 0}</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label"><i class="fas fa-chart-pie"></i> Tổng tỷ lệ sở hữu:</span>
                    <span class="detail-value">${(data.totalOwnership || 0).toFixed(2)}%</span>
                </div>
            </div>
        `;
    } catch (error) {
        console.error('Error loading membership info:', error);
        container.innerHTML = `<div class="error-message">❌ ${error.message}</div>`;
    }
}

async function loadViewGroupMembers(groupId) {
    const container = document.getElementById('view-members-list-container');
    container.innerHTML = '<div class="loading-spinner"><i class="fas fa-spinner fa-spin"></i> Đang tải...</div>';
    
    try {
        const response = await authenticatedRequest(`${API.GROUPS}/${groupId}/members/view`);
        if (!response.ok) throw new Error('Failed to load members');
        
        const data = await response.json();
        const members = data.members || [];
        
        if (members.length === 0) {
            container.innerHTML = '<p style="text-align: center; color: var(--text-light);">Chưa có thành viên nào</p>';
            return;
        }
        
        container.innerHTML = `
            <div class="members-summary">
                <span><strong>Tổng thành viên:</strong> ${data.totalMembers || 0}</span>
                <span><strong>Tổng tỷ lệ sở hữu:</strong> ${(data.totalOwnership || 0).toFixed(2)}%</span>
            </div>
            <div class="members-table">
                <table>
                    <thead>
                        <tr>
                            <th>User ID</th>
                            <th>Quyền</th>
                            <th>Tỷ lệ sở hữu</th>
                            <th>Ngày tham gia</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${members.map(member => `
                            <tr ${member.userId === CURRENT_USER_ID ? 'class="current-user-row"' : ''}>
                                <td>User #${member.userId} ${member.userId === CURRENT_USER_ID ? '<span class="badge badge-info">Bạn</span>' : ''}</td>
                                <td>
                                    <span class="badge ${member.role === 'Admin' ? 'badge-admin' : 'badge-member'}">
                                        ${member.role === 'Admin' ? '<i class="fas fa-crown"></i> Admin' : '<i class="fas fa-user"></i> Thành viên'}
                                    </span>
                                </td>
                                <td>${(member.ownershipPercent || 0).toFixed(2)}%</td>
                                <td>${member.joinedAt ? formatDate(member.joinedAt) : 'N/A'}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (error) {
        console.error('Error loading members:', error);
        container.innerHTML = `<div class="error-message">❌ ${error.message}</div>`;
    }
}

async function loadMyLeaveRequestStatus(groupId) {
    const container = document.getElementById('leave-request-status');
    
    try {
        const response = await authenticatedRequest(`${API.GROUPS}/${groupId}/leave-requests/me/${CURRENT_USER_ID}`);
        if (!response.ok) throw new Error('Failed to load leave request status');
        
        const data = await response.json();
        
        if (!data.hasRequest) {
            container.innerHTML = '<p style="color: var(--text-light);">Bạn chưa có yêu cầu rời nhóm nào</p>';
            document.getElementById('submitLeaveRequestBtn').disabled = false;
            return;
        }
        
        const status = data.status;
        let statusHtml = '';
        
        if (status === 'Pending') {
            statusHtml = `
                <div class="alert alert-warning">
                    <i class="fas fa-clock"></i> 
                    <strong>Yêu cầu đang chờ phê duyệt</strong>
                    <p>Yêu cầu của bạn đã được gửi vào ${data.requestedAt ? formatDate(data.requestedAt) : 'N/A'}. 
                    Vui lòng chờ Admin phê duyệt.</p>
                    ${data.reason ? `<p><strong>Lý do:</strong> ${escapeHtml(data.reason)}</p>` : ''}
                </div>
            `;
            document.getElementById('submitLeaveRequestBtn').disabled = true;
        } else if (status === 'Approved') {
            statusHtml = `
                <div class="alert alert-success">
                    <i class="fas fa-check-circle"></i> 
                    <strong>Yêu cầu đã được phê duyệt</strong>
                    <p>Yêu cầu của bạn đã được Admin phê duyệt vào ${data.processedAt ? formatDate(data.processedAt) : 'N/A'}.</p>
                    ${data.adminNote ? `<p><strong>Ghi chú từ Admin:</strong> ${escapeHtml(data.adminNote)}</p>` : ''}
                </div>
            `;
            document.getElementById('submitLeaveRequestBtn').disabled = true;
        } else if (status === 'Rejected') {
            statusHtml = `
                <div class="alert alert-danger">
                    <i class="fas fa-times-circle"></i> 
                    <strong>Yêu cầu đã bị từ chối</strong>
                    <p>Yêu cầu của bạn đã bị Admin từ chối vào ${data.processedAt ? formatDate(data.processedAt) : 'N/A'}.</p>
                    ${data.adminNote ? `<p><strong>Lý do từ chối:</strong> ${escapeHtml(data.adminNote)}</p>` : ''}
                </div>
            `;
            document.getElementById('submitLeaveRequestBtn').disabled = false;
        }
        
        container.innerHTML = statusHtml;
    } catch (error) {
        console.error('Error loading leave request status:', error);
        container.innerHTML = '';
        document.getElementById('submitLeaveRequestBtn').disabled = false;
    }
}

async function submitLeaveRequest(groupId) {
    const reason = document.getElementById('leaveReason').value.trim();
    const btn = document.getElementById('submitLeaveRequestBtn');
    
    if (!confirm('Bạn có chắc chắn muốn gửi yêu cầu rời nhóm không?')) {
        return;
    }
    
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang gửi...';
    
    try {
        const response = await authenticatedRequest(`${API.GROUPS}/${groupId}/leave-request`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                userId: CURRENT_USER_ID,
                reason: reason || null
            })
        });
        
        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.message || data.error || 'Failed to submit leave request');
        }
        
        showToast('✅ ' + data.message, 'success');
        
        // Reload status
        await loadMyLeaveRequestStatus(groupId);
        
        // Reset form
        document.getElementById('leaveGroupForm').reset();
        
    } catch (error) {
        console.error('Error submitting leave request:', error);
        showToast('❌ ' + error.message, 'error');
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-sign-out-alt"></i> Gửi yêu cầu rời nhóm';
    }
}

// ========================================
// LEAVE REQUESTS MODAL (ADMIN) FUNCTIONS
// ========================================

let currentLeaveRequestsGroupId = null;

async function openLeaveRequestsModal(groupId, groupName) {
    // Đảm bảo groupId là number
    const numGroupId = Number(groupId);
    if (isNaN(numGroupId)) {
        console.error('Invalid groupId:', groupId);
        showToast('Lỗi: ID nhóm không hợp lệ', 'error');
        return;
    }
    
    currentLeaveRequestsGroupId = numGroupId;
    document.getElementById('leave-requests-group-name').textContent = groupName;
    document.getElementById('leaveRequestsModal').classList.add('active');
    
    await loadLeaveRequests(numGroupId);
}

function closeLeaveRequestsModal() {
    document.getElementById('leaveRequestsModal').classList.remove('active');
    currentLeaveRequestsGroupId = null;
}

async function loadLeaveRequests(groupId) {
    // Validate groupId
    const numGroupId = Number(groupId);
    if (isNaN(numGroupId) || numGroupId <= 0) {
        console.error('Invalid groupId in loadLeaveRequests:', groupId);
        const container = document.getElementById('leave-requests-container');
        container.innerHTML = '<p style="text-align: center; color: var(--text-danger);">Lỗi: ID nhóm không hợp lệ</p>';
        return;
    }
    
    const container = document.getElementById('leave-requests-container');
    container.innerHTML = '<div class="loading-spinner"><i class="fas fa-spinner fa-spin"></i> Đang tải...</div>';
    
    try {
        const response = await authenticatedRequest(`${API.GROUPS}/${numGroupId}/leave-requests?currentUserId=${CURRENT_USER_ID}`);
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || error.error || 'Failed to load leave requests');
        }
        
        const data = await response.json();
        const requests = data.requests || [];
        
        if (requests.length === 0) {
            container.innerHTML = '<p style="text-align: center; color: var(--text-light);">Chưa có yêu cầu rời nhóm nào</p>';
            // Cập nhật badge về 0
            updatePendingLeaveRequestsBadge(numGroupId);
            return;
        }
        
        container.innerHTML = `
            <div class="requests-summary">
                <span><strong>Tổng yêu cầu:</strong> ${data.total || 0}</span>
                <span><strong>Đang chờ:</strong> ${data.pending || 0}</span>
            </div>
            <div class="leave-requests-list">
                ${requests.map(req => `
                    <div class="leave-request-card ${req.status === 'Pending' ? 'pending' : req.status === 'Approved' ? 'approved' : 'rejected'}">
                        <div class="request-header">
                            <div>
                                <strong>User #${req.userId}</strong>
                                <span class="badge ${req.role === 'Admin' ? 'badge-admin' : 'badge-member'}">
                                    ${req.role === 'Admin' ? '<i class="fas fa-crown"></i> Admin' : '<i class="fas fa-user"></i> Member'}
                                </span>
                                <span class="badge ${req.status === 'Pending' ? 'badge-warning' : req.status === 'Approved' ? 'badge-success' : 'badge-danger'}">
                                    ${req.status === 'Pending' ? 'Đang chờ' : req.status === 'Approved' ? 'Đã duyệt' : 'Đã từ chối'}
                                </span>
                            </div>
                            <div class="request-meta">
                                <small>Tỷ lệ sở hữu: ${(req.ownershipPercent || 0).toFixed(2)}%</small>
                            </div>
                        </div>
                        ${req.reason ? `<div class="request-reason"><strong>Lý do:</strong> ${escapeHtml(req.reason)}</div>` : ''}
                        <div class="request-dates">
                            <small><i class="fas fa-calendar"></i> Yêu cầu: ${req.requestedAt ? formatDate(req.requestedAt) : 'N/A'}</small>
                            ${req.processedAt ? `<small><i class="fas fa-check"></i> Xử lý: ${formatDate(req.processedAt)}</small>` : ''}
                        </div>
                        ${req.adminNote ? `<div class="admin-note"><strong>Ghi chú Admin:</strong> ${escapeHtml(req.adminNote)}</div>` : ''}
                        ${req.status === 'Pending' ? `
                            <div class="request-actions">
                                <button class="btn btn-success btn-sm" onclick="approveLeaveRequest(${numGroupId}, ${req.requestId})">
                                    <i class="fas fa-check"></i> Phê duyệt
                                </button>
                                <button class="btn btn-danger btn-sm" onclick="rejectLeaveRequest(${numGroupId}, ${req.requestId})">
                                    <i class="fas fa-times"></i> Từ chối
                                </button>
                            </div>
                        ` : ''}
                    </div>
                `).join('')}
            </div>
        `;
        
        // Cập nhật badge sau khi load
        updatePendingLeaveRequestsBadge(numGroupId);
    } catch (error) {
        console.error('Error loading leave requests:', error);
        container.innerHTML = `<div class="error-message">❌ ${error.message}</div>`;
    }
}

async function approveLeaveRequest(groupId, requestId) {
    // Validate groupId
    const numGroupId = Number(groupId);
    if (isNaN(numGroupId) || numGroupId <= 0) {
        console.error('Invalid groupId in approveLeaveRequest:', groupId);
        showToast('❌ Lỗi: ID nhóm không hợp lệ', 'error');
        return;
    }
    
    // Validate requestId
    const numRequestId = Number(requestId);
    if (isNaN(numRequestId) || numRequestId <= 0) {
        console.error('Invalid requestId in approveLeaveRequest:', requestId);
        showToast('❌ Lỗi: ID yêu cầu không hợp lệ', 'error');
        return;
    }
    
    // Validate currentUserId
    if (!CURRENT_USER_ID || isNaN(CURRENT_USER_ID)) {
        console.error('Invalid CURRENT_USER_ID:', CURRENT_USER_ID);
        showToast('❌ Lỗi: Không xác định được người dùng', 'error');
        return;
    }
    
    const note = prompt('Nhập ghi chú (tùy chọn):');
    if (note === null) {
        // User cancelled
        return;
    }
    
    try {
        console.log(`🔵 Approving leave request: groupId=${numGroupId}, requestId=${numRequestId}, currentUserId=${CURRENT_USER_ID}`);
        
        const response = await authenticatedRequest(`${API.GROUPS}/${numGroupId}/leave-requests/${numRequestId}/approve`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                currentUserId: CURRENT_USER_ID,
                adminNote: note || null
            })
        });
        
        let data;
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            data = await response.json();
        } else {
            const text = await response.text();
            console.error('Non-JSON response:', text);
            throw new Error(`Server error: ${response.status} ${response.statusText}`);
        }
        
        if (!response.ok) {
            const errorMsg = data.message || data.error || `Failed to approve leave request (${response.status})`;
            console.error('API Error:', data);
            throw new Error(errorMsg);
        }
        
        showToast('✅ ' + (data.message || 'Phê duyệt thành công'), 'success');
        
        // Reload requests
        await loadLeaveRequests(numGroupId);
        
        // Reload members list in manage group modal (if open)
        if (currentManagingGroupId === numGroupId) {
            await loadGroupMembers(numGroupId);
        }
        
        // Cập nhật badge sau khi phê duyệt
        updatePendingLeaveRequestsBadge(numGroupId);
        
        // Reload groups list
        await loadMyGroups();
        
        // Check if the removed user is viewing the group modal
        // If so, close it or refresh to show they're no longer a member
        if (currentViewingGroupId === numGroupId) {
            // Check if the removed user is the current user
            if (data.userId && data.userId === CURRENT_USER_ID) {
                // Current user was removed, close the modal
                console.log('Current user was removed from group, closing view group modal');
                closeViewGroupModal();
                showToast('⚠️ Bạn đã rời khỏi nhóm này', 'info');
            } else {
                // Another user was removed, just refresh the members list
                try {
                    await loadViewGroupMembers(numGroupId);
                    await loadMyMembershipInfo(numGroupId);
                } catch (error) {
                    console.error('Error refreshing group info after approval:', error);
                }
            }
        }
        
    } catch (error) {
        console.error('Error approving leave request:', error);
        showToast('❌ ' + (error.message || 'Có lỗi xảy ra khi phê duyệt'), 'error');
    }
}

async function rejectLeaveRequest(groupId, requestId) {
    // Validate groupId
    const numGroupId = Number(groupId);
    if (isNaN(numGroupId) || numGroupId <= 0) {
        console.error('Invalid groupId in rejectLeaveRequest:', groupId);
        showToast('❌ Lỗi: ID nhóm không hợp lệ', 'error');
        return;
    }
    
    // Validate requestId
    const numRequestId = Number(requestId);
    if (isNaN(numRequestId) || numRequestId <= 0) {
        console.error('Invalid requestId in rejectLeaveRequest:', requestId);
        showToast('❌ Lỗi: ID yêu cầu không hợp lệ', 'error');
        return;
    }
    
    // Validate currentUserId
    if (!CURRENT_USER_ID || isNaN(CURRENT_USER_ID)) {
        console.error('Invalid CURRENT_USER_ID:', CURRENT_USER_ID);
        showToast('❌ Lỗi: Không xác định được người dùng', 'error');
        return;
    }
    
    const note = prompt('Nhập lý do từ chối (tùy chọn):');
    if (note === null) {
        // User cancelled
        return;
    }
    
    try {
        console.log(`🔴 Rejecting leave request: groupId=${numGroupId}, requestId=${numRequestId}, currentUserId=${CURRENT_USER_ID}`);
        
        const response = await authenticatedRequest(`${API.GROUPS}/${numGroupId}/leave-requests/${numRequestId}/reject`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                currentUserId: CURRENT_USER_ID,
                adminNote: note || null
            })
        });
        
        let data;
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            data = await response.json();
        } else {
            const text = await response.text();
            console.error('Non-JSON response:', text);
            throw new Error(`Server error: ${response.status} ${response.statusText}`);
        }
        
        if (!response.ok) {
            const errorMsg = data.message || data.error || `Failed to reject leave request (${response.status})`;
            console.error('API Error:', data);
            throw new Error(errorMsg);
        }
        
        showToast('✅ ' + (data.message || 'Từ chối thành công'), 'success');
        
        // Reload requests
        await loadLeaveRequests(numGroupId);
        
        // Cập nhật badge sau khi từ chối
        updatePendingLeaveRequestsBadge(numGroupId);
        
    } catch (error) {
        console.error('Error rejecting leave request:', error);
        showToast('❌ ' + (error.message || 'Có lỗi xảy ra khi từ chối'), 'error');
    }
}

