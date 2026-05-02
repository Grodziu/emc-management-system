package pl.emcmanagement.dao;

import pl.emcmanagement.database.DbConnection;
import pl.emcmanagement.model.ClimateDataset;
import pl.emcmanagement.model.ClimateMeasurement;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClimateLogDao {
    private static final Pattern FILENAME_PATTERN = Pattern.compile("^(?<sensor>.+?)_(?<year>\\d{4})_CW(?<week>\\d{1,2})_(?<room>[^.]+)\\.txt$", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm[:ss]");

    public void importFromFile(Path filePath) {
        ClimateFileMetadata metadata = parseFilename(filePath.getFileName().toString());
        String fileContent;
        try {
            fileContent = Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Nie udalo sie odczytac pliku klimatycznego: " + filePath, e);
        }

        String sql = """
                INSERT INTO climate_log_files (
                    sensor_code, year_number, calendar_week, room_code, source_filename, file_content
                ) VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    file_content = VALUES(file_content),
                    imported_at = CURRENT_TIMESTAMP
                """;

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, metadata.sensorCode());
            statement.setInt(2, metadata.yearNumber());
            statement.setInt(3, metadata.calendarWeek());
            statement.setString(4, metadata.roomCode());
            statement.setString(5, filePath.getFileName().toString());
            statement.setString(6, fileContent);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Nie udalo sie zapisac pliku klimatycznego do bazy.", e);
        }
    }

    public ClimateDataset findMeasurements(String roomCode, LocalDate startDate, LocalDate endDate) {
        return findMeasurements(roomCode, null, startDate, endDate);
    }

    public ClimateDataset findMeasurements(String roomCode, String sensorCode, LocalDate startDate, LocalDate endDate) {
        ClimateDataset dataset = new ClimateDataset();
        String normalizedRoomCode = normalizeRoomCode(roomCode);
        String normalizedSensorCode = normalizeSensorCode(sensorCode);
        if ((normalizedRoomCode == null && normalizedSensorCode == null)
                || startDate == null
                || endDate == null
                || endDate.isBefore(startDate)) {
            return dataset;
        }
        dataset.setRoomCode(normalizedRoomCode);
        dataset.setSensorCode(normalizedSensorCode);
        dataset.setSourceDescription(normalizedSensorCode != null
                ? "Climate chamber sensor " + normalizedSensorCode
                : "Room log " + normalizedRoomCode);

        List<WeekKey> weekKeys = resolveWeekKeys(startDate, endDate);
        if (weekKeys.isEmpty()) {
            return dataset;
        }

        StringBuilder sql = new StringBuilder(normalizedSensorCode != null
                ? """
                SELECT source_filename, file_content, sensor_code, room_code
                FROM climate_log_files
                WHERE UPPER(sensor_code) = ?
                  AND (
                """
                : """
                SELECT source_filename, file_content
                FROM climate_log_files
                WHERE UPPER(room_code) = ?
                  AND (
                """);
        for (int index = 0; index < weekKeys.size(); index++) {
            if (index > 0) {
                sql.append(" OR ");
            }
            sql.append("(year_number = ? AND calendar_week = ?)");
        }
        sql.append(") ORDER BY year_number, calendar_week, source_filename");

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameterIndex = 1;
            statement.setString(parameterIndex++, normalizedSensorCode != null ? normalizedSensorCode : normalizedRoomCode);
            for (WeekKey weekKey : weekKeys) {
                statement.setInt(parameterIndex++, weekKey.yearNumber());
                statement.setInt(parameterIndex++, weekKey.calendarWeek());
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String filename = resultSet.getString("source_filename");
                    dataset.getSourceFilenames().add(filename);
                    parseMeasurements(
                            resultSet.getString("file_content"),
                            normalizedRoomCode,
                            normalizedSensorCode,
                            filename,
                            startDate,
                            endDate,
                            dataset.getMeasurements()
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Nie udalo sie pobrac warunkow klimatycznych z bazy.", e);
        }

        return dataset;
    }

    public int countImportedFiles() {
        String sql = "SELECT COUNT(*) FROM climate_log_files";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Nie udalo sie policzyc plikow klimatycznych.", e);
        }
        return 0;
    }

    private void parseMeasurements(String fileContent,
                                   String roomCode,
                                   String sensorCode,
                                   String sourceFilename,
                                   LocalDate startDate,
                                   LocalDate endDate,
                                   List<ClimateMeasurement> target) {
        if (fileContent == null || fileContent.isBlank()) {
            return;
        }

        for (String line : fileContent.lines().toList()) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\t");
            if (parts.length < 4) {
                continue;
            }
            try {
                LocalDate date = LocalDate.parse(parts[0].trim());
                if (date.isBefore(startDate) || date.isAfter(endDate)) {
                    continue;
                }
                DayOfWeek dayOfWeek = date.getDayOfWeek();
                if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                    continue;
                }

                LocalTime time = parseTime(parts[1].trim());
                if (time.isBefore(LocalTime.of(6, 0)) || time.isAfter(LocalTime.of(22, 0))) {
                    continue;
                }

                ClimateMeasurement measurement = new ClimateMeasurement();
                measurement.setMeasurementDate(date);
                measurement.setMeasurementTime(time);
                measurement.setHumidity(Double.parseDouble(parts[2].trim()));
                measurement.setTemperature(Double.parseDouble(parts[3].trim()));
                measurement.setRoomCode(roomCode);
                measurement.setSensorCode(sensorCode);
                measurement.setSourceFilename(sourceFilename);
                target.add(measurement);
            } catch (Exception ignored) {
            }
        }
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value);
        } catch (Exception exception) {
            return LocalTime.parse(value, TIME_FORMATTER);
        }
    }

    private List<WeekKey> resolveWeekKeys(LocalDate startDate, LocalDate endDate) {
        WeekFields weekFields = WeekFields.ISO;
        Set<WeekKey> weekKeys = new LinkedHashSet<>();
        LocalDate pointer = startDate;
        while (!pointer.isAfter(endDate)) {
            weekKeys.add(new WeekKey(pointer.get(weekFields.weekBasedYear()), pointer.get(weekFields.weekOfWeekBasedYear())));
            pointer = pointer.plusDays(1);
        }
        return new ArrayList<>(weekKeys);
    }

    private ClimateFileMetadata parseFilename(String filename) {
        Matcher matcher = FILENAME_PATTERN.matcher(filename);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Niepoprawna nazwa pliku klimatycznego: " + filename);
        }
        return new ClimateFileMetadata(
                matcher.group("sensor"),
                Integer.parseInt(matcher.group("year")),
                Integer.parseInt(matcher.group("week")),
                normalizeRoomCode(matcher.group("room"))
        );
    }

    private String normalizeRoomCode(String roomCode) {
        return roomCode == null ? null : roomCode.replace(" ", "").trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSensorCode(String sensorCode) {
        if (sensorCode == null) {
            return null;
        }
        String normalized = sensorCode.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private record ClimateFileMetadata(String sensorCode, int yearNumber, int calendarWeek, String roomCode) {
    }

    private record WeekKey(int yearNumber, int calendarWeek) {
    }
}
