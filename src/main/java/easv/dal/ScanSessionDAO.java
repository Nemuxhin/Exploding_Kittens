package easv.dal;

import easv.be.Document;
import easv.be.ScanSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScanSessionDAO {
    private final DatabaseConnection databaseConnection;

    public ScanSessionDAO() {
        this(new DatabaseConnection());
    }

    public ScanSessionDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public void save(ScanSession session) {
        try (Connection connection = databaseConnection.getConnection()) {
            if (existsSession(connection, session.getId())) {
                updateSessionState(session);
                return;
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO scan_sessions (id, started_at, box_id, profile_name, selected_barcode_behavior, last_status, created_by_user_id) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                statement.setString(1, session.getId().toString());
                statement.setTimestamp(2, Timestamp.from(session.getStartedAt()));
                statement.setString(3, session.getBox().getId().toString());
                statement.setString(4, session.getProfileName());
                statement.setString(5, session.getSelectedBarcodeBehavior());
                statement.setString(6, session.getLastStatus());
                if (easv.bll.UserSession.getCurrentUser() == null) {
                    statement.setNull(7, java.sql.Types.INTEGER);
                } else {
                    statement.setInt(7, easv.bll.UserSession.getCurrentUser().getId());
                }
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to store scan session " + session.getId(), e);
        }
    }

    public void linkDocument(ScanSession session, Document document) {
        try (Connection connection = databaseConnection.getConnection()) {
            if (existsSessionDocumentLink(connection, session.getId(), document.getId())) {
                return;
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO scan_session_documents (session_id, document_id) VALUES (?, ?)")) {
                statement.setString(1, session.getId().toString());
                statement.setString(2, document.getId().toString());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to link document to session " + session.getId(), e);
        }
    }

    public void recordFailure(ScanSession session, String message) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO session_failures (id, session_id, message) VALUES (?, ?, ?)")) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, session.getId().toString());
            statement.setString(3, message);
            statement.executeUpdate();
            updateSessionState(session);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to record session failure for " + session.getId(), e);
        }
    }

    public void updateSessionState(ScanSession session) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE scan_sessions
                     SET profile_name = ?, selected_barcode_behavior = ?, last_status = ?
                     WHERE id = ?
                     """)) {
            statement.setString(1, session.getProfileName());
            statement.setString(2, session.getSelectedBarcodeBehavior());
            statement.setString(3, session.getLastStatus());
            statement.setString(4, session.getId().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update scan session state for " + session.getId(), e);
        }
    }

    public List<ScanSessionSummary> findHistorySummaries() {
        return findHistorySummariesForUser(null);
    }

    public List<ScanSessionSummary> findHistorySummariesForUser(Integer userId) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT s.id,
                            s.started_at,
                            COALESCE(NULLIF(s.profile_name, ''), 'Unspecified Profile') AS profile_name,
                            COALESCE(NULLIF(s.last_status, ''), 'READY') AS last_status,
                            s.created_by_user_id,
                            b.box_id AS box_id,
                            COUNT(DISTINCT ssd.document_id) AS document_count,
                            COUNT(dp.id) AS page_count
                     FROM scan_sessions s
                     JOIN boxes b ON b.id = s.box_id
                     LEFT JOIN scan_session_documents ssd ON ssd.session_id = s.id
                     LEFT JOIN document_pages dp ON dp.document_id = ssd.document_id
                     WHERE (? IS NULL OR s.created_by_user_id = ?)
                     GROUP BY s.id, s.started_at, s.profile_name, s.last_status, s.created_by_user_id, b.box_id
                     ORDER BY s.started_at DESC
                     """)) {
            if (userId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
                statement.setNull(2, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, userId);
                statement.setInt(2, userId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
            List<ScanSessionSummary> summaries = new ArrayList<>();
            while (resultSet.next()) {
                summaries.add(new ScanSessionSummary(
                        UUID.fromString(resultSet.getString("id")),
                        resultSet.getTimestamp("started_at").toInstant(),
                        resultSet.getString("box_id"),
                        resultSet.getString("profile_name"),
                        normalizeStatus(resultSet.getString("last_status")),
                        resultSet.getInt("document_count"),
                        resultSet.getInt("page_count")
                ));
            }
            return summaries;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch scan session history", e);
        }
    }

    public java.util.Optional<StoredScanSession> findSession(UUID sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT s.id,
                            s.started_at,
                            s.profile_name,
                            s.selected_barcode_behavior,
                            s.last_status,
                            s.created_by_user_id,
                            b.box_id
                     FROM scan_sessions s
                     JOIN boxes b ON b.id = s.box_id
                     WHERE s.id = ?
                     """)) {
            statement.setString(1, sessionId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return java.util.Optional.empty();
                }
                return java.util.Optional.of(new StoredScanSession(
                        UUID.fromString(resultSet.getString("id")),
                        resultSet.getTimestamp("started_at").toInstant(),
                        resultSet.getString("profile_name"),
                        resultSet.getString("selected_barcode_behavior"),
                        resultSet.getString("last_status"),
                        resultSet.getString("box_id"),
                        nullableInteger(resultSet, "created_by_user_id")
                ));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch stored scan session " + sessionId, e);
        }
    }

    public java.util.Optional<StoredScanSession> findLatestSession(String boxId, String profileName) {
        if (boxId == null || boxId.isBlank()) {
            throw new IllegalArgumentException("boxId must not be blank");
        }
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT TOP 1 s.id,
                                  s.started_at,
                                  s.profile_name,
                                  s.selected_barcode_behavior,
                                  s.last_status,
                                  s.created_by_user_id
                     FROM scan_sessions s
                     JOIN boxes b ON b.id = s.box_id
                     WHERE b.box_id = ?
                       AND LOWER(COALESCE(s.profile_name, '')) = LOWER(COALESCE(?, ''))
                     ORDER BY s.started_at DESC
                     """)) {
            statement.setString(1, boxId);
            statement.setString(2, profileName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return java.util.Optional.empty();
                }
                return java.util.Optional.of(new StoredScanSession(
                        UUID.fromString(resultSet.getString("id")),
                        resultSet.getTimestamp("started_at").toInstant(),
                        resultSet.getString("profile_name"),
                        resultSet.getString("selected_barcode_behavior"),
                        resultSet.getString("last_status"),
                        boxId,
                        nullableInteger(resultSet, "created_by_user_id")
                ));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch stored scan session for box " + boxId, e);
        }
    }

    public Integer findCreatorUserId(UUID sessionId) {
        if (sessionId == null) {
            return null;
        }
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT created_by_user_id
                     FROM scan_sessions
                     WHERE id = ?
                     """)) {
            statement.setString(1, sessionId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return nullableInteger(resultSet, "created_by_user_id");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find session creator.", e);
        }
    }

    private boolean existsSession(Connection connection, UUID sessionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM scan_sessions WHERE id = ?")) {
            statement.setString(1, sessionId.toString());
            return statement.executeQuery().next();
        }
    }

    private boolean existsSessionDocumentLink(Connection connection, UUID sessionId, UUID documentId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM scan_session_documents WHERE session_id = ? AND document_id = ?")) {
            statement.setString(1, sessionId.toString());
            statement.setString(2, documentId.toString());
            return statement.executeQuery().next();
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Processing";
        }
        return switch (status.trim().toUpperCase()) {
            case "IMPORTED", "NO_MORE_FILES" -> "Completed";
            case "STOPPED_ON_BARCODE" -> "Processing";
            case "FAILED", "FETCH_FAILED" -> "Failed";
            default -> "Processing";
        };
    }

    private Integer nullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    public record ScanSessionSummary(
            UUID sessionId,
            Instant startedAt,
            String boxId,
            String profileName,
            String status,
            int documentCount,
            int pageCount
    ) {
    }

    public record StoredScanSession(
            UUID sessionId,
            Instant startedAt,
            String profileName,
            String selectedBarcodeBehavior,
            String lastStatus,
            String boxId,
            Integer createdByUserId
    ) {
    }
}
