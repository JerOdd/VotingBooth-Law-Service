package com.votingbooth.law.job.model.sync;

import com.votingbooth.law.job.model.dossierlegislatif.Acte;

import java.time.LocalDate;

// An act of the dossier's tree, flattened in tree order. phaseCode is its top-level ancestor (AN1, SN1, CMP, CC, PROM...).
// parentPosition is null for a phase itself.
public record DossierStep(Acte acte, int position, int depth, Integer parentPosition, String phaseCode, LocalDate date) {

    public String code() {
        return acte.codeActe();
    }

    public boolean isPast(LocalDate today) {
        return date != null && !date.isAfter(today);
    }

    public boolean isScheduled(LocalDate today) {
        return date != null && date.isAfter(today);
    }
}
