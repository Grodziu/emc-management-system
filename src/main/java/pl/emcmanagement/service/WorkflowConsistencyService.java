package pl.emcmanagement.service;

import pl.emcmanagement.dao.WorkflowSyncDao;
import pl.emcmanagement.database.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class WorkflowConsistencyService {
    private final WorkflowSyncDao workflowSyncDao = new WorkflowSyncDao();

    public void synchronizeAll() {
        String sql = "SELECT id FROM leg_test_steps ORDER BY leg_id, step_order";

        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    workflowSyncDao.syncAfterDutResultsChange(connection, resultSet.getInt("id"));
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Nie udalo sie zsynchronizowac statusow i dat testow.", e);
        }
    }
}
