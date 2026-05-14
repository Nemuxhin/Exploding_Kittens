package easv.dal;

import easv.be.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AuditLogDAO {
    private final DatabaseConnection databaseConnection;
    private final List<AuditLog> memoryLogs;
    private int memoryNextId = 1;

    public AuditLogDAO() {
        this(new DatabaseConnection());
    }

    public AuditLogDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection == null ? new DatabaseConnection() : databaseConnection;
        this.memoryLogs = null;
    }

    private AuditLogDAO(List<AuditLog> memoryLogs) {
        this.databaseConnection = null;
        this.memoryLogs = memoryLogs;
    }

    public static AuditLogDAO inMemory() {
        return new AuditLogDAO(new ArrayList<>());
    }

    public List<AuditLog> getAllAuditLogs() {
        if (memoryLogs != null) {
            return memoryLogs.stream()
                    .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                    .toList();
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, timestamp, type, actor, action, target, status, description
                     FROM audit_logs
                     ORDER BY timestamp DESC, id DESC
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            List<AuditLog> logs = new ArrayList<>();

            while (resultSet.next()) {
                logs.add(readAuditLog(resultSet));
            }

            return logs;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to read audit logs.", exception);
        }
    }

    public int nextAuditLogId() {
        if (memoryLogs != null) {
            return memoryNextId;
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COALESCE(MAX(id), 0) + 1 FROM audit_logs");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to calculate next audit log id.", exception);
        }
    }

    public AuditLog saveAuditLog(AuditLog log) {
        if (memoryLogs != null) {
            AuditLog savedLog = new AuditLog(
                    memoryNextId++,
                    log.getTimestamp(),
                    log.getType(),
                    log.getActor(),
                    log.getAction(),
                    log.getTarget(),
                    log.getStatus(),
                    log.getDescription(),
                    log.getDetails()
            );
            memoryLogs.add(savedLog);
            return savedLog;
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO audit_logs
                     (timestamp, type, actor, action, target, status, description)
                     VALUES (?, ?, ?, ?, ?, ?, ?)
                     """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setTimestamp(1, Timestamp.valueOf(log.getTimestamp()));
            statement.setString(2, log.getType());
            statement.setString(3, log.getActor());
            statement.setString(4, log.getAction());
            statement.setString(5, log.getTarget());
            statement.setString(6, log.getStatus());
            statement.setString(7, log.getDescription());
            statement.executeUpdate();

            return new AuditLog(
                    readGeneratedIntId(statement),
                    log.getTimestamp(),
                    log.getType(),
                    log.getActor(),
                    log.getAction(),
                    log.getTarget(),
                    log.getStatus(),
                    log.getDescription(),
                    log.getDetails()
            );
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to save audit log.", exception);
        }
    }

    public List<AuditLog> findAll() {
        return getAllAuditLogs();
    }

    public int nextId() {
        return nextAuditLogId();
    }

    public AuditLog save(AuditLog log) {
        return saveAuditLog(log);
    }

    private AuditLog readAuditLog(ResultSet resultSet) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp("timestamp");
        LocalDateTime loggedAt = timestamp == null ? LocalDateTime.now() : timestamp.toLocalDateTime();

        return new AuditLog(
                resultSet.getInt("id"),
                loggedAt,
                displayType(resultSet.getString("type")),
                resultSet.getString("actor"),
                resultSet.getString("action"),
                resultSet.getString("target"),
                displayStatus(resultSet.getString("status")),
                resultSet.getString("description"),
                List.of()
        );
    }

    private int readGeneratedIntId(Statement statement) throws SQLException {
        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            }
        }

        throw new SQLException("Database did not return a generated audit log id.");
    }

    private String displayType(String type) {
        String cleanedType = clean(type);

        return switch (cleanedType.toLowerCase(java.util.Locale.ROOT)) {
            case "users" -> "Users";
            case "profiles" -> "Profiles";
            case "access" -> "Access";
            case "metadata" -> "Metadata";
            case "scans" -> "Scans";
            case "documents" -> "Documents";
            case "qa" -> "QA";
            case "exports" -> "Exports";
            case "security" -> "Security";
            case "system" -> "System";
            default -> titleCase(cleanedType);
        };
    }

    private String displayStatus(String status) {
        return titleCase(clean(status));
    }

    private String titleCase(String value) {
        if (value.isBlank()) {
            return "";
        }

        String lowerCase = value.toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lowerCase.charAt(0)) + lowerCase.substring(1);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
