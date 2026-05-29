***

# FUNCTIONAL & TECHNICAL ANALYSIS TEMPLATE

## Document metadata

*   **Product / System:** `Tax Collection and Debt Recovery System`
*   **Program / Release:** `MVP0 — Phase 1`
*   **Author:** `Bob`
*   **Contributors:** `Technical Team`
*   **Status:** `Draft`
*   **Version:** `1.0`
*   **Date:** `2026-05-19`
*   **Related references:**
    *   Backlog / Epic: `TBD`
    *   Mockups: `TBD`
    *   OpenAPI / Postman: `TBD`
    *   ADR (decisions): `TBD`
    *   Runbook / Ops: `TBD`

***

## Document structure

This document is organized according to the hierarchy:

*   **EPIC** → **FEATURE** → **USER STORY** → **TECHNICAL ANALYSIS**

**Recommended numbering rules:**

*   EPIC: `E-<n>` (e.g., `E-1`)
*   FEATURE: `F-<epic>.<n>` (e.g., `F-1.1`)
*   USER STORY: `US-<epic>.<feature>.<n>` (e.g., `US-1.1.1`)
*   Business rules: `RG-<NNN>`
*   Edge cases: `EC-<NNN>`
*   Patterns/regex: `RGX-<NNN>`
*   UI errors: `ERR_<SCOPE>_<NAME>`
*   API errors: `API_<DOMAIN>_<NAME>`
*   Tests: `UT_<...>`, `IT_<...>`, `PERF_<...>`, `SEC_<...>`, `A11Y_<...>`

***

# EPIC `E-1`: `Tax Collection and Debt Recovery Management`

## Epic objective

*   **Business problem:** Citizens need a transparent and efficient system to manage tax prepayments, settle debts through various payment methods, and track how their payments are allocated to outstanding debts. The tax authority needs automated allocation rules, complete audit trails, and reconciliation capabilities.
*   **Value:** 
    *   Automated debt settlement reduces manual processing by 80%
    *   Real-time allocation tracking improves citizen satisfaction
    *   Complete accounting audit trail ensures regulatory compliance
    *   Reduced payment processing errors by 95%
*   **Out of scope:** 
    *   Tax calculation and assessment
    *   Penalty and interest calculation
    *   Payment plan negotiations
    *   Legal enforcement procedures
*   **Major constraints:** 
    *   Must comply with financial regulations and audit requirements
    *   All operations must be idempotent and traceable
    *   System must handle concurrent allocations without double-spending
    *   Accounting entries must be immutable once created

***

# FEATURE `F-1.1`: `Prepayment and Tax Credit Management`

## Feature objective

*   **Goal:** Enable citizens to make prepayments (VAT and advance payments) that feed into a tax credit system, which can then be automatically allocated to outstanding debts
*   **Inputs / Triggers:** 
    *   Citizen initiates prepayment (VAT or advance payment)
    *   External payment confirmation from payment gateway
    *   Automatic allocation rules triggered by credit availability
*   **Outputs:** 
    *   Tax credit created and updated
    *   Accounting entries generated
    *   Allocation records created
    *   Citizen notification of credit balance
*   **Dependencies:** 
    *   Payment gateway integration
    *   Debt aggregate for allocation targets
    *   Accounting/Ledger service for audit trail

***

# FEATURE `F-1.2`: `Direct Debt Payment via Bank Transfer`

## Feature objective

*   **Goal:** Allow citizens to settle debts directly via bank transfer using structured references for automatic debt identification
*   **Inputs / Triggers:** 
    *   Bank transfer with structured reference
    *   Payment confirmation from banking system
*   **Outputs:** 
    *   Payment aggregate created
    *   Allocation to identified debt
    *   Accounting entries generated
    *   Debt balance updated
*   **Dependencies:** 
    *   Banking integration for payment notifications
    *   Structured reference validation system
    *   Debt aggregate
    *   Allocation aggregate

***

# FEATURE `F-1.3`: `Allocation and Debt Settlement`

## Feature objective

*   **Goal:** Manage the allocation of payments and tax credits to debts, tracking partial and full settlements with complete audit trail
*   **Inputs / Triggers:** 
    *   Available tax credit
    *   Received payment
    *   Automatic allocation rules
    *   Manual allocation requests
*   **Outputs:** 
    *   Allocation records (credit-to-debt or payment-to-debt)
    *   Updated debt balances
    *   Accounting entries
    *   Settlement confirmations
*   **Dependencies:** 
    *   TaxCredit aggregate
    *   Payment aggregate
    *   Debt aggregate
    *   Accounting/Ledger service

***

## USER STORY `US-1.1.1`: `Citizen Makes VAT Prepayment`

### 1. Context and objective

**User Story reference**

*   **ID:** `US-1.1.1`
*   **Title:** `Citizen Makes VAT Prepayment`
*   **Priority:** `High`
*   **As a:** `Citizen taxpayer`
*   **I want:** `To make a VAT prepayment that creates a tax credit`
*   **So that:** `I can build up credit that will automatically reduce my future tax debts`

**Technical objective**

*   Create a prepayment transaction that generates a TaxCredit aggregate
*   Record the prepayment in the accounting ledger
*   Trigger automatic allocation rules if debts exist
*   Ensure idempotency and complete audit trail

**Technical prerequisites**

*   PostgreSQL database with event sourcing support
*   Payment gateway integration (Stripe/Mollie)
*   Kafka for event streaming
*   Redis for idempotency checks
*   Accounting/Ledger microservice

**Assumptions & out of scope**

*   Assumptions: 
    *   Payment gateway handles payment validation
    *   Citizen is authenticated and authorized
    *   VAT prepayment rules are pre-configured
*   Out of scope: 
    *   Payment gateway implementation
    *   Tax calculation logic
    *   Refund processing

