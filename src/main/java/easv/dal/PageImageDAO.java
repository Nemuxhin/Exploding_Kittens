package easv.dal;

import easv.be.PageImage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PageImageDAO {
    private final DatabaseConnection databaseConnection;

    public PageImageDAO() {
        this(new DatabaseConnection());
    }

    public PageImageDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public void saveAll(UUID documentId, List<PageImage> pageImages) {
        try (Connection connection = databaseConnection.getConnection()) {
            saveAll(connection, documentId, pageImages);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to store pages for document " + documentId, e);
        }
    }

    public void saveAll(Connection connection, UUID documentId, List<PageImage> pageImages) {
        syncDocumentPages(connection, documentId, pageImages);
    }

    public void syncDocumentPages(UUID documentId, List<PageImage> pageImages) {
        try (Connection connection = databaseConnection.getConnection()) {
            syncDocumentPages(connection, documentId, pageImages);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to synchronize pages for document " + documentId, e);
        }
    }

    public void syncDocumentPages(Connection connection, UUID documentId, List<PageImage> pageImages) {
        if (documentId == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }
        if (pageImages == null) {
            throw new IllegalArgumentException("pageImages must not be null");
        }
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }
        try (PreparedStatement deleteStatement = connection.prepareStatement(
                "DELETE FROM document_pages WHERE document_id = ?");
             PreparedStatement insertStatement = connection.prepareStatement("""
                        INSERT INTO document_pages (
                            id, document_id, page_number, page_order, page_type, source_reference,
                            reference_id, rotation_degrees, display_content, deleted_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
            deleteStatement.setString(1, documentId.toString());
            deleteStatement.executeUpdate();

            for (PageImage pageImage : pageImages) {
                insertStatement.setString(1, pageImage.getId().toString());
                insertStatement.setString(2, documentId.toString());
                insertStatement.setInt(3, pageImage.getPageNumber());
                insertStatement.setInt(4, pageImage.getPageNumber());
                insertStatement.setString(5, pageImage.getPageType().name());
                insertStatement.setString(6, pageImage.getSourceReference());
                insertStatement.setInt(7, pageImage.getReferenceId());
                insertStatement.setInt(8, pageImage.getRotationDegrees());
                insertStatement.setString(9, resolvePersistedDisplayContent(pageImage));
                if (pageImage.getDeletedAt() == null) {
                    insertStatement.setNull(10, java.sql.Types.TIMESTAMP);
                } else {
                    insertStatement.setTimestamp(10, Timestamp.from(pageImage.getDeletedAt()));
                }
                insertStatement.addBatch();
            }

            if (!pageImages.isEmpty()) {
                insertStatement.executeBatch();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to store pages for document " + documentId, e);
        }
    }

    private String resolvePersistedDisplayContent(PageImage pageImage) {
        if (pageImage == null) {
            return "";
        }

        String displayContent = pageImage.getDisplayContent();
        if (displayContent != null && !displayContent.isBlank()) {
            return displayContent;
        }

        return pageImage.getPreviewContent();
    }

    public List<PageImage> findByDocumentId(UUID documentId) {
        if (documentId == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     """
                     SELECT id, page_number, page_type, source_reference, reference_id, rotation_degrees,
                            display_content, deleted_at
                     FROM document_pages
                     WHERE document_id = ?
                     ORDER BY page_order, id
                     """)) {
            statement.setString(1, documentId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<PageImage> pages = new ArrayList<>();
                while (resultSet.next()) {
                    pages.add(new PageImage(
                            UUID.fromString(resultSet.getString("id")),
                            resultSet.getInt("page_number"),
                            PageImage.PageType.valueOf(resultSet.getString("page_type")),
                            resultSet.getString("source_reference"),
                            resultSet.getInt("reference_id"),
                            resultSet.getInt("rotation_degrees"),
                            resultSet.getString("display_content"),
                            resultSet.getTimestamp("deleted_at") == null
                                    ? null
                                    : resultSet.getTimestamp("deleted_at").toInstant()
                    ));
                }
                return pages;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch pages for document " + documentId, e);
        }
    }
}
