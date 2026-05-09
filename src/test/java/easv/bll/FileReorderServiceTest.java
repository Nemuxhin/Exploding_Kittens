package easv.bll;

import easv.be.PageImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Reordering is one of the easiest places to accidentally corrupt data.
 * These tests are strict about the difference between:
 * - referenceId: original scan order, must remain stable forever.
 * - fileId: current visible/sidebar order, may be recalculated after reordering.
 */
class FileReorderServiceTest {

    private ScanManager scanManager;

    @BeforeEach
    void setUp() {
        scanManager = new ScanManager();
        scanManager.scanFile("page-001.png");
        scanManager.scanFile("page-002.png");
        scanManager.scanFile("page-003.png");
    }

    @Test
    void moveFile_shouldChangeVisibleOrderButKeepOriginalReferenceIds() {
        scanManager.moveFile(2, 0);

        List<PageImage> files = scanManager.getFiles();

        assertAll(
                () -> assertEquals(List.of(3, 1, 2), referenceIdsOf(files), "The page scanned third should now appear first in the UI."),
                () -> assertEquals(List.of(1, 2, 3), fileIdsOf(files), "Visible file ids should be recalculated after reorder."),
                () -> assertEquals(3, files.get(0).getReferenceId(), "The moved page must keep its original scan-order reference id."),
                () -> assertEquals(1, files.get(0).getFileId(), "The moved page should now have visible file id 1 because it appears first.")
        );
    }

    @Test
    void moveFile_toSameIndex_shouldLeaveOrderUnchanged() {
        scanManager.moveFile(1, 1);

        List<PageImage> files = scanManager.getFiles();

        assertAll(
                () -> assertEquals(List.of(1, 2, 3), referenceIdsOf(files)),
                () -> assertEquals(List.of(1, 2, 3), fileIdsOf(files))
        );
    }

    @Test
    void moveFile_shouldRejectIndexesOutsideTheCurrentFileList() {
        assertAll(
                () -> assertThrows(IndexOutOfBoundsException.class, () -> scanManager.moveFile(-1, 0)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> scanManager.moveFile(0, -1)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> scanManager.moveFile(3, 0)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> scanManager.moveFile(0, 3))
        );
    }

    private static List<Integer> referenceIdsOf(List<PageImage> pages) {
        return pages.stream()
                .map(PageImage::getReferenceId)
                .toList();
    }

    private static List<Integer> fileIdsOf(List<PageImage> pages) {
        return pages.stream()
                .map(PageImage::getFileId)
                .toList();
    }
}
