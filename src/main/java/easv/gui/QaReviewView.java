package easv.gui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.function.Consumer;

public class QaReviewView {
    private final UserPortalModel portalModel;
    private final UserPortalModel.PortalSession session;
    private final Consumer<UserPortalModel.PortalSession> onExport;
    private final Runnable onBackToScanning;
    private final Runnable onDashboard;
    private final ListView<UserPortalModel.PortalFile> fileList = new ListView<>();
    private final Label pageCounterLabel = new Label("Page 1 of 1");
    private final Label previewFileNameLabel = new Label();
    private final Label previewFileSizeLabel = new Label();
    private final Label fileNameValue = new Label();
    private final Label fileSizeValue = new Label();
    private final Label pagesValue = new Label();
    private final Label rotationValue = new Label();
    private final StackPane previewStage = new StackPane();
    private final Button exportButton = new Button("Export");
    private int currentIndex;
    private int currentPage = 1;

    public QaReviewView(UserPortalModel portalModel, UserPortalModel.PortalSession session, Consumer<UserPortalModel.PortalSession> onExport, Runnable onBackToScanning, Runnable onDashboard) {
        this.portalModel = portalModel;
        this.session = session;
        this.onExport = onExport;
        this.onBackToScanning = onBackToScanning;
        this.onDashboard = onDashboard;
    }

    public Parent create() {
        Label title = new Label("QA Review");
        title.getStyleClass().add("page-title");
        Label context = new Label("Fast file-by-file review for " + session.getExportLabel());
        context.getStyleClass().add("muted");

        Button backButton = new Button("Back To Scanning");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(event -> onBackToScanning.run());
        Button dashboardButton = new Button("Back To Dashboard");
        dashboardButton.getStyleClass().add("back-button");
        dashboardButton.setOnAction(event -> onDashboard.run());
        exportButton.getStyleClass().add("large-sea-button");
        exportButton.setDisable(session.getAllFiles().isEmpty());
        exportButton.setOnAction(event -> {
            for (UserPortalModel.PortalFile file : session.getAllFiles()) {
                file.setQaApproved(true);
            }
            onExport.accept(session);
        });

        HBox header = new HBox(16, new VBox(4, title, context), backButton, dashboardButton, exportButton);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header.getChildren().get(0), Priority.ALWAYS);

        fileList.setItems(FXCollections.observableArrayList(session.getAllFiles()));
        fileList.getSelectionModel().selectedItemProperty().addListener((obs, oldFile, file) -> showFile(file));

        Button rotateButton = new Button("Rotate Page");
        rotateButton.getStyleClass().add("accent-button");
        rotateButton.setOnAction(event -> {
            UserPortalModel.PortalFile file = fileList.getSelectionModel().getSelectedItem();
            if (file != null) {
                file.rotateClockwise();
                showFile(file);
            }
        });

        HBox reviewTop = new HBox(16, pageCounterLabel, rotateButton);
        reviewTop.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(pageCounterLabel, Priority.ALWAYS);

        Rectangle previewBox = new Rectangle(620, 230);
        previewBox.setArcWidth(8);
        previewBox.setArcHeight(8);
        previewBox.getStyleClass().add("qa-preview-box");
        Label fileIcon = new Label("File");
        fileIcon.getStyleClass().add("qa-file-icon");
        VBox previewContent = new VBox(12, fileIcon, previewFileNameLabel, previewFileSizeLabel);
        previewContent.setAlignment(Pos.CENTER);
        previewStage.getChildren().addAll(previewBox, previewContent);
        previewStage.getStyleClass().add("qa-preview-stage");

        GridPane metadata = new GridPane();
        metadata.getStyleClass().add("qa-metadata");
        metadata.setHgap(80);
        metadata.setVgap(16);
        metadata.add(metaBlock("File Name", fileNameValue), 0, 0);
        metadata.add(metaBlock("File Size", fileSizeValue), 1, 0);
        metadata.add(metaBlock("Pages", pagesValue), 0, 1);
        metadata.add(metaBlock("Current Rotation", rotationValue), 1, 1);

