// ================================
// COST EDIT JAVASCRIPT
// ================================

class CostEdit {
    constructor() {
        this.form = document.getElementById('costEditForm');
        this.preview = document.getElementById('costPreview');
        this.originalData = this.getFormData();
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.updatePreview();
    }

    setupEventListeners() {
        // Form inputs
        const inputs = this.form.querySelectorAll('input, select, textarea');
        inputs.forEach(input => {
            input.addEventListener('input', () => this.updatePreview());
            input.addEventListener('change', () => this.updatePreview());
        });

        // Cost type selection
        const costTypeOptions = document.querySelectorAll('.cost-type-option');
        costTypeOptions.forEach(option => {
            option.addEventListener('click', () => {
                const radio = option.querySelector('.cost-type-radio');
                if (radio) {
                    radio.checked = true;
                    this.updatePreview();
                }
            });
        });

        // Form submission
        this.form.addEventListener('submit', (e) => {
            e.preventDefault();
            this.submitForm();
        });

        // Amount input formatting
        const amountInput = document.getElementById('amount');
        if (amountInput) {
            amountInput.addEventListener('input', (e) => {
                this.formatAmountInput(e.target);
            });
        }
    }

    formatAmountInput(input) {
        let value = input.value.replace(/[^\d]/g, '');
        if (value) {
            value = parseInt(value);
            input.value = value;
        }
    }

    updatePreview() {
        if (!this.preview) return;

        const formData = this.getFormData();
        
        if (formData.amount && formData.description) {
            this.preview.style.display = 'block';
            this.updatePreviewContent(formData);
        } else {
            this.preview.style.display = 'none';
        }
    }

    getFormData() {
        return {
            vehicleId: document.getElementById('vehicleId')?.value || '',
            costType: document.querySelector('input[name="costType"]:checked')?.value || '',
            amount: document.getElementById('amount')?.value || '',
            description: document.getElementById('description')?.value || ''
        };
    }

    updatePreviewContent(data) {
        // Update preview type icon
        const previewType = document.getElementById('previewType');
        if (previewType) {
            const iconClass = this.getCostTypeIcon(data.costType);
            previewType.innerHTML = `<i class="fas ${iconClass}"></i>`;
            previewType.className = `preview-type type-${data.costType}`;
        }

        // Update preview amount
        const previewAmount = document.getElementById('previewAmount');
        if (previewAmount) {
            const amount = parseFloat(data.amount) || 0;
            previewAmount.textContent = this.formatCurrency(amount) + ' VNĐ';
        }

        // Update preview description
        const previewDescription = document.getElementById('previewDescription');
        if (previewDescription) {
            previewDescription.textContent = data.description || 'Mô tả chi phí';
        }

        // Update preview vehicle
        const previewVehicle = document.getElementById('previewVehicle');
        if (previewVehicle) {
            const vehicleName = this.getVehicleName(data.vehicleId);
            previewVehicle.textContent = `Xe: ${vehicleName}`;
        }

        // Update preview date
        const previewDate = document.getElementById('previewDate');
        if (previewDate) {
            previewDate.textContent = 'Hôm nay';
        }
    }

    getCostTypeIcon(costType) {
        const icons = {
            'ElectricCharge': 'fa-bolt',
            'Maintenance': 'fa-tools',
            'Insurance': 'fa-shield-alt',
            'Inspection': 'fa-search',
            'Cleaning': 'fa-broom',
            'Other': 'fa-file-alt'
        };
        return icons[costType] || 'fa-file-alt';
    }

    getVehicleName(vehicleId) {
        const vehicles = {
            '1': 'Tesla Model 3',
            '2': 'BMW i3',
            '3': 'Audi e-tron'
        };
        return vehicles[vehicleId] || `Xe ID: ${vehicleId}`;
    }

    formatCurrency(amount) {
        return new Intl.NumberFormat('vi-VN').format(amount);
    }

