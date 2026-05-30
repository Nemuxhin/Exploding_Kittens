package easv.bll;

import easv.be.AuditLog;
import easv.be.AuditLog.AuditLogDetail;
import easv.be.Document;
import easv.be.PageImage;
import easv.be.ReviewRecord;
import easv.be.ScanProfile;
import easv.be.User;
import easv.dal.AuditLogDAO;
import easv.dal.MetadataDAO;
import easv.dal.QaReviewDAO;
import easv.dal.SavedScanProgressDAO;
import easv.dal.UserDAO;
import easv.gui.controller.util.Strings;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class AdminManager {
    private static final String QA_RECORD_PREFIX = "qa:";
    private static final DateTimeFormatter ADMIN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserDAO userDAO;
    private final MetadataDAO metadataDAO;
    private final AuditLogDAO auditLogDAO;
    private final QaReviewDAO qaReviewDAO;
    private final SavedScanProgressDAO savedScanProgressDAO;
    private final QAService qaService;

    private final List<User> users = new ArrayList<>();
    private final List<ScanProfile> profiles = new ArrayList<>();
    private final List<ReviewRecord> reviewRecords = new ArrayList<>();
    private final List<AuditLog> auditLogs = new ArrayList<>();

    private final Map<Integer, Set<Integer>> profileAssignments = new HashMap<>();

    private int nextUserId = 1;
    private int nextProfileId = 1;
    private int nextAuditLogId = 1;

    public AdminManager() {
        this(new UserDAO(), new MetadataDAO(), new AuditLogDAO());
    }

    public AdminManager(UserDAO userDAO) {
        this(userDAO, new MetadataDAO(), new AuditLogDAO());
    }

    public AdminManager(UserDAO userDAO, MetadataDAO metadataDAO, AuditLogDAO auditLogDAO) {
        this.userDAO = userDAO == null ? new UserDAO() : userDAO;
        this.metadataDAO = metadataDAO == null ? new MetadataDAO() : metadataDAO;
        this.auditLogDAO = auditLogDAO == null ? new AuditLogDAO() : auditLogDAO;
        this.qaReviewDAO = new QaReviewDAO();
        this.savedScanProgressDAO = new SavedScanProgressDAO();
        this.qaService = new QAService();
        loadAdminData();
    }

    public List<User> getUsers() {
        return users.stream()
                .sorted(Comparator.comparingInt(User::getId))
                .toList();
    }

    public List<User> getQaEligibleUsers() {
        return users.stream()
                .filter(User::isActive)
                .filter(user -> !"Admin".equalsIgnoreCase(user.getRole()))
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public User createUser(UserInput input) {
        validateUserInput(input, null);

        User user = new User(
                0,
                input.getName(),
                input.getUsername(),
                input.getEmail(),
                PasswordHasher.hash(Strings.clean(input.getPlainPassword())),
                input.getRole(),
                input.getStatus(),
                input.getAssignedProfiles(),
                false,
                true
        );

        User savedUser = userDAO.saveUser(user, profileIdsForNames(input.getAssignedProfiles()));
        users.add(savedUser);
        syncProfileAssignmentsForUser(savedUser);
        nextUserId = Math.max(nextUserId, savedUser.getId() + 1);

        addAuditLog("Users", "Created user", savedUser.getName(), "Success",
                "A new user account was created.",
                createdUserChanges(savedUser));

        return savedUser;
    }

    public User updateUser(int userId, UserInput input) {
        User user = findRequiredUser(userId);
        validateUserInput(input, userId);
        User previousUser = copyUser(user);
        boolean passwordChanged = !Strings.clean(input.getPlainPassword()).isBlank();

        String updatedPasswordHash = user.getPasswordHash();

        if (passwordChanged) {
            updatedPasswordHash = PasswordHasher.hash(Strings.clean(input.getPlainPassword()));
        }

        boolean mustChangePassword = input.getMustChangePassword() == null
                ? user.isMustChangePassword()
                : input.getMustChangePassword();

        User updatedUser = new User(
                user.getId(),
                input.getName(),
                input.getUsername(),
                input.getEmail(),
                updatedPasswordHash,
                input.getRole(),
                input.getStatus(),
                input.getAssignedProfiles(),
                user.isCurrentUser(),
                mustChangePassword
        );

        User savedUser = userDAO.updateUser(updatedUser, profileIdsForNames(input.getAssignedProfiles()));

        user.setName(savedUser.getName());
        user.setUsername(savedUser.getUsername());
        user.setEmail(savedUser.getEmail());
        user.setPasswordHash(savedUser.getPasswordHash());
        user.setRole(savedUser.getRole());
        user.setStatus(savedUser.getStatus());
        user.setAssignedProfiles(savedUser.getAssignedProfiles());
        user.setMustChangePassword(savedUser.isMustChangePassword());

        syncProfileAssignmentsForUser(user);

        List<AuditLogDetail> userChanges = changedUserFields(previousUser, user, passwordChanged);
        if (!userChanges.isEmpty()) {
            addAuditLog("Users", "Updated user", user.getName(), "Success",
                    "User details were updated.",
                    userChanges);
        }

        return user;
    }

    public void deleteUser(int userId) {
        User user = findRequiredUser(userId);

        if (user.isCurrentUser()) {
            throw new IllegalArgumentException("The current user cannot be deleted.");
        }

        userDAO.deleteUser(userId);
        removeUserFromAssignments(userId);
        users.remove(user);

        addAuditLog("Users", "Deleted user", user.getName(), "Success",
                "A user account was deleted.",
                deletedUserChanges(user));
    }

    public User deactivateUser(int userId) {
        User user = findRequiredUser(userId);

        if (user.isCurrentUser()) {
            throw new IllegalArgumentException("The current user cannot be deactivated.");
        }

        if (!user.isActive()) {
            return user;
        }

        User deactivatedUser = new User(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                "Inactive",
                user.getAssignedProfiles(),
                user.isCurrentUser(),
                user.isMustChangePassword()
        );

        User savedUser = userDAO.updateUser(deactivatedUser, profileIdsForNames(user.getAssignedProfiles()));

        user.setName(savedUser.getName());
        user.setUsername(savedUser.getUsername());
        user.setEmail(savedUser.getEmail());
        user.setPasswordHash(savedUser.getPasswordHash());
        user.setRole(savedUser.getRole());
        user.setStatus(savedUser.getStatus());
        user.setAssignedProfiles(savedUser.getAssignedProfiles());
        user.setMustChangePassword(savedUser.isMustChangePassword());

        addAuditLog("Users", "Deactivated user", user.getName(), "Success",
                "A user account was deactivated.",
                changeList("Status", "Active", user.getStatus()));

        return user;
    }

    public User reactivateUser(int userId) {
        User user = findRequiredUser(userId);

        if (user.isActive()) {
            return user;
        }

        User reactivatedUser = new User(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                "Active",
                user.getAssignedProfiles(),
                user.isCurrentUser(),
                user.isMustChangePassword()
        );

        User savedUser = userDAO.updateUser(reactivatedUser, profileIdsForNames(user.getAssignedProfiles()));

        user.setName(savedUser.getName());
        user.setUsername(savedUser.getUsername());
        user.setEmail(savedUser.getEmail());
        user.setPasswordHash(savedUser.getPasswordHash());
        user.setRole(savedUser.getRole());
        user.setStatus(savedUser.getStatus());
        user.setAssignedProfiles(savedUser.getAssignedProfiles());
        user.setMustChangePassword(savedUser.isMustChangePassword());

        addAuditLog("Users", "Reactivated user", user.getName(), "Success",
                "A user account was reactivated.",
                changeList("Status", "Inactive", user.getStatus()));

        return user;
    }

    public boolean usernameExists(String username, Integer excludedUserId) {
        String normalizedUsername = Strings.normalize(username);

        if (normalizedUsername.isBlank()) {
            return false;
        }

        return users.stream()
                .filter(user -> excludedUserId == null || user.getId() != excludedUserId)
                .map(User::getUsername)
                .map(Strings::normalize)
                .anyMatch(existingUsername -> existingUsername.equals(normalizedUsername));
    }

    public List<ScanProfile> getProfiles() {
        return profiles.stream()
                .sorted(Comparator.comparingInt(ScanProfile::getId))
                .toList();
    }

    public List<ScanProfile> getAssignableProfiles() {
        return profiles.stream()
                .filter(profile -> !profile.isArchived())
                .sorted(Comparator.comparing(ScanProfile::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public ScanProfile createProfile(ProfileInput input) {
        validateProfileInput(input, null);

        ScanProfile profile = new ScanProfile(
                0,
                input.getName(),
                input.getClient(),
                input.getCode(),
                input.getDescription(),
                input.getStatus(),
                input.getMetadataTemplateName(),
                input.getExportNaming(),
                "Created just now",
                isArchivedStatus(input.getStatus()),
                input.isBarcodeSplitting(),
                input.getBarcodeDetectedBehavior(),
                input.getBarcodePageBehavior(),
                input.getDefaultRotation(),
                input.getBrightness(),
                input.getContrast(),
                input.isDeskew(),
                input.getExportFormat(),
                input.isMetadataRequiredBeforeExport()
        );

        ScanProfile savedProfile = metadataDAO.saveProfile(profile);
        profiles.add(savedProfile);
        nextProfileId = Math.max(nextProfileId, savedProfile.getId() + 1);

        addAuditLog("Profiles", "Created profile", savedProfile.getName(), "Success",
                "A scan profile was created.",
                createdProfileChanges(savedProfile));

        return savedProfile;
    }

    public ScanProfile updateProfile(int profileId, ProfileInput input) {
        ScanProfile profile = findRequiredProfile(profileId);
        validateProfileInput(input, profileId);

        ScanProfile previousProfile = copyProfile(profile);
        String previousName = profile.getName();

        profile.setName(input.getName());
        profile.setClient(input.getClient());
        profile.setCode(input.getCode());
        profile.setDescription(input.getDescription());
        profile.setStatus(input.getStatus());
        profile.setMetadataTemplateName(input.getMetadataTemplateName());
        profile.setExportNaming(input.getExportNaming());
        profile.setLastUpdated("Updated just now");
        profile.setArchived(isArchivedStatus(input.getStatus()));
        profile.setBarcodeSplitting(input.isBarcodeSplitting());
        profile.setBarcodeDetectedBehavior(input.getBarcodeDetectedBehavior());
        profile.setBarcodePageBehavior(input.getBarcodePageBehavior());
        profile.setDefaultRotation(input.getDefaultRotation());
        profile.setBrightness(input.getBrightness());
        profile.setContrast(input.getContrast());
        profile.setDeskew(input.isDeskew());
        profile.setExportFormat(input.getExportFormat());
        profile.setMetadataRequiredBeforeExport(input.isMetadataRequiredBeforeExport());

        metadataDAO.updateProfile(profile);
        renameAssignedProfile(previousName, profile.getName());

        List<AuditLogDetail> profileChanges = changedProfileFields(previousProfile, profile);
        if (!profileChanges.isEmpty()) {
            addAuditLog("Profiles", "Updated profile", profile.getName(), "Success",
                    "A scan profile was updated.",
                    profileChanges);
        }

        return profile;
    }

    public void archiveProfile(int profileId) {
        ScanProfile profile = findRequiredProfile(profileId);
        String previousStatus = profile.getStatus();
        profile.setArchived(true);
        profile.setStatus("Archived");
        profile.setLastUpdated("Archived just now");

        metadataDAO.updateProfile(profile);

        addAuditLog("Profiles", "Archived profile", profile.getName(), "Success",
                "A scan profile was archived.",
                changeList("Profile status", previousStatus, "Archived"));
    }

    public void restoreProfile(int profileId) {
        ScanProfile profile = findRequiredProfile(profileId);
        String previousStatus = profile.getStatus();
        profile.setArchived(false);
        profile.setStatus("Active");
        profile.setLastUpdated("Restored just now");

        metadataDAO.updateProfile(profile);

        addAuditLog("Profiles", "Restored profile", profile.getName(), "Success",
                "A scan profile was restored.",
                changeList("Profile status", previousStatus, "Active"));
    }

    public void deleteProfile(int profileId) {
        ScanProfile profile = findRequiredProfile(profileId);
        String deletedProfileName = profile.getName();
        String normalizedDeletedProfileName = Strings.normalize(deletedProfileName);

        metadataDAO.deleteProfile(profileId);

        profiles.removeIf(storedProfile -> storedProfile.getId() == profileId);
        profileAssignments.remove(profileId);

        users.forEach(user -> user.setAssignedProfiles(
                user.getAssignedProfiles().stream()
                        .filter(profileName -> !Strings.normalize(profileName).equals(normalizedDeletedProfileName))
                        .toList()
        ));

        addAuditLog("Profiles", "Deleted profile", deletedProfileName, "Success",
                "A scan profile was deleted.",
                deletedProfileChanges(profile));
    }

    public boolean profileCodeExists(String code, Integer excludedProfileId) {
        String normalizedCode = Strings.normalize(code);

        if (normalizedCode.isBlank()) {
            return false;
        }

        return profiles.stream()
                .filter(profile -> excludedProfileId == null || profile.getId() != excludedProfileId)
                .map(ScanProfile::getCode)
                .map(Strings::normalize)
                .anyMatch(existingCode -> existingCode.equals(normalizedCode));
    }

    public List<ReviewRecord> getReviewRecords() {
        List<ReviewRecord> allRecords = new ArrayList<>();
        reviewRecords.stream()
                .map(this::copyReviewRecord)
                .forEach(allRecords::add);
        qaService.getAllAssignmentSummariesForAdmin().stream()
                .map(this::toQaReviewRecord)
                .forEach(allRecords::add);
        return allRecords;
    }

    public ReviewRecord saveReviewRecord(ReviewRecord updatedRecord) {
        if (updatedRecord == null || Strings.clean(updatedRecord.getId()).isBlank()) {
            throw new IllegalArgumentException("Review record is required.");
        }

        if (isQaRecordId(updatedRecord.getId())) {
            return saveQaReviewRecord(updatedRecord);
        }

        for (int index = 0; index < reviewRecords.size(); index++) {
            ReviewRecord existingRecord = reviewRecords.get(index);

            if (existingRecord.getId().equals(updatedRecord.getId())) {
                ReviewRecord savedRecord = copyReviewRecord(updatedRecord);
                metadataDAO.saveReviewRecord(savedRecord);
                reviewRecords.set(index, savedRecord);

                addAuditLog("Review", "Updated review", updatedRecord.getIdentity(), "Success",
                        "A review record was updated.");

                return copyReviewRecord(updatedRecord);
            }
        }

        ReviewRecord savedRecord = copyReviewRecord(updatedRecord);
        metadataDAO.saveReviewRecord(savedRecord);
        reviewRecords.add(savedRecord);

        addAuditLog("Review", "Created review", updatedRecord.getIdentity(), "Success",
                "A review record was created.");

        return copyReviewRecord(updatedRecord);
    }

    public ReviewRecord assignReviewRecordToQa(String recordId) {
        return assignReviewRecordToQa(recordId, null);
    }

    public ReviewRecord assignReviewRecordToQa(String recordId, Integer reviewerUserId) {
        if (!isQaRecordId(recordId)) {
            return null;
        }

        QAService.QaAssignmentSnapshot assignment = qaService.assignReview(parseQaReviewId(recordId), reviewerUserId);
        return assignment == null ? null : toQaReviewRecord(assignment);
    }

    public QAService.QaAssignmentSnapshot getQaAssignmentForReviewRecord(String recordId) {
        if (!isQaRecordId(recordId)) {
            return null;
        }
        return qaReviewDAO.findById(parseQaReviewId(recordId));
    }

    public List<Document> getExportableDocumentsForRecord(String recordId) {
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

    public List<QAService.QaDocumentSnapshot> getSavedProgressDocumentsForReviewRecord(String boxId, String profileName) {
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

    public ScanProfile findProfileByName(String profileName) {
        if (profileName == null || profileName.isBlank()) {
            return null;
        }
        String normalized = profileName.trim();
        return profiles.stream()
                .filter(profile -> profile.getName().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
    }

    public List<User> getEligibleQaAssignees(String recordId) {
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

    public Map<Integer, Set<Integer>> getProfileAssignments() {
        return copyAssignments(profileAssignments);
    }

    public Set<Integer> getAssignedUserIds(int profileId) {
        return new HashSet<>(profileAssignments.getOrDefault(profileId, Set.of()));
    }

    public List<Integer> getAssignedProfileIds(int userId) {
        return profileAssignments.entrySet().stream()
                .filter(entry -> entry.getValue().contains(userId))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    public void saveProfileAssignments(Map<Integer, Set<Integer>> assignments) {
        Map<Integer, Set<Integer>> previousAssignments = copyAssignments(profileAssignments);
        Map<Integer, Set<Integer>> updatedAssignments = copyAssignments(assignments);

        profileAssignments.clear();
        profileAssignments.putAll(updatedAssignments);
        userDAO.replaceProfileAssignments(profileAssignments);
        refreshUsersFromProfileAssignments();

        addAuditLog("Access", "Updated profile access", "Assignments", "Success",
                "Profile access assignments were saved.",
                changedProfileAssignments(previousAssignments, updatedAssignments));
    }

    public List<AuditLog> getAuditLogs() {
        loadAuditLogs();
        return auditLogs.stream()
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .toList();
    }

    public AuditLog addAuditLog(String type, String action, String target,
                                String status, String description) {
        return addAuditLog(type, currentActorName(), action, target, status, description, List.of());
    }

    public AuditLog addAuditLog(String type, String action, String target,
                                String status, String description, List<AuditLogDetail> details) {
        return addAuditLog(type, currentActorName(), action, target, status, description, details);
    }

    public AuditLog addAuditLog(String type, String actor, String action, String target,
                                String status, String description) {
        return addAuditLog(type, actor, action, target, status, description, List.of());
    }

    public AuditLog addAuditLog(String type, String actor, String action, String target,
                                String status, String description, List<AuditLogDetail> details) {
        AuditLog log = new AuditLog(
                nextAuditLogId++,
                LocalDateTime.now(),
                type,
                actor,
                action,
                target,
                status,
                description,
                details
        );

        AuditLog savedLog = auditLogDAO.saveAuditLog(log);
        auditLogs.add(savedLog);
        nextAuditLogId = Math.max(nextAuditLogId, savedLog.getId() + 1);
        return savedLog;
    }

    public DashboardSummary getDashboardSummary() {
        loadAuditLogs();
        int totalUsers = users.size();

        int activeProfiles = (int) profiles.stream()
                .filter(profile -> !profile.isArchived())
                .filter(profile -> "Active".equalsIgnoreCase(profile.getStatus()))
                .count();

        int draftProfiles = (int) profiles.stream()
                .filter(profile -> "Draft".equalsIgnoreCase(profile.getStatus()))
                .count();

        int usersWithoutProfiles = (int) users.stream()
                .filter(user -> user.getAssignedProfiles().isEmpty())
                .filter(user -> !"Admin".equalsIgnoreCase(user.getRole()))
                .count();

        int failedEvents = (int) auditLogs.stream()
                .filter(log -> "Failed".equalsIgnoreCase(log.getStatus()))
                .count();

        return new DashboardSummary(totalUsers, activeProfiles, draftProfiles, usersWithoutProfiles, failedEvents);
    }

    private ReviewRecord copyReviewRecord(ReviewRecord record) {
        return new ReviewRecord(
                record.getId(),
                record.getIdentity(),
                record.getClient(),
                record.getArchive(),
                record.getProfile(),
                record.getMetadataTemplate(),
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

    private void loadAdminData() {
        loadUsers();
        loadProfiles();
        loadReviewRecords();
        loadProfileAssignments();
        loadAuditLogs();
        refreshUsersFromProfileAssignments();
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

    private void loadUsers() {
        users.clear();
        users.addAll(userDAO.getAllUsers());
        nextUserId = userDAO.nextUserId();
    }

    private void loadProfiles() {
        profiles.clear();
        profiles.addAll(metadataDAO.getProfiles());
        nextProfileId = metadataDAO.nextProfileId();
    }

    private void loadReviewRecords() {
        reviewRecords.clear();
        reviewRecords.addAll(metadataDAO.getReviewRecords());
    }

    private void loadProfileAssignments() {
        profileAssignments.clear();
        profileAssignments.putAll(copyAssignments(userDAO.getProfileAssignments()));
    }

    private void loadAuditLogs() {
        auditLogs.clear();
        auditLogs.addAll(auditLogDAO.getAllAuditLogs());
        nextAuditLogId = auditLogDAO.nextAuditLogId();
    }

    private String currentActorName() {
        User currentUser = UserSession.getCurrentUser();

        if (currentUser == null || Strings.clean(currentUser.getName()).isBlank()) {
            return "Admin";
        }

        return currentUser.getName();
    }

    private User findRequiredUser(int userId) {
        return users.stream()
                .filter(user -> user.getId() == userId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User could not be found."));
    }

    private ScanProfile findRequiredProfile(int profileId) {
        return profiles.stream()
                .filter(profile -> profile.getId() == profileId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile could not be found."));
    }

    private void validateUserInput(UserInput input, Integer excludedUserId) {
        if (input == null) {
            throw new IllegalArgumentException("User details are required.");
        }

        if (Strings.clean(input.getName()).isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }

        if (Strings.clean(input.getUsername()).isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }

        if (usernameExists(input.getUsername(), excludedUserId)) {
            throw new IllegalArgumentException("Username must be unique.");
        }

        if (excludedUserId == null && Strings.clean(input.getPlainPassword()).isBlank()) {
            throw new IllegalArgumentException("Temporary password is required.");
        }

        if (!Strings.clean(input.getEmail()).isBlank() && !isValidEmail(input.getEmail())) {
            throw new IllegalArgumentException("Email must be valid if entered.");
        }

        if (Strings.clean(input.getRole()).isBlank()) {
            throw new IllegalArgumentException("Role is required.");
        }

        if (Strings.clean(input.getStatus()).isBlank()) {
            throw new IllegalArgumentException("Status is required.");
        }
    }

    private void validateProfileInput(ProfileInput input, Integer excludedProfileId) {
        if (input == null) {
            throw new IllegalArgumentException("Profile details are required.");
        }

        if (Strings.clean(input.getName()).isBlank()) {
            throw new IllegalArgumentException("Profile name is required.");
        }

        if (Strings.clean(input.getClient()).isBlank()) {
            throw new IllegalArgumentException("Client is required.");
        }

        if (Strings.clean(input.getCode()).isBlank()) {
            throw new IllegalArgumentException("Profile code is required.");
        }

        if (profileCodeExists(input.getCode(), excludedProfileId)) {
            throw new IllegalArgumentException("Profile code must be unique.");
        }

        if (Strings.clean(input.getStatus()).isBlank()) {
            throw new IllegalArgumentException("Status is required.");
        }
    }

    private List<AuditLogDetail> createdUserChanges(User user) {
        List<AuditLogDetail> changes = new ArrayList<>();
        addCreatedChange(changes, "Full name", user.getName());
        addCreatedChange(changes, "Username", user.getUsername());
        addCreatedChange(changes, "Email", user.getEmail());
        addCreatedChange(changes, "Role", user.getRole());
        addCreatedChange(changes, "Status", user.getStatus());
        addCreatedChange(changes, "Assigned profiles", user.getAssignedProfiles());
        return changes;
    }

    private List<AuditLogDetail> changedUserFields(User previousUser, User updatedUser, boolean passwordChanged) {
        List<AuditLogDetail> changes = new ArrayList<>();
        addChangedChange(changes, "Full name", previousUser.getName(), updatedUser.getName());
        addChangedChange(changes, "Username", previousUser.getUsername(), updatedUser.getUsername());
        addChangedChange(changes, "Email", previousUser.getEmail(), updatedUser.getEmail());
        addChangedChange(changes, "Role", previousUser.getRole(), updatedUser.getRole());
        addChangedChange(changes, "Status", previousUser.getStatus(), updatedUser.getStatus());
        addChangedChange(changes, "Assigned profiles", previousUser.getAssignedProfiles(), updatedUser.getAssignedProfiles());

        if (passwordChanged) {
            changes.add(AuditLogDetail.change("Password", "Existing password", "Updated"));
        }

        return changes;
    }

    private List<AuditLogDetail> deletedUserChanges(User user) {
        List<AuditLogDetail> changes = new ArrayList<>();
        changes.add(AuditLogDetail.change("Account state", "Existing", "Deleted"));
        addDeletedChange(changes, "Full name", user.getName());
        addDeletedChange(changes, "Username", user.getUsername());
        addDeletedChange(changes, "Email", user.getEmail());
        addDeletedChange(changes, "Role", user.getRole());
        addDeletedChange(changes, "Status", user.getStatus());
        addDeletedChange(changes, "Assigned profiles", user.getAssignedProfiles());
        return changes;
    }

    private List<AuditLogDetail> createdProfileChanges(ScanProfile profile) {
        List<AuditLogDetail> changes = new ArrayList<>();
        addCreatedChange(changes, "Profile name", profile.getName());
        addCreatedChange(changes, "Client", profile.getClient());
        addCreatedChange(changes, "Profile status", profile.getStatus());
        addCreatedChange(changes, "Description", profile.getDescription());
        addCreatedChange(changes, "Export naming", profile.getExportNaming());
        addCreatedChange(changes, "QA required", profile.isMetadataRequiredBeforeExport());
        addCreatedChange(changes, "Barcode splitting", profile.isBarcodeSplitting());
        addCreatedChange(changes, "Barcode detected behavior", profile.getBarcodeDetectedBehavior());
        addCreatedChange(changes, "Barcode page behavior", profile.getBarcodePageBehavior());
        addCreatedChange(changes, "Default rotation", profile.getDefaultRotation());
        addCreatedChange(changes, "Brightness", profile.getBrightness());
        addCreatedChange(changes, "Contrast", profile.getContrast());
        addCreatedChange(changes, "Deskew", profile.isDeskew());
        addCreatedChange(changes, "Export format", profile.getExportFormat());
        return changes;
    }

    private List<AuditLogDetail> changedProfileFields(ScanProfile previousProfile, ScanProfile updatedProfile) {
        List<AuditLogDetail> changes = new ArrayList<>();
        addChangedChange(changes, "Profile name", previousProfile.getName(), updatedProfile.getName());
        addChangedChange(changes, "Client", previousProfile.getClient(), updatedProfile.getClient());
        addChangedChange(changes, "Profile status", previousProfile.getStatus(), updatedProfile.getStatus());
        addChangedChange(changes, "Description", previousProfile.getDescription(), updatedProfile.getDescription());
        addChangedChange(changes, "Export naming", previousProfile.getExportNaming(), updatedProfile.getExportNaming());
        addChangedChange(changes, "QA required", previousProfile.isMetadataRequiredBeforeExport(), updatedProfile.isMetadataRequiredBeforeExport());
        addChangedChange(changes, "Barcode splitting", previousProfile.isBarcodeSplitting(), updatedProfile.isBarcodeSplitting());
        addChangedChange(changes, "Barcode detected behavior", previousProfile.getBarcodeDetectedBehavior(), updatedProfile.getBarcodeDetectedBehavior());
        addChangedChange(changes, "Barcode page behavior", previousProfile.getBarcodePageBehavior(), updatedProfile.getBarcodePageBehavior());
        addChangedChange(changes, "Default rotation", previousProfile.getDefaultRotation(), updatedProfile.getDefaultRotation());
        addChangedChange(changes, "Brightness", previousProfile.getBrightness(), updatedProfile.getBrightness());
        addChangedChange(changes, "Contrast", previousProfile.getContrast(), updatedProfile.getContrast());
        addChangedChange(changes, "Deskew", previousProfile.isDeskew(), updatedProfile.isDeskew());
        addChangedChange(changes, "Export format", previousProfile.getExportFormat(), updatedProfile.getExportFormat());
        return changes;
    }

    private List<AuditLogDetail> deletedProfileChanges(ScanProfile profile) {
        List<AuditLogDetail> changes = new ArrayList<>();
        changes.add(AuditLogDetail.change("Profile state", "Existing", "Deleted"));
        addDeletedChange(changes, "Profile name", profile.getName());
        addDeletedChange(changes, "Client", profile.getClient());
        addDeletedChange(changes, "Profile status", profile.getStatus());
        addDeletedChange(changes, "Description", profile.getDescription());
        addDeletedChange(changes, "Export naming", profile.getExportNaming());
        addDeletedChange(changes, "QA required", profile.isMetadataRequiredBeforeExport());
        addDeletedChange(changes, "Barcode splitting", profile.isBarcodeSplitting());
        addDeletedChange(changes, "Export format", profile.getExportFormat());
        return changes;
    }

    private List<AuditLogDetail> changedProfileAssignments(Map<Integer, Set<Integer>> previousAssignments,
                                                           Map<Integer, Set<Integer>> updatedAssignments) {
        List<AuditLogDetail> changes = new ArrayList<>();
        LinkedHashSet<Integer> profileIds = new LinkedHashSet<>();
        profileIds.addAll(previousAssignments.keySet());
        profileIds.addAll(updatedAssignments.keySet());

        for (Integer profileId : profileIds) {
            if (profileId == null) {
                continue;
            }

            String profileName = profileNameForId(profileId);
            addChangedChange(
                    changes,
                    profileName + " assigned users",
                    formatAssignedUserNames(previousAssignments.getOrDefault(profileId, Set.of())),
                    formatAssignedUserNames(updatedAssignments.getOrDefault(profileId, Set.of()))
            );
        }

        return changes;
    }

    private List<AuditLogDetail> changeList(String field, Object oldValue, Object newValue) {
        List<AuditLogDetail> changes = new ArrayList<>();
        addChangedChange(changes, field, oldValue, newValue);
        return changes;
    }

    private void addCreatedChange(List<AuditLogDetail> changes, String field, Object newValue) {
        changes.add(AuditLogDetail.change(field, "", formatAuditValue(newValue)));
    }

    private void addDeletedChange(List<AuditLogDetail> changes, String field, Object oldValue) {
        changes.add(AuditLogDetail.change(field, formatAuditValue(oldValue), ""));
    }

    private void addChangedChange(List<AuditLogDetail> changes, String field, Object oldValue, Object newValue) {
        String formattedOldValue = formatAuditValue(oldValue);
        String formattedNewValue = formatAuditValue(newValue);

        if (!Objects.equals(formattedOldValue, formattedNewValue)) {
            changes.add(AuditLogDetail.change(field, formattedOldValue, formattedNewValue));
        }
    }

    private String formatAuditValue(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof Boolean booleanValue) {
            return booleanValue ? "Yes" : "No";
        }

        if (value instanceof List<?> listValue) {
            return listValue.stream()
                    .map(item -> item == null ? "" : Strings.clean(String.valueOf(item)))
                    .filter(item -> !item.isBlank())
                    .reduce((first, second) -> first + ", " + second)
                    .orElse("");
        }

        return Strings.clean(String.valueOf(value));
    }

    private String profileNameForId(int profileId) {
        return profiles.stream()
                .filter(profile -> profile.getId() == profileId)
                .map(ScanProfile::getName)
                .findFirst()
                .orElse("Profile " + profileId);
    }

    private String formatAssignedUserNames(Set<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return "";
        }

        return userIds.stream()
                .sorted()
                .map(this::userNameForId)
                .reduce((first, second) -> first + ", " + second)
                .orElse("");
    }

    private String userNameForId(int userId) {
        return users.stream()
                .filter(user -> user.getId() == userId)
                .map(User::getName)
                .findFirst()
                .orElse("User " + userId);
    }

    private User copyUser(User user) {
        return new User(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.getStatus(),
                user.getAssignedProfiles(),
                user.isCurrentUser(),
                user.isMustChangePassword()
        );
    }

    private ScanProfile copyProfile(ScanProfile profile) {
        return new ScanProfile(
                profile.getId(),
                profile.getName(),
                profile.getClient(),
                profile.getCode(),
                profile.getDescription(),
                profile.getStatus(),
                profile.getMetadataTemplateName(),
                profile.getExportNaming(),
                profile.getLastUpdated(),
                profile.isArchived(),
                profile.isBarcodeSplitting(),
                profile.getBarcodeDetectedBehavior(),
                profile.getBarcodePageBehavior(),
                profile.getDefaultRotation(),
                profile.getBrightness(),
                profile.getContrast(),
                profile.isDeskew(),
                profile.getExportFormat(),
                profile.isMetadataRequiredBeforeExport()
        );
    }

    private List<Integer> profileIdsForNames(List<String> profileNames) {
        if (profileNames == null || profileNames.isEmpty()) {
            return List.of();
        }

        List<Integer> profileIds = new ArrayList<>();

        for (String profileName : profileNames) {
            if (Strings.clean(profileName).isBlank()) {
                continue;
            }

            ScanProfile profile = findProfileOptionalByName(profileName)
                    .orElseThrow(() -> new IllegalArgumentException("Profile could not be found: " + profileName));
            profileIds.add(profile.getId());
        }

        return profileIds;
    }

    private void syncProfileAssignmentsForUser(User user) {
        removeUserFromAssignments(user.getId());

        for (String assignedProfileName : user.getAssignedProfiles()) {
            findProfileOptionalByName(assignedProfileName).ifPresent(profile ->
                    profileAssignments
                            .computeIfAbsent(profile.getId(), profileId -> new HashSet<>())
                            .add(user.getId())
            );
        }
    }

    private java.util.Optional<ScanProfile> findProfileOptionalByName(String profileName) {
        String normalizedProfileName = Strings.normalize(profileName);

        return profiles.stream()
                .filter(profile -> Strings.normalize(profile.getName()).equals(normalizedProfileName))
                .findFirst();
    }

    private void refreshUsersFromProfileAssignments() {
        for (User user : users) {
            user.setAssignedProfiles(getAssignedProfileNames(user.getId()));
        }
    }

    private List<String> getAssignedProfileNames(int userId) {
        List<String> assignedProfileNames = new ArrayList<>();

        for (ScanProfile profile : getProfiles()) {
            Set<Integer> assignedUserIds = profileAssignments.getOrDefault(profile.getId(), Set.of());

            if (assignedUserIds.contains(userId)) {
                assignedProfileNames.add(profile.getName());
            }
        }

        return assignedProfileNames;
    }

    private void renameAssignedProfile(String previousName, String newName) {
        if (Strings.normalize(previousName).equals(Strings.normalize(newName))) {
            return;
        }

        for (User user : users) {
            LinkedHashSet<String> updatedProfiles = new LinkedHashSet<>();

            for (String assignedProfile : user.getAssignedProfiles()) {
                if (Strings.normalize(assignedProfile).equals(Strings.normalize(previousName))) {
                    updatedProfiles.add(newName);
                } else {
                    updatedProfiles.add(assignedProfile);
                }
            }

            user.setAssignedProfiles(new ArrayList<>(updatedProfiles));
        }
    }

    private void removeUserFromAssignments(int userId) {
        for (Set<Integer> assignedUserIds : profileAssignments.values()) {
            assignedUserIds.remove(userId);
        }
    }

    private Map<Integer, Set<Integer>> copyAssignments(Map<Integer, Set<Integer>> source) {
        Map<Integer, Set<Integer>> copy = new HashMap<>();

        if (source == null) {
            return copy;
        }

        source.forEach((profileId, userIds) ->
                copy.put(profileId, userIds == null ? new HashSet<>() : new HashSet<>(userIds))
        );

        return copy;
    }

    private boolean isArchivedStatus(String status) {
        return "Archived".equalsIgnoreCase(status);
    }

    private boolean isValidEmail(String email) {
        String cleanedEmail = Strings.clean(email);
        int atIndex = cleanedEmail.indexOf("@");
        int dotIndex = cleanedEmail.lastIndexOf(".");

        return atIndex > 0
                && dotIndex > atIndex + 1
                && dotIndex < cleanedEmail.length() - 1;
    }

    public static class UserInput {
        private final String name;
        private final String username;
        private final String email;
        private final String plainPassword;
        private final String role;
        private final String status;
        private final List<String> assignedProfiles;
        private final Boolean mustChangePassword;

        public UserInput(String name, String username, String email,
                         String role, String status, List<String> assignedProfiles) {
            this(name, username, email, role, status, assignedProfiles, "", null);
        }

        public UserInput(String name, String username, String email,
                         String role, String status, List<String> assignedProfiles, String plainPassword) {
            this(name, username, email, role, status, assignedProfiles, plainPassword, null);
        }

        public UserInput(String name, String username, String email,
                         String role, String status, List<String> assignedProfiles,
                         String plainPassword, Boolean mustChangePassword) {
            this.name = name;
            this.username = username;
            this.email = email;
            this.plainPassword = plainPassword;
            this.role = role;
            this.status = status;
            this.assignedProfiles = assignedProfiles == null ? List.of() : List.copyOf(assignedProfiles);
            this.mustChangePassword = mustChangePassword;
        }

        public String getName() { return name; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getPlainPassword() { return plainPassword; }
        public String getRole() { return role; }
        public String getStatus() { return status; }
        public List<String> getAssignedProfiles() { return assignedProfiles; }
        public Boolean getMustChangePassword() { return mustChangePassword; }
    }

    public static class ProfileInput {
        private final String name;
        private final String client;
        private final String code;
        private final String description;
        private final String status;
        private final String metadataTemplateName;
        private final String exportNaming;
        private final boolean barcodeSplitting;
        private final String barcodeDetectedBehavior;
        private final String barcodePageBehavior;
        private final String defaultRotation;
        private final String brightness;
        private final String contrast;
        private final boolean deskew;
        private final String exportFormat;
        private final boolean metadataRequiredBeforeExport;

        public ProfileInput(
                String name,
                String client,
                String code,
                String description,
                String status,
                String metadataTemplateName,
                String exportNaming,
                boolean barcodeSplitting,
                String barcodeDetectedBehavior,
                String barcodePageBehavior,
                String defaultRotation,
                String brightness,
                String contrast,
                boolean deskew,
                String exportFormat,
                boolean metadataRequiredBeforeExport
        ) {
            this.name = name;
            this.client = client;
            this.code = code;
            this.description = description;
            this.status = status;
            this.metadataTemplateName = metadataTemplateName;
            this.exportNaming = exportNaming;
            this.barcodeSplitting = barcodeSplitting;
            this.barcodeDetectedBehavior = barcodeDetectedBehavior;
            this.barcodePageBehavior = barcodePageBehavior;
            this.defaultRotation = defaultRotation;
            this.brightness = brightness;
            this.contrast = contrast;
            this.deskew = deskew;
            this.exportFormat = exportFormat;
            this.metadataRequiredBeforeExport = metadataRequiredBeforeExport;
        }

        public String getName() { return name; }
        public String getClient() { return client; }
        public String getCode() { return code; }
        public String getDescription() { return description; }
        public String getStatus() { return status; }
        public String getMetadataTemplateName() { return metadataTemplateName; }
        public String getExportNaming() { return exportNaming; }
        public boolean isBarcodeSplitting() { return barcodeSplitting; }
        public String getBarcodeDetectedBehavior() { return barcodeDetectedBehavior; }
        public String getBarcodePageBehavior() { return barcodePageBehavior; }
        public String getDefaultRotation() { return defaultRotation; }
        public String getBrightness() { return brightness; }
        public String getContrast() { return contrast; }
        public boolean isDeskew() { return deskew; }
        public String getExportFormat() { return exportFormat; }
        public boolean isMetadataRequiredBeforeExport() { return metadataRequiredBeforeExport; }
    }

    public static class DashboardSummary {
        private final int totalUsers;
        private final int activeProfiles;
        private final int draftProfiles;
        private final int usersWithoutProfiles;
        private final int failedEvents;

        public DashboardSummary(int totalUsers, int activeProfiles, int draftProfiles,
                                int usersWithoutProfiles, int failedEvents) {
            this.totalUsers = totalUsers;
            this.activeProfiles = activeProfiles;
            this.draftProfiles = draftProfiles;
            this.usersWithoutProfiles = usersWithoutProfiles;
            this.failedEvents = failedEvents;
        }

        public int getTotalUsers() { return totalUsers; }
        public int getActiveProfiles() { return activeProfiles; }
        public int getDraftProfiles() { return draftProfiles; }
        public int getUsersWithoutProfiles() { return usersWithoutProfiles; }
        public int getFailedEvents() { return failedEvents; }
    }
}
