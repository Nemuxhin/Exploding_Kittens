package easv.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    private Stage stage;
    private final UserPortalModel portalModel = new UserPortalModel();
    private UserPortalModel.PortalSession lastSession;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        Scene scene = new Scene(new javafx.scene.layout.StackPane(), 1180, 760);
        String css = getClass().getResource("/css/app.css").toExternalForm();
        scene.getStylesheets().add(css);
        primaryStage.setTitle("PrismScan User Portal");
        primaryStage.setScene(scene);
        showDashboard();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void showDashboard() {
        setRoot(new UserDashboardView(portalModel, this::showScanning, lastSession).create());
    }

    private void showDocuments(UserPortalModel.PortalSession session) {
        setRoot(new UserDashboardView(
                portalModel,
                this::showScanning,
                lastSession,
                session.getProfile(),
                session.getBox()
        ).create());
    }

    private void showScanning(UserPortalModel.PortalSession session) {
        lastSession = session;
        setRoot(new ScanWorkspaceView(portalModel, session, this::showQa, () -> showDocuments(session), this::showDashboard).create());
    }

    private void showQa(UserPortalModel.PortalSession session) {
        setRoot(new QaReviewView(portalModel, session, this::showExport, () -> showScanning(session), this::showDashboard).create());
    }

    private void showExport(UserPortalModel.PortalSession session) {
        setRoot(new ExportView(portalModel, session, this::showDashboard, () -> showQa(session)).create());
    }

    private void setRoot(javafx.scene.Parent parent) {
        stage.getScene().setRoot(parent);
    }
}
