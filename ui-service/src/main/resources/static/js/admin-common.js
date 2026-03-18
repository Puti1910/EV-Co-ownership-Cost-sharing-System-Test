// Admin Dashboard JavaScript

// API Endpoints
const API = {
    GROUPS: '/api/groups',           // GET all, POST create
    GROUP_DETAIL: '/api/groups',     // /api/groups/{id}
    GROUP_MEMBERS: '/api/groups',    // /api/groups/{groupId}/members
    GROUP_VOTES: '/api/groups',      // /api/groups/{groupId}/votes
    COSTS: '/api/costs',
    AUTO_SPLIT: '/api/auto-split',
    USAGE: '/api/usage-tracking',
    PAYMENTS: '/api/payments',
    FUND: '/api/fund'                // Fund management
};

// Global State
let currentSection = 'overview';
let chartsInitialized = false;
let monthlyChart = null;
let categoryChart = null;
let categoryBarChart = null;
let paymentRateChart = null;
let currentGroupId = null;  // For group management
let currentTimePeriod = 'month'; // today, week, month, quarter, year, custom
let customDateRange = null; // {from: date, to: date}

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', function() {
    console.log('Admin Dashboard initializing...');
    
    // Check which section is active by finding the visible one
    const allSections = document.querySelectorAll('.content-section');
    let activeSectionId = null;
    allSections.forEach(section => {
        const style = window.getComputedStyle(section);
        if (style.display !== 'none' && style.visibility !== 'hidden') {
            activeSectionId = section.id;
        }
    });
    
    console.log('Active section:', activeSectionId);
    
    initNavigation();
    initMenuToggle();
    
    // Only initialize functions for active section
    if (activeSectionId === 'overview-section') {
        // Only load overview data if overview section exists and admin-overview.js is not loaded
        // admin-overview.js will handle loading if it's present (it sets window.adminOverviewLoaded)
        const overviewJsLoaded = window.adminOverviewLoaded;
        console.log('admin-common.js: overviewSection exists, admin-overview.js loaded:', !!overviewJsLoaded);
        if (!overviewJsLoaded) {
            console.log('admin-common.js: Loading overview data...');
            loadOverviewData();
        }
        initCharts();
    } else if (activeSectionId === 'auto-split-section') {
        // Auto-split initialization is handled by admin-auto-split.js
        // Don't initialize here to avoid duplicate event listeners
    } else if (activeSectionId === 'group-management-section') {
        initGroupManagement();
    } else if (activeSectionId === 'payment-tracking-section') {
        initPaymentTracking();
    } else if (activeSectionId === 'cost-management-section') {
        // Check if admin-costs.js is loaded (it will handle loading costs)
        // If not loaded, use admin-common.js functions
        const costsJsLoaded = window.adminCostsLoaded;
        console.log('admin-common.js: cost-management-section exists, admin-costs.js loaded:', !!costsJsLoaded);
        if (!costsJsLoaded) {
            console.log('admin-common.js: Loading costs data...');
            loadCosts();
        }
    }
    
    console.log('Admin Dashboard initialized');
});

// ============ NAVIGATION ============
function initNavigation() {
    // Navigation is handled by server-side routing
    // Links in sidebar navigate to different routes (/admin/costs, /admin/payments, etc.)
    // No need to prevent default or handle client-side navigation
    const navLinks = document.querySelectorAll('.admin-nav .nav-link');
    
    // Just update active state based on current page
    navLinks.forEach(link => {
        // Active state is set by server-side Thymeleaf (th:classappend)
        // No additional JavaScript needed for navigation
    });
}

function initMenuToggle() {
    const menuToggle = document.getElementById('menu-toggle');
    const sidebar = document.querySelector('.admin-sidebar');
    const adminMain = document.querySelector('.admin-main');
    
    if (menuToggle && sidebar && adminMain) {
        // Create overlay for mobile
        let overlay = document.querySelector('.sidebar-overlay');
        if (!overlay) {
            overlay = document.createElement('div');
            overlay.className = 'sidebar-overlay';
            document.body.appendChild(overlay);
        }
        
        menuToggle.addEventListener('click', function() {
            sidebar.classList.toggle('collapsed');
            adminMain.classList.toggle('sidebar-collapsed');
            overlay.classList.toggle('active');
        });
        
        // Close sidebar when clicking overlay
        overlay.addEventListener('click', function() {
            sidebar.classList.add('collapsed');
            adminMain.classList.add('sidebar-collapsed');
            overlay.classList.remove('active');
        });
    }
}

function switchSection(section) {
    // Update nav
    document.querySelectorAll('.admin-nav .nav-link').forEach(link => {
        link.classList.remove('active');
    });
    document.querySelector(`[data-section="${section}"]`).classList.add('active');
    
    // Update content
    document.querySelectorAll('.content-section').forEach(sec => {
        sec.classList.remove('active');
    });
    document.getElementById(`${section}-section`).classList.add('active');
    
    // Update title
    const titles = {
        'overview': 'Tổng quan',
        'cost-management': 'Quản lý chi phí',
        'auto-split': 'Chia tự động',
        'payment-tracking': 'Theo dõi thanh toán',
        'group-management': 'Quản lý nhóm',
        'fund-management': 'Quản lý quỹ chung'
    };
    document.getElementById('page-title').textContent = titles[section];
    
    currentSection = section;
    
    // Load data for section
    switch(section) {
        case 'overview':
            loadOverviewData();
            break;
        case 'cost-management':
            loadCosts();
            break;
        case 'auto-split':
            loadGroupsForSplit();
            break;
        case 'payment-tracking':
            loadPayments();
            break;
        case 'group-management':
            loadGroups();
            break;
        case 'fund-management':
            loadFundManagementData();
            break;
    }
}

// ============ OVERVIEW SECTION ============
async function loadOverviewData() {
    try {
        // Load all data in parallel
        const [costsResponse, groupsResponse, paymentsResponse] = await Promise.all([
            fetch(API.COSTS).catch(() => ({ json: () => [] })),
            fetch(API.GROUPS).catch(() => ({ json: () => [] })),
            fetch(`${API.PAYMENTS}/admin/tracking`).catch(() => ({ json: () => ({ payments: [], statistics: {} }) }))
        ]);
        
        const costs = await costsResponse.json();
        const groups = await groupsResponse.json();
        const paymentsData = await paymentsResponse.json();
        const payments = paymentsData.payments || [];
        
        // Check share status for each cost
        const costsWithShares = await Promise.all(costs.map(async (cost) => {
            try {
                const sharesResponse = await fetch(`${API.COSTS}/${cost.costId}/shares`);
                if (sharesResponse.ok) {
                    const shares = await sharesResponse.json();
                    cost.hasShares = shares && shares.length > 0;
                    cost.shareCount = shares ? shares.length : 0;
                } else {
                    cost.hasShares = false;
                    cost.shareCount = 0;
                }
            } catch (e) {
                cost.hasShares = false;
                cost.shareCount = 0;
            }
            return cost;
        }));
        
        // Filter data by time period
        const dateRange = getDateRangeForPeriod(currentTimePeriod);
        const filteredCosts = filterByDateRange(costsWithShares, dateRange);
        const filteredPayments = filterByDateRange(payments, dateRange);
        
        // Calculate and update stats
        updateStatsCards(filteredCosts, payments, groups);
        
        // Load alerts
        loadAlerts(filteredCosts, payments, groups);
        
        // Load top lists
        loadTopCosts(filteredCosts);
        loadTopUnpaidUsers(payments);
        
        // Update charts
        updateCharts(filteredCosts, payments, groups);
        
        // Load recent activities (use all costs, not filtered)
        loadRecentActivities(costsWithShares);
        
    } catch (error) {
        console.error('Error loading overview:', error);
        showNotification('Lỗi khi tải dữ liệu tổng quan', 'error');
    }
}

function getDateRangeForPeriod(period) {
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    
    if (period === 'custom' && customDateRange) {
        return customDateRange;
    }
    
    let from, to;
    
    switch(period) {
        case 'today':
            from = new Date(today);
            to = new Date(today);
            to.setHours(23, 59, 59, 999);
            break;
        case 'week':
            const dayOfWeek = now.getDay();
            from = new Date(today);
            from.setDate(today.getDate() - dayOfWeek);
            to = new Date(today);
            to.setHours(23, 59, 59, 999);
            break;
        case 'month':
            from = new Date(now.getFullYear(), now.getMonth(), 1);
            to = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999);
            break;
        case 'quarter':
            const quarter = Math.floor(now.getMonth() / 3);
            from = new Date(now.getFullYear(), quarter * 3, 1);
            to = new Date(now.getFullYear(), (quarter + 1) * 3, 0, 23, 59, 59, 999);
            break;
        case 'year':
            from = new Date(now.getFullYear(), 0, 1);
            to = new Date(now.getFullYear(), 11, 31, 23, 59, 59, 999);
            break;
        default:
            from = new Date(now.getFullYear(), now.getMonth(), 1);
            to = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999);
    }
    
    return { from, to };
}

function filterByDateRange(items, dateRange) {
    if (!dateRange || !dateRange.from || !dateRange.to) return items;
    
    return items.filter(item => {
        const itemDate = new Date(item.createdAt || item.paymentDate || item.date);
        return itemDate >= dateRange.from && itemDate <= dateRange.to;
    });
}

function updateStatsCards(costs, payments, groups) {
    // Total costs
    const totalCost = costs.reduce((sum, c) => sum + (c.amount || 0), 0);
    document.getElementById('total-cost').textContent = formatCurrency(totalCost);
    
    // Paid amount
    const paidPayments = payments.filter(p => p.status === 'PAID');
    const paidAmount = paidPayments.reduce((sum, p) => sum + (p.amount || 0), 0);
    document.getElementById('paid-amount').textContent = formatCurrency(paidAmount);
    
    // Pending amount
    const pendingPayments = payments.filter(p => p.status === 'PENDING');
    const pendingAmount = pendingPayments.reduce((sum, p) => sum + (p.amount || 0), 0);
    document.getElementById('pending-amount').textContent = formatCurrency(pendingAmount);
    
    // Overdue amount
    const overduePayments = payments.filter(p => p.status === 'OVERDUE');
    const overdueAmount = overduePayments.reduce((sum, p) => sum + (p.amount || 0), 0);
    document.getElementById('overdue-amount').textContent = formatCurrency(overdueAmount);
    document.getElementById('overdue-count-value').textContent = overduePayments.length;
    
    // Total members
    let totalMembers = 0;
    const uniqueVehicles = new Set();
    groups.forEach(g => {
        // API trả về memberCount (số), không phải members (array)
        if (g.memberCount) totalMembers += g.memberCount;
        if (g.vehicleId) uniqueVehicles.add(g.vehicleId);
    });
    document.getElementById('total-members').textContent = totalMembers;
    
    // Total groups
    document.getElementById('total-groups').textContent = groups.length;
    
    // Total vehicles
    document.getElementById('total-vehicles').textContent = uniqueVehicles.size;
    
    // Payment rate
    const totalPaymentAmount = paidAmount + pendingAmount;
    const paymentRate = totalPaymentAmount > 0 ? ((paidAmount / totalPaymentAmount) * 100).toFixed(1) : 0;
    document.getElementById('payment-rate').textContent = paymentRate + '%';
    
    // Update trends (simplified - can be enhanced with previous period comparison)
    updateTrend('total-cost-trend', 0);
    updateTrend('paid-amount-trend', 0);
    updateTrend('pending-amount-trend', 0);
    updateTrend('total-members-trend', 0);
    updateTrend('total-groups-trend', 0);
    updateTrend('total-vehicles-trend', 0);
    updateTrend('payment-rate-trend', 0);
}

function updateTrend(elementId, change) {
    const element = document.getElementById(elementId);
    if (!element) return;
    
    if (change === 0) {
        element.innerHTML = '<i class="fas fa-minus"></i> -';
        element.className = 'stat-trend';
    } else if (change > 0) {
        element.innerHTML = `<i class="fas fa-arrow-up"></i> ${Math.abs(change).toFixed(1)}%`;
        element.className = 'stat-trend positive';
    } else {
        element.innerHTML = `<i class="fas fa-arrow-down"></i> ${Math.abs(change).toFixed(1)}%`;
        element.className = 'stat-trend negative';
    }
}

function loadAlerts(costs, payments, groups) {
    const alertsSection = document.getElementById('alerts-section');
    if (!alertsSection) return;
    
    const alerts = [];
    
    // Overdue payments alert
    const overduePayments = payments.filter(p => p.status === 'OVERDUE');
    if (overduePayments.length > 0) {
        const overdueAmount = overduePayments.reduce((sum, p) => sum + (p.amount || 0), 0);
        alerts.push({
            type: 'danger',
            icon: 'exclamation-triangle',
            title: `${overduePayments.length} khoản thanh toán quá hạn`,
            message: `Tổng số tiền: ${formatCurrency(overdueAmount)}`,
            action: 'Xem chi tiết',
            actionType: 'payment-tracking'
        });
    }
    
    // Unshared costs alert
    const unsharedCosts = costs.filter(c => {
        // Check if cost has shares
        return !c.hasShares;
    });
    if (unsharedCosts.length > 0) {
        alerts.push({
            type: 'warning',
            icon: 'share-alt',
            title: `${unsharedCosts.length} chi phí chưa được chia`,
            message: 'Cần phân chia chi phí để tính toán thanh toán',
            action: 'Xem chi tiết',
            actionType: 'cost-management'
        });
    }
    
    // Empty groups alert
    const emptyGroups = groups.filter(g => !g.memberCount || g.memberCount === 0);
    if (emptyGroups.length > 0) {
        alerts.push({
            type: 'info',
            icon: 'users',
            title: `${emptyGroups.length} nhóm chưa có thành viên`,
            message: 'Cần thêm thành viên vào nhóm để sử dụng các tính năng',
            action: 'Quản lý nhóm',
            actionType: 'group-management'
        });
    }
    
    // Render alerts
    if (alerts.length === 0) {
        alertsSection.innerHTML = '';
    } else {
        alertsSection.innerHTML = alerts.map(alert => `
            <div class="alert-item ${alert.type}">
                <div class="alert-icon">
                    <i class="fas fa-${alert.icon}"></i>
                </div>
                <div class="alert-content">
                    <div class="alert-title">${alert.title}</div>
                    <div class="alert-message">${alert.message}</div>
                </div>
                <button class="alert-action" onclick="switchSection('${alert.actionType}')">${alert.action}</button>
            </div>
        `).join('');
    }
}

function loadTopCosts(costs) {
    const topCostsList = document.getElementById('top-costs-list');
    if (!topCostsList) return;
    
    const sortedCosts = [...costs]
        .sort((a, b) => (b.amount || 0) - (a.amount || 0))
        .slice(0, 5);
    
    if (sortedCosts.length === 0) {
        topCostsList.innerHTML = '<p style="text-align: center; color: var(--text-light); padding: 2rem;">Không có dữ liệu</p>';
        return;
    }
    
    topCostsList.innerHTML = sortedCosts.map((cost, index) => `
        <div class="top-list-item">
            <div class="top-list-item-rank ${index === 0 ? 'top-1' : index === 1 ? 'top-2' : index === 2 ? 'top-3' : ''}">
                ${index + 1}
            </div>
            <div class="top-list-item-info">
                <div class="top-list-item-name">${getCostTypeName(cost.costType)}</div>
                <div class="top-list-item-detail">ID: #${cost.costId} | ${formatDate(cost.createdAt)}</div>
            </div>
            <div class="top-list-item-value">${formatCurrency(cost.amount)}</div>
        </div>
    `).join('');
}

