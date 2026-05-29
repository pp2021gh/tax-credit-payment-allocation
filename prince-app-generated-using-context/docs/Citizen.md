# Citizen Entity

## Overview

The **Citizen** entity represents a taxpayer who can make prepayments, accumulate tax credits, and owe debts to the tax authority. This is a core entity in the tax collection and debt recovery system.

## Identity

- **Identity Key**: `citizenId` (UUID)
- **Human Reference**: `citizenCode` (unique identifier for human communication)

## Attributes

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `citizenId` | string (UUID) | Yes | Unique system identifier |
| `citizenCode` | string | Yes | Human-readable unique code |
| `firstName` | string | Yes | Citizen's first name |
| `lastName` | string | Yes | Citizen's last name |
| `nationalId` | string | Yes | National identification number (unique) |
| `email` | string | Yes | Email address (must be valid format) |
| `phone` | string | No | Phone number (optional) |
| `address` | string | No | Physical address (optional) |

## Invariants (Business Rules)

1. **Unique Citizen Code**: `citizenCode` must be unique across all citizens
2. **Unique National ID**: `nationalId` must be unique across all citizens
3. **Valid Email Format**: `email` must conform to standard email format (RFC 5322)

## Relationships

The Citizen entity relates to the following entities:

- **Prepayment**: A citizen can make multiple prepayments (1:N)
- **TaxCredit**: Each citizen has exactly one tax credit account (1:1)
- **Payment**: A citizen can make multiple payments (1:N)
- **Debt**: A citizen can have multiple debts (1:N)

## State Machine

The Citizen entity does not have an explicit state machine. Citizens are considered active once created.

## Events

The Citizen entity does not emit domain events directly, but operations involving citizens trigger events in related aggregates.

## Usage Examples

### Creating a Citizen

```json
{
  "citizenId": "550e8400-e29b-41d4-a716-446655440000",
  "citizenCode": "CIT-2026-001234",
  "firstName": "Jean",
  "lastName": "Dupont",
  "nationalId": "85.01.15-123.45",
  "email": "jean.dupont@example.com",
  "phone": "+32 2 123 45 67",
  "address": "Rue de la Loi 1, 1000 Brussels"
}
```

### Querying Citizen Information

Citizens are typically queried by:
- `citizenId` (system operations)
- `citizenCode` (user-facing operations)
- `nationalId` (authentication/verification)
- `email` (notifications)

## Implementation Notes

### Database Schema

```sql
CREATE TABLE citizens (
    citizen_id UUID PRIMARY KEY,
    citizen_code VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    national_id VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    address TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);

CREATE INDEX idx_citizens_citizen_code ON citizens(citizen_code);
CREATE INDEX idx_citizens_national_id ON citizens(national_id);
CREATE INDEX idx_citizens_email ON citizens(email);
```

### Validation Rules

1. **Email Validation**: Use regex pattern `^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$`
2. **National ID Format**: Depends on country-specific format (e.g., Belgian format: `YY.MM.DD-XXX.XX`)
3. **Citizen Code Format**: Typically `CIT-YYYY-NNNNNN` (year + sequence number)

### Security Considerations

- **PII Protection**: Citizen data contains personally identifiable information (PII)
- **GDPR Compliance**: Must support data access, rectification, and erasure rights
- **Access Control**: Only authorized users can view/modify citizen data
- **Audit Trail**: All changes to citizen data must be logged

## Related Documentation

- [TaxCredit Entity](./TaxCredit.md) - Tax credit management for citizens
- [Prepayment Entity](./Prepayment.md) - Prepayments made by citizens
- [Payment Entity](./Payment.md) - Direct payments made by citizens
- [Debt Entity](./Debt.md) - Debts owed by citizens

## API Endpoints

### Get Citizen by ID
```
GET /api/v1/citizens/{citizenId}
```

### Get Citizen by Code
```
GET /api/v1/citizens/by-code/{citizenCode}
```

### Update Citizen Information
```
PUT /api/v1/citizens/{citizenId}
```

### Search Citizens
```
GET /api/v1/citizens?search={query}
```

## Business Context

Citizens are the primary actors in the tax collection system. They:
1. Make prepayments (VAT or advance payments)
2. Accumulate tax credits from prepayments
3. Receive tax assessments that create debts
4. Make payments to settle debts
5. Benefit from automatic allocation of credits to debts

The system maintains a complete financial relationship with each citizen, tracking all credits, debts, payments, and allocations.