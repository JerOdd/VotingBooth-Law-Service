package com.votingbooth.law.core.dataaccess.repository;

import com.votingbooth.law.core.model.entity.LawVoteLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LawVoteLinkRepository extends JpaRepository<LawVoteLink, UUID> {

    List<LawVoteLink> findByLawId(UUID lawId);

}
