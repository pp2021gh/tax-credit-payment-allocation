package com.taxauthority.debtrecovery.infrastructure.persistence.repository;

import com.taxauthority.debtrecovery.domain.model.enums.DebtStatus;
import com.taxauthority.debtrecovery.infrastructure.persistence.entity.DebtEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DebtRepository extends JpaRepository<DebtEntity, UUID> {
    List<DebtEntity> findByCitizenId(UUID citizenId);
    List<DebtEntity> findByCitizenIdAndStatus(UUID citizenId, DebtStatus status);
    Optional<DebtEntity> findByStructuredReference(String structuredReference);
    Optional<DebtEntity> findByDebtCode(String debtCode);
}

// Made with Bob
