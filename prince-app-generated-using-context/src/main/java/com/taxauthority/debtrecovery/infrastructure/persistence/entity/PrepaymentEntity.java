package com.taxauthority.debtrecovery.infrastructure.persistence.entity;

import com.taxauthority.debtrecovery.domain.model.enums.PrepaymentStatus;
import com.taxauthority.debtrecovery.domain.model.enums.PrepaymentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prepayments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrepaymentEntity {
    
    @Id
    private UUID prepaymentId;
    
    @Column(unique = true, nullable = false)
    private String prepaymentCode;
    
    @Column(nullable = false)
    private UUID citizenId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrepaymentType prepaymentType;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    private String description;
    
    private String paymentMethod;
    
    private String paymentGatewayReference;
    
    @Column(unique = true, nullable = false)
    private String idempotencyKey;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrepaymentStatus status;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    private Instant confirmedAt;
    
    @PrePersist
    protected void onCreate() {
        if (prepaymentId == null) {
            prepaymentId = UUID.randomUUID();
        }
        if (prepaymentCode == null) {
            prepaymentCode = "PP-" + System.currentTimeMillis();
        }
        if (status == null) {
            status = PrepaymentStatus.PENDING_PAYMENT;
        }
        createdAt = Instant.now();
    }
}

// Made with Bob
