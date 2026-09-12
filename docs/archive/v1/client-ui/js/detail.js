/**
 * Claim Detail View
 */

// Mock data for development (should match dashboard mock data)
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

/**
 * Initialize detail view
 */
async function initDetailView() {
    const claimId = getUrlParameter('id');

    if (!claimId) {
        showError('유효하지 않은 청구 ID입니다.');
        window.location.href = 'index.html';
        return;
    }

    await loadClaimDetail(claimId);
}

/**
 * Load claim detail from API
 */
async function loadClaimDetail(claimId) {
    const loadingState = document.getElementById('loadingState');
    const detailContent = document.getElementById('detailContent');

    try {
        // TODO: Replace with actual API call when backend is ready
        // const claim = await api.get(`/claims/${claimId}`);

        // Use mock data for now
        const claim = MOCK_CLAIMS.find(c => c.id === parseInt(claimId));

        if (!claim) {
            throw new Error('Claim not found');
        }

        renderClaimDetail(claim);

        // Hide loading, show content
        loadingState.style.display = 'none';
        detailContent.style.display = 'block';

    } catch (error) {
        console.error('Failed to load claim detail:', error);
        loadingState.innerHTML = `
            <div class="empty-state">
                <h3 class="empty-title">청구 정보를 불러올 수 없습니다</h3>
                <p class="empty-text">잠시 후 다시 시도해주세요</p>
            </div>
        `;
    }
}

/**
 * Render claim detail
 */
function renderClaimDetail(claim) {
    // Update claim number and status
    document.getElementById('claimNumber').textContent = claim.claimNumber;

    const statusBadge = document.getElementById('statusBadge');
    statusBadge.textContent = getStatusLabel(claim.status);
    statusBadge.className = `status-badge ${claim.status}`;

    // Update claim info
    document.getElementById('policyNumber').textContent = claim.policyNumber;
    document.getElementById('claimAmount').textContent = formatCurrency(claim.claimAmount);
    document.getElementById('incidentDate').textContent = formatDate(claim.incidentDate);
    document.getElementById('submittedAt').textContent = formatDateTime(claim.submittedAt);
    document.getElementById('description').textContent = claim.description;

    // Render status timeline
    renderStatusTimeline(claim);
}

/**
 * Render status timeline
 */
function renderStatusTimeline(claim) {
    const timeline = document.getElementById('statusTimeline');

    // Build timeline based on current status
    const statuses = ['SUBMITTED', 'IN_REVIEW', 'APPROVED', 'PAID'];
    const currentStatusIndex = statuses.indexOf(claim.status);

    // For rejected claims, show different timeline
    if (claim.status === 'REJECTED') {
        timeline.innerHTML = `
            <div class="timeline-item">
                <div class="timeline-dot"></div>
                <div class="timeline-content">
                    <p class="timeline-status">${getStatusLabel('SUBMITTED')}</p>
                    <p class="timeline-date">${formatDate(claim.submittedAt)}</p>
                </div>
            </div>
            <div class="timeline-item">
                <div class="timeline-dot"></div>
                <div class="timeline-content">
                    <p class="timeline-status">${getStatusLabel('REJECTED')}</p>
                    <p class="timeline-date">심사 거절됨</p>
                </div>
            </div>
        `;
        return;
    }

    // Render timeline for normal flow
    let timelineHTML = '';

    for (let i = 0; i <= currentStatusIndex; i++) {
        const status = statuses[i];
        const isLast = i === currentStatusIndex;

        timelineHTML += `
            <div class="timeline-item">
                <div class="timeline-dot"></div>
                <div class="timeline-content">
                    <p class="timeline-status">${getStatusLabel(status)}</p>
                    <p class="timeline-date">${isLast ? '진행중' : formatDate(claim.submittedAt)}</p>
                </div>
            </div>
        `;
    }

    timeline.innerHTML = timelineHTML;
}

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initDetailView);
} else {
    initDetailView();
}