***

### 2. Detailed functional specifications

#### 2.1 User flow

```
Step 1: Citizen Dashboard
    ↓
Step 2: Select "Make Prepayment"
    ↓
Step 3: Choose Prepayment Type (VAT / Advance Payment)
    ↓
Step 4: Enter Amount
    ↓
Step 5: Payment Gateway Redirect
    ↓
Decision: Payment Successful?
    ├─ Yes → Step 6: Create TaxCredit
    └─ No  → Step 7: Show Error
    ↓
Step 6: TaxCredit Created
    ↓
Step 7: Trigger Allocation Rules
    ↓
Step 8: Generate Accounting Entries
    ↓
Success: Credit Available for Allocation
```

#### 2.2 Screen / step specification

##### Screen/Step `1`: `Citizen Dashboard`

**UI components**

*   Page title: "My Tax Account"
*   Current credit balance card
*   Outstanding debts summary
*   "Make Prepayment" button (primary action)
*   Transaction history table
*   Loading state: skeleton loaders
*   Empty state: "No transactions yet"
*   Error state: error banner with retry option

**Rules**

*   Navigation:
    *   "Make Prepayment" → Prepayment form
    *   Transaction row click → Transaction details
*   Enable/Disable:
    *   "Make Prepayment" enabled if citizen is authenticated
*   Behavior:
    *   Auto-refresh balance every 30 seconds
    *   Show notification badge if new allocations occurred
*   Accessibility:
    *   Focus trap in modals
    *   Screen reader announces balance changes
    *   High contrast mode support
    *   Minimum touch target 44x44px

##### Screen/Step `2`: `Prepayment Form`

**UI components**

*   Form title: "Make a Prepayment"
*   Prepayment type selector (radio buttons: VAT / Advance Payment)
*   Amount input field (currency formatted)
*   Description field (optional)
*   Payment method selector
*   Total amount display
*   "Cancel" button (secondary)
*   "Proceed to Payment" button (primary)
*   Validation error messages inline

**Rules**

*   Navigation:
    *   "Cancel" → Dashboard
    *   "Proceed to Payment" → Payment gateway
*   Enable/Disable:
    *   "Proceed to Payment" enabled if form valid and amount > 0
*   Behavior:
    *   Real-time amount validation
    *   Auto-format currency on blur
    *   Confirmation modal before redirect
*   Accessibility:
    *   Form labels properly associated
    *   Error messages announced
    *   Keyboard navigation support

***

#### 2.3 Validations (client-side) & normalizations

**Validation rules per field**

*   Field: `prepaymentType`
    *   Required: `Yes`
    *   Values: `VAT | ADVANCE_PAYMENT`
    *   UI error messages: `ERR_PREPAY_TYPE_REQUIRED`

*   Field: `amount`
    *   Required: `Yes`
    *   Length: `max 10 digits`
    *   Allowed characters: `digits and decimal point`
    *   Pattern: `RGX-001`
    *   Normalization: `trim, format to 2 decimals`
    *   UI error messages: `ERR_PREPAY_AMOUNT_INVALID`

*   Field: `description`
    *   Required: `No`
    *   Length: `max 500 characters`
    *   Normalization: `trim`

**Patterns (referenced by ID)**

*   `RGX-001`: `^\d{1,10}(\.\d{1,2})?$` (positive decimal with max 2 decimal places)
*   `RGX-002`: `^[A-Z0-9]{12}$` (structured reference format)

**Real-time validation**

*   Indicators: `✓ green checkmark / ✗ red cross`
*   Triggers: `onBlur for amount, onChange for type`
*   Debounce: `300ms for amount field`
*   Async checks: `None for prepayment form`

***

#### 2.4 Draft / resume (if applicable)

*   Not applicable for prepayment form (security reasons - no draft storage)

***

### 3. API contract

#### 3.1 Endpoint: `Create Prepayment`

*   **Method & URL:**

```
POST /api/v1/prepayments
```

*   **Required headers**
    *   `Authorization`: `Bearer <JWT>` — Authentication token
    *   `Content-Type`: `application/json`
    *   `X-Idempotency-Key`: `UUID` — Prevents duplicate submissions

*   **Query parameters**
    *   None

*   **Request body**

```json
{
  "citizenId": "CIT-550e8400-e29b-41d4-a716-446655440000",
  "prepaymentType": "VAT",
  "amount": 1500.00,
  "currency": "EUR",
  "description": "Q1 2026 VAT prepayment",
  "paymentMethod": "CREDIT_CARD"
}
```

*   **Constraints (summary)**
    *   `citizenId`: Must be valid UUID, must match authenticated user
    *   `prepaymentType`: Must be VAT or ADVANCE_PAYMENT
    *   `amount`: Must be positive, max 2 decimal places, min 1.00
    *   `currency`: Must be EUR (for MVP)
    *   `paymentMethod`: Must be supported payment method

*   **Response codes**
    *   `201`: Prepayment created, payment gateway URL returned
    *   `400`: Validation error (invalid amount, type, etc.)
    *   `401`: Authentication failed
    *   `403`: Citizen not authorized
    *   `409`: Duplicate idempotency key
    *   `422`: Business rule violation
    *   `500`: Server error

*   **Response examples**

Success response:

```json
{
  "status": "success",
  "data": {
    "prepaymentId": "PREP-550e8400-e29b-41d4-a716-446655440001",
    "citizenId": "CIT-550e8400-e29b-41d4-a716-446655440000",
    "prepaymentType": "VAT",
    "amount": 1500.00,
    "currency": "EUR",
    "status": "PENDING_PAYMENT",
    "paymentGatewayUrl": "https://payment.gateway/checkout/abc123",
    "createdAt": "2026-05-19T11:30:00Z"
  }
}
```

Error response:

