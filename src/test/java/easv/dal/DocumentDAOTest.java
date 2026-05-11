package easv.dal;

import easv.be.Box;
import easv.be.Client;
import easv.be.Document;
import easv.be.PageImage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentDAOTest {

    @Test
    void persistsAddMoveDeleteAndRestorePageOperations() {
        DatabaseConnection databaseConnection = new DatabaseConnection("jdbc:h2:mem:document-dao;DB_CLOSE_DELAY=-1", "sa", "");
        PageImageDAO pageImageDAO = new PageImageDAO(databaseConnection);
        DocumentDAO documentDAO = new DocumentDAO(databaseConnection, pageImageDAO);
        CaseFileDAO caseFileDAO = new CaseFileDAO(databaseConnection, documentDAO);
        ClientDAO clientDAO = new ClientDAO(databaseConnection);
        BoxDAO boxDAO = new BoxDAO(databaseConnection);

        Client client = clientDAO.saveOrGetExisting("CLIENT-1", "Northwind");
        Box box = boxDAO.saveOrGetExisting("BOX-1", "Shelf A");
        var caseFile = caseFileDAO.saveOrGetExisting("CASE-1", client, box);

        Document storedDocument = documentDAO.saveOrGetExisting(new Document(
                "ITEM-1",
                List.of(
                        new PageImage(1, PageImage.PageType.TIFF, "page-1.tiff"),
                        new PageImage(2, PageImage.PageType.TIFF, "page-2.tiff")
                )
        ), caseFile.getId());

        PageImage insertedPage = new PageImage(1, PageImage.PageType.TIFF, "inserted-page.tiff");
        documentDAO.addPage(storedDocument.getId(), insertedPage, 2);
        Document afterInsert = documentDAO.findBySourceItemId("ITEM-1").orElseThrow();
        assertEquals(List.of("page-1.tiff", "inserted-page.tiff", "page-2.tiff"),
                afterInsert.getPages().stream().map(PageImage::getSourceReference).toList());

        documentDAO.movePage(afterInsert.getId(), insertedPage.getId(), 3);
        Document afterMove = documentDAO.findBySourceItemId("ITEM-1").orElseThrow();
        assertEquals(List.of("page-1.tiff", "page-2.tiff", "inserted-page.tiff"),
                afterMove.getPages().stream().map(PageImage::getSourceReference).toList());

        documentDAO.deletePage(afterMove.getId(), insertedPage.getId());
        Document afterDelete = documentDAO.findBySourceItemId("ITEM-1").orElseThrow();
        assertEquals(2, afterDelete.getPages().size());
        assertTrue(afterDelete.getDeletedPages().stream().anyMatch(page -> page.getId().equals(insertedPage.getId())));

        documentDAO.restorePage(afterDelete.getId(), insertedPage.getId(), 1);
        Document afterRestore = documentDAO.findBySourceItemId("ITEM-1").orElseThrow();
        assertEquals(List.of("inserted-page.tiff", "page-1.tiff", "page-2.tiff"),
                afterRestore.getPages().stream().map(PageImage::getSourceReference).toList());
    }
}
