package com.votingbooth.law.api.service;

import com.votingbooth.law.api.model.dto.VoteLinkDto;
import com.votingbooth.law.core.dataaccess.repository.LawVoteLinkRepository;
import com.votingbooth.law.core.model.entity.LawVoteLink;
import com.votingbooth.law.core.model.entity.VoteLinkKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class VoteLinkServiceTests {

    private LawVoteLinkRepository lawVoteLinkRepository;
    private VoteLinkService voteLinkService;

    @BeforeEach
    void setUp() {
        lawVoteLinkRepository = mock(LawVoteLinkRepository.class);
        voteLinkService = new VoteLinkService(lawVoteLinkRepository);
    }

    @Test
    void getVoteLinks() {
        UUID lawId = UUID.randomUUID();
        when(lawVoteLinkRepository.findAll()).thenReturn(List.of(
                new LawVoteLink(lawId, UUID.randomUUID(), "assemblee-nationale", "VTANR5L17V4745", VoteLinkKind.TEXT, true),
                new LawVoteLink(lawId, UUID.randomUUID(), "assemblee-nationale", "VTANR5L17V4700", VoteLinkKind.MOTION, false)));

        assertEquals(List.of(
                new VoteLinkDto(lawId, "assemblee-nationale", "VTANR5L17V4745", VoteLinkKind.TEXT, true),
                new VoteLinkDto(lawId, "assemblee-nationale", "VTANR5L17V4700", VoteLinkKind.MOTION, false)
        ), voteLinkService.getVoteLinks());
    }

}
