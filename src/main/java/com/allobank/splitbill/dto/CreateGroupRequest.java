package com.allobank.splitbill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateGroupRequest(

    @NotBlank(message = "Group name is required")
    @Size(max = 100, message = "Group name must not exceed 100 characters")
    String name,

    @NotEmpty(message = "At least one participant is required")
    @Size(max = 100, message = "A group cannot have more than 100 participants")
    List<
        @NotBlank(message = "Participant names cannot be blank")
        @Size(max = 100, message = "Participant names must not exceed 100 characters")
        String
    > participants

) {
}
