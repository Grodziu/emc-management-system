package pl.emcmanagement.app;

import pl.emcmanagement.database.DatabaseHealthCheck;
import pl.emcmanagement.service.ClimateDataImportService;
import pl.emcmanagement.service.PasswordMigrationService;
import pl.emcmanagement.service.WorkflowConsistencyService;
import pl.emcmanagement.ui.LoginFrame;
import pl.emcmanagement.ui.style.EmcUiTheme;

import javax.swing.*;

public class MainApp {
    private static final ClimateDataImportService CLIMATE_DATA_IMPORT_SERVICE = new ClimateDataImportService();
    private static final PasswordMigrationService PASSWORD_MIGRATION_SERVICE = new PasswordMigrationService();
    private static final WorkflowConsistencyService WORKFLOW_CONSISTENCY_SERVICE = new WorkflowConsistencyService();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                DatabaseHealthCheck.verifyConnection();
                applySystemLookAndFeel();
                migrateLegacyPasswords();
                synchronizeWorkflowState();
                tryImportClimateFiles();
                new LoginFrame().setVisible(true);
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(
                        null,
                        "Nie udało się uruchomić aplikacji.\n\n" + exception.getMessage(),
                        "Błąd startu",
                        JOptionPane.ERROR_MESSAGE
                );
                exception.printStackTrace();
            }
        });
    }

    private static void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            EmcUiTheme.configureUiDefaults();
        } catch (Exception ignored) {
        }
    }

    private static void tryImportClimateFiles() {
        try {
            CLIMATE_DATA_IMPORT_SERVICE.importDesktopClimateFiles();
        } catch (Exception ignored) {
        }
    }

    private static void migrateLegacyPasswords() {
        try {
            PASSWORD_MIGRATION_SERVICE.migrateLegacyPasswords();
        } catch (Exception ignored) {
        }
    }

    private static void synchronizeWorkflowState() {
        try {
            WORKFLOW_CONSISTENCY_SERVICE.synchronizeAll();
        } catch (Exception ignored) {
        }
    }
}
