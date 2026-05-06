package easv.bll;

import easv.be.MetadataField;
import easv.be.MetadataReviewRecord;
import easv.dal.DatabaseConnection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminManagerTest {

    @Test
    void persistsUsersProfilesAssignmentsAndTemplates() {
        AdminManager adminManager = new AdminManager(inMemoryDatabase("admin-manager-main"));

        var profile = adminManager.createProfile(new AdminManager.ProfileInput(
                "Standard Scan",
                "PROFILE-1",
                "Default profile",
                "Active",
                "",
                "{boxId}",
                true,
                "Split",
                "Keep",
                "0",
                "Normal",
                "Normal",
                true,
                "PDF",
                true
        ));

        var user = adminManager.createUser(new AdminManager.UserInput(
                "Alice Example",
                "alice",
                "alice@example.com",
                "Admin",
                "Active",
                List.of(profile.getName())
        ));

        var template = adminManager.createMetadataTemplate(new AdminManager.MetadataTemplateInput(
                "Core Template",
                "Main metadata template",
                List.of(profile.getName()),
                List.of(new MetadataField(0, "caseOwner", "Text", true, "Owner")),
                "Active"
        ));

        assertEquals(1, adminManager.getUsers().size());
        assertEquals(1, adminManager.getProfiles().size());
        assertEquals(1, adminManager.getMetadataTemplates().size());
        assertEquals(List.of(profile.getName()), adminManager.getUsers().get(0).getAssignedProfiles());
        assertEquals(List.of(profile.getName()), adminManager.getMetadataTemplates().get(0).getAssignedProfileNames());
        assertEquals(user.getId(), adminManager.getAssignedUserIds(profile.getId()).stream().findFirst().orElseThrow());
        assertEquals(template.getName(), adminManager.getMetadataTemplates().get(0).getName());
    }

    @Test
    void persistsReviewRecordsAndAuditLogs() {
        AdminManager adminManager = new AdminManager(inMemoryDatabase("admin-manager-review"));

        MetadataReviewRecord record = new MetadataReviewRecord(
                "REC-1",
                "CASE-1",
                "Client A",
                "Archive A",
                "Profile A",
                "Template A",
                "Complete",
                "Ready for QA",
                5,
                "Today",
                "Reviewer",
                "Scanner",
                "Today",
                true
        );

        adminManager.saveMetadataReviewRecord(record);
        adminManager.addAuditLog("System", "Admin", "Did something", "Target", "Failed", "Description");

        assertEquals(1, adminManager.getMetadataReviewRecords().size());
        assertEquals("REC-1", adminManager.getMetadataReviewRecords().get(0).getId());
        assertFalse(adminManager.getAuditLogs().isEmpty());
        assertTrue(adminManager.getDashboardSummary().getFailedEvents() >= 1);
    }

    private DatabaseConnection inMemoryDatabase(String name) {
        return new DatabaseConnection("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1", "sa", "");
    }
}
