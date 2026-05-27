package easv.gui.controller.user;

import easv.be.Document;
import easv.be.PageImage;
import easv.be.ScanProfile;
import easv.bll.QAService;
import easv.bll.TiffImageSupport;
import easv.bll.TiffExportManager;
import easv.gui.controller.util.BackgroundExecutor;
import easv.gui.UserPortalModel;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javafx.util.Duration;

public class AssignedQaController {
    private static final List<String> QA_ROTATION_OPTIONS = List.of("0\u00B0", "90\u00B0", "180\u00B0", "270\u00B0");

    private static final double QA_PREVIEW_PAGE_WIDTH = 500;
    private static final double QA_PREVIEW_PAGE_HEIGHT = 560;
    private static final double QA_PREVIEW_SAFE_HORIZONTAL_PADDING = 132;
    private static final double QA_PREVIEW_SAFE_VERTICAL_PADDING = 72;

    private static final double MIN_QA_PREVIEW_ZOOM = 0.50;
    private static final double MAX_QA_PREVIEW_ZOOM = 2.50;
    private static final double QA_PREVIEW_ZOOM_STEP = 0.10;
    private static final double QA_PREVIEW_NUDGE_AMOUNT = 36;

    @FXML private ScrollPane assignedQaListView;
    @FXML private BorderPane qaReviewWorkspaceView;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private HBox assignedQaFilterPanel;
    @FXML private TilePane qaCardListContainer;

    @FXML private Label qaBoxIdLabel;
    @FXML private Label qaProfileLabel;
    @FXML private Label qaProgressLabel;
    @FXML private Label reviewStatusBadge;
    @FXML private Label qaSidebarSubtitleLabel;

    @FXML private VBox qaDocumentTreeContainer;
    @FXML private Button qaDocumentGridViewButton;
    @FXML private Button qaDocumentListViewButton;
    @FXML private Label selectedQaPageTitleLabel;
    @FXML private Label selectedQaPageSubtitleLabel;
    @FXML private StackPane qaPreviewHost;
    @FXML private Label qaTrayCountLabel;
    @FXML private HBox qaPageTrayContainer;
    @FXML private Label zoomPercentLabel;

    @FXML private Label currentPageStatusLabel;
    @FXML private CheckBox pageReadableCheckBox;
    @FXML private CheckBox rotationCorrectCheckBox;
    @FXML private CheckBox splitCorrectCheckBox;
    @FXML private CheckBox pageCountCorrectCheckBox;
    @FXML private TextArea qaCommentTextArea;
    @FXML private ComboBox<String> qaActionScopeComboBox;
    @FXML private ComboBox<String> qaRotationComboBox;
    @FXML private Button qaRotateLeftButton;
    @FXML private Button qaRotateRightButton;

    private final List<QaAssignment> allAssignments = new ArrayList<>();
    private final List<QaDocument> reviewDocuments = new ArrayList<>();
    private final TiffExportManager tiffExportManager = new TiffExportManager();

    private final DoubleProperty previewZoomMultiplier = new SimpleDoubleProperty(1.0);

    private QaAssignment selectedAssignment;

    private int selectedDocumentIndex = 0;
    private int selectedPageIndex = 0;

    private double previewTranslateX = 0;
    private double previewTranslateY = 0;
    private double previewDragStartX = 0;
    private double previewDragStartY = 0;
    private double previewTranslateStartX = 0;
    private double previewTranslateStartY = 0;
    private StackPane currentQaPreviewWrapper;

    private boolean syncingQaControls = false;
    private boolean syncingQaRotationComboBox = false;
    private boolean qaDocumentListView = false;
    private UserPortalModel portalModel;
    private final PauseTransition qaAutoSaveDelay = new PauseTransition(Duration.millis(400));

    public void setPortalModel(UserPortalModel portalModel) {
        this.portalModel = portalModel;
        loadAssignments();
        renderAssignments();
    }

    @FXML
    private void initialize() {
        configureFilters();
        configureAssignedQaListLayout();
        configureQaRotation();
        configureQaControls();
        configureQaPreviewInteractions();
        loadAssignments();
        renderAssignments();
        showAssignedQaListView();
    }

    // =========================================================
    // FILTER SETUP
    // =========================================================

