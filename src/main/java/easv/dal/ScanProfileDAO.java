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
                     SELECT id, name, code, description, status, metadata_template_name, export_naming,
                            last_updated, archived, barcode_splitting, barcode_detected_behavior,
                            barcode_page_behavior, default_rotation, brightness, contrast, deskew,
                            export_format, metadata_required_before_export
                     FROM %s
                     WHERE id = ?
                     """.formatted(profileTable(connection)))) {
            statement.setInt(1, profileId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapProfile(resultSet));
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
                     SELECT id, name, code, description, status, metadata_template_name, export_naming,
                            last_updated, archived, barcode_splitting, barcode_detected_behavior,
                            barcode_page_behavior, default_rotation, brightness, contrast, deskew,
                            export_format, metadata_required_before_export
                     FROM %s
                     WHERE LOWER(name) = LOWER(?)
                     """.formatted(profileTable(connection)))) {
            statement.setString(1, profileName.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapProfile(resultSet));
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
                        name, code, description, status, metadata_template_name, export_naming,
                        last_updated, archived, barcode_splitting, barcode_detected_behavior,
                        barcode_page_behavior, default_rotation, brightness, contrast, deskew,
                        export_format, metadata_required_before_export
                     ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """.formatted(profileTable(connection)), PreparedStatement.RETURN_GENERATED_KEYS)) {
            bindProfile(statement, profile);
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
                     SET name = ?, code = ?, description = ?, status = ?, metadata_template_name = ?,
                         export_naming = ?, last_updated = ?, archived = ?, barcode_splitting = ?,
                         barcode_detected_behavior = ?, barcode_page_behavior = ?, default_rotation = ?,
                         brightness = ?, contrast = ?, deskew = ?, export_format = ?,
                         metadata_required_before_export = ?
                     WHERE id = ?
                     """.formatted(profileTable(connection)))) {
            bindProfile(statement, profile);
            statement.setInt(18, profile.getId());
            statement.executeUpdate();
            return findById(profile.getId()).orElseThrow();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update profile " + profile.getName(), e);
        }
    }

    private void bindProfile(PreparedStatement statement, ScanProfile profile) throws SQLException {
        statement.setString(1, profile.getName());
        statement.setString(2, profile.getCode());
        statement.setString(3, profile.getDescription());
        statement.setString(4, profile.getStatus());
        statement.setString(5, profile.getMetadataTemplateName());
        statement.setString(6, profile.getExportNaming());
        statement.setString(7, profile.getLastUpdated());
        statement.setBoolean(8, profile.isArchived());
        statement.setBoolean(9, profile.isBarcodeSplitting());
        statement.setString(10, profile.getBarcodeDetectedBehavior());
        statement.setString(11, profile.getBarcodePageBehavior());
        statement.setString(12, profile.getDefaultRotation());
        statement.setString(13, profile.getBrightness());
        statement.setString(14, profile.getContrast());
        statement.setBoolean(15, profile.isDeskew());
        statement.setString(16, profile.getExportFormat());
        statement.setBoolean(17, profile.isMetadataRequiredBeforeExport());
    }

    private ScanProfile mapProfile(ResultSet resultSet) throws SQLException {
        return new ScanProfile(
                resultSet.getInt("id"),
                resultSet.getString("name"),
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
                resultSet.getBoolean("metadata_required_before_export")
        );
    }

    private void ensureDefaultProfiles() {
        if (!loadProfiles().isEmpty()) {
            return;
        }

        save(new ScanProfile(0, "Building Archive", "building-archive", "Used for municipal building archive scans.",
                "Active", "", ScanProfile.DEFAULT_EXPORT_NAMING, "Created just now", false, true,
                "Continue scanning and split automatically", "Remove barcode page from final document",
                "0 deg", "Normal", "Normal", true, "Multi-page TIFF", true));
        save(new ScanProfile(0, "Technical Drawings", "technical-drawings", "Large format technical drawing imports.",
                "Draft", "", ScanProfile.DEFAULT_EXPORT_NAMING, "Created just now", false, false,
                "Start new document", "Keep barcode page in final document",
                "0 deg", "Normal", "Higher", true, "Single-page TIFF", true));
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
                     SELECT id, name, code, description, status, metadata_template_name, export_naming,
                            last_updated, archived, barcode_splitting, barcode_detected_behavior,
                            barcode_page_behavior, default_rotation, brightness, contrast, deskew,
                            export_format, metadata_required_before_export
                     FROM %s
                     ORDER BY id
                     """.formatted(profileTable(connection)));
             ResultSet resultSet = statement.executeQuery()) {
            List<ScanProfile> profiles = new ArrayList<>();
            while (resultSet.next()) {
                profiles.add(mapProfile(resultSet));
            }
            return profiles;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch scan profiles", e);
        }
    }

    private String profileTable(Connection connection) throws SQLException {
        return DatabaseConnection.tableExists(connection, "scan_profiles") ? "scan_profiles" : "profiles";
    }
}
