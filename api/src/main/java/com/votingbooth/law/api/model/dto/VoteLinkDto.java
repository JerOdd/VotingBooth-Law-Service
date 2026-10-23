package com.votingbooth.law.api.model.dto;

import com.votingbooth.law.core.model.entity.VoteLinkKind;

import java.util.UUID;

// A scrutin a law was voted in. decisive marks the Assemblée's last vote on the whole text; a law passed by 49.3 has none,
// only MOTION links (the censure motions).
public record VoteLinkDto(UUID lawId, String source, String scrutinExternalId, VoteLinkKind kind, boolean decisive) {
}
