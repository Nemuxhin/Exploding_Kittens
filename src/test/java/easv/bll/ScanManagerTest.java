package easv.bll;

import easv.be.Box;
import easv.be.Document;
import easv.be.PageImage;
import easv.be.ScanSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for the barcode-to-document grouping rules.
 *
 * The old ScanManager (scanFile/scanBarcode/getDocuments) was replaced by a
 * session + DAO model, so those tests no longer apply. The rule that genuinely
 * matters — how a scanned batch is split into exportable documents at barcode
 * markers — now lives in ScanManager.splitIntoDocuments. We exercise it directly
 * with in-memory pages, so there is NO scanner, database, network, or real TIFF
 * decoding involved. Barcode classification itself is covered separately by
 * BarcodeSplitServiceTest.
 *
 * Vocabulary:
 * - referenceId: stable id based on the original scan order. Allocated per page.
 * - document: a group of scanned pages split by barcode markers.
 */
class ScanManagerTest {

    private static final String START_NEW_DOCUMENT = "Start new document";
    private static final String END_CURRENT_DOCUMENT = "End current document";
    private static final String KEEP_BARCODE_IN_FINAL = "Keep barcode page in final document";
    private static final String MOVE_BARCODE_TO_SEPARATE = "Move barcode page to separate document";

    private final ScanManager scanManager = new ScanManager();

    @Test
    void noBarcode_keepsEveryPageInASingleDocumentInScanOrder() {
        List<TiffFetchService.FetchedPage> pages = List.of(
                normalPage(1, "page-001.tiff"),
                normalPage(2, "page-002.tiff"),
                normalPage(3, "page-003.tiff"));

        ScanManager.BarcodeHandlingResult result = scanManager.splitIntoDocuments(
                "ITEM-1", pages, session(), START_NEW_DOCUMENT, KEEP_BARCODE_IN_FINAL, true);

        assertAll(
                () -> assertEquals(1, result.documents().size(), "No barcode marker → one document."),
                () -> assertEquals(List.of(1, 2, 3), referenceIds(result.documents().get(0)),
                        "All pages stay in the first document, in scan order."),
                () -> assertFalse(result.stoppedOnBarcode(), "Scanning should not stop without an end-document barcode.")
        );
    }

    @Test
    void barcodeMarker_splitsTheBatchAndKeepsTheMarkerWithTheFollowingPages() {
        List<TiffFetchService.FetchedPage> pages = List.of(
                normalPage(1, "page-001.tiff"),
                normalPage(2, "page-002.tiff"),
                barcodePage(3),
                normalPage(4, "page-003.tiff"));

        ScanManager.BarcodeHandlingResult result = scanManager.splitIntoDocuments(
                "ITEM-1", pages, session(), START_NEW_DOCUMENT, KEEP_BARCODE_IN_FINAL, true);

        assertAll(
                () -> assertEquals(2, result.documents().size(), "One barcode marker splits the batch into two documents."),
                () -> assertEquals(List.of(1, 2), referenceIds(result.documents().get(0)),
                        "Pages before the barcode belong to the first document."),
                () -> assertEquals(List.of(3, 4), referenceIds(result.documents().get(1)),
                        "The barcode page leads the second document, followed by later pages."),
                () -> assertFalse(result.stoppedOnBarcode())
        );
    }

    @Test
    void moveBarcodeToSeparateDocument_putsTheMarkerInItsOwnDocument() {
        List<TiffFetchService.FetchedPage> pages = List.of(
                normalPage(1, "page-001.tiff"),
                barcodePage(2),
                normalPage(3, "page-002.tiff"));

        ScanManager.BarcodeHandlingResult result = scanManager.splitIntoDocuments(
                "ITEM-1", pages, session(), START_NEW_DOCUMENT, MOVE_BARCODE_TO_SEPARATE, true);

        assertAll(
                () -> assertEquals(3, result.documents().size(),
                        "The barcode page becomes its own document between the two content documents."),
                () -> assertEquals(List.of(1), referenceIds(result.documents().get(0))),
                () -> assertEquals(List.of(2), referenceIds(result.documents().get(1)), "Barcode page is alone."),
                () -> assertEquals(List.of(3), referenceIds(result.documents().get(2)))
        );
    }

