package easv.dal;

import easv.be.Document;
import easv.be.ScanSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
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
                    "INSERT INTO scan_sessions (id, started_at, box_id, selected_barcode_behavior, last_status) VALUES (?, ?, ?, ?, ?)")) {
                statement.setString(1, session.getId().toString());
                statement.setTimestamp(2, Timestamp.from(session.getStartedAt()));
                statement.setString(3, session.getBox().getId().toString());
                statement.setString(4, session.getSelectedBarcodeBehavior());
                statement.setString(5, session.getLastStatus());
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
                     SET selected_barcode_behavior = ?, last_status = ?
                     WHERE id = ?
                     """)) {
            statement.setString(1, session.getSelectedBarcodeBehavior());
            statement.setString(2, session.getLastStatus());
            statement.setString(3, session.getId().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update scan session state for " + session.getId(), e);
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
}
