package com.votingbooth.law.job.utils;

import com.votingbooth.law.job.model.dossierlegislatif.Acte;
import com.votingbooth.law.job.model.dossierlegislatif.Document;
import com.votingbooth.law.job.model.dossierlegislatif.Dossier;
import com.votingbooth.law.job.model.sync.DossierStep;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DossierParserTests {

    private final DossierParser parser = new DossierParser();

    @Test
    void promulgatedDossier() {
        Dossier dossier = parser.parseDossier(DossierFixtures.dossier("DLR5L17N51852"));

        assertEquals("DLR5L17N51852", dossier.uid());
        assertEquals(17, dossier.legislature());
        assertEquals("2", dossier.procedureParlementaire().code());
        assertEquals("simplification_droit_urbanisme_logement_17e", dossier.titreDossier().titreChemin());
        assertEquals("http://www.senat.fr/dossier-legislatif/ppl24-632.html", dossier.titreDossier().senatChemin());
        // A single initiator is a plain object in the file
        assertEquals(List.of("PA841017"), dossier.initiateur().acteurs().acteur().stream().map(Dossier.ActeurRef::acteurRef).toList());

        List<DossierStep> steps = DossierSteps.flatten(dossier);
        assertEquals(42, steps.size());
        Acte promulgation = steps.stream().map(DossierStep::acte).filter(a -> a.codeActe().equals("PROM-PUB")).findFirst().orElseThrow();
        assertEquals("2025-1129", promulgation.codeLoi());
        assertEquals("ATDX2512888L", promulgation.infoJO().referenceNOR());
        assertEquals(LocalDate.of(2025, 11, 27), SourceDates.parse(promulgation.infoJO().dateJO()));
        // A single voteRef is a plain string in the file
        List<String> votes = steps.stream().flatMap(step -> step.acte().votes().stream()).toList();
        assertEquals(List.of("VTANR5L17V1717", "VTANR5L17V3056"), votes);
    }

    @Test
    void stepsKeepTreeOrderAndPhase() {
        List<DossierStep> steps = DossierSteps.flatten(parser.parseDossier(DossierFixtures.dossier("DLR5L17N54900")));

        DossierStep phase = steps.getFirst();
        assertEquals("AN1", phase.code());
        assertNull(phase.parentPosition());
        assertNull(phase.date());
        DossierStep deposit = steps.get(1);
        assertEquals("AN1-DEPOT", deposit.code());
        assertEquals(0, deposit.parentPosition());
        assertEquals(1, deposit.depth());
        assertTrue(steps.stream().allMatch(step -> step.phaseCode().equals("AN1")));
        assertTrue(steps.stream().anyMatch(step -> LocalDate.of(2026, 10, 28).equals(step.date())));
    }

    @Test
    void organeInitiatorAndNilValues() {
        Dossier dossier = parser.parseDossier(DossierFixtures.dossier("DLR5L17N54096"));

        assertEquals("PO78718", dossier.initiateur().organes().organe().getFirst().organeRef().uid());
        assertNull(dossier.fusionDossier());
    }

    @Test
    void dossierWithoutInitiator() {
        Dossier dossier = parser.parseDossier(DossierFixtures.dossier("DLR5L17N52408"));

        assertNull(dossier.initiateur());
        assertEquals("13", dossier.procedureParlementaire().code());
    }

    @Test
    void mergedDossier() {
        Dossier dossier = parser.parseDossier(DossierFixtures.dossier("DLR5L17N54566"));

        assertEquals("DLR5L17N54608", dossier.fusionDossier().dossierAbsorbantRef());
        assertEquals("Dossier absorbé", dossier.fusionDossier().cause());
    }

    @Test
    void document() {
        Document document = parser.parseDocument(DossierFixtures.document("PIONANR5L17B1240"));

        assertEquals("DLR5L17N51852", document.dossierRef());
        assertEquals(17, document.legislature());
        assertEquals("PION", document.classification().type().code());
        assertEquals("Simplification du droit de l'urbanisme et du logement", document.titres().titrePrincipalCourt());
        assertEquals("1240", document.notice().numNotice());
        assertNull(document.imprimerie().nbPage());
        assertFalse(document.authors().isEmpty());
    }

    @Test
    void senateDocumentHasNoLegislature() {
        Document document = parser.parseDocument(DossierFixtures.document("PRJLSNR5S479B0161"));

        assertNull(document.legislature());
        assertEquals("DLR5L17N53230", document.dossierRef());
    }

    @Test
    void malformedDocument() {
        assertThrows(IllegalArgumentException.class, () -> parser.parseDocument(DossierFixtures.document("PIONANR5L17BTC2806")));
    }

    @Test
    void notADossier() {
        byte[] json = "{\"document\": {}}".getBytes(StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class, () -> parser.parseDossier(json));
    }

}
