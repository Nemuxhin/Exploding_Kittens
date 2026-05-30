package easv.bll;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Password hashing is security-critical: the database must never see the
 * plain-text password, and two identical passwords must always produce the
 * same hash so login can compare them.
 */
class PasswordHasherTest {

    @Test
    void sameInput_shouldProduceSameHash() {
        assertEquals(PasswordHasher.hash("admin123"), PasswordHasher.hash("admin123"),
                "SHA-256 must be deterministic for login comparison to work.");
    }

    @Test
    void differentInputs_shouldProduceDifferentHashes() {
        assertAll(
                () -> assertNotEquals(PasswordHasher.hash("admin123"), PasswordHasher.hash("admin124")),
                () -> assertNotEquals(PasswordHasher.hash("password"), PasswordHasher.hash("Password"),
                        "Hashing must be case-sensitive."),
                () -> assertNotEquals(PasswordHasher.hash(""), PasswordHasher.hash("a"))
        );
    }

    @Test
    void hash_shouldNotEqualThePlainTextInput() {
        String plainText = "admin123";
        assertNotEquals(plainText, PasswordHasher.hash(plainText),
                "The hash must never be the plain-text value itself.");
    }

    @Test
    void hash_shouldBeSixtyFourLowerCaseHexCharacters() {
        String hash = PasswordHasher.hash("admin123");

        assertAll(
                () -> assertEquals(64, hash.length(),    "SHA-256 produces 256 bits → 64 hex chars."),
                () -> assertTrue(hash.matches("[0-9a-f]+"), "Hash must be lower-case hex.")
        );
    }

    @Test
    void emptyString_shouldStillProduceTheWellKnownSha256Constant() {
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                PasswordHasher.hash(""),
                "The SHA-256 of an empty string is a fixed, well-known value — useful as a sanity anchor."
        );
    }
}
