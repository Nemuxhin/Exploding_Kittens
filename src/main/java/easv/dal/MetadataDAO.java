package easv.dal;

import easv.be.CaseMetadata;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple metadata storage by case id.
 * Keeping it behind a DAO makes future database integration easier.
 */
public class MetadataDAO {

    private final Map<String, CaseMetadata> metadataByCaseId = new HashMap<>();

    public void save(CaseMetadata metadata) {
        metadataByCaseId.put(metadata.getCaseId(), metadata);
    }

    public CaseMetadata findByCaseId(String caseId) {
        return metadataByCaseId.get(caseId);
    }
}
