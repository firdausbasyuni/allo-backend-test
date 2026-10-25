package com.allobank.splitbill.controller;

import com.allobank.splitbill.dto.CreatePaymentRequest;
import com.allobank.splitbill.entity.Payment;
import com.allobank.splitbill.service.PaymentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/groups/{groupId}/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createPayment(
            @PathVariable Long groupId,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        Payment payment = paymentService.createPayment(
                groupId,
                request
        );

        return Map.of(
                "id", payment.getId(),
                "groupId", payment.getBillGroup().getId(),
                "paidById", payment.getPaidBy().getId(),
                "paidByName", payment.getPaidBy().getName(),
                "paidToId", payment.getPaidTo().getId(),
                "paidToName", payment.getPaidTo().getName(),
                "amount", payment.getAmount(),
                "description", payment.getDescription() == null
                        ? ""
                        : payment.getDescription(),
                "createdAt", payment.getCreatedAt()
        );
    }
}
