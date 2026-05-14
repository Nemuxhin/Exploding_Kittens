package easv.gui.controller.user;

import easv.be.PageImage;
import easv.be.ScanSession;
import easv.bll.ScanImportResult;
import easv.bll.ScanManager;
import easv.gui.BackgroundExecutor;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
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
import java.net.URL;
import java.util.Base64;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    @FXML private VBox scanSetupView;
    @FXML private BorderPane scanWorkspaceView;
    @FXML private BorderPane reviewWorkspaceView;

    @FXML private ComboBox<String> profileComboBox;
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
    private final ScanManager scanManager = new ScanManager();

    private final DoubleProperty previewZoomMultiplier = new SimpleDoubleProperty(1.0);
    private final DoubleProperty reviewZoomMultiplier = new SimpleDoubleProperty(1.0);

    private int nextReferenceId = 1;
    private int nextFileId = 1;
    private ScanSession activeScanSession;
    private boolean scanInProgress = false;

    private double previewTranslateX = 0;
    private double previewTranslateY = 0;
    private double previewDragStartX = 0;
    private double previewDragStartY = 0;
    private double previewTranslateStartX = 0;
    private double previewTranslateStartY = 0;

    private StackPane currentPreviewWrapper;
    private StackPane currentReviewPreviewWrapper;

    private ScannedPage selectedPage;

    private UserNavigator navigator = UserNavigator.none();

    public void setNavigator(UserNavigator navigator) {
        this.navigator = navigator == null ? UserNavigator.none() : navigator;
    }

    @FXML
    private void initialize() {
        configureProfiles();
        configureProfileInfo();
        configureValidation();
        configureDocumentTreeScroll();
        configurePreviewInteractions();
        configureReviewPreviewInteractions();
        updatePreviewZoomLabel();
        updateReviewZoomLabel();
        updateUndoButtonState();
        hideFinishReviewModal();
        hideSubmitConfirmationModal();
        showSetupView();
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
        activeScanSession = scanManager.startSession(getBoxId(), getSelectedProfile());
        scanInProgress = false;

        selectedPage = null;
        resetPreviewViewState();
        hideFinishReviewModal();
        hideSubmitConfirmationModal();

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

        BackgroundExecutor.io().execute(() -> {
            try {
                ScanImportResult result = scanManager.scanNextItem(
                        activeScanSession,
                        activeScanSession.getSelectedBarcodeBehavior(),
                        "Keep barcode page in final document"
                );
                Platform.runLater(() -> applyScanImportResult(result));
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    scanInProgress = false;
                    selectedFileTitleLabel.setText("Scan failed");
                    selectedFileRefLabel.setText(exception.getMessage() == null || exception.getMessage().isBlank()
                            ? "Failed to import next file."
                            : exception.getMessage());
                });
            }
        });
    }

    private void applyScanImportResult(ScanImportResult result) {
        scanInProgress = false;

        if (result == null) {
            selectedFileTitleLabel.setText("Scan failed");
            selectedFileRefLabel.setText("No scan result returned.");
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
                pageImage.getPreviewContent()
        );
        page.rotationDegrees = pageImage.getRotationDegrees();
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

            if (page.splitReasonAfter != null && !page.splitReasonAfter.isBlank()) {
                DocumentGroup document = createDocument(documentNumber, page.splitReasonAfter, currentDocumentPages);
                documents.add(document);
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
    }

    @FXML
    private void onResetReviewPreviewView() {
        resetReviewPreviewViewState();
        clampReviewPreviewTranslation();
    }

    private void resetReviewPreviewViewState() {
        reviewZoomMultiplier.set(1.0);

        if (currentReviewPreviewWrapper != null) {
            currentReviewPreviewWrapper.setTranslateX(0);
            currentReviewPreviewWrapper.setTranslateY(0);
        }
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

        currentReviewPreviewWrapper.setTranslateX(currentReviewPreviewWrapper.getTranslateX() + deltaX);
        currentReviewPreviewWrapper.setTranslateY(currentReviewPreviewWrapper.getTranslateY() + deltaY);
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

        currentReviewPreviewWrapper.setTranslateX(clamp(currentReviewPreviewWrapper.getTranslateX(), -maxX, maxX));
        currentReviewPreviewWrapper.setTranslateY(clamp(currentReviewPreviewWrapper.getTranslateY(), -maxY, maxY));
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

        profileComboBox.getSelectionModel().clearSelection();
        boxIdTextField.clear();

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
        updateWorkspaceHeader();
        refreshHeaderInfoChips();
        updateRotationButtons();
        renderDocumentTree();
        renderPreview();
        updateUndoButtonState();
    }

    private void updateRotationButtons() {
        if (rotateLeftButton == null || rotateRightButton == null) {
            return;
        }

        if (!hasNormalSelectedPage()) {
            rotateLeftButton.setText("Rotate Left (90°)");
            rotateRightButton.setText("Rotate Right (90°)");
            return;
        }

        int currentRotation = normalizeRotation(selectedPage.rotationDegrees);
        int leftTarget = normalizeRotation(currentRotation - 90);
        int rightTarget = normalizeRotation(currentRotation + 90);

        rotateLeftButton.setText("Rotate Left (" + leftTarget + "°)");
        rotateRightButton.setText("Rotate Right (" + rightTarget + "°)");
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

            HBox documentHeader = createDocumentHeader(document);
            documentHeader.getStyleClass().add("document-tree-document-header-framed");
            documentBlock.getChildren().add(documentHeader);

            if (!collapsedDocuments.contains(document.number)) {
                VBox pageStack = new VBox(18);
                pageStack.setAlignment(Pos.TOP_CENTER);
                pageStack.getStyleClass().add("document-tree-page-stack");
                for (int pageIndex = 0; pageIndex < document.pages.size(); pageIndex++) {
                    ScannedPage page = document.pages.get(pageIndex);
                    VBox pageCard = createDocumentTreePageCard(page, pageIndex + 1);
                    pageStack.getChildren().add(pageCard);
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

            HBox pendingHeader = createPendingDocumentHeader(pendingDocumentNumber, pendingPages);
            pendingHeader.getStyleClass().add("document-tree-document-header-framed");
            pendingBlock.getChildren().add(pendingHeader);

            if (!collapsedDocuments.contains(pendingDocumentNumber)) {
                VBox pageStack = new VBox(18);
                pageStack.setAlignment(Pos.TOP_CENTER);
                pageStack.getStyleClass().add("document-tree-page-stack");
                for (int pageIndex = 0; pageIndex < pendingPages.size(); pageIndex++) {
                    ScannedPage page = pendingPages.get(pageIndex);
                    VBox pageCard = createDocumentTreePageCard(page, pageIndex + 1);
                    pageStack.getChildren().add(pageCard);
                }
                pendingBlock.getChildren().add(pageStack);
            }

            documentTreeContainer.getChildren().add(pendingBlock);
        } else if (shouldShowNextPendingDocumentPlaceholder()) {
            int pendingDocumentNumber = documents.size() + 1;
            VBox pendingBlock = new VBox(12);
            pendingBlock.setAlignment(Pos.TOP_LEFT);
            pendingBlock.getStyleClass().add("document-tree-document-block");

            HBox pendingHeader = createPendingDocumentHeader(pendingDocumentNumber, List.of());
            pendingHeader.getStyleClass().add("document-tree-document-header-framed");
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

        documentHeader.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
                documentHeader.getStyleClass().add("document-tree-drop-target");
            }

            event.consume();
        });

        documentHeader.setOnDragExited(event ->
                documentHeader.getStyleClass().remove("document-tree-drop-target")
        );

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

        return documentHeader;
    }

    private boolean shouldShowTreeSplitRow(DocumentGroup document, int documentIndex) {
        boolean isLastDocument = documentIndex == documents.size() - 1;

        if ("Finish batch".equals(document.splitReason) && isLastDocument) {
            return false;
        }

        return "Barcode split".equals(document.splitReason)
                || "Manual split".equals(document.splitReason);
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

        Label drag = new Label("\u22EE");
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

        DocumentGroup targetDocument = findDocument(targetDocumentNumber);

        if (targetDocument == null) {
            return;
        }

        saveUndoState();

        page.splitReasonAfter = null;
        allPages.remove(page);

        ScannedPage lastTargetPage = targetDocument.pages.isEmpty()
                ? null
                : targetDocument.pages.get(targetDocument.pages.size() - 1);

        int insertIndex = lastTargetPage == null
                ? allPages.size()
                : allPages.indexOf(lastTargetPage) + 1;

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

        Node previewNode = selectedPage.barcode
                ? createBarcodePreview(selectedPage)
                : createDocumentPreview(selectedPage);

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

        Label copy = new Label("Click \"Scan Next File\".");
        copy.getStyleClass().add("scan-preview-empty-copy");

        emptyPreview.getChildren().addAll(title, copy);

        return emptyPreview;
    }

    private Node createBarcodePreview(ScannedPage page) {
        Image image = resolvePageImage(page);
        if (image != null) {
            StackPane preview = new StackPane(createPreviewImageView(image, PREVIEW_PAGE_WIDTH, PREVIEW_PAGE_HEIGHT));
            preview.getStyleClass().add("mock-document-page");
            preview.setMinWidth(PREVIEW_PAGE_WIDTH);
            preview.setPrefWidth(PREVIEW_PAGE_WIDTH);
            preview.setMaxWidth(PREVIEW_PAGE_WIDTH);
            preview.setMinHeight(PREVIEW_PAGE_HEIGHT);
            preview.setPrefHeight(PREVIEW_PAGE_HEIGHT);
            preview.setMaxHeight(PREVIEW_PAGE_HEIGHT);
            preview.setRotate(page.rotationDegrees);

            Label badge = new Label("BARCODE");
            badge.getStyleClass().add("barcode-preview-badge");
            StackPane.setAlignment(badge, Pos.TOP_CENTER);
            preview.getChildren().add(badge);
            return preview;
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
        Image image = resolvePageImage(page);
        if (image != null) {
            StackPane preview = new StackPane(createPreviewImageView(image, PREVIEW_PAGE_WIDTH, PREVIEW_PAGE_HEIGHT));
            preview.getStyleClass().add("mock-document-page");
            preview.setMinWidth(PREVIEW_PAGE_WIDTH);
            preview.setPrefWidth(PREVIEW_PAGE_WIDTH);
            preview.setMaxWidth(PREVIEW_PAGE_WIDTH);
            preview.setMinHeight(PREVIEW_PAGE_HEIGHT);
            preview.setPrefHeight(PREVIEW_PAGE_HEIGHT);
            preview.setMaxHeight(PREVIEW_PAGE_HEIGHT);
            preview.setRotate(page.rotationDegrees);
            return preview;
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
        if (page.cachedPreviewImage != null) {
            return page.cachedPreviewImage;
        }

        String imageContent = page.imageContent();
        if (imageContent.isBlank()) {
            return null;
        }

        page.cachedPreviewImage = decodeDataUriImage(imageContent);
        return page.cachedPreviewImage;
    }

    private Image decodeDataUriImage(String dataUri) {
        if (dataUri == null || dataUri.isBlank()) {
            return null;
        }

        int commaIndex = dataUri.indexOf(',');
        if (commaIndex < 0 || commaIndex >= dataUri.length() - 1) {
            return null;
        }

        try {
            String header = dataUri.substring(0, commaIndex).toLowerCase(Locale.ROOT);
            byte[] bytes = Base64.getDecoder().decode(dataUri.substring(commaIndex + 1));
            if (header.contains("image/png") || header.contains("image/jpeg") || header.contains("image/jpg")) {
                return new Image(new ByteArrayInputStream(bytes));
            }

            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
            if (bufferedImage != null) {
                return SwingFXUtils.toFXImage(bufferedImage, null);
            }

            return new Image(new ByteArrayInputStream(bytes));
        } catch (IllegalArgumentException exception) {
            return null;
        } catch (Exception exception) {
            return null;
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
            } else if (page.splitReasonAfter != null && !"Finish batch".equals(page.splitReasonAfter)) {
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

        if (page.barcode) {
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
        updateReviewSelectionCard(
                selectedPage == null ? null : findDocumentContainingPage(selectedPage),
                selectedPage,
                selectedPage == null ? -1 : getPageNumberInDocument(selectedPage)
        );
        renderReviewDocumentList();
        renderReviewPreview();
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

            HBox documentCard = new HBox(9);
            documentCard.setAlignment(Pos.CENTER_LEFT);
            documentCard.getStyleClass().addAll("document-tree-document-header", "document-tree-document-header-framed");

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

            documentBlock.getChildren().add(documentCard);

            if (!collapsedDocuments.contains(document.number)) {
                VBox pageStack = new VBox(18);
                pageStack.setAlignment(Pos.TOP_CENTER);
                pageStack.getStyleClass().add("document-tree-page-stack");
                for (int pageIndex = 0; pageIndex < document.pages.size(); pageIndex++) {
                    ScannedPage page = document.pages.get(pageIndex);
                    VBox pageCard = createReviewEmbeddedPageCard(page, pageIndex + 1);
                    pageStack.getChildren().add(pageCard);
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

        if (page == selectedPage) {
            card.getStyleClass().add("review-page-tray-item-selected");
        }

        if (page.needsRescan) {
            card.getStyleClass().add("review-page-tray-item-warning");
        }

        StackPane thumbnail = new StackPane();
        thumbnail.getStyleClass().add("review-page-tray-thumbnail");
        applyThumbnailClip(thumbnail, 28);
        Image image = resolvePageImage(page);
        if (image != null) {
            thumbnail.getChildren().add(createThumbnailImageView(image, 148, 214));
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

        Label number = new Label("Page " + pageNumber);
        number.getStyleClass().add("review-page-tray-number");
        number.setMaxWidth(Double.MAX_VALUE);
        number.setAlignment(Pos.CENTER);

        card.getChildren().addAll(thumbnail, number);
        card.setOnMouseClicked(event -> {
            selectedPage = page;
            refreshReviewWorkspace();
        });

        return card;
    }

    @FXML
    private void onOpenExportTypeDialog() {
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
        List<String> boxFiles = buildExportFiles();
        ObjectProperty<TiffExportType> selectedType = new SimpleObjectProperty<>(
                boxFiles.size() > 1 ? TiffExportType.MULTI_PAGE : TiffExportType.SINGLE_PAGE
        );

        Label title = new Label("TIFF Export");
        title.getStyleClass().add("exports-dialog-title");

        VBox header = new VBox(9, title);
        header.getStyleClass().add("exports-dialog-header");

        Label boxValue = new Label(getBoxId());
        boxValue.getStyleClass().add("exports-dialog-box-value");

        Label boxDetail = new Label("Only files from this box can be exported in this dialog.");
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
                "One combined TIFF",
                TiffExportType.MULTI_PAGE,
                selectedType
        );
        HBox.setHgrow(singlePageCard, Priority.ALWAYS);
        HBox.setHgrow(multiPageCard, Priority.ALWAYS);

        HBox typeRow = new HBox(18, singlePageCard, multiPageCard);
        typeRow.getStyleClass().add("exports-dialog-type-row");

        Label selectedFilesTitle = new Label("Files in box");
        selectedFilesTitle.getStyleClass().add("exports-dialog-files-title");

        Label selectedFilesCount = new Label(formatSelectedFileCount(boxFiles.size()));
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

        Label outputValue = new Label(buildOutputText(selectedType.get(), boxFiles.size()));
        outputValue.getStyleClass().add("exports-dialog-output-value");
        outputValue.setWrapText(false);
        outputValue.setMinHeight(Region.USE_PREF_SIZE);
        outputValue.setPrefWidth(420);
        outputValue.setMaxWidth(420);
        selectedType.addListener((observable, oldValue, newValue) ->
                outputValue.setText(buildOutputText(newValue, boxFiles.size()))
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
        exportButton.setOnAction(event -> stage.close());

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
            Label emptyState = new Label("No files available for this export.");
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

    private List<String> buildExportFiles() {
        int count = Math.max(1, documents.size());
        List<String> files = new ArrayList<>(count);
        for (int index = 1; index <= count; index++) {
            files.add("file_" + String.format(Locale.US, "%03d", index));
        }
        return files;
    }

    private String formatSelectedFileCount(int fileCount) {
        return fileCount + " " + (fileCount == 1 ? "file" : "files");
    }

    private String buildOutputText(TiffExportType type, int selectedFileCount) {
        return switch (type) {
            case SINGLE_PAGE -> selectedFileCount + " separate .tiff " + (selectedFileCount == 1 ? "file" : "files") + " will be generated";
            case MULTI_PAGE -> "All selected files will be combined into one .tiff file";
        };
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
        applyThumbnailClip(thumbnail, 28);
        Image image = resolvePageImage(page);
        if (image != null) {
            thumbnail.getChildren().add(createThumbnailImageView(image, 148, 214));
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

        Label number = new Label(labelText);
        number.getStyleClass().add(page.barcode ? "page-tray-barcode-split-label" : "page-tray-number");
        number.setMaxWidth(Double.MAX_VALUE);
        number.setAlignment(Pos.CENTER);

        card.getChildren().addAll(thumbnail, number);
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
        private final Set<Integer> collapsedDocuments = new HashSet<>();
        private final double previewZoomMultiplier;
        private final double previewTranslateX;
        private final double previewTranslateY;

        private ScanSnapshot(
                List<ScannedPage> pages,
                int selectedPageReferenceId,
                int nextReferenceId,
                int nextFileId,
                Set<Integer> collapsedDocuments,
                double previewZoomMultiplier,
                double previewTranslateX,
                double previewTranslateY
        ) {
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
        private final int documentNumber;
        private final int rotationDegrees;
        private final boolean needsRescan;
        private final String splitReasonAfter;
        private final String sourceReference;
        private final String displayContent;
        private final String previewContent;

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
        }

        private ScannedPage toScannedPage() {
            ScannedPage page = new ScannedPage(
                    referenceId,
                    fileId,
                    barcode,
                    needsRescan,
                    sourceReference,
                    displayContent,
                    previewContent
            );
            page.documentNumber = documentNumber;
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

        private int documentNumber;
        private int rotationDegrees;
        private boolean needsRescan;
        private String splitReasonAfter;
        private transient Image cachedPreviewImage;

        private ScannedPage(int referenceId, int fileId, boolean barcode, boolean needsRescan) {
            this(referenceId, fileId, barcode, needsRescan, "", "", "");
        }

        private ScannedPage(
                int referenceId,
                int fileId,
                boolean barcode,
                boolean needsRescan,
                String sourceReference,
                String displayContent,
                String previewContent
        ) {
            this.referenceId = referenceId;
            this.fileId = fileId;
            this.barcode = barcode;
            this.needsRescan = needsRescan;
            this.sourceReference = sourceReference == null ? "" : sourceReference;
            this.displayContent = displayContent == null ? "" : displayContent;
            this.previewContent = previewContent == null ? "" : previewContent;
            this.rotationDegrees = 0;
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
}




