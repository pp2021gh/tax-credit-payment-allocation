package com.taxauthority.debtrecovery.infrastructure.persistence.repository;

import com.taxauthority.debtrecovery.infrastructure.persistence.entity.TaxCreditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxCreditRepository extends JpaRepository<TaxCreditEntity, UUID> {
    Optional<TaxCreditEntity> findByCitizenId(UUID citizenId);
}

// Made with Bob
