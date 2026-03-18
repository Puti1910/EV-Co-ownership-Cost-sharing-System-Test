// ================================
// COST MANAGEMENT JAVASCRIPT
// ================================

class CostManagement {
    constructor() {
        this.costs = [];
        this.currentView = 'grid';
        this.searchTimeout = null;
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.loadCosts();
        this.calculateStatistics();
    }

    setupEventListeners() {
        // Search functionality
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                clearTimeout(this.searchTimeout);
                this.searchTimeout = setTimeout(() => {
                    this.filterCosts();
                }, 300);
            });
        }

        // Filter controls
        const costTypeFilter = document.getElementById('costTypeFilter');
        const vehicleFilter = document.getElementById('vehicleFilter');
        
        if (costTypeFilter) {
            costTypeFilter.addEventListener('change', () => this.filterCosts());
        }
        
        if (vehicleFilter) {
            vehicleFilter.addEventListener('change', () => this.filterCosts());
        }

        // View controls
        const viewBtns = document.querySelectorAll('.view-btn');
        viewBtns.forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.switchView(e.target.dataset.view);
            });
        });

        // Delete confirmation
        const confirmDeleteBtn = document.getElementById('confirmDelete');
        if (confirmDeleteBtn) {
            confirmDeleteBtn.addEventListener('click', () => {
                this.confirmDelete();
            });
        }
    }

    loadCosts() {
        // This would typically fetch from the server
        // For now, we'll work with the data already in the DOM
        this.costs = Array.from(document.querySelectorAll('.cost-card')).map(card => ({
            id: card.dataset.costId,
            type: card.querySelector('.cost-type').classList[1]?.replace('type-', ''),
            amount: this.parseAmount(card.querySelector('.amount').textContent),
            description: card.querySelector('.cost-description').textContent,
            vehicleId: card.querySelector('.vehicle-id strong').textContent,
            date: card.querySelector('.cost-date').textContent
        }));
    }

    parseAmount(amountStr) {
        return parseFloat(amountStr.replace(/[^\d]/g, '')) || 0;
    }

    filterCosts() {
        const searchTerm = document.getElementById('searchInput')?.value.toLowerCase() || '';
        const costType = document.getElementById('costTypeFilter')?.value || '';
        const vehicleId = document.getElementById('vehicleFilter')?.value || '';

        const costCards = document.querySelectorAll('.cost-card');
        
        costCards.forEach(card => {
            const description = card.querySelector('.cost-description').textContent.toLowerCase();
            const cardType = card.querySelector('.cost-type').classList[1]?.replace('type-', '');
            const cardVehicleId = card.querySelector('.vehicle-id strong').textContent;

            const matchesSearch = !searchTerm || description.includes(searchTerm) || 
                                card.dataset.costId.includes(searchTerm);
            const matchesType = !costType || cardType === costType;
            const matchesVehicle = !vehicleId || cardVehicleId === vehicleId;

            if (matchesSearch && matchesType && matchesVehicle) {
                card.style.display = 'block';
            } else {
                card.style.display = 'none';
            }
        });

        this.updateEmptyState();
    }

    updateEmptyState() {
        const visibleCards = document.querySelectorAll('.cost-card[style*="block"], .cost-card:not([style*="none"])');
        const emptyState = document.querySelector('.empty-state');
        
        if (visibleCards.length === 0 && emptyState) {
            emptyState.style.display = 'block';
        } else if (emptyState) {
            emptyState.style.display = 'none';
        }
    }

    switchView(view) {
        this.currentView = view;
        const container = document.getElementById('costsContainer');
        const viewBtns = document.querySelectorAll('.view-btn');

        viewBtns.forEach(btn => {
            btn.classList.toggle('active', btn.dataset.view === view);
        });

        if (view === 'list') {
            container.classList.add('list-view');
            container.classList.remove('grid-view');
        } else {
            container.classList.add('grid-view');
            container.classList.remove('list-view');
        }
    }

    calculateStatistics() {
        const costs = this.costs;
        
        const stats = {
            electric: 0,
            maintenance: 0,
            insurance: 0,
            total: 0
        };

        costs.forEach(cost => {
            stats.total += cost.amount;
            
            switch (cost.type) {
                case 'ElectricCharge':
                    stats.electric += cost.amount;
                    break;
                case 'Maintenance':
                    stats.maintenance += cost.amount;
                    break;
                case 'Insurance':
                    stats.insurance += cost.amount;
                    break;
            }
        });

        this.updateStatCard('electricCosts', stats.electric);
        this.updateStatCard('maintenanceCosts', stats.maintenance);
        this.updateStatCard('insuranceCosts', stats.insurance);
        this.updateStatCard('totalCosts', stats.total);
    }

    updateStatCard(elementId, amount) {
        const element = document.getElementById(elementId);
        if (element) {
            element.textContent = this.formatCurrency(amount) + ' VNĐ';
        }
    }

    formatCurrency(amount) {
        return new Intl.NumberFormat('vi-VN').format(amount);
    }

    editCost(costId) {
        window.location.href = `/costs/${costId}/edit`;
    }

    deleteCost(costId) {
        this.costToDelete = costId;
        this.showDeleteModal();
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
        this.costToDelete = null;
    }

    async confirmDelete() {
        if (!this.costToDelete) return;

        try {
            const response = await fetch(`/costs/${this.costToDelete}/delete`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (response.ok) {
                this.showSuccessMessage('Chi phí đã được xóa thành công!');
                this.removeCostCard(this.costToDelete);
                this.closeDeleteModal();
                this.calculateStatistics();
            } else {
                this.showErrorMessage('Có lỗi xảy ra khi xóa chi phí!');
            }
        } catch (error) {
            console.error('Error deleting cost:', error);
            this.showErrorMessage('Có lỗi xảy ra khi xóa chi phí!');
        }
    }

    removeCostCard(costId) {
        const card = document.querySelector(`[data-cost-id="${costId}"]`);
        if (card) {
            card.remove();
        }
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
        `;

        document.body.appendChild(notification);

        // Animate in
        setTimeout(() => {
            notification.style.transform = 'translateX(0)';
        }, 100);

        // Remove after 3 seconds
        setTimeout(() => {
            notification.style.transform = 'translateX(100%)';
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }, 3000);
    }
}

// Global functions for HTML onclick handlers
function editCost(costId) {
    window.costManagement.editCost(costId);
}

function deleteCost(costId) {
    window.costManagement.deleteCost(costId);
}

function closeDeleteModal() {
    window.costManagement.closeDeleteModal();
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.costManagement = new CostManagement();
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
