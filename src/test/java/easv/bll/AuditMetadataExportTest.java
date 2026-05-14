package easv.bll;

import easv.be.AuditLog;
import easv.be.CaseMetadata;
import easv.be.PageImage;
import easv.be.TiffExportPlan;
import easv.be.User;
import easv.dal.AuditLogDAO;
import easv.dal.MetadataDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
    void metadataCanBeSavedLoadedAndLocked() {
        MetadataManager metadataManager = new MetadataManager(MetadataDAO.inMemory(), new AuditLogManager(AuditLogDAO.inMemory()));

        boolean saved = metadataManager.saveMetadata("CASE-1", "Building Archive", "BOX-1", Map.of("Notes", "Ready"));
        CaseMetadata loaded = metadataManager.loadMetadataForm("CASE-1");

        assertTrue(saved);
        assertEquals("Ready", loaded.getValues().get("Notes"));
        assertTrue(metadataManager.canEdit("CASE-1"));

        metadataManager.saveMetadata("CASE-1", "Building Archive", "BOX-1", Map.of("Notes", "Completed"), true, false);

        assertFalse(metadataManager.saveMetadata("CASE-1", "Building Archive", "BOX-1", Map.of("Notes", "Changed")));
    }

    @Test
    void sharedMetadataManagerKeepsMetadataAcrossControllers() {
        MetadataDAO sharedMetadataDAO = MetadataDAO.inMemory();
        AuditLogManager auditLogManager = new AuditLogManager(AuditLogDAO.inMemory());
        MetadataManager firstControllerManager = new MetadataManager(sharedMetadataDAO, auditLogManager);
        MetadataManager secondControllerManager = new MetadataManager(sharedMetadataDAO, auditLogManager);

        firstControllerManager.saveMetadata("CASE-SHARED", "Building Archive", "BOX-2", Map.of("Notes", "Shared"));

        CaseMetadata loaded = secondControllerManager.loadMetadataForm("CASE-SHARED");

        assertNotNull(loaded);
        assertEquals("Shared", loaded.getValues().get("Notes"));
    }

    @Test
    void adminUserActionsUseLoggedInAdminName() {
        UserSession.setCurrentUser(new User("jenny-admin", "hash", "ADMIN", true));
        AuditLogManager auditLogManager = new AuditLogManager(AuditLogDAO.inMemory());

        AuditLog log = auditLogManager.logUserAction(
                AuditLogManager.PAGE_CREATED,
                "CASE-1",
                "DOC-1",
                "FILE-1",
                1,
                "Building Archive",
                "BOX-1",
                "Admin-triggered action"
        );

        assertEquals("jenny-admin", log.getActor());
    }

    @Test
    void exportPlansShowFilesPagesAndWarnings() {
        TiffExportManager tiffExportManager = new TiffExportManager();
        PageImage pageOne = new PageImage(1, PageImage.PageType.TIFF, "DOC-1");
        PageImage pageTwo = new PageImage(2, PageImage.PageType.TIFF, "DOC-2");

        TiffExportPlan singlePagePlan = tiffExportManager.createSinglePagePlan("Profile A", "BOX-1", List.of(pageOne, pageTwo));
        TiffExportPlan multiPagePlan = tiffExportManager.createMultiPagePlan("", "", List.of(pageOne, pageTwo));

        assertEquals(2, singlePagePlan.getFileCount());
        assertEquals(2, singlePagePlan.getPageCount());
        assertEquals("Profile_A_BOX-1_DOC-1_page-1.tiff", singlePagePlan.getItems().get(0).getFileName());
        assertEquals(1, multiPagePlan.getFileCount());
        assertEquals(2, multiPagePlan.getPageCount());
        assertEquals("MULTI_PAGE_TIFF_FILE", multiPagePlan.getExportType());
        assertEquals(2, multiPagePlan.getWarnings().size());
    }
}
