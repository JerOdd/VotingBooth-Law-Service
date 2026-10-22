package com.votingbooth.law.job.dataaccess.client.assembleenationale;

import com.votingbooth.law.job.model.dossierlegislatif.ArchiveDownload;

public interface DossierArchiveClient {

    // Downloads the archive to a temporary file, which the caller deletes.
    ArchiveDownload download(String url);

}
