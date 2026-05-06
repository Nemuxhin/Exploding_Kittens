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
import java.util.List;

public class AuditLogDAO {
    private final DatabaseConnection databaseConnection;

    public AuditLogDAO() {
        this(new DatabaseConnection());
    }

    public AuditLogDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public List<AuditLog> findAll() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, timestamp, type, actor, action, target, status, description
                     FROM audit_logs
                     ORDER BY timestamp DESC, id DESC
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            List<AuditLog> logs = new ArrayList<>();
            while (resultSet.next()) {
                Timestamp timestamp = resultSet.getTimestamp("timestamp");
                logs.add(new AuditLog(
                        resultSet.getInt("id"),
                        timestamp == null ? null : timestamp.toLocalDateTime(),
                        resultSet.getString("type"),
                        resultSet.getString("actor"),
                        resultSet.getString("action"),
                        resultSet.getString("target"),
                        resultSet.getString("status"),
                        resultSet.getString("description"),
                        List.of()
                ));
            }
            return logs;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch audit logs", e);
        }
    }

    public AuditLog create(String type, String actor, String action, String target, String status, String description) {
        LocalDateTime timestamp = LocalDateTime.now();

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO audit_logs (timestamp, type, actor, action, target, status, description)
                     VALUES (?, ?, ?, ?, ?, ?, ?)
                     """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setTimestamp(1, Timestamp.valueOf(timestamp));
            statement.setString(2, type);
            statement.setString(3, actor);
            statement.setString(4, action);
            statement.setString(5, target);
            statement.setString(6, status);
            statement.setString(7, description);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new DataAccessException("Creating audit log did not return an id.");
                }

                return new AuditLog(
                        generatedKeys.getInt(1),
                        timestamp,
                        type,
                        actor,
                        action,
                        target,
                        status,
                        description,
                        List.of()
                );
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create audit log", e);
        }
    }
}
