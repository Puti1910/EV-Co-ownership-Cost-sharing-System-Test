// Usage Tracking - Nhập km sử dụng
const USAGE_TRACKING_API = '/api/usage-tracking';
const GROUP_API = '/api/groups';

let currentMonth = new Date().getMonth() + 1;
let currentYear = new Date().getFullYear();
let currentGroupId = null;
let usageChart = null;

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', function() {
    initializeForm();
    loadGroups();
    setActiveNavItem();
});

// Initialize form with current month/year
function initializeForm() {
    const monthSelect = document.getElementById('selectMonth');
    const yearSelect = document.getElementById('selectYear');
    
    if (monthSelect) {
        monthSelect.value = currentMonth;
    }
    if (yearSelect) {
        yearSelect.value = currentYear;
    }
    
    // Set current year
    const currentYearValue = new Date().getFullYear();
    const yearOptions = [currentYearValue - 1, currentYearValue, currentYearValue + 1];
    yearSelect.innerHTML = yearOptions.map(y => 
        `<option value="${y}" ${y === currentYearValue ? 'selected' : ''}>${y}</option>`
    ).join('');
}

// Load groups from API
async function loadGroups() {
    try {
        const response = await fetch(GROUP_API);
        if (!response.ok) throw new Error('Failed to fetch groups');
        
        const groups = await response.json();
        const groupSelect = document.getElementById('selectGroup');
        
        if (groupSelect && groups && groups.length > 0) {
            groupSelect.innerHTML = groups.map(g => 
                `<option value="${g.groupId}">${g.groupName}</option>`
            ).join('');
            
            // Auto load first group
            currentGroupId = groups[0].groupId;
            loadUsageData();
        }
    } catch (error) {
        console.error('Error loading groups:', error);
        showNotification('Không thể tải danh sách nhóm', 'error');
    }
}

// Load usage data from API
async function loadUsageData() {
    const month = document.getElementById('selectMonth').value;
    const year = document.getElementById('selectYear').value;
    const groupId = document.getElementById('selectGroup').value;

    currentMonth = parseInt(month);
    currentYear = parseInt(year);
    currentGroupId = parseInt(groupId);

    try {
        showLoading();
        const response = await fetch(
            `${USAGE_TRACKING_API}/group/${groupId}?month=${month}&year=${year}`
        );
        
        if (!response.ok) {
            // If no data, show empty state
            if (response.status === 404) {
                await createDefaultUsageForGroup(groupId, month, year);
                return;
            }
            throw new Error('Failed to fetch usage data');
        }
        
        const usageData = await response.json();
        displayUsageData(usageData);
        calculatePercentages();
        updateStats();
        updateChart(usageData);
        
    } catch (error) {
        console.error('Error loading usage data:', error);
        showNotification('Không thể tải dữ liệu sử dụng', 'error');
    } finally {
        hideLoading();
    }
}

// Create default usage entries for all group members
async function createDefaultUsageForGroup(groupId, month, year) {
    try {
        // Get group members
        const groupResponse = await fetch(`${GROUP_API}/${groupId}`);
        if (!groupResponse.ok) throw new Error('Failed to fetch group');
        
        const group = await groupResponse.json();
        const members = group.members || [];
        
        // Create empty usage data for display
        const usageData = members.map(m => ({
            userId: m.userId,
            userName: `User ${m.userId}`,
            kmDriven: 0,
            percentage: 0
        }));
        
        displayUsageData(usageData);
        calculatePercentages();
        updateStats();
        updateChart(usageData);
        
    } catch (error) {
        console.error('Error creating default usage:', error);
        displayUsageData([]);
    }
}

