package easv.bll;

import easv.be.PageImage;
import easv.be.TiffExportItem;
import easv.be.TiffExportPlan;

import java.util.ArrayList;
import java.util.List;

/**
 * This class prepares TIFF export previews.
 * It does not depend on JavaFX, so any future screen can reuse it.
 */
public class TiffExportManager {

    public TiffExportPlan createSinglePagePlan(String profileName, String boxId, List<PageImage> pages) {
        List<TiffExportItem> items = new ArrayList<>();

        for (PageImage page : safePages(pages)) {
            String documentId = page.getSourceReference();
            String fileName = buildFileName(profileName, boxId, documentId, "page-" + page.getPageNumber());
            items.add(new TiffExportItem(documentId, fileName, List.of(page)));
        }

        return new TiffExportPlan("SINGLE_PAGE_TIFFS", items, buildWarnings(profileName, boxId));
    }

    public TiffExportPlan createMultiPagePlan(String profileName, String boxId, List<PageImage> pages) {
        List<PageImage> selectedPages = safePages(pages);
        List<TiffExportItem> items = new ArrayList<>();

        if (!selectedPages.isEmpty()) {
            // Multi-page means one combined TIFF containing all selected pages/files.
            String fileName = buildFileName(profileName, boxId, "selected-files", "multi-page");
            items.add(new TiffExportItem("SELECTED_FILES", fileName, selectedPages));
        }

        return new TiffExportPlan("MULTI_PAGE_TIFF_FILE", items, buildWarnings(profileName, boxId));
    }

    private List<PageImage> safePages(List<PageImage> pages) {
        if (pages == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(pages);
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
