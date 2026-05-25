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
    public static final String SCAN_SESSION_STARTED = "SCAN_SESSION_STARTED";
    public static final String SCAN_SESSION_RESUMED = "SCAN_SESSION_RESUMED";
    public static final String SCAN_ITEM_IMPORTED = "SCAN_ITEM_IMPORTED";
    public static final String TIFF_FETCHED = "TIFF_FETCHED";
    public static final String SCAN_FAILED = "SCAN_FAILED";
    public static final String RETRY_USED = "RETRY_USED";
    public static final String SCAN_COMPLETED = "SCAN_COMPLETED";
    public static final String PAGE_CREATED = "PAGE_CREATED";
    public static final String PAGE_DELETED = "PAGE_DELETED";
    public static final String METADATA_SAVED = "METADATA_SAVED";
    public static final String EXPORT_PREVIEW_CREATED = "EXPORT_PREVIEW_CREATED";
    public static final String EXPORT_COMPLETED = "EXPORT_COMPLETED";
    public static final String EXPORT_FAILED = "EXPORT_FAILED";
    public static final String BARCODE_DETECTED = "BARCODE_DETECTED";
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGOUT = "LOGOUT";

    private final AuditLogDAO auditLogDAO;

    public AuditLogManager() {
        this(new AuditLogDAO());
    }

    public AuditLogManager(AuditLogDAO auditLogDAO) {
        this.auditLogDAO = auditLogDAO;
    }

    public AuditLog logUserAction(String action, String caseId, String documentId, String fileId,
                                  Integer pageNumber, String profileName, String boxId, String details) {
        return logUserAction(action, caseId, documentId, fileId, pageNumber, profileName, boxId, details, List.of());
    }

    public AuditLog logUserAction(String action, String caseId, String documentId, String fileId,
                                  Integer pageNumber, String profileName, String boxId, String details,
                                  List<AuditLog.AuditLogDetail> auditDetails) {
        User currentUser = UserSession.getCurrentUser();
        String username = currentUser == null ? "SYSTEM" : currentUser.getUsername();
        boolean systemAction = currentUser == null;

        return saveLog(username, systemAction, action, caseId, documentId, fileId, pageNumber, profileName, boxId, details, auditDetails);
    }

    public AuditLog logSystemAction(String action, String caseId, String documentId, String fileId,
                                    Integer pageNumber, String profileName, String boxId, String details) {
        return saveLog("SYSTEM", true, action, caseId, documentId, fileId, pageNumber, profileName, boxId, details, List.of());
    }

    public AuditLog logPageCreated(String caseId, String documentId, String fileId, Integer pageNumber,
                                   String profileName, String boxId) {
        return logUserAction(PAGE_CREATED, caseId, documentId, fileId,
                pageNumber, profileName, boxId, "A scanned page was created.");
    }

    /**
     * Records a sign-in / sign-out event. UserSession is not consulted, because the user
     * may not be authenticated yet (login attempts) or may already be cleared (logout).
     * Failure descriptions are kept generic to avoid revealing whether an account exists
     * or what its status is (OWASP authentication recommendation).
     */
    public AuditLog logAuth(String attemptedUsername, String action, String description) {
        String actor = attemptedUsername == null || attemptedUsername.isBlank()
                ? "Unknown"
                : attemptedUsername.trim();
        boolean failed = LOGIN_FAILED.equals(action);
        String type = failed ? "Security" : "Access";
        String status = failed ? "Failed" : "Success";
        String target = LOGOUT.equals(action) ? "Logout" : "Login";

        AuditLog log = new AuditLog(
                auditLogDAO.nextAuditLogId(),
                java.time.LocalDateTime.now(),
                type,
                actor,
                action,
                target,
                status,
                description,
                List.of()
        );

        return auditLogDAO.saveAuditLog(log);
    }

    public AuditLog logPageDeleted(String caseId, String documentId, String fileId, Integer pageNumber,
                                   String profileName, String boxId) {
        return logUserAction(PAGE_DELETED, caseId, documentId, fileId,
                pageNumber, profileName, boxId, "A scanned page was deleted.");
    }

    public List<AuditLog> getLogs() {
        return auditLogDAO.getAllAuditLogs();
    }

    private AuditLog saveLog(String username, boolean systemAction, String action, String caseId, String documentId,
                             String fileId, Integer pageNumber, String profileName, String boxId, String details,
                             List<AuditLog.AuditLogDetail> auditDetails) {
        List<AuditLog.AuditLogDetail> combinedDetails = new ArrayList<>(detailsFor(
                caseId,
                documentId,
                fileId,
                pageNumber,
                profileName,
                boxId
        ));

        if (auditDetails != null) {
            combinedDetails.addAll(auditDetails);
        }

        AuditLog auditLog = new AuditLog(
                auditLogDAO.nextAuditLogId(),
                java.time.LocalDateTime.now(),
                systemAction ? "System" : typeFor(action),
                systemAction ? "SYSTEM" : username,
                action,
                targetFor(caseId, documentId, fileId, pageNumber),
                statusFor(action),
                details,
                combinedDetails
        );

        return auditLogDAO.saveAuditLog(auditLog);
    }

    private String typeFor(String action) {
        if (action == null) {
            return "System";
        }

        if (action.contains("LOGIN") && action.contains("FAILED")) {
            return "Security";
        }

        if (action.contains("LOGIN") || action.contains("LOGOUT")) {
            return "Access";
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
        return action != null && action.endsWith("_FAILED") ? "Failed" : "Success";
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