```json
{
  "error": "API_PREPAY_AMOUNT_INVALID",
  "message": "Prepayment amount must be at least 1.00 EUR",
  "details": [
    {
      "field": "amount",
      "message": "Amount must be >= 1.00",
      "rejectedValue": "0.50"
    }
  ],
  "correlationId": "trace-550e8400-e29b-41d4-a716-446655440002"
}
```

#### 3.2 Endpoint: `Confirm Prepayment (Webhook)`

*   **Method & URL:**

```
POST /api/v1/prepayments/{prepaymentId}/confirm
```

*   **Required headers**
    *   `X-Webhook-Signature`: `string` — Payment gateway signature
    *   `Content-Type`: `application/json`

*   **Request body**

```json
{
  "prepaymentId": "PREP-550e8400-e29b-41d4-a716-446655440001",
  "paymentStatus": "COMPLETED",
  "paymentReference": "PAY-GW-123456789",
  "paidAmount": 1500.00,
  "paidAt": "2026-05-19T11:35:00Z"
}
```

*   **Response codes**
    *   `200`: Prepayment confirmed, TaxCredit created
    *   `400`: Invalid webhook payload
    *   `401`: Invalid webhook signature
    *   `404`: Prepayment not found
    *   `409`: Prepayment already confirmed
    *   `500`: Server error

#### 3.3 Endpoint: `Get Tax Credit Balance`

*   **Method & URL:**

```
GET /api/v1/citizens/{citizenId}/tax-credit
```

*   **Required headers**
    *   `Authorization`: `Bearer <JWT>`

*   **Response codes**
    *   `200`: Tax credit details returned
    *   `401`: Authentication failed
    *   `403`: Not authorized to view this citizen's credit
    *   `404`: Citizen not found

*   **Response example**

```json
{
  "status": "success",
  "data": {
    "taxCreditId": "TC-550e8400-e29b-41d4-a716-446655440003",
    "citizenId": "CIT-550e8400-e29b-41d4-a716-446655440000",
    "totalCredit": 1500.00,
    "allocatedCredit": 0.00,
    "availableCredit": 1500.00,
    "currency": "EUR",
    "lastUpdated": "2026-05-19T11:35:00Z"
  }
}
```

***

### 4. Data model

#### 4.1 Table/Collection: `prepayments`

*   **Objective:** Store prepayment transactions before payment confirmation
*   **Columns / Attributes:**
    *   `prepayment_id`: `UUID` — `PRIMARY KEY` — Unique identifier
    *   `citizen_id`: `UUID` — `NOT NULL, FOREIGN KEY` — Reference to citizen
    *   `prepayment_type`: `VARCHAR(20)` — `NOT NULL, CHECK IN ('VAT', 'ADVANCE_PAYMENT')` — Type of prepayment
    *   `amount`: `DECIMAL(12,2)` — `NOT NULL, CHECK > 0` — Prepayment amount
    *   `currency`: `VARCHAR(3)` — `NOT NULL, DEFAULT 'EUR'` — Currency code
    *   `description`: `TEXT` — `NULL` — Optional description
    *   `payment_method`: `VARCHAR(50)` — `NOT NULL` — Payment method used
    *   `status`: `VARCHAR(20)` — `NOT NULL, CHECK IN ('PENDING_PAYMENT', 'COMPLETED', 'FAILED', 'CANCELLED')` — Payment status
    *   `payment_gateway_reference`: `VARCHAR(255)` — `NULL` — External payment reference
    *   `idempotency_key`: `UUID` — `NOT NULL, UNIQUE` — Prevents duplicates
    *   `created_at`: `TIMESTAMP` — `NOT NULL, DEFAULT NOW()` — Creation timestamp
    *   `updated_at`: `TIMESTAMP` — `NOT NULL, DEFAULT NOW()` — Last update timestamp
    *   `confirmed_at`: `TIMESTAMP` — `NULL` — Payment confirmation timestamp
*   **Indexes**
    *   `idx_prepayments_citizen_id`: For querying by citizen
    *   `idx_prepayments_status`: For filtering by status
    *   `idx_prepayments_idempotency_key`: For duplicate detection
    *   `idx_prepayments_created_at`: For time-based queries
*   **Constraints / DB rules**
    *   `amount` must be positive
    *   `status` transitions must be valid (PENDING → COMPLETED/FAILED/CANCELLED)
    *   `confirmed_at` must be NULL if status is PENDING_PAYMENT
*   **Identifier strategy**
    *   Prefix `PREP-` + UUID v4
*   **Audit strategy**
    *   `created_at`, `updated_at` timestamps
    *   Append-only for completed prepayments
    *   Status changes logged in separate audit table

#### 4.2 Table/Collection: `tax_credits`

*   **Objective:** Aggregate root for managing citizen tax credit balances
*   **Columns / Attributes:**
    *   `tax_credit_id`: `UUID` — `PRIMARY KEY` — Unique identifier
    *   `citizen_id`: `UUID` — `NOT NULL, UNIQUE, FOREIGN KEY` — One credit per citizen
    *   `total_credit`: `DECIMAL(12,2)` — `NOT NULL, DEFAULT 0, CHECK >= 0` — Total credit accumulated
    *   `allocated_credit`: `DECIMAL(12,2)` — `NOT NULL, DEFAULT 0, CHECK >= 0` — Credit already allocated
    *   `available_credit`: `DECIMAL(12,2)` — `GENERATED ALWAYS AS (total_credit - allocated_credit)` — Computed available credit
    *   `currency`: `VARCHAR(3)` — `NOT NULL, DEFAULT 'EUR'` — Currency code
    *   `version`: `INTEGER` — `NOT NULL, DEFAULT 1` — Optimistic locking version
    *   `created_at`: `TIMESTAMP` — `NOT NULL, DEFAULT NOW()` — Creation timestamp
    *   `updated_at`: `TIMESTAMP` — `NOT NULL, DEFAULT NOW()` — Last update timestamp
