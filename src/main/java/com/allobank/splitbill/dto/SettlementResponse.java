package com.allobank.splitbill.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record SettlementResponse(
    Long groupId,

    @JsonProperty("total_expenses")
    BigDecimal totalExpenses,

    @JsonProperty("service_charge_pct")
    int serviceChargePct,

    @JsonProperty("service_charge_amount")
    BigDecimal serviceChargeAmount,

    List<ParticipantBalance> balances,
    List<Transfer> transfers
) {
    public record ParticipantBalance(
        Long participantId,
        String participantName,
        BigDecimal totalPaid,
        BigDecimal totalShare,
        BigDecimal netBalance
    ) {
    }

    public record Transfer(
        Long fromParticipantId,
        String fromParticipantName,
        Long toParticipantId,
        String toParticipantName,
        BigDecimal amount
    ) {
    }
}
