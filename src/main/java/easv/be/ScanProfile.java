package easv.be;

public class ScanProfile {
    public static final String DEFAULT_EXPORT_NAMING = "{profileName}_{boxId}";

    private static final String PROFILE_CODE_TOKEN = "{profileCode}";
    private static final String PROFILE_NAME_TOKEN = "{profileName}";

    private final int id;
    private String name;
    private String client;
    private String code;
    private String description;
    private String status;
    private String exportNaming;
    private String lastUpdated;

    private boolean archived;
    private boolean barcodeSplitting;
    private String barcodeDetectedBehavior;
    private String barcodePageBehavior;
    private String defaultRotation;
    private String brightness;
    private String contrast;
    private boolean deskew;
    private String exportFormat;
    private boolean metadataRequiredBeforeExport;

    public ScanProfile(
            int id,
            String name,
            String code,
            String description,
            String status,
            String exportNaming,
            String lastUpdated,
            boolean archived,
            boolean barcodeSplitting,
            String barcodeDetectedBehavior,
            String barcodePageBehavior,
            String defaultRotation,
            String brightness,
            String contrast,
            boolean deskew,
            String exportFormat,
            boolean metadataRequiredBeforeExport
    ) {
        this(
                id,
                name,
                "",
                code,
                description,
                status,
                exportNaming,
                lastUpdated,
                archived,
                barcodeSplitting,
                barcodeDetectedBehavior,
                barcodePageBehavior,
                defaultRotation,
                brightness,
                contrast,
                deskew,
                exportFormat,
                metadataRequiredBeforeExport
        );
    }

    public ScanProfile(
            int id,
            String name,
            String client,
            String code,
            String description,
            String status,
            String exportNaming,
            String lastUpdated,
            boolean archived,
            boolean barcodeSplitting,
            String barcodeDetectedBehavior,
            String barcodePageBehavior,
            String defaultRotation,
            String brightness,
            String contrast,
            boolean deskew,
            String exportFormat,
            boolean metadataRequiredBeforeExport
    ) {
        this.id = id;
        this.name = clean(name);
        this.client = clean(client);
        this.code = clean(code);
        this.description = clean(description);
        this.status = clean(status);
        this.exportNaming = normalizeExportNaming(exportNaming);
        this.lastUpdated = clean(lastUpdated);
        this.archived = archived;
        this.barcodeSplitting = barcodeSplitting;
        this.barcodeDetectedBehavior = clean(barcodeDetectedBehavior);
        this.barcodePageBehavior = clean(barcodePageBehavior);
        this.defaultRotation = clean(defaultRotation);
        this.brightness = clean(brightness);
        this.contrast = clean(contrast);
        this.deskew = deskew;
        this.exportFormat = clean(exportFormat);
        this.metadataRequiredBeforeExport = metadataRequiredBeforeExport;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getClient() { return client; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getExportNaming() { return exportNaming; }
    public String getLastUpdated() { return lastUpdated; }

    public boolean isArchived() { return archived; }
    public boolean isBarcodeSplitting() { return barcodeSplitting; }
    public String getBarcodeDetectedBehavior() { return barcodeDetectedBehavior; }
    public String getBarcodePageBehavior() { return barcodePageBehavior; }
    public String getDefaultRotation() { return defaultRotation; }
    public String getBrightness() { return brightness; }
    public String getContrast() { return contrast; }
    public boolean isDeskew() { return deskew; }
    public String getExportFormat() { return exportFormat; }
    public boolean isMetadataRequiredBeforeExport() { return metadataRequiredBeforeExport; }

    public void setName(String name) { this.name = clean(name); }
    public void setClient(String client) { this.client = clean(client); }
    public void setCode(String code) { this.code = clean(code); }
    public void setDescription(String description) { this.description = clean(description); }
    public void setStatus(String status) { this.status = clean(status); }
    public void setExportNaming(String exportNaming) { this.exportNaming = normalizeExportNaming(exportNaming); }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = clean(lastUpdated); }

    public void setArchived(boolean archived) { this.archived = archived; }
    public void setBarcodeSplitting(boolean barcodeSplitting) { this.barcodeSplitting = barcodeSplitting; }
    public void setBarcodeDetectedBehavior(String barcodeDetectedBehavior) { this.barcodeDetectedBehavior = clean(barcodeDetectedBehavior); }
    public void setBarcodePageBehavior(String barcodePageBehavior) { this.barcodePageBehavior = clean(barcodePageBehavior); }
    public void setDefaultRotation(String defaultRotation) { this.defaultRotation = clean(defaultRotation); }
    public void setBrightness(String brightness) { this.brightness = clean(brightness); }
    public void setContrast(String contrast) { this.contrast = clean(contrast); }
    public void setDeskew(boolean deskew) { this.deskew = deskew; }
    public void setExportFormat(String exportFormat) { this.exportFormat = clean(exportFormat); }
    public void setMetadataRequiredBeforeExport(boolean metadataRequiredBeforeExport) {
        this.metadataRequiredBeforeExport = metadataRequiredBeforeExport;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static String normalizeExportNaming(String value) {
        String cleaned = clean(value);

        if (cleaned.isBlank()) {
            return DEFAULT_EXPORT_NAMING;
        }

        return cleaned.replace(PROFILE_CODE_TOKEN, PROFILE_NAME_TOKEN);
    }
}
