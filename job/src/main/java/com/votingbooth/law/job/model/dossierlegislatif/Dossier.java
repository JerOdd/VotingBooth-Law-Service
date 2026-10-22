package com.votingbooth.law.job.model.dossierlegislatif;

import java.util.List;

// A json/dossierParlementaire/<uid>.json file. Lists are never null once parsed (see DossierParser).
public record Dossier(
        String uid,
        int legislature,
        TitreDossier titreDossier,
        CodeLibelle procedureParlementaire,
        Initiateur initiateur,
        ActesLegislatifs actesLegislatifs,
        FusionDossier fusionDossier
) {

    public List<Acte> actes() {
        return actesLegislatifs == null ? List.of() : actesLegislatifs.acteLegislatif();
    }

    public record TitreDossier(String titre, String titreChemin, String senatChemin) {
    }

    public record CodeLibelle(String code, String libelle) {
    }

    public record Initiateur(Acteurs acteurs, Organes organes) {
    }

    public record Acteurs(List<ActeurRef> acteur) {
    }

    public record ActeurRef(String acteurRef) {
    }

    public record Organes(List<Organe> organe) {
    }

    public record Organe(OrganeRef organeRef) {
    }

    public record OrganeRef(String uid) {
    }

    public record FusionDossier(String cause, String dossierAbsorbantRef) {
    }
}
