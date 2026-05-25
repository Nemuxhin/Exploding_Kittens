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
    private final String profileName;
    private final List<Document> importedDocuments = new ArrayList<>();
    private final List<PageImage> pendingPages = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();
    private String selectedBarcodeBehavior = "";
    private String barcodePageBehavior = "";
    private boolean barcodeSplittingEnabled = true;
    private String lastStatus = "READY";
    private int nextReferenceId = 1;
    private int nextImportedItemNumber = 1;

    public ScanSession(Box box, String profileName) {
        this(UUID.randomUUID(), Instant.now(), box, profileName);
    }

    public ScanSession(UUID id, Instant startedAt, Box box, String profileName) {
        this.id = Objects.requireNonNull(id, "id");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.box = Objects.requireNonNull(box, "box");
        this.profileName = profileName == null ? "" : profileName.trim();
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

    public String getProfileName() {
        return profileName;
    }

    public List<Document> getImportedDocuments() {
        return List.copyOf(importedDocuments);
    }

    public List<String> getFailures() {
        return List.copyOf(failures);
    }

    public List<PageImage> getPendingPages() {
        return List.copyOf(pendingPages);
    }

    public String getSelectedBarcodeBehavior() {
        return selectedBarcodeBehavior;
    }

    public String getBarcodePageBehavior() {
        return barcodePageBehavior;
    }

    public boolean isBarcodeSplittingEnabled() {
        return barcodeSplittingEnabled;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setSelectedBarcodeBehavior(String selectedBarcodeBehavior) {
        this.selectedBarcodeBehavior = selectedBarcodeBehavior == null ? "" : selectedBarcodeBehavior.trim();
    }

    public void setBarcodePageBehavior(String barcodePageBehavior) {
        this.barcodePageBehavior = barcodePageBehavior == null ? "" : barcodePageBehavior.trim();
    }

    public void setBarcodeSplittingEnabled(boolean barcodeSplittingEnabled) {
        this.barcodeSplittingEnabled = barcodeSplittingEnabled;
    }

    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus == null || lastStatus.isBlank() ? "READY" : lastStatus.trim();
    }

    public void seedNextReferenceId(int nextReferenceId) {
        this.nextReferenceId = Math.max(1, nextReferenceId);
    }

    public void seedNextImportedItemNumber(int nextImportedItemNumber) {
        this.nextImportedItemNumber = Math.max(1, nextImportedItemNumber);
    }

    public int allocateReferenceId() {
        return nextReferenceId++;
    }

    public int allocateImportedItemNumber() {
        return nextImportedItemNumber++;
    }

    public void addImportedDocument(Document document) {
        Objects.requireNonNull(document, "document");
        boolean exists = importedDocuments.stream()
                .anyMatch(existing -> existing.getSourceItemId().equals(document.getSourceItemId()));
        if (!exists) {
            importedDocuments.add(document);
        }
    }

    public void replacePendingPages(List<PageImage> pages) {
        pendingPages.clear();
        if (pages != null) {
            pendingPages.addAll(pages);
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
