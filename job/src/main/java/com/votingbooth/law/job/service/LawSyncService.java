package com.votingbooth.law.job.service;

import com.votingbooth.law.core.dataaccess.repository.LawDocumentRepository;
import com.votingbooth.law.core.dataaccess.repository.LawRepository;
import com.votingbooth.law.core.dataaccess.repository.RawRecordRepository;
import com.votingbooth.law.core.dataaccess.repository.SyncRunRepository;
import com.votingbooth.law.core.model.entity.RawRecord;
import com.votingbooth.law.core.model.entity.SyncRun;
import com.votingbooth.law.core.model.entity.SyncTrigger;
import com.votingbooth.law.job.dataaccess.client.assembleenationale.DossierArchiveClient;
import com.votingbooth.law.job.dataaccess.client.storage.RawStorageClient;
import com.votingbooth.law.job.model.dossierlegislatif.ArchiveDownload;
import com.votingbooth.law.job.model.dossierlegislatif.ArchiveEntity;
import com.votingbooth.law.job.model.dossierlegislatif.ArchiveEntry;
import com.votingbooth.law.job.model.sync.SyncCounts;
import com.votingbooth.law.job.utils.DossierArchiveReader;
import com.votingbooth.law.job.utils.DossierParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// Nightly sync of the Assemblée nationale's "Dossiers législatifs" archive: dossiers first, then the documents that
// reference them. Each file is hashed: an unchanged file is skipped, a changed one is stored raw in S3 then upserted.
@Service
public class LawSyncService {

    private static final Logger log = LoggerFactory.getLogger(LawSyncService.class);

    static final String SOURCE = "assemblee-nationale";
    // Acts are dated in French local time, so "today" (what is past, what is scheduled) is the Paris date.
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final int MARK_SEEN_BATCH = 1000;

    private final DossierArchiveClient archiveClient;
    private final RawStorageClient rawStorageClient;
    private final DossierArchiveReader archiveReader;
    private final DossierParser parser;
    private final DossierSyncService dossierSyncService;
    private final DocumentSyncService documentSyncService;
    private final LawRepository lawRepository;
    private final LawDocumentRepository lawDocumentRepository;
    private final SyncRunRepository syncRunRepository;
    private final RawRecordRepository rawRecordRepository;
    private final Clock clock;
    private final String archiveUrl;
    private final boolean requireVersioning;
    private final int staleArchiveAttempts;
    private final Duration staleArchiveRetryDelay;

    public LawSyncService(DossierArchiveClient archiveClient, RawStorageClient rawStorageClient,
                          DossierArchiveReader archiveReader, DossierParser parser,
                          DossierSyncService dossierSyncService, DocumentSyncService documentSyncService,
                          LawRepository lawRepository, LawDocumentRepository lawDocumentRepository,
                          SyncRunRepository syncRunRepository, RawRecordRepository rawRecordRepository, Clock clock,
                          @Value("${assemblee-nationale.dossiers-archive-url}") String archiveUrl,
                          @Value("${storage.require-versioning:true}") boolean requireVersioning,
                          @Value("${assemblee-nationale.stale-archive-attempts:3}") int staleArchiveAttempts,
                          @Value("${assemblee-nationale.stale-archive-retry-delay:PT1M}") Duration staleArchiveRetryDelay) {
        this.archiveClient = archiveClient;
        this.rawStorageClient = rawStorageClient;
        this.archiveReader = archiveReader;
        this.parser = parser;
        this.dossierSyncService = dossierSyncService;
        this.documentSyncService = documentSyncService;
        this.lawRepository = lawRepository;
        this.lawDocumentRepository = lawDocumentRepository;
        this.syncRunRepository = syncRunRepository;
        this.rawRecordRepository = rawRecordRepository;
        this.clock = clock;
        this.archiveUrl = archiveUrl;
        this.requireVersioning = requireVersioning;
        this.staleArchiveAttempts = staleArchiveAttempts;
        this.staleArchiveRetryDelay = staleArchiveRetryDelay;
    }

    // force ignores every stored hash, so all records are processed again (e.g. after a mapping change).
    public SyncRun sync(SyncTrigger trigger, boolean force) {
        if (requireVersioning && !rawStorageClient.isVersioningEnabled()) {
            throw new IllegalStateException("Bucket versioning is off, so raw files would be overwritten. "
                    + "Enable it on the bucket, or set storage.require-versioning=false for a local run.");
        }
        SyncRun run = syncRunRepository.save(new SyncRun(SOURCE, trigger, archiveUrl, now()));
        log.info("Sync run {} started for {} (force: {})", run.getId(), archiveUrl, force);

        Path archive = null;
        try {
            Optional<LocalDateTime> latestApplied = syncRunRepository.findLatestAppliedArchive(SOURCE, archiveUrl);
            Optional<ArchiveDownload> download = downloadNotOlderThan(latestApplied.orElse(null));
            if (download.isEmpty()) {
                String reason = "every download was older than the archive of " + latestApplied.get() + " UTC already applied";
                run.skip(reason, now());
                log.warn("Sync run {} skipped: {}", run.getId(), reason);
                return syncRunRepository.save(run);
            }
            archive = download.get().file();
            run.recordArchive(sha256(archive), download.get().lastModified());
            LocalDate today = LocalDate.now(clock.withZone(PARIS));
            SyncCounts counts = new SyncCounts();

            // Unchanged dossiers are re-derived when a scheduled step has passed: it now counts as done.
            Set<String> scheduledPassed = new HashSet<>(lawRepository.findExternalIdsScheduledUntil(SOURCE, today));
            syncEntity(archive, ArchiveEntity.DOSSIER, run, force, scheduledPassed::contains, counts, entry ->
                    dossierSyncService.sync(SOURCE, parser.parseDossier(entry.content()), today, now()));
            syncEntity(archive, ArchiveEntity.DOCUMENT, run, force, externalId -> false, counts, entry ->
                    documentSyncService.sync(SOURCE, parser.parseDocument(entry.content()), now()));
            lawDocumentRepository.linkToLaws(SOURCE);
            lawRepository.fillShortTitles(SOURCE);

            run.succeed(counts.getCreated(), counts.getUpdated(), counts.getUnchanged(), counts.getFailed(), now());
            log.info("Sync run {} succeeded: {} created, {} updated, {} unchanged, {} failed", run.getId(),
                    counts.getCreated(), counts.getUpdated(), counts.getUnchanged(), counts.getFailed());
            return syncRunRepository.save(run);
        } catch (RuntimeException e) {
            run.fail(e.toString(), now());
            syncRunRepository.save(run);
            log.error("Sync run {} failed", run.getId(), e);
            throw e;
        } finally {
            deleteQuietly(archive);
        }
    }

