package com.taxauthority.debtrecovery.infrastructure.web.controller;

import com.taxauthority.debtrecovery.application.service.TaxDebtService;
import com.taxauthority.debtrecovery.domain.model.enums.DebtType;
import com.taxauthority.debtrecovery.domain.model.enums.PrepaymentType;
import com.taxauthority.debtrecovery.infrastructure.persistence.entity.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Tax Debt Recovery", description = "Tax collection and debt recovery API")
public class TaxDebtController {
    
    private final TaxDebtService taxDebtService;
    
    // Citizen endpoints
    @PostMapping("/citizens")
    @Operation(summary = "Create a new citizen")
    public ResponseEntity<CitizenEntity> createCitizen(@RequestBody CreateCitizenRequest request) {
        CitizenEntity citizen = new CitizenEntity();
        citizen.setCitizenCode(request.getCitizenCode());
        citizen.setFirstName(request.getFirstName());
        citizen.setLastName(request.getLastName());
        citizen.setNationalId(request.getNationalId());
        citizen.setEmail(request.getEmail());
        citizen.setPhone(request.getPhone());
        citizen.setAddress(request.getAddress());
        
        CitizenEntity created = taxDebtService.createCitizen(citizen);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @GetMapping("/citizens/{citizenId}")
    @Operation(summary = "Get citizen by ID")
    public ResponseEntity<CitizenEntity> getCitizen(@PathVariable UUID citizenId) {
        return ResponseEntity.ok(taxDebtService.getCitizen(citizenId));
    }
    
    @GetMapping("/citizens/code/{citizenCode}")
    @Operation(summary = "Get citizen by code")
    public ResponseEntity<CitizenEntity> getCitizenByCode(@PathVariable String citizenCode) {
        return ResponseEntity.ok(taxDebtService.getCitizenByCode(citizenCode));
    }
    
    // Prepayment endpoints
    @PostMapping("/prepayments")
    @Operation(summary = "Create a new prepayment")
    public ResponseEntity<PrepaymentEntity> createPrepayment(
            @RequestBody CreatePrepaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        if (idempotencyKey == null) {
            idempotencyKey = UUID.randomUUID().toString();
        }
        
        PrepaymentEntity prepayment = taxDebtService.createPrepayment(
            request.getCitizenId(),
            request.getPrepaymentType(),
            request.getAmount(),
            request.getCurrency(),
            request.getDescription(),
            idempotencyKey
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(prepayment);
    }
    
    @PostMapping("/prepayments/{id}/confirm")
    @Operation(summary = "Confirm prepayment (simulates payment gateway webhook)")
    public ResponseEntity<PrepaymentEntity> confirmPrepayment(
            @PathVariable UUID id,
            @RequestBody ConfirmPrepaymentRequest request) {
        
        PrepaymentEntity confirmed = taxDebtService.confirmPrepayment(id, request.getGatewayReference());
        return ResponseEntity.ok(confirmed);
    }
    
    @GetMapping("/citizens/{citizenId}/prepayments")
    @Operation(summary = "Get all prepayments for a citizen")
    public ResponseEntity<List<PrepaymentEntity>> getPrepayments(@PathVariable UUID citizenId) {
        return ResponseEntity.ok(taxDebtService.getPrepayments(citizenId));
    }
    
    // Tax Credit endpoints
    @GetMapping("/citizens/{citizenId}/tax-credit")
    @Operation(summary = "Get tax credit balance for a citizen")
    public ResponseEntity<TaxCreditEntity> getTaxCredit(@PathVariable UUID citizenId) {
        return ResponseEntity.ok(taxDebtService.getTaxCredit(citizenId));
    }
    
    // Debt endpoints
    @PostMapping("/debts")
    @Operation(summary = "Create a new debt")
    public ResponseEntity<DebtEntity> createDebt(@RequestBody CreateDebtRequest request) {
        DebtEntity debt = new DebtEntity();
        debt.setCitizenId(request.getCitizenId());
        debt.setDebtType(request.getDebtType());
        debt.setOriginalAmount(request.getAmount());
        debt.setOutstandingAmount(request.getAmount());
        debt.setCurrency(request.getCurrency());
        debt.setDueDate(request.getDueDate());
        debt.setPriority(request.getPriority() != null ? request.getPriority() : 1);
        
        DebtEntity created = taxDebtService.createDebt(debt);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @GetMapping("/citizens/{citizenId}/debts")
    @Operation(summary = "Get all debts for a citizen")
    public ResponseEntity<List<DebtEntity>> getDebts(@PathVariable UUID citizenId) {
        return ResponseEntity.ok(taxDebtService.getDebts(citizenId));
    }
    
    @GetMapping("/citizens/{citizenId}/debts/open")
    @Operation(summary = "Get open debts for a citizen")
    public ResponseEntity<List<DebtEntity>> getOpenDebts(@PathVariable UUID citizenId) {
        return ResponseEntity.ok(taxDebtService.getOpenDebts(citizenId));
    }
    
    // DTOs
    @Data
    public static class CreateCitizenRequest {
        private String citizenCode;
        private String firstName;
        private String lastName;
        private String nationalId;
        private String email;
        private String phone;
        private String address;
    }
    
    @Data
    public static class CreatePrepaymentRequest {
        private UUID citizenId;
        private PrepaymentType prepaymentType;
        private BigDecimal amount;
        private String currency;
        private String description;
    }
    
    @Data
    public static class ConfirmPrepaymentRequest {
        private String gatewayReference;
    }
    
    @Data
    public static class CreateDebtRequest {
        private UUID citizenId;
        private DebtType debtType;
        private BigDecimal amount;
        private String currency;
        private LocalDate dueDate;
        private Integer priority;
    }
}

// Made with Bob