function loadTopUnpaidUsers(payments) {
    const topUnpaidList = document.getElementById('top-unpaid-users-list');
    if (!topUnpaidList) return;
    
    const unpaidPayments = payments.filter(p => p.status === 'PENDING' || p.status === 'OVERDUE');
    
    // Group by userId
    const userTotals = {};
    unpaidPayments.forEach(p => {
        const userId = p.userId;
        if (!userTotals[userId]) {
            userTotals[userId] = { userId, amount: 0, count: 0 };
        }
        userTotals[userId].amount += p.amount || 0;
        userTotals[userId].count += 1;
    });
    
    const sortedUsers = Object.values(userTotals)
        .sort((a, b) => b.amount - a.amount)
        .slice(0, 5);
    
    if (sortedUsers.length === 0) {
        topUnpaidList.innerHTML = '<p style="text-align: center; color: var(--text-light); padding: 2rem;">Không có dữ liệu</p>';
        return;
    }
    
    topUnpaidList.innerHTML = sortedUsers.map((user, index) => `
        <div class="top-list-item">
            <div class="top-list-item-rank ${index === 0 ? 'top-1' : index === 1 ? 'top-2' : index === 2 ? 'top-3' : ''}">
                ${index + 1}
            </div>
            <div class="top-list-item-info">
                <div class="top-list-item-name">User #${user.userId}</div>
                <div class="top-list-item-detail">${user.count} khoản chưa thanh toán</div>
            </div>
            <div class="top-list-item-value">${formatCurrency(user.amount)}</div>
        </div>
    `).join('');
}

function updateCharts(costs, payments, groups) {
    // Update monthly chart
    if (monthlyChart) {
        updateMonthlyChartData(costs);
    }
    
    // Update category charts
    if (categoryChart) {
        updateCategoryChartData(costs);
    }
    
    // Update category bar chart
    if (categoryBarChart) {
        updateCategoryBarChartData(costs);
    }
    
    // Update payment rate chart
    if (paymentRateChart) {
        updatePaymentRateChartData(payments);
    }
}

function loadRecentActivities(costs) {
    const activityList = document.getElementById('activity-list');
    const recent = costs.slice(0, 5);
    
    activityList.innerHTML = recent.map(cost => `
        <div class="activity-item">
            <div class="activity-icon">
                <i class="fas fa-dollar-sign"></i>
            </div>
            <div class="activity-info">
                <div class="activity-title">Chi phí ${getCostTypeName(cost.costType)}</div>
                <div class="activity-time">${formatDate(cost.createdAt)}</div>
            </div>
            <div class="activity-amount" style="font-weight: 700; color: var(--primary);">
                ${formatCurrency(cost.amount)}
            </div>
        </div>
    `).join('');
}

// ============ CHARTS ============
function initCharts() {
    if (chartsInitialized) return;
    
    // Monthly Chart
    const monthlyCtx = document.getElementById('monthly-chart');
    if (monthlyCtx) {
        monthlyChart = new Chart(monthlyCtx, {
            type: 'line',
            data: {
                labels: ['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10', 'T11', 'T12'],
                datasets: [{
                    label: 'Chi phí',
                    data: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                    borderColor: '#3B82F6',
                    backgroundColor: 'rgba(59, 130, 246, 0.1)',
                    tension: 0.4,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function(value) {
                                return (value / 1000000).toFixed(1) + 'M';
                            }
                        }
                    }
                }
            }
        });
    }
    
    // Category Doughnut Chart
    const categoryCtx = document.getElementById('category-chart');
    if (categoryCtx) {
        categoryChart = new Chart(categoryCtx, {
            type: 'doughnut',
            data: {
                labels: ['Sạc điện', 'Bảo dưỡng', 'Bảo hiểm', 'Đăng kiểm', 'Vệ sinh', 'Khác'],
                datasets: [{
                    data: [0, 0, 0, 0, 0, 0],
                    backgroundColor: [
                        '#3B82F6',
                        '#10B981',
                        '#F59E0B',
                        '#EF4444',
                        '#8B5CF6',
                        '#6B7280'
                    ]
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom'
                    }
                }
            }
        });
    }
    
    // Category Bar Chart
    const categoryBarCtx = document.getElementById('category-bar-chart');
    if (categoryBarCtx) {
        categoryBarChart = new Chart(categoryBarCtx, {
            type: 'bar',
            data: {
                labels: ['Sạc điện', 'Bảo dưỡng', 'Bảo hiểm', 'Đăng kiểm', 'Vệ sinh', 'Khác'],
                datasets: [{
                    label: 'Chi phí',
                    data: [0, 0, 0, 0, 0, 0],
                    backgroundColor: [
                        '#3B82F6',
                        '#10B981',
                        '#F59E0B',
                        '#EF4444',
                        '#8B5CF6',
                        '#6B7280'
                    ]
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function(value) {
                                return (value / 1000000).toFixed(1) + 'M';
                            }
                        }
                    }
                }
            }
        });
    }
    
    // Payment Rate Chart
    const paymentRateCtx = document.getElementById('payment-rate-chart');
    if (paymentRateCtx) {
        paymentRateChart = new Chart(paymentRateCtx, {
            type: 'doughnut',
            data: {
                labels: ['Đã thanh toán', 'Chưa thanh toán', 'Quá hạn'],
                datasets: [{
                    data: [0, 0, 0],
                    backgroundColor: [
                        '#10B981',
                        '#F59E0B',
                        '#EF4444'
                    ]
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom'
                    }
                }
            }
        });
    }
    
    chartsInitialized = true;
}

function updateMonthlyChartData(costs) {
    if (!monthlyChart) return;
    
    const year = parseInt(document.getElementById('year-select')?.value || new Date().getFullYear());
    const monthlyData = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
    
    costs.forEach(cost => {
        const date = new Date(cost.createdAt);
        if (date.getFullYear() === year) {
            const month = date.getMonth();
            monthlyData[month] += cost.amount || 0;
        }
    });
    
    monthlyChart.data.datasets[0].data = monthlyData;
    monthlyChart.update();
}

function updateMonthlyChart() {
    loadOverviewData();
}

function updateCategoryChartData(costs) {
    if (!categoryChart) return;
    
    const categoryTotals = {
        'ElectricCharge': 0,
        'Maintenance': 0,
        'Insurance': 0,
        'Inspection': 0,
        'Cleaning': 0,
        'Other': 0
    };
    
    costs.forEach(cost => {
        const type = cost.costType || 'Other';
        categoryTotals[type] = (categoryTotals[type] || 0) + (cost.amount || 0);
    });
    
    categoryChart.data.datasets[0].data = [
        categoryTotals['ElectricCharge'],
        categoryTotals['Maintenance'],
        categoryTotals['Insurance'],
        categoryTotals['Inspection'],
        categoryTotals['Cleaning'],
        categoryTotals['Other']
    ];
    
    categoryChart.update();
}

function updateCategoryBarChartData(costs) {
    if (!categoryBarChart) return;
    
    const categoryTotals = {
        'ElectricCharge': 0,
        'Maintenance': 0,
        'Insurance': 0,
        'Inspection': 0,
        'Cleaning': 0,
        'Other': 0
    };
    
    costs.forEach(cost => {
        const type = cost.costType || 'Other';
        categoryTotals[type] = (categoryTotals[type] || 0) + (cost.amount || 0);
    });
    
    categoryBarChart.data.datasets[0].data = [
        categoryTotals['ElectricCharge'],
        categoryTotals['Maintenance'],
        categoryTotals['Insurance'],
        categoryTotals['Inspection'],
        categoryTotals['Cleaning'],
        categoryTotals['Other']
    ];
    
    categoryBarChart.update();
}

function updatePaymentRateChartData(payments) {
    if (!paymentRateChart) return;
    
    const paid = payments.filter(p => p.status === 'PAID').reduce((sum, p) => sum + (p.amount || 0), 0);
    const pending = payments.filter(p => p.status === 'PENDING').reduce((sum, p) => sum + (p.amount || 0), 0);
    const overdue = payments.filter(p => p.status === 'OVERDUE').reduce((sum, p) => sum + (p.amount || 0), 0);
    
    paymentRateChart.data.datasets[0].data = [paid, pending, overdue];
    paymentRateChart.update();
}

// ============ TIME PERIOD FILTERS ============
function setTimePeriod(period) {
    currentTimePeriod = period;
    
    // Update button states
    document.querySelectorAll('.btn-time-filter').forEach(btn => {
        btn.classList.remove('active');
    });
    const activeBtn = document.querySelector(`[data-period="${period}"]`);
    if (activeBtn) {
        activeBtn.classList.add('active');
    }
    
    // Reload data
    loadOverviewData();
}

function showCustomDateRange() {
    openModal('custom-date-modal');
    
    // Set default dates (last 30 days)
    const to = new Date();
    const from = new Date();
    from.setDate(from.getDate() - 30);
    
    document.getElementById('custom-date-from').value = from.toISOString().split('T')[0];
    document.getElementById('custom-date-to').value = to.toISOString().split('T')[0];
}

function applyCustomDateRange() {
    const fromStr = document.getElementById('custom-date-from').value;
    const toStr = document.getElementById('custom-date-to').value;
    
    if (!fromStr || !toStr) {
        showNotification('Vui lòng chọn đầy đủ từ ngày và đến ngày', 'warning');
        return;
    }
    
    customDateRange = {
        from: new Date(fromStr),
        to: new Date(toStr)
    };
    
    customDateRange.to.setHours(23, 59, 59, 999);
    
    currentTimePeriod = 'custom';
    
    // Update button states
    document.querySelectorAll('.btn-time-filter').forEach(btn => {
        btn.classList.remove('active');
    });
    const customBtn = document.querySelector(`[data-period="custom"]`);
    if (customBtn) {
        customBtn.classList.add('active');
    }
    
    closeCustomDateModal();
    loadOverviewData();
}

function closeCustomDateModal() {
    closeModal('custom-date-modal');
}

// ============ EXPORT REPORT ============
function exportOverviewReport() {
    try {
        // Get current data
        Promise.all([
            fetch(API.COSTS).then(r => r.json()).catch(() => []),
            fetch(API.GROUPS).then(r => r.json()).catch(() => []),
            fetch(`${API.PAYMENTS}/admin/tracking`).then(r => r.json()).catch(() => ({ payments: [] }))
        ]).then(([costs, groups, paymentsData]) => {
            const payments = paymentsData.payments || [];
            
            // Create CSV content
            let csv = 'BÁO CÁO TỔNG QUAN\n\n';
            csv += `Thời gian: ${new Date().toLocaleString('vi-VN')}\n`;
            csv += `Kỳ: ${getPeriodLabel(currentTimePeriod)}\n\n`;
            
            // Statistics
            csv += 'THỐNG KÊ TỔNG QUAN\n';
            csv += `Tổng chi phí,${formatCurrency(costs.reduce((sum, c) => sum + (c.amount || 0), 0))}\n`;
            csv += `Tổng số nhóm,${groups.length}\n`;
            csv += `Tổng số thành viên,${groups.reduce((sum, g) => sum + (g.memberCount || 0), 0)}\n`;
            csv += `Tổng số thanh toán,${payments.length}\n\n`;
            
            // Top costs
            csv += 'TOP 5 CHI PHÍ CAO NHẤT\n';
            csv += 'Thứ hạng,Loại chi phí,Số tiền\n';
            const topCosts = [...costs].sort((a, b) => (b.amount || 0) - (a.amount || 0)).slice(0, 5);
            topCosts.forEach((cost, index) => {
                csv += `${index + 1},${getCostTypeName(cost.costType)},${formatCurrency(cost.amount)}\n`;
            });
            
            // Create download link
            const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
            const link = document.createElement('a');
            const url = URL.createObjectURL(blob);
            
            link.setAttribute('href', url);
            link.setAttribute('download', `overview_report_${new Date().toISOString().split('T')[0]}.csv`);
            link.style.visibility = 'hidden';
            
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            
            showNotification('Đã xuất báo cáo thành công!', 'success');
        });
        
    } catch (error) {
        console.error('Error exporting report:', error);
        showNotification('Lỗi khi xuất báo cáo', 'error');
    }
}

function getPeriodLabel(period) {
    const labels = {
        'today': 'Hôm nay',
        'week': 'Tuần này',
        'month': 'Tháng này',
        'quarter': 'Quý này',
        'year': 'Năm này',
        'custom': 'Tùy chọn'
    };
    return labels[period] || period;
}

// ============ COST MANAGEMENT ============
let currentCostsData = [];

