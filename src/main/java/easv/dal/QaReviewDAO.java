package easv.dal;

import easv.be.AuditLog;
import easv.be.QaReview;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QaReviewDAO {
    private static final int PAGE_ROW_BATCH_SIZE = 8;
    private final DatabaseConnection databaseConnection;

    public QaReviewDAO() {
        this(new DatabaseConnection());
    }

    public QaReviewDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public QaReview.QaAssignmentSnapshot createOrResetSubmission(
            UUID sessionId,
            String boxId,
            String profileName,
            Integer createdByUserId,
            Integer assignedToUserId,
            String scannedBy,
            List<QaReview.QaDocumentSnapshot> documents
    ) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }

        Instant now = Instant.now();
        List<QaReview.QaDocumentSnapshot> safeDocuments = documents == null ? List.of() : List.copyOf(documents);
        int documentCount = countDocuments(safeDocuments);
        int pageCount = countPages(safeDocuments);
        UUID reviewId;

        try (Connection connection = databaseConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                StoredHeader existing = findHeaderBySessionId(connection, sessionId);
                reviewId = existing == null ? UUID.randomUUID() : existing.reviewId();

                if (existing == null) {
                    try (PreparedStatement insert = connection.prepareStatement("""
                            INSERT INTO qa_reviews (
                                id,
                                session_id,
                                box,
                                status,
                                profile,
                                scanned_by,
                                documents,
                                pages,
                                assigned_at,
                                reviewed,
                                issues,
                                created_by_user_id,
                                assigned_to_user_id,
                                started_at,
                                completed_at,
                                expires_at,
                                last_updated_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """)) {
                        insert.setString(1, reviewId.toString());
                        insert.setString(2, sessionId.toString());
                        insert.setString(3, clean(boxId));
                        insert.setString(4, QaReview.QaReviewStatus.WAITING_FOR_QA.name());
                        insert.setString(5, clean(profileName));
                        insert.setString(6, clean(scannedBy));
                        insert.setInt(7, documentCount);
                        insert.setInt(8, pageCount);
                        insert.setTimestamp(9, Timestamp.from(now));
                        insert.setInt(10, 0);
                        insert.setInt(11, 0);
                        setNullableInt(insert, 12, createdByUserId);
                        setNullableInt(insert, 13, assignedToUserId);
                        insert.setNull(14, java.sql.Types.TIMESTAMP);
                        insert.setNull(15, java.sql.Types.TIMESTAMP);
                        insert.setNull(16, java.sql.Types.TIMESTAMP);
                        insert.setTimestamp(17, Timestamp.from(now));
                        insert.executeUpdate();
                    }
                } else {
                    try (PreparedStatement update = connection.prepareStatement("""
                            UPDATE qa_reviews
                            SET box = ?,
                                status = ?,
                                profile = ?,
                                scanned_by = ?,
                                documents = ?,
                                pages = ?,
                                assigned_at = ?,
                                reviewed = 0,
                                issues = 0,
                                created_by_user_id = ?,
                                assigned_to_user_id = ?,
                                started_at = NULL,
                                completed_at = NULL,
                                expires_at = NULL,
                                last_updated_at = ?
                            WHERE id = ?
                            """)) {
                        update.setString(1, clean(boxId));
                        update.setString(2, QaReview.QaReviewStatus.WAITING_FOR_QA.name());
                        update.setString(3, clean(profileName));
                        update.setString(4, clean(scannedBy));
                        update.setInt(5, documentCount);
                        update.setInt(6, pageCount);
                        update.setTimestamp(7, Timestamp.from(now));
                        setNullableInt(update, 8, createdByUserId);
                        setNullableInt(update, 9, assignedToUserId);
                        update.setTimestamp(10, Timestamp.from(now));
                        update.setString(11, reviewId.toString());
                        update.executeUpdate();
                    }
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
            replacePageRows(reviewId, clean(boxId), clean(profileName), safeDocuments, now);
            return findById(reviewId);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to submit scan for QA.", e);
        }
    }

    public List<QaReview.QaAssignmentSnapshot> findAssignmentsByAssignee(int userId) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id,
                            session_id,
                            box,
                            status,
                            profile,
                            scanned_by,
                            documents,
                            pages,
                            assigned_at,
                            reviewed,
                            issues,
                            created_by_user_id,
                            assigned_to_user_id,
                            started_at,
                            completed_at,
                            expires_at,
                            last_updated_at
                     FROM qa_reviews
                     WHERE assigned_to_user_id = ?
                       AND status IN (?, ?, ?)
                     ORDER BY assigned_at DESC
                     """)) {
            statement.setInt(1, userId);
            statement.setString(2, QaReview.QaReviewStatus.WAITING_FOR_QA.name());
            statement.setString(3, QaReview.QaReviewStatus.IN_REVIEW.name());
            statement.setString(4, QaReview.QaReviewStatus.APPROVED.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<QaReview.QaAssignmentSnapshot> assignments = new ArrayList<>();
                while (resultSet.next()) {
                    assignments.add(toAssignment(connection, readHeader(resultSet)));
                }
                return assignments;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to read assigned QA work.", e);
        }
    }

    public List<QaReview.QaAssignmentSnapshot> findAll() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id,
                            session_id,
                            box,
                            status,
                            profile,
                            scanned_by,
                            documents,
                            pages,
                            assigned_at,
                            reviewed,
                            issues,
                            created_by_user_id,
                            assigned_to_user_id,
                            started_at,
                            completed_at,
                            expires_at,
                            last_updated_at
                     FROM qa_reviews
                     ORDER BY COALESCE(last_updated_at, assigned_at) DESC, assigned_at DESC
                     """)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                List<QaReview.QaAssignmentSnapshot> assignments = new ArrayList<>();
                while (resultSet.next()) {
                    assignments.add(toAssignment(connection, readHeader(resultSet)));
                }
                return assignments;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to read QA reviews.", e);
        }
    }

    /**
     * Reads only the QA review headers needed for admin overview tables.
     *
     * The full findAll() method also loads qa_review_pages.display_content for every
     * review, which is very expensive because display_content stores the page preview
     * data. The admin overview does not need page images; it only needs row summary
     * fields. Use findById(...) when opening one review in the workspace.
     */
    public List<QaReview.QaAssignmentSnapshot> findAllSummaries() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id,
                            session_id,
                            box,
                            status,
                            profile,
                            scanned_by,
                            documents,
                            pages,
                            assigned_at,
                            reviewed,
                            issues,
                            created_by_user_id,
                            assigned_to_user_id,
                            started_at,
                            completed_at,
                            expires_at,
                            last_updated_at
                     FROM qa_reviews
                     ORDER BY COALESCE(last_updated_at, assigned_at) DESC, assigned_at DESC
                     """)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                List<QaReview.QaAssignmentSnapshot> assignments = new ArrayList<>();
                while (resultSet.next()) {
                    assignments.add(toAssignmentSummary(readHeader(resultSet)));
                }
                return assignments;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to read QA review summaries.", e);
        }
    }

    public Map<UUID, QaReview.SessionQaState> findReviewStatesByCreator(int userId) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT session_id,
                            status,
                            assigned_at,
                            completed_at,
                            reviewed,
                            pages,
                            issues
                     FROM qa_reviews
                     WHERE created_by_user_id = ?
                     ORDER BY assigned_at DESC
                     """)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<UUID, QaReview.SessionQaState> states = new LinkedHashMap<>();
                while (resultSet.next()) {
                    UUID sessionId = UUID.fromString(resultSet.getString("session_id"));
                    states.put(sessionId, new QaReview.SessionQaState(
                            sessionId,
                            QaReview.QaReviewStatus.valueOf(resultSet.getString("status")),
                            resultSet.getTimestamp("assigned_at").toInstant(),
                            timestampToInstant(resultSet, "completed_at"),
                            resultSet.getInt("reviewed"),
                            resultSet.getInt("pages"),
                            resultSet.getInt("issues")
                    ));
                }
                return states;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to read QA states.", e);
        }
    }

    public List<QaReview.QaAssignmentSnapshot> findApprovedByCreator(int userId) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id,
                            session_id,
                            box,
                            status,
                            profile,
                            scanned_by,
                            documents,
                            pages,
                            assigned_at,
                            reviewed,
                            issues,
                            created_by_user_id,
                            assigned_to_user_id,
                            started_at,
                            completed_at,
                            expires_at,
                            last_updated_at
                     FROM qa_reviews
                     WHERE created_by_user_id = ?
                       AND status = ?
                     ORDER BY completed_at DESC, assigned_at DESC
                     """)) {
            statement.setInt(1, userId);
            statement.setString(2, QaReview.QaReviewStatus.APPROVED.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<QaReview.QaAssignmentSnapshot> assignments = new ArrayList<>();
                while (resultSet.next()) {
                    assignments.add(toAssignment(connection, readHeader(resultSet)));
                }
                return assignments;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to read approved QA reviews.", e);
        }
    }

    public QaReview.QaAssignmentSnapshot findRejectedBySessionForCreator(UUID sessionId, int creatorUserId) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT TOP 1 id,
                                  session_id,
                                  box,
                                  status,
                                  profile,
                                  scanned_by,
                                  documents,
                                  pages,
                                  assigned_at,
                                  reviewed,
                                  issues,
                                  created_by_user_id,
                                  assigned_to_user_id,
                                  started_at,
                                  completed_at,
                                  expires_at,
                                  last_updated_at
                     FROM qa_reviews
                     WHERE session_id = ?
                       AND created_by_user_id = ?
                       AND status = ?
                     ORDER BY completed_at DESC, assigned_at DESC
                     """)) {
            statement.setString(1, sessionId.toString());
            statement.setInt(2, creatorUserId);
            statement.setString(3, QaReview.QaReviewStatus.REJECTED.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return toAssignment(connection, readHeader(resultSet));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to read rejected QA snapshot.", e);
        }
    }

    public void markStarted(UUID reviewId) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE qa_reviews
                     SET status = ?,
                         started_at = COALESCE(started_at, ?),
                         last_updated_at = ?
                     WHERE id = ?
                     """)) {
            Instant now = Instant.now();
            statement.setString(1, QaReview.QaReviewStatus.IN_REVIEW.name());
            statement.setTimestamp(2, Timestamp.from(now));
            statement.setTimestamp(3, Timestamp.from(now));
            statement.setString(4, reviewId.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark QA review as started.", e);
        }
    }

    public void assignReviewer(UUID reviewId, Integer assignedToUserId, QaReview.QaReviewStatus status) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE qa_reviews
                     SET assigned_to_user_id = ?,
                         status = ?,
                         started_at = CASE WHEN ? = ? THEN started_at ELSE NULL END,
                         completed_at = CASE WHEN ? = ? THEN completed_at ELSE NULL END,
                         expires_at = CASE WHEN ? = ? THEN expires_at ELSE NULL END,
                         last_updated_at = ?
                     WHERE id = ?
                     """)) {
            Instant now = Instant.now();
            setNullableInt(statement, 1, assignedToUserId);
            statement.setString(2, status.name());
            statement.setString(3, status.name());
            statement.setString(4, QaReview.QaReviewStatus.IN_REVIEW.name());
            statement.setString(5, status.name());
            statement.setString(6, QaReview.QaReviewStatus.APPROVED.name());
            statement.setString(7, status.name());
            statement.setString(8, QaReview.QaReviewStatus.APPROVED.name());
            statement.setTimestamp(9, Timestamp.from(now));
            statement.setString(10, reviewId.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to assign QA review " + reviewId, e);
        }
    }

    public void saveProgress(
            UUID reviewId,
            QaReview.QaReviewStatus status,
            int reviewedPages,
            int totalPages,
            int issueCount,
            List<QaReview.QaDocumentSnapshot> documents
    ) {
        Instant now = Instant.now();
        List<QaReview.QaDocumentSnapshot> safeDocuments = documents == null ? List.of() : List.copyOf(documents);
        String box;
        String profile;
        try (Connection connection = databaseConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                StoredHeader existing = findHeaderById(connection, reviewId);
                if (existing == null) {
                    throw new SQLException("QA review " + reviewId + " could not be found.");
                }
                box = existing.box();
                profile = existing.profile();

                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE qa_reviews
                        SET status = ?,
                            documents = ?,
                            pages = ?,
                            reviewed = ?,
                            issues = ?,
                            last_updated_at = ?
                        WHERE id = ?
                        """)) {
                    update.setString(1, status.name());
                    update.setInt(2, countDocuments(safeDocuments));
                    update.setInt(3, totalPages);
                    update.setInt(4, reviewedPages);
                    update.setInt(5, issueCount);
                    update.setTimestamp(6, Timestamp.from(now));
                    update.setString(7, reviewId.toString());
                    update.executeUpdate();
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
            replacePageRows(reviewId, box, profile, safeDocuments, now);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save QA progress.", e);
        }
    }

    public void completeReview(
            UUID reviewId,
            QaReview.QaReviewStatus status,
            Integer assignedToUserId,
            int reviewedPages,
            int totalPages,
            int issueCount,
            List<QaReview.QaDocumentSnapshot> documents
    ) {
        Instant now = Instant.now();
        Instant expiresAt = status == QaReview.QaReviewStatus.APPROVED ? now.plusSeconds(30L * 24 * 60 * 60) : null;
        List<QaReview.QaDocumentSnapshot> safeDocuments = documents == null ? List.of() : List.copyOf(documents);
        String box;
        String profile;

        try (Connection connection = databaseConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                StoredHeader existing = findHeaderById(connection, reviewId);
                if (existing == null) {
                    throw new SQLException("QA review " + reviewId + " could not be found.");
                }
                box = existing.box();
                profile = existing.profile();

                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE qa_reviews
                        SET status = ?,
                            documents = ?,
                            pages = ?,
                            reviewed = ?,
                            issues = ?,
                            assigned_to_user_id = COALESCE(?, assigned_to_user_id),
                            completed_at = ?,
                            expires_at = ?,
                            last_updated_at = ?
                        WHERE id = ?
                        """)) {
                    update.setString(1, status.name());
                    update.setInt(2, countDocuments(safeDocuments));
                    update.setInt(3, totalPages);
                    update.setInt(4, reviewedPages);
                    update.setInt(5, issueCount);
                    setNullableInt(update, 6, assignedToUserId);
                    update.setTimestamp(7, Timestamp.from(now));
                    if (expiresAt == null) {
                        update.setNull(8, java.sql.Types.TIMESTAMP);
                    } else {
                        update.setTimestamp(8, Timestamp.from(expiresAt));
                    }
                    update.setTimestamp(9, Timestamp.from(now));
                    update.setString(10, reviewId.toString());
                    update.executeUpdate();
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
            replacePageRows(reviewId, box, profile, safeDocuments, now);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to complete QA review.", e);
        }
    }

    public QaReview.QaAssignmentSnapshot findById(UUID reviewId) {
        try (Connection connection = databaseConnection.getConnection()) {
            StoredHeader header = findHeaderById(connection, reviewId);
            return header == null ? null : toAssignment(connection, header);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to read QA review.", e);
        }
    }

    public void deleteExpiredCompletedReviews(NotificationDAO notificationDAO, AuditLogDAO auditLogDAO) {
        Instant now = Instant.now();
        try (Connection connection = databaseConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT id, session_id, box, profile, pages
                    FROM qa_reviews
                    WHERE status = ?
                      AND expires_at IS NOT NULL
                      AND expires_at < ?
                    """)) {
                statement.setString(1, QaReview.QaReviewStatus.APPROVED.name());
                statement.setTimestamp(2, Timestamp.from(now));
                List<ExpiredReview> expired = new ArrayList<>();
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        expired.add(new ExpiredReview(
                                UUID.fromString(resultSet.getString("id")),
                                resultSet.getString("session_id"),
                                resultSet.getString("box"),
                                resultSet.getString("profile"),
                                resultSet.getInt("pages")
                        ));
                    }
                }

                for (ExpiredReview review : expired) {
                    // Audit first, in the same transaction: if either write fails the
                    // whole batch rolls back, so we never lose scanned content without
                    // a trace.
                    writeScanDiscardedAudit(connection, auditLogDAO, review);
                    if (notificationDAO != null) {
                        notificationDAO.deleteByReviewId(connection, review.reviewId());
                    }
                    deletePageRows(connection, review.reviewId());
                    try (PreparedStatement deleteReview = connection.prepareStatement("""
                            DELETE FROM qa_reviews
                            WHERE id = ?
                            """)) {
                        deleteReview.setString(1, review.reviewId().toString());
                        deleteReview.executeUpdate();
                    }
                }

                connection.commit();
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException e) {
                connection.rollback();
                connection.setAutoCommit(previousAutoCommit);
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete expired QA reviews.", e);
        }
    }

    private void writeScanDiscardedAudit(Connection connection, AuditLogDAO auditLogDAO, ExpiredReview review)
            throws SQLException {
        if (auditLogDAO == null) {
            return;
        }

        String profile = review.profile() == null ? "" : review.profile().trim();
        String box = review.box() == null ? "" : review.box().trim();
        String target;
        if (profile.isBlank() && box.isBlank()) {
            target = "QA review " + review.reviewId();
        } else if (profile.isBlank()) {
            target = box;
        } else if (box.isBlank()) {
            target = profile;
        } else {
            target = profile + " / " + box;
        }

        List<AuditLog.AuditLogDetail> details = new ArrayList<>();
        if (!profile.isBlank()) {
            details.add(new AuditLog.AuditLogDetail("Profile", profile));
        }
        if (!box.isBlank()) {
            details.add(new AuditLog.AuditLogDetail("Box", box));
        }
        details.add(new AuditLog.AuditLogDetail("Review", review.reviewId().toString()));
        if (review.sessionId() != null && !review.sessionId().isBlank()) {
            details.add(new AuditLog.AuditLogDetail("Session", review.sessionId()));
        }
        details.add(new AuditLog.AuditLogDetail("Pages", String.valueOf(review.pageCount())));

        AuditLog log = new AuditLog(
                auditLogDAO.nextAuditLogId(connection),
                LocalDateTime.now(),
                "Scans",
                "SYSTEM",
                "SCAN_DISCARDED",
                target,
                "Success",
                "Expired approved QA review purged: " + review.pageCount() + " pages removed.",
                details
        );

        auditLogDAO.saveAuditLog(connection, log);
    }

    private record ExpiredReview(UUID reviewId, String sessionId, String box, String profile, int pageCount) {
    }

    private QaReview.QaAssignmentSnapshot toAssignment(Connection connection, StoredHeader header) throws SQLException {
        List<QaReview.QaDocumentSnapshot> documents = findDocumentsByReviewId(connection, header.reviewId());
        return toAssignment(header, documents);
    }

    private QaReview.QaAssignmentSnapshot toAssignmentSummary(StoredHeader header) {
        return toAssignment(header, List.of());
    }

    private QaReview.QaAssignmentSnapshot toAssignment(
            StoredHeader header,
            List<QaReview.QaDocumentSnapshot> documents
    ) {
        return new QaReview.QaAssignmentSnapshot(
                header.reviewId(),
                header.sessionId(),
                header.box(),
                header.profile(),
                header.createdByUserId(),
                header.assignedToUserId(),
                header.scannedBy(),
                header.status(),
                header.assignedAt(),
                header.startedAt(),
                header.completedAt(),
                header.reviewed(),
                header.pages(),
                header.issues(),
                documents
        );
    }

    private List<QaReview.QaDocumentSnapshot> findDocumentsByReviewId(Connection connection, UUID reviewId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT document_number,
                       document_name,
                       page_number,
                       global_page_number,
                       source_reference,
                       display_content,
                       rotation_degrees,
                       page_status,
                       page_readable,
                       rotation_correct,
                       split_correct,
                       page_count_correct,
                       comment
                FROM qa_review_pages
                WHERE qa_review_id = ?
                ORDER BY document_number, page_number, global_page_number
                """)) {
            statement.setString(1, reviewId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Integer, String> documentNames = new LinkedHashMap<>();
                Map<Integer, List<QaReview.QaPageSnapshot>> pagesByDocument = new LinkedHashMap<>();
                while (resultSet.next()) {
                    int documentNumber = resultSet.getInt("document_number");
                    documentNames.putIfAbsent(documentNumber, clean(resultSet.getString("document_name")));
                    pagesByDocument.computeIfAbsent(documentNumber, ignored -> new ArrayList<>())
                            .add(new QaReview.QaPageSnapshot(
                                    resultSet.getInt("page_number"),
                                    resultSet.getInt("global_page_number"),
                                    resultSet.getString("source_reference"),
                                    resultSet.getString("display_content"),
                                    resultSet.getInt("rotation_degrees"),
                                    QaReview.QaPageReviewStatus.valueOf(resultSet.getString("page_status")),
                                    resultSet.getBoolean("page_readable"),
                                    resultSet.getBoolean("rotation_correct"),
                                    resultSet.getBoolean("split_correct"),
                                    resultSet.getBoolean("page_count_correct"),
                                    resultSet.getString("comment")
                            ));
                }

                List<QaReview.QaDocumentSnapshot> documents = new ArrayList<>();
                for (Map.Entry<Integer, List<QaReview.QaPageSnapshot>> entry : pagesByDocument.entrySet()) {
                    List<QaReview.QaPageSnapshot> pages = new ArrayList<>(entry.getValue());
                    pages.sort(Comparator.comparingInt(QaReview.QaPageSnapshot::pageNumber));
                    documents.add(new QaReview.QaDocumentSnapshot(
                            entry.getKey(),
                            documentNames.getOrDefault(entry.getKey(), "Document " + entry.getKey()),
                            pages
                    ));
                }
                documents.sort(Comparator.comparingInt(QaReview.QaDocumentSnapshot::number));
                return documents;
            }
        }
    }

    private StoredHeader findHeaderBySessionId(Connection connection, UUID sessionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id,
                       session_id,
                       box,
                       status,
                       profile,
                       scanned_by,
                       documents,
                       pages,
                       assigned_at,
                       reviewed,
                       issues,
                       created_by_user_id,
                       assigned_to_user_id,
                       started_at,
                       completed_at,
                       expires_at,
                       last_updated_at
                FROM qa_reviews
                WHERE session_id = ?
                """)) {
            statement.setString(1, sessionId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? readHeader(resultSet) : null;
            }
        }
    }

    private StoredHeader findHeaderById(Connection connection, UUID reviewId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id,
                       session_id,
                       box,
                       status,
                       profile,
                       scanned_by,
                       documents,
                       pages,
                       assigned_at,
                       reviewed,
                       issues,
                       created_by_user_id,
                       assigned_to_user_id,
                       started_at,
                       completed_at,
                       expires_at,
                       last_updated_at
                FROM qa_reviews
                WHERE id = ?
                """)) {
            statement.setString(1, reviewId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? readHeader(resultSet) : null;
            }
        }
    }

    private StoredHeader readHeader(ResultSet resultSet) throws SQLException {
        return new StoredHeader(
                UUID.fromString(resultSet.getString("id")),
                UUID.fromString(resultSet.getString("session_id")),
                clean(resultSet.getString("box")),
                clean(resultSet.getString("profile")),
                clean(resultSet.getString("scanned_by")),
                resultSet.getInt("documents"),
                resultSet.getInt("pages"),
                resultSet.getTimestamp("assigned_at").toInstant(),
                resultSet.getInt("reviewed"),
                resultSet.getInt("issues"),
                nullableInteger(resultSet, "created_by_user_id"),
                nullableInteger(resultSet, "assigned_to_user_id"),
                QaReview.QaReviewStatus.valueOf(resultSet.getString("status")),
                timestampToInstant(resultSet, "started_at"),
                timestampToInstant(resultSet, "completed_at"),
                timestampToInstant(resultSet, "expires_at"),
                resultSet.getTimestamp("last_updated_at").toInstant()
        );
    }

    private void insertPageRows(
            Connection connection,
            UUID reviewId,
            String box,
            String profile,
            List<QaReview.QaDocumentSnapshot> documents,
            Instant now
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO qa_review_pages (
                    id,
                    qa_review_id,
                    box,
                    profile,
                    document_name,
                    document_number,
                    page_number,
                    global_page_number,
                    page_status,
                    rotation_degrees,
                    comment,
                    source_reference,
                    display_content,
                    page_readable,
                    rotation_correct,
                    split_correct,
                    page_count_correct,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            int pendingBatchRows = 0;
            for (QaReview.QaDocumentSnapshot document : documents) {
                if (document == null || document.pages() == null) {
                    continue;
                }
                for (QaReview.QaPageSnapshot page : document.pages()) {
                    if (page == null) {
                        continue;
                    }
                    statement.setString(1, UUID.randomUUID().toString());
                    statement.setString(2, reviewId.toString());
                    statement.setString(3, box);
                    statement.setString(4, profile);
                    statement.setString(5, clean(document.name()));
                    statement.setInt(6, document.number());
                    statement.setInt(7, page.pageNumber());
                    statement.setInt(8, page.globalPageNumber());
                    statement.setString(9, page.reviewStatus().name());
                    statement.setInt(10, page.rotationDegrees());
                    statement.setString(11, page.comment());
                    statement.setString(12, clean(page.sourceReference()));
                    statement.setString(13, clean(page.displayContent()));
                    statement.setBoolean(14, page.pageReadable());
                    statement.setBoolean(15, page.rotationCorrect());
                    statement.setBoolean(16, page.splitCorrect());
                    statement.setBoolean(17, page.pageCountCorrect());
                    statement.setTimestamp(18, Timestamp.from(now));
                    statement.addBatch();
                    pendingBatchRows++;
                    if (pendingBatchRows >= PAGE_ROW_BATCH_SIZE) {
                        statement.executeBatch();
                        pendingBatchRows = 0;
                    }
                }
            }
            if (pendingBatchRows > 0) {
                statement.executeBatch();
            }
        }
    }

    private void replacePageRows(
            UUID reviewId,
            String box,
            String profile,
            List<QaReview.QaDocumentSnapshot> documents,
            Instant now
    ) throws SQLException {
        try (Connection connection = databaseConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                deletePageRows(connection, reviewId);
                if (documents != null && !documents.isEmpty()) {
                    insertPageRows(connection, reviewId, box, profile, documents, now);
                }
                connection.commit();
            } catch (SQLException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private void deletePageRows(Connection connection, UUID reviewId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                DELETE FROM qa_review_pages
                WHERE qa_review_id = ?
                """)) {
            statement.setString(1, reviewId.toString());
            statement.executeUpdate();
        }
    }

    private int countDocuments(List<QaReview.QaDocumentSnapshot> documents) {
        if (documents == null) {
            return 0;
        }
        int count = 0;
        for (QaReview.QaDocumentSnapshot document : documents) {
            if (document != null && document.pages() != null && !document.pages().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private int countPages(List<QaReview.QaDocumentSnapshot> documents) {
        if (documents == null) {
            return 0;
        }
        int totalPages = 0;
        for (QaReview.QaDocumentSnapshot document : documents) {
            if (document != null && document.pages() != null) {
                totalPages += document.pages().size();
            }
        }
        return totalPages;
    }

    private void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private Integer nullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private Instant timestampToInstant(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private record StoredHeader(
            UUID reviewId,
            UUID sessionId,
            String box,
            String profile,
            String scannedBy,
            int documents,
            int pages,
            Instant assignedAt,
            int reviewed,
            int issues,
            Integer createdByUserId,
            Integer assignedToUserId,
            QaReview.QaReviewStatus status,
            Instant startedAt,
            Instant completedAt,
            Instant expiresAt,
            Instant lastUpdatedAt
    ) {
    }
}
