package easv.bll;

import easv.be.AuditLog;
import easv.be.User;
import easv.dal.AuditLogDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditLogManagerTest {

    @AfterEach
    void clearSession() {
        UserSession.clearCurrentUser();
    }

    @Test
    void logPageCreated_shouldUseSessionUserAsActor() {
        UserSession.setCurrentUser(new User("jennifer", "hash", "USER", true));
        AuditLogManager manager = new AuditLogManager(AuditLogDAO.inMemory());

        AuditLog log = manager.logPageCreated("CASE-1", "DOC-1", "FILE-1", 1, "Medical", "BOX-1");

        assertAll(
                () -> assertEquals("jennifer", log.getActor()),
                () -> assertEquals(AuditLogManager.PAGE_CREATED, log.getAction())
        );
    }

    @Test
    void logUserAction_withNoSessionUser_shouldUseSystemAsActor() {
        AuditLogManager manager = new AuditLogManager(AuditLogDAO.inMemory());

        AuditLog log = manager.logUserAction(
                AuditLogManager.SCAN_STARTED, "CASE-1", null, null, null, "Medical", "BOX-1", "Scan began.");

        assertEquals("SYSTEM", log.getActor());
    }

    @Test
    void actionType_shouldRouteToCorrectCategory() {
        UserSession.setCurrentUser(new User("operator", "hash", "USER", true));
        AuditLogManager manager = new AuditLogManager(AuditLogDAO.inMemory());

        AuditLog scanLog     = manager.logUserAction(AuditLogManager.SCAN_STARTED,          "C", null, null, null, "P", "B", "");
        AuditLog tiffLog     = manager.logUserAction(AuditLogManager.TIFF_FETCHED,           "C", null, null, null, "P", "B", "");
        AuditLog barcodeLog  = manager.logUserAction(AuditLogManager.BARCODE_DETECTED,       "C", null, null, null, "P", "B", "");
        AuditLog metaLog     = manager.logUserAction(AuditLogManager.METADATA_SAVED,         "C", null, null, null, "P", "B", "");
        AuditLog exportLog   = manager.logUserAction(AuditLogManager.EXPORT_PREVIEW_CREATED, "C", null, null, null, "P", "B", "");
        AuditLog pageLog     = manager.logPageCreated("C", "D", "F", 1, "P", "B");

        assertAll(
                () -> assertEquals("Scans",    scanLog.getType(),    "SCAN actions → Scans"),
                () -> assertEquals("Scans",    tiffLog.getType(),    "TIFF actions → Scans"),
                () -> assertEquals("Scans",    barcodeLog.getType(), "BARCODE actions → Scans"),
                () -> assertEquals("Metadata", metaLog.getType(),    "METADATA actions → Metadata"),
                () -> assertEquals("Exports",  exportLog.getType(),  "EXPORT actions → Exports"),
                () -> assertEquals("Documents",pageLog.getType(),    "PAGE actions → Documents")
        );
    }

    @Test
    void status_shouldBeFailedOnlyForScanFailed() {
        AuditLogManager manager = new AuditLogManager(AuditLogDAO.inMemory());

        AuditLog failed  = manager.logSystemAction(AuditLogManager.SCAN_FAILED,  null, null, null, null, null, null, "");
        AuditLog started = manager.logSystemAction(AuditLogManager.SCAN_STARTED, null, null, null, null, null, null, "");

        assertAll(
                () -> assertEquals("Failed",  failed.getStatus()),
                () -> assertEquals("Success", started.getStatus())
        );
    }

    @Test
    void target_shouldCombineOnlyNonBlankParts() {
        AuditLogManager manager = new AuditLogManager(AuditLogDAO.inMemory());

        AuditLog full     = manager.logSystemAction(AuditLogManager.PAGE_CREATED, "CASE-1", "DOC-1", "FILE-1", 2, "P", "B", "");
        AuditLog caseOnly = manager.logSystemAction(AuditLogManager.PAGE_CREATED, "CASE-1", null, null, null, null, null, "");
        AuditLog noParts  = manager.logSystemAction(AuditLogManager.PAGE_CREATED, null, null, null, null, null, null, "");

        assertAll(
                () -> assertEquals("case CASE-1 / document DOC-1 / file FILE-1 / page 2", full.getTarget()),
                () -> assertEquals("case CASE-1", caseOnly.getTarget()),
                () -> assertEquals("System", noParts.getTarget(), "No parts → default to System.")
        );
    }

    @Test
    void getLogs_shouldReturnAllSavedEntries() {
        AuditLogManager manager = new AuditLogManager(AuditLogDAO.inMemory());

        manager.logSystemAction(AuditLogManager.SCAN_STARTED,   "C", null, null, null, "P", "B", "");
        manager.logSystemAction(AuditLogManager.SCAN_COMPLETED, "C", null, null, null, "P", "B", "");

        assertEquals(2, manager.getLogs().size());
    }
}
