package easv.gui.controller;

import easv.bll.AuthManager;
import easv.bll.AuthResult;
import easv.gui.BackgroundExecutor;
import easv.gui.MainApp;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

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
    private Button loginButton;

    @FXML
    private Label messageLabel;

    private AuthManager authManager;
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
        setLoginInProgress(true);
        messageLabel.setText("Signing in...");
        setMessageStyle(true);

        String username = usernameField.getText();
        String password = passwordField.getText();

        CompletableFuture
                .supplyAsync(() -> login(username, password), BackgroundExecutor.io())
                .whenComplete((authResult, throwable) -> Platform.runLater(() -> {
                    setLoginInProgress(false);

                    if (throwable != null) {
                        messageLabel.setText("The system could not complete the login request.");
                        setMessageStyle(false);
                        return;
                    }

                    messageLabel.setText(authResult.getMessage());
                    setMessageStyle(authResult.isSuccess());

                    if (authResult.isSuccess()) {
                        try {
                            mainApp.showMainView(authResult.getUser(), password);
                        } catch (IOException exception) {
                            messageLabel.setText("The user portal could not be opened.");
                            setMessageStyle(false);
                        }
                        return;
                    }

                    passwordField.clear();
                }));
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

    private void setLoginInProgress(boolean inProgress) {
        usernameField.setDisable(inProgress);
        passwordField.setDisable(inProgress);
        if (loginButton != null) {
            loginButton.setDisable(inProgress);
        }
    }

    private AuthResult login(String username, String password) {
        if (authManager == null) {
            authManager = new AuthManager();
        }
        return authManager.login(username, password);
    }
}
