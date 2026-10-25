package com.allobank.splitbill.repository;

import com.allobank.splitbill.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    List<Participant> findByBillGroupId(Long groupId);
}
