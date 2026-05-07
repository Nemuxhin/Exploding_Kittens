package easv.be;

import java.util.ArrayList;
import java.util.List;

/**
 * One TIFF file that will be created during export.
 */
public class TiffExportItem {

    private final String documentId;
    private final String fileName;
    private final List<PageImage> pages;

    public TiffExportItem(String documentId, String fileName, List<PageImage> pages) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.pages = new ArrayList<>(pages);
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getFileName() {
        return fileName;
    }

    public List<PageImage> getPages() {
        return new ArrayList<>(pages);
    }
}
