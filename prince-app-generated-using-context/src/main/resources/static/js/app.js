// Tax Debt Recovery System - Frontend Application
// Context: ctx_99a057884357

const API_BASE_URL = '/api/v1';
let currentCitizenId = null;

// Utility functions
function formatCurrency(amount) {
    return new Intl.NumberFormat('en-BE', {
        style: 'currency',
        currency: 'EUR'
    }).format(amount);
}

function formatDate(dateString) {
    return new Date(dateString).toLocaleDateString('en-BE');
}

function getStatusBadgeClass(status) {
    const statusMap = {
        'COMPLETED': 'badge-success',
        'PENDING_PAYMENT': 'badge-warning',
        'FAILED': 'badge-danger',
        'CANCELLED': 'badge-danger',
        'OPEN': 'badge-warning',
        'PARTIALLY_PAID': 'badge-info',
        'PAID': 'badge-success'
    };
    return statusMap[status] || 'badge-info';
}

// Navigation
function showSection(sectionId) {
    // Hide all sections
    document.querySelectorAll('.section').forEach(section => {
        section.classList.remove('active');
    });
    
    // Show selected section
    document.getElementById(sectionId).classList.add('active');
    
    // Update nav links
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.remove('active');
    });
    event.target.classList.add('active');
}

// Load citizen data
async function loadCitizenData() {
    const selector = document.getElementById('citizen-selector');
    currentCitizenId = selector.value;
    
    if (!currentCitizenId) {
        return;
    }
    
    try {
        await Promise.all([
            loadTaxCredit(),
            loadDebts(),
            loadPrepayments()
        ]);
    } catch (error) {
        console.error('Error loading citizen data:', error);
        showError('Failed to load citizen data');
    }
}

// Load tax credit balance
async function loadTaxCredit() {
    if (!currentCitizenId) return;
    
    try {
        const response = await fetch(`${API_BASE_URL}/citizens/${currentCitizenId}/tax-credit`);
        const taxCredit = await response.json();
        
        document.getElementById('total-credit').textContent = formatCurrency(taxCredit.totalCredit);
        document.getElementById('allocated-credit').textContent = formatCurrency(taxCredit.allocatedCredit);
        document.getElementById('available-credit').textContent = formatCurrency(taxCredit.availableCredit);
    } catch (error) {
        console.error('Error loading tax credit:', error);
    }
}

