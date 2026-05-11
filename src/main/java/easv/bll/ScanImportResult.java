package easv.bll;

import easv.be.Document;

import java.util.List;

public class ScanImportResult {
    public enum Status {
        IMPORTED,
        STOPPED_ON_BARCODE,
        FETCH_FAILED,
        NO_MORE_FILES
    }

    private final Status status;
    private final List<Document> importedDocuments;
    private final String message;

    private ScanImportResult(Status status, List<Document> importedDocuments, String message) {
        this.status = status;
        this.importedDocuments = importedDocuments == null ? List.of() : List.copyOf(importedDocuments);
        this.message = message == null ? "" : message.trim();
    }

    public static ScanImportResult imported(List<Document> documents) {
        return new ScanImportResult(Status.IMPORTED, documents, "");
    }

    public static ScanImportResult stoppedOnBarcode(List<Document> documents, String message) {
        return new ScanImportResult(Status.STOPPED_ON_BARCODE, documents, message);
    }

    public static ScanImportResult failed(String message) {
        return new ScanImportResult(Status.FETCH_FAILED, List.of(), message);
    }

    public static ScanImportResult noMoreFiles() {
        return new ScanImportResult(Status.NO_MORE_FILES, List.of(), "");
    }

    public Status getStatus() {
        return status;
    }

    public List<Document> getImportedDocuments() {
        return importedDocuments;
    }

    public String getMessage() {
        return message;
    }
}
