package easv.gui.controller;

import easv.bll.AuthManager;
import easv.bll.AuthResult;
import easv.gui.MainApp;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

/**
 * This controller reads user input from the login screen.
 * Then it asks the AuthManager to check the credentials.
 */
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    private final AuthManager authManager = new AuthManager();
    private MainApp mainApp;

    @FXML
    private void initialize() {
        usernameField.requestFocus();
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void handleLogin() throws IOException {
        AuthResult authResult = authManager.login(usernameField.getText(), passwordField.getText());
        messageLabel.setText(authResult.getMessage());
        setMessageStyle(authResult.isSuccess());

        if (authResult.isSuccess()) {
            mainApp.showMainView(authResult.getUser());
            return;
        }

        // We clear only the password so the user does not need to retype the username.
        passwordField.clear();
    }

    private void setMessageStyle(boolean success) {
        messageLabel.getStyleClass().removeAll("message-error", "message-success");

        // The color helps the user quickly understand if the login was accepted.
        if (success) {
            messageLabel.getStyleClass().add("message-success");
        } else {
            messageLabel.getStyleClass().add("message-error");
        }
    }
}