async function loadCosts() {
    try {
        const response = await fetch(API.COSTS);
        const costs = await response.json();
        currentCostsData = costs;
        
        // Check share status for each cost
        const costsWithShares = await Promise.all(costs.map(async (cost) => {
            try {
                const sharesResponse = await fetch(`${API.COSTS}/${cost.costId}/shares`);
                if (sharesResponse.ok) {
                    const shares = await sharesResponse.json();
                    cost.hasShares = shares && shares.length > 0;
                    cost.shareCount = shares ? shares.length : 0;
                } else {
                    cost.hasShares = false;
                    cost.shareCount = 0;
                }
            } catch (e) {
                cost.hasShares = false;
                cost.shareCount = 0;
            }
            return cost;
        }));
        
        renderCostsTable(costsWithShares);
        
    } catch (error) {
        console.error('Error loading costs:', error);
        document.getElementById('costs-tbody').innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 2rem; color: var(--danger);">
                    <i class="fas fa-exclamation-circle"></i> Lỗi khi tải dữ liệu
                </td>
            </tr>
        `;
    }
}

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
                ${cost.hasShares ? 
                    `<span class="status-badge paid" style="background: #10B981;">Đã chia (${cost.shareCount})</span>` :
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

function applyCostFilters() {
    const typeFilter = document.getElementById('cost-type-filter').value;
    const statusFilter = document.getElementById('cost-status-filter').value;
    const searchInput = document.getElementById('cost-search-input').value.toLowerCase();
    
    let filtered = [...currentCostsData];
    
    if (typeFilter) {
        filtered = filtered.filter(c => c.costType === typeFilter);
    }
    
    if (searchInput) {
        filtered = filtered.filter(c => 
            c.costId.toString().includes(searchInput) ||
            (c.description && c.description.toLowerCase().includes(searchInput))
        );
    }
    
    // Note: status filter will be applied after checking shares
    renderCostsTable(filtered);
}

async function viewCostDetail(costId) {
    try {
        const costResponse = await fetch(`${API.COSTS}/${costId}`);
        const cost = await costResponse.json();
        
        // Get cost shares
        let shares = [];
        try {
            const sharesResponse = await fetch(`${API.COSTS}/${costId}/shares`);
            if (sharesResponse.ok) {
                shares = await sharesResponse.json();
            }
        } catch (e) {
            console.error('Error loading shares:', e);
        }
        
        const modal = document.getElementById('cost-modal');
        const body = document.getElementById('cost-modal-body');
        const actions = document.getElementById('cost-modal-actions');
        
        document.getElementById('cost-modal-title').textContent = `Chi tiết chi phí #${cost.costId}`;
        
        const totalShared = shares.reduce((sum, s) => sum + (s.amountShare || 0), 0);
        
        body.innerHTML = `
            <div style="display: grid; gap: 1.5rem;">
                <div class="info-section">
                    <h4 style="margin-bottom: 1rem; color: var(--primary);">
                        <i class="fas fa-info-circle"></i> Thông tin chi phí
                    </h4>
                    <div class="info-grid">
                        <div class="info-row">
                            <span class="info-label">ID chi phí:</span>
                            <span class="info-value">#${cost.costId}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Vehicle ID:</span>
                            <span class="info-value">#${cost.vehicleId}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Loại chi phí:</span>
                            <span class="info-value">${getCostTypeName(cost.costType)}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Số tiền:</span>
                            <span class="info-value" style="color: var(--primary); font-weight: bold; font-size: 1.2rem;">
                                ${formatCurrency(cost.amount)}
                            </span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Ngày tạo:</span>
                            <span class="info-value">${formatDate(cost.createdAt)}</span>
                        </div>
                        ${cost.description ? `
                        <div class="info-row">
                            <span class="info-label">Mô tả:</span>
                            <span class="info-value">${cost.description}</span>
                        </div>
                        ` : ''}
                    </div>
                </div>
                
                <div class="info-section">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                        <h4 style="color: var(--primary); margin: 0;">
                            <i class="fas fa-users"></i> Phân chia chi phí (${shares.length})
                        </h4>
                        ${shares.length === 0 ? `
                            <button class="btn btn-primary btn-sm" onclick="closeCostModal(); openCostSplitModal(${cost.costId});">
                                <i class="fas fa-share-alt"></i> Chia chi phí
                            </button>
                        ` : ''}
                    </div>
                    ${shares.length > 0 ? `
                        <table style="width: 100%; border-collapse: collapse;">
                            <thead>
                                <tr style="background: var(--light);">
                                    <th style="padding: 0.75rem; text-align: left; border: 1px solid var(--border);">User ID</th>
                                    <th style="padding: 0.75rem; text-align: right; border: 1px solid var(--border);">Phần trăm</th>
                                    <th style="padding: 0.75rem; text-align: right; border: 1px solid var(--border);">Số tiền</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${shares.map(s => `
                                    <tr>
                                        <td style="padding: 0.75rem; border: 1px solid var(--border);">User #${s.userId}</td>
                                        <td style="padding: 0.75rem; text-align: right; border: 1px solid var(--border);">${s.percent}%</td>
                                        <td style="padding: 0.75rem; text-align: right; border: 1px solid var(--border); font-weight: bold;">
                                            ${formatCurrency(s.amountShare)}
                                        </td>
                                    </tr>
                                `).join('')}
                                <tr style="background: var(--light); font-weight: bold;">
                                    <td style="padding: 0.75rem; border: 1px solid var(--border);">TỔNG CỘNG</td>
                                    <td style="padding: 0.75rem; text-align: right; border: 1px solid var(--border);">100%</td>
                                    <td style="padding: 0.75rem; text-align: right; border: 1px solid var(--border);">
                                        ${formatCurrency(totalShared)}
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    ` : `
                        <p style="text-align: center; padding: 2rem; color: var(--text-light);">
                            <i class="fas fa-info-circle"></i> Chi phí này chưa được phân chia
                        </p>
                    `}
                </div>
            </div>
        `;
        
        actions.innerHTML = `
            <button type="button" class="btn btn-secondary" onclick="closeCostModal()">Đóng</button>
            <button type="button" class="btn btn-primary" onclick="closeCostModal(); openCostSplitModal(${cost.costId});">
                <i class="fas fa-share-alt"></i> ${shares.length > 0 ? 'Sửa phân chia' : 'Chia chi phí'}
            </button>
            <button type="button" class="btn btn-secondary" onclick="closeCostModal(); editCost(${cost.costId});">
                <i class="fas fa-edit"></i> Sửa chi phí
            </button>
        `;
        
        openModal('cost-modal');
        
    } catch (error) {
        console.error('Error loading cost detail:', error);
        showNotification('Lỗi khi tải chi tiết chi phí', 'error');
    }
}

// ============ COST CRUD FUNCTIONS ============
function openCreateCostModal() {
    document.getElementById('cost-form-modal-title').textContent = 'Tạo chi phí mới';
    document.getElementById('cost-form').reset();
    document.getElementById('cost-form-id').value = '';
    openModal('cost-form-modal');
}

async function editCost(costId) {
    try {
        const response = await fetch(`${API.COSTS}/${costId}`);
        const cost = await response.json();
        
        document.getElementById('cost-form-modal-title').textContent = 'Sửa chi phí';
        document.getElementById('cost-form-id').value = cost.costId;
        document.getElementById('cost-vehicle-id').value = cost.vehicleId;
        document.getElementById('cost-type-select').value = cost.costType;
        document.getElementById('cost-amount').value = cost.amount;
        document.getElementById('cost-description').value = cost.description || '';
        
        openModal('cost-form-modal');
    } catch (error) {
        console.error('Error loading cost for edit:', error);
        showNotification('Lỗi khi tải thông tin chi phí', 'error');
    }
}

function closeCostModal() {
    closeModal('cost-modal');
}

function closeCostFormModal() {
    closeModal('cost-form-modal');
}

// Cost form submit handler
document.addEventListener('DOMContentLoaded', function() {
    const costForm = document.getElementById('cost-form');
    if (costForm) {
        costForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const costId = document.getElementById('cost-form-id').value;
            const costData = {
                vehicleId: parseInt(document.getElementById('cost-vehicle-id').value),
                costType: document.getElementById('cost-type-select').value,
                amount: parseFloat(document.getElementById('cost-amount').value),
                description: document.getElementById('cost-description').value
            };
            
            try {
                let response;
                if (costId) {
                    // Update
                    response = await fetch(`${API.COSTS}/${costId}`, {
                        method: 'PUT',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(costData)
                    });
                } else {
                    // Create
                    response = await fetch(API.COSTS, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(costData)
                    });
                }
                
                if (response.ok) {
                    showNotification(costId ? 'Cập nhật chi phí thành công!' : 'Tạo chi phí thành công!', 'success');
                    closeCostFormModal();
                    loadCosts();
                } else {
                    const error = await response.text();
                    showNotification('Lỗi: ' + error, 'error');
                }
            } catch (error) {
                console.error('Error saving cost:', error);
                showNotification('Lỗi khi lưu chi phí', 'error');
            }
        });
    }
});

async function deleteCost(costId) {
    if (!confirm('Bạn có chắc muốn xóa chi phí này?\n\nLưu ý: Nếu đã có phân chia, các phân chia sẽ bị xóa theo!')) {
        return;
    }
    
    try {
        const response = await fetch(`${API.COSTS}/${costId}`, { 
            method: 'DELETE' 
        });
        
        if (response.ok) {
            showNotification('Đã xóa chi phí thành công', 'success');
            loadCosts();
        } else {
            const error = await response.text();
            showNotification('Lỗi: ' + error, 'error');
        }
    } catch (error) {
        console.error('Error deleting cost:', error);
        showNotification('Lỗi khi xóa chi phí', 'error');
    }
}

// ============ AUTO SPLIT ============
function initSplitMethodToggle() {
    const splitMethod = document.getElementById('split-method');
    const usagePeriod = document.getElementById('usage-period');
    
    if (splitMethod) {
        splitMethod.addEventListener('change', function() {
            if (this.value === 'BY_USAGE') {
                usagePeriod.style.display = 'block';
            } else {
                usagePeriod.style.display = 'none';
            }
        });
    }
}

async function loadGroupsForSplit() {
    try {
        console.log('Loading groups for split form...');
        const response = await fetch(API.GROUPS);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const groups = await response.json();
        console.log('Groups loaded:', groups);
        
        if (!Array.isArray(groups)) {
            console.error('Groups response is not an array:', groups);
            throw new Error('Invalid response format from server');
        }
        
        const select = document.getElementById('group-select');
        if (!select) {
            console.error('Group select element not found');
            return;
        }
        
        if (groups.length === 0) {
            select.innerHTML = '<option value="">-- Không có nhóm nào --</option>';
            console.warn('No groups found in database');
            return;
        }
        
        // Clear existing options
        select.innerHTML = '';
        
        // Add default option
        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.textContent = '-- Chọn nhóm --';
        select.appendChild(defaultOption);
        
        // Add groups with vehicleId stored in data attribute
        groups.forEach(g => {
            const option = document.createElement('option');
            const groupId = g.groupId || g.id;
            const groupName = g.groupName || g.name || `Nhóm #${groupId}`;
            const vehicleId = g.vehicleId || null;
            
            option.value = String(groupId);
            
            // Store vehicleId in data attribute
            if (vehicleId) {
                option.setAttribute('data-vehicle-id', String(vehicleId));
                option.textContent = `${groupName} (Xe ID: ${vehicleId})`;
            } else {
                option.textContent = `${groupName} (⚠️ Chưa có xe)`;
            }
            
            select.appendChild(option);
        });
        
        console.log(`Successfully loaded ${groups.length} groups`);
            
    } catch (error) {
        console.error('Error loading groups:', error);
        const select = document.getElementById('group-select');
        if (select) {
            select.innerHTML = '<option value="">-- Lỗi tải danh sách nhóm --</option>';
        }
        // Show user-friendly error message
        alert('Không thể tải danh sách nhóm từ database. Vui lòng kiểm tra:\n1. Group Management Service đang chạy (port 8082)\n2. Kết nối database\n3. Console để xem lỗi chi tiết');
    }
}

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

async function previewSplit() {
    let data;
    
    try {
        data = getFormData();
    } catch (error) {
        console.error('Error getting form data:', error);
        showNotification(error.message || 'Lỗi khi lấy dữ liệu form', 'error');
        return;
    }
    
    try {
        const response = await fetch(`${API.AUTO_SPLIT}/preview`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        
        // Check if response is ok before parsing JSON
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            const errorMsg = errorData.error || errorData.message || `Lỗi ${response.status}: ${response.statusText}`;
            showNotification(errorMsg, 'error');
            return;
        }
        
        const result = await response.json();
        
        // Check if result has error field
        if (result.error) {
            showNotification(result.error, 'error');
            return;
        }
        
        const previewCard = document.getElementById('preview-result');
        const previewContent = document.getElementById('preview-content');
        
        previewContent.innerHTML = `
            <div style="margin-bottom: 1rem;">
                <strong>Tổng chi phí:</strong> ${formatCurrency(data.amount)}
            </div>
            <table class="preview-table" style="width: 100%; border-collapse: collapse;">
                <thead>
                    <tr style="background: var(--light);">
                        <th style="padding: 0.75rem; text-align: left;">Người dùng</th>
                        <th style="padding: 0.75rem; text-align: right;">Tỷ lệ</th>
                        <th style="padding: 0.75rem; text-align: right;">Số tiền</th>
                    </tr>
                </thead>
                <tbody>
                    ${result.shares && result.shares.length > 0 ? result.shares.map(share => `
                        <tr>
                            <td style="padding: 0.75rem;">User #${share.userId}</td>
                            <td style="padding: 0.75rem; text-align: right;">${share.percent}%</td>
                            <td style="padding: 0.75rem; text-align: right; font-weight: 700;">${formatCurrency(share.amountShare)}</td>
                        </tr>
                    `).join('') : '<tr><td colspan="3">Không có dữ liệu</td></tr>'}
                </tbody>
            </table>
        `;
        
        previewCard.style.display = 'block';
        
    } catch (error) {
        console.error('Error previewing split:', error);
        showNotification('Lỗi khi xem trước: ' + (error.message || 'Lỗi không xác định'), 'error');
    }
}

