package easv.be;

import java.util.ArrayList;
import java.time.Instant;
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
        this.pages = new ArrayList<>(Objects.requireNonNull(pages, "pages"));
        if (this.pages.stream().noneMatch(page -> !page.isDeleted())) {
            throw new IllegalArgumentException("pages must not be empty");
        }
        reindexActivePages();
    }

    public UUID getId() {
        return id;
    }

    public String getSourceItemId() {
        return sourceItemId;
    }

    public List<PageImage> getPages() {
        return pages.stream()
                .filter(page -> !page.isDeleted())
                .toList();
    }

    public List<PageImage> getDeletedPages() {
        return pages.stream()
                .filter(PageImage::isDeleted)
                .toList();
    }

    public List<PageImage> getAllPages() {
        return List.copyOf(pages);
    }

    public PageImage addPage(PageImage pageImage) {
        return addPage(pageImage, getPages().size() + 1);
    }

    public PageImage addPage(PageImage pageImage, int position) {
        Objects.requireNonNull(pageImage, "pageImage");
        int insertIndex = normalizeInsertPosition(position);
        pageImage.restore();
        pages.add(insertIndex, pageImage);
        reindexActivePages();
        return pageImage;
    }

    public void movePage(UUID pageId, int newPosition) {
        PageImage page = findActivePage(pageId);
        pages.remove(page);
        int insertIndex = normalizeInsertPosition(newPosition);
        pages.add(insertIndex, page);
        reindexActivePages();
    }

    public void deletePage(UUID pageId) {
        PageImage page = findActivePage(pageId);
        pages.remove(page);
        page.markDeleted(Instant.now());
        pages.add(page);
        reindexActivePages();
    }

    public void restorePage(UUID pageId, int position) {
        PageImage page = findAnyPage(pageId);
        page.restore();
        pages.remove(page);
        int insertIndex = normalizeInsertPosition(position);
        pages.add(insertIndex, page);
        reindexActivePages();
    }

    public PageImage extractPage(UUID pageId) {
        PageImage page = findAnyPage(pageId);
        pages.remove(page);
        reindexActivePages();
        return page;
    }

    private int normalizeInsertPosition(int position) {
        List<PageImage> activePages = getPages();
        int normalizedPosition = Math.max(1, Math.min(position, activePages.size() + 1));
        if (normalizedPosition > activePages.size()) {
            return activePages.size();
        }

        PageImage target = activePages.get(normalizedPosition - 1);
        return pages.indexOf(target);
    }

    private PageImage findActivePage(UUID pageId) {
        return getPages().stream()
                .filter(page -> page.getId().equals(pageId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Page could not be found in document."));
    }

    private PageImage findAnyPage(UUID pageId) {
        return pages.stream()
                .filter(page -> page.getId().equals(pageId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Page could not be found in document."));
    }

    private void reindexActivePages() {
        int pageNumber = 1;
        for (PageImage page : pages) {
            page.setPageNumber(pageNumber++);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
