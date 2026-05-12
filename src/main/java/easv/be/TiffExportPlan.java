package easv.be;

import java.util.ArrayList;
import java.util.List;

/**
 * A preview of a TIFF export before files are written.
 */
public class TiffExportPlan {

    private final String exportType;
    private final List<TiffExportItem> items;
    private final List<String> warnings;

    public TiffExportPlan(String exportType, List<TiffExportItem> items, List<String> warnings) {
        this.exportType = exportType;
        this.items = new ArrayList<>(items);
        this.warnings = new ArrayList<>(warnings);
    }

    public String getExportType() {
        return exportType;
    }

    public List<TiffExportItem> getItems() {
        return new ArrayList<>(items);
    }

    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    public int getFileCount() {
        return items.size();
    }

    public int getPageCount() {
        int total = 0;

        for (TiffExportItem item : items) {
            total += item.getPages().size();
        }

        return total;
    }
}
