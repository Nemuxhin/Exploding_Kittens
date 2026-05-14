package easv.bll;

import easv.dal.UserDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
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
        UserDAO userDAO = new FakeUserDAO();
        AuthManager authManager = new AuthManager(userDAO);

        AuthResult authResult = authManager.login("admin", "admin123");

        assertTrue(authResult.isSuccess());
        assertNotNull(authResult.getUser());
        assertEquals("admin", authResult.getUser().getUsername());
        assertTrue(UserSession.hasCurrentUser());
    }

    @Test
    void loginFailsForWrongPassword() {
        UserDAO userDAO = new FakeUserDAO();
        AuthManager authManager = new AuthManager(userDAO);

        AuthResult authResult = authManager.login("admin", "wrong-password");

        assertFalse(authResult.isSuccess());
        assertEquals("Invalid username or password.", authResult.getMessage());
        assertFalse(UserSession.hasCurrentUser());
    }

    @Test
    void loginFailsForInactiveAccount() {
        UserDAO userDAO = new FakeUserDAO();
        AuthManager authManager = new AuthManager(userDAO);

        AuthResult authResult = authManager.login("inactive", "inactive123");

        assertFalse(authResult.isSuccess());
        assertEquals("This account is inactive and cannot log in.", authResult.getMessage());
        assertFalse(UserSession.hasCurrentUser());
    }

    private static class FakeUserDAO extends UserDAO {
        private final Map<String, easv.be.User> users = new HashMap<>();

        private FakeUserDAO() {
            users.put("admin", new easv.be.User("admin", PasswordHasher.hash("admin123"), "Admin", true));
            users.put("inactive", new easv.be.User("inactive", PasswordHasher.hash("inactive123"), "User", false));
        }

        @Override
        public easv.be.User findByUsername(String username) {
            if (username == null) {
                return null;
            }

            return users.get(username.trim().toLowerCase());
        }
    }
}
