# Tax Collection and Debt Recovery System - Implementation Plan
## Part 2: Domain Implementation Guide

**Context Reference:** ctx_99a057884357  
**Focus:** Core domain model, aggregates, and business logic

---

## 1. Domain Layer Structure

```
domain/
├── model/
│   ├── aggregate/
│   │   ├── Citizen.java
│   │   ├── Prepayment.java
│   │   ├── TaxCredit.java
│   │   ├── Payment.java
│   │   ├── Debt.java
│   │   └── Allocation.java
│   ├── valueobject/
│   │   ├── Money.java
│   │   ├── StructuredReference.java
│   │   ├── CitizenCode.java
│   │   ├── DebtCode.java
│   │   └── PrepaymentCode.java
│   └── enums/
│       ├── PrepaymentType.java
│       ├── PrepaymentStatus.java
│       ├── DebtType.java
│       ├── DebtStatus.java
│       ├── AllocationStatus.java
│       └── AllocationTypes.java
├── repository/
│   ├── CitizenRepository.java
│   ├── PrepaymentRepository.java
│   ├── TaxCreditRepository.java
│   ├── PaymentRepository.java
│   ├── DebtRepository.java
│   └── AllocationRepository.java
├── service/
│   ├── AllocationService.java
│   ├── AccountingService.java
│   └── StructuredReferenceService.java
├── event/
│   ├── DomainEvent.java
│   ├── PrepaymentConfirmed.java
│   ├── TaxCreditCreated.java
│   ├── CreditAllocated.java
│   ├── PaymentReceived.java
│   ├── DebtSettled.java
│   └── AllocationApplied.java
└── exception/
    ├── DomainException.java
    ├── InsufficientCreditException.java
    ├── InvalidStateTransitionException.java
    └── ConcurrentModificationException.java
```

---

## 2. Value Objects Implementation

### 2.1 Money Value Object

```java
package com.taxauthority.debtrecovery.domain.model.valueobject;

import lombok.Value;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Value
public class Money {
    BigDecimal amount;
    String currency;
    
    public Money(BigDecimal amount, String currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency.toUpperCase();
    }
    
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    public Money subtract(Money other) {
        validateSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Result cannot be negative");
        }
        return new Money(result, this.currency);
    }
    
    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }
    
    public boolean isGreaterThanOrEqual(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }
    
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot operate on different currencies");
        }
    }
    
    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }
}
```

### 2.2 StructuredReference Value Object

```java
package com.taxauthority.debtrecovery.domain.model.valueobject;

import lombok.Value;
import java.util.regex.Pattern;

@Value
public class StructuredReference {
    private static final Pattern PATTERN = Pattern.compile("^[A-Z0-9]{12}$");
    String value;
    
    public StructuredReference(String value) {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "Structured reference must be 12 alphanumeric characters"
            );
        }
        this.value = value.toUpperCase();
    }
    
    public static StructuredReference generate(String debtCode) {
        // Generate structured reference from debt code
        // Format: YYYY-NNNNNN -> YYYYNNNNNNCC (with check digits)
        String base = debtCode.replace("-", "");
        String checkDigits = calculateCheckDigits(base);
        return new StructuredReference(base + checkDigits);
    }
    
    private static String calculateCheckDigits(String base) {
        // Implement modulo 97 check digit calculation
        long number = Long.parseLong(base);
        int checksum = (int) (number % 97);
        return String.format("%02d", checksum);
    }
}
```

---

## 3. Aggregate Implementations

### 3.1 Citizen Aggregate

```java
package com.taxauthority.debtrecovery.domain.model.aggregate;

import com.taxauthority.debtrecovery.domain.model.valueobject.CitizenCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

@Getter
@NoArgsConstructor
public class Citizen {
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    
    private UUID citizenId;
    private CitizenCode citizenCode;
    private String firstName;
    private String lastName;
    private String nationalId;
    private String email;
    private String phone;
    private String address;
    private Instant createdAt;
    private Instant updatedAt;
    
    public Citizen(
        CitizenCode citizenCode,
        String firstName,
        String lastName,
        String nationalId,
        String email
    ) {
        this.citizenId = UUID.randomUUID();
        this.citizenCode = citizenCode;
        this.firstName = validateNotBlank(firstName, "First name");
        this.lastName = validateNotBlank(lastName, "Last name");
        this.nationalId = validateNotBlank(nationalId, "National ID");
        this.email = validateEmail(email);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public void updateContactInfo(String email, String phone, String address) {
        if (email != null) {
            this.email = validateEmail(email);
        }
        this.phone = phone;
        this.address = address;
        this.updatedAt = Instant.now();
    }
    
    private String validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
    
    private String validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        return email.toLowerCase();
    }
}
```

