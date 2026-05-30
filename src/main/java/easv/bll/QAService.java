package easv.bll;

import easv.be.AuditLog;
import easv.be.Document;
import easv.be.PageImage;
import easv.be.User;
import easv.dal.AuditLogDAO;
import easv.dal.NotificationDAO;
import easv.dal.QaReviewDAO;
import easv.dal.UserDAO;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class QAService {
    private final QaReviewDAO qaReviewDAO;
    private final NotificationDAO notificationDAO;
    private final UserDAO userDAO;
    private final AuditLogDAO auditLogDAO;

    public QAService() {
        this(new QaReviewDAO(), new NotificationDAO(), new UserDAO(), new AuditLogDAO());
    }

    QAService(QaReviewDAO qaReviewDAO, NotificationDAO notificationDAO, UserDAO userDAO) {
        this(qaReviewDAO, notificationDAO, userDAO, new AuditLogDAO());
    }

    QAService(QaReviewDAO qaReviewDAO, NotificationDAO notificationDAO, UserDAO userDAO, AuditLogDAO auditLogDAO) {
        this.qaReviewDAO = Objects.requireNonNull(qaReviewDAO, "qaReviewDAO");
        this.notificationDAO = Objects.requireNonNull(notificationDAO, "notificationDAO");
        this.userDAO = Objects.requireNonNull(userDAO, "userDAO");
        this.auditLogDAO = Objects.requireNonNull(auditLogDAO, "auditLogDAO");
    }

    public void submitScanForQa(UUID sessionId, String boxId, String profileName, List<QaDocumentSnapshot> documents) {
        cleanupExpiredCompletedReviews();
        User currentUser = UserSession.getCurrentUser();
        Integer creatorUserId = currentUser == null ? null : currentUser.getId();
        String scannedBy = currentUser == null ? "" : currentUser.getName();

        QaAssignmentSnapshot assignment = qaReviewDAO.createOrResetSubmission(
                sessionId,
                boxId,
                profileName,
                creatorUserId,
                null,
                scannedBy,
                documents
        );

        writeQaAuditLog(
                "Submitted for QA",
                qaTarget(profileName, boxId),
                "Success",
                "Scan submitted for QA review."
        );
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

    public List<QaAssignmentSnapshot> getAllAssignmentSummariesForAdmin() {
        cleanupExpiredCompletedReviews();
        return qaReviewDAO.findAllSummaries();
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

        writeQaAuditLog(
                approved ? "Approved QA" : "Rejected QA",
                qaTarget(assignment.profileName(), assignment.boxId()),
                approved ? "Success" : "Failed",
                approved
                        ? "QA approved — ready for export."
                        : "QA rejected — returned to scan owner."
        );
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

            String reviewerName = reviewerNameFor(resolvedReviewerId);
            writeQaAuditLog(
                    "Assigned QA reviewer",
                    qaTarget(assignment.profileName(), assignment.boxId()),
                    "Success",
                    reviewerName.isBlank()
                            ? "QA reviewer assigned."
                            : "QA review assigned to " + reviewerName + "."
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

    private String reviewerNameFor(Integer reviewerId) {
        if (reviewerId == null) {
            return "";
        }
        return userDAO.getAllUsers().stream()
                .filter(user -> user.getId() == reviewerId)
                .map(User::getName)
                .map(this::clean)
                .findFirst()
                .orElse("");
    }

    private String qaTarget(String profileName, String boxId) {
        String profile = clean(profileName);
        String box = clean(boxId);
        if (profile.isBlank() && box.isBlank()) {
            return "QA";
        }
        if (profile.isBlank()) {
            return box;
        }
        if (box.isBlank()) {
            return profile;
        }
        return profile + " / " + box;
    }

    private void writeQaAuditLog(String action, String target, String status, String description) {
        try {
            User currentUser = UserSession.getCurrentUser();
            String actor = currentUser == null ? "SYSTEM" : currentUser.getUsername();
            AuditLog log = new AuditLog(
                    auditLogDAO.nextAuditLogId(),
                    LocalDateTime.now(),
                    "QA",
                    actor,
                    action,
                    target,
                    status,
                    description,
                    List.of()
            );
            auditLogDAO.saveAuditLog(log);
        } catch (RuntimeException ignored) {
        }
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
