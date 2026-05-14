package easv.bll;

import java.util.Base64;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;

public class TiffFetchService {
    private static final String TIFF_HEADER_II = "49492A00";
    private static final String TIFF_HEADER_MM = "4D4D002A";
    private static final int PREFETCH_QUEUE_SIZE = 6;
    private static final int PREVIEW_TARGET_MAX_DIMENSION = 640;
    private static final int BARCODE_WORKING_TARGET_MAX_DIMENSION = 960;

    private final ScannerApiClient scannerApiClient;
    private final Deque<CompletableFuture<FetchResult>> prefetchedResults = new ArrayDeque<>();

    public TiffFetchService(ScannerApiClient scannerApiClient) {
        this.scannerApiClient = scannerApiClient;
    }

    public synchronized void prefetchNextItem() {
        while (prefetchedResults.size() < PREFETCH_QUEUE_SIZE) {
            prefetchedResults.addLast(CompletableFuture.supplyAsync(this::fetchNextItemDirect));
        }
    }

    public FetchResult fetchNextItem() {
        CompletableFuture<FetchResult> future;
        synchronized (this) {
            future = prefetchedResults.pollFirst();
        }

        FetchResult result = future == null ? fetchNextItemDirect() : future.join();
        if (!result.noMoreItems()) {
            prefetchNextItem();
        }
        return result;
    }

    private FetchResult fetchNextItemDirect() {
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

        PreparedPageContent preparedPageContent = createPreparedPageContent(page.fileData(), page.pageNumber());
        return new FetchedPage(
                page.pageNumber(),
                page.sourceReference(),
                page.fileData(),
                page.barcodeValue(),
                preparedPageContent.previewContent(),
                preparedPageContent.barcodeWorkingData()
        );
    }

    private PreparedPageContent createPreparedPageContent(byte[] fileData, int pageNumber) {
        String tiffContent = "data:image/tiff;base64," + Base64.getEncoder().encodeToString(fileData);
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(fileData))) {
            if (imageInputStream == null) {
                return new PreparedPageContent(tiffContent, fileData);
            }

            var readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                return new PreparedPageContent(tiffContent, fileData);
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream, true, true);
                int imageIndex = Math.max(0, Math.min(Math.max(0, reader.getNumImages(true) - 1), Math.max(0, pageNumber - 1)));
                int width = reader.getWidth(imageIndex);
                int height = reader.getHeight(imageIndex);

                ImageReadParam param = reader.getDefaultReadParam();
                int subsampling = Math.max(
                        1,
                        (int) Math.ceil((double) Math.max(width, height) / (double) BARCODE_WORKING_TARGET_MAX_DIMENSION)
                );
                if (subsampling > 1) {
                    param.setSourceSubsampling(subsampling, subsampling, 0, 0);
                }

                BufferedImage image = reader.read(imageIndex, param);
                if (image == null) {
                    return new PreparedPageContent(tiffContent, fileData);
                }

                byte[] barcodeWorkingData = encodePng(image);
                BufferedImage previewImage = scaleDownIfNeeded(image, PREVIEW_TARGET_MAX_DIMENSION);
                byte[] previewBytes = previewImage == image ? barcodeWorkingData : encodePng(previewImage);
                ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
                pngBytes.write(previewBytes);
                return new PreparedPageContent(
                        "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes.toByteArray()),
                        barcodeWorkingData
                );
            } finally {
                reader.dispose();
            }
        } catch (Exception exception) {
            return new PreparedPageContent(tiffContent, fileData);
        }
    }

    private byte[] encodePng(BufferedImage image) throws Exception {
        ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", pngBytes);
        return pngBytes.toByteArray();
    }

    private BufferedImage scaleDownIfNeeded(BufferedImage image, int maxDimension) {
        int width = image.getWidth();
        int height = image.getHeight();
        int currentMaxDimension = Math.max(width, height);
        if (currentMaxDimension <= maxDimension) {
            return image;
        }

        double ratio = (double) maxDimension / (double) currentMaxDimension;
        int scaledWidth = Math.max(1, (int) Math.round(width * ratio));
        int scaledHeight = Math.max(1, (int) Math.round(height * ratio));
        BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        var graphics = scaled.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
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

    public record FetchedPage(
            int pageNumber,
            String sourceReference,
            byte[] fileData,
            String barcodeValue,
            String previewContent,
            byte[] barcodeWorkingData
    ) {
        public String displayContent() {
            return "data:image/tiff;base64," + Base64.getEncoder().encodeToString(fileData);
        }
    }

    private record PreparedPageContent(String previewContent, byte[] barcodeWorkingData) {
    }
}
