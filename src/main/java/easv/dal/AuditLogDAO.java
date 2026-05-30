package easv.dal;

import easv.be.AuditLog;
import easv.gui.controller.util.Strings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class AuditLogDAO {
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final String DETAIL_MARKER = "\n[[AUDIT_DETAILS_V1:";
    private static final String DETAIL_MARKER_END = "]]";
    private static final String DETAIL_FIELD_SEPARATOR = "\u001F";
    private static final String DETAIL_ROW_SEPARATOR = "\u001E";

    private final DatabaseConnection databaseConnection;
    private final List<AuditLog> inMemoryLogs;
    private int inMemoryNextId = 1;

    public AuditLogDAO() {
        this(new DatabaseConnection());
    }

    public AuditLogDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection == null ? new DatabaseConnection() : databaseConnection;
        this.inMemoryLogs = null;
    }

    private AuditLogDAO(List<AuditLog> inMemoryLogs) {
        this.databaseConnection = null;
        this.inMemoryLogs = inMemoryLogs;
    }

    public static AuditLogDAO inMemory() {
        return new AuditLogDAO(new ArrayList<>());
    }

    public List<AuditLog> getAllAuditLogs() {
        if (inMemoryLogs != null) {
            return new ArrayList<>(inMemoryLogs);
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
        if (inMemoryLogs != null) {
            return inMemoryNextId++;
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
        return saveAuditLog(log, null);
    }

    public AuditLog saveAuditLog(AuditLog log, UUID exportId) {
        if (inMemoryLogs != null) {
            inMemoryLogs.add(log);
            return log;
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO audit_logs
                     (timestamp, type, actor, action, target, status, export_id, description)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                     """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setTimestamp(1, Timestamp.valueOf(log.getTimestamp()));
            statement.setString(2, log.getType());
            statement.setString(3, log.getActor());
            statement.setString(4, log.getAction());
            statement.setString(5, log.getTarget());
            statement.setString(6, log.getStatus());
            if (exportId == null) {
                statement.setNull(7, java.sql.Types.VARCHAR);
            } else {
                statement.setString(7, exportId.toString());
            }
            statement.setString(8, serializeDescription(log));
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

    private AuditLog readAuditLog(ResultSet resultSet) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp("timestamp");
        LocalDateTime loggedAt = timestamp == null ? LocalDateTime.now() : timestamp.toLocalDateTime();
        ParsedDescription parsedDescription = parseDescription(resultSet.getString("description"));

        return new AuditLog(
                resultSet.getInt("id"),
                loggedAt,
                displayType(resultSet.getString("type")),
                resultSet.getString("actor"),
                displayAction(resultSet.getString("action")),
                resultSet.getString("target"),
                displayStatus(resultSet.getString("status")),
                parsedDescription.description(),
                parsedDescription.details()
        );
    }

    private String serializeDescription(AuditLog log) {
        String description = limitToDatabaseDescription(Strings.clean(log.getDescription()));

        if (log.getDetails().isEmpty()) {
            return description;
        }

        List<AuditLog.AuditLogDetail> details = new ArrayList<>(log.getDetails());

        while (!details.isEmpty()) {
            String serializedDescription = description + DETAIL_MARKER + encodeDetails(details) + DETAIL_MARKER_END;

            if (serializedDescription.length() <= MAX_DESCRIPTION_LENGTH) {
                return serializedDescription;
            }

            details.remove(details.size() - 1);
        }

        return description;
    }

    private String encodeDetails(List<AuditLog.AuditLogDetail> details) {
        StringBuilder payload = new StringBuilder();

        for (AuditLog.AuditLogDetail detail : details) {
            if (!payload.isEmpty()) {
                payload.append(DETAIL_ROW_SEPARATOR);
            }

            payload.append(detail.isFieldChange() ? "1" : "0")
                    .append(DETAIL_FIELD_SEPARATOR)
                    .append(Strings.clean(detail.getLabel()))
                    .append(DETAIL_FIELD_SEPARATOR)
                    .append(Strings.clean(detail.getValue()))
                    .append(DETAIL_FIELD_SEPARATOR)
                    .append(Strings.clean(detail.getOldValue()))
                    .append(DETAIL_FIELD_SEPARATOR)
                    .append(Strings.clean(detail.getNewValue()));
        }

        return encode(payload.toString());
    }

    private String limitToDatabaseDescription(String value) {
        String cleanValue = Strings.clean(value);
        return cleanValue.length() <= MAX_DESCRIPTION_LENGTH
                ? cleanValue
                : cleanValue.substring(0, MAX_DESCRIPTION_LENGTH);
    }

    private ParsedDescription parseDescription(String storedDescription) {
        String description = Strings.clean(storedDescription);
        int markerIndex = description.indexOf(DETAIL_MARKER);

        if (markerIndex < 0) {
            return new ParsedDescription(description, List.of());
        }

        int payloadStart = markerIndex + DETAIL_MARKER.length();
        int payloadEnd = description.indexOf(DETAIL_MARKER_END, payloadStart);

        if (payloadEnd < 0) {
            return new ParsedDescription(description.substring(0, markerIndex).trim(), List.of());
        }

        String visibleDescription = description.substring(0, markerIndex).trim();
        String encodedPayload = description.substring(payloadStart, payloadEnd);

        return new ParsedDescription(visibleDescription, parseDetails(encodedPayload));
    }

    private List<AuditLog.AuditLogDetail> parseDetails(String encodedPayload) {
        String payload = decode(encodedPayload);

        if (payload.isBlank()) {
            return List.of();
        }

        List<AuditLog.AuditLogDetail> details = new ArrayList<>();

        for (String line : payload.split(DETAIL_ROW_SEPARATOR, -1)) {
            String[] parts = line.split(DETAIL_FIELD_SEPARATOR, -1);

            if (parts.length < 5) {
                continue;
            }

            details.add(AuditLog.AuditLogDetail.stored(
                    parts[1],
                    parts[2],
                    parts[3],
                    parts[4],
                    "1".equals(parts[0])
            ));
        }

        return details;
    }

    private String encode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(Strings.clean(value).getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
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
        String cleanedType = Strings.clean(type);

        return switch (cleanedType.toLowerCase(java.util.Locale.ROOT)) {
            case "users" -> "Users";
            case "profiles" -> "Profiles";
            case "access" -> "Access";
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
        return titleCase(Strings.clean(status));
    }

    private String displayAction(String action) {
        String cleanedAction = Strings.clean(action);

        if ("METADATA_SAVED".equalsIgnoreCase(cleanedAction)) {
            return "DOCUMENT_DETAILS_SAVED";
        }

        return cleanedAction;
    }

    private String titleCase(String value) {
        if (value.isBlank()) {
            return "";
        }

        String lowerCase = value.toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lowerCase.charAt(0)) + lowerCase.substring(1);
    }

    private record ParsedDescription(String description, List<AuditLog.AuditLogDetail> details) {
    }
}
