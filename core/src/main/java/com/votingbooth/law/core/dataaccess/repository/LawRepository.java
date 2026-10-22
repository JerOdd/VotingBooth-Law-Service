package com.votingbooth.law.core.dataaccess.repository;

import com.votingbooth.law.core.model.entity.Law;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LawRepository extends JpaRepository<Law, UUID> {

    Optional<Law> findBySourceAndExternalId(String source, String externalId);

    @Query(value = "SELECT id FROM law WHERE source = :source AND external_id = :externalId", nativeQuery = true)
    Optional<UUID> findIdBySourceAndExternalId(@Param("source") String source, @Param("externalId") String externalId);

    // Dossiers whose next scheduled step has passed: their status and dates must be re-derived even if the file is unchanged.
    @Query(value = """
            SELECT external_id FROM law
            WHERE source = :source AND next_scheduled_date <= :today
            """, nativeQuery = true)
    List<String> findExternalIdsScheduledUntil(@Param("source") String source, @Param("today") LocalDate today);

    // The short title is the initial text's titrePrincipalCourt, known only once that document is synced.
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE law SET short_title = document.short_title
            FROM law_document document
            WHERE law.source = :source AND document.source = law.source AND document.external_id = law.initial_text_ref
              AND law.short_title IS DISTINCT FROM document.short_title
            """, nativeQuery = true)
    int fillShortTitles(@Param("source") String source);

}
