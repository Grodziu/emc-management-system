package pl.emcmanagement.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ImageStorageUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void prepareForDatabaseKeepsSmallImageAsPng() throws IOException {
        Path source = tempDir.resolve("small-sample.png");
        BufferedImage image = new BufferedImage(240, 140, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(248, 250, 252, 255));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(new Color(16, 78, 139, 255));
        graphics.fillRoundRect(12, 12, 216, 116, 18, 18);
        graphics.setColor(Color.WHITE);
        graphics.drawString("EMC", 96, 74);
        graphics.dispose();
        ImageIO.write(image, "png", source.toFile());

        ImageStorageUtil.StoredImage storedImage = ImageStorageUtil.prepareForDatabase(source);

        assertTrue(storedImage.fileName().endsWith(".png"));
        assertTrue(storedImage.fileData().length <= 700_000);
        BufferedImage decoded = ImageStorageUtil.decodeImage(storedImage.fileData());
        assertEquals(240, decoded.getWidth());
        assertEquals(140, decoded.getHeight());
    }

    @Test
    void prepareForDatabaseCompressesLargeImageToJpegUnderLimit() throws IOException {
        Path source = tempDir.resolve("large-noise.png");
        BufferedImage image = new BufferedImage(2200, 2200, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(12345L);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, random.nextInt(0x0100_0000));
            }
        }
        ImageIO.write(image, "png", source.toFile());

        ImageStorageUtil.StoredImage storedImage = ImageStorageUtil.prepareForDatabase(source);
        BufferedImage decoded = ImageStorageUtil.decodeImage(storedImage.fileData());

        assertTrue(storedImage.fileName().endsWith(".jpg"));
        assertTrue(storedImage.fileData().length <= 700_000);
        assertTrue(Math.max(decoded.getWidth(), decoded.getHeight()) <= 1800);
    }

    @Test
    void decodeImageRejectsEmptyContent() {
        IOException exception = assertThrows(IOException.class, () -> ImageStorageUtil.decodeImage(new byte[0]));

        assertTrue(exception.getMessage().contains("pusty"));
    }
}
