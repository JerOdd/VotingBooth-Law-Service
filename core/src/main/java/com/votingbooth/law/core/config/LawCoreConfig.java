package com.votingbooth.law.core.config;

import com.votingbooth.law.core.dataaccess.repository.LawRepository;
import com.votingbooth.law.core.model.entity.Law;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// The applications live in com.votingbooth.law.api/job, so Spring Boot's default scanning doesn't reach law-core.
@Configuration
@EntityScan(basePackageClasses = Law.class)
@EnableJpaRepositories(basePackageClasses = LawRepository.class)
public class LawCoreConfig {
}
