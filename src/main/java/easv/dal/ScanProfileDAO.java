package easv.dal;

import easv.be.ScanProfile;
import easv.bll.AdminManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScanProfileDAO {
    private static final DateTimeFormatter LAST_UPDATED_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DatabaseConnection databaseConnection;

    public ScanProfileDAO() {
        this(new DatabaseConnection());
    }

    public ScanProfileDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public List<ScanProfile> findAll() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, name, code, description, status, metadata_template_name, export_naming,
                            last_updated, archived, barcode_splitting, barcode_detected_behavior,
                            barcode_page_behavior, default_rotation, brightness, contrast, deskew,
                            export_format, metadata_required_before_export
                     FROM scan_profiles
                     ORDER BY id
                     """);
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

    public ScanProfile create(AdminManager.ProfileInput input) {
        String lastUpdated = "Created " + LAST_UPDATED_FORMATTER.format(LocalDateTime.now());

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO scan_profiles (
                         name, code, description, status, metadata_template_name, export_naming, last_updated,
                         archived, barcode_splitting, barcode_detected_behavior, barcode_page_behavior,
                         default_rotation, brightness, contrast, deskew, export_format,
                         metadata_required_before_export
                     )
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """, Statement.RETURN_GENERATED_KEYS)) {
            bindProfile(statement, input, lastUpdated);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new DataAccessException("Creating profile did not return an id.");
                }
                return findById(generatedKeys.getInt(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create scan profile " + input.getCode(), e);
        }
    }

    public ScanProfile update(int profileId, AdminManager.ProfileInput input) {
        String lastUpdated = "Updated " + LAST_UPDATED_FORMATTER.format(LocalDateTime.now());

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE scan_profiles
                     SET name = ?,
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
                         metadata_required_before_export = ?,
                         updated_at = CURRENT_TIMESTAMP
                     WHERE id = ?
                     """)) {
            bindProfile(statement, input, lastUpdated);
            statement.setInt(18, profileId);

            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Profile could not be found.");
            }

            return findById(profileId);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update scan profile " + profileId, e);
        }
    }

    public void updateArchivedState(int profileId, boolean archived, String status, String lastUpdated) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE scan_profiles
                     SET archived = ?,
                         status = ?,
                         last_updated = ?,
                         updated_at = CURRENT_TIMESTAMP
                     WHERE id = ?
                     """)) {
            statement.setBoolean(1, archived);
            statement.setString(2, clean(status));
            statement.setString(3, clean(lastUpdated));
            statement.setInt(4, profileId);

            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Profile could not be found.");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update archive state for profile " + profileId, e);
        }
    }

    public boolean codeExists(String code, Integer excludedProfileId) {
        String sql = """
                SELECT 1
                FROM scan_profiles
                WHERE LOWER(code) = ?
                  AND (? IS NULL OR id <> ?)
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clean(code).toLowerCase(Locale.ROOT));
            if (excludedProfileId == null) {
                statement.setNull(2, java.sql.Types.INTEGER);
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setInt(2, excludedProfileId);
                statement.setInt(3, excludedProfileId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to check profile code uniqueness", e);
        }
    }

    private ScanProfile findById(int profileId) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, name, code, description, status, metadata_template_name, export_naming,
                            last_updated, archived, barcode_splitting, barcode_detected_behavior,
                            barcode_page_behavior, default_rotation, brightness, contrast, deskew,
                            export_format, metadata_required_before_export
                     FROM scan_profiles
                     WHERE id = ?
                     """)) {
            statement.setInt(1, profileId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Profile could not be found.");
                }
                return mapProfile(resultSet);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch scan profile " + profileId, e);
        }
    }

    private void bindProfile(PreparedStatement statement, AdminManager.ProfileInput input, String lastUpdated) throws SQLException {
        statement.setString(1, clean(input.getName()));
        statement.setString(2, clean(input.getCode()));
        statement.setString(3, clean(input.getDescription()));
        statement.setString(4, clean(input.getStatus()));
        statement.setString(5, clean(input.getMetadataTemplateName()));
        statement.setString(6, clean(input.getExportNaming()));
        statement.setString(7, lastUpdated);
        statement.setBoolean(8, "Archived".equalsIgnoreCase(input.getStatus()));
        statement.setBoolean(9, input.isBarcodeSplitting());
        statement.setString(10, clean(input.getBarcodeDetectedBehavior()));
        statement.setString(11, clean(input.getBarcodePageBehavior()));
        statement.setString(12, clean(input.getDefaultRotation()));
        statement.setString(13, clean(input.getBrightness()));
        statement.setString(14, clean(input.getContrast()));
        statement.setBoolean(15, input.isDeskew());
        statement.setString(16, clean(input.getExportFormat()));
        statement.setBoolean(17, input.isMetadataRequiredBeforeExport());
    }

    private ScanProfile mapProfile(ResultSet resultSet) throws SQLException {
        return new ScanProfile(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("code"),
                nullableToEmpty(resultSet.getString("description")),
                nullableToEmpty(resultSet.getString("status")),
                nullableToEmpty(resultSet.getString("metadata_template_name")),
                nullableToEmpty(resultSet.getString("export_naming")),
                nullableToEmpty(resultSet.getString("last_updated")),
                resultSet.getBoolean("archived"),
                resultSet.getBoolean("barcode_splitting"),
                nullableToEmpty(resultSet.getString("barcode_detected_behavior")),
                nullableToEmpty(resultSet.getString("barcode_page_behavior")),
                nullableToEmpty(resultSet.getString("default_rotation")),
                nullableToEmpty(resultSet.getString("brightness")),
                nullableToEmpty(resultSet.getString("contrast")),
                resultSet.getBoolean("deskew"),
                nullableToEmpty(resultSet.getString("export_format")),
                resultSet.getBoolean("metadata_required_before_export")
        );
    }

    private String nullableToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