async function createAndSplit() {
    let data;
    
    try {
        data = getFormData();
    } catch (error) {
        console.error('Error getting form data:', error);
        showNotification(error.message || 'Lỗi khi lấy dữ liệu form', 'error');
        return;
    }
    
    console.log('=== CREATING COST ===');
    console.log('Data:', data);
    
    try {
        const response = await fetch(`${API.AUTO_SPLIT}/create-and-split`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        
        console.log('Response status:', response.status);
        
        if (response.ok) {
            const result = await response.json();
            console.log('Result:', result);
            
            showNotification('Đã tạo và chia chi phí thành công!', 'success');
            document.getElementById('auto-split-form').reset();
            document.getElementById('preview-result').style.display = 'none';
            
            // Reload costs
            setTimeout(() => {
                switchSection('cost-management');
            }, 1000);
        } else {
            const errorText = await response.text();
            console.error('Error response:', errorText);
            showNotification('Lỗi khi tạo chi phí: ' + errorText, 'error');
        }
        
    } catch (error) {
        console.error('Error creating cost:', error);
        showNotification('Lỗi khi tạo chi phí: ' + error.message, 'error');
    }
}

function getFormData() {
    const groupSelect = document.getElementById('group-select');
    const selectedOption = groupSelect.options[groupSelect.selectedIndex];
    
    // Get groupId from option value
    const groupId = parseInt(groupSelect.value);
    if (!groupId) {
        throw new Error('Vui lòng chọn nhóm');
    }
    
    // Get vehicleId from data attribute
    const vehicleIdAttr = selectedOption ? selectedOption.getAttribute('data-vehicle-id') : null;
    const vehicleId = vehicleIdAttr ? parseInt(vehicleIdAttr) : null;
    
    if (!vehicleId) {
        throw new Error('Nhóm này chưa có thông tin xe. Vui lòng chọn nhóm khác hoặc cập nhật thông tin nhóm.');
    }
    
    // Get month and year, default to current month/year if not provided
    const month = parseInt(document.getElementById('month').value) || new Date().getMonth() + 1;
    const year = parseInt(document.getElementById('year').value) || new Date().getFullYear();
    
    return {
        groupId: groupId,
        vehicleId: vehicleId,
        costType: document.getElementById('cost-type').value,
        amount: parseFloat(document.getElementById('amount').value),
        description: document.getElementById('description').value,
        splitMethod: document.getElementById('split-method').value,
        month: month,
        year: year
    };
}

// ============ PAYMENT TRACKING ============
// Use window scope to avoid conflicts with admin-payments.js
if (typeof window.currentPaymentsData === 'undefined') {
    window.currentPaymentsData = [];
}
let currentPaymentsData = window.currentPaymentsData;

async function loadPayments(filters = {}) {
    try {
        console.log('Loading payments with filters:', filters);
        
        // Build URL with filters
        let url = '/api/payments/admin/tracking?';
        if (filters.status) url += `status=${filters.status}&`;
        if (filters.startDate) url += `startDate=${filters.startDate}&`;
        if (filters.endDate) url += `endDate=${filters.endDate}&`;
        if (filters.search) url += `search=${encodeURIComponent(filters.search)}&`;
        
        const response = await fetch(url);
        const data = await response.json();
        
        console.log('Payments data:', data);
        
        window.currentPaymentsData = data.payments || [];
        currentPaymentsData = window.currentPaymentsData;
        const stats = data.statistics || { total: 0, totalAmount: 0, paidCount: 0, pendingCount: 0 };
        
        // Update statistics
        document.getElementById('total-payments').textContent = stats.total;
        document.getElementById('paid-count').textContent = stats.paidCount;
        document.getElementById('pending-count').textContent = stats.pendingCount;
        document.getElementById('total-amount').textContent = formatCurrency(stats.totalAmount);
        
        // Render table
        renderPaymentsTable(currentPaymentsData);
        
    } catch (error) {
        console.error('Error loading payments:', error);
        showNotification('Lỗi khi tải danh sách thanh toán', 'error');
    }
}

function renderPaymentsTable(payments) {
    const tbody = document.getElementById('payments-tbody');
    
    if (!payments || payments.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="9" style="text-align: center; padding: 2rem; color: var(--text-light);">
                    <i class="fas fa-inbox"></i><br>
                    Không có dữ liệu thanh toán
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = payments.map(payment => {
        const statusClass = getPaymentStatusClass(payment.status);
        const statusText = getPaymentStatusText(payment.status);
        const paymentDate = payment.paymentDate ? new Date(payment.paymentDate).toLocaleDateString('vi-VN') : '-';
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
                    <div style="display: flex; gap: 0.5rem; flex-wrap: wrap; align-items: center;">
                        <button class="btn btn-sm" style="background: #10B981; color: white; padding: 0.5rem 0.75rem; border-radius: 6px; border: none; cursor: pointer; display: inline-flex; align-items: center; gap: 0.25rem; font-size: 0.875rem; transition: all 0.2s;" onmouseover="this.style.background='#059669'" onmouseout="this.style.background='#10B981'" onclick="openEditPaymentModal(${payment.paymentId})" title="Chỉnh sửa thanh toán">
                            <i class="fas fa-edit"></i>
                            <span>Sửa</span>
                        </button>
                        <button class="btn btn-sm" style="background: var(--danger); color: white; padding: 0.5rem 0.75rem; border-radius: 6px; border: none; cursor: pointer; display: inline-flex; align-items: center; gap: 0.25rem; font-size: 0.875rem;" onclick="deletePayment(${payment.paymentId})" title="Xóa thanh toán">
                            <i class="fas fa-trash"></i>
                        </button>
                        ${payment.status === 'PENDING' ? `
                            <button class="btn btn-sm btn-success" onclick="openConfirmPaymentModal(${payment.paymentId})" title="Xác nhận">
                                <i class="fas fa-check"></i>
                            </button>
                            <button class="btn btn-sm" style="background: var(--warning); color: white;" onclick="sendReminder(${payment.paymentId})" title="Nhắc nhở">
                                <i class="fas fa-bell"></i>
                            </button>
                        ` : ''}
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function getPaymentStatusClass(status) {
    const statusMap = {
        'PAID': 'paid',
        'PENDING': 'pending',
        'OVERDUE': 'overdue',
        'CANCELLED': 'cancelled'
    };
    return statusMap[status] || 'pending';
}

function getPaymentStatusText(status) {
    const statusMap = {
        'PAID': 'Đã thanh toán',
        'PENDING': 'Chờ thanh toán',
        'OVERDUE': 'Quá hạn',
        'CANCELLED': 'Đã hủy'
    };
    return statusMap[status] || status;
}

function initPaymentTracking() {
    console.log('Initializing payment tracking...');
    
    // Filter button
    const btnFilter = document.getElementById('btn-filter-payments');
    if (btnFilter) {
        btnFilter.addEventListener('click', applyPaymentFilters);
    }
    
    // Reset button
    const btnReset = document.getElementById('btn-reset-filters');
    if (btnReset) {
        btnReset.addEventListener('click', resetPaymentFilters);
    }
    
    // Export button
    const btnExport = document.getElementById('btn-export-payments');
    if (btnExport) {
        btnExport.addEventListener('click', exportPaymentsToExcel);
    }
    
    // Confirm payment form
    const confirmForm = document.getElementById('confirm-payment-form');
    if (confirmForm) {
        confirmForm.addEventListener('submit', handleConfirmPayment);
    }
    
    // Enter key on search input
    const searchInput = document.getElementById('payment-search');
    if (searchInput) {
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                applyPaymentFilters();
            }
        });
    }
    
    console.log('Payment tracking initialized');
}

function applyPaymentFilters() {
    const filters = {
        status: document.getElementById('payment-status-filter')?.value || '',
        startDate: document.getElementById('payment-date-from')?.value || '',
        endDate: document.getElementById('payment-date-to')?.value || '',
        search: document.getElementById('payment-search')?.value || ''
    };
    
    loadPayments(filters);
}

function resetPaymentFilters() {
    document.getElementById('payment-status-filter').value = '';
    document.getElementById('payment-date-from').value = '';
    document.getElementById('payment-date-to').value = '';
    document.getElementById('payment-search').value = '';
    
    loadPayments();
}

async function viewPaymentDetails(paymentId) {
    try {
        const response = await fetch(`/api/payments/${paymentId}/details`);
        const details = await response.json();
        
        if (details.error) {
            showNotification('Không tìm thấy thanh toán', 'error');
            return;
        }
        
        const modal = document.getElementById('payment-details-modal');
        const body = document.getElementById('payment-details-body');
        
        const paymentDate = details.paymentDate ? new Date(details.paymentDate).toLocaleString('vi-VN') : '-';
        const costDate = details.cost?.date ? new Date(details.cost.date).toLocaleDateString('vi-VN') : '-';
        
        body.innerHTML = `
            <div style="display: grid; gap: 1.5rem;">
                <div class="info-section">
                    <h4 style="margin-bottom: 1rem; color: var(--primary);">
                        <i class="fas fa-money-bill-wave"></i> Thông tin thanh toán
                    </h4>
                    <div class="info-grid">
                        <div class="info-row">
                            <span class="info-label">Mã thanh toán:</span>
                            <span class="info-value">#${details.paymentId}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Số tiền:</span>
                            <span class="info-value" style="color: var(--primary); font-weight: bold; font-size: 1.2rem;">
                                ${formatCurrency(details.amount)}
                            </span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Phương thức:</span>
                            <span class="info-value">${details.method || '-'}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Mã giao dịch:</span>
                            <span class="info-value" style="font-family: monospace;">${details.transactionCode || '-'}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Trạng thái:</span>
                            <span class="status-badge ${getPaymentStatusClass(details.status)}">
                                ${getPaymentStatusText(details.status)}
                            </span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Ngày thanh toán:</span>
                            <span class="info-value">${paymentDate}</span>
                        </div>
                    </div>
                </div>
                
                ${details.user ? `
                <div class="info-section">
                    <h4 style="margin-bottom: 1rem; color: var(--primary);">
                        <i class="fas fa-user"></i> Thông tin người dùng
                    </h4>
                    <div class="info-grid">
                        <div class="info-row">
                            <span class="info-label">User ID:</span>
                            <span class="info-value">#${details.userId}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Tên:</span>
                            <span class="info-value">${details.user.name || '-'}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Email:</span>
                            <span class="info-value">${details.user.email || '-'}</span>
                        </div>
                    </div>
                </div>
                ` : ''}
                
                ${details.cost ? `
                <div class="info-section">
                    <h4 style="margin-bottom: 1rem; color: var(--primary);">
                        <i class="fas fa-receipt"></i> Thông tin chi phí
                    </h4>
                    <div class="info-grid">
                        <div class="info-row">
                            <span class="info-label">Loại chi phí:</span>
                            <span class="info-value">${details.cost.costType || '-'}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Tổng chi phí:</span>
                            <span class="info-value">${formatCurrency(details.cost.amount || 0)}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Ngày phát sinh:</span>
                            <span class="info-value">${costDate}</span>
                        </div>
                        ${details.cost.description ? `
                        <div class="info-row">
                            <span class="info-label">Mô tả:</span>
                            <span class="info-value">${details.cost.description}</span>
                        </div>
                        ` : ''}
                    </div>
                </div>
                ` : ''}
            </div>
        `;
        
        modal.style.display = 'flex';
        
    } catch (error) {
        console.error('Error loading payment details:', error);
        showNotification('Lỗi khi tải chi tiết thanh toán', 'error');
    }
}

function closePaymentDetailsModal() {
    document.getElementById('payment-details-modal').style.display = 'none';
}

function openConfirmPaymentModal(paymentId) {
    const payment = currentPaymentsData.find(p => p.paymentId === paymentId);
    
    if (!payment) {
        showNotification('Không tìm thấy thanh toán', 'error');
        return;
    }
    
    document.getElementById('confirm-payment-id').value = paymentId;
    document.getElementById('confirm-user-id').textContent = `#${payment.userId}`;
    document.getElementById('confirm-amount').textContent = formatCurrency(payment.amount);
    document.getElementById('confirm-transaction-code').textContent = payment.transactionCode || '-';
    document.getElementById('confirm-note').value = '';
    
    document.getElementById('confirm-payment-modal').style.display = 'flex';
}

function closeConfirmPaymentModal() {
    document.getElementById('confirm-payment-modal').style.display = 'none';
}

async function handleConfirmPayment(event) {
    event.preventDefault();
    
    const paymentId = document.getElementById('confirm-payment-id').value;
    const note = document.getElementById('confirm-note').value;
    
    try {
        const response = await fetch(`/api/payments/${paymentId}/admin-confirm`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ note })
        });
        
        const result = await response.json();
        
        if (result.success) {
            showNotification('Đã xác nhận thanh toán thành công!', 'success');
            closeConfirmPaymentModal();
            applyPaymentFilters(); // Reload with current filters
        } else {
            showNotification(result.message || 'Lỗi khi xác nhận thanh toán', 'error');
        }
        
    } catch (error) {
        console.error('Error confirming payment:', error);
        showNotification('Lỗi khi xác nhận thanh toán', 'error');
    }
}

async function sendReminder(paymentId) {
    if (!confirm('Bạn có chắc chắn muốn gửi nhắc nhở thanh toán?')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/payments/${paymentId}/remind`, {
            method: 'POST'
        });
        
        const result = await response.json();
        
        if (result.success) {
            showNotification('Đã gửi nhắc nhở thanh toán!', 'success');
        } else {
            showNotification(result.message || 'Lỗi khi gửi nhắc nhở', 'error');
        }
        
    } catch (error) {
        console.error('Error sending reminder:', error);
        showNotification('Lỗi khi gửi nhắc nhở', 'error');
    }
}

function exportPaymentsToExcel() {
    if (currentPaymentsData.length === 0) {
        showNotification('Không có dữ liệu để xuất', 'warning');
        return;
    }
    
    try {
        // Create CSV content
        let csv = 'ID,User ID,Chi phí,Số tiền,Phương thức,Mã giao dịch,Ngày,Trạng thái\n';
        
        currentPaymentsData.forEach(payment => {
            const date = payment.paymentDate ? new Date(payment.paymentDate).toLocaleDateString('vi-VN') : '-';
            csv += `${payment.paymentId},`;
            csv += `${payment.userId},`;
            csv += `"${payment.costType || '-'}",`;
            csv += `${payment.amount},`;
            csv += `${payment.method || '-'},`;
            csv += `"${payment.transactionCode || '-'}",`;
            csv += `${date},`;
            csv += `${getPaymentStatusText(payment.status)}\n`;
        });
        
        // Create download link
        const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);
        
        link.setAttribute('href', url);
        link.setAttribute('download', `payments_${new Date().toISOString().split('T')[0]}.csv`);
        link.style.visibility = 'hidden';
        
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
        showNotification('Đã xuất file Excel thành công!', 'success');
        
    } catch (error) {
        console.error('Error exporting to Excel:', error);
        showNotification('Lỗi khi xuất file Excel', 'error');
    }
}

// ============ GROUP MANAGEMENT ============
function initGroupManagement() {
    if (window.IS_CUSTOM_ADMIN_GROUPS_PAGE) {
        return;
    }
    // Create Group button
    const btnCreateGroup = document.getElementById('btn-create-group');
    if (btnCreateGroup) {
        btnCreateGroup.addEventListener('click', openCreateGroupModal);
    }

    // Group Form submit
    const groupForm = document.getElementById('group-form');
    if (groupForm) {
        groupForm.addEventListener('submit', handleGroupFormSubmit);
    }

    // Member Form submit
    const memberForm = document.getElementById('member-form');
    if (memberForm) {
        memberForm.addEventListener('submit', handleMemberFormSubmit);
    }
}

async function loadGroups() {
    try {
        const response = await fetch(API.GROUPS);
        const groups = await response.json();
        
        const grid = document.getElementById('groups-grid');
        grid.innerHTML = groups.map(group => `
            <div class="group-card">
                <h4>${group.groupName}</h4>
                <p>Admin: User #${group.adminId} | Xe ID: ${group.vehicleId || 'N/A'}</p>
                <div style="display: flex; gap: 1rem; margin-top: 1rem;">
                    <div style="flex: 1; padding: 0.75rem; background: var(--light); border-radius: 8px; text-align: center;">
                        <div style="font-size: 1.5rem; font-weight: 700; color: var(--primary);">
                            ${group.memberCount || 0}
                        </div>
                        <div style="font-size: 0.75rem; color: var(--text-light);">Thành viên</div>
                    </div>
                    <div style="flex: 1; padding: 0.75rem; background: var(--light); border-radius: 8px; text-align: center;">
                        <div style="font-size: 1.5rem; font-weight: 700; color: var(--success);">
                            ${group.status === 'Active' ? 'Hoạt động' : 'Không hoạt động'}
                        </div>
                        <div style="font-size: 0.75rem; color: var(--text-light);">Trạng thái</div>
                    </div>
                </div>
                <div style="display: flex; gap: 0.5rem; margin-top: 1rem;">
                    <button class="btn btn-primary" style="flex: 1;" onclick="viewGroupDetail(${group.groupId})">
                        <i class="fas fa-eye"></i> Xem
                    </button>
                    <button class="btn btn-secondary" style="flex: 1;" onclick="editGroup(${group.groupId})">
                        <i class="fas fa-edit"></i> Sửa
                    </button>
                    <button class="btn" style="flex: 1; background: var(--danger); color: white;" onclick="deleteGroup(${group.groupId}, '${group.groupName}')">
                        <i class="fas fa-trash"></i> Xóa
                    </button>
                </div>
            </div>
        `).join('');
        
    } catch (error) {
        console.error('Error loading groups:', error);
        showNotification('Lỗi khi tải danh sách nhóm', 'error');
    }
}

// ========== CREATE GROUP ==========
function openCreateGroupModal() {
    document.getElementById('group-modal-title').textContent = 'Tạo nhóm mới';
    document.getElementById('group-form').reset();
    document.getElementById('group-id').value = '';
    document.getElementById('group-status').value = 'Active';
    openModal('group-modal');
}

async function handleGroupFormSubmit(e) {
    e.preventDefault();
    
    const groupId = document.getElementById('group-id').value;
    const groupData = {
        groupName: document.getElementById('group-name').value,
        adminId: parseInt(document.getElementById('group-admin').value),
        vehicleId: document.getElementById('group-vehicle').value ? parseInt(document.getElementById('group-vehicle').value) : null,
        status: document.getElementById('group-status').value
    };

    try {
        let response;
        if (groupId) {
            // Update existing group
            response = await fetch(`${API.GROUP_DETAIL}/${groupId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(groupData)
            });
        } else {
            // Create new group
            response = await fetch(API.GROUPS, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(groupData)
            });
        }

        if (response.ok) {
            showNotification(groupId ? 'Cập nhật nhóm thành công!' : 'Tạo nhóm thành công!', 'success');
            closeGroupModal();
            loadGroups();
        } else {
            const error = await response.text();
            showNotification('Lỗi: ' + error, 'error');
        }
    } catch (error) {
        console.error('Error saving group:', error);
        showNotification('Lỗi khi lưu nhóm', 'error');
    }
}

