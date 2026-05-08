package easv.bll;

import easv.be.AuditLog;
import easv.be.User;
import easv.dal.AuditLogDAO;

import java.util.ArrayList;
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
    public static final String BARCODE_DETECTED = "BARCODE_DETECTED";

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

    public AuditLog logPageCreated(String caseId, String documentId, String fileId, Integer pageNumber,
                                   String profileName, String boxId) {
        return logUserAction(PAGE_CREATED, caseId, documentId, fileId,
                pageNumber, profileName, boxId, "A scanned page was created.");
    }

    public AuditLog logPageDeleted(String caseId, String documentId, String fileId, Integer pageNumber,
                                   String profileName, String boxId) {
        return logUserAction(PAGE_DELETED, caseId, documentId, fileId,
                pageNumber, profileName, boxId, "A scanned page was deleted.");
    }

    public List<AuditLog> getLogs() {
        return auditLogDAO.findAll();
    }

    private AuditLog saveLog(String username, boolean systemAction, String action, String caseId, String documentId,
                             String fileId, Integer pageNumber, String profileName, String boxId, String details) {
        AuditLog auditLog = new AuditLog(
                auditLogDAO.nextId(),
                java.time.LocalDateTime.now(),
                systemAction ? "System" : typeFor(action),
                systemAction ? "SYSTEM" : username,
                action,
                targetFor(caseId, documentId, fileId, pageNumber),
                statusFor(action),
                details,
                detailsFor(caseId, documentId, fileId, pageNumber, profileName, boxId)
        );

        auditLogDAO.save(auditLog);
        return auditLog;
    }

    private String typeFor(String action) {
        if (action == null) {
            return "System";
        }

        if (action.contains("SCAN") || action.contains("TIFF") || action.contains("BARCODE")) {
            return "Scans";
        }

        if (action.contains("METADATA")) {
            return "Metadata";
        }

        if (action.contains("EXPORT")) {
            return "Exports";
        }

        return "Documents";
    }

    private String statusFor(String action) {
        return SCAN_FAILED.equals(action) ? "Failed" : "Success";
    }

    private String targetFor(String caseId, String documentId, String fileId, Integer pageNumber) {
        List<String> parts = new ArrayList<>();

        addPart(parts, "case", caseId);
        addPart(parts, "document", documentId);
        addPart(parts, "file", fileId);

        if (pageNumber != null) {
            parts.add("page " + pageNumber);
        }

        return parts.isEmpty() ? "System" : String.join(" / ", parts);
    }

    private List<AuditLog.AuditLogDetail> detailsFor(String caseId, String documentId, String fileId,
                                                     Integer pageNumber, String profileName, String boxId) {
        List<AuditLog.AuditLogDetail> details = new ArrayList<>();

        addDetail(details, "Case", caseId);
        addDetail(details, "Document", documentId);
        addDetail(details, "File", fileId);
        addDetail(details, "Page", pageNumber == null ? "" : String.valueOf(pageNumber));
        addDetail(details, "Profile", profileName);
        addDetail(details, "Box", boxId);

        return details;
    }

    private void addDetail(List<AuditLog.AuditLogDetail> details, String label, String value) {
        if (value != null && !value.isBlank()) {
            details.add(new AuditLog.AuditLogDetail(label, value));
        }
    }

    private void addPart(List<String> parts, String label, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(label + " " + value);
        }
    }
}
