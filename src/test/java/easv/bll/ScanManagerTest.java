package easv.bll;

import easv.be.Document;
import easv.be.PageImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pure unit/contract tests for the scanner business rules.
 *
 * This class should NOT use JavaFX, real scanner hardware, database access, DAOs,
 * real image files, or API calls. Those belong in integration tests.
 *
 * Vocabulary used by the project requirements:
 * - referenceId: stable id based on the original scan order. It must never change.
 * - fileId: visible/current id based on the page's current order in the UI/sidebar.
 * - document: a group of scanned pages split by barcode markers.
 */

class ScanManagerTest {

    private ScanManager scanManager;

    @BeforeEach
    void setUp() {
        scanManager = new ScanManager();
    }

    @Test
    void scanFile_shouldAssignStableReferenceIdsAndVisibleFileIdsInScanOrder() {
        PageImage firstPage = scanManager.scanFile("page-001.png");
        PageImage secondPage = scanManager.scanFile("page-002.png");
        PageImage thirdPage = scanManager.scanFile("page-003.png");

        assertAll(
                () -> assertEquals(1, firstPage.getReferenceId(), "First scanned page must get reference id 1."),
                () -> assertEquals(2, secondPage.getReferenceId(), "Second scanned page must get reference id 2."),
                () -> assertEquals(3, thirdPage.getReferenceId(), "Third scanned page must get reference id 3."),

                () -> assertEquals(1, firstPage.getFileId(), "Visible file id should initially match current UI order."),
                () -> assertEquals(2, secondPage.getFileId(), "Visible file id should initially match current UI order."),
                () -> assertEquals(3, thirdPage.getFileId(), "Visible file id should initially match current UI order."),

                () -> assertEquals(3, scanManager.getTotalScanCount(), "Total scan count should count scanned pages only."),
                () -> assertEquals(List.of(1, 2, 3), referenceIdsOf(scanManager.getFiles()), "Files should initially be shown in scan order.")
        );
    }

    @Test
    void scanFile_shouldCreateFirstDocumentAutomatically() {
        PageImage firstPage = scanManager.scanFile("page-001.png");

        List<Document> documents = scanManager.getDocuments();

        assertAll(
                () -> assertEquals(1, documents.size(), "Scanning the first page should create the first document automatically."),
                () -> assertEquals(List.of(firstPage.getReferenceId()), referenceIdsOf(documents.get(0)), "The first page should belong to the first document.")
        );
    }

    @Test
    void scanFile_shouldKeepPagesInCurrentDocumentUntilBarcodeIsScanned() {
        PageImage firstPage = scanManager.scanFile("page-001.png");
        PageImage secondPage = scanManager.scanFile("page-002.png");
        PageImage thirdPage = scanManager.scanFile("page-003.png");

        List<Document> documents = scanManager.getDocuments();

        assertAll(
                () -> assertEquals(1, documents.size(), "Pages should stay in the same document until a barcode marker is scanned."),
                () -> assertEquals(
                        List.of(firstPage.getReferenceId(), secondPage.getReferenceId(), thirdPage.getReferenceId()),
                        referenceIdsOf(documents.get(0)),
                        "All pages scanned before a barcode should belong to document 1."
                )
        );
    }

    @Test
    void scanBarcode_shouldStartNewDocumentForFollowingPagesOnly() {
        PageImage firstPage = scanManager.scanFile("page-001.png");
        PageImage secondPage = scanManager.scanFile("page-002.png");

        scanManager.scanBarcode("REG-2026-0001");

        PageImage thirdPage = scanManager.scanFile("page-003.png");
        PageImage fourthPage = scanManager.scanFile("page-004.png");

        List<Document> documents = scanManager.getDocuments();

        assertAll(
                () -> assertEquals(2, documents.size(), "One barcode marker should split the batch into two exportable documents."),
                () -> assertEquals(List.of(1, 2), referenceIdsOf(documents.get(0)), "Pages before the barcode should stay in document 1."),
                () -> assertEquals(List.of(3, 4), referenceIdsOf(documents.get(1)), "Pages after the barcode should be placed in document 2."),
                () -> assertEquals(4, scanManager.getTotalScanCount(), "The barcode divider itself should not be counted as an exported page."),
                () -> assertEquals(List.of(1, 2, 3, 4), referenceIdsOf(scanManager.getFiles()), "Barcode splitting must not reset reference ids.")
        );
    }

    @Test
    void scanBarcode_beforeAnyPage_shouldNotCreateBlankExportDocument() {
        scanManager.scanBarcode("REG-2026-0001");
        PageImage firstPage = scanManager.scanFile("page-001.png");

        List<Document> documents = scanManager.getDocuments();

        assertAll(
                () -> assertEquals(1, documents.size(), "A barcode before the first real page should not create an empty document."),
                () -> assertEquals(List.of(firstPage.getReferenceId()), referenceIdsOf(documents.get(0)), "The first real page should still become the first exportable document."),
                () -> assertEquals(1, scanManager.getTotalScanCount(), "Only real page files should count as scans.")
        );
    }

    @Test
    void consecutiveBarcodes_shouldNotCreateEmptyExportDocuments() {
        PageImage firstPage = scanManager.scanFile("page-001.png");

        scanManager.scanBarcode("REG-2026-0001");
        scanManager.scanBarcode("REG-2026-0002");

        PageImage secondPage = scanManager.scanFile("page-002.png");

        List<Document> documents = scanManager.getDocuments();

        assertAll(
                () -> assertEquals(2, documents.size(), "Consecutive barcode markers should not create empty documents that later export as blank TIFFs."),
                () -> assertEquals(List.of(firstPage.getReferenceId()), referenceIdsOf(documents.get(0))),
                () -> assertEquals(List.of(secondPage.getReferenceId()), referenceIdsOf(documents.get(1))),
                () -> assertEquals(List.of(1, 2), referenceIdsOf(scanManager.getFiles()), "Barcode markers must not appear in the normal scanned-file list.")
        );
    }

    @Test
    void scanFile_shouldRejectBlankFileNames() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> scanManager.scanFile(null), "Null file names should be rejected."),
                () -> assertThrows(IllegalArgumentException.class, () -> scanManager.scanFile(""), "Empty file names should be rejected."),
                () -> assertThrows(IllegalArgumentException.class, () -> scanManager.scanFile("   "), "Whitespace-only file names should be rejected.")
        );
    }

    private static List<Integer> referenceIdsOf(List<PageImage> pages) {
        return pages.stream()
                .map(PageImage::getReferenceId)
                .toList();
    }

    private static List<Integer> referenceIdsOf(Document document) {
        return referenceIdsOf(document.getPages());
    }
}
