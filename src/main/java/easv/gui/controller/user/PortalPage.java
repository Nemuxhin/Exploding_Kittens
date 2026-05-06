package easv.gui.controller.user;

enum PortalPage {
    DASHBOARD("Dashboard", "Dashboard", "dashboard"),
    NEW_SCAN("New Scan", "Dashboard > New Scan", "scan"),
    SCANS("Scans", "Dashboard > Scans", "scans"),
    EXPORTS("Exports", "Dashboard > Exports", "exports"),
    SETTINGS("Settings", "Dashboard > Settings", "settings");

    private final String label;
    private final String breadcrumb;
    private final String iconKey;

    PortalPage(String label, String breadcrumb, String iconKey) {
        this.label = label;
        this.breadcrumb = breadcrumb;
        this.iconKey = iconKey;
    }

    String label() {
        return label;
    }

    String breadcrumb() {
        return breadcrumb;
    }

    String iconKey() {
        return iconKey;
    }
}
