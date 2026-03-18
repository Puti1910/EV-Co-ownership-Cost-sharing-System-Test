const CONTRACT_API_URL = 'http://localhost:8084/api/legalcontracts';
const GROUP_API_URL = 'http://localhost:8084/api/vehicle-groups';
let currentFilter = 'all';
let contracts = [];
let groups = [];
let selectedContractId = null;

let signatureCanvas = null;
let signatureCtx = null;
let isDrawing = false;

document.addEventListener('DOMContentLoaded', function () {
    initializeSignatureCanvas();
    initializeListeners();
    loadGroups();
    loadContracts();
});

function initializeSignatureCanvas() {
    signatureCanvas = document.getElementById('signature-canvas');
    if (signatureCanvas) {
        signatureCtx = signatureCanvas.getContext('2d');
        signatureCanvas.width = signatureCanvas.offsetWidth;
        signatureCanvas.height = signatureCanvas.offsetHeight;
        signatureCtx.strokeStyle = '#000';
        signatureCtx.lineWidth = 2;
        signatureCtx.lineCap = 'round';
        signatureCtx.lineJoin = 'round';
    }
}

function initializeListeners() {
    document.querySelectorAll('.filter-tab').forEach(tab => {
        tab.addEventListener('click', function () {
            const active = document.querySelector('.filter-tab.active');
            if (active) {
                active.classList.remove('active');
            }
            this.classList.add('active');
            currentFilter = this.dataset.filter;
            renderContractsList();
        });
    });

    document.getElementById('createContractBtn')?.addEventListener('click', () => {
        selectedContractId = null;
        document.getElementById('contract-form-panel').scrollIntoView({ behavior: 'smooth' });
        document.getElementById('contract-code').value = 'CONTRACT-' + Date.now();
    });

    document.getElementById('confirmBtn')?.addEventListener('click', handleConfirm);
    document.getElementById('signBtn')?.addEventListener('click', handleSign);
    document.getElementById('cancelBtn')?.addEventListener('click', resetForm);
    document.getElementById('addPartyBtn')?.addEventListener('click', addParty);

    if (signatureCanvas) {
        signatureCanvas.addEventListener('mousedown', startDrawing);
        signatureCanvas.addEventListener('mousemove', draw);
        signatureCanvas.addEventListener('mouseup', stopDrawing);
        signatureCanvas.addEventListener('mouseout', stopDrawing);
    }

    document.querySelector('.btn-clear-signature')?.addEventListener('click', clearSignature);

    document.getElementById('contract-type')?.addEventListener('change', function() {
        if (!selectedContractId) {
            const code = 'CONTRACT-' + this.value.substring(0, 3).toUpperCase() + '-' + Date.now();
            document.getElementById('contract-code').value = code;
        }
    });

    document.getElementById('contract-status')?.addEventListener('change', function() {
        if (this.value === 'pending' || this.value === 'signed') {
            document.getElementById('signature-group').style.display = 'block';
            document.getElementById('signBtn').style.display = 'block';
        } else {
            document.getElementById('signature-group').style.display = 'none';
            document.getElementById('signBtn').style.display = 'none';
        }
    });
}

function loadGroups() {
    fetch(GROUP_API_URL)
        .then(res => {
            if (!res.ok) {
                throw new Error(`HTTP error! status: ${res.status}`);
            }
            return res.json();
        })
        .then(data => {
            groups = data;
            const select = document.getElementById('vehicle-group');
            if (!select) {
                return;
            }
            select.innerHTML = '<option value="">Chọn nhóm</option>';
            if (Array.isArray(data)) {
                data.forEach(group => {
                    const option = document.createElement('option');
                    option.value = group.groupId || group.id;
                    option.textContent = group.name || group.groupName || `Nhóm ${group.groupId || group.id}`;
                    select.appendChild(option);
                });
            }
        })
        .catch(err => {
            console.error('Error loading groups:', err);
            const select = document.getElementById('vehicle-group');
            if (select) {
                select.innerHTML = '<option value="">Lỗi tải danh sách nhóm</option>';
            }
        });
}

