package easv.bll;

import easv.be.AuditLog;
import easv.be.ReviewRecord;
import easv.be.ScanProfile;
import easv.be.User;
import easv.dal.AuditLogDAO;
import easv.dal.DatabaseConnection;
import easv.dal.MetadataDAO;
import easv.dal.NotificationDAO;
import easv.dal.QaReviewDAO;
import easv.dal.ReviewRecordDAO;
import easv.dal.SavedScanProgressDAO;
import easv.dal.UserDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AdminManager is the gateway for user and profile management. These tests
 * cover validation, the deactivate-instead-of-delete flow, profile-assignment
 * sync, dashboard counters, and the audit trail every action leaves behind.
 */
class AdminManagerTest {

    private AdminManager adminManager;

    @BeforeEach
    void setUp() {
        // Clear session both before and after to guard against pollution from
        // any test that forgot to clean up, and from any leftover state in this run.
        UserSession.clearCurrentUser();
        UserSession.setCurrentUser(new User(999, "Admin User", "admin", "", "hash", "Admin", "Active", List.of(), true));
        adminManager = createAdminManager(new FakeUserDAO(), new FakeMetadataDAO(), AuditLogDAO.inMemory());
    }

    @AfterEach
    void clearSession() {
        UserSession.clearCurrentUser();
    }

    // ---------- validation ----------

