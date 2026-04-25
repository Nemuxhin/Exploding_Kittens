package easv.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static final String ADMIN_SHELL = "/view/AdminViews/admin-view.fxml";

    private static final String WINDOW_ICON =
            "/images/weblager/styleguide/Main Blue/LogoBlue_Logoicon.png";

    private static final double START_WIDTH = 1360;
    private static final double START_HEIGHT = 820;

    private static final double MIN_WIDTH = 1100;
    private static final double MIN_HEIGHT = 680;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(ADMIN_SHELL));
        Parent root = loader.load();

        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();

        double windowWidth = Math.min(START_WIDTH, visualBounds.getWidth());
        double windowHeight = Math.min(START_HEIGHT, visualBounds.getHeight());

        Scene scene = new Scene(root, windowWidth, windowHeight);

        primaryStage.setTitle("WebLager");
        primaryStage.getIcons().setAll(new Image(getClass().getResourceAsStream(WINDOW_ICON)));
        primaryStage.setScene(scene);

        primaryStage.setResizable(true);
        primaryStage.setMinWidth(Math.min(MIN_WIDTH, visualBounds.getWidth()));
        primaryStage.setMinHeight(Math.min(MIN_HEIGHT, visualBounds.getHeight()));

        primaryStage.setX(visualBounds.getMinX());
        primaryStage.setY(visualBounds.getMinY());
        primaryStage.setWidth(windowWidth);
        primaryStage.setHeight(windowHeight);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}