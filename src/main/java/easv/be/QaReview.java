package easv.be;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Shared QA review value types (statuses and immutable snapshots).
 *
 * These live in the entity layer so that both the business layer
 * ({@code easv.bll.QAService}) and the data layer ({@code easv.dal.QaReviewDAO},
 * {@code easv.dal.NotificationDAO}) can depend on them without the data layer
 * having to reach up into the business layer.
 */
public final class QaReview {

    private QaReview() {
    }

    public enum QaReviewStatus {
        WAITING_FOR_QA,
        IN_REVIEW,
        APPROVED,
        REJECTED
    }

    public enum QaPageReviewStatus {
        NOT_REVIEWED,
        APPROVED,
        NEEDS_FIX
    }

    public record QaPageSnapshot(
            int pageNumber,
            int globalPageNumber,
            String sourceReference,
            String displayContent,
            int rotationDegrees,
            QaPageReviewStatus reviewStatus,
            boolean pageReadable,
            boolean rotationCorrect,
            boolean splitCorrect,
            boolean pageCountCorrect,
            String comment
    ) {
        public QaPageSnapshot {
            sourceReference = sourceReference == null ? "" : sourceReference;
            displayContent = displayContent == null ? "" : displayContent;
            comment = comment == null ? "" : comment;
        }
    }

    public record QaDocumentSnapshot(int number, String name, List<QaPageSnapshot> pages) {
        public QaDocumentSnapshot {
            name = name == null || name.isBlank() ? "Document " + Math.max(1, number) : name.trim();
            pages = pages == null ? List.of() : List.copyOf(pages);
        }
    }

    public record QaAssignmentSnapshot(
            UUID reviewId,
            UUID sessionId,
            String boxId,
            String profileName,
            Integer createdByUserId,
            Integer assignedToUserId,
            String scannedByName,
            QaReviewStatus status,
            Instant submittedAt,
            Instant startedAt,
            Instant completedAt,
            int reviewedPages,
            int totalPages,
            int issueCount,
            List<QaDocumentSnapshot> documents
    ) {
        public QaAssignmentSnapshot {
            boxId = boxId == null ? "" : boxId;
            profileName = profileName == null ? "" : profileName;
            scannedByName = scannedByName == null ? "" : scannedByName;
            documents = documents == null ? List.of() : List.copyOf(documents);
        }
    }

    public record SessionQaState(
            UUID sessionId,
            QaReviewStatus status,
            Instant submittedAt,
            Instant completedAt,
            int reviewedPages,
            int totalPages,
            int issueCount
    ) {
    }

    public record NotificationSnapshot(
            UUID id,
            int userId,
            UUID reviewId,
            String title,
            String message,
            Instant createdAt,
            Instant readAt
    ) {
        public boolean unread() {
            return readAt == null;
        }
    }
}
