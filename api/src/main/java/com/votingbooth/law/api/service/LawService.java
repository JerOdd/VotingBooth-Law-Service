package com.votingbooth.law.api.service;

import com.votingbooth.law.api.model.dto.LawDto;
import com.votingbooth.law.api.model.dto.LawsResponse;
import com.votingbooth.law.core.dataaccess.repository.LawRepository;
import org.springframework.stereotype.Service;

@Service
public class LawService {

    private final LawRepository lawRepository;

    public LawService(LawRepository lawRepository) {
        this.lawRepository = lawRepository;
    }

    public LawsResponse getLaws() {
        return new LawsResponse(
            lawRepository.findAll().stream()
            .map(law -> new LawDto(law.getId()))
            .toList()
        );
    }

}
