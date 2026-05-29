package com.taxauthority.debtrecovery.infrastructure.persistence.entity;

import com.taxauthority.debtrecovery.domain.model.enums.DebtStatus;
import com.taxauthority.debtrecovery.domain.model.enums.DebtType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "debts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebtEntity {
    
    @Id
    private UUID debtId;
    
    @Column(unique = true, nullable = false)
    private String debtCode;
    
    @Column(nullable = false)
    private UUID citizenId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebtType debtType;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingAmount;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Column(nullable = false)
    private LocalDate dueDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebtStatus status;
    
    @Column(nullable = false)
    private Integer priority;
    
    @Column(unique = true, nullable = false)
    private String structuredReference;
    
    @Version
    private Integer version;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant updatedAt;
    
    private Instant settledAt;
    
    @PrePersist
    protected void onCreate() {
        if (debtId == null) {
            debtId = UUID.randomUUID();
        }
        if (debtCode == null) {
            debtCode = LocalDate.now().getYear() + "-" + String.format("%06d", System.currentTimeMillis() % 1000000);
        }
        if (structuredReference == null) {
            structuredReference = debtCode.replace("-", "") + "00";
        }
        if (status == null) {
            status = DebtStatus.OPEN;
        }
        if (priority == null) {
            priority = 1;
        }
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

// Made with Bob
