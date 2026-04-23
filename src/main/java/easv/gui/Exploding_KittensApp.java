package easv.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Exploding_KittensApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(new StackPane(), 800, 600);
        primaryStage.setTitle("Exploding Kittens");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}