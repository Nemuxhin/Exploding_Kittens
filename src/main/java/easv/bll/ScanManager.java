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
    private final TiffFetchService tiffFetchService;
    private final BarcodeSplitService barcodeSplitService;
    private final ClientDAO clientDAO;
    private final BoxDAO boxDAO;
    private final CaseFileDAO caseFileDAO;
    private final DocumentDAO documentDAO;
    private final ScanSessionDAO scanSessionDAO;

    public ScanManager() {
        DatabaseConnection databaseConnection = new DatabaseConnection();
        PageImageDAO pageImageDAO = new PageImageDAO(databaseConnection);
        DocumentDAO documentDAO = new DocumentDAO(databaseConnection, pageImageDAO);
        this.scannerApiClient = new ScannerApiClient();
        this.tiffFetchService = new TiffFetchService(this.scannerApiClient);
        this.barcodeSplitService = new BarcodeSplitService();
        this.clientDAO = new ClientDAO(databaseConnection);
        this.boxDAO = new BoxDAO(databaseConnection);
        this.caseFileDAO = new CaseFileDAO(databaseConnection, documentDAO);
        this.documentDAO = documentDAO;
        this.scanSessionDAO = new ScanSessionDAO(databaseConnection);
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
        this.scannerApiClient = Objects.requireNonNull(scannerApiClient, "scannerApiClient");
        this.tiffFetchService = new TiffFetchService(this.scannerApiClient);
        this.barcodeSplitService = Objects.requireNonNull(barcodeSplitService, "barcodeSplitService");
        this.clientDAO = Objects.requireNonNull(clientDAO, "clientDAO");
        this.boxDAO = Objects.requireNonNull(boxDAO, "boxDAO");
        this.caseFileDAO = Objects.requireNonNull(caseFileDAO, "caseFileDAO");
        this.documentDAO = Objects.requireNonNull(documentDAO, "documentDAO");
        this.scanSessionDAO = Objects.requireNonNull(scanSessionDAO, "scanSessionDAO");
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
        return session;
    }

    public Optional<Document> importNextItem(ScanSession session) {
        ScanImportResult result = scanNextItem(session, session.getSelectedBarcodeBehavior(), "Keep barcode page in final document");
        if (!result.getImportedDocuments().isEmpty()) {
            return Optional.of(result.getImportedDocuments().get(0));
        }
        return Optional.empty();
    }

    public ScanImportResult scanNextItem(ScanSession session, String barcodeBehavior, String barcodePageBehavior) {
        Objects.requireNonNull(session, "session");
        session.setSelectedBarcodeBehavior(barcodeBehavior);

        TiffFetchService.FetchResult fetchResult = tiffFetchService.fetchNextItem();
        if (fetchResult.noMoreItems()) {
            session.setLastStatus("NO_MORE_FILES");
            scanSessionDAO.updateSessionState(session);
            return ScanImportResult.noMoreFiles();
        }

        if (fetchResult.isFailure()) {
            session.recordFailure(fetchResult.message());
            scanSessionDAO.recordFailure(session, fetchResult.message());
            return ScanImportResult.failed(fetchResult.message());
        }

        TiffFetchService.FetchedItem fetchedItem = fetchResult.item();
        ScannerApiClient.ApiTiffItem item = fetchedItem.source();
        Client client = registerClient(item.clientNumber(), item.clientName());
        Box box = registerBox(item.boxId(), item.boxDescription());
        CaseFile caseFile = caseFileDAO.saveOrGetExisting(item.caseReference(), client, box);

        final BarcodeHandlingResult handlingResult;
        try {
            handlingResult = splitIntoDocuments(item.itemId(), fetchedItem.pages(), session, barcodeBehavior, barcodePageBehavior);
        } catch (IllegalArgumentException exception) {
            session.recordFailure(exception.getMessage());
            scanSessionDAO.recordFailure(session, exception.getMessage());
            return ScanImportResult.failed(exception.getMessage());
        }
        List<Document> storedDocuments = new ArrayList<>();
        for (Document document : handlingResult.documents()) {
            Document storedDocument = documentDAO.saveOrGetExisting(document, caseFile.getId());
            caseFile.addDocument(storedDocument);
            session.addImportedDocument(storedDocument);
            scanSessionDAO.linkDocument(session, storedDocument);
            storedDocuments.add(storedDocument);
        }

        session.setLastStatus(handlingResult.stoppedOnBarcode() ? "STOPPED_ON_BARCODE" : "IMPORTED");
        scanSessionDAO.updateSessionState(session);
        if (handlingResult.stoppedOnBarcode()) {
            return ScanImportResult.stoppedOnBarcode(storedDocuments, handlingResult.message());
        }
        return ScanImportResult.imported(storedDocuments);
    }

    public List<Document> importAllAvailable(ScanSession session) {
        Objects.requireNonNull(session, "session");
        List<Document> imported = new ArrayList<>();
        while (true) {
            int failuresBefore = session.getFailures().size();
            ScanImportResult result = scanNextItem(session, session.getSelectedBarcodeBehavior(), "Remove barcode page from final document");
            if (!result.getImportedDocuments().isEmpty()) {
                imported.addAll(result.getImportedDocuments());
                if (result.getStatus() == ScanImportResult.Status.STOPPED_ON_BARCODE) {
                    break;
                }
                continue;
            }
            if (session.getFailures().size() > failuresBefore) {
                continue;
            }
            break;
        }
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

    private BarcodeHandlingResult splitIntoDocuments(
            String itemId,
            List<TiffFetchService.FetchedPage> pages,
            ScanSession session,
            String barcodeBehavior,
            String barcodePageBehavior
    ) {
        List<Document> documents = new ArrayList<>();
        List<PageImage> currentPages = new ArrayList<>();
        int documentIndex = 1;
        boolean stopOnBarcode = barcodeBehavior != null && barcodeBehavior.toLowerCase().contains("stop");

        for (TiffFetchService.FetchedPage page : pages) {
            PageImage.PageType pageType = barcodeSplitService.classify(page.sourceReference());
            PageImage pageImage = new PageImage(page.pageNumber(), pageType, page.sourceReference());
            pageImage.setReferenceId(session.allocateReferenceId());
            pageImage.setDisplayContent(page.displayContent());

            if (pageType == PageImage.PageType.BARCODE) {
                if (page.barcodeValue() == null || page.barcodeValue().isBlank()) {
                    throw new IllegalArgumentException("Unreadable barcode result.");
                }

                if ("Keep barcode page in final document".equalsIgnoreCase(barcodePageBehavior)) {
                    currentPages.add(pageImage);
                } else if ("Move barcode page to separate document".equalsIgnoreCase(barcodePageBehavior)) {
                    documents.add(new Document(itemId + "-barcode-" + documentIndex++, List.of(pageImage)));
                }

                if (!currentPages.isEmpty()) {
                    documents.add(new Document(itemId + "-" + documentIndex++, currentPages));
                    currentPages = new ArrayList<>();
                }

                if (stopOnBarcode) {
                    return new BarcodeHandlingResult(documents, true, "Scanning stopped because a barcode was detected.");
                }
                continue;
            }

            currentPages.add(pageImage);
        }

        if (!currentPages.isEmpty()) {
            documents.add(new Document(itemId + "-" + documentIndex, currentPages));
        }

        return new BarcodeHandlingResult(documents, false, "");
    }

    private record BarcodeHandlingResult(List<Document> documents, boolean stoppedOnBarcode, String message) {
    }
}