// Load debts
async function loadDebts() {
    if (!currentCitizenId) return;
    
    try {
        const response = await fetch(`${API_BASE_URL}/citizens/${currentCitizenId}/debts`);
        const debts = await response.json();
        
        // Update dashboard summary
        const summaryDiv = document.getElementById('debts-summary');
        if (debts.length === 0) {
            summaryDiv.innerHTML = '<p>No outstanding debts</p>';
        } else {
            const openDebts = debts.filter(d => d.status === 'OPEN' || d.status === 'PARTIALLY_PAID');
            const totalOutstanding = openDebts.reduce((sum, d) => sum + d.outstandingAmount, 0);
            
            summaryDiv.innerHTML = `
                <div class="balance-item">
                    <span class="label">Number of debts:</span>
                    <span class="value">${openDebts.length}</span>
                </div>
                <div class="balance-item highlight">
                    <span class="label">Total outstanding:</span>
                    <span class="value">${formatCurrency(totalOutstanding)}</span>
                </div>
            `;
        }
        
        // Update debts list
        const debtsListDiv = document.getElementById('debts-list');
        if (debts.length === 0) {
            debtsListDiv.innerHTML = '<p>No debts found</p>';
        } else {
            debtsListDiv.innerHTML = `
                <table>
                    <thead>
                        <tr>
                            <th>Debt Code</th>
                            <th>Type</th>
                            <th>Original Amount</th>
                            <th>Outstanding</th>
                            <th>Due Date</th>
                            <th>Status</th>
                            <th>Reference</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${debts.map(debt => `
                            <tr>
                                <td>${debt.debtCode}</td>
                                <td>${debt.debtType}</td>
                                <td>${formatCurrency(debt.originalAmount)}</td>
                                <td>${formatCurrency(debt.outstandingAmount)}</td>
                                <td>${formatDate(debt.dueDate)}</td>
                                <td><span class="badge ${getStatusBadgeClass(debt.status)}">${debt.status}</span></td>
                                <td><code>${debt.structuredReference}</code></td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            `;
        }
    } catch (error) {
        console.error('Error loading debts:', error);
    }
}

// Load prepayments
async function loadPrepayments() {
    if (!currentCitizenId) return;
    
    try {
        const response = await fetch(`${API_BASE_URL}/citizens/${currentCitizenId}/prepayments`);
        const prepayments = await response.json();
        
        const historyDiv = document.getElementById('prepayment-history');
        if (prepayments.length === 0) {
            historyDiv.innerHTML = '<p>No prepayments yet</p>';
        } else {
            historyDiv.innerHTML = `
                <table>
                    <thead>
                        <tr>
                            <th>Code</th>
                            <th>Type</th>
                            <th>Amount</th>
                            <th>Status</th>
                            <th>Created</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${prepayments.map(prep => `
                            <tr>
                                <td>${prep.prepaymentCode}</td>
                                <td>${prep.prepaymentType}</td>
                                <td>${formatCurrency(prep.amount)}</td>
                                <td><span class="badge ${getStatusBadgeClass(prep.status)}">${prep.status}</span></td>
                                <td>${formatDate(prep.createdAt)}</td>
                                <td>
                                    ${prep.status === 'PENDING_PAYMENT' ? 
                                        `<button class="btn btn-success" onclick="confirmPrepayment('${prep.prepaymentId}')">Confirm</button>` : 
                                        '-'}
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            `;
        }
    } catch (error) {
        console.error('Error loading prepayments:', error);
    }
}

// Submit prepayment
async function submitPrepayment(event) {
    event.preventDefault();
    
    if (!currentCitizenId) {
        showError('Please select a citizen first');
        return;
    }
    
    const form = document.getElementById('prepayment-form');
    const formData = {
        citizenId: currentCitizenId,
        prepaymentType: document.getElementById('prepayment-type').value,
        amount: parseFloat(document.getElementById('amount').value),
        currency: 'EUR',
        description: document.getElementById('description').value
    };
    
    try {
        const response = await fetch(`${API_BASE_URL}/prepayments`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Idempotency-Key': generateIdempotencyKey()
            },
            body: JSON.stringify(formData)
        });
        
        if (!response.ok) {
            throw new Error('Failed to create prepayment');
        }
        
        const prepayment = await response.json();
        showSuccess(`Prepayment created successfully! Code: ${prepayment.prepaymentCode}`);
        form.reset();
        
        // Reload data
        await loadPrepayments();
    } catch (error) {
        console.error('Error creating prepayment:', error);
        showError('Failed to create prepayment: ' + error.message);
    }
}

// Confirm prepayment (simulates payment gateway confirmation)
async function confirmPrepayment(prepaymentId) {
    if (!confirm('Confirm this prepayment? This simulates payment gateway confirmation.')) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/prepayments/${prepaymentId}/confirm`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                gatewayReference: 'GW-' + Date.now()
            })
        });
        
        if (!response.ok) {
            throw new Error('Failed to confirm prepayment');
        }
        
        showSuccess('Prepayment confirmed! Tax credit has been updated.');
        
        // Reload data
        await Promise.all([
            loadTaxCredit(),
            loadPrepayments()
        ]);
    } catch (error) {
        console.error('Error confirming prepayment:', error);
        showError('Failed to confirm prepayment: ' + error.message);
    }
}

// Utility functions
function generateIdempotencyKey() {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

function showError(message) {
    alert('Error: ' + message);
}

function showSuccess(message) {
    alert('Success: ' + message);
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    console.log('Tax Debt Recovery System initialized');
    console.log('Context: ctx_99a057884357');
});

// Made with Bob
