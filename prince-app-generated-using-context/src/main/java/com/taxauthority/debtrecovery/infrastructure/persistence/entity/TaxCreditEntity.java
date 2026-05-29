package com.taxauthority.debtrecovery.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tax_credits")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxCreditEntity {
    
    @Id
    private UUID taxCreditId;
    
    @Column(unique = true, nullable = false)
    private UUID citizenId;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCredit;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal allocatedCredit;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Version
    private Integer version;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (taxCreditId == null) {
            taxCreditId = UUID.randomUUID();
        }
        if (totalCredit == null) {
            totalCredit = BigDecimal.ZERO;
        }
        if (allocatedCredit == null) {
            allocatedCredit = BigDecimal.ZERO;
        }
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    public BigDecimal getAvailableCredit() {
        return totalCredit.subtract(allocatedCredit);
    }
}

// Made with Bob
