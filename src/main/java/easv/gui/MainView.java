package easv.gui;

import easv.be.User;
import easv.bll.AuthManager;
import easv.bll.ShortcutManager;
import easv.bll.UserSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * This is a very small post-login screen.
 * Its main job is to prove that the system knows who is logged in.
 */
public class MainView {

    private final ShortcutManager shortcutManager = new ShortcutManager();

    public Parent createView(MainApp mainApp) {
        User currentUser = UserSession.getCurrentUser();

        Label titleLabel = new Label("Login Completed");
        titleLabel.getStyleClass().add("main-title");

        String usernameText = currentUser == null ? "No user available" : currentUser.getUsername();
        String roleText = currentUser == null ? "Unknown role" : currentUser.getRole();
        String activeText = currentUser != null && currentUser.isActive() ? "Active" : "Inactive";

        Label usernameLabel = new Label("Current user: " + usernameText);
        Label roleLabel = new Label("Role: " + roleText);
        Label statusLabel = new Label("Account status: " + activeText);

        Label explanationLabel = new Label(
                "Later actions can read UserSession to know who is using the system."
        );
        explanationLabel.setWrapText(true);
        explanationLabel.getStyleClass().add("helper-text");

        Button shortcutsButton = new Button("Keyboard Shortcuts");
        shortcutsButton.getStyleClass().add("secondary-button");
        shortcutsButton.setOnAction(event -> AlertHelper.showShortcutHelp(shortcutManager.getShortcuts()));

        Button logoutButton = new Button("Log Out");
        logoutButton.getStyleClass().add("primary-button");
        logoutButton.setOnAction(event -> {
            new AuthManager().logout();

            try {
                mainApp.showLoginView();
            } catch (IOException exception) {
                throw new RuntimeException("The login screen could not be opened again.", exception);
            }
        });

        VBox layout = new VBox(16, titleLabel, usernameLabel, roleLabel, statusLabel, explanationLabel, shortcutsButton, logoutButton);
        layout.setAlignment(Pos.CENTER_LEFT);
        layout.setPadding(new Insets(40));
        layout.getStyleClass().add("main-panel");

        return layout;
    }
}
