package com.allobank.splitbill.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private BillGroup billGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by_id", nullable = false)
    private Participant paidBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_to_id", nullable = false)
    private Participant paidTo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 200)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Payment() {
    }

    public Payment(
            BillGroup billGroup,
            Participant paidBy,
            Participant paidTo,
            BigDecimal amount,
            String description
    ) {
        this.billGroup = billGroup;
        this.paidBy = paidBy;
        this.paidTo = paidTo;
        this.amount = amount;
        this.description = description;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public BillGroup getBillGroup() {
        return billGroup;
    }

    public Participant getPaidBy() {
        return paidBy;
    }

    public Participant getPaidTo() {
        return paidTo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
