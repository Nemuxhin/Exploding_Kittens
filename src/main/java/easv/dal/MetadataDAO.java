package easv.dal;

import easv.be.ReviewRecord;
import easv.be.ScanProfile;
import easv.gui.controller.util.Strings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MetadataDAO {
    private final DatabaseConnection databaseConnection;

    public MetadataDAO() {
        this(new DatabaseConnection());
    }

    public MetadataDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection == null ? new DatabaseConnection() : databaseConnection;
    }

    public List<ScanProfile> getProfiles() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id,
                            name,
                            %s
                            code,
                            description,
                            status,
                            metadata_template_name,
                            export_naming,
                            last_updated,
                            archived,
                            barcode_splitting,
                            barcode_detected_behavior,
                            barcode_page_behavior,
                            default_rotation,
                            brightness,
                            contrast,
                            deskew,
                            export_format,
                            metadata_required_before_export%s
                     FROM scan_profiles
                     ORDER BY id
                     """.formatted(selectClientColumn(connection), selectAutosaveColumns(connection)));
             ResultSet resultSet = statement.executeQuery()) {
            List<ScanProfile> profiles = new ArrayList<>();
            boolean includeClient = hasProfileClientColumn(connection);
            boolean includeAutosave = hasAutosaveColumns(connection);

            while (resultSet.next()) {
                profiles.add(readScanProfile(resultSet, includeClient, includeAutosave));
            }

            return profiles;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to read scan profiles.", exception);
        }
    }

    public int nextProfileId() {
        return nextId("scan_profiles", "profile");
    }

    public ScanProfile saveProfile(ScanProfile profile) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO scan_profiles
                     (name, %scode, description, status, metadata_template_name, export_naming,
                      last_updated, archived, barcode_splitting, barcode_detected_behavior,
                      barcode_page_behavior, default_rotation, brightness, contrast, deskew,
                      export_format, metadata_required_before_export%s, created_at, updated_at)
                     VALUES (%s, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                     """.formatted(
                     insertClientColumn(connection),
                     insertAutosaveColumns(connection),
                     insertProfileValuePlaceholders(connection)
             ), Statement.RETURN_GENERATED_KEYS)) {
            setProfileValues(statement, profile, hasProfileClientColumn(connection), hasAutosaveColumns(connection));
            statement.executeUpdate();
            return copyProfileWithId(readGeneratedIntId(statement, "scan profile"), profile);
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to save scan profile " + profile.getName(), exception);
        }
    }

    public void updateProfile(ScanProfile profile) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE scan_profiles
                     SET name = ?,
                         %s
                         code = ?,
                         description = ?,
                         status = ?,
                         metadata_template_name = ?,
                         export_naming = ?,
                         last_updated = ?,
                         archived = ?,
                         barcode_splitting = ?,
                         barcode_detected_behavior = ?,
                         barcode_page_behavior = ?,
                         default_rotation = ?,
                         brightness = ?,
                         contrast = ?,
                         deskew = ?,
                         export_format = ?,
                         metadata_required_before_export = ?%s,
                         updated_at = CURRENT_TIMESTAMP
                     WHERE id = ?
                     """.formatted(updateClientColumn(connection), updateAutosaveColumns(connection)))) {
            int nextIndex = setProfileValues(statement, profile, hasProfileClientColumn(connection), hasAutosaveColumns(connection));
            statement.setInt(nextIndex, profile.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to update scan profile " + profile.getName(), exception);
        }
    }

    public void deleteProfile(int profileId) {
        try (Connection connection = databaseConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                deleteProfileReferences(connection, profileId);
                deleteProfileRow(connection, profileId);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw new DataAccessException(
                    "Failed to delete scan profile. Archive it if scanned data still uses it.",
                    exception
            );
        }
    }

    public List<ReviewRecord> getReviewRecords() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id,
                            identity_value,
                            client_name,
                            archive_name,
                            profile_name,
                            metadata_template_name,
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
                        resultSet.getString("metadata_template_name"),
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

    private ScanProfile readScanProfile(ResultSet resultSet, boolean includeClient, boolean includeAutosave) throws SQLException {
        return new ScanProfile(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                includeClient ? resultSet.getString("client") : "",
                resultSet.getString("code"),
                resultSet.getString("description"),
                displayStatus(resultSet.getString("status")),
                resultSet.getString("metadata_template_name"),
                resultSet.getString("export_naming"),
                resultSet.getString("last_updated"),
                resultSet.getBoolean("archived"),
                resultSet.getBoolean("barcode_splitting"),
                resultSet.getString("barcode_detected_behavior"),
                resultSet.getString("barcode_page_behavior"),
                resultSet.getString("default_rotation"),
                resultSet.getString("brightness"),
                resultSet.getString("contrast"),
                resultSet.getBoolean("deskew"),
                resultSet.getString("export_format"),
                resultSet.getBoolean("metadata_required_before_export"),
                includeAutosave ? resultSet.getBoolean("autosave_enabled") : true,
                includeAutosave ? resultSet.getInt("autosave_interval_seconds") : ScanProfile.DEFAULT_AUTOSAVE_INTERVAL_SECONDS,
                includeAutosave && resultSet.getBoolean("autosave_locked")
        );
    }

    private int setProfileValues(PreparedStatement statement, ScanProfile profile, boolean includeClient, boolean includeAutosave) throws SQLException {
        int index = 1;
        statement.setString(index++, profile.getName());
        if (includeClient) {
            statement.setString(index++, profile.getClient());
        }
        statement.setString(index++, profile.getCode());
        statement.setString(index++, profile.getDescription());
        statement.setString(index++, profile.getStatus());
        statement.setString(index++, profile.getMetadataTemplateName());
        statement.setString(index++, profile.getExportNaming());
        statement.setString(index++, profile.getLastUpdated());
        statement.setBoolean(index++, profile.isArchived());
        statement.setBoolean(index++, profile.isBarcodeSplitting());
        statement.setString(index++, profile.getBarcodeDetectedBehavior());
        statement.setString(index++, profile.getBarcodePageBehavior());
        statement.setString(index++, profile.getDefaultRotation());
        statement.setString(index++, profile.getBrightness());
        statement.setString(index++, profile.getContrast());
        statement.setBoolean(index++, profile.isDeskew());
        statement.setString(index++, profile.getExportFormat());
        statement.setBoolean(index++, profile.isMetadataRequiredBeforeExport());
        if (includeAutosave) {
            statement.setBoolean(index++, profile.isAutosaveEnabled());
            statement.setInt(index++, profile.getAutosaveIntervalSeconds());
            statement.setBoolean(index++, profile.isAutosaveLocked());
        }
        return index;
    }

    private void deleteProfileReferences(Connection connection, int profileId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                DELETE FROM metadata_template_profile_assignments
                WHERE scan_profile_id = ?
                """)) {
            statement.setInt(1, profileId);
            statement.executeUpdate();
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                DELETE FROM user_profile_assignments
                WHERE scan_profile_id = ?
                """)) {
            statement.setInt(1, profileId);
            statement.executeUpdate();
        }
    }

    private void deleteProfileRow(Connection connection, int profileId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                DELETE FROM scan_profiles
                WHERE id = ?
                """)) {
            statement.setInt(1, profileId);

            if (statement.executeUpdate() == 0) {
                throw new SQLException("Scan profile was not found: " + profileId);
            }
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
                (id, identity_value, client_name, archive_name, profile_name, metadata_template_name,
                 metadata_status, qa_status, pages, last_updated, assigned_to, scanned_by,
                 date_group, warning, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
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
                    metadata_template_name = ?,
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
            statement.setString(5, record.getMetadataTemplate());
            statement.setString(6, record.getMetadataStatus());
            statement.setString(7, record.getQaStatus());
            statement.setInt(8, record.getPages());
            statement.setString(9, record.getLastUpdated());
            statement.setString(10, record.getAssignedTo());
            statement.setString(11, record.getScannedBy());
            statement.setString(12, record.getDateGroup());
            statement.setBoolean(13, record.hasWarning());
            statement.setString(14, record.getId());
            statement.executeUpdate();
        }
    }

    private void setReviewRecordValues(PreparedStatement statement, ReviewRecord record)
            throws SQLException {
        statement.setString(1, record.getId());
        statement.setString(2, record.getIdentity());
        statement.setString(3, record.getClient());
        statement.setString(4, record.getArchive());
        statement.setString(5, record.getProfile());
        statement.setString(6, record.getMetadataTemplate());
        statement.setString(7, record.getMetadataStatus());
        statement.setString(8, record.getQaStatus());
        statement.setInt(9, record.getPages());
        statement.setString(10, record.getLastUpdated());
        statement.setString(11, record.getAssignedTo());
        statement.setString(12, record.getScannedBy());
        statement.setString(13, record.getDateGroup());
        statement.setBoolean(14, record.hasWarning());
    }

    private int nextId(String tableName, String label) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COALESCE(MAX(id), 0) + 1 FROM " + tableName);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to calculate next " + label + " id.", exception);
        }
    }

    private int readGeneratedIntId(Statement statement, String label) throws SQLException {
        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            }
        }

        throw new SQLException("Database did not return a generated " + label + " id.");
    }

    private ScanProfile copyProfileWithId(int id, ScanProfile profile) {
        return new ScanProfile(
                id,
                profile.getName(),
                profile.getClient(),
                profile.getCode(),
                profile.getDescription(),
                profile.getStatus(),
                profile.getMetadataTemplateName(),
                profile.getExportNaming(),
                profile.getLastUpdated(),
                profile.isArchived(),
                profile.isBarcodeSplitting(),
                profile.getBarcodeDetectedBehavior(),
                profile.getBarcodePageBehavior(),
                profile.getDefaultRotation(),
                profile.getBrightness(),
                profile.getContrast(),
                profile.isDeskew(),
                profile.getExportFormat(),
                profile.isMetadataRequiredBeforeExport(),
                profile.isAutosaveEnabled(),
                profile.getAutosaveIntervalSeconds(),
                profile.isAutosaveLocked()
        );
    }

    private boolean hasProfileClientColumn(Connection connection) throws SQLException {
        return DatabaseConnection.columnExists(connection, "scan_profiles", "client");
    }

    private boolean hasAutosaveColumns(Connection connection) throws SQLException {
        // Treat the trio as one feature - either all three columns exist or none.
        return DatabaseConnection.columnExists(connection, "scan_profiles", "autosave_enabled")
                && DatabaseConnection.columnExists(connection, "scan_profiles", "autosave_interval_seconds")
                && DatabaseConnection.columnExists(connection, "scan_profiles", "autosave_locked");
    }

    private String selectClientColumn(Connection connection) throws SQLException {
        return hasProfileClientColumn(connection) ? "client,\n                            " : "";
    }

    private String selectAutosaveColumns(Connection connection) throws SQLException {
        return hasAutosaveColumns(connection)
                ? ",\n                            autosave_enabled,\n                            autosave_interval_seconds,\n                            autosave_locked"
                : "";
    }

    private String insertClientColumn(Connection connection) throws SQLException {
        return hasProfileClientColumn(connection) ? "client, " : "";
    }

    private String insertAutosaveColumns(Connection connection) throws SQLException {
        return hasAutosaveColumns(connection)
                ? ", autosave_enabled, autosave_interval_seconds, autosave_locked"
                : "";
    }

    private String updateClientColumn(Connection connection) throws SQLException {
        return hasProfileClientColumn(connection) ? "client = ?,\n                         " : "";
    }

    private String updateAutosaveColumns(Connection connection) throws SQLException {
        return hasAutosaveColumns(connection)
                ? ",\n                         autosave_enabled = ?,\n                         autosave_interval_seconds = ?,\n                         autosave_locked = ?"
                : "";
    }

    private String insertProfileValuePlaceholders(Connection connection) throws SQLException {
        StringBuilder placeholders = new StringBuilder(hasProfileClientColumn(connection)
                ? "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
                : "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?");
        if (hasAutosaveColumns(connection)) {
            placeholders.append(", ?, ?, ?");
        }
        return placeholders.toString();
    }

    private String displayStatus(String status) {
        String cleanedStatus = Strings.clean(status);

        if (cleanedStatus.isBlank()) {
            return "";
        }

        String lowerCase = cleanedStatus.toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lowerCase.charAt(0)) + lowerCase.substring(1);
    }
}
