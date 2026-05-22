package easv.bll;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Queue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ScannerApiClient {
    private static final String DEFAULT_BASE_URL = "https://studentiffapi-production.up.railway.app";
    private static final String DEFAULT_FETCH_MODE = "paged";
    private static final int DEFAULT_PAGE_SIZE = 1;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final String DATABASE_PROPERTIES_FILE = "database.properties";

    private final Queue<Object> queuedResponses = new ArrayDeque<>();
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String fetchMode;
    private final int pageSize;
    private int offset;

    public ScannerApiClient() {
        this(HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build(), new ObjectMapper());
    }

    ScannerApiClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");

        Properties properties = loadConfiguration();
        this.baseUrl = readConfiguredValue(properties,
                "SCANNER_API_BASE_URL", "scanner.api.base-url", DEFAULT_BASE_URL);
        this.fetchMode = readConfiguredValue(properties,
                "SCANNER_API_FETCH_MODE", "scanner.api.fetch-mode", DEFAULT_FETCH_MODE).trim().toLowerCase();
        this.pageSize = parsePositiveInt(readConfiguredValue(properties,
                "SCANNER_API_PAGE_SIZE", "scanner.api.page-size", String.valueOf(DEFAULT_PAGE_SIZE)), DEFAULT_PAGE_SIZE);
        this.offset = parsePositiveInt(readConfiguredValue(properties,
                "SCANNER_API_OFFSET", "scanner.api.offset", "0"), 0);
    }

    public void queueItem(ApiTiffItem item) {
        queuedResponses.add(Objects.requireNonNull(item, "item"));
    }

    public void queueFailure(String message) {
        queuedResponses.add(new ApiFailure(message));
    }

    public synchronized Optional<ApiTiffItem> fetchNextItem() {
        if (!queuedResponses.isEmpty()) {
            Object next = queuedResponses.remove();
            if (next instanceof ApiFailure failure) {
                throw new ScannerApiException(failure.message());
            }
            return Optional.of((ApiTiffItem) next);
        }

        ApiResponse response = fetchFromApi();
        if (response.body() == null || response.body().length == 0) {
            return Optional.empty();
        }

        String contentType = normalizeContentType(response.contentType());
        try {
            Optional<ApiTiffItem> item = contentType.contains("json")
                    ? parseJsonResponse(response.body())
                    : parseBinaryResponse(response.body(), contentType, response.contentDisposition());

            item.ifPresent(ignored -> advanceOffset());
            return item;
        } catch (IOException exception) {
            throw new ScannerApiException("Failed to parse TIFF API response: " + exception.getMessage(), exception);
        }
    }

    private ApiResponse fetchFromApi() {
        String path = buildFetchPath();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "*/*")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            byte[] body = response.body();

            if (status == 404 && looksLikeEmptyResult(body)) {
                return new ApiResponse(status, headerValue(response, "content-type"), headerValue(response, "content-disposition"), new byte[0]);
            }

            if (status < 200 || status >= 300) {
                throw new ScannerApiException("TIFF API request failed with status " + status + formatApiMessage(body));
            }

            return new ApiResponse(
                    status,
                    headerValue(response, "content-type"),
                    headerValue(response, "content-disposition"),
                    body
            );
        } catch (IOException exception) {
            throw new ScannerApiException("Failed to reach TIFF API: " + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ScannerApiException("TIFF API request was interrupted", exception);
        }
    }

    private Optional<ApiTiffItem> parseBinaryResponse(byte[] body, String contentType, String contentDisposition) {
        if (body.length == 0) {
            return Optional.empty();
        }

        if (isZipResponse(body, contentType, contentDisposition)) {
            return Optional.of(buildItemFromZip(body));
        }

        String sourceReference = "api-file-" + (offset + 1) + ".tiff";
        ApiTiffPage page = new ApiTiffPage(1, sourceReference, contentType.isBlank() ? "image/tiff" : contentType, body, "");
        return Optional.of(new ApiTiffItem(
                "api-item-" + (offset + 1),
                "CASE-" + (offset + 1),
                "CLIENT-" + (offset + 1),
                "Imported Client",
                "BOX-" + (offset + 1),
                "Imported Box",
                List.of(page)
        ));
    }

    private ApiTiffItem buildItemFromZip(byte[] body) {
        List<ApiTiffPage> pages = new ArrayList<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(new java.io.ByteArrayInputStream(body))) {
            ZipEntry entry;
            int pageNumber = 1;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String entryName = entry.getName() == null ? "" : entry.getName().trim();
                if (!isTiffFileName(entryName)) {
                    continue;
                }

                byte[] entryBytes = zipInputStream.readAllBytes();
                if (entryBytes.length == 0) {
                    continue;
                }

                pages.add(new ApiTiffPage(
                        pageNumber++,
                        entryName,
                        "image/tiff",
                        entryBytes,
                        inferBarcodeValue(entryName)
                ));
            }
        } catch (IOException exception) {
            throw new ScannerApiException("Failed to unpack TIFF ZIP response: " + exception.getMessage(), exception);
        }

        if (pages.isEmpty()) {
            throw new ScannerApiException("Invalid API response: ZIP did not contain TIFF files");
        }

        return new ApiTiffItem(
                "api-item-" + (offset + 1),
                "CASE-" + (offset + 1),
                "CLIENT-" + (offset + 1),
                "Imported Client",
                "BOX-" + (offset + 1),
                "Imported Box",
                pages
        );
    }

    private Optional<ApiTiffItem> parseJsonResponse(byte[] body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        if (root == null || root.isNull()) {
            return Optional.empty();
        }

        if (root.isArray()) {
            if (root.isEmpty()) {
                return Optional.empty();
            }

            JsonNode first = root.get(0);
            if (looksLikePageNode(first)) {
                return Optional.of(buildItemFromPages(root));
            }
            return Optional.of(buildItemFromItemNode(first));
        }

        JsonNode dataNode = firstPresent(root,
                "data", "content", "result", "file", "files", "item", "items", "page", "pages");
        if (dataNode != null) {
            if (dataNode.isArray()) {
                if (dataNode.isEmpty()) {
                    return Optional.empty();
                }
                JsonNode first = dataNode.get(0);
                if (looksLikePageNode(first)) {
                    return Optional.of(buildItemFromPages(dataNode));
                }
                return Optional.of(buildItemFromItemNode(first));
            }
            if (looksLikePageNode(dataNode)) {
                return Optional.of(buildItemFromPages(singletonArray(dataNode)));
            }
            return Optional.of(buildItemFromItemNode(dataNode));
        }

        if (looksLikePageNode(root)) {
            return Optional.of(buildItemFromPages(singletonArray(root)));
        }

        return Optional.of(buildItemFromItemNode(root));
    }

    private ApiTiffItem buildItemFromItemNode(JsonNode node) {
        JsonNode pagesNode = firstPresent(node, "pages", "files", "tiffs", "items", "documents");
        List<ApiTiffPage> pages;

        if (pagesNode != null && pagesNode.isArray() && !pagesNode.isEmpty()) {
            pages = buildPages(pagesNode);
        } else if (looksLikePageNode(node)) {
            pages = buildPages(singletonArray(node));
        } else {
            throw new ScannerApiException("Invalid API response: missing page data");
        }

        String itemId = text(node, "itemId", "id", "fileId", "documentId", "sourceItemId");
        if (itemId.isBlank()) {
            itemId = "api-item-" + (offset + 1);
        }

        return new ApiTiffItem(
                itemId,
                fallback(text(node, "caseReference", "caseId", "caseNumber"), "CASE-" + (offset + 1)),
                fallback(text(node, "clientNumber", "clientId"), "CLIENT-" + (offset + 1)),
                fallback(text(node, "clientName", "client"), "Imported Client"),
                fallback(text(node, "boxId", "boxNumber"), "BOX-" + (offset + 1)),
                fallback(text(node, "boxDescription", "boxName", "description"), "Imported Box"),
                pages
        );
    }

    private ApiTiffItem buildItemFromPages(JsonNode pagesNode) {
        return new ApiTiffItem(
                "api-item-" + (offset + 1),
                "CASE-" + (offset + 1),
                "CLIENT-" + (offset + 1),
                "Imported Client",
                "BOX-" + (offset + 1),
                "Imported Box",
                buildPages(pagesNode)
        );
    }

    private List<ApiTiffPage> buildPages(JsonNode pagesNode) {
        List<ApiTiffPage> pages = new ArrayList<>();
        int pageNumber = 1;
        for (JsonNode pageNode : pagesNode) {
            byte[] bytes = extractBytes(pageNode);
            String sourceReference = fallback(text(pageNode, "sourceReference", "fileName", "name", "id"),
                    "api-file-" + (offset + 1) + "-" + pageNumber + ".tiff");
            String contentType = fallback(text(pageNode, "contentType", "mimeType", "type"), "image/tiff");
            String barcodeValue = text(pageNode, "barcodeValue", "barcode", "separator");

            pages.add(new ApiTiffPage(
                    intValue(pageNode, pageNumber, "pageNumber", "page", "index"),
                    sourceReference,
                    contentType,
                    bytes,
                    barcodeValue
            ));
            pageNumber++;
        }

        if (pages.isEmpty()) {
            throw new ScannerApiException("Invalid API response: empty response");
        }
        return pages;
    }

    private byte[] extractBytes(JsonNode pageNode) {
        JsonNode bytesNode = firstPresent(pageNode, "fileData", "data", "content", "bytes", "base64", "file");
        if (bytesNode == null || bytesNode.isNull()) {
            throw new ScannerApiException("Invalid API response: missing file data");
        }

        if (bytesNode.isBinary()) {
            try {
                return bytesNode.binaryValue();
            } catch (IOException exception) {
                throw new ScannerApiException("Invalid API response: corrupted file", exception);
            }
        }

        if (bytesNode.isTextual()) {
            String value = bytesNode.asText().trim();
            if (value.isEmpty()) {
                throw new ScannerApiException("Invalid API response: missing file data");
            }

            int dataUrlSeparator = value.indexOf("base64,");
            if (dataUrlSeparator >= 0) {
                value = value.substring(dataUrlSeparator + 7);
            }

            try {
                return Base64.getDecoder().decode(value);
            } catch (IllegalArgumentException exception) {
                return value.getBytes(StandardCharsets.ISO_8859_1);
            }
        }

        if (bytesNode.isArray()) {
            byte[] bytes = new byte[bytesNode.size()];
            for (int index = 0; index < bytesNode.size(); index++) {
                bytes[index] = (byte) bytesNode.get(index).asInt();
            }
            return bytes;
        }

        throw new ScannerApiException("Invalid API response: missing file data");
    }

    private String buildFetchPath() {
        return switch (fetchMode) {
            case "random" -> "/getRandomFile";
            case "all" -> "/getAllFiles";
            default -> "/getFiles/" + offset + "/" + pageSize;
        };
    }

    private void advanceOffset() {
        if (!"random".equals(fetchMode)) {
            offset += pageSize;
        }
    }

    private boolean looksLikePageNode(JsonNode node) {
        return node != null && firstPresent(node, "fileData", "data", "content", "bytes", "base64", "contentType", "mimeType") != null;
    }

    private boolean looksLikeEmptyResult(byte[] body) {
        if (body == null || body.length == 0) {
            return true;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            String message = text(root, "message");
            return "Application not found".equalsIgnoreCase(message) ? false : root.isArray() && root.isEmpty();
        } catch (IOException exception) {
            return false;
        }
    }

    private String formatApiMessage(byte[] body) {
        if (body == null || body.length == 0) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            String message = text(root, "message", "error", "detail");
            return message.isBlank() ? "" : ": " + message;
        } catch (IOException exception) {
            return "";
        }
    }

    private JsonNode firstPresent(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode child = node.get(fieldName);
            if (child != null && !child.isNull()) {
                return child;
            }
        }
        return null;
    }

    private JsonNode singletonArray(JsonNode node) {
        return objectMapper.createArrayNode().add(node);
    }

    private String text(JsonNode node, String... fieldNames) {
        JsonNode child = firstPresent(node, fieldNames);
        return child == null ? "" : child.asText("").trim();
    }

    private int intValue(JsonNode node, int fallback, String... fieldNames) {
        JsonNode child = firstPresent(node, fieldNames);
        return child == null ? fallback : child.asInt(fallback);
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String headerValue(HttpResponse<?> response, String name) {
        return response.headers().firstValue(name).orElse("");
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int separator = contentType.indexOf(';');
        return (separator >= 0 ? contentType.substring(0, separator) : contentType).trim().toLowerCase();
    }

    private boolean isZipResponse(byte[] body, String contentType, String contentDisposition) {
        if (contentType.contains("zip")) {
            return true;
        }
        if (contentDisposition != null && contentDisposition.toLowerCase().contains(".zip")) {
            return true;
        }
        return body.length >= 4
                && body[0] == 0x50
                && body[1] == 0x4B
                && (body[2] == 0x03 || body[2] == 0x05 || body[2] == 0x07)
                && (body[3] == 0x04 || body[3] == 0x06 || body[3] == 0x08);
    }

    private boolean isTiffFileName(String name) {
        String normalized = name == null ? "" : name.toLowerCase();
        return normalized.endsWith(".tif") || normalized.endsWith(".tiff");
    }

    private static Properties loadConfiguration() {
        Properties properties = new Properties();
        Path propertiesPath = Path.of(DATABASE_PROPERTIES_FILE);
        if (Files.exists(propertiesPath)) {
            try (InputStream inputStream = Files.newInputStream(propertiesPath)) {
                properties.load(inputStream);
            } catch (IOException ignored) {
            }
        }

        try (InputStream inputStream = ScannerApiClient.class.getClassLoader().getResourceAsStream(DATABASE_PROPERTIES_FILE)) {
            if (inputStream == null) {
                return properties;
            }
            Properties classpathProperties = new Properties();
            classpathProperties.load(inputStream);
            classpathProperties.forEach((key, value) ->
                    properties.putIfAbsent(String.valueOf(key), String.valueOf(value)));
        } catch (IOException ignored) {
        }
        return properties;
    }

    private static String readConfiguredValue(Properties properties, String envName, String propertyName, String fallback) {
        String systemValue = System.getProperty(propertyName);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }

        String propertyValue = properties.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return fallback;
    }

    private static int parsePositiveInt(String value, int fallback) {
        try {
            return Math.max(0, Integer.parseInt(value.trim()));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private record ApiFailure(String message) {}

    private record ApiResponse(int statusCode, String contentType, String contentDisposition, byte[] body) {}

    public record ApiTiffItem(
            String itemId,
            String caseReference,
            String clientNumber,
            String clientName,
            String boxId,
            String boxDescription,
            List<ApiTiffPage> pages
    ) {
        public ApiTiffItem {
            requireText(itemId, "itemId");
            requireText(caseReference, "caseReference");
            requireText(clientNumber, "clientNumber");
            requireText(clientName, "clientName");
            requireText(boxId, "boxId");
            requireText(boxDescription, "boxDescription");
            pages = List.copyOf(Objects.requireNonNull(pages, "pages"));
            if (pages.isEmpty()) {
                throw new IllegalArgumentException("pages must not be empty");
            }
        }
    }

    public record ApiTiffPage(
            int pageNumber,
            String sourceReference,
            String contentType,
            byte[] fileData,
            String barcodeValue
    ) {
        private static final byte[] DEFAULT_TIFF_BYTES = new byte[] {0x49, 0x49, 0x2A, 0x00, 0x08, 0x00, 0x00, 0x00};

        public ApiTiffPage(int pageNumber, String sourceReference) {
            this(
                    pageNumber,
                    sourceReference,
                    inferContentType(sourceReference),
                    DEFAULT_TIFF_BYTES,
                    inferBarcodeValue(sourceReference)
            );
        }

        public ApiTiffPage {
            if (pageNumber < 1) {
                throw new IllegalArgumentException("pageNumber must be positive");
            }
            requireText(sourceReference, "sourceReference");
            contentType = contentType == null ? "" : contentType.trim();
            fileData = fileData == null ? null : Arrays.copyOf(fileData, fileData.length);
            barcodeValue = barcodeValue == null ? "" : barcodeValue.trim();
        }
    }

    public static class ScannerApiException extends RuntimeException {
        public ScannerApiException(String message) {
            super(message);
        }

        public ScannerApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static String inferContentType(String sourceReference) {
        return sourceReference != null && sourceReference.toLowerCase().endsWith(".pdf")
                ? "application/pdf"
                : "image/tiff";
    }

    private static String inferBarcodeValue(String sourceReference) {
        if (sourceReference == null) {
            return "";
        }
        String normalized = sourceReference.toLowerCase();
        if (normalized.contains("barcode") || normalized.contains("separator") || normalized.startsWith("bc_")) {
            return "BARCODE:" + sourceReference;
        }
        return "";
    }
}
