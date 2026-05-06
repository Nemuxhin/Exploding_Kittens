package easv.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Exploding_Kittens_Admin extends Application {

    @Override
    public void start(Stage primaryStage) {
        Parent root = loadAdminDashboard();
        Scene scene = new Scene(root, 1512, 886);
        primaryStage.setTitle("Admin Dashboard");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1280);
        primaryStage.setMinHeight(760);
        primaryStage.show();
    }

    private Parent loadAdminDashboard() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AdminViews/admin-view.fxml"));

        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load admin dashboard.", exception);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
