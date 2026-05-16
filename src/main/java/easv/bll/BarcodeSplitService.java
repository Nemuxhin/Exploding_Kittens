package easv.bll;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import easv.be.PageImage;

import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.RenderingHints;
import java.awt.image.ColorConvertOp;
import java.awt.image.BufferedImage;
import java.util.Base64;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BarcodeSplitService {
    private static final int BARCODE_SCAN_WIDTH = 1200;
    private static final int BARCODE_SCAN_HEIGHT = 1200;
    private static final Map<DecodeHintType, Object> DECODE_HINTS = createDecodeHints();

    public DetectionResult classify(String sourceReference, String barcodeValue, String displayContent) {
        byte[] imageBytes = extractImageBytes(displayContent);
        return classify(sourceReference, barcodeValue, imageBytes);
    }

    public DetectionResult classify(String sourceReference, String barcodeValue, byte[] imageBytes) {
        if (sourceReference == null || sourceReference.isBlank()) {
            throw new IllegalArgumentException("sourceReference must not be blank");
        }

        String normalized = sourceReference.toLowerCase(Locale.ROOT);
        String providedBarcodeValue = barcodeValue == null ? "" : barcodeValue.trim();
        if (looksLikeBarcodeSourceReference(normalized) && isLikelyBarcodeValue(providedBarcodeValue)) {
            return new DetectionResult(PageImage.PageType.BARCODE, providedBarcodeValue);
        }

        String decodedBarcodeValue = decodeBarcodeValue(imageBytes);
        if (!decodedBarcodeValue.isBlank()) {
            return new DetectionResult(PageImage.PageType.BARCODE, decodedBarcodeValue);
        }

        if (isStrongProvidedBarcodeValue(providedBarcodeValue)) {
            return new DetectionResult(PageImage.PageType.BARCODE, providedBarcodeValue);
        }

        return new DetectionResult(PageImage.PageType.TIFF, "");
    }

    private String decodeBarcodeValue(byte[] imageBytes) {
        if (imageBytes.length == 0) {
            return "";
        }

        BufferedImage image = TiffImageSupport.readFirstFrame(imageBytes, BARCODE_SCAN_WIDTH, BARCODE_SCAN_HEIGHT);
        if (image == null) {
            return "";
        }
        return tryDecodeVariants(image);
    }

    private String tryDecodeVariants(BufferedImage image) {
        String decoded = tryDecodeRegions(focusRegions(image));
        if (!decoded.isBlank()) {
            return decoded;
        }
        decoded = firstAcceptedDecode(image);
        if (!decoded.isBlank()) {
            return decoded;
        }

        BufferedImage grayscale = toGrayscale(image);
        decoded = tryDecodeRegions(focusRegions(grayscale));
        if (!decoded.isBlank()) {
            return decoded;
        }
        decoded = firstAcceptedDecode(grayscale);
        if (!decoded.isBlank()) {
            return decoded;
        }

        BufferedImage rotated90 = rotateClockwise(image);
        decoded = tryDecodeRegions(focusRegions(rotated90));
        if (!decoded.isBlank()) {
            return decoded;
        }
        decoded = firstAcceptedDecode(rotated90);
        if (!decoded.isBlank()) {
            return decoded;
        }

        BufferedImage rotated180 = rotateClockwise(rotated90);
        decoded = tryDecodeRegions(focusRegions(rotated180));
        if (!decoded.isBlank()) {
            return decoded;
        }
        decoded = firstAcceptedDecode(rotated180);
        if (!decoded.isBlank()) {
            return decoded;
        }

        BufferedImage rotated270 = rotateClockwise(rotated180);
        decoded = tryDecodeRegions(focusRegions(rotated270));
        if (!decoded.isBlank()) {
            return decoded;
        }
        return firstAcceptedDecode(rotated270);
    }

    private String tryDecodeRegions(List<BufferedImage> regions) {
        for (BufferedImage region : regions) {
            String decoded = firstAcceptedDecode(region);
            if (!decoded.isBlank()) {
                return decoded;
            }
        }
        return "";
    }

    private String firstAcceptedDecode(BufferedImage image) {
        String hybridDecoded = tryDecode(image, true);
        if (isLikelyBarcodeValue(hybridDecoded)) {
            return hybridDecoded;
        }

        String histogramDecoded = tryDecode(image, false);
        return isLikelyBarcodeValue(histogramDecoded) ? histogramDecoded : "";
    }

    private String tryDecode(BufferedImage image, boolean hybrid) {
        try {
            LuminanceSource luminanceSource = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = hybrid
                    ? new BinaryBitmap(new HybridBinarizer(luminanceSource))
                    : new BinaryBitmap(new GlobalHistogramBinarizer(luminanceSource));
            Result result = new MultiFormatReader().decode(bitmap, DECODE_HINTS);
            return result == null ? "" : result.getText();
        } catch (NotFoundException exception) {
            return "";
        }
    }

    private List<BufferedImage> focusRegions(BufferedImage image) {
        List<BufferedImage> regions = new java.util.ArrayList<>();
        int width = image.getWidth();
        int height = image.getHeight();

        if (width < 60 || height < 60) {
            return regions;
        }

        BufferedImage bottomHalf = crop(image, 0, height / 2, width, Math.max(1, height / 2));
        BufferedImage bottomBand = crop(image, 0, (height * 2) / 3, width, Math.max(1, height / 3));
        BufferedImage lowerLeftHalf = crop(image, 0, height / 3, Math.max(1, width / 2), height - (height / 3));
        BufferedImage lowerLeftThird = crop(image, 0, height / 2, Math.max(1, width / 3), Math.max(1, height / 2));
        BufferedImage barcodeCluster = crop(
                image,
                0,
                Math.max(0, (height * 45) / 100),
                Math.max(1, (width * 60) / 100),
                Math.max(1, (height * 40) / 100)
        );
        BufferedImage lowerCenterBand = crop(
                image,
                width / 5,
                Math.max(0, (height * 50) / 100),
                Math.max(1, (width * 3) / 5),
                Math.max(1, height - ((height * 50) / 100))
        );

        regions.add(bottomHalf);
        regions.add(bottomBand);
        regions.add(lowerLeftHalf);
        regions.add(lowerLeftThird);
        regions.add(lowerCenterBand);
        regions.add(barcodeCluster);
        regions.add(scale(lowerLeftHalf, lowerLeftHalf.getWidth() * 2, lowerLeftHalf.getHeight() * 2));
        regions.add(scale(barcodeCluster, barcodeCluster.getWidth() * 3, barcodeCluster.getHeight() * 3));

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

    private BufferedImage toGrayscale(BufferedImage source) {
        BufferedImage grayscale = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        ColorConvertOp convertOp = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        convertOp.filter(source, grayscale);
        return grayscale;
    }

    private byte[] extractImageBytes(String displayContent) {
        if (displayContent == null || displayContent.isBlank()) {
            return new byte[0];
        }
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

    private boolean isLikelyBarcodeValue(String decoded) {
        if (decoded == null) {
            return false;
        }
        String normalized = decoded.trim();
        if (normalized.length() < 7 || normalized.length() > 64) {
            return false;
        }
        int lettersOrDigits = 0;
        int digits = 0;
        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);
            if (Character.isLetterOrDigit(character)) {
                lettersOrDigits++;
                if (Character.isDigit(character)) {
                    digits++;
                }
                continue;
            }
            if ("-_/.#:".indexOf(character) < 0) {
                return false;
            }
        }
        if (digits == normalized.length()) {
            return digits >= 7;
        }
        return normalized.length() >= 8
                && digits >= 4
                && lettersOrDigits >= Math.max(8, (normalized.length() * 3) / 4);
    }

    private boolean isStrongProvidedBarcodeValue(String providedBarcodeValue) {
        if (!isLikelyBarcodeValue(providedBarcodeValue)) {
            return false;
        }

        String normalized = providedBarcodeValue.trim();
        int digits = 0;
        int letters = 0;
        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);
            if (Character.isDigit(character)) {
                digits++;
            } else if (Character.isLetter(character)) {
                letters++;
            }
        }

        if (digits == normalized.length()) {
            return digits >= 7;
        }

        return normalized.length() >= 10 && digits >= 5 && letters <= digits;
    }

    private boolean looksLikeBarcodeSourceReference(String normalizedSourceReference) {
        if (normalizedSourceReference == null || normalizedSourceReference.isBlank()) {
            return false;
        }
        return normalizedSourceReference.startsWith("bc_")
                || normalizedSourceReference.contains("separator")
                || normalizedSourceReference.contains("_barcode_")
                || normalizedSourceReference.contains("-barcode-")
                || normalizedSourceReference.startsWith("barcode_")
                || normalizedSourceReference.endsWith("_barcode.tif")
                || normalizedSourceReference.endsWith("_barcode.tiff");
    }

    private static Map<DecodeHintType, Object> createDecodeHints() {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39,
                BarcodeFormat.CODABAR,
                BarcodeFormat.ITF
        ));
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.ALSO_INVERTED, Boolean.TRUE);
        return hints;
    }

    public record DetectionResult(PageImage.PageType pageType, String barcodeValue) {
    }
}
