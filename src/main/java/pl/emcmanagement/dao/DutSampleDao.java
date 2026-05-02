package pl.emcmanagement.dao;

import pl.emcmanagement.database.DbConnection;
import pl.emcmanagement.model.DutSample;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DutSampleDao {
    private final WorkflowSyncDao workflowSyncDao = new WorkflowSyncDao();

    public List<DutSample> findByProjectId(int projectId) {
        String sql = """
                SELECT id, project_id, sample_code, serial_number
                FROM dut_samples
                WHERE project_id = ?
                ORDER BY sample_code
                """;

        List<DutSample> samples = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    DutSample sample = new DutSample();
                    sample.setId(resultSet.getInt("id"));
                    sample.setProjectId(resultSet.getInt("project_id"));
                    sample.setSampleCode(resultSet.getString("sample_code"));
                    sample.setSerialNumber(resultSet.getString("serial_number"));
                    samples.add(sample);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas pobierania DUT-ow projektu.", e);
        }
        return samples;
    }

    public List<Integer> findAssignedSampleIdsByLegId(int legId) {
        String sql = """
                SELECT dut_sample_id
                FROM leg_dut_assignments
                WHERE leg_id = ?
                ORDER BY dut_sample_id
                """;

        List<Integer> sampleIds = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, legId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    sampleIds.add(resultSet.getInt("dut_sample_id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas pobierania przypisanych DUT-ow LEGu.", e);
        }
        return sampleIds;
    }

    public boolean hasCustomAssignmentsForStep(int stepId) {
        String sql = "SELECT 1 FROM step_dut_assignments WHERE leg_test_step_id = ? LIMIT 1";

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stepId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas sprawdzania wyjatkow DUT dla testu.", e);
        }
    }

    public List<Integer> findAssignedSampleIdsByStepId(int stepId) {
        String sql = """
                SELECT dut_sample_id
                FROM step_dut_assignments
                WHERE leg_test_step_id = ?
                ORDER BY dut_sample_id
                """;

        List<Integer> sampleIds = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stepId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    sampleIds.add(resultSet.getInt("dut_sample_id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas pobierania DUT-ow przypisanych do testu.", e);
        }
        return sampleIds;
    }

    public int insertSamples(int projectId, List<String> sampleCodes) {
        String sql = "INSERT IGNORE INTO dut_samples (project_id, sample_code, serial_number) VALUES (?, ?, ?)";
        int inserted = 0;

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String sampleCode : sampleCodes) {
                statement.setInt(1, projectId);
                statement.setString(2, sampleCode);
                statement.setString(3, generateSerialNumber(sampleCode));
                statement.addBatch();
            }

            int[] results = statement.executeBatch();
            for (int result : results) {
                if (result > 0) {
                    inserted++;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas dodawania DUT-ow.", e);
        }

        return inserted;
    }

    private String generateSerialNumber(String sampleCode) {
        String cleaned = sampleCode == null ? "" : sampleCode.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (cleaned.length() > 10) {
            cleaned = cleaned.substring(0, 10);
        }
        if (cleaned.length() < 10) {
            cleaned = cleaned + "X".repeat(10 - cleaned.length());
        }
        return "SN" + cleaned;
    }

    public void deleteSamplesFromProject(int projectId, List<Integer> sampleIds) {
        if (sampleIds == null || sampleIds.isEmpty()) {
            return;
        }

        String placeholders = String.join(",", Collections.nCopies(sampleIds.size(), "?"));
        String sql = "DELETE FROM dut_samples WHERE project_id = ? AND id IN (" + placeholders + ")";

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            for (int index = 0; index < sampleIds.size(); index++) {
                statement.setInt(index + 2, sampleIds.get(index));
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas usuwania DUT-ow z projektu.", e);
        }
    }

    public void replaceAssignmentsForLeg(int legId, List<Integer> sampleIds) {
        String deleteAssignmentsSql = "DELETE FROM leg_dut_assignments WHERE leg_id = ?";
        String insertAssignmentSql = "INSERT INTO leg_dut_assignments (leg_id, dut_sample_id) VALUES (?, ?)";

        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement deleteAssignments = connection.prepareStatement(deleteAssignmentsSql);
                 PreparedStatement insertAssignment = connection.prepareStatement(insertAssignmentSql)) {
                deleteResultsForRemovedLegSamples(connection, legId, sampleIds);

                deleteAssignments.setInt(1, legId);
                deleteAssignments.executeUpdate();

                for (Integer sampleId : sampleIds) {
                    insertAssignment.setInt(1, legId);
                    insertAssignment.setInt(2, sampleId);
                    insertAssignment.addBatch();
                }
                insertAssignment.executeBatch();
                workflowSyncDao.syncAfterLegDutAssignmentChange(connection, legId);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas aktualizacji przypisan DUT do LEGu.", e);
        }
    }

    public void replaceAssignmentsForStep(int stepId, List<Integer> sampleIds) {
        String deleteAssignmentsSql = "DELETE FROM step_dut_assignments WHERE leg_test_step_id = ?";
        String insertAssignmentSql = "INSERT INTO step_dut_assignments (leg_test_step_id, dut_sample_id) VALUES (?, ?)";

        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement deleteAssignments = connection.prepareStatement(deleteAssignmentsSql);
                 PreparedStatement insertAssignment = connection.prepareStatement(insertAssignmentSql)) {
                deleteResultsForRemovedStepSamples(connection, stepId, sampleIds);

                deleteAssignments.setInt(1, stepId);
                deleteAssignments.executeUpdate();

                for (Integer sampleId : sampleIds) {
                    insertAssignment.setInt(1, stepId);
                    insertAssignment.setInt(2, sampleId);
                    insertAssignment.addBatch();
                }
                insertAssignment.executeBatch();
                workflowSyncDao.syncAfterDutResultsChange(connection, stepId);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas aktualizacji DUT-ow dla testu.", e);
        }
    }

    public void clearAssignmentsForStep(int stepId) {
        String deleteAssignmentsSql = "DELETE FROM step_dut_assignments WHERE leg_test_step_id = ?";
        String deleteResultsSql = """
                DELETE FROM dut_test_results
                WHERE leg_test_step_id = ?
                  AND dut_sample_id NOT IN (
                      SELECT lda.dut_sample_id
                      FROM leg_test_steps lts
                      JOIN leg_dut_assignments lda ON lda.leg_id = lts.leg_id
                      WHERE lts.id = ?
                  )
                """;

        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement deleteAssignments = connection.prepareStatement(deleteAssignmentsSql);
                 PreparedStatement deleteResults = connection.prepareStatement(deleteResultsSql)) {
                deleteResults.setInt(1, stepId);
                deleteResults.setInt(2, stepId);
                deleteResults.executeUpdate();

                deleteAssignments.setInt(1, stepId);
                deleteAssignments.executeUpdate();
                workflowSyncDao.syncAfterDutResultsChange(connection, stepId);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas przywracania domyslnych DUT-ow z LEGu.", e);
        }
    }

    private void deleteResultsForRemovedLegSamples(Connection connection, int legId, List<Integer> sampleIds) throws SQLException {
        String sql;
        if (sampleIds.isEmpty()) {
            sql = """
                    DELETE dtr
                    FROM dut_test_results dtr
                    JOIN leg_test_steps lts ON lts.id = dtr.leg_test_step_id
                    WHERE lts.leg_id = ?
                    """;
        } else {
            String placeholders = String.join(",", Collections.nCopies(sampleIds.size(), "?"));
            sql = """
                    DELETE dtr
                    FROM dut_test_results dtr
                    JOIN leg_test_steps lts ON lts.id = dtr.leg_test_step_id
                    WHERE lts.leg_id = ?
                      AND dtr.dut_sample_id NOT IN (""" + placeholders + ")";
        }

        try (PreparedStatement deleteResults = connection.prepareStatement(sql)) {
            deleteResults.setInt(1, legId);
            for (int index = 0; index < sampleIds.size(); index++) {
                deleteResults.setInt(index + 2, sampleIds.get(index));
            }
            deleteResults.executeUpdate();
        }
    }

    private void deleteResultsForRemovedStepSamples(Connection connection, int stepId, List<Integer> sampleIds) throws SQLException {
        String sql;
        if (sampleIds.isEmpty()) {
            sql = "DELETE FROM dut_test_results WHERE leg_test_step_id = ?";
        } else {
            String placeholders = String.join(",", Collections.nCopies(sampleIds.size(), "?"));
            sql = "DELETE FROM dut_test_results WHERE leg_test_step_id = ? AND dut_sample_id NOT IN (" + placeholders + ")";
        }

        try (PreparedStatement deleteResults = connection.prepareStatement(sql)) {
            deleteResults.setInt(1, stepId);
            for (int index = 0; index < sampleIds.size(); index++) {
                deleteResults.setInt(index + 2, sampleIds.get(index));
            }
            deleteResults.executeUpdate();
        }
    }
}
