package com.votingbooth.law.api.service;

import com.votingbooth.law.api.model.dto.LawSummaryDto;
import com.votingbooth.law.api.model.dto.LawsResponse;
import com.votingbooth.law.api.model.exception.InvalidPageException;
import com.votingbooth.law.core.dataaccess.repository.LawRepository;
import com.votingbooth.law.core.model.entity.InitiatorType;
import com.votingbooth.law.core.model.entity.LawStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

@Service
public class LawService {

    static final int MAX_PAGE_SIZE = 100;
    // Law dates are French dates, so "today" is the Paris date.
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private final LawRepository lawRepository;
    private final Clock clock;
    private final int stalledAfterMonths;

    public LawService(LawRepository lawRepository, Clock clock,
                      @Value("${law.stalled-after-months:6}") int stalledAfterMonths) {
        this.lawRepository = lawRepository;
        this.clock = clock;
        this.stalledAfterMonths = stalledAfterMonths;
    }

    // Laws only (not resolutions, reports...), most recent activity first.
    public LawsResponse getLaws(int page, int size, LawStatus status, String category) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPageException(page, size, MAX_PAGE_SIZE);
        }
        LocalDate stalledBefore = LocalDate.now(clock.withZone(PARIS)).minusMonths(stalledAfterMonths);
        Page<LawRepository.LawSummary> laws = lawRepository.findSummaries(
                status == null ? null : status.name(), category, stalledBefore, PageRequest.of(page, size));
        return new LawsResponse(
                laws.getContent().stream().map(LawService::toDto).toList(),
                page,
                size,
                laws.getTotalElements(),
                laws.getTotalPages()
        );
    }

    private static LawSummaryDto toDto(LawRepository.LawSummary law) {
        return new LawSummaryDto(
                law.getId(),
                law.getExternalId(),
                law.getTitle(),
                law.getShortTitle(),
                law.getLegislature(),
                InitiatorType.valueOf(law.getInitiatorType()),
                LawStatus.valueOf(law.getStatus()),
                law.getCurrentStageLabel(),
                law.getLastActDate(),
                law.getNextScheduledDate(),
                law.getPromulgationDate(),
                law.getCategories() == null ? List.of() : Arrays.asList(law.getCategories().split(","))
        );
    }

}