    @Test
    void createUser_shouldRejectMissingFullName() {
        AdminManager.UserInput input = new AdminManager.UserInput(
                "", "alice", "alice@example.com", "User", "Active", List.of(), "secret123");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> adminManager.createUser(input));
        assertTrue(error.getMessage().toLowerCase().contains("name"));
    }

    @Test
    void createUser_shouldRejectNonAdminCaller() {
        UserSession.setCurrentUser(new User(998, "Scanner", "scanner", "", "hash", "User", "Active", List.of(), true));

        AdminManager.UserInput input = new AdminManager.UserInput(
                "Alice", "alice", "alice@example.com", "User", "Active", List.of(), "secret123");

        assertThrows(SecurityException.class, () -> adminManager.createUser(input));
    }

    @Test
    void createUser_shouldRejectMissingUsername() {
        AdminManager.UserInput input = new AdminManager.UserInput(
                "Alice", "", "alice@example.com", "User", "Active", List.of(), "secret123");

        assertThrows(IllegalArgumentException.class, () -> adminManager.createUser(input));
    }

    @Test
    void createUser_shouldRequirePasswordOnInitialCreate() {
        AdminManager.UserInput input = new AdminManager.UserInput(
                "Alice", "alice", "alice@example.com", "User", "Active", List.of(), "");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> adminManager.createUser(input));
        assertTrue(error.getMessage().toLowerCase().contains("password"));
    }

    @Test
    void createUser_shouldRejectInvalidEmailButAllowBlankEmail() {
        AdminManager.UserInput invalidEmail = new AdminManager.UserInput(
                "Alice", "alice", "not-an-email", "User", "Active", List.of(), "secret123");
        AdminManager.UserInput blankEmail = new AdminManager.UserInput(
                "Bob", "bob", "", "User", "Active", List.of(), "secret123");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> adminManager.createUser(invalidEmail)),
                () -> assertNotNull(adminManager.createUser(blankEmail), "Blank email should be allowed.")
        );
    }

    @Test
    void createUser_shouldRejectMissingRoleOrStatus() {
        AdminManager.UserInput noRole = new AdminManager.UserInput(
                "Alice", "alice", "alice@example.com", "", "Active", List.of(), "secret123");
        AdminManager.UserInput noStatus = new AdminManager.UserInput(
                "Bob", "bob", "bob@example.com", "User", "", List.of(), "secret123");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> adminManager.createUser(noRole)),
                () -> assertThrows(IllegalArgumentException.class, () -> adminManager.createUser(noStatus))
        );
    }

    @Test
    void createUser_shouldRejectDuplicateUsername() {
        adminManager.createUser(new AdminManager.UserInput(
                "Alice", "alice", "alice@example.com", "User", "Active", List.of(), "secret123"));

        AdminManager.UserInput duplicate = new AdminManager.UserInput(
                "Alice 2", "alice", "alice2@example.com", "User", "Active", List.of(), "secret123");

        // Behaviour under test: a duplicate username is rejected. We deliberately do not
        // assert the exact message so that rewording the validation text can't break this.
        assertThrows(IllegalArgumentException.class, () -> adminManager.createUser(duplicate));
    }

    @Test
    void usernameExists_shouldIgnoreExcludedUserIdSoUpdateDoesNotCollideWithItself() {
        User user = adminManager.createUser(new AdminManager.UserInput(
                "Alice", "alice", "alice@example.com", "User", "Active", List.of(), "secret123"));

        assertAll(
                () -> assertTrue(adminManager.usernameExists("alice", null),
                        "Other lookups should still see the name."),
                () -> assertFalse(adminManager.usernameExists("alice", user.getId()),
                        "When updating their own record, the user's existing username should not collide.")
        );
    }

    // ---------- deactivate (replaces delete) ----------

    @Test
    void deactivateUser_shouldSetStatusToInactiveAndWriteAuditLog() {
        User user = adminManager.createUser(new AdminManager.UserInput(
                "Alice", "alice", "alice@example.com", "User", "Active", List.of(), "secret123"));

        User deactivated = adminManager.deactivateUser(user.getId());

        AuditLog log = adminManager.getAuditLogs().stream()
                .filter(item -> "Deactivated user".equals(item.getAction()))
                .findFirst()
                .orElseThrow();

        assertAll(
                () -> assertEquals("Inactive", deactivated.getStatus()),
                () -> assertFalse(deactivated.isActive()),
                () -> assertEquals("Deactivated user", log.getAction())
        );
    }

    @Test
    void deactivateUser_shouldRejectTheCurrentlyLoggedInUser() {
        FakeUserDAO userDAO = new FakeUserDAO();
        userDAO.preloadUser(new User(1, "Self", "self", "", "h", "User", "Active", List.of(), true));
        AdminManager manager = createAdminManager(userDAO, new FakeMetadataDAO(), AuditLogDAO.inMemory());

        // Behaviour under test: you cannot deactivate the logged-in user. Wording not asserted.
        assertThrows(IllegalArgumentException.class, () -> manager.deactivateUser(1));
    }

    @Test
    void deactivateUser_shouldBeNoOpWhenAlreadyInactive() {
        FakeUserDAO userDAO = new FakeUserDAO();
        userDAO.preloadUser(new User(1, "Alice", "alice", "", "h", "User", "Inactive", List.of(), false));
        AdminManager manager = createAdminManager(userDAO, new FakeMetadataDAO(), AuditLogDAO.inMemory());

        User user = manager.deactivateUser(1);

        assertAll(
                () -> assertEquals("Inactive", user.getStatus()),
                () -> assertTrue(manager.getAuditLogs().stream()
                                .noneMatch(log -> "Deactivated user".equals(log.getAction())),
                        "An already-inactive user should not produce a new audit entry.")
        );
    }

    // ---------- profile assignment sync ----------

    @Test
    void createUser_withAssignedProfile_shouldSyncProfileAssignments() {
        FakeMetadataDAO metadataDAO = new FakeMetadataDAO();
        ScanProfile profile = new ScanProfile(1, "Medical", "MED", "", "Active", "", "",
                "", false, false, "", "", "0", "Normal", "Normal", false, "TIFF", false);
        metadataDAO.preloadProfile(profile);
        AdminManager manager = createAdminManager(new FakeUserDAO(), metadataDAO, AuditLogDAO.inMemory());

        User user = manager.createUser(new AdminManager.UserInput(
                "Alice", "alice", "alice@example.com", "User", "Active", List.of("Medical"), "secret123"));

        Map<Integer, Set<Integer>> assignments = manager.getProfileAssignments();
        assertTrue(assignments.getOrDefault(profile.getId(), Set.of()).contains(user.getId()),
                "Assigned profile should map back to the user id.");
    }

    // ---------- audit-trail content ----------

    @Test
    void createUserAuditLog_shouldRecordTheLoggedInAdminAsActor() {
        UserSession.setCurrentUser(new User("jenny-admin", "hash", "ADMIN", true));

        adminManager.createUser(new AdminManager.UserInput(
                "New Scanner", "new.scanner", "scanner@example.com", "User", "Active", List.of(), "scanner123"));

        AuditLog log = adminManager.getAuditLogs().stream()
                .filter(item -> "Created user".equals(item.getAction()))
                .findFirst()
                .orElseThrow();

        assertEquals("jenny-admin", log.getActor());
    }

    @Test
    void createUserAuditLog_shouldRecordEveryCreatedFieldAsAChange() {
        adminManager.createUser(new AdminManager.UserInput(
                "New Scanner", "new.scanner", "scanner@example.com", "User", "Active", List.of(), "scanner123"));

        AuditLog log = adminManager.getAuditLogs().stream()
                .filter(item -> "Created user".equals(item.getAction()))
                .findFirst()
                .orElseThrow();

        assertAll(
                () -> assertTrue(log.getDetails().stream().allMatch(AuditLog.AuditLogDetail::isFieldChange),
                        "Every created-user detail should be marked as a field change."),
                () -> assertTrue(hasChange(log, "Full name", "", "New Scanner")),
                () -> assertTrue(hasChange(log, "Username",  "", "new.scanner")),
                () -> assertTrue(hasChange(log, "Status",    "", "Active"))
        );
    }

    @Test
    void updateUserAuditLog_shouldOnlyRecordTheChangedFields() {
        User user = adminManager.createUser(new AdminManager.UserInput(
                "New Scanner", "new.scanner", "scanner@example.com", "User", "Active", List.of(), "scanner123"));

        adminManager.updateUser(user.getId(), new AdminManager.UserInput(
                "New Scanner", "new.scanner", "scanner.renamed@example.com", "User", "Active", List.of()));

        AuditLog log = adminManager.getAuditLogs().stream()
                .filter(item -> "Updated user".equals(item.getAction()))
                .findFirst()
                .orElseThrow();

        assertAll(
                () -> assertEquals(1, log.getDetails().size(), "Only the email change should be recorded."),
                () -> assertTrue(hasChange(log, "Email", "scanner@example.com", "scanner.renamed@example.com"))
        );
    }

    @Test
    void updateDeleteAndDeactivate_shouldRejectNonAdminCaller() {
        User user = adminManager.createUser(new AdminManager.UserInput(
                "Alice", "alice", "alice@example.com", "User", "Active", List.of(), "secret123"));
        UserSession.setCurrentUser(new User(998, "Scanner", "scanner", "", "hash", "User", "Active", List.of(), true));

        assertAll(
                () -> assertThrows(SecurityException.class, () -> adminManager.updateUser(user.getId(), new AdminManager.UserInput(
                        "Alice Updated", "alice", "alice@example.com", "User", "Active", List.of()
                ))),
                () -> assertThrows(SecurityException.class, () -> adminManager.deleteUser(user.getId())),
                () -> assertThrows(SecurityException.class, () -> adminManager.deactivateUser(user.getId()))
        );
    }

    // ---------- dashboard ----------

    @Test
    void dashboardSummary_shouldCountUsersActiveAndDraftProfilesAndFailures() {
        FakeMetadataDAO metadataDAO = new FakeMetadataDAO();
        metadataDAO.preloadProfile(new ScanProfile(1, "Medical", "MED", "", "Active", "", "",
                "", false, false, "", "", "0", "Normal", "Normal", false, "TIFF", false));
        metadataDAO.preloadProfile(new ScanProfile(2, "Draft Profile", "DRAFT", "", "Draft", "", "",
                "", false, false, "", "", "0", "Normal", "Normal", false, "TIFF", false));
        AdminManager manager = createAdminManager(new FakeUserDAO(), metadataDAO, AuditLogDAO.inMemory());

        manager.createUser(new AdminManager.UserInput(
                "Alice", "alice", "alice@example.com", "User", "Active", List.of(), "secret123"));
        manager.addAuditLog("Scans", "SCAN_FAILED", "scanner-1", "Failed", "Scanner timed out.");

        AdminManager.DashboardSummary summary = manager.getDashboardSummary();

        assertAll(
                () -> assertEquals(1, summary.getTotalUsers()),
                () -> assertEquals(1, summary.getActiveProfiles()),
                () -> assertEquals(1, summary.getDraftProfiles()),
                () -> assertEquals(1, summary.getUsersWithoutProfiles()),
                () -> assertEquals(1, summary.getFailedEvents())
        );
    }

    private boolean hasChange(AuditLog log, String field, String oldValue, String newValue) {
        return log.getDetails().stream()
                .filter(AuditLog.AuditLogDetail::isFieldChange)
                .anyMatch(detail -> field.equals(detail.getLabel())
                        && oldValue.equals(detail.getOldValue())
                        && newValue.equals(detail.getNewValue()));
    }

    private AdminManager createAdminManager(UserDAO userDAO, MetadataDAO metadataDAO, AuditLogDAO auditLogDAO) {
        return new AdminManager(
                userDAO,
                metadataDAO,
                new ReviewRecordDAO() {
                    @Override
                    public List<ReviewRecord> getReviewRecords() {
                        return List.of();
                    }

                    @Override
                    public void saveReviewRecord(ReviewRecord record) {
                        // no-op for tests
                    }
                },
                auditLogDAO,
                new QaReviewDAO(new DatabaseConnection("", "", "")),
                new SavedScanProgressDAO(new DatabaseConnection("", "", "")),
                new QAService(
                        new QaReviewDAO(new DatabaseConnection("", "", "")),
                        new NotificationDAO(new DatabaseConnection("", "", "")),
                        new FakeUserDAO(),
                        AuditLogDAO.inMemory()
                ) {
                    @Override
                    public List<QaAssignmentSnapshot> getAllAssignmentsForAdmin() {
                        return List.of();
                    }
                }
        );
    }

    private static class FakeUserDAO extends UserDAO {
        private final List<User> users = new ArrayList<>();
        private int nextId = 1;

        FakeUserDAO() {
            super(null, false); // hermetic: never opens a database connection
        }

        void preloadUser(User user) {
            users.add(user);
            nextId = Math.max(nextId, user.getId() + 1);
        }

        @Override
        public List<User> getAllUsers() {
            return List.copyOf(users);
        }

        @Override
        public int nextUserId() {
            return nextId;
        }

        @Override
        public User saveUser(User user, List<Integer> assignedProfileIds) {
            User savedUser = new User(
                    nextId++,
                    user.getName(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getPasswordHash(),
                    user.getRole(),
                    user.getStatus(),
                    user.getAssignedProfiles(),
                    user.isCurrentUser()
            );
            users.add(savedUser);
            return savedUser;
        }

        @Override
        public User updateUser(User user, List<Integer> assignedProfileIds) {
            users.removeIf(existingUser -> existingUser.getId() == user.getId());
            users.add(user);
            return user;
        }

        @Override
        public void deleteUser(int userId) {
            users.removeIf(user -> user.getId() == userId);
        }

        @Override
        public Map<Integer, Set<Integer>> getProfileAssignments() {
            return new HashMap<>();
        }

        @Override
        public void replaceProfileAssignments(Map<Integer, Set<Integer>> assignments) {
            // no-op for tests
        }
    }

    private static class FakeMetadataDAO extends MetadataDAO {
        private final List<ScanProfile> profiles = new ArrayList<>();
        private int nextProfileId = 1;

        void preloadProfile(ScanProfile profile) {
            profiles.add(profile);
            nextProfileId = Math.max(nextProfileId, profile.getId() + 1);
        }

        @Override
        public List<ScanProfile> getProfiles() {
            return List.copyOf(profiles);
        }

        @Override
        public int nextProfileId() {
            return nextProfileId;
        }

        @Override
        public ScanProfile saveProfile(ScanProfile profile) {
            ScanProfile savedProfile = new ScanProfile(
                    nextProfileId++,
                    profile.getName(),
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
            profiles.add(savedProfile);
            return savedProfile;
        }

        @Override
        public void updateProfile(ScanProfile profile) {
            profiles.removeIf(existingProfile -> existingProfile.getId() == profile.getId());
            profiles.add(profile);
        }

        @Override
        public void deleteProfile(int profileId) {
            profiles.removeIf(profile -> profile.getId() == profileId);
        }

        @Override
        public List<ReviewRecord> getReviewRecords() {
            return List.of();
        }

        @Override
        public void saveReviewRecord(ReviewRecord record) {
            // In-memory no-op: prevents AdminManager.saveReviewRecord() from hitting
            // the real database if a future test exercises the review-record path.
        }
    }
}
