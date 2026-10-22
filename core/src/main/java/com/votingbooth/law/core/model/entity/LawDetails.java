package com.votingbooth.law.core.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;

// Everything the job derives from a dossier, replaced as a whole when the dossier changes.
@Embeddable
public record LawDetails(
        @Column(name = "legislature", nullable = false)
        int legislature,

        @Column(name = "title", nullable = false)
        String title,

        @Column(name = "procedure_code", nullable = false)
        String procedureCode,

        @Column(name = "procedure_label", nullable = false)
        String procedureLabel,

        @Column(name = "is_law", nullable = false)
        boolean isLaw,

        @Enumerated(EnumType.STRING)
        @Column(name = "initiator_type", nullable = false)
        InitiatorType initiatorType,

        @JdbcTypeCode(SqlTypes.ARRAY)
        @Column(name = "initiator_refs", nullable = false)
        List<String> initiatorRefs,

        @Column(name = "initial_text_ref", nullable = true)
        String initialTextRef,

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = true)
        LawStatus status,

        @Column(name = "current_phase_code", nullable = true)
        String currentPhaseCode,

        @Column(name = "current_stage_code", nullable = true)
        String currentStageCode,

        @Column(name = "current_stage_label", nullable = true)
        String currentStageLabel,

        @Column(name = "first_act_date", nullable = true)
        LocalDate firstActDate,

        @Column(name = "last_act_date", nullable = true)
        LocalDate lastActDate,

        @Column(name = "next_scheduled_date", nullable = true)
        LocalDate nextScheduledDate,

        @Column(name = "promulgation_date", nullable = true)
        LocalDate promulgationDate,

        @Column(name = "law_number", nullable = true)
        String lawNumber,

        @Column(name = "law_title", nullable = true)
        String lawTitle,

        @Column(name = "jo_date", nullable = true)
        LocalDate joDate,

        @Column(name = "jo_number", nullable = true)
        String joNumber,

        @Column(name = "nor", nullable = true)
        String nor,

        @Column(name = "legifrance_url", nullable = true)
        String legifranceUrl,

        @Column(name = "an_url", nullable = false)
        String anUrl,

        @Column(name = "senat_url", nullable = true)
        String senatUrl,

        @Column(name = "lead_committee_ref", nullable = true)
        String leadCommitteeRef,

        @Column(name = "merged_into_external_id", nullable = true)
        String mergedIntoExternalId,

        @Column(name = "merge_cause", nullable = true)
        String mergeCause
) {
}
