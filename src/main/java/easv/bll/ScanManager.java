package easv.bll;

import easv.be.Box;
import easv.be.CaseFile;
import easv.be.Client;
import easv.be.Document;
import easv.be.PageImage;
import easv.be.ScanSession;
import easv.dal.BoxDAO;
import easv.dal.CaseFileDAO;
import easv.dal.ClientDAO;
import easv.dal.DatabaseConnection;
import easv.dal.DocumentDAO;
import easv.dal.PageImageDAO;
import easv.dal.ScanSessionDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ScanManager {
    private final ScannerApiClient scannerApiClient;
    private final BarcodeSplitService barcodeSplitService;
    private final ClientDAO clientDAO;
    private final BoxDAO boxDAO;
    private final CaseFileDAO caseFileDAO;
    private final DocumentDAO documentDAO;
    private final ScanSessionDAO scanSessionDAO;
    private final AuditLogManager auditLogManager;

    public ScanManager() {
        DatabaseConnection databaseConnection = new DatabaseConnection();
        PageImageDAO pageImageDAO = new PageImageDAO(databaseConnection);
        DocumentDAO documentDAO = new DocumentDAO(databaseConnection, pageImageDAO);
        this.scannerApiClient = new ScannerApiClient();
        this.barcodeSplitService = new BarcodeSplitService();
        this.clientDAO = new ClientDAO(databaseConnection);
        this.boxDAO = new BoxDAO(databaseConnection);
        this.caseFileDAO = new CaseFileDAO(databaseConnection, documentDAO);
        this.documentDAO = documentDAO;
        this.scanSessionDAO = new ScanSessionDAO(databaseConnection);
        this.auditLogManager = new AuditLogManager();
    }

    public ScanManager(
            ScannerApiClient scannerApiClient,
            BarcodeSplitService barcodeSplitService,
            ClientDAO clientDAO,
            BoxDAO boxDAO,
            CaseFileDAO caseFileDAO,
            DocumentDAO documentDAO,
            ScanSessionDAO scanSessionDAO
    ) {
        this(scannerApiClient, barcodeSplitService, clientDAO, boxDAO, caseFileDAO,
                documentDAO, scanSessionDAO, new AuditLogManager());
    }

    public ScanManager(
            ScannerApiClient scannerApiClient,
            BarcodeSplitService barcodeSplitService,
            ClientDAO clientDAO,
            BoxDAO boxDAO,
            CaseFileDAO caseFileDAO,
            DocumentDAO documentDAO,
            ScanSessionDAO scanSessionDAO,
            AuditLogManager auditLogManager
    ) {
        this.scannerApiClient = Objects.requireNonNull(scannerApiClient, "scannerApiClient");
        this.barcodeSplitService = Objects.requireNonNull(barcodeSplitService, "barcodeSplitService");
        this.clientDAO = Objects.requireNonNull(clientDAO, "clientDAO");
        this.boxDAO = Objects.requireNonNull(boxDAO, "boxDAO");
        this.caseFileDAO = Objects.requireNonNull(caseFileDAO, "caseFileDAO");
        this.documentDAO = Objects.requireNonNull(documentDAO, "documentDAO");
        this.scanSessionDAO = Objects.requireNonNull(scanSessionDAO, "scanSessionDAO");
        this.auditLogManager = Objects.requireNonNull(auditLogManager, "auditLogManager");
    }

    public Client registerClient(String clientNumber, String name) {
        return clientDAO.saveOrGetExisting(clientNumber, name);
    }

    public Box registerBox(String boxId, String description) {
        return boxDAO.saveOrGetExisting(boxId, description);
    }

    public ScanSession startSession(String boxId, String description) {
        ScanSession session = new ScanSession(registerBox(boxId, description));
        scanSessionDAO.save(session);
        auditLogManager.logUserAction(AuditLogManager.SCAN_STARTED, null, null, null,
                null, null, boxId, "Scan started.");
        return session;
    }

    public Optional<Document> importNextItem(ScanSession session) {
        Objects.requireNonNull(session, "session");

        Optional<ScannerApiClient.ApiTiffItem> nextItem;
        try {
            nextItem = scannerApiClient.fetchNextItem();
        } catch (ScannerApiClient.ScannerApiException ex) {
            session.recordFailure(ex.getMessage());
            scanSessionDAO.recordFailure(session, ex.getMessage());
            auditLogManager.logSystemAction(AuditLogManager.SCAN_FAILED, null, null, null,
                    null, null, session.getBox().getBoxId(), ex.getMessage());
            return Optional.empty();
        }

        if (nextItem.isEmpty()) {
            return Optional.empty();
        }

        ScannerApiClient.ApiTiffItem item = nextItem.get();
        List<PageImage> pages = classifyPages(item.pages());
        Client client = registerClient(item.clientNumber(), item.clientName());
        Box box = registerBox(item.boxId(), item.boxDescription());
        CaseFile caseFile = caseFileDAO.saveOrGetExisting(item.caseReference(), client, box);
        Document storedDocument = documentDAO.saveOrGetExisting(new Document(item.itemId(), pages), caseFile.getId());
        caseFile.addDocument(storedDocument);
        session.addImportedDocument(storedDocument);
        scanSessionDAO.linkDocument(session, storedDocument);
        logImportedItem(item, storedDocument, caseFile);
        return Optional.of(storedDocument);
    }

    public List<Document> importAllAvailable(ScanSession session) {
        Objects.requireNonNull(session, "session");
        List<Document> imported = new ArrayList<>();
        while (true) {
            int failuresBefore = session.getFailures().size();
            Optional<Document> document = importNextItem(session);
            if (document.isPresent()) {
                imported.add(document.get());
                continue;
            }
            if (session.getFailures().size() > failuresBefore) {
                auditLogManager.logUserAction(AuditLogManager.RETRY_USED, null, null, null,
                        null, null, session.getBox().getBoxId(), "Scan import continued after a failed scanner call.");
                continue;
            }
            break;
        }
        auditLogManager.logUserAction(AuditLogManager.SCAN_COMPLETED, null, null, null,
                null, null, session.getBox().getBoxId(), "Scan import completed.");
        return imported;
    }

    public Optional<Client> findClient(String clientNumber) {
        return clientDAO.findByClientNumber(clientNumber);
    }

    public Optional<Box> findBox(String boxId) {
        return boxDAO.findByBoxId(boxId);
    }

    public Optional<CaseFile> findCaseFile(String caseReference) {
        return caseFileDAO.findByReference(caseReference);
    }

    private List<PageImage> classifyPages(List<ScannerApiClient.ApiTiffPage> pages) {
        List<PageImage> pageImages = new ArrayList<>();
        for (ScannerApiClient.ApiTiffPage page : pages) {
            PageImage.PageType pageType = barcodeSplitService.classify(page.sourceReference());
            pageImages.add(new PageImage(page.pageNumber(), pageType, page.sourceReference()));
        }
        return pageImages;
    }

    private void logImportedItem(ScannerApiClient.ApiTiffItem item, Document document, CaseFile caseFile) {
        String caseReference = caseFile.getCaseReference();
        String documentId = document.getSourceItemId();

        for (PageImage page : document.getPages()) {
            String fileId = page.getSourceReference();

            auditLogManager.logUserAction(AuditLogManager.TIFF_FETCHED, caseReference, documentId, fileId,
                    page.getPageNumber(), null, item.boxId(), "TIFF fetched from scanner API.");
            auditLogManager.logPageCreated(caseReference, documentId, fileId,
                    page.getPageNumber(), null, item.boxId());

            if (PageImage.PageType.BARCODE.equals(page.getPageType())) {
                auditLogManager.logUserAction(AuditLogManager.BARCODE_DETECTED, caseReference, documentId, fileId,
                        page.getPageNumber(), null, item.boxId(), "Barcode separator page detected.");
            }
        }
    }
}
