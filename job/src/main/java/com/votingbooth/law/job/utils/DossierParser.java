package com.votingbooth.law.job.utils;

import com.votingbooth.law.job.model.dossierlegislatif.Document;
import com.votingbooth.law.job.model.dossierlegislatif.Dossier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

@Component
public class DossierParser {

    private final JsonMapper mapper = JsonMapper.builder()
            // The XML→JSON conversion turns a one-element list into a plain object.
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public Dossier parseDossier(byte[] json) {
        Dossier dossier = parse(json, "dossierParlementaire", Dossier.class);
        if (dossier.uid() == null || dossier.legislature() <= 0 || dossier.titreDossier() == null
                || dossier.titreDossier().titre() == null || dossier.procedureParlementaire() == null
                || dossier.procedureParlementaire().code() == null) {
            throw new IllegalArgumentException("Dossier " + dossier.uid() + " lacks its uid, legislature, title or procedure");
        }
        return dossier;
    }

    public Document parseDocument(byte[] json) {
        Document document = parse(json, "document", Document.class);
        if (document.uid() == null || document.dossierRef() == null || document.classification() == null
                || document.classification().type() == null || document.titres() == null
                || document.titres().titrePrincipal() == null) {
            throw new IllegalArgumentException("Document " + document.uid() + " lacks its uid, dossier, type or title");
        }
        return document;
    }

    private <T> T parse(byte[] json, String root, Class<T> type) {
        JsonNode node = mapper.readTree(json).get(root);
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("Not a " + root + " file");
        }
        return mapper.treeToValue(normalize(node), type);
    }

    // Undoes the other XML→JSON quirk before binding: {"@xsi:nil": "true"} is an empty value.
    private JsonNode normalize(JsonNode node) {
        if (node.isArray()) {
            ArrayNode array = mapper.createArrayNode();
            for (JsonNode element : node.values()) {
                array.add(normalize(element));
            }
            return array;
        }
        if (!node.isObject()) {
            return node;
        }
        JsonNode nil = node.get("@xsi:nil");
        if (nil != null && "true".equals(nil.asString())) {
            return NullNode.getInstance();
        }
        ObjectNode object = mapper.createObjectNode();
        for (String name : node.propertyNames()) {
            object.set(name, normalize(node.get(name)));
        }
        return object;
    }

}
