package easv.bll;

import easv.be.AuditLog;
import easv.be.Document;
import easv.be.PageImage;
import easv.be.ReviewRecord;
import easv.be.ScanProfile;
import easv.be.TiffExportPlan;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditMetadataExportTest {

    @AfterEach
    void clearSession() {
        UserSession.clearCurrentUser();
    }

    @Test
    void auditLogUsesCurrentUserTimestampAndCaseDetails() {
        UserSession.setCurrentUser(new User("scanner", "hash", "USER", true));
        AuditLogManager auditLogManager = new AuditLogManager(AuditLogDAO.inMemory());

        AuditLog log = auditLogManager.logPageCreated(
                "CASE-1",
                "DOC-1",
                "FILE-1",
                2,
                "Building Archive",
                "BOX-1"
        );

        assertEquals("scanner", log.getActor());
        assertEquals("Documents", log.getType());
        assertEquals(AuditLogManager.PAGE_CREATED, log.getAction());
        assertNotNull(log.getTimestamp());
        assertTrue(log.getTarget().contains("CASE-1"));
        assertEquals(6, log.getDetails().size());
    }

    @Test
    void systemActionIsMarkedAsSystem() {
        AuditLogManager auditLogManager = new AuditLogManager(AuditLogDAO.inMemory());

        AuditLog log = auditLogManager.logSystemAction(
                AuditLogManager.SCAN_FAILED,
                "CASE-1",
                null,
                null,
                null,
                "Building Archive",
                "BOX-1",
                "Scanner timeout"
        );

        assertEquals("SYSTEM", log.getActor());
        assertEquals("System", log.getType());
        assertEquals("Failed", log.getStatus());
    }

    @Test
    void adminUserActionsUseLoggedInAdminName() {
        UserSession.setCurrentUser(new User("jenny-admin", "hash", "ADMIN", true));
        AdminManager adminManager = createAdminManager(
                new FakeUserDAO(),
                new FakeMetadataDAO(),
                new FakeReviewRecordDAO(),
                AuditLogDAO.inMemory()
        );

        adminManager.createUser(new AdminManager.UserInput(
                "New Scanner",
                "new.scanner",
                "scanner@example.com",
                "User",
                "Active",
                List.of(),
                "scanner123"
        ));

        AuditLog log = adminManager.getAuditLogs().stream()
                .filter(item -> "Created user".equals(item.getAction()))
                .findFirst()
                .orElseThrow();

        assertEquals("jenny-admin", log.getActor());
    }

    @Test
    void createUserAuditLogRecordsCreatedValues() {
        UserSession.setCurrentUser(new User("jenny-admin", "hash", "ADMIN", true));
        AdminManager adminManager = createAdminManager(
                new FakeUserDAO(),
                new FakeMetadataDAO(),
                new FakeReviewRecordDAO(),
                AuditLogDAO.inMemory()
        );

        adminManager.createUser(new AdminManager.UserInput(
                "New Scanner",
                "new.scanner",
                "scanner@example.com",
                "User",
                "Active",
                List.of(),
                "scanner123"
        ));

        AuditLog log = adminManager.getAuditLogs().stream()
                .filter(item -> "Created user".equals(item.getAction()))
                .findFirst()
                .orElseThrow();

        assertTrue(log.getDetails().stream().allMatch(AuditLog.AuditLogDetail::isFieldChange));
        assertTrue(hasChange(log, "Full name", "", "New Scanner"));
        assertTrue(hasChange(log, "Username", "", "new.scanner"));
        assertTrue(hasChange(log, "Status", "", "Active"));
    }

    @Test
    void updateUserAuditLogRecordsOnlyChangedFields() {
        UserSession.setCurrentUser(new User("jenny-admin", "hash", "ADMIN", true));
        AdminManager adminManager = createAdminManager(
                new FakeUserDAO(),
                new FakeMetadataDAO(),
                new FakeReviewRecordDAO(),
                AuditLogDAO.inMemory()
        );

        User user = adminManager.createUser(new AdminManager.UserInput(
                "New Scanner",
                "new.scanner",
                "scanner@example.com",
                "User",
                "Active",
                List.of(),
                "scanner123"
        ));

        adminManager.updateUser(user.getId(), new AdminManager.UserInput(
                "New Scanner",
                "new.scanner",
                "scanner.renamed@example.com",
                "User",
                "Active",
                List.of()
        ));

        AuditLog log = adminManager.getAuditLogs().stream()
                .filter(item -> "Updated user".equals(item.getAction()))
                .findFirst()
                .orElseThrow();

        assertEquals(1, log.getDetails().size());
        assertTrue(hasChange(log, "Email", "scanner@example.com", "scanner.renamed@example.com"));
    }

    @Test
    void deleteProfileAuditLogRecordsDeletedSnapshot() {
        UserSession.setCurrentUser(new User("jenny-admin", "hash", "ADMIN", true));
        AdminManager adminManager = createAdminManager(
                new FakeUserDAO(),
                new FakeMetadataDAO(),
                new FakeReviewRecordDAO(),
                AuditLogDAO.inMemory()
        );

        ScanProfile profile = adminManager.createProfile(new AdminManager.ProfileInput(
                "New Profile 2",
                "Test Client",
                "NewProfile2",
                "Used for test scanning.",
                "Active",
                "{profileName}_{boxId}",
                true,
                "Start new document",
                "Remove barcode page from final document",
                "0 deg",
                "Normal",
                "Normal",
                true,
                ScanProfile.EXPORT_FORMAT_MULTI_PAGE_TIFF,
                true
        ));

        adminManager.deleteProfile(profile.getId());

        AuditLog log = adminManager.getAuditLogs().stream()
                .filter(item -> "Deleted profile".equals(item.getAction()))
                .findFirst()
                .orElseThrow();

        assertTrue(hasChange(log, "Profile state", "Existing", "Deleted"));
        assertTrue(hasChange(log, "Profile name", "New Profile 2", ""));
        assertTrue(hasChange(log, "Export format", ScanProfile.EXPORT_FORMAT_MULTI_PAGE_TIFF, ""));
    }

    @Test
    void auditLogWithoutChangePayloadStillHasEmptyDetails() {
        AuditLogDAO auditLogDAO = AuditLogDAO.inMemory();
        AuditLog savedLog = auditLogDAO.saveAuditLog(new AuditLog(
                auditLogDAO.nextAuditLogId(),
                java.time.LocalDateTime.now(),
                "System",
                "SYSTEM",
                "Heartbeat",
                "System",
                "Success",
                "A system event was recorded.",
                List.of()
        ));

        assertTrue(savedLog.getDetails().isEmpty());
        assertTrue(auditLogDAO.getAllAuditLogs().get(0).getDetails().isEmpty());
    }

    @Test
    void exportPlansShowFilesPagesAndWarnings() {
        TiffExportManager tiffExportManager = new TiffExportManager();
        PageImage pageOne = new PageImage(1, PageImage.PageType.TIFF, "DOC-1");
        PageImage pageTwo = new PageImage(2, PageImage.PageType.TIFF, "DOC-2");

        TiffExportPlan singlePagePlan = tiffExportManager.createSinglePagePlan("Profile A", "BOX-1", List.of(pageOne, pageTwo));
        TiffExportPlan multiPagePlan = tiffExportManager.createMultiPagePlan(
                "",
                "",
                "{profileName}_{boxId}_{documentNumber}",
                "",
                List.of(
                        new Document("DOC-1", List.of(pageOne)),
                        new Document("DOC-2", List.of(pageTwo))
                )
        );

        assertEquals(2, singlePagePlan.getFileCount());
        assertEquals(2, singlePagePlan.getPageCount());
        assertEquals("Profile_A_BOX-1_DOC-1_page-1.tiff", singlePagePlan.getItems().get(0).getFileName());
        assertEquals(2, multiPagePlan.getFileCount());
        assertEquals(2, multiPagePlan.getPageCount());
        assertEquals("missing_box_1.tiff", multiPagePlan.getItems().get(0).getFileName());
        assertEquals("MULTI_PAGE_TIFFS_BY_DOCUMENT", multiPagePlan.getExportType());
        assertEquals(2, multiPagePlan.getWarnings().size());
    }

    @Test
    void tiffExportAppliesPageRotation(@TempDir Path tempDirectory) throws IOException {
        TiffExportManager tiffExportManager = new TiffExportManager();
        PageImage page = new PageImage(1, PageImage.PageType.TIFF, "DOC-1");
        page.setRotationDegrees(90);
        page.setDisplayContent(createPngDataUri(30, 10));

        TiffExportManager.ExportResult result = tiffExportManager.exportPlan(
                tiffExportManager.createSinglePagePlan("Profile A", "BOX-1", List.of(page)),
                tempDirectory
        );

        BufferedImage exportedImage = ImageIO.read(result.writtenFiles().get(0).toFile());

        assertNotNull(exportedImage);
        assertTrue(exportedImage.getHeight() > exportedImage.getWidth());
    }

    @Test
    void singlePageExportPlanKeepsDuplicateNamesUnique() {
        TiffExportManager tiffExportManager = new TiffExportManager();
        PageImage documentOnePageOne = new PageImage(1, PageImage.PageType.TIFF, "DOC-1");
        PageImage documentTwoPageOne = new PageImage(1, PageImage.PageType.TIFF, "DOC-2");

        TiffExportPlan plan = tiffExportManager.createSinglePagePlan(
                "Profile A",
                "PA",
                "{profileName}_{boxId}_{documentNumber}",
                "BOX-1",
                List.of(documentOnePageOne, documentTwoPageOne)
        );

        assertEquals(2, plan.getFileCount());
        assertFalse(plan.getItems().get(0).getFileName().equals(plan.getItems().get(1).getFileName()));
    }

    private String createPngDataUri(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    private boolean hasChange(AuditLog log, String field, String oldValue, String newValue) {
        return log.getDetails().stream()
                .filter(AuditLog.AuditLogDetail::isFieldChange)
                .anyMatch(detail -> field.equals(detail.getLabel())
                        && oldValue.equals(detail.getOldValue())
                        && newValue.equals(detail.getNewValue()));
    }

    private AdminManager createAdminManager(
            UserDAO userDAO,
            MetadataDAO metadataDAO,
            ReviewRecordDAO reviewRecordDAO,
            AuditLogDAO auditLogDAO
    ) {
        return new AdminManager(
                userDAO,
                metadataDAO,
                reviewRecordDAO,
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

        private FakeUserDAO() {
            super(null);
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
    }

    private static class FakeMetadataDAO extends MetadataDAO {
        private final List<ScanProfile> profiles = new ArrayList<>();
        private int nextProfileId = 1;

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
    }

    private static class FakeReviewRecordDAO extends ReviewRecordDAO {
        @Override
        public List<ReviewRecord> getReviewRecords() {
            return List.of();
        }

        @Override
        public void saveReviewRecord(ReviewRecord record) {
        }
    }
}
