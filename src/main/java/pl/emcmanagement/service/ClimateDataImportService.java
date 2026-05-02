package pl.emcmanagement.service;

import pl.emcmanagement.dao.ClimateLogDao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClimateDataImportService {
    private final ClimateLogDao climateLogDao = new ClimateLogDao();

    public int importDesktopClimateFiles() {
        return importClimateFilesFromDirectories(resolveDesktopCandidates());
    }

    public int importClimateFilesFromDirectory(Path directory) {
        if (directory == null) {
            return 0;
        }
        return importClimateFilesFromDirectories(List.of(directory));
    }

    private int importClimateFilesFromDirectories(List<Path> directories) {
        int imported = 0;
        for (Path directory : directories) {
            if (!Files.isDirectory(directory)) {
                continue;
            }
            try (var stream = Files.list(directory)) {
                List<Path> files = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toUpperCase(Locale.ROOT).matches("EQ-SEN-.+_\\d{4}_CW\\d{1,2}_.+\\.TXT"))
                        .sorted()
                        .toList();
                for (Path file : files) {
                    climateLogDao.importFromFile(file);
                    imported++;
                }
            } catch (IOException ignored) {
            }
        }
        return imported;
    }

    private List<Path> resolveDesktopCandidates() {
        List<Path> candidates = new ArrayList<>();
        Path userHome = Path.of(System.getProperty("user.home"));
        candidates.add(userHome.resolve("Desktop"));
        candidates.add(userHome.resolve("Pulpit"));

        try (var stream = Files.list(userHome)) {
            stream.filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("OneDrive"))
                    .forEach(path -> {
                        candidates.add(path.resolve("Desktop"));
                        candidates.add(path.resolve("Pulpit"));
                    });
        } catch (IOException ignored) {
        }

        return candidates.stream().distinct().toList();
    }
}
