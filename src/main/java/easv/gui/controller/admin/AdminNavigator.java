package easv.gui.controller.admin;

interface AdminNavigator {
    void showPage(AdminPage page);

    default void showDashboard() {
        showPage(AdminPage.DASHBOARD);
    }

    default void showUsers() {
        showPage(AdminPage.USERS);
    }

    default void showProfiles() {
        showPage(AdminPage.PROFILES);
    }

    default void showAssignments() {
        showPage(AdminPage.ASSIGNMENTS);
    }

    default void showReview() {
        showPage(AdminPage.REVIEW);
    }

    default void showExports() {
        showPage(AdminPage.EXPORTS);
    }

    default void showActivity() {
        showPage(AdminPage.ACTIVITY);
    }

    static AdminNavigator none() {
        return page -> {
        };
    }
}
