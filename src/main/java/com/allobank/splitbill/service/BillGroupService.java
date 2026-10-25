package com.allobank.splitbill.service;

import com.allobank.splitbill.dto.CreateGroupRequest;
import com.allobank.splitbill.dto.GroupResponse;
import com.allobank.splitbill.dto.ParticipantResponse;
import com.allobank.splitbill.entity.BillGroup;
import com.allobank.splitbill.entity.Participant;
import com.allobank.splitbill.repository.BillGroupRepository;
import com.allobank.splitbill.repository.ParticipantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BillGroupService {

    private final BillGroupRepository billGroupRepository;
    private final ParticipantRepository participantRepository;

    public BillGroupService(
        BillGroupRepository billGroupRepository,
        ParticipantRepository participantRepository
    ) {
        this.billGroupRepository = billGroupRepository;
        this.participantRepository = participantRepository;
    }

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request) {
        BillGroup group = billGroupRepository.save(
            new BillGroup(request.name().trim())
        );

        List<Participant> participants = request.participants()
            .stream()
            .map(String::trim)
            .map(name -> new Participant(name, group))
            .toList();

        List<ParticipantResponse> participantResponses =
            participantRepository.saveAll(participants)
                .stream()
                .map(participant -> new ParticipantResponse(
                    participant.getId(),
                    participant.getName()
                ))
                .toList();

        return new GroupResponse(
            group.getId(),
            group.getName(),
            group.getCreatedAt(),
            participantResponses
        );
    }
}