function loadContracts() {
    fetch(`${CONTRACT_API_URL}/all`)
        .then(res => res.json())
        .then(data => {
            contracts = data;
            updateStats();
            renderContractsList();
        })
        .catch(err => {
            console.error('Error loading contracts:', err);
        });
}

function renderContractsList() {
    const contractsList = document.getElementById('contracts-list');
    if (!contractsList) {
        return;
    }
    contractsList.innerHTML = '';

    let filteredContracts = contracts;

    if (currentFilter !== 'all') {
        filteredContracts = contracts.filter(c => c.contractStatus === currentFilter);
    }

    if (filteredContracts.length === 0) {
        contractsList.innerHTML = '<div class="empty-state">Không có hợp đồng nào</div>';
        return;
    }

    filteredContracts.sort((a, b) => {
        const dateA = new Date(a.creationDate);
        const dateB = new Date(b.creationDate);
        return dateB - dateA;
    });

    filteredContracts.forEach(contract => {
        const item = createContractItem(contract);
        contractsList.appendChild(item);
    });
}

function createContractItem(contract) {
    const template = document.getElementById('contract-item-template');
    const item = template.content.cloneNode(true);

    item.querySelector('.service-name').textContent = contract.contractCode || 'Chưa có mã';

    const contractTypeMap = {
        'co_ownership': 'Đồng sở hữu',
        'rental': 'Cho thuê',
        'maintenance': 'Bảo trì',
        'insurance': 'Bảo hiểm'
    };
    const contractTypeText = contractTypeMap[contract.contractType] || contract.contractType || 'Hợp đồng';
    item.querySelector('.service-vehicle').textContent = contractTypeText;

    if (contract.creationDate) {
        const creationDate = new Date(contract.creationDate);
        item.querySelector('.service-date').textContent = `Ngày: ${creationDate.toLocaleDateString('vi-VN')}`;
    } else {
        item.querySelector('.service-date').textContent = 'Ngày: N/A';
    }

    const statusBadge = item.querySelector('.status-badge');
    statusBadge.textContent = getStatusText(contract.contractStatus);
    statusBadge.className = `status-badge status-${contract.contractStatus}`;

    if (contract.contractStatus === 'signed' || contract.contractStatus === 'archived') {
        item.querySelector('.btn-sign-contract').style.display = 'none';
    }

    item.querySelector('.btn-edit-contract').addEventListener('click', () => editContract(contract));
    item.querySelector('.btn-sign-contract').addEventListener('click', () => signContract(contract.contractId || contract.id));
    item.querySelector('.btn-delete-contract').addEventListener('click', () => deleteContract(contract.contractId || contract.id));

    return item;
}

function getStatusText(status) {
    const statusMap = {
        'draft': 'Dự thảo',
        'pending': 'Chờ ký',
        'signed': 'Đã ký',
        'archived': 'Đã lưu trữ'
    };
    return statusMap[status] || status;
}

function updateStats() {
    document.getElementById('total-contracts').textContent = contracts.length;
    document.getElementById('pending-contracts').textContent = contracts.filter(c => c.contractStatus === 'pending').length;
    document.getElementById('signed-contracts').textContent = contracts.filter(c => c.contractStatus === 'signed').length;
    document.getElementById('archived-contracts').textContent = contracts.filter(c => c.contractStatus === 'archived').length;
}

function editContract(contract) {
    selectedContractId = contract.contractId || contract.id;

    document.getElementById('contract-code').value = contract.contractCode || '';
    document.getElementById('contract-type').value = contract.contractType || 'co_ownership';
    document.getElementById('contract-status').value = contract.contractStatus || 'draft';
    document.getElementById('contract-description').value = contract.description || '';
    document.getElementById('vehicle-group').value = contract.groupId || '';

    if (contract.parties) {
        try {
            const parties = typeof contract.parties === 'string' ? JSON.parse(contract.parties) : contract.parties;
            const partiesList = document.getElementById('parties-list');
            partiesList.innerHTML = '';
            if (Array.isArray(parties)) {
                parties.forEach(party => {
                    addPartyItem(party);
                });
            }
        } catch (e) {
            console.error('Error parsing parties:', e);
        }
    }

    if (contract.creationDate) {
        const creationDate = new Date(contract.creationDate);
        document.getElementById('creation-date').value = creationDate.toISOString().split('T')[0];
    }

    if (contract.signedDate) {
        const signedDate = new Date(contract.signedDate);
        document.getElementById('signed-date').value = signedDate.toISOString().split('T')[0];
    }

    document.getElementById('contract-form-panel').scrollIntoView({ behavior: 'smooth' });
}

