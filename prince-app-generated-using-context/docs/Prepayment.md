# Prepayment Entity

## Overview

The **Prepayment** entity represents a prepayment made by a citizen (either VAT or advance payment) that feeds into the tax credit system. Prepayments are processed through a payment gateway and, upon confirmation, create or increase the citizen's tax credit balance.

## Identity

- **Identity Key**: `prepaymentId` (UUID)
- **Human Reference**: `prepaymentCode` (unique identifier for human communication)

## Attributes

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `prepaymentId` | string (UUID) | Yes | Unique system identifier |
| `prepaymentCode` | string | Yes | Human-readable unique code (e.g., PREP-XXX) |
| `citizenId` | string (UUID) | Yes | Reference to the citizen making the prepayment |
| `prepaymentType` | enum | Yes | Type: `VAT` or `ADVANCE_PAYMENT` |
| `amount` | decimal | Yes | Prepayment amount (must be >= 1.00) |
| `currency` | string | Yes | Currency code (e.g., EUR) |
| `description` | string | No | Optional description of the prepayment |
| `paymentMethod` | string | Yes | Payment method used (e.g., CREDIT_CARD, BANK_TRANSFER) |
| `paymentGatewayReference` | string | No | External payment gateway reference |
| `idempotencyKey` | string (UUID) | Yes | Unique key to prevent duplicate submissions |
| `status` | enum | Yes | Current status (see State Machine below) |
| `createdAt` | timestamp | Yes | When prepayment was created |
| `confirmedAt` | timestamp | No | When payment was confirmed (null if pending) |

## Invariants (Business Rules)

1. **Unique Prepayment Code**: `prepaymentCode` must be unique across all prepayments
2. **Minimum Amount**: `amount` must be positive and >= 1.00 EUR
3. **Valid Prepayment Type**: `prepaymentType` must be either `VAT` or `ADVANCE_PAYMENT`
4. **Unique Idempotency Key**: `idempotencyKey` must be unique to prevent duplicate submissions
5. **Confirmation Timing**: `confirmedAt` must be null if status is `PENDING_PAYMENT`
6. **Status Consistency**: Status transitions must follow the defined state machine

## State Machine

```
[PENDING_PAYMENT] (initial state)
    ├─ payment_confirmed → [COMPLETED] (terminal)
    ├─ payment_failed → [FAILED] (terminal)
    └─ user_cancelled → [CANCELLED] (terminal)
```

### States

| State | Description | Terminal |
|-------|-------------|----------|
| `PENDING_PAYMENT` | Prepayment created, awaiting payment gateway confirmation | No |
| `COMPLETED` | Payment confirmed, tax credit created/updated | Yes |
| `FAILED` | Payment failed at gateway | Yes |
| `CANCELLED` | Prepayment cancelled by user | Yes |

### State Transitions

- **PENDING_PAYMENT → COMPLETED**: Payment gateway confirms successful payment
- **PENDING_PAYMENT → FAILED**: Payment gateway reports payment failure
- **PENDING_PAYMENT → CANCELLED**: User cancels before payment completion

## Relationships

- **Citizen** (N:1): Each prepayment belongs to one citizen
- **TaxCredit** (N:1): Completed prepayments feed into the citizen's tax credit

## Events

The Prepayment entity emits the following domain events:

1. **PrepaymentConfirmed**: Emitted when payment is confirmed (status → COMPLETED)
2. **TaxCreditCreated**: Emitted when prepayment creates/increases tax credit

## Usage Examples

### Creating a Prepayment

```json
{
  "prepaymentId": "550e8400-e29b-41d4-a716-446655440001",
  "prepaymentCode": "PREP-2026-001234",
  "citizenId": "550e8400-e29b-41d4-a716-446655440000",
  "prepaymentType": "VAT",
  "amount": 1500.00,
  "currency": "EUR",
  "description": "Q1 2026 VAT prepayment",
  "paymentMethod": "CREDIT_CARD",
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440002",
  "status": "PENDING_PAYMENT",
  "createdAt": "2026-05-22T10:30:00Z",
  "confirmedAt": null
}
```

