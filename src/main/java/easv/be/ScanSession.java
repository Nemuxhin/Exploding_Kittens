package easv.be;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ScanSession {
    private final UUID id;
    private final Instant startedAt;
    private final Box box;
    private final List<Document> importedDocuments = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();
    private String selectedBarcodeBehavior = "";
    private String lastStatus = "READY";
    private int nextReferenceId = 1;

    public ScanSession(Box box) {
        this(UUID.randomUUID(), Instant.now(), box);
    }

    public ScanSession(UUID id, Instant startedAt, Box box) {
        this.id = Objects.requireNonNull(id, "id");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.box = Objects.requireNonNull(box, "box");
    }

    public UUID getId() {
        return id;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Box getBox() {
        return box;
    }

    public List<Document> getImportedDocuments() {
        return List.copyOf(importedDocuments);
    }

    public List<String> getFailures() {
        return List.copyOf(failures);
    }

    public String getSelectedBarcodeBehavior() {
        return selectedBarcodeBehavior;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setSelectedBarcodeBehavior(String selectedBarcodeBehavior) {
        this.selectedBarcodeBehavior = selectedBarcodeBehavior == null ? "" : selectedBarcodeBehavior.trim();
    }

    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus == null || lastStatus.isBlank() ? "READY" : lastStatus.trim();
    }

    public int allocateReferenceId() {
        return nextReferenceId++;
    }

    public void addImportedDocument(Document document) {
        Objects.requireNonNull(document, "document");
        boolean exists = importedDocuments.stream()
                .anyMatch(existing -> existing.getSourceItemId().equals(document.getSourceItemId()));
        if (!exists) {
            importedDocuments.add(document);
        }
    }

    public void recordFailure(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        failures.add(message);
        lastStatus = "FAILED";
    }
}
