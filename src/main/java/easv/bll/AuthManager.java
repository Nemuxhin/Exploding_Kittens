package easv.bll;

import easv.be.User;
import easv.dal.UserDAO;

public class AuthManager {
    private final UserDAO userDAO;

    public AuthManager() {
        this(new UserDAO());
    }

    public AuthManager(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public AuthResult login(String username, String password) {
        String safeUsername = username == null ? "" : username.trim();
        String safePassword = password == null ? "" : password;

        if (safeUsername.isBlank() || safePassword.isBlank()) {
            UserSession.clearCurrentUser();
            return AuthResult.failure("Please enter both username and password.");
        }

        User storedUser = userDAO.findByUsername(safeUsername);
        if (storedUser == null) {
            UserSession.clearCurrentUser();
            return AuthResult.failure("Invalid username or password.");
        }

        String storedHash = storedUser.getPasswordHash();
        if (storedHash == null || storedHash.isBlank() || storedHash.startsWith("CHANGE_ME")) {
            UserSession.clearCurrentUser();
            return AuthResult.failure("This account does not have a valid password yet.");
        }

        if (!storedHash.equalsIgnoreCase(PasswordHasher.hash(safePassword))) {
            UserSession.clearCurrentUser();
            return AuthResult.failure("Invalid username or password.");
        }

        if (!storedUser.isActive()) {
            UserSession.clearCurrentUser();
            return AuthResult.failure("This account is inactive and cannot log in.");
        }

        UserSession.setCurrentUser(storedUser);
        return AuthResult.success(storedUser);
    }

    public void logout() {
        UserSession.clearCurrentUser();
    }
}
