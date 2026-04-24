package easv.dal;

import easv.be.Document;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class DocumentDAO {
    private final DatabaseConnection databaseConnection;
    private final PageImageDAO pageImageDAO;

    public DocumentDAO() {
        this(new DatabaseConnection(), new PageImageDAO());
    }

    public DocumentDAO(DatabaseConnection databaseConnection, PageImageDAO pageImageDAO) {
        this.databaseConnection = databaseConnection;
        this.pageImageDAO = pageImageDAO;
    }

    public Document saveOrGetExisting(Document document, UUID caseFileId) {
        if (document == null) {
            throw new IllegalArgumentException("document must not be null");
        }
        if (caseFileId == null) {
            throw new IllegalArgumentException("caseFileId must not be null");
        }
        Optional<Document> existing = findBySourceItemId(document.getSourceItemId());
        if (existing.isPresent()) {
            return existing.get();
        }

        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO documents (id, source_item_id, case_file_id) VALUES (?, ?, ?)")) {
                statement.setString(1, document.getId().toString());
                statement.setString(2, document.getSourceItemId());
                statement.setString(3, caseFileId.toString());
                statement.executeUpdate();
                pageImageDAO.saveAll(connection, document.getId(), document.getPages());
                connection.commit();
                return document;
            } catch (SQLException | DataAccessException e) {
                connection.rollback();
                if (e instanceof SQLException sqlException && isUniqueViolation(sqlException)) {
                    return findBySourceItemId(document.getSourceItemId()).orElseThrow();
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to store document " + document.getSourceItemId(), e);
        } catch (DataAccessException e) {
            throw new DataAccessException("Failed to store document " + document.getSourceItemId(), e);
        }
    }

    public Optional<Document> findBySourceItemId(String sourceItemId) {
        if (sourceItemId == null || sourceItemId.isBlank()) {
            throw new IllegalArgumentException("sourceItemId must not be blank");
        }
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, source_item_id FROM documents WHERE source_item_id = ?")) {
            statement.setString(1, sourceItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    UUID documentId = UUID.fromString(resultSet.getString("id"));
                    return Optional.of(new Document(
                            documentId,
                            resultSet.getString("source_item_id"),
                            pageImageDAO.findByDocumentId(documentId)
                    ));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch document " + sourceItemId, e);
        }
    }

    public Collection<Document> findAll() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, source_item_id FROM documents ORDER BY source_item_id");
             ResultSet resultSet = statement.executeQuery()) {
            Collection<Document> documents = new ArrayList<>();
            while (resultSet.next()) {
                UUID documentId = UUID.fromString(resultSet.getString("id"));
                documents.add(new Document(
                        documentId,
                        resultSet.getString("source_item_id"),
                        pageImageDAO.findByDocumentId(documentId)
                ));
            }
            return documents;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch documents", e);
        }
    }

    public Collection<Document> findByCaseFileId(UUID caseFileId) {
        if (caseFileId == null) {
            throw new IllegalArgumentException("caseFileId must not be null");
        }
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, source_item_id FROM documents WHERE case_file_id = ? ORDER BY source_item_id")) {
            statement.setString(1, caseFileId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                Collection<Document> documents = new ArrayList<>();
                while (resultSet.next()) {
                    UUID documentId = UUID.fromString(resultSet.getString("id"));
                    documents.add(new Document(
                            documentId,
                            resultSet.getString("source_item_id"),
                            pageImageDAO.findByDocumentId(documentId)
                    ));
                }
                return documents;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch documents for case file " + caseFileId, e);
        }
    }

    private boolean isUniqueViolation(SQLException e) {
        return e.getSQLState() != null && e.getSQLState().startsWith("23");
    }
}
