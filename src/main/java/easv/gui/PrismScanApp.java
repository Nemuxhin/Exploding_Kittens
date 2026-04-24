package easv.gui;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class PrismScanApp extends Application {
    private final UserPortalModel portalModel = new UserPortalModel();
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        Scene scene = new Scene(new javafx.scene.layout.StackPane(), 1280, 860);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        stage.setTitle("Scanning Portal");
        stage.setScene(scene);
        setRoot(new UserPortalView(portalModel).create());
        stage.show();
    }

    private void setRoot(Parent parent) {
        stage.getScene().setRoot(parent);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
