package easv.dal;

import easv.be.PageImage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        if (documentId == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }
        if (pageImages == null) {
            throw new IllegalArgumentException("pageImages must not be null");
        }
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }
        try {
            for (PageImage pageImage : pageImages) {
                if (exists(connection, pageImage.getId())) {
                    continue;
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO document_pages (id, document_id, page_number, page_type, source_reference) VALUES (?, ?, ?, ?, ?)")) {
                    statement.setString(1, pageImage.getId().toString());
                    statement.setString(2, documentId.toString());
                    statement.setInt(3, pageImage.getPageNumber());
                    statement.setString(4, pageImage.getPageType().name());
                    statement.setString(5, pageImage.getSourceReference());
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to store pages for document " + documentId, e);
        }
    }

    public List<PageImage> findByDocumentId(UUID documentId) {
        if (documentId == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, page_number, page_type, source_reference FROM document_pages WHERE document_id = ? ORDER BY page_number")) {
            statement.setString(1, documentId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<PageImage> pages = new ArrayList<>();
                while (resultSet.next()) {
                    pages.add(new PageImage(
                            UUID.fromString(resultSet.getString("id")),
                            resultSet.getInt("page_number"),
                            PageImage.PageType.valueOf(resultSet.getString("page_type")),
                            resultSet.getString("source_reference")
                    ));
                }
                return pages;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch pages for document " + documentId, e);
        }
    }

    private boolean exists(Connection connection, UUID pageId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM document_pages WHERE id = ?")) {
            statement.setString(1, pageId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