### 3.2 Prepayment Aggregate

```java
package com.taxauthority.debtrecovery.domain.model.aggregate;

import com.taxauthority.debtrecovery.domain.model.valueobject.Money;
import com.taxauthority.debtrecovery.domain.model.valueobject.PrepaymentCode;
import com.taxauthority.debtrecovery.domain.model.enums.PrepaymentType;
import com.taxauthority.debtrecovery.domain.model.enums.PrepaymentStatus;
import com.taxauthority.debtrecovery.domain.event.PrepaymentConfirmed;
import com.taxauthority.debtrecovery.domain.exception.InvalidStateTransitionException;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Getter
public class Prepayment {
    private UUID prepaymentId;
    private PrepaymentCode prepaymentCode;
    private UUID citizenId;
    private PrepaymentType prepaymentType;
    private Money amount;
    private String description;
    private String paymentMethod;
    private String paymentGatewayReference;
    private String idempotencyKey;
    private PrepaymentStatus status;
    private Instant createdAt;
    private Instant confirmedAt;
    
    // Private constructor for state control
    private Prepayment() {}
    
    public static Prepayment create(
        UUID citizenId,
        PrepaymentType prepaymentType,
        Money amount,
        String description,
        String idempotencyKey
    ) {
        validateAmount(amount);
        
        Prepayment prepayment = new Prepayment();
        prepayment.prepaymentId = UUID.randomUUID();
        prepayment.prepaymentCode = PrepaymentCode.generate();
        prepayment.citizenId = citizenId;
        prepayment.prepaymentType = prepaymentType;
        prepayment.amount = amount;
        prepayment.description = description;
        prepayment.idempotencyKey = idempotencyKey;
        prepayment.status = PrepaymentStatus.PENDING_PAYMENT;
        prepayment.createdAt = Instant.now();
        
        return prepayment;
    }
    
    public PrepaymentConfirmed confirm(String paymentGatewayReference) {
        if (status != PrepaymentStatus.PENDING_PAYMENT) {
            throw new InvalidStateTransitionException(
                "Can only confirm prepayment in PENDING_PAYMENT state"
            );
        }
        
        this.status = PrepaymentStatus.COMPLETED;
        this.paymentGatewayReference = paymentGatewayReference;
        this.confirmedAt = Instant.now();
        
        return new PrepaymentConfirmed(
            prepaymentId,
            citizenId,
            amount,
            prepaymentType,
            confirmedAt
        );
    }
    
    public void fail(String reason) {
        if (status != PrepaymentStatus.PENDING_PAYMENT) {
            throw new InvalidStateTransitionException(
                "Can only fail prepayment in PENDING_PAYMENT state"
            );
        }
        this.status = PrepaymentStatus.FAILED;
    }
    
    public void cancel() {
        if (status != PrepaymentStatus.PENDING_PAYMENT) {
            throw new InvalidStateTransitionException(
                "Can only cancel prepayment in PENDING_PAYMENT state"
            );
        }
        this.status = PrepaymentStatus.CANCELLED;
    }
    
    private static void validateAmount(Money amount) {
        Money minimumAmount = new Money(
            java.math.BigDecimal.ONE,
            amount.getCurrency()
        );
        if (!amount.isGreaterThanOrEqual(minimumAmount)) {
            throw new IllegalArgumentException(
                "Prepayment amount must be at least 1.00"
            );
        }
    }
}
```

### 3.3 TaxCredit Aggregate (with Event Sourcing)

