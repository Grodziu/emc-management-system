package pl.emcmanagement.dao;

import pl.emcmanagement.database.DbConnection;
import pl.emcmanagement.model.EquipmentReservationEntry;
import pl.emcmanagement.model.LabReservationEntry;
import pl.emcmanagement.util.JdbcMappers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationCalendarDao {
    public List<LabReservationEntry> findLabReservations(LocalDate startDate, LocalDate endDate, Integer projectId) {
        String sql = """
                SELECT
                    p.id AS project_id,
                    p.ewr_number,
                    pl.leg_code,
                    lts.step_order,
                    lts.step_name,
                    lts.status,
                    lts.test_room AS room_code,
                    COALESCE(lts.start_date, pl.start_date) AS reserved_from,
                    COALESCE(lts.end_date, pl.end_date) AS reserved_to
                FROM leg_test_steps lts
                JOIN project_legs pl ON pl.id = lts.leg_id
                JOIN projects p ON p.id = pl.project_id
                WHERE lts.test_room IS NOT NULL
                  AND COALESCE(lts.start_date, pl.start_date) IS NOT NULL
                  AND COALESCE(lts.end_date, pl.end_date) IS NOT NULL
                  AND COALESCE(lts.start_date, pl.start_date) <= ?
                  AND COALESCE(lts.end_date, pl.end_date) >= ?
                  AND (? IS NULL OR p.id = ?)
                ORDER BY lts.test_room, reserved_from, p.ewr_number, pl.leg_code, lts.step_order
                """;

        List<LabReservationEntry> entries = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, JdbcMappers.toSqlDate(endDate));
            statement.setDate(2, JdbcMappers.toSqlDate(startDate));
            if (projectId == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, projectId);
                statement.setInt(4, projectId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    LocalDate reservedFrom = JdbcMappers.toLocalDate(resultSet.getDate("reserved_from"));
                    LocalDate reservedTo = JdbcMappers.toLocalDate(resultSet.getDate("reserved_to"));
                    if (reservedFrom == null || reservedTo == null) {
                        continue;
                    }

                    LocalDate effectiveFrom = reservedFrom.isBefore(startDate) ? startDate : reservedFrom;
                    LocalDate effectiveTo = reservedTo.isAfter(endDate) ? endDate : reservedTo;
                    LocalDate pointer = effectiveFrom;
                    while (!pointer.isAfter(effectiveTo)) {
                        LabReservationEntry entry = new LabReservationEntry();
                        entry.setReservationDate(pointer);
                        entry.setRoomCode(resultSet.getString("room_code"));
                        entry.setEwrNumber(resultSet.getString("ewr_number"));
                        entry.setLegCode(resultSet.getString("leg_code"));
                        entry.setStepOrder(resultSet.getInt("step_order"));
                        entry.setStepName(resultSet.getString("step_name"));
                        entry.setStatus(resultSet.getString("status"));
                        entries.add(entry);
                        pointer = pointer.plusDays(1);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Nie udalo sie pobrac rezerwacji laboratoriow.", e);
        }
        return entries;
    }

    public List<EquipmentReservationEntry> findEquipmentReservations(LocalDate startDate, LocalDate endDate, Integer projectId) {
        String sql = """
                SELECT
                    project_id,
                    equipment_code,
                    equipment_name,
                    category,
                    ewr_number,
                    leg_code,
                    step_order,
                    step_name,
                    room_code,
                    reserved_from,
                    reserved_to,
                    status
                FROM vw_equipment_reservations
                WHERE reserved_from <= ?
                  AND reserved_to >= ?
                  AND (? IS NULL OR project_id = ?)
                ORDER BY equipment_code, reserved_from, ewr_number, leg_code, step_order
                """;

        List<EquipmentReservationEntry> entries = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, JdbcMappers.toSqlDate(endDate));
            statement.setDate(2, JdbcMappers.toSqlDate(startDate));
            if (projectId == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, projectId);
                statement.setInt(4, projectId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    EquipmentReservationEntry entry = new EquipmentReservationEntry();
                    entry.setEquipmentCode(resultSet.getString("equipment_code"));
                    entry.setEquipmentName(resultSet.getString("equipment_name"));
                    entry.setCategory(resultSet.getString("category"));
                    entry.setEwrNumber(resultSet.getString("ewr_number"));
                    entry.setLegCode(resultSet.getString("leg_code"));
                    entry.setStepOrder(resultSet.getInt("step_order"));
                    entry.setStepName(resultSet.getString("step_name"));
                    entry.setRoomCode(resultSet.getString("room_code"));
                    entry.setReservedFrom(JdbcMappers.toLocalDate(resultSet.getDate("reserved_from")));
                    entry.setReservedTo(JdbcMappers.toLocalDate(resultSet.getDate("reserved_to")));
                    entry.setStatus(resultSet.getString("status"));
                    entries.add(entry);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Nie udalo sie pobrac rezerwacji sprzetu.", e);
        }
        return entries;
    }
}
