package easv.be;

public class MetadataReviewRecord {
    private final String id;
    private String identity;
    private String client;
    private String archive;
    private String profile;
    private String metadataTemplate;
    private String metadataStatus;
    private String qaStatus;
    private int pages;
    private String lastUpdated;
    private String assignedTo;
    private String scannedBy;
    private String dateGroup;
    private boolean warning;

    public MetadataReviewRecord(
            String id,
            String identity,
            String client,
            String archive,
            String profile,
            String metadataTemplate,
            String metadataStatus,
            String qaStatus,
            int pages,
            String lastUpdated,
            String assignedTo,
            String scannedBy,
            String dateGroup,
            boolean warning
    ) {
        this.id = clean(id);
        this.identity = clean(identity);
        this.client = clean(client);
        this.archive = clean(archive);
        this.profile = clean(profile);
        this.metadataTemplate = clean(metadataTemplate);
        this.metadataStatus = clean(metadataStatus);
        this.qaStatus = clean(qaStatus);
        this.pages = Math.max(0, pages);
        this.lastUpdated = clean(lastUpdated);
        this.assignedTo = clean(assignedTo);
        this.scannedBy = clean(scannedBy);
        this.dateGroup = clean(dateGroup);
        this.warning = warning;
    }

    public String getId() { return id; }
    public String getIdentity() { return identity; }
    public String getClient() { return client; }
    public String getArchive() { return archive; }
    public String getProfile() { return profile; }
    public String getMetadataTemplate() { return metadataTemplate; }
    public String getMetadataStatus() { return metadataStatus; }
    public String getQaStatus() { return qaStatus; }
    public int getPages() { return pages; }
    public String getLastUpdated() { return lastUpdated; }
    public String getAssignedTo() { return assignedTo; }
    public String getScannedBy() { return scannedBy; }
    public String getDateGroup() { return dateGroup; }
    public boolean hasWarning() { return warning; }

    public void setIdentity(String identity) { this.identity = clean(identity); }
    public void setClient(String client) { this.client = clean(client); }
    public void setArchive(String archive) { this.archive = clean(archive); }
    public void setProfile(String profile) { this.profile = clean(profile); }
    public void setMetadataTemplate(String metadataTemplate) { this.metadataTemplate = clean(metadataTemplate); }
    public void setMetadataStatus(String metadataStatus) { this.metadataStatus = clean(metadataStatus); }
    public void setQaStatus(String qaStatus) { this.qaStatus = clean(qaStatus); }
    public void setPages(int pages) { this.pages = Math.max(0, pages); }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = clean(lastUpdated); }
    public void setAssignedTo(String assignedTo) { this.assignedTo = clean(assignedTo); }
    public void setScannedBy(String scannedBy) { this.scannedBy = clean(scannedBy); }
    public void setDateGroup(String dateGroup) { this.dateGroup = clean(dateGroup); }
    public void setWarning(boolean warning) { this.warning = warning; }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
