package com.votingbooth.law.job.model.dossierlegislatif;

// The kinds of file in the Dossiers législatifs archive. The name is the archive directory, reused for raw_record.entity.
// The S3 folder is a plain English name, as agreed for the bucket layout.
public enum ArchiveEntity {
    DOSSIER("dossierParlementaire", "dossiers"),
    DOCUMENT("document", "documents");

    private final String directory;
    private final String storageFolder;

    ArchiveEntity(String directory, String storageFolder) {
        this.directory = directory;
        this.storageFolder = storageFolder;
    }

    public String directory() {
        return directory;
    }

    public String storageFolder() {
        return storageFolder;
    }
}
