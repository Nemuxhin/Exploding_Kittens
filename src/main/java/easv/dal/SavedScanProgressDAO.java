package easv.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SavedScanProgressDAO {
    private final DatabaseConnection databaseConnection;

    public SavedScanProgressDAO() {
        this(new DatabaseConnection());
    }

    public SavedScanProgressDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public void save(UUID sessionId, Integer createdByUserId, StoredProgress progress) {
        if (sessionId == null || progress == null) {
            throw new IllegalArgumentException("sessionId and progress are required.");
        }

        Instant now = Instant.now();
        try (Connection connection = databaseConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement upsert = connection.prepareStatement("""
                        MERGE scan_saved_progress AS target
                        USING (SELECT ? AS session_id) AS source
                        ON target.session_id = source.session_id
                        WHEN MATCHED THEN
                            UPDATE SET box = ?,
                                       profile = ?,
                                       status = ?,
                                       created_by_user_id = ?,
                                       saved_at = ?
                        WHEN NOT MATCHED THEN
                            INSERT (session_id, box, profile, status, created_by_user_id, saved_at)
                            VALUES (?, ?, ?, ?, ?, ?);
                        """)) {
                    upsert.setString(1, sessionId.toString());
                    upsert.setString(2, clean(progress.boxId()));
                    upsert.setString(3, clean(progress.profileName()));
                    upsert.setString(4, clean(progress.status()));
                    setNullableInt(upsert, 5, createdByUserId);
                    upsert.setTimestamp(6, Timestamp.from(now));
                    upsert.setString(7, sessionId.toString());
                    upsert.setString(8, clean(progress.boxId()));
                    upsert.setString(9, clean(progress.profileName()));
                    upsert.setString(10, clean(progress.status()));
                    setNullableInt(upsert, 11, createdByUserId);
                    upsert.setTimestamp(12, Timestamp.from(now));
                    upsert.executeUpdate();
                }

                try (PreparedStatement deletePages = connection.prepareStatement("""
                        DELETE FROM scan_saved_progress_pages
                        WHERE session_id = ?
                        """)) {
                    deletePages.setString(1, sessionId.toString());
                    deletePages.executeUpdate();
                }

                try (PreparedStatement insertPage = connection.prepareStatement("""
                        INSERT INTO scan_saved_progress_pages (
                            id,
                            session_id,
                            page_order,
                            reference_id,
                            file_id,
                            document_number,
                            is_barcode,
                            rotation_degrees,
                            needs_rescan,
                            split_reason_after,
                            source_reference,
                            display_content,
                            preview_content
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    int order = 1;
                    for (StoredPage page : progress.pages()) {
                        if (page == null) {
                            continue;
                        }
                        insertPage.setString(1, UUID.randomUUID().toString());
                        insertPage.setString(2, sessionId.toString());
                        insertPage.setInt(3, order++);
                        insertPage.setInt(4, page.referenceId());
                        insertPage.setInt(5, page.fileId());
                        insertPage.setInt(6, page.documentNumber());
                        insertPage.setBoolean(7, page.barcode());
                        insertPage.setInt(8, page.rotationDegrees());
                        insertPage.setBoolean(9, page.needsRescan());
                        insertPage.setString(10, page.splitReasonAfter());
                        insertPage.setString(11, clean(page.sourceReference()));
                        insertPage.setString(12, clean(page.displayContent()));
                        insertPage.setString(13, clean(page.previewContent()));
                        insertPage.addBatch();
                    }
                    insertPage.executeBatch();
                }

                connection.commit();
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException e) {
                connection.rollback();
                connection.setAutoCommit(previousAutoCommit);
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save scan progress for session " + sessionId, e);
        }
    }

    public StoredProgress findBySessionId(UUID sessionId, Integer createdByUserId) {
        if (sessionId == null) {
            return null;
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement header = connection.prepareStatement("""
                     SELECT box, profile, status, saved_at
                     FROM scan_saved_progress
                     WHERE session_id = ?
                       AND (? IS NULL OR created_by_user_id = ?)
                     """)) {
            header.setString(1, sessionId.toString());
            if (createdByUserId == null) {
                header.setNull(2, java.sql.Types.INTEGER);
                header.setNull(3, java.sql.Types.INTEGER);
            } else {
                header.setInt(2, createdByUserId);
                header.setInt(3, createdByUserId);
            }

            try (ResultSet headerResult = header.executeQuery()) {
                if (!headerResult.next()) {
                    return null;
                }

                List<StoredPage> pages = new ArrayList<>();
                try (PreparedStatement pageStatement = connection.prepareStatement("""
                        SELECT reference_id,
                               file_id,
                               document_number,
                               is_barcode,
                               rotation_degrees,
                               needs_rescan,
                               split_reason_after,
                               source_reference,
                               display_content,
                               preview_content
                        FROM scan_saved_progress_pages
                        WHERE session_id = ?
                        ORDER BY page_order, id
                        """)) {
                    pageStatement.setString(1, sessionId.toString());
                    try (ResultSet resultSet = pageStatement.executeQuery()) {
                        while (resultSet.next()) {
                            pages.add(new StoredPage(
                                    resultSet.getInt("reference_id"),
                                    resultSet.getInt("file_id"),
                                    resultSet.getInt("document_number"),
                                    resultSet.getBoolean("is_barcode"),
                                    resultSet.getInt("rotation_degrees"),
                                    resultSet.getBoolean("needs_rescan"),
                                    resultSet.getString("split_reason_after"),
                                    resultSet.getString("source_reference"),
                                    resultSet.getString("display_content"),
                                    resultSet.getString("preview_content")
                            ));
                        }
                    }
                }

                Timestamp savedAtTimestamp = headerResult.getTimestamp("saved_at");
                return new StoredProgress(
                        headerResult.getString("box"),
                        headerResult.getString("profile"),
                        headerResult.getString("status"),
                        savedAtTimestamp == null ? Instant.now() : savedAtTimestamp.toInstant(),
                        pages
                );
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load saved scan progress for session " + sessionId, e);
        }
    }

    public void deleteBySessionId(UUID sessionId) {
        if (sessionId == null) {
            return;
        }

        try (Connection connection = databaseConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement deletePages = connection.prepareStatement("""
                        DELETE FROM scan_saved_progress_pages
                        WHERE session_id = ?
                        """)) {
                    deletePages.setString(1, sessionId.toString());
                    deletePages.executeUpdate();
                }

                try (PreparedStatement deleteHeader = connection.prepareStatement("""
                        DELETE FROM scan_saved_progress
                        WHERE session_id = ?
                        """)) {
                    deleteHeader.setString(1, sessionId.toString());
                    deleteHeader.executeUpdate();
                }
                connection.commit();
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException e) {
                connection.rollback();
                connection.setAutoCommit(previousAutoCommit);
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete saved scan progress for session " + sessionId, e);
        }
    }

    private void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public record StoredProgress(
            String boxId,
            String profileName,
            String status,
            Instant savedAt,
            List<StoredPage> pages
    ) {
        public StoredProgress {
            pages = pages == null ? List.of() : List.copyOf(pages);
        }
    }

    public record StoredPage(
            int referenceId,
            int fileId,
            int documentNumber,
            boolean barcode,
            int rotationDegrees,
            boolean needsRescan,
            String splitReasonAfter,
            String sourceReference,
            String displayContent,
            String previewContent
    ) {}
}
