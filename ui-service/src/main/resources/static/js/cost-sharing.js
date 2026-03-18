// Cost Sharing JavaScript
class CostSharingManager {
    constructor() {
        this.groups = [];
        this.vehicles = [];
        this.costSplits = [];
        this.currentSplit = null;
        
        this.init();
    }

    async init() {
        await this.loadGroups();
        await this.loadVehicles();
        await this.loadCostSplits();
        this.setupEventListeners();
        this.updateStats();
    }

    async loadGroups() {
        try {
            const response = await fetch('/groups/api/all');
            this.groups = await response.json();
            this.populateGroupSelect();
        } catch (error) {
            console.error('Error loading groups:', error);
        }
    }

    async loadVehicles() {
        try {
            // This would typically come from a vehicle service
            // For now, we'll use mock data
            this.vehicles = [
                { id: 1, name: 'Tesla Model 3', plateNumber: '30A-12345' },
                { id: 2, name: 'BMW i3', plateNumber: '30B-67890' }
            ];
            this.populateVehicleSelect();
        } catch (error) {
            console.error('Error loading vehicles:', error);
        }
    }

    async loadCostSplits() {
        try {
            const response = await fetch('/costs/api/shares');
            const costShares = await response.json();
            this.costSplits = costShares || [];
            this.renderCostSplits();
            this.updateStats();
        } catch (error) {
            console.error('Error loading cost splits:', error);
        }
    }

    populateGroupSelect() {
        const groupSelect = document.getElementById('groupId');
        const filterGroupSelect = document.getElementById('filterGroup');
        
        groupSelect.innerHTML = '<option value="">Chọn nhóm sở hữu</option>';
        filterGroupSelect.innerHTML = '<option value="">Tất cả nhóm</option>';
        
        this.groups.forEach(group => {
            const option = document.createElement('option');
            option.value = group.groupId;
            option.textContent = group.groupName;
            groupSelect.appendChild(option);
            
            const filterOption = document.createElement('option');
            filterOption.value = group.groupId;
            filterOption.textContent = group.groupName;
            filterGroupSelect.appendChild(filterOption);
        });
    }

    populateVehicleSelect() {
        const vehicleSelect = document.getElementById('vehicleId');
        vehicleSelect.innerHTML = '<option value="">Chọn xe</option>';
        
        this.vehicles.forEach(vehicle => {
            const option = document.createElement('option');
            option.value = vehicle.id;
            option.textContent = `${vehicle.name} (${vehicle.plateNumber})`;
            vehicleSelect.appendChild(option);
        });
    }

    setupEventListeners() {
        // Form submission
        document.getElementById('costSharingForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleFormSubmit();
        });

        // Group change - update vehicles
        document.getElementById('groupId').addEventListener('change', (e) => {
            this.updateVehiclesForGroup(e.target.value);
        });

        // Split method change
        document.getElementById('splitMethod').addEventListener('change', (e) => {
            this.handleSplitMethodChange(e.target.value);
        });

        // Filter changes
        document.getElementById('filterStatus').addEventListener('change', (e) => {
            this.filterCostSplits();
        });

        document.getElementById('filterGroup').addEventListener('change', (e) => {
            this.filterCostSplits();
        });

