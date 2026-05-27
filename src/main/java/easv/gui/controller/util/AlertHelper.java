package easv.gui.controller.util;

import easv.bll.KeyboardShortcut;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.stage.Window;

import java.util.List;

public class AlertHelper {

    private static final String APP_SHELL_CLASS = "app-shell";
    private static final String ADMIN_DIALOG_PANE_CLASS = "admin-dialog-pane";

    private AlertHelper() {
    }

    public static void showInformation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleAlert(alert);
        alert.showAndWait();
    }

    public static void showShortcutHelp(List<KeyboardShortcut> shortcuts) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Keyboard Shortcuts");

        TextArea shortcutTextArea = new TextArea(buildShortcutText(shortcuts));
        shortcutTextArea.setEditable(false);
        shortcutTextArea.setWrapText(true);
        shortcutTextArea.setPrefWidth(520);
        shortcutTextArea.setPrefHeight(320);

        alert.getDialogPane().setContent(shortcutTextArea);
        styleAlert(alert);
        alert.showAndWait();
    }

    private static void styleAlert(Alert alert) {
        if (alert == null) {
            return;
        }

        DialogPane dialogPane = alert.getDialogPane();
        if (dialogPane == null) {
            return;
        }

        if (!dialogPane.getStyleClass().contains(APP_SHELL_CLASS)) {
            dialogPane.getStyleClass().add(APP_SHELL_CLASS);
        }
        if (!dialogPane.getStyleClass().contains(ADMIN_DIALOG_PANE_CLASS)) {
            dialogPane.getStyleClass().add(ADMIN_DIALOG_PANE_CLASS);
        }

        Window owner = getActiveWindow();
        if (owner != null) {
            alert.initOwner(owner);
            if (owner.getScene() != null) {
                dialogPane.getStylesheets().setAll(owner.getScene().getStylesheets());
                if (owner.getScene().getRoot() != null
                        && owner.getScene().getRoot().getStyleClass().contains("dark")
                        && !dialogPane.getStyleClass().contains("dark")) {
                    dialogPane.getStyleClass().add("dark");
                }
            }
        }
    }

    private static Window getActiveWindow() {
        for (Window window : Window.getWindows()) {
            if (window != null && window.isShowing() && window.isFocused()) {
                return window;
            }
        }

        for (Window window : Window.getWindows()) {
            if (window != null && window.isShowing()) {
                return window;
            }
        }

        return null;
    }

    private static String buildShortcutText(List<KeyboardShortcut> shortcuts) {
        StringBuilder textBuilder = new StringBuilder();

        for (KeyboardShortcut shortcut : shortcuts) {
            textBuilder
                    .append(shortcut.getDisplayKeys())
                    .append(" - ")
                    .append(shortcut.getActionName())
                    .append(System.lineSeparator())
                    .append("  ")
                    .append(shortcut.getDescription())
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }

        return textBuilder.toString();
    }
}
