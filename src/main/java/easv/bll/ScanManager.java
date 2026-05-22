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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ScanManager {
    private final DatabaseConnection databaseConnection;
    private final ScannerApiClient scannerApiClient;
    private final TiffFetchService tiffFetchService;
    private final BarcodeSplitService barcodeSplitService;
    private final ClientDAO clientDAO;
    private final BoxDAO boxDAO;
    private final CaseFileDAO caseFileDAO;
    private final DocumentDAO documentDAO;
    private final ScanSessionDAO scanSessionDAO;

    public ScanManager() {
        this.databaseConnection = new DatabaseConnection();
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
        this.databaseConnection = new DatabaseConnection();
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
        tiffFetchService.prefetchNextItem();
        return session;
    }

    public Optional<ResumedSession> resumeLatestSession(String boxId, String profileName) {
        Objects.requireNonNull(boxId, "boxId");
        Objects.requireNonNull(profileName, "profileName");

        Optional<ScanSessionDAO.StoredScanSession> storedSession = scanSessionDAO.findLatestSession(boxId, profileName);
        if (storedSession.isEmpty()) {
            return Optional.empty();
        }

        Box box = boxDAO.findByBoxId(boxId)
                .orElseGet(() -> registerBox(boxId, "Scanned box"));
        ScanSessionDAO.StoredScanSession stored = storedSession.get();

        ScanSession resumedSession = new ScanSession(
                stored.sessionId(),
                stored.startedAt(),
                box,
                stored.profileName()
        );
        resumedSession.setSelectedBarcodeBehavior(stored.selectedBarcodeBehavior());
        resumedSession.setLastStatus(stored.lastStatus());

        List<Document> linkedDocuments = new ArrayList<>(documentDAO.findBySessionId(stored.sessionId()));
        int nextReferenceId = 1;
        for (Document document : linkedDocuments) {
            resumedSession.addImportedDocument(document);
            for (PageImage page : document.getPages()) {
                nextReferenceId = Math.max(nextReferenceId, page.getReferenceId() + 1);
            }
        }
        resumedSession.seedNextReferenceId(nextReferenceId);
        resumedSession.seedNextImportedItemNumber(Math.max(1, linkedDocuments.size() + 1));

        return Optional.of(new ResumedSession(resumedSession, linkedDocuments));
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
        long totalStartNs = System.nanoTime();
        long fetchStartNs = totalStartNs;

        TiffFetchService.FetchResult fetchResult = tiffFetchService.fetchNextItem();
        long fetchMs = elapsedMs(fetchStartNs);
        if (fetchResult.noMoreItems()) {
            session.setLastStatus("NO_MORE_FILES");
            scanSessionDAO.updateSessionState(session);
            logScanPerf("NO_MORE_FILES", fetchMs, 0, 0, 0, 0, 0, 0, 0, elapsedMs(totalStartNs));
            return ScanImportResult.noMoreFiles();
        }

        if (fetchResult.isFailure()) {
            session.recordFailure(fetchResult.message());
            scanSessionDAO.recordFailure(session, fetchResult.message());
            logScanPerf("FETCH_FAILED", fetchMs, 0, 0, 0, 0, 0, 0, 0, elapsedMs(totalStartNs));
            return ScanImportResult.failed(fetchResult.message());
        }

        TiffFetchService.FetchedItem fetchedItem = fetchResult.item();
        int fetchedPageCount = fetchedItem.pages().size();
        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long normalizeStartNs = System.nanoTime();
                NormalizedItem item = normalizeImportedItem(session, fetchedItem.source());
                Client client = clientDAO.saveOrGetExisting(connection, item.clientNumber(), item.clientName());
                Box box = session.getBox();
                CaseFile caseFile = caseFileDAO.saveOrGetExisting(connection, item.caseReference(), client, box);
                long normalizeMs = elapsedMs(normalizeStartNs);
                long splitMs;

                final BarcodeHandlingResult handlingResult;
                try {
                    long splitStartNs = System.nanoTime();
                    handlingResult = splitIntoDocuments(item.itemId(), fetchedItem.pages(), session, barcodeBehavior, barcodePageBehavior);
                    splitMs = elapsedMs(splitStartNs);
                } catch (IllegalArgumentException exception) {
                    connection.rollback();
                    session.recordFailure(exception.getMessage());
                    scanSessionDAO.recordFailure(session, exception.getMessage());
                    logScanPerf("BARCODE_FAILED", fetchMs, normalizeMs, 0, 0, fetchedPageCount, 0, 0, 0, elapsedMs(totalStartNs));
                    return ScanImportResult.failed(exception.getMessage());
                }

                long persistStartNs = System.nanoTime();
                List<Document> storedDocuments = new ArrayList<>();
                for (Document document : handlingResult.documents()) {
                    Document storedDocument = documentDAO.saveOrGetExisting(connection, document, caseFile.getId());
                    caseFile.addDocument(storedDocument);
                    session.addImportedDocument(storedDocument);
                    scanSessionDAO.linkDocument(connection, session, storedDocument);
                    storedDocuments.add(storedDocument);
                }

                session.setLastStatus(handlingResult.stoppedOnBarcode() ? "STOPPED_ON_BARCODE" : "IMPORTED");
                scanSessionDAO.updateSessionState(connection, session);
                connection.commit();
                session.replacePendingPages(handlingResult.pendingPages());
                long persistMs = elapsedMs(persistStartNs);
                long totalMs = elapsedMs(totalStartNs);
                int barcodePages = countBarcodePages(handlingResult.scannedPages());
                logScanPerf(
                        handlingResult.stoppedOnBarcode() ? "STOPPED_ON_BARCODE" : "IMPORTED",
                        fetchMs,
                        normalizeMs,
                        splitMs,
                        persistMs,
                        fetchedPageCount,
                        handlingResult.scannedPages().size(),
                        handlingResult.documents().size(),
                        barcodePages,
                        totalMs
                );

                if (handlingResult.stoppedOnBarcode()) {
                    return ScanImportResult.stoppedOnBarcode(storedDocuments, handlingResult.scannedPages(), handlingResult.message());
                }
                return ScanImportResult.imported(storedDocuments, handlingResult.scannedPages());
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to persist scan item.", exception);
        }
    }

    public List<Document> importAllAvailable(ScanSession session) {
        Objects.requireNonNull(session, "session");
        List<Document> imported = new ArrayList<>();
        while (true) {
            int failuresBefore = session.getFailures().size();
            ScanImportResult result = scanNextItem(session, session.getSelectedBarcodeBehavior(), "Keep barcode page in final document");
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
        List<PageImage> scannedPages = new ArrayList<>();
        List<PageImage> currentPages = new ArrayList<>(session.getPendingPages());
        int documentIndex = 1;
        boolean stopOnBarcode = barcodeBehavior != null && barcodeBehavior.toLowerCase().contains("stop");

        for (TiffFetchService.FetchedPage page : pages) {
            BarcodeSplitService.DetectionResult detectionResult = barcodeSplitService.classify(
                    page.sourceReference(),
                    page.barcodeValue(),
                    page.displayContent()
            );
            PageImage.PageType pageType = detectionResult.pageType();
            PageImage pageImage = new PageImage(page.pageNumber(), pageType, page.sourceReference());
            pageImage.setReferenceId(session.allocateReferenceId());
            pageImage.setDisplayContent(page.displayContent());
            pageImage.setPreviewContent(page.previewContent());
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
                    return new BarcodeHandlingResult(documents, scannedPages, currentPages, true, "Scanning stopped because a barcode was detected.");
                }
                continue;
            }

            currentPages.add(pageImage);
        }

        return new BarcodeHandlingResult(documents, scannedPages, currentPages, false, "");
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

    private static int countBarcodePages(List<PageImage> pages) {
        int count = 0;
        for (PageImage page : pages) {
            if (page.getPageType() == PageImage.PageType.BARCODE) {
                count++;
            }
        }
        return count;
    }

    private static long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    private static void logScanPerf(
            String status,
            long fetchMs,
            long normalizeMs,
            long splitMs,
            long persistMs,
            int fetchedPages,
            int scannedPages,
            int documents,
            int barcodePages,
            long totalMs
    ) {
        System.out.println(
                "SCAN_PERF status=" + status
                        + " totalMs=" + totalMs
                        + " fetchMs=" + fetchMs
                        + " normalizeMs=" + normalizeMs
                        + " barcodeSplitMs=" + splitMs
                        + " persistMs=" + persistMs
                        + " fetchedPages=" + fetchedPages
                        + " scannedPages=" + scannedPages
                        + " documents=" + documents
                        + " barcodePages=" + barcodePages
        );
    }

    private record BarcodeHandlingResult(
            List<Document> documents,
            List<PageImage> scannedPages,
            List<PageImage> pendingPages,
            boolean stoppedOnBarcode,
            String message
    ) {
    }

    private record NormalizedItem(String itemId, String caseReference, String clientNumber, String clientName) {
    }

    public record ResumedSession(ScanSession session, List<Document> documents) {
    }
}
