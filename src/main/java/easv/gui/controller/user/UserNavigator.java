package easv.gui.controller.user;

interface UserNavigator {
    void showPage(UserPage page);

    default void showScan() {
        showPage(UserPage.SCAN);
    }

    default void showMyScans() {
        showPage(UserPage.MY_SCANS);
    }

    default void showAssignedQA() {
        showPage(UserPage.ASSIGNED_QA);
    }

    default void showExports() {
        showPage(UserPage.EXPORTS);
    }

    default void showSettings() {
        showPage(UserPage.SETTINGS);
    }

    static UserNavigator none() {
        return page -> {
        };
    }
}