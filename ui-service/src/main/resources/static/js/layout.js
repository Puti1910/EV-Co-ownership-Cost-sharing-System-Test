// Shared Sidebar JavaScript

// Global variables
let sidebarCollapsed = false;

// Initialize sidebar
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded, initializing sidebar...');
    initializeSidebar();
    setupSidebarEventListeners();
    setActiveNavItem();
});

// Also try to set active nav item after window load (fallback)
window.addEventListener('load', function() {
    console.log('Window loaded, setting active nav item...');
    setActiveNavItem();
});

// Force set active nav item after a short delay (another fallback)
setTimeout(function() {
    console.log('Timeout fallback, setting active nav item...');
    setActiveNavItem();
    debugCSS(); // Debug CSS
    forceApplyActiveStyles(); // Force apply styles
}, 100);

// Debug function to check if CSS is loaded
function debugCSS() {
    const testElement = document.createElement('div');
    testElement.className = 'nav-link active';
    testElement.style.position = 'absolute';
    testElement.style.left = '-9999px';
    document.body.appendChild(testElement);
    
    const computedStyle = window.getComputedStyle(testElement);
    console.log('CSS Debug - active background:', computedStyle.backgroundColor);
    console.log('CSS Debug - active color:', computedStyle.color);
    console.log('CSS Debug - active border-right:', computedStyle.borderRight);
    
    document.body.removeChild(testElement);
}

// Force apply active styles (fallback)
function forceApplyActiveStyles() {
    const activeLinks = document.querySelectorAll('.nav-link.active');
    console.log('Found', activeLinks.length, 'active links');
    
    activeLinks.forEach(link => {
        console.log('Applying styles to:', link.textContent.trim());
        link.style.backgroundColor = 'rgba(59, 130, 246, 0.2)';
        link.style.color = '#60a5fa';
        link.style.borderRight = '3px solid #3b82f6';
    });
}

// Initialize sidebar functionality
function initializeSidebar() {
    // Check if sidebar should be collapsed by default on mobile
    if (window.innerWidth <= 1200) {
        sidebarCollapsed = true;
        const sidebar = document.querySelector('.sidebar');
        if (sidebar) {
            sidebar.classList.add('collapsed');
        }
    }
}

// Setup sidebar event listeners
function setupSidebarEventListeners() {
    // Sidebar toggle button
    const toggleBtn = document.querySelector('.sidebar-toggle');
    if (toggleBtn) {
        toggleBtn.addEventListener('click', toggleSidebar);
    }
    
    // Mobile menu toggle
    const mobileToggleBtn = document.querySelector('.mobile-menu-toggle');
    if (mobileToggleBtn) {
        mobileToggleBtn.addEventListener('click', toggleMobileSidebar);
    }
    
    // Sidebar overlay for mobile
    const overlay = document.querySelector('.sidebar-overlay');
    if (overlay) {
        overlay.addEventListener('click', closeMobileSidebar);
    }
    
    // Close sidebar when clicking outside on mobile
    document.addEventListener('click', function(event) {
        if (window.innerWidth <= 1200) {
            const sidebar = document.querySelector('.sidebar');
            const mobileToggle = document.querySelector('.mobile-menu-toggle');
            
            if (sidebar && sidebar.classList.contains('open') && 
                !sidebar.contains(event.target) && 
                !mobileToggle.contains(event.target)) {
                closeMobileSidebar();
            }
        }
    });
    
    // Handle window resize
    window.addEventListener('resize', handleWindowResize);
}

// Toggle sidebar collapsed state
function toggleSidebar() {
    sidebarCollapsed = !sidebarCollapsed;
    const sidebar = document.querySelector('.sidebar');
    
    if (sidebar) {
        sidebar.classList.toggle('collapsed', sidebarCollapsed);
    }
    
    // Save preference to localStorage
    localStorage.setItem('sidebarCollapsed', sidebarCollapsed);
}

