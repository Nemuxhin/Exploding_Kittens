package easv.gui.controller.user;

enum UserPage {
    SCAN("/view/UserViews/scan-view.fxml", "New Scan"),
    MY_SCANS("/view/UserViews/my-scans-view.fxml", "My Scans"),
    ASSIGNED_QA("/view/UserViews/assigned-qa-view.fxml", "Assigned QA"),
    EXPORTS("/view/UserViews/exports-view.fxml", "Exports"),
    SETTINGS("/view/UserViews/settings-view.fxml", "Settings");

    private final String fxmlPath;
    private final String title;

    UserPage(String fxmlPath, String title) {
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