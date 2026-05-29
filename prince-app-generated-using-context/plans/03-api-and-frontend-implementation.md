# Tax Collection and Debt Recovery System - Implementation Plan
## Part 3: API and Frontend Implementation

**Context Reference:** ctx_99a057884357  
**Focus:** REST API endpoints, OpenAPI documentation, and vanilla HTML/CSS/JavaScript frontend

---

## 1. REST API Structure

```
infrastructure/web/
├── controller/
│   ├── CitizenController.java
│   ├── PrepaymentController.java
│   ├── TaxCreditController.java
│   ├── PaymentController.java
│   ├── DebtController.java
│   └── AllocationController.java
├── dto/
│   ├── request/
│   │   ├── CreatePrepaymentRequest.java
│   │   ├── ConfirmPrepaymentRequest.java
│   │   ├── CreatePaymentRequest.java
│   │   ├── CreateDebtRequest.java
│   │   └── CreateAllocationRequest.java
│   └── response/
│       ├── PrepaymentResponse.java
│       ├── TaxCreditResponse.java
│       ├── PaymentResponse.java
│       ├── DebtResponse.java
│       └── AllocationResponse.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── ErrorResponse.java
└── config/
    └── OpenApiConfig.java
```

---

## 2. API Endpoints Design

### 2.1 Prepayment API

**POST /api/v1/prepayments**
- Create a new prepayment
- Request body: CreatePrepaymentRequest
- Response: 201 Created with PrepaymentResponse
- Idempotency: Use idempotency-key header

**POST /api/v1/prepayments/{id}/confirm** (Webhook)
- Confirm prepayment from payment gateway
- Request body: ConfirmPrepaymentRequest
- Response: 200 OK
- Signature verification required

**GET /api/v1/prepayments/{id}**
- Get prepayment details
- Response: 200 OK with PrepaymentResponse

**GET /api/v1/citizens/{citizenId}/prepayments**
- List all prepayments for a citizen
- Query params: status, type, page, size
- Response: 200 OK with paginated list

### 2.2 Tax Credit API

**GET /api/v1/citizens/{citizenId}/tax-credit**
- Get tax credit balance
- Response: 200 OK with TaxCreditResponse

**GET /api/v1/citizens/{citizenId}/tax-credit/events**
- Get tax credit event history
- Query params: page, size, eventType
- Response: 200 OK with paginated events

### 2.3 Payment API

**POST /api/v1/payments**
- Record a bank transfer payment
- Request body: CreatePaymentRequest
- Response: 201 Created with PaymentResponse

**GET /api/v1/payments/{id}**
- Get payment details
- Response: 200 OK with PaymentResponse

**GET /api/v1/citizens/{citizenId}/payments**
- List all payments for a citizen
- Query params: status, page, size
- Response: 200 OK with paginated list

### 2.4 Debt API

**POST /api/v1/debts**
- Create a new debt (admin only)
- Request body: CreateDebtRequest
- Response: 201 Created with DebtResponse

**GET /api/v1/debts/{id}**
- Get debt details
- Response: 200 OK with DebtResponse

**GET /api/v1/citizens/{citizenId}/debts**
- List all debts for a citizen
- Query params: status, type, page, size
- Response: 200 OK with paginated list

**PUT /api/v1/debts/{id}/cancel**
- Cancel a debt (admin only)
- Response: 200 OK

### 2.5 Allocation API

**POST /api/v1/allocations**
- Create manual allocation (admin only)
- Request body: CreateAllocationRequest
- Response: 201 Created with AllocationResponse

**GET /api/v1/allocations/{id}**
- Get allocation details
- Response: 200 OK with AllocationResponse

**GET /api/v1/citizens/{citizenId}/allocations**
- List all allocations for a citizen
- Query params: status, type, page, size
- Response: 200 OK with paginated list

---

## 3. Controller Implementation Examples

### 3.1 PrepaymentController