*   **Indexes**
    *   `idx_tax_credits_citizen_id`: For citizen lookup
    *   `idx_tax_credits_available_credit`: For allocation queries
*   **Constraints / DB rules**
    *   `allocated_credit` <= `total_credit` (enforced by CHECK constraint)
    *   `available_credit` must always be >= 0
    *   Optimistic locking on `version` to prevent concurrent modification
*   **Identifier strategy**
    *   Prefix `TC-` + UUID v4
*   **Audit strategy**
    *   Event sourcing: all changes recorded in `tax_credit_events` table
    *   `updated_at` timestamp on every change
    *   Version incremented on every update

#### 4.3 Table/Collection: `tax_credit_events`

*   **Objective:** Event sourcing for tax credit changes (audit trail)
*   **Columns / Attributes:**
    *   `event_id`: `UUID` — `PRIMARY KEY` — Unique event identifier
    *   `tax_credit_id`: `UUID` — `NOT NULL, FOREIGN KEY` — Reference to tax credit
    *   `event_type`: `VARCHAR(50)` — `NOT NULL` — Type of event (CREDIT_ADDED, CREDIT_ALLOCATED, etc.)
    *   `amount`: `DECIMAL(12,2)` — `NOT NULL` — Amount involved in event
    *   `source_type`: `VARCHAR(50)` — `NOT NULL` — Source of credit (PREPAYMENT, REFUND, etc.)
    *   `source_id`: `UUID` — `NOT NULL` — Reference to source entity
    *   `metadata`: `JSONB` — `NULL` — Additional event data
    *   `created_at`: `TIMESTAMP` — `NOT NULL, DEFAULT NOW()` — Event timestamp
*   **Indexes**
    *   `idx_tax_credit_events_tax_credit_id`: For event replay
    *   `idx_tax_credit_events_created_at`: For time-based queries
*   **Constraints / DB rules**
    *   Append-only (no updates or deletes)
    *   Events must be ordered by `created_at`
*   **Identifier strategy**
    *   UUID v4
*   **Audit strategy**
    *   Immutable event log
    *   Retained indefinitely for audit purposes

#### 4.4 Table/Collection: `payments`

*   **Objective:** Aggregate root for direct debt payments via bank transfer
*   **Columns / Attributes:**
    *   `payment_id`: `UUID` — `PRIMARY KEY` — Unique identifier
    *   `citizen_id`: `UUID` — `NOT NULL, FOREIGN KEY` — Reference to citizen
    *   `amount`: `DECIMAL(12,2)` — `NOT NULL, CHECK > 0` — Payment amount
    *   `currency`: `VARCHAR(3)` — `NOT NULL, DEFAULT 'EUR'` — Currency code
    *   `structured_reference`: `VARCHAR(20)` — `NOT NULL, UNIQUE` — Bank transfer reference
    *   `target_debt_id`: `UUID` — `NULL, FOREIGN KEY` — Identified debt (NULL if not yet matched)
    *   `payment_method`: `VARCHAR(50)` — `NOT NULL, DEFAULT 'BANK_TRANSFER'` — Payment method
    *   `bank_reference`: `VARCHAR(255)` — `NULL` — Bank transaction reference
    *   `status`: `VARCHAR(20)` — `NOT NULL, CHECK IN ('RECEIVED', 'ALLOCATED', 'PARTIALLY_ALLOCATED', 'UNALLOCATED')` — Payment status
    *   `received_at`: `TIMESTAMP` — `NOT NULL` — When payment was received
    *   `allocated_at`: `TIMESTAMP` — `NULL` — When fully allocated
    *   `created_at`: `TIMESTAMP` — `NOT NULL, DEFAULT NOW()` — Creation timestamp
    *   `updated_at`: `TIMESTAMP` — `NOT NULL, DEFAULT NOW()` — Last update timestamp
*   **Indexes**
    *   `idx_payments_citizen_id`: For citizen queries
    *   `idx_payments_structured_reference`: For reference lookup
    *   `idx_payments_status`: For status filtering
    *   `idx_payments_received_at`: For time-based queries
*   **Constraints / DB rules**
    *   `structured_reference` must match format RGX-002
    *   `allocated_at` must be NULL if status is RECEIVED or PARTIALLY_ALLOCATED
*   **Identifier strategy**
    *   Prefix `PAY-` + UUID v4
*   **Audit strategy**
    *   `created_at`, `updated_at` timestamps
    *   Status changes logged in audit table
    *   Immutable once fully allocated

#### 4.5 Table/Collection: `debts`

*   **Objective:** Aggregate root for managing citizen debts
*   **Columns / Attributes:**
    *   `debt_id`: `UUID` — `PRIMARY KEY` — Unique identifier
    *   `citizen_id`: `UUID` — `NOT NULL, FOREIGN KEY` — Reference to citizen
    *   `debt_code`: `VARCHAR(50)` — `NOT NULL, UNIQUE` — Human-readable debt code
    *   `debt_type`: `VARCHAR(50)` — `NOT NULL` — Type of debt (TAX, PENALTY, etc.)
    *   `original_amount`: `DECIMAL(12,2)` — `NOT NULL, CHECK > 0` — Original debt amount
    *   `outstanding_amount`: `DECIMAL(12,2)` — `NOT NULL, CHECK >= 0` — Current outstanding amount
    *   `currency`: `VARCHAR(3)` — `NOT NULL, DEFAULT 'EUR'` — Currency code
    *   `due_date`: `DATE` — `NOT NULL` — Payment due date
    *   `status`: `VARCHAR(20)` — `NOT NULL, CHECK IN ('OPEN', 'PARTIALLY_PAID', 'PAID', 'CANCELLED')` — Debt status
    *   `priority`: `INTEGER` — `NOT NULL, DEFAULT 1` — Allocation priority (1=highest)
    *   `structured_reference`: `VARCHAR(20)` — `NOT NULL, UNIQUE` — For bank transfer identification
    *   `version`: `INTEGER` — `NOT NULL, DEFAULT 1` — Optimistic locking version
    *   `created_at`: `TIMESTAMP` — `NOT NULL, DEFAULT NOW()` — Creation timestamp
    *   `updated_at`: `TIMESTAMP` — `NOT NULL, DEFAULT NOW()` — Last update timestamp
    *   `settled_at`: `TIMESTAMP` — `NULL` — When fully paid
