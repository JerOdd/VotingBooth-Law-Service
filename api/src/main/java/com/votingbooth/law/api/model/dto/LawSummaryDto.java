package com.votingbooth.law.api.model.dto;

import com.votingbooth.law.core.model.entity.InitiatorType;
import com.votingbooth.law.core.model.entity.LawStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// One row of the law list. shortTitle is null until the initial text's document is synced: show title instead.
public record LawSummaryDto(
        UUID id,
        String externalId,
        String title,
        String shortTitle,
        int legislature,
        InitiatorType initiatorType,
        LawStatus status,
        String currentStageLabel,
        LocalDate lastActDate,
        LocalDate nextScheduledDate,
        LocalDate promulgationDate,
        List<String> categories
) {
}
