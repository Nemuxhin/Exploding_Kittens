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

class ScanManagerTest {

    @AfterEach
    void clearSession() {
        UserSession.clearCurrentUser();
    }

    @Test
    void scanActionsCreateAuditLogsWithUserAndTimestamp() {
        UserSession.setCurrentUser(new User("scanner", "hash", "USER", true));
        AuditLogDAO auditLogDAO = new AuditLogDAO();
        AuditLogManager auditLogManager = new AuditLogManager(auditLogDAO);
        ScanManager scanManager = new ScanManager(auditLogManager);

        scanManager.startScan("CASE-1", "Profile-A", "BOX-1");
        scanManager.addPage("CASE-1", "DOC-1", "FILE-1", 1, "page1.tiff", "Profile-A", "BOX-1");
        scanManager.completeScan("CASE-1", "Profile-A", "BOX-1");

        List<AuditLog> logs = auditLogManager.getLogs();

        assertEquals(4, logs.size());
        assertEquals("scanner", logs.get(0).getUsername());
        assertNotNull(logs.get(0).getTimestamp());
        assertEquals(AuditLogManager.TIFF_FETCHED, logs.get(1).getAction());
        assertEquals(AuditLogManager.PAGE_CREATED, logs.get(2).getAction());
        assertEquals("FILE-1", logs.get(2).getFileId());
        assertEquals(1, logs.get(2).getPageNumber());
    }

    @Test
    void systemScanFailureIsMarkedAsSystemAction() {
        AuditLogDAO auditLogDAO = new AuditLogDAO();
        AuditLogManager auditLogManager = new AuditLogManager(auditLogDAO);
        ScanManager scanManager = new ScanManager(auditLogManager);

        scanManager.scanFailed("CASE-1", "Profile-A", "BOX-1", "Scanner timeout");

        AuditLog log = auditLogManager.getLogs().get(0);

        assertEquals("SYSTEM", log.getUsername());
        assertTrue(log.isSystemAction());
        assertEquals(AuditLogManager.SCAN_FAILED, log.getAction());
    }

    @Test
    void deletedPageIsInactiveAndLogged() {
        UserSession.setCurrentUser(new User("scanner", "hash", "USER", true));
        AuditLogManager auditLogManager = new AuditLogManager(new AuditLogDAO());
        ScanManager scanManager = new ScanManager(auditLogManager);

        scanManager.addPage("CASE-1", "DOC-1", "FILE-1", 1, "page1.tiff", "Profile-A", "BOX-1");
        boolean deleted = scanManager.deletePage("CASE-1", "FILE-1", 1, "Profile-A", "BOX-1");

        assertTrue(deleted);
        assertTrue(scanManager.getActivePages("CASE-1").isEmpty());
        assertEquals(AuditLogManager.PAGE_DELETED, auditLogManager.getLogs().get(2).getAction());
    }

    @Test
    void metadataCanBeSavedLoadedAndLocked() {
        AuditLogManager auditLogManager = new AuditLogManager(new AuditLogDAO());
        MetadataManager metadataManager = new MetadataManager(new MetadataDAO(), auditLogManager);

        boolean saved = metadataManager.saveMetadata("CASE-1", "Profile-A", "BOX-1", Map.of("Notes", "Ready"));
        CaseMetadata loaded = metadataManager.loadMetadataForm("CASE-1");

        assertTrue(saved);
        assertEquals("Ready", loaded.getValues().get("Notes"));
        assertTrue(metadataManager.canEdit("CASE-1"));

        metadataManager.saveMetadata("CASE-1", "Profile-A", "BOX-1", Map.of("Notes", "Completed"), true, false);

        assertFalse(metadataManager.saveMetadata("CASE-1", "Profile-A", "BOX-1", Map.of("Notes", "Changed")));
    }

    @Test
    void exportPlansShowFilesPagesAndWarnings() {
        TiffExportManager tiffExportManager = new TiffExportManager();
        PageImage pageOne = new PageImage("CASE-1", "DOC-1", "FILE-1", 1, "page1.tiff");
        PageImage pageTwo = new PageImage("CASE-1", "DOC-1", "FILE-2", 2, "page2.tiff");

        TiffExportPlan singlePagePlan = tiffExportManager.createSinglePagePlan("Profile A", "BOX-1", List.of(pageOne, pageTwo));
        TiffExportPlan multiPagePlan = tiffExportManager.createMultiPagePlan("", "", List.of(pageOne, pageTwo));

        assertEquals(2, singlePagePlan.getFileCount());
        assertEquals(2, singlePagePlan.getPageCount());
        assertEquals("Profile_A_BOX-1_DOC-1_page-1.tiff", singlePagePlan.getItems().get(0).getFileName());
        assertEquals(1, multiPagePlan.getFileCount());
        assertEquals(2, multiPagePlan.getWarnings().size());
    }
}
