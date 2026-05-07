package easv.bll;

import easv.be.AuditLog;
import easv.be.PageImage;
import easv.be.User;
import easv.dal.AuditLogDAO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * This class creates audit logs in one place.
 * That keeps scan, delete, export, and system actions consistent.
 */
public class AuditLogManager {

    public static final String SCAN_STARTED = "SCAN_STARTED";
    public static final String TIFF_FETCHED = "TIFF_FETCHED";
    public static final String SCAN_FAILED = "SCAN_FAILED";
    public static final String RETRY_USED = "RETRY_USED";
    public static final String SCAN_COMPLETED = "SCAN_COMPLETED";
    public static final String PAGE_CREATED = "PAGE_CREATED";
    public static final String PAGE_DELETED = "PAGE_DELETED";
    public static final String METADATA_SAVED = "METADATA_SAVED";
    public static final String EXPORT_PREVIEW_CREATED = "EXPORT_PREVIEW_CREATED";

    private final AuditLogDAO auditLogDAO;

    public AuditLogManager() {
        this(new AuditLogDAO());
    }

    public AuditLogManager(AuditLogDAO auditLogDAO) {
        this.auditLogDAO = auditLogDAO;
    }

    public AuditLog logUserAction(String action, String caseId, String documentId, String fileId,
                                  Integer pageNumber, String profileName, String boxId, String details) {
        User currentUser = UserSession.getCurrentUser();
        String username = currentUser == null ? "SYSTEM" : currentUser.getUsername();
        boolean systemAction = currentUser == null;

        return saveLog(username, systemAction, action, caseId, documentId, fileId, pageNumber, profileName, boxId, details);
    }

    public AuditLog logSystemAction(String action, String caseId, String documentId, String fileId,
                                    Integer pageNumber, String profileName, String boxId, String details) {
        return saveLog("SYSTEM", true, action, caseId, documentId, fileId, pageNumber, profileName, boxId, details);
    }

    public AuditLog logPageCreated(PageImage page, String profileName, String boxId) {
        return logUserAction(PAGE_CREATED, page.getCaseId(), page.getDocumentId(), page.getFileId(),
                page.getPageNumber(), profileName, boxId, "A scanned page was created.");
    }

    public AuditLog logPageDeleted(PageImage page, String profileName, String boxId) {
        return logUserAction(PAGE_DELETED, page.getCaseId(), page.getDocumentId(), page.getFileId(),
                page.getPageNumber(), profileName, boxId, "A scanned page was deleted.");
    }

    public List<AuditLog> getLogs() {
        return auditLogDAO.findAll();
    }

    private AuditLog saveLog(String username, boolean systemAction, String action, String caseId, String documentId,
                             String fileId, Integer pageNumber, String profileName, String boxId, String details) {
        AuditLog auditLog = new AuditLog(LocalDateTime.now(), username, systemAction, action,
                caseId, documentId, fileId, pageNumber, profileName, boxId, details);

        auditLogDAO.save(auditLog);
        return auditLog;
    }
}
