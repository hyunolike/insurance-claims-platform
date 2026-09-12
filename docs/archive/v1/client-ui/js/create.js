/**
 * Create Claim Form
 */

/**
 * Initialize create form
 */
function initCreateForm() {
    const form = document.getElementById('claimForm');

    // Set max date to today
    const incidentDateInput = document.getElementById('incidentDate');
    const today = new Date().toISOString().split('T')[0];
    incidentDateInput.setAttribute('max', today);

    // Handle form submission
    form.addEventListener('submit', handleSubmit);
}

/**
 * Handle form submission
 */
async function handleSubmit(event) {
    event.preventDefault();

    const form = event.target;
    const formData = new FormData(form);

    // Build request payload
    const claimData = {
        policyNumber: formData.get('policyNumber'),
        claimAmount: parseFloat(formData.get('claimAmount')),
        incidentDate: formData.get('incidentDate'),
        description: formData.get('description'),
    };

    try {
        // Disable submit button
        const submitButton = form.querySelector('button[type="submit"]');
        submitButton.disabled = true;
        submitButton.textContent = '제출 중...';

        // Submit to API
        const response = await api.post('/claims', claimData);

        // Show success message
        showSuccess(response.claimNumber);

    } catch (error) {
        console.error('Failed to create claim:', error);
        showError('청구 제출에 실패했습니다. 다시 시도해주세요.');

        // Re-enable submit button
        const submitButton = form.querySelector('button[type="submit"]');
        submitButton.disabled = false;
        submitButton.textContent = '청구 제출';
    }
}

/**
 * Show success message
 */
function showSuccess(claimNumber) {
    const form = document.getElementById('claimForm');
    const successMessage = document.getElementById('successMessage');
    const claimNumberDisplay = document.getElementById('claimNumberDisplay');

    // Hide form and show success message
    form.style.display = 'none';
    successMessage.style.display = 'flex';
    claimNumberDisplay.textContent = claimNumber;
}

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initCreateForm);
} else {
    initCreateForm();
}