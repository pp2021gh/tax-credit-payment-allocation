package com.taxauthority.debtrecovery.infrastructure.persistence.repository;

import com.taxauthority.debtrecovery.infrastructure.persistence.entity.CitizenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CitizenRepository extends JpaRepository<CitizenEntity, UUID> {
    Optional<CitizenEntity> findByCitizenCode(String citizenCode);
    Optional<CitizenEntity> findByNationalId(String nationalId);
    Optional<CitizenEntity> findByEmail(String email);
}

// Made with Bob
