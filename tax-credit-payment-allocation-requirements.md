# Tax Credit Payment Allocation Requirements

Source schema: [`tax-credit-payment-allocation-ica-schema.jsonld`](tax-credit-payment-allocation-ica-schema.jsonld)

## Scope

This document derives detailed requirement statements from the entities, actions, operations, constraints, and states defined in [`tax-credit-payment-allocation-ica-schema.jsonld`](tax-credit-payment-allocation-ica-schema.jsonld).

---

## Functional Requirements

### FR-001: Create and maintain a citizen tax credit account
- The system shall maintain a `TaxCredit` aggregate for each citizen, identified by unique `citizenId`.
- The system shall store `currentBalance`, `totalPrepayments`, `totalAllocations`, `currency`, audit timestamps, and a `version` field for concurrency control.
- The system shall initialize the tax credit in state `TaxCreditActive`.
- The system shall support state transitions for active, depleted, and suspended tax credit situations.
- The system shall enforce the invariant that balance equals total prepayments minus total allocations, as defined by `RG-002`.

### FR-002: Accept citizen prepayments
- The system shall create a `Prepayment` for VAT prepayments and advance payments.
- The system shall require a positive amount greater than zero and not exceeding 999,999.99, as defined by `RG-001`.
- The system shall require a unique payment reference and a unique idempotency key.
- The system shall create prepayments initially in state `PrepaymentPending`, then resolve them to completed or failed terminal states.
- The system shall link each prepayment to the relevant tax credit and citizen.

### FR-003: Process prepayment creation flow
- The system shall execute the `CreatePrepayment` action when a citizen submits a valid prepayment.
- The system shall verify the following preconditions before processing:
  - citizen is authenticated and authorized;
  - payment has been validated by the payment gateway;
  - amount is within allowed limits;
  - idempotency key is unique.
- After successful processing, the system shall:
  - create the prepayment entity;
  - update the tax credit balance;
  - generate accounting entries;
  - publish `PrepaymentCreated`, `TaxCreditUpdated`, and `AccountingEntriesGenerated` events;
  - trigger automatic allocation rules when outstanding debts exist.

### FR-004: Query tax credit balances
- The system shall support the `QueryTaxCreditBalance` action for authenticated citizens.
- The system shall return the current balance, total prepayments, and total allocations for the requesting citizen.
- The system shall enforce that a citizen may access only their own data, according to `RG-010`.

### FR-005: Receive and record bank transfer payments
- The system shall create a `Payment` when a bank transfer notification is received.
- The system shall store bank reference, structured reference, debt reference, amount, currency, payment date, debtor account, debtor name, and lifecycle status.
- The system shall create payments initially in state `PaymentReceived` and support processing, allocated, and failed states.
- The system shall enforce uniqueness of bank reference and ensure a payment is applied only once, according to `RG-009`.

### FR-006: Validate and parse structured references
- The system shall validate the structured reference format `+++XXX/XXXX/XXXXX+++` for bank transfer payments, according to `RG-004`.
- The system shall execute the `ParseStructuredReference` operation to extract the target debt identifier from the incoming payment reference.
- The system shall reject payments with invalid structured reference formats.
- The system shall reject or return not found when the structured reference does not map to an existing debt, as described by `EC-003`.

### FR-007: Process bank transfer payment allocation
- The system shall execute the `ProcessBankTransfer` action for valid bank transfer notifications.
- The system shall validate that:
  - the bank notification is in standard format;
  - the structured reference is valid;
  - the target debt exists and is payable;
  - the bank reference is unique.
- After successful processing, the system shall:
  - create a payment entity;
  - create an allocation linking the payment to the debt;
  - update the debt balance;
  - generate accounting entries;
  - publish `PaymentReceived`, `AllocationCreated`, and `AccountingEntriesGenerated` events.

### FR-008: Manage debt lifecycle and balances
- The system shall maintain a `Debt` aggregate with debt type, original amount, remaining balance, priority, currency, status, and structured reference.
- The system shall support debt lifecycle states `DebtPending`, `DebtActive`, `DebtPartiallyPaid`, `DebtSettled`, and `DebtCancelled`.
- The system shall initialize debts in state `DebtPending` and support transitions through active, partially paid, and terminal states (settled or cancelled).
- The system shall calculate debt balance as original amount minus the sum of allocations.
- The system shall prevent overpayment and disallow new allocations to settled or cancelled debts.
- The system shall use debt priority as an input to allocation ordering, in line with `RG-005`.
- The system shall store a structured reference for each debt to enable bank transfer payment identification.

### FR-009: Create and track allocations
- The system shall create an `Allocation` to represent the assignment of a payment or tax credit to a debt.
- The system shall support `sourceType` values of `TAX_CREDIT` and `PAYMENT`.
- The system shall store source identifier, debt identifier, amount, currency, allocation timestamps, and status.
- The system shall initialize allocations in state `AllocationPending` and support applied and reversed terminal states.
- The system shall ensure each allocation references a valid source and target and becomes immutable once applied.

### FR-010: Automatically allocate tax credit to debt
- The system shall execute the `AllocateTaxCreditToDebt` action when tax credit is available and outstanding debts exist.
- The system shall validate that:
  - tax credit balance is greater than zero;
  - outstanding debts exist for the citizen;
  - allocation rules are configured and validated;
  - target debts are not settled.
- The system shall, after allocation:
  - create allocation records;
  - reduce tax credit balance;
  - update debt balances;
  - generate accounting entries;
  - publish `AllocationCreated`, `AllocationApplied`, `TaxCreditUpdated`, and when relevant `DebtSettled`.

