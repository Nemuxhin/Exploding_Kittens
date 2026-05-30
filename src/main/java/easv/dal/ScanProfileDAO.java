package easv.dal;

import easv.be.ScanProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ScanProfileDAO {

    private final DatabaseConnection databaseConnection;
    private volatile boolean defaultProfilesChecked = false;

    public ScanProfileDAO() {
        this(new DatabaseConnection());
    }

    public ScanProfileDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public List<ScanProfile> findAll() {
        ensureDefaultProfilesIfNeeded();
        return loadProfiles();
    }

    public Optional<ScanProfile> findById(int profileId) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, name, %s code, description, status, metadata_template_name, export_naming,
                            last_updated, archived, barcode_splitting, barcode_detected_behavior,
                            barcode_page_behavior, default_rotation, brightness, contrast, deskew,
                            export_format, metadata_required_before_export%s
                     FROM %s
                     WHERE id = ?
                     """.formatted(selectClientColumn(connection), selectAutosaveColumns(connection), profileTable(connection)))) {
            statement.setInt(1, profileId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapProfile(resultSet, hasClientColumn(connection), hasAutosaveColumns(connection)));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch profile " + profileId, e);
        }
    }

    public Optional<ScanProfile> findByName(String profileName) {
        if (profileName == null || profileName.isBlank()) {
            return Optional.empty();
        }
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, name, %s code, description, status, metadata_template_name, export_naming,
                            last_updated, archived, barcode_splitting, barcode_detected_behavior,
                            barcode_page_behavior, default_rotation, brightness, contrast, deskew,
                            export_format, metadata_required_before_export%s
                     FROM %s
                     WHERE LOWER(name) = LOWER(?)
                     """.formatted(selectClientColumn(connection), selectAutosaveColumns(connection), profileTable(connection)))) {
            statement.setString(1, profileName.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapProfile(resultSet, hasClientColumn(connection), hasAutosaveColumns(connection)));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch profile " + profileName, e);
        }
    }

    public ScanProfile save(ScanProfile profile) {
        if (profile.getId() > 0) {
            return update(profile);
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO %s (
                        name, %s code, description, status, metadata_template_name, export_naming,
                        last_updated, archived, barcode_splitting, barcode_detected_behavior,
                        barcode_page_behavior, default_rotation, brightness, contrast, deskew,
                        export_format, metadata_required_before_export%s
                     ) VALUES (%s)
                     """.formatted(
                     profileTable(connection),
                     insertClientColumn(connection),
                     insertAutosaveColumns(connection),
                     insertProfilePlaceholders(connection)
             ), PreparedStatement.RETURN_GENERATED_KEYS)) {
            bindProfile(statement, profile, hasClientColumn(connection), hasAutosaveColumns(connection));
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("No profile ID returned from insert.");
                }
                return findById(generatedKeys.getInt(1)).orElseThrow();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create profile " + profile.getName(), e);
        }
    }

    public ScanProfile update(ScanProfile profile) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE %s
                     SET name = ?, %s code = ?, description = ?, status = ?, metadata_template_name = ?,
                         export_naming = ?, last_updated = ?, archived = ?, barcode_splitting = ?,
                         barcode_detected_behavior = ?, barcode_page_behavior = ?, default_rotation = ?,
                         brightness = ?, contrast = ?, deskew = ?, export_format = ?,
                         metadata_required_before_export = ?%s
                     WHERE id = ?
                     """.formatted(profileTable(connection), updateClientColumn(connection), updateAutosaveColumns(connection)))) {
            int nextIndex = bindProfile(statement, profile, hasClientColumn(connection), hasAutosaveColumns(connection));
            statement.setInt(nextIndex, profile.getId());
            statement.executeUpdate();
            return findById(profile.getId()).orElseThrow();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update profile " + profile.getName(), e);
        }
    }

    private int bindProfile(PreparedStatement statement, ScanProfile profile, boolean includeClient, boolean includeAutosave) throws SQLException {
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

    private ScanProfile mapProfile(ResultSet resultSet, boolean includeClient, boolean includeAutosave) throws SQLException {
        return new ScanProfile(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                includeClient ? resultSet.getString("client") : "",
                resultSet.getString("code"),
                resultSet.getString("description"),
                resultSet.getString("status"),
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

    private void ensureDefaultProfiles() {
        if (!loadProfiles().isEmpty()) {
            return;
        }

        save(new ScanProfile(0, "Building Archive", "building-archive", "Used for municipal building archive scans.",
                "Active", "", "{profileCode}_{boxId}", "Created just now", false, true,
                "Continue scanning and split automatically", "Remove barcode page from final document",
                "0 deg", "Normal", "Normal", true, "PDF", true));
        save(new ScanProfile(0, "Technical Drawings", "technical-drawings", "Large format technical drawing imports.",
                "Draft", "", "{profileCode}_{boxId}", "Created just now", false, false,
                "Start new document", "Keep barcode page in final document",
                "0 deg", "Normal", "Higher", true, "PDF/A", true));
    }

    private void ensureDefaultProfilesIfNeeded() {
        if (defaultProfilesChecked) {
            return;
        }

        synchronized (this) {
            if (defaultProfilesChecked) {
                return;
            }

            ensureDefaultProfiles();
            defaultProfilesChecked = true;
        }
    }

    private List<ScanProfile> loadProfiles() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, name, %s code, description, status, metadata_template_name, export_naming,
                            last_updated, archived, barcode_splitting, barcode_detected_behavior,
                            barcode_page_behavior, default_rotation, brightness, contrast, deskew,
                            export_format, metadata_required_before_export%s
                     FROM %s
                     ORDER BY id
                     """.formatted(selectClientColumn(connection), selectAutosaveColumns(connection), profileTable(connection)));
             ResultSet resultSet = statement.executeQuery()) {
            List<ScanProfile> profiles = new ArrayList<>();
            boolean includeClient = hasClientColumn(connection);
            boolean includeAutosave = hasAutosaveColumns(connection);
            while (resultSet.next()) {
                profiles.add(mapProfile(resultSet, includeClient, includeAutosave));
            }
            return profiles;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch scan profiles", e);
        }
    }

    private String profileTable(Connection connection) throws SQLException {
        return DatabaseConnection.tableExists(connection, "scan_profiles") ? "scan_profiles" : "profiles";
    }

    private boolean hasClientColumn(Connection connection) throws SQLException {
        return DatabaseConnection.columnExists(connection, profileTable(connection), "client");
    }

    private boolean hasAutosaveColumns(Connection connection) throws SQLException {
        // Treat the trio as one feature - either all three columns exist or none.
        return DatabaseConnection.columnExists(connection, profileTable(connection), "autosave_enabled")
                && DatabaseConnection.columnExists(connection, profileTable(connection), "autosave_interval_seconds")
                && DatabaseConnection.columnExists(connection, profileTable(connection), "autosave_locked");
    }

    private String selectClientColumn(Connection connection) throws SQLException {
        return hasClientColumn(connection) ? "client, " : "";
    }

    private String selectAutosaveColumns(Connection connection) throws SQLException {
        return hasAutosaveColumns(connection)
                ? ", autosave_enabled, autosave_interval_seconds, autosave_locked"
                : "";
    }

    private String insertClientColumn(Connection connection) throws SQLException {
        return hasClientColumn(connection) ? "client, " : "";
    }

    private String insertAutosaveColumns(Connection connection) throws SQLException {
        return hasAutosaveColumns(connection)
                ? ", autosave_enabled, autosave_interval_seconds, autosave_locked"
                : "";
    }

    private String updateClientColumn(Connection connection) throws SQLException {
        return hasClientColumn(connection) ? "client = ?, " : "";
    }

    private String updateAutosaveColumns(Connection connection) throws SQLException {
        return hasAutosaveColumns(connection)
                ? ", autosave_enabled = ?, autosave_interval_seconds = ?, autosave_locked = ?"
                : "";
    }

    private String insertProfilePlaceholders(Connection connection) throws SQLException {
        StringBuilder placeholders = new StringBuilder(hasClientColumn(connection)
                ? "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
                : "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?");
        if (hasAutosaveColumns(connection)) {
            placeholders.append(", ?, ?, ?");
        }
        return placeholders.toString();
    }
}
