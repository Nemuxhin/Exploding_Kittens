package easv.bll;

import easv.be.User;

/**
 * This small helper object lets us return both the outcome and a message.
 * That keeps the controller code simple and beginner-friendly.
 */
public class AuthResult {

    private final boolean success;
    private final String message;
    private final User user;
    private final String token;

    private AuthResult(boolean success, String message, User user, String token) {
        this.success = success;
        this.message = message;
        this.user = user;
        this.token = token;
    }

    public static AuthResult success(User user) {
        return new AuthResult(true, "Login successful.", user, null);
    }

    public static AuthResult success(User user, String token) {
        return new AuthResult(true, "Login successful.", user, token);
    }

    public static AuthResult successMessage(String message, User user) {
        return new AuthResult(true, message, user, null);
    }

    public static AuthResult failure(String message) {
        return new AuthResult(false, message, null, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }
}
