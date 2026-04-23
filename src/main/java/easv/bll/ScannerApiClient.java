package easv.bll;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

public class ScannerApiClient {
    private final Queue<Object> queuedResponses = new ArrayDeque<>();

    public void queueItem(ApiTiffItem item) {
        queuedResponses.add(Objects.requireNonNull(item, "item"));
    }

    public void queueFailure(String message) {
        queuedResponses.add(new ApiFailure(message));
    }

    public Optional<ApiTiffItem> fetchNextItem() {
        if (queuedResponses.isEmpty()) {
            return Optional.empty();
        }

        Object next = queuedResponses.remove();
        if (next instanceof ApiFailure failure) {
            throw new ScannerApiException(failure.message());
        }

        return Optional.of((ApiTiffItem) next);
    }

    private record ApiFailure(String message) {
    }

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

    public record ApiTiffPage(int pageNumber, String sourceReference) {
        public ApiTiffPage {
            if (pageNumber < 1) {
                throw new IllegalArgumentException("pageNumber must be positive");
            }
            requireText(sourceReference, "sourceReference");
        }
    }

    public static class ScannerApiException extends RuntimeException {
        public ScannerApiException(String message) {
            super(message);
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
