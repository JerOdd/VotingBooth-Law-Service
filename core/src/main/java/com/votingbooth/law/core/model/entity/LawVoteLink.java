package com.votingbooth.law.core.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "law_vote_link")
public class LawVoteLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "law_id", nullable = false)
    private UUID lawId;

    @Column(name = "step_id", nullable = false)
    private UUID stepId;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "scrutin_external_id", nullable = false)
    private String scrutinExternalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private VoteLinkKind kind;

    @Column(name = "is_decisive", nullable = false)
    private boolean decisive;

    protected LawVoteLink() {
    }

    public LawVoteLink(UUID lawId, UUID stepId, String source, String scrutinExternalId, VoteLinkKind kind, boolean decisive) {
        this.lawId = lawId;
        this.stepId = stepId;
        this.source = source;
        this.scrutinExternalId = scrutinExternalId;
        this.kind = kind;
        this.decisive = decisive;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLawId() {
        return lawId;
    }

    public UUID getStepId() {
        return stepId;
    }

    public String getSource() {
        return source;
    }

    public String getScrutinExternalId() {
        return scrutinExternalId;
    }

    public VoteLinkKind getKind() {
        return kind;
    }

    public boolean isDecisive() {
        return decisive;
    }

}
