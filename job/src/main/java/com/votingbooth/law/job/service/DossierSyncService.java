package com.votingbooth.law.job.service;

import com.votingbooth.law.core.dataaccess.repository.LawCategoryRepository;
import com.votingbooth.law.core.dataaccess.repository.LawRepository;
import com.votingbooth.law.core.dataaccess.repository.LawStepRepository;
import com.votingbooth.law.core.dataaccess.repository.LawVoteLinkRepository;
import com.votingbooth.law.core.model.entity.InitiatorType;
import com.votingbooth.law.core.model.entity.Law;
import com.votingbooth.law.core.model.entity.LawDetails;
import com.votingbooth.law.core.model.entity.LawStep;
import com.votingbooth.law.core.model.entity.LawVoteLink;
import com.votingbooth.law.core.model.entity.VoteLinkKind;
import com.votingbooth.law.job.model.dossierlegislatif.Acte;
import com.votingbooth.law.job.model.dossierlegislatif.Dossier;
import com.votingbooth.law.job.model.sync.DossierStep;
import com.votingbooth.law.job.utils.DossierSteps;
import com.votingbooth.law.job.utils.SourceDates;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class DossierSyncService {

    static final String JURISDICTION = "FR";
    static final String SENAT_SOURCE = "senat";
    private static final String AN_DOSSIER_URL = "https://www.assemblee-nationale.fr/dyn/%d/dossiers/%s";

    private final LawRepository lawRepository;
    private final LawStepRepository lawStepRepository;
    private final LawVoteLinkRepository lawVoteLinkRepository;
    private final LawCategoryRepository lawCategoryRepository;
    private final LawStatusService lawStatusService;

    public DossierSyncService(LawRepository lawRepository, LawStepRepository lawStepRepository,
                              LawVoteLinkRepository lawVoteLinkRepository, LawCategoryRepository lawCategoryRepository,
                              LawStatusService lawStatusService) {
        this.lawRepository = lawRepository;
        this.lawStepRepository = lawStepRepository;
        this.lawVoteLinkRepository = lawVoteLinkRepository;
        this.lawCategoryRepository = lawCategoryRepository;
        this.lawStatusService = lawStatusService;
    }

    // Returns true if the law is new. Steps and vote links are rewritten whole: a dossier has a few dozen at most.
    @Transactional
    public boolean sync(String source, Dossier dossier, LocalDate today, LocalDateTime now) {
        List<DossierStep> steps = DossierSteps.flatten(dossier);
        LawDetails details = details(dossier, steps, today);

        Optional<Law> existing = lawRepository.findBySourceAndExternalId(source, dossier.uid());
        Law law;
        if (existing.isPresent()) {
            law = existing.get();
            law.update(details, now);
            lawStepRepository.deleteByLawId(law.getId());
        } else {
            law = lawRepository.save(new Law(JURISDICTION, source, dossier.uid(), details, now));
        }
        saveSteps(source, law.getId(), steps, today);

        lawCategoryRepository.deleteCommitteeRule(law.getId());
        if (details.leadCommitteeRef() != null) {
            lawCategoryRepository.insertCommitteeRule(law.getId(), details.leadCommitteeRef());
        }
        return existing.isEmpty();
    }

    LawDetails details(Dossier dossier, List<DossierStep> steps, LocalDate today) {
        boolean isLaw = LawStatusService.isLaw(dossier.procedureParlementaire().code());
        List<DossierStep> past = steps.stream()
                .filter(step -> step.isPast(today))
                .sorted(DossierSteps.CHRONOLOGICAL)
                .toList();
        Optional<DossierStep> current = past.isEmpty() ? Optional.empty() : Optional.of(past.getLast());
        Optional<Acte> promulgation = steps.stream()
                .map(DossierStep::acte)
                .filter(acte -> acte.codeActe().equals("PROM-PUB"))
                .findFirst();
        Optional<Acte.InfoJO> jo = promulgation.map(Acte::infoJO);
        String initialTextRef = steps.stream()
                .map(DossierStep::acte)
                .filter(acte -> acte.codeActe().endsWith("-DEPOT") && acte.texteAssocie() != null)
                .map(Acte::texteAssocie)
                .findFirst()
                .orElse(null);
        Dossier.TitreDossier titre = dossier.titreDossier();
        Dossier.FusionDossier fusion = dossier.fusionDossier();

        return new LawDetails(
                dossier.legislature(),
                titre.titre(),
                dossier.procedureParlementaire().code(),
                dossier.procedureParlementaire().libelle(),
                isLaw,
                initiatorType(initialTextRef),
                initiatorRefs(dossier),
                initialTextRef,
                isLaw ? lawStatusService.status(dossier, steps, today) : null,
                current.map(DossierStep::phaseCode).orElse(null),
                current.map(DossierStep::code).orElse(null),
                current.map(step -> label(step.acte())).orElse(null),
                past.isEmpty() ? null : past.getFirst().date(),
                current.map(DossierStep::date).orElse(null),
                steps.stream().filter(step -> step.isScheduled(today)).map(DossierStep::date).min(LocalDate::compareTo).orElse(null),
                promulgation.map(acte -> SourceDates.parse(acte.dateActe())).orElse(null),
                promulgation.map(Acte::codeLoi).orElse(null),
                promulgation.map(Acte::titreLoi).orElse(null),
                jo.map(info -> SourceDates.parse(info.dateJO())).orElse(null),
                jo.map(Acte.InfoJO::numJO).orElse(null),
                jo.map(Acte.InfoJO::referenceNOR).orElse(null),
                jo.map(Acte.InfoJO::urlLegifrance).orElse(null),
                AN_DOSSIER_URL.formatted(dossier.legislature(), titre.titreChemin() != null ? titre.titreChemin() : dossier.uid()),
                titre.senatChemin(),
                leadCommitteeRef(steps),
                fusion == null ? null : fusion.dossierAbsorbantRef(),
                fusion == null ? null : fusion.cause()
        );
    }

    // From the initial text's uid, e.g. PRJLANR5L17B0123 (government bill) or PIONSNR5L17B0456 (senators' bill).
    // More reliable than the dossier's initiateur, which is missing on many dossiers.
    private static InitiatorType initiatorType(String initialTextRef) {
        if (initialTextRef == null || initialTextRef.length() < 7) {
            return InitiatorType.OTHER;
        }
        String type = initialTextRef.substring(0, 4);
        String chamber = initialTextRef.substring(4, 7);
        if (type.equals("PRJL")) {
            return InitiatorType.GOVERNMENT;
        }
        if (type.equals("PION") || type.equals("PNRE")) {
            return chamber.equals("SNR") ? InitiatorType.SENATOR : InitiatorType.DEPUTY;
        }
        return InitiatorType.OTHER;
    }

    private static List<String> initiatorRefs(Dossier dossier) {
        Dossier.Initiateur initiateur = dossier.initiateur();
        if (initiateur == null) {
            return List.of();
        }
        Stream<String> acteurs = initiateur.acteurs() == null || initiateur.acteurs().acteur() == null
                ? Stream.empty()
                : initiateur.acteurs().acteur().stream().map(Dossier.ActeurRef::acteurRef);
        Stream<String> organes = initiateur.organes() == null || initiateur.organes().organe() == null
                ? Stream.empty()
                : initiateur.organes().organe().stream()
                        .filter(organe -> organe.organeRef() != null)
                        .map(organe -> organe.organeRef().uid());
        return Stream.concat(acteurs, organes).filter(ref -> ref != null).toList();
    }

    // The Assemblée committee that examines the text (commission saisie au fond) in its first reading there.
    private static String leadCommitteeRef(List<DossierStep> steps) {
        return steps.stream()
                .map(DossierStep::acte)
                .filter(acte -> acte.codeActe().startsWith("AN") && acte.codeActe().endsWith("-COM-FOND"))
                .map(Acte::organeRef)
                .filter(ref -> ref != null)
                .findFirst()
                .orElse(null);
    }

    private void saveSteps(String source, UUID lawId, List<DossierStep> steps, LocalDate today) {
        List<UUID> stepIds = new ArrayList<>(steps.size());
        for (DossierStep step : steps) {
            Acte acte = step.acte();
            UUID parentId = step.parentPosition() == null ? null : stepIds.get(step.parentPosition());
            LawStep saved = lawStepRepository.save(new LawStep(
                    lawId,
                    parentId,
                    acte.uid(),
                    step.position(),
                    step.depth(),
                    step.phaseCode(),
                    acte.codeActe(),
                    label(acte),
                    step.date(),
                    step.isScheduled(today),
                    acte.organeRef(),
                    acte.texteAssocie(),
                    acte.texteAdopte(),
                    acte.statutConclusion() == null ? null : acte.statutConclusion().famCode(),
                    acte.statutConclusion() == null ? null : acte.statutConclusion().libelle(),
                    acte.numDecision(),
                    acte.urlConclusion()
            ));
            stepIds.add(saved.getId());
        }
        saveVoteLinks(source, lawId, steps, stepIds);
    }

    // The decisive vote is the Assemblée's last public vote on the whole text. Motions (rejection, censure) never are.
    // A step can hold several scrutins (e.g. a budget voted part by part): the last one is the vote on the whole text.
    private void saveVoteLinks(String source, UUID lawId, List<DossierStep> steps, List<UUID> stepIds) {
        String decisiveRef = steps.stream()
                .filter(step -> step.date() != null && isAssembleeVoteOnText(step))
                .filter(step -> step.acte().votes().stream().anyMatch(ref -> !isSenateScrutin(ref)))
                .max(DossierSteps.CHRONOLOGICAL)
                .flatMap(step -> step.acte().votes().stream().filter(ref -> !isSenateScrutin(ref)).reduce((first, second) -> second))
                .orElse(null);
        Set<String> linked = new HashSet<>();
        for (DossierStep step : steps) {
            for (String ref : step.acte().votes()) {
                if (!linked.add(ref)) {
                    continue;
                }
                lawVoteLinkRepository.save(new LawVoteLink(
                        lawId,
                        stepIds.get(step.position()),
                        isSenateScrutin(ref) ? SENAT_SOURCE : source,
                        ref,
                        step.code().contains("-MOTION") ? VoteLinkKind.MOTION : VoteLinkKind.TEXT,
                        ref.equals(decisiveRef)
                ));
            }
        }
    }

    private static boolean isAssembleeVoteOnText(DossierStep step) {
        String code = step.code();
        return code.endsWith("-DEC") && !code.contains("-MOTION")
                && (step.phaseCode().startsWith("AN") || code.startsWith("CMP-DEBATS-AN"));
    }

    private static boolean isSenateScrutin(String ref) {
        return ref.startsWith("VTSN");
    }

    private static String label(Acte acte) {
        return acte.libelleActe() == null ? null : acte.libelleActe().nomCanonique();
    }

}
