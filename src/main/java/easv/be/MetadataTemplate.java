package easv.be;

import java.util.List;

public class MetadataTemplate {
    private final int id;
    private String name;
    private String description;
    private List<String> assignedProfileNames;
    private List<MetadataField> fields;
    private String status;
    private String lastUpdated;

    public MetadataTemplate(
            int id,
            String name,
            String description,
            List<String> assignedProfileNames,
            List<MetadataField> fields,
            String status,
            String lastUpdated
    ) {
        this.id = id;
        this.name = clean(name);
        this.description = clean(description);
        this.assignedProfileNames = copyStrings(assignedProfileNames);
        this.fields = copyFields(fields);
        this.status = clean(status);
        this.lastUpdated = clean(lastUpdated);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getAssignedProfileNames() { return assignedProfileNames; }
    public List<MetadataField> getFields() { return fields; }
    public String getStatus() { return status; }
    public String getLastUpdated() { return lastUpdated; }

    public void setName(String name) { this.name = clean(name); }
    public void setDescription(String description) { this.description = clean(description); }
    public void setAssignedProfileNames(List<String> assignedProfileNames) {
        this.assignedProfileNames = copyStrings(assignedProfileNames);
    }
    public void setFields(List<MetadataField> fields) { this.fields = copyFields(fields); }
    public void setStatus(String status) { this.status = clean(status); }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = clean(lastUpdated); }

    public int getFieldCount() {
        return fields.size();
    }

    public boolean isArchived() {
        return status.equalsIgnoreCase("Archived");
    }

    private static List<String> copyStrings(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static List<MetadataField> copyFields(List<MetadataField> fields) {
        return fields == null ? List.of() : List.copyOf(fields);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
