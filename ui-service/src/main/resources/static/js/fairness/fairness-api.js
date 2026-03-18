(function(window) {
    const API_BASE = typeof window.getApiBaseUrl === 'function'
        ? window.getApiBaseUrl()
        : 'http://localhost:8084';

    const jsonHeaders = {
        'Content-Type': 'application/json'
    };

    const httpClient = typeof window.authenticatedFetch === 'function'
        ? window.authenticatedFetch
        : (url, options = {}) => fetch(url, options);

    async function handleResponse(res) {
        if (!res.ok) {
            const message = await res.text();
            throw new Error(message || 'Yêu cầu không thành công');
        }
        if (res.status === 204) {
            return null;
        }
        return res.json();
    }

    async function fetchFairnessSummary(vehicleId, rangeDays = 30) {
        const url = `${API_BASE}/api/fairness/vehicles/${vehicleId}?rangeDays=${rangeDays}`;
        const res = await httpClient(url, { headers: jsonHeaders });
        return handleResponse(res);
    }

    async function requestFairnessSuggestion(vehicleId, payload) {
        const url = `${API_BASE}/api/fairness/vehicles/${vehicleId}/suggest`;
        const res = await httpClient(url, {
            method: 'POST',
            headers: jsonHeaders,
            body: JSON.stringify(payload)
        });
        return handleResponse(res);
    }

    async function issueCheckpoint(reservationId, payload) {
        const url = `${API_BASE}/api/reservations/${reservationId}/checkpoints`;
        const res = await httpClient(url, {
            method: 'POST',
            headers: jsonHeaders,
            body: JSON.stringify(payload)
        });
        return handleResponse(res);
    }

    async function listCheckpoints(reservationId) {
        const url = `${API_BASE}/api/reservations/${reservationId}/checkpoints`;
        const res = await httpClient(url);
        return handleResponse(res);
    }

    async function scanCheckpoint(payload) {
        const url = `${API_BASE}/api/reservations/checkpoints/scan`;
        const res = await httpClient(url, {
            method: 'POST',
            headers: jsonHeaders,
            body: JSON.stringify(payload)
        });
        return handleResponse(res);
    }

    async function signCheckpoint(checkpointId, payload) {
        const url = `${API_BASE}/api/reservations/checkpoints/${checkpointId}/sign`;
        const res = await httpClient(url, {
            method: 'POST',
            headers: jsonHeaders,
            body: JSON.stringify(payload)
        });
        return handleResponse(res);
    }

    window.FairnessAPI = {
        fetchFairnessSummary,
        requestFairnessSuggestion,
        issueCheckpoint,
        listCheckpoints,
        scanCheckpoint,
        signCheckpoint
    };
})(window);

