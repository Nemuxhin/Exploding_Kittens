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

    private final MetadataDAO metadataDAO;
    private final AuditLogManager auditLogManager;

    public MetadataManager() {
        this(new MetadataDAO(), new AuditLogManager());
    }

    public MetadataManager(MetadataDAO metadataDAO, AuditLogManager auditLogManager) {
        this.metadataDAO = metadataDAO;
        this.auditLogManager = auditLogManager;
    }

    public boolean saveMetadata(String caseId, String profileName, String boxId, Map<String, String> values) {
        return saveMetadata(caseId, profileName, boxId, values, false, false);
    }

    public boolean saveMetadata(String caseId, String profileName, String boxId, Map<String, String> values,
                                boolean completed, boolean approved) {
        CaseMetadata existingMetadata = metadataDAO.findByCaseId(caseId);

        // A completed or approved case should not be changed anymore.
        if (existingMetadata != null && existingMetadata.isLocked()) {
            return false;
        }

        CaseMetadata metadata = new CaseMetadata(clean(caseId), clean(profileName), clean(boxId),
                cleanValues(values), completed, approved);
        metadataDAO.save(metadata);

        auditLogManager.logUserAction(AuditLogManager.METADATA_SAVED, caseId, null, null,
                null, profileName, boxId, "Metadata was saved for the scanned case.");

        return true;
    }

    public CaseMetadata loadMetadataForm(String caseId) {
        return metadataDAO.findByCaseId(caseId);
    }

    public boolean canEdit(String caseId) {
        CaseMetadata metadata = metadataDAO.findByCaseId(caseId);
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