// Display usage data in table
function displayUsageData(data) {
    const tbody = document.getElementById('usageTableBody');
    tbody.innerHTML = '';

    if (!data || data.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="text-center">
                    <div class="empty-state">
                        <i class="fas fa-inbox"></i>
                        <p>Chưa có dữ liệu sử dụng cho tháng này</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    data.forEach(item => {
        const row = `
            <tr data-user-id="${item.userId}">
                <td>
                    <div class="user-info">
                        <i class="fas fa-user-circle"></i>
                        <strong>${item.userName || 'User ' + item.userId}</strong>
                    </div>
                </td>
                <td>
                    <input type="number" class="form-control km-input" 
                           value="${item.kmDriven || 0}" min="0" step="0.1"
                           data-user-id="${item.userId}"
                           onchange="calculatePercentages()">
                </td>
                <td>
                    <span class="badge badge-primary percent-badge">${item.percentage || 0}%</span>
                </td>
                <td>
                    <input type="text" class="form-control note-input" 
                           value="${item.note || ''}" 
                           placeholder="Ghi chú...">
                </td>
                <td>
                    <button type="button" class="btn btn-sm btn-success" 
                            onclick="saveUserUsage(${item.userId})">
                        <i class="fas fa-save"></i>
                    </button>
                </td>
            </tr>
        `;
        tbody.innerHTML += row;
    });
}

// Calculate percentages
function calculatePercentages() {
    const inputs = document.querySelectorAll('.km-input');
    let total = 0;

    inputs.forEach(input => {
        total += parseFloat(input.value) || 0;
    });

    inputs.forEach(input => {
        const row = input.closest('tr');
        const percentBadge = row.querySelector('.percent-badge');
        const km = parseFloat(input.value) || 0;
        const percent = total > 0 ? ((km / total) * 100).toFixed(2) : 0;
        percentBadge.textContent = percent + '%';
    });

    document.getElementById('totalKmBadge').textContent = `Tổng: ${total.toFixed(1)} km`;
    updateStats();
}

// Update statistics
function updateStats() {
    const inputs = document.querySelectorAll('.km-input');
    let total = 0;
    let max = 0;
    let maxUser = '-';

    inputs.forEach(input => {
        const km = parseFloat(input.value) || 0;
        total += km;
        if (km > max) {
            max = km;
            maxUser = input.closest('tr').querySelector('.user-info strong').textContent;
        }
    });

    const avg = inputs.length > 0 ? (total / inputs.length).toFixed(1) : 0;

    document.getElementById('totalKmStat').textContent = total.toFixed(1) + ' km';
    document.getElementById('avgKmStat').textContent = avg + ' km';
    document.getElementById('maxUserStat').textContent = maxUser;
}

// Update chart
function updateChart(data) {
    const ctx = document.getElementById('usageChart');
    
    if (usageChart) {
        usageChart.destroy();
    }

    const labels = data.map(item => item.userName || 'User ' + item.userId);
    const values = data.map(item => item.kmDriven || 0);

    usageChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Km đã chạy',
                data: values,
                backgroundColor: [
                    'rgba(59, 130, 246, 0.8)',
                    'rgba(16, 185, 129, 0.8)',
                    'rgba(245, 158, 11, 0.8)',
                    'rgba(239, 68, 68, 0.8)',
                    'rgba(139, 92, 246, 0.8)'
                ],
                borderWidth: 0,
                borderRadius: 8
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
                            return value + ' km';
                        }
                    }
                }
            }
        }
    });
}

// Save single user usage
async function saveUserUsage(userId) {
    const row = document.querySelector(`tr[data-user-id="${userId}"]`);
    const kmInput = row.querySelector('.km-input');
    const kmDriven = parseFloat(kmInput.value) || 0;

    try {
        const response = await fetch(`${USAGE_TRACKING_API}/update-km`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                groupId: currentGroupId,
                userId: userId,
                month: currentMonth,
                year: currentYear,
                kmDriven: kmDriven
            })
        });

        if (!response.ok) throw new Error('Failed to save usage');

        showNotification(`Đã lưu km của User ${userId}`, 'success');
        
        // Reload to update percentages
        await loadUsageData();
        
    } catch (error) {
        console.error('Error saving usage:', error);
        showNotification('Không thể lưu dữ liệu', 'error');
    }
}

// Save all usage
async function saveAllUsage() {
    const rows = document.querySelectorAll('#usageTableBody tr[data-user-id]');
    const promises = [];

    for (const row of rows) {
        const userId = parseInt(row.dataset.userId);
        const kmInput = row.querySelector('.km-input');
        const kmDriven = parseFloat(kmInput.value) || 0;

        const promise = fetch(`${USAGE_TRACKING_API}/update-km?groupId=${currentGroupId}&userId=${userId}&month=${currentMonth}&year=${currentYear}&kmDriven=${kmDriven}`, {
            method: 'PUT'
        });
        
        promises.push(promise);
    }

    try {
        showLoading();
        await Promise.all(promises);
        showNotification('Đã lưu tất cả dữ liệu sử dụng!', 'success');
        await loadUsageData();
    } catch (error) {
        console.error('Error saving all usage:', error);
        showNotification('Có lỗi khi lưu dữ liệu', 'error');
    } finally {
        hideLoading();
    }
}

// Reset form
function resetForm() {
    loadUsageData();
}

// Show notification
function showNotification(message, type = 'info') {
    // Simple alert for now, can be replaced with better notification
    const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
    alert(icon + ' ' + message);
}

// Loading state
function showLoading() {
    document.body.style.cursor = 'wait';
}

function hideLoading() {
    document.body.style.cursor = 'default';
}

