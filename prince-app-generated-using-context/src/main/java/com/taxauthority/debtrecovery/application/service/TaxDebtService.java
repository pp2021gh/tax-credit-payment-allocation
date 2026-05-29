package com.taxauthority.debtrecovery.application.service;

import com.taxauthority.debtrecovery.domain.model.enums.DebtStatus;
import com.taxauthority.debtrecovery.domain.model.enums.PrepaymentStatus;
import com.taxauthority.debtrecovery.domain.model.enums.PrepaymentType;
import com.taxauthority.debtrecovery.infrastructure.persistence.entity.*;
import com.taxauthority.debtrecovery.infrastructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxDebtService {
    
    private final CitizenRepository citizenRepository;
    private final PrepaymentRepository prepaymentRepository;
    private final TaxCreditRepository taxCreditRepository;
    private final DebtRepository debtRepository;
    
    @Transactional
    public PrepaymentEntity createPrepayment(UUID citizenId, PrepaymentType type, 
                                            BigDecimal amount, String currency, 
                                            String description, String idempotencyKey) {
        // Check idempotency
        return prepaymentRepository.findByIdempotencyKey(idempotencyKey)
            .orElseGet(() -> {
                PrepaymentEntity prepayment = new PrepaymentEntity();
                prepayment.setCitizenId(citizenId);
                prepayment.setPrepaymentType(type);
                prepayment.setAmount(amount);
                prepayment.setCurrency(currency);
                prepayment.setDescription(description);
                prepayment.setIdempotencyKey(idempotencyKey);
                prepayment.setStatus(PrepaymentStatus.PENDING_PAYMENT);
                
                return prepaymentRepository.save(prepayment);
            });
    }
    
    @Transactional
    public PrepaymentEntity confirmPrepayment(UUID prepaymentId, String gatewayReference) {
        PrepaymentEntity prepayment = prepaymentRepository.findById(prepaymentId)
            .orElseThrow(() -> new RuntimeException("Prepayment not found"));
        
        if (prepayment.getStatus() != PrepaymentStatus.PENDING_PAYMENT) {
            throw new RuntimeException("Prepayment already processed");
        }
        
        prepayment.setStatus(PrepaymentStatus.COMPLETED);
        prepayment.setPaymentGatewayReference(gatewayReference);
        prepayment.setConfirmedAt(Instant.now());
        prepaymentRepository.save(prepayment);
        
        // Add credit to tax credit account
        addCreditToAccount(prepayment.getCitizenId(), prepayment.getAmount(), 
                          prepayment.getCurrency());
        
        log.info("Prepayment {} confirmed and credit added", prepaymentId);
        return prepayment;
    }
    
    @Transactional
    public TaxCreditEntity addCreditToAccount(UUID citizenId, BigDecimal amount, String currency) {
        TaxCreditEntity taxCredit = taxCreditRepository.findByCitizenId(citizenId)
            .orElseGet(() -> {
                TaxCreditEntity newCredit = new TaxCreditEntity();
                newCredit.setCitizenId(citizenId);
                newCredit.setCurrency(currency);
                newCredit.setTotalCredit(BigDecimal.ZERO);
                newCredit.setAllocatedCredit(BigDecimal.ZERO);
                return newCredit;
            });
        
        taxCredit.setTotalCredit(taxCredit.getTotalCredit().add(amount));
        return taxCreditRepository.save(taxCredit);
    }
    
    @Transactional(readOnly = true)
    public TaxCreditEntity getTaxCredit(UUID citizenId) {
        return taxCreditRepository.findByCitizenId(citizenId)
            .orElseGet(() -> {
                TaxCreditEntity newCredit = new TaxCreditEntity();
                newCredit.setCitizenId(citizenId);
                newCredit.setCurrency("EUR");
                newCredit.setTotalCredit(BigDecimal.ZERO);
                newCredit.setAllocatedCredit(BigDecimal.ZERO);
                return newCredit;
            });
    }
    
    @Transactional(readOnly = true)
    public List<PrepaymentEntity> getPrepayments(UUID citizenId) {
        return prepaymentRepository.findByCitizenId(citizenId);
    }
    
    @Transactional(readOnly = true)
    public List<DebtEntity> getDebts(UUID citizenId) {
        return debtRepository.findByCitizenId(citizenId);
    }
    
    @Transactional(readOnly = true)
    public List<DebtEntity> getOpenDebts(UUID citizenId) {
        return debtRepository.findByCitizenIdAndStatus(citizenId, DebtStatus.OPEN);
    }
    
    @Transactional
    public DebtEntity createDebt(DebtEntity debt) {
        return debtRepository.save(debt);
    }
    
    @Transactional
    public CitizenEntity createCitizen(CitizenEntity citizen) {
        return citizenRepository.save(citizen);
    }
    
    @Transactional(readOnly = true)
    public CitizenEntity getCitizen(UUID citizenId) {
        return citizenRepository.findById(citizenId)
            .orElseThrow(() -> new RuntimeException("Citizen not found"));
    }
    
    @Transactional(readOnly = true)
    public CitizenEntity getCitizenByCode(String citizenCode) {
        return citizenRepository.findByCitizenCode(citizenCode)
            .orElseThrow(() -> new RuntimeException("Citizen not found"));
    }
}

// Made with Bob
