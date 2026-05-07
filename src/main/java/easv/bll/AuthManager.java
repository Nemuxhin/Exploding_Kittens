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
    private final AuthTokenManager authTokenManager = new AuthTokenManager();

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
            String token = authTokenManager.createToken(storedUser);
            return AuthResult.success(storedUser, token);
        } catch (IOException exception) {
            UserSession.clearCurrentUser();
            return AuthResult.failure("The system could not read the stored accounts.");
        }
    }

    public AuthResult createUser(String username, String password, String role, boolean active) {
        try {
            String safeUsername = username == null ? "" : username.trim();
            String safePassword = password == null ? "" : password;
            String safeRole = role == null ? "" : role.trim().toUpperCase();

            // A new account needs all important fields before it can be saved.
            if (safeUsername.isBlank() || safePassword.isBlank() || safeRole.isBlank()) {
                return AuthResult.failure("Username, password, and role are required.");
            }

            if (userDAO.findByUsername(safeUsername) != null) {
                return AuthResult.failure("This username already exists.");
            }

            String passwordHash = PasswordHasher.hash(safePassword);
            User savedUser = new User(safeUsername, passwordHash, safeRole, active);
            userDAO.saveUser(savedUser);

            return AuthResult.successMessage("User account saved.", savedUser);
        } catch (IOException exception) {
            return AuthResult.failure("The system could not save the user account.");
        }
    }

    public AuthResult updateUserPassword(String username, String newPassword) {
        try {
            String safeUsername = username == null ? "" : username.trim();
            String safePassword = newPassword == null ? "" : newPassword;

            // Password updates are optional in edit screens, but if sent they must be valid.
            if (safeUsername.isBlank() || safePassword.isBlank()) {
                return AuthResult.failure("Username and new password are required.");
            }

            User storedUser = userDAO.findByUsername(safeUsername);

            if (storedUser == null) {
                return AuthResult.failure("User account was not found.");
            }

            String passwordHash = PasswordHasher.hash(safePassword);
            User updatedUser = userDAO.updatePasswordHash(safeUsername, passwordHash);

            return AuthResult.successMessage("Password updated.", updatedUser);
        } catch (IOException exception) {
            return AuthResult.failure("The system could not update the password.");
        }
    }

    public void logout() {
        UserSession.clearCurrentUser();
    }
}
