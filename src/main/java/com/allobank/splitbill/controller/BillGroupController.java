package com.allobank.splitbill.controller;

import com.allobank.splitbill.dto.CreateGroupRequest;
import com.allobank.splitbill.dto.GroupResponse;
import com.allobank.splitbill.service.BillGroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
public class BillGroupController {

    private final BillGroupService billGroupService;

    public BillGroupController(BillGroupService billGroupService) {
        this.billGroupService = billGroupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse createGroup(
        @Valid @RequestBody CreateGroupRequest request
    ) {
        return billGroupService.createGroup(request);
    }
}
