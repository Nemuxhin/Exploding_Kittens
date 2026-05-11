package easv.dal;

import easv.be.MetadataField;
import easv.be.ReviewRecord;
import easv.be.MetadataTemplate;
import easv.be.ScanProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                            metadata_required_before_export
                     FROM scan_profiles
                     ORDER BY id
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            List<ScanProfile> profiles = new ArrayList<>();

            while (resultSet.next()) {
                profiles.add(readScanProfile(resultSet));
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
                     (name, code, description, status, metadata_template_name, export_naming,
                      last_updated, archived, barcode_splitting, barcode_detected_behavior,
                      barcode_page_behavior, default_rotation, brightness, contrast, deskew,
                      export_format, metadata_required_before_export, created_at, updated_at)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                     """, Statement.RETURN_GENERATED_KEYS)) {
            setProfileValues(statement, profile);
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
            statement.setInt(18, profile.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to update scan profile " + profile.getName(), exception);
        }
    }

    public List<MetadataTemplate> getMetadataTemplates() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, name, description, status, last_updated
                     FROM metadata_templates
                     ORDER BY id
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            Map<Integer, List<MetadataField>> fieldsByTemplate = loadFieldsByTemplate(connection);
            Map<Integer, List<String>> profileNamesByTemplate = loadAssignedProfileNamesByTemplate(connection);
            List<MetadataTemplate> templates = new ArrayList<>();

            while (resultSet.next()) {
                int templateId = resultSet.getInt("id");
                templates.add(new MetadataTemplate(
                        templateId,
                        resultSet.getString("name"),
                        resultSet.getString("description"),
                        profileNamesByTemplate.getOrDefault(templateId, List.of()),
                        fieldsByTemplate.getOrDefault(templateId, List.of()),
                        displayStatus(resultSet.getString("status")),
                        resultSet.getString("last_updated")
                ));
            }

            return templates;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to read metadata templates.", exception);
        }
    }

    public int nextMetadataTemplateId() {
        return nextId("metadata_templates", "metadata template");
    }

    public int nextMetadataFieldId() {
        return nextId("metadata_fields", "metadata field");
    }

    public MetadataTemplate saveMetadataTemplate(MetadataTemplate template) {
        try (Connection connection = databaseConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                int templateId = insertMetadataTemplate(connection, template);
                List<MetadataField> savedFields = insertMetadataFields(connection, templateId, template.getFields());
                MetadataTemplate savedTemplate = copyTemplateWithId(templateId, template, savedFields);
                replaceMetadataTemplateProfileAssignments(connection, savedTemplate);
                connection.commit();
                return savedTemplate;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to save metadata template " + template.getName(), exception);
        }
    }

    public MetadataTemplate updateMetadataTemplate(MetadataTemplate template) {
        try (Connection connection = databaseConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                updateMetadataTemplateRow(connection, template);
                deleteMetadataFields(connection, template.getId());
                List<MetadataField> savedFields = insertMetadataFields(connection, template.getId(), template.getFields());
                template.setFields(savedFields);
                replaceMetadataTemplateProfileAssignments(connection, template);
                connection.commit();
                return template;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to update metadata template " + template.getName(), exception);
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

    private ScanProfile readScanProfile(ResultSet resultSet) throws SQLException {
        return new ScanProfile(
                resultSet.getInt("id"),
                resultSet.getString("name"),
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
                resultSet.getBoolean("metadata_required_before_export")
        );
    }

    private void setProfileValues(PreparedStatement statement, ScanProfile profile) throws SQLException {
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

    private Map<Integer, List<MetadataField>> loadFieldsByTemplate(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, template_id, name, type, required, placeholder
                     FROM metadata_fields
                     ORDER BY template_id, sort_order, id
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            Map<Integer, List<MetadataField>> fieldsByTemplate = new HashMap<>();

            while (resultSet.next()) {
                fieldsByTemplate
                        .computeIfAbsent(resultSet.getInt("template_id"), ignored -> new ArrayList<>())
                        .add(new MetadataField(
                                resultSet.getInt("id"),
                                resultSet.getString("name"),
                                resultSet.getString("type"),
                                resultSet.getBoolean("required"),
                                resultSet.getString("placeholder")
                        ));
            }

            return fieldsByTemplate;
        }
    }

    private Map<Integer, List<String>> loadAssignedProfileNamesByTemplate(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                     SELECT mtpa.metadata_template_id, sp.name
                     FROM metadata_template_profile_assignments mtpa
                     JOIN scan_profiles sp ON sp.id = mtpa.scan_profile_id
                     ORDER BY sp.name
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            Map<Integer, List<String>> profileNamesByTemplate = new HashMap<>();

            while (resultSet.next()) {
                profileNamesByTemplate
                        .computeIfAbsent(resultSet.getInt("metadata_template_id"), ignored -> new ArrayList<>())
                        .add(resultSet.getString("name"));
            }

            return profileNamesByTemplate;
        }
    }

    private int insertMetadataTemplate(Connection connection, MetadataTemplate template) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO metadata_templates
                (name, description, status, last_updated, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, template.getName());
            statement.setString(2, template.getDescription());
            statement.setString(3, template.getStatus());
            statement.setString(4, template.getLastUpdated());
            statement.executeUpdate();
            return readGeneratedIntId(statement, "metadata template");
        }
    }

    private void updateMetadataTemplateRow(Connection connection, MetadataTemplate template) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE metadata_templates
                SET name = ?,
                    description = ?,
                    status = ?,
                    last_updated = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """)) {
            statement.setString(1, template.getName());
            statement.setString(2, template.getDescription());
            statement.setString(3, template.getStatus());
            statement.setString(4, template.getLastUpdated());
            statement.setInt(5, template.getId());
            statement.executeUpdate();
        }
    }

    private void deleteMetadataFields(Connection connection, int templateId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                DELETE FROM metadata_fields
                WHERE template_id = ?
                """)) {
            statement.setInt(1, templateId);
            statement.executeUpdate();
        }
    }

    private List<MetadataField> insertMetadataFields(Connection connection, int templateId, List<MetadataField> fields)
            throws SQLException {
        if (fields == null || fields.isEmpty()) {
            return List.of();
        }

        List<MetadataField> savedFields = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO metadata_fields
                (template_id, name, type, required, placeholder, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, Statement.RETURN_GENERATED_KEYS)) {
            for (int index = 0; index < fields.size(); index++) {
                MetadataField field = fields.get(index);
                statement.setInt(1, templateId);
                statement.setString(2, field.getName());
                statement.setString(3, field.getType());
                statement.setBoolean(4, field.isRequired());
                statement.setString(5, field.getPlaceholder());
                statement.setInt(6, index + 1);
                statement.executeUpdate();

                savedFields.add(new MetadataField(
                        readGeneratedIntId(statement, "metadata field"),
                        field.getName(),
                        field.getType(),
                        field.isRequired(),
                        field.getPlaceholder()
                ));
            }
        }

        return savedFields;
    }

    private void replaceMetadataTemplateProfileAssignments(Connection connection, MetadataTemplate template)
            throws SQLException {
        try (PreparedStatement deleteStatement = connection.prepareStatement("""
                     DELETE FROM metadata_template_profile_assignments
                     WHERE metadata_template_id = ?
                     """)) {
            deleteStatement.setInt(1, template.getId());
            deleteStatement.executeUpdate();
        }

        if (template.getAssignedProfileNames().isEmpty()) {
            return;
        }

        try (PreparedStatement insertStatement = connection.prepareStatement("""
                INSERT INTO metadata_template_profile_assignments
                (metadata_template_id, scan_profile_id)
                VALUES (?, ?)
                """)) {
            for (String profileName : template.getAssignedProfileNames()) {
                insertStatement.setInt(1, template.getId());
                insertStatement.setInt(2, findProfileIdByName(connection, profileName));
                insertStatement.addBatch();
            }

            insertStatement.executeBatch();
        }
    }

    private int findProfileIdByName(Connection connection, String profileName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id
                FROM scan_profiles
                WHERE LOWER(name) = LOWER(?)
                """)) {
            statement.setString(1, clean(profileName));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }

        throw new DataAccessException("Scan profile does not exist in the database: " + clean(profileName), null);
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
                profile.isMetadataRequiredBeforeExport()
        );
    }

    private MetadataTemplate copyTemplateWithId(int id, MetadataTemplate template, List<MetadataField> fields) {
        return new MetadataTemplate(
                id,
                template.getName(),
                template.getDescription(),
                template.getAssignedProfileNames(),
                fields,
                template.getStatus(),
                template.getLastUpdated()
        );
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String displayStatus(String status) {
        String cleanedStatus = clean(status);

        if (cleanedStatus.isBlank()) {
            return "";
        }

        String lowerCase = cleanedStatus.toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lowerCase.charAt(0)) + lowerCase.substring(1);
    }
}
