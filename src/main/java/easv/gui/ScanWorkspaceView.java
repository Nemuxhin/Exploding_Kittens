package easv.gui;

import easv.gui.controller.DashboardController;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ScanWorkspaceView {
    private final DashboardController controller;
    private final PortalViewComponents components;

    public ScanWorkspaceView(DashboardController controller, PortalViewComponents components) {
        this.controller = controller;
        this.components = components;
    }

    public VBox buildScanningBody(UserPortalModel.ScanProfile selectedProfile,
                                  String currentBoxId,
                                  List<UserPortalModel.ScanningDocument> scanningDocuments,
                                  List<UserPortalModel.ScanningDocument> scannedWorkspaceDocuments,
                                  UserPortalModel.ScanningDocument activeScanningDocument,
                                  Consumer<UserPortalModel.ScanningDocument> onSelectScanningDocument,
                                  Consumer<UserPortalModel.ScanningFile> onOpenScanningFile,
                                  Runnable onOpenQaReview,
                                  Runnable onScanningChanged,
                                  Runnable onBackToProfiles,
                                  Runnable onScanAllDocuments) {
        VBox body = new VBox(4);
        body.getStyleClass().add("dashboard-body");

        Label heading = new Label("Scanned Files");
        heading.getStyleClass().add("dashboard-heading");
        Label subtitle = new Label(controller.scanProfileName(selectedProfile) + " / " + currentBoxId);
        subtitle.getStyleClass().add("dashboard-subtitle");
        Button backButton = new Button("Back To Profiles");
        backButton.getStyleClass().add("qa-review-button");
        backButton.setOnAction(event -> onBackToProfiles.run());
        Button qaButton = new Button("QA Review");
        qaButton.getStyleClass().add("qa-review-button");
        qaButton.setDisable(scannedWorkspaceDocuments.isEmpty());
        qaButton.setOnAction(event -> onOpenQaReview.run());
        HBox headerRow = new HBox(12, new VBox(2, heading, subtitle), backButton, qaButton);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headerRow.getChildren().get(0), Priority.ALWAYS);

        HBox layout = new HBox(10);

        VBox center = new VBox(8);
        HBox.setHgrow(center, Priority.ALWAYS);

        ComboBox<String> assignDocumentCombo = new ComboBox<>();
        for (UserPortalModel.ScanningDocument document : scannedWorkspaceDocuments.isEmpty() ? scanningDocuments : scannedWorkspaceDocuments) {
            assignDocumentCombo.getItems().add(document.title());
        }
        assignDocumentCombo.setPromptText("Select document");
        assignDocumentCombo.getStyleClass().add("scan-top-combo");
        assignDocumentCombo.setMaxWidth(Double.MAX_VALUE);
        assignDocumentCombo.setMinHeight(30);
        assignDocumentCombo.setPrefHeight(30);
        assignDocumentCombo.setMaxHeight(30);
        if (activeScanningDocument != null) {
            assignDocumentCombo.getSelectionModel().select(activeScanningDocument.title());
        }
        assignDocumentCombo.setOnAction(event -> {
            String selectedTitle = assignDocumentCombo.getSelectionModel().getSelectedItem();
            if (selectedTitle != null) {
                onSelectScanningDocument.accept(controller.findScanningDocument(
                        scannedWorkspaceDocuments.isEmpty() ? scanningDocuments : scannedWorkspaceDocuments,
                        selectedTitle
                ));
            }
        });

        HBox topPanel = new HBox(6,
                compactScanBlock("Scan Date", "2026-04-27"),
                compactScanBlock("Scanned Documents", String.valueOf(scannedWorkspaceDocuments.size())),
                compactScanBlock("Scanned Files", String.valueOf(controller.totalFiles(scannedWorkspaceDocuments))),
                compactScanComboBlock("Assign To Document", assignDocumentCombo)
        );
        topPanel.getStyleClass().add("scan-top-strip");
        topPanel.setMinHeight(70);
        topPanel.setPrefHeight(70);
        topPanel.setMaxHeight(70);

        VBox workspacePanel = new VBox(8);
        workspacePanel.getStyleClass().add("scan-main-panel");
        HBox.setHgrow(workspacePanel, Priority.ALWAYS);
        VBox.setVgrow(workspacePanel, Priority.ALWAYS);

        VBox documentBoard;
        if (scannedWorkspaceDocuments.isEmpty()) {
            Label emptyState = new Label("No document scanned yet.");
            emptyState.getStyleClass().add("scan-empty-state");
            documentBoard = new VBox(emptyState);
            documentBoard.getStyleClass().add("scan-empty-board");
        } else {
            List<VBox> sections = new ArrayList<>();
            for (UserPortalModel.ScanningDocument scannedDocument : scannedWorkspaceDocuments) {
                VBox[] fileCards = new VBox[scannedDocument.files().size()];
                for (int i = 0; i < scannedDocument.files().size(); i++) {
                    UserPortalModel.ScanningFile file = scannedDocument.files().get(i);
                    fileCards[i] = buildFileCard(scannedDocument, file, i, onOpenScanningFile, onScanningChanged);
                }
                sections.add(documentSection(
                        scannedDocument.title(),
                        controller.documentQaStatus(scannedDocument),
                        fileCards
                ));
            }
            documentBoard = new VBox(10);
            documentBoard.getChildren().addAll(sections);
        }

        ScrollPane documentScroll = new ScrollPane(documentBoard);
        documentScroll.setFitToWidth(true);
        documentScroll.getStyleClass().add("scan-document-scroll");
        VBox.setVgrow(documentScroll, Priority.ALWAYS);

        workspacePanel.getChildren().add(documentScroll);
        center.getChildren().addAll(topPanel, workspacePanel);
        VBox.setVgrow(center, Priority.ALWAYS);

        layout.getChildren().add(center);
        HBox.setHgrow(center, Priority.ALWAYS);
        headerRow.getStyleClass().add("scan-header-row");
        body.getChildren().addAll(headerRow, layout);
        return body;
    }

    public VBox buildFilePagesBody(UserPortalModel.ScanProfile selectedProfile,
                                   String currentBoxId,
                                   UserPortalModel.ScanningDocument activeScanningDocument,
                                   UserPortalModel.ScanningFile activeScanningFile,
                                   Runnable onBackToScanning) {
        VBox body = new VBox(12);
        body.getStyleClass().add("dashboard-body");

        Label heading = new Label("File Pages");
        heading.getStyleClass().add("dashboard-heading");
        String context = activeScanningFile == null
                ? "No file selected"
                : controller.scanProfileName(selectedProfile) + " / " + currentBoxId + " / " + activeScanningFile.fileName();
        Label subtitle = new Label(context);
        subtitle.getStyleClass().add("dashboard-subtitle");

        Button backButton = new Button("Back To Scanning");
        backButton.getStyleClass().add("start-scanning-button");
        backButton.setOnAction(event -> onBackToScanning.run());

        VBox panel = new VBox(14);
        panel.getStyleClass().add("scan-main-panel");

        Label detailsTitle = new Label(activeScanningDocument == null || activeScanningFile == null
                ? "Pages"
                : activeScanningDocument.title() + " / " + activeScanningFile.reference().replace("Ref: ", ""));
        detailsTitle.getStyleClass().add("panel-title");

        FlowPane pages = new FlowPane(12, 12);
        if (activeScanningFile != null) {
            List<Integer> pageNumbers = new ArrayList<>();
            for (int i = 1; i <= activeScanningFile.pageCount(); i++) {
                pageNumbers.add(i);
            }
            Runnable[] renderPages = new Runnable[1];
            renderPages[0] = () -> {
                pages.getChildren().clear();
                for (Integer pageNumber : pageNumbers) {
                    pages.getChildren().add(buildPagePreviewCard(pageNumber, pageNumbers, renderPages));
                }
            };
            renderPages[0].run();
        }

        panel.getChildren().addAll(detailsTitle, pages);
        body.getChildren().addAll(heading, subtitle, backButton, panel);
        return body;
    }

    private VBox buildFileCard(UserPortalModel.ScanningDocument document,
                               UserPortalModel.ScanningFile file,
                               int index,
                               Consumer<UserPortalModel.ScanningFile> onOpenScanningFile,
                               Runnable onScanningChanged) {
        Label badge = new Label("File " + (index + 1));
        badge.getStyleClass().add("file-badge");
        VBox preview = components.buildPaperPreview();
        Label name = new Label(file.fileName());
        name.setWrapText(true);
        name.getStyleClass().add("paper-title");
        Label ref = new Label("Reference ID: " + file.reference().replace("Ref: ", ""));
        ref.getStyleClass().add("scan-file-reference");
        Label pageCount = new Label(file.pages());
        pageCount.getStyleClass().add("muted");
        VBox card;
        if (file.barcode()) {
            Label type = new Label("Barcode detected - new document");
            type.getStyleClass().add("barcode-text");
            card = new VBox(8, badge, preview, name, ref, pageCount, type);
        } else {
            card = new VBox(8, badge, preview, name, ref, pageCount);
        }
        card.getStyleClass().add("scan-file-card");
        if (file.barcode()) {
            card.getStyleClass().add("scan-file-card-barcode");
        }
        enableSmoothFileDrag(card, document, file, ignored -> openFilePagesWindow(document, file), onScanningChanged);
        animateFileCard(card);
        return card;
    }

    private VBox buildPagePreviewCard(int pageNumber, List<Integer> workingPages, Runnable[] renderPages) {
        VBox preview = components.buildLargePaperPreview();
        Label title = new Label("Page " + pageNumber);
        title.getStyleClass().add("paper-title");

        VBox card = new VBox(8, preview, title);
        card.getStyleClass().add("scan-page-card");
        enableSmoothPageDrag(card, pageNumber, workingPages, renderPages);
        animateFileCard(card);
        return card;
    }

    private VBox compactScanBlock(String labelText, String valueText) {
        VBox card = new VBox(3);
        card.getStyleClass().add("scan-top-block");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(58);
        card.setPrefHeight(58);
        card.setMaxHeight(58);
        Label label = new Label(labelText);
        label.getStyleClass().add("scan-top-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("scan-top-value");
        card.getChildren().addAll(label, value);
        return card;
    }

    private VBox compactScanComboBlock(String labelText, ComboBox<String> comboBox) {
        VBox card = new VBox(3);
        card.getStyleClass().add("scan-top-block");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(58);
        card.setPrefHeight(58);
        card.setMaxHeight(58);
        Label label = new Label(labelText);
        label.getStyleClass().add("scan-top-label");
        card.getChildren().addAll(label, comboBox);
        return card;
    }

    private VBox documentSection(String titleText, String status, VBox... cards) {
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        HBox header = new HBox(8, title, components.qaStatusBadge(status));
        header.setAlignment(Pos.CENTER_LEFT);
        FlowPane flow = new FlowPane(12, 12);
        flow.getChildren().addAll(cards);
        VBox section = new VBox(8, header, flow);
        section.getStyleClass().add("document-section");
        return section;
    }

    private void animateFileCard(VBox card) {
        card.setOpacity(0);
        card.setTranslateY(8);

        FadeTransition fade = new FadeTransition(Duration.millis(180), card);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(180), card);
        slide.setFromY(8);
        slide.setToY(0);

        fade.play();
        slide.play();
    }

    private void openFilePagesWindow(UserPortalModel.ScanningDocument document, UserPortalModel.ScanningFile file) {
        List<Integer> originalPages = new ArrayList<>();
        for (int i = 1; i <= file.pageCount(); i++) {
            originalPages.add(i);
        }
        List<Integer> workingPages = new ArrayList<>(originalPages);

        Label heading = new Label(file.fileName());
        heading.getStyleClass().add("dashboard-heading");
        Label subtitle = new Label(document.title() + " / " + file.reference().replace("Ref: ", "") + " / " + file.pages());
        subtitle.getStyleClass().add("dashboard-subtitle");

        FlowPane pages = new FlowPane(12, 12);
        Runnable[] renderPages = new Runnable[1];
        renderPages[0] = () -> {
            pages.getChildren().clear();
            for (Integer pageNumber : workingPages) {
                pages.getChildren().add(buildPagePreviewCard(pageNumber, workingPages, renderPages));
            }
        };
        renderPages[0].run();

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("page-window-cancel-button");
        Button saveButton = new Button("Save");
        saveButton.getStyleClass().add("page-window-save-button");

        HBox footer = new HBox(10, cancelButton, saveButton);
        footer.getStyleClass().add("page-window-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox panel = new VBox(14, heading, subtitle, pages);
        panel.getStyleClass().add("scan-main-panel");
        panel.setPadding(new Insets(14));

        Stage stage = new Stage();
        stage.setTitle("File Pages - " + file.fileName());
        BorderPane windowRoot = new BorderPane();
        windowRoot.getStyleClass().add("dashboard-shell");
        windowRoot.setCenter(panel);
        windowRoot.setBottom(footer);
        BorderPane.setMargin(footer, new Insets(0, 14, 14, 14));

        Scene windowScene = new Scene(windowRoot, 760, 560);
        windowScene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        stage.setScene(windowScene);
        stage.setMinWidth(640);
        stage.setMinHeight(480);

        cancelButton.setOnAction(event -> {
            workingPages.clear();
            workingPages.addAll(originalPages);
            stage.close();
        });
        saveButton.setOnAction(event -> stage.close());

        stage.show();
    }

    private void enableSmoothFileDrag(VBox card,
                                      UserPortalModel.ScanningDocument document,
                                      UserPortalModel.ScanningFile file,
                                      Consumer<UserPortalModel.ScanningFile> onOpenScanningFile,
                                      Runnable onScanningChanged) {
        final double[] dragOffsetX = new double[1];
        final double[] dragOffsetY = new double[1];
        final boolean[] dragging = new boolean[1];

        card.setOnMousePressed(event -> {
            dragOffsetX[0] = event.getSceneX();
            dragOffsetY[0] = event.getSceneY();
            dragging[0] = false;
        });

        card.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - dragOffsetX[0];
            double deltaY = event.getSceneY() - dragOffsetY[0];
            if (!dragging[0] && Math.hypot(deltaX, deltaY) > 8) {
                dragging[0] = true;
                card.toFront();
                if (!card.getStyleClass().contains("scan-file-card-dragging")) {
                    card.getStyleClass().add("scan-file-card-dragging");
                }
            }
            if (!dragging[0]) {
                return;
            }
            card.setTranslateX(deltaX);
            card.setTranslateY(deltaY);
        });

        card.setOnMouseReleased(event -> {
            if (!dragging[0]) {
                onOpenScanningFile.accept(file);
                return;
            }
            Node targetNode = findClosestFileCard(card, event.getSceneX(), event.getSceneY());
            card.setTranslateX(0);
            card.setTranslateY(0);
            card.getStyleClass().remove("scan-file-card-dragging");
            dragging[0] = false;

            if (targetNode instanceof VBox targetCard) {
                String targetReference = (String) targetCard.getProperties().get("referenceId");
                UserPortalModel.ScanningDocument targetDocument = (UserPortalModel.ScanningDocument) targetCard.getProperties().get("scanningDocument");
                if (targetReference != null && controller.moveOrReorderFile(document, targetDocument, file.reference(), targetReference)) {
                    onScanningChanged.run();
                }
            }
        });

        card.getProperties().put("referenceId", file.reference());
        card.getProperties().put("scanningDocument", document);
    }

    private void enableSmoothPageDrag(VBox card,
                                      int pageNumber,
                                      List<Integer> workingPages,
                                      Runnable[] renderPages) {
        final double[] dragOffsetX = new double[1];
        final double[] dragOffsetY = new double[1];
        final boolean[] dragging = new boolean[1];

        card.setOnMousePressed(event -> {
            dragOffsetX[0] = event.getSceneX();
            dragOffsetY[0] = event.getSceneY();
            dragging[0] = false;
        });

        card.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - dragOffsetX[0];
            double deltaY = event.getSceneY() - dragOffsetY[0];
            if (!dragging[0] && Math.hypot(deltaX, deltaY) > 8) {
                dragging[0] = true;
                card.toFront();
                if (!card.getStyleClass().contains("scan-page-card-dragging")) {
                    card.getStyleClass().add("scan-page-card-dragging");
                }
            }
            if (!dragging[0]) {
                return;
            }
            card.setTranslateX(deltaX);
            card.setTranslateY(deltaY);
        });

        card.setOnMouseReleased(event -> {
            if (!dragging[0]) {
                return;
            }
            Node targetNode = findClosestPageCard(card, event.getSceneX(), event.getSceneY());
            card.setTranslateX(0);
            card.setTranslateY(0);
            card.getStyleClass().remove("scan-page-card-dragging");
            dragging[0] = false;

            if (targetNode instanceof VBox targetCard) {
                Integer targetPage = (Integer) targetCard.getProperties().get("pageNumber");
                if (targetPage != null && controller.reorderPages(workingPages, pageNumber, targetPage)) {
                    renderPages[0].run();
                }
            }
        });

        card.getProperties().put("pageNumber", pageNumber);
    }

    private Node findClosestFileCard(VBox draggedCard, double sceneX, double sceneY) {
        if (draggedCard.getScene() == null) {
            return null;
        }
        Node closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Node node : draggedCard.getScene().getRoot().lookupAll(".scan-file-card")) {
            if (node == draggedCard) {
                continue;
            }
            if (!node.getProperties().containsKey("referenceId")) {
                continue;
            }
            var bounds = node.localToScene(node.getBoundsInLocal());
            double centerX = (bounds.getMinX() + bounds.getMaxX()) / 2;
            double centerY = (bounds.getMinY() + bounds.getMaxY()) / 2;
            double distance = Math.hypot(sceneX - centerX, sceneY - centerY);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = node;
            }
        }

        return closestDistance <= 140 ? closest : null;
    }

    private Node findClosestPageCard(VBox draggedCard, double sceneX, double sceneY) {
        if (!(draggedCard.getParent() instanceof FlowPane flowPane)) {
            return null;
        }
        Node closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Node node : flowPane.getChildren()) {
            if (node == draggedCard) {
                continue;
            }
            var bounds = node.localToScene(node.getBoundsInLocal());
            double centerX = (bounds.getMinX() + bounds.getMaxX()) / 2;
            double centerY = (bounds.getMinY() + bounds.getMaxY()) / 2;
            double distance = Math.hypot(sceneX - centerX, sceneY - centerY);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = node;
            }
        }

        return closestDistance <= 170 ? closest : null;
    }
}
