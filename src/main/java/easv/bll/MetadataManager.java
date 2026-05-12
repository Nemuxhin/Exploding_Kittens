package easv.bll;

import easv.be.CaseMetadata;
import easv.dal.MetadataDAO;

import java.util.LinkedHashMap;
import java.util.Map;

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

    private MetadataManager(Map<String, CaseMetadata> metadataByCaseId, AuditLogManager auditLogManager) {
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

        CaseMetadata metadata = new CaseMetadata(cleanCaseId, clean(profileName), clean(boxId),
                cleanValues(values), completed, approved);
        metadataByCaseId.put(cleanCaseId, metadata);

        auditLogManager.logUserAction(AuditLogManager.METADATA_SAVED, caseId, null, null,
                null, profileName, boxId, "Metadata was saved for the scanned case.");

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

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
