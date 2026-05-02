package pl.emcmanagement.dao;

import pl.emcmanagement.database.DbConnection;
import pl.emcmanagement.enums.AccreditationStatus;
import pl.emcmanagement.enums.TestStatus;
import pl.emcmanagement.enums.TestType;
import pl.emcmanagement.model.Leg;
import pl.emcmanagement.model.User;
import pl.emcmanagement.util.JdbcMappers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class LegDao {
    public List<Leg> findLegsByProjectId(int projectId) {
        String sql = baseLegQuery() + " WHERE pl.project_id = ? ORDER BY pl.leg_code";

        List<Leg> legs = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    legs.add(mapLeg(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas pobierania LEGów.", e);
        }
        return legs;
    }

    public Leg findById(int legId) {
        String sql = baseLegQuery() + " WHERE pl.id = ?";

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, legId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapLeg(resultSet);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas pobierania LEGu.", e);
        }
        throw new IllegalArgumentException("Nie znaleziono LEGu o id " + legId);
    }

    public int insertLeg(Leg leg) {
        DocumentPayload isoDocument = prepareDocument(
                leg.getIsoFilePath(),
                leg.getIsoStandardName(),
                "Norma ISO"
        );
        DocumentPayload clientDocument = prepareDocument(
                leg.getClientFilePath(),
                leg.getClientStandardName(),
                "Norma klienta"
        );
        DocumentPayload testPlanDocument = prepareDocument(
                leg.getTestPlanFilePath(),
                leg.getTestPlanName(),
                "Test plan"
        );

        String sql = """
                INSERT INTO project_legs (
                    project_id, leg_code, test_type, accreditation, start_date, end_date, assigned_tt_id,
                    iso_standard_name, iso_file_path, iso_file_name, iso_file_data,
                    client_standard_name, client_file_path, client_file_name, client_file_data,
                    test_plan_name, test_plan_file_path, test_plan_file_name, test_plan_file_data, pca_url
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, leg.getProjectId());
            statement.setString(2, leg.getLegCode());
            statement.setString(3, leg.getTestType().name());
            statement.setString(4, leg.getAccreditation().name());
            statement.setDate(5, JdbcMappers.toSqlDate(leg.getStartDate()));
            statement.setDate(6, JdbcMappers.toSqlDate(leg.getEndDate()));
            if (leg.getAssignedTt() == null) {
                statement.setNull(7, java.sql.Types.INTEGER);
            } else {
                statement.setInt(7, leg.getAssignedTt().getId());
            }
            statement.setString(8, leg.getIsoStandardName());
            statement.setString(9, leg.getIsoFilePath());
            statement.setString(10, isoDocument.fileName());
            statement.setBytes(11, isoDocument.data());
            statement.setString(12, leg.getClientStandardName());
            statement.setString(13, leg.getClientFilePath());
            statement.setString(14, clientDocument.fileName());
            statement.setBytes(15, clientDocument.data());
            statement.setString(16, leg.getTestPlanName());
            statement.setString(17, leg.getTestPlanFilePath());
            statement.setString(18, testPlanDocument.fileName());
            statement.setBytes(19, testPlanDocument.data());
            statement.setString(20, leg.getPcaUrl());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas dodawania LEGu.", e);
        }

        throw new IllegalStateException("Nie udało się pobrać ID nowego LEGu.");
    }

    public void updateLeg(Leg leg) {
        DocumentPayload isoDocument = prepareDocumentForUpdate(
                leg.getIsoFilePath(),
                leg.getIsoStandardName(),
                leg.getIsoFileName(),
                leg.getIsoFileData(),
                "Norma ISO"
        );
        DocumentPayload clientDocument = prepareDocumentForUpdate(
                leg.getClientFilePath(),
                leg.getClientStandardName(),
                leg.getClientFileName(),
                leg.getClientFileData(),
                "Norma klienta"
        );
        DocumentPayload testPlanDocument = prepareDocumentForUpdate(
                leg.getTestPlanFilePath(),
                leg.getTestPlanName(),
                leg.getTestPlanFileName(),
                leg.getTestPlanFileData(),
                "Test plan"
        );

        String sql = """
                UPDATE project_legs
                SET leg_code = ?, test_type = ?, accreditation = ?, start_date = ?, end_date = ?, assigned_tt_id = ?,
                    iso_standard_name = ?, iso_file_path = ?, iso_file_name = ?, iso_file_data = ?,
                    client_standard_name = ?, client_file_path = ?, client_file_name = ?, client_file_data = ?,
                    test_plan_name = ?, test_plan_file_path = ?, test_plan_file_name = ?, test_plan_file_data = ?,
                    pca_url = ?
                WHERE id = ?
                """;

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, leg.getLegCode());
            statement.setString(2, leg.getTestType().name());
            statement.setString(3, leg.getAccreditation().name());
            statement.setDate(4, JdbcMappers.toSqlDate(leg.getStartDate()));
            statement.setDate(5, JdbcMappers.toSqlDate(leg.getEndDate()));
            if (leg.getAssignedTt() == null) {
                statement.setNull(6, java.sql.Types.INTEGER);
            } else {
                statement.setInt(6, leg.getAssignedTt().getId());
            }
            statement.setString(7, leg.getIsoStandardName());
            statement.setString(8, leg.getIsoFilePath());
            statement.setString(9, isoDocument.fileName());
            statement.setBytes(10, isoDocument.data());
            statement.setString(11, leg.getClientStandardName());
            statement.setString(12, leg.getClientFilePath());
            statement.setString(13, clientDocument.fileName());
            statement.setBytes(14, clientDocument.data());
            statement.setString(15, leg.getTestPlanName());
            statement.setString(16, leg.getTestPlanFilePath());
            statement.setString(17, testPlanDocument.fileName());
            statement.setBytes(18, testPlanDocument.data());
            statement.setString(19, leg.getPcaUrl());
            statement.setInt(20, leg.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas aktualizacji LEGu.", e);
        }
    }

    public void assignTechnicianToLeg(int legId, Integer technicianId) {
        String sql = "UPDATE project_legs SET assigned_tt_id = ? WHERE id = ?";

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (technicianId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, technicianId);
            }
            statement.setInt(2, legId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas przypisywania technika do LEGu.", e);
        }
    }

    public void deleteLeg(int legId) {
        String sql = "DELETE FROM project_legs WHERE id = ?";

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, legId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas usuwania LEGu.", e);
        }
    }

    private String baseLegQuery() {
        return """
                SELECT
                    pl.id,
                    pl.project_id,
                    pl.leg_code,
                    pl.test_type,
                    pl.accreditation,
                    pl.start_date,
                    pl.end_date,
                    pl.iso_standard_name,
                    pl.iso_file_path,
                    pl.iso_file_name,
                    pl.iso_file_data,
                    pl.client_standard_name,
                    pl.client_file_path,
                    pl.client_file_name,
                    pl.client_file_data,
                    pl.test_plan_name,
                    pl.test_plan_file_path,
                    pl.test_plan_file_name,
                    pl.test_plan_file_data,
                    pl.pca_url,
                    vls.leg_status,
                    COALESCE(vldc.dut_count, 0) AS dut_count,
                    u.id AS tt_id,
                    u.first_name AS tt_first_name,
                    u.last_name AS tt_last_name,
                    u.email AS tt_email
                FROM project_legs pl
                LEFT JOIN vw_leg_status vls ON vls.leg_id = pl.id
                LEFT JOIN vw_leg_dut_count vldc ON vldc.leg_id = pl.id
                LEFT JOIN users u ON u.id = pl.assigned_tt_id
                """;
    }

    private Leg mapLeg(ResultSet resultSet) throws SQLException {
        Leg leg = new Leg();
        leg.setId(resultSet.getInt("id"));
        leg.setProjectId(resultSet.getInt("project_id"));
        leg.setLegCode(resultSet.getString("leg_code"));
        leg.setTestType(TestType.valueOf(resultSet.getString("test_type")));
        leg.setAccreditation(AccreditationStatus.valueOf(resultSet.getString("accreditation")));
        leg.setStartDate(JdbcMappers.toLocalDate(resultSet.getDate("start_date")));
        leg.setEndDate(JdbcMappers.toLocalDate(resultSet.getDate("end_date")));
        leg.setIsoStandardName(resultSet.getString("iso_standard_name"));
        leg.setIsoFilePath(resultSet.getString("iso_file_path"));
        leg.setIsoFileName(resultSet.getString("iso_file_name"));
        leg.setIsoFileData(resultSet.getBytes("iso_file_data"));
        leg.setClientStandardName(resultSet.getString("client_standard_name"));
        leg.setClientFilePath(resultSet.getString("client_file_path"));
        leg.setClientFileName(resultSet.getString("client_file_name"));
        leg.setClientFileData(resultSet.getBytes("client_file_data"));
        leg.setTestPlanName(resultSet.getString("test_plan_name"));
        leg.setTestPlanFilePath(resultSet.getString("test_plan_file_path"));
        leg.setTestPlanFileName(resultSet.getString("test_plan_file_name"));
        leg.setTestPlanFileData(resultSet.getBytes("test_plan_file_data"));
        leg.setPcaUrl(resultSet.getString("pca_url"));
        leg.setStatus(TestStatus.valueOf(resultSet.getString("leg_status")));
        leg.setDutCount(resultSet.getInt("dut_count"));

        int ttId = resultSet.getInt("tt_id");
        if (!resultSet.wasNull()) {
            User tt = new User();
            tt.setId(ttId);
            tt.setFirstName(resultSet.getString("tt_first_name"));
            tt.setLastName(resultSet.getString("tt_last_name"));
            tt.setEmail(resultSet.getString("tt_email"));
            leg.setAssignedTt(tt);
        }
        return leg;
    }

    private DocumentPayload prepareDocument(String filePath, String displayName, String documentType) {
        String normalizedPath = trimToNull(filePath);
        String normalizedName = trimToNull(displayName);
        if (normalizedPath != null) {
            Path path = Path.of(normalizedPath);
            String fileName = path.getFileName() == null
                    ? fallbackFileName(normalizedName, documentType)
                    : path.getFileName().toString();
            if (Files.exists(path) && Files.isRegularFile(path)) {
                try {
                    return new DocumentPayload(fileName, Files.readAllBytes(path));
                } catch (Exception e) {
                    throw new RuntimeException("Nie udalo sie odczytac pliku dokumentu: " + normalizedPath, e);
                }
            }
            return new DocumentPayload(fileName, buildPlaceholderDocument(documentType, normalizedName, normalizedPath));
        }

        if (normalizedName == null) {
            return new DocumentPayload(null, null);
        }
        return new DocumentPayload(fallbackFileName(normalizedName, documentType), buildPlaceholderDocument(documentType, normalizedName, null));
    }

    private DocumentPayload prepareDocumentForUpdate(String filePath,
                                                     String displayName,
                                                     String existingFileName,
                                                     byte[] existingData,
                                                     String documentType) {
        String normalizedPath = trimToNull(filePath);
        if (normalizedPath != null) {
            return prepareDocument(normalizedPath, displayName, documentType);
        }
        if (existingData != null && existingData.length > 0) {
            return new DocumentPayload(existingFileName, existingData);
        }
        return prepareDocument(null, displayName, documentType);
    }

    private byte[] buildPlaceholderDocument(String documentType, String displayName, String originalPath) {
        StringBuilder builder = new StringBuilder();
        builder.append(documentType).append(System.lineSeparator());
        builder.append("Nazwa: ").append(displayName == null ? "---" : displayName).append(System.lineSeparator());
        if (originalPath != null) {
            builder.append("Oryginalna sciezka importu: ").append(originalPath).append(System.lineSeparator());
        }
        builder.append(System.lineSeparator());
        builder.append("Dokument zostal zapisany w bazie danych aplikacji EMC Management System.");
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String fallbackFileName(String displayName, String documentType) {
        String base = trimToNull(displayName);
        if (base == null) {
            base = documentType;
        }
        return base.replaceAll("[^A-Za-z0-9._-]+", "_") + ".txt";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record DocumentPayload(String fileName, byte[] data) {
    }
}
