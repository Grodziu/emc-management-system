package pl.emcmanagement.dao;

import pl.emcmanagement.database.DbConnection;
import pl.emcmanagement.model.MeasurementEquipment;
import pl.emcmanagement.util.JdbcMappers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MeasurementEquipmentDao {
    public List<MeasurementEquipment> findByLegId(int legId) {
        String sql = """
                SELECT
                    me.id,
                    me.equipment_code,
                    me.equipment_name,
                    me.category,
                    me.manufacturer,
                    me.model,
                    me.serial_number,
                    me.calibration_valid_until,
                    me.lab_owned,
                    me.location,
                    me.notes,
                    me.climate_sensor_code,
                    GROUP_CONCAT(
                        DISTINCT CONCAT(lts.step_order, '. ', lts.step_name)
                        ORDER BY lts.step_order
                        SEPARATOR ', '
                    ) AS assigned_steps_summary,
                    MIN(COALESCE(lts.start_date, pl.start_date)) AS reserved_from,
                    MAX(COALESCE(lts.end_date, pl.end_date)) AS reserved_to
                FROM step_equipment_assignments sea
                JOIN leg_test_steps lts ON lts.id = sea.leg_test_step_id
                JOIN project_legs pl ON pl.id = lts.leg_id
                JOIN measurement_equipment me ON me.id = sea.equipment_id
                WHERE lts.leg_id = ?
                GROUP BY
                    me.id,
                    me.equipment_code,
                    me.equipment_name,
                    me.category,
                    me.manufacturer,
                    me.model,
                    me.serial_number,
                    me.calibration_valid_until,
                    me.location,
                    me.notes
                ORDER BY me.category, me.equipment_code
                """;

        List<MeasurementEquipment> equipment = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, legId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    MeasurementEquipment item = mapEquipment(resultSet);
                    item.setAssignedStepsSummary(resultSet.getString("assigned_steps_summary"));
                    item.setReservedFrom(JdbcMappers.toLocalDate(resultSet.getDate("reserved_from")));
                    item.setReservedTo(JdbcMappers.toLocalDate(resultSet.getDate("reserved_to")));
                    equipment.add(item);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas pobierania sprzetu pomiarowego LEGu.", e);
        }
        return equipment;
    }

    public List<MeasurementEquipment> findByStepId(int stepId) {
        String sql = """
                SELECT
                    me.id,
                    me.equipment_code,
                    me.equipment_name,
                    me.category,
                    me.manufacturer,
                    me.model,
                    me.serial_number,
                    me.calibration_valid_until,
                    me.lab_owned,
                    me.location,
                    me.notes,
                    me.climate_sensor_code,
                    COALESCE(lts.start_date, pl.start_date) AS reserved_from,
                    COALESCE(lts.end_date, pl.end_date) AS reserved_to
                FROM step_equipment_assignments sea
                JOIN leg_test_steps lts ON lts.id = sea.leg_test_step_id
                JOIN project_legs pl ON pl.id = lts.leg_id
                JOIN measurement_equipment me ON me.id = sea.equipment_id
                WHERE sea.leg_test_step_id = ?
                ORDER BY me.category, me.equipment_code
                """;

        List<MeasurementEquipment> equipment = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stepId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    MeasurementEquipment item = mapEquipment(resultSet);
                    item.setReservedFrom(JdbcMappers.toLocalDate(resultSet.getDate("reserved_from")));
                    item.setReservedTo(JdbcMappers.toLocalDate(resultSet.getDate("reserved_to")));
                    equipment.add(item);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas pobierania sprzetu przypisanego do testu.", e);
        }
        return equipment;
    }

    public List<MeasurementEquipment> findAll() {
        String sql = """
                SELECT
                    id,
                    equipment_code,
                    equipment_name,
                    category,
                    manufacturer,
                    model,
                    serial_number,
                    calibration_valid_until,
                    lab_owned,
                    location,
                    notes,
                    climate_sensor_code
                FROM measurement_equipment
                ORDER BY category, equipment_code
                """;

        List<MeasurementEquipment> equipment = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                equipment.add(mapEquipment(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas pobierania katalogu sprzetu.", e);
        }
        return equipment;
    }

    public List<MeasurementEquipment> findAvailableForStep(int stepId, LocalDate reservationStart, LocalDate reservationEnd, String searchTerm) {
        String sql = """
                SELECT
                    me.id,
                    me.equipment_code,
                    me.equipment_name,
                    me.category,
                    me.manufacturer,
                    me.model,
                    me.serial_number,
                    me.calibration_valid_until,
                    me.lab_owned,
                    me.location,
                    me.notes,
                    me.climate_sensor_code
                FROM measurement_equipment me
                WHERE (? IS NULL
                       OR UPPER(me.equipment_code) LIKE ?
                       OR UPPER(me.equipment_name) LIKE ?
                       OR UPPER(COALESCE(me.category, '')) LIKE ?
                       OR UPPER(COALESCE(me.manufacturer, '')) LIKE ?
                       OR UPPER(COALESCE(me.model, '')) LIKE ?)
                ORDER BY me.category, me.equipment_code
                """;

        String searchPattern = normalizeSearchPattern(searchTerm);
        List<MeasurementEquipment> equipment = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);
            statement.setString(3, searchPattern);
            statement.setString(4, searchPattern);
            statement.setString(5, searchPattern);
            statement.setString(6, searchPattern);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    equipment.add(mapEquipment(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas wyszukiwania dostepnego sprzetu.", e);
        }
        return equipment;
    }

    public MeasurementEquipment findAvailableByIdentifier(int stepId, LocalDate reservationStart, LocalDate reservationEnd, String identifier) {
        String trimmed = identifier == null ? "" : identifier.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        MeasurementEquipment equipment = findByIdentifier(trimmed);
        if (equipment == null) {
            return null;
        }
        return findConflictsForStep(stepId, equipment.getId(), reservationStart, reservationEnd).isEmpty() ? equipment : null;
    }

    public MeasurementEquipment findByIdentifier(String identifier) {
        String trimmed = identifier == null ? "" : identifier.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String sql = """
                SELECT
                    id,
                    equipment_code,
                    equipment_name,
                    category,
                    manufacturer,
                    model,
                    serial_number,
                    calibration_valid_until,
                    lab_owned,
                    location,
                    notes,
                    climate_sensor_code
                FROM measurement_equipment
                WHERE CAST(id AS CHAR) = ? OR UPPER(equipment_code) = ?
                ORDER BY equipment_code
                LIMIT 1
                """;

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, trimmed);
            statement.setString(2, trimmed.toUpperCase());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapEquipment(resultSet);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas wyszukiwania sprzetu po identyfikatorze.", e);
        }
        return null;
    }

    public List<String> findConflictsForStep(int stepId, int equipmentId, LocalDate reservationStart, LocalDate reservationEnd) {
        String sql = """
                SELECT
                    p.ewr_number,
                    pl.leg_code,
                    other_step.step_order,
                    other_step.step_name,
                    COALESCE(other_step.start_date, pl.start_date) AS reserved_from,
                    COALESCE(other_step.end_date, pl.end_date) AS reserved_to
                FROM step_equipment_assignments sea
                JOIN leg_test_steps other_step ON other_step.id = sea.leg_test_step_id
                JOIN project_legs pl ON pl.id = other_step.leg_id
                JOIN projects p ON p.id = pl.project_id
                WHERE sea.equipment_id = ?
                  AND other_step.id <> ?
                  AND other_step.leg_id <> (SELECT leg_id FROM leg_test_steps WHERE id = ?)
                  AND COALESCE(other_step.start_date, pl.start_date) IS NOT NULL
                  AND COALESCE(other_step.end_date, pl.end_date) IS NOT NULL
                  AND COALESCE(other_step.start_date, pl.start_date) <= ?
                  AND COALESCE(other_step.end_date, pl.end_date) >= ?
                ORDER BY reserved_from, p.ewr_number, pl.leg_code, other_step.step_order
                """;

        List<String> conflicts = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, equipmentId);
            statement.setInt(2, stepId);
            statement.setInt(3, stepId);
            statement.setDate(4, JdbcMappers.toSqlDate(reservationEnd));
            statement.setDate(5, JdbcMappers.toSqlDate(reservationStart));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    conflicts.add(
                            resultSet.getString("ewr_number")
                                    + " | " + resultSet.getString("leg_code")
                                    + " | " + resultSet.getInt("step_order") + ". " + resultSet.getString("step_name")
                                    + " | " + resultSet.getDate("reserved_from")
                                    + " - " + resultSet.getDate("reserved_to")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas sprawdzania rezerwacji sprzetu.", e);
        }
        return conflicts;
    }

    public int insertEquipment(MeasurementEquipment equipment) {
        String sql = """
                INSERT INTO measurement_equipment (
                    equipment_code, equipment_name, category, manufacturer, model,
                    serial_number, calibration_valid_until, lab_owned, location, notes, climate_sensor_code
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, equipment.getEquipmentCode());
            statement.setString(2, equipment.getEquipmentName());
            statement.setString(3, equipment.getCategory());
            statement.setString(4, equipment.getManufacturer());
            statement.setString(5, equipment.getModel());
            statement.setString(6, equipment.getSerialNumber());
            statement.setDate(7, JdbcMappers.toSqlDate(equipment.getCalibrationValidUntil()));
            statement.setString(8, equipment.isLabOwned() ? "YES" : "NO");
            statement.setString(9, equipment.getLocation());
            statement.setString(10, equipment.getNotes());
            statement.setString(11, equipment.getClimateSensorCode());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas dodawania sprzetu pomiarowego.", e);
        }

        throw new IllegalStateException("Nie udalo sie pobrac ID nowego sprzetu.");
    }

    public void assignToStep(int stepId, int equipmentId, LocalDate reservationStart, LocalDate reservationEnd) {
        List<String> conflicts = findConflictsForStep(stepId, equipmentId, reservationStart, reservationEnd);
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Sprzet jest juz zarezerwowany w tym czasie:\n- " + String.join("\n- ", conflicts));
        }

        String sql = "INSERT IGNORE INTO step_equipment_assignments (leg_test_step_id, equipment_id) VALUES (?, ?)";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stepId);
            statement.setInt(2, equipmentId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas przypisywania sprzetu do testu.", e);
        }
    }

    public void removeFromStep(int stepId, int equipmentId) {
        String sql = "DELETE FROM step_equipment_assignments WHERE leg_test_step_id = ? AND equipment_id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stepId);
            statement.setInt(2, equipmentId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas usuwania sprzetu z testu.", e);
        }
    }

    public boolean isAssignedToStep(int stepId, int equipmentId) {
        String sql = "SELECT 1 FROM step_equipment_assignments WHERE leg_test_step_id = ? AND equipment_id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stepId);
            statement.setInt(2, equipmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas sprawdzania przypisania sprzetu do testu.", e);
        }
    }

    public void updateEquipment(MeasurementEquipment equipment) {
        String sql = """
                UPDATE measurement_equipment
                SET equipment_code = ?, equipment_name = ?, category = ?, manufacturer = ?, model = ?,
                    serial_number = ?, calibration_valid_until = ?, lab_owned = ?, location = ?, notes = ?, climate_sensor_code = ?
                WHERE id = ?
                """;

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, equipment.getEquipmentCode());
            statement.setString(2, equipment.getEquipmentName());
            statement.setString(3, equipment.getCategory());
            statement.setString(4, equipment.getManufacturer());
            statement.setString(5, equipment.getModel());
            statement.setString(6, equipment.getSerialNumber());
            statement.setDate(7, JdbcMappers.toSqlDate(equipment.getCalibrationValidUntil()));
            statement.setString(8, equipment.isLabOwned() ? "YES" : "NO");
            statement.setString(9, equipment.getLocation());
            statement.setString(10, equipment.getNotes());
            statement.setString(11, equipment.getClimateSensorCode());
            statement.setInt(12, equipment.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas aktualizacji sprzetu.", e);
        }
    }

    public void deleteEquipment(int equipmentId) {
        String sql = "DELETE FROM measurement_equipment WHERE id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, equipmentId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas usuwania sprzetu z bazy.", e);
        }
    }

    private String normalizeSearchPattern(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return null;
        }
        return "%" + searchTerm.trim().toUpperCase() + "%";
    }

    private MeasurementEquipment mapEquipment(ResultSet resultSet) throws SQLException {
        MeasurementEquipment equipment = new MeasurementEquipment();
        equipment.setId(resultSet.getInt("id"));
        equipment.setEquipmentCode(resultSet.getString("equipment_code"));
        equipment.setEquipmentName(resultSet.getString("equipment_name"));
        equipment.setCategory(resultSet.getString("category"));
        equipment.setManufacturer(resultSet.getString("manufacturer"));
        equipment.setModel(resultSet.getString("model"));
        equipment.setSerialNumber(resultSet.getString("serial_number"));
        equipment.setCalibrationValidUntil(JdbcMappers.toLocalDate(resultSet.getDate("calibration_valid_until")));
        equipment.setLabOwned("YES".equalsIgnoreCase(resultSet.getString("lab_owned")));
        equipment.setLocation(resultSet.getString("location"));
        equipment.setNotes(resultSet.getString("notes"));
        equipment.setClimateSensorCode(resultSet.getString("climate_sensor_code"));
        return equipment;
    }
}
