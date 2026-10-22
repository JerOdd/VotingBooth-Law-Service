package com.votingbooth.law.core.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "law")
public class Law {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "jurisdiction_code", nullable = false)
    private String jurisdictionCode;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Embedded
    private LawDetails details;

    // Comes from the initial text's document, so it's set by SQL once documents are synced, never from the dossier.
    @Column(name = "short_title", nullable = true, insertable = false, updatable = false)
    private String shortTitle;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Law() {
    }

    public Law(String jurisdictionCode, String source, String externalId, LawDetails details, LocalDateTime now) {
        this.jurisdictionCode = jurisdictionCode;
        this.source = source;
        this.externalId = externalId;
        this.details = details;
        this.firstSeenAt = now;
        this.updatedAt = now;
    }

    public void update(LawDetails details, LocalDateTime now) {
        this.details = details;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getJurisdictionCode() {
        return jurisdictionCode;
    }

    public String getSource() {
        return source;
    }

    public String getExternalId() {
        return externalId;
    }

    public LawDetails getDetails() {
        return details;
    }

    public String getShortTitle() {
        return shortTitle;
    }

    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

}
