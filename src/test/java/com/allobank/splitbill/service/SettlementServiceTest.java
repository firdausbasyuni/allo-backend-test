package com.allobank.splitbill.service;

import com.allobank.splitbill.dto.SettlementResponse;
import com.allobank.splitbill.entity.BillGroup;
import com.allobank.splitbill.entity.Expense;
import com.allobank.splitbill.entity.ExpenseShare;
import com.allobank.splitbill.entity.Participant;
import com.allobank.splitbill.entity.Payment;
import com.allobank.splitbill.repository.BillGroupRepository;
import com.allobank.splitbill.repository.ExpenseRepository;
import com.allobank.splitbill.repository.ExpenseShareRepository;
import com.allobank.splitbill.repository.ParticipantRepository;
import com.allobank.splitbill.repository.PaymentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private BillGroupRepository groupRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseShareRepository shareRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ServiceChargeCalculator serviceChargeCalculator;

    private SettlementService settlementService;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService(
            groupRepository,
            participantRepository,
            expenseRepository,
            shareRepository,
            paymentRepository,
            serviceChargeCalculator,
            "firdausbasyuni"
        );
    }

    @Test
    void calculatesBalancesAndTransfersCorrectly() {
        BillGroup group = new BillGroup("Bali Trip");

        Participant firdaus = new Participant("Firdaus", group);
        Participant andi = new Participant("Andi", group);
        Participant budi = new Participant("Budi", group);

        setId(firdaus, 1L);
        setId(andi, 2L);
        setId(budi, 3L);
        setId(group, 1L);

        Expense dinner = new Expense(
            group,
            firdaus,
            new BigDecimal("300000.00"),
            "Dinner"
        );
        setId(dinner, 1L);

        when(groupRepository.findById(1L))
            .thenReturn(Optional.of(group));

        when(participantRepository.findByBillGroupId(1L))
            .thenReturn(List.of(firdaus, andi, budi));

        when(expenseRepository.findByBillGroupId(1L))
            .thenReturn(List.of(dinner));

        when(shareRepository.findByExpenseIdIn(List.of(1L)))
            .thenReturn(List.of(
                new ExpenseShare(dinner, firdaus, new BigDecimal("100000.00")),
                new ExpenseShare(dinner, andi, new BigDecimal("100000.00")),
                new ExpenseShare(dinner, budi, new BigDecimal("100000.00"))
            ));

        when(paymentRepository.findByBillGroupIdOrderByCreatedAtDesc(1L))
            .thenReturn(List.of());

        when(serviceChargeCalculator.calculatePercentage("firdausbasyuni"))
            .thenReturn(3);

        SettlementResponse result = settlementService.getSettlement(1L);

        assertEquals(new BigDecimal("300000.00"), result.totalExpenses());
        assertEquals(3, result.serviceChargePct());
        assertEquals(
            new BigDecimal("9000.00"),
            result.serviceChargeAmount()
        );

        assertEquals(3, result.balances().size());

        assertEquals(
            new BigDecimal("200000.00"),
            result.balances().get(0).netBalance()
        );

        assertEquals(
            new BigDecimal("-100000.00"),
            result.balances().get(1).netBalance()
        );

        assertEquals(
            new BigDecimal("-100000.00"),
            result.balances().get(2).netBalance()
        );

        assertEquals(2, result.transfers().size());

        assertEquals(
            new BigDecimal("100000.00"),
            result.transfers().get(0).amount()
        );

        assertEquals(
            new BigDecimal("100000.00"),
            result.transfers().get(1).amount()
        );
    }

    @Test
    void producesNoTransfersWhenEveryoneHasPaidTheirOwnShare() {
        BillGroup group = new BillGroup("Balanced Dinner");

        Participant firdaus = new Participant("Firdaus", group);
        Participant andi = new Participant("Andi", group);
        Participant budi = new Participant("Budi", group);

        setId(group, 1L);
        setId(firdaus, 1L);
        setId(andi, 2L);
        setId(budi, 3L);

        Expense firdausExpense = new Expense(
            group, firdaus, new BigDecimal("100.00"), "Firdaus dinner"
        );
        Expense andiExpense = new Expense(
            group, andi, new BigDecimal("100.00"), "Andi dinner"
        );
        Expense budiExpense = new Expense(
            group, budi, new BigDecimal("100.00"), "Budi dinner"
        );

        setId(firdausExpense, 1L);
        setId(andiExpense, 2L);
        setId(budiExpense, 3L);

        when(groupRepository.findById(1L))
            .thenReturn(Optional.of(group));

        when(participantRepository.findByBillGroupId(1L))
            .thenReturn(List.of(firdaus, andi, budi));

        when(expenseRepository.findByBillGroupId(1L))
            .thenReturn(List.of(firdausExpense, andiExpense, budiExpense));

        when(shareRepository.findByExpenseIdIn(List.of(1L, 2L, 3L)))
            .thenReturn(List.of(
                new ExpenseShare(firdausExpense, firdaus, new BigDecimal("100.00")),
                new ExpenseShare(andiExpense, andi, new BigDecimal("100.00")),
                new ExpenseShare(budiExpense, budi, new BigDecimal("100.00"))
            ));

        when(paymentRepository.findByBillGroupIdOrderByCreatedAtDesc(1L))
            .thenReturn(List.of());

        when(serviceChargeCalculator.calculatePercentage("firdausbasyuni"))
            .thenReturn(3);

        SettlementResponse result = settlementService.getSettlement(1L);

        assertEquals(new BigDecimal("300.00"), result.totalExpenses());
        assertTrue(result.transfers().isEmpty());

        for (SettlementResponse.ParticipantBalance balance : result.balances()) {
            assertEquals(
                new BigDecimal("0.00"),
                balance.netBalance(),
                balance.participantName() + " should have a zero balance"
            );
        }
    }


    @Test
    void repaymentReducesOutstandingBalances() {
        BillGroup group = new BillGroup("Bali Trip");

        Participant firdaus = new Participant("Firdaus", group);
        Participant andi = new Participant("Andi", group);

        setId(group, 1L);
        setId(firdaus, 1L);
        setId(andi, 2L);

        Expense dinner = new Expense(
            group,
            firdaus,
            new BigDecimal("200.00"),
            "Dinner"
        );
        setId(dinner, 1L);

        Payment repayment = new Payment(
            group,
            andi,
            firdaus,
            new BigDecimal("40.00"),
            "Repayment"
        );

        when(groupRepository.findById(1L))
            .thenReturn(Optional.of(group));

        when(participantRepository.findByBillGroupId(1L))
            .thenReturn(List.of(firdaus, andi));

        when(expenseRepository.findByBillGroupId(1L))
            .thenReturn(List.of(dinner));

        when(shareRepository.findByExpenseIdIn(List.of(1L)))
            .thenReturn(List.of(
                new ExpenseShare(dinner, firdaus, new BigDecimal("100.00")),
                new ExpenseShare(dinner, andi, new BigDecimal("100.00"))
            ));

        when(paymentRepository.findByBillGroupIdOrderByCreatedAtDesc(1L))
            .thenReturn(List.of(repayment));

        when(serviceChargeCalculator.calculatePercentage("firdausbasyuni"))
            .thenReturn(3);

        SettlementResponse result = settlementService.getSettlement(1L);

        assertEquals(new BigDecimal("200.00"), result.totalExpenses());
        assertEquals(2, result.balances().size());

        assertEquals(
            new BigDecimal("60.00"),
            result.balances().get(0).netBalance()
        );

        assertEquals(
            new BigDecimal("-60.00"),
            result.balances().get(1).netBalance()
        );

        assertEquals(1, result.transfers().size());
        assertEquals(
            new BigDecimal("60.00"),
            result.transfers().get(0).amount()
        );
    }


    private static void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(
                "Unable to set test entity ID",
                exception
            );
        }
    }
}
