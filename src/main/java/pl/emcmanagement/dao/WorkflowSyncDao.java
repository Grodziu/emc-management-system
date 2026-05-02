package pl.emcmanagement.dao;

import pl.emcmanagement.enums.TestStatus;
import pl.emcmanagement.util.JdbcMappers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class WorkflowSyncDao {
    public void syncAfterStepChange(Connection connection, int legId) throws SQLException {
        Integer projectId = syncLegTimeline(connection, legId);
        if (projectId != null) {
            syncProjectTimeline(connection, projectId);
        }
    }

    public void syncAfterDutResultsChange(Connection connection, int stepId) throws SQLException {
        Integer legId = syncStepStatusFromAssignments(connection, stepId);
        if (legId != null) {
            syncAfterStepChange(connection, legId);
        }
    }

    public void syncAfterLegDutAssignmentChange(Connection connection, int legId) throws SQLException {
        String sql = "SELECT id FROM leg_test_steps WHERE leg_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, legId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    syncStepStatusFromAssignments(connection, resultSet.getInt("id"));
                }
            }
        }
        syncAfterStepChange(connection, legId);
    }

    Integer syncStepStatusFromAssignments(Connection connection, int stepId) throws SQLException {
        String summarySql = """
                SELECT
                    lts.leg_id,
                    COUNT(assigned_duts.dut_sample_id) AS dut_count,
                    SUM(COALESCE(dtr.result_status, 'NOT_STARTED') = 'NOT_STARTED') AS not_started_count,
                    SUM(COALESCE(dtr.result_status, 'NOT_STARTED') = 'ONGOING') AS ongoing_count,
                    SUM(COALESCE(dtr.result_status, 'NOT_STARTED') = 'DATA_IN_ANALYSIS') AS data_in_analysis_count,
                    SUM(COALESCE(dtr.result_status, 'NOT_STARTED') = 'PASSED') AS passed_count,
                    SUM(COALESCE(dtr.result_status, 'NOT_STARTED') = 'FAILED') AS failed_count
                FROM leg_test_steps lts
                LEFT JOIN (
                    SELECT sda.dut_sample_id
                    FROM step_dut_assignments sda
                    WHERE sda.leg_test_step_id = ?
                    UNION
                    SELECT lda.dut_sample_id
                    FROM leg_test_steps lts_inherited
                    JOIN leg_dut_assignments lda ON lda.leg_id = lts_inherited.leg_id
                    WHERE lts_inherited.id = ?
                      AND NOT EXISTS (
                          SELECT 1
                          FROM step_dut_assignments sda_check
                          WHERE sda_check.leg_test_step_id = ?
                      )
                ) assigned_duts ON TRUE
                LEFT JOIN dut_test_results dtr
                    ON dtr.leg_test_step_id = lts.id
                   AND dtr.dut_sample_id = assigned_duts.dut_sample_id
                WHERE lts.id = ?
                GROUP BY lts.leg_id
                """;
        String updateSql = "UPDATE leg_test_steps SET status = ? WHERE id = ?";

        try (PreparedStatement summaryStatement = connection.prepareStatement(summarySql)) {
            summaryStatement.setInt(1, stepId);
            summaryStatement.setInt(2, stepId);
            summaryStatement.setInt(3, stepId);
            summaryStatement.setInt(4, stepId);

            try (ResultSet resultSet = summaryStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                int legId = resultSet.getInt("leg_id");
                int dutCount = resultSet.getInt("dut_count");
                int notStartedCount = resultSet.getInt("not_started_count");
                int ongoingCount = resultSet.getInt("ongoing_count");
                int dataInAnalysisCount = resultSet.getInt("data_in_analysis_count");
                int passedCount = resultSet.getInt("passed_count");
                int failedCount = resultSet.getInt("failed_count");

                TestStatus derivedStatus = deriveStepStatus(
                        dutCount,
                        notStartedCount,
                        ongoingCount,
                        dataInAnalysisCount,
                        passedCount,
                        failedCount
                );

                try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                    updateStatement.setString(1, derivedStatus.name());
                    updateStatement.setInt(2, stepId);
                    updateStatement.executeUpdate();
                }
                return legId;
            }
        }
    }

    private Integer syncLegTimeline(Connection connection, int legId) throws SQLException {
        String summarySql = """
                SELECT
                    pl.project_id,
                    COUNT(lts.id) AS step_count,
                    SUM(CASE WHEN lts.status IN ('PASSED', 'FAILED') THEN 1 ELSE 0 END) AS finished_step_count,
                    SUM(CASE WHEN lts.status IN ('PASSED', 'FAILED') AND lts.end_date IS NOT NULL THEN 1 ELSE 0 END) AS finished_with_end_count,
                    MIN(CASE WHEN lts.status <> 'NOT_STARTED' THEN lts.start_date END) AS earliest_actual_start,
                    MAX(CASE WHEN lts.status IN ('PASSED', 'FAILED') THEN lts.end_date END) AS latest_finished_end
                FROM project_legs pl
                LEFT JOIN leg_test_steps lts ON lts.leg_id = pl.id
                WHERE pl.id = ?
                GROUP BY pl.project_id
                """;
        String updateSql = "UPDATE project_legs SET start_date = ?, end_date = ? WHERE id = ?";

        try (PreparedStatement summaryStatement = connection.prepareStatement(summarySql)) {
            summaryStatement.setInt(1, legId);
            try (ResultSet resultSet = summaryStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                int projectId = resultSet.getInt("project_id");
                int stepCount = resultSet.getInt("step_count");
                int finishedStepCount = resultSet.getInt("finished_step_count");
                int finishedWithEndCount = resultSet.getInt("finished_with_end_count");

                LocalDate earliestStart = JdbcMappers.toLocalDate(resultSet.getDate("earliest_actual_start"));
                LocalDate latestEnd = JdbcMappers.toLocalDate(resultSet.getDate("latest_finished_end"));
                LocalDate syncedEnd = stepCount > 0 && finishedStepCount == stepCount && finishedWithEndCount == stepCount
                        ? latestEnd
                        : null;

                try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                    updateStatement.setDate(1, JdbcMappers.toSqlDate(earliestStart));
                    updateStatement.setDate(2, JdbcMappers.toSqlDate(syncedEnd));
                    updateStatement.setInt(3, legId);
                    updateStatement.executeUpdate();
                }
                return projectId;
            }
        }
    }

    private void syncProjectTimeline(Connection connection, int projectId) throws SQLException {
        String summarySql = """
                SELECT
                    COUNT(pl.id) AS leg_count,
                    MIN(pl.start_date) AS earliest_start,
                    SUM(CASE WHEN COALESCE(leg_state.all_finished, 0) = 1 THEN 1 ELSE 0 END) AS finished_leg_count,
                    SUM(CASE WHEN pl.end_date IS NOT NULL THEN 1 ELSE 0 END) AS ended_leg_count,
                    MAX(pl.end_date) AS latest_end
                FROM projects p
                LEFT JOIN project_legs pl ON pl.project_id = p.id
                LEFT JOIN (
                    SELECT
                        lts.leg_id,
                        CASE
                            WHEN COUNT(lts.id) > 0
                                 AND SUM(CASE WHEN lts.status IN ('PASSED', 'FAILED') THEN 1 ELSE 0 END) = COUNT(lts.id)
                                THEN 1
                            ELSE 0
                        END AS all_finished
                    FROM leg_test_steps lts
                    GROUP BY lts.leg_id
                ) leg_state ON leg_state.leg_id = pl.id
                WHERE p.id = ?
                GROUP BY p.id
                """;
        String updateSql = "UPDATE projects SET start_date = ?, end_date = ? WHERE id = ?";

        try (PreparedStatement summaryStatement = connection.prepareStatement(summarySql)) {
            summaryStatement.setInt(1, projectId);
            try (ResultSet resultSet = summaryStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return;
                }

                int legCount = resultSet.getInt("leg_count");
                int finishedLegCount = resultSet.getInt("finished_leg_count");
                int endedLegCount = resultSet.getInt("ended_leg_count");
                LocalDate earliestStart = JdbcMappers.toLocalDate(resultSet.getDate("earliest_start"));
                LocalDate latestEnd = JdbcMappers.toLocalDate(resultSet.getDate("latest_end"));

                LocalDate syncedEnd = legCount > 0 && finishedLegCount == legCount && endedLegCount == legCount
                        ? latestEnd
                        : null;

                try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                    updateStatement.setDate(1, JdbcMappers.toSqlDate(earliestStart));
                    updateStatement.setDate(2, JdbcMappers.toSqlDate(syncedEnd));
                    updateStatement.setInt(3, projectId);
                    updateStatement.executeUpdate();
                }
            }
        }
    }

    private TestStatus deriveStepStatus(
            int dutCount,
            int notStartedCount,
            int ongoingCount,
            int dataInAnalysisCount,
            int passedCount,
            int failedCount
    ) {
        if (dutCount == 0 || notStartedCount == dutCount) {
            return TestStatus.NOT_STARTED;
        }
        if (ongoingCount > 0) {
            return TestStatus.ONGOING;
        }
        if (dataInAnalysisCount > 0) {
            return TestStatus.DATA_IN_ANALYSIS;
        }
        if (passedCount + failedCount == dutCount) {
            return failedCount > 0 ? TestStatus.FAILED : TestStatus.PASSED;
        }
        return TestStatus.ONGOING;
    }
}
