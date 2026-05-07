package easv.be;

/**
 * One metadata field connected to one case.
 */
public class MetadataField {

    private final String caseId;
    private final String fieldName;
    private final String value;

    public MetadataField(String caseId, String fieldName, String value) {
        this.caseId = caseId;
        this.fieldName = fieldName;
        this.value = value;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getValue() {
        return value;
    }
}
