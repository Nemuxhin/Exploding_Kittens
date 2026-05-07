package easv.gui;

import easv.bll.KeyboardShortcut;
import easv.bll.ShortcutManager;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * This is the JavaFX entry point.
 * We start with the login screen and switch scenes after a successful login.
 */
public class MainApp extends Application {

    private Stage primaryStage;
    private final ShortcutManager shortcutManager = new ShortcutManager();

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

    public void showScanWorkspaceView() {
        ScanWorkspaceView scanWorkspaceView = new ScanWorkspaceView();
        Parent scanRoot = scanWorkspaceView.createView(this);
        setScene(scanRoot, "WebLager Scan Workspace");
    }

    private void setScene(Parent root, String title) {
        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        registerKeyboardShortcuts(scene);

        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
    }

    private void registerKeyboardShortcuts(Scene scene) {
        List<KeyboardShortcut> shortcuts = shortcutManager.getShortcuts();

        for (KeyboardShortcut shortcut : shortcuts) {
            Runnable shortcutAction = createShortcutAction(shortcut);
            scene.getAccelerators().put(KeyCombination.valueOf(shortcut.getKeyCombination()), shortcutAction);
        }
    }

    private Runnable createShortcutAction(KeyboardShortcut shortcut) {
        if ("Shortcut help".equals(shortcut.getActionName())) {
            return () -> AlertHelper.showShortcutHelp(shortcutManager.getShortcuts());
        }

        // The real scan actions are not implemented yet, so the shortcut is ready for later connection.
        return () -> AlertHelper.showInformation(
                shortcut.getActionName(),
                shortcut.getActionName() + " shortcut is registered, but this action is not connected yet."
        );
    }

    public static void main(String[] args) {
        launch(args);
    }
}