```java
package com.taxauthority.debtrecovery.domain.model.aggregate;

import com.taxauthority.debtrecovery.domain.model.valueobject.Money;
import com.taxauthority.debtrecovery.domain.event.CreditAdded;
import com.taxauthority.debtrecovery.domain.event.CreditAllocated;
import com.taxauthority.debtrecovery.domain.event.CreditReleased;
import com.taxauthority.debtrecovery.domain.exception.InsufficientCreditException;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Getter
public class TaxCredit {
    private UUID taxCreditId;
    private UUID citizenId;
    private Money totalCredit;
    private Money allocatedCredit;
    private String currency;
    private Integer version; // Optimistic locking
    private Instant createdAt;
    private Instant updatedAt;
    
    private TaxCredit() {}
    
    public static TaxCredit create(UUID citizenId, String currency) {
        TaxCredit taxCredit = new TaxCredit();
        taxCredit.taxCreditId = UUID.randomUUID();
        taxCredit.citizenId = citizenId;
        taxCredit.currency = currency;
        taxCredit.totalCredit = Money.zero(currency);
        taxCredit.allocatedCredit = Money.zero(currency);
        taxCredit.version = 0;
        taxCredit.createdAt = Instant.now();
        taxCredit.updatedAt = Instant.now();
        return taxCredit;
    }
    
    public CreditAdded addCredit(Money amount, UUID sourceId, String sourceType) {
        validateCurrency(amount);
        
        this.totalCredit = this.totalCredit.add(amount);
        this.updatedAt = Instant.now();
        this.version++;
        
        return new CreditAdded(
            taxCreditId,
            citizenId,
            amount,
            sourceType,
            sourceId,
            updatedAt
        );
    }
    
    public CreditAllocated allocateCredit(Money amount, UUID targetDebtId) {
        validateCurrency(amount);
        
        Money availableCredit = getAvailableCredit();
        if (!availableCredit.isGreaterThanOrEqual(amount)) {
            throw new InsufficientCreditException(
                "Insufficient credit. Available: " + availableCredit.getAmount() +
                ", Requested: " + amount.getAmount()
            );
        }
        
        this.allocatedCredit = this.allocatedCredit.add(amount);
        this.updatedAt = Instant.now();
        this.version++;
        
        return new CreditAllocated(
            taxCreditId,
            citizenId,
            amount,
            targetDebtId,
            updatedAt
        );
    }
    
    public CreditReleased releaseCredit(Money amount, UUID allocationId) {
        validateCurrency(amount);
        
        if (!this.allocatedCredit.isGreaterThanOrEqual(amount)) {
            throw new IllegalStateException("Cannot release more than allocated");
        }
        
        this.allocatedCredit = this.allocatedCredit.subtract(amount);
        this.updatedAt = Instant.now();
        this.version++;
        
        return new CreditReleased(
            taxCreditId,
            citizenId,
            amount,
            allocationId,
            updatedAt
        );
    }
    
    public Money getAvailableCredit() {
        return totalCredit.subtract(allocatedCredit);
    }
    
    private void validateCurrency(Money amount) {
        if (!amount.getCurrency().equals(this.currency)) {
            throw new IllegalArgumentException(
                "Currency mismatch. Expected: " + this.currency +
                ", Got: " + amount.getCurrency()
            );
        }
    }
}
```

### 3.4 Debt Aggregate

```java
package com.taxauthority.debtrecovery.domain.model.aggregate;

import com.taxauthority.debtrecovery.domain.model.valueobject.Money;
import com.taxauthority.debtrecovery.domain.model.valueobject.DebtCode;
import com.taxauthority.debtrecovery.domain.model.valueobject.StructuredReference;
import com.taxauthority.debtrecovery.domain.model.enums.DebtType;
import com.taxauthority.debtrecovery.domain.model.enums.DebtStatus;
import com.taxauthority.debtrecovery.domain.event.DebtCreated;
import com.taxauthority.debtrecovery.domain.event.DebtReduced;
import com.taxauthority.debtrecovery.domain.event.DebtSettled;
import lombok.Getter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
public class Debt {
    private UUID debtId;
    private DebtCode debtCode;
    private UUID citizenId;
    private DebtType debtType;
    private Money originalAmount;
    private Money outstandingAmount;
    private LocalDate dueDate;
    private DebtStatus status;
    private Integer priority;
    private StructuredReference structuredReference;
    private Integer version; // Optimistic locking
    private Instant createdAt;
    private Instant updatedAt;
    private Instant settledAt;
    
    private Debt() {}
    
    public static Debt create(
        UUID citizenId,
        DebtType debtType,
        Money amount,
        LocalDate dueDate,
        Integer priority
    ) {
        Debt debt = new Debt();
        debt.debtId = UUID.randomUUID();
        debt.debtCode = DebtCode.generate();
        debt.citizenId = citizenId;
        debt.debtType = debtType;
        debt.originalAmount = amount;
        debt.outstandingAmount = amount;
        debt.dueDate = dueDate;
        debt.status = DebtStatus.OPEN;
        debt.priority = priority != null ? priority : 1;
        debt.structuredReference = StructuredReference.generate(
            debt.debtCode.getValue()
        );
        debt.version = 0;
        debt.createdAt = Instant.now();
        debt.updatedAt = Instant.now();
        
        return debt;
    }
    
    public DebtReduced reduce(Money amount, UUID allocationId) {
        if (!outstandingAmount.isGreaterThanOrEqual(amount)) {
            throw new IllegalArgumentException(
                "Cannot reduce debt by more than outstanding amount"
            );
        }
        
        this.outstandingAmount = this.outstandingAmount.subtract(amount);
        this.updatedAt = Instant.now();
        this.version++;
        
        // Check if debt is fully settled
        if (this.outstandingAmount.isZero()) {
            this.status = DebtStatus.PAID;
            this.settledAt = Instant.now();
            return new DebtSettled(debtId, citizenId, originalAmount, settledAt);
        } else {
            this.status = DebtStatus.PARTIALLY_PAID;
            return new DebtReduced(debtId, citizenId, amount, outstandingAmount, updatedAt);
        }
    }
    
    public void cancel() {
        if (status == DebtStatus.PAID) {
            throw new IllegalStateException("Cannot cancel a paid debt");
        }
        this.status = DebtStatus.CANCELLED;
        this.updatedAt = Instant.now();
        this.version++;
    }
    
    public boolean isOverdue() {
        return LocalDate.now().isAfter(dueDate) && 
               status != DebtStatus.PAID && 
               status != DebtStatus.CANCELLED;
    }
}
```

