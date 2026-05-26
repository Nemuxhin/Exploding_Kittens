package easv.gui.controller.user;

import easv.be.Document;
import easv.be.PageImage;
import easv.be.ScanProfile;
import easv.be.ScanSession;
import easv.bll.ScanImportResult;
import easv.bll.ScanManager;
import easv.bll.TiffExportManager;
import easv.bll.TiffImageSupport;
import easv.gui.BackgroundExecutor;
import easv.gui.UserPortalModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.input.ZoomEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ScanController {

    private static final double PREVIEW_PAGE_WIDTH = 500;
    private static final double PREVIEW_PAGE_HEIGHT = 560;
    private static final double PREVIEW_SAFE_HORIZONTAL_PADDING = 132;
    private static final double PREVIEW_SAFE_VERTICAL_PADDING = 72;

    private static final double MIN_PREVIEW_ZOOM = 0.50;
    private static final double MAX_PREVIEW_ZOOM = 2.50;
    private static final double PREVIEW_ZOOM_STEP = 0.10;
    private static final double PREVIEW_NUDGE_AMOUNT = 36;

    private static final int MAX_UNDO_STEPS = 30;
    private static final int ROTATION_STEP_DEGREES = 1;
    private static final List<String> BOX_ROTATION_OPTIONS = List.of("0°", "90°", "180°", "270°");

    @FXML private VBox scanSetupView;
    @FXML private BorderPane scanWorkspaceView;
    @FXML private BorderPane reviewWorkspaceView;

    @FXML private ComboBox<String> profileComboBox;
    @FXML private ComboBox<String> boxRotationComboBox;
    @FXML private ComboBox<String> pageRotationComboBox;
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
    @FXML private Button rotateLeftButton;
    @FXML private Button rotateRightButton;
    @FXML private Button documentTreeGridViewButton;
    @FXML private Button documentTreeListViewButton;
    @FXML private Button reviewDocumentGridViewButton;
    @FXML private Button reviewDocumentListViewButton;

    @FXML private Label workspaceSessionTitleLabel;
    @FXML private Label workspaceSessionSubtitleLabel;
    @FXML private Label workspaceSessionInlineSubtitleLabel;
    @FXML private Label headerReferenceInfoLabel;
    @FXML private Label headerFilesInfoLabel;
    @FXML private Label headerDocumentsInfoLabel;

    @FXML private Label boxStructureSubtitleLabel;
    @FXML private Label selectedFileTitleLabel;
    @FXML private Label selectedFileRefLabel;
    @FXML private Label totalFilesLabel;
    @FXML private Label pageTrayTitleLabel;
    @FXML private Label previewZoomLabel;
    @FXML private Label reviewReferenceInfoLabel;
    @FXML private Label reviewFilesInfoLabel;
    @FXML private Label reviewDocumentsInfoLabel;
    @FXML private Label reviewZoomLabel;

    @FXML private VBox scanLeftPanel;
    @FXML private VBox scanToolsPanel;
    @FXML private VBox documentTreeContainer;
    @FXML private ScrollPane documentTreeScrollPane;
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
    @FXML private Label reviewSidebarSubtitleLabel;
    @FXML private Label reviewSelectionProfileValueLabel;
    @FXML private Label reviewSelectionBoxValueLabel;
    @FXML private Label reviewSelectionDocumentValueLabel;
    @FXML private Label reviewSelectionFileValueLabel;
    @FXML private Label reviewSelectionReferenceValueLabel;
    @FXML private Label reviewSelectionFileIdValueLabel;
    @FXML private VBox reviewDocumentListContainer;
    @FXML private Label reviewSelectedTitleLabel;
    @FXML private StackPane reviewPreviewHost;
    @FXML private HBox reviewPageTrayContainer;

    private final List<ScannedPage> allPages = new ArrayList<>();
    private final List<ScannedPage> pendingPages = new ArrayList<>();
    private final List<DocumentGroup> documents = new ArrayList<>();
    private final Set<Integer> collapsedDocuments = new HashSet<>();
    private final Deque<ScanSnapshot> undoStack = new ArrayDeque<>();
    private final Map<Integer, Image> previewImageCache = new HashMap<>();
    private final Map<Integer, String> previewLoadFailures = new HashMap<>();
    private final Set<Integer> previewLoadsInProgress = new HashSet<>();
    private final ScanManager scanManager = new ScanManager();
    private final TiffExportManager tiffExportManager = new TiffExportManager();

    private final DoubleProperty previewZoomMultiplier = new SimpleDoubleProperty(1.0);
    private final DoubleProperty reviewZoomMultiplier = new SimpleDoubleProperty(1.0);

    private int nextReferenceId = 1;
    private int nextFileId = 1;
    private ScanSession activeScanSession;
    private boolean scanInProgress = false;
    private int sessionRotationDegrees = 0;
    private boolean syncingBoxRotationComboBox = false;
    private boolean syncingPageRotationComboBox = false;

    private double previewTranslateX = 0;
    private double previewTranslateY = 0;
    private double previewDragStartX = 0;
    private double previewDragStartY = 0;
    private double previewTranslateStartX = 0;
    private double previewTranslateStartY = 0;
    private double reviewPreviewTranslateX = 0;
    private double reviewPreviewTranslateY = 0;

    private StackPane currentPreviewWrapper;
    private StackPane currentReviewPreviewWrapper;

    private ScannedPage selectedPage;
    private boolean documentTreeListView = false;
    private boolean reviewDocumentListView = false;

    private UserNavigator navigator = UserNavigator.none();
    private UserPortalModel portalModel = new UserPortalModel();

    public void setNavigator(UserNavigator navigator) {
        this.navigator = navigator == null ? UserNavigator.none() : navigator;
    }

    public void setPortalModel(UserPortalModel portalModel) {
        if (portalModel != null) {
            this.portalModel = portalModel;
        }
    }

    public void resumeHistoryScan(UserPortalModel.HistoryItem item) {
        if (item == null) {
            return;
        }

        openScanSession(item.sessionId(), item.boxId(), item.profileName());
    }

    public void resumeRecentScan(UserPortalModel.RecentScanItem item) {
        if (item == null) {
            return;
        }

        openScanSession(item.sessionId(), item.boxId(), item.profileName());
    }

    @FXML
    private void initialize() {
        configureProfiles();
        configureBoxRotation();
        configurePageRotation();
        configureProfileInfo();
        configureValidation();
        configureDocumentTreeScroll();
        configureDocumentTreeViewToggle();
        configureWorkspacePanelClipping();
        configurePreviewInteractions();
        configureReviewPreviewInteractions();
        updatePreviewZoomLabel();
        updateReviewZoomLabel();
        updateUndoButtonState();
        hideFinishReviewModal();
        hideSubmitConfirmationModal();
        showSetupView();
    }

    private void configurePageRotation() {
        if (pageRotationComboBox == null) {
            return;
        }

        pageRotationComboBox.getItems().setAll(BOX_ROTATION_OPTIONS);
        pageRotationComboBox.setEditable(true);
        pageRotationComboBox.setPromptText("Enter rotation in degrees");
        syncPageRotationComboBox();
        pageRotationComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isBlank() || syncingPageRotationComboBox) {
                return;
            }

            applyPageRotationSelection(newValue);
        });

        if (pageRotationComboBox.getEditor() != null) {
            pageRotationComboBox.getEditor().setOnAction(event -> commitCustomPageRotation());
            pageRotationComboBox.getEditor().focusedProperty().addListener((observable, oldValue, focused) -> {
                if (!focused) {
                    commitCustomPageRotation();
                }
            });
        }

        if (rotateLeftButton != null) {
            rotateLeftButton.setVisible(false);
            rotateLeftButton.setManaged(false);
        }
        if (rotateRightButton != null) {
            rotateRightButton.setVisible(false);
            rotateRightButton.setManaged(false);
        }
    }

    private void configureDocumentTreeScroll() {
        if (documentTreeScrollPane == null) {
            return;
        }

        documentTreeScrollPane.setFitToHeight(false);
        documentTreeScrollPane.setPannable(true);
        documentTreeScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        installDocumentTreeScrollHandler(documentTreeScrollPane);
        if (documentTreeContainer != null) {
            documentTreeContainer.setMinHeight(Region.USE_PREF_SIZE);
            installDocumentTreeScrollHandler(documentTreeContainer);
        }
    }

    private void configureDocumentTreeViewToggle() {
        updateDocumentTreeViewToggleButtons();
        updateReviewDocumentViewToggleButtons();
    }

    private void configureWorkspacePanelClipping() {
        installPanelClip(scanLeftPanel);
        installPanelClip(scanToolsPanel);
    }

    private void installPanelClip(Region panel) {
        if (panel == null) {
            return;
        }

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(panel.widthProperty());
        clip.heightProperty().bind(panel.heightProperty());
        panel.setClip(clip);
    }

    @FXML
    private void onShowDocumentTreeGridView() {
        documentTreeListView = false;
        updateDocumentTreeViewToggleButtons();
        renderDocumentTree();
    }

    @FXML
    private void onShowDocumentTreeListView() {
        documentTreeListView = true;
        updateDocumentTreeViewToggleButtons();
        renderDocumentTree();
    }

    private void updateDocumentTreeViewToggleButtons() {
        setDocumentTreeViewButtonActive(documentTreeGridViewButton, !documentTreeListView);
        setDocumentTreeViewButtonActive(documentTreeListViewButton, documentTreeListView);
    }

    @FXML
    private void onShowReviewDocumentGridView() {
        reviewDocumentListView = false;
        updateReviewDocumentViewToggleButtons();
        renderReviewDocumentList();
    }

    @FXML
    private void onShowReviewDocumentListView() {
        reviewDocumentListView = true;
        updateReviewDocumentViewToggleButtons();
        renderReviewDocumentList();
    }

    private void updateReviewDocumentViewToggleButtons() {
        setDocumentTreeViewButtonActive(reviewDocumentGridViewButton, !reviewDocumentListView);
        setDocumentTreeViewButtonActive(reviewDocumentListViewButton, reviewDocumentListView);
    }

    private void setDocumentTreeViewButtonActive(Button button, boolean active) {
        if (button == null) {
            return;
        }

        button.getStyleClass().remove("document-tree-view-toggle-button-active");
        if (active) {
            button.getStyleClass().add("document-tree-view-toggle-button-active");
        }
    }

    private void installDocumentTreeScrollHandler(Node node) {
        node.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (documentTreeContainer == null || documentTreeScrollPane == null) {
                return;
            }

            double contentHeight = documentTreeContainer.getBoundsInLocal().getHeight();
            double viewportHeight = documentTreeScrollPane.getViewportBounds().getHeight();
            double scrollableHeight = contentHeight - viewportHeight;

            if (scrollableHeight <= 0) {
                return;
            }

            double delta = event.getDeltaY();
            double nextValue = documentTreeScrollPane.getVvalue() - (delta / scrollableHeight);
            documentTreeScrollPane.setVvalue(clamp(nextValue, 0.0, 1.0));
            event.consume();
        });
    }

    private void configureProfiles() {
        profileComboBox.getItems().setAll(
                "Building Archive",
                "Technical Drawings",
                "Court Records",
                "Standard Scan"
        );

        profileComboBox.setPromptText("Select profile");
        profileComboBox.getSelectionModel().clearSelection();

        profileComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
                updateProfileInfo(newValue)
        );
    }

    private void configureBoxRotation() {
        if (boxRotationComboBox == null) {
            return;
        }

        boxRotationComboBox.getItems().setAll(BOX_ROTATION_OPTIONS);
        boxRotationComboBox.setEditable(true);
        boxRotationComboBox.setPromptText("Enter rotation in degrees");
        syncBoxRotationComboBox();
        boxRotationComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isBlank()) {
                return;
            }

            applyBoxRotationSelection(oldValue, newValue);
        });

        if (boxRotationComboBox.getEditor() != null) {
            boxRotationComboBox.getEditor().setOnAction(event -> commitCustomBoxRotation());
            boxRotationComboBox.getEditor().focusedProperty().addListener((observable, oldValue, focused) -> {
                if (!focused) {
                    commitCustomBoxRotation();
                }
            });
        }
    }

    private void commitCustomBoxRotation() {
        if (boxRotationComboBox == null || boxRotationComboBox.getEditor() == null) {
            return;
        }

        String editorValue = boxRotationComboBox.getEditor().getText();
        if (editorValue == null || editorValue.isBlank()) {
            syncBoxRotationComboBox();
            return;
        }

        boxRotationComboBox.setValue(formatRotationDegrees(parseRotationDegrees(editorValue)));
    }

    private void openScanSession(UUID sessionId, String boxId, String profileName) {
        if (boxIdTextField != null) {
            boxIdTextField.setText(boxId == null ? "" : boxId);
        }
        if (profileComboBox != null) {
            profileComboBox.setValue(profileName);
            updateProfileInfo(profileName);
        }

        updateStartScanningState();
        showWorkspaceView();
        selectedFileTitleLabel.setText("Loading scan session");
        selectedFileRefLabel.setText("Please wait while we open the saved box session.");
        refreshWorkspace();

        String selectedBoxId = getBoxId();
        String selectedProfile = getSelectedProfile();
        BackgroundExecutor.io().execute(() -> {
            ScanManager.ResumedSession resumedSession = (sessionId == null
                    ? scanManager.resumeLatestSession(selectedBoxId, selectedProfile)
                    : scanManager.resumeSession(sessionId))
                    .orElse(null);
            UserPortalModel.InMemoryScanProgress savedProgress = sessionId == null
                    ? null
                    : portalModel.fetchSavedScanProgress(sessionId);
            UserPortalModel.InMemoryScanProgress returnedQaProgress = sessionId == null
                    ? null
                    : portalModel.fetchReturnedQaProgress(sessionId);
            Platform.runLater(() -> {
                if (!selectedBoxId.equals(getBoxId()) || !selectedProfile.equals(getSelectedProfile())) {
                    return;
                }

                if (resumedSession == null) {
                    beginScanSession();
                } else {
                    restoreScanSession(resumedSession, returnedQaProgress != null ? returnedQaProgress : savedProgress);
                }
            });
        });
    }

    private void restoreScanSession(ScanManager.ResumedSession resumedSession, UserPortalModel.InMemoryScanProgress returnedQaProgress) {
        allPages.clear();
        pendingPages.clear();
        documents.clear();
        collapsedDocuments.clear();
        undoStack.clear();
        previewImageCache.clear();
        previewLoadFailures.clear();
        previewLoadsInProgress.clear();

        nextReferenceId = 1;
        nextFileId = 1;
        activeScanSession = resumedSession.session();
        scanInProgress = false;
        sessionRotationDegrees = 0;

        if (returnedQaProgress != null && !returnedQaProgress.pages().isEmpty()) {
            restoreFromSavedProgress(returnedQaProgress);
        } else {
            for (Document document : resumedSession.documents()) {
                for (PageImage pageImage : document.getPages()) {
                    allPages.add(mapStoredPage(pageImage));
                }
            }
            rebuildDocumentsFromPages();
        }
        selectedPage = allPages.stream()
                .filter(page -> !page.barcode)
                .findFirst()
                .orElseGet(() -> allPages.isEmpty() ? null : allPages.get(0));

        resetPreviewViewState();
        hideFinishReviewModal();
        hideSubmitConfirmationModal();

        syncBoxRotationComboBox();
        refreshWorkspace();
        updateUndoButtonState();
    }

    private void restoreFromSavedProgress(UserPortalModel.InMemoryScanProgress progress) {
        nextReferenceId = 1;
        nextFileId = 1;
        for (UserPortalModel.InMemoryScanPage storedPage : progress.pages()) {
            ScannedPage page = new ScannedPage(
                    Math.max(1, storedPage.referenceId()),
                    Math.max(1, storedPage.fileId()),
                    storedPage.barcode(),
                    storedPage.needsRescan(),
                    storedPage.sourceReference(),
                    storedPage.displayContent(),
                    storedPage.previewContent(),
                    extractDisplayContentBytes(storedPage.previewContent())
            );
            page.documentNumber = storedPage.documentNumber();
            page.rotationDegrees = normalizeRotation(storedPage.rotationDegrees());
            page.splitReasonAfter = storedPage.splitReasonAfter();
            allPages.add(page);
            nextReferenceId = Math.max(nextReferenceId, page.referenceId + 1);
            nextFileId = Math.max(nextFileId, page.fileId + 1);
        }
        rebuildDocumentsFromPages();
    }

    private void commitCustomPageRotation() {
        if (pageRotationComboBox == null || pageRotationComboBox.getEditor() == null) {
            return;
        }

        String editorValue = pageRotationComboBox.getEditor().getText();
        if (editorValue == null || editorValue.isBlank()) {
            syncPageRotationComboBox();
            return;
        }

        pageRotationComboBox.setValue(formatRotationDegrees(parseRotationDegrees(editorValue)));
    }

    private void applyBoxRotationSelection(String oldValue, String newValue) {
        int newRotationDegrees = parseRotationDegrees(newValue);

        if (syncingBoxRotationComboBox || sessionRotationDegrees == newRotationDegrees) {
            sessionRotationDegrees = newRotationDegrees;
            return;
        }

        int oldRotationDegrees = oldValue == null || oldValue.isBlank()
                ? sessionRotationDegrees
                : parseRotationDegrees(oldValue);

        sessionRotationDegrees = newRotationDegrees;

        if (allPages.isEmpty()) {
            return;
        }

        int rotationDelta = normalizeRotation(newRotationDegrees - oldRotationDegrees);
        if (rotationDelta == 0) {
            return;
        }

        saveUndoState();

        for (ScannedPage page : allPages) {
            page.rotationDegrees = normalizeRotation(page.rotationDegrees + rotationDelta);
        }

        refreshWorkspace();
    }

    private void applyPageRotationSelection(String newValue) {
        ScannedPage page = resolveActiveNormalPage();
        if (page == null) {
            syncPageRotationComboBox();
            return;
        }

        int newRotationDegrees = parseRotationDegrees(newValue);
        if (page.rotationDegrees == newRotationDegrees) {
            syncPageRotationComboBox();
            return;
        }

        saveUndoState();
        page.rotationDegrees = newRotationDegrees;
        refreshWorkspace();
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

    private void updateProfileInfo(String selectedProfile) {
        if (selectedProfile == null || selectedProfile.isBlank()) {
            profileInfoTitleLabel.setText("No profile selected");
            profileInfoMetadataLabel.setText("Metadata required: ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â");
            profileInfoQaLabel.setText("QA required: ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â");
            profileInfoSplittingLabel.setText("Splitting method: ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â");
            return;
        }

        profileInfoTitleLabel.setText(selectedProfile);

        switch (selectedProfile) {
            case "Building Archive" -> {
                profileInfoMetadataLabel.setText("Metadata required: ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â");
                profileInfoQaLabel.setText("QA required: ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â");
                profileInfoSplittingLabel.setText("Splitting method: ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â");
            }
            case "Technical Drawings" -> {
                profileInfoMetadataLabel.setText("Metadata required: ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â");
                profileInfoQaLabel.setText("QA required: ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â");
                profileInfoSplittingLabel.setText("Splitting method: ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â");
            }
            case "Court Records" -> {
                profileInfoMetadataLabel.setText("Metadata required: ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â");
                profileInfoQaLabel.setText("QA required: ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â");
                profileInfoSplittingLabel.setText("Splitting method: ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â");
            }
            case "Standard Scan" -> {
                profileInfoMetadataLabel.setText("Metadata required: ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â");
                profileInfoQaLabel.setText("QA required: ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â");
                profileInfoSplittingLabel.setText("Splitting method: ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â");
            }
            default -> {
                profileInfoMetadataLabel.setText("Metadata required: ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â");
                profileInfoQaLabel.setText("QA required: ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â");
                profileInfoSplittingLabel.setText("Splitting method: ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â");
            }
        }
    }

    private void configureValidation() {
        startScanningButton.setDisable(true);

        profileComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
                updateStartScanningState()
        );

        boxIdTextField.textProperty().addListener((observable, oldValue, newValue) ->
                updateStartScanningState()
        );
    }

    private void updateStartScanningState() {
        boolean profileSelected = profileComboBox.getValue() != null
                && !profileComboBox.getValue().isBlank();

        boolean boxIdEntered = boxIdTextField.getText() != null
                && !boxIdTextField.getText().isBlank();

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

    private void configureReviewPreviewInteractions() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(reviewPreviewHost.widthProperty());
        clip.heightProperty().bind(reviewPreviewHost.heightProperty());
        reviewPreviewHost.setClip(clip);

        reviewPreviewHost.widthProperty().addListener((observable, oldValue, newValue) -> clampReviewPreviewTranslation());
        reviewPreviewHost.heightProperty().addListener((observable, oldValue, newValue) -> clampReviewPreviewTranslation());

        reviewZoomMultiplier.addListener((observable, oldValue, newValue) -> {
            updateReviewZoomLabel();
            clampReviewPreviewTranslation();
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

        double zoomDelta = deltaY > 0 ? PREVIEW_ZOOM_STEP : -PREVIEW_ZOOM_STEP;
        zoomPreviewBy(zoomDelta);

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

    public boolean handleGlobalShortcut(KeyEvent event) {
        if (event.isConsumed() || isTextInputShortcutTarget(event)) {
            return false;
        }

        return handlePrioritizedGlobalShortcut(event);
    }

    private boolean isTextInputShortcutTarget(KeyEvent event) {
        return event != null
                && (isTextInputTarget(event.getTarget()) || isTextInputTarget(focusedNode()));
    }

    private Node focusedNode() {
        Scene scene = null;
        if (scanWorkspaceView != null && scanWorkspaceView.getScene() != null) {
            scene = scanWorkspaceView.getScene();
        } else if (reviewWorkspaceView != null) {
            scene = reviewWorkspaceView.getScene();
        }
        return scene == null ? null : scene.getFocusOwner();
    }

    private boolean isTextInputTarget(Object target) {
        if (target instanceof TextInputControl) {
            return true;
        }

        if (!(target instanceof Node node)) {
            return false;
        }

        for (Node current = node; current != null; current = current.getParent()) {
            if (current instanceof TextInputControl) {
                return true;
            }
        }

        return false;
    }

    public boolean handlePrioritizedGlobalShortcut(KeyEvent event) {
        if (event.isConsumed()) {
            return false;
        }

        return runShortcut(event.getCode(), event.isShortcutDown(), event.getText());
    }

    public boolean handleSelectedPageArrowShortcut(KeyEvent event) {
        if (event.isConsumed() || !hasSelectedWorkspacePage()) {
            return false;
        }

        KeyCode code = event.getCode();
        if (code != KeyCode.LEFT && code != KeyCode.RIGHT) {
            return false;
        }

        return handlePrioritizedGlobalShortcut(event);
    }

    private boolean hasSelectedWorkspacePage() {
        return selectedPage != null
                && ((scanWorkspaceView != null && scanWorkspaceView.isVisible())
                || isReviewWorkspaceVisible());
    }

    public boolean runShortcut(KeyCode code, boolean shortcutDown, String typedText) {
        if (code == KeyCode.RIGHT) {
            if (isReviewWorkspaceVisible()) {
                onNextReviewPage();
            } else {
                onNextFile();
            }
            return true;
        } else if (code == KeyCode.LEFT) {
            if (isReviewWorkspaceVisible()) {
                onPreviousReviewPage();
            } else {
                onPreviousFile();
            }
            return true;
        } else if (shortcutDown && code == KeyCode.Z) {
            onUndoLastAction();
            return true;
        } else if (shortcutDown && code == KeyCode.S) {
            onSaveProgress();
            return true;
        } else if (shortcutDown && code == KeyCode.F) {
            showSearchOrJumpDialog();
            return true;
        } else if (shortcutDown && code == KeyCode.E) {
            navigator.showExports();
            return true;
        } else if (code == KeyCode.DELETE || code == KeyCode.BACK_SPACE) {
            onDeleteSelectedPage();
            return true;
        } else if (code == KeyCode.R) {
            onRotateShortcut();
            return true;
        } else if (isZoomInShortcut(code, typedText)) {
            onZoomIn();
            return true;
        } else if (isZoomOutShortcut(code, typedText)) {
            onZoomOut();
            return true;
        } else if (code == KeyCode.ESCAPE) {
            return closeVisibleModal();
        }

        return false;
    }

    public boolean runTypedShortcut(String character) {
        if ("+".equals(character)) {
            onZoomIn();
            return true;
        }

        if ("-".equals(character)) {
            onZoomOut();
            return true;
        }

        return false;
    }

    private boolean isZoomInShortcut(KeyCode code, String typedText) {
        return code == KeyCode.PLUS
                || code == KeyCode.ADD
                || code == KeyCode.EQUALS
                || "+".equals(typedText);
    }

    private boolean isZoomOutShortcut(KeyCode code, String typedText) {
        return code == KeyCode.MINUS
                || code == KeyCode.SUBTRACT
                || "-".equals(typedText);
    }

    private boolean closeVisibleModal() {
        if (finishReviewOverlay != null && finishReviewOverlay.isVisible()) {
            hideFinishReviewModal();
            return true;
        }

        if (submitConfirmationOverlay != null && submitConfirmationOverlay.isVisible()) {
            hideSubmitConfirmationModal();
            return true;
        }

        return false;
    }

    private boolean isReviewWorkspaceVisible() {
        return reviewWorkspaceView != null && reviewWorkspaceView.isVisible();
    }

    private void saveUndoState() {
        undoStack.push(new ScanSnapshot(
                allPages,
                selectedPage == null ? -1 : selectedPage.referenceId,
                nextReferenceId,
                nextFileId,
                collapsedDocuments,
                sessionRotationDegrees
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
        sessionRotationDegrees = snapshot.sessionRotationDegrees;
        syncBoxRotationComboBox();

        collapsedDocuments.clear();
        collapsedDocuments.addAll(snapshot.collapsedDocuments);

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

        beginScanSession();
        showWorkspaceView();
    }

    private void beginScanSession() {
        allPages.clear();
        pendingPages.clear();
        documents.clear();
        collapsedDocuments.clear();
        undoStack.clear();
        previewImageCache.clear();
        previewLoadFailures.clear();
        previewLoadsInProgress.clear();

        nextReferenceId = 1;
        nextFileId = 1;
        activeScanSession = scanManager.startSession(getBoxId(), getSelectedProfile());
        scanInProgress = false;

        selectedPage = null;
        resetPreviewViewState();
        hideFinishReviewModal();
        hideSubmitConfirmationModal();

        syncBoxRotationComboBox();
        refreshWorkspace();
        updateUndoButtonState();
    }

    @FXML
    private void onScanNextFile() {
        if (scanInProgress || activeScanSession == null) {
            return;
        }

        saveUndoState();
        scanInProgress = true;
        selectedFileTitleLabel.setText("Scanning next file");
        selectedFileRefLabel.setText("Please wait while we process your document.");
        refreshWorkspace();

        BackgroundExecutor.scan().execute(() -> {
            try {
                ScanImportResult result = scanManager.scanNextItem(activeScanSession);
                Platform.runLater(() -> applyScanImportResult(result));
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    scanInProgress = false;
                    selectedFileTitleLabel.setText("Scan failed");
                    selectedFileRefLabel.setText(exception.getMessage() == null || exception.getMessage().isBlank()
                            ? "Failed to import next file."
                            : exception.getMessage());
                    refreshWorkspace();
                });
            }
        });
    }

    private void applyScanImportResult(ScanImportResult result) {
        scanInProgress = false;

        if (result == null) {
            selectedFileTitleLabel.setText("Scan failed");
            selectedFileRefLabel.setText("No scan result returned.");
            refreshWorkspace();
            return;
        }

        if (result.getStatus() == ScanImportResult.Status.NO_MORE_FILES) {
            selectedFileTitleLabel.setText("No more files");
            selectedFileRefLabel.setText("The remote scanner queue is empty.");
            refreshWorkspace();
            return;
        }

        if (result.getStatus() == ScanImportResult.Status.FETCH_FAILED) {
            selectedFileTitleLabel.setText("Scan failed");
            selectedFileRefLabel.setText(result.getMessage().isBlank()
                    ? "Failed to import the next file."
                    : result.getMessage());
            refreshWorkspace();
            return;
        }

        for (PageImage pageImage : result.getScannedPages()) {
            ScannedPage scannedPage = mapImportedPage(pageImage);
            allPages.add(scannedPage);
            selectedPage = scannedPage;
        }

        rebuildDocumentsFromPages();
        refreshWorkspace();
        scrollDocumentTreeToLatest();

        if (result.getStatus() == ScanImportResult.Status.STOPPED_ON_BARCODE && !result.getMessage().isBlank()) {
            selectedFileRefLabel.setText(result.getMessage());
        }
    }

    private void scrollDocumentTreeToLatest() {
        if (documentTreeScrollPane == null) {
            return;
        }
        Platform.runLater(() -> {
            if (documentTreeContainer != null) {
                documentTreeContainer.applyCss();
                documentTreeContainer.layout();
            }
            documentTreeScrollPane.layout();
            Platform.runLater(() -> documentTreeScrollPane.setVvalue(1.0));
        });
    }

    private ScannedPage mapImportedPage(PageImage pageImage) {
        boolean barcode = pageImage.getPageType() == PageImage.PageType.BARCODE;
        ScannedPage page = new ScannedPage(
                Math.max(pageImage.getReferenceId(), nextReferenceId),
                nextFileId,
                barcode,
                false,
                pageImage.getSourceReference(),
                pageImage.getDisplayContent(),
                pageImage.getPreviewContent(),
                pageImage.getPreviewSourceBytes()
        );
        page.rotationDegrees = normalizeRotation(pageImage.getRotationDegrees() + sessionRotationDegrees);
        nextReferenceId = Math.max(nextReferenceId, page.referenceId + 1);
        nextFileId++;
        return page;
    }

    private ScannedPage mapStoredPage(PageImage pageImage) {
        boolean barcode = pageImage.getPageType() == PageImage.PageType.BARCODE;
        ScannedPage page = new ScannedPage(
                Math.max(pageImage.getReferenceId(), nextReferenceId),
                nextFileId,
                barcode,
                false,
                pageImage.getSourceReference(),
                pageImage.getDisplayContent(),
                pageImage.getPreviewContent(),
                pageImage.getPreviewSourceBytes()
        );
        page.rotationDegrees = normalizeRotation(pageImage.getRotationDegrees());
        nextReferenceId = Math.max(nextReferenceId, page.referenceId + 1);
        nextFileId++;
        return page;
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
                    DocumentGroup document = createDocument(documentNumber, "Barcode split", currentDocumentPages);
                    documents.add(document);
                    documentNumber++;
                    currentDocumentPages = new ArrayList<>();
                }

                currentDocumentPages.add(page);
                continue;
            }

            currentDocumentPages.add(page);

            if ("Finish batch".equals(page.splitReasonAfter)) {
                DocumentGroup document = createDocument(documentNumber, page.splitReasonAfter, currentDocumentPages);
                documents.add(document);
                documentNumber++;
                currentDocumentPages.clear();
            }
        }

        if (!currentDocumentPages.isEmpty() && currentDocumentPages.get(0).barcode) {
            DocumentGroup document = createDocument(documentNumber, null, currentDocumentPages);
            documents.add(document);
        } else {
            pendingPages.addAll(currentDocumentPages);
        }
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
            selectedPage = direction < 0
                    ? reviewPages.get(reviewPages.size() - 1)
                    : reviewPages.get(0);

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
        persistSelectedPreviewState();
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

        persistSelectedPreviewState();
    }

    private void updatePreviewZoomLabel() {
        if (previewZoomLabel != null) {
            previewZoomLabel.setText(Math.round(previewZoomMultiplier.get() * 100) + "%");
        }
    }

    private void updateReviewZoomLabel() {
        if (reviewZoomLabel != null) {
            reviewZoomLabel.setText(Math.round(reviewZoomMultiplier.get() * 100) + "%");
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

        double scaledWidth = getPreviewContentWidth() * scale;
        double scaledHeight = getPreviewContentHeight() * scale;

        double hostWidth = Math.max(1, previewHost.getWidth());
        double hostHeight = Math.max(1, previewHost.getHeight());

        double maxX = Math.abs(hostWidth - scaledWidth) / 2;
        double maxY = Math.abs(hostHeight - scaledHeight) / 2;

        previewTranslateX = clamp(previewTranslateX, -maxX, maxX);
        previewTranslateY = clamp(previewTranslateY, -maxY, maxY);

        currentPreviewWrapper.setTranslateX(previewTranslateX);
        currentPreviewWrapper.setTranslateY(previewTranslateY);
        persistSelectedPreviewState();
    }

    private double getPreviewContentWidth() {
        return rotatedBoundsWidth(PREVIEW_PAGE_WIDTH, PREVIEW_PAGE_HEIGHT, selectedPage == null ? 0 : selectedPage.rotationDegrees);
    }

    private double getPreviewContentHeight() {
        return rotatedBoundsHeight(PREVIEW_PAGE_WIDTH, PREVIEW_PAGE_HEIGHT, selectedPage == null ? 0 : selectedPage.rotationDegrees);
    }

    private double rotatedBoundsWidth(double width, double height, int rotationDegrees) {
        double radians = Math.toRadians(normalizeRotation(rotationDegrees));
        return Math.abs(width * Math.cos(radians)) + Math.abs(height * Math.sin(radians));
    }

    private double rotatedBoundsHeight(double width, double height, int rotationDegrees) {
        double radians = Math.toRadians(normalizeRotation(rotationDegrees));
        return Math.abs(width * Math.sin(radians)) + Math.abs(height * Math.cos(radians));
    }

    private void loadSelectedPreviewState() {
        if (selectedPage == null) {
            previewZoomMultiplier.set(1.0);
            previewTranslateX = 0;
            previewTranslateY = 0;
            updatePreviewZoomLabel();
            return;
        }

        previewZoomMultiplier.set(selectedPage.previewZoomMultiplier);
        previewTranslateX = selectedPage.previewTranslateX;
        previewTranslateY = selectedPage.previewTranslateY;
        updatePreviewZoomLabel();
    }

    private void persistSelectedPreviewState() {
        if (selectedPage == null) {
            return;
        }

        selectedPage.previewZoomMultiplier = previewZoomMultiplier.get();
        selectedPage.previewTranslateX = previewTranslateX;
        selectedPage.previewTranslateY = previewTranslateY;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    @FXML
    private void onReviewZoomIn() {
        zoomReviewPreviewBy(PREVIEW_ZOOM_STEP);
    }

    @FXML
    private void onReviewZoomOut() {
        zoomReviewPreviewBy(-PREVIEW_ZOOM_STEP);
    }

    private void zoomReviewPreviewBy(double zoomDelta) {
        setReviewPreviewZoom(reviewZoomMultiplier.get() + zoomDelta);
    }

    private void setReviewPreviewZoom(double zoom) {
        reviewZoomMultiplier.set(clamp(zoom, MIN_PREVIEW_ZOOM, MAX_PREVIEW_ZOOM));
        persistSelectedReviewPreviewState();
    }

    @FXML
    private void onResetReviewPreviewView() {
        resetReviewPreviewViewState();
        clampReviewPreviewTranslation();
    }

    private void resetReviewPreviewViewState() {
        reviewZoomMultiplier.set(1.0);
        reviewPreviewTranslateX = 0;
        reviewPreviewTranslateY = 0;

        if (currentReviewPreviewWrapper != null) {
            currentReviewPreviewWrapper.setTranslateX(0);
            currentReviewPreviewWrapper.setTranslateY(0);
        }

        persistSelectedReviewPreviewState();
        updateReviewZoomLabel();
    }

    @FXML
    private void onNudgeReviewPreviewUp() {
        nudgeReviewPreview(0, -PREVIEW_NUDGE_AMOUNT);
    }

    @FXML
    private void onNudgeReviewPreviewDown() {
        nudgeReviewPreview(0, PREVIEW_NUDGE_AMOUNT);
    }

    @FXML
    private void onNudgeReviewPreviewLeft() {
        nudgeReviewPreview(-PREVIEW_NUDGE_AMOUNT, 0);
    }

    @FXML
    private void onNudgeReviewPreviewRight() {
        nudgeReviewPreview(PREVIEW_NUDGE_AMOUNT, 0);
    }

    private void nudgeReviewPreview(double deltaX, double deltaY) {
        if (currentReviewPreviewWrapper == null) {
            return;
        }

        reviewPreviewTranslateX += deltaX;
        reviewPreviewTranslateY += deltaY;
        clampReviewPreviewTranslation();
    }

    private void clampReviewPreviewTranslation() {
        if (currentReviewPreviewWrapper == null || reviewPreviewHost == null) {
            return;
        }

        double scale = currentReviewPreviewWrapper.getScaleX();

        if (scale <= 0) {
            scale = 1;
        }

        double scaledWidth = PREVIEW_PAGE_WIDTH * scale;
        double scaledHeight = PREVIEW_PAGE_HEIGHT * scale;

        double hostWidth = Math.max(1, reviewPreviewHost.getWidth());
        double hostHeight = Math.max(1, reviewPreviewHost.getHeight());

        double maxX = Math.abs(hostWidth - scaledWidth) / 2;
        double maxY = Math.abs(hostHeight - scaledHeight) / 2;

        reviewPreviewTranslateX = clamp(reviewPreviewTranslateX, -maxX, maxX);
        reviewPreviewTranslateY = clamp(reviewPreviewTranslateY, -maxY, maxY);

        currentReviewPreviewWrapper.setTranslateX(reviewPreviewTranslateX);
        currentReviewPreviewWrapper.setTranslateY(reviewPreviewTranslateY);
        persistSelectedReviewPreviewState();
    }

    private void loadSelectedReviewPreviewState() {
        if (selectedPage == null) {
            reviewZoomMultiplier.set(1.0);
            reviewPreviewTranslateX = 0;
            reviewPreviewTranslateY = 0;
            updateReviewZoomLabel();
            return;
        }

        reviewZoomMultiplier.set(selectedPage.previewZoomMultiplier);
        reviewPreviewTranslateX = selectedPage.previewTranslateX;
        reviewPreviewTranslateY = selectedPage.previewTranslateY;
        updateReviewZoomLabel();
    }

    private void persistSelectedReviewPreviewState() {
        if (selectedPage == null) {
            return;
        }

        selectedPage.previewZoomMultiplier = reviewZoomMultiplier.get();
        selectedPage.previewTranslateX = reviewPreviewTranslateX;
        selectedPage.previewTranslateY = reviewPreviewTranslateY;
    }

    @FXML
    private void onRotateLeft() {
        ScannedPage page = resolveActiveNormalPage();
        if (page == null) {
            return;
        }

        saveUndoState();

        page.rotationDegrees = normalizeRotation(page.rotationDegrees - ROTATION_STEP_DEGREES);
        refreshWorkspace();
    }

    @FXML
    private void onRotateRight() {
        ScannedPage page = resolveActiveNormalPage();
        if (page == null) {
            return;
        }

        saveUndoState();

        page.rotationDegrees = normalizeRotation(page.rotationDegrees + ROTATION_STEP_DEGREES);
        refreshWorkspace();
    }

    private void onRotateShortcut() {
        ScannedPage page = resolveActiveNormalPage();
        if (page == null) {
            return;
        }

        saveUndoState();

        page.rotationDegrees = normalizeRotation(page.rotationDegrees + ROTATION_STEP_DEGREES);
        if (isReviewWorkspaceVisible()) {
            refreshReviewWorkspace();
        } else {
            refreshWorkspace();
        }
    }

    private void refreshVisibleWorkspace() {
        if (isReviewWorkspaceVisible()) {
            refreshReviewWorkspace();
        } else {
            refreshWorkspace();
        }
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
        // Document boundaries are created by barcode pages only.
    }

    @FXML
    private void onSaveProgress() {
        if (activeScanSession == null) {
            return;
        }

        portalModel.saveScanProgress(activeScanSession.getId(), createInMemoryScanProgress("Saved"));
        selectedFileRefLabel.setText("Progress saved to the database.");
        setWorkspaceSessionSubtitle(
                allPages.size() + " files scanned - "
                        + documents.size() + " documents created - progress saved"
        );
        refreshWorkspace();
    }

    @FXML
    private void onCreateMetadata() {
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

        ensureReviewSelection();
        refreshReviewWorkspace();
        showReviewWorkspaceView();
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
        if (finishReviewOverlay == null) {
            return;
        }

        finishReviewOverlay.setVisible(false);
        finishReviewOverlay.setManaged(false);
    }

        private void updateSubmitConfirmationModal() {
        submitConfirmationBoxIdLabel.setText(getBoxId());
        submitConfirmationProfileLabel.setText(getSelectedProfile());
        submitConfirmationSummaryLabel.setText(
                documents.size() + " documents - "
                        + getNormalPageCount() + " pages submitted for QA"
        );
    }

    private void showSubmitConfirmationModal() {
        submitConfirmationOverlay.setVisible(true);
        submitConfirmationOverlay.setManaged(true);
        submitConfirmationOverlay.toFront();
    }

    private void hideSubmitConfirmationModal() {
        if (submitConfirmationOverlay == null) {
            return;
        }

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
        hideFinishReviewModal();
        hideSubmitConfirmationModal();
        refreshWorkspace();
        showWorkspaceView();
    }

    @FXML
    private void onSubmitForQaFromReview() {
        markScanSubmittedForQa();
    }

    private void markScanSubmittedForQa() {
        hideFinishReviewModal();

        portalModel.submitScanForQa(
                activeScanSession == null ? null : activeScanSession.getId(),
                createInMemoryScanProgress("Submitted for QA")
        );

        setWorkspaceSessionSubtitle(
                allPages.size() + " files scanned - "
                        + documents.size() + " documents created - "
                        + getNormalPageCount() + " pages submitted for QA"
        );

        if (reviewDocumentsValueLabel != null) {
            reviewDocumentsValueLabel.setText(documents.size() + " - " + getNormalPageCount() + " pages");
        }

        updateSubmitConfirmationModal();
        showSubmitConfirmationModal();
    }

    private UserPortalModel.InMemoryScanProgress createInMemoryScanProgress(String status) {
        List<UserPortalModel.InMemoryScanPage> savedPages = allPages.stream()
                .map(this::toInMemoryScanPage)
                .toList();
        List<UserPortalModel.InMemoryScanDocument> savedDocuments = buildInMemoryScanDocuments();

        return new UserPortalModel.InMemoryScanProgress(
                getBoxId(),
                getSelectedProfile(),
                savedDocuments,
                savedPages,
                LocalDateTime.now(),
                status
        );
    }

    private List<UserPortalModel.InMemoryScanDocument> buildInMemoryScanDocuments() {
        List<UserPortalModel.InMemoryScanDocument> savedDocuments = new ArrayList<>();

        for (DocumentGroup document : documents) {
            savedDocuments.add(new UserPortalModel.InMemoryScanDocument(
                    document.number,
                    document.splitReason,
                    document.pages.stream().map(this::toInMemoryScanPage).toList(),
                    false
            ));
        }

        if (!pendingPages.isEmpty()) {
            savedDocuments.add(new UserPortalModel.InMemoryScanDocument(
                    documents.size() + 1,
                    "",
                    pendingPages.stream().map(this::toInMemoryScanPage).toList(),
                    true
            ));
        }

        return savedDocuments;
    }

    private UserPortalModel.InMemoryScanPage toInMemoryScanPage(ScannedPage page) {
        return new UserPortalModel.InMemoryScanPage(
                page.referenceId,
                page.fileId,
                page.documentNumber,
                page.barcode,
                page.rotationDegrees,
                page.needsRescan,
                page.splitReasonAfter,
                page.sourceReference,
                page.displayContent,
                page.previewContent
        );
    }

    @FXML
    private void onStartNextScanAfterSubmit() {
        resetAfterSubmittedScan();
        hideSubmitConfirmationModal();
        showSetupView();
    }

    private void resetAfterSubmittedScan() {
        if (activeScanSession != null) {
            portalModel.clearSavedScanProgress(activeScanSession.getId());
        }
        allPages.clear();
        pendingPages.clear();
        documents.clear();
        collapsedDocuments.clear();
        undoStack.clear();
        previewImageCache.clear();
        previewLoadFailures.clear();
        previewLoadsInProgress.clear();

        nextReferenceId = 1;
        nextFileId = 1;

        selectedPage = null;

        profileComboBox.getSelectionModel().clearSelection();
        boxIdTextField.clear();
        sessionRotationDegrees = 0;
        syncBoxRotationComboBox();

        profileInfoPanel.setVisible(false);
        profileInfoPanel.setManaged(false);
        updateProfileInfo(null);

        resetPreviewViewState();
        refreshWorkspace();
        updateStartScanningState();
        updateUndoButtonState();
    }

    private boolean hasNormalSelectedPage() {
        return selectedPage != null && !selectedPage.barcode;
    }

    private ScannedPage resolveActiveNormalPage() {
        if (hasNormalSelectedPage()) {
            return selectedPage;
        }

        for (ScannedPage page : allPages) {
            if (!page.barcode) {
                selectedPage = page;
                return page;
            }
        }

        return null;
    }

    @FXML
    private void onViewMyScans() {
        navigator.showMyScans();
    }

    @FXML
    private void onBackToScanSetup() {
        showSetupView();
    }

    private void showSetupView() {
        scanSetupView.setVisible(true);
        scanSetupView.setManaged(true);

        scanWorkspaceView.setVisible(false);
        scanWorkspaceView.setManaged(false);

        reviewWorkspaceView.setVisible(false);
        reviewWorkspaceView.setManaged(false);

        scanSetupView.toFront();
    }

    private void showWorkspaceView() {
        scanSetupView.setVisible(false);
        scanSetupView.setManaged(false);

        scanWorkspaceView.setVisible(true);
        scanWorkspaceView.setManaged(true);

        reviewWorkspaceView.setVisible(false);
        reviewWorkspaceView.setManaged(false);

        scanWorkspaceView.toFront();
    }

    private void showReviewWorkspaceView() {
        scanSetupView.setVisible(false);
        scanSetupView.setManaged(false);

        scanWorkspaceView.setVisible(false);
        scanWorkspaceView.setManaged(false);

        reviewWorkspaceView.setVisible(true);
        reviewWorkspaceView.setManaged(true);

        reviewWorkspaceView.toFront();
    }

    private void refreshWorkspace() {
        loadSelectedPreviewState();
        updateWorkspaceHeader();
        refreshHeaderInfoChips();
        updateRotationButtons();
        syncPageRotationComboBox();
        renderDocumentTree();
        renderPageTray();
        renderPreview();
        updateUndoButtonState();
    }

    private void updateRotationButtons() {
        if (rotateLeftButton == null || rotateRightButton == null) {
            return;
        }

        rotateLeftButton.setText("Rotate Left (1\u00B0)");
        rotateRightButton.setText("Rotate Right (1\u00B0)");
    }

    private void syncBoxRotationComboBox() {
        if (boxRotationComboBox != null) {
            syncingBoxRotationComboBox = true;
            boxRotationComboBox.setValue(formatRotationDegrees(sessionRotationDegrees));
            syncingBoxRotationComboBox = false;
        }
    }

    private void syncPageRotationComboBox() {
        if (pageRotationComboBox == null) {
            return;
        }

        syncingPageRotationComboBox = true;
        ScannedPage page = hasNormalSelectedPage() ? selectedPage : null;
        String rotationValue = formatRotationDegrees(page == null ? 0 : page.rotationDegrees);
        pageRotationComboBox.setValue(rotationValue);
        if (pageRotationComboBox.getEditor() != null) {
            pageRotationComboBox.getEditor().setText(rotationValue);
        }
        syncingPageRotationComboBox = false;
    }

    private String formatRotationDegrees(int rotationDegrees) {
        return normalizeRotation(rotationDegrees) + "°";
    }

    private int parseRotationDegrees(String value) {
        if (value == null) {
            return 0;
        }

        String normalizedValue = value
                .replace("Â°", "")
                .replace("°", "")
                .trim();
        if (!normalizedValue.isBlank()) {
            try {
                return normalizeRotation(Integer.parseInt(normalizedValue));
            } catch (NumberFormatException ignored) {
            }
        }

        String digitsOnly = value.replaceAll("[^0-9]", "");
        if (digitsOnly.isBlank()) {
            return 0;
        }

        try {
            return normalizeRotation(Integer.parseInt(digitsOnly));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void refreshHeaderInfoChips() {
        if (headerFilesInfoLabel != null) {
            headerFilesInfoLabel.setText("Scanned Files: " + allPages.size());
        }
        if (headerDocumentsInfoLabel != null) {
            headerDocumentsInfoLabel.setText("Documents: " + documents.size());
        }
        if (headerReferenceInfoLabel != null) {
            headerReferenceInfoLabel.setText(
                    selectedPage == null ? "Ref: \u2014" : "Ref: " + selectedPage.referenceIdLabel()
            );
        }
    }

    private void updateWorkspaceHeader() {
        String boxId = getBoxId();
        String profile = getSelectedProfile();

        workspaceSessionTitleLabel.setText("Scanning Session \u00B7 " + boxId);
        setWorkspaceSessionSubtitle(
                allPages.size() + " files scanned \u00B7 "
                        + documents.size() + " documents \u00B7 "
                        + pendingPages.size() + " pending pages"
        );

        boxStructureSubtitleLabel.setText(profile + " \u00B7 " + boxId);
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
            selectedInfo += " \u00B7 Document " + selectedPage.documentNumber;

            int pageNumber = getPageNumberInDocument(selectedPage);

            if (pageNumber > 0) {
                selectedInfo += " \u00B7 Page " + pageNumber;
            }
        } else if (selectedPage.barcode) {
            selectedInfo += " \u00B7 Barcode split marker";
        } else {
            selectedInfo += " \u00B7 Pending document";
        }

        if (selectedPage.needsRescan) {
            selectedInfo += " \u00B7 Needs rescan";
        }

        if (selectedPage.rotationDegrees != 0) {
            selectedInfo += " \u00B7 Rotated " + selectedPage.rotationDegrees + "\u00B0";
        }

        selectedFileRefLabel.setText(selectedInfo);
    }

    private void setWorkspaceSessionSubtitle(String text) {
        if (workspaceSessionSubtitleLabel != null) {
            workspaceSessionSubtitleLabel.setText(text);
        }
        if (workspaceSessionInlineSubtitleLabel != null) {
            workspaceSessionInlineSubtitleLabel.setText(text);
        }
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
        String profile = profileComboBox.getValue();
        return profile == null || profile.isBlank() ? "No profile" : profile;
    }

    private String getBoxId() {
        String boxId = boxIdTextField.getText();
        return boxId == null || boxId.isBlank() ? "No box ID" : boxId.trim();
    }

    private void renderDocumentTree() {
        documentTreeContainer.getChildren().clear();

        if (documents.isEmpty() && pendingPages.isEmpty()) {
            VBox emptyState = new VBox(6);
            emptyState.getStyleClass().add("document-tree-empty-state");

            Label title = new Label("No documents created yet");
            title.getStyleClass().add("document-tree-empty-title");

            Label copy = new Label("Scanned files appear directly inside the current document as pages are scanned. Documents close after barcode detection, manual split, or finishing the batch.");
            copy.setWrapText(true);
            copy.getStyleClass().add("document-tree-empty-copy");

            emptyState.getChildren().addAll(title, copy);
            documentTreeContainer.getChildren().add(emptyState);
            return;
        }

        for (int index = 0; index < documents.size(); index++) {
            DocumentGroup document = documents.get(index);

            VBox documentBlock = new VBox(12);
            documentBlock.setAlignment(Pos.TOP_LEFT);
            documentBlock.getStyleClass().add("document-tree-document-block");
            if (documentTreeListView) {
                documentBlock.getStyleClass().add("document-tree-list-block");
            }

            HBox documentHeader = createDocumentHeader(document);
            documentHeader.getStyleClass().add("document-tree-document-header-framed");
            if (documentTreeListView) {
                documentHeader.getStyleClass().add("document-tree-list-header");
            }
            documentBlock.getChildren().add(documentHeader);

            if (!collapsedDocuments.contains(document.number)) {
                VBox pageStack = new VBox(documentTreeListView ? 0 : 18);
                pageStack.setAlignment(documentTreeListView ? Pos.TOP_LEFT : Pos.TOP_CENTER);
                pageStack.getStyleClass().add("document-tree-page-stack");
                if (documentTreeListView) {
                    pageStack.getStyleClass().add("document-tree-list-page-stack");
                }
                for (int pageIndex = 0; pageIndex < document.pages.size(); pageIndex++) {
                    ScannedPage page = document.pages.get(pageIndex);
                    Node pageNode = documentTreeListView
                            ? createDocumentTreePageRow(page, pageIndex + 1)
                            : createDocumentTreePageCard(page, pageIndex + 1);
                    pageStack.getChildren().add(pageNode);
                }
                documentBlock.getChildren().add(pageStack);
            }

            documentTreeContainer.getChildren().add(documentBlock);

            if (shouldShowTreeSplitRow(document, index)) {
                documentTreeContainer.getChildren().add(createDocumentTreeSplitRow(document.splitReason));
            }
        }

        if (!pendingPages.isEmpty()) {
            int pendingDocumentNumber = documents.size() + 1;
            VBox pendingBlock = new VBox(12);
            pendingBlock.setAlignment(Pos.TOP_LEFT);
            pendingBlock.getStyleClass().add("document-tree-document-block");
            if (documentTreeListView) {
                pendingBlock.getStyleClass().add("document-tree-list-block");
            }

            HBox pendingHeader = createPendingDocumentHeader(pendingDocumentNumber, pendingPages);
            pendingHeader.getStyleClass().add("document-tree-document-header-framed");
            if (documentTreeListView) {
                pendingHeader.getStyleClass().add("document-tree-list-header");
            }
            pendingBlock.getChildren().add(pendingHeader);

            if (!collapsedDocuments.contains(pendingDocumentNumber)) {
                VBox pageStack = new VBox(documentTreeListView ? 0 : 18);
                pageStack.setAlignment(documentTreeListView ? Pos.TOP_LEFT : Pos.TOP_CENTER);
                pageStack.getStyleClass().add("document-tree-page-stack");
                if (documentTreeListView) {
                    pageStack.getStyleClass().add("document-tree-list-page-stack");
                }
                for (int pageIndex = 0; pageIndex < pendingPages.size(); pageIndex++) {
                    ScannedPage page = pendingPages.get(pageIndex);
                    Node pageNode = documentTreeListView
                            ? createDocumentTreePageRow(page, pageIndex + 1)
                            : createDocumentTreePageCard(page, pageIndex + 1);
                    pageStack.getChildren().add(pageNode);
                }
                pendingBlock.getChildren().add(pageStack);
            }

            documentTreeContainer.getChildren().add(pendingBlock);
        } else if (shouldShowNextPendingDocumentPlaceholder()) {
            int pendingDocumentNumber = documents.size() + 1;
            VBox pendingBlock = new VBox(12);
            pendingBlock.setAlignment(Pos.TOP_LEFT);
            pendingBlock.getStyleClass().add("document-tree-document-block");
            if (documentTreeListView) {
                pendingBlock.getStyleClass().add("document-tree-list-block");
            }

            HBox pendingHeader = createPendingDocumentHeader(pendingDocumentNumber, List.of());
            pendingHeader.getStyleClass().add("document-tree-document-header-framed");
            if (documentTreeListView) {
                pendingHeader.getStyleClass().add("document-tree-list-header");
            }
            pendingBlock.getChildren().add(pendingHeader);

            if (!collapsedDocuments.contains(pendingDocumentNumber)) {
                Label waitingLabel = new Label("Waiting for the next scanned page");
                waitingLabel.getStyleClass().add("document-tree-empty-copy");
                waitingLabel.setWrapText(true);
                waitingLabel.setMaxWidth(180);
                pendingBlock.getChildren().add(waitingLabel);
            }

            documentTreeContainer.getChildren().add(pendingBlock);
        }
    }

    private boolean shouldShowNextPendingDocumentPlaceholder() {
        return pendingPages.isEmpty()
                && !allPages.isEmpty()
                && allPages.get(allPages.size() - 1).barcode;
    }

    private HBox createDocumentHeader(DocumentGroup document) {
        HBox documentHeader = new HBox(9);
        documentHeader.setAlignment(Pos.CENTER_LEFT);
        documentHeader.getStyleClass().add("document-tree-document-header");

        Region chevron = new Region();
        chevron.getStyleClass().add("document-tree-chevron-icon");
        chevron.setRotate(collapsedDocuments.contains(document.number) ? 0 : 90);

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

        configureDocumentDropTarget(documentHeader, document.number);

        return documentHeader;
    }

    private HBox createPendingDocumentHeader(int documentNumber, List<ScannedPage> pages) {
        HBox documentHeader = new HBox(9);
        documentHeader.setAlignment(Pos.CENTER_LEFT);
        documentHeader.getStyleClass().add("document-tree-document-header");

        Region chevron = new Region();
        chevron.getStyleClass().add("document-tree-chevron-icon");
        chevron.setRotate(collapsedDocuments.contains(documentNumber) ? 0 : 90);

        Label documentName = new Label("Document " + documentNumber);
        documentName.getStyleClass().add("document-tree-document-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean hasRescanPages = pages.stream().anyMatch(page -> page.needsRescan);
        Label warning = new Label(hasRescanPages ? "!" : "");
        warning.getStyleClass().add("document-tree-warning");

        Label pageCount = new Label(pages.size() + " pages");
        pageCount.getStyleClass().add("document-tree-count");

        documentHeader.getChildren().addAll(chevron, documentName, spacer, warning, pageCount);

        documentHeader.setOnMouseClicked(event -> {
            if (collapsedDocuments.contains(documentNumber)) {
                collapsedDocuments.remove(documentNumber);
            } else {
                collapsedDocuments.add(documentNumber);
            }

            renderDocumentTree();
        });

        configureDocumentDropTarget(documentHeader, documentNumber);

        return documentHeader;
    }

    private void configureDocumentDropTarget(Node node, int targetDocumentNumber) {
        node.setOnDragOver(event -> {
            Dragboard dragboard = event.getDragboard();

            if (dragboard.hasString()) {
                ScannedPage draggedPage = findPageByReferenceId(dragboard.getString());

                if (draggedPage != null && !draggedPage.barcode) {
                    event.acceptTransferModes(TransferMode.MOVE);
                    node.getStyleClass().add("document-tree-drop-target");
                }
            }

            event.consume();
        });

        node.setOnDragExited(event -> node.getStyleClass().remove("document-tree-drop-target"));

        node.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;

            if (dragboard.hasString()) {
                ScannedPage draggedPage = findPageByReferenceId(dragboard.getString());

                if (draggedPage != null && !draggedPage.barcode) {
                    movePageToDocumentEnd(draggedPage, targetDocumentNumber);
                    success = true;
                }
            }

            node.getStyleClass().remove("document-tree-drop-target");
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private boolean shouldShowTreeSplitRow(DocumentGroup document, int documentIndex) {
        boolean isLastDocument = documentIndex == documents.size() - 1;

        if ("Finish batch".equals(document.splitReason) && isLastDocument) {
            return false;
        }

        return "Barcode split".equals(document.splitReason);
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
        if (documentTreeListView) {
            row.getStyleClass().add("document-tree-list-page-row");
        }

        if (page == selectedPage) {
            row.getStyleClass().add("document-tree-page-selected");
        }

        HBox labelRow = createScanPageLabelRow(page, "Page " + pageNumberInDocument, false);

        row.getChildren().add(labelRow);
        row.setOnMouseClicked(event -> {
            selectedPage = page;
            refreshWorkspace();
        });

        configurePageDrag(row, page);

        return row;
    }

    private VBox createDocumentTreePageCard(ScannedPage page, int pageNumberInDocument) {
        VBox card = createEmbeddedPageCard(page, "Page " + pageNumberInDocument);
        card.getStyleClass().add("document-embedded-page-card");
        card.setOnMouseClicked(event -> {
            selectedPage = page;
            refreshWorkspace();
        });
        configurePageDrag(card, page);
        return card;
    }

    private void movePageToDocumentEnd(ScannedPage page, int targetDocumentNumber) {
        if (page == null || page.barcode) {
            return;
        }
        movePageBetweenDocuments(page, targetDocumentNumber, null);
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

        if (scanInProgress) {
            currentPreviewWrapper = null;
            previewHost.getChildren().add(createScanningPreview());
            return;
        }

        if (selectedPage == null) {
            currentPreviewWrapper = null;
            previewHost.getChildren().add(createEmptyPreview());
            return;
        }

        Node previewNode = selectedPage.barcode
                ? createBarcodePreview(selectedPage)
                : createDocumentPreview(selectedPage);

        previewHost.getChildren().add(wrapPreviewWithAutoScale(previewNode));
    }

    private Node wrapPreviewWithAutoScale(Node previewNode) {
        StackPane previewWrapper = new StackPane(previewNode);
        previewWrapper.setAlignment(Pos.CENTER);
        previewWrapper.setPickOnBounds(true);
        double previewWidth = getPreviewContentWidth();
        double previewHeight = getPreviewContentHeight();
        previewWrapper.setMinWidth(previewWidth);
        previewWrapper.setPrefWidth(previewWidth);
        previewWrapper.setMaxWidth(previewWidth);
        previewWrapper.setMinHeight(previewHeight);
        previewWrapper.setPrefHeight(previewHeight);
        previewWrapper.setMaxHeight(previewHeight);

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

        Label copy = new Label("Click \"Scan Next File\".");
        copy.getStyleClass().add("scan-preview-empty-copy");

        emptyPreview.getChildren().addAll(title, copy);

        return emptyPreview;
    }

    private Node createScanningPreview() {
        VBox scanningPreview = new VBox(20);
        scanningPreview.setAlignment(Pos.CENTER);
        scanningPreview.getStyleClass().add("scan-preview-loading");

        ProgressIndicator indicator = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
        indicator.setMaxSize(160, 160);
        indicator.setPrefSize(160, 160);
        indicator.getStyleClass().add("scan-progress-indicator");

        Label progressCopy = new Label("Scanning");
        progressCopy.getStyleClass().add("scan-progress-indicator-copy");

        StackPane indicatorShell = new StackPane(indicator, progressCopy);
        indicatorShell.getStyleClass().add("scan-progress-indicator-shell");

        Label title = new Label("Scanning in Progress");
        title.getStyleClass().add("scan-preview-loading-title");

        scanningPreview.getChildren().addAll(indicatorShell, title);
        return scanningPreview;
    }

    private Node createBarcodePreview(ScannedPage page) {
        Node actualPreview = createActualDocumentPreview(page);
        if (actualPreview != null) {
            return actualPreview;
        }

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
        Node actualPreview = createActualDocumentPreview(page);
        if (actualPreview != null) {
            return actualPreview;
        }

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

        VBox topLeft = new VBox(9);
        topLeft.getChildren().addAll(
                createLine("mock-line-dark", 180, 15),
                createLine("mock-line-medium", 126, 9)
        );

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        VBox topRight = new VBox(6);
        topRight.setAlignment(Pos.TOP_RIGHT);
        topRight.getChildren().addAll(
                createLine("mock-line-medium", 90, 9),
                createLine("mock-line-medium", 108, 9)
        );

        topSection.getChildren().addAll(topLeft, topSpacer, topRight);

        VBox textLines = new VBox(6);
        textLines.getChildren().addAll(
                createLine("mock-line-light", 405, 7),
                createLine("mock-line-light", 405, 7),
                createLine("mock-line-light", 372, 7),
                createLine("mock-line-light", 405, 7),
                createLine("mock-line-light", 318, 7)
        );

        VBox formArea = new VBox(9);
        formArea.getStyleClass().add("mock-form-area");

        HBox formHeading = new HBox(18);
        formHeading.getChildren().addAll(
                createLine("mock-line-dark", 144, 12),
                createLine("mock-line-medium", 78, 8)
        );

        HBox formInputs = new HBox(12);
        formInputs.getChildren().addAll(
                createInputSkeleton(),
                createInputSkeleton()
        );

        Region wideInput = createInputSkeleton();
        wideInput.setMaxWidth(Double.MAX_VALUE);

        formArea.getChildren().addAll(formHeading, formInputs, wideInput);

        VBox bottomText = new VBox(6);
        bottomText.getChildren().addAll(
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

    private Node createActualDocumentPreview(ScannedPage page) {
        if (page == null || (page.displayContent.isBlank() && page.previewSourceBytes.length == 0)) {
            return null;
        }

        String failureMessage = previewLoadFailures.get(page.referenceId);
        if (failureMessage != null) {
            return createPreviewUnavailableState(failureMessage);
        }

        Image image = previewImageCache.get(page.referenceId);
        if (image == null) {
            ensurePreviewImageLoaded(page);
            return createPreviewLoadingState();
        }

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(PREVIEW_PAGE_WIDTH);
        imageView.setFitHeight(PREVIEW_PAGE_HEIGHT);
        imageView.setRotate(page.rotationDegrees);
        imageView.setSmooth(true);

        StackPane preview = new StackPane(imageView);
        preview.setAlignment(Pos.CENTER);
        preview.getStyleClass().add("mock-document-page");
        preview.setMinWidth(PREVIEW_PAGE_WIDTH);
        preview.setPrefWidth(PREVIEW_PAGE_WIDTH);
        preview.setMaxWidth(PREVIEW_PAGE_WIDTH);
        preview.setMinHeight(PREVIEW_PAGE_HEIGHT);
        preview.setPrefHeight(PREVIEW_PAGE_HEIGHT);
        preview.setMaxHeight(PREVIEW_PAGE_HEIGHT);

        if (page.needsRescan) {
            Label warning = new Label("Marked for rescan");
            warning.getStyleClass().add("preview-warning-banner");
            StackPane.setAlignment(warning, Pos.TOP_CENTER);
            preview.getChildren().add(warning);
        }

        if (page.barcode) {
            Label barcodeLabel = new Label("Barcode Page");
            barcodeLabel.getStyleClass().add("barcode-preview-badge");
            StackPane.setAlignment(barcodeLabel, Pos.TOP_LEFT);
            preview.getChildren().add(barcodeLabel);
        }

        return preview;
    }

    private Node createPreviewLoadingState() {
        VBox loading = new VBox(9);
        loading.setAlignment(Pos.CENTER);
        loading.getStyleClass().add("mock-document-page");
        loading.setMinWidth(PREVIEW_PAGE_WIDTH);
        loading.setPrefWidth(PREVIEW_PAGE_WIDTH);
        loading.setMaxWidth(PREVIEW_PAGE_WIDTH);
        loading.setMinHeight(PREVIEW_PAGE_HEIGHT);
        loading.setPrefHeight(PREVIEW_PAGE_HEIGHT);
        loading.setMaxHeight(PREVIEW_PAGE_HEIGHT);

        Label title = new Label("Loading TIFF preview");
        title.getStyleClass().add("scan-preview-empty-title");

        Label copy = new Label("The scanned page is being decoded from the API response.");
        copy.getStyleClass().add("scan-preview-empty-copy");
        copy.setWrapText(true);

        loading.getChildren().addAll(title, copy);
        return loading;
    }

    private Node createPreviewUnavailableState(String message) {
        VBox unavailable = new VBox(9);
        unavailable.setAlignment(Pos.CENTER);
        unavailable.getStyleClass().add("mock-document-page");
        unavailable.setMinWidth(PREVIEW_PAGE_WIDTH);
        unavailable.setPrefWidth(PREVIEW_PAGE_WIDTH);
        unavailable.setMaxWidth(PREVIEW_PAGE_WIDTH);
        unavailable.setMinHeight(PREVIEW_PAGE_HEIGHT);
        unavailable.setPrefHeight(PREVIEW_PAGE_HEIGHT);
        unavailable.setMaxHeight(PREVIEW_PAGE_HEIGHT);

        Label title = new Label("TIFF preview unavailable");
        title.getStyleClass().add("scan-preview-empty-title");

        Label copy = new Label(message);
        copy.getStyleClass().add("scan-preview-empty-copy");
        copy.setWrapText(true);
        copy.setMaxWidth(360);

        unavailable.getChildren().addAll(title, copy);
        return unavailable;
    }

    private ImageView createPreviewImageView(Image image, double fitWidth, double fitHeight) {
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);
        imageView.setFitWidth(fitWidth);
        imageView.setFitHeight(fitHeight);
        return imageView;
    }

    private ImageView createThumbnailImageView(Image image, double fitWidth, double fitHeight) {
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);
        imageView.setFitWidth(fitWidth);
        imageView.setFitHeight(fitHeight);
        return imageView;
    }

    private Node createRotatedThumbnailNode(ScannedPage page, double fitWidth, double fitHeight) {
        Image image = resolvePageImage(page);
        if (image == null) {
            return null;
        }

        double rotationDegrees = page == null ? 0 : page.rotationDegrees;
        double rotatedWidth = rotatedBoundsWidth(fitWidth, fitHeight, (int) rotationDegrees);
        double rotatedHeight = rotatedBoundsHeight(fitWidth, fitHeight, (int) rotationDegrees);
        double fitScale = Math.min(
                fitWidth / Math.max(1, rotatedWidth),
                fitHeight / Math.max(1, rotatedHeight)
        );

        ImageView imageView = createThumbnailImageView(image, fitWidth * fitScale, fitHeight * fitScale);
        imageView.setRotate(page.rotationDegrees);
        return imageView;
    }

    private void configureThumbnailFrame(StackPane thumbnail, ScannedPage page, double width, double height) {
        boolean quarterTurn = isQuarterTurnRotation(page.rotationDegrees);
        double frameWidth = quarterTurn ? height : width;
        double frameHeight = quarterTurn ? width : height;

        thumbnail.setMinWidth(frameWidth);
        thumbnail.setPrefWidth(frameWidth);
        thumbnail.setMaxWidth(frameWidth);
        thumbnail.setMinHeight(frameHeight);
        thumbnail.setPrefHeight(frameHeight);
        thumbnail.setMaxHeight(frameHeight);
        thumbnail.setStyle(String.format(
                Locale.US,
                "-fx-min-width: %.0f; -fx-pref-width: %.0f; -fx-max-width: %.0f; "
                        + "-fx-min-height: %.0f; -fx-pref-height: %.0f; -fx-max-height: %.0f;",
                frameWidth,
                frameWidth,
                frameWidth,
                frameHeight,
                frameHeight,
                frameHeight
        ));
    }

    private void configureEmbeddedPageCardFrame(VBox card, ScannedPage page, double cardWidth, double thumbnailWidth, double thumbnailHeight) {
        double adjustedWidth = isQuarterTurnRotation(page.rotationDegrees)
                ? cardWidth + (thumbnailHeight - thumbnailWidth)
                : cardWidth;

        card.setMinWidth(adjustedWidth);
        card.setPrefWidth(adjustedWidth);
        card.setMaxWidth(adjustedWidth);
        card.setStyle(String.format(
                Locale.US,
                "-fx-min-width: %.0f; -fx-pref-width: %.0f; -fx-max-width: %.0f;",
                adjustedWidth,
                adjustedWidth,
                adjustedWidth
        ));
    }

    private boolean isQuarterTurnRotation(int rotationDegrees) {
        int normalizedRotation = normalizeRotation(rotationDegrees);
        return normalizedRotation == 90 || normalizedRotation == 270;
    }

    private void applyThumbnailClip(StackPane thumbnail, double arcSize) {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(thumbnail.widthProperty());
        clip.heightProperty().bind(thumbnail.heightProperty());
        clip.setArcWidth(arcSize);
        clip.setArcHeight(arcSize);
        thumbnail.setClip(clip);
    }

    private Image resolvePageImage(ScannedPage page) {
        if (page == null) {
            return null;
        }
        return previewImageCache.get(page.referenceId);
    }

    private void ensurePreviewImageLoaded(ScannedPage page) {
        if (page == null || (page.displayContent.isBlank() && page.previewSourceBytes.length == 0)) {
            return;
        }
        if (previewImageCache.containsKey(page.referenceId) || previewLoadsInProgress.contains(page.referenceId)) {
            return;
        }

        previewLoadsInProgress.add(page.referenceId);
        int referenceId = page.referenceId;
        String displayContent = page.displayContent;
        byte[] previewSourceBytes = Arrays.copyOf(page.previewSourceBytes, page.previewSourceBytes.length);

        BackgroundExecutor.preview().execute(() -> {
            PreviewDecodeResult result = decodePreviewImage(previewSourceBytes, displayContent);
            Platform.runLater(() -> {
                previewLoadsInProgress.remove(referenceId);
                if (result.image() != null) {
                    previewImageCache.put(referenceId, result.image());
                    previewLoadFailures.remove(referenceId);
                } else {
                    previewLoadFailures.put(referenceId, result.message());
                }

                if (selectedPage != null && selectedPage.referenceId == referenceId) {
                    if (reviewWorkspaceView.isVisible()) {
                        renderReviewPreview();
                    } else {
                        renderPreview();
                    }
                }
            });
        });
    }

    private PreviewDecodeResult decodePreviewImage(byte[] previewSourceBytes, String displayContent) {
        if (previewSourceBytes.length > 0) {
            Image directPreview = loadStandardPreviewImage(previewSourceBytes);
            if (directPreview != null) {
                return PreviewDecodeResult.success(directPreview);
            }

            PreviewDecodeResult decodedTiff = decodeTiffPreviewImage(previewSourceBytes);
            if (decodedTiff.image() != null) {
                return decodedTiff;
            }
        }

        byte[] bytes = extractDisplayContentBytes(displayContent);
        if (bytes.length == 0) {
            return PreviewDecodeResult.failed("The API response did not contain previewable TIFF data.");
        }

        Image directImage = loadStandardPreviewImage(bytes);
        if (directImage != null) {
            return PreviewDecodeResult.success(directImage);
        }

        return decodeTiffPreviewImage(bytes);
    }

    private PreviewDecodeResult decodeTiffPreviewImage(byte[] bytes) {
        BufferedImage bufferedImage = TiffImageSupport.readFirstFrame(
                bytes,
                (int) PREVIEW_PAGE_WIDTH,
                (int) PREVIEW_PAGE_HEIGHT
        );
        if (bufferedImage == null) {
            return PreviewDecodeResult.failed("The TIFF preview could not be decoded.");
        }

        Image convertedImage = SwingFXUtils.toFXImage(bufferedImage, null);
        if (convertedImage == null) {
            return PreviewDecodeResult.failed("The TIFF preview image could not be loaded.");
        }

        return PreviewDecodeResult.success(convertedImage);
    }

    private Image loadStandardPreviewImage(byte[] bytes) {
        try {
            Image image = new Image(new ByteArrayInputStream(bytes), 0, 0, true, true);
            if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return null;
            }
            return image;
        } catch (Exception exception) {
            return null;
        }
    }

    private byte[] extractDisplayContentBytes(String displayContent) {
        if (displayContent == null || displayContent.isBlank()) {
            return new byte[0];
        }

        int commaIndex = displayContent.indexOf(',');
        if (commaIndex < 0 || commaIndex >= displayContent.length() - 1) {
            return new byte[0];
        }

        try {
            return Base64.getDecoder().decode(displayContent.substring(commaIndex + 1));
        } catch (IllegalArgumentException exception) {
            return new byte[0];
        }
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
            VBox pageCard = createTrayPageCard(page);
            pageTrayContainer.getChildren().add(pageCard);

            if (page.barcode) {
                pageTrayContainer.getChildren().add(createTraySplitMarker());
            }
        }
    }

    private VBox createTrayPageCard(ScannedPage page) {
        VBox card = new VBox(3);
        card.setAlignment(Pos.CENTER);

        if (page.barcode) {
            card.getStyleClass().add("page-tray-barcode-split-card");
        } else {
            card.getStyleClass().add("page-tray-item");
        }

        if (page == selectedPage) {
            card.getStyleClass().add(page.barcode
                    ? "page-tray-barcode-split-card-selected"
                    : "page-tray-item-selected"
            );
        }

        if (page.needsRescan) {
            card.getStyleClass().add("page-tray-item-warning");
        }

        StackPane thumbnail = new StackPane();
        thumbnail.getStyleClass().add("page-tray-thumbnail");
        configureThumbnailFrame(thumbnail, page, 72, 66);

        if (page.barcode) {
            Label barcode = new Label("||||");
            barcode.getStyleClass().add("page-tray-barcode-mark");
            thumbnail.getChildren().add(barcode);
        } else {
            Node imageNode = createRotatedThumbnailNode(page, 148, 214);
            if (imageNode != null) {
                thumbnail.getChildren().add(imageNode);
            } else {
                VBox lines = new VBox(3);
                lines.setAlignment(Pos.TOP_LEFT);
                lines.getChildren().addAll(
                        createLine("tray-line-dark", 27, 3),
                        createLine("tray-line-light", 42, 3),
                        createLine("tray-line-light", 36, 3),
                        createLine("tray-line-light", 30, 3)
                );
                thumbnail.getChildren().add(lines);
            }
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

        return "";
    }

    private Node createTraySplitMarker() {
        StackPane splitMarker = new StackPane();
        splitMarker.getStyleClass().add("page-tray-document-split");

        Region divider = new Region();
        divider.getStyleClass().add("page-tray-document-split-line");

        splitMarker.getChildren().add(divider);

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

        int referenceId;

        try {
            referenceId = Integer.parseInt(referenceIdText);
        } catch (NumberFormatException exception) {
            return null;
        }

        return findPageByReferenceId(referenceId);
    }

    private ScannedPage findPageByReferenceId(int referenceId) {
        for (ScannedPage page : allPages) {
            if (page.referenceId == referenceId) {
                return page;
            }
        }

        return null;
    }

    private void showSearchOrJumpDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Search or jump");
        dialog.setHeaderText("Search scanned pages");
        dialog.setContentText("Enter a reference, file name, or document number:");

        dialog.showAndWait().ifPresent(value -> {
            ScannedPage page = findPageBySearchText(value);

            if (page != null) {
                selectedPage = page;
                refreshVisibleWorkspace();
            }
        });
    }

    private ScannedPage findPageBySearchText(String searchText) {
        String query = normalizeSearchText(searchText);

        if (query.isBlank()) {
            return null;
        }

        ScannedPage pageByReference = findPageByReferenceId(query);

        if (pageByReference != null) {
            return pageByReference;
        }

        for (ScannedPage page : allPages) {
            if (normalizeSearchText(page.referenceIdLabel()).contains(query)
                    || normalizeSearchText(page.fileName()).contains(query)
                    || normalizeSearchText(page.sourceReference).contains(query)
                    || normalizeSearchText("document " + page.documentNumber).contains(query)
                    || normalizeSearchText("doc " + page.documentNumber).contains(query)) {
                return page;
            }
        }

        return null;
    }

    private String normalizeSearchText(String text) {
        if (text == null) {
            return "";
        }

        return text.trim()
                .toLowerCase(Locale.ROOT)
                .replace("ref-", "")
                .replace("ref", "")
                .replace("#", "");
    }

    private void movePageBefore(ScannedPage draggedPage, ScannedPage targetPage) {
        if (draggedPage == null || targetPage == null || draggedPage == targetPage) {
            return;
        }
        movePageBetweenDocuments(draggedPage, targetPage.documentNumber, targetPage);
    }

    private void movePageBetweenDocuments(ScannedPage draggedPage, int targetDocumentNumber, ScannedPage targetPage) {
        if (draggedPage == null || draggedPage.barcode) {
            return;
        }

        List<DocumentMoveGroup> groups = buildDocumentMoveGroups();
        if (groups.isEmpty()) {
            return;
        }

        int sourceGroupIndex = -1;
        int targetGroupIndex = -1;

        for (int index = 0; index < groups.size(); index++) {
            DocumentMoveGroup group = groups.get(index);

            if (group.pages.contains(draggedPage)) {
                sourceGroupIndex = index;
            }

            if (group.documentNumber == targetDocumentNumber) {
                targetGroupIndex = index;
            }
        }

        if (sourceGroupIndex < 0 || targetGroupIndex < 0) {
            return;
        }

        saveUndoState();

        DocumentMoveGroup sourceGroup = groups.get(sourceGroupIndex);
        sourceGroup.pages.remove(draggedPage);

        if (sourceGroup.pages.isEmpty()) {
            groups.remove(sourceGroupIndex);
            if (sourceGroupIndex < targetGroupIndex) {
                targetGroupIndex--;
            }
        }

        if (targetGroupIndex < 0 || targetGroupIndex >= groups.size()) {
            return;
        }

        DocumentMoveGroup targetGroup = groups.get(targetGroupIndex);
        int insertIndex = targetGroup.pages.size();

        if (targetPage != null) {
            int targetPageIndex = targetGroup.pages.indexOf(targetPage);
            if (targetPageIndex >= 0) {
                insertIndex = targetPageIndex;
            }
        }

        targetGroup.pages.add(insertIndex, draggedPage);

        rebuildPagesFromDocumentGroups(groups);
        selectedPage = draggedPage;
        rebuildDocumentsFromPages();
        refreshWorkspace();
        refreshReviewWorkspace();
    }

    private List<DocumentMoveGroup> buildDocumentMoveGroups() {
        List<DocumentMoveGroup> groups = new ArrayList<>();

        for (DocumentGroup document : documents) {
            groups.add(new DocumentMoveGroup(document.number, document.splitReason, document.pages, false));
        }

        if (!pendingPages.isEmpty()) {
            groups.add(new DocumentMoveGroup(documents.size() + 1, null, pendingPages, true));
        }

        return groups;
    }

    private void rebuildPagesFromDocumentGroups(List<DocumentMoveGroup> groups) {
        allPages.clear();

        for (DocumentMoveGroup group : groups) {
            if (group.pages.isEmpty()) {
                continue;
            }

            for (ScannedPage page : group.pages) {
                page.splitReasonAfter = null;
            }

            if (!group.pending && "Finish batch".equals(group.splitReason)) {
                group.pages.get(group.pages.size() - 1).splitReasonAfter = group.splitReason;
            }

            allPages.addAll(group.pages);
        }
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
        loadSelectedReviewPreviewState();
        updateReviewHeader();
        updateReviewSelectionCard(
                selectedPage == null ? null : findDocumentContainingPage(selectedPage),
                selectedPage,
                selectedPage == null ? -1 : getPageNumberInDocument(selectedPage)
        );
        renderReviewDocumentList();
        renderReviewPreview();
        renderReviewPageTray();
    }

    private void updateReviewHeader() {
        String referenceText = selectedPage == null ? "Ref: -" : "Ref: " + selectedPage.referenceIdLabel();

        reviewBoxValueLabel.setText(getBoxId());
        reviewProfileValueLabel.setText(getSelectedProfile());
        if (reviewSidebarSubtitleLabel != null) {
            reviewSidebarSubtitleLabel.setText(getSelectedProfile() + " · " + getBoxId());
        }
        if (reviewReferenceInfoLabel != null) {
            reviewReferenceInfoLabel.setText(referenceText);
        }

        if (reviewFilesInfoLabel != null) {
            reviewFilesInfoLabel.setText("Scanned Files: " + allPages.size());
        }

        if (reviewDocumentsInfoLabel != null) {
            reviewDocumentsInfoLabel.setText("Documents: " + documents.size());
        }
        reviewDocumentsValueLabel.setText(documents.size() + " \u00B7 " + getNormalPageCount() + " pages");

        if (selectedPage == null) {
            reviewSelectedTitleLabel.setText("No page selected");
            updateReviewSelectionCard(null, null, -1);
            return;
        }

        DocumentGroup document = findDocumentContainingPage(selectedPage);
        int pageNumber = getPageNumberInDocument(selectedPage);

        if (document == null || pageNumber < 1) {
            reviewSelectedTitleLabel.setText(selectedPage.fileName());
            updateReviewSelectionCard(null, selectedPage, -1);
            return;
        }

        reviewSelectedTitleLabel.setText("Document " + document.number + " \u00B7 Page " + pageNumber);
    }

    private void updateReviewSelectionCard(DocumentGroup document, ScannedPage page, int pageNumber) {
        if (reviewSelectionProfileValueLabel != null) {
            reviewSelectionProfileValueLabel.setText(getSelectedProfile());
        }

        if (reviewSelectionBoxValueLabel != null) {
            reviewSelectionBoxValueLabel.setText(getBoxId());
        }

        if (reviewSelectionDocumentValueLabel != null) {
            reviewSelectionDocumentValueLabel.setText(document == null ? "-" : "Document " + document.number);
        }

        if (reviewSelectionFileValueLabel != null) {
            if (page == null) {
                reviewSelectionFileValueLabel.setText("-");
            } else if (pageNumber > 0) {
                reviewSelectionFileValueLabel.setText("File " + pageNumber);
            } else {
                reviewSelectionFileValueLabel.setText(page.fileName());
            }
        }

        if (reviewSelectionReferenceValueLabel != null) {
            reviewSelectionReferenceValueLabel.setText(page == null ? "-" : page.referenceIdLabel());
        }

        if (reviewSelectionFileIdValueLabel != null) {
            reviewSelectionFileIdValueLabel.setText(page == null ? "-" : String.valueOf(page.fileId));
        }
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

            VBox documentBlock = new VBox(12);
            documentBlock.setAlignment(Pos.TOP_LEFT);
            documentBlock.getStyleClass().add("document-tree-document-block");
            if (reviewDocumentListView) {
                documentBlock.getStyleClass().add("document-tree-list-block");
            }

            HBox documentCard = new HBox(9);
            documentCard.setAlignment(Pos.CENTER_LEFT);
            documentCard.getStyleClass().addAll("document-tree-document-header", "document-tree-document-header-framed");
            if (reviewDocumentListView) {
                documentCard.getStyleClass().add("document-tree-list-header");
            }

            Region chevron = new Region();
            chevron.getStyleClass().add("document-tree-chevron-icon");
            chevron.setRotate(collapsedDocuments.contains(document.number) ? 0 : 90);

            Label title = new Label("Document " + document.number);
            title.getStyleClass().add("document-tree-document-title");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label warning = new Label(document.hasPagesNeedingRescan() ? "!" : "");
            warning.getStyleClass().add("document-tree-warning");

            Label count = new Label(document.pages.size() + " pages");
            count.getStyleClass().add("document-tree-count");

            documentCard.getChildren().addAll(chevron, title, spacer, warning, count);

            documentCard.setOnMouseClicked(event -> {
                if (collapsedDocuments.contains(document.number)) {
                    collapsedDocuments.remove(document.number);
                } else {
                    collapsedDocuments.add(document.number);
                }

                if (!document.pages.isEmpty()) {
                    selectedPage = document.pages.get(0);
                }
                refreshReviewWorkspace();
            });

            configureDocumentDropTarget(documentCard, document.number);

            documentBlock.getChildren().add(documentCard);

            if (!collapsedDocuments.contains(document.number)) {
                VBox pageStack = new VBox(reviewDocumentListView ? 0 : 18);
                pageStack.setAlignment(reviewDocumentListView ? Pos.TOP_LEFT : Pos.TOP_CENTER);
                pageStack.getStyleClass().add("document-tree-page-stack");
                if (reviewDocumentListView) {
                    pageStack.getStyleClass().add("document-tree-list-page-stack");
                }
                for (int pageIndex = 0; pageIndex < document.pages.size(); pageIndex++) {
                    ScannedPage page = document.pages.get(pageIndex);
                    Node pageNode = reviewDocumentListView
                            ? createReviewPageRow(page, pageIndex + 1)
                            : createReviewEmbeddedPageCard(page, pageIndex + 1);
                    pageStack.getChildren().add(pageNode);
                }
                documentBlock.getChildren().add(pageStack);
            }

            reviewDocumentListContainer.getChildren().add(documentBlock);

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
            currentReviewPreviewWrapper = null;
            reviewPreviewHost.getChildren().add(createReviewEmptyPreview());
            return;
        }

        Node previewNode = createDocumentPreview(selectedPage);
        reviewPreviewHost.getChildren().add(wrapReviewPreviewWithAutoScale(previewNode));
    }

    private VBox createReviewEmbeddedPageCard(ScannedPage page, int pageNumber) {
        VBox card = new VBox(3);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().addAll("review-page-tray-item", "review-embedded-page-card");
        configureEmbeddedPageCardFrame(card, page, 184, 164, 218);

        if (page == selectedPage) {
            card.getStyleClass().add("review-page-tray-item-selected");
        }

        if (page.needsRescan) {
            card.getStyleClass().add("review-page-tray-item-warning");
        }

        StackPane thumbnail = new StackPane();
        thumbnail.getStyleClass().add("review-page-tray-thumbnail");
        configureThumbnailFrame(thumbnail, page, 164, 218);
        applyThumbnailClip(thumbnail, 28);
        Node imageNode = createRotatedThumbnailNode(page, 148, 214);
        if (imageNode != null) {
            thumbnail.getChildren().add(imageNode);
        } else {
            VBox lines = new VBox(3);
            lines.setAlignment(Pos.TOP_LEFT);
            lines.getChildren().addAll(
                    createLine("tray-line-dark", 27, 3),
                    createLine("tray-line-light", 42, 3),
                    createLine("tray-line-light", 36, 3),
                    createLine("tray-line-light", 30, 3)
            );
            thumbnail.getChildren().add(lines);
        }

        Label status = new Label(getTrayStatusText(page));
        status.getStyleClass().add("page-tray-status-badge");
        StackPane.setAlignment(status, Pos.TOP_RIGHT);
        thumbnail.getChildren().add(status);

        HBox labelRow = createScanPageLabelRow(page, "Page " + pageNumber, true);

        card.getChildren().addAll(thumbnail, labelRow);
        card.setOnMouseClicked(event -> {
            selectedPage = page;
            refreshReviewWorkspace();
        });

        configurePageDrag(card, page);

        return card;
    }

    private HBox createReviewPageRow(ScannedPage page, int pageNumber) {
        HBox row = new HBox(9);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("document-tree-page-row", "document-tree-list-page-row");

        if (page == selectedPage) {
            row.getStyleClass().add("document-tree-page-selected");
        }

        HBox labelRow = createScanPageLabelRow(page, "Page " + pageNumber, false);

        row.getChildren().add(labelRow);
        row.setOnMouseClicked(event -> {
            selectedPage = page;
            refreshReviewWorkspace();
        });

        configurePageDrag(row, page);

        return row;
    }

    private HBox createScanPageLabelRow(ScannedPage page, String baseLabel, boolean centered) {
        HBox labelRow = new HBox(6);
        labelRow.setAlignment(centered ? Pos.CENTER : Pos.CENTER_LEFT);

        Label pageLabel = new Label(baseLabel);
        pageLabel.getStyleClass().add(centered ? "review-page-tray-number" : "document-tree-page-title");
        labelRow.getChildren().add(pageLabel);

        if (page.needsRescan) {
            Label statusLabel = new Label("Needs Rescan");
            statusLabel.getStyleClass().add("qa-page-status-text-fix");
            labelRow.getChildren().add(statusLabel);
        }

        return labelRow;
    }

    @FXML
    public void onOpenScanExport() {
        onOpenExportTypeDialog();
    }

    @FXML
    public void onOpenExportTypeDialog() {
        Stage stage = new Stage();
        stage.setTitle("TIFF Export");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setResizable(false);

        if (reviewWorkspaceView != null && reviewWorkspaceView.getScene() != null) {
            stage.initOwner(reviewWorkspaceView.getScene().getWindow());
        }

        VBox content = buildExportDialogContent(stage);
        StackPane root = new StackPane(content);
        root.getStyleClass().addAll("app-shell", "exports-dialog-stage");

        URL stylesheetUrl = getClass().getResource("/css/app.css");
        Scene scene = new Scene(root);
        if (stylesheetUrl != null) {
            scene.getStylesheets().add(stylesheetUrl.toExternalForm());
        }

        stage.setScene(scene);
        stage.sizeToScene();
        stage.showAndWait();
    }

    private VBox buildExportDialogContent(Stage stage) {
        List<Document> exportDocuments = buildExportDocuments();
        List<String> boxFiles = buildExportFiles(exportDocuments);
        ObjectProperty<TiffExportType> selectedType = new SimpleObjectProperty<>(TiffExportType.MULTI_PAGE);

        Label title = new Label("TIFF Export");
        title.getStyleClass().add("exports-dialog-title");

        VBox header = new VBox(9, title);
        header.getStyleClass().add("exports-dialog-header");

        Label boxValue = new Label(getBoxId());
        boxValue.getStyleClass().add("exports-dialog-box-value");

        Label boxDetail = new Label("Only scanned documents from this box can be exported in this dialog.");
        boxDetail.getStyleClass().add("exports-dialog-box-detail");

        VBox boxCard = new VBox(6, boxValue, boxDetail);
        boxCard.getStyleClass().add("exports-dialog-box-card");

        Button singlePageCard = buildExportTypeCard(
                "Single-page TIFF",
                "Separate TIFF files",
                TiffExportType.SINGLE_PAGE,
                selectedType
        );
        Button multiPageCard = buildExportTypeCard(
                "Multi-page TIFF",
                "One TIFF per document",
                TiffExportType.MULTI_PAGE,
                selectedType
        );
        HBox.setHgrow(singlePageCard, Priority.ALWAYS);
        HBox.setHgrow(multiPageCard, Priority.ALWAYS);

        HBox typeRow = new HBox(18, singlePageCard, multiPageCard);
        typeRow.getStyleClass().add("exports-dialog-type-row");

        Label selectedFilesTitle = new Label("Documents in box");
        selectedFilesTitle.getStyleClass().add("exports-dialog-files-title");

        Label selectedFilesCount = new Label(formatSelectedDocumentCount(boxFiles.size()));
        selectedFilesCount.getStyleClass().add("exports-dialog-files-count");

        Region filesSpacer = new Region();
        HBox.setHgrow(filesSpacer, Priority.ALWAYS);

        HBox filesHeader = new HBox(18, selectedFilesTitle, filesSpacer, selectedFilesCount);
        filesHeader.setAlignment(Pos.CENTER_LEFT);

        GridPane fileGrid = new GridPane();
        fileGrid.getStyleClass().add("exports-dialog-file-grid");

        ScrollPane fileListScroll = new ScrollPane(fileGrid);
        fileListScroll.getStyleClass().add("exports-dialog-file-scroll");
        fileListScroll.setFitToWidth(true);
        fileListScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        fileListScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        fileListScroll.setPrefViewportHeight(156);
        renderSelectedFiles(fileGrid, boxFiles);

        VBox filesCard = new VBox(18, filesHeader, fileListScroll);
        filesCard.getStyleClass().add("exports-dialog-files-card");

        Region divider = new Region();
        divider.getStyleClass().add("portal-divider");
        divider.setMaxWidth(Double.MAX_VALUE);

        Label outputLabel = new Label("Output:");
        outputLabel.getStyleClass().add("exports-dialog-output-label");

        Label outputValue = new Label(buildOutputText(selectedType.get(), exportDocuments));
        outputValue.getStyleClass().add("exports-dialog-output-value");
        outputValue.setWrapText(false);
        outputValue.setMinHeight(Region.USE_PREF_SIZE);
        outputValue.setPrefWidth(420);
        outputValue.setMaxWidth(420);
        selectedType.addListener((observable, oldValue, newValue) ->
                outputValue.setText(buildOutputText(newValue, exportDocuments))
        );

        HBox outputBox = new HBox(9, outputLabel, outputValue);
        outputBox.getStyleClass().add("exports-dialog-output-box");
        outputBox.setAlignment(Pos.CENTER_LEFT);
        outputBox.setMinHeight(36);
        outputBox.setPrefHeight(36);
        outputBox.setMaxHeight(36);

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().addAll("portal-secondary-button", "exports-dialog-cancel-button");
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(event -> stage.close());

        Button exportButton = new Button("Export");
        exportButton.getStyleClass().addAll("portal-primary-button", "exports-dialog-export-button");
        exportButton.setDefaultButton(true);
        exportButton.setOnAction(event -> handleWorkspaceExport(stage, selectedType.get(), exportDocuments));

        HBox footerActions = new HBox(9, cancelButton, exportButton);
        footerActions.getStyleClass().add("exports-dialog-footer-actions");
        footerActions.setAlignment(Pos.CENTER_RIGHT);

        VBox footer = new VBox(9, outputBox, footerActions);
        footer.getStyleClass().add("exports-dialog-footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setFillWidth(true);

        VBox content = new VBox(18, header, boxCard, typeRow, filesCard, divider, footer);
        content.getStyleClass().add("exports-dialog-content");
        content.setFillWidth(true);
        return content;
    }

    private Button buildExportTypeCard(
            String titleText,
            String subtitleText,
            TiffExportType type,
            ObjectProperty<TiffExportType> selectedType
    ) {
        Label title = new Label(titleText);
        title.getStyleClass().add("exports-dialog-option-title");

        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("exports-dialog-option-subtitle");

        VBox copy = new VBox(9, title, subtitle);
        copy.getStyleClass().add("exports-dialog-option-copy");

        StackPane checkBadge = new StackPane(UserPortalUi.buildIcon("selected-check", "exports-dialog-option-check-icon"));
        checkBadge.getStyleClass().add("exports-dialog-option-check-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox graphic = new HBox(12, copy, spacer, checkBadge);
        graphic.getStyleClass().add("exports-dialog-option-content");
        graphic.setAlignment(Pos.TOP_LEFT);

        Button button = new Button();
        button.getStyleClass().add("exports-dialog-option-button");
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setGraphic(graphic);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setFocusTraversable(false);
        button.setOnAction(event -> selectedType.set(type));

        Runnable refreshSelection = () -> updateExportTypeCard(button, checkBadge, selectedType.get() == type);
        selectedType.addListener((observable, oldValue, newValue) -> refreshSelection.run());
        refreshSelection.run();

        return button;
    }

    private void updateExportTypeCard(Button button, StackPane checkBadge, boolean selected) {
        button.getStyleClass().removeAll(
                "exports-dialog-option-button-selected",
                "exports-dialog-option-button-unselected"
        );
        button.getStyleClass().add(selected
                ? "exports-dialog-option-button-selected"
                : "exports-dialog-option-button-unselected");
        checkBadge.setVisible(selected);
        checkBadge.setManaged(true);
    }

    private void renderSelectedFiles(GridPane fileGrid, List<String> selectedFiles) {
        fileGrid.getChildren().clear();
        fileGrid.getColumnConstraints().setAll(
                percentColumn(33.333),
                percentColumn(33.333),
                percentColumn(33.333)
        );

        if (selectedFiles.isEmpty()) {
            Label emptyState = new Label("No scanned documents available for this export.");
            emptyState.getStyleClass().add("exports-dialog-empty-state");
            fileGrid.add(emptyState, 0, 0, 3, 1);
            return;
        }

        for (int index = 0; index < selectedFiles.size(); index++) {
            int column = index % 3;
            int row = index / 3;
            fileGrid.add(createSelectedFileCell(selectedFiles.get(index), column < 2), column, row);
        }
    }

    private HBox createSelectedFileCell(String fileName, boolean withRightBorder) {
        Label fileLabel = new Label(fileName);
        fileLabel.getStyleClass().add("exports-dialog-file-name");

        HBox cell = new HBox(6, fileLabel);
        cell.getStyleClass().add("exports-dialog-file-cell-box");
        if (withRightBorder) {
            cell.getStyleClass().add("exports-dialog-file-cell-box-bordered");
        }
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cell, Priority.ALWAYS);
        return cell;
    }

    private List<Document> buildExportDocuments() {
        List<Document> exportDocuments = new ArrayList<>();

        for (DocumentGroup document : documents) {
            List<PageImage> pages = toExportPages(document.pages);
            if (!pages.isEmpty()) {
                exportDocuments.add(new Document("document_" + String.format(Locale.US, "%03d", document.number), pages));
            }
        }

        List<PageImage> pendingExportPages = toExportPages(pendingPages);
        if (!pendingExportPages.isEmpty()) {
            exportDocuments.add(new Document("pending_document_" + String.format(Locale.US, "%03d", documents.size() + 1), pendingExportPages));
        }

        return exportDocuments;
    }

    private List<PageImage> toExportPages(List<ScannedPage> scannedPages) {
        if (scannedPages == null || scannedPages.isEmpty()) {
            return List.of();
        }

        List<PageImage> pages = new ArrayList<>();
        int pageNumber = 1;
        for (ScannedPage scannedPage : scannedPages) {
            if (scannedPage == null || scannedPage.needsRescan) {
                continue;
            }

            PageImage pageImage = new PageImage(
                    pageNumber++,
                    scannedPage.barcode ? PageImage.PageType.BARCODE : PageImage.PageType.TIFF,
                    firstNonBlank(scannedPage.sourceReference, scannedPage.fileName())
            );
            pageImage.setReferenceId(scannedPage.referenceId);
            pageImage.setRotationDegrees(scannedPage.rotationDegrees);
            pageImage.setDisplayContent(scannedPage.displayContent);
            pageImage.setPreviewSourceBytes(scannedPage.previewSourceBytes);
            pages.add(pageImage);
        }
        return pages;
    }

    private List<String> buildExportFiles(List<Document> exportDocuments) {
        if (exportDocuments == null || exportDocuments.isEmpty()) {
            return List.of();
        }

        List<String> files = new ArrayList<>(exportDocuments.size());
        for (Document document : exportDocuments) {
            int pageCount = document.getPages().size();
            files.add(document.getSourceItemId() + " (" + pageCount + " " + pluralize(pageCount, "page") + ")");
        }
        return files;
    }

    private String formatSelectedDocumentCount(int documentCount) {
        return documentCount + " " + pluralize(documentCount, "document");
    }

    private String buildOutputText(TiffExportType type, List<Document> exportDocuments) {
        int documentCount = exportDocuments == null ? 0 : exportDocuments.size();
        if (documentCount == 0) {
            return "No scanned documents are available for export";
        }

        return switch (type) {
            case SINGLE_PAGE -> countExportPages(exportDocuments) + " separate .tiff "
                    + pluralize(countExportPages(exportDocuments), "file") + " will be generated";
            case MULTI_PAGE -> documentCount + " multi-page .tiff " + pluralize(documentCount, "file")
                    + " will be generated, one per document";
        };
    }

    private int countExportPages(List<Document> exportDocuments) {
        if (exportDocuments == null) {
            return 0;
        }

        int pageCount = 0;
        for (Document document : exportDocuments) {
            pageCount += document.getPages().size();
        }
        return pageCount;
    }

    private void handleWorkspaceExport(Stage stage, TiffExportType exportType, List<Document> exportDocuments) {
        if (exportDocuments == null || exportDocuments.isEmpty()) {
            showExportAlert(stage, Alert.AlertType.ERROR, "No documents to export",
                    "There are no saved scanned pages ready for TIFF export.");
            return;
        }

        String profileName = getSelectedProfile();
        ScanProfile profile = portalModel.fetchScanProfileByName(profileName);
        String profileCode = firstNonBlank(profile == null ? null : profile.getCode(), profileName);
        String exportNaming = firstNonBlank(profile == null ? null : profile.getExportNaming(), "{profileCode}_{boxId}");

        try {
            Path outputDirectory = Path.of(
                    System.getProperty("user.home"),
                    "Downloads",
                    "WebLager Exports",
                    safeFolderName(profileName, getBoxId())
            );
            TiffExportManager.ExportResult result = tiffExportManager.exportPlan(
                    exportType == TiffExportType.SINGLE_PAGE
                            ? tiffExportManager.createSinglePagePlan(
                            profileName,
                            profileCode,
                            exportNaming,
                            getBoxId(),
                            flattenExportPages(exportDocuments)
                    )
                            : tiffExportManager.createMultiPagePlan(
                            profileName,
                            profileCode,
                            exportNaming,
                            getBoxId(),
                            exportDocuments
                    ),
                    outputDirectory
            );

            showExportAlert(stage, Alert.AlertType.INFORMATION, "Export completed",
                    result.writtenFiles().size() + " TIFF " + pluralize(result.writtenFiles().size(), "file")
                            + " written to " + result.outputDirectory());
            stage.close();
        } catch (IOException | RuntimeException exception) {
            showExportAlert(stage, Alert.AlertType.ERROR, "Export failed", exception.getMessage());
        }
    }

    private List<PageImage> flattenExportPages(List<Document> exportDocuments) {
        List<PageImage> pages = new ArrayList<>();
        if (exportDocuments == null) {
            return pages;
        }

        for (Document document : exportDocuments) {
            pages.addAll(document.getPages());
        }
        return pages;
    }

    private void showExportAlert(Stage owner, Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null || message.isBlank() ? "The export could not be completed." : message);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    private String safeFolderName(String profileName, String boxId) {
        return firstNonBlank(profileName, "profile").replaceAll("[^a-zA-Z0-9._-]", "_")
                + "_" + firstNonBlank(boxId, "box").replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred.trim();
    }

    private String pluralize(int count, String singular) {
        return count == 1 ? singular : singular + "s";
    }

    private javafx.scene.layout.ColumnConstraints percentColumn(double percentWidth) {
        javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
        column.setPercentWidth(percentWidth);
        column.setFillWidth(true);
        column.setHgrow(Priority.ALWAYS);
        return column;
    }

    private VBox createEmbeddedPageCard(ScannedPage page, String labelText) {
        VBox card = new VBox(3);
        card.setAlignment(Pos.CENTER);
        configureEmbeddedPageCardFrame(card, page, 184, 164, 218);

        if (page.barcode) {
            card.getStyleClass().add("page-tray-barcode-split-card");
        } else {
            card.getStyleClass().add("page-tray-item");
        }

        if (page == selectedPage) {
            card.getStyleClass().add(page.barcode
                    ? "page-tray-barcode-split-card-selected"
                    : "page-tray-item-selected"
            );
        }

        if (page.needsRescan) {
            card.getStyleClass().add("page-tray-item-warning");
        }

        StackPane thumbnail = new StackPane();
        thumbnail.getStyleClass().add("page-tray-thumbnail");
        configureThumbnailFrame(thumbnail, page, 164, 218);
        applyThumbnailClip(thumbnail, 28);
        Node imageNode = createRotatedThumbnailNode(page, 148, 214);
        if (imageNode != null) {
            thumbnail.getChildren().add(imageNode);
        } else if (page.barcode) {
            Label barcode = new Label("||||");
            barcode.getStyleClass().add("page-tray-barcode-mark");
            thumbnail.getChildren().add(barcode);
        } else {
            VBox lines = new VBox(3);
            lines.setAlignment(Pos.TOP_LEFT);
            lines.getChildren().addAll(
                    createLine("tray-line-dark", 27, 3),
                    createLine("tray-line-light", 42, 3),
                    createLine("tray-line-light", 36, 3),
                    createLine("tray-line-light", 30, 3)
            );
            thumbnail.getChildren().add(lines);
        }

        Label status = new Label(getTrayStatusText(page));
        status.getStyleClass().add("page-tray-status-badge");
        StackPane.setAlignment(status, Pos.TOP_RIGHT);
        thumbnail.getChildren().add(status);

        if (page.barcode) {
            Label number = new Label(labelText);
            number.getStyleClass().add("page-tray-barcode-split-label");
            number.setMaxWidth(Double.MAX_VALUE);
            number.setAlignment(Pos.CENTER);
            card.getChildren().addAll(thumbnail, number);
        } else {
            HBox labelRow = createScanPageLabelRow(page, labelText, true);
            card.getChildren().addAll(thumbnail, labelRow);
        }
        return card;
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

            double autoScale = Math.min(1.0, Math.min(widthScale, heightScale));
            return autoScale * reviewZoomMultiplier.get();
        }, reviewPreviewHost.widthProperty(), reviewPreviewHost.heightProperty(), reviewZoomMultiplier);

        wrapper.scaleXProperty().bind(scaleBinding);
        wrapper.scaleYProperty().bind(scaleBinding);
        currentReviewPreviewWrapper = wrapper;
        scaleBinding.addListener((observable, oldValue, newValue) -> clampReviewPreviewTranslation());
        clampReviewPreviewTranslation();

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
            ScannedPage page = selectedDocument.pages.get(index);

            VBox card = createReviewTrayPageCard(page, index + 1);
            reviewPageTrayContainer.getChildren().add(card);
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

        VBox lines = new VBox(3);
        lines.setAlignment(Pos.TOP_LEFT);
        lines.getChildren().addAll(
                createLine("tray-line-dark", 27, 3),
                createLine("tray-line-light", 42, 3),
                createLine("tray-line-light", 36, 3),
                createLine("tray-line-light", 30, 3)
        );

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

        configurePageDrag(card, page);

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

    private static final class ScanSnapshot {
        private final List<PageSnapshot> pages = new ArrayList<>();
        private final int selectedPageReferenceId;
        private final int nextReferenceId;
        private final int nextFileId;
        private final int sessionRotationDegrees;
        private final Set<Integer> collapsedDocuments = new HashSet<>();

        private ScanSnapshot(
                List<ScannedPage> pages,
                int selectedPageReferenceId,
                int nextReferenceId,
                int nextFileId,
                Set<Integer> collapsedDocuments,
                int sessionRotationDegrees
        ) {
            for (ScannedPage page : pages) {
                this.pages.add(new PageSnapshot(page));
            }

            this.selectedPageReferenceId = selectedPageReferenceId;
            this.nextReferenceId = nextReferenceId;
            this.nextFileId = nextFileId;
            this.sessionRotationDegrees = sessionRotationDegrees;
            this.collapsedDocuments.addAll(collapsedDocuments);
        }
    }

    private static final class PageSnapshot {
        private final int referenceId;
        private final int fileId;
        private final boolean barcode;
        private final int documentNumber;
        private final int rotationDegrees;
        private final boolean needsRescan;
        private final String splitReasonAfter;
        private final String sourceReference;
        private final String displayContent;
        private final String previewContent;
        private final byte[] previewSourceBytes;
        private final double previewZoomMultiplier;
        private final double previewTranslateX;
        private final double previewTranslateY;

        private PageSnapshot(ScannedPage page) {
            this.referenceId = page.referenceId;
            this.fileId = page.fileId;
            this.barcode = page.barcode;
            this.documentNumber = page.documentNumber;
            this.rotationDegrees = page.rotationDegrees;
            this.needsRescan = page.needsRescan;
            this.splitReasonAfter = page.splitReasonAfter;
            this.sourceReference = page.sourceReference;
            this.displayContent = page.displayContent;
            this.previewContent = page.previewContent;
            this.previewSourceBytes = Arrays.copyOf(page.previewSourceBytes, page.previewSourceBytes.length);
            this.previewZoomMultiplier = page.previewZoomMultiplier;
            this.previewTranslateX = page.previewTranslateX;
            this.previewTranslateY = page.previewTranslateY;
        }

        private ScannedPage toScannedPage() {
            ScannedPage page = new ScannedPage(
                    referenceId,
                    fileId,
                    barcode,
                    needsRescan,
                    sourceReference,
                    displayContent,
                    previewContent,
                    previewSourceBytes
            );
            page.documentNumber = documentNumber;
            page.rotationDegrees = rotationDegrees;
            page.splitReasonAfter = splitReasonAfter;
            page.previewZoomMultiplier = previewZoomMultiplier;
            page.previewTranslateX = previewTranslateX;
            page.previewTranslateY = previewTranslateY;
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

    private static final class DocumentMoveGroup {
        private final int documentNumber;
        private final String splitReason;
        private final List<ScannedPage> pages = new ArrayList<>();
        private final boolean pending;

        private DocumentMoveGroup(int documentNumber, String splitReason, List<ScannedPage> pages, boolean pending) {
            this.documentNumber = documentNumber;
            this.splitReason = splitReason;
            this.pages.addAll(pages);
            this.pending = pending;
        }
    }

    private enum TiffExportType {
        SINGLE_PAGE,
        MULTI_PAGE
    }

    private static final class ScannedPage {
        private final int referenceId;
        private final int fileId;
        private final boolean barcode;
        private final String sourceReference;
        private final String displayContent;
        private final String previewContent;
        private final byte[] previewSourceBytes;

        private int documentNumber;
        private int rotationDegrees;
        private double previewZoomMultiplier;
        private double previewTranslateX;
        private double previewTranslateY;
        private boolean needsRescan;
        private String splitReasonAfter;
        private transient Image cachedPreviewImage;

        private ScannedPage(int referenceId, int fileId, boolean barcode, boolean needsRescan) {
            this(referenceId, fileId, barcode, needsRescan, "", "", "", new byte[0]);
        }

        private ScannedPage(
                int referenceId,
                int fileId,
                boolean barcode,
                boolean needsRescan,
                String sourceReference,
                String displayContent,
                String previewContent,
                byte[] previewSourceBytes
        ) {
            this.referenceId = referenceId;
            this.fileId = fileId;
            this.barcode = barcode;
            this.needsRescan = needsRescan;
            this.sourceReference = sourceReference == null ? "" : sourceReference;
            this.displayContent = displayContent == null ? "" : displayContent;
            this.previewContent = previewContent == null ? "" : previewContent;
            this.previewSourceBytes = previewSourceBytes == null ? new byte[0] : Arrays.copyOf(previewSourceBytes, previewSourceBytes.length);
            this.rotationDegrees = 0;
            this.previewZoomMultiplier = 1.0;
        }

        private String referenceIdLabel() {
            return "REF-" + String.format("%03d", referenceId);
        }

        private String fileName() {
            if (!sourceReference.isBlank()) {
                return sourceReference;
            }
            if (barcode) {
                return "barcode_split_" + String.format("%02d", referenceId) + ".tiff";
            }

            return "scan_file_" + String.format("%02d", fileId) + ".tiff";
        }

        private String imageContent() {
            if (!previewContent.isBlank()) {
                return previewContent;
            }
            return displayContent;
        }
    }

    private record PreviewDecodeResult(Image image, String message) {
        private static PreviewDecodeResult success(Image image) {
            return new PreviewDecodeResult(image, "");
        }

        private static PreviewDecodeResult failed(String message) {
            return new PreviewDecodeResult(null, message == null ? "The TIFF preview could not be decoded." : message);
        }
    }
}
