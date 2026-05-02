package pl.emcmanagement.util;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DesktopActions {
    private DesktopActions() {
    }

    public static void openFile(Component parent, String path) {
        if (path == null || path.isBlank()) {
            JOptionPane.showMessageDialog(parent, "Brak sciezki do pliku.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            File file = new File(path);
            if (!file.exists()) {
                JOptionPane.showMessageDialog(parent,
                        "Plik nie istnieje:\n" + path,
                        "Brak pliku",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            Desktop.getDesktop().open(file);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent,
                    "Nie udalo sie otworzyc pliku:\n" + e.getMessage(),
                    "Blad",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void openStoredDocument(Component parent, String fileName, byte[] content) {
        if (content == null || content.length == 0) {
            JOptionPane.showMessageDialog(parent, "Brak dokumentu zapisanego w bazie danych.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            String safeName = sanitizeFileName(fileName);
            String suffix = extractSuffix(safeName);
            Path tempFile = Files.createTempFile("emc-doc-", suffix);
            Files.write(tempFile, content);
            tempFile.toFile().deleteOnExit();
            Desktop.getDesktop().open(tempFile.toFile());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent,
                    "Nie udalo sie otworzyc dokumentu z bazy:\n" + e.getMessage(),
                    "Blad",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void openUrl(Component parent, String url) {
        if (url == null || url.isBlank()) {
            JOptionPane.showMessageDialog(parent, "Brak adresu URL.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent,
                    "Nie udalo sie otworzyc linku:\n" + e.getMessage(),
                    "Blad",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void sendMail(Component parent, String email) {
        if (email == null || email.isBlank()) {
            JOptionPane.showMessageDialog(parent, "Brak adresu e-mail.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            Desktop.getDesktop().mail(new URI("mailto:" + email));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent,
                    "Nie udalo sie uruchomic klienta poczty:\n" + e.getMessage(),
                    "Blad",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "dokument.txt";
        }
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static String extractSuffix(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return ".tmp";
        }
        return fileName.substring(dotIndex);
    }
}
