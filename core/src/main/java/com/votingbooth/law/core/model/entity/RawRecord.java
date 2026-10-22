package com.votingbooth.law.core.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

// Written only through RawRecordRepository.upsert.
@Entity
@Table(name = "raw_record")
public class RawRecord {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "entity", nullable = false)
    private String entity;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "sha256", nullable = false)
    private String sha256;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "s3_version_id", nullable = true)
    private String s3VersionId;

    @Column(name = "last_seen_run_id", nullable = false)
    private UUID lastSeenRunId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected RawRecord() {
    }

    public UUID getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getEntity() {
        return entity;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getSha256() {
        return sha256;
    }

    public String getS3Key() {
        return s3Key;
    }

    public String getS3VersionId() {
        return s3VersionId;
    }

    public UUID getLastSeenRunId() {
        return lastSeenRunId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

}
