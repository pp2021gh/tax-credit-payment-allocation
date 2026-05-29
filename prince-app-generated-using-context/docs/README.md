# Tax Collection and Debt Recovery System - Domain Documentation

## Overview

This documentation describes the domain entities, operations, and business rules for the tax collection and debt recovery system. The system enables citizens to make prepayments, accumulate tax credits, settle debts through various payment methods, and provides automated allocation with complete audit trails.

## Domain Model

The system is built around the following core domain entities:

### Core Entities

1. **[Citizen](./Citizen.md)** - Taxpayers who interact with the system
2. **[Prepayment](./Prepayment.md)** - VAT and advance payments made by citizens
3. **[TaxCredit](./TaxCredit.md)** - Aggregate managing citizen credit balances
4. **[TaxCreditEvent](./TaxCreditEvent.md)** - Event sourcing log for credit changes
5. **[Payment](./Payment.md)** - Direct debt payments via bank transfer
6. **[Debt](./Debt.md)** - Tax debts owed by citizens
7. **[Allocation](./Allocation.md)** - Credit/payment-to-debt assignments
8. **[AccountingEntry](./AccountingEntry.md)** - Double-entry accounting ledger

### Domain Operations

- **[FeedsInto](./Operations.md#feedsinto)** - Prepayment → TaxCredit
- **[AllocatesTo](./Operations.md#allocatesto)** - TaxCredit/Payment → Debt
- **[Identifies](./Operations.md#identifies)** - Payment → Debt (via structured reference)
- **[Reverses](./Operations.md#reverses)** - Allocation reversal
- **[Triggers](./Operations.md#triggers)** - Automatic allocation rules
- **[Records](./Operations.md#records)** - Accounting entry generation

## Key Concepts

### Prepayments and Tax Credits

Citizens can make two types of prepayments:
- **VAT Prepayments**: Quarterly prepayments for businesses
- **Advance Payments**: Income tax prepayments for individuals

Completed prepayments feed into a **TaxCredit** aggregate that tracks:
- `totalCredit`: Total accumulated credit
- `allocatedCredit`: Credit already assigned to debts
- `availableCredit`: Credit available for allocation (computed)

### Payments and Structured References

Citizens can settle debts directly via bank transfer using a **structured reference** (12 alphanumeric characters) that identifies the specific debt. The system:
1. Receives payment notification from banking system
2. Matches structured reference to debt
3. Creates allocation to settle the debt

### Allocations

The **Allocation** aggregate materializes the assignment of credit or payment to a debt:
- **CREDIT_TO_DEBT**: Allocates tax credit to debt
- **PAYMENT_TO_DEBT**: Allocates payment to debt

Allocations can be:
- **Automatic**: Triggered by allocation rules when credit becomes available
- **Manual**: Created by tax authority staff
- **Partial**: Debt partially settled
- **Full**: Debt completely settled

### Automatic Allocation Rules

When tax credit becomes available, the system automatically allocates to debts based on:
1. **Priority**: Debt priority field (1 = highest)
2. **Due Date**: Oldest debts first
3. **Amount**: Largest debts first (tiebreaker)

### Accounting and Audit Trail

Every financial operation generates:
- **AccountingEntry**: Double-entry bookkeeping (debits = credits)
- **TaxCreditEvent**: Event sourcing for credit changes
- **Domain Events**: Published for integration and notifications

## State Machines

### Prepayment States
```
PENDING_PAYMENT → COMPLETED (terminal)
                → FAILED (terminal)
                → CANCELLED (terminal)
```

### Payment States
```
RECEIVED → PARTIALLY_ALLOCATED → ALLOCATED (terminal)
        → UNALLOCATED (terminal)
```

### Debt States
```
OPEN → PARTIALLY_PAID → PAID (terminal)
                      → CANCELLED (terminal)
```

### Allocation States
```
PENDING → APPLIED (terminal)
        → REVERSED (terminal)
```

## Business Rules

### Critical Invariants

1. **Tax Credit Balance**: `availableCredit` = `totalCredit` - `allocatedCredit` (must be >= 0)
2. **Debt Balance**: `outstandingAmount` <= `originalAmount` (must be >= 0)
3. **One Credit Per Citizen**: Each citizen has exactly one TaxCredit account
4. **Unique References**: Structured references must be unique per debt
5. **Idempotency**: All operations must be idempotent (safe to retry)
6. **Double-Entry**: All accounting entries must balance (debits = credits)
7. **Immutability**: Accounting entries and events are append-only
8. **No Circular Dependencies**: Allocation dependencies must be acyclic

### Concurrency Control

- **Optimistic Locking**: TaxCredit and Debt use version fields
- **Idempotency Keys**: Prepayments use unique keys to prevent duplicates
- **Event Sourcing**: TaxCredit changes recorded as immutable events
- **Retry Logic**: Failed operations retry with exponential backoff

## Architecture Patterns

### Domain-Driven Design (DDD)

- **Aggregates**: TaxCredit, Debt, Payment, Allocation
- **Entities**: Citizen, Prepayment, AccountingEntry
- **Value Objects**: Money (amount + currency)
- **Domain Events**: CreditAdded, DebtReduced, AllocationApplied
- **Repositories**: Aggregate persistence
- **Services**: Allocation rules, accounting

### Event Sourcing

TaxCredit uses event sourcing for complete audit trail:
- All state changes recorded as events
- Current state derived from event replay
- Events are immutable and append-only
- Enables temporal queries and debugging

### CQRS (Command Query Responsibility Segregation)

- **Commands**: CreatePrepayment, AllocateCredit, SettleDebt
- **Queries**: GetTaxCreditBalance, ListDebts, GetAllocationHistory
- **Read Models**: Optimized for queries (denormalized)
- **Write Models**: Optimized for consistency (normalized)

## Integration Points

### External Systems

1. **Payment Gateway** (Stripe/Mollie)
   - Prepayment processing
   - Webhook confirmations
   - Signature verification

2. **Banking System** (SEPA)
   - Payment notifications
   - Structured reference matching
   - Bank reconciliation

3. **Tax Assessment System**
   - Debt creation
   - Tax calculations
   - Assessment notifications

4. **Notification Service**
   - Email confirmations
   - SMS alerts
   - Push notifications

### Internal Events

Domain events published to event bus (Kafka):
- `PrepaymentConfirmed`
- `TaxCreditCreated`
- `CreditAllocated`
- `PaymentReceived`
- `DebtSettled`
- `AllocationApplied`

## Security Considerations

### Authentication & Authorization

- **JWT Tokens**: Bearer token authentication
- **Role-Based Access**: Citizen, TaxAuthority, Admin
- **Resource Ownership**: Citizens can only access own data
- **Admin Operations**: Require elevated permissions

### Data Protection

- **PII Encryption**: Sensitive citizen data encrypted at rest
- **GDPR Compliance**: Data access, rectification, erasure rights
- **Audit Logging**: All operations logged with user context
- **Rate Limiting**: Prevent abuse (e.g., max 10 prepayments/hour)

### Financial Security

- **Idempotency**: Prevent duplicate transactions
- **Optimistic Locking**: Prevent double-spending
- **Webhook Verification**: Validate payment gateway signatures
- **Amount Validation**: Prevent negative or zero amounts
- **Balance Checks**: Ensure sufficient credit/payment before allocation

## Performance Considerations

### Scalability

- **Read Replicas**: Separate read/write databases
- **Caching**: Redis for frequently accessed data
- **Async Processing**: Background jobs for allocations
- **Batch Operations**: Process multiple allocations efficiently

### Optimization

- **Database Indexes**: Optimized for common queries
- **Computed Columns**: availableCredit calculated by database
- **Event Batching**: Batch event publishing
- **Connection Pooling**: Reuse database connections

## Testing Strategy

### Unit Tests

- Aggregate business logic
- Domain rule validation
- State machine transitions
- Event generation

### Integration Tests

- API endpoints
- Database operations
- Event publishing
- External service mocks

### End-to-End Tests

- Complete user flows
- Payment gateway integration
- Allocation scenarios
- Error handling

## Monitoring & Observability

### Metrics

- Prepayment success rate
- Allocation processing time
- Credit balance distribution
- Debt settlement rate
- API response times

### Alerts

- Failed prepayments
- Allocation errors
- Accounting imbalances
- Concurrency conflicts
- System errors

### Logging

- Structured logging (JSON)
- Correlation IDs for tracing
- User context in logs
- Performance metrics
- Error stack traces

## Getting Started

1. Read the [Citizen](./Citizen.md) entity documentation
2. Understand [Prepayment](./Prepayment.md) and [TaxCredit](./TaxCredit.md) flow
3. Learn about [Payment](./Payment.md) and [Debt](./Debt.md) settlement
4. Study [Allocation](./Allocation.md) logic and rules
5. Review [Operations](./Operations.md) for business processes

## Additional Resources

- [Technical Specification](../tax-debt-recovery-technical-spec.md)
- [Domain Ontology](../tax-debt-domain-ontology.jsonld)
- [API Documentation](./API.md)
- [Deployment Guide](./Deployment.md)

## Contributing

When adding new features or modifying existing entities:

1. Update the domain ontology (tax-debt-domain-ontology.jsonld)
2. Update entity documentation
3. Add/update business rules
4. Update state machines if applicable
5. Add integration tests
6. Update API documentation

## Support

For questions or issues:
- Technical: tech-support@tax-authority.gov
- Business: business-support@tax-authority.gov
- Security: security@tax-authority.gov