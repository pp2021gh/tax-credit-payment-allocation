# Tax Collection and Debt Recovery System

**Context Reference:** ctx_99a057884357

A comprehensive web application for managing tax prepayments, credits, and debt settlement using hexagonal architecture.

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **H2 Database** (in-memory)
- **Maven** for build management
- **Lombok** for reducing boilerplate
- **MapStruct** for object mapping
- **OpenAPI/Swagger** for API documentation
- **Vanilla HTML/CSS/JavaScript** for frontend

## Architecture

The application follows **Hexagonal Architecture** (Ports and Adapters):

```
├── domain/              # Core business logic
│   ├── model/          # Aggregates, Value Objects, Enums
│   ├── repository/     # Repository interfaces (ports)
│   └── service/        # Domain services
├── application/        # Use cases and application services
│   └── service/        # Application services
└── infrastructure/     # Adapters
    ├── persistence/    # JPA entities and repositories
    └── web/           # REST controllers and configuration
```

## Features

### Implemented Features

1. **Citizen Management**
   - Create and retrieve citizens
   - Unique citizen codes and national IDs

2. **Prepayment Management**
   - Create VAT and advance tax prepayments
   - Idempotency support to prevent duplicate transactions
   - Payment confirmation workflow
   - Automatic tax credit creation upon confirmation

3. **Tax Credit Management**
   - Track total, allocated, and available credit
   - Optimistic locking for concurrency control
   - Real-time balance updates

4. **Debt Management**
   - Create and track tax debts
   - Multiple debt types (TAX, PENALTY, INTEREST, OTHER)
   - Structured references for payment identification
   - Priority-based debt ordering

5. **Web Interface**
   - Citizen dashboard with credit balance
   - Prepayment creation form
   - Debt overview
   - Prepayment history with confirmation

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Running the Application

1. **Clone the repository** (if applicable)

2. **Build the project:**
   ```bash
   mvn clean install
   ```

3. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application:**
   - **Web Interface:** http://localhost:8080
   - **API Documentation (Swagger):** http://localhost:8080/swagger-ui.html
   - **H2 Console:** http://localhost:8080/h2-console
     - JDBC URL: `jdbc:h2:mem:taxdebtdb`
     - Username: `sa`
     - Password: (leave empty)

## Sample Data

The application comes with pre-loaded sample data:

### Citizens
- **John Doe** (CIT-001) - ID: `550e8400-e29b-41d4-a716-446655440000`
- **Jane Smith** (CIT-002) - ID: `550e8400-e29b-41d4-a716-446655440001`

### Debts
- John Doe has 2 debts (€1,500 tax + €250 penalty)
- Jane Smith has 1 debt (€2,000 tax)

## Using the Application

### Web Interface Workflow

1. **Select a Citizen** from the dropdown at the top
2. **View Dashboard** to see:
   - Tax credit balance (total, allocated, available)
   - Outstanding debts summary
3. **Make a Prepayment:**
   - Click "Make Prepayment"
   - Select prepayment type (VAT or Advance Payment)
   - Enter amount (minimum €1.00)
   - Submit the form
4. **Confirm Prepayment:**
   - Go to Prepayments section
   - Click "Confirm" button (simulates payment gateway confirmation)
   - Tax credit will be automatically updated
5. **View Debts:**
   - Click "Debts" tab to see all debts
   - View structured references for bank transfers

### API Endpoints

#### Citizens
- `POST /api/v1/citizens` - Create a citizen
- `GET /api/v1/citizens/{id}` - Get citizen by ID
- `GET /api/v1/citizens/code/{code}` - Get citizen by code

#### Prepayments
- `POST /api/v1/prepayments` - Create a prepayment
- `POST /api/v1/prepayments/{id}/confirm` - Confirm prepayment
- `GET /api/v1/citizens/{citizenId}/prepayments` - List prepayments

#### Tax Credits
- `GET /api/v1/citizens/{citizenId}/tax-credit` - Get tax credit balance

#### Debts
- `POST /api/v1/debts` - Create a debt
- `GET /api/v1/citizens/{citizenId}/debts` - List all debts
- `GET /api/v1/citizens/{citizenId}/debts/open` - List open debts

## Domain Model

### Key Entities

1. **Citizen** - Taxpayer with unique code and national ID
2. **Prepayment** - VAT or advance payment (states: PENDING → COMPLETED/FAILED/CANCELLED)
3. **TaxCredit** - Aggregate managing credit balance with optimistic locking
4. **Debt** - Tax obligation with structured reference for payment
5. **Money** - Value object ensuring currency consistency

### Business Rules

- Prepayments must be at least €1.00
- Tax credits use optimistic locking (version field) to prevent concurrent modifications
- Debts have unique structured references for bank transfer identification
- One tax credit account per citizen
- Idempotency keys prevent duplicate prepayments

## Testing

### Manual Testing Steps

1. **Create a Prepayment:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/prepayments \
     -H "Content-Type: application/json" \
     -H "Idempotency-Key: test-key-123" \
     -d '{
       "citizenId": "550e8400-e29b-41d4-a716-446655440000",
       "prepaymentType": "VAT",
       "amount": 500.00,
       "currency": "EUR",
       "description": "Q1 VAT prepayment"
     }'
   ```

2. **Confirm Prepayment:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/prepayments/{prepaymentId}/confirm \
     -H "Content-Type: application/json" \
     -d '{
       "gatewayReference": "GW-123456"
     }'
   ```

3. **Check Tax Credit:**
   ```bash
   curl http://localhost:8080/api/v1/citizens/550e8400-e29b-41d4-a716-446655440000/tax-credit
   ```

## Project Structure

```
fodfin-agpr/
├── pom.xml
├── README.md
├── plans/                                    # Implementation plans
│   ├── 01-project-setup-and-architecture.md
│   ├── 02-domain-implementation-guide.md
│   └── 03-api-and-frontend-implementation.md
├── docs/                                     # Domain documentation
└── src/
    ├── main/
    │   ├── java/com/taxauthority/debtrecovery/
    │   │   ├── TaxDebtRecoveryApplication.java
    │   │   ├── domain/
    │   │   │   └── model/
    │   │   │       ├── enums/
    │   │   │       └── valueobject/
    │   │   ├── application/
    │   │   │   └── service/
    │   │   └── infrastructure/
    │   │       ├── persistence/
    │   │       │   ├── entity/
    │   │       │   └── repository/
    │   │       └── web/
    │   │           ├── controller/
    │   │           └── config/
    │   └── resources/
    │       ├── application.yml
    │       ├── data.sql
    │       └── static/
    │           ├── index.html
    │           ├── css/main.css
    │           └── js/app.js
    └── test/
```

## Future Enhancements

Based on the implementation plans, the following features can be added:

1. **Automatic Allocation** - Allocate tax credits to debts automatically based on priority
2. **Payment Processing** - Handle direct bank transfers with structured references
3. **Allocation Management** - Track credit-to-debt and payment-to-debt allocations
4. **Event Sourcing** - Complete audit trail for tax credit changes
5. **Accounting Entries** - Double-entry bookkeeping for all financial operations
6. **Advanced Security** - JWT authentication, role-based access control
7. **Notifications** - Email/SMS alerts for payment confirmations

## Support

For questions or issues related to this implementation:
- Review the implementation plans in the `plans/` directory
- Check the domain documentation in the `docs/` directory
- Consult the API documentation at `/swagger-ui.html`

## License

Internal use - Tax Authority

---

**Implementation Context:** ctx_99a057884357  
**Version:** 1.0.0  
**Last Updated:** 2026-05-22