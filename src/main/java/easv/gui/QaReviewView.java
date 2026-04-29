package easv.gui;

import easv.gui.controller.DashboardController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class QaReviewView {
    private final DashboardController controller;
    private final PortalViewComponents components;

    public QaReviewView(DashboardController controller, PortalViewComponents components) {
        this.controller = controller;
        this.components = components;
    }

    public Parent buildQaReviewBody(UserPortalModel.ScanProfile selectedProfile,
                                    String currentBoxId,
                                    List<UserPortalModel.ScanningDocument> scannedWorkspaceDocuments,
                                    UserPortalModel.ScanningDocument activeScanningDocument,
                                    UserPortalModel.ScanningFile activeScanningFile,
                                    int qaCurrentPage,
                                    int qaRotation,
                                    double qaZoom,
                                    double qaPanX,
                                    double qaPanY,
                                    boolean qaHighlight,
                                    boolean qaApproved,
                                    Set<String> qaHighlightedText,
                                    String selectedTiffAction,
                                    Consumer<UserPortalModel.ScanningFile> onSelectQaFile,
                                    Runnable onBackToScanning,
                                    Runnable onDashboard,
                                    Runnable onExport,
                                    Consumer<Integer> onStepPage,
                                    Runnable onRotatePage,
                                    Consumer<Double> onZoomPage,
                                    Consumer<double[]> onPanPage,
                                    Runnable onToggleHighlight,
                                    Consumer<String> onToggleTextHighlight,
                                    Runnable onApprove,
                                    Runnable onReject,
                                    Consumer<String> onSelectTiffAction) {
        BorderPane body = new BorderPane();
        body.getStyleClass().add("dashboard-body");

        List<UserPortalModel.ScanningFile> qaFiles = controller.flattenScannedFiles(scannedWorkspaceDocuments);
        UserPortalModel.ScanningFile selectedFile = activeScanningFile;
        if (selectedFile == null && !qaFiles.isEmpty()) {
            selectedFile = qaFiles.get(0);
        }
        UserPortalModel.ScanningDocument selectedDocument = activeScanningDocument;
        if (selectedDocument == null && selectedFile != null) {
            selectedDocument = controller.findDocumentForFile(scannedWorkspaceDocuments, selectedFile);
        }
        if (selectedDocument == null && !scannedWorkspaceDocuments.isEmpty()) {
            selectedDocument = scannedWorkspaceDocuments.get(0);
        }
        List<UserPortalModel.ScanningFile> documentFiles = selectedDocument == null ? List.of() : selectedDocument.files();

        String profileName = selectedProfile == null ? "Profile" : selectedProfile.title().replace(" Profile", "");
        String documentName = selectedDocument == null ? "Document" : selectedDocument.title();
        String exportFileName = controller.buildExportFileName(profileName, currentBoxId);
        boolean exportReady = !selectedTiffAction.isBlank() && !qaFiles.isEmpty();

        HBox brandBlock = new HBox(components.buildQaHeaderLogo());
        brandBlock.setAlignment(Pos.CENTER_LEFT);

        HBox breadcrumbs = new HBox(10,
                new Label("Profiles"),
                new Label(">"),
                new Label(profileName),
                new Label(">"),
                new Label(currentBoxId.isBlank() ? "Box" : currentBoxId),
                new Label(">"),
                new Label(documentName)
        );
        breadcrumbs.getStyleClass().add("qa-breadcrumbs");
        breadcrumbs.setAlignment(Pos.CENTER_LEFT);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox headerRow = new HBox(14, brandBlock, breadcrumbs, headerSpacer);
        headerRow.getStyleClass().add("qa-app-header");
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Button backButton = new Button("Back To Scanning");
        backButton.getStyleClass().add("page-window-cancel-button");
        backButton.setOnAction(event -> onBackToScanning.run());

        Button dashboardButton = new Button("Back To Dashboard");
        dashboardButton.getStyleClass().add("page-window-cancel-button");
        dashboardButton.setOnAction(event -> onDashboard.run());
        headerRow.getChildren().addAll(backButton, dashboardButton);

        if (qaFiles.isEmpty() || selectedFile == null) {
            VBox emptyPanel = new VBox(new Label("No scanned files available for QA yet."));
            emptyPanel.getStyleClass().add("panel-card");
            emptyPanel.setPadding(new Insets(22));
            body.setTop(headerRow);
            body.setCenter(emptyPanel);
            return body;
        }

        VBox documentsPanel = new VBox(10);
        documentsPanel.getStyleClass().addAll("qa-file-list-panel", "qa-documents-panel");
        Label documentsTitle = new Label("Documents");
        documentsTitle.getStyleClass().add("panel-title");
        Label documentsCount = new Label(String.valueOf(scannedWorkspaceDocuments.size()));
        documentsCount.getStyleClass().add("qa-count-pill");
        HBox documentsHeader = new HBox(8, documentsTitle, documentsCount);
        documentsHeader.setAlignment(Pos.CENTER_LEFT);
        TextField documentSearch = new TextField();
        documentSearch.setPromptText("Search documents");
        documentSearch.getStyleClass().addAll("box-id-field", "qa-side-search");
        FlowPane documentRows = new FlowPane(12, 12);
        documentRows.setAlignment(Pos.TOP_LEFT);
        documentRows.setPrefWrapLength(188);
        for (UserPortalModel.ScanningDocument document : scannedWorkspaceDocuments) {
            documentRows.getChildren().add(buildQaScanningDocumentCard(
                    document,
                    selectedDocument != null && document.title().equals(selectedDocument.title()),
                    controller.documentQaStatus(document),
                    () -> {
                        if (!document.files().isEmpty()) {
                            onSelectQaFile.accept(document.files().get(0));
                        }
                    }
            ));
        }
        ScrollPane documentScroll = new ScrollPane(documentRows);
        documentScroll.setFitToWidth(true);
        documentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        documentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        documentScroll.getStyleClass().add("scan-document-scroll");
        VBox.setVgrow(documentScroll, Priority.ALWAYS);
        documentsPanel.getChildren().addAll(documentsHeader, documentSearch, documentScroll);

        VBox filesPanel = new VBox(10);
        filesPanel.getStyleClass().addAll("qa-file-list-panel", "qa-files-panel");
        Label filesTitle = new Label("Files in " + (selectedDocument == null ? "Document" : selectedDocument.title()));
        filesTitle.getStyleClass().add("panel-title");
        Label filesCount = new Label(String.valueOf(documentFiles.size()));
        filesCount.getStyleClass().add("qa-count-pill");
        HBox filesHeader = new HBox(8, filesTitle, filesCount);
        filesHeader.setAlignment(Pos.CENTER_LEFT);
        FlowPane fileRows = new FlowPane(12, 12);
        fileRows.getStyleClass().add("qa-mini-file-list");
        fileRows.setAlignment(Pos.TOP_LEFT);
        fileRows.setPrefWrapLength(206);
        int fileIndex = 1;
        for (UserPortalModel.ScanningFile file : documentFiles) {
            fileRows.getChildren().add(buildQaScanningFileCard(
                    file,
                    fileIndex++,
                    selectedFile != null && file.reference().equals(selectedFile.reference()),
                    () -> onSelectQaFile.accept(file)
            ));
        }
        ScrollPane fileScroll = new ScrollPane(fileRows);
        fileScroll.setFitToWidth(true);
        fileScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        fileScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        fileScroll.getStyleClass().add("scan-document-scroll");
        VBox.setVgrow(fileScroll, Priority.ALWAYS);
        filesPanel.getChildren().addAll(filesHeader, fileScroll);

        VBox leftColumn = new VBox(10, documentsPanel, filesPanel);
        leftColumn.getStyleClass().add("qa-left-column");
        leftColumn.setPrefWidth(240);
        leftColumn.setMinWidth(240);
        leftColumn.setMaxWidth(240);

        VBox previewPaper = buildQaPreviewPaper(qaCurrentPage, qaHighlight, qaHighlightedText, onToggleTextHighlight);
        previewPaper.setRotate(qaRotation);
        previewPaper.setScaleX(qaZoom);
        previewPaper.setScaleY(qaZoom);
        previewPaper.setTranslateX(qaPanX);
        previewPaper.setTranslateY(qaPanY);
        StackPane previewStage = new StackPane(previewPaper);
        previewStage.getStyleClass().add("qa-preview-stage");
        Rectangle previewClip = new Rectangle();
        previewClip.arcWidthProperty().set(24);
        previewClip.arcHeightProperty().set(24);
        previewClip.widthProperty().bind(previewStage.widthProperty());
        previewClip.heightProperty().bind(previewStage.heightProperty());
        previewStage.setClip(previewClip);

        Button highlightButton = qaToolbarButton("\u270E");
        highlightButton.getStyleClass().add("qa-highlight-button");
        if (qaHighlight) {
            highlightButton.getStyleClass().add("qa-toolbar-button-active");
        }
        highlightButton.setOnAction(event -> onToggleHighlight.run());
        Button zoomOutButton = qaToolbarButton("-");
        zoomOutButton.setDisable(qaZoom <= 0.48);
        zoomOutButton.setOnAction(event -> onZoomPage.accept(-0.1));
        Button zoomInButton = qaToolbarButton("+");
        zoomInButton.setDisable(qaZoom >= 1.22);
        zoomInButton.setOnAction(event -> onZoomPage.accept(0.1));
        boolean panDisabled = qaZoom <= 0.78;
        Button panLeftButton = qaToolbarButton("\u2190");
        panLeftButton.getStyleClass().add("qa-pan-button");
        panLeftButton.setDisable(panDisabled);
        panLeftButton.setOnAction(event -> onPanPage.accept(new double[]{30, 0}));
        Button panRightButton = qaToolbarButton("\u2192");
        panRightButton.getStyleClass().add("qa-pan-button");
        panRightButton.setDisable(panDisabled);
        panRightButton.setOnAction(event -> onPanPage.accept(new double[]{-30, 0}));
        Button panUpButton = qaToolbarButton("\u2191");
        panUpButton.getStyleClass().add("qa-pan-button");
        panUpButton.setDisable(panDisabled);
        panUpButton.setOnAction(event -> onPanPage.accept(new double[]{0, 30}));
        Button panDownButton = qaToolbarButton("\u2193");
        panDownButton.getStyleClass().add("qa-pan-button");
        panDownButton.setDisable(panDisabled);
        panDownButton.setOnAction(event -> onPanPage.accept(new double[]{0, -30}));

        HBox toolbar = new HBox(8,
                highlightButton,
                zoomOutButton,
                zoomInButton,
                qaToolbarSelect(Math.round((qaZoom / 0.78) * 100) + "%"),
                panLeftButton,
                panRightButton,
                panUpButton,
                panDownButton
        );
        toolbar.getStyleClass().add("qa-toolbar");

        Button previousButton = qaPagerButton("<", () -> onStepPage.accept(-1));
        Button nextButton = qaPagerButton(">", () -> onStepPage.accept(1));
        Button rotateButton = qaPagerButton("Rotate", onRotatePage);
        Label pageCounter = new Label(qaCurrentPage + " / " + selectedFile.pageCount());
        pageCounter.getStyleClass().add("qa-pager-label");
        HBox pageControls = new HBox(10, previousButton, pageCounter, nextButton, rotateButton);
        pageControls.getStyleClass().add("qa-page-controls");
        pageControls.setAlignment(Pos.CENTER);

        VBox previewColumn = new VBox(10, toolbar, previewStage, pageControls);
        previewColumn.getStyleClass().add("qa-preview-column");
        previewColumn.setMinHeight(570);
        previewColumn.setPrefHeight(570);
        previewColumn.setMaxHeight(570);
        HBox.setHgrow(previewColumn, Priority.ALWAYS);

        VBox selectionPanel = qaSidePanel("Current Selection",
                qaMetaBlock("Profile", profileName),
                qaMetaBlock("Box", currentBoxId),
                qaMetaBlock("Document", selectedDocument == null ? "-" : selectedDocument.title()),
                qaMetaBlock("File", selectedFile.badge()),
                qaMetaBlock("File Reference ID", selectedFile.reference().replace("Ref: ", "")),
                qaMetaBlock("File ID", selectedFile.badge().replace("File ", "")),
                qaMetaBlock("Scanned At", "May 15, 2024 10:15 AM"),
                qaMetaBlock("Scanned By", "John Doe")
        );

        Button rejectButton = qaActionButton("Reject, Send Back To Scanning", true, onReject);
        VBox toolsPanel = qaUntitledSidePanel(rejectButton);

        Button multiPageTiff = qaActionButton("Multi Page TIFF", false, () -> {
            onSelectTiffAction.accept("Multi Page TIFF");
            onApprove.run();
        });
        Button singlePageTiff = qaActionButton("Single Page TIFF", false, () -> {
            onSelectTiffAction.accept("Single Page TIFF");
            onApprove.run();
        });
        if ("Multi Page TIFF".equals(selectedTiffAction)) {
            multiPageTiff.getStyleClass().add("qa-action-selected");
        }
        if ("Single Page TIFF".equals(selectedTiffAction)) {
            singlePageTiff.getStyleClass().add("qa-action-selected");
        }
        VBox exportFilenamePanel = qaExportFilenamePanel(exportFileName);
        VBox actionsPanel = qaUntitledSidePanel(multiPageTiff, singlePageTiff, exportFilenamePanel);

        VBox rightColumn = new VBox(10, selectionPanel, toolsPanel, actionsPanel);
        rightColumn.getStyleClass().add("qa-right-column");
        rightColumn.setPrefWidth(190);
        rightColumn.setMinWidth(190);
        rightColumn.setMaxWidth(190);
        rightColumn.setMinHeight(570);
        rightColumn.setPrefHeight(570);
        rightColumn.setMaxHeight(570);

        HBox workspace = new HBox(10, leftColumn, previewColumn, rightColumn);
        workspace.getStyleClass().add("qa-workspace");
        workspace.setMinHeight(570);
        workspace.setPrefHeight(570);
        workspace.setMaxHeight(570);
        HBox.setHgrow(previewColumn, Priority.ALWAYS);

        ComboBox<String> rotationCombo = new ComboBox<>();
        rotationCombo.getItems().addAll("0 deg (No rotation)", "90 deg", "180 deg", "270 deg");
        rotationCombo.getSelectionModel().select(0);
        rotationCombo.getStyleClass().add("qa-export-combo");
        rotationCombo.setMaxWidth(Double.MAX_VALUE);

        Button exportDocumentButton = new Button("Export Document");
        SVGPath exportIcon = new SVGPath();
        exportIcon.setContent("M12 3V15 M7 10L12 15L17 10 M5 21H19");
        exportIcon.getStyleClass().add("qa-export-button-icon");
        exportDocumentButton.setGraphic(exportIcon);
        exportDocumentButton.setGraphicTextGap(8);
        exportDocumentButton.getStyleClass().addAll("approve-button", "qa-export-document-button");
        exportDocumentButton.setMaxWidth(Double.MAX_VALUE);
        exportDocumentButton.setDisable(!exportReady || !qaApproved);
        exportDocumentButton.setOnAction(event -> onExport.run());

        HBox footer = new HBox(12,
                qaProgressStat("Scanning Progress", qaFiles.size() + " / 250 files"),
                qaFooterControl("Profile Rotation", rotationCombo),
                exportDocumentButton
        );
        footer.getStyleClass().add("qa-footer-bar");
        footer.setMinHeight(54);
        footer.setPrefHeight(54);
        footer.setMaxHeight(54);
        HBox.setHgrow(footer.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(footer.getChildren().get(1), Priority.ALWAYS);

        body.setTop(headerRow);
        body.setCenter(workspace);
        body.setBottom(footer);
        return body;
    }

    private VBox qaMetaBlock(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("qa-side-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("qa-side-value");
        return new VBox(2, label, value);
    }

    private VBox buildQaScanningDocumentCard(UserPortalModel.ScanningDocument document,
                                             boolean selected,
                                             String status,
                                             Runnable onSelect) {
        Label title = new Label(document.title());
        title.getStyleClass().add("scan-document-title");
        title.setWrapText(true);
        Label count = new Label(document.fileCountText());
        count.getStyleClass().add("scan-document-count");

        Label iconCorner = new Label("");
        iconCorner.getStyleClass().add("scan-document-corner");
        Label iconLineOne = new Label("");
        iconLineOne.getStyleClass().add("scan-document-line");
        Label iconLineTwo = new Label("");
        iconLineTwo.getStyleClass().add("scan-document-line-short");
        VBox iconGlyph = new VBox(2, iconCorner, iconLineOne, iconLineTwo);
        iconGlyph.getStyleClass().add("scan-document-glyph");
        StackPane iconWrap = new StackPane(iconGlyph);
        iconWrap.getStyleClass().add("scan-document-icon-wrap");

        VBox text = new VBox(2, title, count);
        text.setAlignment(Pos.CENTER);

        VBox card = new VBox(8, iconWrap, text, components.qaStatusBadge(status));
        card.getStyleClass().addAll("scan-document-row", selected ? "scan-document-row-selected" : "scan-document-row-plain");
        card.getStyleClass().add("qa-document-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setOnMouseClicked(event -> onSelect.run());
        return card;
    }

    private VBox buildQaScanningFileCard(UserPortalModel.ScanningFile file,
                                         int displayIndex,
                                         boolean selected,
                                         Runnable onSelect) {
        Label badge = new Label("File " + displayIndex);
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
        card.getStyleClass().add("qa-sidebar-file-card");
        card.getStyleClass().add("qa-scan-file-card");
        if (file.barcode()) {
            card.getStyleClass().add("scan-file-card-barcode");
        }
        if (selected) {
            card.getStyleClass().add("qa-sidebar-file-selected");
        }
        card.setOnMouseClicked(event -> onSelect.run());
        return card;
    }

    private Button qaToolbarButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("qa-toolbar-button");
        return button;
    }

    private ComboBox<String> qaToolbarSelect(String value) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(value);
        combo.getSelectionModel().selectFirst();
        combo.getStyleClass().add("qa-toolbar-select");
        return combo;
    }

    private Button qaPagerButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("qa-pager-button");
        button.setOnAction(event -> action.run());
        return button;
    }

    private VBox qaSidePanel(String titleText, javafx.scene.Node... items) {
        Label title = new Label(titleText);
        title.getStyleClass().add("panel-title");
        VBox panel = new VBox(8);
        panel.getStyleClass().add("qa-side-panel");
        panel.getChildren().add(title);
        panel.getChildren().addAll(items);
        return panel;
    }

    private VBox qaUntitledSidePanel(javafx.scene.Node... items) {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("qa-side-panel");
        panel.getChildren().addAll(items);
        return panel;
    }

    private Button qaActionButton(String text, boolean destructive, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add(destructive ? "reject-button" : "page-window-cancel-button");
        button.getStyleClass().add("qa-side-action-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> action.run());
        return button;
    }

    private VBox qaProgressStat(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("qa-footer-label");
        Region track = new Region();
        track.getStyleClass().add("qa-progress-track");
        Region fill = new Region();
        fill.getStyleClass().add("qa-progress-fill");
        StackPane progress = new StackPane(track, fill);
        progress.getStyleClass().add("qa-progress-wrap");
        Label value = new Label(valueText);
        value.getStyleClass().add("qa-progress-value");
        VBox box = new VBox(8, label, progress, value);
        box.getStyleClass().add("qa-footer-card");
        return box;
    }

    private VBox qaFooterControl(String labelText, ComboBox<String> comboBox) {
        Label label = new Label(labelText);
        label.getStyleClass().add("qa-footer-label");
        VBox box = new VBox(8, label, comboBox);
        box.getStyleClass().add("qa-footer-card");
        return box;
    }

    private VBox qaExportFilenamePanel(String fileName) {
        Label label = new Label("Export filename:");
        label.getStyleClass().add("qa-export-filename-label");
        Label value = new Label(fileName);
        value.getStyleClass().add("qa-export-filename-value");
        value.setWrapText(true);

        VBox panel = new VBox(4, label, value);
        panel.getStyleClass().add("qa-export-filename-panel");
        panel.setMaxWidth(Double.MAX_VALUE);
        return panel;
    }

    private VBox buildQaPreviewPaper(int pageNumber,
                                     boolean highlightMode,
                                     Set<String> highlightedText,
                                     Consumer<String> onToggleTextHighlight) {
        Label logo = new Label("C");
        logo.getStyleClass().add("qa-invoice-logo");
        Label companyName = qaPreviewText("company-name", "Acme Corporation", highlightMode, highlightedText, onToggleTextHighlight);
        companyName.getStyleClass().add("qa-invoice-strong");
        VBox company = new VBox(2,
                companyName,
                qaPreviewText("company-address-1", "123 Business Rd.", highlightMode, highlightedText, onToggleTextHighlight),
                qaPreviewText("company-address-2", "Suite 100", highlightMode, highlightedText, onToggleTextHighlight),
                qaPreviewText("company-address-3", "New York, NY 10001", highlightMode, highlightedText, onToggleTextHighlight),
                qaPreviewText("company-phone", "(212) 555-0199", highlightMode, highlightedText, onToggleTextHighlight)
        );
        company.getStyleClass().add("qa-invoice-company");

        Label invoiceTitle = new Label("INVOICE");
        invoiceTitle.getStyleClass().add("qa-invoice-title");
        GridPane invoiceDetails = new GridPane();
        invoiceDetails.setHgap(16);
        invoiceDetails.setVgap(6);
        invoiceDetails.add(new Label("Invoice #:"), 0, 0);
        invoiceDetails.add(qaPreviewText("invoice-number", "INV-1001", highlightMode, highlightedText, onToggleTextHighlight), 1, 0);
        invoiceDetails.add(new Label("Invoice Date:"), 0, 1);
        invoiceDetails.add(qaPreviewText("invoice-date", "May 01, 2024", highlightMode, highlightedText, onToggleTextHighlight), 1, 1);
        invoiceDetails.add(new Label("Due Date:"), 0, 2);
        invoiceDetails.add(qaPreviewText("due-date", "May 31, 2024", highlightMode, highlightedText, onToggleTextHighlight), 1, 2);
        invoiceDetails.getStyleClass().add("qa-invoice-details");
        VBox invoiceBlock = new VBox(16, invoiceTitle, invoiceDetails);
        invoiceBlock.setAlignment(Pos.TOP_RIGHT);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topRow = new HBox(12, logo, company, topSpacer, invoiceBlock);
        topRow.setAlignment(Pos.TOP_LEFT);

        Label billToTitle = new Label("Bill To:");
        billToTitle.getStyleClass().add("qa-invoice-strong");
        VBox billTo = new VBox(3,
                billToTitle,
                qaPreviewText("bill-company", "ABC Company", highlightMode, highlightedText, onToggleTextHighlight),
                qaPreviewText("bill-address-1", "456 Market St.", highlightMode, highlightedText, onToggleTextHighlight),
                qaPreviewText("bill-address-2", "San Francisco, CA 94105", highlightMode, highlightedText, onToggleTextHighlight),
                qaPreviewText("bill-phone", "(415) 555-0123", highlightMode, highlightedText, onToggleTextHighlight)
        );
        billTo.getStyleClass().add("qa-invoice-bill");

        GridPane invoiceTable = new GridPane();
        invoiceTable.getStyleClass().add("qa-invoice-table");
        Label consultingDescription = qaPreviewText("line-consulting", "Consulting Services", highlightMode, highlightedText, onToggleTextHighlight);
        Label softwareDescription = qaPreviewText("line-software", "Software License", highlightMode, highlightedText, onToggleTextHighlight);
        Label supportDescription = qaPreviewText("line-support", "Support & Maintenance", highlightMode, highlightedText, onToggleTextHighlight);
        invoiceTable.add(new Label("Description"), 0, 0);
        invoiceTable.add(new Label("Quantity"), 1, 0);
        invoiceTable.add(new Label("Unit Price"), 2, 0);
        invoiceTable.add(new Label("Amount"), 3, 0);
        invoiceTable.add(consultingDescription, 0, 1);
        invoiceTable.add(new Label("10"), 1, 1);
        invoiceTable.add(new Label("$150.00"), 2, 1);
        invoiceTable.add(new Label("$1,500.00"), 3, 1);
        invoiceTable.add(softwareDescription, 0, 2);
        invoiceTable.add(new Label("2"), 1, 2);
        invoiceTable.add(new Label("$500.00"), 2, 2);
        invoiceTable.add(new Label("$1,000.00"), 3, 2);
        invoiceTable.add(supportDescription, 0, 3);
        invoiceTable.add(new Label("1"), 1, 3);
        invoiceTable.add(new Label("$250.00"), 2, 3);
        invoiceTable.add(new Label("$250.00"), 3, 3);
        invoiceTable.getColumnConstraints().addAll(
                components.column(140),
                components.column(52),
                components.column(68),
                components.column(72)
        );

        Label totalLabel = new Label("Total          $2,990.63");
        totalLabel.getStyleClass().add("qa-invoice-strong");
        VBox totalBlock = new VBox(4,
                qaPreviewText("subtotal", "Subtotal        $2,750.00", highlightMode, highlightedText, onToggleTextHighlight),
                qaPreviewText("tax", "Tax (8.75%)       $240.63", highlightMode, highlightedText, onToggleTextHighlight),
                totalLabel
        );
        totalBlock.getStyleClass().add("qa-invoice-total");
        totalBlock.setAlignment(Pos.CENTER_RIGHT);

        Region bodySpacer = new Region();
        VBox.setVgrow(bodySpacer, Priority.ALWAYS);
        Label notes = new Label("Notes:\nThank you for your business!");
        notes.getStyleClass().add("qa-invoice-notes");
        Label pageFooter = new Label("Page " + pageNumber + " of 1");
        pageFooter.getStyleClass().add("qa-invoice-footer");
        pageFooter.setMaxWidth(Double.MAX_VALUE);
        pageFooter.setAlignment(Pos.CENTER);

        VBox paper = new VBox(22, topRow, billTo, invoiceTable, totalBlock, bodySpacer, notes, pageFooter);
        paper.getStyleClass().add("qa-preview-paper");
        paper.setAlignment(Pos.TOP_LEFT);
        return paper;
    }

    private Label qaPreviewText(String id,
                                String text,
                                boolean highlightMode,
                                Set<String> highlightedText,
                                Consumer<String> onToggleTextHighlight) {
        Label label = new Label(text);
        if (highlightedText.contains(id)) {
            label.getStyleClass().add("qa-text-highlight");
        }
        if (highlightMode) {
            label.getStyleClass().add("qa-highlight-target");
            label.setOnMouseClicked(event -> onToggleTextHighlight.accept(id));
        }
        return label;
    }
}
