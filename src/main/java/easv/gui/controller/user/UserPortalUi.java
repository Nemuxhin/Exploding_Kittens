package easv.gui.controller.user;

import javafx.scene.control.Label;
import javafx.scene.shape.SVGPath;

final class UserPortalUi {
    private UserPortalUi() {
    }

    static SVGPath buildIcon(String key, String styleClass) {
        SVGPath icon = new SVGPath();
        icon.setContent(iconPath(key));
        icon.getStyleClass().add(styleClass);
        return icon;
    }

    static Label buildStatusChip(String status) {
        String labelText = status == null || status.isBlank() ? "Unknown" : status;
        Label label = new Label(labelText);
        label.getStyleClass().add("status-chip");
        String normalized = labelText.trim().toLowerCase().replace(' ', '-');
        label.getStyleClass().add("status-" + normalized);
        return label;
    }

    private static String iconPath(String key) {
        return switch (key) {
            case "dashboard" -> "M10 2L2 8h2v8h4v-5h4v5h4V8h2L10 2z";
            case "scan" -> "M3 3h14v14H3z M9 6h2v3h3v2h-3v3H9v-3H6V9h3z";
            case "scans" -> "M5 2h7l5 5v11H5z M11 2v5h5";
            case "exports" -> "M3 5h14v4h-2V7H5v2H3z M5 11h10v5H5z M9 2h2v5h3l-4 4-4-4h3z";
            case "settings" -> "M9 2h2l.5 2.1 1.9.8 1.8-1 1.4 1.4-1 1.8.8 1.9L18 9v2l-2.1.5-.8 1.9 1 1.8-1.4 1.4-1.8-1-.9 1.9L11 18H9l-.5-2.1-1.9-.8-1.8 1L3.4 14.7l1-1.8L3.6 11 2 10V8l2.1-.5.8-1.9-1-1.8L5.3 2.4l1.8 1 .9-1.9z M10 7a3 3 0 100 6 3 3 0 000-6z";
            case "help" -> "M10 2a8 8 0 100 16 8 8 0 000-16zm0 12h1v1H9v-1h1zm2.1-6.8c0 1.8-2.1 2.1-2.1 3.8H8.5c0-2.4 1.9-2.5 1.9-3.8 0-.7-.6-1.2-1.4-1.2-.8 0-1.4.4-1.8 1.2L6 6.6C6.6 5.3 7.8 4.5 9.4 4.5c1.7 0 2.7 1 2.7 2.7z";
            case "clock" -> "M10 2a8 8 0 100 16 8 8 0 000-16zm1 4H9v5l4 2 1-1-3-1.5V6z";
            case "account" -> "M10 2a3 3 0 110 6 3 3 0 010-6zm0 8c3 0 5 1.5 5 4v2H5v-2c0-2.5 2-4 5-4z";
            case "user" -> "M10 2a3 3 0 110 6 3 3 0 010-6zm0 8c3 0 5 1.5 5 4v2H5v-2c0-2.5 2-4 5-4z";
            case "shortcuts" -> "M3 5h14v10H3z M5 7h2v2H5z M8 7h2v2H8z M11 7h2v2h-2z M14 7h1v2h-1z M5 10h8v2H5z M14 10h1v2h-1z";
            case "notifications" -> "M10 2a4 4 0 00-4 4v2.7L4.7 11v1h10.6v-1L14 8.7V6a4 4 0 00-4-4zm0 14a2 2 0 001.9-1.3H8.1A2 2 0 0010 16z";
            case "scanning" -> "M4 2h3v2H4v3H2V4c0-1.1.9-2 2-2zm9 0h3c1.1 0 2 .9 2 2v3h-2V4h-3V2zM2 13h2v3h3v2H4c-1.1 0-2-.9-2-2v-3zm14 0h2v3c0 1.1-.9 2-2 2h-3v-2h3v-3z";
            case "privacy" -> "M4 3h12v4H4z M4 9h12v4H4z M4 15h12v2H4z";
            case "save" -> "M4 2h10l2 2v12H4z M7 2v4h6V2z M7 11h6v3H7z";
            case "download" -> "M9 2h2v7h3l-4 4-4-4h3z M4 14h12v2H4z";
            case "logo" -> "M4 2h3v2H4v3H2V4c0-1.1.9-2 2-2zm9 0h3c1.1 0 2 .9 2 2v3h-2V4h-3V2zM2 13h2v3h3v2H4c-1.1 0-2-.9-2-2v-3zm14 0h2v3c0 1.1-.9 2-2 2h-3v-2h3v-3z";
            default -> "M4 4h12v12H4z";
        };
    }
}
