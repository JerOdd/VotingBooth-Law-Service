package com.votingbooth.law.api.controller;

import com.votingbooth.law.api.model.dto.VoteLinkDto;
import com.votingbooth.law.api.service.VoteLinkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vote-links")
public class VoteLinkController {

    private final VoteLinkService voteLinkService;

    public VoteLinkController(VoteLinkService voteLinkService) {
        this.voteLinkService = voteLinkService;
    }

    @GetMapping
    public ResponseEntity<List<VoteLinkDto>> getVoteLinks() {
        return ResponseEntity.ok(voteLinkService.getVoteLinks());
    }

}
