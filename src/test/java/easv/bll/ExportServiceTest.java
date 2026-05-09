package easv.bll;

import easv.be.ExportJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These tests intentionally test export planning, not low-level TIFF encoding.
 *
 * Why: JUnit should prove that the business rules are correct:
 * - multi-page export creates one TIFF per document.
 * - single-page export creates one TIFF per scanned page.
 * - export names are connected to profile/client and box.
 * - no non-TIFF export plan is allowed.
 *
 * Actual image encoding can be tested later with a smaller integration test.
 */
class ExportServiceTest {

    @TempDir
    Path exportDirectory;

    private ScanManager scanManager;
    private ExportService exportService;

    @BeforeEach
    void setUp() {
        scanManager = new ScanManager();
        exportService = new ExportService();
    }

    @Test
    void planMultiPageExport_shouldCreateOneTiffPerDocument() {
        scanManager.scanFile("page-001.png");
        scanManager.scanFile("page-002.png");
        scanManager.scanBarcode("REG-2026-0001");
        scanManager.scanFile("page-003.png");

        List<ExportJob> jobs = exportService.planExport(
                scanManager.getDocuments(),
                ExportMode.MULTI_PAGE,
                "ClientA",
                "BOX-42",
                exportDirectory
        );

        assertAll(
                () -> assertEquals(2, jobs.size(), "Multi-page export should create one output TIFF per document."),
                () -> assertEquals(2, jobs.get(0).getPages().size(), "The first document contains two pages."),
                () -> assertEquals(1, jobs.get(1).getPages().size(), "The second document contains one page."),
                () -> assertTrue(fileNameOf(jobs.get(0)).startsWith("ClientA_BOX-42_"), "Export name should include profile/client and box id."),
                () -> assertTrue(fileNameOf(jobs.get(0)).endsWith(".tiff"), "Export must be TIFF."),
                () -> assertTrue(fileNameOf(jobs.get(1)).endsWith(".tiff"), "Export must be TIFF.")
        );
    }

    @Test
    void planSinglePageExport_shouldCreateOneTiffPerScannedFile() {
        scanManager.scanFile("page-001.png");
        scanManager.scanFile("page-002.png");
        scanManager.scanBarcode("REG-2026-0001");
        scanManager.scanFile("page-003.png");

        List<ExportJob> jobs = exportService.planExport(
                scanManager.getDocuments(),
                ExportMode.SINGLE_PAGE,
                "ClientA",
                "BOX-42",
                exportDirectory
        );

        assertAll(
                () -> assertEquals(3, jobs.size(), "Single-page export should create one output TIFF per scanned file."),
                () -> assertTrue(jobs.stream().allMatch(job -> job.getPages().size() == 1), "Each single-page job should contain exactly one page."),
                () -> assertTrue(jobs.stream().allMatch(job -> fileNameOf(job).startsWith("ClientA_BOX-42_")), "Every export name should include profile/client and box id."),
                () -> assertTrue(jobs.stream().allMatch(job -> fileNameOf(job).endsWith(".tiff")), "Every export job must output TIFF.")
        );
    }

    @Test
    void planExport_shouldRejectMissingBoxIdBecauseBoxesMustBeAttachedToExport() {
        scanManager.scanFile("page-001.png");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> exportService.planExport(scanManager.getDocuments(), ExportMode.MULTI_PAGE, "ClientA", null, exportDirectory)),
                () -> assertThrows(IllegalArgumentException.class, () -> exportService.planExport(scanManager.getDocuments(), ExportMode.MULTI_PAGE, "ClientA", "", exportDirectory)),
                () -> assertThrows(IllegalArgumentException.class, () -> exportService.planExport(scanManager.getDocuments(), ExportMode.MULTI_PAGE, "ClientA", "   ", exportDirectory))
        );
    }

    @Test
    void planExport_shouldRejectMissingProfileNameBecauseProfileLabelsTheExport() {
        scanManager.scanFile("page-001.png");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> exportService.planExport(scanManager.getDocuments(), ExportMode.MULTI_PAGE, null, "BOX-42", exportDirectory)),
                () -> assertThrows(IllegalArgumentException.class, () -> exportService.planExport(scanManager.getDocuments(), ExportMode.MULTI_PAGE, "", "BOX-42", exportDirectory)),
                () -> assertThrows(IllegalArgumentException.class, () -> exportService.planExport(scanManager.getDocuments(), ExportMode.MULTI_PAGE, "   ", "BOX-42", exportDirectory))
        );
    }

    @Test
    void validateExportFormat_shouldRejectAnythingExceptTiff() {
        assertAll(
                () -> exportService.validateExportFormat("tiff"),
                () -> exportService.validateExportFormat("tif"),
                () -> assertThrows(IllegalArgumentException.class, () -> exportService.validateExportFormat("pdf")),
                () -> assertThrows(IllegalArgumentException.class, () -> exportService.validateExportFormat("png")),
                () -> assertThrows(IllegalArgumentException.class, () -> exportService.validateExportFormat("jpg"))
        );
    }

    private static String fileNameOf(ExportJob job) {
        return job.getOutputPath().getFileName().toString();
    }
}
