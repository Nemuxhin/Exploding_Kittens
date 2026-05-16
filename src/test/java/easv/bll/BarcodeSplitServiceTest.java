package easv.bll;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code39Writer;
import com.google.zxing.oned.Code128Writer;
import easv.be.PageImage;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BarcodeSplitServiceTest {

    @Test
    void classifyUsesProvidedBarcodeValueForBarcodeSourceReference() {
        BarcodeSplitService service = new BarcodeSplitService();

        BarcodeSplitService.DetectionResult result = service.classify("separator_barcode.tiff", "ABC-12345", new byte[0]);

        assertEquals(PageImage.PageType.BARCODE, result.pageType());
        assertEquals("ABC-12345", result.barcodeValue());
    }

    @Test
    void classifyDetectsBarcodeFromTiffBytes() throws Exception {
        BarcodeSplitService service = new BarcodeSplitService();
        byte[] tiffBytes = createBarcodeTiff("123456789012");

        BarcodeSplitService.DetectionResult result = service.classify("scan-002.tiff", "", tiffBytes);

        assertEquals(PageImage.PageType.BARCODE, result.pageType());
        assertEquals("123456789012", result.barcodeValue());
    }

    @Test
    void classifyRejectsWeakProvidedBarcodeValue() {
        BarcodeSplitService service = new BarcodeSplitService();

        BarcodeSplitService.DetectionResult result = service.classify("scan-003.tiff", "A12", new byte[0]);

        assertEquals(PageImage.PageType.TIFF, result.pageType());
        assertEquals("", result.barcodeValue());
    }

    @Test
    void classifyAcceptsShortNumericBarcodeValue() {
        BarcodeSplitService service = new BarcodeSplitService();

        BarcodeSplitService.DetectionResult result = service.classify("scan-004.tiff", "4643719", new byte[0]);

        assertEquals(PageImage.PageType.BARCODE, result.pageType());
        assertEquals("4643719", result.barcodeValue());
    }

    @Test
    void classifyDetectsCode39BarcodeFromTiffBytes() throws Exception {
        BarcodeSplitService service = new BarcodeSplitService();
        byte[] tiffBytes = createBarcodeTiff("SEP4643719", BarcodeFormat.CODE_39);

        BarcodeSplitService.DetectionResult result = service.classify("scan-004b.tiff", "", tiffBytes);

        assertEquals(PageImage.PageType.BARCODE, result.pageType());
        assertEquals("SEP4643719", result.barcodeValue());
    }

    @Test
    void classifyRejectsGenericProvidedMixedValueWithoutDecode() {
        BarcodeSplitService service = new BarcodeSplitService();

        BarcodeSplitService.DetectionResult result = service.classify("scan-005.tiff", "ABC-12345", new byte[0]);

        assertEquals(PageImage.PageType.TIFF, result.pageType());
        assertEquals("", result.barcodeValue());
    }

    @Test
    void previewConversionProducesPngBytes() throws Exception {
        byte[] tiffBytes = createBarcodeTiff("998877665544");

        byte[] pngBytes = TiffImageSupport.createPreviewPng(tiffBytes, 900, 1200);

        assertTrue(pngBytes.length > 8);
        assertArrayEquals(
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47},
                new byte[] {pngBytes[0], pngBytes[1], pngBytes[2], pngBytes[3]}
        );
    }

    private byte[] createBarcodeTiff(String value) throws Exception {
        return createBarcodeTiff(value, BarcodeFormat.CODE_128);
    }

    private byte[] createBarcodeTiff(String value, BarcodeFormat format) throws Exception {
        BufferedImage page = new BufferedImage(1700, 2200, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = page.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, page.getWidth(), page.getHeight());

            BitMatrix matrix = switch (format) {
                case CODE_39 -> new Code39Writer().encode(value, format, 900, 220);
                case CODE_128 -> new Code128Writer().encode(value, format, 900, 220);
                default -> throw new IllegalArgumentException("Unsupported test barcode format: " + format);
            };
            BufferedImage barcode = MatrixToImageWriter.toBufferedImage(matrix);
            graphics.drawImage(barcode, 120, 1320, null);
        } finally {
            graphics.dispose();
        }

        ImageIO.scanForPlugins();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            assertTrue(ImageIO.write(page, "tiff", output));
            return output.toByteArray();
        }
    }
}
