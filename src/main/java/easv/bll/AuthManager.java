package easv.bll;

import easv.be.User;
import easv.dal.UserDAO;

import java.io.IOException;

/**
 * This class contains the login rules.
 * The controller asks this class to validate the username and password.
 */
public class AuthManager {

    private final UserDAO userDAO;

    public AuthManager() {
        this(new UserDAO());
    }

    public AuthManager(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public AuthResult login(String username, String password) {
        try {
            String safeUsername = username == null ? "" : username.trim();
            String safePassword = password == null ? "" : password;

            // We reject empty fields early so the user gets a clear message.
            if (safeUsername.isBlank() || safePassword.isBlank()) {
                UserSession.clearCurrentUser();
                return AuthResult.failure("Please enter both username and password.");
            }

            User storedUser = userDAO.findByUsername(safeUsername);

            // If the username does not exist, we stop here.
            if (storedUser == null) {
                UserSession.clearCurrentUser();
                return AuthResult.failure("Invalid username or password.");
            }

            String enteredPasswordHash = PasswordHasher.hash(safePassword);

            // We compare hashes instead of plain text passwords.
            if (!storedUser.getPasswordHash().equals(enteredPasswordHash)) {
                UserSession.clearCurrentUser();
                return AuthResult.failure("Invalid username or password.");
            }

            // Inactive accounts are valid accounts, but they are not allowed to enter.
            if (!storedUser.isActive()) {
                UserSession.clearCurrentUser();
                return AuthResult.failure("This account is inactive and cannot log in.");
            }

            // After a successful login, we keep the user in memory for later actions.
            UserSession.setCurrentUser(storedUser);
            return AuthResult.success(storedUser);
        } catch (IOException exception) {
            UserSession.clearCurrentUser();
            return AuthResult.failure("The system could not read the stored accounts.");
        }
    }

    public void logout() {
        UserSession.clearCurrentUser();
    }
}
