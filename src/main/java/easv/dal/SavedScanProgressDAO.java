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
    private static final int PAGE_ROW_BATCH_SIZE = 4;

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
                upsertHeader(connection, sessionId, createdByUserId, progress, now);
                connection.commit();
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                    // Keep the original failure.
                }
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }

            replacePages(sessionId, progress.pages());
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save scan progress for session " + sessionId, e);
        }
    }

    private void replacePages(UUID sessionId, List<StoredPage> pages) throws SQLException {
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
                connection.commit();

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
                    int pendingRows = 0;
                    for (StoredPage page : pages) {
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
                        insertPage.executeUpdate();
                        pendingRows++;

                        if (pendingRows >= PAGE_ROW_BATCH_SIZE) {
                            connection.commit();
                            pendingRows = 0;
                        }
                    }

                    if (pendingRows > 0) {
                        connection.commit();
                    }
                }
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                    // Keep the original failure.
                }
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private void upsertHeader(
            Connection connection,
            UUID sessionId,
            Integer createdByUserId,
            StoredProgress progress,
            Instant now
    ) throws SQLException {
        String normalizedSessionId = sessionId.toString();
        String boxId = clean(progress.boxId());
        String profileName = clean(progress.profileName());
        String status = clean(progress.status());
        Timestamp savedAt = Timestamp.from(now);

        int updated;
        try (PreparedStatement update = connection.prepareStatement("""
                UPDATE scan_saved_progress
                SET box = ?,
                    profile = ?,
                    status = ?,
                    created_by_user_id = ?,
                    saved_at = ?
                WHERE session_id = ?
                """)) {
            update.setString(1, boxId);
            update.setString(2, profileName);
            update.setString(3, status);
            setNullableInt(update, 4, createdByUserId);
            update.setTimestamp(5, savedAt);
            update.setString(6, normalizedSessionId);
            updated = update.executeUpdate();
        }

        if (updated > 0) {
            return;
        }

        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO scan_saved_progress (
                    session_id,
                    box,
                    profile,
                    status,
                    created_by_user_id,
                    saved_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            insert.setString(1, normalizedSessionId);
            insert.setString(2, boxId);
            insert.setString(3, profileName);
            insert.setString(4, status);
            setNullableInt(insert, 5, createdByUserId);
            insert.setTimestamp(6, savedAt);
            insert.executeUpdate();
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

    public StoredProgress findLatestByBoxAndProfile(String boxId, String profileName) {
        String normalizedBox = clean(boxId);
        String normalizedProfile = clean(profileName);
        if (normalizedBox.isBlank() || normalizedProfile.isBlank()) {
            return null;
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement header = connection.prepareStatement("""
                     SELECT TOP 1 session_id, box, profile, status, saved_at
                     FROM scan_saved_progress
                     WHERE box = ?
                       AND profile = ?
                     ORDER BY saved_at DESC, session_id DESC
                     """)) {
            header.setString(1, normalizedBox);
            header.setString(2, normalizedProfile);

            try (ResultSet headerResult = header.executeQuery()) {
                if (!headerResult.next()) {
                    return null;
                }

                String sessionId = headerResult.getString("session_id");
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
                    pageStatement.setString(1, sessionId);
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
            throw new DataAccessException("Failed to load latest saved scan progress for box " + normalizedBox, e);
        }
    }

    public StoredProgressSummary findLatestForUser(Integer createdByUserId) {
        if (createdByUserId == null) {
            return null;
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT TOP 1 sp.session_id,
                                  sp.box,
                                  sp.profile,
                                  sp.status,
                                  sp.saved_at,
                                  (
                                      SELECT COUNT(*)
                                      FROM scan_saved_progress_pages spp
                                      WHERE spp.session_id = sp.session_id
                                  ) AS page_count
                     FROM scan_saved_progress sp
                     WHERE sp.created_by_user_id = ?
                     ORDER BY sp.saved_at DESC, sp.session_id DESC
                     """)) {
            statement.setInt(1, createdByUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                Timestamp savedAtTimestamp = resultSet.getTimestamp("saved_at");
                return new StoredProgressSummary(
                        UUID.fromString(resultSet.getString("session_id")),
                        resultSet.getString("box"),
                        resultSet.getString("profile"),
                        resultSet.getString("status"),
                        savedAtTimestamp == null ? Instant.now() : savedAtTimestamp.toInstant(),
                        resultSet.getInt("page_count")
                );
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load latest saved scan progress for user " + createdByUserId, e);
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

    public record StoredProgressSummary(
            UUID sessionId,
            String boxId,
            String profileName,
            String status,
            Instant savedAt,
            int pageCount
    ) {}
}
