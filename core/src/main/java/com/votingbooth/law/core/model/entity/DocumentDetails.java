package com.votingbooth.law.core.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;

// Everything the job reads from a document file, replaced as a whole when the file changes.
@Embeddable
public record DocumentDetails(
        @Column(name = "dossier_external_id", nullable = false)
        String dossierExternalId,

        @Enumerated(EnumType.STRING)
        @Column(name = "chamber", nullable = false)
        Chamber chamber,

        @Column(name = "legislature", nullable = true)
        Integer legislature,

        @Column(name = "type_code", nullable = false)
        String typeCode,

        @Column(name = "type_label", nullable = false)
        String typeLabel,

        @Column(name = "subtype_code", nullable = true)
        String subtypeCode,

        @Column(name = "subtype_label", nullable = true)
        String subtypeLabel,

        @Column(name = "title", nullable = false)
        String title,

        @Column(name = "short_title", nullable = true)
        String shortTitle,

        @Column(name = "creation_date", nullable = true)
        LocalDate creationDate,

        @Column(name = "deposit_date", nullable = true)
        LocalDate depositDate,

        @Column(name = "publication_date", nullable = true)
        LocalDate publicationDate,

        @Column(name = "notice_number", nullable = true)
        String noticeNumber,

        @Column(name = "page_count", nullable = true)
        Integer pageCount,

        @JdbcTypeCode(SqlTypes.ARRAY)
        @Column(name = "author_refs", nullable = false)
        List<String> authorRefs
) {
}