```java
package com.taxauthority.debtrecovery.infrastructure.web.controller;

import com.taxauthority.debtrecovery.application.usecase.CreatePrepaymentUseCase;
import com.taxauthority.debtrecovery.application.usecase.ConfirmPrepaymentUseCase;
import com.taxauthority.debtrecovery.infrastructure.web.dto.request.CreatePrepaymentRequest;
import com.taxauthority.debtrecovery.infrastructure.web.dto.response.PrepaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prepayments")
@RequiredArgsConstructor
@Tag(name = "Prepayments", description = "Prepayment management API")
public class PrepaymentController {
    
    private final CreatePrepaymentUseCase createPrepaymentUseCase;
    private final ConfirmPrepaymentUseCase confirmPrepaymentUseCase;
    
    @PostMapping
    @Operation(summary = "Create a new prepayment")
    public ResponseEntity<PrepaymentResponse> createPrepayment(
        @Valid @RequestBody CreatePrepaymentRequest request,
        @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        PrepaymentResponse response = createPrepaymentUseCase.execute(
            request,
            idempotencyKey
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm prepayment (webhook)")
    public ResponseEntity<Void> confirmPrepayment(
        @PathVariable UUID id,
        @RequestHeader("X-Payment-Signature") String signature,
        @Valid @RequestBody ConfirmPrepaymentRequest request
    ) {
        confirmPrepaymentUseCase.execute(id, request, signature);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get prepayment details")
    public ResponseEntity<PrepaymentResponse> getPrepayment(@PathVariable UUID id) {
        // Implementation
        return ResponseEntity.ok().build();
    }
}
```

### 3.2 TaxCreditController

```java
package com.taxauthority.debtrecovery.infrastructure.web.controller;

import com.taxauthority.debtrecovery.application.usecase.GetTaxCreditBalanceUseCase;
import com.taxauthority.debtrecovery.infrastructure.web.dto.response.TaxCreditResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/citizens/{citizenId}/tax-credit")
@RequiredArgsConstructor
@Tag(name = "Tax Credit", description = "Tax credit management API")
public class TaxCreditController {
    
    private final GetTaxCreditBalanceUseCase getTaxCreditBalanceUseCase;
    
    @GetMapping
    @Operation(summary = "Get tax credit balance")
    public ResponseEntity<TaxCreditResponse> getTaxCreditBalance(
        @PathVariable UUID citizenId
    ) {
        TaxCreditResponse response = getTaxCreditBalanceUseCase.execute(citizenId);
        return ResponseEntity.ok(response);
    }
}
```

---

## 4. DTO Definitions

### 4.1 Request DTOs

```java
package com.taxauthority.debtrecovery.infrastructure.web.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreatePrepaymentRequest {
    
    @NotNull(message = "Citizen ID is required")
    private String citizenId;
    
    @NotNull(message = "Prepayment type is required")
    @Pattern(regexp = "VAT|ADVANCE_PAYMENT", message = "Invalid prepayment type")
    private String prepaymentType;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least 1.00")
    private BigDecimal amount;
    
    @NotNull(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}
```

### 4.2 Response DTOs

```java
package com.taxauthority.debtrecovery.infrastructure.web.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PrepaymentResponse {
    private String prepaymentId;
    private String prepaymentCode;
    private String citizenId;
    private String prepaymentType;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String description;
    private Instant createdAt;
    private Instant confirmedAt;
}

@Data
public class TaxCreditResponse {
    private String taxCreditId;
    private String citizenId;
    private BigDecimal totalCredit;
    private BigDecimal allocatedCredit;
    private BigDecimal availableCredit;
    private String currency;
    private Instant updatedAt;
}
```

---

## 5. OpenAPI Configuration

### 5.1 OpenApiConfig

```java
package com.taxauthority.debtrecovery.infrastructure.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI taxDebtRecoveryOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Tax Collection and Debt Recovery API")
                .description("REST API for managing tax prepayments, credits, and debt settlement")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Tax Authority")
                    .email("support@taxauthority.gov")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Development server"),
                new Server()
                    .url("https://api.taxauthority.gov")
                    .description("Production server")
            ));
    }
}
```

---

## 6. Frontend Structure

