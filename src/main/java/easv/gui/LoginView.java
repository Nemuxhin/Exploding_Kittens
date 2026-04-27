package easv.gui;

import easv.gui.controller.LoginController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

/**
 * This class loads the login FXML file and connects it to the main app.
 */
public class LoginView {

    public Parent load(MainApp mainApp) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/LoginViews/login-view.fxml"));
        Parent root = loader.load();

        LoginController controller = loader.getController();
        controller.setMainApp(mainApp);

        return root;
    }
}
