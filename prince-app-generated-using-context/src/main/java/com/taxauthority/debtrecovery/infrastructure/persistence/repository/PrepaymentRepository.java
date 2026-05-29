package com.taxauthority.debtrecovery.infrastructure.persistence.repository;

import com.taxauthority.debtrecovery.infrastructure.persistence.entity.PrepaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrepaymentRepository extends JpaRepository<PrepaymentEntity, UUID> {
    Optional<PrepaymentEntity> findByIdempotencyKey(String idempotencyKey);
    List<PrepaymentEntity> findByCitizenId(UUID citizenId);
    Optional<PrepaymentEntity> findByPrepaymentCode(String prepaymentCode);
}

// Made with Bob
