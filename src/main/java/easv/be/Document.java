package easv.be;

import java.util.ArrayList;
import java.util.List;

/**
 * A document belongs to one case and contains scanned pages.
 */
public class Document {

    private final String documentId;
    private final String caseId;
    private final List<PageImage> pages = new ArrayList<>();

    public Document(String documentId, String caseId) {
        this.documentId = documentId;
        this.caseId = caseId;
    }

    public void addPage(PageImage page) {
        pages.add(page);
    }

    public List<PageImage> getActivePages() {
        List<PageImage> activePages = new ArrayList<>();

        for (PageImage page : pages) {
            if (page.isActive()) {
                activePages.add(page);
            }
        }

        return activePages;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getCaseId() {
        return caseId;
    }
}
