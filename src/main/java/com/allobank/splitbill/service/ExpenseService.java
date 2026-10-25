package com.allobank.splitbill.service;

import com.allobank.splitbill.dto.CreateExpenseRequest;
import com.allobank.splitbill.dto.ExpenseResponse;
import com.allobank.splitbill.dto.ExpenseSplitRequest;
import com.allobank.splitbill.entity.BillGroup;
import com.allobank.splitbill.entity.Expense;
import com.allobank.splitbill.entity.ExpenseShare;
import com.allobank.splitbill.entity.Participant;
import com.allobank.splitbill.entity.SplitType;
import com.allobank.splitbill.repository.BillGroupRepository;
import com.allobank.splitbill.repository.ExpenseRepository;
import com.allobank.splitbill.repository.ExpenseShareRepository;
import com.allobank.splitbill.repository.ParticipantRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final BillGroupRepository billGroupRepository;
    private final ParticipantRepository participantRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository expenseShareRepository;
    private final AuditService auditService;

    public ExpenseService(
            BillGroupRepository billGroupRepository,
            ParticipantRepository participantRepository,
            ExpenseRepository expenseRepository,
            ExpenseShareRepository expenseShareRepository,
            AuditService auditService
    ) {
        this.billGroupRepository = billGroupRepository;
        this.participantRepository = participantRepository;
        this.expenseRepository = expenseRepository;
        this.expenseShareRepository = expenseShareRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ExpenseResponse createExpense(
            Long groupId,
            CreateExpenseRequest request
    ) {
        BillGroup group = billGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Bill group not found"
                ));

        List<Participant> groupParticipants =
                participantRepository.findByBillGroupId(groupId);

        Map<Long, Participant> participantsById =
                groupParticipants.stream().collect(Collectors.toMap(
                        Participant::getId,
                        Function.identity()
                ));

        Participant payer = participantsById.get(request.paidById());

        if (payer == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payer must belong to the specified group"
            );
        }

        List<ExpenseSplitRequest> splits = request.splits();

        Set<Long> requestedIds = new HashSet<>();

        for (ExpenseSplitRequest split : splits) {
            if (!requestedIds.add(split.participantId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Duplicate participant IDs are not allowed"
                );
            }

            if (!participantsById.containsKey(split.participantId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "All participants must belong to the specified group"
                );
            }
        }

        BigDecimal amount;

        try {
            amount = request.amount().setScale(
                    2, RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Expense amount must have no more than 2 decimal places"
            );
        }

        String description = request.description().trim();

        if (description.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Description is required"
            );
        }

        List<BigDecimal> shareAmounts =
                calculateShares(amount, request.splitType(), splits);

        Expense expense = expenseRepository.save(
                new Expense(group, payer, amount, description)
        );

        List<ExpenseShare> shares = new ArrayList<>();

        for (int i = 0; i < splits.size(); i++) {
            Participant participant =
                    participantsById.get(splits.get(i).participantId());

            shares.add(new ExpenseShare(
                    expense,
                    participant,
                    shareAmounts.get(i)
            ));
        }

        List<ExpenseShare> savedShares =
                expenseShareRepository.saveAll(shares);

        String auditDetails =
                "Expense added: " + description
                + "; amount=" + amount.toPlainString()
                + "; payer=" + payer.getName()
                + "; splitType=" + request.splitType();

        auditService.logEvent(
                "EXPENSE_ADDED",
                group.getId(),
                expense.getId(),
                auditDetails
        );

        List<ExpenseResponse.ExpenseShareResponse> shareResponses =
                savedShares.stream()
                        .map(share ->
                                new ExpenseResponse.ExpenseShareResponse(
                                        share.getParticipant().getId(),
                                        share.getParticipant().getName(),
                                        share.getShareAmount()
                                )
                        )
                        .toList();

        return new ExpenseResponse(
                expense.getId(),
                group.getId(),
                expense.getDescription(),
                expense.getAmount(),
                payer.getId(),
                payer.getName(),
                expense.getCreatedAt(),
                shareResponses
        );
    }

    private List<BigDecimal> calculateShares(
            BigDecimal amount,
            SplitType splitType,
            List<ExpenseSplitRequest> splits
    ) {
        return switch (splitType) {
            case EQUAL -> calculateEqualShares(amount, splits);
            case PERCENTAGE -> calculatePercentageShares(amount, splits);
            case EXACT_AMOUNT -> calculateExactShares(amount, splits);
        };
    }

    private List<BigDecimal> calculateEqualShares(
            BigDecimal amount,
            List<ExpenseSplitRequest> splits
    ) {
        for (ExpenseSplitRequest split : splits) {
            if (split.percentage() != null
                    || split.shareAmount() != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Equal splits must only contain participant IDs"
                );
            }
        }

        BigDecimal baseShare = amount.divide(
                BigDecimal.valueOf(splits.size()),
                2,
                RoundingMode.DOWN
        );

        List<BigDecimal> amounts = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;

        for (int i = 0; i < splits.size(); i++) {
            BigDecimal share = baseShare;

            if (i == splits.size() - 1) {
                share = amount.subtract(allocated);
            }

            amounts.add(share);
            allocated = allocated.add(share);
        }

        return amounts;
    }

    private List<BigDecimal> calculatePercentageShares(
            BigDecimal amount,
            List<ExpenseSplitRequest> splits
    ) {
        BigDecimal totalPercentage = BigDecimal.ZERO;

        for (ExpenseSplitRequest split : splits) {
            if (split.percentage() == null
                    || split.shareAmount() != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Percentage splits require a percentage for each participant and no share amount"
                );
            }

            totalPercentage = totalPercentage.add(split.percentage());
        }

        if (totalPercentage.compareTo(new BigDecimal("100")) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Percentages must total exactly 100"
            );
        }

        List<BigDecimal> amounts = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;

        for (int i = 0; i < splits.size(); i++) {
            BigDecimal share;

            if (i == splits.size() - 1) {
                share = amount.subtract(allocated);
            } else {
                share = amount
                        .multiply(splits.get(i).percentage())
                        .divide(
                                new BigDecimal("100"),
                                2,
                                RoundingMode.DOWN
                        );

                allocated = allocated.add(share);
            }

            amounts.add(share);
        }

        return amounts;
    }

    private List<BigDecimal> calculateExactShares(
            BigDecimal amount,
            List<ExpenseSplitRequest> splits
    ) {
        List<BigDecimal> amounts = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (ExpenseSplitRequest split : splits) {
            if (split.shareAmount() == null
                    || split.percentage() != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Exact splits require a share amount for each participant and no percentage"
                );
            }

            BigDecimal share;

            try {
                share = split.shareAmount().setScale(
                        2, RoundingMode.UNNECESSARY
                );
            } catch (ArithmeticException exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Each share amount must have no more than 2 decimal places"
                );
            }

            amounts.add(share);
            total = total.add(share);
        }

        if (total.compareTo(amount) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Exact share amounts must equal the expense amount"
            );
        }

        return amounts;
    }
}
