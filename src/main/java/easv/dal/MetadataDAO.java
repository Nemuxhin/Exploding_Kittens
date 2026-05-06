package easv.dal;

import easv.be.MetadataField;
import easv.be.MetadataReviewRecord;
import easv.be.MetadataTemplate;
import easv.bll.AdminManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MetadataDAO {
    private static final DateTimeFormatter LAST_UPDATED_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DatabaseConnection databaseConnection;

    public MetadataDAO() {
        this(new DatabaseConnection());
    }

    public MetadataDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public List<MetadataTemplate> findAllTemplates() {
        try (Connection connection = databaseConnection.getConnection()) {
            Map<Integer, MetadataTemplateAccumulator> templates = loadTemplateRows(connection);
            loadTemplateFields(connection, templates);
            loadTemplateAssignments(connection, templates);

            List<MetadataTemplate> mappedTemplates = new ArrayList<>();
            for (MetadataTemplateAccumulator accumulator : templates.values()) {
                mappedTemplates.add(accumulator.toTemplate());
            }
            return mappedTemplates;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch metadata templates", e);
        }
    }

    public MetadataTemplate createTemplate(AdminManager.MetadataTemplateInput input) {
        String lastUpdated = "Created " + LAST_UPDATED_FORMATTER.format(LocalDateTime.now());

        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int templateId;
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO metadata_templates (name, description, status, last_updated)
                        VALUES (?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, clean(input.getName()));
                    statement.setString(2, clean(input.getDescription()));
                    statement.setString(3, clean(input.getStatus()));
                    statement.setString(4, lastUpdated);
                    statement.executeUpdate();

                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (!generatedKeys.next()) {
                            throw new DataAccessException("Creating metadata template did not return an id.");
                        }
                        templateId = generatedKeys.getInt(1);
                    }
                }

                replaceTemplateFields(connection, templateId, input.getFields());
                replaceTemplateAssignments(connection, templateId, input.getAssignedProfileNames());
                connection.commit();
                return findTemplateById(templateId);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create metadata template " + input.getName(), e);
        }
    }

    public MetadataTemplate updateTemplate(int templateId, AdminManager.MetadataTemplateInput input) {
        String lastUpdated = "Updated " + LAST_UPDATED_FORMATTER.format(LocalDateTime.now());

        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE metadata_templates
                        SET name = ?,
                            description = ?,
                            status = ?,
                            last_updated = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """)) {
                    statement.setString(1, clean(input.getName()));
                    statement.setString(2, clean(input.getDescription()));
                    statement.setString(3, clean(input.getStatus()));
                    statement.setString(4, lastUpdated);
                    statement.setInt(5, templateId);

                    if (statement.executeUpdate() == 0) {
                        throw new IllegalArgumentException("Metadata template could not be found.");
                    }
                }

                replaceTemplateFields(connection, templateId, input.getFields());
                replaceTemplateAssignments(connection, templateId, input.getAssignedProfileNames());
                connection.commit();
                return findTemplateById(templateId);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update metadata template " + templateId, e);
        }
    }

    public void updateTemplateStatus(int templateId, String status, String lastUpdated) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE metadata_templates
                     SET status = ?,
                         last_updated = ?,
                         updated_at = CURRENT_TIMESTAMP
                     WHERE id = ?
                     """)) {
            statement.setString(1, clean(status));
            statement.setString(2, clean(lastUpdated));
            statement.setInt(3, templateId);

            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Metadata template could not be found.");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update metadata template status " + templateId, e);
        }
    }

    public boolean templateNameExists(String name, Integer excludedTemplateId) {
        String sql = """
                SELECT 1
                FROM metadata_templates
                WHERE LOWER(name) = ?
                  AND (? IS NULL OR id <> ?)
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clean(name).toLowerCase(Locale.ROOT));
            if (excludedTemplateId == null) {
                statement.setNull(2, java.sql.Types.INTEGER);
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setInt(2, excludedTemplateId);
                statement.setInt(3, excludedTemplateId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to check metadata template uniqueness", e);
        }
    }

    public List<MetadataReviewRecord> findAllReviewRecords() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, identity_value, client_name, archive_name, profile_name,
                            metadata_template_name, metadata_status, qa_status, pages, last_updated,
                            assigned_to, scanned_by, date_group, warning
                     FROM metadata_review_records
                     ORDER BY id
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            List<MetadataReviewRecord> records = new ArrayList<>();
            while (resultSet.next()) {
                records.add(new MetadataReviewRecord(
                        resultSet.getString("id"),
                        nullableToEmpty(resultSet.getString("identity_value")),
                        nullableToEmpty(resultSet.getString("client_name")),
                        nullableToEmpty(resultSet.getString("archive_name")),
                        nullableToEmpty(resultSet.getString("profile_name")),
                        nullableToEmpty(resultSet.getString("metadata_template_name")),
                        nullableToEmpty(resultSet.getString("metadata_status")),
                        nullableToEmpty(resultSet.getString("qa_status")),
                        resultSet.getInt("pages"),
                        nullableToEmpty(resultSet.getString("last_updated")),
                        nullableToEmpty(resultSet.getString("assigned_to")),
                        nullableToEmpty(resultSet.getString("scanned_by")),
                        nullableToEmpty(resultSet.getString("date_group")),
                        resultSet.getBoolean("warning")
                ));
            }
            return records;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch metadata review records", e);
        }
    }

    public MetadataReviewRecord saveReviewRecord(MetadataReviewRecord record) {
        try (Connection connection = databaseConnection.getConnection()) {
            boolean exists;
            try (PreparedStatement existsStatement = connection.prepareStatement(
                    "SELECT 1 FROM metadata_review_records WHERE id = ?")) {
                existsStatement.setString(1, clean(record.getId()));
                try (ResultSet resultSet = existsStatement.executeQuery()) {
                    exists = resultSet.next();
                }
            }

            if (exists) {
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
                    bindReviewRecord(statement, record);
                    statement.setString(14, clean(record.getId()));
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO metadata_review_records (
                            id, identity_value, client_name, archive_name, profile_name,
                            metadata_template_name, metadata_status, qa_status, pages, last_updated,
                            assigned_to, scanned_by, date_group, warning
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    statement.setString(1, clean(record.getId()));
                    bindReviewRecord(statement, record, 2);
                    statement.executeUpdate();
                }
            }

            return record;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save metadata review record " + record.getId(), e);
        }
    }

    private MetadataTemplate findTemplateById(int templateId) throws SQLException {
        Map<Integer, MetadataTemplateAccumulator> templates;
        try (Connection connection = databaseConnection.getConnection()) {
            templates = loadTemplateRows(connection, templateId);
            loadTemplateFields(connection, templates);
            loadTemplateAssignments(connection, templates);
        }

        MetadataTemplateAccumulator accumulator = templates.get(templateId);
        if (accumulator == null) {
            throw new IllegalArgumentException("Metadata template could not be found.");
        }
        return accumulator.toTemplate();
    }

    private Map<Integer, MetadataTemplateAccumulator> loadTemplateRows(Connection connection) throws SQLException {
        return loadTemplateRows(connection, null);
    }

    private Map<Integer, MetadataTemplateAccumulator> loadTemplateRows(Connection connection, Integer templateId) throws SQLException {
        String sql = """
                SELECT id, name, description, status, last_updated
                FROM metadata_templates
                %s
                ORDER BY id
                """.formatted(templateId == null ? "" : "WHERE id = ?");

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (templateId != null) {
                statement.setInt(1, templateId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Integer, MetadataTemplateAccumulator> templates = new LinkedHashMap<>();
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    templates.put(id, new MetadataTemplateAccumulator(
                            id,
                            nullableToEmpty(resultSet.getString("name")),
                            nullableToEmpty(resultSet.getString("description")),
                            nullableToEmpty(resultSet.getString("status")),
                            nullableToEmpty(resultSet.getString("last_updated"))
                    ));
                }
                return templates;
            }
        }
    }

    private void loadTemplateFields(Connection connection, Map<Integer, MetadataTemplateAccumulator> templates) throws SQLException {
        if (templates.isEmpty()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, template_id, name, type, required, placeholder
                FROM metadata_fields
                ORDER BY template_id, sort_order, id
                """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                MetadataTemplateAccumulator accumulator = templates.get(resultSet.getInt("template_id"));
                if (accumulator == null) {
                    continue;
                }

                accumulator.fields.add(new MetadataField(
                        resultSet.getInt("id"),
                        nullableToEmpty(resultSet.getString("name")),
                        nullableToEmpty(resultSet.getString("type")),
                        resultSet.getBoolean("required"),
                        nullableToEmpty(resultSet.getString("placeholder"))
                ));
            }
        }
    }

    private void loadTemplateAssignments(Connection connection, Map<Integer, MetadataTemplateAccumulator> templates) throws SQLException {
        if (templates.isEmpty()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT mtpa.metadata_template_id, sp.name
                FROM metadata_template_profile_assignments mtpa
                JOIN scan_profiles sp ON sp.id = mtpa.scan_profile_id
                ORDER BY mtpa.metadata_template_id, sp.name
                """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                MetadataTemplateAccumulator accumulator = templates.get(resultSet.getInt("metadata_template_id"));
                if (accumulator == null) {
                    continue;
                }

                accumulator.assignedProfiles.add(nullableToEmpty(resultSet.getString("name")));
            }
        }
    }

    private void replaceTemplateFields(Connection connection, int templateId, List<MetadataField> fields) throws SQLException {
        try (PreparedStatement deleteStatement = connection.prepareStatement(
                "DELETE FROM metadata_fields WHERE template_id = ?")) {
            deleteStatement.setInt(1, templateId);
            deleteStatement.executeUpdate();
        }

        if (fields == null || fields.isEmpty()) {
            return;
        }

        try (PreparedStatement insertStatement = connection.prepareStatement("""
                INSERT INTO metadata_fields (template_id, name, type, required, placeholder, sort_order)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            int sortOrder = 1;
            for (MetadataField field : fields) {
                insertStatement.setInt(1, templateId);
                insertStatement.setString(2, clean(field.getName()));
                insertStatement.setString(3, clean(field.getType()));
                insertStatement.setBoolean(4, field.isRequired());
                insertStatement.setString(5, clean(field.getPlaceholder()));
                insertStatement.setInt(6, sortOrder++);
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        }
    }

    private void replaceTemplateAssignments(Connection connection, int templateId, List<String> assignedProfileNames) throws SQLException {
        try (PreparedStatement deleteStatement = connection.prepareStatement(
                "DELETE FROM metadata_template_profile_assignments WHERE metadata_template_id = ?")) {
            deleteStatement.setInt(1, templateId);
            deleteStatement.executeUpdate();
        }

        if (assignedProfileNames == null || assignedProfileNames.isEmpty()) {
            return;
        }

        try (PreparedStatement profileLookup = connection.prepareStatement("""
                SELECT id
                FROM scan_profiles
                WHERE LOWER(name) = ?
                """);
             PreparedStatement insertStatement = connection.prepareStatement("""
                     INSERT INTO metadata_template_profile_assignments (metadata_template_id, scan_profile_id)
                     VALUES (?, ?)
                     """)) {
            for (String profileName : assignedProfileNames) {
                String normalizedProfileName = clean(profileName).toLowerCase(Locale.ROOT);
                if (normalizedProfileName.isBlank()) {
                    continue;
                }

                profileLookup.setString(1, normalizedProfileName);
                try (ResultSet resultSet = profileLookup.executeQuery()) {
                    if (!resultSet.next()) {
                        continue;
                    }

                    insertStatement.setInt(1, templateId);
                    insertStatement.setInt(2, resultSet.getInt("id"));
                    insertStatement.addBatch();
                }
            }

            insertStatement.executeBatch();
        }
    }

    private void bindReviewRecord(PreparedStatement statement, MetadataReviewRecord record) throws SQLException {
        bindReviewRecord(statement, record, 1);
    }

    private void bindReviewRecord(PreparedStatement statement, MetadataReviewRecord record, int offset) throws SQLException {
        statement.setString(offset, clean(record.getIdentity()));
        statement.setString(offset + 1, clean(record.getClient()));
        statement.setString(offset + 2, clean(record.getArchive()));
        statement.setString(offset + 3, clean(record.getProfile()));
        statement.setString(offset + 4, clean(record.getMetadataTemplate()));
        statement.setString(offset + 5, clean(record.getMetadataStatus()));
        statement.setString(offset + 6, clean(record.getQaStatus()));
        statement.setInt(offset + 7, record.getPages());
        statement.setString(offset + 8, clean(record.getLastUpdated()));
        statement.setString(offset + 9, clean(record.getAssignedTo()));
        statement.setString(offset + 10, clean(record.getScannedBy()));
        statement.setString(offset + 11, clean(record.getDateGroup()));
        statement.setBoolean(offset + 12, record.hasWarning());
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullableToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class MetadataTemplateAccumulator {
        private final int id;
        private final String name;
        private final String description;
        private final String status;
        private final String lastUpdated;
        private final List<String> assignedProfiles = new ArrayList<>();
        private final List<MetadataField> fields = new ArrayList<>();

        private MetadataTemplateAccumulator(int id, String name, String description, String status, String lastUpdated) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.status = status;
            this.lastUpdated = lastUpdated;
        }

        private MetadataTemplate toTemplate() {
            return new MetadataTemplate(id, name, description, assignedProfiles, fields, status, lastUpdated);
        }
    }
}
