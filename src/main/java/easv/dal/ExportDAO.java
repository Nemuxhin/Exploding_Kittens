package easv.dal;

import easv.be.ExportRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExportDAO {
    private final DatabaseConnection databaseConnection;

    public ExportDAO() {
        this(new DatabaseConnection());
    }

    public ExportDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection == null ? new DatabaseConnection() : databaseConnection;
    }

    public ExportRecord save(ExportRecord record) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO exports (
                         id,
                         session_id,
                         document_id,
                         exported_by_user_id,
                         export_format,
                         export_status,
                         file_name,
                         file_path,
                         exported_at,
                         error_message,
                         created_at
                     ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                     """)) {
            statement.setString(1, record.id().toString());
            statement.setString(2, record.sessionId().toString());
            if (record.documentId() == null) {
                statement.setNull(3, java.sql.Types.VARCHAR);
            } else {
                statement.setString(3, record.documentId().toString());
            }
            if (record.exportedByUserId() == null) {
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(4, record.exportedByUserId());
            }
            statement.setString(5, record.exportFormat());
            statement.setString(6, record.exportStatus());
            statement.setString(7, record.fileName());
            statement.setString(8, record.filePath());
            statement.setTimestamp(9, Timestamp.from(record.exportedAt()));
            statement.setString(10, record.errorMessage());
            statement.executeUpdate();
            return record;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to save export record " + record.fileName(), exception);
        }
    }

    public List<ExportHistoryItem> findByExportedByUser(Integer userId) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT e.id,
                            e.session_id,
                            e.document_id,
                            e.file_name,
                            e.file_path,
                            e.exported_at,
                            e.export_status,
                            b.box_id,
                            COALESCE(NULLIF(s.profile_name, ''), 'Unspecified Profile') AS profile_name,
                            CASE
                                WHEN e.document_id IS NULL THEN
                                    (SELECT COUNT(*) FROM scan_session_documents ssd WHERE ssd.session_id = e.session_id)
                                ELSE 1
                            END AS document_count
                     FROM exports e
                     JOIN scan_sessions s ON s.id = e.session_id
                     JOIN boxes b ON b.id = s.box_id
                     WHERE (? IS NULL OR e.exported_by_user_id = ?)
                     ORDER BY e.exported_at DESC, e.id DESC
                     """)) {
            if (userId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
                statement.setNull(2, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, userId);
                statement.setInt(2, userId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                List<ExportHistoryItem> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(new ExportHistoryItem(
                            UUID.fromString(resultSet.getString("id")),
                            UUID.fromString(resultSet.getString("session_id")),
                            nullableUuid(resultSet, "document_id"),
                            resultSet.getString("file_name"),
                            resultSet.getString("file_path"),
                            resultSet.getString("box_id"),
                            resultSet.getString("profile_name"),
                            resultSet.getTimestamp("exported_at").toInstant(),
                            resultSet.getString("export_status"),
                            resultSet.getInt("document_count")
                    ));
                }
                return items;
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to read export history.", exception);
        }
    }

    private UUID nullableUuid(ResultSet resultSet, String columnName) throws SQLException {
        String value = resultSet.getString(columnName);
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    public record ExportHistoryItem(
            UUID exportId,
            UUID sessionId,
            UUID documentId,
            String fileName,
            String filePath,
            String boxId,
            String profileName,
            Instant exportedAt,
            String exportStatus,
            int documentCount
    ) {
    }
}
