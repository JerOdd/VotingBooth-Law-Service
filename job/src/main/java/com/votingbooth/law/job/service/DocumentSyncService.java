package com.votingbooth.law.job.service;

import com.votingbooth.law.core.dataaccess.repository.LawDocumentRepository;
import com.votingbooth.law.core.dataaccess.repository.LawRepository;
import com.votingbooth.law.core.model.entity.Chamber;
import com.votingbooth.law.core.model.entity.DocumentDetails;
import com.votingbooth.law.core.model.entity.Law;
import com.votingbooth.law.core.model.entity.LawDocument;
import com.votingbooth.law.job.model.dossierlegislatif.Document;
import com.votingbooth.law.job.utils.SourceDates;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentSyncService {

    private final LawDocumentRepository lawDocumentRepository;
    private final LawRepository lawRepository;

    public DocumentSyncService(LawDocumentRepository lawDocumentRepository, LawRepository lawRepository) {
        this.lawDocumentRepository = lawDocumentRepository;
        this.lawRepository = lawRepository;
    }

    // Returns true if the document is new.
    @Transactional
    public boolean sync(String source, Document document, LocalDateTime now) {
        Optional<Law> law = lawRepository.findBySourceAndExternalId(source, document.dossierRef());
        DocumentDetails details = details(document, law);
        UUID lawId = law.map(Law::getId).orElse(null);

        Optional<LawDocument> existing = lawDocumentRepository.findBySourceAndExternalId(source, document.uid());
        if (existing.isPresent()) {
            existing.get().update(lawId, details, now);
            return false;
        }
        lawDocumentRepository.save(new LawDocument(source, document.uid(), lawId, details, now));
        return true;
    }

    private static DocumentDetails details(Document document, Optional<Law> law) {
        Document.Chrono chrono = document.cycleDeVie() == null ? null : document.cycleDeVie().chrono();
        Document.CodeLibelle sousType = document.classification().sousType();
        return new DocumentDetails(
                document.dossierRef(),
                chamber(document.uid()),
                // Senate documents carry no legislature: they take their dossier's.
                document.legislature() != null ? document.legislature() : law.map(l -> l.getDetails().legislature()).orElse(null),
                document.classification().type().code(),
                document.classification().type().libelle(),
                sousType == null ? null : sousType.code(),
                sousType == null ? null : sousType.libelle(),
                document.titres().titrePrincipal(),
                document.titres().titrePrincipalCourt(),
                chrono == null ? null : SourceDates.parse(chrono.dateCreation()),
                chrono == null ? null : SourceDates.parse(chrono.dateDepot()),
                chrono == null ? null : SourceDates.parse(chrono.datePublication()),
                document.notice() == null ? null : document.notice().numNotice(),
                document.imprimerie() == null ? null : document.imprimerie().nbPage(),
                authorRefs(document)
        );
    }

    // The uid names the chamber that published the text: PIONANR5L17B0123 (Assemblée) or PIONSNR5L17B0456 (Senate).
    private static Chamber chamber(String uid) {
        return uid.length() >= 7 && uid.startsWith("SNR", 4) ? Chamber.SN : Chamber.AN;
    }

    private static List<String> authorRefs(Document document) {
        return document.authors().stream()
                .map(auteur -> auteur.acteur() != null ? auteur.acteur().acteurRef()
                        : auteur.organe() != null ? auteur.organe().organeRef() : null)
                .filter(Objects::nonNull)
                .toList();
    }

}
