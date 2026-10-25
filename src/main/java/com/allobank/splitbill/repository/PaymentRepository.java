package com.allobank.splitbill.repository;

import com.allobank.splitbill.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    List<Payment> findByBillGroupIdOrderByCreatedAtDesc(Long groupId);

    List<Payment> findByPaidByIdOrderByCreatedAtDesc(Long participantId);

    List<Payment> findByPaidToIdOrderByCreatedAtDesc(Long participantId);
}
