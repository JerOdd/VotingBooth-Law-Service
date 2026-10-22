package com.votingbooth.law.job.service;

import com.votingbooth.law.core.model.entity.LawStatus;
import com.votingbooth.law.job.model.dossierlegislatif.Acte;
import com.votingbooth.law.job.model.dossierlegislatif.Dossier;
import com.votingbooth.law.job.model.sync.DossierStep;
import com.votingbooth.law.job.utils.DossierSteps;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// Derives where a law stands from its acts. Only acts dated today or earlier count: future acts are scheduled sittings.
@Service
public class LawStatusService {

    // procedureParlementaire codes that produce a law. The others are resolutions, reports, inquiries, 49.3 declarations...
    private static final Set<String> LAW_PROCEDURES = Set.of(
            "1",  // Projet de loi ordinaire
            "2",  // Proposition de loi ordinaire
            "3",  // Projet de loi de finances de l'année
            "4",  // Projet de loi de financement de la sécurité sociale
            "5",  // Projet ou proposition de loi organique
            "6",  // Projet de ratification des traités et conventions
            "7",  // Projet ou proposition de loi constitutionnelle
            "21", // Projet de loi de finances rectificative
            "23", // Proposition de loi présentée en application de l'article 11 de la Constitution
            "33"  // Projet de loi relative aux résultats de la gestion et portant approbation des comptes
    );

    // statutConclusion.fam_code values
    private static final Set<String> ADOPTIONS = Set.of(
            "TSORTF01", // adopté(e)
            "TSORTF02", // adopté(e) avec modifications
            "TSORTF03", // adopté(e) sans modification
            "TSORTF05", // modifié(e)
            "TSORTF06", // considéré comme adopté (49.3)
            "TSORTF18"  // adopté(e) dans les conditions de l'article 45, alinéa 3 (CMP text)
    );
    private static final Set<String> REJECTIONS = Set.of(
            "TSORTF07", // rejeté(e)
            "TSORTF24", // considéré comme rejeté (49.3)
            "TMRC01"    // rejet du texte par la commission préalable
    );
    private static final String ADOPTED_WITHOUT_CHANGE = "TSORTF03";

    // Acts that only record the text's arrival in a reading: deposit, referral to committees, accelerated procedure,
    // impact study... A reading that has only these hasn't started yet.
    private static final List<String> PROCEDURAL_SUFFIXES =
            List.of("-DEPOT", "-SAISIE", "-PROCACC", "-ETI", "-ACIN", "-AVCE", "-DPTLETTRECT");

    private final int currentLegislature;

    public LawStatusService(@Value("${assemblee-nationale.legislature}") int currentLegislature) {
        this.currentLegislature = currentLegislature;
    }

    public static boolean isLaw(String procedureCode) {
        return LAW_PROCEDURES.contains(procedureCode);
    }

    public LawStatus status(Dossier dossier, List<DossierStep> steps, LocalDate today) {
        List<DossierStep> past = steps.stream()
                .filter(step -> step.isPast(today))
                .sorted(DossierSteps.CHRONOLOGICAL)
                .toList();
        if (past.stream().anyMatch(step -> step.code().equals("PROM-PUB"))) {
            return LawStatus.PROMULGATED;
        }
        if (past.stream().anyMatch(step -> step.code().endsWith("-RTRINI"))) {
            return LawStatus.WITHDRAWN;
        }
        if (past.isEmpty()) {
            return LawStatus.DEPOSITED;
        }

        Optional<DossierStep> decision = lastDecision(past);
        if (decision.isPresent() && isCurrent(decision.get(), past)) {
            String famCode = decision.get().acte().statutConclusion().famCode();
            if (REJECTIONS.contains(famCode)) {
                return LawStatus.REJECTED;
            }
            if (isFinalAdoption(decision.get(), past)) {
                return LawStatus.ADOPTED;
            }
        }

        DossierStep last = past.getLast();
        LawStatus status = byPhase(last.phaseCode(), past);
        if (isLapsed(dossier, status, steps)) {
            return LawStatus.LAPSED;
        }
        return status;
    }

    private static Optional<DossierStep> lastDecision(List<DossierStep> past) {
        return past.stream()
                .filter(step -> step.code().endsWith("-DEC") && step.acte().statutConclusion() != null)
                .reduce((first, second) -> second);
    }

    // A decision still describes the law if the procedure hasn't moved to another phase since (e.g. the Senate after
    // the Assemblée's vote). Sittings of the same reading after a rejection don't revive the text.
    private static boolean isCurrent(DossierStep decision, List<DossierStep> past) {
        return past.subList(past.indexOf(decision) + 1, past.size()).stream()
                .allMatch(step -> step.phaseCode().equals(decision.phaseCode()));
    }

    // The text is adopted in identical terms by both chambers: the second one adopts it without change, both adopt
    // the joint committee's text, or the Assemblée has the final say (lecture définitive).
    private static boolean isFinalAdoption(DossierStep decision, List<DossierStep> past) {
        String famCode = decision.acte().statutConclusion().famCode();
        if (!ADOPTIONS.contains(famCode)) {
            return false;
        }
        if (famCode.equals(ADOPTED_WITHOUT_CHANGE) || decision.phaseCode().equals("ANLDEF")) {
            return true;
        }
        return decision.phaseCode().equals("CMP")
                && isAdopted(past, "CMP-DEBATS-AN-DEC")
                && isAdopted(past, "CMP-DEBATS-SN-DEC");
    }

    private static boolean isAdopted(List<DossierStep> past, String code) {
        return past.stream().anyMatch(step -> step.code().equals(code)
                && step.acte().statutConclusion() != null
                && ADOPTIONS.contains(step.acte().statutConclusion().famCode()));
    }

    private static LawStatus byPhase(String phaseCode, List<DossierStep> past) {
        if (phaseCode.equals("CC")) {
            return LawStatus.CONSTITUTIONAL_COUNCIL;
        }
        if (phaseCode.equals("CMP")) {
            return LawStatus.CMP;
        }
        if (phaseCode.equals("ANNLEC") || phaseCode.equals("SNNLEC") || phaseCode.equals("ANLDEF")) {
            return LawStatus.NEW_READING;
        }
        if (phaseCode.startsWith("SN")) {
            return LawStatus.IN_SENATE;
        }
        // An Assemblée reading (AN1, AN2, ANLUNI...): where its latest real activity happened.
        Optional<Acte> activity = past.stream()
                .filter(step -> step.phaseCode().equals(phaseCode) && step.depth() > 0)
                .map(DossierStep::acte)
                .filter(acte -> PROCEDURAL_SUFFIXES.stream().noneMatch(acte.codeActe()::endsWith))
                .reduce((first, second) -> second);
        if (activity.isEmpty()) {
            return LawStatus.DEPOSITED;
        }
        return activity.get().codeActe().contains("-COM") ? LawStatus.IN_COMMITTEE : LawStatus.IN_PLENARY;
    }

    // Texts still pending at the Assemblée lapse when its legislature ends, unless the Senate has already seized them.
    private boolean isLapsed(Dossier dossier, LawStatus status, List<DossierStep> steps) {
        boolean pendingAtAssemblee = status == LawStatus.DEPOSITED || status == LawStatus.IN_COMMITTEE
                || status == LawStatus.IN_PLENARY;
        return pendingAtAssemblee
                && dossier.legislature() < currentLegislature
                && steps.stream().noneMatch(step -> step.phaseCode().startsWith("SN"));
    }

}
