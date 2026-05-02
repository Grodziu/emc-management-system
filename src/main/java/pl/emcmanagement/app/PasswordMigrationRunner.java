package pl.emcmanagement.app;

import pl.emcmanagement.service.PasswordMigrationService;

public class PasswordMigrationRunner {
    public static void main(String[] args) {
        int migrated = new PasswordMigrationService().migrateLegacyPasswords();
        System.out.println("Migrated passwords: " + migrated);
    }
}
