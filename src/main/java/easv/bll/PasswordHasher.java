package easv.bll;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Passwords are stored with a salted PBKDF2 hash.
 * Legacy SHA-256 hashes are still accepted during login and upgraded after a
 * successful authentication.
 */
public final class PasswordHasher {
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String HASH_PREFIX = "pbkdf2$sha256$";
    private static final int CURRENT_ITERATIONS = 210_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String plainTextPassword) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] derivedKey = deriveKey(requiredPassword(plainTextPassword), salt, CURRENT_ITERATIONS, KEY_LENGTH_BITS);
        return HASH_PREFIX
                + CURRENT_ITERATIONS
                + "$"
                + Base64.getEncoder().encodeToString(salt)
                + "$"
                + Base64.getEncoder().encodeToString(derivedKey);
    }

    public static boolean verify(String plainTextPassword, String storedHash) {
        String safeStoredHash = storedHash == null ? "" : storedHash.trim();
        if (safeStoredHash.isBlank()) {
            return false;
        }

        if (isLegacySha256Hash(safeStoredHash)) {
            return MessageDigest.isEqual(
                    legacySha256(requiredPassword(plainTextPassword)).getBytes(StandardCharsets.US_ASCII),
                    safeStoredHash.getBytes(StandardCharsets.US_ASCII)
            );
        }

        if (!safeStoredHash.startsWith(HASH_PREFIX)) {
            return false;
        }

        String[] parts = safeStoredHash.split("\\$");
        if (parts.length != 5) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[2]);
            byte[] salt = Base64.getDecoder().decode(parts[3]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[4]);
            byte[] actualHash = deriveKey(requiredPassword(plainTextPassword), salt, iterations, expectedHash.length * 8);
            return MessageDigest.isEqual(actualHash, expectedHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static boolean needsRehash(String storedHash) {
        String safeStoredHash = storedHash == null ? "" : storedHash.trim();
        if (safeStoredHash.isBlank()) {
            return true;
        }
        if (isLegacySha256Hash(safeStoredHash)) {
            return true;
        }
        if (!safeStoredHash.startsWith(HASH_PREFIX)) {
            return true;
        }

        String[] parts = safeStoredHash.split("\\$");
        if (parts.length != 5) {
            return true;
        }

        try {
            return Integer.parseInt(parts[2]) < CURRENT_ITERATIONS;
        } catch (NumberFormatException exception) {
            return true;
        }
    }

    static String legacySha256(String plainTextPassword) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = messageDigest.digest(requiredPassword(plainTextPassword).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexBuilder = new StringBuilder(hashBytes.length * 2);
            for (byte currentByte : hashBytes) {
                hexBuilder.append(String.format("%02x", currentByte));
            }
            return hexBuilder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available in this Java runtime.", exception);
        }
    }

    private static byte[] deriveKey(String plainTextPassword, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(plainTextPassword.toCharArray(), salt, iterations, keyLengthBits);
            try {
                return SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("PBKDF2 password hashing is not available in this Java runtime.", exception);
        }
    }

    private static String requiredPassword(String plainTextPassword) {
        return plainTextPassword == null ? "" : plainTextPassword;
    }

    private static boolean isLegacySha256Hash(String value) {
        return value.matches("[0-9a-f]{64}");
    }
}
