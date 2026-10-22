package com.votingbooth.law.core.dataaccess.repository;

import com.votingbooth.law.core.model.entity.LawDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LawDocumentRepository extends JpaRepository<LawDocument, UUID> {

    Optional<LawDocument> findBySourceAndExternalId(String source, String externalId);

    // A document can be synced while its dossier is missing (e.g. the dossier file failed); this attaches it once the dossier exists.
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE law_document SET law_id = law.id
            FROM law
            WHERE law_document.source = :source AND law_document.law_id IS NULL
              AND law.source = law_document.source AND law.external_id = law_document.dossier_external_id
            """, nativeQuery = true)
    int linkToLaws(@Param("source") String source);

}