function signContract(id, signatureDataParam = null) {
    if (!confirm('Bạn có chắc muốn ký hợp đồng này?')) {
        return;
    }

    let signatureData = signatureDataParam;
    if (!signatureData) {
        signatureData = document.getElementById('signature-image')?.value || null;
    }
    if (!signatureData && signatureCanvas && signatureCtx) {
        const imageData = signatureCtx.getImageData(0, 0, signatureCanvas.width, signatureCanvas.height);
        const hasDrawing = imageData.data.some((channel, index) => {
            return index % 4 !== 3 && channel !== 0;
        });
        if (hasDrawing) {
            signatureData = signatureCanvas.toDataURL('image/png');
        }
    }

    const signerId = 'ADMIN';

    const requestData = {
        signerId: signerId
    };

    if (signatureData) {
        requestData.signatureData = signatureData;
    }

    fetch(`${CONTRACT_API_URL}/sign/${id}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
    })
        .then(res => {
            if (!res.ok) {
                return res.text().then(text => {
                    throw new Error(text);
                });
            }
            return res.json();
        })
        .then(contract => {
            console.log('Contract signed:', contract);
            alert('✅ Đã ký hợp đồng thành công!');
            resetForm();
            loadContracts();
        })
        .catch(err => {
            console.error('Error signing contract:', err);
            alert('❌ Không thể ký hợp đồng: ' + err.message);
        });
}

function deleteContract(id) {
    if (!confirm('Bạn có chắc muốn xóa hợp đồng này? Hành động này không thể hoàn tác.')) {
        return;
    }

    fetch(`${CONTRACT_API_URL}/${id}`, {
        method: 'DELETE'
    })
        .then(res => {
            if (!res.ok) {
                return res.text().then(text => {
                    throw new Error(text);
                });
            }
            return res.json();
        })
        .then(result => {
            console.log('Contract deleted:', result);
            alert('✅ Đã xóa hợp đồng thành công');
            loadContracts();
        })
        .catch(err => {
            console.error('Error deleting contract:', err);
            alert('❌ Không thể xóa hợp đồng: ' + err.message);
        });
}

function handleConfirm() {
    const contractCode = document.getElementById('contract-code').value;
    const contractType = document.getElementById('contract-type').value;
    const contractStatus = document.getElementById('contract-status').value;
    const description = document.getElementById('contract-description').value;
    const groupId = document.getElementById('vehicle-group').value;

    if (!contractCode || contractCode.trim() === '') {
        alert('⚠️ Vui lòng nhập mã hợp đồng!');
        return;
    }

    const parties = [];
    document.querySelectorAll('#parties-list .party-item input').forEach(input => {
        if (input.value && input.value.trim() !== '') {
            parties.push(input.value.trim());
        }
    });

    const data = {
        contractCode: contractCode.trim(),
        contractType: contractType,
        contractStatus: contractStatus,
        description: description ? description.trim() : null,
        groupId: groupId || null,
        parties: parties.length > 0 ? JSON.stringify(parties) : null
    };

    if (selectedContractId) {
        updateContract(selectedContractId, data);
    } else {
        createContract(data);
    }
}

function createContract(data) {
    fetch(`${CONTRACT_API_URL}/create`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })
        .then(res => {
            if (!res.ok) {
                return res.text().then(text => {
                    throw new Error(text);
                });
            }
            return res.json();
        })
        .then(contract => {
            console.log('Contract created:', contract);
            alert('✅ Tạo hợp đồng thành công!');
            resetForm();
            loadContracts();
        })
        .catch(err => {
            console.error('Error creating contract:', err);
            alert('❌ Không thể tạo hợp đồng: ' + err.message);
        });
}

function updateContract(id, data) {
    fetch(`${CONTRACT_API_URL}/update/${id}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })
        .then(res => {
            if (!res.ok) {
                return res.text().then(text => {
                    throw new Error(text);
                });
            }
            return res.json();
        })
        .then(contract => {
            console.log('Contract updated:', contract);
            alert('✅ Cập nhật hợp đồng thành công!');
            resetForm();
            loadContracts();
        })
        .catch(err => {
            console.error('Error updating contract:', err);
            alert('❌ Không thể cập nhật hợp đồng: ' + err.message);
        });
}

