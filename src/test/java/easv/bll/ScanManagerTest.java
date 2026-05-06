package easv.bll;

import org.junit.jupiter.api.Test;

import easv.dal.BoxDAO;
import easv.dal.CaseFileDAO;
import easv.dal.ClientDAO;
import easv.dal.DatabaseConnection;
import easv.dal.DocumentDAO;
import easv.dal.PageImageDAO;
import easv.dal.ScanSessionDAO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanManagerTest {
    @Test
    void importsTiffItemsOneAtATimeAndClassifiesBarcodePages() {
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

        ScanManager scanManager = createScanManager(apiClient, "imports");

        var session = scanManager.startSession("BOX-9", "Shelf A");
        var imported = scanManager.importNextItem(session);

        assertTrue(imported.isPresent());
        assertEquals(1, session.getImportedDocuments().size());
        assertEquals(2, imported.get().getPages().size());
        assertEquals(easv.be.PageImage.PageType.TIFF, imported.get().getPages().get(0).getPageType());
        assertEquals(easv.be.PageImage.PageType.BARCODE, imported.get().getPages().get(1).getPageType());
        assertEquals("CLIENT-1", scanManager.findCaseFile("CASE-7").orElseThrow().getClient().getClientNumber());
        assertEquals("BOX-9", scanManager.findCaseFile("CASE-7").orElseThrow().getBox().getBoxId());
    }

    @Test
    void deduplicatesClientsAndBoxesAcrossImports() {
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

        ScanManager scanManager = createScanManager(apiClient, "dedupe");

        var session = scanManager.startSession("BOX-1", "Rack 1");
        scanManager.importAllAvailable(session);

        var firstCase = scanManager.findCaseFile("CASE-1").orElseThrow();
        var secondCase = scanManager.findCaseFile("CASE-2").orElseThrow();

        assertEquals(firstCase.getClient().getId(), secondCase.getClient().getId());
        assertEquals(firstCase.getBox().getId(), secondCase.getBox().getId());
        assertEquals(2, session.getImportedDocuments().size());
    }

    @Test
    void handlesApiFailuresWithoutPersistingPartialItemsAndContinuesToNextItem() {
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

        ScanManager scanManager = createScanManager(apiClient, "failures");

        var session = scanManager.startSession("BOX-99", "Overflow");
        var imported = scanManager.importAllAvailable(session);

        assertEquals(1, session.getFailures().size());
        assertEquals("Temporary API outage", session.getFailures().get(0));
        assertEquals(1, imported.size());
        assertTrue(scanManager.findCaseFile("CASE-99").isPresent());
        assertFalse(scanManager.findCaseFile("Temporary API outage").isPresent());
    }

    private ScanManager createScanManager(ScannerApiClient apiClient, String databaseName) {
        DatabaseConnection databaseConnection = new DatabaseConnection(
                "jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1",
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
