package easv.bll;

import easv.be.User;
import easv.dal.AuditLogDAO;
import easv.dal.UserDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthManagerTest {

    @AfterEach
    void clearSession() {
        UserSession.clearCurrentUser();
    }

    @Test
    void loginSucceedsForValidActiveUser() {
        UserDAO userDAO = new FakeUserDAO(Map.of(
                "admin", user("admin", "admin123", true)
        ));
        AuthManager authManager = new AuthManager(userDAO, new AuditLogManager(AuditLogDAO.inMemory()));

        AuthResult authResult = authManager.login("admin", "admin123");

        assertTrue(authResult.isSuccess());
        assertNotNull(authResult.getUser());
        assertEquals("admin", authResult.getUser().getUsername());
        assertTrue(UserSession.hasCurrentUser());
    }

    @Test
    void loginFailsForWrongPassword() {
        UserDAO userDAO = new FakeUserDAO(Map.of(
                "admin", user("admin", "admin123", true)
        ));
        AuthManager authManager = new AuthManager(userDAO, new AuditLogManager(AuditLogDAO.inMemory()));

        AuthResult authResult = authManager.login("admin", "wrong-password");

        assertFalse(authResult.isSuccess());
        assertEquals("Invalid username or password.", authResult.getMessage());
        assertFalse(UserSession.hasCurrentUser());
    }

    @Test
    void loginFailsForInactiveAccount() {
        UserDAO userDAO = new FakeUserDAO(Map.of(
                "inactive", user("inactive", "inactive123", false)
        ));
        AuthManager authManager = new AuthManager(userDAO, new AuditLogManager(AuditLogDAO.inMemory()));

        AuthResult authResult = authManager.login("inactive", "inactive123");

        assertFalse(authResult.isSuccess());
        assertEquals("This account is inactive and cannot log in.", authResult.getMessage());
        assertFalse(UserSession.hasCurrentUser());
    }

    @Test
    void loginUpgradesLegacyPasswordHashAfterSuccessfulAuthentication() {
        FakeUserDAO userDAO = new FakeUserDAO(Map.of(
                "admin", legacyUser("admin", "admin123", true)
        ));
        AuthManager authManager = new AuthManager(userDAO, new AuditLogManager(AuditLogDAO.inMemory()));

        AuthResult authResult = authManager.login("admin", "admin123");

        assertTrue(authResult.isSuccess());
        assertTrue(userDAO.passwordHashUpdated);
        assertNotNull(userDAO.updatedPasswordHash);
        assertTrue(userDAO.updatedPasswordHash.startsWith("pbkdf2$sha256$"));
        assertTrue(PasswordHasher.verify("admin123", userDAO.updatedPasswordHash));
    }

    private User user(String username, String password, boolean active) {
        return new User(username, PasswordHasher.hash(password), "User", active);
    }

    private User legacyUser(String username, String password, boolean active) {
        return new User(username, PasswordHasher.legacySha256(password), "User", active);
    }

    private static class FakeUserDAO extends UserDAO {
        private final Map<String, User> usersByUsername;
        private boolean passwordHashUpdated;
        private String updatedPasswordHash;

        private FakeUserDAO(Map<String, User> usersByUsername) {
            super(null); // hermetic: never opens a database connection
            this.usersByUsername = usersByUsername;
        }

        @Override
        public User findByUsername(String username) {
            if (username == null) {
                return null;
            }

            return usersByUsername.get(username.trim().toLowerCase(Locale.ROOT));
        }

        @Override
        public void updatePasswordHash(int userId, String passwordHash) {
            passwordHashUpdated = true;
            updatedPasswordHash = passwordHash;
            usersByUsername.values().stream()
                    .filter(user -> user.getId() == userId || userId == 0)
                    .findFirst()
                    .ifPresent(user -> user.setPasswordHash(passwordHash));
        }
    }
}
