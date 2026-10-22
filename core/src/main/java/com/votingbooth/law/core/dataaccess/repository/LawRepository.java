package com.votingbooth.law.core.dataaccess.repository;

import com.votingbooth.law.core.model.entity.Law;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LawRepository extends JpaRepository<Law, UUID> {
}
