package com.votingbooth.law.core.dataaccess.repository;

import com.votingbooth.law.core.model.entity.RawRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface RawRecordRepository extends JpaRepository<RawRecord, UUID> {

    // Loaded once per entity at the start of a run, so per-record change detection doesn't query per file.
    List<RawRecord> findBySourceAndEntity(String source, String entity);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO raw_record (source, entity, external_id, sha256, s3_key, s3_version_id, last_seen_run_id, updated_at)
            VALUES (:source, :entity, :externalId, :sha256, :s3Key, :s3VersionId, :runId, :updatedAt)
            ON CONFLICT (source, entity, external_id) DO UPDATE
            SET sha256 = EXCLUDED.sha256, s3_key = EXCLUDED.s3_key, s3_version_id = EXCLUDED.s3_version_id,
                last_seen_run_id = EXCLUDED.last_seen_run_id, updated_at = EXCLUDED.updated_at
            """, nativeQuery = true)
    void upsert(@Param("source") String source, @Param("entity") String entity, @Param("externalId") String externalId,
                @Param("sha256") String sha256, @Param("s3Key") String s3Key, @Param("s3VersionId") String s3VersionId,
                @Param("runId") UUID runId, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE raw_record SET last_seen_run_id = :runId
            WHERE source = :source AND entity = :entity AND external_id IN (:externalIds)
            """, nativeQuery = true)
    void markSeen(@Param("source") String source, @Param("entity") String entity, @Param("runId") UUID runId,
                  @Param("externalIds") Collection<String> externalIds);

}
