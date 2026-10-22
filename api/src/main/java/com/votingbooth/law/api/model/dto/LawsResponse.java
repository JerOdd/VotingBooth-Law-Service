package com.votingbooth.law.api.model.dto;

import java.util.List;

public record LawsResponse(List<LawDto> laws) {
}
