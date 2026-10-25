package com.allobank.splitbill.service;

import com.allobank.splitbill.dto.CreatePaymentRequest;
import com.allobank.splitbill.entity.BillGroup;
import com.allobank.splitbill.entity.Participant;
import com.allobank.splitbill.entity.Payment;
import com.allobank.splitbill.repository.BillGroupRepository;
import com.allobank.splitbill.repository.ParticipantRepository;
import com.allobank.splitbill.repository.PaymentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PaymentService {

    private final BillGroupRepository billGroupRepository;
    private final ParticipantRepository participantRepository;
    private final PaymentRepository paymentRepository;
    private final AuditService auditService;

    public PaymentService(
            BillGroupRepository billGroupRepository,
            ParticipantRepository participantRepository,
            PaymentRepository paymentRepository,
            AuditService auditService
    ) {
        this.billGroupRepository = billGroupRepository;
        this.participantRepository = participantRepository;
        this.paymentRepository = paymentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Payment createPayment(
            Long groupId,
            CreatePaymentRequest request
    ) {
        BillGroup group = billGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bill group not found"
                ));

        Participant paidBy = participantRepository
                .findById(request.paidById())
                .filter(p -> p.getBillGroup().getId().equals(groupId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Payer must belong to the specified group"
                ));

        Participant paidTo = participantRepository
                .findById(request.paidToId())
                .filter(p -> p.getBillGroup().getId().equals(groupId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Recipient must belong to the specified group"
                ));

        if (paidBy.getId().equals(paidTo.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payer and recipient must be different participants"
            );
        }

        BigDecimal amount;

        try {
            amount = request.amount().setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment amount must have no more than 2 decimal places"
            );
        }

        String description = request.description() == null
                ? null
                : request.description().trim();

        if (description != null && description.isEmpty()) {
            description = null;
        }

        Payment payment = paymentRepository.save(
                new Payment(
                        group,
                        paidBy,
                        paidTo,
                        amount,
                        description
                )
        );

        String auditDetails =
                "Payment added: amount=" + amount.toPlainString()
                + "; paidBy=" + paidBy.getName()
                + "; paidTo=" + paidTo.getName()
                + (description == null
                    ? ""
                    : "; description=" + description);

        auditService.logEvent(
                "PAYMENT_ADDED",
                group.getId(),
                payment.getId(),
                auditDetails
        );

        return payment;
    }
}
