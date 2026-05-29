package com.taxauthority.debtrecovery.domain.model.enums;

/**
 * Status of a prepayment in its lifecycle.
 * State machine: PENDING_PAYMENT → COMPLETED/FAILED/CANCELLED
 */
public enum PrepaymentStatus {
    PENDING_PAYMENT,
    COMPLETED,
    FAILED,
    CANCELLED
}

// Made with Bob
