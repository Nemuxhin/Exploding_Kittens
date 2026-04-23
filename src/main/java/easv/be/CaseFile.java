package easv.be;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CaseFile {
    private final UUID id;
    private final String caseReference;
    private final Client client;
    private final Box box;
    private final List<Document> documents = new ArrayList<>();

    public CaseFile(String caseReference, Client client, Box box) {
        this(UUID.randomUUID(), caseReference, client, box);
    }

    public CaseFile(UUID id, String caseReference, Client client, Box box) {
        this.id = Objects.requireNonNull(id, "id");
        this.caseReference = requireText(caseReference, "caseReference");
        this.client = Objects.requireNonNull(client, "client");
        this.box = Objects.requireNonNull(box, "box");
    }

    public UUID getId() {
        return id;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public Client getClient() {
        return client;
    }

    public Box getBox() {
        return box;
    }

    public List<Document> getDocuments() {
        return List.copyOf(documents);
    }

    public void addDocument(Document document) {
        Objects.requireNonNull(document, "document");
        boolean exists = documents.stream()
                .anyMatch(existing -> existing.getSourceItemId().equals(document.getSourceItemId()));
        if (!exists) {
            documents.add(document);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
