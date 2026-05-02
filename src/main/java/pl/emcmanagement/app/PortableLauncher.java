package pl.emcmanagement.app;

import javax.swing.JOptionPane;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PortableLauncher {
    private static final int PORTABLE_DB_PORT = 3317;
    private static final String DATABASE_NAME = "emc_management_system";
    private static Process databaseProcess;

    private PortableLauncher() {
    }

    public static void main(String[] args) {
        try {
            Path appHome = findAppHome();
            Path mariaDbHome = appHome.resolve("db").resolve("mariadb");
            Path dataDir = appHome.resolve("db").resolve("data");
            Path dumpFile = appHome.resolve("database").resolve("emc_management_system.sql");
            Path logsDir = appHome.resolve("logs");
            Files.createDirectories(logsDir);

            ensureDatabaseInitialized(mariaDbHome, dataDir, dumpFile, logsDir);
            startDatabase(mariaDbHome, dataDir, logsDir);
            configureApplicationForPortableDatabase();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> stopDatabase(mariaDbHome, logsDir), "portable-db-shutdown"));

            MainApp.main(args);
            waitForSwingApplicationToClose();
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(
                    null,
                    "Nie udalo sie uruchomic wersji portable aplikacji.\n\n" + exception.getMessage(),
                    "EMC Management System - blad portable",
                    JOptionPane.ERROR_MESSAGE
            );
            exception.printStackTrace();
            System.exit(1);
        } finally {
            stopDatabaseQuietly();
        }
    }

    private static void configureApplicationForPortableDatabase() {
        System.setProperty("db.url", "jdbc:mysql://127.0.0.1:" + PORTABLE_DB_PORT + "/" + DATABASE_NAME
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        System.setProperty("db.user", "root");
        System.setProperty("db.password", "");
    }

    private static Path findAppHome() throws URISyntaxException {
        List<Path> candidates = new ArrayList<>();
        candidates.add(Path.of(System.getProperty("user.dir")).toAbsolutePath());

        CodeSource codeSource = PortableLauncher.class.getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            Path codePath = Path.of(codeSource.getLocation().toURI()).toAbsolutePath();
            candidates.add(Files.isRegularFile(codePath) ? codePath.getParent() : codePath);
            Path parent = codePath.getParent();
            if (parent != null) {
                candidates.add(parent.getParent());
            }
        }

        for (Path candidate : candidates) {
            if (candidate != null && hasPortableDatabase(candidate)) {
                return candidate;
            }
        }

        for (Path candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            Path parent = candidate.getParent();
            while (parent != null) {
                if (hasPortableDatabase(parent)) {
                    return parent;
                }
                parent = parent.getParent();
            }
        }

        throw new IllegalStateException("Nie znaleziono folderu portable z db\\mariadb obok aplikacji.");
    }

    private static boolean hasPortableDatabase(Path candidate) {
        return Files.isRegularFile(candidate.resolve("db").resolve("mariadb").resolve("bin").resolve("mysqld.exe"));
    }

    private static void ensureDatabaseInitialized(Path mariaDbHome, Path dataDir, Path dumpFile, Path logsDir)
            throws IOException, InterruptedException {
        Path marker = dataDir.resolve(".emc_initialized");
        if (Files.isRegularFile(marker)) {
            return;
        }

        Files.createDirectories(dataDir);
        Path installDb = mariaDbHome.resolve("bin").resolve("mysql_install_db.exe");
        runCommand(logsDir.resolve("mysql-install.log"), installDb.toString(),
                "--datadir=" + dataDir.toAbsolutePath(),
                "--password=");

        startDatabase(mariaDbHome, dataDir, logsDir);
        try {
            Path mysql = mariaDbHome.resolve("bin").resolve("mysql.exe");
            runCommand(logsDir.resolve("mysql-create-db.log"), mysql.toString(),
                    "--protocol=tcp",
                    "-h", "127.0.0.1",
                    "-P", String.valueOf(PORTABLE_DB_PORT),
                    "-u", "root",
                    "-e", "CREATE DATABASE IF NOT EXISTS " + DATABASE_NAME
                            + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");

            if (!Files.isRegularFile(dumpFile)) {
                throw new IllegalStateException("Brakuje pliku dumpa bazy: " + dumpFile);
            }
            runCommandWithInput(dumpFile, logsDir.resolve("mysql-import.log"), mysql.toString(),
                    "--protocol=tcp",
                    "-h", "127.0.0.1",
                    "-P", String.valueOf(PORTABLE_DB_PORT),
                    "-u", "root",
                    DATABASE_NAME);
            Files.writeString(marker, "initialized=true%n");
        } finally {
            stopDatabase(mariaDbHome, logsDir);
        }
    }

    private static void startDatabase(Path mariaDbHome, Path dataDir, Path logsDir) throws IOException, InterruptedException {
        if (isPortOpen()) {
            return;
        }
        if (databaseProcess != null && databaseProcess.isAlive()) {
            return;
        }

        Path mysqld = mariaDbHome.resolve("bin").resolve("mysqld.exe");
        Files.createDirectories(logsDir);
        ProcessBuilder processBuilder = new ProcessBuilder(
                mysqld.toString(),
                "--no-defaults",
                "--basedir=" + mariaDbHome.toAbsolutePath(),
                "--datadir=" + dataDir.toAbsolutePath(),
                "--port=" + PORTABLE_DB_PORT,
                "--bind-address=127.0.0.1",
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_unicode_ci",
                "--console"
        );
        processBuilder.directory(mariaDbHome.toFile());
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(logsDir.resolve("mariadb.log").toFile()));
        databaseProcess = processBuilder.start();
        waitForPort(Duration.ofSeconds(45));
    }

    private static void stopDatabase(Path mariaDbHome, Path logsDir) {
        try {
            Path mysqlAdmin = mariaDbHome.resolve("bin").resolve("mysqladmin.exe");
            runCommand(logsDir.resolve("mysql-shutdown.log"), mysqlAdmin.toString(),
                    "--protocol=tcp",
                    "-h", "127.0.0.1",
                    "-P", String.valueOf(PORTABLE_DB_PORT),
                    "-u", "root",
                    "shutdown");
        } catch (Exception ignored) {
            stopDatabaseQuietly();
        }
    }

    private static void stopDatabaseQuietly() {
        if (databaseProcess != null && databaseProcess.isAlive()) {
            databaseProcess.destroy();
        }
    }

    private static void waitForPort(Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (isPortOpen()) {
                return;
            }
            Thread.sleep(250L);
        }
        throw new IllegalStateException("Lokalna baza portable nie wystartowala na porcie " + PORTABLE_DB_PORT + ".");
    }

    private static boolean isPortOpen() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", PORTABLE_DB_PORT), 200);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void runCommand(Path logFile, String... command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Polecenie zakonczone kodem " + exitCode + ": " + String.join(" ", command));
        }
    }

    private static void runCommandWithInput(Path inputFile, Path logFile, String... command)
            throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectInput(inputFile.toFile());
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Import bazy zakonczony kodem " + exitCode + ": " + String.join(" ", command));
        }
    }

    private static void waitForSwingApplicationToClose() throws InterruptedException {
        boolean applicationWindowSeen = false;
        while (true) {
            boolean hasDisplayableWindow = Arrays.stream(Window.getWindows()).anyMatch(Window::isDisplayable);
            applicationWindowSeen = applicationWindowSeen || hasDisplayableWindow;
            if (applicationWindowSeen && !hasDisplayableWindow) {
                return;
            }
            Thread.sleep(1000L);
        }
    }
}