        Button previousButton = new Button("Previous Page");
        previousButton.setOnAction(event -> stepPage(-1));
        Button nextButton = new Button("Next Page");
        nextButton.setOnAction(event -> stepPage(1));
        HBox pageControls = new HBox(16, previousButton, nextButton);
        pageControls.setAlignment(Pos.CENTER);

        VBox reviewPanel = new VBox(14, reviewTop, previewStage, metadata, pageControls);
        reviewPanel.getStyleClass().add("qa-review-panel");

        Button approveButton = new Button("Approve Document");
        approveButton.getStyleClass().add("approve-button");
        approveButton.setMaxWidth(Double.MAX_VALUE);
        approveButton.setOnAction(event -> {
            for (UserPortalModel.PortalFile file : session.getAllFiles()) {
                file.setQaApproved(true);
            }
            exportButton.setDisable(false);
        });

        Button rejectButton = new Button("Reject & Send Back");
        rejectButton.getStyleClass().add("reject-button");
        rejectButton.setMaxWidth(Double.MAX_VALUE);
        rejectButton.setOnAction(event -> onBackToScanning.run());

        HBox decisions = new HBox(18, approveButton, rejectButton);
        HBox.setHgrow(approveButton, Priority.ALWAYS);
        HBox.setHgrow(rejectButton, Priority.ALWAYS);

        VBox content = new VBox(14, reviewPanel, decisions);
        content.setAlignment(Pos.CENTER);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("page");
        root.setTop(header);
        root.setCenter(scrollPane);
        BorderPane.setMargin(header, new Insets(0, 0, 18, 0));

        if (!fileList.getItems().isEmpty()) {
            fileList.getSelectionModel().selectFirst();
        }
        return root;
    }

    private void showFile(UserPortalModel.PortalFile file) {
        currentIndex = fileList.getSelectionModel().getSelectedIndex();
        if (file == null) {
            pageCounterLabel.setText("Page 0 of 0");
            previewFileNameLabel.setText("No file selected");
            previewFileSizeLabel.setText("");
            fileNameValue.setText("");
            fileSizeValue.setText("");
            pagesValue.setText("");
            rotationValue.setText("");
            return;
        }
        if (currentPage > file.getPageCount()) {
            currentPage = file.getPageCount();
        }
        pageCounterLabel.setText("Page " + currentPage + " of " + file.getPageCount());
        previewFileNameLabel.setText(file.getApiFileName());
        previewFileSizeLabel.setText(mockFileSize(file));
        fileNameValue.setText(file.getApiFileName());
        fileSizeValue.setText(mockFileSize(file));
        pagesValue.setText(String.valueOf(file.getPageCount()));
        rotationValue.setText(file.getRotationDegrees() + " degrees");
        previewStage.setRotate(file.getRotationDegrees());
    }

    private void stepPage(int offset) {
        UserPortalModel.PortalFile file = fileList.getSelectionModel().getSelectedItem();
        if (file == null) {
            return;
        }
        int nextPage = currentPage + offset;
        if (nextPage >= 1 && nextPage <= file.getPageCount()) {
            currentPage = nextPage;
            showFile(file);
            return;
        }
        int nextFile = currentIndex + offset;
        if (nextFile >= 0 && nextFile < fileList.getItems().size()) {
            currentPage = offset < 0 ? fileList.getItems().get(nextFile).getPageCount() : 1;
            fileList.getSelectionModel().select(nextFile);
        }
    }

    private VBox metaBlock(String label, Label valueLabel) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("muted");
        valueLabel.getStyleClass().add("qa-meta-value");
        return new VBox(6, labelNode, valueLabel);
    }

    private String mockFileSize(UserPortalModel.PortalFile file) {
        return (120 + file.getPageCount() * 36) + " KB";
    }
}
