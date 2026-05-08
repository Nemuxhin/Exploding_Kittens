package easv.be;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Document {
    private final UUID id;
    private final String sourceItemId;
    private final List<PageImage> pages;

    public Document(String sourceItemId, List<PageImage> pages) {
        this(UUID.randomUUID(), sourceItemId, pages);
    }

    public Document(UUID id, String sourceItemId, List<PageImage> pages) {
        this.id = Objects.requireNonNull(id, "id");
        this.sourceItemId = requireText(sourceItemId, "sourceItemId");
        this.pages = List.copyOf(new ArrayList<>(Objects.requireNonNull(pages, "pages")));
        if (this.pages.isEmpty()) {
            throw new IllegalArgumentException("pages must not be empty");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getSourceItemId() {
        return sourceItemId;
    }

    public List<PageImage> getPages() {
        return pages;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