*   **Indexes**
    *   `idx_debts_citizen_id`: For citizen queries
    *   `idx_debts_structured_reference`: For payment matching
    *   `idx_debts_status`: For status filtering
    *   `idx_debts_priority_due_date`: For allocation ordering
*   **Constraints / DB rules**
    *   `outstanding_amount` <= `original_amount`
    *   `settled_at` must be NULL if status is not PAID
    *   `status` = PAID when `outstanding_amount` = 0
*   **Identifier strategy**
    *   Prefix `DEBT-` + UUID v4
    *   `debt_code` format: `YYYY-NNNNNN` (year + sequence)
*   **Audit strategy**
    *   Event sourcing: all changes in `debt_events` table
    *   Version incremented on every update
    *   Immutable once status is PAID or CANCELLED

#### 4.6 Table/Collection: `allocations`

*   **Objective:** Aggregate root for tracking allocation of credits/payments to debts
*   **Columns / Attributes:**
    *   `allocation_id`: `UUID` — `PRIMARY KEY` — Unique identifier
    *   `allocation_type`: `VARCHAR(50)` — `NOT NULL, CHECK IN ('CREDIT_TO_DEBT', 'PAYMENT_TO_DEBT')` — Type of allocation
    *   `source_id`: `UUID` — `NOT NULL` — Reference to TaxCredit or Payment
    *   `target_debt_id`: `UUID` — `NOT NULL, FOREIGN KEY` — Reference to Debt
    *   `amount`: `DECIMAL(12,2)` — `NOT NULL, CHECK > 0` — Allocated amount
    *   `currency`: `VARCHAR(3)` — `NOT NULL, DEFAULT 'EUR'` — Currency code
    *   `allocation_rule`: `VARCHAR(100)` — `NULL` — Rule that triggered allocation (if automatic)
    *   `status`: `VARCHAR(20)` — `NOT NULL, CHECK IN ('PENDING', 'APPLIED', 'REVERSED')` — Allocation status
    *   `applied_at`: `TIMESTAMP` — `NULL` — When allocation was applied to debt
    *   `reversed_at`: `TIMESTAMP` — `NULL` — When allocation was reversed (if applicable)
    *   `created_at`: `TIMESTAMP` — `NOT NULL, DEFAULT NOW()` — Creation timestamp
*   **Indexes**
    *   `idx_allocations_source_id`: For source queries
    *   `idx_allocations_target_debt_id`: For debt queries
    *   `idx_allocations_status`: For status filtering
    *   `idx_allocations_created_at`: For time-based queries
*   **Constraints / DB rules**
    *   `applied_at` must be NULL if status is PENDING
    *   `reversed_at` must be NULL if status is not REVERSED
    *   Cannot allocate more than available credit/payment amount
*   **Identifier strategy**
    *   Prefix `ALLOC-` + UUID v4
*   **Audit strategy**
    *   Immutable once status is APPLIED
    *   Reversal creates new allocation record with REVERSED status
    *   All changes logged in accounting ledger

#### 4.7 Table/Collection: `accounting_entries`

*   **Objective:** Immutable accounting ledger for all financial operations
*   **Columns / Attributes:**
    *   `entry_id`: `UUID` — `PRIMARY KEY` — Unique identifier
    *   `entry_type`: `VARCHAR(50)` — `NOT NULL` — Type of entry (DEBIT, CREDIT)
    *   `account`: `VARCHAR(100)` — `NOT NULL` — Account code
    *   `amount`: `DECIMAL(12,2)` — `NOT NULL, CHECK > 0` — Entry amount
    *   `currency`: `VARCHAR(3)` — `NOT NULL, DEFAULT 'EUR'` — Currency code
    *   `operation_type`: `VARCHAR(50)` — `NOT NULL` — Operation that triggered entry
    *   `operation_id`: `UUID` — `NOT NULL` — Reference to source operation
    *   `description`: `TEXT` — `NOT NULL` — Entry description
    *   `metadata`: `JSONB` — `NULL` — Additional entry data
    *   `created_at`: `TIMESTAMP` — `NOT NULL, DEFAULT NOW()` — Entry timestamp
*   **Indexes**
    *   `idx_accounting_entries_operation_id`: For operation lookup
    *   `idx_accounting_entries_account`: For account queries
    *   `idx_accounting_entries_created_at`: For time-based queries
*   **Constraints / DB rules**
    *   Append-only (no updates or deletes)
    *   Double-entry bookkeeping: every operation creates balanced entries
*   **Identifier strategy**
    *   UUID v4
*   **Audit strategy**
    *   Immutable ledger
    *   Retained indefinitely
    *   Cryptographic hash chain for tamper detection

#### 4.2 Migrations

*   Tool: `Flyway`
*   Scripts: 
    *   `V001__create_prepayments_table.sql`
    *   `V002__create_tax_credits_table.sql`
    *   `V003__create_tax_credit_events_table.sql`
    *   `V004__create_payments_table.sql`
    *   `V005__create_debts_table.sql`
    *   `V006__create_allocations_table.sql`
    *   `V007__create_accounting_entries_table.sql`
    *   `V008__create_indexes.sql`
