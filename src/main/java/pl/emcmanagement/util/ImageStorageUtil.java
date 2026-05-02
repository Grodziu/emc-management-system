package pl.emcmanagement.util;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

public final class ImageStorageUtil {
    private static final int MAX_UPLOAD_BYTES = 700_000;
    private static final int MAX_DIMENSION = 1800;
    private static final float[] JPEG_QUALITIES = {0.92f, 0.85f, 0.78f, 0.70f, 0.62f, 0.55f, 0.48f, 0.40f};

    private ImageStorageUtil() {
    }

    public static StoredImage prepareForDatabase(Path path) throws IOException {
        String originalName = path.getFileName().toString();
        BufferedImage sourceImage = readImage(path);
        BufferedImage workingImage = scaleDownIfNeeded(sourceImage, MAX_DIMENSION);

        byte[] pngBytes = writePng(workingImage);
        if (pngBytes.length <= MAX_UPLOAD_BYTES) {
            return new StoredImage(replaceExtension(originalName, "png"), pngBytes);
        }

        BufferedImage jpegSource = ensureOpaque(workingImage);
        BufferedImage candidate = jpegSource;
        for (int attempt = 0; attempt < 8; attempt++) {
            for (float quality : JPEG_QUALITIES) {
                byte[] jpegBytes = writeJpeg(candidate, quality);
                if (jpegBytes.length <= MAX_UPLOAD_BYTES) {
                    return new StoredImage(replaceExtension(originalName, "jpg"), jpegBytes);
                }
            }

            int nextWidth = Math.max(320, Math.round(candidate.getWidth() * 0.82f));
            int nextHeight = Math.max(320, Math.round(candidate.getHeight() * 0.82f));
            if (nextWidth == candidate.getWidth() && nextHeight == candidate.getHeight()) {
                break;
            }
            candidate = scaleImage(candidate, nextWidth, nextHeight);
        }

        byte[] fallbackBytes = writeJpeg(candidate, 0.34f);
        if (fallbackBytes.length <= MAX_UPLOAD_BYTES) {
            return new StoredImage(replaceExtension(originalName, "jpg"), fallbackBytes);
        }

        throw new IOException("Wybrany obraz jest zbyt duzy do zapisania w bazie nawet po kompresji. Uzyj mniejszego pliku.");
    }

    public static BufferedImage readImage(Path path) throws IOException {
        byte[] content = Files.readAllBytes(path);
        return decodeImage(content);
    }

    public static BufferedImage decodeImage(byte[] content) throws IOException {
        if (content == null || content.length == 0) {
            throw new IOException("Plik obrazu jest pusty.");
        }
        try (InputStream inputStream = new java.io.ByteArrayInputStream(content)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image != null) {
                return image;
            }
        }

        ImageIcon icon = new ImageIcon(content);
        if (icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
            BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            icon.paintIcon(null, graphics, 0, 0);
            graphics.dispose();
            return image;
        }

        throw new IOException("Nieobslugiwany format obrazu. Uzyj PNG, JPG, JPEG, BMP lub GIF.");
    }

    private static BufferedImage scaleDownIfNeeded(BufferedImage source, int maxDimension) {
        int width = source.getWidth();
        int height = source.getHeight();
        int longestEdge = Math.max(width, height);
        if (longestEdge <= maxDimension) {
            return source;
        }

        float scale = maxDimension / (float) longestEdge;
        int targetWidth = Math.max(1, Math.round(width * scale));
        int targetHeight = Math.max(1, Math.round(height * scale));
        return scaleImage(source, targetWidth, targetHeight);
    }

    private static BufferedImage scaleImage(BufferedImage source, int targetWidth, int targetHeight) {
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight,
                source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return scaled;
    }

    private static BufferedImage ensureOpaque(BufferedImage source) {
        if (!source.getColorModel().hasAlpha()) {
            return source;
        }
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return rgb;
    }

    private static byte[] writePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    private static byte[] writeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("Brak wsparcia dla zapisu JPG w aktualnym srodowisku Java.");
        }

        ImageWriter writer = writers.next();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
        return outputStream.toByteArray();
    }

    private static String replaceExtension(String fileName, String extension) {
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex >= 0 ? fileName.substring(0, dotIndex) : fileName;
        return baseName + "." + extension;
    }

    public record StoredImage(String fileName, byte[] fileData) {
    }
}
