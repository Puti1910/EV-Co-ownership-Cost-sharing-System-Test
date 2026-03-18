(function () {
    const API_BASE_URL = typeof window.getApiBaseUrl === 'function'
        ? window.getApiBaseUrl()
        : 'http://localhost:8084';
    const REFRESH_ENDPOINT = `${API_BASE_URL}/api/auth/users/refresh`;

    let refreshPromise = null;

    async function requestNewAccessToken() {
        if (refreshPromise) {
            return refreshPromise;
        }

        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
            return null;
        }

        refreshPromise = fetch(REFRESH_ENDPOINT, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken })
        })
            .then(async response => {
                if (!response.ok) {
                    throw new Error('Không thể làm mới phiên làm việc.');
                }
                const session = await response.json();
                if (typeof window.saveAuthSession === 'function') {
                    window.saveAuthSession(session);
                }
                return session;
            })
            .catch(error => {
                if (typeof window.clearAuthSession === 'function') {
                    window.clearAuthSession();
                } else {
                    localStorage.clear();
                }
                console.warn('Refresh token failed:', error);
                return null;
            })
            .finally(() => {
                refreshPromise = null;
            });

        return refreshPromise;
    }

    async function authenticatedFetch(url, options = {}, allowRetry = true) {
        const headers = new Headers(options.headers || {});
        const token = localStorage.getItem('jwtToken');
        if (token && !headers.has('Authorization')) {
            headers.set('Authorization', `Bearer ${token}`);
        }

        const requestConfig = {
            ...options,
            headers
        };

        let response;
        try {
            response = await fetch(url, requestConfig);
        } catch (error) {
            throw error;
        }

        if (response.status === 401 && allowRetry && localStorage.getItem('refreshToken')) {
            const refreshed = await requestNewAccessToken();
            if (refreshed) {
                return authenticatedFetch(url, options, false);
            }
        }

        return response;
    }

    window.authenticatedFetch = authenticatedFetch;
})();

