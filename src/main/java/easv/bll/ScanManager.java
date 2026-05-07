package easv.bll;

import easv.be.PageImage;
import easv.be.ScanSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class keeps the scan logic separate from the screen.
 * The UI can call these methods from buttons or scanner events.
 */
public class ScanManager {

    private final AuditLogManager auditLogManager;
    private final Map<String, ScanSession> sessionsByCaseId = new HashMap<>();
    private final Map<String, List<PageImage>> pagesByCaseId = new HashMap<>();

    public ScanManager() {
        this(new AuditLogManager());
    }

    public ScanManager(AuditLogManager auditLogManager) {
        this.auditLogManager = auditLogManager;
    }

    public ScanSession startScan(String caseId, String profileName, String boxId) {
        ScanSession scanSession = new ScanSession(clean(caseId), clean(profileName), clean(boxId));
        sessionsByCaseId.put(scanSession.getCaseId(), scanSession);

        auditLogManager.logUserAction(AuditLogManager.SCAN_STARTED, scanSession.getCaseId(), null, null,
                null, scanSession.getProfileName(), scanSession.getBoxId(), "Scan started.");

        return scanSession;
    }

    public PageImage addPage(String caseId, String documentId, String fileId, int pageNumber,
                             String tiffFileName, String profileName, String boxId) {
        PageImage page = new PageImage(clean(caseId), clean(documentId), clean(fileId), pageNumber, clean(tiffFileName));
        pagesByCaseId.computeIfAbsent(page.getCaseId(), key -> new ArrayList<>()).add(page);

        // We log both the scanner event and the page creation event.
        auditLogManager.logUserAction(AuditLogManager.TIFF_FETCHED, page.getCaseId(), page.getDocumentId(),
                page.getFileId(), page.getPageNumber(), profileName, boxId, "TIFF fetched from the scanner.");
        auditLogManager.logPageCreated(page, profileName, boxId);

        return page;
    }

    public boolean deletePage(String caseId, String fileId, int pageNumber, String profileName, String boxId) {
        for (PageImage page : getPages(caseId)) {
            if (page.getFileId().equals(fileId) && page.getPageNumber() == pageNumber && page.isActive()) {
                page.markDeleted();
                auditLogManager.logPageDeleted(page, profileName, boxId);
                return true;
            }
        }

        return false;
    }

    public void scanFailed(String caseId, String profileName, String boxId, String reason) {
        ScanSession scanSession = sessionsByCaseId.get(clean(caseId));

        if (scanSession != null) {
            scanSession.setStatus("FAILED");
        }

        auditLogManager.logSystemAction(AuditLogManager.SCAN_FAILED, clean(caseId), null, null,
                null, profileName, boxId, clean(reason));
    }

    public void retryUsed(String caseId, String profileName, String boxId) {
        auditLogManager.logUserAction(AuditLogManager.RETRY_USED, clean(caseId), null, null,
                null, profileName, boxId, "The user retried the scan.");
    }

    public void completeScan(String caseId, String profileName, String boxId) {
        ScanSession scanSession = sessionsByCaseId.get(clean(caseId));

        if (scanSession != null) {
            scanSession.setStatus("COMPLETED");
        }

        auditLogManager.logUserAction(AuditLogManager.SCAN_COMPLETED, clean(caseId), null, null,
                null, profileName, boxId, "Scan completed.");
    }

    public List<PageImage> getActivePages(String caseId) {
        List<PageImage> activePages = new ArrayList<>();

        for (PageImage page : getPages(caseId)) {
            if (page.isActive()) {
                activePages.add(page);
            }
        }

        return activePages;
    }

    private List<PageImage> getPages(String caseId) {
        return pagesByCaseId.getOrDefault(clean(caseId), new ArrayList<>());
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
