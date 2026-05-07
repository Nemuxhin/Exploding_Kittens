package easv.dal;

import easv.be.AuditLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple audit log storage.
 * Later this class can be replaced with database code without changing the BLL.
 */
public class AuditLogDAO {

    private final List<AuditLog> auditLogs = new ArrayList<>();

    public void save(AuditLog auditLog) {
        auditLogs.add(auditLog);
    }

    public List<AuditLog> findAll() {
        return new ArrayList<>(auditLogs);
    }

    public void clear() {
        auditLogs.clear();
    }
}
