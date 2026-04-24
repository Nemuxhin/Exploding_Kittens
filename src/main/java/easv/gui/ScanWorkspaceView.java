package easv.gui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ScanWorkspaceView {
    private final UserPortalModel portalModel;
    private final UserPortalModel.PortalSession session;
    private final Consumer<UserPortalModel.PortalSession> onQa;
    private final Runnable onBackToDocuments;
    private final Runnable onDashboard;
    private final ListView<UserPortalModel.PortalDocument> documentList = new ListView<>();
    private final ListView<String> readyFileList = new ListView<>();
    private final Button qaButton = new Button("QA Review");
    private final Label scanCountLabel = new Label();
    private final Label structureLabel = new Label();
    private final Label testStatusLabel = new Label("Drag files from the dashboard list into the scanning field.");
    private final Label selectedFileLabel = new Label("Select a file to assign it to a document.");
    private final ComboBox<UserPortalModel.PortalDocument> targetDocumentComboBox = new ComboBox<>();
    private final VBox documentBoard = new VBox(18);
    private final StackPane dropZoneContainer = new StackPane();
    private final VBox idleDropZone = new VBox(8);
    private final VBox progressDropZone = new VBox(10);
    private final Label scanningPercentLabel = new Label("0%");
    private final DoubleProperty scanningProgress = new SimpleDoubleProperty(0);
    private boolean progressListenerInitialized;
    private UserPortalModel.PortalFile selectedFile;
    private UserPortalModel.PortalFile draggedFile;

    public ScanWorkspaceView(UserPortalModel portalModel, UserPortalModel.PortalSession session, Consumer<UserPortalModel.PortalSession> onQa, Runnable onBackToDocuments, Runnable onDashboard) {
        this.portalModel = portalModel;
        this.session = session;
        this.onQa = onQa;
        this.onBackToDocuments = onBackToDocuments;
        this.onDashboard = onDashboard;
    }

    public Parent create() {
        Label title = new Label("Scanning");
        title.getStyleClass().add("page-title");
        Label context = new Label(session.getProfile().name() + " / " + session.getBox().id());
        context.getStyleClass().add("muted");
        testStatusLabel.getStyleClass().add("muted");
        testStatusLabel.setWrapText(true);

        qaButton.getStyleClass().add("large-sea-button");
        qaButton.setDisable(session.getAllFiles().isEmpty());
        qaButton.setOnAction(event -> onQa.accept(session));

        Button documentsButton = new Button("Back to Documents");
        documentsButton.getStyleClass().add("back-button");
        documentsButton.setOnAction(event -> onBackToDocuments.run());
        Button dashboardButton = new Button("Back to Dashboard");
        dashboardButton.getStyleClass().add("back-button");
        dashboardButton.setOnAction(event -> onDashboard.run());

        HBox actions = new HBox(10, qaButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(16, new VBox(4, title, context), actions);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header.getChildren().get(0), Priority.ALWAYS);

        VBox sidebar = buildSidebar();
        HBox assignmentPanel = buildAssignmentPanel();
        VBox mainArea = buildMainArea();
        VBox centerArea = new VBox(12, assignmentPanel, mainArea);
        VBox.setVgrow(mainArea, Priority.ALWAYS);
        HBox.setHgrow(centerArea, Priority.ALWAYS);
        HBox pageBody = new HBox(18, sidebar, centerArea);
        pageBody.setFillHeight(true);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("page");
        root.setTop(new VBox(12, new HBox(10, documentsButton, dashboardButton), header));
        root.setCenter(pageBody);
        BorderPane.setMargin(header, new Insets(0, 0, 18, 0));

        refreshLists();
        return root;
    }

    private VBox buildSidebar() {
        documentList.getSelectionModel().selectedItemProperty().addListener((obs, oldDoc, document) -> refreshFiles(document));
        readyFileList.getStyleClass().add("scan-file-list");
        readyFileList.setOnDragDetected(event -> {
            String selectedFileName = readyFileList.getSelectionModel().getSelectedItem();
            if (selectedFileName != null) {
                Dragboard dragboard = readyFileList.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(selectedFileName);
                dragboard.setContent(content);
                event.consume();
            }
        });
        readyFileList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String fileName, boolean empty) {
                super.updateItem(fileName, empty);
                if (empty || fileName == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);
                    setGraphic(buildPaperListCard(fileName, getIndex() + 1));
                }
            }
        });
        Label sidebarTitle = new Label("Files To Scan");
        sidebarTitle.getStyleClass().add("scan-sidebar-title");
        VBox sidebar = new VBox(10,
                sidebarTitle,
                readyFileList
        );
        sidebar.getStyleClass().addAll("sidebar", "scan-sidebar");
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(220);
        VBox.setVgrow(readyFileList, Priority.ALWAYS);
        return sidebar;
    }

    private VBox buildMainArea() {
        Label title = new Label("Scanning Area");
        title.getStyleClass().add("page-title");
        StackPane dropZone = buildDropZone();

        ScrollPane scrollPane = new ScrollPane(documentBoard);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("document-scroll");
        scrollPane.setMinHeight(0);

        VBox mainArea = new VBox(16, title, dropZone, scrollPane);
        mainArea.getStyleClass().add("panel");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        VBox.setVgrow(mainArea, Priority.ALWAYS);
        mainArea.setMaxWidth(Double.MAX_VALUE);
        return mainArea;
    }

    private StackPane buildDropZone() {
        Label icon = new Label("Upload");
        icon.getStyleClass().add("drop-icon");
        Label title = new Label("Drop scanned files here");
        title.getStyleClass().add("drop-title");
        Label hint = new Label("Drag files from the left list into this field. A document can contain many files; a file can contain many pages.");
        hint.getStyleClass().add("muted");

        idleDropZone.getChildren().setAll(icon, title, hint);
        idleDropZone.getStyleClass().add("drop-zone");
        idleDropZone.setAlignment(Pos.CENTER);

        Arc progressTrack = new Arc(0, 0, 47, 47, 90, -360);
        progressTrack.setType(ArcType.OPEN);
        progressTrack.getStyleClass().add("scan-progress-track");
        Arc progressArc = new Arc(0, 0, 47, 47, 90, 0);
        progressArc.setType(ArcType.OPEN);
        progressArc.getStyleClass().add("scan-progress-arc");
        if (!progressListenerInitialized) {
            scanningProgress.addListener((obs, oldValue, newValue) -> {
                double clamped = Math.max(0, Math.min(1, newValue.doubleValue()));
                progressArc.setLength(-360 * clamped);
                progressArc.setVisible(clamped > 0.001);
                scanningPercentLabel.setText(Math.max(0, Math.min(100, (int) Math.round(clamped * 100))) + "%");
            });
            scanningProgress.set(0);
            progressArc.setVisible(false);
            progressListenerInitialized = true;
        }
        scanningPercentLabel.getStyleClass().add("scan-progress-percent");
        StackPane ringPane = new StackPane(progressTrack, progressArc, scanningPercentLabel);
        Label progressTitle = new Label("Scanning in Progress");
        progressTitle.getStyleClass().add("scan-progress-title");
        Label progressSubtitle = new Label("Please wait while we process your documents");
        progressSubtitle.getStyleClass().add("muted");
        progressDropZone.getChildren().setAll(ringPane, progressTitle, progressSubtitle);
        progressDropZone.getStyleClass().addAll("drop-zone", "scan-progress-zone");
        progressDropZone.setAlignment(Pos.CENTER);
        progressDropZone.setVisible(false);
        progressDropZone.setManaged(false);

        dropZoneContainer.getChildren().setAll(idleDropZone, progressDropZone);
        dropZoneContainer.setOnDragOver(event -> {
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasString() || dragboard.hasFiles()) {
                event.acceptTransferModes(TransferMode.MOVE, TransferMode.COPY);
            }
            event.consume();
        });
        dropZoneContainer.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            if (dragboard.hasString()) {
                playScanningProgress(List.of(dragboard.getString()));
                success = true;
            } else if (dragboard.hasFiles()) {
                List<String> fileNames = new ArrayList<>();
                for (File file : dragboard.getFiles()) {
                    fileNames.add(file.getName());
                }
                if (!fileNames.isEmpty()) {
                    playScanningProgress(fileNames);
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
        return dropZoneContainer;
    }

    private void playScanningProgress(List<String> droppedFiles) {
        if (droppedFiles == null || droppedFiles.isEmpty()) {
            return;
        }
        scanningPercentLabel.setText("0%");
        scanningProgress.set(0);
        animateDropZoneTransition(idleDropZone, progressDropZone);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(scanningProgress, 0)),
                new KeyFrame(Duration.millis(220),
                        new KeyValue(scanningProgress, 0.24)),
                new KeyFrame(Duration.millis(460),
                        new KeyValue(scanningProgress, 0.57)),
                new KeyFrame(Duration.millis(700),
                        new KeyValue(scanningProgress, 0.82)),
                new KeyFrame(Duration.millis(960), event -> {
                    scanningProgress.set(1);
                    scanningPercentLabel.setText("100%");
                    UserPortalModel.ScanResult lastResult = null;
                    for (String fileName : droppedFiles) {
                        lastResult = portalModel.scanDroppedFile(session, fileName);
                    }
                    refreshLists();
                    if (lastResult != null) {
                        selectFile(lastResult.file());
                        testStatusLabel.setText(droppedFiles.size() == 1
                                ? lastResult.message()
                                : droppedFiles.size() + " dropped file(s) scanned.");
                    }
                    qaButton.setDisable(false);
                    animateDropZoneTransition(progressDropZone, idleDropZone);
                })
        );
        timeline.playFromStart();
    }

    private void animateDropZoneTransition(VBox from, VBox to) {
        to.setOpacity(0);
        to.setScaleX(0.98);
        to.setScaleY(0.98);
        to.setVisible(true);
        to.setManaged(true);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(180), from);
        fadeOut.setFromValue(from.getOpacity());
        fadeOut.setToValue(0);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(180), from);
        scaleOut.setFromX(from.getScaleX());
        scaleOut.setFromY(from.getScaleY());
        scaleOut.setToX(0.985);
        scaleOut.setToY(0.985);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), to);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(220), to);
        scaleIn.setFromX(0.98);
        scaleIn.setFromY(0.98);
        scaleIn.setToX(1);
        scaleIn.setToY(1);

        fadeOut.setOnFinished(event -> {
            from.setVisible(false);
            from.setManaged(false);
            from.setOpacity(1);
            from.setScaleX(1);
            from.setScaleY(1);
            fadeIn.play();
            scaleIn.play();
        });

        fadeOut.play();
        scaleOut.play();
    }

    private HBox buildAssignmentPanel() {
        selectedFileLabel.setWrapText(true);

        targetDocumentComboBox.setMaxWidth(Double.MAX_VALUE);
        targetDocumentComboBox.setPromptText("Choose target document");

        Button assignButton = new Button("Assign Selected File");
        assignButton.getStyleClass().add("primary-button");
        assignButton.setOnAction(event -> {
            UserPortalModel.PortalDocument source = documentList.getSelectionModel().getSelectedItem();
            UserPortalModel.PortalDocument target = targetDocumentComboBox.getSelectionModel().getSelectedItem();
            UserPortalModel.PortalFile file = selectedFile;
            if (source != null && target != null && file != null) {
                portalModel.moveFileToDocument(session, source, target, file);
                testStatusLabel.setText("File ID " + file.getFileId() + " / Ref ID " + file.getReferenceId()
                        + " assigned to Document " + target.getId() + ". File IDs were updated automatically.");
                refreshLists();
                selectFile(file);
            }
        });

        Button createDocumentButton = new Button("Create Empty Document");
        createDocumentButton.setOnAction(event -> {
            UserPortalModel.PortalDocument document = portalModel.createDocument(session);
            refreshLists();
            documentList.getSelectionModel().select(document);
            targetDocumentComboBox.getSelectionModel().select(document);
            testStatusLabel.setText("Document " + document.getId() + " created. Assign files to it from the list.");
        });

        VBox scanBlock = compactInfoBlock("Scans", scanCountLabel, 110);
        VBox structureBlock = compactInfoBlock("Structure", structureLabel, 150);
        VBox statusBlock = compactInfoBlock("Status", testStatusLabel, 190);
        VBox selectedFileBlock = compactInfoBlock("Selected File", selectedFileLabel, 230);
        VBox targetBlock = compactControlBlock("Target Document", targetDocumentComboBox, 170);
        VBox actionBlock = compactActionBlock(assignButton, createDocumentButton);

        HBox panel = new HBox(8, scanBlock, structureBlock, statusBlock, selectedFileBlock, targetBlock, actionBlock);
        panel.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(statusBlock, Priority.ALWAYS);
        HBox.setHgrow(selectedFileBlock, Priority.ALWAYS);
        panel.setMaxWidth(Double.MAX_VALUE);
        assignButton.getStyleClass().add("compact-action-button");
        createDocumentButton.getStyleClass().add("compact-action-button");
        panel.getStyleClass().addAll("panel", "scan-top-panel");
        return panel;
    }

    private VBox compactInfoBlock(String labelText, Label valueLabel, double width) {
        Label label = new Label(labelText);
        label.getStyleClass().add("scan-top-label");
        valueLabel.setWrapText(true);
        VBox box = new VBox(3, label, valueLabel);
        box.getStyleClass().add("scan-top-block");
        box.setPrefWidth(width);
        box.setMinWidth(90);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private VBox compactControlBlock(String labelText, ComboBox<UserPortalModel.PortalDocument> comboBox, double width) {
        Label label = new Label(labelText);
        label.getStyleClass().add("scan-top-label");
        VBox box = new VBox(3, label, comboBox);
        box.getStyleClass().add("scan-top-block");
        box.setPrefWidth(width);
        box.setMinWidth(120);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private VBox compactActionBlock(Button assignButton, Button createDocumentButton) {
        Label label = new Label("Actions");
        label.getStyleClass().add("scan-top-label");
        HBox row = new HBox(6, assignButton, createDocumentButton);
        VBox box = new VBox(3, label, row);
        box.getStyleClass().add("scan-top-block");
        return box;
    }

    private void refreshLists() {
        scanCountLabel.setText("Total scans: " + session.getTotalScans());
        structureLabel.setText("Documents: " + session.getDocuments().size() + " / Files: " + session.getAllFiles().size());
        documentList.setItems(FXCollections.observableArrayList(session.getDocuments()));
        readyFileList.setItems(FXCollections.observableArrayList(session.getReadyFiles()));
        targetDocumentComboBox.setItems(FXCollections.observableArrayList(session.getDocuments()));
        if (!session.getDocuments().isEmpty() && documentList.getSelectionModel().getSelectedItem() == null) {
            documentList.getSelectionModel().selectFirst();
        }
        rebuildDocumentBoard();
    }

    private void refreshFiles(UserPortalModel.PortalDocument document) {
        rebuildDocumentBoard();
    }

    private void selectFile(UserPortalModel.PortalFile file) {
        selectedFile = file;
        for (UserPortalModel.PortalDocument document : session.getDocuments()) {
            if (document.getFiles().contains(file)) {
                documentList.getSelectionModel().select(document);
                updateSelectedFile(file);
                rebuildDocumentBoard();
                return;
            }
        }
        updateSelectedFile(null);
        rebuildDocumentBoard();
    }

    private UserPortalModel.PortalDocument findDocumentForFile(UserPortalModel.PortalFile file) {
        if (file == null) {
            return null;
        }
        for (UserPortalModel.PortalDocument document : session.getDocuments()) {
            if (document.getFiles().contains(file)) {
                return document;
            }
        }
        return null;
    }

    private void updateSelectedFile(UserPortalModel.PortalFile file) {
        selectedFile = file;
        if (file == null) {
            selectedFileLabel.setText("Select a file to assign it to a document.");
            return;
        }
        selectedFileLabel.setText("File ID: " + file.getFileId()
                + "\nReference ID: " + file.getReferenceId()
                + "\nSource file: " + file.getApiFileName()
                + "\nPages in file: " + file.getPageCount()
                + "\nType: " + (file.isBarcode() ? "Barcode split marker" : "Normal file"));
    }

    private void rebuildDocumentBoard() {
        documentBoard.getChildren().clear();
        for (UserPortalModel.PortalDocument document : session.getDocuments()) {
            Label documentTitle = new Label("Document " + document.getId());
            documentTitle.getStyleClass().add("section-title");
            FlowPane cards = new FlowPane(14, 14);
            for (UserPortalModel.PortalFile file : document.getFiles()) {
                VBox card = buildFileCard(file);
                cards.getChildren().add(card);
            }
            VBox documentSection = new VBox(10, documentTitle, cards);
            documentSection.getStyleClass().add("document-section");
            documentBoard.getChildren().add(documentSection);
        }
    }

    private VBox buildFileCard(UserPortalModel.PortalFile file) {
        Label idBadge = new Label("#" + file.getFileId());
        idBadge.getStyleClass().add("file-badge");
        VBox paperPreview = buildPaperPreview();
        Label name = new Label(file.getApiFileName());
        name.setWrapText(true);
        Label ref = new Label("Ref: REF-" + String.format("%03d", file.getReferenceId()));
        ref.getStyleClass().add("muted");
        Label pages = new Label(file.getPageCount() + " page" + (file.getPageCount() == 1 ? "" : "s"));
        pages.getStyleClass().add("muted");
        Label type = new Label(file.isBarcode() ? "Barcode detected - new document" : "Scanned file");
        type.getStyleClass().add(file.isBarcode() ? "barcode-text" : "muted");

        VBox card = new VBox(8, idBadge, paperPreview, name, ref, pages, type);
        card.getStyleClass().add("file-card");
        card.setOnMouseClicked(event -> selectFile(file));
        card.setOnDragDetected(event -> {
            draggedFile = file;
            Dragboard dragboard = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(file.getReferenceId()));
            dragboard.setContent(content);
            event.consume();
        });
        card.setOnDragOver(event -> {
            if (draggedFile != null && draggedFile != file && findDocumentForFile(draggedFile) == findDocumentForFile(file)) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        card.setOnDragDropped(event -> {
            boolean success = false;
            if (draggedFile != null && draggedFile != file) {
                UserPortalModel.PortalDocument document = findDocumentForFile(file);
                UserPortalModel.PortalDocument draggedDocument = findDocumentForFile(draggedFile);
                if (document != null && document == draggedDocument) {
                    reorderWithinDocument(document, draggedFile, file);
                    portalModel.logCustomAction("reordered File " + draggedFile.getFileId() + " inside Document " + document.getId());
                    testStatusLabel.setText("File reordered. File IDs were updated automatically.");
                    refreshLists();
                    selectFile(draggedFile);
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
        card.setOnDragDone(event -> draggedFile = null);
        if (file == selectedFile) {
            card.getStyleClass().add("selected-card");
        }
        return card;
    }

    private void reorderWithinDocument(UserPortalModel.PortalDocument document, UserPortalModel.PortalFile movingFile, UserPortalModel.PortalFile targetFile) {
        int movingIndex = document.getFiles().indexOf(movingFile);
        int targetIndex = document.getFiles().indexOf(targetFile);
        if (movingIndex < 0 || targetIndex < 0 || movingIndex == targetIndex) {
            return;
        }
        document.getFiles().remove(movingIndex);
        if (movingIndex < targetIndex) {
            targetIndex--;
        }
        document.getFiles().add(targetIndex, movingFile);
        renumberSessionFiles();
    }

    private void renumberSessionFiles() {
        int fileId = 1;
        for (UserPortalModel.PortalDocument document : session.getDocuments()) {
            for (UserPortalModel.PortalFile file : document.getFiles()) {
                file.setFileId(fileId++);
            }
        }
    }

    private VBox buildPaperPreview() {
        Label corner = new Label("");
        corner.getStyleClass().add("paper-corner");
        Label lineOne = new Label("");
        lineOne.getStyleClass().add("paper-line");
        Label lineTwo = new Label("");
        lineTwo.getStyleClass().add("paper-line");
        Label lineThree = new Label("");
        lineThree.getStyleClass().add("paper-line-short");

        VBox preview = new VBox(8, corner, lineOne, lineTwo, lineThree);
        preview.getStyleClass().add("paper-preview");
        return preview;
    }

    private VBox buildPaperListCard(String title, int pageNumber) {
        VBox preview = buildSidebarPaperPreview();
        Label pageLabel = new Label(String.valueOf(pageNumber));
        pageLabel.getStyleClass().add("sidebar-paper-number");
        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.getStyleClass().add("sidebar-paper-caption");

        VBox card = new VBox(10, preview, pageLabel, titleLabel);
        card.setAlignment(Pos.TOP_CENTER);
        card.getStyleClass().add("paper-list-card");
        return card;
    }

    private VBox buildSidebarPaperPreview() {
        Label foldedCorner = new Label("");
        foldedCorner.getStyleClass().add("paper-corner");
        Label lineOne = new Label("");
        lineOne.getStyleClass().add("sidebar-paper-line");
        Label lineTwo = new Label("");
        lineTwo.getStyleClass().add("sidebar-paper-line");
        Label lineThree = new Label("");
        lineThree.getStyleClass().add("sidebar-paper-line-short");
        Label lineFour = new Label("");
        lineFour.getStyleClass().add("sidebar-paper-line");
        Label lineFive = new Label("");
        lineFive.getStyleClass().add("sidebar-paper-line");

        VBox preview = new VBox(7, foldedCorner, lineOne, lineTwo, lineThree, lineFour, lineFive);
        preview.getStyleClass().add("sidebar-paper-preview");
        return preview;
    }

}
