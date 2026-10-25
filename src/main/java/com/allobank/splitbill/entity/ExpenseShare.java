package com.allobank.splitbill.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "expense_shares",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_expense_share_participant",
            columnNames = {"expense_id", "participant_id"}
        )
    }
)
public class ExpenseShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal shareAmount;

    public ExpenseShare() {
    }

    public ExpenseShare(
        Expense expense,
        Participant participant,
        BigDecimal shareAmount
    ) {
        this.expense = expense;
        this.participant = participant;
        this.shareAmount = shareAmount;
    }

    public Long getId() {
        return id;
    }

    public Expense getExpense() {
        return expense;
    }

    public Participant getParticipant() {
        return participant;
    }

    public BigDecimal getShareAmount() {
        return shareAmount;
    }
}
