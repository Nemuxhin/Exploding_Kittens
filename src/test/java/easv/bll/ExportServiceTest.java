package easv.bll;

import easv.be.PageImage;
import easv.be.TiffExportItem;
import easv.be.TiffExportPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Export planning is tested here, not actual TIFF encoding.
 *
 * TiffExportManager builds a plan (file names, page assignments, warnings)
 * that the GUI shows before any files are written. These tests verify that
 * the plan is correct — encoding is a separate concern.
 */
class ExportServiceTest {

    private TiffExportManager exportManager;
    private PageImage pageOne;
    private PageImage pageTwo;
    private PageImage pageThree;

    @BeforeEach
    void setUp() {
        exportManager = new TiffExportManager();
        pageOne   = new PageImage(1, PageImage.PageType.TIFF, "DOC-001");
        pageTwo   = new PageImage(2, PageImage.PageType.TIFF, "DOC-001");
        pageThree = new PageImage(3, PageImage.PageType.TIFF, "DOC-002");
    }

    @Test
    void createSinglePagePlan_shouldCreateOneItemPerPage() {
        TiffExportPlan plan = exportManager.createSinglePagePlan("Medical", "BOX-1",
                List.of(pageOne, pageTwo, pageThree));

        assertAll(
                () -> assertEquals("SINGLE_PAGE_TIFFS", plan.getExportType()),
                () -> assertEquals(3, plan.getFileCount(), "One TIFF file per page."),
                () -> assertEquals(3, plan.getPageCount()),
                () -> assertTrue(plan.getItems().stream().allMatch(item -> item.getPages().size() == 1),
                        "Each export item must contain exactly one page.")
        );
    }

    @Test
    void createMultiPagePlan_shouldCombineAllPagesIntoOneItem() {
        TiffExportPlan plan = exportManager.createMultiPagePlan("Medical", "BOX-1",
                List.of(pageOne, pageTwo, pageThree));

        assertAll(
                () -> assertEquals("MULTI_PAGE_TIFF_FILE", plan.getExportType()),
                () -> assertEquals(1, plan.getFileCount(), "Multi-page export produces exactly one TIFF file."),
                () -> assertEquals(3, plan.getPageCount(), "All three pages go into the single file.")
        );
    }

    @Test
    void singlePageFileName_shouldFollowProfileBoxDocumentPagePattern() {
        TiffExportPlan plan = exportManager.createSinglePagePlan("Medical", "BOX-1", List.of(pageOne));

        String fileName = plan.getItems().get(0).getFileName();

        assertAll(
                () -> assertTrue(fileName.startsWith("Medical_BOX-1_"), "Profile and box must prefix the filename."),
                () -> assertTrue(fileName.endsWith(".tiff"), "Export must be TIFF.")
        );
    }

    @Test
    void specialCharactersInProfileName_shouldBeReplacedWithUnderscores() {
        TiffExportPlan plan = exportManager.createSinglePagePlan("Building Archive", "BOX-1", List.of(pageOne));

        String fileName = plan.getItems().get(0).getFileName();

        assertTrue(fileName.startsWith("Building_Archive_BOX-1_"),
                "Spaces in profile name should be replaced by underscores.");
    }

    @Test
    void warnings_shouldBeEmptyWhenProfileAndBoxAreProvided() {
        TiffExportPlan plan = exportManager.createSinglePagePlan("Medical", "BOX-1", List.of(pageOne));

        assertTrue(plan.getWarnings().isEmpty(), "No warnings expected when profile and box are both set.");
    }

    @Test
    void warnings_shouldBeAddedWhenProfileOrBoxIsMissing() {
        TiffExportPlan missingProfile = exportManager.createSinglePagePlan("", "BOX-1", List.of(pageOne));
        TiffExportPlan missingBox     = exportManager.createSinglePagePlan("Medical", "", List.of(pageOne));
        TiffExportPlan bothMissing    = exportManager.createMultiPagePlan("", "", List.of(pageOne));

        assertAll(
                () -> assertEquals(1, missingProfile.getWarnings().size(), "Missing profile → one warning."),
                () -> assertEquals(1, missingBox.getWarnings().size(),     "Missing box → one warning."),
                () -> assertEquals(2, bothMissing.getWarnings().size(),    "Both missing → two warnings.")
        );
    }

    @Test
    void nullPageList_shouldProduceEmptyPlanWithoutThrowing() {
        TiffExportPlan single = exportManager.createSinglePagePlan("Medical", "BOX-1", null);
        TiffExportPlan multi  = exportManager.createMultiPagePlan( "Medical", "BOX-1", null);

        assertAll(
                () -> assertEquals(0, single.getFileCount()),
                () -> assertEquals(0, multi.getFileCount())
        );
    }
}
