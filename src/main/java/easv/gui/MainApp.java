package easv.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(loadUserView(), 1260, 760);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        primaryStage.setTitle("WebLager");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1220);
        primaryStage.setMinHeight(740);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private Parent loadUserView() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/UserViews/user-view.fxml"));
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load user dashboard FXML.", exception);
        }
    }
}
