package easv.gui.controller.admin;

enum AdminPage {
    DASHBOARD("/view/AdminViews/dashboard-view.fxml", "Dashboard"),
    USERS("/view/AdminViews/manage-users-view.fxml", "Users"),
    PROFILES("/view/AdminViews/manage-profiles-view.fxml", "Profiles"),
    ASSIGNMENTS("/view/AdminViews/assignments-view.fxml", "Assignments"),
    METADATA_TEMPLATES("/view/AdminViews/metadata-view.fxml", "Metadata Templates"),
    REVIEW("/view/AdminViews/review-view.fxml", "Review Center"),
    ACTIVITY("/view/AdminViews/activity-view.fxml", "Activity");

    private final String fxmlPath;
    private final String title;

    AdminPage(String fxmlPath, String title) {
        this.fxmlPath = fxmlPath;
        this.title = title;
    }

    String fxmlPath() {
        return fxmlPath;
    }

    String title() {
        return title;
    }
}
