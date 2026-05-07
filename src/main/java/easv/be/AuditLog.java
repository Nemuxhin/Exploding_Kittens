package easv.be;

import java.time.LocalDateTime;

/**
 * One audit log row.
 * It tells us who did something, when it happened, and what it was connected to.
 */
public class AuditLog {

    private final LocalDateTime timestamp;
    private final String username;
    private final boolean systemAction;
    private final String action;
    private final String caseId;
    private final String documentId;
    private final String fileId;
    private final Integer pageNumber;
    private final String profileName;
    private final String boxId;
    private final String details;

    public AuditLog(LocalDateTime timestamp, String username, boolean systemAction, String action,
                    String caseId, String documentId, String fileId, Integer pageNumber,
                    String profileName, String boxId, String details) {
        this.timestamp = timestamp;
        this.username = username;
        this.systemAction = systemAction;
        this.action = action;
        this.caseId = caseId;
        this.documentId = documentId;
        this.fileId = fileId;
        this.pageNumber = pageNumber;
        this.profileName = profileName;
        this.boxId = boxId;
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getUsername() {
        return username;
    }

    public boolean isSystemAction() {
        return systemAction;
    }

    public String getAction() {
        return action;
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

    public Integer getPageNumber() {
        return pageNumber;
    }

    public String getProfileName() {
        return profileName;
    }

    public String getBoxId() {
        return boxId;
    }

    public String getDetails() {
        return details;
    }
}
