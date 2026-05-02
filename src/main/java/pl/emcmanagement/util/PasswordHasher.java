package pl.emcmanagement.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordHasher {
    private static final String PREFIX = "sha256$";

    private PasswordHasher() {
    }

    public static boolean isHashed(String value) {
        return value != null && value.startsWith(PREFIX) && value.length() == PREFIX.length() + 64;
    }

    public static String ensureHashed(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return isHashed(value) ? value : hash(value);
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        if (isHashed(storedPassword)) {
            return storedPassword.equals(hash(rawPassword));
        }
        return storedPassword.equals(rawPassword);
    }

    public static String hash(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return PREFIX + toHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
