package pl.emcmanagement.app;

import pl.emcmanagement.database.DbConnection;
import pl.emcmanagement.service.ClimateDataImportService;
import pl.emcmanagement.service.WorkflowConsistencyService;

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

public final class DemoDatasetRunner {
    private static final Path METADATA_MIGRATION_PATH = Path.of("src", "main", "resources", "sql", "09_equipment_and_dut_metadata.sql");
    private static final Path SQL_SCRIPT_PATH = Path.of("src", "main", "resources", "sql", "08_expanded_demo_seed.sql");
    private static final Path DEMO_CLIMATE_DIRECTORY = Path.of("src", "main", "resources", "demo", "climate");

    private final ClimateDataImportService climateDataImportService = new ClimateDataImportService();
    private final WorkflowConsistencyService workflowConsistencyService = new WorkflowConsistencyService();

    private DemoDatasetRunner() {
    }

    public static void main(String[] args) {
        try {
            new DemoDatasetRunner().seed();
            System.out.println("DEMO_DATASET_OK");
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private void seed() throws Exception {
        executeSqlScript(METADATA_MIGRATION_PATH);
        executeSqlScript(SQL_SCRIPT_PATH);
        int importedClimateFiles = climateDataImportService.importClimateFilesFromDirectory(DEMO_CLIMATE_DIRECTORY);
        workflowConsistencyService.synchronizeAll();
        printSummary(importedClimateFiles);
    }

    private void executeSqlScript(Path scriptPath) throws Exception {
        if (!Files.exists(scriptPath)) {
            throw new IllegalStateException("Nie znaleziono skryptu seed danych demo: " + scriptPath);
        }

        String script = Files.readString(scriptPath, StandardCharsets.UTF_8);
        List<String> statements = splitSqlStatements(script);
        if (statements.isEmpty()) {
            throw new IllegalStateException("Skrypt seed danych demo nie zawiera zadnych instrukcji SQL.");
        }

        try (Connection connection = DbConnection.getConnection();
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                for (String sql : statements) {
                    statement.execute(sql);
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private List<String> splitSqlStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int index = 0; index < script.length(); index++) {
            char currentChar = script.charAt(index);
            char nextChar = index + 1 < script.length() ? script.charAt(index + 1) : '\0';

            if (inLineComment) {
                if (currentChar == '\n') {
                    inLineComment = false;
                    current.append(currentChar);
                }
                continue;
            }

            if (inBlockComment) {
                if (currentChar == '*' && nextChar == '/') {
                    inBlockComment = false;
                    index++;
                }
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote) {
                if (currentChar == '-' && nextChar == '-') {
                    char afterComment = index + 2 < script.length() ? script.charAt(index + 2) : '\0';
                    if (Character.isWhitespace(afterComment) || afterComment == '\0') {
                        inLineComment = true;
                        index++;
                        continue;
                    }
                }
                if (currentChar == '#') {
                    inLineComment = true;
                    continue;
                }
                if (currentChar == '/' && nextChar == '*') {
                    inBlockComment = true;
                    index++;
                    continue;
                }
            }

            if (currentChar == '\'' && !inDoubleQuote) {
                boolean escapedQuote = inSingleQuote && nextChar == '\'';
                current.append(currentChar);
                if (escapedQuote) {
                    current.append(nextChar);
                    index++;
                } else {
                    inSingleQuote = !inSingleQuote;
                }
                continue;
            }

            if (currentChar == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                current.append(currentChar);
                continue;
            }

            if (currentChar == ';' && !inSingleQuote && !inDoubleQuote) {
                String statement = current.toString().trim();
                if (!statement.isEmpty()) {
                    statements.add(statement);
                }
                current.setLength(0);
                continue;
            }

            current.append(currentChar);
        }

        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            statements.add(tail);
        }

        return statements;
    }

    private void printSummary(int importedClimateFiles) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            System.out.println("DEMO_CLIMATE_IMPORTED=" + importedClimateFiles);
            System.out.println("SEEDED_PROJECTS=" + queryForInt(
                    connection,
                    "SELECT COUNT(*) FROM projects WHERE ewr_number IN ('EWR105031', 'EWR105084', 'EWR105129', 'EWR105176')"
            ));
            System.out.println("SEEDED_DUTS=" + queryForInt(
                    connection,
                    "SELECT COUNT(*) FROM dut_samples WHERE sample_code LIKE '105031%' OR sample_code LIKE '105084%' OR sample_code LIKE '105129%' OR sample_code LIKE '105176%'"
            ));
            System.out.println("SEEDED_LEGS=" + queryForInt(
                    connection,
                    """
                    SELECT COUNT(*)
                    FROM project_legs pl
                    JOIN projects p ON p.id = pl.project_id
                    WHERE p.ewr_number IN ('EWR105031', 'EWR105084', 'EWR105129', 'EWR105176')
                    """
            ));
            System.out.println("SEEDED_CLIMATE_FILES=" + queryForInt(
                    connection,
                    "SELECT COUNT(*) FROM climate_log_files WHERE year_number = 2026 AND calendar_week BETWEEN 1 AND 12"
            ));
        }
    }

    private int queryForInt(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }
}
