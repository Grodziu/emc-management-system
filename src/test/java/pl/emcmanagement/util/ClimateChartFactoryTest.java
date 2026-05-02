package pl.emcmanagement.util;

import org.junit.jupiter.api.Test;
import pl.emcmanagement.model.ClimateMeasurement;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClimateChartFactoryTest {

    @Test
    void createChartImageCreatesPlaceholderForEmptyMeasurements() {
        BufferedImage chart = ClimateChartFactory.createChartImage("Empty climate chart", List.of(), 800, 420);

        assertEquals(800, chart.getWidth());
        assertEquals(420, chart.getHeight());
        assertTrue(containsNonWhitePixel(chart));
    }

    @Test
    void createCombinedChartImageRendersTemperatureAndHumiditySeries() {
        List<ClimateMeasurement> measurements = List.of(
                measurement(LocalDate.of(2026, 4, 1), LocalTime.of(10, 0), 22.1, 43.4),
                measurement(LocalDate.of(2026, 4, 1), LocalTime.of(8, 0), 21.6, 45.8),
                measurement(LocalDate.of(2026, 4, 1), LocalTime.of(12, 30), 23.0, 41.9),
                measurement(LocalDate.of(2026, 4, 1), LocalTime.of(14, 0), 24.2, 40.7)
        );

        BufferedImage chart = ClimateChartFactory.createCombinedChartImage("Combined climate chart", measurements, 920, 460);

        assertEquals(920, chart.getWidth());
        assertEquals(460, chart.getHeight());
        assertTrue(containsNonWhitePixel(chart));
        assertTrue(containsPredominantlyRedPixel(chart));
        assertTrue(containsPredominantlyBluePixel(chart));
    }

    private static ClimateMeasurement measurement(LocalDate date, LocalTime time, double temperature, double humidity) {
        ClimateMeasurement measurement = new ClimateMeasurement();
        measurement.setMeasurementDate(date);
        measurement.setMeasurementTime(time);
        measurement.setTemperature(temperature);
        measurement.setHumidity(humidity);
        return measurement;
    }

    private static boolean containsNonWhitePixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y += 4) {
            for (int x = 0; x < image.getWidth(); x += 4) {
                if ((image.getRGB(x, y) & 0x00FF_FFFF) != 0x00FF_FFFF) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsPredominantlyRedPixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y += 2) {
            for (int x = 0; x < image.getWidth(); x += 2) {
                Color color = new Color(image.getRGB(x, y), true);
                if (color.getAlpha() > 0 && color.getRed() > 150 && color.getBlue() < 140) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsPredominantlyBluePixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y += 2) {
            for (int x = 0; x < image.getWidth(); x += 2) {
                Color color = new Color(image.getRGB(x, y), true);
                if (color.getAlpha() > 0 && color.getBlue() > 140 && color.getRed() < 140) {
                    return true;
                }
            }
        }
        return false;
    }
}
