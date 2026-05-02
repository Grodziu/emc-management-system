package pl.emcmanagement.dao;

import pl.emcmanagement.database.DbConnection;
import pl.emcmanagement.enums.Role;
import pl.emcmanagement.enums.TestStatus;
import pl.emcmanagement.model.Project;
import pl.emcmanagement.model.User;
import pl.emcmanagement.util.JdbcMappers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public class ProjectDao {
    private final UserDao userDao = new UserDao();

    public List<Project> findProjectsForUser(User user) {
        String sql;
        if (user.getRole() == Role.VE) {
            sql = baseProjectQuery() + " WHERE p.ve_id = ? ORDER BY p.ewr_number";
        } else if (user.getRole() == Role.TE) {
            sql = baseProjectQuery() + " WHERE p.te_id = ? ORDER BY p.ewr_number";
        } else if (user.getRole() == Role.TT) {
            sql = baseProjectQuery() + " JOIN project_tt_assignments pta ON pta.project_id = p.id WHERE pta.tt_user_id = ? ORDER BY p.ewr_number";
        } else {
            sql = baseProjectQuery() + " ORDER BY p.ewr_number";
        }

        List<Project> projects = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (user.getRole() == Role.VE || user.getRole() == Role.TE || user.getRole() == Role.TT) {
                statement.setInt(1, user.getId());
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Project project = mapProject(resultSet);
                    project.setTtUsers(userDao.findTechniciansByProjectId(project.getId()));
                    projects.add(project);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas pobierania projektów.", e);
        }
        return projects;
    }

    public void assignTechnicianToProject(int projectId, int technicianId) {
        String sql = "INSERT IGNORE INTO project_tt_assignments (project_id, tt_user_id) VALUES (?, ?)";

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            statement.setInt(2, technicianId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas przypisywania technika do projektu.", e);
        }
    }

    public void replaceTechniciansForProject(int projectId, List<Integer> technicianIds) {
        List<Integer> safeIds = technicianIds == null ? List.of() : technicianIds;
        String deleteAssignmentsSql = "DELETE FROM project_tt_assignments WHERE project_id = ?";
        String insertAssignmentSql = "INSERT INTO project_tt_assignments (project_id, tt_user_id) VALUES (?, ?)";
        String clearLegAssignmentsSql;
        if (safeIds.isEmpty()) {
            clearLegAssignmentsSql = "UPDATE project_legs SET assigned_tt_id = NULL WHERE project_id = ?";
        } else {
            String placeholders = String.join(",", Collections.nCopies(safeIds.size(), "?"));
            clearLegAssignmentsSql = """
                    UPDATE project_legs
                    SET assigned_tt_id = NULL
                    WHERE project_id = ?
                      AND assigned_tt_id IS NOT NULL
                      AND assigned_tt_id NOT IN (""" + placeholders + ")";
        }

        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteAssignments = connection.prepareStatement(deleteAssignmentsSql);
                 PreparedStatement insertAssignment = connection.prepareStatement(insertAssignmentSql);
                 PreparedStatement clearLegAssignments = connection.prepareStatement(clearLegAssignmentsSql)) {
                deleteAssignments.setInt(1, projectId);
                deleteAssignments.executeUpdate();

                for (Integer technicianId : safeIds) {
                    insertAssignment.setInt(1, projectId);
                    insertAssignment.setInt(2, technicianId);
                    insertAssignment.addBatch();
                }
                if (!safeIds.isEmpty()) {
                    insertAssignment.executeBatch();
                }

                clearLegAssignments.setInt(1, projectId);
                for (int index = 0; index < safeIds.size(); index++) {
                    clearLegAssignments.setInt(index + 2, safeIds.get(index));
                }
                clearLegAssignments.executeUpdate();

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas aktualizacji przypisania TT do projektu.", e);
        }
    }

    private String baseProjectQuery() {
        return """
                SELECT DISTINCT
                    p.id,
                    p.ewr_number,
                    p.brand,
                    p.device_name,
                    p.short_description,
                    p.start_date,
                    p.end_date,
                    vps.project_status,
                    ve.id AS ve_id, ve.first_name AS ve_first_name, ve.last_name AS ve_last_name, ve.email AS ve_email,
                    te.id AS te_id, te.first_name AS te_first_name, te.last_name AS te_last_name, te.email AS te_email
                FROM projects p
                JOIN users ve ON ve.id = p.ve_id
                JOIN users te ON te.id = p.te_id
                LEFT JOIN vw_project_status vps ON vps.project_id = p.id
                """;
    }

    private Project mapProject(ResultSet resultSet) throws SQLException {
        Project project = new Project();
        project.setId(resultSet.getInt("id"));
        project.setEwrNumber(resultSet.getString("ewr_number"));
        project.setBrand(resultSet.getString("brand"));
        project.setDeviceName(resultSet.getString("device_name"));
        project.setShortDescription(resultSet.getString("short_description"));
        project.setStartDate(JdbcMappers.toLocalDate(resultSet.getDate("start_date")));
        project.setEndDate(JdbcMappers.toLocalDate(resultSet.getDate("end_date")));

        User ve = new User();
        ve.setId(resultSet.getInt("ve_id"));
        ve.setFirstName(resultSet.getString("ve_first_name"));
        ve.setLastName(resultSet.getString("ve_last_name"));
        ve.setEmail(resultSet.getString("ve_email"));
        project.setVe(ve);

        User te = new User();
        te.setId(resultSet.getInt("te_id"));
        te.setFirstName(resultSet.getString("te_first_name"));
        te.setLastName(resultSet.getString("te_last_name"));
        te.setEmail(resultSet.getString("te_email"));
        project.setTe(te);

        String status = resultSet.getString("project_status");
        project.setStatus(status == null ? TestStatus.NOT_STARTED : TestStatus.valueOf(status));
        return project;
    }
}
