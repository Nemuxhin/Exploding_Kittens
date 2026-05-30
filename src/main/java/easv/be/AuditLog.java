package easv.be;

import java.time.LocalDateTime;
import java.util.List;

public class AuditLog {
    private final int id;
    private final LocalDateTime timestamp;
    private final String type;
    private final String actor;
    private final String action;
    private final String target;
    private final String status;
    private final String description;
    private final List<AuditLogDetail> details;

    public AuditLog(
            int id,
            LocalDateTime timestamp,
            String type,
            String actor,
            String action,
            String target,
            String status,
            String description,
            List<AuditLogDetail> details
    ) {
        this.id = id;
        this.timestamp = timestamp;
        this.type = clean(type);
        this.actor = clean(actor);
        this.action = clean(action);
        this.target = clean(target);
        this.status = clean(status);
        this.description = clean(description);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public int getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getType() { return type; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getTarget() { return target; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    public List<AuditLogDetail> getDetails() { return details; }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static class AuditLogDetail {
        private final String label;
        private final String value;
        private final String oldValue;
        private final String newValue;
        private final boolean fieldChange;

        public AuditLogDetail(String label, String value) {
            this(label, value, "", "", false);
        }

        private AuditLogDetail(String label, String value, String oldValue, String newValue, boolean fieldChange) {
            this.label = clean(label);
            this.value = clean(value);
            this.oldValue = clean(oldValue);
            this.newValue = clean(newValue);
            this.fieldChange = fieldChange;
        }

        public static AuditLogDetail change(String label, String oldValue, String newValue) {
            return new AuditLogDetail(label, "", oldValue, newValue, true);
        }

        public static AuditLogDetail stored(
                String label,
                String value,
                String oldValue,
                String newValue,
                boolean fieldChange
        ) {
            return new AuditLogDetail(label, value, oldValue, newValue, fieldChange);
        }

        public String getLabel() { return label; }
        public String getValue() { return value; }
        public String getOldValue() { return oldValue; }
        public String getNewValue() { return newValue; }
        public boolean isFieldChange() { return fieldChange; }
    }
}
