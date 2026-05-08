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

        public AuditLogDetail(String label, String value) {
            this.label = clean(label);
            this.value = clean(value);
        }

        public String getLabel() { return label; }
        public String getValue() { return value; }
    }
}
