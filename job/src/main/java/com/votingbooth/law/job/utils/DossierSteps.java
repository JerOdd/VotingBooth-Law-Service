package com.votingbooth.law.job.utils;

import com.votingbooth.law.job.model.dossierlegislatif.Acte;
import com.votingbooth.law.job.model.dossierlegislatif.Dossier;
import com.votingbooth.law.job.model.sync.DossierStep;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DossierSteps {

    // Chronological order. Acts of the same day keep their tree order, which is the order they happened in
    // (e.g. a text adopted by the Assemblée and transmitted to the Senate the same day).
    public static final Comparator<DossierStep> CHRONOLOGICAL =
            Comparator.comparing(DossierStep::date).thenComparingInt(DossierStep::position);

    private DossierSteps() {
    }

    public static List<DossierStep> flatten(Dossier dossier) {
        List<DossierStep> steps = new ArrayList<>();
        for (Acte phase : dossier.actes()) {
            add(steps, phase, 0, null, phase.codeActe());
        }
        return steps;
    }

    private static void add(List<DossierStep> steps, Acte acte, int depth, Integer parentPosition, String phaseCode) {
        int position = steps.size();
        steps.add(new DossierStep(acte, position, depth, parentPosition, phaseCode, SourceDates.parse(acte.dateActe())));
        for (Acte child : acte.children()) {
            add(steps, child, depth + 1, position, phaseCode);
        }
    }
}
