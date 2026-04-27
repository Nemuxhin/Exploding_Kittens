package easv.bll;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This helper turns a plain text password into a SHA-256 hash.
 * We store the hash, not the original password.
 */
public final class PasswordHasher {

    private PasswordHasher() {
    }

    public static String hash(String plainTextPassword) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = messageDigest.digest(plainTextPassword.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexBuilder = new StringBuilder();
            for (byte currentByte : hashBytes) {
                hexBuilder.append(String.format("%02x", currentByte));
            }

            return hexBuilder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available in this Java runtime.", exception);
        }
    }
}
