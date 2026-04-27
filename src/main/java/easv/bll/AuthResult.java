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

    private AuthResult(boolean success, String message, User user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }

    public static AuthResult success(User user) {
        return new AuthResult(true, "Login successful.", user);
    }

    public static AuthResult failure(String message) {
        return new AuthResult(false, message, null);
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
}
