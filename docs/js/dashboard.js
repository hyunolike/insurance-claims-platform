/**
 * Dashboard - Claims List
 */

// Mock data for development (remove when backend is ready)
const MOCK_CLAIMS = [
    {
        id: 1,
        claimNumber: 'CLM-20260108-00001',
        policyNumber: 'POL-20260101-00001',
        claimAmount: 1500000,
        incidentDate: '2026-01-05',
        description: '교통사고로 인한 치료비 청구',
        status: 'SUBMITTED',
        submittedAt: '2026-01-08T09:30:00',
    },
    {
        id: 2,
        claimNumber: 'CLM-20260107-00002',
        policyNumber: 'POL-20260102-00002',
        claimAmount: 3200000,
        incidentDate: '2026-01-03',
        description: '화재로 인한 재산 피해 청구',
        status: 'IN_REVIEW',
        submittedAt: '2026-01-07T14:20:00',
    },
    {
        id: 3,
        claimNumber: 'CLM-20260106-00003',
        policyNumber: 'POL-20260103-00003',
        claimAmount: 850000,
        incidentDate: '2026-01-02',
        description: '입원 치료비 청구',
        status: 'APPROVED',
        submittedAt: '2026-01-06T11:15:00',
    },
    {
        id: 4,
        claimNumber: 'CLM-20260105-00004',
        policyNumber: 'POL-20260104-00004',
        claimAmount: 2100000,
        incidentDate: '2025-12-28',
        description: '수술비 청구',
        status: 'PAID',
        submittedAt: '2026-01-05T16:45:00',
    },
];

let currentFilter = 'all';
let allClaims = [];

/**
 * Initialize dashboard
 */
async function initDashboard() {
    await loadClaims();
    setupFilterButtons();
}

/**
 * Load claims from API
 */
async function loadClaims() {
    const claimsList = document.getElementById('claimsList');

    try {
        // TODO: Replace with actual API call when backend is ready
        // const claims = await api.get('/claims');

        // Use mock data for now
        allClaims = MOCK_CLAIMS;

        renderClaims(allClaims);
    } catch (error) {
        console.error('Failed to load claims:', error);
        claimsList.innerHTML = `
            <div class="empty-state">
                <h3 class="empty-title">청구 내역을 불러올 수 없습니다</h3>
                <p class="empty-text">잠시 후 다시 시도해주세요</p>
            </div>
        `;
    }
}

/**
 * Render claims list
 */
function renderClaims(claims) {
    const claimsList = document.getElementById('claimsList');

    // Filter claims based on current filter
    let filteredClaims = claims;
    if (currentFilter !== 'all') {
        filteredClaims = claims.filter(claim => claim.status === currentFilter);
    }

    if (filteredClaims.length === 0) {
        claimsList.innerHTML = `
            <div class="empty-state">
                <h3 class="empty-title">청구 내역이 없습니다</h3>
                <p class="empty-text">새로운 청구를 생성해보세요</p>
            </div>
        `;
        return;
    }

    // Sort by submission date (newest first)
    filteredClaims.sort((a, b) => new Date(b.submittedAt) - new Date(a.submittedAt));

    claimsList.innerHTML = filteredClaims.map(claim => `
        <a href="detail.html?id=${claim.id}" class="claim-card">
            <div class="claim-info">
                <h3 class="claim-number">${claim.claimNumber}</h3>
                <p class="claim-meta">${claim.policyNumber} • ${formatDate(claim.incidentDate)}</p>
            </div>
            <div class="claim-amount">${formatCurrency(claim.claimAmount)}</div>
            <div class="claim-status">
                <span class="status-badge ${claim.status}">${getStatusLabel(claim.status)}</span>
            </div>
        </a>
    `).join('');
}

/**
 * Setup filter buttons
 */
function setupFilterButtons() {
    const filterButtons = document.querySelectorAll('.filter-button');

    filterButtons.forEach(button => {
        button.addEventListener('click', () => {
            // Update active state
            filterButtons.forEach(btn => btn.classList.remove('active'));
            button.classList.add('active');

            // Update current filter
            currentFilter = button.dataset.status;

            // Re-render claims
            renderClaims(allClaims);
        });
    });
}

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initDashboard);
} else {
    initDashboard();
}