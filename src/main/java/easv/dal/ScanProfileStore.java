package easv.dal;

import easv.be.ScanProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class ScanProfileStore {
    private static final String PROFILE_SELECT = """
            SELECT id,
                   name,
                   client,
                   code,
                   description,
                   status,
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
                   metadata_required_before_export
            FROM scan_profiles
            """;

    private final DatabaseConnection databaseConnection;

    ScanProfileStore(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    List<ScanProfile> listAll() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(PROFILE_SELECT + " ORDER BY id");
             ResultSet resultSet = statement.executeQuery()) {
            List<ScanProfile> profiles = new ArrayList<>();
            while (resultSet.next()) {
                profiles.add(mapProfile(resultSet));
            }
            return profiles;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch scan profiles", exception);
        }
    }

    Optional<ScanProfile> findById(int profileId) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(PROFILE_SELECT + " WHERE id = ?")) {
            statement.setInt(1, profileId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapProfile(resultSet));
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
             PreparedStatement statement = connection.prepareStatement(PROFILE_SELECT + " WHERE LOWER(name) = LOWER(?)")) {
            statement.setString(1, profileName.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapProfile(resultSet));
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to fetch profile " + profileName, exception);
        }
    }

    ScanProfile insert(ScanProfile profile) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO scan_profiles (
                        name, client, code, description, status, export_naming,
                        last_updated, archived, barcode_splitting, barcode_detected_behavior,
                        barcode_page_behavior, default_rotation, brightness, contrast, deskew,
                        export_format, metadata_required_before_export
                     ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """, PreparedStatement.RETURN_GENERATED_KEYS)) {
            bindProfile(statement, profile);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("No profile ID returned from insert.");
                }
                return findById(generatedKeys.getInt(1)).orElseThrow();
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to create profile " + profile.getName(), exception);
        }
    }

    ScanProfile update(ScanProfile profile) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE scan_profiles
                     SET name = ?,
                         client = ?,
                         code = ?,
                         description = ?,
                         status = ?,
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
                         metadata_required_before_export = ?
                     WHERE id = ?
                     """)) {
            int nextIndex = bindProfile(statement, profile);
            statement.setInt(nextIndex, profile.getId());
            statement.executeUpdate();
            return findById(profile.getId()).orElseThrow();
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
             PreparedStatement statement = connection.prepareStatement("SELECT COALESCE(MAX(id), 0) + 1 FROM scan_profiles");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to calculate next profile id.", exception);
        }
    }

    private int bindProfile(PreparedStatement statement, ScanProfile profile) throws SQLException {
        int index = 1;
        statement.setString(index++, profile.getName());
        statement.setString(index++, profile.getClient());
        statement.setString(index++, profile.getCode());
        statement.setString(index++, profile.getDescription());
        statement.setString(index++, profile.getStatus());
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
        return index;
    }

    private ScanProfile mapProfile(ResultSet resultSet) throws SQLException {
        return new ScanProfile(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("client"),
                resultSet.getString("code"),
                resultSet.getString("description"),
                resultSet.getString("status"),
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

    private void deleteProfileReferences(Connection connection, int profileId) throws SQLException {
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
}
