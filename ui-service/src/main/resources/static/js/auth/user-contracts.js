// user-contracts.js
// (File user-guard.js đã được chèn vào <head> để bảo vệ)

document.addEventListener('DOMContentLoaded', function() {
    const token = localStorage.getItem('jwtToken');

    // --- (Lệnh gọi checkAuthAndLoadUser() đã được XÓA khỏi đây) ---
    // (File auth-utils.js sẽ tự động chạy)

    const contractList = document.getElementById('contract-list');
    const statsGrid = document.getElementById('stats-grid');
    const statusMessage = document.getElementById('status-message');
    const tabs = document.querySelectorAll('.tab-item');

    let currentStatusFilter = 'PENDING';
    let allContracts = [];

    // DỮ LIỆU MẪU (Sẽ thay bằng API thật)
    const MOCK_CONTRACTS = [
        { id: 1, title: 'HĐ Đồng sở hữu Vinfast VF8', vehicle: 'VF8 2024', plate: '29A-12345', img: 'https://via.placeholder.com/40/FF0000/FFFFFF?text=TC', date: '15/03/2024', time: '10:30 AM', duration: '24 tháng', expiry: '15/03/2026', status: 'PENDING' },
        { id: 2, title: 'HĐ Đồng sở hữu Vinfast VF9', vehicle: 'VF9 2023', plate: '30B-87890', img: 'https://via.placeholder.com/40/0000FF/FFFFFF?text=HC', date: '2025-09-15', time: '02:15 PM', duration: '36 tháng', expiry: '22/03/2027', status: 'ACTIVE' },
        { id: 3, title: 'HĐ Thuê xe VF E34', vehicle: 'VF E34', plate: '51F-11111', img: 'https://via.placeholder.com/40/000000/FFFFFF?text=BX', date: '2024-01-10', time: '09:45 AM', duration: '12 tháng', expiry: '28/03/2025', status: 'EXPIRED' },
    ];

    // 1. Hàm tải Thống kê
    function loadStats() {
        const total = MOCK_CONTRACTS.length;
        const active = MOCK_CONTRACTS.filter(c => c.status === 'ACTIVE').length;
        const pending = MOCK_CONTRACTS.filter(c => c.status === 'PENDING').length;
        const expired = MOCK_CONTRACTS.filter(c => c.status === 'EXPIRED').length;

        statsGrid.innerHTML = `
            <div class="stat-card">
                <div class="stat-icon blue"><i class="fas fa-file-alt"></i></div>
                <div class="stat-info"><h3>Tổng hợp đồng</h3><span>${total}</span></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon green"><i class="fas fa-check-circle"></i></div>
                <div class="stat-info"><h3>Đang hoạt động</h3><span>${active}</span></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon orange"><i class="fas fa-clock"></i></div>
                <div class="stat-info"><h3>Chờ ký</h3><span>${pending}</span></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon gray"><i class="fas fa-times-circle"></i></div>
                <div class="stat-info"><h3>Đã kết thúc</h3><span>${expired}</span></div>
            </div>
        `;
    }

    // 2. Hàm render Bảng (đã sửa lại để gọi tableBody)
    function renderTable(data) {
        const tableBody = document.getElementById('contract-table-body');
        tableBody.innerHTML = '';

        const filteredData = data.filter(c => c.status === currentStatusFilter);

        if (filteredData.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="6" style="text-align: center;">Không tìm thấy hợp đồng nào.</td></tr>';
            return;
        }

        filteredData.forEach(contract => {
            const row = document.createElement('tr');

            let statusClass = contract.status.toLowerCase();
            let statusText = contract.status === 'ACTIVE' ? 'Đang hoạt động' : (contract.status === 'PENDING' ? 'Chờ ký' : 'Đã kết thúc');

            let actions = `
                <a href="#" title="Tải xuống"><i class="fas fa-download"></i></a>
                <a href="#" title="Chi tiết xe"><i class="fas fa-car"></i></a>
            `;
            if (contract.status === 'PENDING') {
                actions += `<a href="#" class="action-sign" data-id="${contract.id}" title="Ký hợp đồng"><i class="fas fa-edit"></i></a>`;
            } else {
                actions += `<a href="#" title="Xem chi tiết"><i class="fas fa-eye"></i></a>`;
            }
            actions += `<a href="#" title="Thêm..."><i class="fas fa-ellipsis-v"></i></a>`;

            row.innerHTML = `
                <td>
                    <div class="contract-id"><h4>${contract.id}</h4><p>${contract.title}</p></div>
                </td>
                <td>
                    <div class="vehicle-info">
                        <img src="${contract.img}" alt="Xe">
                        <div><span>${contract.vehicle}</span><p>${contract.plate}</p></div>
                    </div>
                </td>
                <td>${contract.date}<br><small>${contract.time}</small></td>
                <td>${contract.duration}<br><small>Đến ${contract.expiry}</small></td>
                <td><span class="status-pill ${statusClass}">${statusText}</span></td>
                <td class="action-icons">${actions}</td>
            `;
            tableBody.appendChild(row);
        });
    }

    // 3. Xử lý sự kiện (Filter, Ký)
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            currentStatusFilter = tab.dataset.status;
            renderTable(MOCK_DATA);
        });
    });

    document.getElementById('contract-table-body').addEventListener('click', function(e) {
        const signButton = e.target.closest('.action-sign');
        if (signButton) {
            e.preventDefault();
            const contractId = signButton.dataset.id;
            if (confirm(`Bạn có chắc muốn ký hợp đồng ${contractId} không?`)) {
                signContract(contractId);
            }
        }
    });

    // 4. Hàm ký hợp đồng (Giả lập)
    async function signContract(contractId) {
        statusMessage.className = 'status-message success';
        statusMessage.textContent = 'Đang ký hợp đồng...';
        statusMessage.style.display = 'block';

        setTimeout(() => {
            statusMessage.textContent = `Hợp đồng #${contractId} đã được ký thành công!`;
            const signed = MOCK_CONTRACTS.find(c => c.id == contractId);
            if(signed) signed.status = 'ACTIVE';
            loadStats();
            renderTable(MOCK_CONTRACTS);
        }, 1000);
    }

    // 5. Tải dữ liệu ban đầu
    function loadData() {
        allContracts = MOCK_CONTRACTS;
        loadStats();
        renderTable(allContracts);
    }

    loadData();
});