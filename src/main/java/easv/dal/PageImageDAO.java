package easv.dal;

import easv.be.PageImage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PageImageDAO {
    private final Map<UUID, List<PageImage>> pagesByDocumentId = new LinkedHashMap<>();

    public void saveAll(UUID documentId, List<PageImage> pageImages) {
        if (documentId == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }
        pagesByDocumentId.put(documentId, List.copyOf(new ArrayList<>(pageImages)));
    }

    public List<PageImage> findByDocumentId(UUID documentId) {
        if (documentId == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }
        return pagesByDocumentId.getOrDefault(documentId, List.of());
    }
}
