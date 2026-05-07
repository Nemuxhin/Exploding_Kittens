package easv.bll;

import easv.be.PageImage;
import easv.be.TiffExportItem;
import easv.be.TiffExportPlan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class prepares TIFF export previews.
 * It does not depend on JavaFX, so any future screen can reuse it.
 */
public class TiffExportManager {

    public TiffExportPlan createSinglePagePlan(String profileName, String boxId, List<PageImage> pages) {
        List<TiffExportItem> items = new ArrayList<>();

        for (PageImage page : activePagesOnly(pages)) {
            String fileName = buildFileName(profileName, boxId, page.getDocumentId(), "page-" + page.getPageNumber());
            items.add(new TiffExportItem(page.getDocumentId(), fileName, List.of(page)));
        }

        return new TiffExportPlan("SINGLE_PAGE_TIFFS", items, buildWarnings(profileName, boxId));
    }

    public TiffExportPlan createMultiPagePlan(String profileName, String boxId, List<PageImage> pages) {
        Map<String, List<PageImage>> pagesByDocument = new LinkedHashMap<>();

        for (PageImage page : activePagesOnly(pages)) {
            pagesByDocument.computeIfAbsent(page.getDocumentId(), key -> new ArrayList<>()).add(page);
        }

        List<TiffExportItem> items = new ArrayList<>();

        for (Map.Entry<String, List<PageImage>> entry : pagesByDocument.entrySet()) {
            String fileName = buildFileName(profileName, boxId, entry.getKey(), "multi-page");
            items.add(new TiffExportItem(entry.getKey(), fileName, entry.getValue()));
        }

        return new TiffExportPlan("MULTI_PAGE_TIFF_PER_DOCUMENT", items, buildWarnings(profileName, boxId));
    }

    private List<PageImage> activePagesOnly(List<PageImage> pages) {
        List<PageImage> activePages = new ArrayList<>();

        if (pages == null) {
            return activePages;
        }

        for (PageImage page : pages) {
            if (page.isActive()) {
                activePages.add(page);
            }
        }

        return activePages;
    }

    private List<String> buildWarnings(String profileName, String boxId) {
        List<String> warnings = new ArrayList<>();

        if (isBlank(profileName)) {
            warnings.add("Profile name is missing.");
        }

        if (isBlank(boxId)) {
            warnings.add("Box ID is missing.");
        }

        return warnings;
    }

    private String buildFileName(String profileName, String boxId, String documentId, String ending) {
        return safeName(profileName) + "_" + safeName(boxId) + "_" + safeName(documentId) + "_" + safeName(ending) + ".tiff";
    }

    private String safeName(String value) {
        if (isBlank(value)) {
            return "missing";
        }

        return value.trim().replaceAll("[^a-zA-Z0-9-]", "_");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
