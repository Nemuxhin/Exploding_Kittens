package easv.gui;

import easv.be.User;
import easv.gui.controller.login.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Exploding_Kittens_App extends Application {
    private Stage stage;

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.stage = primaryStage;
        showLogin();
        stage.show();
    }

    public void showLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/LoginViews/login-view.fxml"));
        Parent root = loader.load();
        LoginController controller = loader.getController();
        controller.setApp(this);
        setScene(root, "Exploding_Kittens Login");
    }

    public void showHomeFor(User user) {
        if (user != null && "ADMIN".equalsIgnoreCase(user.getRole())) {
            setScene(load("/view/AdminViews/admin-view.fxml"), "Exploding_Kittens Admin");
            stage.setMinWidth(1280);
            stage.setMinHeight(760);
            return;
        }

        setScene(load("/view/UserViews/user-view.fxml"), "Exploding_Kittens User");
        stage.setMinWidth(1280);
        stage.setMinHeight(760);
    }

    private Parent load(String resourcePath) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load view " + resourcePath, exception);
        }
    }

    private void setScene(Parent root, String title) {
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root, 1512, 886);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }

        if (scene.getStylesheets().isEmpty()) {
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        }

        stage.setTitle(title);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
