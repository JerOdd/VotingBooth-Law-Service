package com.votingbooth.law.job.service;

import com.votingbooth.law.core.dataaccess.repository.LawCategoryRepository;
import com.votingbooth.law.core.dataaccess.repository.LawRepository;
import com.votingbooth.law.core.dataaccess.repository.LawStepRepository;
import com.votingbooth.law.core.dataaccess.repository.LawVoteLinkRepository;
import com.votingbooth.law.core.model.entity.InitiatorType;
import com.votingbooth.law.core.model.entity.Law;
import com.votingbooth.law.core.model.entity.LawDetails;
import com.votingbooth.law.core.model.entity.LawStatus;
import com.votingbooth.law.core.model.entity.LawStep;
import com.votingbooth.law.core.model.entity.LawVoteLink;
import com.votingbooth.law.core.model.entity.VoteLinkKind;
import com.votingbooth.law.job.model.dossierlegislatif.Dossier;
import com.votingbooth.law.job.utils.DossierFixtures;
import com.votingbooth.law.job.utils.DossierParser;
import com.votingbooth.law.job.utils.DossierSteps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DossierSyncServiceTests {

    private static final String SOURCE = "assemblee-nationale";
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 22, 1, 30);

    private final DossierParser parser = new DossierParser();
    private LawRepository lawRepository;
    private LawStepRepository lawStepRepository;
    private LawVoteLinkRepository lawVoteLinkRepository;
    private LawCategoryRepository lawCategoryRepository;
    private DossierSyncService dossierSyncService;

    @BeforeEach
    void setUp() {
        lawRepository = mock(LawRepository.class);
        lawStepRepository = mock(LawStepRepository.class);
        lawVoteLinkRepository = mock(LawVoteLinkRepository.class);
        lawCategoryRepository = mock(LawCategoryRepository.class);
        dossierSyncService = new DossierSyncService(lawRepository, lawStepRepository, lawVoteLinkRepository,
                lawCategoryRepository, new LawStatusService(17));
        when(lawRepository.findBySourceAndExternalId(any(), any())).thenReturn(Optional.empty());
        when(lawRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(lawStepRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));
    }

    @Test
    void promulgatedLaw() {
        LawDetails details = details("DLR5L17N51852");

        assertTrue(details.isLaw());
        assertEquals(LawStatus.PROMULGATED, details.status());
        assertEquals(InitiatorType.DEPUTY, details.initiatorType());
        assertEquals("PIONANR5L17B1240", details.initialTextRef());
        assertEquals(List.of("PA841017"), details.initiatorRefs());
        assertEquals("2025-1129", details.lawNumber());
        assertEquals(LocalDate.of(2025, 11, 26), details.promulgationDate());
        assertEquals(LocalDate.of(2025, 11, 27), details.joDate());
        assertEquals("ATDX2512888L", details.nor());
        assertEquals(LocalDate.of(2025, 4, 1), details.firstActDate());
        assertEquals(LocalDate.of(2025, 11, 26), details.lastActDate());
        assertEquals("PROM-PUB", details.currentStageCode());
        assertNull(details.nextScheduledDate());
        assertEquals("PO419610", details.leadCommitteeRef());
        assertEquals("https://www.assemblee-nationale.fr/dyn/17/dossiers/simplification_droit_urbanisme_logement_17e", details.anUrl());
        assertEquals("http://www.senat.fr/dossier-legislatif/ppl24-632.html", details.senatUrl());
    }

    @Test
    void scheduledSitting() {
        LawDetails details = details("DLR5L17N54900");

        assertEquals(LocalDate.of(2026, 10, 21), details.lastActDate());
        assertEquals(LocalDate.of(2026, 10, 28), details.nextScheduledDate());
        // The committee met and adopted its report the same day: the report comes after the meeting in the tree
        assertEquals("AN1-COM-FOND-RAPPORT", details.currentStageCode());
    }

    @Test
    void governmentBillAtTheSenate() {
        LawDetails details = details("DLR5L17N53230");

        assertEquals(InitiatorType.GOVERNMENT, details.initiatorType());
        assertEquals("PRJLSNR5S479B0161", details.initialTextRef());
        assertNull(details.leadCommitteeRef());
        // No title slug for the Assemblée page: the uid stands in
        assertEquals("https://www.assemblee-nationale.fr/dyn/17/dossiers/DLR5L17N53230", details.anUrl());
    }

    @Test
    void notALaw() {
        LawDetails details = details("DLR5L17N52408");

        assertFalse(details.isLaw());
        assertNull(details.status());
        assertEquals(List.of(), details.initiatorRefs());
    }

    @Test
    void mergedDossier() {
        LawDetails details = details("DLR5L17N54566");

        assertEquals("DLR5L17N54608", details.mergedIntoExternalId());
        assertEquals("Dossier absorbé", details.mergeCause());
    }

    @Test
    void newLawWithStepsVotesAndCategory() {
        Dossier dossier = parser.parseDossier(DossierFixtures.dossier("DLR5L17N51852"));

        assertTrue(dossierSyncService.sync(SOURCE, dossier, TODAY, NOW));

        ArgumentCaptor<LawStep> steps = ArgumentCaptor.forClass(LawStep.class);
        verify(lawStepRepository, times(42)).save(steps.capture());
        LawStep phase = steps.getAllValues().getFirst();
        LawStep deposit = steps.getAllValues().get(1);
        assertNull(phase.getParentStepId());
        assertEquals(phase.getId(), deposit.getParentStepId());

        ArgumentCaptor<LawVoteLink> links = ArgumentCaptor.forClass(LawVoteLink.class);
        verify(lawVoteLinkRepository, times(2)).save(links.capture());
        LawVoteLink firstReading = links.getAllValues().get(0);
        LawVoteLink jointCommitteeText = links.getAllValues().get(1);
        assertEquals("VTANR5L17V1717", firstReading.getScrutinExternalId());
        assertFalse(firstReading.isDecisive());
        assertEquals("VTANR5L17V3056", jointCommitteeText.getScrutinExternalId());
        assertTrue(jointCommitteeText.isDecisive());
        assertEquals(VoteLinkKind.TEXT, jointCommitteeText.getKind());
        assertEquals(SOURCE, jointCommitteeText.getSource());

        verify(lawCategoryRepository).insertCommitteeRule(any(), eq("PO419610"));
        verify(lawStepRepository, never()).deleteByLawId(any());
    }

    @Test
    void censureMotionIsNeverDecisive() {
        Dossier dossier = parser.parseDossier(DossierFixtures.dossier("DLR5L17N52408"));

        dossierSyncService.sync(SOURCE, dossier, TODAY, NOW);

        ArgumentCaptor<LawVoteLink> links = ArgumentCaptor.forClass(LawVoteLink.class);
        verify(lawVoteLinkRepository).save(links.capture());
        assertEquals(VoteLinkKind.MOTION, links.getValue().getKind());
        assertFalse(links.getValue().isDecisive());
        verify(lawCategoryRepository, never()).insertCommitteeRule(any(), any());
    }

    @Test
    void existingLawIsRewritten() {
        Dossier dossier = parser.parseDossier(DossierFixtures.dossier("DLR5L17N54900"));
        Law existing = withId(new Law("FR", SOURCE, "DLR5L17N54900", details("DLR5L17N50417"), NOW.minusDays(1)));
        when(lawRepository.findBySourceAndExternalId(SOURCE, "DLR5L17N54900")).thenReturn(Optional.of(existing));

        assertFalse(dossierSyncService.sync(SOURCE, dossier, TODAY, NOW));

        assertEquals(LocalDate.of(2026, 10, 28), existing.getDetails().nextScheduledDate());
        assertEquals(NOW, existing.getUpdatedAt());
        verify(lawStepRepository).deleteByLawId(existing.getId());
        verify(lawRepository, never()).save(any());
    }

    private LawDetails details(String uid) {
        Dossier dossier = parser.parseDossier(DossierFixtures.dossier(uid));
        return dossierSyncService.details(dossier, DossierSteps.flatten(dossier), TODAY);
    }

    // Stands in for the UUID Hibernate generates on persist.
    private static <T> T withId(T entity) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, UUID.randomUUID());
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

}