---

## 4. Domain Services

### 4.1 AllocationService

```java
package com.taxauthority.debtrecovery.domain.service;

import com.taxauthority.debtrecovery.domain.model.aggregate.TaxCredit;
import com.taxauthority.debtrecovery.domain.model.aggregate.Debt;
import com.taxauthority.debtrecovery.domain.model.aggregate.Allocation;
import com.taxauthority.debtrecovery.domain.model.valueobject.Money;
import com.taxauthority.debtrecovery.domain.repository.DebtRepository;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class AllocationService {
    private final DebtRepository debtRepository;
    
    /**
     * Automatically allocate available credit to debts based on priority rules:
     * 1. Priority (1 = highest)
     * 2. Due date (oldest first)
     * 3. Amount (largest first)
     */
    public List<Allocation> allocateAutomatically(
        TaxCredit taxCredit,
        UUID citizenId
    ) {
        Money availableCredit = taxCredit.getAvailableCredit();
        
        if (availableCredit.isZero()) {
            return List.of();
        }
        
        // Get all open debts for citizen, sorted by allocation priority
        List<Debt> debts = debtRepository.findOpenDebtsByCitizenId(citizenId)
            .stream()
            .sorted((d1, d2) -> {
                // Sort by priority (ascending), then due date, then amount
                int priorityCompare = d1.getPriority().compareTo(d2.getPriority());
                if (priorityCompare != 0) return priorityCompare;
                
                int dateCompare = d1.getDueDate().compareTo(d2.getDueDate());
                if (dateCompare != 0) return dateCompare;
                
                return d2.getOutstandingAmount().getAmount()
                    .compareTo(d1.getOutstandingAmount().getAmount());
            })
            .collect(Collectors.toList());
        
        List<Allocation> allocations = new java.util.ArrayList<>();
        Money remainingCredit = availableCredit;
        
        for (Debt debt : debts) {
            if (remainingCredit.isZero()) {
                break;
            }
            
            Money allocationAmount = calculateAllocationAmount(
                remainingCredit,
                debt.getOutstandingAmount()
            );
            
            Allocation allocation = Allocation.createCreditToDebt(
                taxCredit.getTaxCreditId(),
                debt.getDebtId(),
                allocationAmount
            );
            
            allocations.add(allocation);
            remainingCredit = remainingCredit.subtract(allocationAmount);
        }
        
        return allocations;
    }
    
    private Money calculateAllocationAmount(Money available, Money outstanding) {
        // Allocate the minimum of available credit and outstanding debt
        return available.isGreaterThan(outstanding) ? outstanding : available;
    }
}
```

---

## 5. Domain Events

### 5.1 Base Domain Event

```java
package com.taxauthority.debtrecovery.domain.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID getEventId();
    Instant getOccurredAt();
    String getEventType();
}
```

### 5.2 PrepaymentConfirmed Event

```java
package com.taxauthority.debtrecovery.domain.event;

import com.taxauthority.debtrecovery.domain.model.valueobject.Money;
import com.taxauthority.debtrecovery.domain.model.enums.PrepaymentType;
import lombok.Value;
import java.time.Instant;
import java.util.UUID;

@Value
public class PrepaymentConfirmed implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    UUID prepaymentId;
    UUID citizenId;
    Money amount;
    PrepaymentType prepaymentType;
    Instant occurredAt;
    
    @Override
    public String getEventType() {
        return "PrepaymentConfirmed";
    }
}
```