### Confirming a Prepayment (Webhook)

```json
{
  "prepaymentId": "550e8400-e29b-41d4-a716-446655440001",
  "paymentGatewayReference": "PAY-GW-123456789",
  "status": "COMPLETED",
  "confirmedAt": "2026-05-22T10:35:00Z"
}
```

## Implementation Notes

### Database Schema

```sql
CREATE TABLE prepayments (
    prepayment_id UUID PRIMARY KEY,
    prepayment_code VARCHAR(50) UNIQUE NOT NULL,
    citizen_id UUID NOT NULL REFERENCES citizens(citizen_id),
    prepayment_type VARCHAR(20) NOT NULL CHECK (prepayment_type IN ('VAT', 'ADVANCE_PAYMENT')),
    amount DECIMAL(12,2) NOT NULL CHECK (amount >= 1.00),
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    description TEXT,
    payment_method VARCHAR(50) NOT NULL,
    payment_gateway_reference VARCHAR(255),
    idempotency_key UUID UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING_PAYMENT', 'COMPLETED', 'FAILED', 'CANCELLED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    confirmed_at TIMESTAMP,
    CONSTRAINT confirmed_at_consistency CHECK (
        (status = 'PENDING_PAYMENT' AND confirmed_at IS NULL) OR
        (status != 'PENDING_PAYMENT' AND confirmed_at IS NOT NULL)
    )
);

CREATE INDEX idx_prepayments_citizen_id ON prepayments(citizen_id);
CREATE INDEX idx_prepayments_status ON prepayments(status);
CREATE INDEX idx_prepayments_idempotency_key ON prepayments(idempotency_key);
CREATE INDEX idx_prepayments_created_at ON prepayments(created_at);
```

### Idempotency Handling

The `idempotencyKey` ensures that duplicate prepayment requests (e.g., due to network retries) don't create multiple prepayments:

1. Client generates unique UUID for each prepayment request
2. Server checks if prepayment with same idempotency key exists
3. If exists, return existing prepayment (HTTP 200)
4. If not exists, create new prepayment (HTTP 201)

### Payment Gateway Integration

```javascript
// Prepayment creation flow
async function createPrepayment(request) {
  // 1. Validate request
  validatePrepaymentRequest(request);
  
  // 2. Check idempotency
  const existing = await findByIdempotencyKey(request.idempotencyKey);
  if (existing) return existing;
  
  // 3. Create prepayment record
  const prepayment = await savePrepayment({
    ...request,
    status: 'PENDING_PAYMENT'
  });
  
  // 4. Create payment gateway session
  const gatewayUrl = await paymentGateway.createSession({
    amount: prepayment.amount,
    currency: prepayment.currency,
    reference: prepayment.prepaymentId,
    returnUrl: `${baseUrl}/prepayments/${prepayment.prepaymentId}/return`,
    webhookUrl: `${baseUrl}/prepayments/${prepayment.prepaymentId}/confirm`
  });
  
  // 5. Return prepayment with gateway URL
  return {
    ...prepayment,
    paymentGatewayUrl: gatewayUrl
  };
}

// Webhook confirmation handler
async function confirmPrepayment(prepaymentId, webhookData) {
  // 1. Verify webhook signature
  verifyWebhookSignature(webhookData);
  
  // 2. Load prepayment
  const prepayment = await findById(prepaymentId);
  if (!prepayment) throw new Error('Prepayment not found');
  
  // 3. Check if already processed
  if (prepayment.status !== 'PENDING_PAYMENT') {
    return prepayment; // Idempotent
  }
  
  // 4. Update prepayment status
  prepayment.status = webhookData.paymentStatus === 'COMPLETED' ? 'COMPLETED' : 'FAILED';
  prepayment.confirmedAt = new Date();
  prepayment.paymentGatewayReference = webhookData.paymentReference;
  await savePrepayment(prepayment);
  
  // 5. If completed, feed into tax credit
  if (prepayment.status === 'COMPLETED') {
    await taxCreditService.addCredit(prepayment.citizenId, prepayment.amount, prepayment.prepaymentId);
    await accountingService.recordPrepayment(prepayment);
  }
  
  return prepayment;
}
```

