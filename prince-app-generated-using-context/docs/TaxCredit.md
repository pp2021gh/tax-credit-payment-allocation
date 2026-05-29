# TaxCredit Entity

## Overview

The **TaxCredit** entity is an aggregate root that manages a citizen's tax credit balance. It tracks the total credit accumulated from prepayments, the amount already allocated to debts, and the available credit for future allocations. Each citizen has exactly one TaxCredit account.

## Identity

- **Identity Key**: `taxCreditId` (UUID)
- **Human Reference**: `citizenId` (one-to-one relationship with Citizen)

## Attributes

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `taxCreditId` | string (UUID) | Yes | Unique system identifier |
| `citizenId` | string (UUID) | Yes | Reference to citizen (unique - one credit per citizen) |
| `totalCredit` | decimal | Yes | Total credit accumulated (must be >= 0) |
| `allocatedCredit` | decimal | Yes | Credit already allocated to debts (must be >= 0) |
| `availableCredit` | decimal | Yes | Computed: totalCredit - allocatedCredit |
| `currency` | string | Yes | Currency code (e.g., EUR) |
| `version` | integer | Yes | Optimistic locking version (incremented on every update) |
| `createdAt` | timestamp | Yes | When tax credit account was created |
| `updatedAt` | timestamp | Yes | Last update timestamp |

## Invariants (Business Rules)

1. **One Credit Per Citizen**: Each citizen has exactly one TaxCredit account (`citizenId` must be unique)
2. **Non-Negative Total**: `totalCredit` must be >= 0
3. **Non-Negative Allocated**: `allocatedCredit` must be >= 0
4. **Allocation Constraint**: `allocatedCredit` must be <= `totalCredit`
5. **Available Credit Calculation**: `availableCredit` = `totalCredit` - `allocatedCredit` (computed field)
6. **Optimistic Locking**: `version` incremented on every update to prevent concurrent modification
7. **No Negative Balance**: Operations that would result in negative `availableCredit` are rejected

## State Machine

The TaxCredit entity does not have an explicit state machine. It exists in an active state once created and is updated through operations.

## Relationships

- **Citizen** (1:1): Each tax credit belongs to exactly one citizen
- **Prepayment** (1:N): Tax credits are increased by completed prepayments
- **Allocation** (1:N): Tax credits are allocated to debts through allocations
- **TaxCreditEvent** (1:N): All changes are recorded as events for audit trail

## Events

The TaxCredit entity emits the following domain events:

1. **CreditAdded**: Emitted when credit is added from a prepayment
2. **CreditAllocated**: Emitted when credit is allocated to a debt
3. **CreditReleased**: Emitted when an allocation is reversed

## Usage Examples

### Tax Credit Account

```json
{
  "taxCreditId": "550e8400-e29b-41d4-a716-446655440003",
  "citizenId": "550e8400-e29b-41d4-a716-446655440000",
  "totalCredit": 1500.00,
  "allocatedCredit": 500.00,
  "availableCredit": 1000.00,
  "currency": "EUR",
  "version": 5,
  "createdAt": "2026-01-15T10:00:00Z",
  "updatedAt": "2026-05-22T10:35:00Z"
}
```

### Adding Credit (from Prepayment)

```javascript
// Before
{
  "totalCredit": 1000.00,
  "allocatedCredit": 500.00,
  "availableCredit": 500.00,
  "version": 4
}

// Add 500.00 from prepayment
addCredit(500.00)

// After
{
  "totalCredit": 1500.00,
  "allocatedCredit": 500.00,
  "availableCredit": 1000.00,
  "version": 5
}
```

### Allocating Credit (to Debt)

```javascript
// Before
{
  "totalCredit": 1500.00,
  "allocatedCredit": 500.00,
  "availableCredit": 1000.00,
  "version": 5
}

// Allocate 300.00 to debt
allocateCredit(300.00)

// After
{
  "totalCredit": 1500.00,
  "allocatedCredit": 800.00,
  "availableCredit": 700.00,
  "version": 6
}
```

## Implementation Notes

### Database Schema

```sql
CREATE TABLE tax_credits (
    tax_credit_id UUID PRIMARY KEY,
    citizen_id UUID UNIQUE NOT NULL REFERENCES citizens(citizen_id),
    total_credit DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (total_credit >= 0),
    allocated_credit DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (allocated_credit >= 0),
    available_credit DECIMAL(12,2) GENERATED ALWAYS AS (total_credit - allocated_credit) STORED,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT allocated_within_total CHECK (allocated_credit <= total_credit),
    CONSTRAINT available_non_negative CHECK (total_credit - allocated_credit >= 0)
);

CREATE INDEX idx_tax_credits_citizen_id ON tax_credits(citizen_id);
CREATE INDEX idx_tax_credits_available_credit ON tax_credits(available_credit) WHERE available_credit > 0;
```

