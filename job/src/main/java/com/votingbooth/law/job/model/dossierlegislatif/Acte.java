package com.votingbooth.law.job.model.dossierlegislatif;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// One node of a dossier's act tree: a phase (AN1, SN1, CMP...), a step within it, or a sub-step.
public record Acte(
        String uid,
        String codeActe,
        LibelleActe libelleActe,
        String organeRef,
        String dateActe,
        ActesLegislatifs actesLegislatifs,
        String texteAssocie,
        String texteAdopte,
        Statut statutConclusion,
        Statut decision,
        VoteRefs voteRefs,
        String codeLoi,
        String titreLoi,
        InfoJO infoJO,
        String numDecision,
        String urlConclusion
) {

    public List<Acte> children() {
        return actesLegislatifs == null ? List.of() : actesLegislatifs.acteLegislatif();
    }

    public List<String> votes() {
        return voteRefs == null || voteRefs.voteRef() == null ? List.of() : voteRefs.voteRef();
    }

    public record LibelleActe(String nomCanonique, String libelleCourt) {
    }

    public record Statut(@JsonProperty("fam_code") String famCode, String libelle) {
    }

    public record VoteRefs(List<String> voteRef) {
    }

    public record InfoJO(String dateJO, String numJO, String referenceNOR, String urlLegifrance) {
    }
}
