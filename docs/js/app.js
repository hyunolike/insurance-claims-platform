/**
 * Insurance Claims API Client
 * Shared utilities and API functions
 */

const API_BASE_URL = 'http://localhost:8080';

/**
 * API utility functions
 */
const api = {
    /**
     * Make a GET request
     */
    async get(endpoint) {
        try {
            const response = await fetch(`${API_BASE_URL}${endpoint}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                },
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('API GET error:', error);
            throw error;
        }
    },

    /**
     * Make a POST request
     */
    async post(endpoint, data) {
        try {
            const response = await fetch(`${API_BASE_URL}${endpoint}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(data),
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('API POST error:', error);
            throw error;
        }
    },
};

/**
 * Format currency
 */
function formatCurrency(amount) {
    return new Intl.NumberFormat('ko-KR', {
        style: 'decimal',
    }).format(amount) + '원';
}

/**
 * Format date
 */
function formatDate(dateString) {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('ko-KR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
    }).format(date);
}

/**
 * Format datetime
 */
function formatDateTime(dateString) {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('ko-KR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    }).format(date);
}

/**
 * Get status label in Korean
 */
function getStatusLabel(status) {
    const labels = {
        'SUBMITTED': '제출됨',
        'IN_REVIEW': '검토중',
        'APPROVED': '승인됨',
        'REJECTED': '거절됨',
        'PAID': '지급완료',
    };
    return labels[status] || status;
}

/**
 * Get URL parameter
 */
function getUrlParameter(name) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name);
}

/**
 * Show error message
 */
function showError(message) {
    alert(message); // Simple alert for now, can be enhanced with a toast/notification system
}
