package easv.gui.controller.login;

import easv.bll.AuthManager;
import easv.bll.AuthResult;
import easv.gui.Exploding_Kittens_App;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private final AuthManager authManager = new AuthManager();
    private Exploding_Kittens_App app;

    public void setApp(Exploding_Kittens_App app) {
        this.app = app;
    }

    @FXML
    private void handleLogin() throws IOException {
        AuthResult authResult = authManager.login(usernameField.getText(), passwordField.getText());
        messageLabel.setText(authResult.getMessage());

        if (!authResult.isSuccess()) {
            passwordField.clear();
            return;
        }

        app.showHomeFor(authResult.getUser());
    }
}
