package easv.bll;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public final class TiffImageSupport {
    private static volatile boolean imageIoPluginsLoaded;

    private TiffImageSupport() {
    }

    public static byte[] createPreviewPng(byte[] sourceBytes, int maxWidth, int maxHeight) {
        BufferedImage image = readFirstFrame(sourceBytes, maxWidth, maxHeight);
        if (image == null) {
            return new byte[0];
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", output)) {
                return new byte[0];
            }
            return output.toByteArray();
        } catch (Exception exception) {
            return new byte[0];
        }
    }

    public static BufferedImage readFirstFrame(byte[] sourceBytes, int maxWidth, int maxHeight) {
        if (sourceBytes == null || sourceBytes.length == 0) {
            return null;
        }

        ensureImageIoPluginsLoaded();
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(sourceBytes))) {
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

                ImageReadParam param = reader.getDefaultReadParam();
                if (maxWidth > 0 && maxHeight > 0) {
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    int subsampling = Math.max(1, Math.min(
                            Math.max(1, width / maxWidth),
                            Math.max(1, height / maxHeight)
                    ));
                    param.setSourceSubsampling(subsampling, subsampling, 0, 0);
                }

                return reader.read(0, param);
            } finally {
                reader.dispose();
            }
        } catch (Exception exception) {
            return null;
        }
    }

    private static void ensureImageIoPluginsLoaded() {
        if (imageIoPluginsLoaded) {
            return;
        }
        synchronized (TiffImageSupport.class) {
            if (imageIoPluginsLoaded) {
                return;
            }
            ImageIO.scanForPlugins();
            imageIoPluginsLoaded = true;
        }
    }
}
