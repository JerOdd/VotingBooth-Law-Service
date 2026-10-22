package com.votingbooth.law.api.model.dto;

import java.util.List;

public record LawsResponse(List<LawSummaryDto> laws, int page, int size, long totalElements, int totalPages) {
}
