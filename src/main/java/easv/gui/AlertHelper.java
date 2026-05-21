package easv.gui;

import easv.bll.KeyboardShortcut;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;

import java.util.List;

public class AlertHelper {

    private AlertHelper() {
    }

    public static void showInformation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
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
        alert.showAndWait();
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
