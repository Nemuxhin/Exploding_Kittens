package easv.gui.controller.admin;

import javafx.scene.Node;
import javafx.scene.control.DialogPane;

import java.net.URL;

final class AdminDialogStyler {

    private AdminDialogStyler() {
    }

    static void apply(DialogPane dialogPane, Node ownerNode) {
        URL stylesheetUrl = AdminDialogStyler.class.getResource("/css/app.css");

        if (stylesheetUrl != null && !dialogPane.getStylesheets().contains(stylesheetUrl.toExternalForm())) {
            dialogPane.getStylesheets().add(stylesheetUrl.toExternalForm());
        }

        dialogPane.getStyleClass().removeAll("app-shell", "dark", "create-user-dialog-pane");
        dialogPane.getStyleClass().addAll("app-shell", "create-user-dialog-pane");

        if (isApplicationInDarkMode(ownerNode)) {
            dialogPane.getStyleClass().add("dark");
        }
    }

    private static boolean isApplicationInDarkMode(Node ownerNode) {
        return ownerNode != null
                && ownerNode.getScene() != null
                && ownerNode.getScene().getRoot() != null
                && ownerNode.getScene().getRoot().getStyleClass().contains("dark");
    }
}
