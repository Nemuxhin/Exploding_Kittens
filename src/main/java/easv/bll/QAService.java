package easv.bll;

import easv.be.Document;
import easv.be.PageImage;
import easv.be.User;
import easv.dal.NotificationDAO;
import easv.dal.QaReviewDAO;
import easv.dal.UserDAO;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class QAService {
    private final QaReviewDAO qaReviewDAO;
    private final NotificationDAO notificationDAO;
    private final UserDAO userDAO;

    public QAService() {
        this(new QaReviewDAO(), new NotificationDAO(), new UserDAO());
    }

    QAService(QaReviewDAO qaReviewDAO, NotificationDAO notificationDAO, UserDAO userDAO) {
        this.qaReviewDAO = Objects.requireNonNull(qaReviewDAO, "qaReviewDAO");
        this.notificationDAO = Objects.requireNonNull(notificationDAO, "notificationDAO");
        this.userDAO = Objects.requireNonNull(userDAO, "userDAO");
    }

    public void submitScanForQa(UUID sessionId, String boxId, String profileName, List<QaDocumentSnapshot> documents) {
        cleanupExpiredCompletedReviews();
        User currentUser = UserSession.getCurrentUser();
        Integer creatorUserId = currentUser == null ? null : currentUser.getId();
        String scannedBy = currentUser == null ? "" : currentUser.getName();
        Integer assignedReviewerId = selectReviewer(profileName, creatorUserId);

        QaAssignmentSnapshot assignment = qaReviewDAO.createOrResetSubmission(
                sessionId,
                boxId,
                profileName,
                creatorUserId,
                assignedReviewerId,
                scannedBy,
                documents
        );

        if (assignedReviewerId != null) {
            notificationDAO.create(
                    assignedReviewerId,
                    assignment.reviewId(),
                    "New QA work assigned",
                    "Box " + assignment.boxId() + " from " + assignment.profileName() + " is waiting for QA."
            );
        }
    }

    public List<QaAssignmentSnapshot> getAssignmentsForCurrentUser() {
        cleanupExpiredCompletedReviews();
        User currentUser = requireCurrentUser();
        return qaReviewDAO.findAssignmentsByAssignee(currentUser.getId());
    }

    public List<QaAssignmentSnapshot> getAllAssignmentsForAdmin() {
        cleanupExpiredCompletedReviews();
        return qaReviewDAO.findAll();
    }

    public Map<UUID, SessionQaState> getReviewStatesForCurrentUser() {
        cleanupExpiredCompletedReviews();
        User currentUser = requireCurrentUser();
        return qaReviewDAO.findReviewStatesByCreator(currentUser.getId());
    }

    public List<QaAssignmentSnapshot> getApprovedExportsForCurrentUser() {
        cleanupExpiredCompletedReviews();
        User currentUser = requireCurrentUser();
        return qaReviewDAO.findApprovedByCreator(currentUser.getId());
    }

    public QaAssignmentSnapshot getReturnedAssignmentForSession(UUID sessionId) {
        cleanupExpiredCompletedReviews();
        User currentUser = requireCurrentUser();
        return qaReviewDAO.findRejectedBySessionForCreator(sessionId, currentUser.getId());
    }

    public void markAssignmentStarted(UUID reviewId) {
        qaReviewDAO.markStarted(reviewId);
    }

    public void saveProgress(
            UUID reviewId,
            QaReviewStatus status,
            int reviewedPages,
            int totalPages,
            int issueCount,
            List<QaDocumentSnapshot> documents
    ) {
        qaReviewDAO.saveProgress(reviewId, status, reviewedPages, totalPages, issueCount, documents);
    }

    public void completeReview(
            UUID reviewId,
            boolean approved,
            int reviewedPages,
            int totalPages,
            int issueCount,
            List<QaDocumentSnapshot> documents
    ) {
        QaAssignmentSnapshot assignment = qaReviewDAO.findById(reviewId);
        if (assignment == null) {
            return;
        }

        QaReviewStatus completionStatus = approved ? QaReviewStatus.APPROVED : QaReviewStatus.REJECTED;
        Integer nextAssignee = approved ? assignment.assignedToUserId() : assignment.createdByUserId();

        qaReviewDAO.completeReview(
                reviewId,
                completionStatus,
                nextAssignee,
                reviewedPages,
                totalPages,
                issueCount,
                documents
        );

        if (assignment.createdByUserId() != null) {
            if (approved) {
                notificationDAO.create(
                        assignment.createdByUserId(),
                        reviewId,
                        "QA approved " + assignment.boxId(),
                        "Your scan for " + assignment.profileName() + " passed QA and is ready for export."
                );
            } else {
                notificationDAO.create(
                        assignment.createdByUserId(),
                        reviewId,
                        "QA rejected " + assignment.boxId(),
                        buildRejectionMessage(assignment.profileName(), documents)
                );
            }
        }
    }

    public QaAssignmentSnapshot assignReview(UUID reviewId) {
        return assignReview(reviewId, null);
    }

    public QaAssignmentSnapshot assignReview(UUID reviewId, Integer reviewerId) {
        cleanupExpiredCompletedReviews();
        QaAssignmentSnapshot assignment = qaReviewDAO.findById(reviewId);
        if (assignment == null) {
            return null;
        }

        Integer resolvedReviewerId = reviewerId;
        if (resolvedReviewerId == null) {
            resolvedReviewerId = assignment.assignedToUserId();
        }
        if (resolvedReviewerId == null
                || assignment.createdByUserId() != null && resolvedReviewerId.equals(assignment.createdByUserId())) {
            resolvedReviewerId = selectReviewer(assignment.profileName(), assignment.createdByUserId());
        }

        qaReviewDAO.assignReviewer(reviewId, resolvedReviewerId, QaReviewStatus.WAITING_FOR_QA);

        if (resolvedReviewerId != null) {
            notificationDAO.create(
                    resolvedReviewerId,
                    reviewId,
                    "New QA work assigned",
                    "Box " + assignment.boxId() + " from " + assignment.profileName() + " is waiting for QA."
            );
        }

        return qaReviewDAO.findById(reviewId);
    }

    public List<NotificationSnapshot> getNotificationsForCurrentUser() {
        User currentUser = requireCurrentUser();
        return notificationDAO.findByUser(currentUser.getId());
    }

    public void markAllNotificationsRead() {
        User currentUser = requireCurrentUser();
        notificationDAO.markAllRead(currentUser.getId());
    }

    public void cleanupExpiredCompletedReviews() {
        qaReviewDAO.deleteExpiredCompletedReviews(notificationDAO);
    }

    private Integer selectReviewer(String profileName, Integer creatorUserId) {
        List<User> users = userDAO.getAllUsers().stream()
                .filter(User::isActive)
                .filter(user -> creatorUserId == null || user.getId() != creatorUserId)
                .toList();

        for (User user : users) {
            if (matchesAssignedProfile(user, profileName)) {
                return user.getId();
            }
        }

        return users.isEmpty() ? creatorUserId : users.get(0).getId();
    }

    private boolean matchesAssignedProfile(User user, String profileName) {
        if (user == null || profileName == null || profileName.isBlank()) {
            return false;
        }
        return user.getAssignedProfiles().stream()
                .anyMatch(profile -> profile.equalsIgnoreCase(profileName));
    }

    private String buildRejectionMessage(String profileName, List<QaDocumentSnapshot> documents) {
        List<String> comments = new ArrayList<>();
        if (documents != null) {
            for (QaDocumentSnapshot document : documents) {
                if (document == null || document.pages() == null) {
                    continue;
                }
                for (QaPageSnapshot page : document.pages()) {
                    if (page == null || page.reviewStatus() != QaPageReviewStatus.NEEDS_FIX) {
                        continue;
                    }
                    String comment = clean(page.comment());
                    if (comment.isBlank()) {
                        comment = "Needs correction.";
                    }
                    comments.add("Document " + document.number() + ", page " + page.pageNumber() + ": " + comment);
                    if (comments.size() >= 3) {
                        break;
                    }
                }
                if (comments.size() >= 3) {
                    break;
                }
            }
        }

        if (comments.isEmpty()) {
            return "Your " + clean(profileName) + " scan was rejected and returned for changes.";
        }

        return "Returned for changes. " + String.join(" | ", comments);
    }

    private User requireCurrentUser() {
        User currentUser = UserSession.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("No current user is available.");
        }
        return currentUser;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static List<QaDocumentSnapshot> fromDocuments(List<Document> documents) {
        List<QaDocumentSnapshot> snapshots = new ArrayList<>();
        int globalPageNumber = 0;
        if (documents == null) {
            return snapshots;
        }

        for (int documentIndex = 0; documentIndex < documents.size(); documentIndex++) {
            Document document = documents.get(documentIndex);
            if (document == null) {
                continue;
            }

            List<QaPageSnapshot> pages = new ArrayList<>();
            for (PageImage page : document.getPages()) {
                if (page.getPageType() == PageImage.PageType.BARCODE) {
                    continue;
                }
                globalPageNumber++;
                pages.add(new QaPageSnapshot(
                        page.getPageNumber(),
                        globalPageNumber,
                        page.getSourceReference(),
                        page.getDisplayContent(),
                        page.getRotationDegrees(),
                        QaPageReviewStatus.NOT_REVIEWED,
                        false,
                        false,
                        false,
                        false,
                        ""
                ));
            }

            if (!pages.isEmpty()) {
                snapshots.add(new QaDocumentSnapshot(
                        documentIndex + 1,
                        "Document " + (documentIndex + 1),
                        pages
                ));
            }
        }

        return snapshots;
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
