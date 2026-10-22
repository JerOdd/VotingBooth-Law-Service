package com.votingbooth.law.core.dataaccess.repository;

import com.votingbooth.law.core.model.entity.LawStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LawStepRepository extends JpaRepository<LawStep, UUID> {

    List<LawStep> findByLawIdOrderByPosition(UUID lawId);

    // Also deletes the law's vote links, which cascade from their step.
    @Modifying
    @Query(value = "DELETE FROM law_step WHERE law_id = :lawId", nativeQuery = true)
    void deleteByLawId(@Param("lawId") UUID lawId);

}
