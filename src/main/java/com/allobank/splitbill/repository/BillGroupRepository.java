package com.allobank.splitbill.repository;

import com.allobank.splitbill.entity.BillGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillGroupRepository extends JpaRepository<BillGroup, Long> {
}
