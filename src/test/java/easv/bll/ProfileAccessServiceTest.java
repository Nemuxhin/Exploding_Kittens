package easv.bll;

import easv.be.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Profile access should be tested separately from JavaFX selection controls.
 * The GUI should ask this service which profiles the current user may select.
 */
class ProfileAccessServiceTest {

    private ProfileAccessService profileAccessService;

    @BeforeEach
    void setUp() {
        profileAccessService = new ProfileAccessService(Map.of(
                "regularUser", Set.of("Medical", "Legal"),
                "limitedUser", Set.of("Archive")
        ));
    }

    @Test
    void admin_shouldHaveAccessToEveryProfile() {
        User admin = new User("admin", "hash", "admin", true);

        assertAll(
                () -> assertTrue(profileAccessService.canAccessProfile(admin, "Medical")),
                () -> assertTrue(profileAccessService.canAccessProfile(admin, "Legal")),
                () -> assertTrue(profileAccessService.canAccessProfile(admin, "AnythingNew"))
        );
    }

    @Test
    void user_shouldOnlyHaveAccessToAssignedProfiles() {
        User regularUser = new User("regularUser", "hash", "user", true);

        assertAll(
                () -> assertTrue(profileAccessService.canAccessProfile(regularUser, "Medical")),
                () -> assertTrue(profileAccessService.canAccessProfile(regularUser, "Legal")),
                () -> assertFalse(profileAccessService.canAccessProfile(regularUser, "Archive"))
        );
    }

    @Test
    void inactiveUser_shouldNotAccessProfilesEvenWhenAssigned() {
        User inactiveUser = new User("regularUser", "hash", "user", false);

        assertFalse(profileAccessService.canAccessProfile(inactiveUser, "Medical"));
    }

    @Test
    void blankOrMissingProfile_shouldBeDenied() {
        User regularUser = new User("regularUser", "hash", "user", true);

        assertAll(
                () -> assertFalse(profileAccessService.canAccessProfile(regularUser, null)),
                () -> assertFalse(profileAccessService.canAccessProfile(regularUser, "")),
                () -> assertFalse(profileAccessService.canAccessProfile(regularUser, "   "))
        );
    }
}
