package easv.bll;

import easv.be.Document;
import easv.be.PageImage;

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
    private final List<PageImage> scannedPages;
    private final String message;

    private ScanImportResult(Status status, List<Document> importedDocuments, List<PageImage> scannedPages, String message) {
        this.status = status;
        this.importedDocuments = importedDocuments == null ? List.of() : List.copyOf(importedDocuments);
        this.scannedPages = scannedPages == null ? List.of() : List.copyOf(scannedPages);
        this.message = message == null ? "" : message.trim();
    }

    public static ScanImportResult imported(List<Document> documents, List<PageImage> scannedPages) {
        return new ScanImportResult(Status.IMPORTED, documents, scannedPages, "");
    }

    public static ScanImportResult stoppedOnBarcode(List<Document> documents, List<PageImage> scannedPages, String message) {
        return new ScanImportResult(Status.STOPPED_ON_BARCODE, documents, scannedPages, message);
    }

    public static ScanImportResult failed(String message) {
        return new ScanImportResult(Status.FETCH_FAILED, List.of(), List.of(), message);
    }

    public static ScanImportResult noMoreFiles() {
        return new ScanImportResult(Status.NO_MORE_FILES, List.of(), List.of(), "");
    }

    public Status getStatus() {
        return status;
    }

    public List<Document> getImportedDocuments() {
        return importedDocuments;
    }

    public List<PageImage> getScannedPages() {
        return scannedPages;
    }

    public String getMessage() {
        return message;
    }
}
