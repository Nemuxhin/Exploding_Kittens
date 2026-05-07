package easv.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    private final UserPortalModel portalModel = new UserPortalModel();

    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(new UserDashboardView(portalModel).create(), 1260, 760);
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
}