function handleSign() {
    if (!selectedContractId) {
        alert('⚠️ Vui lòng chọn hoặc tạo hợp đồng để ký');
        return;
    }

    let signatureData = null;
    if (signatureCanvas && signatureCtx) {
        const imageData = signatureCtx.getImageData(0, 0, signatureCanvas.width, signatureCanvas.height);
        const hasDrawing = imageData.data.some((channel, index) => {
            return index % 4 !== 3 && channel !== 0;
        });

        if (hasDrawing) {
            signatureData = signatureCanvas.toDataURL('image/png');
        } else {
            if (!confirm('Bạn chưa ký vào canvas. Bạn có muốn tiếp tục ký hợp đồng mà không có chữ ký?')) {
                return;
            }
        }
    }

    signContract(selectedContractId, signatureData);
}

function addParty() {
    addPartyItem();
}

function addPartyItem(partyName = '') {
    const partiesList = document.getElementById('parties-list');
    const partyItem = document.createElement('div');
    partyItem.className = 'party-item';
    partyItem.innerHTML = `
        <input type="text" placeholder="Nhập tên bên" value="${partyName}">
        <button class="btn-remove-party" type="button"><i class="fas fa-times"></i></button>
    `;
    partiesList.appendChild(partyItem);

    partyItem.querySelector('.btn-remove-party').addEventListener('click', () => {
        partyItem.remove();
    });
}

function startDrawing(e) {
    isDrawing = true;
    const rect = signatureCanvas.getBoundingClientRect();
    signatureCtx.beginPath();
    signatureCtx.moveTo(e.clientX - rect.left, e.clientY - rect.top);
}

function draw(e) {
    if (!isDrawing) return;
    const rect = signatureCanvas.getBoundingClientRect();
    signatureCtx.lineTo(e.clientX - rect.left, e.clientY - rect.top);
    signatureCtx.stroke();
}

function stopDrawing() {
    if (isDrawing) {
        isDrawing = false;
        signatureCtx.beginPath();
        saveSignature();
    }
}

function clearSignature() {
    signatureCtx.clearRect(0, 0, signatureCanvas.width, signatureCanvas.height);
    const signatureInput = document.getElementById('signature-image');
    if (signatureInput) {
        signatureInput.value = '';
    }
}

function saveSignature() {
    const signatureData = signatureCanvas.toDataURL('image/png');
    const signatureInput = document.getElementById('signature-image');
    if (signatureInput) {
        signatureInput.value = signatureData;
    }
}

function resetForm() {
    document.getElementById('contract-code').value = '';
    document.getElementById('contract-type').value = 'co_ownership';
    document.getElementById('contract-status').value = 'draft';
    document.getElementById('contract-description').value = '';
    document.getElementById('vehicle-group').value = '';
    document.getElementById('parties-list').innerHTML = '<div class="party-item"><input type="text" placeholder="Tên bên thứ nhất"><button class="btn-remove-party" type="button"><i class="fas fa-times"></i></button></div>';
    document.getElementById('creation-date').value = '';
    document.getElementById('signed-date').value = '';
    if (signatureCanvas && signatureCtx) {
        clearSignature();
    }
    selectedContractId = null;

    document.querySelectorAll('#parties-list .btn-remove-party').forEach(btn => {
        btn.addEventListener('click', function() {
            this.closest('.party-item').remove();
        });
    });
}