// Toggle mobile sidebar
function toggleMobileSidebar() {
    const sidebar = document.querySelector('.sidebar');
    const overlay = document.querySelector('.sidebar-overlay');
    
    if (sidebar) {
        sidebar.classList.toggle('open');
    }
    
    if (overlay) {
        overlay.classList.toggle('show');
    }
    
    // Prevent body scroll when sidebar is open
    document.body.style.overflow = sidebar.classList.contains('open') ? 'hidden' : '';
}

// Close mobile sidebar
function closeMobileSidebar() {
    const sidebar = document.querySelector('.sidebar');
    const overlay = document.querySelector('.sidebar-overlay');
    
    if (sidebar) {
        sidebar.classList.remove('open');
    }
    
    if (overlay) {
        overlay.classList.remove('show');
    }
    
    // Restore body scroll
    document.body.style.overflow = '';
}

// Handle window resize
function handleWindowResize() {
    const sidebar = document.querySelector('.sidebar');
    
    if (window.innerWidth > 1200) {
        // Desktop view
        sidebar.classList.remove('open');
        document.querySelector('.sidebar-overlay')?.classList.remove('show');
        document.body.style.overflow = '';
        
        // Restore collapsed state from localStorage
        const savedState = localStorage.getItem('sidebarCollapsed');
        if (savedState !== null) {
            sidebarCollapsed = savedState === 'true';
            sidebar.classList.toggle('collapsed', sidebarCollapsed);
        }
    } else {
        // Mobile view
        sidebar.classList.remove('collapsed');
        sidebar.classList.remove('open');
    }
}

// Set active navigation item based on current page
function setActiveNavItem() {
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('.nav-link');
    
    console.log('=== NAVIGATION DEBUG ===');
    console.log('Current path:', currentPath);
    console.log('Found', navLinks.length, 'nav links');
    
    navLinks.forEach((link, index) => {
        link.classList.remove('active');
        
        const href = link.getAttribute('href');
        console.log(`Link ${index}: href="${href}"`);
        
        // Exact match
        if (href === currentPath) {
            link.classList.add('active');
            console.log(`✓ EXACT MATCH: ${href}`);
        }
        // For sub-pages, match if the path starts with href + '/'
        else if (href !== '/' && currentPath.startsWith(href + '/')) {
            link.classList.add('active');
            console.log(`✓ SUB-PAGE MATCH: ${href}`);
        }
        // Special case for groups section - if we're on any groups page, highlight the main groups link
        else if (href === '/groups' && currentPath.startsWith('/groups')) {
            link.classList.add('active');
            console.log(`✓ GROUPS SECTION MATCH: ${href}`);
        }
    });
    
    console.log('=== END DEBUG ===');
}

// Utility function to show notifications
function showNotification(message, type = 'info') {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 1rem 1.5rem;
        border-radius: 0.5rem;
        color: white;
        font-weight: 500;
        z-index: 3000;
        animation: slideIn 0.3s ease;
        max-width: 400px;
        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    `;
    
    // Set background color based on type
    const colors = {
        success: '#10b981',
        error: '#ef4444',
        warning: '#f59e0b',
        info: '#3b82f6'
    };
    notification.style.backgroundColor = colors[type] || colors.info;
    
    notification.textContent = message;
    
    // Add to page
    document.body.appendChild(notification);
    
    // Remove after 3 seconds
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 300);
    }, 3000);
}

// Utility function to format currency
function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

// Utility function to format date
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN');
}

// Utility function to format date and time
function formatDateTime(dateString) {
    const date = new Date(dateString);
    return date.toLocaleString('vi-VN');
}

// Utility function to debounce function calls
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Utility function to throttle function calls
function throttle(func, limit) {
    let inThrottle;
    return function() {
        const args = arguments;
        const context = this;
        if (!inThrottle) {
            func.apply(context, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

// Export functions for use in other scripts
window.SidebarUtils = {
    toggleSidebar,
    toggleMobileSidebar,
    closeMobileSidebar,
    showNotification,
    formatCurrency,
    formatDate,
    formatDateTime,
    debounce,
    throttle
};

// Add CSS for notifications
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(100%);
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
            transform: translateX(100%);
            opacity: 0;
        }
    }
    
    .notification {
        font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    }
`;
document.head.appendChild(style);
