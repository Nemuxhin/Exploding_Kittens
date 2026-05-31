package easv.bll;

import easv.be.User;
import easv.dal.DataAccessException;
import easv.dal.UserDAO;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class contains the login rules.
 * The controller asks this class to validate the username and password.
 */
public class AuthManager {

    private static final Logger LOGGER = Logger.getLogger(AuthManager.class.getName());
    private static final String GENERIC_FAILURE_DESCRIPTION = "Authentication failed.";

    private final UserDAO userDAO;
    private final AuditLogManager auditLogManager;

    public AuthManager() {
        this(new UserDAO(), new AuditLogManager());
    }

    public AuthManager(UserDAO userDAO) {
        this(userDAO, new AuditLogManager());
    }

    public AuthManager(UserDAO userDAO, AuditLogManager auditLogManager) {
        this.userDAO = userDAO;
        this.auditLogManager = auditLogManager;
    }

    public AuthResult login(String username, String password) {
        String safeUsername = username == null ? "" : username.trim();

        try {
            String safePassword = password == null ? "" : password;

            // We reject empty fields early so the user gets a clear message.
            if (safeUsername.isBlank() || safePassword.isBlank()) {
                UserSession.clearCurrentUser();
                recordAuthFailure(safeUsername);
                return AuthResult.failure("Please enter both username and password.");
            }

            User storedUser = userDAO.findByUsername(safeUsername);

            // If the username does not exist, we stop here.
            if (storedUser == null) {
                UserSession.clearCurrentUser();
                recordAuthFailure(safeUsername);
                return AuthResult.failure("Invalid username or password.");
            }

            if (!PasswordHasher.verify(safePassword, storedUser.getPasswordHash())) {
                UserSession.clearCurrentUser();
                recordAuthFailure(safeUsername);
                return AuthResult.failure("Invalid username or password.");
            }

            // Inactive accounts are valid accounts, but they are not allowed to enter.
            if (!storedUser.isActive()) {
                UserSession.clearCurrentUser();
                recordAuthFailure(safeUsername);
                return AuthResult.failure("This account is inactive and cannot log in.");
            }

            upgradeLegacyPasswordHashIfNeeded(storedUser, safePassword);

            // After a successful login, we keep the user in memory for later actions.
            UserSession.setCurrentUser(storedUser);
            recordAuthSuccess(storedUser);
            return AuthResult.success(storedUser);
        } catch (DataAccessException exception) {
            UserSession.clearCurrentUser();
            recordAuthFailure(safeUsername);
            return AuthResult.failure("The system could not read the stored accounts.");
        }
    }

    public void logout() {
        User currentUser = UserSession.getCurrentUser();
        UserSession.clearCurrentUser();

        if (currentUser != null) {
            recordLogout(currentUser);
        }
    }

    private void recordAuthSuccess(User user) {
        if (auditLogManager == null) {
            return;
        }

        try {
            auditLogManager.logAuth(user.getUsername(), AuditLogManager.LOGIN_SUCCESS, "User signed in.");
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Audit write failed for LOGIN_SUCCESS", exception);
        }
    }

    private void recordAuthFailure(String attemptedUsername) {
        if (auditLogManager == null) {
            return;
        }

        try {
            auditLogManager.logAuth(attemptedUsername, AuditLogManager.LOGIN_FAILED, GENERIC_FAILURE_DESCRIPTION);
        } catch (RuntimeException exception) {
            // Audit failure must never block authentication, but it must not
            // vanish either — a silent failed-login swallow hides exactly the
            // signal a security audit cares about.
            LOGGER.log(Level.WARNING, "Audit write failed for LOGIN_FAILED", exception);
        }
    }

    private void recordLogout(User user) {
        if (auditLogManager == null) {
            return;
        }

        try {
            auditLogManager.logAuth(user.getUsername(), AuditLogManager.LOGOUT, "User signed out.");
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Audit write failed for LOGOUT", exception);
        }
    }

    private void upgradeLegacyPasswordHashIfNeeded(User storedUser, String plainTextPassword) {
        if (storedUser == null || !PasswordHasher.needsRehash(storedUser.getPasswordHash())) {
            return;
        }

        try {
            String upgradedHash = PasswordHasher.hash(plainTextPassword);
            userDAO.updatePasswordHash(storedUser.getId(), upgradedHash);
            storedUser.setPasswordHash(upgradedHash);
        } catch (RuntimeException exception) {
            // Login should still succeed even if the opportunistic upgrade
            // fails, but a permanent silent failure leaves SHA-256 hashes in
            // the table forever — log without exposing the plaintext or hash.
            LOGGER.log(Level.WARNING,
                    "Opportunistic password-hash upgrade failed for user id "
                            + storedUser.getId(),
                    exception);
        }
    }
}
