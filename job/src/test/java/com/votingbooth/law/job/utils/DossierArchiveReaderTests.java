package com.votingbooth.law.job.utils;

import com.votingbooth.law.job.model.dossierlegislatif.ArchiveEntity;
import com.votingbooth.law.job.model.dossierlegislatif.ArchiveEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DossierArchiveReaderTests {

    @TempDir
    Path tempDir;

    private final DossierArchiveReader reader = new DossierArchiveReader();

    @Test
    void readsOneKindOfEntry() {
        Path archive = DossierFixtures.archive(tempDir.resolve("archive.zip"));
        List<ArchiveEntry> dossiers = new ArrayList<>();
        List<ArchiveEntry> documents = new ArrayList<>();

        reader.forEach(archive, ArchiveEntity.DOSSIER, dossiers::add);
        reader.forEach(archive, ArchiveEntity.DOCUMENT, documents::add);

        assertEquals(DossierFixtures.DOSSIERS, dossiers.stream().map(ArchiveEntry::externalId).toList());
        assertEquals(DossierFixtures.DOCUMENTS, documents.stream().map(ArchiveEntry::externalId).toList());
        assertArrayEquals(DossierFixtures.dossier("DLR5L17N51852"), dossiers.get(5).content());
    }

}
