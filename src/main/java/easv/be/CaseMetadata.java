package easv.be;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Metadata saved for one scanned case.
 * A LinkedHashMap keeps the fields in the same order the user entered them.
 */
public class CaseMetadata {

    private final String caseId;
    private final String profileName;
    private final String boxId;
    private final Map<String, String> values;
    private final boolean completed;
    private final boolean approved;

    public CaseMetadata(String caseId, String profileName, String boxId, Map<String, String> values,
                        boolean completed, boolean approved) {
        this.caseId = caseId;
        this.profileName = profileName;
        this.boxId = boxId;
        this.values = new LinkedHashMap<>(values);
        this.completed = completed;
        this.approved = approved;
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

    public Map<String, String> getValues() {
        return new LinkedHashMap<>(values);
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isApproved() {
        return approved;
    }

    public boolean isLocked() {
        return completed || approved;
    }
}
