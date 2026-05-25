package easv.be;

import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public class PageImage {
    public enum PageType {
        TIFF,
        BARCODE
    }

    private final UUID id;
    private int pageNumber;
    private final PageType pageType;
    private final String sourceReference;
    private int referenceId;
    private int rotationDegrees;
    private String displayContent;
    private byte[] previewSourceBytes = new byte[0];
    private Instant deletedAt;

    public PageImage(int pageNumber, PageType pageType, String sourceReference) {
        this(UUID.randomUUID(), pageNumber, pageType, sourceReference, 0, 0, "", null);
    }

    public PageImage(UUID id, int pageNumber, PageType pageType, String sourceReference) {
        this(id, pageNumber, pageType, sourceReference, 0, 0, "", null);
    }

    public PageImage(
            UUID id,
            int pageNumber,
            PageType pageType,
            String sourceReference,
            int referenceId,
            int rotationDegrees,
            String displayContent,
            Instant deletedAt
    ) {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be positive");
        }
        this.id = Objects.requireNonNull(id, "id");
        this.pageNumber = pageNumber;
        this.pageType = Objects.requireNonNull(pageType, "pageType");
        this.sourceReference = requireText(sourceReference, "sourceReference");
        this.referenceId = Math.max(referenceId, 0);
        this.rotationDegrees = normalizeRotation(rotationDegrees);
        this.displayContent = displayContent == null ? "" : displayContent;
        this.deletedAt = deletedAt;
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

    public int getReferenceId() {
        return referenceId;
    }

    public int getRotationDegrees() {
        return rotationDegrees;
    }

    public String getDisplayContent() {
        return displayContent;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public byte[] getPreviewSourceBytes() {
        return Arrays.copyOf(previewSourceBytes, previewSourceBytes.length);
    }

    public String getPreviewContent() {
        if (previewSourceBytes.length == 0) {
            return "";
        }
        return "data:image/tiff;base64," + Base64.getEncoder().encodeToString(previewSourceBytes);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void setPageNumber(int pageNumber) {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be positive");
        }
        this.pageNumber = pageNumber;
    }

    public void setReferenceId(int referenceId) {
        this.referenceId = Math.max(referenceId, 0);
    }

    public void setRotationDegrees(int rotationDegrees) {
        this.rotationDegrees = normalizeRotation(rotationDegrees);
    }

    public void setDisplayContent(String displayContent) {
        this.displayContent = displayContent == null ? "" : displayContent;
    }

    public void setPreviewSourceBytes(byte[] previewSourceBytes) {
        this.previewSourceBytes = previewSourceBytes == null ? new byte[0] : Arrays.copyOf(previewSourceBytes, previewSourceBytes.length);
    }

    public void markDeleted(Instant deletedAt) {
        this.deletedAt = Objects.requireNonNull(deletedAt, "deletedAt");
    }

    public void restore() {
        this.deletedAt = null;
    }

    private static int normalizeRotation(int rotationDegrees) {
        int normalized = rotationDegrees % 360;
        if (normalized < 0) {
            normalized += 360;
        }
        return normalized;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
