package easv.be;

import java.time.LocalDateTime;

/**
 * A small object that describes one scan run.
 */
public class ScanSession {

    private final String caseId;
    private final String profileName;
    private final String boxId;
    private final LocalDateTime startedAt;
    private String status;

    public ScanSession(String caseId, String profileName, String boxId) {
        this.caseId = caseId;
        this.profileName = profileName;
        this.boxId = boxId;
        this.startedAt = LocalDateTime.now();
        this.status = "STARTED";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getProfileName() {
        return profileName;
    }

    public String getBoxId() {
        return boxId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public String getStatus() {
        return status;
    }
}