### Optimistic Locking Implementation

```javascript
class TaxCreditAggregate {
  async addCredit(amount, sourceId, sourceType) {
    // 1. Load current state
    const current = await this.load();
    
    // 2. Validate operation
    if (amount <= 0) {
      throw new BusinessRuleException('Amount must be positive');
    }
    
    // 3. Calculate new state
    const newTotalCredit = current.totalCredit + amount;
    const newVersion = current.version + 1;
    
    // 4. Persist with optimistic locking
    const updated = await this.repository.update({
      taxCreditId: this.taxCreditId,
      totalCredit: newTotalCredit,
      version: newVersion,
      updatedAt: new Date()
    }, {
      where: {
        taxCreditId: this.taxCreditId,
        version: current.version // Optimistic lock
      }
    });
    
    // 5. Check if update succeeded
    if (updated === 0) {
      throw new ConcurrencyException('Tax credit was modified by another transaction');
    }
    
    // 6. Record event
    await this.eventStore.append({
      taxCreditId: this.taxCreditId,
      eventType: 'CREDIT_ADDED',
      amount: amount,
      sourceType: sourceType,
      sourceId: sourceId,
      createdAt: new Date()
    });
    
    // 7. Emit domain event
    await this.eventBus.publish(new CreditAdded(this.taxCreditId, amount));
    
    return this.load(); // Return updated state
  }
  
  async allocateCredit(amount, allocationId) {
    // 1. Load current state
    const current = await this.load();
    
    // 2. Validate operation
    if (amount <= 0) {
      throw new BusinessRuleException('Amount must be positive');
    }
    
    if (amount > current.availableCredit) {
      throw new BusinessRuleException(
        `Insufficient credit: requested ${amount}, available ${current.availableCredit}`
      );
    }
    
    // 3. Calculate new state
    const newAllocatedCredit = current.allocatedCredit + amount;
    const newVersion = current.version + 1;
    
    // 4. Persist with optimistic locking
    const updated = await this.repository.update({
      taxCreditId: this.taxCreditId,
      allocatedCredit: newAllocatedCredit,
      version: newVersion,
      updatedAt: new Date()
    }, {
      where: {
        taxCreditId: this.taxCreditId,
        version: current.version // Optimistic lock
      }
    });
    
    // 5. Check if update succeeded
    if (updated === 0) {
      throw new ConcurrencyException('Tax credit was modified by another transaction');
    }
    
    // 6. Record event
    await this.eventStore.append({
      taxCreditId: this.taxCreditId,
      eventType: 'CREDIT_ALLOCATED',
      amount: amount,
      sourceType: 'ALLOCATION',
      sourceId: allocationId,
      createdAt: new Date()
    });
    
    // 7. Emit domain event
    await this.eventBus.publish(new CreditAllocated(this.taxCreditId, amount, allocationId));
    
    return this.load(); // Return updated state
  }
  
  async releaseCredit(amount, allocationId) {
    // Similar to allocateCredit but decreases allocatedCredit
    // Used when an allocation is reversed
    const current = await this.load();
    
    if (amount <= 0) {
      throw new BusinessRuleException('Amount must be positive');
    }
    
    if (amount > current.allocatedCredit) {
      throw new BusinessRuleException(
        `Cannot release more than allocated: requested ${amount}, allocated ${current.allocatedCredit}`
      );
    }
    
    const newAllocatedCredit = current.allocatedCredit - amount;
    const newVersion = current.version + 1;
    
    const updated = await this.repository.update({
      taxCreditId: this.taxCreditId,
      allocatedCredit: newAllocatedCredit,
      version: newVersion,
      updatedAt: new Date()
    }, {
      where: {
        taxCreditId: this.taxCreditId,
        version: current.version
      }
    });
    
    if (updated === 0) {
      throw new ConcurrencyException('Tax credit was modified by another transaction');
    }
    
    await this.eventStore.append({
      taxCreditId: this.taxCreditId,
      eventType: 'CREDIT_RELEASED',
      amount: amount,
      sourceType: 'ALLOCATION_REVERSAL',
      sourceId: allocationId,
      createdAt: new Date()
    });
    
    await this.eventBus.publish(new CreditReleased(this.taxCreditId, amount, allocationId));
    
    return this.load();
  }
}
```

### Event Sourcing

All changes to TaxCredit are recorded in the `tax_credit_events` table for complete audit trail:

