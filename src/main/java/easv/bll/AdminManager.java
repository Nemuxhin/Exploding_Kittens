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
import easv.dal.ReviewRecordDAO;
import easv.dal.SavedScanProgressDAO;
import easv.dal.UserDAO;
import easv.gui.controller.util.Strings;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AdminManager {
    private final UserDAO userDAO;
    private final MetadataDAO metadataDAO;
    private final ReviewRecordDAO reviewRecordDAO;
    private final AuditLogDAO auditLogDAO;
    private final QaReviewDAO qaReviewDAO;
    private final SavedScanProgressDAO savedScanProgressDAO;
    private final QAService qaService;
    private final AdminReviewService adminReviewService;
    private final AdminProfileService adminProfileService;

    private final List<User> users = new ArrayList<>();
    private final List<ScanProfile> profiles = new ArrayList<>();
    private final List<ReviewRecord> reviewRecords = new ArrayList<>();
    private final List<AuditLog> auditLogs = new ArrayList<>();

    private final Map<Integer, Set<Integer>> profileAssignments = new HashMap<>();

    private int nextUserId = 1;
    private int nextProfileId = 1;
    private int nextAuditLogId = 1;

    public AdminManager() {
        this(new UserDAO(), new MetadataDAO(), new ReviewRecordDAO(), new AuditLogDAO());
    }

    public AdminManager(UserDAO userDAO) {
        this(userDAO, new MetadataDAO(), new ReviewRecordDAO(), new AuditLogDAO());
    }

    public AdminManager(UserDAO userDAO, MetadataDAO metadataDAO, AuditLogDAO auditLogDAO) {
        this(userDAO, metadataDAO, reviewRecordDAOFor(metadataDAO), auditLogDAO);
    }

    public AdminManager(UserDAO userDAO, MetadataDAO metadataDAO, ReviewRecordDAO reviewRecordDAO, AuditLogDAO auditLogDAO) {
        this.userDAO = userDAO == null ? new UserDAO() : userDAO;
        this.metadataDAO = metadataDAO == null ? new MetadataDAO() : metadataDAO;
        this.reviewRecordDAO = reviewRecordDAO == null ? new ReviewRecordDAO() : reviewRecordDAO;
        this.auditLogDAO = auditLogDAO == null ? new AuditLogDAO() : auditLogDAO;
        this.qaReviewDAO = new QaReviewDAO();
        this.savedScanProgressDAO = new SavedScanProgressDAO();
        this.qaService = new QAService();
        this.adminReviewService = new AdminReviewService(
                this.reviewRecordDAO,
                this.qaReviewDAO,
                this.savedScanProgressDAO,
                this.qaService,
                this.users,
                this.profiles,
                this.reviewRecords
        );
        this.adminProfileService = new AdminProfileService(
                this.metadataDAO,
                this.profiles,
                this.users,
                this.profileAssignments
        );
        loadAdminData();
    }

    private static ReviewRecordDAO reviewRecordDAOFor(MetadataDAO metadataDAO) {
        return new MetadataBackedReviewRecordDAO(metadataDAO);
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
        adminProfileService.removeUserFromAssignments(userId);
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
        return adminProfileService.getProfiles();
    }

    public List<ScanProfile> getAssignableProfiles() {
        return adminProfileService.getAssignableProfiles();
    }

    public ScanProfile createProfile(ProfileInput input) {
        validateProfileInput(input, null);
        ScanProfile savedProfile = adminProfileService.createProfile(input);
        nextProfileId = Math.max(nextProfileId, savedProfile.getId() + 1);

        addAuditLog("Profiles", "Created profile", savedProfile.getName(), "Success",
                "A scan profile was created.",
                createdProfileChanges(savedProfile));

        return savedProfile;
    }

    public ScanProfile updateProfile(int profileId, ProfileInput input) {
        validateProfileInput(input, profileId);
        AdminProfileService.ProfileUpdateResult result = adminProfileService.updateProfile(profileId, input);
        List<AuditLogDetail> profileChanges = changedProfileFields(result.previousProfile(), result.profile());
        if (!profileChanges.isEmpty()) {
            addAuditLog("Profiles", "Updated profile", result.profile().getName(), "Success",
                    "A scan profile was updated.",
                    profileChanges);
        }

        return result.profile();
    }

    public void archiveProfile(int profileId) {
        AdminProfileService.ProfileStatusResult result = adminProfileService.archiveProfile(profileId);
        addAuditLog("Profiles", "Archived profile", result.profile().getName(), "Success",
                "A scan profile was archived.",
                changeList("Profile status", result.previousStatus(), "Archived"));
    }

    public void restoreProfile(int profileId) {
        AdminProfileService.ProfileStatusResult result = adminProfileService.restoreProfile(profileId);
        addAuditLog("Profiles", "Restored profile", result.profile().getName(), "Success",
                "A scan profile was restored.",
                changeList("Profile status", result.previousStatus(), "Active"));
    }

    public void deleteProfile(int profileId) {
        ScanProfile profile = adminProfileService.deleteProfile(profileId);
        addAuditLog("Profiles", "Deleted profile", profile.getName(), "Success",
                "A scan profile was deleted.",
                deletedProfileChanges(profile));
    }

    public boolean profileCodeExists(String code, Integer excludedProfileId) {
        return adminProfileService.profileCodeExists(code, excludedProfileId);
    }

    public List<ReviewRecord> getReviewRecords() {
        return adminReviewService.getReviewRecords();
    }

    public ReviewRecord saveReviewRecord(ReviewRecord updatedRecord) {
        AdminReviewService.ReviewSaveResult result = adminReviewService.saveReviewRecord(updatedRecord);
        if (result.change() == AdminReviewService.ReviewChange.UPDATED) {
            addAuditLog("Review", "Updated review", updatedRecord.getIdentity(), "Success",
                    "A review record was updated.");
        } else if (result.change() == AdminReviewService.ReviewChange.CREATED) {
            addAuditLog("Review", "Created review", updatedRecord.getIdentity(), "Success",
                    "A review record was created.");
        }
        return result.record();
    }

    public ReviewRecord assignReviewRecordToQa(String recordId) {
        return assignReviewRecordToQa(recordId, null);
    }

    public ReviewRecord assignReviewRecordToQa(String recordId, Integer reviewerUserId) {
        return adminReviewService.assignReviewRecordToQa(recordId, reviewerUserId);
    }

    public QAService.QaAssignmentSnapshot getQaAssignmentForReviewRecord(String recordId) {
        return adminReviewService.getQaAssignmentForReviewRecord(recordId);
    }

    public List<Document> getExportableDocumentsForRecord(String recordId) {
        return adminReviewService.getExportableDocumentsForRecord(recordId);
    }

    public ReviewRecord completeQaReviewRecord(
            String recordId,
            boolean approved,
            List<QAService.QaDocumentSnapshot> documents
    ) {
        return adminReviewService.completeQaReviewRecord(recordId, approved, documents);
    }

    public List<QAService.QaDocumentSnapshot> getSavedProgressDocumentsForReviewRecord(String boxId, String profileName) {
        return adminReviewService.getSavedProgressDocumentsForReviewRecord(boxId, profileName);
    }

    public ScanProfile findProfileByName(String profileName) {
        return adminProfileService.findProfileByName(profileName);
    }

    public List<User> getEligibleQaAssignees(String recordId) {
        return adminReviewService.getEligibleQaAssignees(recordId);
    }

    public Map<Integer, Set<Integer>> getProfileAssignments() {
        return adminProfileService.copyAssignments(profileAssignments);
    }

    public Set<Integer> getAssignedUserIds(int profileId) {
        return adminProfileService.getAssignedUserIds(profileId);
    }

    public List<Integer> getAssignedProfileIds(int userId) {
        return adminProfileService.getAssignedProfileIds(userId);
    }

    public void saveProfileAssignments(Map<Integer, Set<Integer>> assignments) {
        Map<Integer, Set<Integer>> previousAssignments = adminProfileService.copyAssignments(profileAssignments);
        Map<Integer, Set<Integer>> updatedAssignments = adminProfileService.copyAssignments(assignments);

        profileAssignments.clear();
        profileAssignments.putAll(updatedAssignments);
        userDAO.replaceProfileAssignments(profileAssignments);
        adminProfileService.refreshUsersFromProfileAssignments();

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

    private void loadAdminData() {
        loadUsers();
        loadProfiles();
        loadReviewRecords();
        loadProfileAssignments();
        loadAuditLogs();
        adminProfileService.refreshUsersFromProfileAssignments();
    }

    private void loadUsers() {
        users.clear();
        users.addAll(userDAO.getAllUsers());
        nextUserId = userDAO.nextUserId();
    }

    private void loadProfiles() {
        adminProfileService.loadProfiles();
        nextProfileId = metadataDAO.nextProfileId();
    }

    private void loadReviewRecords() {
        adminReviewService.loadReviewRecords();
    }

    private void loadProfileAssignments() {
        profileAssignments.clear();
        profileAssignments.putAll(adminProfileService.copyAssignments(userDAO.getProfileAssignments()));
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
        addCreatedChange(changes, "Autosave enabled", profile.isAutosaveEnabled());
        addCreatedChange(changes, "Autosave interval (seconds)", profile.getAutosaveIntervalSeconds());
        addCreatedChange(changes, "Autosave locked", profile.isAutosaveLocked());
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
        addChangedChange(changes, "Autosave enabled", previousProfile.isAutosaveEnabled(), updatedProfile.isAutosaveEnabled());
        addChangedChange(changes, "Autosave interval (seconds)", previousProfile.getAutosaveIntervalSeconds(), updatedProfile.getAutosaveIntervalSeconds());
        addChangedChange(changes, "Autosave locked", previousProfile.isAutosaveLocked(), updatedProfile.isAutosaveLocked());
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
                profile.isMetadataRequiredBeforeExport(),
                profile.isAutosaveEnabled(),
                profile.getAutosaveIntervalSeconds(),
                profile.isAutosaveLocked()
        );
    }

    private List<Integer> profileIdsForNames(List<String> profileNames) {
        return adminProfileService.profileIdsForNames(profileNames);
    }

    private void syncProfileAssignmentsForUser(User user) {
        adminProfileService.syncProfileAssignmentsForUser(user);
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
        private final boolean autosaveEnabled;
        private final int autosaveIntervalSeconds;
        private final boolean autosaveLocked;

        public ProfileInput(
                String name,
                String client,
                String code,
                String description,
                String status,
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
            this(name, client, code, description, status, "", exportNaming,
                    barcodeSplitting, barcodeDetectedBehavior, barcodePageBehavior,
                    defaultRotation, brightness, contrast, deskew, exportFormat,
                    metadataRequiredBeforeExport);
        }

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
            this(name, client, code, description, status, metadataTemplateName, exportNaming,
                    barcodeSplitting, barcodeDetectedBehavior, barcodePageBehavior,
                    defaultRotation, brightness, contrast, deskew, exportFormat,
                    metadataRequiredBeforeExport,
                    true, ScanProfile.DEFAULT_AUTOSAVE_INTERVAL_SECONDS, false);
        }

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
                boolean metadataRequiredBeforeExport,
                boolean autosaveEnabled,
                int autosaveIntervalSeconds,
                boolean autosaveLocked
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
            this.autosaveEnabled = autosaveEnabled;
            this.autosaveIntervalSeconds = autosaveIntervalSeconds;
            this.autosaveLocked = autosaveLocked;
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
        public boolean isAutosaveEnabled() { return autosaveEnabled; }
        public int getAutosaveIntervalSeconds() { return autosaveIntervalSeconds; }
        public boolean isAutosaveLocked() { return autosaveLocked; }
    }

    private static class MetadataBackedReviewRecordDAO extends ReviewRecordDAO {
        private final MetadataDAO metadataDAO;

        MetadataBackedReviewRecordDAO(MetadataDAO metadataDAO) {
            this.metadataDAO = metadataDAO == null ? new MetadataDAO() : metadataDAO;
        }

        @Override
        public List<ReviewRecord> getReviewRecords() {
            return metadataDAO.getReviewRecords();
        }

        @Override
        public void saveReviewRecord(ReviewRecord record) {
            metadataDAO.saveReviewRecord(record);
        }
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
