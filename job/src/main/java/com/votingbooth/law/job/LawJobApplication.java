package com.votingbooth.law.job;

import com.votingbooth.law.core.config.LawCoreConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(LawCoreConfig.class)
public class LawJobApplication {

	public static void main(String[] args) {
		SpringApplication.run(LawJobApplication.class, args);
	}

}
