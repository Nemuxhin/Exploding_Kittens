package easv.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Exploding_Kittens_User extends Application {
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        Scene scene = new Scene(new javafx.scene.layout.StackPane(), 1512, 886);
        stage.setTitle("User Dashboard");
        stage.setScene(scene);
        stage.setMinWidth(1280);
        stage.setMinHeight(760);
        setRoot(loadUserPortal());
        stage.show();
    }

    private void setRoot(Parent parent) {
        stage.getScene().setRoot(parent);
    }

    private Parent loadUserPortal() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/UserViews/user-view.fxml"));
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load user portal FXML.", exception);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
