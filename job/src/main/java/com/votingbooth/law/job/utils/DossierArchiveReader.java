package com.votingbooth.law.job.utils;

import com.votingbooth.law.job.model.dossierlegislatif.ArchiveEntity;
import com.votingbooth.law.job.model.dossierlegislatif.ArchiveEntry;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Component
public class DossierArchiveReader {

    private static final String EXTENSION = ".json";

    // Reads one kind of entry (json/<directory>/<uid>.json) straight from the zip, one file at a time, without extracting it.
    // Called once per kind so the sync can process dossiers before the documents that reference them.
    public void forEach(Path archive, ArchiveEntity entity, Consumer<ArchiveEntry> consumer) {
        String prefix = "json/" + entity.directory() + "/";
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            for (ZipEntry entry : Collections.list(zip.entries())) {
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(prefix) || !name.endsWith(EXTENSION)) {
                    continue;
                }
                String externalId = name.substring(prefix.length(), name.length() - EXTENSION.length());
                if (externalId.contains("/")) {
                    continue;
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    consumer.accept(new ArchiveEntry(entity, externalId, in.readAllBytes()));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read archive " + archive, e);
        }
    }

}