    async submitForm() {
        const formData = this.getFormData();
        
        // Validate form
        if (!this.validateForm(formData)) {
            return;
        }

        // Show loading state
        this.setLoadingState(true);

        try {
            const costId = this.getCostId();
            const response = await fetch(`/costs/${costId}/edit`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: this.serializeFormData(formData)
            });

            if (response.ok) {
                this.showSuccessModal(formData);
            } else {
                this.showErrorMessage('Có lỗi xảy ra khi cập nhật chi phí!');
            }
        } catch (error) {
            console.error('Error updating cost:', error);
            this.showErrorMessage('Có lỗi xảy ra khi cập nhật chi phí!');
        } finally {
            this.setLoadingState(false);
        }
    }

    getCostId() {
        const path = window.location.pathname;
        const matches = path.match(/\/costs\/(\d+)\/edit/);
        return matches ? matches[1] : null;
    }

    validateForm(data) {
        const errors = [];

        if (!data.vehicleId) {
            errors.push('Vui lòng chọn xe điện');
        }

        if (!data.costType) {
            errors.push('Vui lòng chọn loại chi phí');
        }

        if (!data.amount || parseFloat(data.amount) < 1000) {
            errors.push('Số tiền phải tối thiểu 1,000 VNĐ');
        }

        if (!data.description || data.description.trim().length < 10) {
            errors.push('Mô tả phải có ít nhất 10 ký tự');
        }

        if (errors.length > 0) {
            this.showValidationErrors(errors);
            return false;
        }

        return true;
    }

    showValidationErrors(errors) {
        const errorMessage = errors.join('<br>');
        this.showErrorMessage(errorMessage);
    }

    serializeFormData(data) {
        const params = new URLSearchParams();
        params.append('vehicleId', data.vehicleId);
        params.append('costType', data.costType);
        params.append('amount', data.amount);
        params.append('description', data.description);
        return params.toString();
    }

    setLoadingState(loading) {
        const submitBtn = this.form.querySelector('button[type="submit"]');
        if (submitBtn) {
            if (loading) {
                submitBtn.disabled = true;
                submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> <span>Đang cập nhật...</span>';
            } else {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="fas fa-save"></i> <span>Cập nhật</span>';
            }
        }
    }

    showSuccessModal(data) {
        const modal = document.getElementById('successModal');
        const successDetails = document.getElementById('successDetails');
        
        if (successDetails) {
            successDetails.textContent = `Chi phí "${data.description}" đã được cập nhật thành công với số tiền ${this.formatCurrency(parseFloat(data.amount))} VNĐ`;
        }

        if (modal) {
            modal.classList.add('show');
        }
    }

    closeSuccessModal() {
        const modal = document.getElementById('successModal');
        if (modal) {
            modal.classList.remove('show');
        }
    }

    async deleteCost() {
        const costId = this.getCostId();
        if (!costId) return;

        try {
            const response = await fetch(`/costs/${costId}/delete`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (response.ok) {
                this.showSuccessMessage('Chi phí đã được xóa thành công!');
                setTimeout(() => {
                    window.location.href = '/costs';
                }, 2000);
            } else {
                this.showErrorMessage('Có lỗi xảy ra khi xóa chi phí!');
            }
        } catch (error) {
            console.error('Error deleting cost:', error);
            this.showErrorMessage('Có lỗi xảy ra khi xóa chi phí!');
        }
    }

    showDeleteModal() {
        const modal = document.getElementById('deleteModal');
        if (modal) {
            modal.classList.add('show');
        }
    }

    closeDeleteModal() {
        const modal = document.getElementById('deleteModal');
        if (modal) {
            modal.classList.remove('show');
        }
    }

    confirmDelete() {
        this.deleteCost();
        this.closeDeleteModal();
    }

    showSuccessMessage(message) {
        this.showNotification(message, 'success');
    }

    showErrorMessage(message) {
        this.showNotification(message, 'error');
    }

    showNotification(message, type) {
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        notification.innerHTML = `
            <div class="notification-content">
                <i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-circle'}"></i>
                <span>${message}</span>
            </div>
        `;

        // Add styles
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: ${type === 'success' ? '#10b981' : '#ef4444'};
            color: white;
            padding: 1rem 1.5rem;
            border-radius: 0.5rem;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
            z-index: 3000;
            transform: translateX(100%);
            transition: transform 0.3s ease;
            max-width: 400px;
        `;

        document.body.appendChild(notification);

        // Animate in
        setTimeout(() => {
            notification.style.transform = 'translateX(0)';
        }, 100);

        // Remove after 5 seconds
        setTimeout(() => {
            notification.style.transform = 'translateX(100%)';
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }, 5000);
    }
}

// Global functions for HTML onclick handlers
function resetForm() {
    const form = document.getElementById('costEditForm');
    if (form) {
        form.reset();
        window.costEdit.updatePreview();
    }
}

function deleteCost() {
    window.costEdit.showDeleteModal();
}

function closeDeleteModal() {
    window.costEdit.closeDeleteModal();
}

function confirmDelete() {
    window.costEdit.confirmDelete();
}

function closeSuccessModal() {
    window.costEdit.closeSuccessModal();
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.costEdit = new CostEdit();
});

// Add CSS for notifications
const notificationStyles = `
    .notification {
        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    }
    
    .notification-content {
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }
    
    .notification i {
        font-size: 1.2rem;
    }
`;

// Inject notification styles
const styleSheet = document.createElement('style');
styleSheet.textContent = notificationStyles;
document.head.appendChild(styleSheet);