```sql
CREATE TABLE tax_credit_events (
    event_id UUID PRIMARY KEY,
    tax_credit_id UUID NOT NULL REFERENCES tax_credits(tax_credit_id),
    event_type VARCHAR(50) NOT NULL CHECK (event_type IN ('CREDIT_ADDED', 'CREDIT_ALLOCATED', 'CREDIT_RELEASED')),
    amount DECIMAL(12,2) NOT NULL CHECK (amount > 0),
    source_type VARCHAR(50) NOT NULL CHECK (source_type IN ('PREPAYMENT', 'REFUND', 'ADJUSTMENT', 'ALLOCATION', 'ALLOCATION_REVERSAL')),
    source_id UUID NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tax_credit_events_tax_credit_id ON tax_credit_events(tax_credit_id);
CREATE INDEX idx_tax_credit_events_created_at ON tax_credit_events(created_at);
```

### Concurrency Handling

The TaxCredit aggregate uses optimistic locking to handle concurrent modifications:

1. **Version Field**: Each update increments the version
2. **Conditional Update**: Updates only succeed if version matches
3. **Retry Logic**: Failed updates trigger retry with fresh data
4. **Conflict Resolution**: Application decides whether to retry or fail

```javascript
async function withRetry(operation, maxRetries = 3) {
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await operation();
    } catch (error) {
      if (error instanceof ConcurrencyException && attempt < maxRetries) {
        // Wait with exponential backoff
        await sleep(Math.pow(2, attempt) * 100);
        continue;
      }
      throw error;
    }
  }
}

// Usage
await withRetry(() => taxCredit.allocateCredit(300.00, allocationId));
```

## Business Rules

### RG-002: Tax Credit Cannot Be Negative
- **Description**: Available tax credit must always be >= 0. Allocations that would result in negative credit are rejected
- **Valid Example**: `availableCredit = 100, allocation = 50 → new available = 50`
- **Invalid Example**: `availableCredit = 100, allocation = 150 → REJECTED`
- **Verification**: Database CHECK constraint + optimistic locking in TaxCredit aggregate

### RG-009: Concurrent Allocation Prevention
- **Description**: Use optimistic locking to prevent concurrent allocations from the same tax credit
- **Valid Example**: Two concurrent allocations, first succeeds, second fails with version conflict
- **Invalid Example**: Two concurrent allocations both succeed, causing double-spending
- **Verification**: Database version column + application-level retry logic

## Related Documentation

- [Citizen Entity](./Citizen.md) - Citizens who own tax credits
- [Prepayment Entity](./Prepayment.md) - Prepayments that create credits
- [Allocation Entity](./Allocation.md) - Allocations that consume credits
- [TaxCreditEvent Entity](./TaxCreditEvent.md) - Event sourcing log
- [FeedsInto Operation](./Operations.md#feedsinto) - How prepayments feed into credit
- [AllocatesTo Operation](./Operations.md#allocatesto) - How credits allocate to debts

## API Endpoints

### Get Tax Credit Balance
```
GET /api/v1/citizens/{citizenId}/tax-credit
Headers:
  Authorization: Bearer <JWT>
Response: 200 OK
  {
    "taxCreditId": "uuid",
    "citizenId": "uuid",
    "totalCredit": 1500.00,
    "allocatedCredit": 500.00,
    "availableCredit": 1000.00,
    "currency": "EUR",
    "version": 5,
    "updatedAt": "2026-05-22T10:35:00Z"
  }
```

### Get Tax Credit History
```
GET /api/v1/citizens/{citizenId}/tax-credit/history
Headers:
  Authorization: Bearer <JWT>
Query Parameters:
  from: ISO 8601 date
  to: ISO 8601 date
  eventType: CREDIT_ADDED|CREDIT_ALLOCATED|CREDIT_RELEASED
Response: 200 OK
  {
    "events": [
      {
        "eventId": "uuid",
        "eventType": "CREDIT_ADDED",
        "amount": 1500.00,
        "sourceType": "PREPAYMENT",
        "sourceId": "uuid",
        "createdAt": "2026-05-22T10:35:00Z"
      },
      ...
    ]
  }
```

### Manual Credit Adjustment (Admin)
```
POST /api/v1/tax-credits/{taxCreditId}/adjust
Headers:
  Authorization: Bearer <JWT>
  X-Admin-Authorization: <admin-token>
Body:
  {
    "amount": 100.00,
    "reason": "Manual adjustment for overpayment refund",
    "adjustmentType": "INCREASE|DECREASE"
  }
Response: 200 OK
```

## Business Context

The TaxCredit aggregate is central to the tax collection system's prepayment mechanism:

1. **Credit Accumulation**: Citizens build up credit through prepayments
2. **Automatic Allocation**: Credits automatically reduce debts based on priority rules
3. **Balance Tracking**: System maintains accurate credit balance at all times
4. **Audit Trail**: Complete history of all credit changes via event sourcing
5. **Concurrency Safety**: Optimistic locking prevents double-spending
6. **Financial Integrity**: Invariants ensure credit balance never goes negative

The one-to-one relationship between Citizen and TaxCredit ensures each citizen has a single, authoritative credit account, simplifying balance management and allocation logic.