// ========== VIEW GROUP DETAIL ==========
async function viewGroupDetail(groupId) {
    try {
        // Fetch group info
        const groupResponse = await fetch(`${API.GROUP_DETAIL}/${groupId}`);
        const group = await groupResponse.json();

        // Fetch members
        const membersResponse = await fetch(`${API.GROUP_MEMBERS}/${groupId}/members`);
        const members = await membersResponse.json();

        // Fetch votes
        const votesResponse = await fetch(`${API.GROUP_VOTES}/${groupId}/votes`);
        const votes = await votesResponse.json();

        // Display in modal
        document.getElementById('group-detail-title').textContent = `Chi tiết nhóm: ${group.groupName}`;
        
        const content = document.getElementById('group-detail-content');
        content.innerHTML = `
            <div style="margin-bottom: 2rem;">
                <h4 style="margin-bottom: 1rem; color: var(--primary);">
                    <i class="fas fa-info-circle"></i> Thông tin nhóm
                </h4>
                <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem;">
                    <div class="info-item">
                        <strong>Tên nhóm:</strong> ${group.groupName}
                    </div>
                    <div class="info-item">
                        <strong>Admin ID:</strong> ${group.adminId}
                    </div>
                    <div class="info-item">
                        <strong>Vehicle ID:</strong> ${group.vehicleId || 'N/A'}
                    </div>
                    <div class="info-item">
                        <strong>Trạng thái:</strong> 
                        <span class="badge ${group.status === 'Active' ? 'badge-success' : 'badge-secondary'}">
                            ${group.status === 'Active' ? 'Hoạt động' : 'Không hoạt động'}
                        </span>
                    </div>
                    <div class="info-item">
                        <strong>Ngày tạo:</strong> ${formatDate(group.createdAt)}
                    </div>
                </div>
            </div>

            <div style="margin-bottom: 2rem;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                    <h4 style="color: var(--primary);">
                        <i class="fas fa-users"></i> Thành viên (${members.length})
                    </h4>
                    <button class="btn btn-primary btn-sm" onclick="openAddMemberModal(${groupId})">
                        <i class="fas fa-plus"></i> Thêm thành viên
                    </button>
                </div>
                ${members.length > 0 ? `
                    <table class="table">
                        <thead>
                            <tr>
                                <th>User ID</th>
                                <th>Vai trò</th>
                                <th>Sở hữu (%)</th>
                                <th>Ngày tham gia</th>
                                <th>Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${members.map(member => `
                                <tr>
                                    <td>${member.userId}</td>
                                    <td>
                                        <span class="badge ${member.role === 'Admin' ? 'badge-primary' : 'badge-secondary'}">
                                            ${member.role === 'Admin' ? 'Quản trị' : 'Thành viên'}
                                        </span>
                                    </td>
                                    <td>${member.ownershipPercent || 0}%</td>
                                    <td>${formatDate(member.joinedAt)}</td>
                                    <td>
                                        <button class="btn btn-sm btn-secondary" onclick="editMember(${groupId}, ${member.memberId})">
                                            <i class="fas fa-edit"></i>
                                        </button>
                                        <button class="btn btn-sm" style="background: var(--danger); color: white;" onclick="deleteMember(${groupId}, ${member.memberId})">
                                            <i class="fas fa-trash"></i>
                                        </button>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                ` : '<p style="color: var(--text-light); text-align: center; padding: 2rem;">Chưa có thành viên nào</p>'}
            </div>

            <div>
                <h4 style="margin-bottom: 1rem; color: var(--primary);">
                    <i class="fas fa-vote-yea"></i> Lịch sử bỏ phiếu (${votes.length})
                </h4>
                ${votes.length > 0 ? `
                    <div style="display: grid; gap: 1rem;">
                        ${votes.map(vote => `
                            <div class="vote-card" style="padding: 1rem; background: var(--light); border-radius: 8px;">
                                <h5 style="margin-bottom: 0.5rem;">${vote.topic}</h5>
                                <div style="display: flex; gap: 1rem; margin-top: 0.5rem;">
                                    <div><strong>Lựa chọn A:</strong> ${vote.optionA}</div>
                                    <div><strong>Lựa chọn B:</strong> ${vote.optionB}</div>
                                </div>
                                <div style="margin-top: 0.5rem;">
                                    <strong>Kết quả:</strong> 
                                    <span class="badge badge-success">${vote.finalResult || 'Đang bỏ phiếu'}</span>
                                    <span style="margin-left: 1rem; color: var(--text-light);">
                                        Tổng số phiếu: ${vote.totalVotes}
                                    </span>
                                </div>
                                <div style="margin-top: 0.5rem; color: var(--text-light); font-size: 0.875rem;">
                                    ${formatDate(vote.createdAt)}
                                </div>
                            </div>
                        `).join('')}
                    </div>
                ` : '<p style="color: var(--text-light); text-align: center; padding: 2rem;">Chưa có cuộc bỏ phiếu nào</p>'}
            </div>
        `;

        openModal('group-detail-modal');
        
    } catch (error) {
        console.error('Error loading group detail:', error);
        showNotification('Lỗi khi tải chi tiết nhóm', 'error');
    }
}

// ========== EDIT GROUP ==========
async function editGroup(groupId) {
    try {
        const response = await fetch(`${API.GROUP_DETAIL}/${groupId}`);
        const group = await response.json();

        document.getElementById('group-modal-title').textContent = 'Chỉnh sửa nhóm';
        document.getElementById('group-id').value = group.groupId;
        document.getElementById('group-name').value = group.groupName;
        document.getElementById('group-admin').value = group.adminId;
        document.getElementById('group-vehicle').value = group.vehicleId || '';
        document.getElementById('group-status').value = group.status;

        openModal('group-modal');
    } catch (error) {
        console.error('Error loading group for edit:', error);
        showNotification('Lỗi khi tải thông tin nhóm', 'error');
    }
}

// ========== DELETE GROUP ==========
async function deleteGroup(groupId, groupName) {
    if (!confirm(`Bạn có chắc chắn muốn xóa nhóm "${groupName}"?\n\nLưu ý: Tất cả thành viên và dữ liệu liên quan sẽ bị xóa!`)) {
        return;
    }

    try {
        const response = await fetch(`${API.GROUP_DETAIL}/${groupId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            showNotification('Xóa nhóm thành công!', 'success');
            loadGroups();
        } else {
            showNotification('Lỗi khi xóa nhóm', 'error');
        }
    } catch (error) {
        console.error('Error deleting group:', error);
        showNotification('Lỗi khi xóa nhóm', 'error');
    }
}

// ========== MEMBER MANAGEMENT ==========
function openAddMemberModal(groupId) {
    currentGroupId = groupId;
    document.getElementById('member-modal-title').textContent = 'Thêm thành viên';
    document.getElementById('member-form').reset();
    document.getElementById('member-group-id').value = groupId;
    document.getElementById('member-id').value = '';
    document.getElementById('member-role').value = 'Member';
    document.getElementById('member-ownership').value = '0';
    
    closeGroupDetailModal();
    openModal('member-modal');
}

async function editMember(groupId, memberId) {
    try {
        const response = await fetch(`${API.GROUP_MEMBERS}/${groupId}/members`);
        const members = await response.json();
        const member = members.find(m => m.memberId === memberId);

        if (member) {
            currentGroupId = groupId;
            document.getElementById('member-modal-title').textContent = 'Chỉnh sửa thành viên';
            document.getElementById('member-group-id').value = groupId;
            document.getElementById('member-id').value = member.memberId;
            document.getElementById('member-user-id').value = member.userId;
            document.getElementById('member-role').value = member.role;
            document.getElementById('member-ownership').value = member.ownershipPercent || 0;

            closeGroupDetailModal();
            openModal('member-modal');
        }
    } catch (error) {
        console.error('Error loading member for edit:', error);
        showNotification('Lỗi khi tải thông tin thành viên', 'error');
    }
}

async function handleMemberFormSubmit(e) {
    e.preventDefault();
    
    const groupId = document.getElementById('member-group-id').value;
    const memberId = document.getElementById('member-id').value;
    
    const memberData = {
        userId: parseInt(document.getElementById('member-user-id').value),
        role: document.getElementById('member-role').value,
        ownershipPercent: parseFloat(document.getElementById('member-ownership').value) || 0
    };

    try {
        let response;
        if (memberId) {
            // Update member - need to implement PUT endpoint
            response = await fetch(`${API.GROUP_MEMBERS}/${groupId}/members/${memberId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(memberData)
            });
        } else {
            // Add new member
            response = await fetch(`${API.GROUP_MEMBERS}/${groupId}/members`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(memberData)
            });
        }

        if (response.ok) {
            showNotification(memberId ? 'Cập nhật thành viên thành công!' : 'Thêm thành viên thành công!', 'success');
            closeMemberModal();
            viewGroupDetail(groupId);
        } else {
            const error = await response.text();
            showNotification('Lỗi: ' + error, 'error');
        }
    } catch (error) {
        console.error('Error saving member:', error);
        showNotification('Lỗi khi lưu thành viên', 'error');
    }
}

async function deleteMember(groupId, memberId) {
    if (!confirm('Bạn có chắc chắn muốn xóa thành viên này khỏi nhóm?')) {
        return;
    }

    try {
        const response = await fetch(`${API.GROUP_MEMBERS}/${groupId}/members/${memberId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            showNotification('Xóa thành viên thành công!', 'success');
            viewGroupDetail(groupId);
        } else {
            showNotification('Lỗi khi xóa thành viên', 'error');
        }
    } catch (error) {
        console.error('Error deleting member:', error);
        showNotification('Lỗi khi xóa thành viên', 'error');
    }
}

// ========== MODAL CONTROLS ==========
function openModal(modalId) {
    document.getElementById('modal-overlay').classList.add('active');
    document.getElementById(modalId).classList.add('active');
}

function closeModal(modalId) {
    document.getElementById('modal-overlay').classList.remove('active');
    document.getElementById(modalId).classList.remove('active');
}

function closeGroupModal() {
    closeModal('group-modal');
}

function closeGroupDetailModal() {
    closeModal('group-detail-modal');
}

function closeMemberModal() {
    closeModal('member-modal');
}

// Close modal when clicking overlay
document.addEventListener('DOMContentLoaded', function() {
    const overlay = document.getElementById('modal-overlay');
    if (overlay) {
        overlay.addEventListener('click', function() {
            document.querySelectorAll('.modal.active').forEach(modal => {
                modal.classList.remove('active');
            });
            this.classList.remove('active');
        });
    }
});

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
        'ElectricCharge': 'Sạc điện',
        'Maintenance': 'Bảo dưỡng',
        'Insurance': 'Bảo hiểm',
        'Inspection': 'Đăng kiểm',
        'Cleaning': 'Vệ sinh',
        'Other': 'Khác'
    };
    return types[type] || type;
}

function getSplitMethodName(method) {
    const methods = {
        'BY_OWNERSHIP': 'Theo sở hữu',
        'BY_USAGE': 'Theo km',
        'EQUAL': 'Chia đều',
        'CUSTOM': 'Tùy chỉnh',
        'N/A': 'Chưa chia'
    };
    return methods[method] || method;
}

function getStatusClass(status) {
    return status === 'PAID' ? 'paid' : status === 'OVERDUE' ? 'overdue' : 'pending';
}

function getStatusName(status) {
    const statuses = {
        'PENDING': 'Chưa thanh toán',
        'PAID': 'Đã thanh toán',
        'OVERDUE': 'Quá hạn'
    };
    return statuses[status] || status;
}

function showNotification(message, type) {
    // Create toast notification
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-circle'}"></i>
        <span>${message}</span>
    `;
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.style.opacity = '1';
        toast.style.transform = 'translateY(0)';
    }, 100);
    
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(20px)';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// ============================================
// EDIT PAYMENT FUNCTIONS
// ============================================

function openEditPaymentModal(paymentId) {
    // Fetch payment details first
    fetch(`/api/payments/${paymentId}/details`)
        .then(response => response.json())
        .then(payment => {
            document.getElementById('edit-payment-id').value = payment.paymentId;
            document.getElementById('edit-user-id').value = payment.userId;
            document.getElementById('edit-cost-id').value = payment.costId;
            document.getElementById('edit-amount').value = payment.amount;
            document.getElementById('edit-method').value = payment.method || '';
            document.getElementById('edit-transaction-code').value = payment.transactionCode || '';
            
            // Normalize status to uppercase to match select option values
            const statusValue = payment.status ? String(payment.status).trim().toUpperCase() : 'PENDING';
            document.getElementById('edit-status').value = statusValue;
            
            // Format date for input
            if (payment.paymentDate) {
                const date = new Date(payment.paymentDate);
                document.getElementById('edit-payment-date').value = date.toISOString().split('T')[0];
            }
            
            // Show modal
            document.getElementById('edit-payment-modal').classList.add('active');
            document.getElementById('modal-overlay').classList.add('active');
        })
        .catch(error => {
            console.error('Error loading payment details:', error);
            showNotification('Lỗi khi tải thông tin thanh toán', 'error');
        });
}

function closeEditPaymentModal() {
    document.getElementById('edit-payment-modal').classList.remove('active');
    document.getElementById('modal-overlay').classList.remove('active');
    document.getElementById('edit-payment-form').reset();
}

// Handle edit payment form submission
document.addEventListener('DOMContentLoaded', function() {
    const editForm = document.getElementById('edit-payment-form');
    if (editForm) {
        editForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const paymentId = document.getElementById('edit-payment-id').value;
            const statusValue = document.getElementById('edit-status').value;
            
            // Normalize status to uppercase to match database ENUM
            const normalizedStatus = statusValue ? statusValue.trim().toUpperCase() : 'PENDING';
            
            const paymentData = {
                userId: parseInt(document.getElementById('edit-user-id').value),
                costId: parseInt(document.getElementById('edit-cost-id').value),
                amount: parseFloat(document.getElementById('edit-amount').value),
                method: document.getElementById('edit-method').value,
                transactionCode: document.getElementById('edit-transaction-code').value,
                paymentDate: document.getElementById('edit-payment-date').value || null,
                status: normalizedStatus
            };
            
            try {
                const response = await fetch(`/api/payments/${paymentId}`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(paymentData)
                });
                
                if (response.ok) {
                    showNotification('Cập nhật thanh toán thành công!', 'success');
                    closeEditPaymentModal();
                    loadPayments(); // Reload the table
                } else {
                    const error = await response.text();
                    showNotification('Lỗi: ' + error, 'error');
                }
            } catch (error) {
                console.error('Error updating payment:', error);
                showNotification('Lỗi khi cập nhật thanh toán', 'error');
            }
        });
    }
});

// ============================================
// DELETE PAYMENT FUNCTIONS
// ============================================

function deletePayment(paymentId) {
    if (!confirm('Bạn có chắc chắn muốn xóa thanh toán này?\n\nThao tác này không thể hoàn tác!')) {
        return;
    }
    
    fetch(`/api/payments/${paymentId}`, {
        method: 'DELETE'
    })
    .then(response => {
        if (response.ok) {
            showNotification('Xóa thanh toán thành công!', 'success');
            loadPayments(); // Reload the table
        } else {
            return response.text().then(error => {
                throw new Error(error);
            });
        }
    })
    .catch(error => {
        console.error('Error deleting payment:', error);
        showNotification('Lỗi khi xóa thanh toán: ' + error.message, 'error');
    });
}

// ============================================
// PRINT INVOICE FUNCTIONS
// ============================================

function printPaymentInvoice(paymentId) {
    // Fetch payment details
    fetch(`/api/payments/${paymentId}/details`)
        .then(response => response.json())
        .then(payment => {
            // Generate invoice HTML
            const invoiceHTML = generateInvoiceHTML(payment);
            document.getElementById('invoice-content').innerHTML = invoiceHTML;
            
            // Show modal
            document.getElementById('invoice-modal').classList.add('active');
            document.getElementById('modal-overlay').classList.add('active');
        })
        .catch(error => {
            console.error('Error loading payment for invoice:', error);
            showNotification('Lỗi khi tải thông tin thanh toán', 'error');
        });
}

function generateInvoiceHTML(payment) {
    const invoiceDate = new Date().toLocaleDateString('vi-VN');
    const paymentDate = payment.paymentDate ? new Date(payment.paymentDate).toLocaleDateString('vi-VN') : 'Chưa thanh toán';
    const statusText = getPaymentStatusText(payment.status);
    
    return `
        <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 800px; margin: 0 auto;">
            <!-- Header -->
            <div style="text-align: center; border-bottom: 3px solid #2196F3; padding-bottom: 1.5rem; margin-bottom: 2rem;">
                <h1 style="color: #2196F3; margin: 0; font-size: 2rem;">HÓA ĐƠN THANH TOÁN</h1>
                <p style="color: #666; margin: 0.5rem 0 0 0; font-size: 0.95rem;">Hệ thống quản lý chi phí xe điện</p>
            </div>
            
            <!-- Invoice Info -->
            <div style="display: flex; justify-content: space-between; margin-bottom: 2rem;">
                <div>
                    <p style="margin: 0.3rem 0; color: #333;"><strong>Số hóa đơn:</strong> #${payment.paymentId}</p>
                    <p style="margin: 0.3rem 0; color: #333;"><strong>Ngày lập:</strong> ${invoiceDate}</p>
                    <p style="margin: 0.3rem 0; color: #333;"><strong>Trạng thái:</strong> <span style="color: ${payment.status === 'PAID' ? '#4CAF50' : '#FF9800'}; font-weight: bold;">${statusText}</span></p>
                </div>
                <div style="text-align: right;">
                    <p style="margin: 0.3rem 0; color: #333;"><strong>User ID:</strong> #${payment.userId}</p>
                    <p style="margin: 0.3rem 0; color: #333;"><strong>Mã GD:</strong> ${payment.transactionCode || 'N/A'}</p>
                    <p style="margin: 0.3rem 0; color: #333;"><strong>Ngày TT:</strong> ${paymentDate}</p>
                </div>
            </div>
            
            <!-- Payment Details -->
            <div style="background: #f5f5f5; padding: 1.5rem; border-radius: 8px; margin-bottom: 2rem;">
                <h3 style="margin: 0 0 1rem 0; color: #333; font-size: 1.2rem;">Chi tiết thanh toán</h3>
                <table style="width: 100%; border-collapse: collapse;">
                    <thead>
                        <tr style="background: #e0e0e0;">
                            <th style="padding: 0.75rem; text-align: left; border: 1px solid #ccc;">Mô tả</th>
                            <th style="padding: 0.75rem; text-align: right; border: 1px solid #ccc;">Số tiền</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td style="padding: 0.75rem; border: 1px solid #ccc;">
                                <strong>Chi phí ID:</strong> #${payment.costId}<br>
                                <span style="color: #666; font-size: 0.9rem;">Phương thức: ${payment.method || 'Chưa xác định'}</span>
                            </td>
                            <td style="padding: 0.75rem; text-align: right; border: 1px solid #ccc; font-size: 1.1rem;">
                                <strong>${formatCurrency(payment.amount)}</strong>
                            </td>
                        </tr>
                    </tbody>
                    <tfoot>
                        <tr style="background: #2196F3; color: white;">
                            <td style="padding: 1rem; border: 1px solid #1976D2; font-size: 1.2rem;"><strong>TỔNG CỘNG</strong></td>
                            <td style="padding: 1rem; text-align: right; border: 1px solid #1976D2; font-size: 1.3rem;">
                                <strong>${formatCurrency(payment.amount)}</strong>
                            </td>
                        </tr>
                    </tfoot>
                </table>
            </div>
            
            <!-- Notes -->
            <div style="margin-bottom: 2rem; padding: 1rem; background: #fff3cd; border-left: 4px solid #ffc107; border-radius: 4px;">
                <p style="margin: 0; color: #856404; font-size: 0.9rem;">
                    <strong>Lưu ý:</strong> Vui lòng giữ hóa đơn này để đối chiếu. Mọi thắc mắc xin liên hệ bộ phận hỗ trợ.
                </p>
            </div>
            
            <!-- Footer -->
            <div style="text-align: center; padding-top: 1.5rem; border-top: 2px solid #e0e0e0; color: #666; font-size: 0.85rem;">
                <p style="margin: 0.3rem 0;">Cảm ơn bạn đã sử dụng dịch vụ!</p>
                <p style="margin: 0.3rem 0;">Hệ thống quản lý chi phí xe điện - EV Co-ownership System</p>
                <p style="margin: 0.3rem 0;">Email: support@evsharing.com | Hotline: 1900-xxxx</p>
            </div>
        </div>
    `;
}

function closeInvoiceModal() {
    document.getElementById('invoice-modal').classList.remove('active');
    document.getElementById('modal-overlay').classList.remove('active');
}

function printInvoice() {
    const invoiceContent = document.getElementById('invoice-content').innerHTML;
    const printWindow = window.open('', '_blank');
    printWindow.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
            <title>Hóa đơn thanh toán</title>
            <style>
                body { margin: 0; padding: 20px; }
                @media print {
                    body { margin: 0; }
                }
            </style>
        </head>
        <body>
            ${invoiceContent}
            <script>
                window.onload = function() {
                    window.print();
                    setTimeout(function() { window.close(); }, 100);
                }
            </script>
        </body>
        </html>
    `);
    printWindow.document.close();
}

