package easv.bll;

import easv.be.Document;
import easv.be.PageImage;
import easv.be.QaReview;
import easv.be.ScanProfile;
import easv.be.User;
import easv.dal.BoxDAO;
import easv.dal.DataAccessException;
import easv.dal.ExportDAO;
import easv.dal.SavedScanProgressDAO;
import easv.dal.ScanProfileDAO;
import easv.dal.ScanSessionDAO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserPortalModel {
    private static final String BOX_ID_PATTERN = "^BOX-\\d{4}-\\d{3}$";
    private static final AccountProfile DEFAULT_ACCOUNT =
            new AccountProfile("User", "", "Scanning");
    private static final DateTimeFormatter HISTORY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ScanProfileDAO scanProfileDAO;
    private final BoxDAO boxDAO;
    private final ScanSessionDAO scanSessionDAO;
    private final ExportDAO exportDAO;
    private final QAService qaService;
    private final SavedScanProgressDAO savedScanProgressDAO;
    private AccountProfile accountProfile = DEFAULT_ACCOUNT;

    public UserPortalModel() {
        this(new ScanProfileDAO(), new BoxDAO(), new ScanSessionDAO(), new ExportDAO());
    }

    UserPortalModel(ScanProfileDAO scanProfileDAO, BoxDAO boxDAO, ScanSessionDAO scanSessionDAO, ExportDAO exportDAO) {
        this.scanProfileDAO = scanProfileDAO;
        this.boxDAO = boxDAO;
        this.scanSessionDAO = scanSessionDAO;
        this.exportDAO = exportDAO;
        this.qaService = new QAService();
        this.savedScanProgressDAO = new SavedScanProgressDAO();
        syncAccountFromSession();
    }

    public record DashboardMetric(String label, String value) {}
    public record AccountProfile(String fullName, String email, String department) {}
    public record ScanProfileInfo(String metadataRequired, String qaRequired, String splittingMethod) {}
    public record BoxItem(String id, String description) {
        @Override
        public String toString() {
            return id;
        }
    }
    public record ProfileSetting(String label, String value) {}
    public record ProfileItem(int id, String name, String description, boolean defaultProfile, List<BoxItem> assignedBoxes) {
        @Override
        public String toString() {
            return name;
        }
    }
    public record RecentScanItem(java.util.UUID sessionId, String boxId, String profileName, String status, String startedAt, int pages) {}
    public record HistoryItem(java.util.UUID sessionId, String boxId, String profileName, int documents, String status, String startedAt, String completedAt, int pages, String size) {}
    public record ExportItem(java.util.UUID sessionId, String fileName, String boxId, String profileName, int documents, String createdAt, String size, String status) {}
    public record InMemoryScanPage(
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
    public record InMemoryScanDocument(int number, String splitReason, List<InMemoryScanPage> pages, boolean pending) {
        public InMemoryScanDocument {
            pages = pages == null ? List.of() : List.copyOf(pages);
        }
    }
    public record InMemoryScanProgress(
            String boxId,
            String profileName,
            List<InMemoryScanDocument> documents,
            List<InMemoryScanPage> pages,
            LocalDateTime savedAt,
            String status
    ) {
        public InMemoryScanProgress {
            documents = documents == null ? List.of() : List.copyOf(documents);
            pages = pages == null ? List.of() : List.copyOf(pages);
            savedAt = savedAt == null ? LocalDateTime.now() : savedAt;
            status = status == null || status.isBlank() ? "Saved in memory" : status.trim();
        }

        public int normalPageCount() {
            return (int) pages.stream().filter(page -> !page.barcode()).count();
        }
    }
    public record PortalSession(ProfileItem profile, BoxItem box) {
        public String exportName() {
            return profile.name() + "_" + box.id();
        }
    }

    public List<DashboardMetric> fetchDashboardMetrics() {
        syncAccountFromSession();

        List<HistoryItem> history = fetchScanHistory();
        int documents = history.stream()
                .mapToInt(HistoryItem::documents)
                .sum();
        int pages = history.stream()
                .mapToInt(HistoryItem::pages)
                .sum();

        return List.of(
                new DashboardMetric("Assigned Profiles", String.valueOf(fetchAssignedProfileCount())),
                new DashboardMetric("Batches", String.valueOf(history.size())),
                new DashboardMetric("Documents", String.valueOf(documents)),
                new DashboardMetric("Pages", String.valueOf(pages))
        );
    }

    public List<ProfileItem> fetchProfilesForUser() {
        syncAccountFromSession();

        User currentUser = UserSession.getCurrentUser();
        List<ScanProfile> availableProfiles = safeProfiles();

        if (availableProfiles.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> allowedNames = new LinkedHashSet<>();
        if (currentUser != null
                && currentUser.getAssignedProfiles() != null
                && !currentUser.getAssignedProfiles().isEmpty()) {
            allowedNames.addAll(currentUser.getAssignedProfiles());
        }

        List<ScanProfile> filteredProfiles = allowedNames.isEmpty()
                ? availableProfiles.stream()
                .filter(profile -> !profile.isArchived())
                .toList()
                : availableProfiles.stream()
                .filter(profile -> !profile.isArchived())
                .filter(profile -> allowedNames.stream()
                        .anyMatch(assigned -> assigned.equalsIgnoreCase(profile.getName())))
                .toList();

        List<BoxItem> boxes = fetchDistinctBoxes();
        List<ProfileItem> items = new ArrayList<>();

        for (int index = 0; index < filteredProfiles.size(); index++) {
            ScanProfile profile = filteredProfiles.get(index);
            items.add(new ProfileItem(
                    profile.getId(),
                    profile.getName(),
                    profile.getDescription(),
                    index == 0,
                    boxes
            ));
        }

        return items;
    }

    public ProfileItem getDefaultProfileForUser() {
        return fetchProfilesForUser().stream()
                .findFirst()
                .orElse(null);
    }

    public List<RecentScanItem> fetchRecentScans() {
        return fetchScanHistory().stream()
                .limit(5)
                .map(item -> new RecentScanItem(
                        item.sessionId(),
                        item.boxId(),
                        item.profileName(),
                        item.status(),
                        item.startedAt(),
                        item.pages()
                ))
                .toList();
    }

    public RecentScanItem fetchLatestInterruptedScan() {
        try {
            Integer currentUserId = UserSession.getCurrentUser() == null ? null : UserSession.getCurrentUser().getId();
            SavedScanProgressDAO.StoredProgressSummary progress = savedScanProgressDAO.findLatestForUser(currentUserId);
            if (progress == null) {
                return null;
            }

            return new RecentScanItem(
                    progress.sessionId(),
                    progress.boxId(),
                    progress.profileName(),
                    progress.status(),
                    formatHistoryTime(progress.savedAt()),
                    progress.pageCount()
            );
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public List<HistoryItem> fetchScanHistory() {
        try {
            Integer currentUserId = UserSession.getCurrentUser() == null ? null : UserSession.getCurrentUser().getId();
            Map<java.util.UUID, QaReview.SessionQaState> qaStates = currentUserId == null
                    ? Map.of()
                    : qaService.getReviewStatesForCurrentUser();

            return scanSessionDAO.findHistorySummariesForUser(currentUserId).stream()
                    .map(summary -> {
                        QaReview.SessionQaState qaState = qaStates.get(summary.sessionId());
                        String status = qaState == null
                                ? summary.status()
                                : toHistoryStatus(qaState.status());
                        String completedAt = qaState != null && qaState.completedAt() != null
                                ? formatHistoryTime(qaState.completedAt())
                                : isCompletedStatus(summary.status()) ? formatHistoryTime(summary.startedAt()) : "-";
                        return new HistoryItem(
                                summary.sessionId(),
                                summary.boxId(),
                                summary.profileName(),
                                summary.documentCount(),
                                status,
                                formatHistoryTime(summary.startedAt()),
                                completedAt,
                                summary.pageCount(),
                                formatHistorySize(summary.totalSizeBytes())
                        );
                    })
                    .toList();
        } catch (DataAccessException exception) {
            return List.of();
        }
    }

    public List<ExportItem> fetchExports() {
        try {
            Integer currentUserId = UserSession.getCurrentUser() == null ? null : UserSession.getCurrentUser().getId();
            return exportDAO.findByExportedByUser(currentUserId).stream()
                    .map(item -> new ExportItem(
                            item.sessionId(),
                            item.fileName(),
                            item.boxId(),
                            item.profileName(),
                            item.documentCount(),
                            formatHistoryTime(item.exportedAt()),
                            formatFileSize(item.filePath()),
                            displayExportStatus(item.exportStatus())
                    ))
                    .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    public List<Document> fetchExportDocuments(ExportItem item) {
        if (item == null || item.sessionId() == null) {
            return List.of();
        }
        return qaService.getApprovedExportsForCurrentUser().stream()
                .filter(assignment -> item.sessionId().equals(assignment.sessionId()))
                .findFirst()
                .map(this::toExportDocuments)
                .orElse(List.of());
    }

    public ScanProfile fetchExportProfile(ExportItem item) {
        if (item == null || item.profileName() == null || item.profileName().isBlank()) {
            return null;
        }

        return fetchScanProfileByName(item.profileName());
    }

    public ScanProfile fetchScanProfileByName(String profileName) {
        if (profileName == null || profileName.isBlank()) {
            return null;
        }

        return safeProfiles().stream()
                .filter(profile -> profile.getName().equalsIgnoreCase(profileName))
                .findFirst()
                .orElse(null);
    }

    public List<ProfileSetting> fetchProfileSettings(ProfileItem profile) {
        ScanProfile scanProfile = findProfile(profile);
        if (scanProfile == null) {
            return List.of();
        }

        return List.of(
                new ProfileSetting("Rotation", emptyAsDash(scanProfile.getDefaultRotation())),
                new ProfileSetting("Brightness", emptyAsDash(scanProfile.getBrightness())),
                new ProfileSetting("Contrast", emptyAsDash(scanProfile.getContrast())),
                new ProfileSetting("Export format", emptyAsDash(scanProfile.getExportFormat())),
                new ProfileSetting("Barcode behavior", emptyAsDash(scanProfile.getBarcodeDetectedBehavior()))
        );
    }

    public ScanProfileInfo fetchScanProfileInfo(ProfileItem profile) {
        ScanProfile scanProfile = findProfile(profile);
        if (scanProfile == null) {
            return new ScanProfileInfo("-", "-", "-");
        }

        String metadataRequired = scanProfile.isMetadataRequiredBeforeExport() ? "Yes" : "No";
        String qaRequired = scanProfile.isMetadataRequiredBeforeExport() ? "Yes" : "No";
        String splittingMethod = scanProfile.isBarcodeSplitting() ? "Barcode" : "Manual";
        return new ScanProfileInfo(metadataRequired, qaRequired, splittingMethod);
    }

    public AccountProfile fetchAccountProfile() {
        syncAccountFromSession();
        return accountProfile;
    }

    public void updateAccountProfile(String fullName, String email, String department) {
        accountProfile = new AccountProfile(
                normalizedValue(fullName, accountProfile.fullName()),
                normalizedValue(email, accountProfile.email()),
                normalizedValue(department, accountProfile.department())
        );
    }

    public void saveScanProgress(java.util.UUID sessionId, InMemoryScanProgress progress) {
        if (sessionId == null || progress == null) {
            return;
        }

        Integer currentUserId = UserSession.getCurrentUser() == null ? null : UserSession.getCurrentUser().getId();
        savedScanProgressDAO.save(sessionId, currentUserId, new SavedScanProgressDAO.StoredProgress(
                progress.boxId(),
                progress.profileName(),
                progress.status(),
                progress.savedAt().atZone(ZoneId.systemDefault()).toInstant(),
                progress.pages().stream()
                        .map(page -> new SavedScanProgressDAO.StoredPage(
                                page.referenceId(),
                                page.fileId(),
                                page.documentNumber(),
                                page.barcode(),
                                page.rotationDegrees(),
                                page.needsRescan(),
                                page.splitReasonAfter(),
                                page.sourceReference(),
                                page.displayContent(),
                                page.previewContent()
                        ))
                        .toList()
        ));
    }

    public void submitScanForQa(java.util.UUID sessionId, InMemoryScanProgress progress) {
        if (sessionId == null || progress == null) {
            return;
        }

        saveScanProgress(sessionId, new InMemoryScanProgress(
                progress.boxId(),
                progress.profileName(),
                progress.documents(),
                progress.pages(),
                LocalDateTime.now(),
                "Submitted for QA"
        ));
        qaService.submitScanForQa(
                sessionId,
                progress.boxId(),
                progress.profileName(),
                toQaDocumentSnapshots(progress)
        );
        savedScanProgressDAO.deleteBySessionId(sessionId);
    }

    public List<QaReview.QaAssignmentSnapshot> fetchAssignedQaAssignments() {
        try {
            return qaService.getAssignmentsForCurrentUser();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    public List<QaReview.NotificationSnapshot> fetchNotifications() {
        try {
            return qaService.getNotificationsForCurrentUser();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    public void markAllNotificationsRead() {
        try {
            qaService.markAllNotificationsRead();
        } catch (RuntimeException exception) {
            // Keep the portal responsive when notifications cannot be updated.
        }
    }

    public void markQaAssignmentStarted(java.util.UUID reviewId) {
        if (reviewId == null) {
            return;
        }
        qaService.markAssignmentStarted(reviewId);
    }

    public void saveQaProgress(
            java.util.UUID reviewId,
            QaReview.QaReviewStatus status,
            int reviewedPages,
            int totalPages,
            int issueCount,
            List<QaReview.QaDocumentSnapshot> documents
    ) {
        if (reviewId == null) {
            return;
        }
        qaService.saveProgress(reviewId, status, reviewedPages, totalPages, issueCount, documents);
    }

    public void completeQaReview(
            java.util.UUID reviewId,
            boolean approved,
            int reviewedPages,
            int totalPages,
            int issueCount,
            List<QaReview.QaDocumentSnapshot> documents
    ) {
        if (reviewId == null) {
            return;
        }
        qaService.completeReview(reviewId, approved, reviewedPages, totalPages, issueCount, documents);
    }

    public InMemoryScanProgress fetchReturnedQaProgress(java.util.UUID sessionId) {
        if (sessionId == null) {
            return null;
        }
        try {
            QaReview.QaAssignmentSnapshot assignment = qaService.getReturnedAssignmentForSession(sessionId);
            if (assignment == null) {
                return null;
            }
            return toInMemoryScanProgress(assignment);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public InMemoryScanProgress fetchSavedScanProgress(java.util.UUID sessionId) {
        if (sessionId == null) {
            return null;
        }

        try {
            Integer currentUserId = UserSession.getCurrentUser() == null ? null : UserSession.getCurrentUser().getId();
            SavedScanProgressDAO.StoredProgress progress = savedScanProgressDAO.findBySessionId(sessionId, currentUserId);
            if (progress == null) {
                return null;
            }

            List<InMemoryScanPage> pages = progress.pages().stream()
                    .map(page -> new InMemoryScanPage(
                            page.referenceId(),
                            page.fileId(),
                            page.documentNumber(),
                            page.barcode(),
                            page.rotationDegrees(),
                            page.needsRescan(),
                            page.splitReasonAfter(),
                            page.sourceReference(),
                            page.displayContent(),
                            page.previewContent()
                    ))
                    .toList();

            return new InMemoryScanProgress(
                    progress.boxId(),
                    progress.profileName(),
                    List.of(),
                    pages,
                    LocalDateTime.ofInstant(progress.savedAt(), ZoneId.systemDefault()),
                    progress.status()
            );
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public void clearSavedScanProgress(java.util.UUID sessionId) {
        if (sessionId == null) {
            return;
        }
        savedScanProgressDAO.deleteBySessionId(sessionId);
    }

    public String formatExportName(String profileName, String boxId) {
        return profileName.toLowerCase(Locale.ROOT).replace(' ', '_') + "_" + boxId;
    }

    public boolean isValidBoxId(String boxId) {
        return boxId != null && boxId.trim().matches(BOX_ID_PATTERN);
    }

    public PortalSession startSession(ProfileItem profile, String boxId) {
        String normalizedBoxId = boxId == null || boxId.isBlank() ? "BOX-UNSPECIFIED" : boxId.trim();
        String description = "Scanning session";
        return new PortalSession(profile, new BoxItem(normalizedBoxId, description));
    }

    public PortalSession resumeSession(RecentScanItem item) {
        return resumeSession(item.boxId(), item.profileName());
    }

    public PortalSession resumeSession(HistoryItem item) {
        return resumeSession(item.boxId(), item.profileName());
    }

    private PortalSession resumeSession(String boxId, String profileName) {
        ProfileItem profile = fetchProfilesForUser().stream()
                .filter(candidate -> candidate.name().equalsIgnoreCase(profileName))
                .findFirst()
                .orElse(getDefaultProfileForUser());
        return new PortalSession(profile, new BoxItem(boxId, "Resumed from stored history"));
    }

    private void syncAccountFromSession() {
        User currentUser = UserSession.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String department = currentUser.getAssignedProfiles().isEmpty()
                ? "Scanning"
                : currentUser.getAssignedProfiles().get(0);

        accountProfile = new AccountProfile(
                normalizedValue(currentUser.getName(), DEFAULT_ACCOUNT.fullName()),
                normalizedValue(currentUser.getEmail(), DEFAULT_ACCOUNT.email()),
                normalizedValue(department, DEFAULT_ACCOUNT.department())
        );
    }

    private List<ScanProfile> safeProfiles() {
        try {
            return scanProfileDAO.findAll();
        } catch (DataAccessException exception) {
            return List.of();
        }
    }

    private int fetchAssignedProfileCount() {
        User currentUser = UserSession.getCurrentUser();
        List<ScanProfile> availableProfiles = safeProfiles();
        if (availableProfiles.isEmpty()) {
            return 0;
        }

        LinkedHashSet<String> allowedNames = new LinkedHashSet<>();
        if (currentUser != null && currentUser.getAssignedProfiles() != null && !currentUser.getAssignedProfiles().isEmpty()) {
            allowedNames.addAll(currentUser.getAssignedProfiles());
        }

        return (int) (allowedNames.isEmpty()
                ? availableProfiles.stream().filter(profile -> !profile.isArchived()).count()
                : availableProfiles.stream()
                .filter(profile -> allowedNames.stream().anyMatch(assigned -> assigned.equalsIgnoreCase(profile.getName())))
                .count());
    }

    private List<BoxItem> fetchDistinctBoxes() {
        try {
            return boxDAO.findAll().stream()
                    .map(box -> new BoxItem(box.getBoxId(), box.getDescription()))
                    .toList();
        } catch (DataAccessException exception) {
            return List.of();
        }
    }

    private ScanProfile findProfile(ProfileItem profile) {
        if (profile == null) {
            return null;
        }
        return safeProfiles().stream()
                .filter(candidate -> candidate.getId() == profile.id() || candidate.getName().equalsIgnoreCase(profile.name()))
                .findFirst()
                .orElse(null);
    }

    private String normalizedValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String emptyAsDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String formatHistoryTime(java.time.Instant instant) {
        return HISTORY_TIME_FORMAT.format(instant.atZone(ZoneId.systemDefault()));
    }

    private String formatFileSize(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "-";
        }

        try {
            long sizeBytes = Files.size(Path.of(filePath));
            if (sizeBytes <= 0) {
                return "-";
            }

            double sizeKb = sizeBytes / 1024.0;
            if (sizeKb < 1024) {
                return String.format(Locale.US, "%.1f KB", sizeKb);
            }

            double sizeMb = sizeKb / 1024.0;
            return String.format(Locale.US, "%.1f MB", sizeMb);
        } catch (RuntimeException | java.io.IOException ignored) {
            return "-";
        }
    }

    private String displayExportStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Unknown";
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "SUCCESS" -> "Success";
            case "FAILED" -> "Failed";
            default -> status.trim();
        };
    }

    private boolean isCompletedStatus(String status) {
        return "Completed".equalsIgnoreCase(status);
    }

    private String toHistoryStatus(QaReview.QaReviewStatus status) {
        if (status == null) {
            return "Processing";
        }
        return switch (status) {
            case WAITING_FOR_QA -> "Submitted for QA";
            case IN_REVIEW -> "QA In Progress";
            case APPROVED -> "QA Approved";
            case REJECTED -> "QA Rejected";
        };
    }

    private String formatHistorySize(long totalSizeBytes) {
        if (totalSizeBytes <= 0) {
            return "-";
        }

        double sizeKb = totalSizeBytes / 1024.0;
        if (sizeKb < 1024) {
            return String.format(Locale.US, "%.1f KB", sizeKb);
        }

        double sizeMb = sizeKb / 1024.0;
        return String.format(Locale.US, "%.1f MB", sizeMb);
    }

    private List<Document> toExportDocuments(QaReview.QaAssignmentSnapshot assignment) {
        List<Document> documents = new ArrayList<>();
        if (assignment == null) {
            return documents;
        }

        for (QaReview.QaDocumentSnapshot qaDocument : assignment.documents()) {
            List<PageImage> pages = new ArrayList<>();
            for (QaReview.QaPageSnapshot qaPage : qaDocument.pages()) {
                if (qaPage.reviewStatus() != QaReview.QaPageReviewStatus.APPROVED) {
                    continue;
                }
                PageImage pageImage = new PageImage(
                        qaPage.pageNumber(),
                        PageImage.PageType.TIFF,
                        normalizedValue(qaPage.sourceReference(), "Document " + qaDocument.number())
                );
                pageImage.setRotationDegrees(qaPage.rotationDegrees());
                pageImage.setDisplayContent(qaPage.displayContent());
                pages.add(pageImage);
            }
            if (!pages.isEmpty()) {
                documents.add(new Document("document_" + String.format(Locale.US, "%03d", qaDocument.number()), pages));
            }
        }

        return documents;
    }

    private List<QaReview.QaDocumentSnapshot> toQaDocumentSnapshots(InMemoryScanProgress progress) {
        List<QaReview.QaDocumentSnapshot> documents = new ArrayList<>();
        if (progress == null || progress.documents() == null) {
            return documents;
        }

        int globalPageNumber = 0;
        for (InMemoryScanDocument document : progress.documents()) {
            List<QaReview.QaPageSnapshot> pages = new ArrayList<>();
            for (InMemoryScanPage page : document.pages()) {
                if (page.barcode()) {
                    continue;
                }
                globalPageNumber++;
                pages.add(new QaReview.QaPageSnapshot(
                        pages.size() + 1,
                        globalPageNumber,
                        page.sourceReference(),
                        firstNonBlank(page.previewContent(), page.displayContent()),
                        page.rotationDegrees(),
                        QaReview.QaPageReviewStatus.NOT_REVIEWED,
                        false,
                        false,
                        false,
                        false,
                        ""
                ));
            }
            if (!pages.isEmpty()) {
                documents.add(new QaReview.QaDocumentSnapshot(
                        document.number(),
                        deriveDocumentName(document),
                        pages
                ));
            }
        }
        return documents;
    }

    private InMemoryScanProgress toInMemoryScanProgress(QaReview.QaAssignmentSnapshot assignment) {
        List<InMemoryScanDocument> documents = new ArrayList<>();
        List<InMemoryScanPage> pages = new ArrayList<>();
        int referenceId = 1;
        int fileId = 1;

        for (QaReview.QaDocumentSnapshot document : assignment.documents()) {
            List<InMemoryScanPage> documentPages = new ArrayList<>();
            for (int pageIndex = 0; pageIndex < document.pages().size(); pageIndex++) {
                QaReview.QaPageSnapshot page = document.pages().get(pageIndex);
                InMemoryScanPage storedPage = new InMemoryScanPage(
                        referenceId++,
                        fileId++,
                        document.number(),
                        false,
                        page.rotationDegrees(),
                        page.reviewStatus() == QaReview.QaPageReviewStatus.NEEDS_FIX,
                        pageIndex == document.pages().size() - 1 ? "Finish batch" : "",
                        page.sourceReference(),
                        page.displayContent(),
                        page.displayContent()
                );
                documentPages.add(storedPage);
                pages.add(storedPage);
            }
            documents.add(new InMemoryScanDocument(document.number(), "", documentPages, false));
        }

        return new InMemoryScanProgress(
                assignment.boxId(),
                assignment.profileName(),
                documents,
                pages,
                LocalDateTime.now(),
                toHistoryStatus(assignment.status())
        );
    }

    private String deriveDocumentName(InMemoryScanDocument document) {
        if (document != null && document.pages() != null) {
            for (InMemoryScanPage page : document.pages()) {
                if (page != null && page.sourceReference() != null && !page.sourceReference().isBlank()) {
                    return page.sourceReference().trim();
                }
            }
        }
        return "Document " + (document == null ? "?" : document.number());
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback == null ? "" : fallback;
    }
}
