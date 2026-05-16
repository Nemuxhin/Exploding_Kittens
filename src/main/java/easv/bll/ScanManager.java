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

    public ScanSession startSession(String boxId, String profileName) {
        ScanSession session = new ScanSession(registerBox(boxId, "Scanned box"), profileName);
        scanSessionDAO.save(session);
        return session;
    }

    public Optional<Document> importNextItem(ScanSession session) {
        ScanImportResult result = scanNextItem(
                session,
                session.getSelectedBarcodeBehavior(),
                "Keep barcode page in final document",
                true
        );
        if (!result.getImportedDocuments().isEmpty()) {
            return Optional.of(result.getImportedDocuments().get(0));
        }
        return Optional.empty();
    }

    public ScanImportResult scanNextItem(
            ScanSession session,
            String barcodeBehavior,
            String barcodePageBehavior,
            boolean barcodeSplittingEnabled
    ) {
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
        NormalizedItem item = normalizeImportedItem(session, fetchedItem.source());
        Client client = registerClient(item.clientNumber(), item.clientName());
        Box box = session.getBox();
        CaseFile caseFile = caseFileDAO.saveOrGetExisting(item.caseReference(), client, box);

        final BarcodeHandlingResult handlingResult;
        try {
            handlingResult = splitIntoDocuments(
                    item.itemId(),
                    fetchedItem.pages(),
                    session,
                    barcodeBehavior,
                    barcodePageBehavior,
                    barcodeSplittingEnabled
            );
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
            return ScanImportResult.stoppedOnBarcode(storedDocuments, handlingResult.scannedPages(), handlingResult.message());
        }
        return ScanImportResult.imported(storedDocuments, handlingResult.scannedPages());
    }

    public List<Document> importAllAvailable(ScanSession session) {
        Objects.requireNonNull(session, "session");
        List<Document> imported = new ArrayList<>();
        while (true) {
            int failuresBefore = session.getFailures().size();
            ScanImportResult result = scanNextItem(
                    session,
                    session.getSelectedBarcodeBehavior(),
                    "Remove barcode page from final document",
                    true
            );
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
            String barcodePageBehavior,
            boolean barcodeSplittingEnabled
    ) {
        List<Document> documents = new ArrayList<>();
        List<PageImage> scannedPages = new ArrayList<>();
        List<PageImage> currentPages = new ArrayList<>();
        int documentIndex = 1;
        boolean stopOnBarcode = barcodeBehavior != null && barcodeBehavior.toLowerCase().contains("stop");

        for (TiffFetchService.FetchedPage page : pages) {
            BarcodeSplitService.DetectionResult detectionResult = barcodeSplittingEnabled
                    ? barcodeSplitService.classify(
                    page.sourceReference(),
                    page.barcodeValue(),
                    page.fileData()
            )
                    : new BarcodeSplitService.DetectionResult(PageImage.PageType.TIFF, "");
            PageImage.PageType pageType = detectionResult.pageType();
            PageImage pageImage = new PageImage(page.pageNumber(), pageType, page.sourceReference());
            pageImage.setReferenceId(session.allocateReferenceId());
            pageImage.setDisplayContent(page.displayContent());
            pageImage.setPreviewSourceBytes(page.fileData());
            scannedPages.add(pageImage);

            if (pageType == PageImage.PageType.BARCODE) {
                if (detectionResult.barcodeValue().isBlank()) {
                    throw new IllegalArgumentException("Unreadable barcode result.");
                }

                if (!currentPages.isEmpty()) {
                    documents.add(new Document(itemId + "-" + documentIndex++, currentPages));
                    currentPages = new ArrayList<>();
                }

                if ("Keep barcode page in final document".equalsIgnoreCase(barcodePageBehavior)) {
                    currentPages.add(pageImage);
                } else if ("Move barcode page to separate document".equalsIgnoreCase(barcodePageBehavior)) {
                    documents.add(new Document(itemId + "-barcode-" + documentIndex++, List.of(pageImage)));
                }

                if (stopOnBarcode) {
                    if (!currentPages.isEmpty()) {
                        documents.add(new Document(itemId + "-" + documentIndex++, currentPages));
                        currentPages = new ArrayList<>();
                    }
                    return new BarcodeHandlingResult(documents, scannedPages, true, "Scanning stopped because a barcode was detected.");
                }
                continue;
            }

            currentPages.add(pageImage);
        }

        if (!currentPages.isEmpty()) {
            documents.add(new Document(itemId + "-" + documentIndex, currentPages));
        }

        return new BarcodeHandlingResult(documents, scannedPages, false, "");
    }

    private NormalizedItem normalizeImportedItem(ScanSession session, ScannerApiClient.ApiTiffItem item) {
        int importedItemNumber = session.allocateImportedItemNumber();
        String itemKey = String.format("%s-item-%04d", shortSessionKey(session), importedItemNumber);

        String itemId = isGeneratedItemId(item.itemId())
                ? itemKey
                : item.itemId();

        String caseReference = isGeneratedCaseReference(item.caseReference())
                ? session.getBox().getBoxId() + "-CASE-" + String.format("%04d", importedItemNumber)
                : item.caseReference();

        String clientNumber = isGeneratedClientNumber(item.clientNumber())
                ? session.getBox().getBoxId() + "-CLIENT"
                : item.clientNumber();

        String clientName = isGeneratedClientName(item.clientName())
                ? "Scanned Import"
                : item.clientName();

        return new NormalizedItem(itemId, caseReference, clientNumber, clientName);
    }

    private String shortSessionKey(ScanSession session) {
        String id = session.getId().toString().replace("-", "");
        return id.substring(0, Math.min(12, id.length()));
    }

    private boolean isGeneratedItemId(String itemId) {
        return itemId == null || itemId.isBlank() || itemId.startsWith("api-item-");
    }

    private boolean isGeneratedCaseReference(String caseReference) {
        return caseReference == null || caseReference.isBlank() || caseReference.startsWith("CASE-");
    }

    private boolean isGeneratedClientNumber(String clientNumber) {
        return clientNumber == null || clientNumber.isBlank() || clientNumber.startsWith("CLIENT-");
    }

    private boolean isGeneratedClientName(String clientName) {
        return clientName == null || clientName.isBlank() || "Imported Client".equalsIgnoreCase(clientName.trim());
    }

    private record BarcodeHandlingResult(List<Document> documents, List<PageImage> scannedPages, boolean stoppedOnBarcode, String message) {
    }

    private record NormalizedItem(String itemId, String caseReference, String clientNumber, String clientName) {
    }
}
