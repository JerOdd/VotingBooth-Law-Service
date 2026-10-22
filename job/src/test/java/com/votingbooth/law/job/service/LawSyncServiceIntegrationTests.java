package com.votingbooth.law.job.service;

import com.votingbooth.law.core.dataaccess.repository.LawCategoryRepository;
import com.votingbooth.law.core.dataaccess.repository.LawDocumentRepository;
import com.votingbooth.law.core.dataaccess.repository.LawRepository;
import com.votingbooth.law.core.dataaccess.repository.LawStepRepository;
import com.votingbooth.law.core.dataaccess.repository.LawVoteLinkRepository;
import com.votingbooth.law.core.dataaccess.repository.RawRecordRepository;
import com.votingbooth.law.core.dataaccess.repository.SyncRunRepository;
import com.votingbooth.law.core.model.entity.Chamber;
import com.votingbooth.law.core.model.entity.Law;
import com.votingbooth.law.core.model.entity.LawDocument;
import com.votingbooth.law.core.model.entity.LawStatus;
import com.votingbooth.law.core.model.entity.LawVoteLink;
import com.votingbooth.law.core.model.entity.SyncRun;
import com.votingbooth.law.core.model.entity.SyncStatus;
import com.votingbooth.law.core.model.entity.SyncTrigger;
import com.votingbooth.law.job.dataaccess.client.assembleenationale.DossierArchiveClient;
import com.votingbooth.law.job.dataaccess.client.storage.RawStorageClient;
import com.votingbooth.law.job.model.dossierlegislatif.ArchiveDownload;
import com.votingbooth.law.job.utils.DossierFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// End to end on a mini archive of real files: Flyway schema on a real Postgres, raw storage and download stubbed.
// The scheduler is enabled with a disabled cron ("-") so that the application doesn't run a sync on startup.
@Testcontainers
@SpringBootTest(properties = {
        "law.sync.schedule.enabled=true",
        "law.sync.cron=-",
        "storage.endpoint=http://localhost:1",
        "storage.region=us-east-1",
        "storage.bucket=law-raw",
        "storage.access-key=test",
        "storage.secret-key=test",
        "assemblee-nationale.stale-archive-retry-delay=PT0S"
})
public class LawSyncServiceIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @TempDir
    Path tempDir;

    @MockitoBean
    DossierArchiveClient archiveClient;

    @MockitoBean
    RawStorageClient rawStorageClient;

    @Autowired
    LawSyncService lawSyncService;

    @Autowired
    LawRepository lawRepository;

    @Autowired
    LawStepRepository lawStepRepository;

    @Autowired
    LawVoteLinkRepository lawVoteLinkRepository;

    @Autowired
    LawDocumentRepository lawDocumentRepository;

    @Autowired
    LawCategoryRepository lawCategoryRepository;

    @Autowired
    RawRecordRepository rawRecordRepository;

    @Autowired
    SyncRunRepository syncRunRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    // 2026-10-22 01:30 in Paris, the day the fixtures were extracted.
    private static final Instant FIXTURES_DAY = Instant.parse("2026-10-21T23:30:00Z");
    // Last-Modified of two archive versions served the same day by different Assemblée servers.
    private static final LocalDateTime CURRENT_ARCHIVE = LocalDateTime.of(2026, 10, 22, 10, 16, 32);
    private static final LocalDateTime OLDER_ARCHIVE = LocalDateTime.of(2026, 10, 22, 8, 16, 31);

    @Autowired
    MovableClock clock;

    @TestConfiguration
    static class TestClock {

        @Bean
        @Primary
        MovableClock movableClock() {
            return new MovableClock();
        }
    }

    static class MovableClock extends Clock {

        private Instant instant = FIXTURES_DAY;

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE law, law_document, raw_record, sync_run CASCADE");
        clock.set(FIXTURES_DAY);
        when(rawStorageClient.isVersioningEnabled()).thenReturn(true);
        when(rawStorageClient.put(any(), any())).thenReturn("v1");
    }

    @Test
    void firstRunThenUnchangedRun() {
        Path archive = DossierFixtures.archive(tempDir.resolve("archive.zip"));
        when(archiveClient.download(any())).thenAnswer(invocation -> copy(archive, CURRENT_ARCHIVE));

        SyncRun first = lawSyncService.sync(SyncTrigger.SCHEDULED, false);

        assertEquals(SyncStatus.SUCCEEDED, first.getStatus());
        assertEquals(17, first.getCreated()); // 14 dossiers + 3 valid documents
        assertEquals(0, first.getUpdated());
        assertEquals(0, first.getUnchanged());
        assertEquals(1, first.getFailed()); // PIONANR5L17BTC2806
        assertNotNull(first.getArchiveSha256());
        verify(rawStorageClient).put(eq("assemblee-nationale/dossiers/DLR5L17N51852.json"), any());
        verify(rawStorageClient).put(eq("assemblee-nationale/documents/PIONANR5L17B1240.json"), any());
        verify(rawStorageClient, never()).put(eq("assemblee-nationale/documents/PIONANR5L17BTC2806.json"), any());
        assertEquals(17, rawRecordRepository.count());

        Law promulgated = lawRepository.findBySourceAndExternalId("assemblee-nationale", "DLR5L17N51852").orElseThrow();
        assertEquals(LawStatus.PROMULGATED, promulgated.getDetails().status());
        assertEquals("FR", promulgated.getJurisdictionCode());
        assertEquals("Simplification du droit de l'urbanisme et du logement", promulgated.getShortTitle());
        assertEquals(42, lawStepRepository.findByLawIdOrderByPosition(promulgated.getId()).size());
        List<LawVoteLink> votes = lawVoteLinkRepository.findByLawId(promulgated.getId());
        assertEquals(1, votes.stream().filter(LawVoteLink::isDecisive).count());
        assertEquals(1, lawCategoryRepository.findByIdLawId(promulgated.getId()).size());

        Law scheduled = lawRepository.findBySourceAndExternalId("assemblee-nationale", "DLR5L17N54900").orElseThrow();
        assertEquals(LocalDate.of(2026, 10, 28), scheduled.getDetails().nextScheduledDate());

        LawDocument senateText = lawDocumentRepository.findBySourceAndExternalId("assemblee-nationale", "PRJLSNR5S479B0161").orElseThrow();
        Law senateDossier = lawRepository.findBySourceAndExternalId("assemblee-nationale", "DLR5L17N53230").orElseThrow();
        assertEquals(senateDossier.getId(), senateText.getLawId());
        assertEquals(Chamber.SN, senateText.getDetails().chamber());
        assertEquals(17, senateText.getDetails().legislature());

        SyncRun second = lawSyncService.sync(SyncTrigger.SCHEDULED, false);

        assertEquals(SyncStatus.SUCCEEDED, second.getStatus());
        assertEquals(0, second.getCreated());
        assertEquals(0, second.getUpdated());
        assertEquals(17, second.getUnchanged());
        assertEquals(1, second.getFailed()); // still invalid, retried every run
        verify(rawStorageClient, times(17)).put(any(), any());
        assertEquals(2, syncRunRepository.count());
    }

    @Test
    void changedRecordIsUpdated() throws IOException {
        Path archive = DossierFixtures.archive(tempDir.resolve("archive.zip"));
        when(archiveClient.download(any())).thenAnswer(invocation -> copy(archive, CURRENT_ARCHIVE));
        lawSyncService.sync(SyncTrigger.SCHEDULED, false);

        String changedTitle = new String(DossierFixtures.dossier("DLR5L17N50417"), StandardCharsets.UTF_8)
                .replace("\"titre\": \"", "\"titre\": \"Nouveau titre : ");
        Path changed = tempDir.resolve("changed.zip");
        try (OutputStream out = Files.newOutputStream(changed); ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("json/dossierParlementaire/DLR5L17N50417.json"));
            zip.write(changedTitle.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        when(archiveClient.download(any())).thenAnswer(invocation -> copy(changed, CURRENT_ARCHIVE));

        SyncRun run = lawSyncService.sync(SyncTrigger.MANUAL, false);

        assertEquals(1, run.getUpdated());
        Law law = lawRepository.findBySourceAndExternalId("assemblee-nationale", "DLR5L17N50417").orElseThrow();
        assertTrue(law.getDetails().title().startsWith("Nouveau titre : "));
        assertEquals(5, lawStepRepository.findByLawIdOrderByPosition(law.getId()).size());
    }

    @Test
    void unchangedDossierIsRederivedOnceItsScheduledSittingHasPassed() {
        Path archive = DossierFixtures.archive(tempDir.resolve("archive.zip"));
        when(archiveClient.download(any())).thenAnswer(invocation -> copy(archive, CURRENT_ARCHIVE));
        lawSyncService.sync(SyncTrigger.SCHEDULED, false);

        clock.set(Instant.parse("2026-10-29T00:30:00Z"));
        SyncRun run = lawSyncService.sync(SyncTrigger.SCHEDULED, false);

        assertEquals(1, run.getUpdated());
        assertEquals(16, run.getUnchanged());
        Law law = lawRepository.findBySourceAndExternalId("assemblee-nationale", "DLR5L17N54900").orElseThrow();
        assertEquals(LawStatus.IN_PLENARY, law.getDetails().status());
        // Sittings were scheduled on 2026-10-29
        assertEquals(LocalDate.of(2026, 10, 29), law.getDetails().lastActDate());
        assertNull(law.getDetails().nextScheduledDate());
        // The file itself didn't change: no new raw version
        verify(rawStorageClient, times(17)).put(any(), any());
    }

    @Test
    void olderArchiveIsRetriedThenSkipped() throws IOException {
        Path archive = DossierFixtures.archive(tempDir.resolve("archive.zip"));
        when(archiveClient.download(any())).thenAnswer(invocation -> copy(archive, CURRENT_ARCHIVE));
        SyncRun first = lawSyncService.sync(SyncTrigger.SCHEDULED, false);
        assertEquals(CURRENT_ARCHIVE, first.getArchiveLastModified());

        Path older = emptyArchive("older.zip");
        when(archiveClient.download(any())).thenAnswer(invocation -> copy(older, OLDER_ARCHIVE));
        SyncRun run = lawSyncService.sync(SyncTrigger.SCHEDULED, false);

        assertEquals(SyncStatus.SKIPPED, run.getStatus());
        assertNotNull(run.getError());
        verify(archiveClient, times(1 + 3)).download(any());
        verify(rawStorageClient, times(17)).put(any(), any());
        assertEquals(17, rawRecordRepository.count());
        Law promulgated = lawRepository.findBySourceAndExternalId("assemblee-nationale", "DLR5L17N51852").orElseThrow();
        assertEquals(LawStatus.PROMULGATED, promulgated.getDetails().status());
    }

    @Test
    void olderArchiveIsRetriedUntilTheCurrentOneIsServed() throws IOException {
        Path archive = DossierFixtures.archive(tempDir.resolve("archive.zip"));
        when(archiveClient.download(any())).thenAnswer(invocation -> copy(archive, CURRENT_ARCHIVE));
        lawSyncService.sync(SyncTrigger.SCHEDULED, false);

        Path older = emptyArchive("older.zip");
        when(archiveClient.download(any()))
                .thenAnswer(invocation -> copy(older, OLDER_ARCHIVE))
                .thenAnswer(invocation -> copy(archive, CURRENT_ARCHIVE));
        SyncRun run = lawSyncService.sync(SyncTrigger.SCHEDULED, false);

        assertEquals(SyncStatus.SUCCEEDED, run.getStatus());
        assertEquals(17, run.getUnchanged());
        assertEquals(CURRENT_ARCHIVE, run.getArchiveLastModified());
    }

    @Test
    void forceReprocessesEverything() {
        Path archive = DossierFixtures.archive(tempDir.resolve("archive.zip"));
        when(archiveClient.download(any())).thenAnswer(invocation -> copy(archive, CURRENT_ARCHIVE));
        lawSyncService.sync(SyncTrigger.SCHEDULED, false);

        SyncRun run = lawSyncService.sync(SyncTrigger.MANUAL, true);

        assertEquals(17, run.getUpdated());
        assertEquals(0, run.getUnchanged());
        // Same content: nothing new to store
        verify(rawStorageClient, times(17)).put(any(), any());
    }

    @Test
    void failedDownloadFailsTheRun() {
        when(archiveClient.download(any())).thenThrow(new IllegalStateException("HTTP 503"));

        assertThrows(IllegalStateException.class, () -> lawSyncService.sync(SyncTrigger.SCHEDULED, false));

        SyncRun run = syncRunRepository.findAll().getFirst();
        assertEquals(SyncStatus.FAILED, run.getStatus());
        assertTrue(run.getError().contains("HTTP 503"));
    }

    // Its content doesn't matter: an older archive must never be read.
    private Path emptyArchive(String name) throws IOException {
        Path file = tempDir.resolve(name);
        try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("json/"));
            zip.closeEntry();
        }
        return file;
    }

    // The service deletes the downloaded file, so each download gets its own copy.
    private ArchiveDownload copy(Path archive, LocalDateTime lastModified) throws IOException {
        Path copy = Files.createTempFile(tempDir, "download-", ".zip");
        Files.copy(archive, copy, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return new ArchiveDownload(copy, lastModified);
    }

}
