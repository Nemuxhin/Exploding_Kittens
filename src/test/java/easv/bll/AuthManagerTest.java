package easv.bll;

import easv.dal.UserDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import easv.dal.DatabaseConnection;

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
        UserDAO userDAO = new UserDAO(createDatabase("auth-success"));
        AuthManager authManager = new AuthManager(userDAO);

        AuthResult authResult = authManager.login("admin", "admin123");

        assertTrue(authResult.isSuccess());
        assertNotNull(authResult.getUser());
        assertEquals("admin", authResult.getUser().getUsername());
        assertTrue(UserSession.hasCurrentUser());
    }

    @Test
    void loginFailsForWrongPassword() {
        UserDAO userDAO = new UserDAO(createDatabase("auth-wrong-password"));
        AuthManager authManager = new AuthManager(userDAO);

        AuthResult authResult = authManager.login("admin", "wrong-password");

        assertFalse(authResult.isSuccess());
        assertEquals("Invalid username or password.", authResult.getMessage());
        assertFalse(UserSession.hasCurrentUser());
    }

    @Test
    void loginFailsForInactiveAccount() {
        UserDAO userDAO = new UserDAO(createDatabase("auth-inactive"));
        AuthManager authManager = new AuthManager(userDAO);

        AuthResult authResult = authManager.login("inactive", "inactive123");

        assertFalse(authResult.isSuccess());
        assertEquals("This account is inactive and cannot log in.", authResult.getMessage());
        assertFalse(UserSession.hasCurrentUser());
    }

    private DatabaseConnection createDatabase(String databaseName) {
        return new DatabaseConnection("jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1", "sa", "");
    }
}