function downloadInvoicePDF() {
    showNotification('Tính năng tải PDF đang được phát triển...', 'info');
    // TODO: Implement PDF download using jsPDF or similar library
    // For now, users can use the print function and "Save as PDF"
}

// ============ COST SPLIT MANAGEMENT ============
let customSplitMembers = [];

async function openCostSplitModal(costId) {
    try {
        // Load cost info
        const costResponse = await fetch(`${API.COSTS}/${costId}`);
        const cost = await costResponse.json();
        
        document.getElementById('split-cost-id').value = costId;
        document.getElementById('cost-split-modal-title').textContent = `Phân chia chi phí #${costId}`;
        
        // Display cost info
        document.getElementById('cost-split-info').innerHTML = `
            <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem;">
                <div>
                    <strong>Loại chi phí:</strong> ${getCostTypeName(cost.costType)}
                </div>
                <div>
                    <strong>Vehicle ID:</strong> #${cost.vehicleId}
                </div>
                <div>
                    <strong>Số tiền:</strong> 
                    <span style="color: var(--primary); font-weight: bold; font-size: 1.1rem;">
                        ${formatCurrency(cost.amount)}
                    </span>
                </div>
                <div>
                    <strong>Ngày tạo:</strong> ${formatDate(cost.createdAt)}
                </div>
            </div>
        `;
        
        // Load existing shares if any
        let existingShares = [];
        try {
            const sharesResponse = await fetch(`${API.COSTS}/${costId}/shares`);
            if (sharesResponse.ok) {
                existingShares = await sharesResponse.json();
            }
        } catch (e) {
            console.error('Error loading existing shares:', e);
        }
        
        // Reset form
        document.getElementById('split-method-select').value = '';
        document.getElementById('split-group-container').style.display = 'none';
        document.getElementById('split-usage-period').style.display = 'none';
        document.getElementById('split-custom-container').style.display = 'none';
        document.getElementById('split-preview-section').style.display = 'none';
        customSplitMembers = [];
        
        // Load groups for dropdown
        await loadGroupsForCostSplitModal();
        
        // Setup form handlers
        setupSplitFormHandlers();
        
        openModal('cost-split-modal');
        
    } catch (error) {
        console.error('Error opening split modal:', error);
        showNotification('Lỗi khi mở form phân chia', 'error');
    }
}

function setupSplitFormHandlers() {
    const methodSelect = document.getElementById('split-method-select');
    const groupContainer = document.getElementById('split-group-container');
    const usagePeriod = document.getElementById('split-usage-period');
    const customContainer = document.getElementById('split-custom-container');
    
    methodSelect.onchange = function() {
        const method = this.value;
        
        // Hide all optional fields
        groupContainer.style.display = 'none';
        usagePeriod.style.display = 'none';
        customContainer.style.display = 'none';
        document.getElementById('split-preview-section').style.display = 'none';
        
        if (method === 'BY_OWNERSHIP' || method === 'BY_USAGE' || method === 'EQUAL') {
            groupContainer.style.display = 'block';
            if (method === 'BY_USAGE') {
                usagePeriod.style.display = 'block';
            }
        } else if (method === 'CUSTOM') {
            customContainer.style.display = 'block';
            if (customSplitMembers.length === 0) {
                addCustomSplitMember();
            }
        }
    };
}

async function loadGroupsForCostSplitModal() {
    try {
        console.log('Loading groups for split modal...');
        const response = await fetch(API.GROUPS);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const groups = await response.json();
        console.log('Groups loaded for split modal:', groups);
        
        if (!Array.isArray(groups)) {
            console.error('Groups response is not an array:', groups);
            throw new Error('Invalid response format from server');
        }
        
        const select = document.getElementById('split-group-id');
        if (!select) {
            console.error('Split group select element not found');
            return;
        }
        
        if (groups.length === 0) {
            select.innerHTML = '<option value="">-- Không có nhóm nào --</option>';
            console.warn('No groups found in database');
            return;
        }
        
        select.innerHTML = '<option value="">-- Chọn nhóm --</option>' +
            groups.map(g => {
                const groupId = g.groupId || g.id;
                const groupName = g.groupName || g.name || `Nhóm #${groupId}`;
                return `<option value="${groupId}">${groupName}</option>`;
            }).join('');
        
        console.log(`Successfully loaded ${groups.length} groups for split modal`);
            
    } catch (error) {
        console.error('Error loading groups for split modal:', error);
        const select = document.getElementById('split-group-id');
        if (select) {
            select.innerHTML = '<option value="">-- Lỗi tải danh sách nhóm --</option>';
        }
    }
}

function addCustomSplitMember() {
    const index = customSplitMembers.length;
    customSplitMembers.push({ userId: '', percent: '' });
    
    const list = document.getElementById('split-members-list');
    const memberDiv = document.createElement('div');
    memberDiv.className = 'custom-split-member';
    memberDiv.style.cssText = 'display: grid; grid-template-columns: 2fr 1fr auto; gap: 1rem; align-items: end; margin-bottom: 1rem;';
    memberDiv.innerHTML = `
        <div>
            <label style="display: block; margin-bottom: 0.5rem; font-size: 0.875rem;">User ID</label>
            <input type="number" class="form-control split-user-id" placeholder="Nhập User ID" 
                   data-index="${index}" onchange="updateCustomSplitMember(${index})">
        </div>
        <div>
            <label style="display: block; margin-bottom: 0.5rem; font-size: 0.875rem;">Phần trăm (%)</label>
            <input type="number" class="form-control split-percent" placeholder="%" 
                   min="0" max="100" step="0.01" data-index="${index}" onchange="updateCustomSplitMember(${index})">
        </div>
        <div>
            <button type="button" class="btn btn-sm" style="background: var(--danger); color: white;" 
                    onclick="removeCustomSplitMember(${index})">
                <i class="fas fa-trash"></i>
            </button>
        </div>
    `;
    list.appendChild(memberDiv);
}

function updateCustomSplitMember(index) {
    const memberDiv = document.querySelector(`.custom-split-member input[data-index="${index}"].split-user-id`).closest('.custom-split-member');
    const userId = memberDiv.querySelector('.split-user-id').value;
    const percent = memberDiv.querySelector('.split-percent').value;
    
    customSplitMembers[index] = {
        userId: userId ? parseInt(userId) : '',
        percent: percent ? parseFloat(percent) : ''
    };
}

function removeCustomSplitMember(index) {
    customSplitMembers.splice(index, 1);
    renderCustomSplitMembers();
}

function renderCustomSplitMembers() {
    const list = document.getElementById('split-members-list');
    list.innerHTML = '';
    
    customSplitMembers.forEach((member, index) => {
        const memberDiv = document.createElement('div');
        memberDiv.className = 'custom-split-member';
        memberDiv.style.cssText = 'display: grid; grid-template-columns: 2fr 1fr auto; gap: 1rem; align-items: end; margin-bottom: 1rem;';
        memberDiv.innerHTML = `
            <div>
                <label style="display: block; margin-bottom: 0.5rem; font-size: 0.875rem;">User ID</label>
                <input type="number" class="form-control split-user-id" placeholder="Nhập User ID" 
                       value="${member.userId || ''}" data-index="${index}" onchange="updateCustomSplitMember(${index})">
            </div>
            <div>
                <label style="display: block; margin-bottom: 0.5rem; font-size: 0.875rem;">Phần trăm (%)</label>
                <input type="number" class="form-control split-percent" placeholder="%" 
                       value="${member.percent || ''}" min="0" max="100" step="0.01" 
                       data-index="${index}" onchange="updateCustomSplitMember(${index})">
            </div>
            <div>
                <button type="button" class="btn btn-sm" style="background: var(--danger); color: white;" 
                        onclick="removeCustomSplitMember(${index})">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        `;
        list.appendChild(memberDiv);
    });
}

