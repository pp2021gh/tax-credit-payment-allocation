package com.taxauthority.debtrecovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application for Tax Collection and Debt Recovery System.
 * 
 * Context Reference: ctx_99a057884357
 * 
 * This application implements a hexagonal architecture with:
 * - Domain layer: Core business logic and aggregates
 * - Application layer: Use cases and DTOs
 * - Infrastructure layer: Adapters for persistence, web, and external services
 */
@SpringBootApplication
public class TaxDebtRecoveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaxDebtRecoveryApplication.class, args);
    }
}

// Made with Bob
