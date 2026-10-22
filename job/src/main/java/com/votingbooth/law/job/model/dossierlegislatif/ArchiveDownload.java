package com.votingbooth.law.job.model.dossierlegislatif;

import java.nio.file.Path;
import java.time.LocalDateTime;

// A downloaded archive in a temporary file, with its Last-Modified header (UTC, null if the server didn't send one).
public record ArchiveDownload(Path file, LocalDateTime lastModified) {
}