    @Test
    void endCurrentDocumentBehavior_stopsScanningAtTheBarcode() {
        List<TiffFetchService.FetchedPage> pages = List.of(
                normalPage(1, "page-001.tiff"),
                normalPage(2, "page-002.tiff"),
                barcodePage(3),
                normalPage(4, "page-003.tiff")); // must never be processed

        ScanManager.BarcodeHandlingResult result = scanManager.splitIntoDocuments(
                "ITEM-1", pages, session(), END_CURRENT_DOCUMENT, KEEP_BARCODE_IN_FINAL, true);

        assertAll(
                () -> assertTrue(result.stoppedOnBarcode(), "An end-current-document barcode halts scanning."),
                () -> assertFalse(result.message().isBlank(), "A stop reason should be reported."),
                () -> assertEquals(3, result.scannedPages().size(),
                        "Only the pages up to and including the barcode are processed; the 4th page is left untouched."),
                () -> assertEquals(2, result.documents().size())
        );
    }

    @Test
    void barcodeBeforeAnyPage_doesNotCreateAnEmptyLeadingDocument() {
        List<TiffFetchService.FetchedPage> pages = List.of(
                barcodePage(1),
                normalPage(2, "page-001.tiff"));

        ScanManager.BarcodeHandlingResult result = scanManager.splitIntoDocuments(
                "ITEM-1", pages, session(), START_NEW_DOCUMENT, KEEP_BARCODE_IN_FINAL, true);

        assertAll(
                () -> assertEquals(1, result.documents().size(),
                        "A leading barcode must not produce an empty document that would export as a blank TIFF."),
                () -> assertNoEmptyDocuments(result)
        );
    }

    @Test
    void consecutiveBarcodes_neverProduceEmptyDocuments() {
        List<TiffFetchService.FetchedPage> pages = List.of(
                normalPage(1, "page-001.tiff"),
                barcodePage(2),
                barcodePage(3),
                normalPage(4, "page-002.tiff"));

        ScanManager.BarcodeHandlingResult result = scanManager.splitIntoDocuments(
                "ITEM-1", pages, session(), START_NEW_DOCUMENT, KEEP_BARCODE_IN_FINAL, true);

        assertAll(
                () -> assertTrue(result.documents().size() >= 2, "Consecutive barcodes still split the batch."),
                () -> assertNoEmptyDocuments(result),
                () -> assertEquals(4, totalPages(result.documents()),
                        "Every scanned page ends up in exactly one document; none are dropped or duplicated.")
        );
    }

    @Test
    void barcodeSplittingDisabled_ignoresBarcodeMarkersEntirely() {
        List<TiffFetchService.FetchedPage> pages = List.of(
                normalPage(1, "page-001.tiff"),
                barcodePage(2),
                normalPage(3, "page-002.tiff"));

        ScanManager.BarcodeHandlingResult result = scanManager.splitIntoDocuments(
                "ITEM-1", pages, session(), START_NEW_DOCUMENT, KEEP_BARCODE_IN_FINAL, false);

        assertAll(
                () -> assertEquals(1, result.documents().size(),
                        "With splitting off, a barcode-looking page is treated as an ordinary page."),
                () -> assertEquals(List.of(1, 2, 3), referenceIds(result.documents().get(0)))
        );
    }

    // ---------- helpers ----------

    private ScanSession session() {
        return new ScanSession(new Box("BOX-1", "Test box"), "Medical");
    }

    /** An ordinary content page: empty file data means no TIFF decoding is attempted. */
    private static TiffFetchService.FetchedPage normalPage(int pageNumber, String sourceReference) {
        return new TiffFetchService.FetchedPage(pageNumber, sourceReference, "", "", new byte[0]);
    }

    /**
     * A page that classifies as a barcode separator. A barcode-like source reference
     * plus a strong barcode value is enough for BarcodeSplitService to flag it without
     * any real image bytes, which keeps this test free of TIFF decoding.
     */
    private static TiffFetchService.FetchedPage barcodePage(int pageNumber) {
        return new TiffFetchService.FetchedPage(pageNumber, "separator_barcode.tiff", "", "ABC-12345", new byte[0]);
    }

    private static List<Integer> referenceIds(Document document) {
        return document.getPages().stream().map(PageImage::getReferenceId).toList();
    }

    private static int totalPages(List<Document> documents) {
        return documents.stream().mapToInt(document -> document.getPages().size()).sum();
    }

    private static void assertNoEmptyDocuments(ScanManager.BarcodeHandlingResult result) {
        assertTrue(result.documents().stream().allMatch(document -> !document.getPages().isEmpty()),
                "No exportable document should be empty.");
    }
}
