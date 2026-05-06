package easv.be;

public class MetadataField {
    private final int id;
    private String name;
    private String type;
    private boolean required;
    private String placeholder;

    public MetadataField(int id, String name, String type, boolean required, String placeholder) {
        this.id = id;
        this.name = clean(name);
        this.type = clean(type);
        this.required = required;
        this.placeholder = clean(placeholder);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public boolean isRequired() { return required; }
    public String getPlaceholder() { return placeholder; }

    public void setName(String name) { this.name = clean(name); }
    public void setType(String type) { this.type = clean(type); }
    public void setRequired(boolean required) { this.required = required; }
    public void setPlaceholder(String placeholder) { this.placeholder = clean(placeholder); }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
