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
        try (Connection connection = databaseConnection.getConnection()) {
            boolean includeMetadataTemplate = hasMetadataTemplateColumn(connection);
            try (PreparedStatement statement = connection.prepareStatement("""
                     SELECT id,
                            identity_value,
                            client_name,
                            archive_name,
                            profile_name,
                            %s
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
                     """.formatted(selectMetadataTemplateColumn(includeMetadataTemplate)));
                 ResultSet resultSet = statement.executeQuery()) {
                List<ReviewRecord> records = new ArrayList<>();

                while (resultSet.next()) {
                    records.add(new ReviewRecord(
                            resultSet.getString("id"),
                            resultSet.getString("identity_value"),
                            resultSet.getString("client_name"),
                            resultSet.getString("archive_name"),
                            resultSet.getString("profile_name"),
                            includeMetadataTemplate ? resultSet.getString("metadata_template_name") : "",
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
            }
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
        boolean includeMetadataTemplate = hasMetadataTemplateColumn(connection);
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO metadata_review_records
                (id, identity_value, client_name, archive_name, profile_name, %s
                 metadata_status, qa_status, pages, last_updated, assigned_to, scanned_by,
                 date_group, warning, created_at, updated_at)
                VALUES (%s, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """.formatted(
                insertMetadataTemplateColumn(includeMetadataTemplate),
                reviewRecordPlaceholders(includeMetadataTemplate)
        ))) {
            setReviewRecordValues(statement, record, includeMetadataTemplate);
            statement.executeUpdate();
        }
    }

    private void updateReviewRecord(Connection connection, ReviewRecord record) throws SQLException {
        boolean includeMetadataTemplate = hasMetadataTemplateColumn(connection);
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE metadata_review_records
                SET identity_value = ?,
                    client_name = ?,
                    archive_name = ?,
                    profile_name = ?,
                    %s
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
                """.formatted(updateMetadataTemplateColumn(includeMetadataTemplate)))) {
            statement.setString(1, record.getIdentity());
            statement.setString(2, record.getClient());
            statement.setString(3, record.getArchive());
            statement.setString(4, record.getProfile());
            int index = 5;
            if (includeMetadataTemplate) {
                statement.setString(index++, record.getMetadataTemplate());
            }
            statement.setString(index++, record.getMetadataStatus());
            statement.setString(index++, record.getQaStatus());
            statement.setInt(index++, record.getPages());
            statement.setString(index++, record.getLastUpdated());
            statement.setString(index++, record.getAssignedTo());
            statement.setString(index++, record.getScannedBy());
            statement.setString(index++, record.getDateGroup());
            statement.setBoolean(index++, record.hasWarning());
            statement.setString(index, record.getId());
            statement.executeUpdate();
        }
    }

    private void setReviewRecordValues(
            PreparedStatement statement,
            ReviewRecord record,
            boolean includeMetadataTemplate
    ) throws SQLException {
        int index = 1;
        statement.setString(index++, record.getId());
        statement.setString(index++, record.getIdentity());
        statement.setString(index++, record.getClient());
        statement.setString(index++, record.getArchive());
        statement.setString(index++, record.getProfile());
        if (includeMetadataTemplate) {
            statement.setString(index++, record.getMetadataTemplate());
        }
        statement.setString(index++, record.getMetadataStatus());
        statement.setString(index++, record.getQaStatus());
        statement.setInt(index++, record.getPages());
        statement.setString(index++, record.getLastUpdated());
        statement.setString(index++, record.getAssignedTo());
        statement.setString(index++, record.getScannedBy());
        statement.setString(index++, record.getDateGroup());
        statement.setBoolean(index, record.hasWarning());
    }

    private boolean hasMetadataTemplateColumn(Connection connection) throws SQLException {
        return DatabaseConnection.columnExists(connection, "metadata_review_records", "metadata_template_name");
    }

    private String selectMetadataTemplateColumn(boolean includeMetadataTemplate) {
        return includeMetadataTemplate ? "metadata_template_name,\n                            " : "";
    }

    private String insertMetadataTemplateColumn(boolean includeMetadataTemplate) {
        return includeMetadataTemplate ? "metadata_template_name, " : "";
    }

    private String updateMetadataTemplateColumn(boolean includeMetadataTemplate) {
        return includeMetadataTemplate ? "metadata_template_name = ?,\n                    " : "";
    }

    private String reviewRecordPlaceholders(boolean includeMetadataTemplate) {
        return includeMetadataTemplate
                ? "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
                : "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?";
    }
}