*   Rollback: `Forward-only migrations (no rollback for production)`

***

### 5. Business rules and validations (server-side)

*   **RG-001:** `Prepayment Minimum Amount`
    *   Description: All prepayments must be at least 1.00 EUR to prevent micro-transactions
    *   Valid example: `amount = 1.00`
    *   Invalid example: `amount = 0.50`
    *   Verification: Service layer validation before payment gateway redirect

*   **RG-002:** `Tax Credit Cannot Be Negative`
    *   Description: Available tax credit must always be >= 0. Allocations that would result in negative credit are rejected
    *   Valid example: `available_credit = 100, allocation = 50 → new available = 50`
    *   Invalid example: `available_credit = 100, allocation = 150 → REJECTED`
    *   Verification: Database CHECK constraint + optimistic locking in TaxCredit aggregate

*   **RG-003:** `Allocation Priority Rules`
    *   Description: When automatically allocating tax credit to debts, prioritize by: (1) priority field, (2) due date (oldest first), (3) original amount (largest first)
    *   Valid example: `Debt A (priority=1, due=2026-01-01) allocated before Debt B (priority=2, due=2026-01-01)`
    *   Invalid example: `Random allocation without considering priority`
    *   Verification: Allocation service business logic with unit tests

*   **RG-004:** `Structured Reference Uniqueness`
    *   Description: Each debt must have a unique structured reference for bank transfer identification. Format: 12 alphanumeric characters
    *   Valid example: `structured_reference = "ABC123456789"`
    *   Invalid example: `structured_reference = "ABC123"` (too short)
    *   Verification: Database UNIQUE constraint + format validation (RGX-002)

*   **RG-005:** `Payment Allocation Completeness`
    *   Description: A payment must be fully allocated before being marked as ALLOCATED status. Partial allocations keep status as PARTIALLY_ALLOCATED
    *   Valid example: `payment_amount = 100, total_allocations = 100 → status = ALLOCATED`
    *   Invalid example: `payment_amount = 100, total_allocations = 50 → status = ALLOCATED` (should be PARTIALLY_ALLOCATED)
    *   Verification: Payment aggregate business logic with state machine validation

*   **RG-006:** `Debt Settlement Calculation`
    *   Description: Debt outstanding_amount is reduced by sum of all APPLIED allocations. When outstanding_amount reaches 0, status becomes PAID
    *   Valid example: `original = 1000, allocations = [500, 500] → outstanding = 0, status = PAID`
    *   Invalid example: `original = 1000, allocations = [600] → outstanding = 400, status = PAID` (should be PARTIALLY_PAID)
    *   Verification: Debt aggregate business logic + database trigger

*   **RG-007:** `Idempotency Enforcement`
    *   Description: All prepayment requests must include unique idempotency key. Duplicate keys return existing prepayment without creating new one
    *   Valid example: `First request with key X creates prepayment, second request with key X returns same prepayment`
    *   Invalid example: `Two requests with same key create two prepayments`
    *   Verification: Redis cache check + database UNIQUE constraint on idempotency_key

*   **RG-008:** `Accounting Double-Entry`
    *   Description: Every financial operation must generate balanced accounting entries (total debits = total credits)
    *   Valid example: `Prepayment 1000 → DEBIT Bank 1000, CREDIT TaxCredit 1000`
    *   Invalid example: `Prepayment 1000 → DEBIT Bank 1000` (missing credit entry)
    *   Verification: Accounting service validation + database transaction

*   **RG-009:** `Concurrent Allocation Prevention`
    *   Description: Use optimistic locking to prevent concurrent allocations from the same tax credit or payment
    *   Valid example: `Two concurrent allocations, first succeeds, second fails with version conflict`
    *   Invalid example: `Two concurrent allocations both succeed, causing double-spending`
    *   Verification: Database version column + application-level retry logic

*   **RG-010:** `Allocation Reversal Rules`
    *   Description: Allocations can only be reversed if the debt has not been marked as PAID. Reversal creates compensating allocation with REVERSED status
    *   Valid example: `Debt status = PARTIALLY_PAID → allocation can be reversed`
    *   Invalid example: `Debt status = PAID → allocation reversal rejected`
    *   Verification: Allocation service business logic with status checks

***

### 6. Error handling

#### 6.1 UI errors (client-side)

*   `ERR_PREPAY_TYPE_REQUIRED`: `Please select a prepayment type (VAT or Advance Payment)`
    *   Trigger: Form submission without selecting type
    *   Expected action: User must select a type
    *   Severity: `Blocking`

*   `ERR_PREPAY_AMOUNT_INVALID`: `Please enter a valid amount (minimum 1.00 EUR)`
    *   Trigger: Amount field validation fails
    *   Expected action: User must enter valid amount
    *   Severity: `Blocking`

*   `ERR_PAYMENT_GATEWAY_TIMEOUT`: `Payment gateway is temporarily unavailable. Please try again in a few minutes.`
    *   Trigger: Payment gateway redirect fails
    *   Expected action: User should retry later
    *   Severity: `Warning`

*   `ERR_CREDIT_BALANCE_LOAD_FAILED`: `Unable to load your credit balance. Please refresh the page.`
    *   Trigger: API call to get credit balance fails
    *   Expected action: User should refresh page
    *   Severity: `Warning`

#### 6.2 API errors (server-side)

*   `API_PREPAY_AMOUNT_INVALID` associated with HTTP `400`
    *   Cause: Prepayment amount below minimum or invalid format
    *   Remediation: Client should validate amount before submission
    *   Observability: `WARN level log, increment validation_error_count metric`

