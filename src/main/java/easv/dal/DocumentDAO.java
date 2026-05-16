package easv.dal;

import easv.be.Document;
import easv.be.PageImage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
        try (Connection connection = databaseConnection.getConnection()) {
            Optional<Document> existing = findBySourceItemId(connection, document.getSourceItemId());
            if (existing.isPresent()) {
                return existing.get();
            }
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO documents (id, source_item_id, case_file_id) VALUES (?, ?, ?)")) {
                statement.setString(1, document.getId().toString());
                statement.setString(2, document.getSourceItemId());
                statement.setString(3, caseFileId.toString());
                statement.executeUpdate();
                pageImageDAO.syncDocumentPages(connection, document.getId(), document.getAllPages());
                connection.commit();
                return document;
            } catch (SQLException | DataAccessException e) {
                connection.rollback();
                if (e instanceof SQLException sqlException && isUniqueViolation(sqlException)) {
                    return findBySourceItemId(connection, document.getSourceItemId()).orElseThrow();
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
             ) {
            return findBySourceItemId(connection, sourceItemId);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch document " + sourceItemId, e);
        }
    }

    Optional<Document> findBySourceItemId(Connection connection, String sourceItemId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, source_item_id FROM documents WHERE source_item_id = ?")) {
            statement.setString(1, sourceItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    UUID documentId = UUID.fromString(resultSet.getString("id"));
                    return loadPersistedDocument(connection, documentId, resultSet.getString("source_item_id"));
                }
                return Optional.empty();
            }
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
                loadPersistedDocument(connection, documentId, resultSet.getString("source_item_id"))
                        .ifPresent(documents::add);
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
             ) {
            return findByCaseFileId(connection, caseFileId);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch documents for case file " + caseFileId, e);
        }
    }

    Collection<Document> findByCaseFileId(Connection connection, UUID caseFileId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, source_item_id FROM documents WHERE case_file_id = ? ORDER BY source_item_id")) {
            statement.setString(1, caseFileId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                Collection<Document> documents = new ArrayList<>();
                while (resultSet.next()) {
                    UUID documentId = UUID.fromString(resultSet.getString("id"));
                    loadPersistedDocument(connection, documentId, resultSet.getString("source_item_id"))
                            .ifPresent(documents::add);
                }
                return documents;
            }
        }
    }

    public Document synchronizePages(Document document) {
        try (Connection connection = databaseConnection.getConnection()) {
            pageImageDAO.syncDocumentPages(connection, document.getId(), document.getAllPages());
            return document;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to synchronize document pages for " + document.getId(), e);
        }
    }

    public Document addPage(UUID documentId, PageImage pageImage, int position) {
        Document document = findRequiredDocument(documentId);
        document.addPage(pageImage, position);
        return synchronizePages(document);
    }

    public Document movePage(UUID documentId, UUID pageId, int newPosition) {
        Document document = findRequiredDocument(documentId);
        document.movePage(pageId, newPosition);
        return synchronizePages(document);
    }

    public Document deletePage(UUID documentId, UUID pageId) {
        Document document = findRequiredDocument(documentId);
        document.deletePage(pageId);
        return synchronizePages(document);
    }

    public Document restorePage(UUID documentId, UUID pageId, int position) {
        Document document = findRequiredDocument(documentId);
        document.restorePage(pageId, position);
        return synchronizePages(document);
    }

    public void movePageToDocument(UUID sourceDocumentId, UUID targetDocumentId, UUID pageId, int targetPosition) {
        Document sourceDocument = findRequiredDocument(sourceDocumentId);
        Document targetDocument = findRequiredDocument(targetDocumentId);

        PageImage pageImage = sourceDocument.extractPage(pageId);
        pageImage.restore();
        targetDocument.addPage(pageImage, targetPosition);

        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (sourceDocument.getPages().isEmpty()) {
                    deleteDocument(connection, sourceDocument.getId());
                } else {
                    pageImageDAO.syncDocumentPages(connection, sourceDocument.getId(), sourceDocument.getAllPages());
                }
                pageImageDAO.syncDocumentPages(connection, targetDocument.getId(), targetDocument.getAllPages());
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to move page between documents", e);
        }
    }

    private Document findRequiredDocument(UUID documentId) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, source_item_id FROM documents WHERE id = ?")) {
            statement.setString(1, documentId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Document could not be found.");
                }
                return loadPersistedDocument(connection,
                        UUID.fromString(resultSet.getString("id")),
                        resultSet.getString("source_item_id"))
                        .orElseThrow(() -> new IllegalArgumentException("Document could not be found."));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch document " + documentId, e);
        }
    }

    private Optional<Document> loadPersistedDocument(Connection connection, UUID documentId, String sourceItemId) {
        List<PageImage> pages = pageImageDAO.findByDocumentId(documentId);
        boolean hasActivePages = pages.stream().anyMatch(page -> !page.isDeleted());
        if (!hasActivePages) {
            deleteDocument(connection, documentId);
            return Optional.empty();
        }
        return Optional.of(new Document(documentId, sourceItemId, pages));
    }

    private void deleteDocument(Connection connection, UUID documentId) {
        try (PreparedStatement deleteSessionLinks = connection.prepareStatement(
                "DELETE FROM scan_session_documents WHERE document_id = ?");
             PreparedStatement deletePages = connection.prepareStatement(
                "DELETE FROM document_pages WHERE document_id = ?");
             PreparedStatement deleteDocument = connection.prepareStatement(
                     "DELETE FROM documents WHERE id = ?")) {
            deleteSessionLinks.setString(1, documentId.toString());
            deleteSessionLinks.executeUpdate();
            deletePages.setString(1, documentId.toString());
            deletePages.executeUpdate();
            deleteDocument.setString(1, documentId.toString());
            deleteDocument.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete empty document " + documentId, e);
        }
    }

    private boolean isUniqueViolation(SQLException e) {
        return e.getSQLState() != null && e.getSQLState().startsWith("23");
    }
}
