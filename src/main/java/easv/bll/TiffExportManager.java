package easv.bll;

import easv.be.Document;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * This class prepares TIFF export previews.
 * It does not depend on JavaFX, so any future screen can reuse it.
 */
public class TiffExportManager {
    private static final String DEFAULT_NAMING_PATTERN = "{profileCode}_{boxId}";

    public TiffExportPlan createSinglePagePlan(String profileName, String boxId, List<PageImage> pages) {
        return createSinglePagePlan(profileName, profileName, DEFAULT_NAMING_PATTERN, boxId, pages);
    }

    public TiffExportPlan createSinglePagePlan(
            String profileName,
            String profileCode,
            String exportNaming,
            String boxId,
            List<PageImage> pages
    ) {
        List<TiffExportItem> items = new ArrayList<>();

        for (PageImage page : safePages(pages)) {
            String documentId = page.getSourceReference();
            String fileName = buildFileName(
                    profileName,
                    profileCode,
                    exportNaming,
                    boxId,
                    documentId,
                    page.getPageNumber(),
                    "page-" + page.getPageNumber()
            );
            items.add(new TiffExportItem(documentId, fileName, List.of(page)));
        }

        return new TiffExportPlan("SINGLE_PAGE_TIFFS", items, buildWarnings(profileName, boxId));
    }

    public TiffExportPlan createMultiPagePlan(String profileName, String boxId, List<PageImage> pages) {
        List<PageImage> selectedPages = safePages(pages);
        if (selectedPages.isEmpty()) {
            return new TiffExportPlan("MULTI_PAGE_TIFFS_BY_DOCUMENT", List.of(), buildWarnings(profileName, boxId));
        }

        return createMultiPagePlan(profileName, profileName, DEFAULT_NAMING_PATTERN, boxId,
                List.of(new Document("selected-files", selectedPages)));
    }

    public TiffExportPlan createMultiPagePlan(
            String profileName,
            String profileCode,
            String exportNaming,
            String boxId,
            List<Document> documents
    ) {
        List<Document> selectedDocuments = safeDocuments(documents);
        List<TiffExportItem> items = new ArrayList<>();

        int documentNumber = 1;
        for (Document document : selectedDocuments) {
            List<PageImage> documentPages = safePages(document.getPages());
            if (documentPages.isEmpty()) {
                continue;
            }

            String documentId = document.getSourceItemId();
            String fileName = buildFileName(
                    profileName,
                    profileCode,
                    exportNaming,
                    boxId,
                    documentId,
                    documentNumber,
                    "multi-page"
            );
            items.add(new TiffExportItem(documentId, fileName, documentPages));
            documentNumber++;
        }

        return new TiffExportPlan("MULTI_PAGE_TIFFS_BY_DOCUMENT", items, buildWarnings(profileName, boxId));
    }

    public ExportResult exportPlan(TiffExportPlan plan, Path outputDirectory) throws IOException {
        if (plan == null) {
            throw new IllegalArgumentException("plan must not be null");
        }
        if (outputDirectory == null) {
            throw new IllegalArgumentException("outputDirectory must not be null");
        }

        Files.createDirectories(outputDirectory);
        List<Path> writtenFiles = new ArrayList<>();

        for (TiffExportItem item : plan.getItems()) {
            Path outputFile = outputDirectory.resolve(safeFileName(item.getFileName()));
            writeTiff(outputFile, item.getPages());
            writtenFiles.add(outputFile);
        }

        return new ExportResult(outputDirectory, writtenFiles);
    }

    private List<PageImage> safePages(List<PageImage> pages) {
        if (pages == null) {
            return new ArrayList<>();
        }

        List<PageImage> activePages = new ArrayList<>();
        for (PageImage page : pages) {
            if (page != null) {
                activePages.add(page);
            }
        }
        return activePages;
    }

    private List<Document> safeDocuments(List<Document> documents) {
        if (documents == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(documents);
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

    private String buildFileName(
            String profileName,
            String profileCode,
            String exportNaming,
            String boxId,
            String documentId,
            int documentNumber,
            String ending
    ) {
        String pattern = isBlank(exportNaming) ? DEFAULT_NAMING_PATTERN : exportNaming.trim();
        boolean containsDocumentToken = pattern.contains("{documentId}") || pattern.contains("{documentNumber}");
        boolean containsEndingToken = pattern.contains("{ending}");

        String baseName = pattern
                .replace("{profileCode}", fallback(profileCode, profileName))
                .replace("{profileName}", fallback(profileName, profileCode))
                .replace("{boxId}", fallback(boxId, "box"))
                .replace("{documentId}", fallback(documentId, "document-" + documentNumber))
                .replace("{documentNumber}", String.valueOf(documentNumber))
                .replace("{ending}", fallback(ending, "export"));

        if (!containsDocumentToken) {
            baseName += "_" + fallback(documentId, "document-" + documentNumber);
        }

        if (!containsEndingToken && !isBlank(ending) && !"multi-page".equalsIgnoreCase(ending)) {
            baseName += "_" + ending;
        }

        return safeName(baseName) + ".tiff";
    }

    private void writeTiff(Path outputFile, List<PageImage> pages) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        for (PageImage page : safePages(pages)) {
            images.add(resolvePageImage(page));
        }

        if (images.isEmpty()) {
            throw new IOException("Cannot export an empty TIFF file.");
        }

        var writers = ImageIO.getImageWritersByFormatName("TIFF");
        ImageWriter writer = writers.hasNext() ? writers.next() : null;
        if (writer == null) {
            throw new IOException("No TIFF writer is available in this Java runtime.");
        }

        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(outputFile.toFile())) {
            writer.setOutput(outputStream);
            writer.prepareWriteSequence(null);
            for (BufferedImage image : images) {
                writer.writeToSequence(new IIOImage(image, null, null), null);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    private BufferedImage resolvePageImage(PageImage page) {
        BufferedImage image = decodeDataUri(page.getDisplayContent());
        if (image != null) {
            return image;
        }

        image = decodeDataUri(page.getPreviewContent());
        if (image != null) {
            return image;
        }

        return createPlaceholderPage(page);
    }

    private BufferedImage decodeDataUri(String value) {
        if (isBlank(value)) {
            return null;
        }

        int commaIndex = value.indexOf(',');
        if (commaIndex < 0) {
            return null;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(value.substring(commaIndex + 1));
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IllegalArgumentException | IOException exception) {
            return null;
        }
    }

    private BufferedImage createPlaceholderPage(PageImage page) {
        BufferedImage image = new BufferedImage(850, 1100, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.DARK_GRAY);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 28));
            graphics.drawString("Scanned page", 72, 120);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
            graphics.drawString("Source: " + fallback(page == null ? null : page.getSourceReference(), "unknown"), 72, 170);
            graphics.drawString("Page: " + (page == null ? "-" : page.getPageNumber()), 72, 210);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private String fallback(String value, String fallback) {
        if (!isBlank(value)) {
            return value.trim();
        }
        return isBlank(fallback) ? "missing" : fallback.trim();
    }

    private String safeFileName(String value) {
        String fileName = safeName(value);
        return fileName.toLowerCase().endsWith(".tiff") ? fileName : fileName + ".tiff";
    }

    private String safeName(String value) {
        if (isBlank(value)) {
            return "missing";
        }

        return value.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record ExportResult(Path outputDirectory, List<Path> writtenFiles) {
        public ExportResult {
            writtenFiles = writtenFiles == null ? List.of() : List.copyOf(writtenFiles);
        }
    }
}
