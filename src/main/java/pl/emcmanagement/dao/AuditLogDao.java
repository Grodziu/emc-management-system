package pl.emcmanagement.dao;

import pl.emcmanagement.database.DbConnection;
import pl.emcmanagement.model.AuditLogEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDao {
    public void insert(AuditLogEntry entry) {
        String sql = """
                INSERT INTO audit_log (
                    actor_user_id, actor_name, actor_role, action_type, entity_type, entity_id,
                    project_id, leg_id, step_id, summary, details
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (entry.getActorUserId() == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, entry.getActorUserId());
            }
            statement.setString(2, entry.getActorName());
            statement.setString(3, entry.getActorRole());
            statement.setString(4, entry.getActionType());
            statement.setString(5, entry.getEntityType());
            if (entry.getEntityId() == null) {
                statement.setNull(6, java.sql.Types.INTEGER);
            } else {
                statement.setInt(6, entry.getEntityId());
            }
            if (entry.getProjectId() == null) {
                statement.setNull(7, java.sql.Types.INTEGER);
            } else {
                statement.setInt(7, entry.getProjectId());
            }
            if (entry.getLegId() == null) {
                statement.setNull(8, java.sql.Types.INTEGER);
            } else {
                statement.setInt(8, entry.getLegId());
            }
            if (entry.getStepId() == null) {
                statement.setNull(9, java.sql.Types.INTEGER);
            } else {
                statement.setInt(9, entry.getStepId());
            }
            statement.setString(10, entry.getSummary());
            statement.setString(11, entry.getDetails());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Nie udalo sie zapisac wpisu audit log.", e);
        }
    }

    public List<AuditLogEntry> findRecent(Integer projectId, Integer legId, int limit) {
        String sql = """
                SELECT
                    id,
                    actor_user_id,
                    actor_name,
                    actor_role,
                    action_type,
                    entity_type,
                    entity_id,
                    project_id,
                    leg_id,
                    step_id,
                    summary,
                    details,
                    created_at
                FROM audit_log
                WHERE (? IS NULL OR project_id = ?)
                  AND (? IS NULL OR leg_id = ?)
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """;

        List<AuditLogEntry> entries = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (projectId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
                statement.setNull(2, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, projectId);
                statement.setInt(2, projectId);
            }
            if (legId == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, legId);
                statement.setInt(4, legId);
            }
            statement.setInt(5, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(mapEntry(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Nie udalo sie pobrac audit log.", e);
        }
        return entries;
    }

    private AuditLogEntry mapEntry(ResultSet resultSet) throws SQLException {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setId(resultSet.getInt("id"));

        int actorId = resultSet.getInt("actor_user_id");
        entry.setActorUserId(resultSet.wasNull() ? null : actorId);
        entry.setActorName(resultSet.getString("actor_name"));
        entry.setActorRole(resultSet.getString("actor_role"));
        entry.setActionType(resultSet.getString("action_type"));
        entry.setEntityType(resultSet.getString("entity_type"));

        int entityId = resultSet.getInt("entity_id");
        entry.setEntityId(resultSet.wasNull() ? null : entityId);
        int projectId = resultSet.getInt("project_id");
        entry.setProjectId(resultSet.wasNull() ? null : projectId);
        int legId = resultSet.getInt("leg_id");
        entry.setLegId(resultSet.wasNull() ? null : legId);
        int stepId = resultSet.getInt("step_id");
        entry.setStepId(resultSet.wasNull() ? null : stepId);

        entry.setSummary(resultSet.getString("summary"));
        entry.setDetails(resultSet.getString("details"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        entry.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return entry;
    }
}