### FR-011: Apply allocations to debts
- The system shall execute the `ApplyAllocation` action for pending allocations.
- The system shall only apply an allocation when the allocation is pending, the debt exists, the debt is not settled, and the allocation amount does not exceed the debt balance.
- On successful application, the system shall:
  - change allocation status to `APPLIED`;
  - reduce the debt balance by the allocation amount;
  - change debt status to `SETTLED` when remaining balance becomes zero;
  - publish `AllocationApplied` and, if applicable, `DebtSettled`.

### FR-012: Apply allocation priority rules
- The system shall execute the `ApplyAllocationRules` operation to determine the order in which debts are allocated.
- The system shall sort debts using the priority logic defined by `RG-005`:
  - oldest debts first;
  - then by debt type priority (`PENALTY` before `TAX_DEBT` before `ADMINISTRATIVE_FEE`);
  - then by creation date as tie-breaker.
- The system shall support partial allocations when available credit is insufficient, including the behavior described by `EC-006` and `EC-007`.

### FR-013: Generate balanced accounting entries
- The system shall create `AccountingEntry` records for prepayments, bank transfers, and tax credit allocations.
- The system shall execute the `GenerateAccountingEntries` operation for valid financial transactions.
- The system shall group related accounting lines by `transactionId`.
- The system shall maintain double-entry bookkeeping where total debit equals total credit, according to `RG-006`.
- The system shall publish `AccountingEntriesGenerated` after successful entry creation.

### FR-014: Enforce data and transaction integrity rules
- The system shall enforce currency consistency across related entities in a transaction, according to `RG-007`.
- The system shall prevent allocation amounts from exceeding available source balance, according to `RG-008`.
- The system shall prevent duplicate processing through idempotency and uniqueness constraints.
- The system shall ensure all operations are auditable across tax credit, payment, allocation, and accounting domains.

### FR-015: Handle identified business exceptions
- The system shall return an error and create no allocation when payment is received for a settled debt, as defined by `EC-002`.
- The system shall return not found when a structured reference points to a non-existent debt, as defined by `EC-003`.
- The system shall roll back the full transaction when accounting entry generation fails, as defined by `EC-005`.
- The system shall support partial allocations when the available credit is lower than the debt balance, as defined by `EC-006`.

---

## NonFunctional Requirements

### NFR-001: Auditability and traceability
- Every financial operation shall be auditable, as explicitly required by the `TaxCredit` invariants.
- The system shall generate accounting entries for financial transactions and link them using `transactionId`.
- The system shall emit business events for major lifecycle changes to provide end-to-end traceability.

### NFR-002: Data consistency and integrity
- The system shall preserve invariant correctness for balance, allocation, debt settlement, and accounting equality rules.
- The system shall guarantee that related financial records remain consistent within each business transaction.
- The system shall enforce uniqueness for business identifiers such as payment reference, bank reference, allocation ID, and idempotency key.

### NFR-003: Concurrency control
- The system shall handle concurrent prepayment updates using optimistic locking with the `version` field, as defined by `EC-001`.
- The system shall protect allocation and debt update consistency using transaction isolation, as defined by `EC-004`.
- The system shall prevent lost updates and conflicting balance changes under concurrent load.

### NFR-004: Transactional reliability
- The system shall process financial actions atomically so that partial updates are not persisted on failure.
- The system shall roll back the entire transaction if accounting generation fails, per `EC-005`.
- The system shall ensure that entity state changes, balance updates, allocation creation, and accounting entries remain synchronized.

### NFR-005: Idempotency and duplicate protection
- The system shall support idempotent prepayment creation using a unique idempotency key, according to `RG-003`.
- The system shall ensure duplicate requests return the same logical result rather than creating duplicate business records.
- The system shall prevent duplicate payment ingestion by enforcing unique bank references and unique payment references.

### NFR-006: Security and access control
- The system shall require authentication for citizen-initiated and system-initiated operations identified in the schema preconditions.
- The system shall enforce authorization so that citizens can only access their own tax credits, payments, and prepayments, according to `RG-010`.
- The system shall prevent unauthorized cross-citizen data exposure.

### NFR-007: Validation quality
- The system shall validate key business inputs before processing, including amount ranges, structured reference patterns, payable debt state, and uniqueness constraints.
- The system shall reject invalid requests before they affect balances, allocations, or accounting records.
- Validation logic shall be aligned with the constraint rules `RG-001` through `RG-010` and relevant edge constraints.

### NFR-008: State model robustness
- The system shall preserve valid lifecycle transitions across tax credit, prepayment, payment, allocation, and debt states.
- The system shall support explicit initial and terminal states where defined in the schema.
- The system shall prevent invalid transitions that would break business invariants or cause double application of funds.

### NFR-009: Financial correctness
- The system shall maintain accurate monetary calculations for balances, allocations, and accounting entries.
- The system shall prevent negative balances, over-allocation, overpayment, and unbalanced accounting transactions.
- The system shall preserve currency consistency across related financial entities in the same transaction, per `RG-007`.

### NFR-010: Interoperability and event-driven integration
- The system shall support integration with external payment gateway, banking system, and accounting capabilities as implied by action preconditions and postconditions.
- The system shall publish domain events for downstream consumers after successful business processing.
- Event emission shall reflect the actual committed business outcome.

### NFR-011: Maintainability and explicit business-rule alignment
- The implementation shall remain traceable to named schema constraints such as `RG-001`, `RG-005`, and `EC-005`.
- Business behavior shall be expressible in terms of entities, actions, operations, constraints, and states defined in the schema.
- Requirement documentation shall preserve clear mapping between behavior and governing rules for future evolution and compliance review.