---

## 6. Repository Interfaces (Ports)

### 6.1 TaxCreditRepository

```java
package com.taxauthority.debtrecovery.domain.repository;

import com.taxauthority.debtrecovery.domain.model.aggregate.TaxCredit;
import java.util.Optional;
import java.util.UUID;

public interface TaxCreditRepository {
    TaxCredit save(TaxCredit taxCredit);
    Optional<TaxCredit> findById(UUID taxCreditId);
    Optional<TaxCredit> findByCitizenId(UUID citizenId);
    void delete(TaxCredit taxCredit);
}
```

### 6.2 DebtRepository

```java
package com.taxauthority.debtrecovery.domain.repository;

import com.taxauthority.debtrecovery.domain.model.aggregate.Debt;
import com.taxauthority.debtrecovery.domain.model.valueobject.StructuredReference;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DebtRepository {
    Debt save(Debt debt);
    Optional<Debt> findById(UUID debtId);
    Optional<Debt> findByStructuredReference(StructuredReference reference);
    List<Debt> findOpenDebtsByCitizenId(UUID citizenId);
    List<Debt> findAllByCitizenId(UUID citizenId);
    void delete(Debt debt);
}
```

---

## 7. Testing Strategy

### 7.1 Unit Tests for Aggregates

```java
package com.taxauthority.debtrecovery.domain.model.aggregate;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TaxCreditTest {
    
    @Test
    void shouldCreateTaxCreditWithZeroBalance() {
        UUID citizenId = UUID.randomUUID();
        TaxCredit taxCredit = TaxCredit.create(citizenId, "EUR");
        
        assertEquals(citizenId, taxCredit.getCitizenId());
        assertTrue(taxCredit.getTotalCredit().isZero());
        assertTrue(taxCredit.getAllocatedCredit().isZero());
        assertTrue(taxCredit.getAvailableCredit().isZero());
    }
    
    @Test
    void shouldAddCreditAndEmitEvent() {
        TaxCredit taxCredit = TaxCredit.create(UUID.randomUUID(), "EUR");
        Money amount = new Money(new BigDecimal("100.00"), "EUR");
        
        CreditAdded event = taxCredit.addCredit(
            amount,
            UUID.randomUUID(),
            "PREPAYMENT"
        );
        
        assertNotNull(event);
        assertEquals(amount, taxCredit.getTotalCredit());
        assertEquals(amount, taxCredit.getAvailableCredit());
    }
    
    @Test
    void shouldThrowExceptionWhenAllocatingMoreThanAvailable() {
        TaxCredit taxCredit = TaxCredit.create(UUID.randomUUID(), "EUR");
        Money amount = new Money(new BigDecimal("100.00"), "EUR");
        
        assertThrows(InsufficientCreditException.class, () -> {
            taxCredit.allocateCredit(amount, UUID.randomUUID());
        });
    }
}
```

---

## 8. Implementation Checklist

### Domain Model
- [ ] Implement all value objects (Money, StructuredReference, etc.)
- [ ] Implement all enums (PrepaymentType, DebtStatus, etc.)
- [ ] Implement Citizen aggregate
- [ ] Implement Prepayment aggregate with state machine
- [ ] Implement TaxCredit aggregate with event sourcing
- [ ] Implement Payment aggregate
- [ ] Implement Debt aggregate with optimistic locking
- [ ] Implement Allocation aggregate

### Domain Services
- [ ] Implement AllocationService with automatic allocation rules
- [ ] Implement AccountingService for double-entry bookkeeping
- [ ] Implement StructuredReferenceService

### Domain Events
- [ ] Define all domain event interfaces
- [ ] Implement event classes for each aggregate operation

### Repository Interfaces
- [ ] Define repository interfaces for all aggregates
- [ ] Document expected behavior and constraints

### Testing
- [ ] Write unit tests for all value objects
- [ ] Write unit tests for all aggregates
- [ ] Write unit tests for domain services
- [ ] Achieve >80% code coverage for domain layer

---

## Next Steps

After completing domain implementation:
1. Proceed to **Part 3: API and Frontend** for REST endpoints and UI
2. Implement infrastructure adapters (JPA repositories)
3. Wire everything together with Spring Boot configuration