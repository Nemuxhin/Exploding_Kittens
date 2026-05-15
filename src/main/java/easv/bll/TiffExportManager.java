package easv.bll;

import easv.be.PageImage;
import easv.be.TiffExportItem;
import easv.be.TiffExportPlan;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    public TiffExportPlan createMultiPagePerDocumentPlan(String profileName, String boxId,
                                                         Map<String, List<PageImage>> pagesByDocument) {
        List<TiffExportItem> items = new ArrayList<>();

        if (pagesByDocument != null) {
            for (Map.Entry<String, List<PageImage>> entry : pagesByDocument.entrySet()) {
                List<PageImage> documentPages = safePages(entry.getValue());

                if (!documentPages.isEmpty()) {
                    // Each document becomes one TIFF that can contain many pages.
                    String documentId = entry.getKey();
                    String fileName = buildFileName(profileName, boxId, documentId, "multi-page");
                    items.add(new TiffExportItem(documentId, fileName, documentPages));
                }
            }
        }

        return new TiffExportPlan("MULTI_PAGE_TIFF_PER_DOCUMENT", items, buildWarnings(profileName, boxId));
    }

    public List<Path> exportPlanToFolder(TiffExportPlan plan, Path outputFolder) throws IOException {
        List<Path> exportedFiles = new ArrayList<>();

        if (plan == null || outputFolder == null) {
            return exportedFiles;
        }

        Files.createDirectories(outputFolder);

        for (TiffExportItem item : plan.getItems()) {
            Path outputFile = outputFolder.resolve(item.getFileName());
            writeTiffFile(outputFile, item.getPages());
            exportedFiles.add(outputFile);
        }

        return exportedFiles;
    }

    private void writeTiffFile(Path outputFile, List<PageImage> pages) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("TIFF");

        if (!writers.hasNext()) {
            throw new IOException("No TIFF writer is available in this Java runtime.");
        }

        ImageWriter writer = writers.next();

        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(outputFile.toFile())) {
            writer.setOutput(outputStream);
            writer.prepareWriteSequence(null);

            for (PageImage page : safePages(pages)) {
                writer.writeToSequence(new IIOImage(createPlaceholderImage(page), null, null), null);
            }

            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    private BufferedImage createPlaceholderImage(PageImage page) {
        BufferedImage image = new BufferedImage(900, 1200, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            // The scanner mock has no real pixels, so we write a simple readable TIFF page.
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(new Color(31, 41, 55));
            graphics.setFont(new Font("SansSerif", Font.BOLD, 34));
            graphics.drawString("WebLager TIFF export", 72, 120);
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 24));
            graphics.drawString("Document: " + page.getSourceReference(), 72, 190);
            graphics.drawString("Page: " + page.getPageNumber(), 72, 235);
            graphics.drawString("Type: " + page.getPageType(), 72, 280);
        } finally {
            graphics.dispose();
        }

        return image;
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
