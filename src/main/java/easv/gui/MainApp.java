package easv.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainApp extends Application {

    private static final String APP_TITLE = "WebLager";

    private static final String START_VIEW =
            "/view/UserViews/user-view.fxml";

    private static final String WINDOW_ICON =
            "/images/weblager/styleguide/Main Blue/LogoBlue_Logoicon.png";

    private static final double PREFERRED_WIDTH = 1360;
    private static final double PREFERRED_HEIGHT = 820;

    private static final double MIN_WIDTH = 1100;
    private static final double MIN_HEIGHT = 680;

    private static final double SCREEN_MARGIN = 0.95;

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = loadView(START_VIEW);

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();

        double width = fitToScreen(PREFERRED_WIDTH, screen.getWidth());
        double height = fitToScreen(PREFERRED_HEIGHT, screen.getHeight());

        Scene scene = new Scene(root, width, height);

        stage.setTitle(APP_TITLE);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);

        setWindowIcon(stage);
        centerStage(stage, screen, width, height);

        stage.show();
    }

    private Parent loadView(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getRequiredResource(fxmlPath));
        return loader.load();
    }

    private void setWindowIcon(Stage stage) {
        URL iconUrl = getClass().getResource(WINDOW_ICON);

        if (iconUrl != null) {
            stage.getIcons().setAll(new Image(iconUrl.toExternalForm()));
        }
    }

    private void centerStage(
            Stage stage,
            Rectangle2D screen,
            double width,
            double height
    ) {
        stage.setWidth(width);
        stage.setHeight(height);

        stage.setX(screen.getMinX() + (screen.getWidth() - width) / 2);
        stage.setY(screen.getMinY() + (screen.getHeight() - height) / 2);
    }

    private double fitToScreen(double preferredSize, double availableSize) {
        return Math.min(preferredSize, availableSize * SCREEN_MARGIN);
    }

    private URL getRequiredResource(String path) {
        URL resource = getClass().getResource(path);

        if (resource == null) {
            throw new IllegalStateException("Could not find resource: " + path);
        }

        return resource;
    }

    public static void main(String[] args) {
        launch(args);
    }
}