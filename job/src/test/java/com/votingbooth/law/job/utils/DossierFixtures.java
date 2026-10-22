package com.votingbooth.law.job.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// Real files from the L17 Dossiers législatifs archive (22/10/2026), in src/test/resources/dossiers-legislatifs/<directory>/<uid>.json.
public final class DossierFixtures {

    public static final List<String> DOSSIERS = List.of(
            "DLR5L16N48747", // L16 law promulgated in 2026, voted by a L16 scrutin
            "DLR5L17N50172", // rejected by the Senate
            "DLR5L17N50417", // deposited only
            "DLR5L17N50686", // new reading
            "DLR5L17N51702", // joint committee (CMP)
            "DLR5L17N51852", // promulgated after CMP and Conseil constitutionnel, two scrutins
            "DLR5L17N52408", // censure motion on a 49.3: not a law
            "DLR5L17N53230", // government bill deposited at the Senate
            "DLR5L17N53660", // withdrawn by its author
            "DLR5L17N54096", // adopted without change by the Assemblée after the Senate
            "DLR5L17N54248", // in committee
            "DLR5L17N54557", // debated in plenary, no decision yet
            "DLR5L17N54566", // absorbed into another dossier
            "DLR5L17N54900"  // in committee, with a sitting scheduled on 2026-10-28
    );
    public static final List<String> DOCUMENTS = List.of(
            "PIONANR5L17B1240",   // initial text of DLR5L17N51852
            "PIONANR5L17B3170",   // initial text of DLR5L17N54900
            "PRJLSNR5S479B0161",  // Senate text: no legislature
            "PIONANR5L17BTC2806"  // malformed: only "metadatas"
    );

    private DossierFixtures() {
    }

    public static byte[] dossier(String uid) {
        return bytes("dossierParlementaire/" + uid + ".json");
    }

    public static byte[] document(String uid) {
        return bytes("document/" + uid + ".json");
    }

    public static byte[] bytes(String path) {
        try (InputStream in = DossierFixtures.class.getResourceAsStream("/dossiers-legislatifs/" + path)) {
            if (in == null) {
                throw new IllegalArgumentException("No fixture " + path);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // A mini archive with the same layout as the real one.
    public static Path archive(Path file) {
        try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
            for (String uid : DOSSIERS) {
                zip.putNextEntry(new ZipEntry("json/dossierParlementaire/" + uid + ".json"));
                zip.write(dossier(uid));
                zip.closeEntry();
            }
            for (String uid : DOCUMENTS) {
                zip.putNextEntry(new ZipEntry("json/document/" + uid + ".json"));
                zip.write(document(uid));
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return file;
    }

}
