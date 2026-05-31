package easv.bll;

import easv.be.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Access control is determined by a user's role, status, and assigned-profile
 * list. These facts live on User: status drives {@link User#isActive()}, and the
 * assigned-profile list must be stored, replaceable, and safely encapsulated.
 */
class UserTest {

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
    void assignedProfiles_cannotBeMutatedThroughTheGetter() {
        User user = new User(0, "alice", "alice", "", "", "USER", "Active", List.of("Medical"), false);

        // The getter may hand back an unmodifiable view (add throws) or a defensive copy
        // (add succeeds on the copy). Either is fine — the contract we actually care about
        // is that mutating the returned list can't change the user's own stored profiles.
        try {
            user.getAssignedProfiles().add("Legal");
        } catch (UnsupportedOperationException ignored) {
            // Unmodifiable view — also acceptable.
        }

        assertEquals(List.of("Medical"), user.getAssignedProfiles(),
                "Mutating the returned list must not change the user's stored profiles.");
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
