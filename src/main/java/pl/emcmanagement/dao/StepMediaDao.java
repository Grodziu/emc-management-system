package pl.emcmanagement.dao;

import pl.emcmanagement.database.DbConnection;
import pl.emcmanagement.enums.StepMediaKind;
import pl.emcmanagement.model.StepMedia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StepMediaDao {
    public List<StepMedia> findByStepIdAndKind(int stepId, StepMediaKind mediaKind) {
        String sql = """
                SELECT id, leg_test_step_id, media_kind, slot_code, display_name, sort_order, file_name, file_data
                FROM leg_test_step_media
                WHERE leg_test_step_id = ? AND media_kind = ?
                ORDER BY sort_order, display_name, id
                """;

        List<StepMedia> media = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stepId);
            statement.setString(2, mediaKind.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    media.add(mapMedia(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas pobierania mediow testu.", e);
        }
        return media;
    }

    public void upsertMedia(int stepId,
                            StepMediaKind mediaKind,
                            String slotCode,
                            String displayName,
                            int sortOrder,
                            String fileName,
                            byte[] fileData) {
        String sql = """
                INSERT INTO leg_test_step_media (
                    leg_test_step_id, media_kind, slot_code, display_name, sort_order, file_name, file_data
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    display_name = VALUES(display_name),
                    file_name = VALUES(file_name),
                    file_data = VALUES(file_data)
                """;

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stepId);
            statement.setString(2, mediaKind.name());
            statement.setString(3, slotCode);
            statement.setString(4, displayName);
            statement.setInt(5, sortOrder);
            statement.setString(6, fileName);
            statement.setBytes(7, fileData);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas zapisu mediow testu.", e);
        }
    }

    public void deleteMedia(int mediaId) {
        String sql = "DELETE FROM leg_test_step_media WHERE id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, mediaId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas usuwania mediow testu.", e);
        }
    }

    private StepMedia mapMedia(ResultSet resultSet) throws SQLException {
        StepMedia media = new StepMedia();
        media.setId(resultSet.getInt("id"));
        media.setLegTestStepId(resultSet.getInt("leg_test_step_id"));
        media.setMediaKind(StepMediaKind.valueOf(resultSet.getString("media_kind")));
        media.setSlotCode(resultSet.getString("slot_code"));
        media.setDisplayName(resultSet.getString("display_name"));
        media.setSortOrder(resultSet.getInt("sort_order"));
        media.setFileName(resultSet.getString("file_name"));
        media.setFileData(resultSet.getBytes("file_data"));
        return media;
    }
}
