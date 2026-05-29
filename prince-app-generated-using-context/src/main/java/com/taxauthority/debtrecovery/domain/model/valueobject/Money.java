package com.taxauthority.debtrecovery.domain.model.valueobject;

import lombok.Value;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money value object representing an amount with currency.
 * Immutable and ensures currency consistency in operations.
 */
@Value
public class Money {
    BigDecimal amount;
    String currency;
    
    public Money(BigDecimal amount, String currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency.toUpperCase();
    }
    
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    public Money subtract(Money other) {
        validateSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Result cannot be negative");
        }
        return new Money(result, this.currency);
    }
    
    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }
    
    public boolean isGreaterThanOrEqual(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }
    
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot operate on different currencies");
        }
    }
    
    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }
    
    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }
}

// Made with Bob
