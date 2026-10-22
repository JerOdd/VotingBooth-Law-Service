package com.votingbooth.law.api.controller;

import com.votingbooth.law.api.model.dto.LawsResponse;
import com.votingbooth.law.api.service.LawService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/laws")
public class LawController {

    private final LawService lawService;

    public LawController(LawService lawService) {
        this.lawService = lawService;
    }

    @GetMapping
    public ResponseEntity<LawsResponse> getLaws() {
        return ResponseEntity.ok(lawService.getLaws());
    }

}