        // Payment form
        document.getElementById('paymentForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.handlePaymentSubmit();
        });
    }

    async updateVehiclesForGroup(groupId) {
        if (!groupId) return;
        
        try {
            // In a real app, this would fetch vehicles for the specific group
            const response = await fetch(`/api/groups/${groupId}/vehicles`);
            const groupVehicles = await response.json();
            
            const vehicleSelect = document.getElementById('vehicleId');
            vehicleSelect.innerHTML = '<option value="">Chọn xe</option>';
            
            groupVehicles.forEach(vehicle => {
                const option = document.createElement('option');
                option.value = vehicle.id;
                option.textContent = `${vehicle.name} (${vehicle.plateNumber})`;
                vehicleSelect.appendChild(option);
            });
        } catch (error) {
            console.error('Error loading vehicles for group:', error);
        }
    }

    handleSplitMethodChange(method) {
        const customSplitDiv = document.getElementById('customSplitDiv');
        if (method === 'CUSTOM' && !customSplitDiv) {
            this.createCustomSplitInterface();
        } else if (method !== 'CUSTOM' && customSplitDiv) {
            customSplitDiv.remove();
        }
    }

    createCustomSplitInterface() {
        const form = document.getElementById('costSharingForm');
        const customDiv = document.createElement('div');
        customDiv.id = 'customSplitDiv';
        customDiv.innerHTML = `
            <div class="form-group">
                <label>Phân chia tùy chỉnh</label>
                <div id="customSplitMembers">
                    <!-- Custom split members will be added here -->
                </div>
                <button type="button" class="btn btn-outline" onclick="costSharingManager.addCustomSplitMember()">
                    <i class="fas fa-plus"></i>
                    Thêm thành viên
                </button>
            </div>
        `;
        
        form.insertBefore(customDiv, form.querySelector('.form-actions'));
        this.loadGroupMembers();
    }

    async loadGroupMembers() {
        const groupId = document.getElementById('groupId').value;
        if (!groupId) return;

        try {
            const response = await fetch(`/api/groups/${groupId}/members`);
            const members = await response.json();
            
            const customSplitMembers = document.getElementById('customSplitMembers');
            customSplitMembers.innerHTML = '';
            
            members.forEach(member => {
                const memberDiv = document.createElement('div');
                memberDiv.className = 'custom-split-member';
                memberDiv.innerHTML = `
                    <div class="member-info">
                        <span>${member.userName || `User ${member.userId}`}</span>
                    </div>
                    <div class="member-inputs">
                        <input type="number" placeholder="%" min="0" max="100" 
                               data-user-id="${member.userId}" class="split-percentage">
                        <input type="number" placeholder="Số tiền" 
                               data-user-id="${member.userId}" class="split-amount">
                    </div>
                `;
                customSplitMembers.appendChild(memberDiv);
            });
        } catch (error) {
            console.error('Error loading group members:', error);
        }
    }

    addCustomSplitMember() {
        // This would allow adding additional members not in the group
        console.log('Add custom split member functionality');
    }

    async previewSplit() {
        const formData = this.getFormData();
        if (!this.validateForm(formData)) return;

        // First create the cost
        try {
            const costResponse = await fetch('/costs/api/costs', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    vehicleId: formData.vehicleId,
                    costType: formData.costType,
                    amount: formData.amount,
                    description: formData.description
                })
            });

            if (!costResponse.ok) {
                throw new Error('Failed to create cost');
            }

            const cost = await costResponse.json();
            
            // Then calculate shares based on split method
            const shares = await this.calculateShares(cost.costId, formData);
            this.showSplitPreview(cost, shares);
        } catch (error) {
            console.error('Error previewing split:', error);
            this.showError('Không thể xem trước chia sẻ. Vui lòng thử lại.');
        }
    }

    async calculateShares(costId, formData) {
        // Mock calculation based on split method
        const splitMethod = formData.splitMethod;
        let userIds = [1, 2, 3]; // Mock user IDs
        let percentages = [33.33, 33.33, 33.34]; // Mock percentages

        if (splitMethod === 'OWNERSHIP_PERCENTAGE') {
            // Get ownership percentages from group
            percentages = [50, 30, 20];
        } else if (splitMethod === 'EQUAL_SPLIT') {
            const count = userIds.length;
            percentages = new Array(count).fill(100 / count);
        }

        try {
            const response = await fetch(`/costs/api/costs/${costId}/calculate-shares`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    userIds: userIds,
                    percentages: percentages
                })
            });

            if (response.ok) {
                return await response.json();
            } else {
                throw new Error('Failed to calculate shares');
            }
        } catch (error) {
            console.error('Error calculating shares:', error);
            // Return mock data for preview
            return userIds.map((userId, index) => ({
                userId: userId,
                percent: percentages[index],
                amountShare: (formData.amount * percentages[index]) / 100
            }));
        }
    }

    showSplitPreview(cost, shares) {
        const previewDiv = document.getElementById('splitPreview');
        const contentDiv = document.getElementById('splitPreviewContent');
        
        contentDiv.innerHTML = `
            <div class="split-preview-header">
                <h4>Xem trước chia sẻ</h4>
                <p>Tổng chi phí: ${this.formatCurrency(cost.amount)}</p>
                <p>Loại chi phí: ${this.getCostTypeLabel(cost.costType)}</p>
            </div>
            <div class="split-preview-list">
                ${shares.map(share => `
                    <div class="split-preview-item">
                        <div class="share-info">
                            <span>User ID: ${share.userId}</span>
                            <span class="share-percent">${share.percent}%</span>
                        </div>
                        <strong>${this.formatCurrency(share.amountShare)}</strong>
                    </div>
                `).join('')}
            </div>
            <div class="split-preview-actions">
                <button class="btn btn-primary" onclick="costSharingManager.confirmSplit(${cost.costId})">
                    <i class="fas fa-check"></i>
                    Xác nhận chia sẻ
                </button>
                <button class="btn btn-outline" onclick="costSharingManager.cancelSplit()">
                    <i class="fas fa-times"></i>
                    Hủy
                </button>
            </div>
        `;
        
        previewDiv.style.display = 'block';
    }

    async confirmSplit(costId) {
        try {
            // The shares are already calculated and saved in the backend
            this.showSuccess('Chia sẻ chi phí thành công!');
            document.getElementById('splitPreview').style.display = 'none';
            document.getElementById('costSharingForm').reset();
            await this.loadCostSplits();
        } catch (error) {
            console.error('Error confirming split:', error);
            this.showError('Không thể xác nhận chia sẻ. Vui lòng thử lại.');
        }
    }

    cancelSplit() {
        document.getElementById('splitPreview').style.display = 'none';
    }

    async handleFormSubmit() {
        const formData = this.getFormData();
        if (!this.validateForm(formData)) return;

        try {
            const response = await fetch('/costs/api/costs', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(formData)
            });

            if (response.ok) {
                this.showSuccess('Chi phí đã được tạo và chia sẻ thành công!');
                document.getElementById('costSharingForm').reset();
                document.getElementById('splitPreview').style.display = 'none';
                await this.loadCostSplits();
            } else {
                throw new Error('Failed to create cost split');
            }
        } catch (error) {
            console.error('Error creating cost split:', error);
            this.showError('Không thể tạo chi phí. Vui lòng thử lại.');
        }
    }

    getFormData() {
        const form = document.getElementById('costSharingForm');
        const formData = new FormData(form);
        
        return {
            groupId: formData.get('groupId'),
            vehicleId: formData.get('vehicleId'),
            costType: formData.get('costType'),
            splitMethod: formData.get('splitMethod'),
            amount: parseFloat(formData.get('amount')),
            description: formData.get('description'),
            invoiceNumber: formData.get('invoiceNumber'),
            receiptUrl: formData.get('receiptUrl')
        };
    }

    validateForm(data) {
        if (!data.groupId) {
            this.showError('Vui lòng chọn nhóm sở hữu');
            return false;
        }
        if (!data.vehicleId) {
            this.showError('Vui lòng chọn xe');
            return false;
        }
        if (!data.costType) {
            this.showError('Vui lòng chọn loại chi phí');
            return false;
        }
        if (!data.amount || data.amount <= 0) {
            this.showError('Vui lòng nhập số tiền hợp lệ');
            return false;
        }
        return true;
    }

    renderCostSplits() {
        const tbody = document.getElementById('splitsTableBody');
        if (!tbody) return;
        
        tbody.innerHTML = '';

        this.costSplits.forEach(share => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>
                    <div class="cost-info">
                        <div class="cost-title">Chi phí ID: ${share.costId}</div>
                        <div class="cost-type">Chia sẻ chi phí</div>
                    </div>
                </td>
                <td>Group ${share.groupId || 'N/A'}</td>
                <td>
                    <div class="user-info">
                        <span>User ${share.userId}</span>
                        <span class="share-percent">${share.percent}%</span>
                    </div>
                </td>
                <td>${this.formatCurrency(share.amountShare)}</td>
                <td>
                    <span class="status-badge status-${(share.status || 'PENDING').toLowerCase()}">
                        ${this.getStatusLabel(share.status || 'PENDING')}
                    </span>
                </td>
                <td>${this.formatDate(share.calculatedAt)}</td>
                <td>
                    <div class="action-buttons">
                        <button class="btn btn-sm btn-primary" onclick="costSharingManager.openPaymentModal(${share.shareId}, ${share.amountShare})">
                            <i class="fas fa-credit-card"></i>
                            Thanh toán
                        </button>
                        <button class="btn btn-sm btn-outline" onclick="costSharingManager.viewShareDetails(${share.shareId})">
                            <i class="fas fa-eye"></i>
                            Xem
                        </button>
                    </div>
                </td>
            `;
            tbody.appendChild(row);
        });
    }

    filterCostSplits() {
        const statusFilter = document.getElementById('filterStatus').value;
        const groupFilter = document.getElementById('filterGroup').value;
        
        // This would filter the cost splits based on the selected filters
        console.log('Filtering by status:', statusFilter, 'group:', groupFilter);
    }

    openPaymentModal(splitId, amount) {
        this.currentSplit = splitId;
        
        // Set payment amount
        document.getElementById('paymentAmount').value = amount;
        
        // Update displayed amount
        document.getElementById('displayAmount').textContent = formatCurrency(amount);
        
        // Set payment content
        document.getElementById('displayContent').textContent = `SPLIT${splitId}`;
        
        // Show modal
        document.getElementById('paymentModal').style.display = 'block';
        
        // Generate QR code immediately with default method (EWallet)
        setTimeout(() => {
            updateQRCode();
        }, 100);
    }

    closePaymentModal() {
        document.getElementById('paymentModal').style.display = 'none';
        this.currentSplit = null;
    }

    async handlePaymentSubmit() {
        const formData = new FormData(document.getElementById('paymentForm'));
        
        const paymentData = {
            splitId: this.currentSplit,
            method: formData.get('paymentMethod'),
            amount: parseFloat(formData.get('paymentAmount')),
            transactionCode: formData.get('transactionCode')
        };

        try {
            const response = await fetch('/api/payments', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(paymentData)
            });

            if (response.ok) {
                this.showSuccess('Thanh toán thành công!');
                this.closePaymentModal();
                await this.loadCostSplits();
            } else {
                throw new Error('Payment failed');
            }
        } catch (error) {
            console.error('Error processing payment:', error);
            this.showError('Không thể xử lý thanh toán. Vui lòng thử lại.');
        }
    }
}

// Global function for updating QR code when payment method changes
function updateQRCode() {
    const method = document.getElementById('paymentMethod').value;
    const amount = parseFloat(document.getElementById('paymentAmount').value) || 0;
    const content = document.getElementById('displayContent').textContent;
    
    const qrSection = document.getElementById('qrCodeSection');
    const cashSection = document.getElementById('cashPaymentSection');
    
    const methodInfo = {
        'EWallet': { 
            name: 'MoMo', 
            account: '0123456789', 
            accountName: 'NGUYEN VAN A',
            note: 'Quét mã QR bằng app MoMo của bạn',
            instructions: [
                'Mở app MoMo trên điện thoại',
                'Chọn "Quét QR" hoặc "Chuyển tiền"',
                'Quét mã QR hoặc nhập số điện thoại: 0123456789',
                'Kiểm tra số tiền và nội dung chuyển khoản',
                'Xác nhận thanh toán',
                'Nhập mã giao dịch và bấm "Xác nhận thanh toán"'
            ]
        },
        'Banking': { 
            name: 'Vietcombank', 
            account: '0987654321', 
            accountName: 'NGUYEN VAN A',
            note: 'Quét mã QR bằng app ngân hàng của bạn',
            instructions: [
                'Mở app Vietcombank (hoặc app ngân hàng khác)',
                'Chọn "Chuyển khoản" hoặc "Quét QR"',
                'Quét mã QR hoặc nhập STK: 0987654321',
                'Kiểm tra thông tin: Vietcombank - NGUYEN VAN A',
                'Nhập số tiền và nội dung chuyển khoản',
                'Xác nhận và hoàn tất giao dịch',
                'Nhập mã giao dịch và bấm "Xác nhận thanh toán"'
            ]
        },
        'Cash': {
            name: 'Tiền mặt',
            account: 'N/A',
            accountName: 'Admin',
            note: 'Thanh toán trực tiếp',
            instructions: [
                'Chuẩn bị số tiền cần thanh toán',
                'Liên hệ với admin để thanh toán',
                'Nhận biên lai (nếu có)',
                'Bấm "Xác nhận thanh toán" sau khi đã thanh toán'
            ]
        }
    };
    
    const info = methodInfo[method] || methodInfo['EWallet'];
    
    // Update bank info
    document.getElementById('bankName').textContent = info.name;
    document.getElementById('accountNumber').textContent = info.account;
    document.getElementById('accountName').textContent = info.accountName;
    document.getElementById('qrNote').textContent = info.note;
    
    // Update instructions
    const instructionsList = document.getElementById('instructionsList');
    instructionsList.innerHTML = info.instructions.map(step => `<li>${step}</li>`).join('');
    
    if (method === 'Cash') {
        // Hide QR section, show cash section
        qrSection.style.display = 'none';
        cashSection.style.display = 'block';
        document.getElementById('cashAmount').textContent = formatCurrency(amount);
    } else {
        // Show QR section, hide cash section
        qrSection.style.display = 'block';
        cashSection.style.display = 'none';
        
        // Generate QR code
        const qrContent = `Bank: ${info.name}\nAccount: ${info.account}\nAmount: ${amount}\nContent: ${content}`;
        const qrCodeUrl = `https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${encodeURIComponent(qrContent)}`;
        document.getElementById('qrCodeImage').src = qrCodeUrl;
    }
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', { 
        style: 'currency', 
        currency: 'VND' 
    }).format(amount);
}

    viewSplitDetails(splitId) {
        // This would open a modal or navigate to details page
        console.log('View split details for:', splitId);
    }

    viewCostDetails(costId) {
        // This would open a modal or navigate to details page
        console.log('View cost details for:', costId);
        // For now, just show an alert with the cost ID
        this.showSuccess(`Xem chi tiết chi phí ID: ${costId}`);
    }

    viewShareDetails(shareId) {
        // This would open a modal or navigate to details page
        console.log('View share details for:', shareId);
        this.showSuccess(`Xem chi tiết chia sẻ ID: ${shareId}`);
    }

    updateStats() {
        const totalCosts = this.costSplits.reduce((sum, share) => sum + (share.amountShare || 0), 0);
        const totalShares = this.costSplits.length;
        const totalPaid = this.costSplits
            .filter(share => share.status === 'PAID')
            .reduce((sum, share) => sum + (share.amountShare || 0), 0);
        const pendingPayments = this.costSplits.filter(share => share.status === 'PENDING').length;

        const totalCostsEl = document.getElementById('totalCosts');
        const totalSharesEl = document.getElementById('totalShares');
        const totalPaidEl = document.getElementById('totalPaid');
        const pendingPaymentsEl = document.getElementById('pendingPayments');

        if (totalCostsEl) totalCostsEl.textContent = this.formatCurrency(totalCosts);
        if (totalSharesEl) totalSharesEl.textContent = totalShares;
        if (totalPaidEl) totalPaidEl.textContent = this.formatCurrency(totalPaid);
        if (pendingPaymentsEl) pendingPaymentsEl.textContent = pendingPayments;
    }

    async generateReport() {
        try {
            const response = await fetch('/api/reports/cost-sharing', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (response.ok) {
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'cost-sharing-report.pdf';
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
            }
        } catch (error) {
            console.error('Error generating report:', error);
            this.showError('Không thể tạo báo cáo. Vui lòng thử lại.');
        }
    }

    // Utility functions
    formatCurrency(amount) {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(amount);
    }

    formatDate(dateString) {
        return new Date(dateString).toLocaleDateString('vi-VN');
    }

    getCostTypeLabel(type) {
        const labels = {
            'ElectricCharge': '⚡ Sạc điện',
            'Maintenance': '🔧 Bảo dưỡng',
            'Insurance': '🛡️ Bảo hiểm',
            'Inspection': '🔍 Kiểm định',
            'Cleaning': '🧽 Vệ sinh',
            'Other': '📝 Khác'
        };
        return labels[type] || type;
    }

    getStatusLabel(status) {
        const labels = {
            'PENDING': 'Chờ thanh toán',
            'PAID': 'Đã thanh toán',
            'OVERDUE': 'Quá hạn',
            'WAIVED': 'Miễn phí'
        };
        return labels[status] || status;
    }

    showSuccess(message) {
        // You would implement a proper notification system here
        alert(message);
    }

    showError(message) {
        // You would implement a proper error notification system here
        alert(message);
    }

    /**
     * 🔍 Tìm kiếm chi phí theo ID
     */
    async searchCostById() {
        const costId = document.getElementById('searchCostId').value;
        if (!costId) {
            this.showError('Vui lòng nhập ID chi phí');
            return;
        }

        try {
            const response = await fetch(`/costs/api/costs/${costId}`);
            const resultDiv = document.getElementById('searchResult');
            
            if (response.ok) {
                const cost = await response.json();
                resultDiv.innerHTML = `
                    <div class="search-result-success">
                        <h4>✅ Tìm thấy chi phí</h4>
                        <div class="cost-details">
                            <div class="detail-row">
                                <strong>ID:</strong> ${cost.costId}
                            </div>
                            <div class="detail-row">
                                <strong>Loại:</strong> ${this.getCostTypeLabel(cost.costType)}
                            </div>
                            <div class="detail-row">
                                <strong>Số tiền:</strong> ${this.formatCurrency(cost.amount)}
                            </div>
                            <div class="detail-row">
                                <strong>Mô tả:</strong> ${cost.description || 'Không có mô tả'}
                            </div>
                            <div class="detail-row">
                                <strong>Ngày tạo:</strong> ${this.formatDate(cost.createdAt)}
                            </div>
                        </div>
                        <div class="search-actions">
                            <button class="btn btn-primary" onclick="costSharingManager.viewCostShares(${cost.costId})">
                                <i class="fas fa-share-alt"></i>
                                Xem chia sẻ
                            </button>
                            <button class="btn btn-outline" onclick="costSharingManager.viewCostHistory(${cost.costId})">
                                <i class="fas fa-history"></i>
                                Lịch sử
                            </button>
                        </div>
                    </div>
                `;
            } else if (response.status === 404) {
                resultDiv.innerHTML = `
                    <div class="search-result-error">
                        <h4>❌ Không tìm thấy</h4>
                        <p>Không tìm thấy chi phí với ID: ${costId}</p>
                    </div>
                `;
            } else {
                throw new Error('Search failed');
            }
            
            resultDiv.style.display = 'block';
        } catch (error) {
            console.error('Error searching cost:', error);
            this.showError('Lỗi khi tìm kiếm chi phí');
        }
    }

    /**
     * 📊 Xem chia sẻ chi phí
     */
    async viewCostShares(costId) {
        try {
            const response = await fetch(`/costs/${costId}/splits`);
            if (response.ok) {
                const shares = await response.json();
                this.showCostSharesModal(costId, shares);
            } else {
                throw new Error('Failed to load cost shares');
            }
        } catch (error) {
            console.error('Error loading cost shares:', error);
            this.showError('Không thể tải thông tin chia sẻ');
        }
    }

    /**
     * 📈 Xem lịch sử chia sẻ chi phí
     */
    async viewCostHistory(costId) {
        try {
            const response = await fetch(`/api/costs/${costId}/shares/history`);
            if (response.ok) {
                const history = await response.json();
                this.showCostHistoryModal(costId, history);
            } else {
                throw new Error('Failed to load cost history');
            }
        } catch (error) {
            console.error('Error loading cost history:', error);
            this.showError('Không thể tải lịch sử chia sẻ');
        }
    }

    /**
     * Hiển thị modal chia sẻ chi phí
     */
    showCostSharesModal(costId, shares) {
        const modalHtml = `
            <div class="modal" id="costSharesModal">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>Chia sẻ chi phí ID: ${costId}</h3>
                        <button class="modal-close" onclick="costSharingManager.closeModal('costSharesModal')">&times;</button>
                    </div>
                    <div class="modal-body">
                        <div class="shares-list">
                            ${shares.map(share => `
                                <div class="share-item">
                                    <div class="share-info">
                                        <strong>User ID: ${share.userId}</strong>
                                        <span class="share-percent">${share.percent}%</span>
                                    </div>
                                    <div class="share-amount">${this.formatCurrency(share.amountShare)}</div>
                                </div>
                            `).join('')}
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        document.getElementById('costSharesModal').style.display = 'block';
    }

    /**
     * Hiển thị modal lịch sử chia sẻ
     */
    showCostHistoryModal(costId, history) {
        const modalHtml = `
            <div class="modal" id="costHistoryModal">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>Lịch sử chia sẻ chi phí ID: ${costId}</h3>
                        <button class="modal-close" onclick="costSharingManager.closeModal('costHistoryModal')">&times;</button>
                    </div>
                    <div class="modal-body">
                        <div class="history-list">
                            ${history.map(item => `
                                <div class="history-item">
                                    <div class="history-header">
                                        <strong>User ID: ${item.userId}</strong>
                                        <span class="history-date">${this.formatDate(item.calculatedAt)}</span>
                                    </div>
                                    <div class="history-details">
                                        <div class="detail-row">
                                            <strong>Loại chi phí:</strong> ${item.costType}
                                        </div>
                                        <div class="detail-row">
                                            <strong>Tổng chi phí:</strong> ${this.formatCurrency(item.totalCostAmount)}
                                        </div>
                                        <div class="detail-row">
                                            <strong>Phần trăm:</strong> ${item.percent}%
                                        </div>
                                        <div class="detail-row">
                                            <strong>Số tiền chia sẻ:</strong> ${this.formatCurrency(item.amountShare)}
                                        </div>
                                        ${item.description ? `
                                            <div class="detail-row">
                                                <strong>Mô tả:</strong> ${item.description}
                                            </div>
                                        ` : ''}
                                    </div>
                                </div>
                            `).join('')}
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        document.getElementById('costHistoryModal').style.display = 'block';
    }

    /**
     * Đóng modal
     */
    closeModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.remove();
        }
    }
}

// Global functions for HTML onclick handlers
function openAddCostModal() {
    // This would open a modal for adding costs
    console.log('Open add cost modal');
}

function previewSplit() {
    costSharingManager.previewSplit();
}

function closePaymentModal() {
    costSharingManager.closePaymentModal();
}

function generateReport() {
    costSharingManager.generateReport();
}

function searchCostById() {
    costSharingManager.searchCostById();
}

// Initialize the cost sharing manager when the page loads
let costSharingManager;
document.addEventListener('DOMContentLoaded', () => {
    costSharingManager = new CostSharingManager();
});