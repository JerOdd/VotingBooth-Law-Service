package com.votingbooth.law.core.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "law_step")
public class LawStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "law_id", nullable = false)
    private UUID lawId;

    @Column(name = "parent_step_id", nullable = true)
    private UUID parentStepId;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "depth", nullable = false)
    private int depth;

    @Column(name = "phase_code", nullable = false)
    private String phaseCode;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "label", nullable = true)
    private String label;

    @Column(name = "act_date", nullable = true)
    private LocalDate actDate;

    @Column(name = "is_scheduled", nullable = false)
    private boolean scheduled;

    @Column(name = "organe_ref", nullable = true)
    private String organeRef;

    @Column(name = "associated_text_ref", nullable = true)
    private String associatedTextRef;

    @Column(name = "adopted_text_ref", nullable = true)
    private String adoptedTextRef;

    @Column(name = "conclusion_code", nullable = true)
    private String conclusionCode;

    @Column(name = "conclusion_label", nullable = true)
    private String conclusionLabel;

    @Column(name = "decision_number", nullable = true)
    private String decisionNumber;

    @Column(name = "decision_url", nullable = true)
    private String decisionUrl;

    protected LawStep() {
    }

    public LawStep(UUID lawId, UUID parentStepId, String externalId, int position, int depth, String phaseCode,
                   String code, String label, LocalDate actDate, boolean scheduled, String organeRef,
                   String associatedTextRef, String adoptedTextRef, String conclusionCode, String conclusionLabel,
                   String decisionNumber, String decisionUrl) {
        this.lawId = lawId;
        this.parentStepId = parentStepId;
        this.externalId = externalId;
        this.position = position;
        this.depth = depth;
        this.phaseCode = phaseCode;
        this.code = code;
        this.label = label;
        this.actDate = actDate;
        this.scheduled = scheduled;
        this.organeRef = organeRef;
        this.associatedTextRef = associatedTextRef;
        this.adoptedTextRef = adoptedTextRef;
        this.conclusionCode = conclusionCode;
        this.conclusionLabel = conclusionLabel;
        this.decisionNumber = decisionNumber;
        this.decisionUrl = decisionUrl;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLawId() {
        return lawId;
    }

    public UUID getParentStepId() {
        return parentStepId;
    }

    public String getExternalId() {
        return externalId;
    }

    public int getPosition() {
        return position;
    }

    public int getDepth() {
        return depth;
    }

    public String getPhaseCode() {
        return phaseCode;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public LocalDate getActDate() {
        return actDate;
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public String getOrganeRef() {
        return organeRef;
    }

    public String getAssociatedTextRef() {
        return associatedTextRef;
    }

    public String getAdoptedTextRef() {
        return adoptedTextRef;
    }

    public String getConclusionCode() {
        return conclusionCode;
    }

    public String getConclusionLabel() {
        return conclusionLabel;
    }

    public String getDecisionNumber() {
        return decisionNumber;
    }

    public String getDecisionUrl() {
        return decisionUrl;
    }

}
