package com.votingbooth.law.api.service;

import com.votingbooth.law.api.model.dto.VoteLinkDto;
import com.votingbooth.law.core.dataaccess.repository.LawVoteLinkRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VoteLinkService {

    private final LawVoteLinkRepository lawVoteLinkRepository;

    public VoteLinkService(LawVoteLinkRepository lawVoteLinkRepository) {
        this.lawVoteLinkRepository = lawVoteLinkRepository;
    }

    // Every link at once: a few hundred rows, read by the vote service's nightly job.
    public List<VoteLinkDto> getVoteLinks() {
        return lawVoteLinkRepository.findAll().stream()
                .map(link -> new VoteLinkDto(link.getLawId(), link.getSource(), link.getScrutinExternalId(), link.getKind(),
                        link.isDecisive()))
                .toList();
    }

}
