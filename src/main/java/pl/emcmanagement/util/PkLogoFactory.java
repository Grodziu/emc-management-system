package pl.emcmanagement.util;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PkLogoFactory {
    private static final Color PK_BLUE = new Color(17, 69, 134);
    private static final String RESOURCE_PATH = "/images/pk-logo.png";
    private static final Map<String, Icon> CACHE = new ConcurrentHashMap<>();

    private PkLogoFactory() {
    }

    public static Icon createLogo(int width, int height) {
        String key = width + "x" + height;
        return CACHE.computeIfAbsent(key, unused -> new ImageIcon(drawLogo(width, height)));
    }

    private static BufferedImage drawLogo(int width, int height) {
        BufferedImage resourceLogo = loadFromResources(width, height);
        return resourceLogo != null ? resourceLogo : drawFallbackLogo(width, height);
    }

    private static BufferedImage loadFromResources(int width, int height) {
        try (InputStream inputStream = PkLogoFactory.class.getResourceAsStream(RESOURCE_PATH)) {
            if (inputStream == null) {
                return null;
            }
            BufferedImage source = ImageIO.read(inputStream);
            if (source == null) {
                return null;
            }
            return scaleToFit(source, width, height);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static BufferedImage scaleToFit(BufferedImage source, int targetWidth, int targetHeight) {
        BufferedImage canvas = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        double widthRatio = targetWidth / (double) source.getWidth();
        double heightRatio = targetHeight / (double) source.getHeight();
        double scale = Math.min(widthRatio, heightRatio);
        int scaledWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int scaledHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int x = (targetWidth - scaledWidth) / 2;
        int y = (targetHeight - scaledHeight) / 2;

        g2.drawImage(source, x, y, scaledWidth, scaledHeight, null);
        g2.dispose();
        return canvas;
    }

    private static BufferedImage drawFallbackLogo(int width, int height) {
        int oversample = 4;
        BufferedImage image = new BufferedImage(width * oversample, height * oversample, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double scale = Math.min(width * oversample, height * oversample) / 100.0;
        double offsetX = ((width * oversample) - 100 * scale) / 2.0;
        double offsetY = ((height * oversample) - 100 * scale) / 2.0;
        g2.translate(offsetX, offsetY);
        g2.scale(scale, scale);

        paintOuterShape(g2);
        paintFullDetails(g2);

        g2.dispose();
        return scaleToFit(image, width, height);
    }

    private static void paintOuterShape(Graphics2D g2) {
        Path2D outer = new Path2D.Double();
        outer.moveTo(14, 90);
        outer.lineTo(14, 18);
        outer.lineTo(29, 18);
        outer.lineTo(29, 30);
        outer.lineTo(43, 30);
        outer.lineTo(43, 18);
        outer.lineTo(57, 18);
        outer.lineTo(57, 30);
        outer.lineTo(71, 30);
        outer.lineTo(71, 18);
        outer.lineTo(86, 18);
        outer.lineTo(86, 90);
        outer.closePath();

        g2.setColor(PK_BLUE);
        g2.fill(outer);
    }

    private static void paintCompactDetails(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(18, 58, 82, 58);

        Font font = new Font("Segoe UI", Font.BOLD, 30);
        g2.setFont(font);
        FontMetrics metrics = g2.getFontMetrics();
        String text = "PK";
        int textX = (100 - metrics.stringWidth(text)) / 2;
        int textY = 82 - metrics.getDescent();
        g2.drawString(text, textX, textY);
    }

    private static void paintFullDetails(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Arc2D.Double(14, 28, 25, 36, 0, 180, Arc2D.OPEN));
        g2.draw(new Arc2D.Double(37, 28, 26, 36, 0, 180, Arc2D.OPEN));
        g2.draw(new Arc2D.Double(61, 28, 25, 36, 0, 180, Arc2D.OPEN));

        g2.drawLine(39, 51, 61, 51);
        g2.drawLine(45, 36, 45, 68);
        g2.drawLine(53, 36, 53, 71);
        g2.drawLine(61, 36, 61, 68);

        g2.drawLine(16, 58, 84, 58);
        g2.drawLine(16, 78, 84, 78);

        Font font = new Font("Segoe UI", Font.BOLD, 28);
        g2.setFont(font);
        FontMetrics metrics = g2.getFontMetrics();
        String text = "PK";
        int textX = (100 - metrics.stringWidth(text)) / 2;
        int textY = 83 - metrics.getDescent();
        g2.drawString(text, textX, textY);
    }
}
