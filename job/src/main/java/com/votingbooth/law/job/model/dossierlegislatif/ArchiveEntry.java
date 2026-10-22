package com.votingbooth.law.job.model.dossierlegislatif;

// One file of the archive, kept as raw bytes so it can be hashed and stored before (or without) being parsed.
public record ArchiveEntry(ArchiveEntity entity, String externalId, byte[] content) {
}
