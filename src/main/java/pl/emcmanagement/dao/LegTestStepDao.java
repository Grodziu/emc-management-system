package pl.emcmanagement.dao;

import pl.emcmanagement.database.DbConnection;
import pl.emcmanagement.enums.TestStatus;
import pl.emcmanagement.model.LegTestStep;
import pl.emcmanagement.util.JdbcMappers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LegTestStepDao {
    private final WorkflowSyncDao workflowSyncDao = new WorkflowSyncDao();

    public List<LegTestStep> findByLegId(int legId) {
        String sql = """
                SELECT id, leg_id, step_order, step_name, status, start_date, end_date, test_room
                FROM leg_test_steps
                WHERE leg_id = ?
                ORDER BY step_order
                """;

        List<LegTestStep> steps = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, legId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    LegTestStep step = new LegTestStep();
                    step.setId(resultSet.getInt("id"));
                    step.setLegId(resultSet.getInt("leg_id"));
                    step.setStepOrder(resultSet.getInt("step_order"));
                    step.setStepName(resultSet.getString("step_name"));
                    step.setStatus(TestStatus.valueOf(resultSet.getString("status")));
                    step.setStartDate(JdbcMappers.toLocalDate(resultSet.getDate("start_date")));
                    step.setEndDate(JdbcMappers.toLocalDate(resultSet.getDate("end_date")));
                    step.setTestRoom(resultSet.getString("test_room"));
                    steps.add(step);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas pobierania krokow testu.", e);
        }
        return steps;
    }

    public void updateStep(LegTestStep step) {
        updateStepSchedule(step.getId(), step.getLegId(), step.getStartDate(), step.getEndDate(), step.getTestRoom());
    }

    public void updateStepSchedule(int stepId, int legId, java.time.LocalDate startDate, java.time.LocalDate endDate, String testRoom) {
        String updateStepSql = """
                UPDATE leg_test_steps
                SET start_date = ?, end_date = ?, test_room = ?
                WHERE id = ?
                """;

        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement updateStepStatement = connection.prepareStatement(updateStepSql)) {
                updateStepStatement.setDate(1, JdbcMappers.toSqlDate(startDate));
                updateStepStatement.setDate(2, JdbcMappers.toSqlDate(endDate));
                updateStepStatement.setString(3, testRoom);
                updateStepStatement.setInt(4, stepId);
                updateStepStatement.executeUpdate();
                workflowSyncDao.syncAfterStepChange(connection, legId);

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas aktualizacji kroku testowego.", e);
        }
    }

    public void insertSteps(int legId, List<String> stepNames) {
        String sql = "INSERT INTO leg_test_steps (leg_id, step_order, step_name) VALUES (?, ?, ?)";

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int order = 1;
            for (String stepName : stepNames) {
                statement.setInt(1, legId);
                statement.setInt(2, order++);
                statement.setString(3, stepName);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas dodawania krokow testowych.", e);
        }
    }
}
