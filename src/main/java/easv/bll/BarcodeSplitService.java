package easv.bll;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import easv.be.PageImage;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BarcodeSplitService {
    private static final double MAX_SEPARATOR_DARK_PIXEL_RATIO = 0.08;
    private static final Map<DecodeHintType, Object> DECODE_HINTS = createDecodeHints();

    public DetectionResult classify(String sourceReference, String barcodeValue, String displayContent) {
        if (sourceReference == null || sourceReference.isBlank()) {
            throw new IllegalArgumentException("sourceReference must not be blank");
        }

        String normalized = sourceReference.toLowerCase(Locale.ROOT);
        if (normalized.contains("barcode") || normalized.contains("separator") || normalized.startsWith("bc_")) {
            String markerValue = barcodeValue == null ? "" : barcodeValue.trim();
            return new DetectionResult(
                    PageImage.PageType.BARCODE,
                    markerValue.isBlank() ? "BARCODE:" + sourceReference : markerValue
            );
        }

        String providedBarcodeValue = barcodeValue == null ? "" : barcodeValue.trim();
        if (!providedBarcodeValue.isBlank()) {
            return new DetectionResult(PageImage.PageType.BARCODE, providedBarcodeValue);
        }

        String decodedBarcodeValue = decodeSeparatorBarcodeValue(displayContent);
        if (!decodedBarcodeValue.isBlank()) {
            return new DetectionResult(PageImage.PageType.BARCODE, decodedBarcodeValue);
        }

        return new DetectionResult(PageImage.PageType.TIFF, "");
    }

    private String decodeSeparatorBarcodeValue(String displayContent) {
        DecodedPageBarcode decodedPageBarcode = decodePageBarcode(displayContent);
        if (decodedPageBarcode.barcodeValue().isBlank() || !isLikelySeparatorPage(decodedPageBarcode.image())) {
            return "";
        }

        return decodedPageBarcode.barcodeValue();
    }

    private DecodedPageBarcode decodePageBarcode(String displayContent) {
        if (displayContent == null || displayContent.isBlank()) {
            return DecodedPageBarcode.empty();
        }

        byte[] imageBytes = extractImageBytes(displayContent);
        if (imageBytes.length == 0) {
            return DecodedPageBarcode.empty();
        }

        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
            if (imageInputStream == null) {
                return DecodedPageBarcode.empty();
            }

            var readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                return DecodedPageBarcode.empty();
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream, true, true);
                BufferedImage image = reader.read(0);
                if (image == null) {
                    return DecodedPageBarcode.empty();
                }

                return new DecodedPageBarcode(tryDecodeVariants(image), image);
            } finally {
                reader.dispose();
            }
        } catch (Exception exception) {
            return DecodedPageBarcode.empty();
        }
    }

    private boolean isLikelySeparatorPage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int stride = Math.max(1, Math.max(width, height) / 700);
        int darkPixels = 0;
        int sampledPixels = 0;

        for (int y = 0; y < height; y += stride) {
            for (int x = 0; x < width; x += stride) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xff;
                int green = (rgb >> 8) & 0xff;
                int blue = rgb & 0xff;
                int brightness = (red + green + blue) / 3;

                if (brightness < 220) {
                    darkPixels++;
                }
                sampledPixels++;
            }
        }

        return sampledPixels > 0 && ((double) darkPixels / (double) sampledPixels) <= MAX_SEPARATOR_DARK_PIXEL_RATIO;
    }

    private String tryDecodeVariants(BufferedImage image) {
        for (BufferedImage variant : buildDecodeVariants(image)) {
            String decoded = tryDecode(variant);
            if (!decoded.isBlank()) {
                return decoded;
            }
        }
        return "";
    }

    private String tryDecode(BufferedImage image) {
        try {
            LuminanceSource luminanceSource = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(luminanceSource));
            Result result = new MultiFormatReader().decode(bitmap, DECODE_HINTS);
            return result == null ? "" : result.getText();
        } catch (NotFoundException exception) {
            return "";
        }
    }

    private List<BufferedImage> buildDecodeVariants(BufferedImage source) {
        List<BufferedImage> variants = new ArrayList<>();
        variants.add(source);

        BufferedImage rotated90 = rotateClockwise(source);
        BufferedImage rotated180 = rotateClockwise(rotated90);
        BufferedImage rotated270 = rotateClockwise(rotated180);

        variants.add(rotated90);
        variants.add(rotated180);
        variants.add(rotated270);

        for (BufferedImage image : List.of(source, rotated90, rotated180, rotated270)) {
            variants.addAll(cropRegions(image));
        }

        return variants;
    }

    private List<BufferedImage> cropRegions(BufferedImage image) {
        List<BufferedImage> regions = new ArrayList<>();

        int width = image.getWidth();
        int height = image.getHeight();

        if (width < 40 || height < 40) {
            return regions;
        }

        regions.add(crop(image, 0, 0, width, height / 2));
        regions.add(crop(image, 0, 0, width / 2, height / 2));
        regions.add(crop(image, width / 2, 0, width / 2, height / 2));
        regions.add(crop(image, 0, height / 2, width / 2, height / 2));
        regions.add(crop(image, width / 2, height / 2, width / 2, height / 2));
        regions.add(crop(image, width / 4, 0, width / 2, height / 2));

        BufferedImage topLeft = crop(image, 0, 0, Math.max(1, width / 2), Math.max(1, height / 2));
        regions.add(scale(topLeft, topLeft.getWidth() * 2, topLeft.getHeight() * 2));

        BufferedImage topBand = crop(image, 0, 0, width, Math.max(1, height / 3));
        regions.add(scale(topBand, topBand.getWidth() * 2, topBand.getHeight() * 2));

        return regions;
    }

    private BufferedImage crop(BufferedImage image, int x, int y, int width, int height) {
        int safeWidth = Math.max(1, Math.min(width, image.getWidth() - x));
        int safeHeight = Math.max(1, Math.min(height, image.getHeight() - y));
        return image.getSubimage(x, y, safeWidth, safeHeight);
    }

    private BufferedImage scale(BufferedImage source, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    private BufferedImage rotateClockwise(BufferedImage source) {
        BufferedImage rotated = new BufferedImage(source.getHeight(), source.getWidth(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                rotated.setRGB(source.getHeight() - 1 - y, x, source.getRGB(x, y));
            }
        }
        return rotated;
    }

    private byte[] extractImageBytes(String displayContent) {
        String encoded = displayContent.trim();
        int separator = encoded.indexOf("base64,");
        if (separator >= 0) {
            encoded = encoded.substring(separator + 7);
        }

        try {
            return Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException exception) {
            return new byte[0];
        }
    }

    private static Map<DecodeHintType, Object> createDecodeHints() {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(com.google.zxing.BarcodeFormat.class));
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        return hints;
    }

    public record DetectionResult(PageImage.PageType pageType, String barcodeValue) {
    }

    private record DecodedPageBarcode(String barcodeValue, BufferedImage image) {
        private static DecodedPageBarcode empty() {
            return new DecodedPageBarcode("", null);
        }
    }
}
