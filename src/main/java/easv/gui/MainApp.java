package easv.gui;

import easv.be.User;
import easv.bll.UserSession;
import easv.gui.controller.util.LoginController;
import easv.gui.controller.admin.AdminController;
import easv.gui.controller.user.UserController;
import easv.gui.controller.util.PrimeIcons;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainApp extends Application {

    private static final String APP_TITLE = "WebLager";
    private static final String LOGIN_VIEW = "/view/LoginViews/login-view.fxml";
    private static final String ADMIN_VIEW = "/view/AdminViews/admin-view.fxml";
    private static final String USER_VIEW = "/view/UserViews/user-view.fxml";

    private static final String[] LOGIN_STYLESHEETS = {
            "/css/login.css",
            "/css/typography.css"
    };

    private static final String[] ADMIN_STYLESHEETS = {
            "/css/app.css",
            "/css/shell.css",
            "/css/admin-common.css",
            "/css/dashboard.css",
            "/css/admin-profiles.css",
            "/css/admin-review.css",
            "/css/dialogs.css",
            "/css/activity-log.css",
            "/css/typography.css"
    };

    private static final String[] USER_STYLESHEETS = {
            "/css/app.css",
            "/css/shell.css",
            "/css/user-portal.css",
            "/css/user-scan.css",
            "/css/user-qa.css",
            "/css/dialogs.css",
            "/css/typography.css"
    };

    private static final String[] FONT_RESOURCES = {
            "/fonts/Montserrat-Regular.ttf",
            "/fonts/Montserrat-Medium.ttf",
            "/fonts/Montserrat-SemiBold.ttf",
            "/fonts/Montserrat-Bold.ttf",
            "/fonts/primeicons.ttf"
    };

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

        loadApplicationFonts();
        configureStage();
        showLoginView();
        stage.show();
    }

    public void showLoginView() throws IOException {
        UserSession.clearCurrentUser();

        FXMLLoader loader = new FXMLLoader(getRequiredResource(LOGIN_VIEW));
        Parent root = loader.load();

        LoginController controller = loader.getController();
        controller.setMainApp(this);

        showView(root, "WebLager Login", LOGIN_STYLESHEETS);
    }

    public void showMainView(User user) throws IOException {
        UserSession.setCurrentUser(user);

        String view = isAdmin(user) ? ADMIN_VIEW : USER_VIEW;

        FXMLLoader loader = new FXMLLoader(getRequiredResource(view));
        Parent root = loader.load();

        Object controller = loader.getController();

        if (controller instanceof AdminController adminController) {
            adminController.setMainApp(this);
        }

        if (controller instanceof UserController userController) {
            userController.setMainApp(this);
        }

        showView(root, APP_TITLE, isAdmin(user) ? ADMIN_STYLESHEETS : USER_STYLESHEETS);
    }

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private void configureStage() {
        stage.setTitle(APP_TITLE);
        stage.setResizable(true);
        stage.setMinWidth(fitToScreen(MIN_WIDTH, screen.getWidth()));
        stage.setMinHeight(fitToScreen(MIN_HEIGHT, screen.getHeight()));
        setWindowIcon();
    }

    private void showView(Parent root, String title, String[] stylesheets) {
        double width = fitToScreen(PREFERRED_WIDTH, screen.getWidth());
        double height = fitToScreen(PREFERRED_HEIGHT, screen.getHeight());

        Scene scene = new Scene(root, width, height);
        addStylesheets(scene, stylesheets);
        PrimeIcons.applyFont(root);

        stage.setTitle(title);
        stage.setScene(scene);
        centerStage(width, height);

        javafx.application.Platform.runLater(() -> {
            debugTypography(scene, "FIRST CSS PASS");

            javafx.application.Platform.runLater(() ->
                    debugTypography(scene, "SECOND CSS PASS AFTER NEXT PULSE")
            );
        });
    }

    private void addStylesheets(Scene scene, String[] stylesheets) {
        scene.getStylesheets().clear();

        for (String stylesheet : stylesheets) {
            URL stylesheetUrl = getClass().getResource(stylesheet);

            if (stylesheetUrl != null) {
                scene.getStylesheets().add(stylesheetUrl.toExternalForm());
            } else {
                System.out.println("STYLESHEET MISSING: " + stylesheet);
            }
        }
    }

    private void loadApplicationFonts() {
        System.out.println("\n===== FONT LOAD DEBUG =====");

        for (String fontResource : FONT_RESOURCES) {
            URL fontUrl = getClass().getResource(fontResource);

            if (fontUrl == null) {
                System.out.println("FONT RESOURCE MISSING: " + fontResource);
                continue;
            }

            Font loadedFont = Font.loadFont(fontUrl.toExternalForm(), 12);

            if (loadedFont == null) {
                System.out.println("FONT LOAD FAILED: " + fontResource);
                continue;
            }

            System.out.println(
                    "FONT LOADED: " + fontResource
                            + " | family=" + loadedFont.getFamily()
                            + " | name=" + loadedFont.getName()
                            + " | style=" + loadedFont.getStyle()
            );

            if ("/fonts/primeicons.ttf".equals(fontResource)) {
                PrimeIcons.registerFont(loadedFont);
            }
        }

        printRegisteredFontInfo();
    }

    private void printRegisteredFontInfo() {
        System.out.println("\n===== REGISTERED MONTSERRAT FAMILIES =====");
        Font.getFamilies().stream()
                .filter(family -> family.toLowerCase().contains("montserrat"))
                .sorted()
                .forEach(System.out::println);

        System.out.println("\n===== REGISTERED MONTSERRAT FONT NAMES =====");
        Font.getFontNames().stream()
                .filter(fontName -> fontName.toLowerCase().contains("montserrat"))
                .sorted()
                .forEach(System.out::println);

        System.out.println("\n===== REGISTERED PRIMEICON FONT NAMES =====");
        Font.getFontNames().stream()
                .filter(fontName -> fontName.toLowerCase().contains("prime"))
                .sorted()
                .forEach(System.out::println);

        System.out.println("===========================================\n");
    }

    private void debugTypography(Scene scene, String passName) {
        scene.getRoot().applyCss();

        System.out.println("\n===== TYPOGRAPHY RUNTIME DEBUG: " + passName + " =====");

        System.out.println("\nStylesheet order:");
        scene.getStylesheets().forEach(System.out::println);

        String[] selectors = {
                ".page-title",
                ".label.page-title",
                ".page-subtitle",
                ".label.page-subtitle",

                ".dashboard-stat-title",
                ".label.dashboard-stat-title",
                ".dashboard-stat-value",
                ".label.dashboard-stat-value",
                ".dashboard-section-title",
                ".label.dashboard-section-title",
                ".dashboard-activity-title",
                ".label.dashboard-activity-title",

                ".admin-top-nav-label",
                ".label.admin-top-nav-label",

                ".login-brand-title",
                ".label.login-brand-title",
                ".login-form-title",
                ".label.login-form-title",
                ".login-helper-text",
                ".label.login-helper-text",
                ".login-field-label",
                ".label.login-field-label",

                ".button",
                ".text-field"
        };

        for (String selector : selectors) {
            var nodes = scene.getRoot().lookupAll(selector);

            System.out.println("\nSelector: " + selector);
            System.out.println("Matches: " + nodes.size());

            nodes.stream()
                    .limit(8)
                    .forEach(node -> {
                        System.out.println("Node type: " + node.getClass().getName());
                        System.out.println("Style classes: " + node.getStyleClass());
                        System.out.println("Node id: " + node.getId());
                        System.out.println("Inline style: " + node.getStyle());

                        if (node instanceof javafx.scene.control.Labeled labeled) {
                            Font font = labeled.getFont();

                            System.out.println("Text: " + labeled.getText());
                            System.out.println("Computed font family: " + font.getFamily());
                            System.out.println("Computed font name: " + font.getName());
                            System.out.println("Computed font style: " + font.getStyle());
                            System.out.println("Computed font size: " + font.getSize());
                        }

                        if (node instanceof javafx.scene.text.Text text) {
                            Font font = text.getFont();

                            System.out.println("Text node content: " + text.getText());
                            System.out.println("Computed font family: " + font.getFamily());
                            System.out.println("Computed font name: " + font.getName());
                            System.out.println("Computed font style: " + font.getStyle());
                            System.out.println("Computed font size: " + font.getSize());
                        }

                        System.out.println("---");
                    });
        }

        System.out.println("===== END TYPOGRAPHY RUNTIME DEBUG: " + passName + " =====\n");
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