package com.votingbooth.law.job.model.dossierlegislatif;

import java.util.List;

// A json/document/<uid>.json file: the metadata of a text (bill, report, opinion...). The full text isn't in the archive.
public record Document(
        String uid,
        Integer legislature,
        String dossierRef,
        CycleDeVie cycleDeVie,
        Titres titres,
        Classification classification,
        Auteurs auteurs,
        Notice notice,
        Imprimerie imprimerie
) {

    public List<Auteur> authors() {
        return auteurs == null || auteurs.auteur() == null ? List.of() : auteurs.auteur();
    }

    public record CycleDeVie(Chrono chrono) {
    }

    public record Chrono(String dateCreation, String dateDepot, String datePublication) {
    }

    public record Titres(String titrePrincipal, String titrePrincipalCourt) {
    }

    public record Classification(CodeLibelle type, CodeLibelle sousType) {
    }

    public record CodeLibelle(String code, String libelle) {
    }

    public record Auteurs(List<Auteur> auteur) {
    }

    public record Auteur(ActeurAuteur acteur, OrganeAuteur organe) {
    }

    public record ActeurAuteur(String acteurRef, String qualite) {
    }

    public record OrganeAuteur(String organeRef) {
    }

    public record Notice(String numNotice) {
    }

    public record Imprimerie(Integer nbPage) {
    }
}
