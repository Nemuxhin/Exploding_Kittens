package easv.gui;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * This is the JavaFX entry point.
 * We start with the login screen and switch scenes after a successful login.
 */
public class MainApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.primaryStage = primaryStage;
        showLoginView();
        primaryStage.show();
    }

    public void showLoginView() throws IOException {
        LoginView loginView = new LoginView();
        Parent loginRoot = loginView.load(this);
        setScene(loginRoot, "WebLager Login");
    }

    public void showMainView() {
        MainView mainView = new MainView();
        Parent mainRoot = mainView.createView(this);
        setScene(mainRoot, "WebLager Dashboard");
    }

    private void setScene(Parent root, String title) {
        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
