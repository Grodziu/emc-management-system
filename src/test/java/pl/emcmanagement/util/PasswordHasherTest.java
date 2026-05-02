package pl.emcmanagement.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    @Test
    void hashAddsPrefixAndProducesSha256Length() {
        String hashed = PasswordHasher.hash("admin123");

        assertTrue(hashed.startsWith("sha256$"));
        assertEquals(71, hashed.length());
        assertTrue(hashed.matches("sha256\\$[0-9a-f]{64}"));
    }

    @Test
    void ensureHashedHashesPlainTextAndDoesNotDoubleHash() {
        String plain = "secret";
        String firstHash = PasswordHasher.ensureHashed(plain);
        String secondHash = PasswordHasher.ensureHashed(firstHash);

        assertNotEquals(plain, firstHash);
        assertEquals(firstHash, secondHash);
        assertEquals("   ", PasswordHasher.ensureHashed("   "));
        assertNull(PasswordHasher.ensureHashed(null));
    }

    @Test
    void matchesSupportsHashedAndLegacyPlaintextPasswords() {
        String raw = "test-password";
        String hashed = PasswordHasher.hash(raw);

        assertTrue(PasswordHasher.matches(raw, hashed));
        assertTrue(PasswordHasher.matches(raw, raw));
        assertFalse(PasswordHasher.matches("wrong", hashed));
        assertFalse(PasswordHasher.matches(raw, null));
        assertFalse(PasswordHasher.matches(null, hashed));
        assertFalse(PasswordHasher.matches(raw, ""));
    }
}
