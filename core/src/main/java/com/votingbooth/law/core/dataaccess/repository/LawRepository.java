package com.votingbooth.law.core.dataaccess.repository;

import com.votingbooth.law.core.model.entity.Law;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // A law still in progress whose last step is older than stalledBefore, with nothing scheduled, shows as STALLED.
    // CAST(... AS VARCHAR) lets Postgres type the optional filters when they're null.
    @Query(value = """
            SELECT * FROM (
                SELECT law.id, law.external_id, law.title, law.short_title, law.legislature, law.initiator_type,
                       CASE WHEN law.status IN ('DEPOSITED', 'IN_COMMITTEE', 'IN_PLENARY', 'IN_SENATE', 'CMP', 'NEW_READING')
                                 AND law.next_scheduled_date IS NULL
                                 AND (law.last_act_date IS NULL OR law.last_act_date < :stalledBefore)
                            THEN 'STALLED' ELSE law.status END AS status,
                       law.current_stage_label, law.last_act_date, law.next_scheduled_date, law.promulgation_date,
                       (SELECT string_agg(category.code, ',' ORDER BY category.code)
                        FROM law_category JOIN category ON category.id = law_category.category_id
                        WHERE law_category.law_id = law.id) AS categories
                FROM law
                WHERE law.is_law
                  AND (CAST(:category AS VARCHAR) IS NULL OR EXISTS (
                      SELECT 1 FROM law_category JOIN category ON category.id = law_category.category_id
                      WHERE law_category.law_id = law.id AND category.code = :category))
            ) summary
            WHERE (CAST(:status AS VARCHAR) IS NULL OR summary.status = :status)
            ORDER BY summary.last_act_date DESC NULLS LAST, summary.external_id
            """,
            countQuery = """
            SELECT COUNT(*) FROM (
                SELECT CASE WHEN law.status IN ('DEPOSITED', 'IN_COMMITTEE', 'IN_PLENARY', 'IN_SENATE', 'CMP', 'NEW_READING')
                                 AND law.next_scheduled_date IS NULL
                                 AND (law.last_act_date IS NULL OR law.last_act_date < :stalledBefore)
                            THEN 'STALLED' ELSE law.status END AS status
                FROM law
                WHERE law.is_law
                  AND (CAST(:category AS VARCHAR) IS NULL OR EXISTS (
                      SELECT 1 FROM law_category JOIN category ON category.id = law_category.category_id
                      WHERE law_category.law_id = law.id AND category.code = :category))
            ) summary
            WHERE (CAST(:status AS VARCHAR) IS NULL OR summary.status = :status)
            """,
            nativeQuery = true)
    Page<LawSummary> findSummaries(@Param("status") String status, @Param("category") String category,
                                   @Param("stalledBefore") LocalDate stalledBefore, Pageable pageable);

    interface LawSummary {
        UUID getId();
        String getExternalId();
        String getTitle();
        String getShortTitle();
        Integer getLegislature();
        String getInitiatorType();
        String getStatus();
        String getCurrentStageLabel();
        LocalDate getLastActDate();
        LocalDate getNextScheduledDate();
        LocalDate getPromulgationDate();
        String getCategories(); // comma-separated category codes, null if none
    }

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
