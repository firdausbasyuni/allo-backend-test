package com.allobank.splitbill.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ExpenseSplitRequest(

    @NotNull(message = "Participant ID is required")
    @Positive(message = "Participant ID must be positive")
    Long participantId,

    @DecimalMin(value = "0.00", message = "Percentage cannot be negative")
    @DecimalMax(value = "100.00", message = "Percentage cannot exceed 100")
    BigDecimal percentage,

    @DecimalMin(value = "0.00", message = "Share amount cannot be negative")
    BigDecimal shareAmount

) {}
