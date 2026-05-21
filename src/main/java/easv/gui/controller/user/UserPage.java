package easv.gui.controller.user;

enum UserPage {
    DASHBOARD(null, "Dashboard"),
    SCAN("/view/UserViews/scan-view.fxml", "New Scan"),
    MY_SCANS(null, "My Scans"),
    ASSIGNED_QA("/view/UserViews/assigned-qa-view.fxml", "Assigned QA"),
    EXPORTS(null, "Exports"),
    EDIT_PROFILE(null, "Edit User Profile"),
    SETTINGS(null, "Settings");

    private final String fxmlPath;
    private final String title;

    UserPage(String fxmlPath, String title) {
        this.fxmlPath = fxmlPath;
        this.title = title;
    }

    String fxmlPath() {
        return fxmlPath;
    }

    boolean hasFxml() {
        return fxmlPath != null && !fxmlPath.isBlank();
    }

    String title() {
        return title;
    }
}
