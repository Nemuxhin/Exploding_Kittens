package easv.bll;

import easv.be.Document;
import easv.be.PageImage;
import easv.be.ReviewRecord;
import easv.be.ScanProfile;
import easv.be.User;
import easv.dal.QaReviewDAO;
import easv.dal.ReviewRecordDAO;
import easv.dal.SavedScanProgressDAO;
import easv.util.Strings;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class AdminReviewService {
    private static final String QA_RECORD_PREFIX = "qa:";
    private static final DateTimeFormatter ADMIN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ReviewRecordDAO reviewRecordDAO;
    private final QaReviewDAO qaReviewDAO;
    private final SavedScanProgressDAO savedScanProgressDAO;
    private final QAService qaService;
    private final List<User> users;
    private final List<ScanProfile> profiles;
    private final List<ReviewRecord> reviewRecords;

    AdminReviewService(
            ReviewRecordDAO reviewRecordDAO,
            QaReviewDAO qaReviewDAO,
            SavedScanProgressDAO savedScanProgressDAO,
            QAService qaService,
            List<User> users,
            List<ScanProfile> profiles,
            List<ReviewRecord> reviewRecords
    ) {
        this.reviewRecordDAO = reviewRecordDAO;
        this.qaReviewDAO = qaReviewDAO;
        this.savedScanProgressDAO = savedScanProgressDAO;
        this.qaService = qaService;
        this.users = users;
        this.profiles = profiles;
        this.reviewRecords = reviewRecords;
    }

    List<ReviewRecord> getReviewRecords() {
        List<ReviewRecord> allRecords = new ArrayList<>();
        reviewRecords.stream()
                .map(this::copyReviewRecord)
                .forEach(allRecords::add);
        qaService.getAllAssignmentsForAdmin().stream()
                .map(this::toQaReviewRecord)
                .forEach(allRecords::add);
        return allRecords;
    }

    ReviewSaveResult saveReviewRecord(ReviewRecord updatedRecord) {
        if (updatedRecord == null || Strings.clean(updatedRecord.getId()).isBlank()) {
            throw new IllegalArgumentException("Review record is required.");
        }

        if (isQaRecordId(updatedRecord.getId())) {
            return new ReviewSaveResult(saveQaReviewRecord(updatedRecord), ReviewChange.NONE);
        }

        for (int index = 0; index < reviewRecords.size(); index++) {
            ReviewRecord existingRecord = reviewRecords.get(index);

            if (existingRecord.getId().equals(updatedRecord.getId())) {
                ReviewRecord savedRecord = copyReviewRecord(updatedRecord);
                reviewRecordDAO.saveReviewRecord(savedRecord);
                reviewRecords.set(index, savedRecord);
                return new ReviewSaveResult(copyReviewRecord(updatedRecord), ReviewChange.UPDATED);
            }
        }

        ReviewRecord savedRecord = copyReviewRecord(updatedRecord);
        reviewRecordDAO.saveReviewRecord(savedRecord);
        reviewRecords.add(savedRecord);
        return new ReviewSaveResult(copyReviewRecord(updatedRecord), ReviewChange.CREATED);
    }

    ReviewRecord assignReviewRecordToQa(String recordId, Integer reviewerUserId) {
        if (!isQaRecordId(recordId)) {
            return null;
        }

        QAService.QaAssignmentSnapshot assignment = qaService.assignReview(parseQaReviewId(recordId), reviewerUserId);
        return assignment == null ? null : toQaReviewRecord(assignment);
    }

    QAService.QaAssignmentSnapshot getQaAssignmentForReviewRecord(String recordId) {
        if (!isQaRecordId(recordId)) {
            return null;
        }
        return qaReviewDAO.findById(parseQaReviewId(recordId));
    }

    List<Document> getExportableDocumentsForRecord(String recordId) {
        QAService.QaAssignmentSnapshot assignment = getQaAssignmentForReviewRecord(recordId);
        if (assignment == null || assignment.status() != QAService.QaReviewStatus.APPROVED) {
            return List.of();
        }

        List<Document> documents = new ArrayList<>();
        for (QAService.QaDocumentSnapshot qaDocument : assignment.documents()) {
            List<PageImage> pages = new ArrayList<>();
            for (QAService.QaPageSnapshot qaPage : qaDocument.pages()) {
                if (qaPage.reviewStatus() != QAService.QaPageReviewStatus.APPROVED) {
                    continue;
                }
                String sourceReference = qaPage.sourceReference() == null || qaPage.sourceReference().isBlank()
                        ? "Document " + qaDocument.number()
                        : qaPage.sourceReference();
                PageImage pageImage = new PageImage(qaPage.pageNumber(), PageImage.PageType.TIFF, sourceReference);
                pageImage.setRotationDegrees(qaPage.rotationDegrees());
                pageImage.setDisplayContent(qaPage.displayContent());
                pages.add(pageImage);
            }
            if (!pages.isEmpty()) {
                documents.add(new Document(
                        "document_" + String.format(Locale.US, "%03d", qaDocument.number()),
                        pages
                ));
            }
        }
        return documents;
    }

    List<QAService.QaDocumentSnapshot> getSavedProgressDocumentsForReviewRecord(String boxId, String profileName) {
        SavedScanProgressDAO.StoredProgress progress = savedScanProgressDAO.findLatestByBoxAndProfile(boxId, profileName);
        if (progress == null || progress.pages().isEmpty()) {
            return List.of();
        }

        Map<Integer, List<QAService.QaPageSnapshot>> pagesByDocument = new HashMap<>();
        for (SavedScanProgressDAO.StoredPage page : progress.pages()) {
            if (page == null || page.barcode()) {
                continue;
            }

            String previewContent = page.previewContent() == null || page.previewContent().isBlank()
                    ? page.displayContent()
                    : page.previewContent();

            pagesByDocument.computeIfAbsent(Math.max(1, page.documentNumber()), ignored -> new ArrayList<>())
                    .add(new QAService.QaPageSnapshot(
                            Math.max(1, page.referenceId()),
                            Math.max(1, page.fileId()),
                            page.sourceReference(),
                            previewContent,
                            page.rotationDegrees(),
                            QAService.QaPageReviewStatus.NOT_REVIEWED,
                            false,
                            false,
                            false,
                            false,
                            ""
                    ));
        }

        if (pagesByDocument.isEmpty()) {
            return List.of();
        }

        List<QAService.QaDocumentSnapshot> documents = new ArrayList<>();
        pagesByDocument.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    List<QAService.QaPageSnapshot> pages = new ArrayList<>(entry.getValue());
                    pages.sort(Comparator.comparingInt(QAService.QaPageSnapshot::pageNumber));
                    documents.add(new QAService.QaDocumentSnapshot(
                            entry.getKey(),
                            "Document " + entry.getKey(),
                            pages
                    ));
                });
        return documents;
    }

    List<User> getEligibleQaAssignees(String recordId) {
        if (!isQaRecordId(recordId)) {
            return List.of();
        }

        QAService.QaAssignmentSnapshot assignment = qaReviewDAO.findById(parseQaReviewId(recordId));
        if (assignment == null) {
            return List.of();
        }

        List<User> activeUsers = users.stream()
                .filter(User::isActive)
                .filter(user -> assignment.createdByUserId() == null || user.getId() != assignment.createdByUserId())
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<User> matchingProfile = activeUsers.stream()
                .filter(user -> user.getAssignedProfiles().stream()
                        .anyMatch(profile -> profile.equalsIgnoreCase(assignment.profileName())))
                .toList();

        return matchingProfile.isEmpty() ? activeUsers : matchingProfile;
    }

    void loadReviewRecords() {
        reviewRecords.clear();
        reviewRecords.addAll(reviewRecordDAO.getReviewRecords());
    }

    private ReviewRecord copyReviewRecord(ReviewRecord record) {
        return new ReviewRecord(
                record.getId(),
                record.getIdentity(),
                record.getClient(),
                record.getArchive(),
                record.getProfile(),
                record.getMetadataStatus(),
                record.getQaStatus(),
                record.getPages(),
                record.getLastUpdated(),
                record.getAssignedTo(),
                record.getScannedBy(),
                record.getDateGroup(),
                record.hasWarning()
        );
    }

    private ReviewRecord saveQaReviewRecord(ReviewRecord updatedRecord) {
        UUID reviewId = parseQaReviewId(updatedRecord.getId());
        QAService.QaAssignmentSnapshot assignment = qaReviewDAO.findById(reviewId);
        if (assignment == null) {
            throw new IllegalArgumentException("QA review could not be found.");
        }

        QAService.QaReviewStatus targetStatus = toQaReviewStatus(updatedRecord.getQaStatus());
        int reviewedPages = Math.max(assignment.reviewedPages(), assignment.totalPages());
        int issueCount = Math.max(assignment.issueCount(), updatedRecord.hasWarning() ? 1 : 0);

        if (targetStatus == QAService.QaReviewStatus.APPROVED || targetStatus == QAService.QaReviewStatus.REJECTED) {
            qaService.completeReview(
                    reviewId,
                    targetStatus == QAService.QaReviewStatus.APPROVED,
                    reviewedPages,
                    assignment.totalPages(),
                    issueCount,
                    assignment.documents()
            );
        } else {
            qaReviewDAO.saveProgress(
                    reviewId,
                    targetStatus,
                    reviewedPages,
                    assignment.totalPages(),
                    issueCount,
                    assignment.documents()
            );
        }

        QAService.QaAssignmentSnapshot saved = qaReviewDAO.findById(reviewId);
        return saved == null ? updatedRecord : toQaReviewRecord(saved);
    }

    private ReviewRecord toQaReviewRecord(QAService.QaAssignmentSnapshot assignment) {
        Map<Integer, User> usersById = new HashMap<>();
        for (User user : users) {
            usersById.put(user.getId(), user);
        }

        String client = findProfileOptionalByName(assignment.profileName())
                .map(ScanProfile::getClient)
                .orElse("");

        String assignedTo = "";
        if (assignment.assignedToUserId() != null) {
            User assignedUser = usersById.get(assignment.assignedToUserId());
            if (assignedUser != null) {
                assignedTo = assignedUser.getName();
            }
        }

        String metadataStatus = assignment.status() == QAService.QaReviewStatus.REJECTED
                ? "Returned for changes"
                : "Complete";

        return new ReviewRecord(
                QA_RECORD_PREFIX + assignment.reviewId(),
                assignment.boxId(),
                client,
                assignment.boxId(),
                assignment.profileName(),
                metadataStatus,
                toAdminQaStatus(assignment.status()),
                assignment.totalPages(),
                formatTimestamp(assignment.completedAt() != null ? assignment.completedAt() : assignment.submittedAt()),
                assignedTo,
                assignment.scannedByName(),
                formatTimestamp(assignment.submittedAt()),
                assignment.issueCount() > 0 || assignment.status() == QAService.QaReviewStatus.REJECTED
        );
    }

    private String toAdminQaStatus(QAService.QaReviewStatus status) {
        return switch (status) {
            case WAITING_FOR_QA -> "Ready for QA";
            case IN_REVIEW -> "QA In Progress";
            case APPROVED -> "QA Approved";
            case REJECTED -> "QA Rejected";
        };
    }

    private QAService.QaReviewStatus toQaReviewStatus(String status) {
        String normalized = Strings.normalize(status);
        return switch (normalized) {
            case "qa approved" -> QAService.QaReviewStatus.APPROVED;
            case "qa rejected" -> QAService.QaReviewStatus.REJECTED;
            case "qa in progress" -> QAService.QaReviewStatus.IN_REVIEW;
            case "ready for qa", "waiting for qa" -> QAService.QaReviewStatus.WAITING_FOR_QA;
            default -> QAService.QaReviewStatus.WAITING_FOR_QA;
        };
    }

    private boolean isQaRecordId(String recordId) {
        return Strings.clean(recordId).startsWith(QA_RECORD_PREFIX);
    }

    private UUID parseQaReviewId(String recordId) {
        String value = Strings.clean(recordId);
        return UUID.fromString(value.substring(QA_RECORD_PREFIX.length()));
    }

    private String formatTimestamp(java.time.Instant instant) {
        if (instant == null) {
            return "";
        }
        return ADMIN_TIME_FORMATTER.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    private java.util.Optional<ScanProfile> findProfileOptionalByName(String profileName) {
        String normalizedProfileName = Strings.normalize(profileName);

        return profiles.stream()
                .filter(profile -> Strings.normalize(profile.getName()).equals(normalizedProfileName))
                .findFirst();
    }

    record ReviewSaveResult(ReviewRecord record, ReviewChange change) {
    }

    enum ReviewChange {
        NONE,
        CREATED,
        UPDATED
    }
}
