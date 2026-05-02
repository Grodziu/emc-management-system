package pl.emcmanagement.dao;

import pl.emcmanagement.database.DbConnection;
import pl.emcmanagement.enums.DutMediaType;
import pl.emcmanagement.model.DutMedia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DutMediaDao {
    public List<DutMedia> findByProjectId(int projectId) {
        String sql = """
                SELECT dm.id, dm.dut_sample_id, dm.media_type, dm.file_name, dm.file_data
                FROM dut_media dm
                JOIN dut_samples ds ON ds.id = dm.dut_sample_id
                WHERE ds.project_id = ?
                ORDER BY ds.sample_code, FIELD(dm.media_type, 'FRONT_VIEW', 'BACK_VIEW', 'CONNECTOR_VIEW', 'LABEL')
                """;

        List<DutMedia> media = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    media.add(mapMedia(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas pobierania mediow DUT.", e);
        }
        return media;
    }

    public List<DutMedia> findBySampleId(int sampleId) {
        String sql = """
                SELECT id, dut_sample_id, media_type, file_name, file_data
                FROM dut_media
                WHERE dut_sample_id = ?
                ORDER BY FIELD(media_type, 'FRONT_VIEW', 'BACK_VIEW', 'CONNECTOR_VIEW', 'LABEL')
                """;

        List<DutMedia> media = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, sampleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    media.add(mapMedia(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas pobierania mediow DUT.", e);
        }
        return media;
    }

    public void upsertMedia(int dutSampleId, DutMediaType mediaType, String fileName, byte[] fileData) {
        String sql = """
                INSERT INTO dut_media (dut_sample_id, media_type, file_name, file_data)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    file_name = VALUES(file_name),
                    file_data = VALUES(file_data)
                """;

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, dutSampleId);
            statement.setString(2, mediaType.name());
            statement.setString(3, fileName);
            statement.setBytes(4, fileData);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas zapisu mediow DUT.", e);
        }
    }

    public void deleteMedia(int dutSampleId, DutMediaType mediaType) {
        String sql = "DELETE FROM dut_media WHERE dut_sample_id = ? AND media_type = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, dutSampleId);
            statement.setString(2, mediaType.name());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas usuwania mediow DUT.", e);
        }
    }

    private DutMedia mapMedia(ResultSet resultSet) throws SQLException {
        DutMedia media = new DutMedia();
        media.setId(resultSet.getInt("id"));
        media.setDutSampleId(resultSet.getInt("dut_sample_id"));
        media.setMediaType(DutMediaType.valueOf(resultSet.getString("media_type")));
        media.setFileName(resultSet.getString("file_name"));
        media.setFileData(resultSet.getBytes("file_data"));
        return media;
    }
}
