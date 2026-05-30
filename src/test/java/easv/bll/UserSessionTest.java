package easv.bll;

import easv.be.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UserSession holds the logged-in user in a static field that many other
 * classes read (AuthManager, AuditLogManager, AdminManager). These tests
 * pin the contract so a future refactor (e.g. thread-local, scoped session)
 * has a target to match.
 */
class UserSessionTest {

    @BeforeEach
    void clearBefore() {
        UserSession.clearCurrentUser();
    }

    @AfterEach
    void clearAfter() {
        UserSession.clearCurrentUser();
    }

    @Test
    void initially_shouldHaveNoCurrentUser() {
        assertAll(
                () -> assertFalse(UserSession.hasCurrentUser()),
                () -> assertNull(UserSession.getCurrentUser())
        );
    }

    @Test
    void setCurrentUser_shouldStoreAndReturnTheSameInstance() {
        User alice = new User("alice", "hash", "USER", true);

        UserSession.setCurrentUser(alice);

        assertAll(
                () -> assertTrue(UserSession.hasCurrentUser()),
                () -> assertSame(alice, UserSession.getCurrentUser(), "Same instance should be returned, not a copy.")
        );
    }

    @Test
    void clearCurrentUser_shouldResetTheSession() {
        UserSession.setCurrentUser(new User("alice", "hash", "USER", true));

        UserSession.clearCurrentUser();

        assertAll(
                () -> assertFalse(UserSession.hasCurrentUser()),
                () -> assertNull(UserSession.getCurrentUser())
        );
    }

    @Test
    void setCurrentUser_shouldReplaceThePreviousUser() {
        UserSession.setCurrentUser(new User("alice", "hash", "USER",  true));
        UserSession.setCurrentUser(new User("bob",   "hash", "ADMIN", true));

        assertEquals("bob", UserSession.getCurrentUser().getUsername(),
                "Second setCurrentUser should overwrite the first.");
    }

    @Test
    void clearCurrentUser_shouldBeSafeWhenNoUserPresent() {
        UserSession.clearCurrentUser();
        UserSession.clearCurrentUser();

        assertFalse(UserSession.hasCurrentUser(), "Double-clear should not throw or change state.");
    }

    @Test
    void setCurrentUser_withNull_shouldClearTheSession() {
        UserSession.setCurrentUser(new User("alice", "hash", "USER", true));
        UserSession.setCurrentUser(null);

        assertFalse(UserSession.hasCurrentUser(), "Setting null is equivalent to clearing.");
    }
}