async function previewCostSplit() {
    const costId = document.getElementById('split-cost-id').value;
    const method = document.getElementById('split-method-select').value;
    
    if (!method) {
        showNotification('Vui lòng chọn phương thức chia', 'warning');
        return;
    }
    
    try {
        // Load cost amount
        const costResponse = await fetch(`${API.COSTS}/${costId}`);
        const cost = await costResponse.json();
        const totalAmount = cost.amount;
        
        let shares = [];
        
        if (method === 'CUSTOM') {
            // Validate custom split
            const totalPercent = customSplitMembers.reduce((sum, m) => sum + (parseFloat(m.percent) || 0), 0);
            if (Math.abs(totalPercent - 100) > 0.01) {
                showNotification(`Tổng phần trăm phải bằng 100%. Hiện tại: ${totalPercent.toFixed(2)}%`, 'error');
                return;
            }
            
            // Calculate custom shares
            shares = customSplitMembers.map(m => ({
                userId: m.userId,
                percent: parseFloat(m.percent),
                amountShare: (totalAmount * parseFloat(m.percent)) / 100
            }));
            
        } else {
            // For other methods, call API to preview
            const groupId = document.getElementById('split-group-id').value;
            if (!groupId) {
                showNotification('Vui lòng chọn nhóm', 'warning');
                return;
            }
            
            const previewData = {
                costId: parseInt(costId),
                groupId: parseInt(groupId),
                splitMethod: method,
                month: method === 'BY_USAGE' ? parseInt(document.getElementById('split-month').value) : null,
                year: method === 'BY_USAGE' ? parseInt(document.getElementById('split-year').value) : null
            };
            
            try {
                const previewResponse = await fetch(`${API.AUTO_SPLIT}/preview`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(previewData)
                });
                
                if (previewResponse.ok) {
                    const result = await previewResponse.json();
                    shares = result.shares || [];
                } else {
                    throw new Error('Không thể preview');
                }
            } catch (e) {
                // If preview API doesn't exist, calculate manually for EQUAL
                if (method === 'EQUAL') {
                    // This would require getting group members - for now, show a message
                    showNotification('Vui lòng lưu phân chia để xem kết quả', 'info');
                    return;
                }
                throw e;
            }
        }
        
        // Display preview
        displaySplitPreview(shares, totalAmount);
        
    } catch (error) {
        console.error('Error previewing split:', error);
        showNotification('Lỗi khi xem trước phân chia: ' + error.message, 'error');
    }
}

function displaySplitPreview(shares, totalAmount) {
    const previewSection = document.getElementById('split-preview-section');
    const previewContent = document.getElementById('split-preview-content');
    
    const totalShared = shares.reduce((sum, s) => sum + (s.amountShare || 0), 0);
    
    previewContent.innerHTML = `
        <table style="width: 100%; border-collapse: collapse;">
            <thead>
                <tr style="background: var(--light);">
                    <th style="padding: 0.75rem; text-align: left; border: 1px solid var(--border);">User ID</th>
                    <th style="padding: 0.75rem; text-align: right; border: 1px solid var(--border);">Phần trăm</th>
                    <th style="padding: 0.75rem; text-align: right; border: 1px solid var(--border);">Số tiền</th>
                </tr>
            </thead>
            <tbody>
                ${shares.map(s => `
                    <tr>
                        <td style="padding: 0.75rem; border: 1px solid var(--border);">User #${s.userId}</td>
                        <td style="padding: 0.75rem; text-align: right; border: 1px solid var(--border);">${s.percent.toFixed(2)}%</td>
                        <td style="padding: 0.75rem; text-align: right; border: 1px solid var(--border); font-weight: bold;">
                            ${formatCurrency(s.amountShare)}
                        </td>
                    </tr>
                `).join('')}
                <tr style="background: var(--light); font-weight: bold;">
                    <td style="padding: 0.75rem; border: 1px solid var(--border);">TỔNG CỘNG</td>
                    <td style="padding: 0.75rem; text-align: right; border: 1px solid var(--border);">100%</td>
                    <td style="padding: 0.75rem; text-align: right; border: 1px solid var(--border);">
                        ${formatCurrency(totalShared)}
                    </td>
                </tr>
            </tbody>
        </table>
        ${Math.abs(totalShared - totalAmount) > 0.01 ? `
            <div style="margin-top: 1rem; padding: 1rem; background: #fff3cd; border-left: 4px solid #ffc107; border-radius: 4px;">
                <strong>Lưu ý:</strong> Có sự chênh lệch nhỏ do làm tròn số (${formatCurrency(Math.abs(totalShared - totalAmount))})
            </div>
        ` : ''}
    `;
    
    previewSection.style.display = 'block';
}

// Cost split form submit
document.addEventListener('DOMContentLoaded', function() {
    const splitForm = document.getElementById('cost-split-form');
    if (splitForm) {
        splitForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const costId = document.getElementById('split-cost-id').value;
            const method = document.getElementById('split-method-select').value;
            
            if (!method) {
                showNotification('Vui lòng chọn phương thức chia', 'warning');
                return;
            }
            
            try {
                let requestData;
                
                if (method === 'CUSTOM') {
                    // Validate custom split
                    const totalPercent = customSplitMembers.reduce((sum, m) => sum + (parseFloat(m.percent) || 0), 0);
                    if (Math.abs(totalPercent - 100) > 0.01) {
                        showNotification(`Tổng phần trăm phải bằng 100%. Hiện tại: ${totalPercent.toFixed(2)}%`, 'error');
                        return;
                    }
                    
                    // Prepare custom split request
                    const userIds = customSplitMembers.map(m => parseInt(m.userId));
                    const percentages = customSplitMembers.map(m => parseFloat(m.percent));
                    
                    requestData = {
                        userIds: userIds,
                        percentages: percentages
                    };
                    
                    const response = await fetch(`${API.COSTS}/${costId}/shares`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(requestData)
                    });
                    
                    if (response.ok) {
                        showNotification('Đã phân chia chi phí thành công!', 'success');
                        closeCostSplitModal();
                        // Reload costs if function exists (from admin-costs.js)
                        if (typeof window.loadCosts === 'function') {
                            window.loadCosts();
                        } else {
                            // Set flag for admin-costs.js to reload when page becomes visible
                            localStorage.setItem('reloadCosts', 'true');
                        }
                    } else {
                        const error = await response.text();
                        showNotification('Lỗi: ' + error, 'error');
                    }
                    
                } else {
                    // For other methods, use auto-split API
                    const groupId = document.getElementById('split-group-id').value;
                    if (!groupId) {
                        showNotification('Vui lòng chọn nhóm', 'warning');
                        return;
                    }
                    
                    requestData = {
                        costId: parseInt(costId),
                        vehicleId: null, // Will be determined from cost
                        groupId: parseInt(groupId),
                        splitMethod: method,
                        month: method === 'BY_USAGE' ? parseInt(document.getElementById('split-month').value) : null,
                        year: method === 'BY_USAGE' ? parseInt(document.getElementById('split-year').value) : null
                    };
                    
                    const response = await fetch(`${API.AUTO_SPLIT}/create-and-split`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(requestData)
                    });
                    
                    if (response.ok) {
                        showNotification('Đã tạo và phân chia chi phí thành công!', 'success');
                        closeCostSplitModal();
                        // Reload costs if function exists (from admin-costs.js)
                        if (typeof window.loadCosts === 'function') {
                            window.loadCosts();
                        } else {
                            // Set flag for admin-costs.js to reload when page becomes visible
                            localStorage.setItem('reloadCosts', 'true');
                        }
                    } else {
                        const error = await response.text();
                        showNotification('Lỗi: ' + error, 'error');
                    }
                }
                
            } catch (error) {
                console.error('Error splitting cost:', error);
                showNotification('Lỗi khi phân chia chi phí: ' + error.message, 'error');
            }
        });
    }
});

function closeCostSplitModal() {
    closeModal('cost-split-modal');
    document.getElementById('split-preview-section').style.display = 'none';
    customSplitMembers = [];
}

// ============ FUND MANAGEMENT SECTION ============
let currentFundId = null;
let currentFundGroupId = null;