*   `API_PREPAY_DUPLICATE_IDEMPOTENCY` associated with HTTP `409`
    *   Cause: Duplicate idempotency key submitted
    *   Remediation: Return existing prepayment details
    *   Observability: `INFO level log, increment duplicate_request_count metric`

*   `API_CREDIT_INSUFFICIENT_BALANCE` associated with HTTP `422`
    *   Cause: Attempting to allocate more credit than available
    *   Remediation: Refresh credit balance and retry with valid amount
    *   Observability: `WARN level log, increment business_rule_violation_count metric`

*   `API_DEBT_NOT_FOUND` associated with HTTP `404`
    *   Cause: Structured reference does not match any existing debt
    *   Remediation: Verify reference or create manual allocation
    *   Observability: `WARN level log, increment debt_lookup_failure_count metric`

*   `API_ALLOCATION_CONCURRENT_MODIFICATION` associated with HTTP `409`
    *   Cause: Optimistic locking conflict during allocation
    *   Remediation: Retry allocation with fresh data
    *   Observability: `INFO level log, increment concurrency_conflict_count metric`

*   `API_ACCOUNTING_ENTRY_FAILED` associated with HTTP `500`
    *   Cause: Accounting ledger service unavailable or validation failed
    *   Remediation: Operation rolled back, retry after service recovery
    *   Observability: `ERROR level log, increment accounting_failure_count metric, trigger alert`

#### 6.3 Retry / timeouts (if applicable)

*   Timeout: `30 seconds for payment gateway redirects`
*   Auto-retry: `Yes for 5xx errors, No for 4xx errors`
*   Policy:
    *   5xx: `3 attempts with exponential backoff (1s, 2s, 4s)`
    *   429: `Respect Retry-After header, max 60s backoff`
    *   4xx: `No retry, return error to user`

***

### 7. Limit cases and edge cases

*   **EC-001:** `Concurrent Credit Allocation`
    *   Scenario: Two allocation requests for the same tax credit arrive simultaneously
    *   Expected behavior: First allocation succeeds, second fails with version conflict error
    *   Mechanism: Optimistic locking on tax_credits.version field + database transaction isolation

*   **EC-002:** `Payment Gateway Webhook Replay`
    *   Scenario: Payment gateway sends duplicate confirmation webhooks
    *   Expected behavior: First webhook creates TaxCredit, subsequent webhooks are ignored
    *   Mechanism: Idempotency check on prepayment_id + status validation

*   **EC-003:** `Partial Payment Allocation`
    *   Scenario: Payment amount is less than smallest outstanding debt
    *   Expected behavior: Payment remains in PARTIALLY_ALLOCATED status until more funds available
    *   Mechanism: Allocation service tracks remaining payment balance

*   **EC-004:** `Debt Overpayment`
    *   Scenario: Allocation amount exceeds debt outstanding balance
    *   Expected behavior: Allocation is capped at outstanding amount, excess remains in credit/payment
    *   Mechanism: Allocation service validates amount against debt balance

*   **EC-005:** `System Failure During Allocation`
    *   Scenario: Server crashes after creating allocation but before updating debt balance
    *   Expected behavior: Transaction rollback ensures consistency, allocation can be retried
    *   Mechanism: Database transactions + saga pattern for cross-aggregate operations

*   **EC-006:** `Invalid Structured Reference Payment`
    *   Scenario: Bank transfer received with malformed or non-existent structured reference
    *   Expected behavior: Payment created with UNALLOCATED status, manual intervention required
    *   Mechanism: Reference validation with fallback to manual processing queue

*   **EC-007:** `Accounting Service Unavailable`
    *   Scenario: Accounting ledger service is down during prepayment confirmation
    *   Expected behavior: Operation fails, prepayment remains in PENDING status for retry
    *   Mechanism: Circuit breaker pattern + async retry with dead letter queue

*   **EC-008:** `Large Batch Allocation Processing`
    *   Scenario: Citizen has 100+ small debts and makes large prepayment
    *   Expected behavior: Allocations processed in priority order with rate limiting
    *   Mechanism: Async processing with batch size limits (max 10 allocations per batch)

***

### 8. Dependencies

#### 8.1 Technical dependencies

*   Frameworks: `Spring Boot 3.2, Spring Data JPA, Spring Security`
*   DB: `PostgreSQL 15+ with JSONB support`
*   Auth: `OAuth 2.0 / JWT with citizen identity provider`
*   Tools: `Kafka for events, Redis for caching, Docker for deployment`

#### 8.2 Functional dependencies

*   Internal services: 
    *   `Citizen Identity Service` (authentication/authorization)
    *   `Accounting/Ledger Service` (financial audit trail)
    *   `Notification Service` (citizen alerts)
*   External services: 
    *   `Payment Gateway` (Stripe/Mollie for card payments)
    *   `Banking Integration` (SEPA notifications for transfers)
    *   `Tax Assessment Service` (debt creation)

#### 8.3 Roadmap/business dependencies

*   Prerequisites: 
    *   `US-0.1.1: Citizen Authentication`
    *   `US-0.2.1: Debt Management System`
    *   `US-0.3.1: Basic Accounting Framework`
*   Next step: 
    *   `US-1.2.1: Direct Debt Payment via Bank Transfer`
    *   `US-1.3.1: Automatic Allocation Rules Configuration`

***

### 9. Technical acceptance criteria

#### 9.1 Unit tests

*   Minimum coverage: `85%`
*   Classes/services to cover: 
    *   `PrepaymentService, TaxCreditAggregate, AllocationService, AccountingService`
