package pl.emcmanagement.util;

import pl.emcmanagement.database.AppConfig;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BrandLogoFactory {
    private static final Map<String, Icon> CACHE = new ConcurrentHashMap<>();

    private BrandLogoFactory() {
    }

    public static Icon createLogo(String brandName, int width, int height) {
        String normalizedBrand = normalizeBrand(brandName);
        String cacheKey = normalizedBrand + "|" + width + "x" + height + "|" + AppConfig.getOrDefault("brandfetch.client.id", "");
        return CACHE.computeIfAbsent(cacheKey, key -> loadLogo(normalizedBrand, width, height));
    }

    private static Icon loadLogo(String normalizedBrand, int width, int height) {
        String clientId = trimToNull(AppConfig.get("brandfetch.client.id"));
        String domainIdentifier = resolveDomainIdentifier(normalizedBrand);

        if (clientId != null && domainIdentifier != null) {
            Icon remoteIcon = tryLoadFromBrandfetch(domainIdentifier, clientId, width, height);
            if (remoteIcon != null) {
                return remoteIcon;
            }
        }

        return createFallbackBadge(normalizedBrand, width, height);
    }

    private static Icon tryLoadFromBrandfetch(String domainIdentifier, String clientId, int width, int height) {
        try {
            String encodedIdentifier = URLEncoder.encode(domainIdentifier, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            String encodedClientId = URLEncoder.encode(clientId, StandardCharsets.UTF_8);

            String[] candidateUrls = new String[]{
                    "https://cdn.brandfetch.io/" + encodedIdentifier + "/logo.png?c=" + encodedClientId,
                    "https://cdn.brandfetch.io/" + encodedIdentifier + "/icon.png?c=" + encodedClientId,
                    "https://cdn.brandfetch.io/domain/" + encodedIdentifier + "?c=" + encodedClientId
            };

            for (String url : candidateUrls) {
                Icon icon = tryLoadRemoteIcon(url, width, height);
                if (icon != null) {
                    return icon;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static Icon tryLoadRemoteIcon(String url, int width, int height) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(3500);
            connection.setReadTimeout(3500);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Accept", "image/png,image/*;q=0.9,*/*;q=0.1");
            connection.setRequestProperty("User-Agent", "EMC-Management-System/1.0");
            connection.setRequestProperty("Referer", AppConfig.getOrDefault("brandfetch.referer", "https://emc-management.local/"));

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            try (InputStream inputStream = connection.getInputStream()) {
                BufferedImage image = ImageIO.read(inputStream);
                if (image == null) {
                    return null;
                }
                return new ImageIcon(scaleToFit(image, width, height));
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static BufferedImage scaleToFit(BufferedImage source, int targetWidth, int targetHeight) {
        BufferedImage canvas = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
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

    private static String resolveDomainIdentifier(String normalizedBrand) {
        if (normalizedBrand.contains(".")) {
            return normalizedBrand.toLowerCase();
        }

        String configured = trimToNull(AppConfig.get("brandfetch.domain." + normalizedBrand));
        if (configured != null) {
            return configured;
        }

        return switch (normalizedBrand) {
            case "BMW" -> "bmw.com";
            case "STELLANTIS" -> "stellantis.com";
            case "VOLVO" -> "volvocars.com";
            default -> null;
        };
    }

    private static Icon createFallbackBadge(String normalizedBrand, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        Shape badge = new RoundRectangle2D.Double(6, 10, width - 12, height - 20, 18, 18);
        g2.setColor(new Color(236, 240, 244));
        g2.fill(badge);
        g2.setColor(new Color(136, 145, 154));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(badge);
        g2.setColor(new Color(54, 63, 72));
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(13, width / 9)));
        drawCenteredString(g2, shorten(normalizedBrand), new Rectangle(8, 8, width - 16, height - 16));

        g2.dispose();
        return new ImageIcon(image);
    }

    private static String normalizeBrand(String brandName) {
        if (brandName == null || brandName.isBlank()) {
            return "KLIENT";
        }
        return brandName.trim().toUpperCase();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String shorten(String brand) {
        return brand.length() <= 12 ? brand : brand.substring(0, 12);
    }

    private static void drawCenteredString(Graphics2D g2, String text, Rectangle area) {
        FontMetrics metrics = g2.getFontMetrics();
        int x = area.x + (area.width - metrics.stringWidth(text)) / 2;
        int y = area.y + ((area.height - metrics.getHeight()) / 2) + metrics.getAscent();
        g2.drawString(text, x, y);
    }
}
