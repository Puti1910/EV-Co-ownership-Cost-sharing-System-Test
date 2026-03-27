document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.auth-action').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            e.preventDefault();
            const reservationId = btn.getAttribute('data-reservation-id');
            const action = btn.getAttribute('data-action');
            const status = action === 'checkin' ? 'IN_USE' : 'COMPLETED';
            await sendAuthRequest(`/admin/reservations/${reservationId}/status`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams({ status })
            });
        });
    });

    const editForm = document.getElementById('editForm');
    if (editForm) {
        editForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const formData = new FormData(editForm);
            await sendAuthRequest(editForm.action, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams(formData)
            });
        });
    }

    const deleteForm = document.getElementById('deleteForm');
    if (deleteForm) {
        deleteForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await sendAuthRequest(deleteForm.action, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
            });
        });
    }
});

async function sendAuthRequest(url, options) {
    try {
        console.log('📤 Sending request to:', url);
        const finalOptions = enrichRequestOptions(options || {});
        console.log('📦 Request options:', {
            method: finalOptions.method,
            hasAuth: !!finalOptions.headers['Authorization'],
            hasCsrf: !!finalOptions.headers['X-CSRF-TOKEN'],
            hasBody: !!finalOptions.body,
            credentials: finalOptions.credentials
        });
        
        const response = await fetch(url, finalOptions);
        
        console.log('📥 Response status:', response.status);
        console.log('📥 Response redirected:', response.redirected);
        console.log('📥 Response URL:', response.url);
        
        // Kiểm tra nếu bị redirect về login
        if (response.redirected && (response.url.includes('/login') || response.url.includes('/admin/login'))) {
            console.error('❌ Redirected to login page - session expired');
            alert('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
            window.location.href = '/admin/login';
            return;
        }
        
        if (response.redirected) {
            console.log('🔄 Following redirect to:', response.url);
            window.location.href = response.url;
            return;
        }
        
        if (!response.ok) {
            const text = await response.text();
            console.error('❌ Request failed:', response.status, text);
            alert('Lỗi: ' + (text || response.statusText));
            return;
        }
        
        console.log('✅ Request successful, reloading page');
        window.location.reload();
    } catch (error) {
        console.error('❌ Auth request error:', error);
        alert('Không thể thực hiện hành động: ' + error.message);
    }
}

// Helper function để lấy token từ cookie
function getTokenFromCookie() {
    const cookies = document.cookie.split(';');
    for (let cookie of cookies) {
        const [name, value] = cookie.trim().split('=');
        if (name === 'jwtToken' && value) {
            return value.startsWith('Bearer ') ? value : 'Bearer ' + value;
        }
    }
    return null;
}

// Helper function để lấy CSRF token từ nhiều nguồn
function getCsrfToken() {
    // Thử từ window._csrfMeta
    if (window._csrfMeta && window._csrfMeta.token) {
        return window._csrfMeta.token;
    }
    
    // Thử từ input hidden trong form
    const csrfInput = document.querySelector('input[name="_csrf"]');
    if (csrfInput) {
        return csrfInput.value;
    }
    
    // Thử từ meta tag
    const csrfMeta = document.querySelector('meta[name="_csrf"]');
    if (csrfMeta) {
        return csrfMeta.getAttribute('content');
    }
    
    return null;
}

function enrichRequestOptions(options) {
    if (!options.headers) {
        options.headers = {};
    }
    options.credentials = 'same-origin';

    // Thêm JWT token từ cookie
    const jwtToken = getTokenFromCookie();
    if (jwtToken) {
        options.headers['Authorization'] = jwtToken;
        console.log('✅ Added JWT token to Authorization header');
    } else {
        console.warn('⚠️ No JWT token found in cookie');
    }

    // Thêm CSRF token
    const csrfToken = getCsrfToken();
    const csrfMeta = window._csrfMeta || {};
    const paramName = csrfMeta.parameterName || '_csrf';
    
    if (csrfToken) {
        const contentType = options.headers['Content-Type'] || options.headers['content-type'] || '';

        if (options.body instanceof URLSearchParams) {
            options.body.append(paramName, csrfToken);
            console.log('✅ Added CSRF token to body:', paramName);
        } else if (contentType.includes('application/json')) {
            options.headers['X-CSRF-TOKEN'] = csrfToken;
            console.log('✅ Added CSRF token to header: X-CSRF-TOKEN');
        } else {
            // Mặc định thêm vào cả header và body
            options.headers['X-CSRF-TOKEN'] = csrfToken;
            if (options.body instanceof URLSearchParams) {
                options.body.append(paramName, csrfToken);
            }
            console.log('✅ Added CSRF token to header and body');
        }
    } else {
        console.warn('⚠️ No CSRF token found from any source');
        console.warn('   - window._csrfMeta:', window._csrfMeta);
        console.warn('   - CSRF input:', document.querySelector('input[name="_csrf"]'));
        console.warn('   - CSRF meta tag:', document.querySelector('meta[name="_csrf"]'));
    }

    return options;
}

