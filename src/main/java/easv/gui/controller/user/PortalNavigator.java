package easv.gui.controller.user;

import easv.gui.UserPortalModel;

interface PortalNavigator {
    void showPage(PortalPage page);

    default void showDashboard() {
        showPage(PortalPage.DASHBOARD);
    }

    default void showNewScan() {
        showPage(PortalPage.NEW_SCAN);
    }

    default void showScans() {
        showPage(PortalPage.SCANS);
    }

    default void showAssignedQa() {
    }

    default void showExports() {
        showPage(PortalPage.EXPORTS);
    }

    default void showSettings() {
        showPage(PortalPage.SETTINGS);
    }

    default void resumeRecentScan(UserPortalModel.RecentScanItem item) {
    }

    default void resumeHistoryScan(UserPortalModel.HistoryItem item) {
    }

    static PortalNavigator none() {
        return page -> {
        };
    }
}