async function loadFundManagementData() {
    try {
        // Load groups
        const groupsResponse = await fetch(API.GROUPS);
        const groups = groupsResponse.ok ? await groupsResponse.json() : [];
        
        // Populate group filter
        const groupFilter = document.getElementById('fund-group-filter');
        if (groupFilter) {
            groupFilter.innerHTML = '<option value="">Tất cả nhóm</option>' +
                groups.map(g => `<option value="${g.groupId}">${g.groupName}</option>`).join('');
        }
        
        // Load all funds with statistics
        const fundsData = await Promise.all(
            groups.map(async (group) => {
                try {
                    const fundResponse = await fetch(`${API.FUND}/group/${group.groupId}`);
                    if (fundResponse.ok) {
                        const fund = await fundResponse.json();
                        
                        // Fetch statistics for this fund
                        let statistics = null;
                        try {
                            const statsUrl = `/api/funds/${fund.fundId}/statistics`;
                            console.log(`Fetching statistics from: ${statsUrl}`);
                            const statsResponse = await fetch(statsUrl);
                            if (statsResponse.ok) {
                                statistics = await statsResponse.json();
                                console.log(`Statistics for fund ${fund.fundId}:`, statistics);
                            } else {
                                console.warn(`Failed to load statistics for fund ${fund.fundId}: ${statsResponse.status}`);
                            }
                        } catch (e) {
                            console.error(`Error loading statistics for fund ${fund.fundId}:`, e);
                        }
                        
                        // Fetch transaction count
                        let transactionCount = 0;
                        try {
                            const transUrl = `/api/funds/${fund.fundId}/transactions`;
                            const transResponse = await fetch(transUrl);
                            if (transResponse.ok) {
                                const transactions = await transResponse.json();
                                transactionCount = transactions ? transactions.length : 0;
                            }
                        } catch (e) {
                            console.error(`Error loading transactions for fund ${fund.fundId}:`, e);
                        }
                        
                        return { 
                            ...fund, 
                            groupName: group.groupName, 
                            groupId: group.groupId,
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
        console.log(`Loaded ${funds.length} funds with statistics`);
        
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
        showNotification('Lỗi khi tải dữ liệu quỹ', 'error');
    }
}

function applyFundFilters(funds) {
    const groupFilter = document.getElementById('fund-group-filter')?.value;
    const statusFilter = document.getElementById('fund-status-filter')?.value;
    const searchInput = document.getElementById('fund-search-input')?.value.toLowerCase();
    
    let filtered = funds;
    
    if (groupFilter) {
        filtered = filtered.filter(f => f.groupId == groupFilter);
    }
    
    if (statusFilter) {
        // Assuming fund has status field
        filtered = filtered.filter(f => f.status === statusFilter);
    }
    
    if (searchInput) {
        filtered = filtered.filter(f => 
            f.groupName?.toLowerCase().includes(searchInput) ||
            f.fundId?.toString().includes(searchInput)
        );
    }
    
    return filtered;
}

function updateFundStats(funds) {
    let totalBalance = 0;
    let totalDeposits = 0;
    let totalWithdraws = 0;
    // Don't reset pendingCount here - it will be updated by loadPendingWithdrawRequests()
    // Get current pending count if element exists, otherwise keep it 0
    const pendingCountElement = document.getElementById('pending-requests-count');
    let pendingCount = pendingCountElement ? parseInt(pendingCountElement.textContent) || 0 : 0;
    
    // Calculate stats from funds data (which now includes statistics)
    funds.forEach(fund => {
        totalBalance += fund.currentBalance || 0;
        totalDeposits += fund.totalDeposit || 0;
        totalWithdraws += fund.totalWithdraw || 0;
    });
    
    console.log(`Fund stats - Balance: ${totalBalance}, Deposits: ${totalDeposits}, Withdraws: ${totalWithdraws}`);
    
    // Update UI
    const balanceEl = document.getElementById('total-fund-balance');
    const countEl = document.getElementById('total-funds-count');
    const depositsEl = document.getElementById('total-deposits');
    const withdrawsEl = document.getElementById('total-withdraws');
    
    if (balanceEl) balanceEl.textContent = formatCurrency(totalBalance);
    if (countEl) countEl.textContent = funds.length;
    if (depositsEl) depositsEl.textContent = formatCurrency(totalDeposits);
    if (withdrawsEl) withdrawsEl.textContent = formatCurrency(totalWithdraws);
    // Don't update pending count here - it will be updated by loadPendingWithdrawRequests()
}

function renderFundsTable(funds) {
    const tbody = document.getElementById('funds-tbody');
    if (!tbody) return;
    
    if (funds.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align: center; padding: 2rem;">
                    Không có quỹ nào
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = funds.map(fund => `
        <tr>
            <td>${fund.fundId}</td>
            <td><strong>${fund.groupName || 'N/A'}</strong></td>
            <td><strong style="color: var(--primary);">${formatCurrency(fund.currentBalance || 0)}</strong></td>
            <td>${formatCurrency(fund.totalDeposit || 0)}</td>
            <td>${formatCurrency(fund.totalWithdraw || 0)}</td>
            <td>
                <span class="badge badge-info">${fund.transactionCount || 0}</span>
            </td>
            <td>
                <span class="badge badge-success">Hoạt động</span>
            </td>
            <td>
                <button class="btn btn-sm btn-primary" onclick="viewFundDetail(${fund.fundId}, ${fund.groupId})">
                    <i class="fas fa-eye"></i> Chi tiết
                </button>
                <button class="btn btn-sm btn-info" onclick="viewFundTransactions(${fund.fundId}, ${fund.groupId})">
                    <i class="fas fa-history"></i> Lịch sử
                </button>
            </td>
        </tr>
    `).join('');
}

async function viewFundDetail(fundId, groupId) {
    try {
        const response = await fetch(`${API.FUND}/group/${groupId}`);
        if (!response.ok) throw new Error('Failed to load fund');
        
        const fund = await response.json();
        
        // Also get summary
        let summary = null;
        try {
            const summaryResponse = await fetch(`/api/funds/${fundId}/summary`);
            if (summaryResponse.ok) {
                summary = await summaryResponse.json();
            }
        } catch (e) {
            console.warn('Could not load summary');
        }
        
        const content = `
            <div style="display: grid; gap: 1.5rem;">
                <div class="info-box" style="background: var(--light); padding: 1rem; border-radius: 8px;">
                    <h4 style="margin-bottom: 1rem;">Thông tin quỹ</h4>
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                        <div>
                            <strong>Fund ID:</strong> ${fund.fundId}
                        </div>
                        <div>
                            <strong>Group ID:</strong> ${fund.groupId}
                        </div>
                        <div>
                            <strong>Tên nhóm:</strong> ${fund.groupName || 'N/A'}
                        </div>
                        <div>
                            <strong>Số dư hiện tại:</strong> 
                            <span style="color: var(--primary); font-weight: bold; font-size: 1.2rem;">
                                ${formatCurrency(fund.currentBalance || 0)}
                            </span>
                        </div>
                        <div>
                            <strong>Ngày tạo:</strong> ${formatDate(fund.createdAt)}
                        </div>
                        <div>
                            <strong>Cập nhật:</strong> ${formatDate(fund.updatedAt)}
                        </div>
                    </div>
                </div>
                ${summary ? `
                <div class="info-box" style="background: var(--light); padding: 1rem; border-radius: 8px;">
                    <h4 style="margin-bottom: 1rem;">Thống kê</h4>
                    <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem;">
                        <div>
                            <div style="color: var(--text-light); font-size: 0.9rem;">Tổng nạp</div>
                            <div style="color: var(--success); font-weight: bold; font-size: 1.1rem;">
                                ${formatCurrency(summary.totalDeposit || 0)}
                            </div>
                        </div>
                        <div>
                            <div style="color: var(--text-light); font-size: 0.9rem;">Tổng rút</div>
                            <div style="color: var(--warning); font-weight: bold; font-size: 1.1rem;">
                                ${formatCurrency(summary.totalWithdraw || 0)}
                            </div>
                        </div>
                        <div>
                            <div style="color: var(--text-light); font-size: 0.9rem;">Giao dịch</div>
                            <div style="font-weight: bold; font-size: 1.1rem;">
                                ${summary.transactionCount || 0}
                            </div>
                        </div>
                    </div>
                </div>
                ` : ''}
            </div>
        `;
        
        document.getElementById('fund-detail-title').textContent = `Chi tiết quỹ - ${fund.groupName || 'N/A'}`;
        document.getElementById('fund-detail-content').innerHTML = content;
        openModal('fund-detail-modal');
        
    } catch (error) {
        console.error('Error loading fund detail:', error);
        showNotification('Lỗi khi tải chi tiết quỹ', 'error');
    }
}

async function viewFundTransactions(fundId, groupId) {
    currentFundId = fundId;
    currentFundGroupId = groupId;
    
    try {
        const response = await fetch(`/api/funds/${fundId}/transactions`);
        if (!response.ok) throw new Error('Failed to load transactions');
        
        const transactions = await response.json();
        
        document.getElementById('fund-transactions-title').textContent = `Lịch sử giao dịch - Fund #${fundId}`;
        loadFundTransactions(); // Will use currentFundId
        
        openModal('fund-transactions-modal');
    } catch (error) {
        console.error('Error loading transactions:', error);
        showNotification('Lỗi khi tải lịch sử giao dịch', 'error');
    }
}

async function loadFundTransactions() {
    if (!currentFundId) return;
    
    try {
        const typeFilter = document.getElementById('transaction-type-filter')?.value || '';
        const statusFilter = document.getElementById('transaction-status-filter')?.value || '';
        
        let url = `/api/funds/${currentFundId}/transactions`;
        const params = new URLSearchParams();
        if (typeFilter) params.append('type', typeFilter);
        if (statusFilter) params.append('status', statusFilter);
        if (params.toString()) url += '?' + params.toString();
        
        const response = await fetch(url);
        if (!response.ok) throw new Error('Failed to load transactions');
        
        const transactions = await response.json();
        
        const content = `
            <div style="max-height: 500px; overflow-y: auto;">
                ${transactions.length === 0 ? '<p style="text-align: center; padding: 2rem;">Không có giao dịch nào</p>' : `
                <table style="width: 100%; border-collapse: collapse;">
                    <thead>
                        <tr style="background: var(--light);">
                            <th style="padding: 0.75rem; text-align: left; border-bottom: 2px solid var(--border);">ID</th>
                            <th style="padding: 0.75rem; text-align: left; border-bottom: 2px solid var(--border);">Loại</th>
                            <th style="padding: 0.75rem; text-align: left; border-bottom: 2px solid var(--border);">Số tiền</th>
                            <th style="padding: 0.75rem; text-align: left; border-bottom: 2px solid var(--border);">User</th>
                            <th style="padding: 0.75rem; text-align: left; border-bottom: 2px solid var(--border);">Trạng thái</th>
                            <th style="padding: 0.75rem; text-align: left; border-bottom: 2px solid var(--border);">Ngày</th>
                            <th style="padding: 0.75rem; text-align: left; border-bottom: 2px solid var(--border);">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${transactions.map(t => `
                            <tr>
                                <td style="padding: 0.75rem; border-bottom: 1px solid var(--border);">${t.transactionId}</td>
                                <td style="padding: 0.75rem; border-bottom: 1px solid var(--border);">
                                    <span class="badge ${t.transactionType === 'DEPOSIT' ? 'badge-success' : 'badge-warning'}">
                                        ${t.transactionType === 'DEPOSIT' ? 'Nạp' : 'Rút'}
                                    </span>
                                </td>
                                <td style="padding: 0.75rem; border-bottom: 1px solid var(--border); font-weight: bold;">
                                    ${formatCurrency(t.amount || 0)}
                                </td>
                                <td style="padding: 0.75rem; border-bottom: 1px solid var(--border);">${t.userId || 'N/A'}</td>
                                <td style="padding: 0.75rem; border-bottom: 1px solid var(--border);">
                                    <span class="badge ${getStatusBadgeClass(t.status)}">
                                        ${getStatusText(t.status)}
                                    </span>
                                </td>
                                <td style="padding: 0.75rem; border-bottom: 1px solid var(--border);">${formatDate(t.date || t.createdAt || t.transactionDate)}</td>
                                <td style="padding: 0.75rem; border-bottom: 1px solid var(--border);">
                                    ${(t.status === 'PENDING' || t.status === 'Pending') ? `
                                        <button class="btn btn-sm btn-success" onclick="approveTransaction(${t.transactionId})">
                                            <i class="fas fa-check"></i>
                                        </button>
                                        <button class="btn btn-sm btn-danger" onclick="rejectTransaction(${t.transactionId})">
                                            <i class="fas fa-times"></i>
                                        </button>
                                    ` : '-'}
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
                `}
            </div>
        `;
        
        document.getElementById('fund-transactions-content').innerHTML = content;
        
    } catch (error) {
        console.error('Error loading transactions:', error);
        document.getElementById('fund-transactions-content').innerHTML = 
            '<p style="color: var(--danger); text-align: center; padding: 2rem;">Lỗi khi tải giao dịch</p>';
    }
}

async function loadPendingWithdrawRequests() {
    try {
        console.log('Loading pending withdraw requests...');
        // Load all groups and their pending requests
        const groupsResponse = await fetch(API.GROUPS);
        if (!groupsResponse.ok) {
            console.error('Failed to load groups:', groupsResponse.status);
            return;
        }
        const groups = await groupsResponse.json();
        console.log(`Found ${groups.length} groups`);
        
        const allPendingRequests = [];
        
        for (const group of groups) {
            try {
                const fundResponse = await fetch(`${API.FUND}/group/${group.groupId}`);
                if (fundResponse.ok) {
                    const fund = await fundResponse.json();
                    console.log(`Found fund ${fund.fundId} for group ${group.groupId}`);
                    
                    const pendingUrl = `/api/funds/${fund.fundId}/pending-requests`;
                    console.log(`Fetching pending requests from: ${pendingUrl}`);
                    
                    const requestsResponse = await fetch(pendingUrl);
                    if (requestsResponse.ok) {
                        const requests = await requestsResponse.json();
                        console.log(`Found ${requests.length} pending requests for fund ${fund.fundId}`);
                        
                        // Filter to ensure we only get Withdraw transactions with Pending status
                        requests.forEach(req => {
                            // Enum values: Deposit, Withdraw (capitalized) and Pending, Approved, Rejected, Completed
                            // Check transaction type (case-insensitive for safety)
                            const transactionType = req.transactionType || req.transaction_type;
                            const status = req.status || req.transaction_status;
                            
                            const isWithdraw = transactionType === 'Withdraw' || 
                                             transactionType === 'WITHDRAW' ||
                                             transactionType === 'withdraw';
                            const isPending = status === 'Pending' || 
                                            status === 'PENDING' ||
                                            status === 'pending';
                            
                            console.log(`Checking request ${req.transactionId}: type=${transactionType}, status=${status}, isWithdraw=${isWithdraw}, isPending=${isPending}`);
                            
                            if (isWithdraw && isPending) {
                                allPendingRequests.push({
                                    ...req,
                                    groupName: group.groupName,
                                    groupId: group.groupId,
                                    fundId: fund.fundId
                                });
                            }
                        });
                    } else {
                        console.warn(`Failed to load pending requests for fund ${fund.fundId}: ${requestsResponse.status}`);
                    }
                } else {
                    console.log(`No fund found for group ${group.groupId}`);
                }
            } catch (e) {
                console.error(`Error loading fund for group ${group.groupId}:`, e);
                // Skip groups without funds
            }
        }
        
        console.log(`Total pending requests found: ${allPendingRequests.length}`);
        
        // Update pending count
        const pendingCountElement = document.getElementById('pending-requests-count');
        if (pendingCountElement) {
            pendingCountElement.textContent = allPendingRequests.length;
        }
        
        // Render pending requests
        const container = document.getElementById('pending-requests-list');
        if (!container) {
            console.warn('pending-requests-list container not found');
            return;
        }
        
        if (allPendingRequests.length === 0) {
            container.innerHTML = '<p style="text-align: center; padding: 2rem; color: var(--text-light);">Không có yêu cầu nào chờ duyệt</p>';
            return;
        }
        
        container.innerHTML = allPendingRequests.map(req => `
            <div class="pending-request-card" style="background: white; padding: 1.5rem; border-radius: 8px; margin-bottom: 1rem; border: 1px solid var(--border);">
                <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 1rem;">
                    <div>
                        <h4 style="margin: 0 0 0.5rem 0;">Yêu cầu rút tiền #${req.transactionId}</h4>
                        <div style="color: var(--text-light); font-size: 0.9rem;">
                            Nhóm: <strong>${req.groupName}</strong> | User ID: ${req.userId}
                        </div>
                    </div>
                    <span class="badge badge-warning">Chờ duyệt</span>
                </div>
                <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; margin-bottom: 1rem;">
                    <div>
                        <div style="color: var(--text-light); font-size: 0.9rem;">Số tiền</div>
                        <div style="font-weight: bold; font-size: 1.2rem; color: var(--primary);">
                            ${formatCurrency(req.amount || 0)}
                        </div>
                    </div>
                    <div>
                        <div style="color: var(--text-light); font-size: 0.9rem;">Mục đích</div>
                        <div>${req.purpose || 'N/A'}</div>
                    </div>
                    <div>
                        <div style="color: var(--text-light); font-size: 0.9rem;">Ngày tạo</div>
                        <div>${formatDate(req.date || req.createdAt)}</div>
                    </div>
                </div>
                ${req.receiptUrl ? `
                    <div style="margin-bottom: 1rem;">
                        <a href="${req.receiptUrl}" target="_blank" class="btn btn-sm btn-outline">
                            <i class="fas fa-image"></i> Xem hóa đơn
                        </a>
                    </div>
                ` : ''}
                <div style="display: flex; gap: 0.5rem;">
                    <button class="btn btn-sm btn-success" onclick="openWithdrawRequestModal(${req.transactionId}, ${req.fundId}, true)">
                        <i class="fas fa-check"></i> Phê duyệt
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="openWithdrawRequestModal(${req.transactionId}, ${req.fundId}, false)">
                        <i class="fas fa-times"></i> Từ chối
                    </button>
                </div>
            </div>
        `).join('');
        
    } catch (error) {
        console.error('Error loading pending requests:', error);
        showNotification('Lỗi khi tải yêu cầu chờ duyệt', 'error');
    }
}

function openWithdrawRequestModal(transactionId, fundId, approve) {
    document.getElementById('request-id').value = transactionId;
    document.getElementById('request-action').value = approve ? 'approve' : 'reject';
    document.getElementById('withdraw-request-title').textContent = 
        approve ? 'Phê duyệt yêu cầu rút tiền' : 'Từ chối yêu cầu rút tiền';
    
    // Load transaction details
    fetch(`${API.FUND}/transactions/${transactionId}`)
        .then(res => res.json())
        .then(transaction => {
            const content = `
                <div class="info-box" style="background: var(--light); padding: 1rem; border-radius: 8px; margin-bottom: 1rem;">
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                        <div><strong>Transaction ID:</strong> ${transaction.transactionId}</div>
                        <div><strong>Fund ID:</strong> ${fundId}</div>
                        <div><strong>User ID:</strong> ${transaction.userId}</div>
                        <div><strong>Số tiền:</strong> <span style="color: var(--primary); font-weight: bold;">${formatCurrency(transaction.amount || 0)}</span></div>
                        <div><strong>Mục đích:</strong> ${transaction.purpose || 'N/A'}</div>
                        <div><strong>Ngày tạo:</strong> ${formatDate(transaction.createdAt)}</div>
                    </div>
                </div>
            `;
            document.getElementById('withdraw-request-content').innerHTML = content;
            openModal('withdraw-request-modal');
        })
        .catch(err => {
            console.error('Error loading transaction:', err);
            showNotification('Lỗi khi tải chi tiết giao dịch', 'error');
        });
}

function approveWithdrawRequest() {
    const transactionId = document.getElementById('request-id').value;
    const note = document.getElementById('request-note').value;
    
    fetch(`${API.FUND}/transactions/${transactionId}/approve`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ adminId: 1, note: note }) // TODO: Get adminId from session
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showNotification('Đã phê duyệt yêu cầu thành công!', 'success');
            closeWithdrawRequestModal();
            loadFundManagementData();
        } else {
            showNotification('Lỗi: ' + (data.error || 'Không thể phê duyệt'), 'error');
        }
    })
    .catch(err => {
        console.error('Error approving request:', err);
        showNotification('Lỗi khi phê duyệt yêu cầu', 'error');
    });
}

function rejectWithdrawRequest() {
    const transactionId = document.getElementById('request-id').value;
    const note = document.getElementById('request-note').value;
    
    fetch(`${API.FUND}/transactions/${transactionId}/reject`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ adminId: 1, reason: note }) // TODO: Get adminId from session
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showNotification('Đã từ chối yêu cầu thành công!', 'success');
            closeWithdrawRequestModal();
            loadFundManagementData();
        } else {
            showNotification('Lỗi: ' + (data.error || 'Không thể từ chối'), 'error');
        }
    })
    .catch(err => {
        console.error('Error rejecting request:', err);
        showNotification('Lỗi khi từ chối yêu cầu', 'error');
    });
}

async function approveTransaction(transactionId) {
    if (!confirm('Bạn có chắc chắn muốn phê duyệt giao dịch này?')) return;
    
    try {
        const response = await fetch(`${API.FUND}/transactions/${transactionId}/approve`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ adminId: 1 })
        });
        
        const data = await response.json();
        if (data.success) {
            showNotification('Đã phê duyệt thành công!', 'success');
            loadFundTransactions();
            loadPendingWithdrawRequests();
        } else {
            showNotification('Lỗi: ' + (data.error || 'Không thể phê duyệt'), 'error');
        }
    } catch (error) {
        console.error('Error approving transaction:', error);
        showNotification('Lỗi khi phê duyệt', 'error');
    }
}

async function rejectTransaction(transactionId) {
    const reason = prompt('Nhập lý do từ chối:');
    if (!reason) return;
    
    try {
        const response = await fetch(`${API.FUND}/transactions/${transactionId}/reject`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ adminId: 1, reason: reason })
        });
        
        const data = await response.json();
        if (data.success) {
            showNotification('Đã từ chối thành công!', 'success');
            loadFundTransactions();
            loadPendingWithdrawRequests();
        } else {
            showNotification('Lỗi: ' + (data.error || 'Không thể từ chối'), 'error');
        }
    } catch (error) {
        console.error('Error rejecting transaction:', error);
        showNotification('Lỗi khi từ chối', 'error');
    }
}

function closeFundDetailModal() {
    closeModal('fund-detail-modal');
}

function closeFundTransactionsModal() {
    closeModal('fund-transactions-modal');
    currentFundId = null;
    currentFundGroupId = null;
}

function closeWithdrawRequestModal() {
    closeModal('withdraw-request-modal');
    document.getElementById('withdraw-request-form').reset();
}

function exportFundReport() {
    showNotification('Chức năng xuất báo cáo đang được phát triển', 'info');
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount || 0);
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString('vi-VN');
}

function getStatusBadgeClass(status) {
    switch(status) {
        case 'APPROVED': return 'badge-success';
        case 'PENDING': return 'badge-warning';
        case 'REJECTED': return 'badge-danger';
        default: return 'badge-secondary';
    }
}

function getStatusText(status) {
    switch(status) {
        case 'APPROVED': return 'Đã duyệt';
        case 'PENDING': return 'Chờ duyệt';
        case 'REJECTED': return 'Từ chối';
        default: return status;
    }
}

