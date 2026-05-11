package easv.bll;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class TiffFetchService {
    private static final String TIFF_HEADER_II = "49492A00";
    private static final String TIFF_HEADER_MM = "4D4D002A";

    private final ScannerApiClient scannerApiClient;

    public TiffFetchService(ScannerApiClient scannerApiClient) {
        this.scannerApiClient = scannerApiClient;
    }

    public FetchResult fetchNextItem() {
        final Optional<ScannerApiClient.ApiTiffItem> fetchedItem;
        try {
            fetchedItem = scannerApiClient.fetchNextItem();
        } catch (ScannerApiClient.ScannerApiException exception) {
            return FetchResult.failure(exception.getMessage());
        }

        if (fetchedItem.isEmpty()) {
            return FetchResult.empty();
        }

        ScannerApiClient.ApiTiffItem item = fetchedItem.get();
        if (item.pages() == null || item.pages().isEmpty()) {
            return FetchResult.failure("Invalid API response: empty response");
        }

        try {
            List<FetchedPage> pages = item.pages().stream()
                    .map(this::validatePage)
                    .toList();
            return FetchResult.success(new FetchedItem(item, pages));
        } catch (IllegalArgumentException exception) {
            return FetchResult.failure(exception.getMessage());
        }
    }

    private FetchedPage validatePage(ScannerApiClient.ApiTiffPage page) {
        if (page.fileData() == null || page.fileData().length == 0) {
            throw new IllegalArgumentException("Invalid API response: missing file data");
        }

        boolean declaredAsTiff = isTiffContentType(page.contentType()) || hasTiffFileName(page.sourceReference());
        if (!declaredAsTiff) {
            throw new IllegalArgumentException("Invalid API response: wrong file type");
        }

        if (!hasValidTiffHeader(page.fileData())) {
            throw new IllegalArgumentException("Invalid API response: corrupted file");
        }

        String displayContent = "data:image/tiff;base64," + Base64.getEncoder().encodeToString(page.fileData());
        return new FetchedPage(page.pageNumber(), page.sourceReference(), displayContent, page.barcodeValue());
    }

    private boolean isTiffContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        String normalized = contentType.trim().toLowerCase();
        return normalized.equals("image/tiff") || normalized.equals("image/x-tiff");
    }

    private boolean hasTiffFileName(String sourceReference) {
        if (sourceReference == null || sourceReference.isBlank()) {
            return false;
        }
        String normalized = sourceReference.trim().toLowerCase();
        return normalized.endsWith(".tif") || normalized.endsWith(".tiff");
    }

    private boolean hasValidTiffHeader(byte[] bytes) {
        if (bytes.length < 4) {
            return false;
        }

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < 4; index++) {
            builder.append(String.format("%02X", bytes[index]));
        }
        String header = builder.toString();
        return TIFF_HEADER_II.equals(header) || TIFF_HEADER_MM.equals(header);
    }

    public record FetchResult(FetchedItem item, String message, boolean noMoreItems) {
        public static FetchResult success(FetchedItem item) {
            return new FetchResult(item, "", false);
        }

        public static FetchResult failure(String message) {
            return new FetchResult(null, message, false);
        }

        public static FetchResult empty() {
            return new FetchResult(null, "", true);
        }

        public boolean isSuccess() {
            return item != null;
        }

        public boolean isFailure() {
            return !noMoreItems && item == null;
        }
    }

    public record FetchedItem(ScannerApiClient.ApiTiffItem source, List<FetchedPage> pages) {}

    public record FetchedPage(int pageNumber, String sourceReference, String displayContent, String barcodeValue) {}
}
