package easv.bll;

import easv.be.User;
import easv.dal.AuditLogDAO;
import easv.dal.DatabaseConnection;
import easv.dal.MetadataDAO;
import easv.dal.UserDAO;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminManagerTest {

    @Test
    void assignsAndRemovesProfilesForUserAndPersistsAssignmentQuery() {
        AdminManager adminManager = createAdminManager("admin-assignments");

        User targetUser = adminManager.getUsers().stream()
                .filter(user -> "scanner".equalsIgnoreCase(user.getUsername()))
                .findFirst()
                .orElseThrow();
        int profileId = adminManager.getProfiles().get(0).getId();

        adminManager.saveProfileAssignments(Map.of(profileId, Set.of(targetUser.getId())));

        assertEquals(Set.of(targetUser.getId()), adminManager.getAssignedUserIds(profileId));
        assertEquals(1, adminManager.getAssignedProfileIds(targetUser.getId()).size());
        assertTrue(adminManager.getUsers().stream()
                .filter(user -> user.getId() == targetUser.getId())
                .findFirst()
                .orElseThrow()
                .getAssignedProfiles()
                .contains(adminManager.getProfiles().get(0).getName()));

        adminManager.saveProfileAssignments(Map.of());

        assertTrue(adminManager.getAssignedUserIds(profileId).isEmpty());
        assertTrue(adminManager.getAssignedProfileIds(targetUser.getId()).isEmpty());
    }

    @Test
    void rejectsInvalidProfileIdsWhenSavingAssignments() {
        AdminManager adminManager = createAdminManager("admin-invalid-assignments");
        int userId = adminManager.getUsers().get(0).getId();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> adminManager.saveProfileAssignments(Map.of(9999, Set.of(userId))));

        assertEquals("Profile could not be found.", exception.getMessage());
    }

    @Test
    void updatesAndDeletesUserRecordsInDatabase() {
        AdminManager adminManager = createAdminManager("admin-user-crud");
        String profileName = adminManager.getProfiles().get(0).getName();

        User createdUser = adminManager.createUser(new AdminManager.UserInput(
                "Case Worker",
                "caseworker",
                "caseworker@example.com",
                "User",
                "Active",
                java.util.List.of(profileName),
                "case1234"
        ));

        User updatedUser = adminManager.updateUser(createdUser.getId(), new AdminManager.UserInput(
                "Case Worker Updated",
                "caseworker",
                "caseworker.updated@example.com",
                "User",
                "Inactive",
                java.util.List.of(),
                ""
        ));

        assertEquals("Case Worker Updated", updatedUser.getName());
        assertEquals("Inactive", updatedUser.getStatus());
        assertTrue(updatedUser.getAssignedProfiles().isEmpty());

        adminManager.deleteUser(updatedUser.getId());
        assertTrue(adminManager.getUsers().stream().noneMatch(user -> user.getId() == updatedUser.getId()));
    }

    private DatabaseConnection createDatabase(String databaseName) {
        return new DatabaseConnection("jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    private AdminManager createAdminManager(String databaseName) {
        DatabaseConnection connection = createDatabase(databaseName);
        return new AdminManager(
                new UserDAO(connection),
                new MetadataDAO(connection),
                new AuditLogDAO(connection)
        );
    }
}
