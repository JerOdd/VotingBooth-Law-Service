package com.votingbooth.law.job.service;

import com.votingbooth.law.core.model.entity.SyncTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

// Long-running mode (law.sync.schedule.enabled=true): syncs on the cron, in Paris time. Without it, the job runs once and exits.
@Service
@ConditionalOnProperty(name = "law.sync.schedule.enabled", havingValue = "true")
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final LawSyncService lawSyncService;

    public SyncScheduler(LawSyncService lawSyncService) {
        this.lawSyncService = lawSyncService;
    }

    // A failed run is already recorded on sync_run; the scheduler keeps going and retries on the next tick.
    @Scheduled(cron = "${law.sync.cron}", zone = "${law.sync.zone}")
    public void sync() {
        try {
            lawSyncService.sync(SyncTrigger.SCHEDULED, false);
        } catch (RuntimeException e) {
            log.error("Scheduled sync failed", e);
        }
    }

}