    // The Assemblée's servers don't all hold the latest archive: a download can be hours older than the previous one.
    // Applying it would roll laws back (e.g. a promulgated law back to adopted), so it's retried, then the run is skipped.
    private Optional<ArchiveDownload> downloadNotOlderThan(LocalDateTime latestApplied) {
        for (int attempt = 1; attempt <= staleArchiveAttempts; attempt++) {
            ArchiveDownload download = archiveClient.download(archiveUrl);
            if (latestApplied == null || download.lastModified() == null || !download.lastModified().isBefore(latestApplied)) {
                return Optional.of(download);
            }
            log.warn("Downloaded archive of {} UTC is older than the one of {} UTC already applied (attempt {}/{})",
                    download.lastModified(), latestApplied, attempt, staleArchiveAttempts);
            deleteQuietly(download.file());
            if (attempt < staleArchiveAttempts) {
                pause();
            }
        }
        return Optional.empty();
    }

    private void pause() {
        try {
            Thread.sleep(staleArchiveRetryDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to download the archive again", e);
        }
    }

    private void syncEntity(Path archive, ArchiveEntity entity, SyncRun run, boolean force, Predicate<String> refresh,
                            SyncCounts counts, RecordSync recordSync) {
        Map<String, String> knownHashes = rawRecordRepository.findBySourceAndEntity(SOURCE, entity.directory()).stream()
                .collect(Collectors.toMap(RawRecord::getExternalId, RawRecord::getSha256));
        List<String> unchanged = new ArrayList<>();
        archiveReader.forEach(archive, entity, entry -> {
            String sha256 = sha256(entry.content());
            boolean sameContent = sha256.equals(knownHashes.get(entry.externalId()));
            if (sameContent && !force && !refresh.test(entry.externalId())) {
                unchanged.add(entry.externalId());
                return;
            }
            apply(entry, sha256, sameContent, run, counts, recordSync);
        });
        markSeen(entity, run, unchanged, counts);
    }

    // Parses and syncs the tables, stores the raw file if it changed, then records the hash. A failure leaves the old
    // hash, so the record is retried next run. An invalid file is never stored, so it doesn't add an S3 version every night.
    // A storage error isn't about the record: it propagates and fails the run, instead of failing every record one by one.
    private void apply(ArchiveEntry entry, String sha256, boolean sameContent, SyncRun run, SyncCounts counts,
                       RecordSync recordSync) {
        String key = SOURCE + "/" + entry.entity().storageFolder() + "/" + entry.externalId() + ".json";
        boolean created;
        try {
            created = recordSync.sync(entry);
        } catch (RuntimeException e) {
            counts.failed();
            log.warn("Could not sync {} {}: {}", entry.entity().directory(), entry.externalId(), e.toString());
            return;
        }
        if (!sameContent) {
            String versionId = rawStorageClient.put(key, entry.content());
            rawRecordRepository.upsert(SOURCE, entry.entity().directory(), entry.externalId(), sha256, key, versionId,
                    run.getId(), now());
        } else {
            rawRecordRepository.markSeen(SOURCE, entry.entity().directory(), run.getId(), List.of(entry.externalId()));
        }
        if (created) {
            counts.created();
        } else {
            counts.updated();
        }
    }

    private void markSeen(ArchiveEntity entity, SyncRun run, List<String> unchanged, SyncCounts counts) {
        for (int from = 0; from < unchanged.size(); from += MARK_SEEN_BATCH) {
            List<String> batch = unchanged.subList(from, Math.min(from + MARK_SEEN_BATCH, unchanged.size()));
            rawRecordRepository.markSeen(SOURCE, entity.directory(), run.getId(), batch);
        }
        counts.unchanged(unchanged.size());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static String sha256(byte[] content) {
        return HexFormat.of().formatHex(digest().digest(content));
    }

    private static String sha256(Path file) {
        MessageDigest digest = digest();
        try (InputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
            in.transferTo(OutputStream.nullOutputStream());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot hash " + file, e);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete temporary archive {}", file);
        }
    }

    @FunctionalInterface
    private interface RecordSync {
        // Returns true if the record was created, false if it was updated.
        boolean sync(ArchiveEntry entry);
    }

}
