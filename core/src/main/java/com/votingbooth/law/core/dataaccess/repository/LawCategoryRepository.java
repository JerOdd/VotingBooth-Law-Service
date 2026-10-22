package com.votingbooth.law.core.dataaccess.repository;

import com.votingbooth.law.core.model.entity.LawCategory;
import com.votingbooth.law.core.model.entity.LawCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LawCategoryRepository extends JpaRepository<LawCategory, LawCategoryId> {

    List<LawCategory> findByIdLawId(UUID lawId);

    @Modifying
    @Query(value = "DELETE FROM law_category WHERE law_id = :lawId AND origin = 'COMMITTEE_RULE'", nativeQuery = true)
    void deleteCommitteeRule(@Param("lawId") UUID lawId);

    // DO NOTHING keeps a category already set by hand or by AI with its own origin.
    @Modifying
    @Query(value = """
            INSERT INTO law_category (law_id, category_id, origin)
            SELECT :lawId, category_id, 'COMMITTEE_RULE' FROM committee_category WHERE organe_ref = :organeRef
            ON CONFLICT (law_id, category_id) DO NOTHING
            """, nativeQuery = true)
    void insertCommitteeRule(@Param("lawId") UUID lawId, @Param("organeRef") String organeRef);

}
