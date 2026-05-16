package easv.bll;

import easv.be.CaseMetadata;
import easv.be.AuditLog;
import easv.dal.MetadataDAO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class controls metadata rules after scanning.
 * Metadata can be edited until the case is completed or approved.
 */
public class MetadataManager {

    private static final Map<String, CaseMetadata> SHARED_METADATA_BY_CASE_ID = new LinkedHashMap<>();
    private static final AuditLogManager SHARED_AUDIT_LOG_MANAGER = new AuditLogManager();

    private final Map<String, CaseMetadata> metadataByCaseId;
    private final AuditLogManager auditLogManager;

    public MetadataManager() {
        this(new LinkedHashMap<>(), SHARED_AUDIT_LOG_MANAGER);
    }

    public static MetadataManager shared() {
        return new MetadataManager(SHARED_METADATA_BY_CASE_ID, SHARED_AUDIT_LOG_MANAGER);
    }

    public MetadataManager(MetadataDAO metadataDAO, AuditLogManager auditLogManager) {
        this(new LinkedHashMap<>(), auditLogManager);
    }

    MetadataManager(Map<String, CaseMetadata> metadataByCaseId, AuditLogManager auditLogManager) {
        this.metadataByCaseId = metadataByCaseId;
        this.auditLogManager = auditLogManager == null ? SHARED_AUDIT_LOG_MANAGER : auditLogManager;
    }

    public boolean saveMetadata(String caseId, String profileName, String boxId, Map<String, String> values) {
        return saveMetadata(caseId, profileName, boxId, values, false, false);
    }

    public boolean saveMetadata(String caseId, String profileName, String boxId, Map<String, String> values,
                                boolean completed, boolean approved) {
        String cleanCaseId = clean(caseId);
        CaseMetadata existingMetadata = metadataByCaseId.get(cleanCaseId);

        // A completed or approved case should not be changed anymore.
        if (existingMetadata != null && existingMetadata.isLocked()) {
            return false;
        }

        Map<String, String> cleanedValues = cleanValues(values);
        CaseMetadata metadata = new CaseMetadata(cleanCaseId, clean(profileName), clean(boxId),
                cleanedValues, completed, approved);
        metadataByCaseId.put(cleanCaseId, metadata);

        auditLogManager.logUserAction(AuditLogManager.METADATA_SAVED, caseId, null, null,
                null, profileName, boxId, "Metadata was saved for the scanned case.",
                metadataChanges(existingMetadata, metadata));

        return true;
    }

    public CaseMetadata loadMetadataForm(String caseId) {
        return metadataByCaseId.get(clean(caseId));
    }

    public boolean canEdit(String caseId) {
        CaseMetadata metadata = metadataByCaseId.get(clean(caseId));
        return metadata == null || !metadata.isLocked();
    }

    private Map<String, String> cleanValues(Map<String, String> values) {
        Map<String, String> cleanValues = new LinkedHashMap<>();

        if (values == null) {
            return cleanValues;
        }

        for (Map.Entry<String, String> entry : values.entrySet()) {
            cleanValues.put(clean(entry.getKey()), clean(entry.getValue()));
        }

        return cleanValues;
    }

    private List<AuditLog.AuditLogDetail> metadataChanges(CaseMetadata previousMetadata, CaseMetadata updatedMetadata) {
        List<AuditLog.AuditLogDetail> changes = new ArrayList<>();

        if (updatedMetadata == null) {
            return changes;
        }

        addChangedValue(changes, "Case ID", previousMetadata == null ? "" : previousMetadata.getCaseId(), updatedMetadata.getCaseId());
        addChangedValue(changes, "Profile", previousMetadata == null ? "" : previousMetadata.getProfileName(), updatedMetadata.getProfileName());
        addChangedValue(changes, "Box", previousMetadata == null ? "" : previousMetadata.getBoxId(), updatedMetadata.getBoxId());
        addChangedValue(changes, "Completed", previousMetadata == null ? "" : String.valueOf(previousMetadata.isCompleted()), String.valueOf(updatedMetadata.isCompleted()));
        addChangedValue(changes, "Approved", previousMetadata == null ? "" : String.valueOf(previousMetadata.isApproved()), String.valueOf(updatedMetadata.isApproved()));

        Map<String, String> previousValues = previousMetadata == null ? Map.of() : previousMetadata.getValues();
        Map<String, String> updatedValues = updatedMetadata.getValues();
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        fields.addAll(previousValues.keySet());
        fields.addAll(updatedValues.keySet());

        for (String field : fields) {
            addChangedValue(changes, field, previousValues.get(field), updatedValues.get(field));
        }

        return changes;
    }

    private void addChangedValue(List<AuditLog.AuditLogDetail> changes, String label, String oldValue, String newValue) {
        String cleanOldValue = cleanAuditValue(oldValue);
        String cleanNewValue = cleanAuditValue(newValue);

        if (!Objects.equals(cleanOldValue, cleanNewValue)) {
            changes.add(AuditLog.AuditLogDetail.change(label, cleanOldValue, cleanNewValue));
        }
    }

    private String cleanAuditValue(String value) {
        String cleanedValue = clean(value);

        if ("true".equalsIgnoreCase(cleanedValue)) {
            return "Yes";
        }

        if ("false".equalsIgnoreCase(cleanedValue)) {
            return "No";
        }

        return cleanedValue;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
