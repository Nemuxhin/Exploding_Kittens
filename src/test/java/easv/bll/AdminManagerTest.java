package easv.bll;

import easv.be.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for AdminManager — connected to the real database.
 *
 * WHY REAL DATABASE? (for the exam):
 * ──────────────────────────────────
 * These tests verify that the BLL validation logic works correctly
 * with the real DAL layer connected to the school's SQL Server.
 *
 * The AdminManager is the core BLL class — it handles:
 * - User CRUD operations (with validation)
 * - Profile management
 * - Dashboard statistics
 * - Audit logging
 *
 * REQUIRES: VPN connection to the EASV academic network.
 */
class AdminManagerTest {

    private AdminManager adminManager;

    @BeforeEach
    void setUp() {
        // Uses the real database connection (database.properties)
        adminManager = new AdminManager();
    }

    // ── Validation tests ────────────────────────────────────────────
    // These test the BLL validation layer — no data is written to DB
    // because the validation throws BEFORE the DAO is called.

    @Test
    void createUserRejectsBlankName() {
        AdminManager.UserInput input = new AdminManager.UserInput(
                "",           // blank name — should fail validation
                "testuser",
                "test@test.com",
                "User",
                "Active",
                java.util.List.of(),
                "password123"
        );

        assertThrows(IllegalArgumentException.class, () -> adminManager.createUser(input));
    }

    @Test
    void createUserRejectsBlankUsername() {
        AdminManager.UserInput input = new AdminManager.UserInput(
                "Test User",
                "",           // blank username — should fail validation
                "test@test.com",
                "User",
                "Active",
                java.util.List.of(),
                "password123"
        );

        assertThrows(IllegalArgumentException.class, () -> adminManager.createUser(input));
    }

    @Test
    void createUserRejectsInvalidEmail() {
        AdminManager.UserInput input = new AdminManager.UserInput(
                "Test User",
                "testuser",
                "not-an-email",   // invalid email — should fail validation
                "User",
                "Active",
                java.util.List.of(),
                "password123"
        );

        assertThrows(IllegalArgumentException.class, () -> adminManager.createUser(input));
    }

    @Test
    void createUserRejectsBlankRole() {
        AdminManager.UserInput input = new AdminManager.UserInput(
                "Test User",
                "testuser",
                "test@test.com",
                "",           // blank role — should fail validation
                "Active",
                java.util.List.of(),
                "password123"
        );

        assertThrows(IllegalArgumentException.class, () -> adminManager.createUser(input));
    }

    @Test
    void createUserRejectsBlankStatus() {
        AdminManager.UserInput input = new AdminManager.UserInput(
                "Test User",
                "testuser",
                "test@test.com",
                "User",
                "",           // blank status — should fail validation
                java.util.List.of(),
                "password123"
        );

        assertThrows(IllegalArgumentException.class, () -> adminManager.createUser(input));
    }

    // ── Dashboard tests ─────────────────────────────────────────────
    // The dashboard reads aggregated data from all tables.

    @Test
    void dashboardSummaryIsNotNull() {
        AdminManager.DashboardSummary summary = adminManager.getDashboardSummary();

        assertNotNull(summary);
        assertTrue(summary.getTotalUsers() >= 0);
        assertTrue(summary.getActiveProfiles() >= 0);
    }

    // ── Username check ──────────────────────────────────────────────

    @Test
    void usernameExistsReturnsFalseForBlank() {
        assertFalse(adminManager.usernameExists("", null));
    }

    @Test
    void usernameExistsReturnsFalseForNull() {
        assertFalse(adminManager.usernameExists(null, null));
    }

    // ── User list from DB ───────────────────────────────────────────

    @Test
    void getUsersReturnsNonEmptyList() {
        // The database should have at least one user (the admin)
        assertFalse(adminManager.getUsers().isEmpty());
    }

    // ── Profile list from DB ────────────────────────────────────────

    @Test
    void getProfilesReturnsNonNull() {
        assertNotNull(adminManager.getProfiles());
    }

    // ── Audit log ───────────────────────────────────────────────────

    @Test
    void auditLogIsCreated() {
        int beforeCount = adminManager.getAuditLogs().size();

        adminManager.addAuditLog("Test", "Test action", "TestTarget", "Success", "Test description.");

        assertEquals(beforeCount + 1, adminManager.getAuditLogs().size());
    }
}
