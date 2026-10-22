package com.votingbooth.law.job.service;

import com.votingbooth.law.core.model.entity.LawStatus;
import com.votingbooth.law.job.model.dossierlegislatif.Dossier;
import com.votingbooth.law.job.utils.DossierFixtures;
import com.votingbooth.law.job.utils.DossierParser;
import com.votingbooth.law.job.utils.DossierSteps;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class LawStatusServiceTests {

    // The day the fixtures were extracted: DLR5L17N54900 has a sitting scheduled after it.
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

    private final DossierParser parser = new DossierParser();
    private final LawStatusService lawStatusService = new LawStatusService(17);

    @Test
    void deposited() {
        assertEquals(LawStatus.DEPOSITED, status("DLR5L17N50417"));
    }

    @Test
    void inCommittee() {
        assertEquals(LawStatus.IN_COMMITTEE, status("DLR5L17N54248"));
    }

    @Test
    void inPlenary() {
        assertEquals(LawStatus.IN_PLENARY, status("DLR5L17N54557"));
    }

    @Test
    void scheduledSittingDoesNotCountYet() {
        assertEquals(LawStatus.IN_COMMITTEE, status("DLR5L17N54900"));
    }

    @Test
    void scheduledSittingCountsOnceItHasHappened() {
        assertEquals(LawStatus.IN_PLENARY, status("DLR5L17N54900", TODAY.plusDays(7), lawStatusService));
    }

    @Test
    void inSenate() {
        assertEquals(LawStatus.IN_SENATE, status("DLR5L17N53230"));
    }

    @Test
    void jointCommittee() {
        assertEquals(LawStatus.CMP, status("DLR5L17N51702"));
    }

    @Test
    void newReading() {
        assertEquals(LawStatus.NEW_READING, status("DLR5L17N50686"));
    }

    @Test
    void adoptedWithoutChange() {
        assertEquals(LawStatus.ADOPTED, status("DLR5L17N54096"));
    }

    @Test
    void promulgated() {
        assertEquals(LawStatus.PROMULGATED, status("DLR5L17N51852"));
        assertEquals(LawStatus.PROMULGATED, status("DLR5L16N48747"));
    }

    @Test
    void rejected() {
        assertEquals(LawStatus.REJECTED, status("DLR5L17N50172"));
    }

    @Test
    void withdrawn() {
        assertEquals(LawStatus.WITHDRAWN, status("DLR5L17N53660"));
    }

    @Test
    void lapsedWhenTheLegislatureEnds() {
        LawStatusService nextLegislature = new LawStatusService(18);

        assertEquals(LawStatus.LAPSED, status("DLR5L17N50417", TODAY, nextLegislature));
        // Already at the Senate: it survives the end of the legislature
        assertEquals(LawStatus.IN_SENATE, status("DLR5L17N53230", TODAY, nextLegislature));
    }

    @Test
    void lawProcedures() {
        assertTrue(LawStatusService.isLaw("2"));
        assertFalse(LawStatusService.isLaw("13"));
    }

    private LawStatus status(String uid) {
        return status(uid, TODAY, lawStatusService);
    }

    private LawStatus status(String uid, LocalDate today, LawStatusService service) {
        Dossier dossier = parser.parseDossier(DossierFixtures.dossier(uid));
        return service.status(dossier, DossierSteps.flatten(dossier), today);
    }

}
