package com.allobank.splitbill.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ExpenseResponse(
    Long id,
    Long groupId,
    String description,
    BigDecimal amount,
    Long paidById,
    String paidByName,
    LocalDateTime createdAt,
    List<ExpenseShareResponse> shares
) {

    public record ExpenseShareResponse(
        Long participantId,
        String participantName,
        BigDecimal shareAmount
    ) {
    }
}