```
src/main/resources/static/
├── index.html
├── css/
│   ├── main.css
│   ├── components.css
│   └── responsive.css
├── js/
│   ├── app.js
│   ├── api.js
│   ├── components/
│   │   ├── dashboard.js
│   │   ├── prepayment-form.js
│   │   ├── tax-credit-display.js
│   │   ├── debt-list.js
│   │   └── allocation-history.js
│   └── utils/
│       ├── formatter.js
│       └── validator.js
└── assets/
    ├── images/
    └── icons/
```

---

## 7. Frontend Implementation

### 7.1 index.html

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tax Collection & Debt Recovery</title>
    <link rel="stylesheet" href="/css/main.css">
    <link rel="stylesheet" href="/css/components.css">
    <link rel="stylesheet" href="/css/responsive.css">
</head>
<body>
    <header class="header">
        <div class="container">
            <h1>Tax Collection & Debt Recovery System</h1>
            <nav class="nav">
                <a href="#dashboard" class="nav-link active">Dashboard</a>
                <a href="#prepayments" class="nav-link">Prepayments</a>
                <a href="#debts" class="nav-link">Debts</a>
                <a href="#allocations" class="nav-link">Allocations</a>
            </nav>
        </div>
    </header>

    <main class="main">
        <div class="container">
            <!-- Dashboard Section -->
            <section id="dashboard" class="section active">
                <h2>Citizen Dashboard</h2>
                
                <!-- Tax Credit Balance Card -->
                <div class="card">
                    <h3>Tax Credit Balance</h3>
                    <div id="tax-credit-display">
                        <div class="balance-item">
                            <span class="label">Total Credit:</span>
                            <span class="value" id="total-credit">€0.00</span>
                        </div>
                        <div class="balance-item">
                            <span class="label">Allocated:</span>
                            <span class="value" id="allocated-credit">€0.00</span>
                        </div>
                        <div class="balance-item highlight">
                            <span class="label">Available:</span>
                            <span class="value" id="available-credit">€0.00</span>
                        </div>
                    </div>
                </div>

                <!-- Outstanding Debts Card -->
                <div class="card">
                    <h3>Outstanding Debts</h3>
                    <div id="debts-summary">
                        <p>Loading...</p>
                    </div>
                </div>

                <!-- Quick Actions -->
                <div class="card">
                    <h3>Quick Actions</h3>
                    <div class="actions">
                        <button class="btn btn-primary" onclick="showPrepaymentForm()">
                            Make Prepayment
                        </button>
                        <button class="btn btn-secondary" onclick="viewDebts()">
                            View All Debts
                        </button>
                    </div>
                </div>
            </section>

            <!-- Prepayments Section -->
            <section id="prepayments" class="section">
                <h2>Make a Prepayment</h2>
                
                <div class="card">
                    <form id="prepayment-form">
                        <div class="form-group">
                            <label for="prepayment-type">Prepayment Type</label>
                            <select id="prepayment-type" required>
                                <option value="">Select type...</option>
                                <option value="VAT">VAT Prepayment</option>
                                <option value="ADVANCE_PAYMENT">Advance Payment</option>
                            </select>
                        </div>

                        <div class="form-group">
                            <label for="amount">Amount (EUR)</label>
                            <input type="number" id="amount" min="1" step="0.01" required>
                        </div>

                        <div class="form-group">
                            <label for="description">Description (Optional)</label>
                            <textarea id="description" rows="3"></textarea>
                        </div>

                        <div class="form-actions">
                            <button type="submit" class="btn btn-primary">
                                Submit Prepayment
                            </button>
                            <button type="button" class="btn btn-secondary" onclick="cancelForm()">
                                Cancel
                            </button>
                        </div>
                    </form>
                </div>

                <!-- Prepayment History -->
                <div class="card">
                    <h3>Prepayment History</h3>
                    <div id="prepayment-history">
                        <p>Loading...</p>
                    </div>
                </div>
            </section>

            <!-- Debts Section -->
            <section id="debts" class="section">
                <h2>My Debts</h2>
                <div id="debts-list">
                    <p>Loading...</p>
                </div>
            </section>

            <!-- Allocations Section -->
            <section id="allocations" class="section">
                <h2>Allocation History</h2>
                <div id="allocations-list">
                    <p>Loading...</p>
                </div>
            </section>
        </div>
    </main>

    <footer class="footer">
        <div class="container">
            <p>&copy; 2026 Tax Authority. All rights reserved.</p>
        </div>
    </footer>

    <script src="/js/utils/formatter.js"></script>
    <script src="/js/utils/validator.js"></script>
    <script src="/js/api.js"></script>
    <script src="/js/components/dashboard.js"></script>
    <script src="/js/components/prepayment-form.js"></script>
    <script src="/js/components/tax-credit-display.js"></script>
    <script src="/js/components/debt-list.js"></script>
    <script src="/js/components/allocation-history.js"></script>
    <script src="/js/app.js"></script>
