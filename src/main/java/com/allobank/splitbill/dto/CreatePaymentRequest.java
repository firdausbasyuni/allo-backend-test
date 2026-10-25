package com.allobank.splitbill.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePaymentRequest(

        @NotNull(message = "Payer ID is required")
        @Positive(message = "Payer ID must be positive")
        Long paidById,

        @NotNull(message = "Recipient ID is required")
        @Positive(message = "Recipient ID must be positive")
        Long paidToId,

        @NotNull(message = "Payment amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Payment amount must be greater than zero"
        )
        BigDecimal amount,

        @Size(
                max = 200,
                message = "Description cannot exceed 200 characters"
        )
        String description

) {}
