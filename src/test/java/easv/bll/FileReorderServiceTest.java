package easv.bll;

import easv.be.Document;
import easv.be.PageImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Page reordering, deletion, and restoration live on Document.
 * These tests are strict about page-number reindexing after every mutation,
 * because stale page numbers can corrupt export order.
 */
class FileReorderServiceTest {

    private Document document;
    private PageImage firstPage;
    private PageImage secondPage;
    private PageImage thirdPage;

    @BeforeEach
    void setUp() {
        firstPage  = new PageImage(1, PageImage.PageType.TIFF, "page-001.png");
        secondPage = new PageImage(2, PageImage.PageType.TIFF, "page-002.png");
        thirdPage  = new PageImage(3, PageImage.PageType.TIFF, "page-003.png");
        document   = new Document("ITEM-001", List.of(firstPage, secondPage, thirdPage));
    }

    @Test
    void movePage_shouldChangeOrderAndReindexPageNumbers() {
        document.movePage(thirdPage.getId(), 1);
        List<PageImage> pages = document.getPages();

        assertAll(
                () -> assertEquals(thirdPage.getId(), pages.get(0).getId(), "Third page should now appear first."),
                () -> assertEquals(1, pages.get(0).getPageNumber(), "Moved page should have page number 1 after reindex."),
                () -> assertEquals(2, pages.get(1).getPageNumber()),
                () -> assertEquals(3, pages.get(2).getPageNumber())
        );
    }

    @Test
    void movePage_toSamePosition_shouldLeaveOrderUnchanged() {
        document.movePage(firstPage.getId(), 1);
        List<PageImage> pages = document.getPages();

        assertAll(
                () -> assertEquals(firstPage.getId(),  pages.get(0).getId()),
                () -> assertEquals(secondPage.getId(), pages.get(1).getId()),
                () -> assertEquals(thirdPage.getId(),  pages.get(2).getId())
        );
    }

    @Test
    void movePage_withUnknownId_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> document.movePage(UUID.randomUUID(), 1),
                "Moving a page that does not exist should throw.");
    }

    @Test
    void deletePage_shouldExcludePageFromGetPagesAndReindexRemaining() {
        document.deletePage(secondPage.getId());

        assertAll(
                () -> assertEquals(2, document.getPages().size(),        "Active pages exclude the deleted one."),
                () -> assertEquals(1, document.getDeletedPages().size(), "Deleted page should appear in deleted list."),
                () -> assertEquals(secondPage.getId(), document.getDeletedPages().get(0).getId()),
                () -> assertEquals(1, document.getPages().get(0).getPageNumber(), "Active pages must be reindexed."),
                () -> assertEquals(2, document.getPages().get(1).getPageNumber())
        );
    }

    @Test
    void restorePage_shouldBringPageBackAtRequestedPosition() {
        document.deletePage(secondPage.getId());
        document.restorePage(secondPage.getId(), 2);

        List<PageImage> pages = document.getPages();

        assertAll(
                () -> assertEquals(3, pages.size(), "Restored page should appear back in the active list."),
                () -> assertEquals(secondPage.getId(), pages.get(1).getId(), "Restored page placed at position 2.")
        );
    }

    @Test
    void addPage_shouldInsertAtSpecifiedPositionAndReindex() {
        PageImage newPage = new PageImage(1, PageImage.PageType.TIFF, "new-page.png");
        document.addPage(newPage, 2);

        List<PageImage> pages = document.getPages();

        assertAll(
                () -> assertEquals(4, pages.size()),
                () -> assertEquals(newPage.getId(), pages.get(1).getId(), "New page inserted at position 2."),
                () -> assertEquals(2, newPage.getPageNumber(), "Inserted page renumbered to reflect its position.")
        );
    }
}
