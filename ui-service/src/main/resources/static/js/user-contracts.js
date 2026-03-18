(function () {
    const API_ENDPOINTS = {
        OVERVIEW: '/user/contracts/api'
        // Future endpoints for upload / detail can be added here
    };

    const state = {
        contracts: [],
        filteredContracts: [],
        stats: { total: 0, active: 0, pending: 0, archived: 0 },
        filters: {
            search: '',
            status: 'all',
            vehicleType: 'all'
        },
        loading: false,
        initialized: false
    };

    const dom = {};

    function cacheDom() {
        dom.root = document.getElementById('contracts-page');
        if (!dom.root) {
            return false;
        }
        dom.stats = document.getElementById('contracts-stats');
        dom.search = document.getElementById('contract-search');
        dom.statusFilter = document.getElementById('status-filter');
        dom.vehicleFilter = document.getElementById('vehicle-filter');
        dom.refreshBtn = document.getElementById('btn-refresh-contracts');
        dom.tableBody = document.getElementById('contracts-table-body');
        dom.tableMeta = document.getElementById('contracts-table-meta');
        dom.recentList = document.getElementById('recent-contracts');
        dom.signAllBtn = document.getElementById('btn-sign-pending');
        dom.openGroupBtn = document.getElementById('btn-open-group-page');
        dom.drawer = document.getElementById('contract-detail-drawer');
        dom.drawerBody = document.getElementById('drawer-body');
        dom.drawerCode = document.getElementById('drawer-contract-code');
        dom.drawerTitle = document.getElementById('drawer-contract-title');
        dom.drawerSignBtn = document.getElementById('btn-sign-contract');
        dom.drawerClose = document.getElementById('btn-close-drawer');
        return true;
    }

    async function loadData(showToastOnSuccess = false) {
        if (state.loading) {
            return;
        }
        try {
            state.loading = true;
            setLoadingState(true);
            const response = await authenticatedFetchWrapper(API_ENDPOINTS.OVERVIEW);
            if (!response.ok) {
                throw new Error('Không thể tải danh sách hợp đồng');
            }
            const data = await response.json();
            state.contracts = Array.isArray(data.contracts) ? data.contracts : [];
            state.stats = data.stats || state.stats;
            state.requiresSignatureBeforeJoin = !!data.requiresSignatureBeforeJoin;

            populateFilters(data.filters);
            applyFilters();
            renderStats();
            renderRecentContracts();
            updateActionsAvailability();

            if (showToastOnSuccess) {
                notify('Đã làm mới danh sách hợp đồng', 'success');
            }
        } catch (error) {
            console.error('[UserContracts] loadData error', error);
            notify(error.message || 'Không thể tải hợp đồng', 'error');
            showErrorState(error.message);
        } finally {
            state.loading = false;
            setLoadingState(false);
        }
    }

    function authenticatedFetchWrapper(url, options = {}) {
        if (typeof window.authenticatedFetch === 'function') {
            return window.authenticatedFetch(url, options);
        }
        return fetch(url, options);
    }

    function setLoadingState(isLoading) {
        dom.refreshBtn?.classList.toggle('spinning', isLoading);
        if (isLoading && dom.tableBody) {
            dom.tableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="empty-state">
                        <div class="loading-state">
                            <i class="fas fa-spinner fa-spin"></i>
                            <p>Đang tải hợp đồng...</p>
                        </div>
                    </td>
                </tr>`;
        }
    }

    function showErrorState(message) {
        if (!dom.tableBody) return;
        dom.tableBody.innerHTML = `
            <tr>
                <td colspan="6" class="empty-state">
                    <div class="loading-state">
                        <i class="fas fa-circle-exclamation" style="color:#f87171;"></i>
                        <p>${message || 'Không thể tải dữ liệu hợp đồng.'}</p>
                        <button class="btn btn-outline" id="contracts-retry-btn">
                            <i class="fas fa-sync"></i> Thử lại
                        </button>
                    </div>
                </td>
            </tr>`;
        document.getElementById('contracts-retry-btn')?.addEventListener('click', () => loadData(true));
    }

    function populateFilters(filters) {
        if (!filters) return;
        const statusOptions = filters.statuses || [];
        const vehicleOptions = filters.vehicleTypes || [];

        if (dom.statusFilter) {
            dom.statusFilter.innerHTML = statusOptions.map(opt =>
                `<option value="${opt.value}">${opt.label}</option>`
            ).join('');
        }

        if (dom.vehicleFilter) {
            dom.vehicleFilter.innerHTML = vehicleOptions.map(opt =>
                `<option value="${opt.value}">${opt.label}</option>`
            ).join('');
        }
    }

    function applyFilters() {
        const search = state.filters.search.trim().toLowerCase();
        state.filteredContracts = state.contracts.filter(contract => {
            const matchesStatus = state.filters.status === 'all'
                || contract.status === state.filters.status;

            const matchesVehicle = state.filters.vehicleType === 'all'
                || (contract.vehicleType || '').toLowerCase() === state.filters.vehicleType.toLowerCase();

            const matchesSearch = !search
                || (contract.contractCode || '').toLowerCase().includes(search)
                || (contract.groupName || '').toLowerCase().includes(search)
                || (contract.vehicleName || '').toLowerCase().includes(search);

            return matchesStatus && matchesVehicle && matchesSearch;
        });

        renderTable();
    }

    function renderStats() {
        if (!dom.stats) return;
        const cards = [
            {
                label: 'Tổng hợp đồng',
                value: state.stats.total || 0,
                icon: 'fa-file-contract',
                color: '#3b82f6'
            },
            {
                label: 'Đang hoạt động',
                value: state.stats.active || 0,
                icon: 'fa-circle-check',
                color: '#10b981'
            },
            {
                label: 'Chờ ký',
                value: state.stats.pending || 0,
                icon: 'fa-clock',
                color: '#f59e0b'
            },
            {
                label: 'Đã kết thúc',
                value: state.stats.archived || 0,
                icon: 'fa-circle-xmark',
                color: '#94a3b8'
            }
        ];

        dom.stats.innerHTML = cards.map(card => `
            <div class="stat-card">
                <div class="stat-icon" style="background:${card.color}">
                    <i class="fas ${card.icon}"></i>
                </div>
                <div class="stat-info">
                    <span>${card.label}</span>
                    <strong>${card.value}</strong>
                </div>
            </div>
        `).join('');
    }

    function renderTable() {
        if (!dom.tableBody) return;
        if (!state.filteredContracts.length) {
            dom.tableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="empty-state">
                        <i class="fas fa-folder-open" style="font-size:36px;color:#cbd5f5;"></i>
                        <p>Không có hợp đồng phù hợp với bộ lọc hiện tại.</p>
                    </td>
                </tr>`;
            dom.tableMeta.textContent = '0 hợp đồng';
            return;
        }

        dom.tableMeta.textContent = `Hiển thị ${state.filteredContracts.length} / ${state.contracts.length} hợp đồng`;

        dom.tableBody.innerHTML = state.filteredContracts.map(contract => `
            <tr data-contract-id="${contract.contractId}">
                <td>
                    <div class="contract-id">${contract.contractCode || (contract.status === 'no-contract' ? 'Chưa có hợp đồng' : '-')}</div>
                    ${contract.contractId ? `<div class="contract-meta">#${contract.contractId}</div>` : ''}
                </td>
                <td>
                    <div class="contract-card">
                        <strong>${contract.groupName || 'Nhóm chưa xác định'}</strong>
                        <span>${contract.groupDescription || 'Chưa có mô tả'}</span>
                    </div>
                </td>
                <td>
                    <div class="contract-card">
                        <strong>${contract.vehicleName || '-'}</strong>
                        <span>${contract.vehicleType || ''} · ${contract.vehiclePlate || ''}</span>
                    </div>
                </td>
                <td>
                    <div>${formatDate(contract.creationDate)}</div>
                    <div class="contract-meta">Ký: ${contract.signedDate ? formatDate(contract.signedDate) : 'Chưa ký'}</div>
                </td>
                <td>
                    <span class="status-pill ${contract.statusBadge}">
                        ${contract.statusLabel}
                    </span>
                </td>
                <td>
                    <div class="action-buttons">
                        ${renderActionButtons(contract)}
                    </div>
                </td>
            </tr>
        `).join('');
    }

    function renderActionButtons(contract) {
        const actions = [];
        
        // Xử lý nút ký hợp đồng
        if (contract.status === 'no-contract') {
            // Chưa có hợp đồng: hiển thị nút "Tạo và ký hợp đồng"
            actions.push(`<button class="btn btn-primary" data-action="sign" data-group-id="${contract.groupId}">
                <i class="fas fa-file-signature"></i> Tạo và ký hợp đồng
            </button>`);
        } else if (contract.status === 'pending') {
            // Đã có hợp đồng nhưng chưa ký
            actions.push(`<button class="btn btn-primary" data-action="sign" data-group-id="${contract.groupId}" data-contract-id="${contract.contractId}">
                <i class="fas fa-pen"></i> Ký ngay
            </button>`);
        }

        // Nút tham gia nhóm (chỉ bật khi đã ký và chưa tham gia)
        const joinDisabled = !(contract.canJoinGroup && state.requiresSignatureBeforeJoin);
        actions.push(`<button class="btn ${joinDisabled ? 'btn-outline' : 'btn'}" data-action="join" data-group-id="${contract.groupId}" ${joinDisabled ? 'disabled' : ''}>
            <i class="fas fa-users"></i> Tham gia nhóm
        </button>`);

        return actions.join('');
    }

    function renderRecentContracts() {
        if (!dom.recentList) return;
        const latest = [...state.contracts]
            .sort((a, b) => new Date(b.creationDate || 0) - new Date(a.creationDate || 0))
            .slice(0, 3);

        if (!latest.length) {
            dom.recentList.innerHTML = '<li>Chưa có hợp đồng nào.</li>';
            return;
        }

        dom.recentList.innerHTML = latest.map(contract => `
            <li class="contract-card">
                <strong>${contract.contractCode || '-'}</strong>
                <span>${contract.groupName || 'Nhóm chưa xác định'}</span>
                <span style="font-size:12px;color:#9ca3af;">${formatDate(contract.creationDate)}</span>
            </li>
        `).join('');
    }

    function updateActionsAvailability() {
        if (!dom.signAllBtn) return;
        // Đếm cả pending và no-contract
        const pendingCount = state.contracts.filter(c => 
            c.status === 'pending' || c.status === 'no-contract'
        ).length;
        dom.signAllBtn.disabled = pendingCount === 0;
        dom.signAllBtn.textContent = pendingCount > 0
            ? `Ký ${pendingCount} hợp đồng chờ`
            : 'Không có hợp đồng chờ';
    }

    function bindEvents() {
        dom.search?.addEventListener('input', (e) => {
            state.filters.search = e.target.value || '';
            applyFilters();
        });
        dom.statusFilter?.addEventListener('change', (e) => {
            state.filters.status = e.target.value;
            applyFilters();
        });
        dom.vehicleFilter?.addEventListener('change', (e) => {
            state.filters.vehicleType = e.target.value;
            applyFilters();
        });
        dom.refreshBtn?.addEventListener('click', () => loadData(true));
        dom.signAllBtn?.addEventListener('click', bulkSignPending);
        dom.openGroupBtn?.addEventListener('click', () => window.location.href = '/user/groups');
        dom.drawerClose?.addEventListener('click', closeDrawer);

        dom.tableBody?.addEventListener('click', handleTableAction);
        dom.drawerSignBtn?.addEventListener('click', () => {
            const contractId = dom.drawerSignBtn.dataset.contractId;
            if (contractId) {
                signContract(contractId);
            }
        });
    }

    async function bulkSignPending() {
        // Lấy tất cả hợp đồng cần ký (pending hoặc no-contract)
        const contractsToSign = state.contracts.filter(c => 
            c.status === 'pending' || c.status === 'no-contract'
        );
        if (!contractsToSign.length) return;
        if (!confirm(`Bạn có chắc muốn ký ${contractsToSign.length} hợp đồng?`)) {
            return;
        }
        for (const contract of contractsToSign) {
            if (contract.groupId) {
                await signContractByGroup(contract.groupId, { silent: true });
            }
        }
        await loadData();
        notify('Đã ký tất cả hợp đồng chờ.', 'success');
    }

    function handleTableAction(event) {
        const button = event.target.closest('[data-action]');
        if (!button) return;
        const contractId = button.dataset.id;
        const groupId = button.dataset.groupId;
        console.log('[UserContracts] handleTableAction', {
            action: button.dataset.action,
            contractId,
            groupId
        });
        switch (button.dataset.action) {
            case 'sign':
                // Sử dụng groupId để ký hợp đồng (API mới)
                if (groupId) {
                    signContractByGroup(groupId);
                } else if (contractId) {
                    // Fallback cho trường hợp cũ
                    signContract(contractId);
                }
                break;
            case 'join':
                console.log('[UserContracts] Join button clicked', { groupId, contractId, stateRequiresSignature: state.requiresSignatureBeforeJoin });
                if (!groupId) {
                    console.warn('[UserContracts] Missing groupId for join action');
                    notify('Không tìm thấy thông tin nhóm để tham gia.', 'error');
                    return;
                }

                const numericGroupId = Number(groupId);
                if (Number.isNaN(numericGroupId) || numericGroupId <= 0) {
                    console.warn('[UserContracts] Invalid groupId for join action', groupId);
                    notify('ID nhóm không hợp lệ.', 'error');
                    return;
                }

                if (typeof window.openJoinGroupModal === 'function') {
                    window.openJoinGroupModal(numericGroupId);
                    if (typeof window.setTimeout === 'function') {
                        // Focus the ownership input so user can proceed immediately
                        setTimeout(() => {
                            document.getElementById('joinOwnershipPercent')?.focus();
                        }, 250);
                    }
                } else if (typeof openJoinGroupModal === 'function') {
                    openJoinGroupModal(numericGroupId);
                } else {
                    console.warn('[UserContracts] openJoinGroupModal is not available, falling back to user groups page');
                    window.location.href = `/user/groups?groupId=${numericGroupId}`;
                }
                break;
            default:
                break;
        }
    }

    function openDrawer(contractId) {
        const contract = state.contracts.find(c => `${c.contractId}` === `${contractId}`);
        if (!contract || !dom.drawer) return;
        dom.drawer.hidden = false;
        dom.drawerCode.textContent = contract.contractCode || `#${contract.contractId}`;
        dom.drawerTitle.textContent = contract.groupName || 'Chi tiết hợp đồng';
        dom.drawerBody.innerHTML = `
            <div class="contract-card">
                <strong>Nhóm xe</strong>
                <span>${contract.groupName || '-'}</span>
            </div>
            <div class="contract-card">
                <strong>Trạng thái</strong>
                <span>${contract.statusLabel}</span>
            </div>
            <div class="contract-card">
                <strong>Xe đại diện</strong>
                <span>${contract.vehicleName || '-'} · ${contract.vehiclePlate || ''}</span>
            </div>
        `;
        dom.drawerSignBtn.dataset.contractId = contract.contractId;
        dom.drawerSignBtn.hidden = contract.status !== 'pending';
    }

    function closeDrawer() {
        if (dom.drawer) {
            dom.drawer.hidden = true;
        }
    }

    /**
     * Ký hợp đồng theo groupId (API mới: tự động tạo hợp đồng nếu chưa có)
     */
    async function signContractByGroup(groupId, options = {}) {
        if (!groupId) {
            notify('Không tìm thấy thông tin nhóm.', 'error');
            return;
        }
        try {
            const response = await authenticatedFetchWrapper(`/user/contracts/api/${groupId}/sign`, {
                method: 'PUT'
            });
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || 'Không thể ký hợp đồng, vui lòng thử lại.');
            }
            const result = await response.json();
            if (!options.silent) {
                notify(result.message || 'Đã ký hợp đồng thành công.', 'success');
            }
            closeDrawer();
            await loadData();
        } catch (error) {
            console.error('[UserContracts] signContractByGroup error', error);
            notify(error.message || 'Không thể ký hợp đồng', 'error');
        }
    }

    async function signContract(contractId, options = {}) {
        if (!contractId) return;
        const contract = state.contracts.find(c => `${c.contractId}` === `${contractId}`);
        if (contract && contract.groupId) {
            return signContractByGroup(contract.groupId, options);
        }
        notify('Không xác định được nhóm để ký hợp đồng.', 'error');
    }

    function navigateToGroup(contractId) {
        const contract = state.contracts.find(c => `${c.contractId}` === `${contractId}`);
        if (!contract || !contract.groupId) {
            notify('Không tìm thấy thông tin nhóm.', 'error');
            return;
        }
        window.location.href = `/user/groups?groupId=${contract.groupId}`;
    }

    function formatDate(value) {
        if (!value) return '-';
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return '-';
        return date.toLocaleDateString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        });
    }

    function notify(message, type = 'info') {
        if (typeof window.showToast === 'function') {
            window.showToast(message, type);
        } else {
            console.log(`[${type}] ${message}`);
        }
    }

    function init() {
        if (state.initialized) return;
        if (!cacheDom()) return;
        bindEvents();
        loadData();
        state.initialized = true;
    }

    window.loadContractsPage = init;

    document.addEventListener('DOMContentLoaded', () => {
        if (window.location.pathname.includes('/user/contracts')) {
            init();
        }
    });
})();