### Validation Rules

1. **Amount Validation**: Must be >= 1.00 EUR
2. **Type Validation**: Must be 'VAT' or 'ADVANCE_PAYMENT'
3. **Idempotency Key**: Must be valid UUID
4. **Citizen Validation**: Citizen must exist and be active

### Security Considerations

- **Webhook Signature Verification**: Always verify payment gateway webhook signatures
- **Idempotency Protection**: Prevent duplicate prepayments from network retries
- **Amount Validation**: Prevent negative or zero amounts
- **Rate Limiting**: Limit prepayment creation to prevent abuse (e.g., max 10 per hour per citizen)

## Business Rules

### RG-001: Prepayment Minimum Amount
- **Description**: All prepayments must be at least 1.00 EUR to prevent micro-transactions
- **Valid Example**: `amount = 1.00`
- **Invalid Example**: `amount = 0.50`
- **Verification**: Service layer validation before payment gateway redirect

### RG-007: Idempotency Enforcement
- **Description**: All prepayment requests must include unique idempotency key. Duplicate keys return existing prepayment without creating new one
- **Valid Example**: First request with key X creates prepayment, second request with key X returns same prepayment
- **Invalid Example**: Two requests with same key create two prepayments
- **Verification**: Redis cache check + database UNIQUE constraint on idempotency_key

## Related Documentation

- [Citizen Entity](./Citizen.md) - Citizens who make prepayments
- [TaxCredit Entity](./TaxCredit.md) - Tax credit created from prepayments
- [AccountingEntry Entity](./AccountingEntry.md) - Accounting entries for prepayments
- [FeedsInto Operation](./Operations.md#feedsinto) - How prepayments feed into tax credit

## API Endpoints

### Create Prepayment
```
POST /api/v1/prepayments
Headers:
  Authorization: Bearer <JWT>
  Content-Type: application/json
  X-Idempotency-Key: <UUID>
Body:
  {
    "citizenId": "uuid",
    "prepaymentType": "VAT|ADVANCE_PAYMENT",
    "amount": 1500.00,
    "currency": "EUR",
    "description": "Optional description",
    "paymentMethod": "CREDIT_CARD"
  }
Response: 201 Created
  {
    "prepaymentId": "uuid",
    "prepaymentCode": "PREP-2026-001234",
    "status": "PENDING_PAYMENT",
    "paymentGatewayUrl": "https://payment.gateway/checkout/abc123",
    ...
  }
```

### Confirm Prepayment (Webhook)
```
POST /api/v1/prepayments/{prepaymentId}/confirm
Headers:
  X-Webhook-Signature: <signature>
  Content-Type: application/json
Body:
  {
    "prepaymentId": "uuid",
    "paymentStatus": "COMPLETED",
    "paymentReference": "PAY-GW-123456789",
    "paidAmount": 1500.00,
    "paidAt": "2026-05-22T10:35:00Z"
  }
Response: 200 OK
```

### Get Prepayment
```
GET /api/v1/prepayments/{prepaymentId}
Headers:
  Authorization: Bearer <JWT>
Response: 200 OK
```

### List Citizen Prepayments
```
GET /api/v1/citizens/{citizenId}/prepayments
Headers:
  Authorization: Bearer <JWT>
Query Parameters:
  status: PENDING_PAYMENT|COMPLETED|FAILED|CANCELLED
  from: ISO 8601 date
  to: ISO 8601 date
Response: 200 OK
```

## Business Context

Prepayments are a key mechanism for citizens to proactively manage their tax obligations:

1. **VAT Prepayments**: Businesses make quarterly VAT prepayments based on estimated turnover
2. **Advance Payments**: Individuals make advance payments on expected income tax
3. **Credit Accumulation**: Prepayments create tax credits that automatically reduce future debts
4. **Cash Flow Management**: Citizens can smooth tax payments throughout the year
5. **Interest Avoidance**: Prepayments may reduce or eliminate late payment interest

The prepayment process is designed to be simple and secure, with immediate confirmation and automatic credit allocation.