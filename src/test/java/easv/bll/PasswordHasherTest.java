package easv.bll;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void sameInputProducesDifferentSaltedHashes() {
        String firstHash = PasswordHasher.hash("admin123");
        String secondHash = PasswordHasher.hash("admin123");

        assertNotEquals(firstHash, secondHash);
        assertTrue(PasswordHasher.verify("admin123", firstHash));
        assertTrue(PasswordHasher.verify("admin123", secondHash));
    }

    @Test
    void verifyRejectsWrongPassword() {
        String hash = PasswordHasher.hash("admin123");

        assertFalse(PasswordHasher.verify("wrong-password", hash));
    }

    @Test
    void verifySupportsLegacySha256Hashes() {
        String legacyHash = PasswordHasher.legacySha256("admin123");

        assertTrue(PasswordHasher.verify("admin123", legacyHash));
        assertTrue(PasswordHasher.needsRehash(legacyHash));
    }

    @Test
    void currentHashesDoNotNeedRehash() {
        String currentHash = PasswordHasher.hash("admin123");

        assertTrue(currentHash.startsWith("pbkdf2$sha256$"));
        assertFalse(PasswordHasher.needsRehash(currentHash));
    }
}
