package easv.bll;

import easv.be.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Profile access control is determined by a user's role, status, and
 * assigned-profile list. These facts live on User and are tested here.
 *
 * A ProfileAccessService can be extracted later — for now the controller
 * reads these fields directly, so pinning their behaviour is the right scope.
 */
class ProfileAccessServiceTest {

    @Test
    void activeUser_shouldHaveActiveStatus() {
        User user = new User("alice", "hash", "USER", true);
        assertTrue(user.isActive());
    }

    @Test
    void inactiveUser_shouldNotBeActive() {
        User user = new User("alice", "hash", "USER", false);
        assertFalse(user.isActive());
    }

    @Test
    void isActive_shouldBeCaseInsensitive() {
        User upper = new User(0, "alice", "alice", "", "", "USER", "ACTIVE",   List.of(), false);
        User lower = new User(0, "alice", "alice", "", "", "USER", "active",   List.of(), false);
        User mixed = new User(0, "alice", "alice", "", "", "USER", "Inactive", List.of(), false);

        assertAll(
                () -> assertTrue(upper.isActive(),  "ACTIVE in upper-case should be considered active."),
                () -> assertTrue(lower.isActive(),  "active in lower-case should be considered active."),
                () -> assertFalse(mixed.isActive(), "Inactive should not be active.")
        );
    }

    @Test
    void adminRole_shouldBeDifferentFromUserRole() {
        User admin = new User("admin", "hash", "ADMIN", true);
        User user  = new User("user",  "hash", "USER",  true);

        assertAll(
                () -> assertEquals("ADMIN", admin.getRole()),
                () -> assertNotEquals("ADMIN", user.getRole())
        );
    }

    @Test
    void assignedProfiles_shouldBeStoredAndReadBack() {
        User user = new User(0, "alice", "alice", "", "", "USER", "Active", List.of("Medical", "Legal"), false);

        assertAll(
                () -> assertEquals(2, user.getAssignedProfiles().size()),
                () -> assertTrue(user.getAssignedProfiles().contains("Medical")),
                () -> assertTrue(user.getAssignedProfiles().contains("Legal"))
        );
    }

    @Test
    void nullAssignedProfiles_shouldBeStoredAsEmptyList() {
        User user = new User(0, "alice", "alice", "", "", "USER", "Active", null, false);
        assertTrue(user.getAssignedProfiles().isEmpty());
    }

    @Test
    void assignedProfiles_shouldBeImmutable() {
        User user = new User("alice", "hash", "USER", true);
        assertThrows(UnsupportedOperationException.class,
                () -> user.getAssignedProfiles().add("Medical"),
                "Assigned profiles should not be modifiable from outside.");
    }

    @Test
    void setAssignedProfiles_shouldReplaceExistingList() {
        User user = new User(0, "alice", "alice", "", "", "USER", "Active", List.of("Medical"), false);
        user.setAssignedProfiles(List.of("Legal", "Archive"));

        assertAll(
                () -> assertEquals(2, user.getAssignedProfiles().size()),
                () -> assertTrue(user.getAssignedProfiles().contains("Legal")),
                () -> assertFalse(user.getAssignedProfiles().contains("Medical"), "Old profiles replaced.")
        );
    }
}
