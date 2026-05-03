package easv.gui.controller.user;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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

    private final List<ScannedPage> allPages = new ArrayList<>();
    private final List<ScannedPage> pendingPages = new ArrayList<>();
    private final List<DocumentGroup> documents = new ArrayList<>();
    private final Set<Integer> collapsedDocuments = new HashSet<>();
    private final Deque<ScanSnapshot> undoStack = new ArrayDeque<>();

    private final DoubleProperty previewZoomMultiplier = new SimpleDoubleProperty(1.0);

    private int nextReferenceId = 1;
    private int nextFileId = 1;

    private double previewTranslateX = 0;
    private double previewTranslateY = 0;
    private double previewDragStartX = 0;
    private double previewDragStartY = 0;
    private double previewTranslateStartX = 0;
    private double previewTranslateStartY = 0;

    private StackPane currentPreviewWrapper;

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
        configurePreviewInteractions();
        updatePreviewZoomLabel();
        updateUndoButtonState();
        hideFinishReviewModal();
        hideSubmitConfirmationModal();
        showSetupView();
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
            profileInfoMetadataLabel.setText("Metadata required: —");
            profileInfoQaLabel.setText("QA required: —");
            profileInfoSplittingLabel.setText("Splitting method: —");
            return;
        }

        profileInfoTitleLabel.setText(selectedProfile);

        switch (selectedProfile) {
            case "Building Archive" -> {
                profileInfoMetadataLabel.setText("Metadata required: Yes");
                profileInfoQaLabel.setText("QA required: Yes");
                profileInfoSplittingLabel.setText("Splitting method: Manual or barcode");
            }
            case "Technical Drawings" -> {
                profileInfoMetadataLabel.setText("Metadata required: Yes");
                profileInfoQaLabel.setText("QA required: No");
                profileInfoSplittingLabel.setText("Splitting method: Single document");
            }
            case "Court Records" -> {
                profileInfoMetadataLabel.setText("Metadata required: Yes");
                profileInfoQaLabel.setText("QA required: Yes");
                profileInfoSplittingLabel.setText("Splitting method: Barcode");
            }
            case "Standard Scan" -> {
                profileInfoMetadataLabel.setText("Metadata required: No");
                profileInfoQaLabel.setText("QA required: No");
                profileInfoSplittingLabel.setText("Splitting method: Manual");
            }
            default -> {
                profileInfoMetadataLabel.setText("Metadata required: Unknown");
                profileInfoQaLabel.setText("QA required: Unknown");
                profileInfoSplittingLabel.setText("Splitting method: Unknown");
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

        ScannedPage scannedPage = new ScannedPage(
                nextReferenceId,
                nextFileId,
                barcode,
                needsRescan
        );

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
                    DocumentGroup document = createDocument(documentNumber, "Barcode split", currentDocumentPages);
                    documents.add(document);
                    documentNumber++;
                    currentDocumentPages.clear();
                }

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
                documents.size() + " documents · "
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

        if (reviewDocumentsValueLabel != null) {
            reviewDocumentsValueLabel.setText(
                    documents.size() + " · "
                            + getNormalPageCount()
                            + " pages · submitted for QA"
            );
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
        String profile = profileComboBox.getValue();
        return profile == null || profile.isBlank() ? "No profile" : profile;
    }

    private String getBoxId() {
        String boxId = boxIdTextField.getText();
        return boxId == null || boxId.isBlank() ? "No box ID" : boxId.trim();
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
                    ScannedPage page = document.pages.get(pageIndex);
                    HBox pageRow = createDocumentTreePageRow(page, pageIndex + 1);
                    documentBlock.getChildren().add(pageRow);
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

        Label warning = new Label(document.hasPagesNeedingRescan() ? "⚠" : "");
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

        Label drag = new Label("⋮⋮");
        drag.getStyleClass().add("document-tree-page-drag");

        Label name = new Label("Page " + pageNumberInDocument);
        name.getStyleClass().add("document-tree-page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label warning = new Label(page.needsRescan ? "⚠" : "");
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

        Label copy = new Label("Place a file in the scanner and click “Scan Next File”.");
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
            Label warning = new Label("⚠ Marked for rescan");
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
            VBox lines = new VBox(4);
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
            return "⚠";
        }

        if (page.barcode) {
            return "▥";
        }

        if (page.splitReasonAfter != null && !"Finish batch".equals(page.splitReasonAfter)) {
            return "↵";
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

            Label warning = new Label(document.hasPagesNeedingRescan() ? "⚠" : "");
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

        VBox lines = new VBox(4);
        lines.setAlignment(Pos.TOP_LEFT);
        lines.getChildren().addAll(
                createLine("tray-line-dark", 27, 3),
                createLine("tray-line-light", 42, 3),
                createLine("tray-line-light", 36, 3),
                createLine("tray-line-light", 30, 3)
        );

        thumbnail.getChildren().add(lines);

        Label status = new Label(page.needsRescan ? "⚠" : "");
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

        private PageSnapshot(ScannedPage page) {
            this.referenceId = page.referenceId;
            this.fileId = page.fileId;
            this.barcode = page.barcode;
            this.documentNumber = page.documentNumber;
            this.rotationDegrees = page.rotationDegrees;
            this.needsRescan = page.needsRescan;
            this.splitReasonAfter = page.splitReasonAfter;
        }

        private ScannedPage toScannedPage() {
            ScannedPage page = new ScannedPage(referenceId, fileId, barcode, needsRescan);
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

    private static final class ScannedPage {
        private final int referenceId;
        private final int fileId;
        private final boolean barcode;

        private int documentNumber;
        private int rotationDegrees;
        private boolean needsRescan;
        private String splitReasonAfter;

        private ScannedPage(int referenceId, int fileId, boolean barcode, boolean needsRescan) {
            this.referenceId = referenceId;
            this.fileId = fileId;
            this.barcode = barcode;
            this.needsRescan = needsRescan;
            this.rotationDegrees = 0;
        }

        private String referenceIdLabel() {
            return "REF-" + String.format("%03d", referenceId);
        }

        private String fileName() {
            if (barcode) {
                return "barcode_split_" + String.format("%02d", referenceId) + ".tiff";
            }

            return "scan_file_" + String.format("%02d", fileId) + ".tiff";
        }
    }
}