</body>
</html>
```

### 7.2 api.js (API Client)

```javascript
// API client for backend communication
const API_BASE_URL = '/api/v1';

const api = {
    // Tax Credit API
    async getTaxCredit(citizenId) {
        const response = await fetch(`${API_BASE_URL}/citizens/${citizenId}/tax-credit`);
        if (!response.ok) throw new Error('Failed to fetch tax credit');
        return response.json();
    },

    // Prepayment API
    async createPrepayment(data) {
        const idempotencyKey = generateIdempotencyKey();
        const response = await fetch(`${API_BASE_URL}/prepayments`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Idempotency-Key': idempotencyKey
            },
            body: JSON.stringify(data)
        });
        if (!response.ok) throw new Error('Failed to create prepayment');
        return response.json();
    },

    async getPrepayments(citizenId, params = {}) {
        const queryString = new URLSearchParams(params).toString();
        const response = await fetch(
            `${API_BASE_URL}/citizens/${citizenId}/prepayments?${queryString}`
        );
        if (!response.ok) throw new Error('Failed to fetch prepayments');
        return response.json();
    },

    // Debt API
    async getDebts(citizenId, params = {}) {
        const queryString = new URLSearchParams(params).toString();
        const response = await fetch(
            `${API_BASE_URL}/citizens/${citizenId}/debts?${queryString}`
        );
        if (!response.ok) throw new Error('Failed to fetch debts');
        return response.json();
    },

    // Allocation API
    async getAllocations(citizenId, params = {}) {
        const queryString = new URLSearchParams(params).toString();
        const response = await fetch(
            `${API_BASE_URL}/citizens/${citizenId}/allocations?${queryString}`
        );
        if (!response.ok) throw new Error('Failed to fetch allocations');
        return response.json();
    }
};

function generateIdempotencyKey() {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}
```

### 7.3 prepayment-form.js

```javascript
// Prepayment form component
class PrepaymentForm {
    constructor() {
        this.form = document.getElementById('prepayment-form');
        this.init();
    }

    init() {
        this.form.addEventListener('submit', (e) => this.handleSubmit(e));
    }

    async handleSubmit(event) {
        event.preventDefault();

        const formData = {
            citizenId: getCurrentCitizenId(),
            prepaymentType: document.getElementById('prepayment-type').value,
            amount: parseFloat(document.getElementById('amount').value),
            currency: 'EUR',
            description: document.getElementById('description').value
        };

        try {
            this.showLoading();
            const response = await api.createPrepayment(formData);
            this.showSuccess(response);
            this.form.reset();
            // Refresh dashboard
            await refreshDashboard();
        } catch (error) {
            this.showError(error.message);
        } finally {
            this.hideLoading();
        }
    }

    showLoading() {
        const submitBtn = this.form.querySelector('button[type="submit"]');
        submitBtn.disabled = true;
        submitBtn.textContent = 'Processing...';
    }

    hideLoading() {
        const submitBtn = this.form.querySelector('button[type="submit"]');
        submitBtn.disabled = false;
        submitBtn.textContent = 'Submit Prepayment';
    }

    showSuccess(response) {
        alert(`Prepayment created successfully! Code: ${response.prepaymentCode}`);
    }

    showError(message) {
        alert(`Error: ${message}`);
    }
}
```

### 7.4 main.css

```css
/* Main styles */
:root {
    --primary-color: #2563eb;
    --secondary-color: #64748b;
    --success-color: #10b981;
    --danger-color: #ef4444;
    --warning-color: #f59e0b;
    --background-color: #f8fafc;
    --card-background: #ffffff;
    --text-primary: #1e293b;
    --text-secondary: #64748b;
    --border-color: #e2e8f0;
}

* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
    background-color: var(--background-color);
    color: var(--text-primary);
    line-height: 1.6;
}

.container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
}

/* Header */
.header {
    background-color: var(--card-background);
    border-bottom: 1px solid var(--border-color);
    padding: 1rem 0;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.header h1 {
    font-size: 1.5rem;
    margin-bottom: 1rem;
}

.nav {
    display: flex;
    gap: 1rem;
}

.nav-link {
    padding: 0.5rem 1rem;
    text-decoration: none;
    color: var(--text-secondary);
    border-radius: 0.375rem;
    transition: all 0.2s;
}

.nav-link:hover,
.nav-link.active {
    background-color: var(--primary-color);
    color: white;
}

/* Main content */
.main {
    padding: 2rem 0;
}

.section {
    display: none;
}

.section.active {
    display: block;
}

/* Cards */
.card {
    background-color: var(--card-background);
    border-radius: 0.5rem;
    padding: 1.5rem;
    margin-bottom: 1.5rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.card h3 {
    margin-bottom: 1rem;
    color: var(--text-primary);
}

/* Forms */
.form-group {
    margin-bottom: 1rem;
}

.form-group label {
    display: block;
    margin-bottom: 0.5rem;
    font-weight: 500;
    color: var(--text-primary);
}

.form-group input,
.form-group select,
.form-group textarea {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid var(--border-color);
    border-radius: 0.375rem;
    font-size: 1rem;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
    outline: none;
    border-color: var(--primary-color);
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

/* Buttons */
.btn {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 0.375rem;
    font-size: 1rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
}

.btn-primary {
    background-color: var(--primary-color);
    color: white;
}

.btn-primary:hover {
    background-color: #1d4ed8;
}

.btn-secondary {
    background-color: var(--secondary-color);
    color: white;
}

.btn-secondary:hover {
    background-color: #475569;
}

.form-actions {
    display: flex;
    gap: 1rem;
    margin-top: 1.5rem;
}

/* Balance display */
.balance-item {
    display: flex;
    justify-content: space-between;
    padding: 0.75rem 0;
    border-bottom: 1px solid var(--border-color);
}

.balance-item:last-child {
    border-bottom: none;
}

.balance-item.highlight {
    font-weight: 600;
    font-size: 1.25rem;
    color: var(--primary-color);
}

/* Footer */
.footer {
    background-color: var(--card-background);
    border-top: 1px solid var(--border-color);
    padding: 2rem 0;
    margin-top: 4rem;
    text-align: center;
    color: var(--text-secondary);
}
```

---

## 8. Implementation Checklist

### REST API
- [ ] Implement all controller classes
- [ ] Create request/response DTOs
- [ ] Add validation annotations
- [ ] Implement global exception handler
- [ ] Configure OpenAPI/Swagger
- [ ] Add API documentation
- [ ] Implement CORS configuration

### Frontend
- [ ] Create HTML structure
- [ ] Implement CSS styling
- [ ] Create API client (api.js)
- [ ] Implement dashboard component
- [ ] Implement prepayment form
- [ ] Implement debt list display
- [ ] Implement allocation history
- [ ] Add form validation
- [ ] Add error handling
- [ ] Add loading states
- [ ] Test responsive design

### Integration
- [ ] Connect frontend to backend API
- [ ] Test all user flows
- [ ] Implement error messages
- [ ] Add success notifications
- [ ] Test with H2 console

---

## 9. Testing Strategy

### API Testing
- Integration tests for all endpoints
- Validation testing
- Error handling testing
- Idempotency testing

### Frontend Testing
- Manual testing of all user flows
- Cross-browser testing
- Responsive design testing
- Form validation testing

---

## Summary

This plan provides:
1. Complete REST API structure with OpenAPI documentation
2. Vanilla HTML/CSS/JavaScript frontend
3. Clear separation of concerns
4. Comprehensive implementation checklist

The implementation follows hexagonal architecture principles with clear boundaries between layers.