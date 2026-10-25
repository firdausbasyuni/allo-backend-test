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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SettlementService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final BillGroupRepository groupRepository;
    private final ParticipantRepository participantRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository shareRepository;
    private final PaymentRepository paymentRepository;
    private final String githubUsername;
    private final ServiceChargeCalculator serviceChargeCalculator;

    public SettlementService(
            BillGroupRepository groupRepository,
            ParticipantRepository participantRepository,
            ExpenseRepository expenseRepository,
            ExpenseShareRepository shareRepository,
            PaymentRepository paymentRepository,
            ServiceChargeCalculator serviceChargeCalculator,
            @Value("${app.github-username}") String githubUsername
    ) {
        this.groupRepository = groupRepository;
        this.participantRepository = participantRepository;
        this.expenseRepository = expenseRepository;
        this.shareRepository = shareRepository;
        this.paymentRepository = paymentRepository;
        this.githubUsername = githubUsername;
        this.serviceChargeCalculator = serviceChargeCalculator;
    }

    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(Long groupId) {
        BillGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Bill group not found"
                ));

        List<Participant> participants =
                participantRepository.findByBillGroupId(groupId);

        List<Expense> expenses =
                expenseRepository.findByBillGroupId(groupId);

        Map<Long, BigDecimal> paid = new LinkedHashMap<>();
        Map<Long, BigDecimal> shares = new LinkedHashMap<>();
        Map<Long, Participant> participantById = new LinkedHashMap<>();

        for (Participant participant : participants) {
            Long id = participant.getId();
            participantById.put(id, participant);
            paid.put(id, ZERO);
            shares.put(id, ZERO);
        }

        BigDecimal totalExpenses = ZERO;

        for (Expense expense : expenses) {
            BigDecimal amount = expense.getAmount();
            totalExpenses = totalExpenses.add(amount);

            Long payerId = expense.getPaidBy().getId();
            paid.put(payerId, paid.get(payerId).add(amount));
        }

        List<ExpenseShare> allShares = expenses.isEmpty()
                ? List.of()
                : shareRepository.findByExpenseIdIn(
                        expenses.stream()
                                .map(Expense::getId)
                                .toList()
                );

        for (ExpenseShare share : allShares) {
            Long participantId = share.getParticipant().getId();

            shares.put(
                    participantId,
                    shares.get(participantId).add(share.getShareAmount())
            );
        }

        int serviceChargePct =
                serviceChargeCalculator.calculatePercentage(githubUsername);

        BigDecimal serviceChargeAmount = totalExpenses
                .multiply(BigDecimal.valueOf(serviceChargePct))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        Map<Long, BigDecimal> netBalances = new LinkedHashMap<>();

        for (Participant participant : participants) {
            Long id = participant.getId();

            BigDecimal net = paid.get(id)
                    .subtract(shares.get(id))
                    .setScale(2, RoundingMode.HALF_UP);

            netBalances.put(id, net);
        }

        List<Payment> payments =
                paymentRepository.findByBillGroupIdOrderByCreatedAtDesc(groupId);

        for (Payment payment : payments) {
            Long senderId = payment.getPaidBy().getId();
            Long recipientId = payment.getPaidTo().getId();
            BigDecimal amount = payment.getAmount();

            if (!netBalances.containsKey(senderId)
                    || !netBalances.containsKey(recipientId)) {
                throw new IllegalStateException(
                        "Payment references a participant outside its bill group"
                );
            }

            netBalances.put(
                    senderId,
                    netBalances.get(senderId).add(amount)
            );

            netBalances.put(
                    recipientId,
                    netBalances.get(recipientId).subtract(amount)
            );
        }

        List<SettlementResponse.ParticipantBalance> balances =
                new ArrayList<>();

        for (Participant participant : participants) {
            Long id = participant.getId();

            BigDecimal totalPaid =
                    paid.get(id).setScale(2, RoundingMode.HALF_UP);

            BigDecimal totalShare =
                    shares.get(id).setScale(2, RoundingMode.HALF_UP);

            BigDecimal net =
                    netBalances.get(id).setScale(2, RoundingMode.HALF_UP);

            balances.add(new SettlementResponse.ParticipantBalance(
                    id,
                    participant.getName(),
                    totalPaid,
                    totalShare,
                    net
            ));
        }

        List<SettlementResponse.Transfer> transfers =
                calculateTransfers(participants, netBalances);

        return new SettlementResponse(
                group.getId(),
                totalExpenses.setScale(2, RoundingMode.HALF_UP),
                serviceChargePct,
                serviceChargeAmount,
                balances,
                transfers
        );
    }

    private List<SettlementResponse.Transfer> calculateTransfers(
            List<Participant> participants,
            Map<Long, BigDecimal> netBalances
    ) {
        Map<Long, BigDecimal> debtors = new LinkedHashMap<>();
        Map<Long, BigDecimal> creditors = new LinkedHashMap<>();

        for (Participant participant : participants) {
            Long id = participant.getId();
            BigDecimal balance = netBalances.get(id);

            if (balance.compareTo(ZERO) < 0) {
                debtors.put(id, balance.abs());
            } else if (balance.compareTo(ZERO) > 0) {
                creditors.put(id, balance);
            }
        }

        List<SettlementResponse.Transfer> transfers = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> debtor : debtors.entrySet()) {
            Long debtorId = debtor.getKey();
            BigDecimal remainingDebt = debtor.getValue();

            for (Map.Entry<Long, BigDecimal> creditor : creditors.entrySet()) {
                if (remainingDebt.compareTo(ZERO) <= 0) {
                    break;
                }

                Long creditorId = creditor.getKey();
                BigDecimal remainingCredit = creditor.getValue();

                if (remainingCredit.compareTo(ZERO) <= 0) {
                    continue;
                }

                BigDecimal transferAmount = remainingDebt.min(remainingCredit)
                        .setScale(2, RoundingMode.HALF_UP);

                transfers.add(new SettlementResponse.Transfer(
                        debtorId,
                        findName(participants, debtorId),
                        creditorId,
                        findName(participants, creditorId),
                        transferAmount
                ));

                remainingDebt = remainingDebt.subtract(transferAmount);

                creditors.put(
                        creditorId,
                        remainingCredit.subtract(transferAmount)
                );
            }
        }

        return transfers;
    }

    private String findName(List<Participant> participants, Long id) {
        return participants.stream()
                .filter(participant -> participant.getId().equals(id))
                .map(Participant::getName)
                .findFirst()
                .orElseThrow();
    }
}
