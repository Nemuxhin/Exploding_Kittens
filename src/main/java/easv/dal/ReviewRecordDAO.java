package easv.dal;

import easv.be.ReviewRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReviewRecordDAO {
    private final DatabaseConnection databaseConnection;

    public ReviewRecordDAO() {
        this(new DatabaseConnection());
    }

    public ReviewRecordDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection == null ? new DatabaseConnection() : databaseConnection;
    }

    public List<ReviewRecord> getReviewRecords() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id,
                            identity_value,
                            client_name,
                            archive_name,
                            profile_name,
                            metadata_status,
                            qa_status,
                            pages,
                            last_updated,
                            assigned_to,
                            scanned_by,
                            date_group,
                            warning
                     FROM metadata_review_records
                     ORDER BY updated_at DESC, created_at DESC, id
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            List<ReviewRecord> records = new ArrayList<>();

            while (resultSet.next()) {
                records.add(new ReviewRecord(
                        resultSet.getString("id"),
                        resultSet.getString("identity_value"),
                        resultSet.getString("client_name"),
                        resultSet.getString("archive_name"),
                        resultSet.getString("profile_name"),
                        resultSet.getString("metadata_status"),
                        resultSet.getString("qa_status"),
                        resultSet.getInt("pages"),
                        resultSet.getString("last_updated"),
                        resultSet.getString("assigned_to"),
                        resultSet.getString("scanned_by"),
                        resultSet.getString("date_group"),
                        resultSet.getBoolean("warning")
                ));
            }

            return records;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to read review records.", exception);
        }
    }

    public void saveReviewRecord(ReviewRecord record) {
        try (Connection connection = databaseConnection.getConnection()) {
            if (reviewRecordExists(connection, record.getId())) {
                updateReviewRecord(connection, record);
            } else {
                insertReviewRecord(connection, record);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to save review record " + record.getId(), exception);
        }
    }

    private boolean reviewRecordExists(Connection connection, String recordId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                FROM metadata_review_records
                WHERE id = ?
                """)) {
            statement.setString(1, recordId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void insertReviewRecord(Connection connection, ReviewRecord record) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO metadata_review_records
                (id, identity_value, client_name, archive_name, profile_name,
                 metadata_status, qa_status, pages, last_updated, assigned_to, scanned_by,
                 date_group, warning, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """)) {
            setReviewRecordValues(statement, record);
            statement.executeUpdate();
        }
    }

    private void updateReviewRecord(Connection connection, ReviewRecord record) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE metadata_review_records
                SET identity_value = ?,
                    client_name = ?,
                    archive_name = ?,
                    profile_name = ?,
                    metadata_status = ?,
                    qa_status = ?,
                    pages = ?,
                    last_updated = ?,
                    assigned_to = ?,
                    scanned_by = ?,
                    date_group = ?,
                    warning = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """)) {
            statement.setString(1, record.getIdentity());
            statement.setString(2, record.getClient());
            statement.setString(3, record.getArchive());
            statement.setString(4, record.getProfile());
            statement.setString(5, record.getMetadataStatus());
            statement.setString(6, record.getQaStatus());
            statement.setInt(7, record.getPages());
            statement.setString(8, record.getLastUpdated());
            statement.setString(9, record.getAssignedTo());
            statement.setString(10, record.getScannedBy());
            statement.setString(11, record.getDateGroup());
            statement.setBoolean(12, record.hasWarning());
            statement.setString(13, record.getId());
            statement.executeUpdate();
        }
    }

    private void setReviewRecordValues(PreparedStatement statement, ReviewRecord record) throws SQLException {
        statement.setString(1, record.getId());
        statement.setString(2, record.getIdentity());
        statement.setString(3, record.getClient());
        statement.setString(4, record.getArchive());
        statement.setString(5, record.getProfile());
        statement.setString(6, record.getMetadataStatus());
        statement.setString(7, record.getQaStatus());
        statement.setInt(8, record.getPages());
        statement.setString(9, record.getLastUpdated());
        statement.setString(10, record.getAssignedTo());
        statement.setString(11, record.getScannedBy());
        statement.setString(12, record.getDateGroup());
        statement.setBoolean(13, record.hasWarning());
    }
}
