import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import java.io.File;
import java.util.concurrent.CountDownLatch;

public class InspectSidebar3 {
    public static void main(String[] args) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(() -> {});
        Platform.runLater(() -> {
            try {
                File fxml = new File("C:/Users/Pheas/Documents/GitHub/Exploding_Kittens/target/classes/view/AdminViews/admin-view.fxml");
                Parent root = FXMLLoader.load(fxml.toURI().toURL());
                new Scene(root);
                root.applyCss();
                BorderPane shell = (BorderPane) root;
                BorderPane sidebar = (BorderPane) shell.getLeft();
                VBox top = (VBox) sidebar.getTop();
                VBox nav = (VBox) top.getChildren().get(1);
                HBox active = (HBox) nav.getChildren().get(1);
                System.out.println("Style class count: " + active.getStyleClass().size());
                for (int i = 0; i < active.getStyleClass().size(); i++) {
                    System.out.println("Class[" + i + "]=" + active.getStyleClass().get(i));
                }
                System.out.println("Background: " + active.getBackground());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        latch.await();
        Platform.exit();
    }
}
