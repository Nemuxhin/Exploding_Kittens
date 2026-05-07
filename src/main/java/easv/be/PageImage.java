package easv.be;

/**
 * One scanned TIFF page.
 * Active pages can be exported; deleted pages stay known but inactive.
 */
public class PageImage {

    private final String caseId;
    private final String documentId;
    private final String fileId;
    private final int pageNumber;
    private final String tiffFileName;
    private boolean active;

    public PageImage(String caseId, String documentId, String fileId, int pageNumber, String tiffFileName) {
        this.caseId = caseId;
        this.documentId = documentId;
        this.fileId = fileId;
        this.pageNumber = pageNumber;
        this.tiffFileName = tiffFileName;
        this.active = true;
    }

    public void markDeleted() {
        active = false;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getFileId() {
        return fileId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getTiffFileName() {
        return tiffFileName;
    }

    public boolean isActive() {
        return active;
    }
}
