package com.taxauthority.debtrecovery.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "citizens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenEntity {
    
    @Id
    private UUID citizenId;
    
    @Column(unique = true, nullable = false)
    private String citizenCode;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(unique = true, nullable = false)
    private String nationalId;
    
    @Column(nullable = false)
    private String email;
    
    private String phone;
    
    private String address;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (citizenId == null) {
            citizenId = UUID.randomUUID();
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
