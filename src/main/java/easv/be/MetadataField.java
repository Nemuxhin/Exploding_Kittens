package easv.be;

public class MetadataField {
    private final int id;
    private String name;
    private String type;
    private boolean required;
    private String placeholder;
    private String options;

    public MetadataField(int id, String name, String type, boolean required, String placeholder) {
        this(id, name, type, required, placeholder, "");
    }

    public MetadataField(int id, String name, String type, boolean required, String placeholder, String options) {
        this.id = id;
        this.name = clean(name);
        this.type = clean(type);
        this.required = required;
        this.placeholder = clean(placeholder);
        this.options = clean(options);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public boolean isRequired() { return required; }
    public String getPlaceholder() { return placeholder; }
    public String getOptions() { return options; }

    public void setName(String name) { this.name = clean(name); }
    public void setType(String type) { this.type = clean(type); }
    public void setRequired(boolean required) { this.required = required; }
    public void setPlaceholder(String placeholder) { this.placeholder = clean(placeholder); }
    public void setOptions(String options) { this.options = clean(options); }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
