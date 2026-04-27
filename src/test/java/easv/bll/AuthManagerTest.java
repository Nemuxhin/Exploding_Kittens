package easv.bll;

import easv.dal.UserDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthManagerTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearSession() {
        UserSession.clearCurrentUser();
    }

    @Test
    void loginSucceedsForValidActiveUser() {
        UserDAO userDAO = new UserDAO(tempDir.resolve("users.txt"));
        AuthManager authManager = new AuthManager(userDAO);

        AuthResult authResult = authManager.login("admin", "admin123");

        assertTrue(authResult.isSuccess());
        assertNotNull(authResult.getUser());
        assertEquals("admin", authResult.getUser().getUsername());
        assertTrue(UserSession.hasCurrentUser());
    }

    @Test
    void loginFailsForWrongPassword() {
        UserDAO userDAO = new UserDAO(tempDir.resolve("users.txt"));
        AuthManager authManager = new AuthManager(userDAO);

        AuthResult authResult = authManager.login("admin", "wrong-password");

        assertFalse(authResult.isSuccess());
        assertEquals("Invalid username or password.", authResult.getMessage());
        assertFalse(UserSession.hasCurrentUser());
    }

    @Test
    void loginFailsForInactiveAccount() {
        UserDAO userDAO = new UserDAO(tempDir.resolve("users.txt"));
        AuthManager authManager = new AuthManager(userDAO);

        AuthResult authResult = authManager.login("inactive", "inactive123");

        assertFalse(authResult.isSuccess());
        assertEquals("This account is inactive and cannot log in.", authResult.getMessage());
        assertFalse(UserSession.hasCurrentUser());
    }
}
