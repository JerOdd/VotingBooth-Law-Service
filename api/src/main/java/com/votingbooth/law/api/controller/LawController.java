package com.votingbooth.law.api.controller;

import com.votingbooth.law.api.model.dto.LawsResponse;
import com.votingbooth.law.api.service.LawService;
import com.votingbooth.law.core.model.entity.LawStatus;
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
    public ResponseEntity<LawsResponse> getLaws(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(required = false) LawStatus status,
                                                @RequestParam(required = false) String category) {
        return ResponseEntity.ok(lawService.getLaws(page, size, status, category));
    }

}
