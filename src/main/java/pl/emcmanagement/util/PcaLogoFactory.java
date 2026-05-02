package pl.emcmanagement.util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public final class PcaLogoFactory {
    private static final Color PCA_BLUE = new Color(12, 88, 167);
    private static final Color PCA_DARK_BLUE = new Color(9, 60, 128);
    private static final Color PCA_RED = new Color(220, 54, 59);
    private static final Color PCA_WHITE = Color.WHITE;

    private PcaLogoFactory() {
    }

    public static BufferedImage createImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g2.setColor(PCA_WHITE);
        g2.fillRect(0, 0, width, height);

        float outerMargin = Math.max(2f, width * 0.02f);
        float arc = Math.max(16f, width * 0.08f);
        Shape border = new RoundRectangle2D.Float(
                outerMargin,
                outerMargin,
                width - 2 * outerMargin,
                height - 2 * outerMargin,
                arc,
                arc
        );
        g2.setColor(PCA_WHITE);
        g2.fill(border);
        g2.setColor(PCA_BLUE);
        g2.setStroke(new BasicStroke(Math.max(2f, width * 0.01f)));
        g2.draw(border);

        int topAreaHeight = Math.round(height * 0.45f);
        drawPcaHeader(g2, width, topAreaHeight);
        drawBottomPanel(g2, width, height, topAreaHeight);

        g2.dispose();
        return image;
    }

    public static Icon createIcon(int width, int height) {
        return new ImageIcon(createImage(width, height));
    }

    private static void drawPcaHeader(Graphics2D g2, int width, int topAreaHeight) {
        int left = Math.round(width * 0.10f);
        int top = Math.round(topAreaHeight * 0.10f);

        g2.setColor(PCA_BLUE);
        g2.setFont(new Font("Arial", Font.BOLD, Math.max(28, Math.round(width * 0.23f))));
        FontMetrics largeMetrics = g2.getFontMetrics();
        String pca = "PCA";
        int pcaX = left;
        int pcaY = top + largeMetrics.getAscent();
        g2.drawString(pca, pcaX, pcaY);

        int letterWidth = largeMetrics.stringWidth("A");
        int redStripeX = pcaX + largeMetrics.stringWidth("PC") + Math.round(letterWidth * 0.22f);
        int redStripeY = pcaY - Math.round(largeMetrics.getAscent() * 0.18f);
        Polygon stripe = new Polygon();
        stripe.addPoint(redStripeX, redStripeY + Math.round(letterWidth * 0.52f));
        stripe.addPoint(redStripeX + Math.round(letterWidth * 0.92f), redStripeY + Math.round(letterWidth * 0.52f));
        stripe.addPoint(redStripeX + Math.round(letterWidth * 0.78f), redStripeY + Math.round(letterWidth * 0.68f));
        stripe.addPoint(redStripeX - Math.round(letterWidth * 0.14f), redStripeY + Math.round(letterWidth * 0.68f));
        g2.setColor(PCA_RED);
        g2.fillPolygon(stripe);

        g2.setColor(PCA_BLUE);
        Font labelFont = new Font("Arial", Font.BOLD, Math.max(10, Math.round(width * 0.075f)));
        g2.setFont(labelFont);
        FontMetrics labelMetrics = g2.getFontMetrics();
        String line1 = "POLSKIE CENTRUM";
        String line2 = "AKREDYTACJI";
        int textWidth = Math.max(labelMetrics.stringWidth(line1), labelMetrics.stringWidth(line2));
        int textX = (width - textWidth) / 2;
        int line1Y = topAreaHeight - Math.round(topAreaHeight * 0.18f);
        int line2Y = line1Y + labelMetrics.getHeight();
        g2.drawString(line1, textX, line1Y);
        g2.drawString(line2, (width - labelMetrics.stringWidth(line2)) / 2, line2Y);
    }

    private static void drawBottomPanel(Graphics2D g2, int width, int height, int topAreaHeight) {
        int panelX = Math.round(width * 0.12f);
        int panelWidth = width - 2 * panelX;
        int panelY = topAreaHeight + Math.round(height * 0.03f);
        int panelHeight = height - panelY - Math.round(height * 0.10f);

        g2.setColor(PCA_BLUE);
        g2.fillRect(panelX, panelY, panelWidth, panelHeight);

        int emblemSize = Math.round(Math.min(panelWidth, panelHeight) * 0.42f);
        int emblemX = panelX + (panelWidth - emblemSize) / 2;
        int emblemY = panelY + Math.round(panelHeight * 0.12f);
        drawVerificationEmblem(g2, emblemX, emblemY, emblemSize);

        g2.setColor(PCA_WHITE);
        Font font = new Font("Arial", Font.BOLD, Math.max(10, Math.round(width * 0.072f)));
        g2.setFont(font);
        FontMetrics metrics = g2.getFontMetrics();
        String line1 = "WERYFIKACJA";
        String line2 = "I WALIDACJA";
        int line1X = (width - metrics.stringWidth(line1)) / 2;
        int line2X = (width - metrics.stringWidth(line2)) / 2;
        int line1Y = panelY + panelHeight - Math.round(panelHeight * 0.22f);
        int line2Y = line1Y + metrics.getHeight();
        g2.drawString(line1, line1X, line1Y);
        g2.drawString(line2, line2X, line2Y);
    }

    private static void drawVerificationEmblem(Graphics2D g2, int x, int y, int size) {
        g2.setColor(PCA_WHITE);
        g2.setStroke(new BasicStroke(Math.max(2f, size * 0.05f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        int outerRadius = size / 2 - 2;
        int innerRadius = Math.round(size * 0.22f);

        for (int tooth = 0; tooth < 8; tooth++) {
            double angle = Math.toRadians(45d * tooth);
            int x1 = centerX + (int) Math.round(Math.cos(angle) * (innerRadius + 6));
            int y1 = centerY + (int) Math.round(Math.sin(angle) * (innerRadius + 6));
            int x2 = centerX + (int) Math.round(Math.cos(angle) * outerRadius);
            int y2 = centerY + (int) Math.round(Math.sin(angle) * outerRadius);
            g2.drawLine(x1, y1, x2, y2);
        }

        g2.drawOval(centerX - outerRadius + 8, centerY - outerRadius + 8, (outerRadius - 8) * 2, (outerRadius - 8) * 2);
        g2.drawOval(centerX - innerRadius, centerY - innerRadius, innerRadius * 2, innerRadius * 2);
        g2.drawArc(centerX - innerRadius, centerY - innerRadius + 2, innerRadius * 2, innerRadius * 2 - 4, 40, 210);
        g2.drawArc(centerX - innerRadius + 4, centerY - innerRadius + 6, innerRadius * 2 - 8, innerRadius * 2 - 12, 230, 180);
    }
}
