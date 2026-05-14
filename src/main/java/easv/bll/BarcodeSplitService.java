package easv.bll;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import easv.be.PageImage;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BarcodeSplitService {
    private static final Map<DecodeHintType, Object> DECODE_HINTS = createDecodeHints();
    private static final int BARCODE_DECODE_TARGET_MAX_DIMENSION = 1400;
    private static final double MIN_SEPARATOR_SPAN_RATIO = 0.18;
    private static final int MIN_SEPARATOR_BARCODE_COUNT = 2;
    private static final Set<BarcodeFormat> ALLOWED_SEPARATOR_FORMATS = EnumSet.of(
            BarcodeFormat.CODE_39,
            BarcodeFormat.CODE_93,
            BarcodeFormat.CODE_128,
            BarcodeFormat.CODABAR,
            BarcodeFormat.ITF,
            BarcodeFormat.EAN_8,
            BarcodeFormat.EAN_13,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E
    );

    public DetectionResult classify(String sourceReference, String barcodeValue, byte[] imageBytes) {
        if (sourceReference == null || sourceReference.isBlank()) {
            throw new IllegalArgumentException("sourceReference must not be blank");
        }

        DecodedBarcode decodedBarcode = decodeSeparatorBarcode(imageBytes);
        if (decodedBarcode != null) {
            return new DetectionResult(PageImage.PageType.BARCODE, decodedBarcode.value());
        }

        return new DetectionResult(PageImage.PageType.TIFF, "");
    }

    private DecodedBarcode decodeSeparatorBarcode(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }

        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
            if (imageInputStream == null) {
                return null;
            }

            var readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                return null;
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                ImageReadParam param = reader.getDefaultReadParam();
                int subsampling = Math.max(
                        1,
                        Math.max(width, height) / BARCODE_DECODE_TARGET_MAX_DIMENSION
                );
                if (subsampling > 1) {
                    param.setSourceSubsampling(subsampling, subsampling, 0, 0);
                }

                BufferedImage image = reader.read(0, param);
                if (image == null) {
                    return null;
                }

                return tryDecodeVariants(image);
            } finally {
                reader.dispose();
            }
        } catch (Exception exception) {
            return null;
        }
    }

    private DecodedBarcode tryDecodeVariants(BufferedImage image) {
        for (BufferedImage variant : buildDecodeVariants(image)) {
            List<DecodedBarcode> decodedBarcodes = tryDecodeAll(variant);
            if (decodedBarcodes.isEmpty()) {
                continue;
            }

            for (DecodedBarcode decoded : decodedBarcodes) {
                if (looksLikeSeparatorValue(decoded.value())) {
                    return decoded;
                }
            }

            if (decodedBarcodes.size() >= MIN_SEPARATOR_BARCODE_COUNT) {
                return decodedBarcodes.get(0);
            }
        }

        return null;
    }

    private List<DecodedBarcode> tryDecodeAll(BufferedImage image) {
        List<DecodedBarcode> decodedBarcodes = new ArrayList<>();
        collectDecodedBarcodes(image, decodedBarcodes);
        return decodedBarcodes;
    }

    private void collectDecodedBarcodes(BufferedImage image, List<DecodedBarcode> decodedBarcodes) {
        try {
            LuminanceSource luminanceSource = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(luminanceSource));
            MultiFormatReader reader = new MultiFormatReader();

            Result single = reader.decode(bitmap, DECODE_HINTS);
            addIfSeparatorLike(single, image, decodedBarcodes);

            Result[] multiple = new GenericMultipleBarcodeReader(reader).decodeMultiple(bitmap, DECODE_HINTS);
            if (multiple != null) {
                for (Result result : multiple) {
                    addIfSeparatorLike(result, image, decodedBarcodes);
                }
            }
        } catch (NotFoundException exception) {
            // no barcode found in this variant
        } catch (Exception exception) {
            // malformed variant or unsupported result shape
        }
    }

    private void addIfSeparatorLike(Result result, BufferedImage image, List<DecodedBarcode> decodedBarcodes) {
        if (result == null) {
            return;
        }

        if (!ALLOWED_SEPARATOR_FORMATS.contains(result.getBarcodeFormat())) {
            return;
        }

        if (!hasSeparatorScale(result.getResultPoints(), image.getWidth(), image.getHeight())) {
            return;
        }

        String value = result.getText() == null ? "" : result.getText().trim();
        if (value.isBlank()) {
            return;
        }

        boolean alreadyPresent = decodedBarcodes.stream()
                .anyMatch(existing -> existing.value().equals(value) && existing.format() == result.getBarcodeFormat());
        if (!alreadyPresent) {
            decodedBarcodes.add(new DecodedBarcode(value, result.getBarcodeFormat()));
        }
    }

    private boolean looksLikeSeparatorValue(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("sep") || normalized.contains("separator");
    }

    private boolean hasSeparatorScale(ResultPoint[] points, int imageWidth, int imageHeight) {
        if (points == null || points.length < 2) {
            return false;
        }

        float minX = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;

        for (ResultPoint point : points) {
            if (point == null) {
                continue;
            }
            minX = Math.min(minX, point.getX());
            maxX = Math.max(maxX, point.getX());
            minY = Math.min(minY, point.getY());
            maxY = Math.max(maxY, point.getY());
        }

        if (minX == Float.MAX_VALUE || minY == Float.MAX_VALUE) {
            return false;
        }

        double spanX = Math.max(0, maxX - minX);
        double spanY = Math.max(0, maxY - minY);
        double dominantSpan = Math.max(spanX, spanY);
        double maxDimension = Math.max(imageWidth, imageHeight);
        return dominantSpan >= maxDimension * MIN_SEPARATOR_SPAN_RATIO;
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

    private static Map<DecodeHintType, Object> createDecodeHints() {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(com.google.zxing.BarcodeFormat.class));
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        return hints;
    }

    public record DetectionResult(PageImage.PageType pageType, String barcodeValue) {
    }

    private record DecodedBarcode(String value, BarcodeFormat format) {
    }
}
