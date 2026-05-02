package pl.emcmanagement.dao;

import pl.emcmanagement.database.DbConnection;
import pl.emcmanagement.enums.ObservedFunctionalClass;
import pl.emcmanagement.enums.TestStatus;
import pl.emcmanagement.util.JdbcMappers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DutTestResultDao {
    private final WorkflowSyncDao workflowSyncDao = new WorkflowSyncDao();

    public List<DutResultRow> findResultsByStepId(int legTestStepId) {
        String sql = """
                SELECT
                    ds.id AS dut_sample_id,
                    ds.sample_code,
                    ds.serial_number,
                    COALESCE(dtr.observed_functional_class, 'CLASS_A') AS observed_functional_class,
                    COALESCE(dtr.result_status, 'NOT_STARTED') AS result_status,
                    dtr.execution_date,
                    dtr.comment
                FROM (
                    SELECT sda.dut_sample_id
                    FROM step_dut_assignments sda
                    WHERE sda.leg_test_step_id = ?
                    UNION
                    SELECT lda.dut_sample_id
                    FROM leg_test_steps lts
                    JOIN leg_dut_assignments lda ON lda.leg_id = lts.leg_id
                    WHERE lts.id = ?
                      AND NOT EXISTS (
                          SELECT 1
                          FROM step_dut_assignments sda_check
                          WHERE sda_check.leg_test_step_id = ?
                      )
                ) assigned_duts
                JOIN dut_samples ds ON ds.id = assigned_duts.dut_sample_id
                LEFT JOIN dut_test_results dtr
                    ON dtr.dut_sample_id = ds.id
                   AND dtr.leg_test_step_id = ?
                ORDER BY ds.sample_code
                """;

        List<DutResultRow> rows = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, legTestStepId);
            statement.setInt(2, legTestStepId);
            statement.setInt(3, legTestStepId);
            statement.setInt(4, legTestStepId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    DutResultRow row = new DutResultRow();
                    row.setDutSampleId(resultSet.getInt("dut_sample_id"));
                    row.setSampleCode(resultSet.getString("sample_code"));
                    row.setSerialNumber(resultSet.getString("serial_number"));
                    row.setObservedFunctionalClass(ObservedFunctionalClass.valueOf(resultSet.getString("observed_functional_class")));
                    row.setResultStatus(TestStatus.valueOf(resultSet.getString("result_status")));
                    row.setExecutionDate(JdbcMappers.toLocalDate(resultSet.getDate("execution_date")));
                    row.setComment(resultSet.getString("comment"));
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas pobierania wynikow DUT dla kroku testowego.", e);
        }

        return rows;
    }

    public void upsertResults(int legTestStepId, List<DutResultRow> rows) {
        String sql = """
                INSERT INTO dut_test_results (
                    dut_sample_id, leg_test_step_id, observed_functional_class, result_status, execution_date, comment
                )
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    observed_functional_class = VALUES(observed_functional_class),
                    result_status = VALUES(result_status),
                    execution_date = VALUES(execution_date),
                    comment = VALUES(comment)
                """;

        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (DutResultRow row : rows) {
                    statement.setInt(1, row.getDutSampleId());
                    statement.setInt(2, legTestStepId);
                    statement.setString(3, row.getObservedFunctionalClass().name());
                    statement.setString(4, row.getResultStatus().name());
                    statement.setDate(5, JdbcMappers.toSqlDate(row.getExecutionDate()));
                    statement.setString(6, row.getComment());
                    statement.addBatch();
                }
                statement.executeBatch();
                workflowSyncDao.syncAfterDutResultsChange(connection, legTestStepId);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas zapisu wynikow DUT.", e);
        }
    }
}
