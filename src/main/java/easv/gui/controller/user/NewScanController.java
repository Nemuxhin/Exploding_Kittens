package easv.gui.controller.user;

import easv.gui.UserPortalModel;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class NewScanController {
    private static final double PREVIEW_PAGE_WIDTH = 500;
    private static final double PREVIEW_PAGE_HEIGHT = 560;
    private static final double PREVIEW_SAFE_HORIZONTAL_PADDING = 132;
    private static final double PREVIEW_SAFE_VERTICAL_PADDING = 72;

    private static final double MIN_PREVIEW_ZOOM = 0.50;
    private static final double MAX_PREVIEW_ZOOM = 2.50;
    private static final double PREVIEW_ZOOM_STEP = 0.10;
    private static final double PREVIEW_NUDGE_AMOUNT = 36;

    private static final int MAX_UNDO_STEPS = 30;

    private final UserPortalModel portalModel;
    private final Consumer<UserPortalModel.PortalSession> onSessionStarted;
    private final PortalNavigator navigator;

    private final List<ScannedPage> allPages = new ArrayList<>();
    private final List<ScannedPage> pendingPages = new ArrayList<>();
    private final List<DocumentGroup> documents = new ArrayList<>();
    private final Set<Integer> collapsedDocuments = new HashSet<>();
    private final Deque<ScanSnapshot> undoStack = new ArrayDeque<>();
    private final DoubleProperty previewZoomMultiplier = new SimpleDoubleProperty(1.0);

    private StackPane root;
    @FXML private VBox scanSetupView;
    @FXML private BorderPane scanWorkspaceView;
    @FXML private BorderPane reviewWorkspaceView;

    @FXML private ComboBox<UserPortalModel.ProfileItem> profileComboBox;
    @FXML private TextField boxIdTextField;

    @FXML private Button profileInfoButton;
    @FXML private VBox profileInfoPanel;
    @FXML private Label profileInfoTitleLabel;
    @FXML private Label profileInfoMetadataLabel;
    @FXML private Label profileInfoQaLabel;
    @FXML private Label profileInfoSplittingLabel;

    @FXML private Button startScanningButton;
    @FXML private Button viewMyScansButton;
    @FXML private Button undoLastActionButton;

    @FXML private Label workspaceSessionTitleLabel;
    @FXML private Label workspaceSessionSubtitleLabel;
    @FXML private Label boxStructureSubtitleLabel;
    @FXML private Label selectedFileTitleLabel;
    @FXML private Label selectedFileRefLabel;
    @FXML private Label totalFilesLabel;
    @FXML private Label pageTrayTitleLabel;
    @FXML private Label previewZoomLabel;

    @FXML private VBox documentTreeContainer;
    @FXML private StackPane previewHost;
    @FXML private HBox pageTrayContainer;

    @FXML private StackPane finishReviewOverlay;
    @FXML private Label finishReviewBoxIdLabel;
    @FXML private Label finishReviewProfileLabel;
    @FXML private Label finishReviewDocumentsLabel;
    @FXML private Label finishReviewPagesLabel;

    @FXML private StackPane submitConfirmationOverlay;
    @FXML private Label submitConfirmationBoxIdLabel;
    @FXML private Label submitConfirmationProfileLabel;
    @FXML private Label submitConfirmationSummaryLabel;

    @FXML private Label reviewBoxValueLabel;
    @FXML private Label reviewProfileValueLabel;
    @FXML private Label reviewDocumentsValueLabel;
    @FXML private VBox reviewDocumentListContainer;
    @FXML private Label reviewSelectedTitleLabel;
    @FXML private StackPane reviewPreviewHost;
    @FXML private HBox reviewPageTrayContainer;

    private UserPortalModel.PortalSession currentSession;
    private StackPane currentPreviewWrapper;
    private ScannedPage selectedPage;

    private int nextReferenceId = 1;
    private int nextFileId = 1;

    private double previewTranslateX = 0;
    private double previewTranslateY = 0;
    private double previewDragStartX = 0;
    private double previewDragStartY = 0;
    private double previewTranslateStartX = 0;
    private double previewTranslateStartY = 0;

    public NewScanController(UserPortalModel portalModel,
                             Consumer<UserPortalModel.PortalSession> onSessionStarted,
                             PortalNavigator navigator) {
        this.portalModel = portalModel;
        this.onSessionStarted = onSessionStarted;
        this.navigator = navigator == null ? PortalNavigator.none() : navigator;
    }

    public Node create(UserPortalModel.PortalSession activeSession) {
        ensureViewBuilt();
        applyIncomingSession(activeSession);
        return root;
    }

    public void activate(UserPortalModel.PortalSession activeSession) {
        applyIncomingSession(activeSession);

        if (activeSession != null) {
            hideFinishReviewModal();
            hideSubmitConfirmationModal();
            showSetupView();
        }
    }

    private void ensureViewBuilt() {
        if (root != null) {
            return;
        }

        root = new StackPane();
        root.getStyleClass().add("scan-page");
        root.setMinHeight(780);
        root.setMaxWidth(Double.MAX_VALUE);
        root.setMaxHeight(Double.MAX_VALUE);

        scanSetupView = buildSetupView();
        scanWorkspaceView = buildWorkspaceView();
        reviewWorkspaceView = buildReviewWorkspaceView();
        finishReviewOverlay = buildFinishReviewOverlay();
        submitConfirmationOverlay = buildSubmitConfirmationOverlay();

        root.getChildren().addAll(
                scanSetupView,
                scanWorkspaceView,
                reviewWorkspaceView,
                finishReviewOverlay,
                submitConfirmationOverlay
        );

        initialize();
    }

    private VBox buildSetupView() {
        VBox setupView = new VBox();
        setupView.getStyleClass().add("scan-setup-view");
        setupView.setAlignment(Pos.TOP_CENTER);
        setupView.setFillWidth(true);
        setupView.setMaxWidth(Double.MAX_VALUE);

        VBox content = new VBox(18);
        content.getStyleClass().add("scan-setup-content");
        content.setAlignment(Pos.TOP_LEFT);
        content.setPrefWidth(1110);
        content.setMaxWidth(1110);

        Label title = new Label("New Scan");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Choose a profile and box before scanning.");
        subtitle.getStyleClass().add("page-subtitle");
        VBox header = new VBox(6, title, subtitle);

        VBox quickCard = new VBox(18);
        quickCard.getStyleClass().add("scan-setup-quick-card");
        quickCard.setAlignment(Pos.TOP_LEFT);
        quickCard.setMinWidth(270);
        quickCard.setPrefWidth(270);
        quickCard.setMaxWidth(270);
        quickCard.getChildren().addAll(
                titledCopy("Quick start", "Set the profile, enter the box ID, then start scanning.",
                        "scan-setup-quick-title", "scan-setup-quick-copy"),
                quickStep("1", "Select profile", "Uses your assigned scan rules."),
                quickStep("2", "Enter box ID", "Use the archive box identifier."),
                quickStep("3", "Start scanning", "Review, split, reorder, and submit after scan.")
        );

        VBox primaryCard = new VBox(24);
        primaryCard.getStyleClass().add("scan-setup-primary-card");
        primaryCard.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(primaryCard, Priority.ALWAYS);
        primaryCard.setMaxWidth(Double.MAX_VALUE);

        primaryCard.getChildren().add(
                titledCopy("Scan setup", "Complete the required fields below to begin the scanning session.",
                        "scan-setup-primary-title", "scan-setup-primary-copy")
        );

        profileComboBox = new ComboBox<>();
        profileComboBox.getStyleClass().add("weblager-combo-box");
        profileComboBox.setMaxWidth(Double.MAX_VALUE);

        profileInfoButton = new Button("Profile info");
        profileInfoButton.getStyleClass().add("profile-info-link-button");

        HBox profileLabelRow = new HBox();
        profileLabelRow.setAlignment(Pos.CENTER_LEFT);
        Label profileLabel = new Label("Profile");
        profileLabel.getStyleClass().add("form-label");
        Region profileSpacer = new Region();
        HBox.setHgrow(profileSpacer, Priority.ALWAYS);
        profileLabelRow.getChildren().addAll(profileLabel, profileSpacer, profileInfoButton);

        Label profileHelper = new Label("Only profiles assigned to you are shown.");
        profileHelper.getStyleClass().add("helper-text");

        profileInfoTitleLabel = new Label("No profile selected");
        profileInfoTitleLabel.getStyleClass().add("profile-info-title");
        profileInfoMetadataLabel = new Label("Metadata required: —");
        profileInfoMetadataLabel.getStyleClass().add("profile-info-line");
        profileInfoQaLabel = new Label("QA required: —");
        profileInfoQaLabel.getStyleClass().add("profile-info-line");
        profileInfoSplittingLabel = new Label("Splitting method: —");
        profileInfoSplittingLabel.getStyleClass().add("profile-info-line");

        profileInfoPanel = new VBox(9,
                profileInfoTitleLabel,
                profileInfoMetadataLabel,
                profileInfoQaLabel,
                profileInfoSplittingLabel
        );
        profileInfoPanel.getStyleClass().add("profile-info-panel");
        profileInfoPanel.setVisible(false);
        profileInfoPanel.setManaged(false);

        VBox profileField = new VBox(6, profileLabelRow, profileComboBox, profileHelper, profileInfoPanel);
        profileField.setMaxWidth(Double.MAX_VALUE);

        Label boxLabel = new Label("Box ID");
        boxLabel.getStyleClass().add("form-label");
        boxIdTextField = new TextField();
        boxIdTextField.setPromptText("Enter box ID");
        boxIdTextField.getStyleClass().add("weblager-text-field");
        boxIdTextField.setMaxWidth(Double.MAX_VALUE);
        VBox boxField = new VBox(6, boxLabel, boxIdTextField);
        boxField.setMaxWidth(Double.MAX_VALUE);

        startScanningButton = new Button("Start Scanning");
        startScanningButton.getStyleClass().add("primary-button");
        startScanningButton.setMaxWidth(Double.MAX_VALUE);
        startScanningButton.setOnAction(event -> onStartScanning());
        HBox.setHgrow(startScanningButton, Priority.ALWAYS);

        viewMyScansButton = new Button("View My Scans");
        viewMyScansButton.getStyleClass().add("tertiary-button");
        viewMyScansButton.setMinWidth(180);
        viewMyScansButton.setPrefWidth(180);
        viewMyScansButton.setMaxWidth(180);
        viewMyScansButton.setOnAction(event -> onViewMyScans());

        HBox actionRow = new HBox(15, startScanningButton, viewMyScansButton);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setMaxWidth(Double.MAX_VALUE);

        Label assignedProfilesOnly = chipLabel("Assigned profiles only");
        Label reviewSubmitAfter = chipLabel("Review and submit after scanning");
        HBox metaRow = new HBox(9, assignedProfilesOnly, reviewSubmitAfter);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        metaRow.getStyleClass().add("scan-setup-meta-row");

        VBox formStack = new VBox(18, profileField, boxField, actionRow, metaRow);
        formStack.setMaxWidth(Double.MAX_VALUE);
        primaryCard.getChildren().add(formStack);

        HBox mainRow = new HBox(18, quickCard, primaryCard);
        mainRow.getStyleClass().add("scan-setup-main-row");
        mainRow.setAlignment(Pos.TOP_LEFT);
        mainRow.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(header, mainRow);
        setupView.getChildren().add(content);
        return setupView;
    }

    private BorderPane buildWorkspaceView() {
        BorderPane workspace = new BorderPane();
        workspace.getStyleClass().add("scan-workspace");
        workspace.setVisible(false);
        workspace.setManaged(false);
        workspace.setMinHeight(760);
        workspace.setMaxWidth(Double.MAX_VALUE);
        workspace.setMaxHeight(Double.MAX_VALUE);

        workspaceSessionTitleLabel = new Label("Scanning Session");
        workspaceSessionTitleLabel.getStyleClass().add("workspace-title");
        workspaceSessionSubtitleLabel = new Label("0 files scanned · 0 documents created · 0 pending pages");
        workspaceSessionSubtitleLabel.getStyleClass().add("workspace-subtitle");
        VBox headerText = new VBox(3, workspaceSessionTitleLabel, workspaceSessionSubtitleLabel);
        headerText.setMinWidth(0);
        HBox.setHgrow(headerText, Priority.ALWAYS);

        Button scanNextFileButton = new Button("Scan Next File");
        scanNextFileButton.getStyleClass().add("scan-primary-action-button");
        scanNextFileButton.setMinWidth(168);
        scanNextFileButton.setPrefWidth(168);
        scanNextFileButton.setMaxWidth(168);
        scanNextFileButton.setOnAction(event -> onScanNextFile());

        HBox sessionHeader = new HBox(18, headerText, scanNextFileButton);
        sessionHeader.getStyleClass().add("scan-session-header");
        sessionHeader.setAlignment(Pos.CENTER_LEFT);
        sessionHeader.setPadding(new Insets(15, 18, 15, 18));
        workspace.setTop(sessionHeader);

        VBox leftPanel = new VBox(15);
        leftPanel.getStyleClass().add("scan-left-panel");
        leftPanel.setMinWidth(300);
        leftPanel.setPrefWidth(300);
        leftPanel.setMaxWidth(300);

        Label structureTitle = new Label("Box Structure");
        structureTitle.getStyleClass().add("scan-panel-title");
        boxStructureSubtitleLabel = new Label("No profile · No box ID");
        boxStructureSubtitleLabel.getStyleClass().add("scan-panel-subtitle");
        VBox structureHeader = new VBox(3, structureTitle, boxStructureSubtitleLabel);

        documentTreeContainer = new VBox(9);
        documentTreeContainer.getStyleClass().add("document-tree-container");
        ScrollPane documentTreeScroll = new ScrollPane(documentTreeContainer);
        documentTreeScroll.setFitToWidth(true);
        documentTreeScroll.setFitToHeight(true);
        documentTreeScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        documentTreeScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        documentTreeScroll.getStyleClass().add("document-tree-scroll");
        documentTreeScroll.setMinHeight(0);
        VBox.setVgrow(documentTreeScroll, Priority.ALWAYS);

        leftPanel.getChildren().addAll(structureHeader, documentTreeScroll);
        workspace.setLeft(leftPanel);

        BorderPane centerPanel = new BorderPane();
        centerPanel.getStyleClass().add("scan-center-panel");

        selectedFileTitleLabel = new Label("No file selected");
        selectedFileTitleLabel.getStyleClass().add("selected-file-title");
        selectedFileRefLabel = new Label("Scan a file to begin.");
        selectedFileRefLabel.getStyleClass().add("selected-file-subtitle");
        VBox selectedFileText = new VBox(3, selectedFileTitleLabel, selectedFileRefLabel);
        selectedFileText.setMinWidth(0);
        HBox.setHgrow(selectedFileText, Priority.ALWAYS);

        HBox currentFileHeader = new HBox(12, selectedFileText);
        currentFileHeader.getStyleClass().add("scan-current-file-header");
        currentFileHeader.setAlignment(Pos.CENTER_LEFT);
        currentFileHeader.setMinHeight(66);
        currentFileHeader.setPrefHeight(66);
        currentFileHeader.setMaxHeight(66);
        currentFileHeader.setPadding(new Insets(0, 18, 0, 18));

        Label viewLabel = new Label("View");
        viewLabel.getStyleClass().add("view-toolbar-label");

        Button zoomOutButton = viewButton("−", this::onZoomOut);
        previewZoomLabel = new Label("100%");
        previewZoomLabel.getStyleClass().add("view-zoom-label");
        previewZoomLabel.setMinWidth(54);
        previewZoomLabel.setAlignment(Pos.CENTER);
        Button zoomInButton = viewButton("+", this::onZoomIn);

        Region divider = new Region();
        divider.getStyleClass().add("view-toolbar-divider");

        Button upButton = viewButton("↑", this::onNudgePreviewUp);
        Button leftButton = viewButton("←", this::onNudgePreviewLeft);
        Label moveLabel = new Label("Move");
        moveLabel.getStyleClass().add("view-toolbar-muted-label");
        Button rightButton = viewButton("→", this::onNudgePreviewRight);
        Button downButton = viewButton("↓", this::onNudgePreviewDown);

        Region toolbarSpacer = new Region();
        HBox.setHgrow(toolbarSpacer, Priority.ALWAYS);

        Button resetViewButton = new Button("Reset View");
        resetViewButton.getStyleClass().add("view-reset-button");
        resetViewButton.setOnAction(event -> onResetPreviewView());

        HBox toolbar = new HBox(12,
                viewLabel,
                zoomOutButton,
                previewZoomLabel,
                zoomInButton,
                divider,
                upButton,
                leftButton,
                moveLabel,
                rightButton,
                downButton,
                toolbarSpacer,
                resetViewButton
        );
        toolbar.getStyleClass().add("scan-view-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setMinHeight(51);
        toolbar.setPrefHeight(51);
        toolbar.setMaxHeight(51);
        toolbar.setPadding(new Insets(0, 18, 0, 18));

        VBox centerTop = new VBox(currentFileHeader, toolbar);
        centerPanel.setTop(centerTop);

        previewHost = new StackPane();
        previewHost.getStyleClass().add("preview-host");
        previewHost.setMinSize(0, 0);
        previewHost.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        HBox.setHgrow(previewHost, Priority.ALWAYS);

        Button previousFileButton = previewArrow("‹", this::onPreviousFile);
        Button nextFileButton = previewArrow("›", this::onNextFile);

        HBox previewRow = new HBox(18, previousFileButton, previewHost, nextFileButton);
        previewRow.setAlignment(Pos.CENTER);
        previewRow.setMinSize(0, 0);
        previewRow.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        StackPane previewStage = new StackPane(previewRow);
        previewStage.getStyleClass().add("scan-preview-stage");
        previewStage.setMinSize(0, 0);
        previewStage.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        centerPanel.setCenter(previewStage);

        pageTrayTitleLabel = new Label("Scanned Files");
        pageTrayTitleLabel.getStyleClass().add("page-tray-title");
        totalFilesLabel = new Label("0 files");
        totalFilesLabel.getStyleClass().add("page-tray-count");
        Region trayHeaderSpacer = new Region();
        HBox.setHgrow(trayHeaderSpacer, Priority.ALWAYS);
        HBox trayHeader = new HBox(pageTrayTitleLabel, trayHeaderSpacer, totalFilesLabel);
        trayHeader.setAlignment(Pos.CENTER_LEFT);
        trayHeader.setMinHeight(18);
        trayHeader.setPrefHeight(18);
        trayHeader.setMaxHeight(18);

        pageTrayContainer = new HBox(9);
        pageTrayContainer.getStyleClass().add("page-tray-container");
        pageTrayContainer.setMinHeight(75);
        pageTrayContainer.setAlignment(Pos.CENTER_LEFT);

        ScrollPane trayScroll = new ScrollPane(pageTrayContainer);
        trayScroll.setFitToHeight(true);
        trayScroll.setFitToWidth(false);
        trayScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        trayScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        trayScroll.setMinHeight(81);
        trayScroll.setPrefHeight(81);
        trayScroll.setMaxHeight(81);
        trayScroll.getStyleClass().add("page-tray-scroll");

        VBox trayShell = new VBox(6, trayHeader, trayScroll);
        trayShell.getStyleClass().add("page-tray-shell");
        trayShell.setMinHeight(111);
        trayShell.setPrefHeight(111);
        trayShell.setMaxHeight(111);
        centerPanel.setBottom(trayShell);

        workspace.setCenter(centerPanel);

        VBox toolsPanel = new VBox(15);
        toolsPanel.getStyleClass().add("scan-tools-panel");
        toolsPanel.setMinWidth(285);
        toolsPanel.setPrefWidth(285);
        toolsPanel.setMaxWidth(285);

        Label toolsTitle = new Label("Tools");
        toolsTitle.getStyleClass().add("scan-panel-title");

        VBox pageTools = new VBox(9,
                sectionLabel("Page Tools"),
                actionButton("Rotate Left", "scan-tool-button", this::onRotateLeft),
                actionButton("Rotate Right", "scan-tool-button", this::onRotateRight),
                actionButton("Delete Page", "scan-danger-button", this::onDeleteSelectedPage),
                actionButton("Mark for Rescan", "scan-warning-button", this::onToggleRescanSelected),
                actionButton("Rescan Marked Pages", "scan-tool-button", this::onRescanMarkedPages)
        );

        Region panelDivider = new Region();
        panelDivider.getStyleClass().add("scan-panel-divider");

        VBox documentTools = new VBox(9,
                sectionLabel("Document Tools"),
                actionButton("Split Here", "scan-tool-button", this::onSplitHere)
        );

        Region toolsSpacer = new Region();
        VBox.setVgrow(toolsSpacer, Priority.ALWAYS);

        undoLastActionButton = actionButton("Undo Last Action", "scan-tool-button", this::onUndoLastAction);
        VBox workflowBox = new VBox(9,
                sectionLabel("Workflow"),
                undoLastActionButton,
                actionButton("Save Progress", "scan-tool-button", this::onSaveProgress),
                actionButton("Finish & Review", "scan-finish-button", this::onFinishReview)
        );
        workflowBox.getStyleClass().add("scan-workflow-box");

        toolsPanel.getChildren().addAll(
                toolsTitle,
                pageTools,
                panelDivider,
                documentTools,
                toolsSpacer,
                workflowBox
        );
        workspace.setRight(toolsPanel);

        return workspace;
    }

    private BorderPane buildReviewWorkspaceView() {
        BorderPane review = new BorderPane();
        review.getStyleClass().add("scan-review-workspace");
        review.setVisible(false);
        review.setManaged(false);
        review.setMinHeight(760);
        review.setMaxWidth(Double.MAX_VALUE);
        review.setMaxHeight(Double.MAX_VALUE);

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(event -> onBackToScanningFromReview());

        Label reviewTitle = new Label("Scan Review");
        reviewTitle.getStyleClass().add("review-summary-title");

        Label boxLabel = new Label("Box");
        boxLabel.getStyleClass().add("review-summary-label");
        reviewBoxValueLabel = new Label("—");
        reviewBoxValueLabel.getStyleClass().add("review-summary-value");
        Label profileLabel = new Label("Profile");
        profileLabel.getStyleClass().add("review-summary-label");
        reviewProfileValueLabel = new Label("—");
        reviewProfileValueLabel.getStyleClass().add("review-summary-value");
        Label documentsLabel = new Label("Documents");
        documentsLabel.getStyleClass().add("review-summary-label");
        reviewDocumentsValueLabel = new Label("—");
        reviewDocumentsValueLabel.getStyleClass().add("review-summary-value");

        HBox reviewSummary = new HBox(18,
                boxLabel,
                reviewBoxValueLabel,
                profileLabel,
                reviewProfileValueLabel,
                documentsLabel,
                reviewDocumentsValueLabel
        );
        reviewSummary.setAlignment(Pos.CENTER_LEFT);

        VBox reviewHeader = new VBox(6, backButton, reviewTitle, reviewSummary);
        reviewHeader.getStyleClass().add("review-summary-bar");
        reviewHeader.setAlignment(Pos.CENTER_LEFT);
        reviewHeader.setPadding(new Insets(12, 18, 12, 18));
        reviewHeader.setMinHeight(111);
        reviewHeader.setPrefHeight(111);
        reviewHeader.setMaxHeight(111);
        review.setTop(reviewHeader);

        VBox reviewLeftPanel = new VBox(15);
        reviewLeftPanel.getStyleClass().add("review-left-panel");
        reviewLeftPanel.setMinWidth(255);
        reviewLeftPanel.setPrefWidth(255);
        reviewLeftPanel.setMaxWidth(255);

        Label documentsTitle = new Label("Documents");
        documentsTitle.getStyleClass().add("review-panel-title");

        reviewDocumentListContainer = new VBox(9);
        reviewDocumentListContainer.getStyleClass().add("review-document-list");
        ScrollPane reviewDocumentScroll = new ScrollPane(reviewDocumentListContainer);
        reviewDocumentScroll.setFitToWidth(true);
        reviewDocumentScroll.setFitToHeight(true);
        reviewDocumentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        reviewDocumentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        reviewDocumentScroll.getStyleClass().add("review-document-scroll");
        reviewDocumentScroll.setMinHeight(0);
        VBox.setVgrow(reviewDocumentScroll, Priority.ALWAYS);

        reviewLeftPanel.getChildren().addAll(documentsTitle, reviewDocumentScroll);
        review.setLeft(reviewLeftPanel);

        BorderPane reviewCenterPanel = new BorderPane();
        reviewCenterPanel.getStyleClass().add("review-center-panel");

        reviewSelectedTitleLabel = new Label("No page selected");
        reviewSelectedTitleLabel.getStyleClass().add("review-selected-title");
        Region reviewHeaderSpacer = new Region();
        HBox.setHgrow(reviewHeaderSpacer, Priority.ALWAYS);
        Label reviewView1 = reviewIcon("↗");
        Label reviewView2 = reviewIcon("⊖");
        Label reviewView3 = reviewIcon("⊕");

        HBox reviewCurrentHeader = new HBox(12,
                reviewSelectedTitleLabel,
                reviewHeaderSpacer,
                reviewView1,
                reviewView2,
                reviewView3
        );
        reviewCurrentHeader.getStyleClass().add("review-current-file-header");
        reviewCurrentHeader.setAlignment(Pos.CENTER_LEFT);
        reviewCurrentHeader.setMinHeight(60);
        reviewCurrentHeader.setPrefHeight(60);
        reviewCurrentHeader.setMaxHeight(60);
        reviewCurrentHeader.setPadding(new Insets(0, 18, 0, 18));
        reviewCenterPanel.setTop(reviewCurrentHeader);

        reviewPreviewHost = new StackPane();
        reviewPreviewHost.getStyleClass().add("review-preview-host");
        reviewPreviewHost.setMinSize(0, 0);
        reviewPreviewHost.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        HBox.setHgrow(reviewPreviewHost, Priority.ALWAYS);

        HBox reviewPreviewRow = new HBox(18,
                previewArrow("‹", this::onPreviousReviewPage),
                reviewPreviewHost,
                previewArrow("›", this::onNextReviewPage)
        );
        reviewPreviewRow.setAlignment(Pos.CENTER);
        reviewPreviewRow.setMinSize(0, 0);
        reviewPreviewRow.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        StackPane reviewPreviewStage = new StackPane(reviewPreviewRow);
        reviewPreviewStage.getStyleClass().add("review-preview-stage");
        reviewPreviewStage.setMinSize(0, 0);
        reviewPreviewStage.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        reviewCenterPanel.setCenter(reviewPreviewStage);

        Label trayTitle = new Label("Page Tray");
        trayTitle.getStyleClass().add("review-page-tray-title");
        HBox reviewTrayHeader = new HBox(trayTitle);
        reviewTrayHeader.setAlignment(Pos.CENTER_LEFT);
        reviewTrayHeader.setMinHeight(18);
        reviewTrayHeader.setPrefHeight(18);
        reviewTrayHeader.setMaxHeight(18);

        reviewPageTrayContainer = new HBox(9);
        reviewPageTrayContainer.getStyleClass().add("review-page-tray-container");
        reviewPageTrayContainer.setMinHeight(75);
        reviewPageTrayContainer.setAlignment(Pos.CENTER_LEFT);

        ScrollPane reviewTrayScroll = new ScrollPane(reviewPageTrayContainer);
        reviewTrayScroll.setFitToHeight(true);
        reviewTrayScroll.setFitToWidth(false);
        reviewTrayScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        reviewTrayScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        reviewTrayScroll.setMinHeight(81);
        reviewTrayScroll.setPrefHeight(81);
        reviewTrayScroll.setMaxHeight(81);
        reviewTrayScroll.getStyleClass().add("review-page-tray-scroll");

        VBox reviewTrayShell = new VBox(6, reviewTrayHeader, reviewTrayScroll);
        reviewTrayShell.getStyleClass().add("review-page-tray-shell");
        reviewTrayShell.setMinHeight(111);
        reviewTrayShell.setPrefHeight(111);
        reviewTrayShell.setMaxHeight(111);
        reviewCenterPanel.setBottom(reviewTrayShell);

        review.setCenter(reviewCenterPanel);

        VBox nextStepPanel = new VBox(15);
        nextStepPanel.getStyleClass().add("review-next-step-panel");
        nextStepPanel.setMinWidth(255);
        nextStepPanel.setPrefWidth(255);
        nextStepPanel.setMaxWidth(255);

        Label nextStepTitle = new Label("Next Step");
        nextStepTitle.getStyleClass().add("review-panel-title");
        Button submitForQaButton = new Button("Submit for QA");
        submitForQaButton.getStyleClass().add("review-submit-button");
        submitForQaButton.setMaxWidth(Double.MAX_VALUE);
        submitForQaButton.setOnAction(event -> onSubmitForQaFromReview());
        Label nextStepCopy = new Label("Once submitted, this scan will be assigned to another user for QA.");
        nextStepCopy.getStyleClass().add("review-next-step-copy");
        nextStepCopy.setWrapText(true);
        Region nextStepSpacer = new Region();
        VBox.setVgrow(nextStepSpacer, Priority.ALWAYS);

        nextStepPanel.getChildren().addAll(nextStepTitle, submitForQaButton, nextStepCopy, nextStepSpacer);
        review.setRight(nextStepPanel);

        return review;
    }

    private StackPane buildFinishReviewOverlay() {
        finishReviewBoxIdLabel = new Label("Box ID");
        finishReviewBoxIdLabel.getStyleClass().add("finish-review-box-id");
        finishReviewProfileLabel = new Label("Profile");
        finishReviewProfileLabel.getStyleClass().add("finish-review-summary-muted");
        finishReviewDocumentsLabel = new Label("0 documents created");
        finishReviewDocumentsLabel.getStyleClass().add("finish-review-summary-muted");
        finishReviewPagesLabel = new Label("0 pages scanned");
        finishReviewPagesLabel.getStyleClass().add("finish-review-summary-muted");

        StackPane successIcon = new StackPane();
        successIcon.getStyleClass().add("finish-review-success-icon");
        Region successCheck = new Region();
        successCheck.getStyleClass().add("finish-review-success-check");
        successIcon.getChildren().add(successCheck);

        Label modalTitle = new Label("Scanning Complete");
        modalTitle.getStyleClass().add("finish-review-title");
        HBox modalHeader = new HBox(12, successIcon, modalTitle);
        modalHeader.setAlignment(Pos.CENTER_LEFT);

        HBox summaryCounts = new HBox(24, finishReviewDocumentsLabel, finishReviewPagesLabel);
        summaryCounts.setAlignment(Pos.CENTER_LEFT);
        VBox summaryCard = new VBox(9, finishReviewBoxIdLabel, finishReviewProfileLabel, summaryCounts);
        summaryCard.getStyleClass().add("finish-review-summary-card");

        Label copy = new Label("Review your scanned pages before submitting them for QA.");
        copy.getStyleClass().add("finish-review-copy");
        copy.setWrapText(true);

        Region reviewIcon = new Region();
        reviewIcon.getStyleClass().add("finish-review-button-icon");
        Label reviewLabel = new Label("Review Scan");
        reviewLabel.getStyleClass().add("finish-review-button-label");
        HBox reviewGraphic = new HBox(9, reviewIcon, reviewLabel);
        reviewGraphic.setAlignment(Pos.CENTER);

        Button reviewScanButton = new Button();
        reviewScanButton.getStyleClass().add("finish-review-secondary-button");
        reviewScanButton.setGraphic(reviewGraphic);
        reviewScanButton.setMaxWidth(Double.MAX_VALUE);
        reviewScanButton.setOnAction(event -> onReviewScanFromFinishModal());

        Button submitForQaButton = new Button("Submit for QA");
        submitForQaButton.getStyleClass().add("finish-review-primary-button");
        submitForQaButton.setMaxWidth(Double.MAX_VALUE);
        submitForQaButton.setOnAction(event -> onSubmitForQaFromFinishModal());

        Button backButton = new Button("Back to Scanning");
        backButton.getStyleClass().add("finish-review-link-button");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setOnAction(event -> onBackToScanningFromFinishModal());

        VBox modal = new VBox(15,
                modalHeader,
                summaryCard,
                copy,
                reviewScanButton,
                submitForQaButton,
                backButton
        );
        modal.getStyleClass().add("finish-review-modal");
        modal.setMaxWidth(432);
        modal.setPrefWidth(432);

        StackPane overlay = new StackPane(modal);
        overlay.getStyleClass().add("finish-review-overlay");
        overlay.setVisible(false);
        overlay.setManaged(false);
        return overlay;
    }

    private StackPane buildSubmitConfirmationOverlay() {
        submitConfirmationBoxIdLabel = new Label("Box ID");
        submitConfirmationBoxIdLabel.getStyleClass().add("submit-confirmation-box-id");
        submitConfirmationProfileLabel = new Label("Profile");
        submitConfirmationProfileLabel.getStyleClass().add("submit-confirmation-summary-muted");
        submitConfirmationSummaryLabel = new Label("0 documents · 0 pages submitted for QA");
        submitConfirmationSummaryLabel.getStyleClass().add("submit-confirmation-summary-muted");

        StackPane successIcon = new StackPane();
        successIcon.getStyleClass().add("submit-confirmation-success-icon");
        Region successCheck = new Region();
        successCheck.getStyleClass().add("submit-confirmation-success-check");
        successIcon.getChildren().add(successCheck);

        Label title = new Label("Submitted for QA");
        title.getStyleClass().add("submit-confirmation-title");
        HBox header = new HBox(12, successIcon, title);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox summaryCard = new VBox(9,
                submitConfirmationBoxIdLabel,
                submitConfirmationProfileLabel,
                submitConfirmationSummaryLabel
        );
        summaryCard.getStyleClass().add("submit-confirmation-summary-card");

        Label copy = new Label("This scan has been sent to the QA queue. You can start another scan now.");
        copy.getStyleClass().add("submit-confirmation-copy");
        copy.setWrapText(true);

        Button nextScanButton = new Button("Start Next Scan");
        nextScanButton.getStyleClass().add("submit-confirmation-primary-button");
        nextScanButton.setMaxWidth(Double.MAX_VALUE);
        nextScanButton.setOnAction(event -> onStartNextScanAfterSubmit());

        VBox modal = new VBox(15, header, summaryCard, copy, nextScanButton);
        modal.getStyleClass().add("submit-confirmation-modal");
        modal.setMaxWidth(432);
        modal.setPrefWidth(432);

        StackPane overlay = new StackPane(modal);
        overlay.getStyleClass().add("submit-confirmation-overlay");
        overlay.setVisible(false);
        overlay.setManaged(false);
        return overlay;
    }

    private VBox titledCopy(String titleText, String bodyText, String titleClass, String bodyClass) {
        Label title = new Label(titleText);
        title.getStyleClass().add(titleClass);
        Label body = new Label(bodyText);
        body.getStyleClass().add(bodyClass);
        body.setWrapText(true);
        return new VBox(6, title, body);
    }

    private HBox quickStep(String numberText, String titleText, String copyText) {
        Label number = new Label(numberText);
        number.getStyleClass().add("scan-setup-step-number");

        Label title = new Label(titleText);
        title.getStyleClass().add("scan-setup-step-title");
        Label copy = new Label(copyText);
        copy.getStyleClass().add("scan-setup-step-copy");
        copy.setWrapText(true);

        VBox text = new VBox(3, title, copy);
        HBox item = new HBox(12, number, text);
        item.getStyleClass().add("scan-setup-quick-item");
        item.setAlignment(Pos.TOP_LEFT);
        return item;
    }

    private Label chipLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("scan-setup-meta-pill");
        return label;
    }

    private Button actionButton(String text, String styleClass, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> action.run());
        return button;
    }

    private Button viewButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("view-tool-button");
        button.setOnAction(event -> action.run());
        return button;
    }

    private Button previewArrow(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("preview-arrow-button");
        button.setOnAction(event -> action.run());
        return button;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("scan-tools-section-label");
        return label;
    }

    private Label reviewIcon(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("review-view-icon");
        return label;
    }

    @FXML
    private void initialize() {
        configureProfiles();
        configureProfileInfo();
        configureValidation();
        configurePreviewInteractions();
        updatePreviewZoomLabel();
        updateUndoButtonState();
        hideFinishReviewModal();
        hideSubmitConfirmationModal();
        showSetupView();
        applyIncomingSession(null);
    }

    private void applyIncomingSession(UserPortalModel.PortalSession activeSession) {
        if (profileComboBox == null || boxIdTextField == null) {
            return;
        }

        if (activeSession != null) {
            currentSession = activeSession;
            profileComboBox.getSelectionModel().select(activeSession.profile());
            boxIdTextField.setText(activeSession.box().id());
            updateProfileInfo(activeSession.profile());
            updateStartScanningState();
            return;
        }

        if (profileComboBox.getValue() == null) {
            UserPortalModel.ProfileItem defaultProfile = portalModel.getDefaultProfileForUser();
            profileComboBox.getSelectionModel().select(defaultProfile);
            if (boxIdTextField.getText() == null || boxIdTextField.getText().isBlank()) {
                boxIdTextField.setText(defaultBoxId(defaultProfile));
            }
            updateProfileInfo(defaultProfile);
            updateStartScanningState();
        }
    }

    private void configureProfiles() {
        profileComboBox.getItems().setAll(portalModel.fetchProfilesForUser());
        profileComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateProfileInfo(newValue));
    }

    private void configureProfileInfo() {
        profileInfoPanel.setVisible(false);
        profileInfoPanel.setManaged(false);
        profileInfoButton.setOnAction(event -> toggleProfileInfoPanel());
        updateProfileInfo(null);
    }

    private void toggleProfileInfoPanel() {
        boolean shouldShow = !profileInfoPanel.isVisible();
        profileInfoPanel.setVisible(shouldShow);
        profileInfoPanel.setManaged(shouldShow);
    }

    private void updateProfileInfo(UserPortalModel.ProfileItem selectedProfile) {
        if (selectedProfile == null) {
            profileInfoTitleLabel.setText("No profile selected");
            profileInfoMetadataLabel.setText("Metadata required: —");
            profileInfoQaLabel.setText("QA required: —");
            profileInfoSplittingLabel.setText("Splitting method: —");
            return;
        }

        UserPortalModel.ScanProfileInfo info = portalModel.fetchScanProfileInfo(selectedProfile);
        profileInfoTitleLabel.setText(selectedProfile.name());
        profileInfoMetadataLabel.setText("Metadata required: " + info.metadataRequired());
        profileInfoQaLabel.setText("QA required: " + info.qaRequired());
        profileInfoSplittingLabel.setText("Splitting method: " + info.splittingMethod());

        if ((boxIdTextField.getText() == null || boxIdTextField.getText().isBlank())
                && !selectedProfile.assignedBoxes().isEmpty()) {
            boxIdTextField.setText(selectedProfile.assignedBoxes().get(0).id());
        }
    }

    private void configureValidation() {
        startScanningButton.setDisable(true);
        profileComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateStartScanningState());
        boxIdTextField.textProperty().addListener((observable, oldValue, newValue) -> updateStartScanningState());
    }

    private void updateStartScanningState() {
        boolean profileSelected = profileComboBox.getValue() != null;
        boolean boxIdEntered = portalModel.isValidBoxId(normalizedBoxIdInput());
        startScanningButton.setDisable(!(profileSelected && boxIdEntered));
    }

    private void configurePreviewInteractions() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(previewHost.widthProperty());
        clip.heightProperty().bind(previewHost.heightProperty());
        previewHost.setClip(clip);
        previewHost.setFocusTraversable(true);

        previewHost.widthProperty().addListener((observable, oldValue, newValue) -> clampPreviewTranslation());
        previewHost.heightProperty().addListener((observable, oldValue, newValue) -> clampPreviewTranslation());

        previewHost.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> previewHost.requestFocus());
        previewHost.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handlePreviewMousePressed);
        previewHost.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handlePreviewMouseDragged);
        previewHost.addEventFilter(ScrollEvent.SCROLL, this::handlePreviewScroll);
        previewHost.addEventFilter(ZoomEvent.ZOOM, this::handlePreviewZoom);
        previewHost.addEventFilter(KeyEvent.KEY_PRESSED, this::handlePreviewKeyPressed);

        previewZoomMultiplier.addListener((observable, oldValue, newValue) -> {
            updatePreviewZoomLabel();
            clampPreviewTranslation();
        });
    }

    private void handlePreviewMousePressed(MouseEvent event) {
        if (selectedPage == null || currentPreviewWrapper == null) {
            return;
        }

        previewHost.requestFocus();
        previewDragStartX = event.getSceneX();
        previewDragStartY = event.getSceneY();
        previewTranslateStartX = previewTranslateX;
        previewTranslateStartY = previewTranslateY;
        event.consume();
    }

    private void handlePreviewMouseDragged(MouseEvent event) {
        if (selectedPage == null || currentPreviewWrapper == null) {
            return;
        }

        previewTranslateX = previewTranslateStartX + event.getSceneX() - previewDragStartX;
        previewTranslateY = previewTranslateStartY + event.getSceneY() - previewDragStartY;
        clampPreviewTranslation();
        event.consume();
    }

    private void handlePreviewScroll(ScrollEvent event) {
        if (selectedPage == null) {
            return;
        }

        double deltaY = event.getDeltaY();
        if (deltaY == 0) {
            return;
        }

        zoomPreviewBy(deltaY > 0 ? PREVIEW_ZOOM_STEP : -PREVIEW_ZOOM_STEP);
        event.consume();
    }

    private void handlePreviewZoom(ZoomEvent event) {
        if (selectedPage == null) {
            return;
        }

        setPreviewZoom(previewZoomMultiplier.get() * event.getZoomFactor());
        event.consume();
    }

    private void handlePreviewKeyPressed(KeyEvent event) {
        if (selectedPage == null) {
            return;
        }

        KeyCode code = event.getCode();
        if (code == KeyCode.PLUS || code == KeyCode.ADD || code == KeyCode.EQUALS) {
            zoomPreviewBy(PREVIEW_ZOOM_STEP);
            event.consume();
            return;
        }
        if (code == KeyCode.MINUS || code == KeyCode.SUBTRACT) {
            zoomPreviewBy(-PREVIEW_ZOOM_STEP);
            event.consume();
            return;
        }
        if (code == KeyCode.DIGIT0 || code == KeyCode.NUMPAD0) {
            onResetPreviewView();
            event.consume();
            return;
        }
        if (code == KeyCode.UP) {
            nudgePreview(0, -PREVIEW_NUDGE_AMOUNT);
            event.consume();
            return;
        }
        if (code == KeyCode.DOWN) {
            nudgePreview(0, PREVIEW_NUDGE_AMOUNT);
            event.consume();
            return;
        }
        if (code == KeyCode.LEFT) {
            nudgePreview(-PREVIEW_NUDGE_AMOUNT, 0);
            event.consume();
            return;
        }
        if (code == KeyCode.RIGHT) {
            nudgePreview(PREVIEW_NUDGE_AMOUNT, 0);
            event.consume();
        }
    }

    private void saveUndoState() {
        undoStack.push(new ScanSnapshot(
                allPages,
                selectedPage == null ? -1 : selectedPage.referenceId,
                nextReferenceId,
                nextFileId,
                collapsedDocuments,
                previewZoomMultiplier.get(),
                previewTranslateX,
                previewTranslateY
        ));

        while (undoStack.size() > MAX_UNDO_STEPS) {
            undoStack.removeLast();
        }

        updateUndoButtonState();
    }

    @FXML
    private void onUndoLastAction() {
        if (undoStack.isEmpty()) {
            return;
        }

        ScanSnapshot snapshot = undoStack.pop();
        allPages.clear();
        for (PageSnapshot pageSnapshot : snapshot.pages) {
            allPages.add(pageSnapshot.toScannedPage());
        }

        nextReferenceId = snapshot.nextReferenceId;
        nextFileId = snapshot.nextFileId;

        collapsedDocuments.clear();
        collapsedDocuments.addAll(snapshot.collapsedDocuments);

        previewZoomMultiplier.set(snapshot.previewZoomMultiplier);
        previewTranslateX = snapshot.previewTranslateX;
        previewTranslateY = snapshot.previewTranslateY;

        rebuildDocumentsFromPages();
        selectedPage = findPageByReferenceId(snapshot.selectedPageReferenceId);
        refreshWorkspace();
        clampPreviewTranslation();
        updateUndoButtonState();
    }

    private void updateUndoButtonState() {
        if (undoLastActionButton != null) {
            undoLastActionButton.setDisable(undoStack.isEmpty());
        }
    }

    @FXML
    private void onStartScanning() {
        if (startScanningButton.isDisabled()) {
            return;
        }

        UserPortalModel.ProfileItem profile = profileComboBox.getValue();
        String boxId = normalizedBoxIdInput();
        boxIdTextField.setText(boxId);
        currentSession = portalModel.startSession(profile, boxId);
        onSessionStarted.accept(currentSession);

        beginScanSession();
        showWorkspaceView();
    }

    private void beginScanSession() {
        allPages.clear();
        pendingPages.clear();
        documents.clear();
        collapsedDocuments.clear();
        undoStack.clear();

        nextReferenceId = 1;
        nextFileId = 1;

        selectedPage = null;
        resetPreviewViewState();
        hideFinishReviewModal();
        hideSubmitConfirmationModal();

        refreshWorkspace();
        updateUndoButtonState();
    }

    @FXML
    private void onScanNextFile() {
        saveUndoState();

        boolean barcode = shouldMockBarcode();
        boolean needsRescan = !barcode && shouldMockNeedsRescan();

        ScannedPage scannedPage = new ScannedPage(nextReferenceId, nextFileId, barcode, needsRescan);
        allPages.add(scannedPage);
        selectedPage = scannedPage;

        nextReferenceId++;
        nextFileId++;

        rebuildDocumentsFromPages();
        refreshWorkspace();
    }

    private boolean shouldMockBarcode() {
        return nextReferenceId > 1 && nextReferenceId % 5 == 0;
    }

    private boolean shouldMockNeedsRescan() {
        return nextReferenceId > 1 && nextReferenceId % 6 == 0;
    }

    private void rebuildDocumentsFromPages() {
        documents.clear();
        pendingPages.clear();

        for (ScannedPage page : allPages) {
            page.documentNumber = 0;
        }

        List<ScannedPage> currentDocumentPages = new ArrayList<>();
        int documentNumber = 1;

        for (ScannedPage page : allPages) {
            if (page.barcode) {
                if (!currentDocumentPages.isEmpty()) {
                    documents.add(createDocument(documentNumber, "Barcode split", currentDocumentPages));
                    documentNumber++;
                    currentDocumentPages.clear();
                }
                continue;
            }

            currentDocumentPages.add(page);

            if (page.splitReasonAfter != null && !page.splitReasonAfter.isBlank()) {
                documents.add(createDocument(documentNumber, page.splitReasonAfter, currentDocumentPages));
                documentNumber++;
                currentDocumentPages.clear();
            }
        }

        pendingPages.addAll(currentDocumentPages);
        collapsedDocuments.removeIf(documentId -> documentId > documents.size());
    }

    private DocumentGroup createDocument(int documentNumber, String splitReason, List<ScannedPage> pages) {
        DocumentGroup document = new DocumentGroup(documentNumber, splitReason);
        document.pages.addAll(pages);
        for (ScannedPage page : document.pages) {
            page.documentNumber = documentNumber;
        }
        return document;
    }

    @FXML
    private void onPreviousFile() {
        if (allPages.isEmpty() || selectedPage == null) {
            return;
        }

        int selectedIndex = allPages.indexOf(selectedPage);
        if (selectedIndex > 0) {
            selectedPage = allPages.get(selectedIndex - 1);
            refreshWorkspace();
        }
    }

    @FXML
    private void onNextFile() {
        if (allPages.isEmpty() || selectedPage == null) {
            return;
        }

        int selectedIndex = allPages.indexOf(selectedPage);
        if (selectedIndex < allPages.size() - 1) {
            selectedPage = allPages.get(selectedIndex + 1);
            refreshWorkspace();
        }
    }

    @FXML
    private void onPreviousReviewPage() {
        selectAdjacentReviewPage(-1);
    }

    @FXML
    private void onNextReviewPage() {
        selectAdjacentReviewPage(1);
    }

    private void selectAdjacentReviewPage(int direction) {
        List<ScannedPage> reviewPages = getReviewPages();

        if (reviewPages.isEmpty()) {
            selectedPage = null;
            refreshReviewWorkspace();
            return;
        }

        if (selectedPage == null || selectedPage.barcode || !reviewPages.contains(selectedPage)) {
            selectedPage = direction < 0 ? reviewPages.get(reviewPages.size() - 1) : reviewPages.get(0);
            refreshReviewWorkspace();
            return;
        }

        int selectedIndex = reviewPages.indexOf(selectedPage);
        int nextIndex = selectedIndex + direction;
        if (nextIndex < 0 || nextIndex >= reviewPages.size()) {
            return;
        }

        selectedPage = reviewPages.get(nextIndex);
        refreshReviewWorkspace();
    }

    private List<ScannedPage> getReviewPages() {
        List<ScannedPage> reviewPages = new ArrayList<>();
        for (ScannedPage page : allPages) {
            if (!page.barcode) {
                reviewPages.add(page);
            }
        }
        return reviewPages;
    }

    @FXML
    private void onZoomIn() {
        zoomPreviewBy(PREVIEW_ZOOM_STEP);
    }

    @FXML
    private void onZoomOut() {
        zoomPreviewBy(-PREVIEW_ZOOM_STEP);
    }

    private void zoomPreviewBy(double zoomDelta) {
        setPreviewZoom(previewZoomMultiplier.get() + zoomDelta);
    }

    private void setPreviewZoom(double zoom) {
        previewZoomMultiplier.set(clamp(zoom, MIN_PREVIEW_ZOOM, MAX_PREVIEW_ZOOM));
    }

    @FXML
    private void onResetPreviewView() {
        resetPreviewViewState();
        clampPreviewTranslation();
    }

    private void resetPreviewViewState() {
        previewZoomMultiplier.set(1.0);
        previewTranslateX = 0;
        previewTranslateY = 0;
        updatePreviewZoomLabel();

        if (currentPreviewWrapper != null) {
            currentPreviewWrapper.setTranslateX(0);
            currentPreviewWrapper.setTranslateY(0);
        }
    }

    private void updatePreviewZoomLabel() {
        if (previewZoomLabel != null) {
            previewZoomLabel.setText(Math.round(previewZoomMultiplier.get() * 100) + "%");
        }
    }

    @FXML
    private void onNudgePreviewUp() {
        nudgePreview(0, -PREVIEW_NUDGE_AMOUNT);
    }

    @FXML
    private void onNudgePreviewDown() {
        nudgePreview(0, PREVIEW_NUDGE_AMOUNT);
    }

    @FXML
    private void onNudgePreviewLeft() {
        nudgePreview(-PREVIEW_NUDGE_AMOUNT, 0);
    }

    @FXML
    private void onNudgePreviewRight() {
        nudgePreview(PREVIEW_NUDGE_AMOUNT, 0);
    }

    private void nudgePreview(double deltaX, double deltaY) {
        if (selectedPage == null || currentPreviewWrapper == null) {
            return;
        }

        previewHost.requestFocus();
        previewTranslateX += deltaX;
        previewTranslateY += deltaY;
        clampPreviewTranslation();
    }

    private void clampPreviewTranslation() {
        if (currentPreviewWrapper == null || previewHost == null) {
            return;
        }

        double scale = currentPreviewWrapper.getScaleX();
        if (scale <= 0) {
            scale = 1;
        }

        double scaledWidth = PREVIEW_PAGE_WIDTH * scale;
        double scaledHeight = PREVIEW_PAGE_HEIGHT * scale;
        double hostWidth = Math.max(1, previewHost.getWidth());
        double hostHeight = Math.max(1, previewHost.getHeight());

        double maxX = Math.abs(hostWidth - scaledWidth) / 2;
        double maxY = Math.abs(hostHeight - scaledHeight) / 2;

        previewTranslateX = clamp(previewTranslateX, -maxX, maxX);
        previewTranslateY = clamp(previewTranslateY, -maxY, maxY);

        currentPreviewWrapper.setTranslateX(previewTranslateX);
        currentPreviewWrapper.setTranslateY(previewTranslateY);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    @FXML
    private void onRotateLeft() {
        if (!hasNormalSelectedPage()) {
            return;
        }

        saveUndoState();
        selectedPage.rotationDegrees = normalizeRotation(selectedPage.rotationDegrees - 90);
        refreshWorkspace();
    }

    @FXML
    private void onRotateRight() {
        if (!hasNormalSelectedPage()) {
            return;
        }

        saveUndoState();
        selectedPage.rotationDegrees = normalizeRotation(selectedPage.rotationDegrees + 90);
        refreshWorkspace();
    }

    private int normalizeRotation(int rotationDegrees) {
        int normalized = rotationDegrees % 360;
        if (normalized < 0) {
            normalized += 360;
        }
        return normalized;
    }

    @FXML
    private void onDeleteSelectedPage() {
        if (selectedPage == null) {
            return;
        }

        saveUndoState();
        int selectedIndex = allPages.indexOf(selectedPage);
        allPages.remove(selectedPage);

        if (allPages.isEmpty()) {
            selectedPage = null;
        } else {
            int nextIndex = Math.min(selectedIndex, allPages.size() - 1);
            selectedPage = allPages.get(nextIndex);
        }

        rebuildDocumentsFromPages();
        refreshWorkspace();
    }

    @FXML
    private void onToggleRescanSelected() {
        if (!hasNormalSelectedPage()) {
            return;
        }

        saveUndoState();
        selectedPage.needsRescan = !selectedPage.needsRescan;
        refreshWorkspace();
    }

    @FXML
    private void onRescanMarkedPages() {
        boolean hasMarkedPages = false;
        for (ScannedPage page : allPages) {
            if (page.needsRescan) {
                hasMarkedPages = true;
                break;
            }
        }

        if (!hasMarkedPages) {
            return;
        }

        saveUndoState();
        for (ScannedPage page : allPages) {
            page.needsRescan = false;
        }
        refreshWorkspace();
    }

    @FXML
    private void onSplitHere() {
        if (!hasNormalSelectedPage()) {
            return;
        }

        saveUndoState();
        selectedPage.splitReasonAfter = "Manual split";
        rebuildDocumentsFromPages();
        refreshWorkspace();
    }

    @FXML
    private void onSaveProgress() {
        refreshWorkspace();
    }

    @FXML
    private void onFinishReview() {
        if (allPages.isEmpty()) {
            return;
        }

        if (!pendingPages.isEmpty()) {
            saveUndoState();
            ScannedPage lastPendingPage = pendingPages.get(pendingPages.size() - 1);
            lastPendingPage.splitReasonAfter = "Finish batch";
            rebuildDocumentsFromPages();
            refreshWorkspace();
        }

        updateFinishReviewModal();
        showFinishReviewModal();
    }

    private void updateFinishReviewModal() {
        finishReviewBoxIdLabel.setText(getBoxId());
        finishReviewProfileLabel.setText(getSelectedProfile());
        finishReviewDocumentsLabel.setText(documents.size() + " documents created");
        finishReviewPagesLabel.setText(getNormalPageCount() + " pages scanned");
    }

    private void showFinishReviewModal() {
        finishReviewOverlay.setVisible(true);
        finishReviewOverlay.setManaged(true);
        finishReviewOverlay.toFront();
    }

    private void hideFinishReviewModal() {
        finishReviewOverlay.setVisible(false);
        finishReviewOverlay.setManaged(false);
    }

    private void updateSubmitConfirmationModal() {
        submitConfirmationBoxIdLabel.setText(getBoxId());
        submitConfirmationProfileLabel.setText(getSelectedProfile());
        submitConfirmationSummaryLabel.setText(
                documents.size() + " documents · " + getNormalPageCount() + " pages submitted for QA"
        );
    }

    private void showSubmitConfirmationModal() {
        submitConfirmationOverlay.setVisible(true);
        submitConfirmationOverlay.setManaged(true);
        submitConfirmationOverlay.toFront();
    }

    private void hideSubmitConfirmationModal() {
        submitConfirmationOverlay.setVisible(false);
        submitConfirmationOverlay.setManaged(false);
    }

    @FXML
    private void onBackToScanningFromFinishModal() {
        hideFinishReviewModal();
    }

    @FXML
    private void onReviewScanFromFinishModal() {
        hideFinishReviewModal();
        ensureReviewSelection();
        refreshReviewWorkspace();
        showReviewWorkspaceView();
    }

    @FXML
    private void onSubmitForQaFromFinishModal() {
        markScanSubmittedForQa();
    }

    @FXML
    private void onBackToScanningFromReview() {
        refreshWorkspace();
        showWorkspaceView();
    }

    @FXML
    private void onSubmitForQaFromReview() {
        markScanSubmittedForQa();
    }

    private void markScanSubmittedForQa() {
        hideFinishReviewModal();

        workspaceSessionSubtitleLabel.setText(
                allPages.size() + " files scanned · "
                        + documents.size() + " documents created · submitted for QA"
        );
        reviewDocumentsValueLabel.setText(
                documents.size() + " · " + getNormalPageCount() + " pages · submitted for QA"
        );

        updateSubmitConfirmationModal();
        showSubmitConfirmationModal();
    }

    @FXML
    private void onStartNextScanAfterSubmit() {
        resetAfterSubmittedScan();
        hideSubmitConfirmationModal();
        showSetupView();
    }

    private void resetAfterSubmittedScan() {
        allPages.clear();
        pendingPages.clear();
        documents.clear();
        collapsedDocuments.clear();
        undoStack.clear();

        nextReferenceId = 1;
        nextFileId = 1;
        selectedPage = null;
        currentSession = null;

        UserPortalModel.ProfileItem defaultProfile = portalModel.getDefaultProfileForUser();
        profileComboBox.getSelectionModel().select(defaultProfile);
        boxIdTextField.setText(defaultBoxId(defaultProfile));

        profileInfoPanel.setVisible(false);
        profileInfoPanel.setManaged(false);
        updateProfileInfo(defaultProfile);

        resetPreviewViewState();
        refreshWorkspace();
        updateStartScanningState();
        updateUndoButtonState();
    }

    private boolean hasNormalSelectedPage() {
        return selectedPage != null && !selectedPage.barcode;
    }

    @FXML
    private void onViewMyScans() {
        navigator.showScans();
    }

    private void showSetupView() {
        scanSetupView.setVisible(true);
        scanSetupView.setManaged(true);
        scanWorkspaceView.setVisible(false);
        scanWorkspaceView.setManaged(false);
        reviewWorkspaceView.setVisible(false);
        reviewWorkspaceView.setManaged(false);
    }

    private void showWorkspaceView() {
        scanSetupView.setVisible(false);
        scanSetupView.setManaged(false);
        scanWorkspaceView.setVisible(true);
        scanWorkspaceView.setManaged(true);
        reviewWorkspaceView.setVisible(false);
        reviewWorkspaceView.setManaged(false);
    }

    private void showReviewWorkspaceView() {
        scanSetupView.setVisible(false);
        scanSetupView.setManaged(false);
        scanWorkspaceView.setVisible(false);
        scanWorkspaceView.setManaged(false);
        reviewWorkspaceView.setVisible(true);
        reviewWorkspaceView.setManaged(true);
    }

    private void refreshWorkspace() {
        updateWorkspaceHeader();
        renderDocumentTree();
        renderPreview();
        renderPageTray();
        updateUndoButtonState();
    }

    private void updateWorkspaceHeader() {
        String boxId = getBoxId();
        String profile = getSelectedProfile();

        workspaceSessionTitleLabel.setText("Scanning Session · " + boxId);
        workspaceSessionSubtitleLabel.setText(
                allPages.size() + " files scanned · "
                        + documents.size() + " documents created · "
                        + pendingPages.size() + " pending pages"
        );

        boxStructureSubtitleLabel.setText(profile + " · " + boxId);
        totalFilesLabel.setText(allPages.size() + " files");
        pageTrayTitleLabel.setText("Scanned Files");

        if (selectedPage == null) {
            selectedFileTitleLabel.setText("No file selected");
            selectedFileRefLabel.setText("Scan a file to begin.");
            return;
        }

        selectedFileTitleLabel.setText(selectedPage.fileName());
        String selectedInfo = "Ref: " + selectedPage.referenceIdLabel();

        if (selectedPage.documentNumber > 0) {
            selectedInfo += " · Document " + selectedPage.documentNumber;
            int pageNumber = getPageNumberInDocument(selectedPage);
            if (pageNumber > 0) {
                selectedInfo += " · Page " + pageNumber;
            }
        } else if (selectedPage.barcode) {
            selectedInfo += " · Barcode split marker";
        } else {
            selectedInfo += " · Pending document";
        }

        if (selectedPage.needsRescan) {
            selectedInfo += " · Needs rescan";
        }

        if (selectedPage.rotationDegrees != 0) {
            selectedInfo += " · Rotated " + selectedPage.rotationDegrees + "°";
        }

        selectedFileRefLabel.setText(selectedInfo);
    }

    private int getPageNumberInDocument(ScannedPage page) {
        for (DocumentGroup document : documents) {
            int pageIndex = document.pages.indexOf(page);
            if (pageIndex >= 0) {
                return pageIndex + 1;
            }
        }
        return -1;
    }

    private String getSelectedProfile() {
        UserPortalModel.ProfileItem profile = profileComboBox.getValue();
        return profile == null ? "No profile" : profile.name();
    }

    private String getBoxId() {
        String boxId = normalizedBoxIdInput();
        return boxId == null || boxId.isBlank() ? "No box ID" : boxId.trim();
    }

    private String normalizedBoxIdInput() {
        if (boxIdTextField == null || boxIdTextField.getText() == null) {
            return "";
        }
        return boxIdTextField.getText().trim().toUpperCase();
    }

    private void renderDocumentTree() {
        documentTreeContainer.getChildren().clear();

        if (documents.isEmpty()) {
            VBox emptyState = new VBox(6);
            emptyState.getStyleClass().add("document-tree-empty-state");

            Label title = new Label("No documents created yet");
            title.getStyleClass().add("document-tree-empty-title");

            Label copy = new Label("Scanned files appear in the tray below. Documents appear here after barcode detection, manual split, or finishing the batch.");
            copy.setWrapText(true);
            copy.getStyleClass().add("document-tree-empty-copy");

            emptyState.getChildren().addAll(title, copy);
            documentTreeContainer.getChildren().add(emptyState);
            return;
        }

        for (int index = 0; index < documents.size(); index++) {
            DocumentGroup document = documents.get(index);
            VBox documentBlock = new VBox(0);
            documentBlock.getStyleClass().add("document-tree-document-block");

            HBox documentHeader = createDocumentHeader(document);
            documentBlock.getChildren().add(documentHeader);

            if (!collapsedDocuments.contains(document.number)) {
                for (int pageIndex = 0; pageIndex < document.pages.size(); pageIndex++) {
                    documentBlock.getChildren().add(createDocumentTreePageRow(document.pages.get(pageIndex), pageIndex + 1));
                }
            }

            documentTreeContainer.getChildren().add(documentBlock);

            if (shouldShowTreeSplitRow(document, index)) {
                documentTreeContainer.getChildren().add(createDocumentTreeSplitRow(document.splitReason));
            }
        }
    }

    private HBox createDocumentHeader(DocumentGroup document) {
        HBox documentHeader = new HBox(9);
        documentHeader.setAlignment(Pos.CENTER_LEFT);
        documentHeader.getStyleClass().add("document-tree-document-header");

        Label chevron = new Label(collapsedDocuments.contains(document.number) ? "›" : "⌄");
        chevron.getStyleClass().add("document-tree-chevron");

        Label documentName = new Label("Document " + document.number);
        documentName.getStyleClass().add("document-tree-document-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label warning = new Label(document.hasPagesNeedingRescan() ? "!" : "");
        warning.getStyleClass().add("document-tree-warning");

        Label pageCount = new Label(document.pages.size() + " pages");
        pageCount.getStyleClass().add("document-tree-count");

        documentHeader.getChildren().addAll(chevron, documentName, spacer, warning, pageCount);

        documentHeader.setOnMouseClicked(event -> {
            if (collapsedDocuments.contains(document.number)) {
                collapsedDocuments.remove(document.number);
            } else {
                collapsedDocuments.add(document.number);
            }
            renderDocumentTree();
        });

        documentHeader.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
                documentHeader.getStyleClass().add("document-tree-drop-target");
            }
            event.consume();
        });

        documentHeader.setOnDragExited(event -> documentHeader.getStyleClass().remove("document-tree-drop-target"));
        documentHeader.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;

            if (dragboard.hasString()) {
                ScannedPage draggedPage = findPageByReferenceId(dragboard.getString());
                if (draggedPage != null && !draggedPage.barcode) {
                    movePageToDocumentEnd(draggedPage, document.number);
                    success = true;
                }
            }

            documentHeader.getStyleClass().remove("document-tree-drop-target");
            event.setDropCompleted(success);
            event.consume();
        });

        return documentHeader;
    }

    private boolean shouldShowTreeSplitRow(DocumentGroup document, int documentIndex) {
        boolean isLastDocument = documentIndex == documents.size() - 1;
        if ("Finish batch".equals(document.splitReason) && isLastDocument) {
            return false;
        }
        return "Barcode split".equals(document.splitReason) || "Manual split".equals(document.splitReason);
    }

    private HBox createDocumentTreeSplitRow(String splitReason) {
        HBox splitRow = new HBox(9);
        splitRow.setAlignment(Pos.CENTER);
        splitRow.getStyleClass().add("document-tree-barcode-split-row");

        Region leftLine = new Region();
        leftLine.getStyleClass().add("document-tree-barcode-split-line");
        HBox.setHgrow(leftLine, Priority.ALWAYS);

        Label splitLabel = new Label("||||  " + splitReason);
        splitLabel.getStyleClass().add("document-tree-split-row");

        Region rightLine = new Region();
        rightLine.getStyleClass().add("document-tree-barcode-split-line");
        HBox.setHgrow(rightLine, Priority.ALWAYS);

        splitRow.getChildren().addAll(leftLine, splitLabel, rightLine);
        return splitRow;
    }

    private HBox createDocumentTreePageRow(ScannedPage page, int pageNumberInDocument) {
        HBox row = new HBox(9);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("document-tree-page-row");

        if (page == selectedPage) {
            row.getStyleClass().add("document-tree-page-selected");
        }

        Label drag = new Label("⋮⋮");
        drag.getStyleClass().add("document-tree-page-drag");

        Label name = new Label("Page " + pageNumberInDocument);
        name.getStyleClass().add("document-tree-page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label warning = new Label(page.needsRescan ? "!" : "");
        warning.getStyleClass().add("document-tree-warning");

        row.getChildren().addAll(drag, name, spacer, warning);
        row.setOnMouseClicked(event -> {
            selectedPage = page;
            refreshWorkspace();
        });

        configurePageDrag(row, page);
        return row;
    }

    private void movePageToDocumentEnd(ScannedPage page, int targetDocumentNumber) {
        if (page == null || page.barcode) {
            return;
        }

        DocumentGroup targetDocument = findDocument(targetDocumentNumber);
        if (targetDocument == null) {
            return;
        }

        saveUndoState();
        page.splitReasonAfter = null;
        allPages.remove(page);

        ScannedPage lastTargetPage = targetDocument.pages.isEmpty() ? null : targetDocument.pages.get(targetDocument.pages.size() - 1);
        int insertIndex = lastTargetPage == null ? allPages.size() : allPages.indexOf(lastTargetPage) + 1;
        allPages.add(insertIndex, page);
        selectedPage = page;

        rebuildDocumentsFromPages();
        refreshWorkspace();
    }

    private DocumentGroup findDocument(int documentNumber) {
        for (DocumentGroup document : documents) {
            if (document.number == documentNumber) {
                return document;
            }
        }
        return null;
    }

    private void renderPreview() {
        previewHost.getChildren().clear();

        if (selectedPage == null) {
            currentPreviewWrapper = null;
            previewHost.getChildren().add(createEmptyPreview());
            return;
        }

        Node previewNode = selectedPage.barcode ? createBarcodePreview(selectedPage) : createDocumentPreview(selectedPage);
        previewHost.getChildren().add(wrapPreviewWithAutoScale(previewNode));
    }

    private Node wrapPreviewWithAutoScale(Node previewNode) {
        StackPane previewWrapper = new StackPane(previewNode);
        previewWrapper.setAlignment(Pos.CENTER);
        previewWrapper.setPickOnBounds(true);
        previewWrapper.setMaxWidth(PREVIEW_PAGE_WIDTH);
        previewWrapper.setMaxHeight(PREVIEW_PAGE_HEIGHT);

        DoubleBinding scaleBinding = Bindings.createDoubleBinding(() -> {
            double availableWidth = Math.max(1, previewHost.getWidth() - PREVIEW_SAFE_HORIZONTAL_PADDING);
            double availableHeight = Math.max(1, previewHost.getHeight() - PREVIEW_SAFE_VERTICAL_PADDING);

            double widthScale = availableWidth / PREVIEW_PAGE_WIDTH;
            double heightScale = availableHeight / PREVIEW_PAGE_HEIGHT;
            double autoScale = Math.min(1.0, Math.min(widthScale, heightScale));
            return autoScale * previewZoomMultiplier.get();
        }, previewHost.widthProperty(), previewHost.heightProperty(), previewZoomMultiplier);

        previewWrapper.scaleXProperty().bind(scaleBinding);
        previewWrapper.scaleYProperty().bind(scaleBinding);
        previewWrapper.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> previewHost.requestFocus());
        previewWrapper.addEventFilter(ScrollEvent.SCROLL, this::handlePreviewScroll);
        previewWrapper.addEventFilter(ZoomEvent.ZOOM, this::handlePreviewZoom);

        currentPreviewWrapper = previewWrapper;
        scaleBinding.addListener((observable, oldValue, newValue) -> clampPreviewTranslation());
        clampPreviewTranslation();

        return previewWrapper;
    }

    private Node createEmptyPreview() {
        VBox emptyPreview = new VBox(9);
        emptyPreview.setAlignment(Pos.CENTER);
        emptyPreview.getStyleClass().add("scan-preview-empty");

        Label title = new Label("Ready to scan");
        title.getStyleClass().add("scan-preview-empty-title");
        Label copy = new Label("Place a file in the scanner and click \"Scan Next File\".");
        copy.getStyleClass().add("scan-preview-empty-copy");

        emptyPreview.getChildren().addAll(title, copy);
        return emptyPreview;
    }

    private Node createBarcodePreview(ScannedPage page) {
        VBox barcodePreview = new VBox(15);
        barcodePreview.setAlignment(Pos.CENTER);
        barcodePreview.getStyleClass().add("mock-document-page");
        barcodePreview.setMinWidth(PREVIEW_PAGE_WIDTH);
        barcodePreview.setPrefWidth(PREVIEW_PAGE_WIDTH);
        barcodePreview.setMaxWidth(PREVIEW_PAGE_WIDTH);
        barcodePreview.setMinHeight(PREVIEW_PAGE_HEIGHT);
        barcodePreview.setPrefHeight(PREVIEW_PAGE_HEIGHT);
        barcodePreview.setMaxHeight(PREVIEW_PAGE_HEIGHT);

        Label badge = new Label("BARCODE DETECTED");
        badge.getStyleClass().add("barcode-preview-badge");
        Label title = new Label("Document split marker");
        title.getStyleClass().add("barcode-preview-title");
        Label barcode = new Label("||||||||||||||||||||||||||||");
        barcode.getStyleClass().add("barcode-preview-bars");
        Label copy = new Label("The previous scanned pages have been grouped into a document. The next normal file starts the next pending document.");
        copy.setWrapText(true);
        copy.setMaxWidth(360);
        copy.getStyleClass().add("barcode-preview-copy");

        barcodePreview.getChildren().addAll(badge, title, barcode, copy);
        return barcodePreview;
    }

    private Node createDocumentPreview(ScannedPage page) {
        VBox documentPage = new VBox(15);
        documentPage.setAlignment(Pos.TOP_LEFT);
        documentPage.getStyleClass().add("mock-document-page");
        documentPage.setMinWidth(PREVIEW_PAGE_WIDTH);
        documentPage.setPrefWidth(PREVIEW_PAGE_WIDTH);
        documentPage.setMaxWidth(PREVIEW_PAGE_WIDTH);
        documentPage.setMinHeight(PREVIEW_PAGE_HEIGHT);
        documentPage.setPrefHeight(PREVIEW_PAGE_HEIGHT);
        documentPage.setMaxHeight(PREVIEW_PAGE_HEIGHT);
        documentPage.setRotate(page.rotationDegrees);

        HBox topSection = new HBox();
        topSection.setAlignment(Pos.TOP_LEFT);

        VBox topLeft = new VBox(9,
                createLine("mock-line-dark", 180, 15),
                createLine("mock-line-medium", 126, 9)
        );

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        VBox topRight = new VBox(6,
                createLine("mock-line-medium", 90, 9),
                createLine("mock-line-medium", 108, 9)
        );
        topRight.setAlignment(Pos.TOP_RIGHT);
        topSection.getChildren().addAll(topLeft, topSpacer, topRight);

        VBox textLines = new VBox(6,
                createLine("mock-line-light", 405, 7),
                createLine("mock-line-light", 405, 7),
                createLine("mock-line-light", 372, 7),
                createLine("mock-line-light", 405, 7),
                createLine("mock-line-light", 318, 7)
        );

        VBox formArea = new VBox(9);
        formArea.getStyleClass().add("mock-form-area");
        HBox formHeading = new HBox(18,
                createLine("mock-line-dark", 144, 12),
                createLine("mock-line-medium", 78, 8)
        );
        HBox formInputs = new HBox(12, createInputSkeleton(), createInputSkeleton());
        Region wideInput = createInputSkeleton();
        wideInput.setMaxWidth(Double.MAX_VALUE);
        formArea.getChildren().addAll(formHeading, formInputs, wideInput);

        VBox bottomText = new VBox(6,
                createLine("mock-line-light", 405, 7),
                createLine("mock-line-light", 405, 7),
                createLine("mock-line-light", 315, 7)
        );

        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.BOTTOM_RIGHT);
        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

        VBox barcode = new VBox(3);
        barcode.setAlignment(Pos.CENTER);
        barcode.getStyleClass().add("mock-barcode-box");
        Label bars = new Label("||||||||||||");
        bars.getStyleClass().add("mock-barcode-bars");
        Region barcodeLine = createLine("mock-line-medium", 48, 6);
        barcode.getChildren().addAll(bars, barcodeLine);
        bottomRow.getChildren().addAll(bottomSpacer, barcode);

        if (page.needsRescan) {
            Label warning = new Label("Marked for rescan");
            warning.getStyleClass().add("preview-warning-banner");
            documentPage.getChildren().add(warning);
        }

        documentPage.getChildren().addAll(topSection, textLines, formArea, bottomText, bottomRow);
        return documentPage;
    }

    private Region createLine(String styleClass, double width, double height) {
        Region line = new Region();
        line.getStyleClass().add(styleClass);
        line.setMinWidth(width);
        line.setPrefWidth(width);
        line.setMaxWidth(width);
        line.setMinHeight(height);
        line.setPrefHeight(height);
        line.setMaxHeight(height);
        return line;
    }

    private Region createInputSkeleton() {
        Region input = new Region();
        input.getStyleClass().add("mock-input");
        input.setMinHeight(30);
        input.setPrefHeight(30);
        input.setMinWidth(174);
        input.setPrefWidth(174);
        return input;
    }

    private void renderPageTray() {
        pageTrayContainer.getChildren().clear();

        if (allPages.isEmpty()) {
            Label empty = new Label("No scanned files yet.");
            empty.getStyleClass().add("page-tray-empty-copy");
            pageTrayContainer.getChildren().add(empty);
            return;
        }

        for (ScannedPage page : allPages) {
            pageTrayContainer.getChildren().add(createTrayPageCard(page));

            if (page.barcode) {
                pageTrayContainer.getChildren().add(createTraySplitMarker());
            } else if (page.splitReasonAfter != null && !"Finish batch".equals(page.splitReasonAfter)) {
                pageTrayContainer.getChildren().add(createTraySplitMarker());
            }
        }
    }

    private VBox createTrayPageCard(ScannedPage page) {
        VBox card = new VBox(3);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add(page.barcode ? "page-tray-barcode-split-card" : "page-tray-item");

        if (page == selectedPage) {
            card.getStyleClass().add(page.barcode ? "page-tray-barcode-split-card-selected" : "page-tray-item-selected");
        }
        if (page.needsRescan) {
            card.getStyleClass().add("page-tray-item-warning");
        }

        StackPane thumbnail = new StackPane();
        thumbnail.getStyleClass().add("page-tray-thumbnail");

        if (page.barcode) {
            Label barcode = new Label("||||");
            barcode.getStyleClass().add("page-tray-barcode-mark");
            thumbnail.getChildren().add(barcode);
        } else {
            VBox lines = new VBox(3,
                    createLine("tray-line-dark", 27, 3),
                    createLine("tray-line-light", 42, 3),
                    createLine("tray-line-light", 36, 3),
                    createLine("tray-line-light", 30, 3)
            );
            lines.setAlignment(Pos.TOP_LEFT);
            thumbnail.getChildren().add(lines);
        }

        Label status = new Label(getTrayStatusText(page));
        status.getStyleClass().add("page-tray-status-badge");
        StackPane.setAlignment(status, Pos.TOP_RIGHT);
        thumbnail.getChildren().add(status);

        Label number = new Label(page.barcode ? "Split" : String.valueOf(page.fileId));
        number.getStyleClass().add(page.barcode ? "page-tray-barcode-split-label" : "page-tray-number");

        card.getChildren().addAll(thumbnail, number);
        card.setOnMouseClicked(event -> {
            selectedPage = page;
            refreshWorkspace();
        });

        configurePageDrag(card, page);
        return card;
    }

    private String getTrayStatusText(ScannedPage page) {
        if (page.needsRescan) {
            return "!";
        }
        if (page.barcode) {
            return "B";
        }
        if (page.splitReasonAfter != null && !"Finish batch".equals(page.splitReasonAfter)) {
            return "S";
        }
        return "";
    }

    private Node createTraySplitMarker() {
        StackPane splitMarker = new StackPane();
        splitMarker.getStyleClass().add("page-tray-document-split");
        Region line = new Region();
        line.getStyleClass().add("page-tray-document-split-line");
        splitMarker.getChildren().add(line);
        return splitMarker;
    }

    private void configurePageDrag(Node node, ScannedPage page) {
        node.setOnDragDetected(event -> {
            Dragboard dragboard = node.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(page.referenceId));
            dragboard.setContent(content);
            node.getStyleClass().add("page-tray-item-dragging");
            event.consume();
        });

        node.setOnDragDone(event -> node.getStyleClass().remove("page-tray-item-dragging"));

        node.setOnDragOver(event -> {
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasString()) {
                ScannedPage draggedPage = findPageByReferenceId(dragboard.getString());
                if (draggedPage != null && draggedPage != page) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
            }
            event.consume();
        });

        node.setOnDragEntered(event -> {
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasString()) {
                ScannedPage draggedPage = findPageByReferenceId(dragboard.getString());
                if (draggedPage != null && draggedPage != page) {
                    node.getStyleClass().add("page-tray-drop-target");
                }
            }
        });

        node.setOnDragExited(event -> node.getStyleClass().remove("page-tray-drop-target"));

        node.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;

            if (dragboard.hasString()) {
                ScannedPage draggedPage = findPageByReferenceId(dragboard.getString());
                if (draggedPage != null && draggedPage != page) {
                    movePageBefore(draggedPage, page);
                    success = true;
                }
            }

            node.getStyleClass().remove("page-tray-drop-target");
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private ScannedPage findPageByReferenceId(String referenceIdText) {
        if (referenceIdText == null || referenceIdText.isBlank()) {
            return null;
        }

        try {
            return findPageByReferenceId(Integer.parseInt(referenceIdText));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private ScannedPage findPageByReferenceId(int referenceId) {
        for (ScannedPage page : allPages) {
            if (page.referenceId == referenceId) {
                return page;
            }
        }
        return null;
    }

    private void movePageBefore(ScannedPage draggedPage, ScannedPage targetPage) {
        if (draggedPage == null || targetPage == null || draggedPage == targetPage) {
            return;
        }

        saveUndoState();
        int targetIndex = allPages.indexOf(targetPage);
        allPages.remove(draggedPage);

        int adjustedTargetIndex = allPages.indexOf(targetPage);
        if (adjustedTargetIndex < 0) {
            adjustedTargetIndex = Math.min(targetIndex, allPages.size());
        }

        allPages.add(adjustedTargetIndex, draggedPage);
        selectedPage = draggedPage;
        rebuildDocumentsFromPages();
        refreshWorkspace();
    }

    private void ensureReviewSelection() {
        if (selectedPage != null && !selectedPage.barcode && findDocumentContainingPage(selectedPage) != null) {
            return;
        }

        for (DocumentGroup document : documents) {
            if (!document.pages.isEmpty()) {
                selectedPage = document.pages.get(0);
                return;
            }
        }

        for (ScannedPage page : allPages) {
            if (!page.barcode) {
                selectedPage = page;
                return;
            }
        }

        selectedPage = null;
    }

    private void refreshReviewWorkspace() {
        updateReviewHeader();
        renderReviewDocumentList();
        renderReviewPreview();
        renderReviewPageTray();
    }

    private void updateReviewHeader() {
        reviewBoxValueLabel.setText(getBoxId());
        reviewProfileValueLabel.setText(getSelectedProfile());
        reviewDocumentsValueLabel.setText(documents.size() + " · " + getNormalPageCount() + " pages");

        if (selectedPage == null) {
            reviewSelectedTitleLabel.setText("No page selected");
            return;
        }

        DocumentGroup document = findDocumentContainingPage(selectedPage);
        int pageNumber = getPageNumberInDocument(selectedPage);

        if (document == null || pageNumber < 1) {
            reviewSelectedTitleLabel.setText(selectedPage.fileName());
            return;
        }

        reviewSelectedTitleLabel.setText("Document " + document.number + " · Page " + pageNumber);
    }

    private int getNormalPageCount() {
        int count = 0;
        for (ScannedPage page : allPages) {
            if (!page.barcode) {
                count++;
            }
        }
        return count;
    }

    private void renderReviewDocumentList() {
        reviewDocumentListContainer.getChildren().clear();

        if (documents.isEmpty()) {
            VBox emptyState = new VBox(6);
            emptyState.getStyleClass().add("review-empty-state");

            Label title = new Label("No documents created");
            title.getStyleClass().add("review-empty-title");
            Label copy = new Label("Finish the scan first so documents can be reviewed.");
            copy.setWrapText(true);
            copy.getStyleClass().add("review-empty-copy");

            emptyState.getChildren().addAll(title, copy);
            reviewDocumentListContainer.getChildren().add(emptyState);
            return;
        }

        for (int index = 0; index < documents.size(); index++) {
            DocumentGroup document = documents.get(index);

            HBox documentCard = new HBox(9);
            documentCard.setAlignment(Pos.CENTER_LEFT);
            documentCard.getStyleClass().add("review-document-card");
            if (document.pages.contains(selectedPage)) {
                documentCard.getStyleClass().add("review-document-card-selected");
            }

            Label title = new Label("Document " + document.number);
            title.getStyleClass().add("review-document-title");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label warning = new Label(document.hasPagesNeedingRescan() ? "!" : "");
            warning.getStyleClass().add("review-document-warning");

            Label count = new Label(document.pages.size() + " pages");
            count.getStyleClass().add("review-document-count");

            documentCard.getChildren().addAll(title, spacer, warning, count);
            documentCard.setOnMouseClicked(event -> {
                if (!document.pages.isEmpty()) {
                    selectedPage = document.pages.get(0);
                    refreshReviewWorkspace();
                }
            });

            reviewDocumentListContainer.getChildren().add(documentCard);
            if (index < documents.size() - 1) {
                reviewDocumentListContainer.getChildren().add(createReviewSplitRow());
            }
        }
    }

    private Node createReviewSplitRow() {
        HBox row = new HBox(9);
        row.setAlignment(Pos.CENTER);
        row.getStyleClass().add("review-split-row");

        Region leftLine = new Region();
        leftLine.getStyleClass().add("review-split-line");
        HBox.setHgrow(leftLine, Priority.ALWAYS);

        Label label = new Label("||||  Barcode split");
        label.getStyleClass().add("review-split-label");

        Region rightLine = new Region();
        rightLine.getStyleClass().add("review-split-line");
        HBox.setHgrow(rightLine, Priority.ALWAYS);

        row.getChildren().addAll(leftLine, label, rightLine);
        return row;
    }

    private void renderReviewPreview() {
        reviewPreviewHost.getChildren().clear();

        if (selectedPage == null) {
            reviewPreviewHost.getChildren().add(createReviewEmptyPreview());
            return;
        }

        Node previewNode = createDocumentPreview(selectedPage);
        reviewPreviewHost.getChildren().add(wrapReviewPreviewWithAutoScale(previewNode));
    }

    private Node wrapReviewPreviewWithAutoScale(Node previewNode) {
        StackPane wrapper = new StackPane(previewNode);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMaxWidth(PREVIEW_PAGE_WIDTH);
        wrapper.setMaxHeight(PREVIEW_PAGE_HEIGHT);

        DoubleBinding scaleBinding = Bindings.createDoubleBinding(() -> {
            double availableWidth = Math.max(1, reviewPreviewHost.getWidth() - PREVIEW_SAFE_HORIZONTAL_PADDING);
            double availableHeight = Math.max(1, reviewPreviewHost.getHeight() - PREVIEW_SAFE_VERTICAL_PADDING);

            double widthScale = availableWidth / PREVIEW_PAGE_WIDTH;
            double heightScale = availableHeight / PREVIEW_PAGE_HEIGHT;
            return Math.min(1.0, Math.min(widthScale, heightScale));
        }, reviewPreviewHost.widthProperty(), reviewPreviewHost.heightProperty());

        wrapper.scaleXProperty().bind(scaleBinding);
        wrapper.scaleYProperty().bind(scaleBinding);
        return wrapper;
    }

    private Node createReviewEmptyPreview() {
        VBox emptyPreview = new VBox(9);
        emptyPreview.setAlignment(Pos.CENTER);
        emptyPreview.getStyleClass().add("review-preview-empty");

        Region icon = new Region();
        icon.getStyleClass().add("review-preview-empty-icon");
        Label copy = new Label("Scanned Page Preview");
        copy.getStyleClass().add("review-preview-empty-copy");

        emptyPreview.getChildren().addAll(icon, copy);
        return emptyPreview;
    }

    private void renderReviewPageTray() {
        reviewPageTrayContainer.getChildren().clear();
        DocumentGroup selectedDocument = findDocumentContainingPage(selectedPage);

        if (selectedDocument == null || selectedDocument.pages.isEmpty()) {
            Label empty = new Label("No pages available.");
            empty.getStyleClass().add("page-tray-empty-copy");
            reviewPageTrayContainer.getChildren().add(empty);
            return;
        }

        for (int index = 0; index < selectedDocument.pages.size(); index++) {
            reviewPageTrayContainer.getChildren().add(createReviewTrayPageCard(selectedDocument.pages.get(index), index + 1));
        }
    }

    private VBox createReviewTrayPageCard(ScannedPage page, int pageNumber) {
        VBox card = new VBox(3);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("review-page-tray-item");

        if (page == selectedPage) {
            card.getStyleClass().add("review-page-tray-item-selected");
        }
        if (page.needsRescan) {
            card.getStyleClass().add("review-page-tray-item-warning");
        }

        StackPane thumbnail = new StackPane();
        thumbnail.getStyleClass().add("review-page-tray-thumbnail");

        VBox lines = new VBox(3,
                createLine("tray-line-dark", 27, 3),
                createLine("tray-line-light", 42, 3),
                createLine("tray-line-light", 36, 3),
                createLine("tray-line-light", 30, 3)
        );
        lines.setAlignment(Pos.TOP_LEFT);
        thumbnail.getChildren().add(lines);

        Label status = new Label(page.needsRescan ? "!" : "");
        status.getStyleClass().add("page-tray-status-badge");
        StackPane.setAlignment(status, Pos.TOP_RIGHT);
        thumbnail.getChildren().add(status);

        Label number = new Label(String.valueOf(pageNumber));
        number.getStyleClass().add("review-page-tray-number");

        card.getChildren().addAll(thumbnail, number);
        card.setOnMouseClicked(event -> {
            selectedPage = page;
            refreshReviewWorkspace();
        });
        return card;
    }

    private DocumentGroup findDocumentContainingPage(ScannedPage page) {
        if (page == null) {
            return null;
        }

        for (DocumentGroup document : documents) {
            if (document.pages.contains(page)) {
                return document;
            }
        }
        return null;
    }

    private String defaultBoxId(UserPortalModel.ProfileItem profile) {
        if (profile != null && !profile.assignedBoxes().isEmpty()) {
            return profile.assignedBoxes().get(0).id();
        }
        return "BOX-2026-042";
    }

    private static final class ScanSnapshot {
        private final List<PageSnapshot> pages = new ArrayList<>();
        private final int selectedPageReferenceId;
        private final int nextReferenceId;
        private final int nextFileId;
        private final Set<Integer> collapsedDocuments = new HashSet<>();
        private final double previewZoomMultiplier;
        private final double previewTranslateX;
        private final double previewTranslateY;

        private ScanSnapshot(List<ScannedPage> pages,
                             int selectedPageReferenceId,
                             int nextReferenceId,
                             int nextFileId,
                             Set<Integer> collapsedDocuments,
                             double previewZoomMultiplier,
                             double previewTranslateX,
                             double previewTranslateY) {
            for (ScannedPage page : pages) {
                this.pages.add(new PageSnapshot(page));
            }
            this.selectedPageReferenceId = selectedPageReferenceId;
            this.nextReferenceId = nextReferenceId;
            this.nextFileId = nextFileId;
            this.collapsedDocuments.addAll(collapsedDocuments);
            this.previewZoomMultiplier = previewZoomMultiplier;
            this.previewTranslateX = previewTranslateX;
            this.previewTranslateY = previewTranslateY;
        }
    }

    private static final class PageSnapshot {
        private final int referenceId;
        private final int fileId;
        private final boolean barcode;
        private final boolean needsRescan;
        private final int rotationDegrees;
        private final String splitReasonAfter;

        private PageSnapshot(ScannedPage page) {
            this.referenceId = page.referenceId;
            this.fileId = page.fileId;
            this.barcode = page.barcode;
            this.needsRescan = page.needsRescan;
            this.rotationDegrees = page.rotationDegrees;
            this.splitReasonAfter = page.splitReasonAfter;
        }

        private ScannedPage toScannedPage() {
            ScannedPage page = new ScannedPage(referenceId, fileId, barcode, needsRescan);
            page.rotationDegrees = rotationDegrees;
            page.splitReasonAfter = splitReasonAfter;
            return page;
        }
    }

    private static final class DocumentGroup {
        private final int number;
        private final String splitReason;
        private final List<ScannedPage> pages = new ArrayList<>();

        private DocumentGroup(int number, String splitReason) {
            this.number = number;
            this.splitReason = splitReason;
        }

        private boolean hasPagesNeedingRescan() {
            for (ScannedPage page : pages) {
                if (page.needsRescan) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class ScannedPage {
        private final int referenceId;
        private final int fileId;
        private final boolean barcode;
        private boolean needsRescan;
        private int rotationDegrees;
        private int documentNumber;
        private String splitReasonAfter;

        private ScannedPage(int referenceId, int fileId, boolean barcode, boolean needsRescan) {
            this.referenceId = referenceId;
            this.fileId = fileId;
            this.barcode = barcode;
            this.needsRescan = needsRescan;
        }

        private String fileName() {
            return barcode ? "Barcode Marker" : "File " + fileId;
        }

        private String referenceIdLabel() {
            return "REF-" + String.format("%04d", referenceId);
        }
    }
}
