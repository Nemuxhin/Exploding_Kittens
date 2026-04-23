package easv.be;

import java.util.Objects;
import java.util.UUID;

public class PageImage {
    public enum PageType {
        TIFF,
        BARCODE
    }

    private final UUID id;
    private final int pageNumber;
    private final PageType pageType;
    private final String sourceReference;

    public PageImage(int pageNumber, PageType pageType, String sourceReference) {
        this(UUID.randomUUID(), pageNumber, pageType, sourceReference);
    }

    public PageImage(UUID id, int pageNumber, PageType pageType, String sourceReference) {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be positive");
        }
        this.id = Objects.requireNonNull(id, "id");
        this.pageNumber = pageNumber;
        this.pageType = Objects.requireNonNull(pageType, "pageType");
        this.sourceReference = requireText(sourceReference, "sourceReference");
    }

    public UUID getId() {
        return id;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public PageType getPageType() {
        return pageType;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