    private void configureFilters() {
        statusFilterComboBox.getItems().setAll(
                "All Statuses",
                "Waiting for QA",
                "In Review",
                "Issues Found",
                "QA Completed"
        );
        statusFilterComboBox.getSelectionModel().selectFirst();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> renderAssignments());
        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> renderAssignments());
        UserPortalUi.configureDateFilterPicker(fromDatePicker);
        UserPortalUi.configureDateFilterPicker(toDatePicker);
        fromDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> renderAssignments());
        toDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> renderAssignments());
    }

    private void configureAssignedQaListLayout() {
        if (assignedQaFilterPanel != null && qaCardListContainer != null) {
            qaCardListContainer.prefWidthProperty().bind(assignedQaFilterPanel.widthProperty());
            qaCardListContainer.maxWidthProperty().bind(assignedQaFilterPanel.widthProperty());
        }
        updateQaDocumentViewToggleButtons();
    }

    @FXML
    private void onShowQaDocumentGridView() {
        qaDocumentListView = false;
        updateQaDocumentViewToggleButtons();
        renderQaDocumentTree();
    }

    @FXML
    private void onShowQaDocumentListView() {
        qaDocumentListView = true;
        updateQaDocumentViewToggleButtons();
        renderQaDocumentTree();
    }

    private void updateQaDocumentViewToggleButtons() {
        setDocumentViewButtonActive(qaDocumentGridViewButton, !qaDocumentListView);
        setDocumentViewButtonActive(qaDocumentListViewButton, qaDocumentListView);
    }

    private void setDocumentViewButtonActive(Button button, boolean active) {
        if (button == null) {
            return;
        }

        button.getStyleClass().remove("document-tree-view-toggle-button-active");
        if (active) {
            button.getStyleClass().add("document-tree-view-toggle-button-active");
        }
    }

    // =========================================================
    // QA CONTROL SETUP
    // =========================================================

    private void configureQaControls() {
        if (qaActionScopeComboBox != null) {
            qaActionScopeComboBox.getItems().setAll("Selected Page", "This Document");
            qaActionScopeComboBox.getSelectionModel().selectFirst();
        }

        if (pageReadableCheckBox != null) {
            pageReadableCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (syncingQaControls) {
                    return;
                }

                QaPage page = getSelectedQaPage();
                if (page != null) {
                    page.pageReadable = newValue;
                    scheduleQaProgressSave();
                }
            });
        }

        if (rotationCorrectCheckBox != null) {
            rotationCorrectCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (syncingQaControls) {
                    return;
                }

                QaPage page = getSelectedQaPage();
                if (page != null) {
                    page.rotationCorrect = newValue;
                    scheduleQaProgressSave();
                }
            });
        }

        if (splitCorrectCheckBox != null) {
            splitCorrectCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (syncingQaControls) {
                    return;
                }

                QaPage page = getSelectedQaPage();
                if (page != null) {
                    page.splitCorrect = newValue;
                    scheduleQaProgressSave();
                }
            });
        }

        if (pageCountCorrectCheckBox != null) {
            pageCountCorrectCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (syncingQaControls) {
                    return;
                }

                QaPage page = getSelectedQaPage();
                if (page != null) {
                    page.pageCountCorrect = newValue;
                    scheduleQaProgressSave();
                }
            });
        }

        qaCommentTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (syncingQaControls) {
                return;
            }

            QaPage page = getSelectedQaPage();
            if (page != null) {
                page.comment = newValue == null ? "" : newValue;
                scheduleQaProgressSave();
            }
        });

        qaAutoSaveDelay.setOnFinished(event -> persistQaProgressAsync());
    }

    private void configureQaRotation() {
        if (qaRotationComboBox == null) {
            return;
        }

        qaRotationComboBox.getItems().setAll(QA_ROTATION_OPTIONS);
        qaRotationComboBox.setEditable(true);
        qaRotationComboBox.setPromptText("Enter rotation in degrees");
        syncQaRotationComboBox();
        qaRotationComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isBlank()) {
                return;
            }

            applyQaRotationSelection(newValue);
        });

        if (qaRotationComboBox.getEditor() != null) {
            qaRotationComboBox.getEditor().setOnAction(event -> commitCustomQaRotation());
            qaRotationComboBox.getEditor().focusedProperty().addListener((observable, oldValue, focused) -> {
                if (!focused) {
                    commitCustomQaRotation();
                }
            });
        }
    }

    private void commitCustomQaRotation() {
        if (qaRotationComboBox == null || qaRotationComboBox.getEditor() == null) {
            return;
        }

        String editorValue = qaRotationComboBox.getEditor().getText();
        if (editorValue == null || editorValue.isBlank()) {
            syncQaRotationComboBox();
            return;
        }

        qaRotationComboBox.setValue(formatRotationDegrees(parseRotationDegrees(editorValue)));
    }

    private void applyQaRotationSelection(String newValue) {
        if (syncingQaRotationComboBox) {
            return;
        }

        QaPage page = getSelectedQaPage();
        if (page == null) {
            syncQaRotationComboBox();
            return;
        }

        int newRotationDegrees = parseRotationDegrees(newValue);
        if (page.rotationDegrees == newRotationDegrees) {
            syncQaRotationComboBox();
            return;
        }

        page.rotationDegrees = newRotationDegrees;
        renderQaPreview();
        updateQaRotationButtons();
        scheduleQaProgressSave();
    }

    // =========================================================
    // QA PREVIEW INTERACTION SETUP
    // =========================================================

    private void configureQaPreviewInteractions() {
        Rectangle previewClip = new Rectangle();
        previewClip.widthProperty().bind(qaPreviewHost.widthProperty());
        previewClip.heightProperty().bind(qaPreviewHost.heightProperty());
        qaPreviewHost.setClip(previewClip);

        qaPreviewHost.setFocusTraversable(true);

        qaPreviewHost.widthProperty().addListener((observable, oldValue, newValue) -> clampQaPreviewTranslation());
        qaPreviewHost.heightProperty().addListener((observable, oldValue, newValue) -> clampQaPreviewTranslation());

        qaPreviewHost.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> qaPreviewHost.requestFocus());
        qaPreviewHost.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleQaPreviewMousePressed);
        qaPreviewHost.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleQaPreviewMouseDragged);
        qaPreviewHost.addEventFilter(ScrollEvent.SCROLL, this::handleQaPreviewScroll);
        qaPreviewHost.addEventFilter(ZoomEvent.ZOOM, this::handleQaPreviewZoom);
        qaPreviewHost.addEventFilter(KeyEvent.KEY_PRESSED, this::handleQaPreviewKeyPressed);

        previewZoomMultiplier.addListener((observable, oldValue, newValue) -> {
            updateQaZoomLabel();
            clampQaPreviewTranslation();
        });

        updateQaZoomLabel();
    }

    private void handleQaPreviewMousePressed(MouseEvent event) {
        if (getSelectedQaPage() == null || currentQaPreviewWrapper == null) {
            return;
        }

        qaPreviewHost.requestFocus();

        previewDragStartX = event.getSceneX();
        previewDragStartY = event.getSceneY();
        previewTranslateStartX = previewTranslateX;
        previewTranslateStartY = previewTranslateY;

        event.consume();
    }

    private void handleQaPreviewMouseDragged(MouseEvent event) {
        if (getSelectedQaPage() == null || currentQaPreviewWrapper == null) {
            return;
        }

        previewTranslateX = previewTranslateStartX + event.getSceneX() - previewDragStartX;
        previewTranslateY = previewTranslateStartY + event.getSceneY() - previewDragStartY;

        clampQaPreviewTranslation();

        event.consume();
    }

    private void handleQaPreviewScroll(ScrollEvent event) {
        if (getSelectedQaPage() == null) {
            return;
        }

        double deltaY = event.getDeltaY();

        if (deltaY == 0) {
            return;
        }

        double zoomDelta = deltaY > 0 ? QA_PREVIEW_ZOOM_STEP : -QA_PREVIEW_ZOOM_STEP;
        zoomQaPreviewBy(zoomDelta);

        event.consume();
    }

    private void handleQaPreviewZoom(ZoomEvent event) {
        if (getSelectedQaPage() == null) {
            return;
        }

        setQaPreviewZoom(previewZoomMultiplier.get() * event.getZoomFactor());

        event.consume();
    }

    private void handleQaPreviewKeyPressed(KeyEvent event) {
        if (getSelectedQaPage() == null) {
            return;
        }

        KeyCode code = event.getCode();

        if (code == KeyCode.PLUS || code == KeyCode.ADD || code == KeyCode.EQUALS) {
            zoomQaPreviewBy(QA_PREVIEW_ZOOM_STEP);
            event.consume();
            return;
        }

        if (code == KeyCode.MINUS || code == KeyCode.SUBTRACT) {
            zoomQaPreviewBy(-QA_PREVIEW_ZOOM_STEP);
            event.consume();
            return;
        }

        if (code == KeyCode.DIGIT0 || code == KeyCode.NUMPAD0) {
            onResetView();
            event.consume();
            return;
        }

        if (code == KeyCode.UP) {
            nudgeQaPreview(0, -QA_PREVIEW_NUDGE_AMOUNT);
            event.consume();
            return;
        }

        if (code == KeyCode.DOWN) {
            nudgeQaPreview(0, QA_PREVIEW_NUDGE_AMOUNT);
            event.consume();
            return;
        }

        if (code == KeyCode.LEFT) {
            nudgeQaPreview(-QA_PREVIEW_NUDGE_AMOUNT, 0);
            event.consume();
            return;
        }

        if (code == KeyCode.RIGHT) {
            nudgeQaPreview(QA_PREVIEW_NUDGE_AMOUNT, 0);
            event.consume();
        }
    }

    private void zoomQaPreviewBy(double zoomDelta) {
        setQaPreviewZoom(previewZoomMultiplier.get() + zoomDelta);
    }

    private void setQaPreviewZoom(double zoom) {
        previewZoomMultiplier.set(clamp(zoom, MIN_QA_PREVIEW_ZOOM, MAX_QA_PREVIEW_ZOOM));
        persistSelectedQaViewState();
    }

    private void nudgeQaPreview(double deltaX, double deltaY) {
        if (getSelectedQaPage() == null || currentQaPreviewWrapper == null) {
            return;
        }

        qaPreviewHost.requestFocus();

        previewTranslateX += deltaX;
        previewTranslateY += deltaY;

        clampQaPreviewTranslation();
    }

    private void resetQaPreviewViewState() {
        previewZoomMultiplier.set(1.0);
        previewTranslateX = 0;
        previewTranslateY = 0;

        if (currentQaPreviewWrapper != null) {
            currentQaPreviewWrapper.setTranslateX(0);
            currentQaPreviewWrapper.setTranslateY(0);
        }

        persistSelectedQaViewState();
        updateQaZoomLabel();
    }

    private void clampQaPreviewTranslation() {
        if (currentQaPreviewWrapper == null || qaPreviewHost == null) {
            return;
        }

        double scale = currentQaPreviewWrapper.getScaleX();

        if (scale <= 0) {
            scale = 1;
        }

        double scaledWidth = QA_PREVIEW_PAGE_WIDTH * scale;
        double scaledHeight = QA_PREVIEW_PAGE_HEIGHT * scale;

        double hostWidth = Math.max(1, qaPreviewHost.getWidth());
        double hostHeight = Math.max(1, qaPreviewHost.getHeight());

        double maxX = Math.abs(hostWidth - scaledWidth) / 2;
        double maxY = Math.abs(hostHeight - scaledHeight) / 2;

        previewTranslateX = clamp(previewTranslateX, -maxX, maxX);
        previewTranslateY = clamp(previewTranslateY, -maxY, maxY);

        currentQaPreviewWrapper.setTranslateX(previewTranslateX);
        currentQaPreviewWrapper.setTranslateY(previewTranslateY);
        persistSelectedQaViewState();
    }

    private void updateQaZoomLabel() {
        if (zoomPercentLabel != null) {
            zoomPercentLabel.setText(Math.round(previewZoomMultiplier.get() * 100) + "%");
        }
    }

    private void loadSelectedQaViewState() {
        QaPage page = getSelectedQaPage();
        if (page == null) {
            previewZoomMultiplier.set(1.0);
            previewTranslateX = 0;
            previewTranslateY = 0;
            updateQaZoomLabel();
            return;
        }

        previewZoomMultiplier.set(page.previewZoomMultiplier);
        previewTranslateX = page.previewTranslateX;
        previewTranslateY = page.previewTranslateY;
        updateQaZoomLabel();
    }

    private void persistSelectedQaViewState() {
        QaPage page = getSelectedQaPage();
        if (page == null) {
            return;
        }

        page.previewZoomMultiplier = previewZoomMultiplier.get();
        page.previewTranslateX = previewTranslateX;
        page.previewTranslateY = previewTranslateY;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    // =========================================================
    // ASSIGNMENT DATA
    // =========================================================

    private void loadAssignments() {
        allAssignments.clear();

        if (portalModel == null) {
            return;
        }
        for (QAService.QaAssignmentSnapshot assignment : portalModel.fetchAssignedQaAssignments()) {
            allAssignments.add(new QaAssignment(
                    assignment.reviewId(),
                    assignment.sessionId(),
                    assignment.boxId(),
                    assignment.profileName(),
                    assignment.scannedByName(),
                    assignment.documents().size(),
                    assignment.totalPages(),
                    assignment.submittedAt().atZone(ZoneId.systemDefault()).toLocalDate(),
                    formatAssignedTimeLabel(assignment.submittedAt()),
                    assignment.reviewedPages(),
                    toQaStatus(assignment.status(), assignment.issueCount()),
                    assignment.issueCount(),
                    assignment.documents()
            ));
        }
    }

    private QaStatus toQaStatus(QAService.QaReviewStatus status, int issueCount) {
        if (status == null) {
            return QaStatus.WAITING_FOR_QA;
        }
        return switch (status) {
            case WAITING_FOR_QA -> QaStatus.WAITING_FOR_QA;
            case IN_REVIEW -> issueCount > 0 ? QaStatus.ISSUES_FOUND : QaStatus.IN_REVIEW;
            case APPROVED -> QaStatus.QA_COMPLETED;
            case REJECTED -> QaStatus.ISSUES_FOUND;
        };
    }

    private String formatAssignedTimeLabel(java.time.Instant submittedAt) {
        if (submittedAt == null) {
            return "-";
        }
        return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .format(submittedAt.atZone(ZoneId.systemDefault()));
    }

    // =========================================================
    // VIEW SWITCHING
    // =========================================================

    private void showAssignedQaListView() {
        assignedQaListView.setVisible(true);
        assignedQaListView.setManaged(true);

        qaReviewWorkspaceView.setVisible(false);
        qaReviewWorkspaceView.setManaged(false);
    }

    private void showQaReviewWorkspaceView() {
        assignedQaListView.setVisible(false);
        assignedQaListView.setManaged(false);

        qaReviewWorkspaceView.setVisible(true);
        qaReviewWorkspaceView.setManaged(true);

        qaPreviewHost.requestFocus();
    }

    // =========================================================
    // ASSIGNED QA LIST FILTERING
    // =========================================================

    private boolean matchesSearch(QaAssignment assignment) {
        String query = searchField.getText();

        if (query == null || query.isBlank()) {
            return true;
        }

        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);

        return assignment.boxId.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || assignment.profile.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || assignment.scannedBy.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private boolean matchesStatus(QaAssignment assignment) {
        String selectedStatus = statusFilterComboBox.getValue();

        if (selectedStatus == null || selectedStatus.equals("All Statuses")) {
            return true;
        }

        return assignment.status.displayName.equals(selectedStatus);
    }

    private boolean matchesFromDate(QaAssignment assignment) {
        LocalDate fromDate = fromDatePicker.getValue();
        return fromDate == null || !assignment.assignedDate.isBefore(fromDate);
    }

    private boolean matchesToDate(QaAssignment assignment) {
        LocalDate toDate = toDatePicker.getValue();
        return toDate == null || !assignment.assignedDate.isAfter(toDate);
    }

    // =========================================================
    // ASSIGNED QA LIST RENDERING
    // =========================================================

    private void renderAssignments() {
        qaCardListContainer.getChildren().clear();

        List<QaAssignment> filteredAssignments = allAssignments.stream()
                .filter(this::matchesSearch)
                .filter(this::matchesStatus)
                .filter(this::matchesFromDate)
                .filter(this::matchesToDate)
                .collect(Collectors.toList());

        if (filteredAssignments.isEmpty()) {
            qaCardListContainer.getChildren().add(createEmptyState());
            return;
        }

        for (QaAssignment assignment : filteredAssignments) {
            qaCardListContainer.getChildren().add(createAssignmentCard(assignment));
        }
    }

    private VBox createEmptyState() {
        VBox emptyState = new VBox(6);
        emptyState.getStyleClass().add("assigned-qa-empty-state");

        Label title = new Label("No assigned QA sessions found");
        title.getStyleClass().add("assigned-qa-empty-title");

        Label copy = new Label("Try changing your search or filters.");
        copy.getStyleClass().add("assigned-qa-empty-copy");

        emptyState.getChildren().addAll(title, copy);

        return emptyState;
    }

    // =========================================================
    // ASSIGNED QA CARD RENDERING
    // =========================================================

    private VBox createAssignmentCard(QaAssignment assignment) {
        VBox card = new VBox(18);
        card.getStyleClass().add("assigned-qa-card");
        card.setMinWidth(280);
        card.setPrefWidth(280);
        card.setMaxWidth(280);
        card.setMinHeight(350);
        card.setPrefHeight(350);
        card.setMaxHeight(350);

        HBox titleRow = new HBox(15);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setMaxWidth(Double.MAX_VALUE);

        Label boxIdLabel = new Label(assignment.boxId);
        boxIdLabel.getStyleClass().add("assigned-qa-card-title");

        Label statusBadge = new Label(getAssignmentStatusBadgeText(assignment.status));
        statusBadge.getStyleClass().addAll(
                "assigned-qa-status-badge",
                getAssignmentStatusBadgeStyleClass(assignment.status)
        );

        titleRow.getChildren().addAll(boxIdLabel, statusBadge);

        Label profileLabel = new Label(assignment.profile);
        profileLabel.getStyleClass().add("assigned-qa-card-profile");

        Label scannedByLabel = new Label("Scanned by " + assignment.scannedBy);
        scannedByLabel.getStyleClass().add("assigned-qa-card-meta");

        Label documentSummaryLabel = new Label(
                assignment.documentCount + " document" + (assignment.documentCount == 1 ? "" : "s")
                        + " \u00B7 "
                        + assignment.totalPages + " pages"
        );
        documentSummaryLabel.getStyleClass().add("assigned-qa-card-detail-strong");

        Label assignedLabel = new Label("Assigned: " + assignment.assignedTimeLabel);
        assignedLabel.getStyleClass().add("assigned-qa-card-detail");

        Label progressLabel = new Label(
                "Progress: " + assignment.reviewedPages + " / " + assignment.totalPages + " pages reviewed"
        );
        progressLabel.getStyleClass().add("assigned-qa-card-detail");
        progressLabel.setWrapText(true);

        if (assignment.status == QaStatus.ISSUES_FOUND && assignment.issueCount > 0) {
            progressLabel.setText(progressLabel.getText() + " \u00B7 " + assignment.issueCount + " issues found");
            progressLabel.getStyleClass().add("assigned-qa-card-issues");
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        StackPane progressBar = createAssignmentProgressBar(assignment);

        Button actionButton = new Button(getAssignmentActionButtonText(assignment.status));
        actionButton.getStyleClass().add(getAssignmentActionButtonStyleClass(assignment.status));
        actionButton.setMaxWidth(Double.MAX_VALUE);
        actionButton.setMinHeight(40);
        actionButton.setPrefHeight(40);
        actionButton.setOnAction(event -> onOpenAssignment(assignment));

        card.getChildren().addAll(
                titleRow,
                profileLabel,
                scannedByLabel,
                documentSummaryLabel,
                assignedLabel,
                progressLabel,
                spacer,
                progressBar,
                actionButton
        );

        return card;
    }
    // =========================================================
    // ASSIGNED QA PROGRESS BAR
    // =========================================================

    private StackPane createAssignmentProgressBar(QaAssignment assignment) {
        StackPane progressTrack = new StackPane();
        progressTrack.setAlignment(Pos.CENTER_LEFT);
        progressTrack.getStyleClass().add("assigned-qa-progress-track");
        progressTrack.setMinHeight(10);
        progressTrack.setPrefHeight(10);
        progressTrack.setMaxHeight(10);
        progressTrack.setMaxWidth(Double.MAX_VALUE);

        Region progressFill = new Region();
        progressFill.getStyleClass().addAll(
                "assigned-qa-progress-fill",
                getAssignmentProgressFillStyleClass(assignment)
        );
        progressFill.setMinHeight(10);
        progressFill.setPrefHeight(10);
        progressFill.setMaxHeight(10);

        StackPane.setAlignment(progressFill, Pos.CENTER_LEFT);

        DoubleBinding fillWidthBinding = Bindings.createDoubleBinding(
                () -> {
                    double trackWidth = progressTrack.getWidth();
                    double progressPercent = getAssignmentProgressPercent(assignment);
                    return Math.max(0, trackWidth * progressPercent);
                },
                progressTrack.widthProperty()
        );

        progressFill.prefWidthProperty().bind(fillWidthBinding);
        progressFill.minWidthProperty().bind(fillWidthBinding);
        progressFill.maxWidthProperty().bind(fillWidthBinding);

        progressTrack.getChildren().add(progressFill);

        return progressTrack;
    }

    private double getAssignmentProgressPercent(QaAssignment assignment) {
        if (assignment == null || assignment.totalPages <= 0) {
            return 0.0;
        }

        if (assignment.status == QaStatus.QA_COMPLETED) {
            return 1.0;
        }

        double progress = (double) assignment.reviewedPages / (double) assignment.totalPages;

        if (progress < 0.0) {
            return 0.0;
        }

        if (progress > 1.0) {
            return 1.0;
        }

        return progress;
    }

    private String getAssignmentProgressFillStyleClass(QaAssignment assignment) {
        if (assignment == null) {
            return "assigned-qa-progress-none";
        }

        if (assignment.status == QaStatus.QA_COMPLETED) {
            return "assigned-qa-progress-complete";
        }

        if (assignment.reviewedPages <= 0) {
            return "assigned-qa-progress-none";
        }

        if (assignment.status == QaStatus.ISSUES_FOUND) {
            return "assigned-qa-progress-issues";
        }

        return "assigned-qa-progress-review";
    }

    // =========================================================
    // ASSIGNED QA STATUS AND ACTION HELPERS
    // =========================================================

    private String getAssignmentStatusBadgeText(QaStatus status) {
        return switch (status) {
            case WAITING_FOR_QA -> "Waiting for QA";
            case IN_REVIEW -> "In Review";
            case ISSUES_FOUND -> "Issues Found";
            case QA_COMPLETED -> "QA Completed";
        };
    }

    private String getAssignmentStatusBadgeStyleClass(QaStatus status) {
        return switch (status) {
            case WAITING_FOR_QA -> "assigned-qa-status-waiting";
            case IN_REVIEW -> "assigned-qa-status-review";
            case ISSUES_FOUND -> "assigned-qa-status-issues";
            case QA_COMPLETED -> "assigned-qa-status-complete";
        };
    }

    private String getAssignmentActionButtonText(QaStatus status) {
        return switch (status) {
            case WAITING_FOR_QA -> "Start QA";
            case IN_REVIEW, ISSUES_FOUND -> "Continue QA";
            case QA_COMPLETED -> "View Review";
        };
    }

    private String getAssignmentActionButtonStyleClass(QaStatus status) {
        return switch (status) {
            case QA_COMPLETED -> "assigned-qa-secondary-button";
            case WAITING_FOR_QA, IN_REVIEW, ISSUES_FOUND -> "assigned-qa-primary-button";
        };
    }

    // =========================================================
    // OPEN QA REVIEW WORKSPACE
    // =========================================================

    private void onOpenAssignment(QaAssignment assignment) {
        selectedAssignment = assignment;

        if (selectedAssignment.status == QaStatus.WAITING_FOR_QA) {
            selectedAssignment.status = QaStatus.IN_REVIEW;
            if (portalModel != null) {
                portalModel.markQaAssignmentStarted(selectedAssignment.reviewId);
            }
        }

        loadQaReviewDataFromAssignment(selectedAssignment);
        refreshQaReviewWorkspace();
        showQaReviewWorkspaceView();
    }

    private void loadQaReviewDataFromAssignment(QaAssignment assignment) {
        reviewDocuments.clear();

        selectedDocumentIndex = 0;
        selectedPageIndex = 0;
        resetQaPreviewViewState();

        int selectedGlobalPage = 1;
        for (QAService.QaDocumentSnapshot submittedDocument : assignment.submittedDocuments) {
            QaDocument document = new QaDocument(submittedDocument.name());

            for (QAService.QaPageSnapshot submittedPage : submittedDocument.pages()) {
                QaPage page = new QaPage(submittedPage.pageNumber(), submittedPage.globalPageNumber());
                page.rotationDegrees = submittedPage.rotationDegrees();
                page.sourceReference = submittedPage.sourceReference();
                page.displayContent = submittedPage.displayContent();
                page.previewContent = submittedPage.displayContent();
                page.status = toQaPageStatus(submittedPage.reviewStatus());
                page.pageReadable = submittedPage.pageReadable();
                page.rotationCorrect = submittedPage.rotationCorrect();
                page.splitCorrect = submittedPage.splitCorrect();
                page.pageCountCorrect = submittedPage.pageCountCorrect();
                page.comment = submittedPage.comment();
                document.pages.add(page);
            }

            if (!document.pages.isEmpty()) {
                reviewDocuments.add(document);
            }
        }

        if (assignment.reviewedPages > 0) {
            selectedGlobalPage = Math.min(assignment.reviewedPages, Math.max(1, getTotalReviewPageCount()));
        }
        selectPageByGlobalNumber(selectedGlobalPage);
    }

    private QaPageStatus toQaPageStatus(QAService.QaPageReviewStatus status) {
        if (status == null) {
            return QaPageStatus.NOT_REVIEWED;
        }
        return switch (status) {
            case NOT_REVIEWED -> QaPageStatus.NOT_REVIEWED;
            case APPROVED -> QaPageStatus.APPROVED;
            case NEEDS_FIX -> QaPageStatus.NEEDS_FIX;
        };
    }

    // =========================================================
    // QA REVIEW REFRESH
    // =========================================================

    private void refreshQaReviewWorkspace() {
        updateSelectedAssignmentFromReview();
        loadSelectedQaViewState();
        renderQaHeader();
        renderQaDocumentTree();
        renderQaPreview();
        renderQaTools();
        updateQaRotationButtons();
    }

    private void updateSelectedAssignmentFromReview() {
        if (selectedAssignment == null || reviewDocuments.isEmpty()) {
            return;
        }

        int reviewedPages = getReviewedPageCount();
        int issueCount = getIssueCount();

        selectedAssignment.reviewedPages = reviewedPages;
        selectedAssignment.issueCount = issueCount;

        if (reviewedPages <= 0) {
            selectedAssignment.status = QaStatus.IN_REVIEW;
            return;
        }

        if (issueCount > 0) {
            selectedAssignment.status = QaStatus.ISSUES_FOUND;
            return;
        }

        if (reviewedPages >= selectedAssignment.totalPages) {
            selectedAssignment.status = QaStatus.QA_COMPLETED;
            return;
        }

        selectedAssignment.status = QaStatus.IN_REVIEW;
    }

    // =========================================================
    // QA REVIEW HEADER
    // =========================================================

    private void renderQaHeader() {
        if (selectedAssignment == null) {
            return;
        }

        qaBoxIdLabel.setText(selectedAssignment.boxId);
        qaProfileLabel.setText(selectedAssignment.profile);
        updateQaSidebarSubtitle();
        qaProgressLabel.setText(
                selectedAssignment.reviewedPages + " / "
                        + selectedAssignment.totalPages
                        + " pages reviewed"
        );

        reviewStatusBadge.setText(selectedAssignment.status.displayName);
        reviewStatusBadge.getStyleClass().removeAll(
                "qa-review-status-waiting",
                "qa-review-status-review",
                "qa-review-status-issues",
                "qa-review-status-complete"
        );
        reviewStatusBadge.getStyleClass().add(getReviewStatusStyleClass(selectedAssignment.status));

        QaDocument document = getSelectedDocument();
        QaPage page = getSelectedQaPage();

        if (document == null || page == null) {
            selectedQaPageTitleLabel.setText("No page selected");
            selectedQaPageSubtitleLabel.setText("Select a page to review.");
        } else {
            selectedQaPageTitleLabel.setText(document.name + " \u00B7 Page " + page.pageNumber);
            selectedQaPageSubtitleLabel.setText(getPageStatusText(page.status));
        }

        updateQaZoomLabel();
    }

    private void updateQaRotationButtons() {
        if (qaRotateLeftButton == null || qaRotateRightButton == null) {
            return;
        }

        QaPage page = getSelectedQaPage();
        if (page == null) {
            qaRotateLeftButton.setText("Rotate Left (90°)");
            qaRotateRightButton.setText("Rotate Right (90°)");
            return;
        }

        int currentRotation = normalizeRotation(page.rotationDegrees);
        int leftTarget = normalizeRotation(currentRotation - 90);
        int rightTarget = normalizeRotation(currentRotation + 90);

        qaRotateLeftButton.setText("Rotate Left (" + leftTarget + "°)");
        qaRotateRightButton.setText("Rotate Right (" + rightTarget + "°)");
    }

    private String getReviewStatusStyleClass(QaStatus status) {
        return switch (status) {
            case WAITING_FOR_QA -> "qa-review-status-waiting";
            case IN_REVIEW -> "qa-review-status-review";
            case ISSUES_FOUND -> "qa-review-status-issues";
            case QA_COMPLETED -> "qa-review-status-complete";
        };
    }

    // =========================================================
    // QA DOCUMENT TREE
    // =========================================================

    private void renderQaDocumentTree() {
        qaDocumentTreeContainer.getChildren().clear();

        for (int documentIndex = 0; documentIndex < reviewDocuments.size(); documentIndex++) {
            QaDocument document = reviewDocuments.get(documentIndex);

            VBox documentBlock = new VBox(12);
            documentBlock.setAlignment(Pos.TOP_LEFT);
            documentBlock.getStyleClass().add("document-tree-document-block");
            if (qaDocumentListView) {
                documentBlock.getStyleClass().add("document-tree-list-block");
            }

            HBox documentHeader = new HBox(9);
            documentHeader.setAlignment(Pos.CENTER_LEFT);
            documentHeader.getStyleClass().addAll("document-tree-document-header", "document-tree-document-header-framed");
            if (qaDocumentListView) {
                documentHeader.getStyleClass().add("document-tree-list-header");
            }

            Region chevron = new Region();
            chevron.getStyleClass().add("document-tree-chevron-icon");
            chevron.setRotate(document.expanded ? 90 : 0);

            Label title = new Label(document.name);
            title.getStyleClass().add("document-tree-document-title");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label count = new Label(document.pages.size() + " pages");
            count.getStyleClass().add("document-tree-count");

            documentHeader.getChildren().addAll(chevron, title, spacer, count);

            final int headerDocumentIndex = documentIndex;
            documentHeader.setOnMouseClicked(event -> {
                reviewDocuments.get(headerDocumentIndex).expanded = !reviewDocuments.get(headerDocumentIndex).expanded;
                renderQaDocumentTree();
            });

            documentBlock.getChildren().add(documentHeader);

            if (document.expanded) {
                VBox pageStack = new VBox(qaDocumentListView ? 0 : 18);
                pageStack.setAlignment(qaDocumentListView ? Pos.TOP_LEFT : Pos.TOP_CENTER);
                pageStack.getStyleClass().add("document-tree-page-stack");
                if (qaDocumentListView) {
                    pageStack.getStyleClass().add("document-tree-list-page-stack");
                }
                for (int pageIndex = 0; pageIndex < document.pages.size(); pageIndex++) {
                    QaPage page = document.pages.get(pageIndex);
                    final int rowDocumentIndex = documentIndex;
                    final int rowPageIndex = pageIndex;
                    Node pageNode = qaDocumentListView
                            ? createQaPageRow(page, rowDocumentIndex, rowPageIndex)
                            : createQaEmbeddedPageCard(page, rowDocumentIndex, rowPageIndex);
                    pageStack.getChildren().add(pageNode);
                }
                documentBlock.getChildren().add(pageStack);
            }

            qaDocumentTreeContainer.getChildren().add(documentBlock);
        }
    }

    // =========================================================
    // QA PREVIEW
    // =========================================================

    private void renderQaPreview() {
        qaPreviewHost.getChildren().clear();

        QaDocument document = getSelectedDocument();
        QaPage page = getSelectedQaPage();

        if (document == null || page == null) {
            currentQaPreviewWrapper = null;

            VBox emptyPreview = new VBox(9);
            emptyPreview.setAlignment(Pos.CENTER);
            emptyPreview.getStyleClass().add("scan-preview-empty");

            Label title = new Label("No page selected");
            title.getStyleClass().add("scan-preview-empty-title");

            Label copy = new Label("Select a page from the documents list.");
            copy.getStyleClass().add("scan-preview-empty-copy");

            emptyPreview.getChildren().addAll(title, copy);
            qaPreviewHost.getChildren().add(emptyPreview);
            return;
        }

        Node previewPage = createSubmittedDocumentPage(document, page);
        qaPreviewHost.getChildren().add(wrapQaPreviewWithAutoScale(previewPage));
    }

    private Node wrapQaPreviewWithAutoScale(Node previewNode) {
        QaPage page = getSelectedQaPage();
        StackPane previewWrapper = new StackPane(previewNode);
        previewWrapper.setAlignment(Pos.CENTER);
        previewWrapper.setPickOnBounds(true);
        previewWrapper.setMaxWidth(QA_PREVIEW_PAGE_WIDTH);
        previewWrapper.setMaxHeight(QA_PREVIEW_PAGE_HEIGHT);
        previewWrapper.setRotate(page == null ? 0 : page.rotationDegrees);

        DoubleBinding scaleBinding = Bindings.createDoubleBinding(() -> {
            double availableWidth = Math.max(1, qaPreviewHost.getWidth() - QA_PREVIEW_SAFE_HORIZONTAL_PADDING);
            double availableHeight = Math.max(1, qaPreviewHost.getHeight() - QA_PREVIEW_SAFE_VERTICAL_PADDING);

            double widthScale = availableWidth / QA_PREVIEW_PAGE_WIDTH;
            double heightScale = availableHeight / QA_PREVIEW_PAGE_HEIGHT;
            double autoScale = Math.min(1.0, Math.min(widthScale, heightScale));

            return autoScale * previewZoomMultiplier.get();
        }, qaPreviewHost.widthProperty(), qaPreviewHost.heightProperty(), previewZoomMultiplier);

        previewWrapper.scaleXProperty().bind(scaleBinding);
        previewWrapper.scaleYProperty().bind(scaleBinding);

        previewWrapper.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> qaPreviewHost.requestFocus());
        previewWrapper.addEventFilter(ScrollEvent.SCROLL, this::handleQaPreviewScroll);
        previewWrapper.addEventFilter(ZoomEvent.ZOOM, this::handleQaPreviewZoom);

        currentQaPreviewWrapper = previewWrapper;

        scaleBinding.addListener((observable, oldValue, newValue) -> clampQaPreviewTranslation());

        clampQaPreviewTranslation();

        return previewWrapper;
    }

    private void updateQaSidebarSubtitle() {
        if (qaSidebarSubtitleLabel == null) {
            return;
        }

        if (selectedAssignment == null) {
            qaSidebarSubtitleLabel.setText("No assignment selected");
            return;
        }

        qaSidebarSubtitleLabel.setText(selectedAssignment.profile + " \u00B7 " + selectedAssignment.boxId);
    }

    private VBox createQaEmbeddedPageCard(QaPage page, int documentIndex, int pageIndex) {
        VBox card = new VBox(3);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().addAll("page-tray-item", "qa-embedded-page-card");

        if (pageIndex == selectedPageIndex && documentIndex == selectedDocumentIndex) {
            card.getStyleClass().add("page-tray-item-selected");
        }

        StackPane thumbnail = new StackPane();
        thumbnail.getStyleClass().add("page-tray-thumbnail");

        Image pageImage = decodeDataUriImage(page.imageContent());
        if (pageImage == null) {
            Region pageBlock = new Region();
            pageBlock.getStyleClass().add("qa-tray-page-block");
            thumbnail.getChildren().add(pageBlock);
        } else {
            ImageView imageView = new ImageView(pageImage);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setFitWidth(104);
            imageView.setFitHeight(126);
            imageView.setRotate(page.rotationDegrees);
            thumbnail.getChildren().add(imageView);
        }

        HBox labelRow = createQaPageLabelRow(page, true);
        card.getChildren().addAll(thumbnail, labelRow);
        card.setOnMouseClicked(event -> selectQaPage(documentIndex, pageIndex));
        return card;
    }

    private HBox createQaPageRow(QaPage page, int documentIndex, int pageIndex) {
        HBox row = new HBox(9);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("document-tree-page-row", "document-tree-list-page-row");

        if (pageIndex == selectedPageIndex && documentIndex == selectedDocumentIndex) {
            row.getStyleClass().add("document-tree-page-selected");
        }

        HBox labelRow = createQaPageLabelRow(page, false);

        row.getChildren().add(labelRow);
        row.setOnMouseClicked(event -> selectQaPage(documentIndex, pageIndex));
        return row;
    }

    private void selectQaPage(int documentIndex, int pageIndex) {
        if (documentIndex < 0 || documentIndex >= reviewDocuments.size()) {
            return;
        }

        if (pageIndex < 0 || pageIndex >= reviewDocuments.get(documentIndex).pages.size()) {
            return;
        }

        persistQaProgressAsync();
        selectedDocumentIndex = documentIndex;
        selectedPageIndex = pageIndex;

        refreshQaReviewWorkspace();
        qaPreviewHost.requestFocus();
    }

    private HBox createQaPageLabelRow(QaPage page, boolean centered) {
        HBox labelRow = new HBox(6);
        labelRow.setAlignment(centered ? Pos.CENTER : Pos.CENTER_LEFT);

        Label pageLabel = new Label("Page " + page.pageNumber);
        pageLabel.getStyleClass().add(centered ? "page-tray-number" : "document-tree-page-title");

        labelRow.getChildren().add(pageLabel);

        if (page.status != QaPageStatus.NOT_REVIEWED) {
            Label statusLabel = new Label(page.status == QaPageStatus.APPROVED ? "Approved" : "Needs Fix");
            statusLabel.getStyleClass().add(
                    page.status == QaPageStatus.APPROVED
                            ? "qa-page-status-text-approved"
                            : "qa-page-status-text-fix"
            );
            labelRow.getChildren().add(statusLabel);
        }

        return labelRow;
    }

    private void selectPageByGlobalNumber(int globalPageNumber) {
        for (int documentIndex = 0; documentIndex < reviewDocuments.size(); documentIndex++) {
            QaDocument document = reviewDocuments.get(documentIndex);

            for (int pageIndex = 0; pageIndex < document.pages.size(); pageIndex++) {
                if (document.pages.get(pageIndex).globalPageNumber == globalPageNumber) {
                    selectedDocumentIndex = documentIndex;
                    selectedPageIndex = pageIndex;
                    return;
                }
            }
        }

        selectedDocumentIndex = 0;
        selectedPageIndex = 0;
    }

    private QaDocument getSelectedDocument() {
        if (selectedDocumentIndex < 0 || selectedDocumentIndex >= reviewDocuments.size()) {
            return null;
        }

        return reviewDocuments.get(selectedDocumentIndex);
    }

    private QaPage getSelectedQaPage() {
        QaDocument document = getSelectedDocument();

        if (document == null) {
            return null;
        }

        if (selectedPageIndex < 0 || selectedPageIndex >= document.pages.size()) {
            return null;
        }

        return document.pages.get(selectedPageIndex);
    }

    private List<QaPage> getQaPagesForAction() {
        if (qaActionScopeComboBox != null
                && "This Document".equals(qaActionScopeComboBox.getValue())) {
            QaDocument document = getSelectedDocument();
            if (document == null) {
                return List.of();
            }
            return new ArrayList<>(document.pages);
        }

        QaPage selectedPage = getSelectedQaPage();
        if (selectedPage == null) {
            return List.of();
        }

        return List.of(selectedPage);
    }

    // =========================================================
    // QA TOOLS PANEL
    // =========================================================

    private void renderQaTools() {
        QaPage page = getSelectedQaPage();

        if (page == null) {
            return;
        }

        if (currentPageStatusLabel != null) {
            currentPageStatusLabel.setText(getPageStatusText(page.status));
            currentPageStatusLabel.getStyleClass().removeAll(
                    "qa-current-status-pending",
                    "qa-current-status-approved",
                    "qa-current-status-fix"
            );
            currentPageStatusLabel.getStyleClass().add(getCurrentStatusStyleClass(page.status));
        }

        syncingQaControls = true;

        if (pageReadableCheckBox != null) {
            pageReadableCheckBox.setSelected(page.pageReadable);
        }
        if (rotationCorrectCheckBox != null) {
            rotationCorrectCheckBox.setSelected(page.rotationCorrect);
        }
        if (splitCorrectCheckBox != null) {
            splitCorrectCheckBox.setSelected(page.splitCorrect);
        }
        if (pageCountCorrectCheckBox != null) {
            pageCountCorrectCheckBox.setSelected(page.pageCountCorrect);
        }
        qaCommentTextArea.setText(page.comment == null ? "" : page.comment);
        syncQaRotationComboBox();

        syncingQaControls = false;
    }

    // =========================================================
    // QA REVIEW COUNTS
    // =========================================================

    private int getReviewedPageCount() {
        int reviewedPages = 0;

        for (QaDocument document : reviewDocuments) {
            for (QaPage page : document.pages) {
                if (page.status != QaPageStatus.NOT_REVIEWED) {
                    reviewedPages++;
                }
            }
        }

        return reviewedPages;
    }

    private int getIssueCount() {
        int issueCount = 0;

        for (QaDocument document : reviewDocuments) {
            for (QaPage page : document.pages) {
                if (page.status == QaPageStatus.NEEDS_FIX) {
                    issueCount++;
                }
            }
        }

        return issueCount;
    }

    private void scheduleQaProgressSave() {
        if (selectedAssignment == null || portalModel == null) {
            return;
        }
        qaAutoSaveDelay.playFromStart();
    }

    private void persistQaProgressAsync() {
        if (selectedAssignment == null || portalModel == null) {
            return;
        }
        updateSelectedAssignmentFromReview();
        UUID reviewId = selectedAssignment.reviewId;
        QAService.QaReviewStatus status = toPersistedReviewStatus(selectedAssignment.status);
        int reviewedPages = selectedAssignment.reviewedPages;
        int totalPages = getTotalReviewPageCount();
        int issueCount = selectedAssignment.issueCount;
        List<QAService.QaDocumentSnapshot> snapshot = toQaDocumentSnapshots();
        BackgroundExecutor.io().execute(() ->
                portalModel.saveQaProgress(
                        reviewId,
                        status,
                        reviewedPages,
                        totalPages,
                        issueCount,
                        snapshot
                )
        );
    }

    private QAService.QaReviewStatus toPersistedReviewStatus(QaStatus status) {
        if (status == null) {
            return QAService.QaReviewStatus.IN_REVIEW;
        }
        return switch (status) {
            case WAITING_FOR_QA -> QAService.QaReviewStatus.WAITING_FOR_QA;
            case IN_REVIEW, ISSUES_FOUND -> QAService.QaReviewStatus.IN_REVIEW;
            case QA_COMPLETED -> QAService.QaReviewStatus.APPROVED;
        };
    }

    private List<QAService.QaDocumentSnapshot> toQaDocumentSnapshots() {
        List<QAService.QaDocumentSnapshot> documents = new ArrayList<>();
        for (int documentIndex = 0; documentIndex < reviewDocuments.size(); documentIndex++) {
            QaDocument document = reviewDocuments.get(documentIndex);
            List<QAService.QaPageSnapshot> pages = new ArrayList<>();
            for (QaPage page : document.pages) {
                pages.add(new QAService.QaPageSnapshot(
                        page.pageNumber,
                        page.globalPageNumber,
                        page.sourceReference,
                        firstNonBlank(page.displayContent, page.previewContent),
                        page.rotationDegrees,
                        toPersistedPageStatus(page.status),
                        page.pageReadable,
                        page.rotationCorrect,
                        page.splitCorrect,
                        page.pageCountCorrect,
                        page.comment
                ));
            }
            documents.add(new QAService.QaDocumentSnapshot(documentIndex + 1, document.name, pages));
        }
        return documents;
    }

    private QAService.QaPageReviewStatus toPersistedPageStatus(QaPageStatus status) {
        if (status == null) {
            return QAService.QaPageReviewStatus.NOT_REVIEWED;
        }
        return switch (status) {
            case NOT_REVIEWED -> QAService.QaPageReviewStatus.NOT_REVIEWED;
            case APPROVED -> QAService.QaPageReviewStatus.APPROVED;
            case NEEDS_FIX -> QAService.QaPageReviewStatus.NEEDS_FIX;
        };
    }

    private Node createSubmittedDocumentPage(QaDocument document, QaPage page) {
        Image image = decodeDataUriImage(page.imageContent());
        if (image == null) {
            VBox unavailable = new VBox(9);
            unavailable.setAlignment(Pos.CENTER);
            unavailable.getStyleClass().add("mock-document-page");
            unavailable.setMinWidth(QA_PREVIEW_PAGE_WIDTH);
            unavailable.setPrefWidth(QA_PREVIEW_PAGE_WIDTH);
            unavailable.setMaxWidth(QA_PREVIEW_PAGE_WIDTH);
            unavailable.setMinHeight(QA_PREVIEW_PAGE_HEIGHT);
            unavailable.setPrefHeight(QA_PREVIEW_PAGE_HEIGHT);
            unavailable.setMaxHeight(QA_PREVIEW_PAGE_HEIGHT);

            Label title = new Label("Preview unavailable");
            title.getStyleClass().add("scan-preview-empty-title");
            Label copy = new Label(document.name + " · Page " + page.pageNumber);
            copy.getStyleClass().add("scan-preview-empty-copy");
            unavailable.getChildren().addAll(title, copy);
            return unavailable;
        }

        StackPane documentPage = new StackPane();
        documentPage.getStyleClass().add("mock-document-page");
        documentPage.setMinWidth(QA_PREVIEW_PAGE_WIDTH);
        documentPage.setPrefWidth(QA_PREVIEW_PAGE_WIDTH);
        documentPage.setMaxWidth(QA_PREVIEW_PAGE_WIDTH);
        documentPage.setMinHeight(QA_PREVIEW_PAGE_HEIGHT);
        documentPage.setPrefHeight(QA_PREVIEW_PAGE_HEIGHT);
        documentPage.setMaxHeight(QA_PREVIEW_PAGE_HEIGHT);

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setFitWidth(QA_PREVIEW_PAGE_WIDTH - 36);
        imageView.setFitHeight(QA_PREVIEW_PAGE_HEIGHT - 48);

        Label pageLabel = new Label(document.name + " · Page " + page.pageNumber);
        pageLabel.getStyleClass().add("qa-preview-page-label");
        StackPane.setAlignment(pageLabel, Pos.BOTTOM_CENTER);

        documentPage.getChildren().addAll(imageView, pageLabel);
        return documentPage;
    }

    private Image decodeDataUriImage(String imageContent) {
        if (imageContent == null || imageContent.isBlank()) {
            return null;
        }

        try {
            String encoded = imageContent.trim();
            int separator = encoded.indexOf("base64,");
            if (separator >= 0) {
                encoded = encoded.substring(separator + 7);
            }

            byte[] imageBytes = Base64.getDecoder().decode(encoded);
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (bufferedImage == null) {
                bufferedImage = TiffImageSupport.readFirstFrame(
                        imageBytes,
                        (int) QA_PREVIEW_PAGE_WIDTH,
                        (int) QA_PREVIEW_PAGE_HEIGHT
                );
            }
            if (bufferedImage != null) {
                return SwingFXUtils.toFXImage(bufferedImage, null);
            }

            Image image = new Image(new ByteArrayInputStream(imageBytes));
            if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return null;
            }
            return image;
        } catch (IllegalArgumentException exception) {
            return null;
        } catch (Exception exception) {
            return null;
        }
    }

    private int getApprovedPageCount() {
        int approvedPages = 0;

        for (QaDocument document : reviewDocuments) {
            for (QaPage page : document.pages) {
                if (page.status == QaPageStatus.APPROVED) {
                    approvedPages++;
                }
            }
        }

        return approvedPages;
    }

    private int getTotalReviewPageCount() {
        int totalPages = 0;

        for (QaDocument document : reviewDocuments) {
            totalPages += document.pages.size();
        }

        return totalPages;
    }

    // =========================================================
    // QA STATUS HELPERS
    // =========================================================

    private String getPageStatusText(QaPageStatus status) {
        return switch (status) {
            case NOT_REVIEWED -> "Not Reviewed";
            case APPROVED -> "Approved";
            case NEEDS_FIX -> "Needs Fix";
        };
    }

    private String getPageStatusGlyph(QaPageStatus status) {
        return switch (status) {
            case NOT_REVIEWED -> "";
            case APPROVED -> "\u2713";
            case NEEDS_FIX -> "!";
        };
    }

    private String getPageStatusStyleClass(QaPageStatus status) {
        return switch (status) {
            case NOT_REVIEWED -> "qa-status-pending";
            case APPROVED -> "qa-status-approved";
            case NEEDS_FIX -> "qa-status-fix";
        };
    }

    private String getCurrentStatusStyleClass(QaPageStatus status) {
        return switch (status) {
            case NOT_REVIEWED -> "qa-current-status-pending";
            case APPROVED -> "qa-current-status-approved";
            case NEEDS_FIX -> "qa-current-status-fix";
        };
    }

    // =========================================================
    // FXML ACTIONS: NAVIGATION
    // =========================================================

    @FXML
    private void onBackToAssignedQaList() {
        updateSelectedAssignmentFromReview();
        qaAutoSaveDelay.stop();
        persistQaProgressAsync();
        loadAssignments();
        renderAssignments();
        showAssignedQaListView();
    }

    @FXML
    private void onPreviousPage() {
        int currentLinearIndex = getLinearPageIndex(selectedDocumentIndex, selectedPageIndex);

        if (currentLinearIndex <= 0) {
            return;
        }

        selectPageByLinearIndex(currentLinearIndex - 1);
    }

    @FXML
    private void onNextPage() {
        int currentLinearIndex = getLinearPageIndex(selectedDocumentIndex, selectedPageIndex);

        if (currentLinearIndex < 0 || currentLinearIndex >= getTotalReviewPageCount() - 1) {
            return;
        }

        selectPageByLinearIndex(currentLinearIndex + 1);
    }

    @FXML
    private void onNextUnreviewed() {
        int currentLinearIndex = getLinearPageIndex(selectedDocumentIndex, selectedPageIndex);
        int totalPages = getTotalReviewPageCount();

        for (int index = currentLinearIndex + 1; index < totalPages; index++) {
            PagePointer pointer = getPagePointerByLinearIndex(index);

            if (pointer != null) {
                QaPage page = reviewDocuments.get(pointer.documentIndex).pages.get(pointer.pageIndex);

                if (page.status == QaPageStatus.NOT_REVIEWED) {
                    selectQaPage(pointer.documentIndex, pointer.pageIndex);
                    return;
                }
            }
        }

        for (int index = 0; index < currentLinearIndex; index++) {
            PagePointer pointer = getPagePointerByLinearIndex(index);

            if (pointer != null) {
                QaPage page = reviewDocuments.get(pointer.documentIndex).pages.get(pointer.pageIndex);

                if (page.status == QaPageStatus.NOT_REVIEWED) {
                    selectQaPage(pointer.documentIndex, pointer.pageIndex);
                    return;
                }
            }
        }
    }

    // =========================================================
    // FXML ACTIONS: VIEW-ONLY PREVIEW CONTROLS
    // =========================================================

    @FXML
    private void onZoomIn() {
        zoomQaPreviewBy(QA_PREVIEW_ZOOM_STEP);
    }

    @FXML
    private void onZoomOut() {
        zoomQaPreviewBy(-QA_PREVIEW_ZOOM_STEP);
    }

    @FXML
    private void onResetView() {
        resetQaPreviewViewState();
        renderQaHeader();
        renderQaPreview();
    }

    @FXML
    private void onRotateLeft() {
        QaPage page = getSelectedQaPage();
        if (page == null) {
            return;
        }

        page.rotationDegrees = normalizeRotation(page.rotationDegrees - 90);
        renderQaPreview();
        updateQaRotationButtons();
        syncQaRotationComboBox();
        scheduleQaProgressSave();
    }

    @FXML
    private void onRotateRight() {
        QaPage page = getSelectedQaPage();
        if (page == null) {
            return;
        }

        page.rotationDegrees = normalizeRotation(page.rotationDegrees + 90);
        renderQaPreview();
        updateQaRotationButtons();
        syncQaRotationComboBox();
        scheduleQaProgressSave();
    }

    private int normalizeRotation(int rotationDegrees) {
        int normalized = rotationDegrees % 360;

        if (normalized < 0) {
            normalized += 360;
        }

        return normalized;
    }

    private void syncQaRotationComboBox() {
        if (qaRotationComboBox == null) {
            return;
        }

        syncingQaRotationComboBox = true;
        QaPage page = getSelectedQaPage();
        String rotationValue = formatRotationDegrees(page == null ? 0 : page.rotationDegrees);
        qaRotationComboBox.setValue(rotationValue);
        if (qaRotationComboBox.getEditor() != null) {
            qaRotationComboBox.getEditor().setText(rotationValue);
        }
        syncingQaRotationComboBox = false;
    }

    private String formatRotationDegrees(int rotationDegrees) {
        return normalizeRotation(rotationDegrees) + "\u00B0";
    }

    private int parseRotationDegrees(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        String normalizedValue = value.trim().replace("\u00B0", "");
        try {
            return normalizeRotation(Integer.parseInt(normalizedValue));
        } catch (NumberFormatException ignored) {
            String digitsOnly = normalizedValue.replaceAll("[^0-9-]", "");
            if (digitsOnly.isBlank() || "-".equals(digitsOnly)) {
                return 0;
            }
            try {
                return normalizeRotation(Integer.parseInt(digitsOnly));
            } catch (NumberFormatException ignoredAgain) {
                return 0;
            }
        }
    }

    // =========================================================
    // FXML ACTIONS: QA WORK
    // =========================================================

    @FXML
    private void onApprovePage() {
        List<QaPage> pages = getQaPagesForAction();
        if (pages.isEmpty()) {
            return;
        }

        for (QaPage page : pages) {
            page.status = QaPageStatus.APPROVED;
            page.pageReadable = true;
            page.rotationCorrect = true;
            page.splitCorrect = true;
            page.pageCountCorrect = true;
        }

        refreshQaReviewWorkspace();
        persistQaProgressAsync();
    }

    @FXML
    private void onMarkNeedsFix() {
        List<QaPage> pages = getQaPagesForAction();
        if (pages.isEmpty()) {
            return;
        }

        for (QaPage page : pages) {
            page.status = QaPageStatus.NEEDS_FIX;
            if (page.comment == null || page.comment.isBlank()) {
                page.comment = "Needs correction before QA can be completed.";
            }
        }

        refreshQaReviewWorkspace();
        persistQaProgressAsync();
    }

    @FXML
    private void onClearReviewStatus() {
        List<QaPage> pages = getQaPagesForAction();
        if (pages.isEmpty()) {
            return;
        }

        for (QaPage page : pages) {
            page.status = QaPageStatus.NOT_REVIEWED;
            page.pageReadable = false;
            page.rotationCorrect = false;
            page.splitCorrect = false;
            page.pageCountCorrect = false;
            page.comment = "";
        }

        refreshQaReviewWorkspace();
        persistQaProgressAsync();
    }

    @FXML
    private void onCompleteQa() {
        updateSelectedAssignmentFromReview();
        qaAutoSaveDelay.stop();
        if (selectedAssignment == null) {
            return;
        }

        int totalPages = getTotalReviewPageCount();
        if (getReviewedPageCount() < totalPages) {
            showExportAlert(null, Alert.AlertType.WARNING, "QA incomplete",
                    "Review every page before completing QA.");
            return;
        }

        boolean approved = getIssueCount() == 0;
        if (portalModel != null) {
            portalModel.completeQaReview(
                    selectedAssignment.reviewId,
                    approved,
                    getReviewedPageCount(),
                    totalPages,
                    getIssueCount(),
                    toQaDocumentSnapshots()
            );
        }

        selectedAssignment.status = approved ? QaStatus.QA_COMPLETED : QaStatus.ISSUES_FOUND;
        selectedAssignment.reviewedPages = getReviewedPageCount();
        selectedAssignment.issueCount = getIssueCount();

        if (approved) {
            refreshQaReviewWorkspace();
            loadAssignments();
            renderAssignments();
            showExportAlert(null, Alert.AlertType.INFORMATION, "QA approved",
                    "QA is complete. Export is now available for this review.");
            return;
        }

        loadAssignments();
        renderAssignments();
        showAssignedQaListView();
        showExportAlert(null, Alert.AlertType.INFORMATION, "QA rejected",
                "QA was rejected and returned to the scan owner with your comments.");
    }

    @FXML
    private void onOpenExportTypeDialog() {
        if (selectedAssignment == null) {
            return;
        }

        if (!areAllQaDocumentsApproved()) {
            showExportBlockedMessage();
            return;
        }

        Stage stage = new Stage();
        stage.setTitle("TIFF Export");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setResizable(false);

        if (qaReviewWorkspaceView != null && qaReviewWorkspaceView.getScene() != null) {
            stage.initOwner(qaReviewWorkspaceView.getScene().getWindow());
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

    private boolean areAllQaDocumentsApproved() {
        if (reviewDocuments.isEmpty()) {
            return false;
        }

        for (QaDocument document : reviewDocuments) {
            if (document.pages.isEmpty()) {
                return false;
            }

            for (QaPage page : document.pages) {
                if (page.status != QaPageStatus.APPROVED) {
                    return false;
                }
            }
        }

        return true;
    }

    private void showExportBlockedMessage() {
        int totalPages = getTotalReviewPageCount();
        int approvedPages = getApprovedPageCount();
        int remainingPages = Math.max(0, totalPages - approvedPages);

        String message = "You can export only after all QA documents are approved. "
                + remainingPages + " pages still need approval.";

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Export blocked");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Node defaultCloseButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (defaultCloseButton != null) {
            defaultCloseButton.setVisible(false);
            defaultCloseButton.setManaged(false);
        }

        dialog.getDialogPane().getStyleClass().addAll("app-shell", "profile-created-dialog-pane");

        if (qaReviewWorkspaceView != null && qaReviewWorkspaceView.getScene() != null) {
            dialog.initOwner(qaReviewWorkspaceView.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().setAll(qaReviewWorkspaceView.getScene().getStylesheets());

            if (qaReviewWorkspaceView.getScene().getRoot() != null
                    && qaReviewWorkspaceView.getScene().getRoot().getStyleClass().contains("dark")) {
                dialog.getDialogPane().getStyleClass().add("dark");
            }
        }

        dialog.getDialogPane().setPrefWidth(540);
        dialog.getDialogPane().setMaxWidth(540);
        dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        dialog.getDialogPane().setPrefHeight(Region.USE_COMPUTED_SIZE);
        dialog.getDialogPane().setMaxHeight(Region.USE_PREF_SIZE);
        dialog.getDialogPane().setGraphic(null);
        dialog.getDialogPane().setContent(createExportBlockedDialogContent(dialog, message));

        dialog.showAndWait();
    }

    private VBox createExportBlockedDialogContent(Dialog<ButtonType> dialog, String message) {
        VBox root = new VBox();
        root.getStyleClass().add("profile-created-dialog-root");

        HBox header = createExportBlockedDialogHeader(dialog);
        makeDialogDraggable(dialog, header);

        root.getChildren().addAll(
                header,
                createExportBlockedDialogBody(dialog, message)
        );

        return root;
    }

    private HBox createExportBlockedDialogHeader(Dialog<ButtonType> dialog) {
        Label brandLabel = new Label("W");
        brandLabel.getStyleClass().add("profile-created-dialog-brand-label");

        StackPane brandShell = new StackPane(brandLabel);
        brandShell.getStyleClass().add("profile-created-dialog-brand-shell");

        Label titleLabel = new Label("Export blocked");
        titleLabel.getStyleClass().add("profile-created-dialog-title");

        Button closeButton = new Button("×");
        closeButton.getStyleClass().add("profile-created-dialog-close-button");
        closeButton.setFocusTraversable(false);
        closeButton.setOnAction(event -> {
            dialog.setResult(ButtonType.CLOSE);
            dialog.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(18, brandShell, titleLabel, spacer, closeButton);
        header.getStyleClass().add("profile-created-dialog-header");
        header.setAlignment(Pos.CENTER_LEFT);

        return header;
    }

    private VBox createExportBlockedDialogBody(Dialog<ButtonType> dialog, String message) {
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("profile-created-dialog-message");
        messageLabel.setWrapText(true);

        Button okButton = new Button("OK");
        okButton.getStyleClass().addAll("profile-created-dialog-ok-button", "profile-open-button");
        okButton.setFocusTraversable(false);
        okButton.setOnAction(event -> {
            dialog.setResult(ButtonType.OK);
            dialog.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(16, spacer, okButton);
        actions.getStyleClass().add("profile-created-dialog-actions");
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(28, messageLabel, actions);
        body.getStyleClass().add("profile-created-dialog-body");

        return body;
    }

    private void makeDialogDraggable(Dialog<?> dialog, Node dragHandle) {
        if (dialog == null || dragHandle == null) {
            return;
        }

        final double[] dragOffset = new double[2];

        dragHandle.setOnMousePressed(event -> {
            if (!(dialog.getDialogPane().getScene().getWindow() instanceof Stage stage)) {
                return;
            }

            dragOffset[0] = event.getScreenX() - stage.getX();
            dragOffset[1] = event.getScreenY() - stage.getY();
        });

        dragHandle.setOnMouseDragged(event -> {
            if (!(dialog.getDialogPane().getScene().getWindow() instanceof Stage stage)) {
                return;
            }

            stage.setX(event.getScreenX() - dragOffset[0]);
            stage.setY(event.getScreenY() - dragOffset[1]);
        });
    }

    private VBox buildExportDialogContent(Stage stage) {
        List<Document> exportDocuments = buildExportDocuments();
        List<String> boxFiles = buildExportFiles(exportDocuments);
        ObjectProperty<TiffExportType> selectedType = new SimpleObjectProperty<>(TiffExportType.MULTI_PAGE);

        Label title = new Label("TIFF Export");
        title.getStyleClass().add("exports-dialog-title");

        VBox header = new VBox(9, title);
        header.getStyleClass().add("exports-dialog-header");

        Label boxValue = new Label(selectedAssignment.boxId);
        boxValue.getStyleClass().add("exports-dialog-box-value");

        Label boxDetail = new Label("Only approved QA documents from this box can be exported in this dialog.");
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
        exportButton.setOnAction(event -> handleQaExport(stage, selectedType.get(), exportDocuments));

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
            Label emptyState = new Label("No approved documents available for this export.");
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

        for (int documentIndex = 0; documentIndex < reviewDocuments.size(); documentIndex++) {
            QaDocument qaDocument = reviewDocuments.get(documentIndex);
            List<PageImage> pages = new ArrayList<>();

            for (QaPage qaPage : qaDocument.pages) {
                if (qaPage.status != QaPageStatus.APPROVED) {
                    continue;
                }

                PageImage pageImage = new PageImage(
                        qaPage.pageNumber,
                        PageImage.PageType.TIFF,
                        firstNonBlank(qaPage.sourceReference, qaDocument.name)
                );
                pageImage.setRotationDegrees(qaPage.rotationDegrees);
                pageImage.setDisplayContent(firstNonBlank(qaPage.displayContent, qaPage.previewContent));
                pages.add(pageImage);
            }

            if (!pages.isEmpty()) {
                exportDocuments.add(new Document("document_" + String.format(Locale.US, "%03d", documentIndex + 1), pages));
            }
        }

        return exportDocuments;
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
            return "No approved documents are available for export";
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

    private void handleQaExport(Stage stage, TiffExportType exportType, List<Document> exportDocuments) {
        if (!areAllQaDocumentsApproved()) {
            showExportBlockedMessage();
            return;
        }

        if (exportDocuments == null || exportDocuments.isEmpty()) {
            showExportAlert(stage, Alert.AlertType.ERROR, "No documents to export",
                    "There are no approved QA documents ready for TIFF export.");
            return;
        }

        String profileName = selectedAssignment == null ? "" : selectedAssignment.profile;
        String boxId = selectedAssignment == null ? "" : selectedAssignment.boxId;
        ScanProfile profile = portalModel == null ? null : portalModel.fetchScanProfileByName(profileName);
        String profileCode = firstNonBlank(profile == null ? null : profile.getCode(), profileName);
        String exportNaming = firstNonBlank(profile == null ? null : profile.getExportNaming(), "{profileCode}_{boxId}");

        try {
            Path outputDirectory = Path.of(
                    System.getProperty("user.home"),
                    "Downloads",
                    "WebLager Exports",
                    safeFolderName(profileName, boxId)
            );

            TiffExportManager.ExportResult result = tiffExportManager.exportPlan(
                    exportType == TiffExportType.SINGLE_PAGE
                            ? tiffExportManager.createSinglePagePlan(
                            profileName,
                            profileCode,
                            exportNaming,
                            boxId,
                            flattenExportPages(exportDocuments)
                    )
                            : tiffExportManager.createMultiPagePlan(
                            profileName,
                            profileCode,
                            exportNaming,
                            boxId,
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

    // =========================================================
    // LINEAR PAGE NAVIGATION
    // =========================================================

    private int getLinearPageIndex(int documentIndex, int pageIndex) {
        int counter = 0;

        for (int currentDocumentIndex = 0; currentDocumentIndex < reviewDocuments.size(); currentDocumentIndex++) {
            QaDocument document = reviewDocuments.get(currentDocumentIndex);

            for (int currentPageIndex = 0; currentPageIndex < document.pages.size(); currentPageIndex++) {
                if (currentDocumentIndex == documentIndex && currentPageIndex == pageIndex) {
                    return counter;
                }

                counter++;
            }
        }

        return -1;
    }

    private void selectPageByLinearIndex(int linearIndex) {
        PagePointer pointer = getPagePointerByLinearIndex(linearIndex);

        if (pointer != null) {
            selectQaPage(pointer.documentIndex, pointer.pageIndex);
        }
    }

    private PagePointer getPagePointerByLinearIndex(int linearIndex) {
        int counter = 0;

        for (int documentIndex = 0; documentIndex < reviewDocuments.size(); documentIndex++) {
            QaDocument document = reviewDocuments.get(documentIndex);

            for (int pageIndex = 0; pageIndex < document.pages.size(); pageIndex++) {
                if (counter == linearIndex) {
                    return new PagePointer(documentIndex, pageIndex);
                }

                counter++;
            }
        }

        return null;
    }

    // =========================================================
    // INTERNAL TYPES
    // =========================================================

    private enum QaStatus {
        WAITING_FOR_QA("Waiting for QA"),
        IN_REVIEW("In Review"),
        ISSUES_FOUND("Issues Found"),
        QA_COMPLETED("QA Completed");

        private final String displayName;

        QaStatus(String displayName) {
            this.displayName = displayName;
        }
    }

    private enum QaPageStatus {
        NOT_REVIEWED,
        APPROVED,
        NEEDS_FIX
    }

    private static final class QaAssignment {
        private final UUID reviewId;
        private final UUID sessionId;
        private final String boxId;
        private final String profile;
        private final String scannedBy;
        private final int documentCount;
        private final int totalPages;
        private final LocalDate assignedDate;
        private final String assignedTimeLabel;

        private int reviewedPages;
        private QaStatus status;
        private int issueCount;
        private final List<QAService.QaDocumentSnapshot> submittedDocuments;

        private QaAssignment(
                UUID reviewId,
                UUID sessionId,
                String boxId,
                String profile,
                String scannedBy,
                int documentCount,
                int totalPages,
                LocalDate assignedDate,
                String assignedTimeLabel,
                int reviewedPages,
                QaStatus status,
                int issueCount,
                List<QAService.QaDocumentSnapshot> submittedDocuments
        ) {
            this.reviewId = Objects.requireNonNull(reviewId);
            this.sessionId = sessionId;
            this.boxId = Objects.requireNonNull(boxId);
            this.profile = Objects.requireNonNull(profile);
            this.scannedBy = Objects.requireNonNull(scannedBy);
            this.documentCount = documentCount;
            this.totalPages = totalPages;
            this.assignedDate = Objects.requireNonNull(assignedDate);
            this.assignedTimeLabel = Objects.requireNonNull(assignedTimeLabel);
            this.reviewedPages = reviewedPages;
            this.status = Objects.requireNonNull(status);
            this.issueCount = issueCount;
            this.submittedDocuments = submittedDocuments == null ? List.of() : List.copyOf(submittedDocuments);
        }
    }

    private static final class QaDocument {
        private final String name;
        private final List<QaPage> pages = new ArrayList<>();
        private boolean expanded = false;

        private QaDocument(String name) {
            this.name = name;
        }
    }

    private static final class QaPage {
        private final int pageNumber;
        private final int globalPageNumber;

        private QaPageStatus status = QaPageStatus.NOT_REVIEWED;
        private int rotationDegrees = 0;
        private double previewZoomMultiplier = 1.0;
        private double previewTranslateX = 0;
        private double previewTranslateY = 0;
        private boolean pageReadable = false;
        private boolean rotationCorrect = false;
        private boolean splitCorrect = false;
        private boolean pageCountCorrect = false;
        private String comment = "";
        private String sourceReference = "";
        private String displayContent = "";
        private String previewContent = "";

        private QaPage(int pageNumber, int globalPageNumber) {
            this.pageNumber = pageNumber;
            this.globalPageNumber = globalPageNumber;
        }

        private String imageContent() {
            if (previewContent != null && !previewContent.isBlank()) {
                return previewContent;
            }
            return displayContent == null ? "" : displayContent;
        }
    }

    private static final class PagePointer {
        private final int documentIndex;
        private final int pageIndex;

        private PagePointer(int documentIndex, int pageIndex) {
            this.documentIndex = documentIndex;
            this.pageIndex = pageIndex;
        }
    }

    private enum TiffExportType {
        SINGLE_PAGE,
        MULTI_PAGE
    }
}
