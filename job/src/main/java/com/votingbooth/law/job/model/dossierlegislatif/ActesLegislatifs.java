package com.votingbooth.law.job.model.dossierlegislatif;

import java.util.List;

public record ActesLegislatifs(List<Acte> acteLegislatif) {

    public ActesLegislatifs {
        acteLegislatif = acteLegislatif == null ? List.of() : acteLegislatif;
    }
}