*   Mandatory tests:
    *   `UT_PREPAY_001`: `Valid prepayment creation` → `Prepayment entity created with PENDING status`
    *   `UT_PREPAY_002`: `Duplicate idempotency key` → `Returns existing prepayment without creating new`
    *   `UT_CREDIT_001`: `Credit allocation within balance` → `Available credit reduced correctly`
    *   `UT_CREDIT_002`: `Credit allocation exceeds balance` → `BusinessRuleException thrown`
    *   `UT_ALLOC_001`: `Automatic allocation priority order` → `Debts allocated by priority then due date`
    *   `UT_ACCOUNT_001`: `Double-entry accounting` → `Debit and credit entries balance`

#### 9.2 Integration tests (API)

*   `IT_PREPAY_001`: `POST /api/v1/prepayments` → `201 Created` + `Valid prepayment response`
*   `IT_PREPAY_002`: `POST /api/v1/prepayments with invalid amount` → `400 Bad Request` + `Validation error details`
*   `IT_PREPAY_003`: `POST /api/v1/prepayments/{id}/confirm` → `200 OK` + `TaxCredit created`
*   `IT_CREDIT_001`: `GET /api/v1/citizens/{id}/tax-credit` → `200 OK` + `Current credit balance`
*   `IT_ALLOC_001`: `Prepayment confirmation triggers allocation` → `Debt balance reduced automatically`

#### 9.3 Performance

*   Objectives:
    *   `API response time p95`: `< 500ms` (excluding payment gateway redirect)
    *   `Allocation processing throughput`: `> 100 allocations/second`
    *   `Database query performance`: `< 100ms p95 for single-record lookups`
    *   `Concurrent user capacity`: `> 1000 simultaneous prepayments`
*   Method:
    *   `JMeter load testing with realistic data volumes`
    *   `Database query analysis with EXPLAIN ANALYZE`
    *   `Application profiling with async-profiler`

#### 9.4 Security

*   Controls:
    *   `JWT authentication for all endpoints`
    *   `Authorization: citizens can only access own data`
    *   `Input validation: all fields validated against business rules`
    *   `SQL injection prevention: parameterized queries only`
    *   `Rate limiting: max 10 prepayments per citizen per hour`
    *   `Webhook signature validation for payment confirmations`
*   Tests:
    *   `SEC_AUTH_001`: `Unauthenticated request rejected with 401`
    *   `SEC_AUTHZ_001`: `Cross-citizen data access rejected with 403`
    *   `SEC_INPUT_001`: `SQL injection attempts blocked`
    *   `SEC_RATE_001`: `Rate limit exceeded returns 429`

#### 9.5 Accessibility (if UI)

*   Standard: `WCAG 2.1 AA`
*   Rules:
    *   `Keyboard navigation: all interactive elements accessible via keyboard`
    *   `Color contrast: minimum 4.5:1 ratio for normal text`
    *   `Screen reader: proper ARIA labels and semantic HTML`
    *   `Touch targets: minimum 44x44px for mobile`
    *   `Focus indicators: visible focus outline on all interactive elements`

#### 9.6 UAT / product metrics (if applicable)

*   KPI:
    *   `Prepayment success rate`: `> 95%` — `Measured by successful payment confirmations / total attempts`
    *   `Allocation accuracy`: `> 99%` — `Measured by correct allocations / total allocations`
    *   `Processing time`: `< 2 minutes` — `From payment confirmation to debt allocation`
    *   `User satisfaction`: `> 4.0/5.0` — `Post-transaction survey rating`

***

### 10. Appendices

#### 10.1 Diagrams (optional)

**Sequence Diagram: Prepayment Flow**

```
Citizen -> UI: Submit prepayment form
UI -> API: POST /prepayments
API -> PaymentGW: Create payment session
PaymentGW -> API: Return checkout URL
API -> UI: Return prepayment + URL
UI -> Citizen: Redirect to payment gateway
Citizen -> PaymentGW: Complete payment
PaymentGW -> API: Webhook confirmation
API -> TaxCredit: Create/update credit
API -> Allocation: Trigger auto-allocation
Allocation -> Debt: Update balances
API -> Accounting: Generate ledger entries
API -> Notification: Send confirmation
```

**State Diagram: Prepayment States**

```
[PENDING_PAYMENT] 
    ├─ payment_confirmed → [COMPLETED]
    ├─ payment_failed → [FAILED]
    └─ user_cancelled → [CANCELLED]

[COMPLETED] → (terminal state)
[FAILED] → (terminal state)
[CANCELLED] → (terminal state)
```

#### 10.2 DTO ↔ Entity mapping (optional)

*   DTO: `PrepaymentRequest` → Entity: `Prepayment`
    *   Transformations: `amount rounded to 2 decimals, description trimmed, prepaymentType uppercase`

*   DTO: `TaxCreditResponse` → Entity: `TaxCredit`
    *   Transformations: `availableCredit computed field, amounts formatted with currency`

#### 10.3 Environment variables / configuration (optional)

```properties
# Payment Gateway Configuration
PAYMENT_GATEWAY_URL=https://api.stripe.com/v1
PAYMENT_GATEWAY_API_KEY=${STRIPE_SECRET_KEY}
PAYMENT_GATEWAY_WEBHOOK_SECRET=${STRIPE_WEBHOOK_SECRET}

# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/tax_system
DATABASE_USERNAME=${DB_USER}
DATABASE_PASSWORD=${DB_PASSWORD}
DATABASE_POOL_SIZE=20

# Redis Configuration
REDIS_URL=redis://localhost:6379
REDIS_TIMEOUT=5000

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC_PREPAYMENTS=tax.prepayments
KAFKA_TOPIC_ALLOCATIONS=tax.allocations

# Business Rules
PREPAYMENT_MIN_AMOUNT=1.00
ALLOCATION_BATCH_SIZE=10
RATE_LIMIT_PREPAYMENTS_PER_HOUR=10

# Security
JWT_SECRET=${JWT_SECRET_KEY}
JWT_EXPIRATION=3600
CORS_ALLOWED_ORIGINS=https://tax-portal.gov.be
```

***