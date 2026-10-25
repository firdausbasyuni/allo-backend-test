package com.allobank.splitbill.dto;

import com.allobank.splitbill.entity.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateExpenseRequest(

    @NotNull(message = "Payer ID is required")
    @Positive(message = "Payer ID must be positive")
    Long paidById,

    @NotNull(message = "Expense amount is required")
    @DecimalMin(value = "0.01", message = "Expense amount must be greater than zero")
    BigDecimal amount,

    @NotBlank(message = "Description is required")
    @Size(max = 200, message = "Description cannot exceed 200 characters")
    String description,

    @NotNull(message = "Split type is required")
    SplitType splitType,

    @NotEmpty(message = "At least one participant is required")
    @Size(max = 100, message = "Cannot split an expense among more than 100 participants")
    List<@NotNull @Valid ExpenseSplitRequest> splits

) {}
