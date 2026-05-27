package easv.gui;

import easv.bll.KeyboardShortcut;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.List;

public class AlertHelper {

    private static final String APP_SHELL_CLASS = "app-shell";
    private static final String ADMIN_DIALOG_PANE_CLASS = "admin-dialog-pane";
    private static final String INFORMATION_DIALOG_PANE_CLASS = "barcode-alert-dialog-pane";

    private AlertHelper() {
    }

    public static void showInformation(String title, String message) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle(title == null || title.isBlank() ? "Information" : title.trim());
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Node defaultCloseButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (defaultCloseButton != null) {
            defaultCloseButton.setVisible(false);
            defaultCloseButton.setManaged(false);
        }

        Window owner = getActiveWindow();
        if (owner != null) {
            dialog.initOwner(owner);
        }

        styleDialogPane(dialog.getDialogPane(), INFORMATION_DIALOG_PANE_CLASS);
        dialog.getDialogPane().setPrefWidth(620);
        dialog.getDialogPane().setMaxWidth(620);
        dialog.getDialogPane().setGraphic(null);
        dialog.getDialogPane().setContent(createInformationDialogContent(
                dialog,
                dialog.getTitle(),
                message == null || message.isBlank() ? "Operation completed." : message
        ));

        dialog.showAndWait();
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

    private static VBox createInformationDialogContent(Dialog<ButtonType> dialog, String title, String message) {
        VBox root = new VBox();
        root.getStyleClass().add("barcode-alert-dialog-root");

        HBox header = createInformationDialogHeader(dialog, title);
        VBox body = createInformationDialogBody(dialog, message);

        root.getChildren().addAll(header, body);
        return root;
    }

    private static HBox createInformationDialogHeader(Dialog<ButtonType> dialog, String title) {
        Label iconLabel = new Label("W");
        iconLabel.getStyleClass().add("barcode-alert-dialog-brand-label");

        StackPane iconShell = new StackPane(iconLabel);
        iconShell.getStyleClass().add("barcode-alert-dialog-brand-shell");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("barcode-alert-dialog-title");

        Button closeButton = new Button("\u00D7");
        closeButton.getStyleClass().add("barcode-alert-dialog-close-button");
        closeButton.setFocusTraversable(false);
        closeButton.setOnAction(event -> {
            dialog.setResult(ButtonType.CLOSE);
            dialog.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(18, iconShell, titleLabel, spacer, closeButton);
        header.getStyleClass().add("barcode-alert-dialog-header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private static VBox createInformationDialogBody(Dialog<ButtonType> dialog, String message) {
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("barcode-alert-dialog-message");
        messageLabel.setWrapText(true);

        Button okButton = new Button("OK");
        okButton.getStyleClass().add("barcode-alert-dialog-ok-button");
        okButton.setFocusTraversable(false);
        okButton.setOnAction(event -> {
            dialog.setResult(ButtonType.OK);
            dialog.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(16, spacer, okButton);
        actions.getStyleClass().add("barcode-alert-dialog-actions");
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(28, messageLabel, actions);
        body.getStyleClass().add("barcode-alert-dialog-body");
        return body;
    }

    private static void styleAlert(Alert alert) {
        if (alert == null) {
            return;
        }

        Window owner = getActiveWindow();
        if (owner != null) {
            alert.initOwner(owner);
        }

        DialogPane dialogPane = alert.getDialogPane();
        if (dialogPane == null) {
            return;
        }

        styleDialogPane(dialogPane, ADMIN_DIALOG_PANE_CLASS);
    }

    private static void styleDialogPane(DialogPane dialogPane, String paneClass) {
        if (dialogPane == null) {
            return;
        }

        if (!dialogPane.getStyleClass().contains(APP_SHELL_CLASS)) {
            dialogPane.getStyleClass().add(APP_SHELL_CLASS);
        }
        if (paneClass != null && !paneClass.isBlank() && !dialogPane.getStyleClass().contains(paneClass)) {
            dialogPane.getStyleClass().add(paneClass);
        }

        Window owner = getActiveWindow();
        if (owner != null) {
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
