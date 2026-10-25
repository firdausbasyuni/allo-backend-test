package com.allobank.splitbill.dto;

import java.time.LocalDateTime;
import java.util.List;

public record GroupResponse(
    Long id,
    String name,
    LocalDateTime createdAt,
    List<ParticipantResponse> participants
) {
}
