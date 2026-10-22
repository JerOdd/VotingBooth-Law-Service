package com.votingbooth.law.job;

import com.votingbooth.law.core.config.LawCoreConfig;
import com.votingbooth.law.core.model.entity.SyncTrigger;
import com.votingbooth.law.job.service.LawSyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

// By default, runs one sync and exits (Kubernetes CronJob); a failed run exits with a non-zero code.
//   --manual  records the run as MANUAL instead of SCHEDULED
//   --force   reprocesses every record, ignoring the stored hashes (implies --manual)
// With law.sync.schedule.enabled=true it stays up and syncs on law.sync.cron instead (see SyncScheduler).
@SpringBootApplication
@Import(LawCoreConfig.class)
public class LawJobApplication implements ApplicationRunner {

	private final LawSyncService lawSyncService;
	private final boolean scheduled;

	public LawJobApplication(LawSyncService lawSyncService,
							 @Value("${law.sync.schedule.enabled:false}") boolean scheduled) {
		this.lawSyncService = lawSyncService;
		this.scheduled = scheduled;
	}

	public static void main(String[] args) {
		SpringApplication.run(LawJobApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) {
		if (scheduled) {
			return;
		}
		boolean force = args.containsOption("force");
		SyncTrigger trigger = force || args.containsOption("manual") ? SyncTrigger.MANUAL : SyncTrigger.SCHEDULED;
		lawSyncService.sync(trigger, force);
	}

}
