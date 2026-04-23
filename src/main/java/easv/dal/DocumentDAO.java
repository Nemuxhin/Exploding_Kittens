package easv.dal;

import easv.be.Document;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class DocumentDAO {
    private final Map<String, Document> documentsBySourceItemId = new LinkedHashMap<>();

    public Document saveOrGetExisting(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("document must not be null");
        }
        return documentsBySourceItemId.computeIfAbsent(document.getSourceItemId(), key -> document);
    }

    public Optional<Document> findBySourceItemId(String sourceItemId) {
        if (sourceItemId == null || sourceItemId.isBlank()) {
            throw new IllegalArgumentException("sourceItemId must not be blank");
        }
        return Optional.ofNullable(documentsBySourceItemId.get(sourceItemId));
    }

    public Collection<Document> findAll() {
        return documentsBySourceItemId.values();
    }
}
