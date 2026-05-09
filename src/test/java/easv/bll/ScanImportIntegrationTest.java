package easv.bll;

import easv.be.CaseFile;
import easv.be.PageImage;
import easv.dal.BoxDAO;
import easv.dal.CaseFileDAO;
import easv.dal.ClientDAO;
import easv.dal.DatabaseConnection;
import easv.dal.DocumentDAO;
import easv.dal.PageImageDAO;
import easv.dal.ScanSessionDAO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the scanner import workflow.
 *
 * This class intentionally uses ScannerApiClient, DAOs, and an in-memory H2 database.
 * It is separate from ScanManagerTest because this checks cross-layer behavior:
 * API item -> BLL import -> DAO persistence -> lookup.
 */

@Tag("integration")
class ScanImportIntegrationTest {

    @Test
    void importNextItem_shouldImportRawPagesClassifyBarcodePagesAndPersistCaseLinks() {
        ScannerApiClient apiClient = new ScannerApiClient();
        apiClient.queueItem(new ScannerApiClient.ApiTiffItem(
                "ITEM-1",
                "CASE-7",
                "CLIENT-1",
                "Northwind",
                "BOX-9",
                "Shelf A",
                List.of(
                        new ScannerApiClient.ApiTiffPage(1, "page-001.tiff"),
                        new ScannerApiClient.ApiTiffPage(2, "barcode-separator-002.tiff")
                )
        ));

        ScanManager scanManager = createScanManager(apiClient, "import-next-item");

        var session = scanManager.startSession("BOX-9", "Shelf A");
        var importedDocument = scanManager.importNextItem(session);
        CaseFile caseFile = scanManager.findCaseFile("CASE-7").orElseThrow();

        assertAll(
                () -> assertTrue(importedDocument.isPresent(), "The queued API item should be imported."),
                () -> assertEquals(1, session.getImportedDocuments().size(), "The session should record the imported document once."),
                () -> assertEquals(2, importedDocument.orElseThrow().getPages().size(), "Both raw API pages should be imported."),
                () -> assertEquals(PageImage.PageType.TIFF, importedDocument.orElseThrow().getPages().get(0).getPageType(), "A normal TIFF page should stay a TIFF page."),
                () -> assertEquals(PageImage.PageType.BARCODE, importedDocument.orElseThrow().getPages().get(1).getPageType(), "A barcode/separator page should be classified as BARCODE."),
                () -> assertEquals("CLIENT-1", caseFile.getClient().getClientNumber(), "The imported case should be linked to the API client number."),
                () -> assertEquals("BOX-9", caseFile.getBox().getBoxId(), "The imported case should be linked to the selected/API box id.")
        );
    }

    @Test
    void importAllAvailable_shouldReuseExistingClientAndBoxForMultipleCaseFiles() {
        ScannerApiClient apiClient = new ScannerApiClient();
        apiClient.queueItem(new ScannerApiClient.ApiTiffItem(
                "ITEM-1",
                "CASE-1",
                "CLIENT-7",
                "Acme",
                "BOX-1",
                "Rack 1",
                List.of(new ScannerApiClient.ApiTiffPage(1, "page-001.tiff"))
        ));
        apiClient.queueItem(new ScannerApiClient.ApiTiffItem(
                "ITEM-2",
                "CASE-2",
                "CLIENT-7",
                "Acme",
                "BOX-1",
                "Rack 1",
                List.of(new ScannerApiClient.ApiTiffPage(1, "page-002.tiff"))
        ));

        ScanManager scanManager = createScanManager(apiClient, "deduplicate-client-box");

        var session = scanManager.startSession("BOX-1", "Rack 1");
        var importedDocuments = scanManager.importAllAvailable(session);

        CaseFile firstCase = scanManager.findCaseFile("CASE-1").orElseThrow();
        CaseFile secondCase = scanManager.findCaseFile("CASE-2").orElseThrow();

        assertAll(
                () -> assertEquals(2, importedDocuments.size(), "Both queued API items should be imported."),
                () -> assertEquals(2, session.getImportedDocuments().size(), "The session should contain both imported documents."),
                () -> assertEquals(firstCase.getClient().getId(), secondCase.getClient().getId(), "Cases from the same client number should reuse the same Client record."),
                () -> assertEquals(firstCase.getBox().getId(), secondCase.getBox().getId(), "Cases from the same box id should reuse the same Box record.")
        );
    }

    @Test
    void importAllAvailable_shouldRecordApiFailureAndContinueWithNextItem() {
        ScannerApiClient apiClient = new ScannerApiClient();
        apiClient.queueFailure("Temporary API outage");
        apiClient.queueItem(new ScannerApiClient.ApiTiffItem(
                "ITEM-99",
                "CASE-99",
                "CLIENT-99",
                "Contoso",
                "BOX-99",
                "Overflow",
                List.of(new ScannerApiClient.ApiTiffPage(1, "page-099.tiff"))
        ));

        ScanManager scanManager = createScanManager(apiClient, "api-failure-continues");

        var session = scanManager.startSession("BOX-99", "Overflow");
        var importedDocuments = scanManager.importAllAvailable(session);

        assertAll(
                () -> assertEquals(1, session.getFailures().size(), "The API failure should be recorded on the session."),
                () -> assertEquals("Temporary API outage", session.getFailures().get(0)),
                () -> assertEquals(1, importedDocuments.size(), "The later valid item should still be imported after the API failure."),
                () -> assertEquals(1, session.getImportedDocuments().size(), "Only the successfully imported item should be added to the session."),
                () -> assertTrue(scanManager.findCaseFile("CASE-99").isPresent(), "The valid item after the failure should be persisted."),
                () -> assertFalse(scanManager.findCaseFile("Temporary API outage").isPresent(), "A failure message must never be persisted as a case file id.")
        );
    }

    private ScanManager createScanManager(ScannerApiClient apiClient, String databaseNamePrefix) {
        DatabaseConnection databaseConnection = new DatabaseConnection(
                "jdbc:h2:mem:" + databaseNamePrefix + "-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );

        PageImageDAO pageImageDAO = new PageImageDAO(databaseConnection);
        DocumentDAO documentDAO = new DocumentDAO(databaseConnection, pageImageDAO);

        return new ScanManager(
                apiClient,
                new BarcodeSplitService(),
                new ClientDAO(databaseConnection),
                new BoxDAO(databaseConnection),
                new CaseFileDAO(databaseConnection, documentDAO),
                documentDAO,
                new ScanSessionDAO(databaseConnection)
        );
    }
}
