// auth-utils.js

(function () {
    const DEFAULT_GATEWAY_URL = window.API_GATEWAY_BASE_URL || 'http://localhost:8084';
    window.API_GATEWAY_BASE_URL = DEFAULT_GATEWAY_URL;

    function getApiBaseUrl() {
        return window.API_GATEWAY_BASE_URL || DEFAULT_GATEWAY_URL;
    }

    function clearAuthSession() {
    localStorage.removeItem('jwtToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userId');
    localStorage.removeItem('userName');
        localStorage.removeItem('userEmail');
    localStorage.removeItem('userRole');
        localStorage.removeItem('profileStatus');
        document.cookie = 'jwtToken=; Max-Age=0; path=/';
    }

    function saveAuthSession(session) {
        if (!session) return;
        if (session.accessToken) {
            localStorage.setItem('jwtToken', session.accessToken);
        }
        if (session.refreshToken) {
            localStorage.setItem('refreshToken', session.refreshToken);
        }
        if (session.userId) {
            localStorage.setItem('userId', session.userId);
}
        if (session.fullName) {
            localStorage.setItem('userName', session.fullName);
        }
        if (session.email) {
            localStorage.setItem('userEmail', session.email);
        }
        if (session.role) {
            localStorage.setItem('userRole', session.role);
        }
        if (session.profileStatus) {
            localStorage.setItem('profileStatus', session.profileStatus);
        }
        if (session.accessToken) {
            // Set cookie with proper attributes for cross-origin requests
            const cookieValue = `jwtToken=${session.accessToken}; path=/; max-age=86400; SameSite=Lax; Secure=false`;
            document.cookie = cookieValue;
            console.log('JWT cookie set:', cookieValue.substring(0, 50) + '...');
        }
    }

    async function logout(options = {}) {
        const { skipRemote = false, redirectUrl = '/auth/login' } = options;
        const refreshToken = localStorage.getItem('refreshToken');
        if (!skipRemote && refreshToken) {
            try {
                await fetch(`${getApiBaseUrl()}/api/auth/users/logout`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ refreshToken })
                });
            } catch (error) {
                console.warn('Không thể gửi yêu cầu logout:', error);
            }
        }
        clearAuthSession();
        window.location.href = redirectUrl;
    }

function checkAuthAndLoadUser() {
        const token = localStorage.getItem('jwtToken');
        if (!token) {
            return;
        }
        const userName = localStorage.getItem('userName');
        const userNameDisplay = document.getElementById('userNameDisplay');
        if (userNameDisplay && userName && userName !== 'null') {
            userNameDisplay.textContent = userName;
        }
    }

    function updateStoredProfileStatus(status) {
        if (status) {
            localStorage.setItem('profileStatus', status);
        }
    }

    window.getApiBaseUrl = getApiBaseUrl;
    window.saveAuthSession = saveAuthSession;
    window.clearAuthSession = clearAuthSession;
    window.logout = logout;
    window.updateStoredProfileStatus = updateStoredProfileStatus;

    document.addEventListener('DOMContentLoaded', checkAuthAndLoadUser);
})();