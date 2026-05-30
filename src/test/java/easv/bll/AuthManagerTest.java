package easv.bll;

import easv.be.User;
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
        AuthManager authManager = new AuthManager(userDAO);

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
        AuthManager authManager = new AuthManager(userDAO);

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
        AuthManager authManager = new AuthManager(userDAO);

        AuthResult authResult = authManager.login("inactive", "inactive123");

        assertFalse(authResult.isSuccess());
        assertEquals("This account is inactive and cannot log in.", authResult.getMessage());
        assertFalse(UserSession.hasCurrentUser());
    }

    private User user(String username, String password, boolean active) {
        return new User(username, PasswordHasher.hash(password), "User", active);
    }

    private static class FakeUserDAO extends UserDAO {
        private final Map<String, User> usersByUsername;

        private FakeUserDAO(Map<String, User> usersByUsername) {
            super(null, false); // hermetic: never opens a database connection
            this.usersByUsername = usersByUsername;
        }

        @Override
        public User findByUsername(String username) {
            if (username == null) {
                return null;
            }

            return usersByUsername.get(username.trim().toLowerCase(Locale.ROOT));
        }
    }
}
