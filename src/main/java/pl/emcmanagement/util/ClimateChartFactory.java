package pl.emcmanagement.util;

import pl.emcmanagement.model.ClimateMeasurement;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ClimateChartFactory {
    private static final Color TEXT_PRIMARY = new Color(26, 35, 44);
    private static final Color TEXT_MUTED = new Color(98, 111, 126);
    private static final Color GRID = new Color(220, 229, 239);
    private static final Color TEMP_LINE = new Color(193, 73, 71);
    private static final Color TEMP_FILL = new Color(250, 227, 226);
    private static final Color HUMIDITY_LINE = new Color(18, 98, 150);
    private static final Color HUMIDITY_FILL = new Color(225, 238, 248);

    private ClimateChartFactory() {
    }

    public static BufferedImage createChartImage(String title, List<ClimateMeasurement> measurements, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        List<ClimateMeasurement> sorted = new ArrayList<>(measurements);
        sorted.sort(Comparator
                .comparing(ClimateMeasurement::getMeasurementDate)
                .thenComparing(ClimateMeasurement::getMeasurementTime));

        g2.setColor(TEXT_PRIMARY);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g2.drawString(title, 36, 34);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(TEXT_MUTED);
        g2.drawString("Wykres temperatury i wilgotnosci dla wybranego testu", 36, 54);

        if (sorted.isEmpty()) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
            g2.setColor(TEXT_MUTED);
            g2.drawString("Brak pomiarow do narysowania wykresu.", 36, height / 2);
            g2.dispose();
            return image;
        }

        Rectangle tempArea = new Rectangle(44, 90, width - 88, (height - 150) / 2);
        Rectangle humidityArea = new Rectangle(44, tempArea.y + tempArea.height + 36, width - 88, (height - 150) / 2);

        drawSeries(g2, tempArea, sorted, true, "Temperatura [C]", TEMP_LINE, TEMP_FILL);
        drawSeries(g2, humidityArea, sorted, false, "Wilgotnosc [%RH]", HUMIDITY_LINE, HUMIDITY_FILL);

        g2.dispose();
        return image;
    }

    public static BufferedImage createCombinedChartImage(String title, List<ClimateMeasurement> measurements, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        List<ClimateMeasurement> sorted = new ArrayList<>(measurements);
        sorted.sort(Comparator
                .comparing(ClimateMeasurement::getMeasurementDate)
                .thenComparing(ClimateMeasurement::getMeasurementTime));

        g2.setColor(TEXT_PRIMARY);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g2.drawString(title, 40, 36);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(TEXT_MUTED);
        g2.drawString("Red line = temperature, blue line = relative humidity.", 40, 56);

        if (sorted.isEmpty()) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
            g2.setColor(TEXT_MUTED);
            g2.drawString("No climate measurements available.", 40, height / 2);
            g2.dispose();
            return image;
        }

        int chartLeft = 86;
        int chartRight = width - 82;
        int chartTop = 86;
        int chartBottom = height - 66;
        int chartWidth = chartRight - chartLeft;
        int chartHeight = chartBottom - chartTop;

        g2.setColor(new Color(247, 250, 253));
        g2.fillRoundRect(chartLeft - 18, chartTop - 18, chartWidth + 36, chartHeight + 36, 22, 22);
        g2.setColor(new Color(214, 224, 236));
        g2.drawRoundRect(chartLeft - 18, chartTop - 18, chartWidth + 36, chartHeight + 36, 22, 22);

        double[] temperatureRange = paddedRange(sorted, true);
        double[] humidityRange = paddedRange(sorted, false);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        for (int tick = 0; tick <= 4; tick++) {
            int y = chartTop + Math.round(chartHeight * (tick / 4f));
            g2.setColor(GRID);
            g2.drawLine(chartLeft, y, chartRight, y);

            double tempValue = temperatureRange[1] - ((temperatureRange[1] - temperatureRange[0]) * tick / 4d);
            double humidityValue = humidityRange[1] - ((humidityRange[1] - humidityRange[0]) * tick / 4d);

            g2.setColor(TEMP_LINE);
            g2.drawString(String.format(java.util.Locale.US, "%.1f", tempValue), 18, y + 4);
            g2.setColor(HUMIDITY_LINE);
            String humidityLabel = String.format(java.util.Locale.US, "%.1f", humidityValue);
            int labelWidth = g2.getFontMetrics().stringWidth(humidityLabel);
            g2.drawString(humidityLabel, width - 18 - labelWidth, y + 4);
        }

        Path2D tempPath = new Path2D.Double();
        Path2D humidityPath = new Path2D.Double();

        for (int index = 0; index < sorted.size(); index++) {
            ClimateMeasurement measurement = sorted.get(index);
            int x = chartLeft + (sorted.size() == 1
                    ? chartWidth / 2
                    : Math.round(chartWidth * (index / (float) (sorted.size() - 1))));

            int tempY = chartBottom - Math.round((float) ((measurement.getTemperature() - temperatureRange[0])
                    / (temperatureRange[1] - temperatureRange[0]) * chartHeight));
            int humidityY = chartBottom - Math.round((float) ((measurement.getHumidity() - humidityRange[0])
                    / (humidityRange[1] - humidityRange[0]) * chartHeight));

            if (index == 0) {
                tempPath.moveTo(x, tempY);
                humidityPath.moveTo(x, humidityY);
            } else {
                tempPath.lineTo(x, tempY);
                humidityPath.lineTo(x, humidityY);
            }
        }

        g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(TEMP_LINE);
        g2.draw(tempPath);
        g2.setColor(HUMIDITY_LINE);
        g2.draw(humidityPath);

        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(170, 181, 194));
        g2.drawRect(chartLeft, chartTop, chartWidth, chartHeight);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.setColor(TEMP_LINE);
        g2.drawString("Temperature [°C]", chartLeft, chartTop - 8);
        g2.setColor(HUMIDITY_LINE);
        String humidityTitle = "Humidity [%]";
        int humidityTitleWidth = g2.getFontMetrics().stringWidth(humidityTitle);
        g2.drawString(humidityTitle, chartRight - humidityTitleWidth, chartTop - 8);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        int maxLabels = Math.min(6, sorted.size());
        for (int labelIndex = 0; labelIndex < maxLabels; labelIndex++) {
            int sampleIndex = sorted.size() == 1
                    ? 0
                    : Math.round(labelIndex * (sorted.size() - 1f) / Math.max(1, maxLabels - 1));
            ClimateMeasurement measurement = sorted.get(sampleIndex);
            int x = chartLeft + (sorted.size() == 1
                    ? chartWidth / 2
                    : Math.round(chartWidth * (sampleIndex / (float) (sorted.size() - 1))));
            String labelText = formatter.format(measurement.getMeasurementDate().atTime(measurement.getMeasurementTime()));
            int labelWidth = g2.getFontMetrics().stringWidth(labelText);
            g2.setColor(TEXT_MUTED);
            g2.drawString(labelText, x - labelWidth / 2, chartBottom + 20);
        }

        g2.dispose();
        return image;
    }

    private static void drawSeries(Graphics2D g2,
                                   Rectangle area,
                                   List<ClimateMeasurement> measurements,
                                   boolean temperature,
                                   String label,
                                   Color lineColor,
                                   Color fillColor) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (ClimateMeasurement measurement : measurements) {
            double value = temperature ? measurement.getTemperature() : measurement.getHumidity();
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        if (Math.abs(max - min) < 0.0001d) {
            min -= 1d;
            max += 1d;
        } else {
            double padding = Math.max(0.8d, (max - min) * 0.12d);
            min -= padding;
            max += padding;
        }

        g2.setColor(new Color(246, 249, 252));
        g2.fillRoundRect(area.x, area.y, area.width, area.height, 18, 18);
        g2.setColor(new Color(214, 224, 236));
        g2.drawRoundRect(area.x, area.y, area.width, area.height, 18, 18);

        g2.setColor(TEXT_PRIMARY);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.drawString(label, area.x + 14, area.y + 22);

        int plotLeft = area.x + 64;
        int plotTop = area.y + 30;
        int plotWidth = area.width - 84;
        int plotHeight = area.height - 52;
        int plotBottom = plotTop + plotHeight;

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        for (int tick = 0; tick <= 4; tick++) {
            int y = plotTop + Math.round(plotHeight * (tick / 4f));
            double tickValue = max - ((max - min) * tick / 4d);
            g2.setColor(GRID);
            g2.drawLine(plotLeft, y, plotLeft + plotWidth, y);
            g2.setColor(TEXT_MUTED);
            g2.drawString(String.format(java.util.Locale.US, "%.1f", tickValue), area.x + 10, y + 4);
        }

        Path2D fillPath = new Path2D.Double();
        Path2D linePath = new Path2D.Double();

        for (int index = 0; index < measurements.size(); index++) {
            ClimateMeasurement measurement = measurements.get(index);
            double value = temperature ? measurement.getTemperature() : measurement.getHumidity();
            double normalized = (value - min) / (max - min);
            int x = plotLeft + (measurements.size() == 1 ? plotWidth / 2 : Math.round(plotWidth * (index / (float) (measurements.size() - 1))));
            int y = plotBottom - Math.round((float) (normalized * plotHeight));

            if (index == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, plotBottom);
                fillPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }

        int lastX = plotLeft + (measurements.size() == 1 ? plotWidth / 2 : plotWidth);
        fillPath.lineTo(lastX, plotBottom);
        fillPath.closePath();

        g2.setColor(fillColor);
        g2.fill(fillPath);
        g2.setColor(lineColor);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(linePath);

        g2.setColor(TEXT_MUTED);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        int maxLabels = Math.min(6, measurements.size());
        for (int labelIndex = 0; labelIndex < maxLabels; labelIndex++) {
            int sampleIndex = measurements.size() == 1
                    ? 0
                    : Math.round(labelIndex * (measurements.size() - 1f) / Math.max(1, maxLabels - 1));
            ClimateMeasurement measurement = measurements.get(sampleIndex);
            int x = plotLeft + (measurements.size() == 1 ? plotWidth / 2 : Math.round(plotWidth * (sampleIndex / (float) (measurements.size() - 1))));
            String labelText = formatter.format(measurement.getMeasurementDate().atTime(measurement.getMeasurementTime()));
            g2.drawString(labelText, x - 18, plotBottom + 18);
        }
    }

    private static double[] paddedRange(List<ClimateMeasurement> measurements, boolean temperature) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (ClimateMeasurement measurement : measurements) {
            double value = temperature ? measurement.getTemperature() : measurement.getHumidity();
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        if (Math.abs(max - min) < 0.0001d) {
            min -= 1d;
            max += 1d;
        } else {
            double padding = Math.max(0.8d, (max - min) * 0.12d);
            min -= padding;
            max += padding;
        }
        return new double[]{min, max};
    }
}
