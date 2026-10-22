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
@Table(name = "law_document")
public class LawDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "law_id", nullable = true)
    private UUID lawId;

    @Embedded
    private DocumentDetails details;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected LawDocument() {
    }

    public LawDocument(String source, String externalId, UUID lawId, DocumentDetails details, LocalDateTime now) {
        this.source = source;
        this.externalId = externalId;
        this.lawId = lawId;
        this.details = details;
        this.firstSeenAt = now;
        this.updatedAt = now;
    }

    public void update(UUID lawId, DocumentDetails details, LocalDateTime now) {
        this.lawId = lawId;
        this.details = details;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getExternalId() {
        return externalId;
    }

    public UUID getLawId() {
        return lawId;
    }

    public DocumentDetails getDetails() {
        return details;
    }

    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

}
