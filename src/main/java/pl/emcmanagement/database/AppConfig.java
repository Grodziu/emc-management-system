package pl.emcmanagement.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public final class AppConfig {
    private static final Properties PROPERTIES = new Properties();

    static {
        try {
            loadProperties();
        } catch (IOException e) {
            throw new IllegalStateException("Nie udalo sie wczytac app.properties.", e);
        }
    }

    private AppConfig() {
    }

    public static String get(String key) {
        String systemValue = trimToNull(System.getProperty(key));
        if (systemValue != null) {
            return systemValue;
        }

        String environmentKey = key.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
        String environmentValue = trimToNull(System.getenv(environmentKey));
        if (environmentValue != null) {
            return environmentValue;
        }

        return PROPERTIES.getProperty(key);
    }

    public static String getOrDefault(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    private static void loadProperties() throws IOException {
        if (!loadMandatoryProperties("app.properties")) {
            throw new IllegalStateException("Nie znaleziono pliku app.properties ani na classpath, ani w lokalnych sciezkach projektu.");
        }
        loadOptionalProperties("app-local.properties");
    }

    private static boolean loadMandatoryProperties(String fileName) throws IOException {
        return loadFromClasspath(fileName) || loadFromFallbackPaths(fileName);
    }

    private static void loadOptionalProperties(String fileName) throws IOException {
        if (!loadFromClasspath(fileName)) {
            loadFromFallbackPaths(fileName);
        }
    }

    private static boolean loadFromClasspath(String fileName) throws IOException {
        try (InputStream inputStream = AppConfig.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream != null) {
                PROPERTIES.load(inputStream);
                return true;
            }
        }
        return false;
    }

    private static boolean loadFromFallbackPaths(String fileName) throws IOException {
        Path[] fallbackPaths = {
                Path.of("src", "main", "resources", fileName),
                Path.of("main", "resources", fileName),
                Path.of(fileName)
        };

        for (Path fallbackPath : fallbackPaths) {
            if (Files.isRegularFile(fallbackPath)) {
                try (InputStream fileInputStream = Files.newInputStream(fallbackPath)) {
                    PROPERTIES.load(fileInputStream);
                    return true;
                }
            }
        }
        return false;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
