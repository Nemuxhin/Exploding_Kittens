package easv.gui.controller.user;

import easv.gui.UserPortalModel;

interface UserNavigator {
    void showPage(UserPage page);

    default void showDashboard() {
        showPage(UserPage.DASHBOARD);
    }

    default void showScan() {
        showPage(UserPage.SCAN);
    }

    default void showNewScan() {
        showScan();
    }

    default void showMyScans() {
        showDashboard();
    }

    default void showScans() {
        showDashboard();
    }

    default void showAssignedQA() {
        showPage(UserPage.ASSIGNED_QA);
    }

    default void showAssignedQa() {
        showAssignedQA();
    }

    default void showExports() {
        showPage(UserPage.EXPORTS);
    }

    default void showSettings() {
        showPage(UserPage.SETTINGS);
    }

    default void resumeRecentScan(UserPortalModel.RecentScanItem item) {
        showScan();
    }

    default void resumeHistoryScan(UserPortalModel.HistoryItem item) {
        showScan();
    }

    static UserNavigator none() {
        return page -> {
        };
    }
}
