package easv.gui;

import easv.be.User;
import easv.gui.controller.LoginController;
import easv.gui.controller.admin.AdminController;
import easv.gui.controller.user.UserController;
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
    private static final String LOGIN_VIEW = "/view/LoginViews/login-view.fxml";
    private static final String ADMIN_VIEW = "/view/AdminViews/admin-view.fxml";
    private static final String USER_VIEW = "/view/UserViews/user-view.fxml";
    private static final String STYLESHEET = "/css/app.css";

    private static final String WINDOW_ICON =
            "/images/weblager/styleguide/Main Blue/LogoBlue_Logoicon.png";

    private static final double PREFERRED_WIDTH = 1360;
    private static final double PREFERRED_HEIGHT = 820;

    private static final double MIN_WIDTH = 1100;
    private static final double MIN_HEIGHT = 680;

    private static final double SCREEN_MARGIN = 0.95;

    private Stage stage;
    private Rectangle2D screen;

    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;
        this.screen = Screen.getPrimary().getVisualBounds();

        configureStage();
        showLoginView();
        stage.show();
    }

    public void showLoginView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getRequiredResource(LOGIN_VIEW));
        Parent root = loader.load();

        LoginController controller = loader.getController();
        controller.setMainApp(this);

        showView(root, "WebLager Login");
    }

    public void showMainView(User user) throws IOException {
        String view = isAdmin(user) ? ADMIN_VIEW : USER_VIEW;
        showView(loadView(view), APP_TITLE);
    }

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private Parent loadView(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getRequiredResource(fxmlPath));
        Parent root = loader.load();

        Object controller = loader.getController();

        if (controller instanceof AdminController adminController) {
            adminController.setMainApp(this);
        } else if (controller instanceof UserController userController) {
            userController.setMainApp(this);
        }

        return root;
    }

    private void configureStage() {
        stage.setTitle(APP_TITLE);
        stage.setResizable(true);
        stage.setMinWidth(fitToScreen(MIN_WIDTH, screen.getWidth()));
        stage.setMinHeight(fitToScreen(MIN_HEIGHT, screen.getHeight()));
        setWindowIcon();
    }

    private void showView(Parent root, String title) {
        double width = fitToScreen(PREFERRED_WIDTH, screen.getWidth());
        double height = fitToScreen(PREFERRED_HEIGHT, screen.getHeight());

        Scene scene = new Scene(root, width, height);
        addStylesheet(scene);

        stage.setTitle(title);
        stage.setScene(scene);
        centerStage(width, height);
    }

    private void addStylesheet(Scene scene) {
        URL stylesheetUrl = getClass().getResource(STYLESHEET);

        if (stylesheetUrl != null) {
            scene.getStylesheets().setAll(stylesheetUrl.toExternalForm());
        }
    }

    private void setWindowIcon() {
        URL iconUrl = getClass().getResource(WINDOW_ICON);

        if (iconUrl != null) {
            stage.getIcons().setAll(new Image(iconUrl.toExternalForm()));
        }
    }

    private void centerStage(double width, double height) {
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
