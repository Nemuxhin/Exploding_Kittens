package easv.dal;

import easv.be.ScanProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

final class ScanProfileStore {
    private final DatabaseConnection databaseConnection;

    ScanProfileStore(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection == null ? new DatabaseConnection() : databaseConnection;
    }

    List<ScanProfile> listAll() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(profileSelectSql(connection) + " ORDER BY id");
             ResultSet resultSet = statement.executeQuery()) {
            List<ScanProfile> profiles = new ArrayList<>();
            boolean includeClient = hasClientColumn(connection);
            boolean includeMetadataTemplate = hasMetadataTemplateColumn(connection);
            boolean includeAutosave = hasAutosaveColumns(connection);
            while (resultSet.next()) {
                profiles.add(mapProfile(resultSet, includeClient, includeMetadataTemplate, includeAutosave));
            }
            return profiles;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch scan profiles", exception);
        }
    }

    Optional<ScanProfile> findById(int profileId) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(profileSelectSql(connection) + " WHERE id = ?")) {
            statement.setInt(1, profileId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapProfile(
                        resultSet,
                        hasClientColumn(connection),
                        hasMetadataTemplateColumn(connection),
                        hasAutosaveColumns(connection)
                ));
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch profile " + profileId, exception);
        }
    }

    Optional<ScanProfile> findByName(String profileName) {
        if (profileName == null || profileName.isBlank()) {
            return Optional.empty();
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(profileSelectSql(connection) + " WHERE LOWER(name) = LOWER(?)")) {
            statement.setString(1, profileName.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapProfile(
                        resultSet,
                        hasClientColumn(connection),
                        hasMetadataTemplateColumn(connection),
                        hasAutosaveColumns(connection)
                ));
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch profile " + profileName, exception);
        }
    }

    ScanProfile insert(ScanProfile profile) {
        try (Connection connection = databaseConnection.getConnection()) {
            boolean includeClient = hasClientColumn(connection);
            boolean includeMetadataTemplate = hasMetadataTemplateColumn(connection);
            boolean includeAutosave = hasAutosaveColumns(connection);
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO %s (
                       name, %s code, description, status, %s export_naming,
                       last_updated, archived, barcode_splitting, barcode_detected_behavior,
                       barcode_page_behavior, default_rotation, brightness, contrast, deskew,
                       export_format, metadata_required_before_export%s
                    ) VALUES (%s)
                    """.formatted(
                    profileTable(connection),
                    insertClientColumn(includeClient),
                    insertMetadataTemplateColumn(includeMetadataTemplate),
                    insertAutosaveColumns(includeAutosave),
                    placeholders(profileValueCount(includeClient, includeMetadataTemplate, includeAutosave))
            ), PreparedStatement.RETURN_GENERATED_KEYS)) {
                bindProfile(statement, profile, includeClient, includeMetadataTemplate, includeAutosave);
                statement.executeUpdate();
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        throw new SQLException("No profile ID returned from insert.");
                    }
                    return findById(generatedKeys.getInt(1)).orElseThrow();
                }
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to create profile " + profile.getName(), exception);
        }
    }

    ScanProfile update(ScanProfile profile) {
        try (Connection connection = databaseConnection.getConnection()) {
            boolean includeClient = hasClientColumn(connection);
            boolean includeMetadataTemplate = hasMetadataTemplateColumn(connection);
            boolean includeAutosave = hasAutosaveColumns(connection);
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE %s
                    SET name = ?,
                        %s
                        code = ?,
                        description = ?,
                        status = ?,
                        %s
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
                        metadata_required_before_export = ?%s
                    WHERE id = ?
                    """.formatted(
                    profileTable(connection),
                    updateClientColumn(includeClient),
                    updateMetadataTemplateColumn(includeMetadataTemplate),
                    updateAutosaveColumns(includeAutosave)
            ))) {
                int nextIndex = bindProfile(statement, profile, includeClient, includeMetadataTemplate, includeAutosave);
                statement.setInt(nextIndex, profile.getId());
                statement.executeUpdate();
                return findById(profile.getId()).orElseThrow();
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to update profile " + profile.getName(), exception);
        }
    }

    void delete(int profileId) {
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

    int nextProfileId() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COALESCE(MAX(id), 0) + 1 FROM " + profileTable(connection));
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to calculate next profile id.", exception);
        }
    }

    private String profileSelectSql(Connection connection) throws SQLException {
        return """
                SELECT id,
                       name,
                       %s
                       code,
                       description,
                       status,
                       %s
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
                FROM %s
                """.formatted(
                selectClientColumn(connection),
                selectMetadataTemplateColumn(connection),
                selectAutosaveColumns(connection),
                profileTable(connection)
        );
    }

    private int bindProfile(
            PreparedStatement statement,
            ScanProfile profile,
            boolean includeClient,
            boolean includeMetadataTemplate,
            boolean includeAutosave
    ) throws SQLException {
        int index = 1;
        statement.setString(index++, profile.getName());
        if (includeClient) {
            statement.setString(index++, profile.getClient());
        }
        statement.setString(index++, profile.getCode());
        statement.setString(index++, profile.getDescription());
        statement.setString(index++, profile.getStatus());
        if (includeMetadataTemplate) {
            statement.setString(index++, profile.getMetadataTemplateName());
        }
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

    private ScanProfile mapProfile(
            ResultSet resultSet,
            boolean includeClient,
            boolean includeMetadataTemplate,
            boolean includeAutosave
    ) throws SQLException {
        return new ScanProfile(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                includeClient ? resultSet.getString("client") : "",
                resultSet.getString("code"),
                resultSet.getString("description"),
                resultSet.getString("status"),
                includeMetadataTemplate ? resultSet.getString("metadata_template_name") : "",
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

    private String profileTable(Connection connection) throws SQLException {
        return DatabaseConnection.tableExists(connection, "scan_profiles") ? "scan_profiles" : "profiles";
    }

    private boolean hasClientColumn(Connection connection) throws SQLException {
        return DatabaseConnection.columnExists(connection, profileTable(connection), "client");
    }

    private boolean hasMetadataTemplateColumn(Connection connection) throws SQLException {
        return DatabaseConnection.columnExists(connection, profileTable(connection), "metadata_template_name");
    }

    private boolean hasAutosaveColumns(Connection connection) throws SQLException {
        String table = profileTable(connection);
        return DatabaseConnection.columnExists(connection, table, "autosave_enabled")
                && DatabaseConnection.columnExists(connection, table, "autosave_interval_seconds")
                && DatabaseConnection.columnExists(connection, table, "autosave_locked");
    }

    private String selectClientColumn(Connection connection) throws SQLException {
        return hasClientColumn(connection) ? "client,\n                       " : "";
    }

    private String selectMetadataTemplateColumn(Connection connection) throws SQLException {
        return hasMetadataTemplateColumn(connection) ? "metadata_template_name,\n                       " : "";
    }

    private String selectAutosaveColumns(Connection connection) throws SQLException {
        return hasAutosaveColumns(connection)
                ? ",\n                       autosave_enabled,\n                       autosave_interval_seconds,\n                       autosave_locked"
                : "";
    }

    private String insertClientColumn(boolean includeClient) {
        return includeClient ? "client, " : "";
    }

    private String insertMetadataTemplateColumn(boolean includeMetadataTemplate) {
        return includeMetadataTemplate ? "metadata_template_name, " : "";
    }

    private String insertAutosaveColumns(boolean includeAutosave) {
        return includeAutosave ? ", autosave_enabled, autosave_interval_seconds, autosave_locked" : "";
    }

    private String updateClientColumn(boolean includeClient) {
        return includeClient ? "client = ?,\n                        " : "";
    }

    private String updateMetadataTemplateColumn(boolean includeMetadataTemplate) {
        return includeMetadataTemplate ? "metadata_template_name = ?,\n                        " : "";
    }

    private String updateAutosaveColumns(boolean includeAutosave) {
        return includeAutosave
                ? ",\n                        autosave_enabled = ?,\n                        autosave_interval_seconds = ?,\n                        autosave_locked = ?"
                : "";
    }

    private int profileValueCount(boolean includeClient, boolean includeMetadataTemplate, boolean includeAutosave) {
        return 16 + (includeClient ? 1 : 0) + (includeMetadataTemplate ? 1 : 0) + (includeAutosave ? 3 : 0);
    }

    private String placeholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }

    private void deleteProfileReferences(Connection connection, int profileId) throws SQLException {
        deleteProfileReferenceTable(connection, "metadata_template_profile_assignments", profileId);
        deleteProfileReferenceTable(connection, "user_profile_assignments", profileId);
    }

    private void deleteProfileReferenceTable(Connection connection, String tableName, int profileId) throws SQLException {
        if (!DatabaseConnection.tableExists(connection, tableName)) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                DELETE FROM %s
                WHERE scan_profile_id = ?
                """.formatted(tableName))) {
            statement.setInt(1, profileId);
            statement.executeUpdate();
        }
    }

    private void deleteProfileRow(Connection connection, int profileId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                DELETE FROM %s
                WHERE id = ?
                """.formatted(profileTable(connection)))) {
            statement.setInt(1, profileId);

            if (statement.executeUpdate() == 0) {
                throw new SQLException("Scan profile was not found: " + profileId);
            }
        }
    }
}
