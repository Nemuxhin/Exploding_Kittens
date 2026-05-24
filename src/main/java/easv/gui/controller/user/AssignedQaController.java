package easv.gui.controller.user;

import easv.be.AuditLog.AuditLogDetail;
import easv.be.ReviewRecord;
import easv.be.User;
import easv.bll.AdminManager;
import easv.bll.UserSession;
import easv.gui.controller.utilities.SearchableComboBoxes;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class AssignedQaController {

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
    @FXML private ComboBox<String> profileFilterComboBox;
    @FXML private VBox qaCardListContainer;

    @FXML private Label qaBoxIdLabel;
    @FXML private Label qaProfileLabel;
    @FXML private Label qaProgressLabel;
    @FXML private Label reviewStatusBadge;

    @FXML private VBox qaDocumentTreeContainer;
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

    private final List<QaAssignment> allAssignments = new ArrayList<>();
    private final List<QaDocument> reviewDocuments = new ArrayList<>();
    private final AdminManager adminManager = new AdminManager();
    private int qaSessionRotationCount;

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
    private int previewRotationDegrees = 0;

    private StackPane currentQaPreviewWrapper;

    private boolean syncingQaControls = false;

    @FXML
    private void initialize() {
        configureFilters();
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

        // TODO: populate profile filter from real scan profiles loaded from the database
        profileFilterComboBox.getItems().setAll("All Profiles");
        profileFilterComboBox.getSelectionModel().selectFirst();

        SearchableComboBoxes.configure(statusFilterComboBox);
        SearchableComboBoxes.configure(profileFilterComboBox);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> renderAssignments());
        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> renderAssignments());
        profileFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> renderAssignments());
    }

    // =========================================================
    // QA CONTROL SETUP
    // =========================================================

    private void configureQaControls() {
        pageReadableCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (syncingQaControls) {
                return;
            }

            QaPage page = getSelectedQaPage();
            if (page != null) {
                page.pageReadable = newValue;
            }
        });

        rotationCorrectCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (syncingQaControls) {
                return;
            }

            QaPage page = getSelectedQaPage();
            if (page != null) {
                page.rotationCorrect = newValue;
            }
        });

        splitCorrectCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (syncingQaControls) {
                return;
            }

            QaPage page = getSelectedQaPage();
            if (page != null) {
                page.splitCorrect = newValue;
            }
        });

        pageCountCorrectCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (syncingQaControls) {
                return;
            }

            QaPage page = getSelectedQaPage();
            if (page != null) {
                page.pageCountCorrect = newValue;
            }
        });

        qaCommentTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (syncingQaControls) {
                return;
            }

            QaPage page = getSelectedQaPage();
            if (page != null) {
                page.comment = newValue == null ? "" : newValue;
            }
        });
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
        previewRotationDegrees = 0;

        if (currentQaPreviewWrapper != null) {
            currentQaPreviewWrapper.setTranslateX(0);
            currentQaPreviewWrapper.setTranslateY(0);
            currentQaPreviewWrapper.setRotate(0);
        }

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
    }

    private void updateQaZoomLabel() {
        if (zoomPercentLabel != null) {
            zoomPercentLabel.setText(Math.round(previewZoomMultiplier.get() * 100) + "%");
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    // =========================================================
    // ASSIGNMENT DATA
    // =========================================================

    private void loadAssignments() {
        allAssignments.clear();

        User currentUser = UserSession.getCurrentUser();
        if (currentUser == null || adminManager == null) {
            return;
        }
        String myName = currentUser.getName();
        if (myName == null || myName.isBlank()) {
            return;
        }

        for (ReviewRecord record : adminManager.getReviewRecords()) {
            if (!myName.equalsIgnoreCase(record.getAssignedTo())) {
                continue;
            }
            QaStatus status = mapQaStatus(record.getQaStatus());
            if (status == null) {
                continue;
            }
            allAssignments.add(new QaAssignment(
                    record.getIdentity(),
                    record.getProfile(),
                    record.getScannedBy(),
                    1,
                    record.getPages(),
                    record.getLastUpdated(),
                    0,
                    status
            ));
        }
    }

    private QaStatus mapQaStatus(String qaStatus) {
        if (qaStatus == null) {
            return null;
        }
        return switch (qaStatus.trim().toLowerCase(Locale.ROOT)) {
            case "waiting for qa" -> QaStatus.WAITING_FOR_QA;
            case "qa in progress" -> QaStatus.IN_REVIEW;
            case "qa rejected" -> QaStatus.ISSUES_FOUND;
            default -> null;
        };
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

    private boolean matchesProfile(QaAssignment assignment) {
        String selectedProfile = profileFilterComboBox.getValue();

        if (selectedProfile == null || selectedProfile.equals("All Profiles")) {
            return true;
        }

        return assignment.profile.equals(selectedProfile);
    }

    // =========================================================
    // ASSIGNED QA LIST RENDERING
    // =========================================================

    private void renderAssignments() {
        qaCardListContainer.getChildren().clear();

        List<QaAssignment> filteredAssignments = allAssignments.stream()
                .filter(this::matchesSearch)
                .filter(this::matchesStatus)
                .filter(this::matchesProfile)
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
        VBox card = new VBox(15);
        card.getStyleClass().add("assigned-qa-card");
        card.setMaxWidth(Double.MAX_VALUE);

        HBox topRow = new HBox(15);
        topRow.setAlignment(Pos.TOP_LEFT);
        topRow.setMaxWidth(Double.MAX_VALUE);

        VBox leftContent = new VBox(12);
        leftContent.setMaxWidth(Double.MAX_VALUE);
        leftContent.setFillWidth(true);
        HBox.setHgrow(leftContent, Priority.ALWAYS);

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label boxIdLabel = new Label(assignment.boxId);
        boxIdLabel.getStyleClass().add("assigned-qa-card-title");

        Label statusBadge = new Label(getAssignmentStatusBadgeText(assignment.status));
        statusBadge.getStyleClass().addAll(
                "assigned-qa-status-badge",
                getAssignmentStatusBadgeStyleClass(assignment.status)
        );

        titleRow.getChildren().addAll(boxIdLabel, statusBadge);

        HBox metaRowOne = new HBox(18);
        metaRowOne.setAlignment(Pos.CENTER_LEFT);

        Label profileLabel = new Label(assignment.profile);
        profileLabel.getStyleClass().add("assigned-qa-card-meta");

        Label separatorOne = new Label("·");
        separatorOne.getStyleClass().add("assigned-qa-card-meta-separator");

        Label scannedByLabel = new Label("Scanned by " + assignment.scannedBy);
        scannedByLabel.getStyleClass().add("assigned-qa-card-meta");

        metaRowOne.getChildren().addAll(profileLabel, separatorOne, scannedByLabel);

        Label documentSummaryLabel = new Label(
                assignment.documentCount + " document" + (assignment.documentCount == 1 ? "" : "s")
                        + " · "
                        + assignment.totalPages + " pages"
        );
        documentSummaryLabel.getStyleClass().add("assigned-qa-card-detail");

        HBox metaRowTwo = new HBox(18);
        metaRowTwo.setAlignment(Pos.CENTER_LEFT);

        Label assignedLabel = new Label("Assigned: " + assignment.assignedTimeLabel);
        assignedLabel.getStyleClass().add("assigned-qa-card-detail");

        Label separatorTwo = new Label("·");
        separatorTwo.getStyleClass().add("assigned-qa-card-meta-separator");

        Label progressLabel = new Label(
                "Progress: " + assignment.reviewedPages + " / " + assignment.totalPages + " pages reviewed"
        );
        progressLabel.getStyleClass().add("assigned-qa-card-detail");

        metaRowTwo.getChildren().addAll(assignedLabel, separatorTwo, progressLabel);

        if (assignment.status == QaStatus.ISSUES_FOUND && assignment.issueCount > 0) {
            Label separatorThree = new Label("·");
            separatorThree.getStyleClass().add("assigned-qa-card-meta-separator");

            Label issuesLabel = new Label(assignment.issueCount + " issues found");
            issuesLabel.getStyleClass().add("assigned-qa-card-issues");

            metaRowTwo.getChildren().addAll(separatorThree, issuesLabel);
        }

        StackPane progressBar = createAssignmentProgressBar(assignment);

        leftContent.getChildren().addAll(
                titleRow,
                metaRowOne,
                documentSummaryLabel,
                metaRowTwo,
                progressBar
        );

        VBox rightContent = new VBox();
        rightContent.setAlignment(Pos.TOP_RIGHT);
        rightContent.setMinWidth(150);
        rightContent.setPrefWidth(150);
        rightContent.setMaxWidth(150);

        Button actionButton = new Button(getAssignmentActionButtonText(assignment.status));
        actionButton.getStyleClass().add(getAssignmentActionButtonStyleClass(assignment.status));
        actionButton.setMinWidth(132);
        actionButton.setPrefWidth(132);
        actionButton.setMaxWidth(132);
        actionButton.setOnAction(event -> onOpenAssignment(assignment));

        rightContent.getChildren().add(actionButton);

        topRow.getChildren().addAll(leftContent, rightContent);
        card.getChildren().add(topRow);

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
        qaSessionRotationCount = 0;

        if (selectedAssignment.status == QaStatus.WAITING_FOR_QA) {
            selectedAssignment.status = QaStatus.IN_REVIEW;
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

        int documentCount = Math.max(1, assignment.documentCount);
        int totalPages = Math.max(1, assignment.totalPages);
        int basePagesPerDocument = totalPages / documentCount;
        int extraPages = totalPages % documentCount;

        int globalPageCounter = 0;

        for (int documentIndex = 0; documentIndex < documentCount; documentIndex++) {
            int pagesInDocument = basePagesPerDocument + (documentIndex < extraPages ? 1 : 0);
            QaDocument document = new QaDocument("Document " + (documentIndex + 1));

            for (int pageIndex = 0; pageIndex < pagesInDocument; pageIndex++) {
                globalPageCounter++;

                QaPage page = new QaPage(pageIndex + 1, globalPageCounter);
                document.pages.add(page);
            }

            reviewDocuments.add(document);
        }

        int reviewedCount = assignment.status == QaStatus.QA_COMPLETED
                ? totalPages
                : Math.max(0, Math.min(assignment.reviewedPages, totalPages));

        int issueCount = assignment.status == QaStatus.ISSUES_FOUND
                ? Math.max(1, assignment.issueCount)
                : 0;

        int firstIssueGlobalIndex = Math.max(1, reviewedCount - issueCount + 1);

        for (QaDocument document : reviewDocuments) {
            for (QaPage page : document.pages) {
                if (page.globalPageNumber <= reviewedCount) {
                    page.status = QaPageStatus.APPROVED;
                    page.pageReadable = true;
                    page.rotationCorrect = true;
                    page.splitCorrect = true;
                    page.pageCountCorrect = true;
                }

                if (assignment.status == QaStatus.ISSUES_FOUND
                        && page.globalPageNumber >= firstIssueGlobalIndex
                        && page.globalPageNumber <= reviewedCount) {
                    page.status = QaPageStatus.NEEDS_FIX;
                    page.rotationCorrect = false;
                    page.comment = "Needs correction before QA can be completed.";
                }
            }
        }

        int selectedGlobalPage = reviewedCount <= 0 ? 1 : Math.min(reviewedCount, totalPages);

        if (assignment.status == QaStatus.ISSUES_FOUND) {
            selectedGlobalPage = firstIssueGlobalIndex;
        }

        selectPageByGlobalNumber(selectedGlobalPage);
    }

    // =========================================================
    // QA REVIEW REFRESH
    // =========================================================

    private void refreshQaReviewWorkspace() {
        updateSelectedAssignmentFromReview();
        renderQaHeader();
        renderQaDocumentTree();
        renderQaPreview();
        renderQaPageTray();
        renderQaTools();
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
            selectedQaPageTitleLabel.setText(document.name + " · Page " + page.pageNumber);
            selectedQaPageSubtitleLabel.setText(getPageStatusText(page.status));
        }

        updateQaZoomLabel();
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

            VBox documentBlock = new VBox(0);
            documentBlock.getStyleClass().add("document-tree-document-block");

            HBox documentHeader = new HBox(9);
            documentHeader.setAlignment(Pos.CENTER_LEFT);
            documentHeader.getStyleClass().add("document-tree-document-header");

            Label chevron = new Label(document.expanded ? "⌄" : "›");
            chevron.getStyleClass().add("document-tree-chevron");

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
                for (int pageIndex = 0; pageIndex < document.pages.size(); pageIndex++) {
                    QaPage page = document.pages.get(pageIndex);

                    HBox pageRow = new HBox(9);
                    pageRow.setAlignment(Pos.CENTER_LEFT);
                    pageRow.getStyleClass().add("document-tree-page-row");

                    if (documentIndex == selectedDocumentIndex && pageIndex == selectedPageIndex) {
                        pageRow.getStyleClass().add("document-tree-page-selected");
                    }

                    Label pageName = new Label("Page " + page.pageNumber);
                    pageName.getStyleClass().add("document-tree-page-title");

                    Region rowSpacer = new Region();
                    HBox.setHgrow(rowSpacer, Priority.ALWAYS);

                    Label pageStatus = new Label(getPageStatusGlyph(page.status));
                    pageStatus.getStyleClass().addAll(
                            "qa-tree-page-status",
                            getPageStatusStyleClass(page.status)
                    );

                    pageRow.getChildren().addAll(pageName, rowSpacer, pageStatus);

                    final int rowDocumentIndex = documentIndex;
                    final int rowPageIndex = pageIndex;

                    pageRow.setOnMouseClicked(event -> selectQaPage(rowDocumentIndex, rowPageIndex));

                    documentBlock.getChildren().add(pageRow);
                }
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

            Label copy = new Label("Select a page from the document list or page tray.");
            copy.getStyleClass().add("scan-preview-empty-copy");

            emptyPreview.getChildren().addAll(title, copy);
            qaPreviewHost.getChildren().add(emptyPreview);
            return;
        }

        Node documentPreviewPage = createDocumentPreviewPage(document, page);
        qaPreviewHost.getChildren().add(wrapQaPreviewWithAutoScale(documentPreviewPage));
    }

    private Node wrapQaPreviewWithAutoScale(Node previewNode) {
        StackPane previewWrapper = new StackPane(previewNode);
        previewWrapper.setAlignment(Pos.CENTER);
        previewWrapper.setPickOnBounds(true);
        previewWrapper.setMaxWidth(QA_PREVIEW_PAGE_WIDTH);
        previewWrapper.setMaxHeight(QA_PREVIEW_PAGE_HEIGHT);
        previewWrapper.setRotate(previewRotationDegrees);

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

    private Node createDocumentPreviewPage(QaDocument document, QaPage page) {
        VBox documentPage = new VBox(15);
        documentPage.setAlignment(Pos.TOP_LEFT);
        documentPage.getStyleClass().add("document-preview-page");
        documentPage.setMinWidth(QA_PREVIEW_PAGE_WIDTH);
        documentPage.setPrefWidth(QA_PREVIEW_PAGE_WIDTH);
        documentPage.setMaxWidth(QA_PREVIEW_PAGE_WIDTH);
        documentPage.setMinHeight(QA_PREVIEW_PAGE_HEIGHT);
        documentPage.setPrefHeight(QA_PREVIEW_PAGE_HEIGHT);
        documentPage.setMaxHeight(QA_PREVIEW_PAGE_HEIGHT);

        HBox topSection = new HBox();
        topSection.setAlignment(Pos.TOP_LEFT);

        VBox topLeft = new VBox(9);
        topLeft.getChildren().addAll(
                createLine("document-preview-line-dark", 180, 15),
                createLine("document-preview-line-medium", 126, 9)
        );

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        VBox topRight = new VBox(6);
        topRight.setAlignment(Pos.TOP_RIGHT);
        topRight.getChildren().addAll(
                createLine("document-preview-line-medium", 90, 9),
                createLine("document-preview-line-medium", 108, 9)
        );

        topSection.getChildren().addAll(topLeft, topSpacer, topRight);

        VBox textLines = new VBox(6);
        textLines.getChildren().addAll(
                createLine("document-preview-line-light", 405, 7),
                createLine("document-preview-line-light", 405, 7),
                createLine("document-preview-line-light", 372, 7),
                createLine("document-preview-line-light", 405, 7),
                createLine("document-preview-line-light", 318, 7)
        );

        VBox formArea = new VBox(9);
        formArea.getStyleClass().add("document-preview-form-area");

        HBox formHeading = new HBox(18);
        formHeading.getChildren().addAll(
                createLine("document-preview-line-dark", 144, 12),
                createLine("document-preview-line-medium", 78, 8)
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
                createLine("document-preview-line-light", 405, 7),
                createLine("document-preview-line-light", 405, 7),
                createLine("document-preview-line-light", 315, 7)
        );

        Label pageLabel = new Label(document.name + " · Page " + page.pageNumber);
        pageLabel.getStyleClass().add("qa-preview-page-label");
        pageLabel.setMaxWidth(Double.MAX_VALUE);
        pageLabel.setAlignment(Pos.CENTER);

        documentPage.getChildren().addAll(
                topSection,
                textLines,
                formArea,
                bottomText,
                pageLabel
        );

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
        input.getStyleClass().add("document-preview-input");
        input.setMinHeight(30);
        input.setPrefHeight(30);
        input.setMinWidth(174);
        input.setPrefWidth(174);
        return input;
    }

    // =========================================================
    // QA PAGE TRAY
    // =========================================================

    private void renderQaPageTray() {
        qaPageTrayContainer.getChildren().clear();

        QaDocument selectedDocument = getSelectedDocument();

        if (selectedDocument == null) {
            qaTrayCountLabel.setText("0 pages");
            return;
        }

        qaTrayCountLabel.setText(selectedDocument.pages.size() + " pages");

        for (int pageIndex = 0; pageIndex < selectedDocument.pages.size(); pageIndex++) {
            QaPage page = selectedDocument.pages.get(pageIndex);

            VBox card = new VBox(3);
            card.setAlignment(Pos.CENTER);
            card.getStyleClass().add("page-tray-item");

            if (pageIndex == selectedPageIndex) {
                card.getStyleClass().add("page-tray-item-selected");
            }

            StackPane thumbnail = new StackPane();
            thumbnail.getStyleClass().add("page-tray-thumbnail");

            Region pageBlock = new Region();
            pageBlock.getStyleClass().add("qa-tray-page-block");

            Label status = new Label(getPageStatusGlyph(page.status));
            status.getStyleClass().addAll(
                    "qa-tray-status-mark",
                    getPageStatusStyleClass(page.status)
            );
            StackPane.setAlignment(status, Pos.TOP_RIGHT);

            thumbnail.getChildren().addAll(pageBlock, status);

            Label number = new Label(String.valueOf(page.pageNumber));
            number.getStyleClass().add("page-tray-number");

            card.getChildren().addAll(thumbnail, number);

            final int trayPageIndex = pageIndex;
            card.setOnMouseClicked(event -> selectQaPage(selectedDocumentIndex, trayPageIndex));

            qaPageTrayContainer.getChildren().add(card);
        }
    }

    // =========================================================
    // QA TOOLS PANEL
    // =========================================================

    private void renderQaTools() {
        QaPage page = getSelectedQaPage();

        if (page == null) {
            return;
        }

        currentPageStatusLabel.setText(getPageStatusText(page.status));
        currentPageStatusLabel.getStyleClass().removeAll(
                "qa-current-status-pending",
                "qa-current-status-approved",
                "qa-current-status-fix"
        );
        currentPageStatusLabel.getStyleClass().add(getCurrentStatusStyleClass(page.status));

        syncingQaControls = true;

        pageReadableCheckBox.setSelected(page.pageReadable);
        rotationCorrectCheckBox.setSelected(page.rotationCorrect);
        splitCorrectCheckBox.setSelected(page.splitCorrect);
        pageCountCorrectCheckBox.setSelected(page.pageCountCorrect);
        qaCommentTextArea.setText(page.comment == null ? "" : page.comment);

        syncingQaControls = false;
    }

    // =========================================================
    // QA PAGE SELECTION
    // =========================================================

    private void selectQaPage(int documentIndex, int pageIndex) {
        if (documentIndex < 0 || documentIndex >= reviewDocuments.size()) {
            return;
        }

        if (pageIndex < 0 || pageIndex >= reviewDocuments.get(documentIndex).pages.size()) {
            return;
        }

        selectedDocumentIndex = documentIndex;
        selectedPageIndex = pageIndex;

        refreshQaReviewWorkspace();
        qaPreviewHost.requestFocus();
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
        previewRotationDegrees = normalizeRotation(previewRotationDegrees - 90);
        qaSessionRotationCount++;
        renderQaPreview();
    }

    @FXML
    private void onRotateRight() {
        previewRotationDegrees = normalizeRotation(previewRotationDegrees + 90);
        qaSessionRotationCount++;
        renderQaPreview();
    }

    private int normalizeRotation(int rotationDegrees) {
        int normalized = rotationDegrees % 360;

        if (normalized < 0) {
            normalized += 360;
        }

        return normalized;
    }

    // =========================================================
    // FXML ACTIONS: QA WORK
    // =========================================================

    @FXML
    private void onApprovePage() {
        QaPage page = getSelectedQaPage();

        if (page == null) {
            return;
        }

        page.status = QaPageStatus.APPROVED;
        page.pageReadable = true;
        page.rotationCorrect = true;
        page.splitCorrect = true;
        page.pageCountCorrect = true;

        refreshQaReviewWorkspace();
    }

    @FXML
    private void onMarkNeedsFix() {
        QaPage page = getSelectedQaPage();

        if (page == null) {
            return;
        }

        page.status = QaPageStatus.NEEDS_FIX;

        if (page.comment == null || page.comment.isBlank()) {
            page.comment = "Needs correction before QA can be completed.";
        }

        refreshQaReviewWorkspace();
    }

    @FXML
    private void onCompleteQa() {
        updateSelectedAssignmentFromReview();
        recordQaSessionAuditLog();
        renderAssignments();
        showAssignedQaListView();
    }

    private void recordQaSessionAuditLog() {
        if (selectedAssignment == null) {
            return;
        }

        boolean hasIssues = selectedAssignment.issueCount > 0;
        String status = hasIssues ? "Failed" : "Success";
        String action = hasIssues ? "Rejected QA review" : "Completed QA review";
        String description = hasIssues
                ? "QA review found " + selectedAssignment.issueCount + " issue"
                        + (selectedAssignment.issueCount == 1 ? "" : "s") + "."
                : "QA review completed without issues.";

        List<AuditLogDetail> details = new ArrayList<>();
        details.add(new AuditLogDetail("Box", selectedAssignment.boxId));
        details.add(new AuditLogDetail("Profile", selectedAssignment.profile));
        details.add(new AuditLogDetail("Pages reviewed",
                selectedAssignment.reviewedPages + " / " + selectedAssignment.totalPages));
        details.add(new AuditLogDetail("Issues found", String.valueOf(selectedAssignment.issueCount)));
        details.add(new AuditLogDetail("Page rotations", String.valueOf(qaSessionRotationCount)));

        adminManager.addAuditLog("QA", action, selectedAssignment.boxId, status, description, details);

        qaSessionRotationCount = 0;
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
        private final String boxId;
        private final String profile;
        private final String scannedBy;
        private final int documentCount;
        private final int totalPages;
        private final String assignedTimeLabel;

        private int reviewedPages;
        private QaStatus status;
        private int issueCount;

        private QaAssignment(
                String boxId,
                String profile,
                String scannedBy,
                int documentCount,
                int totalPages,
                String assignedTimeLabel,
                int reviewedPages,
                QaStatus status
        ) {
            this(
                    boxId,
                    profile,
                    scannedBy,
                    documentCount,
                    totalPages,
                    assignedTimeLabel,
                    reviewedPages,
                    status,
                    0
            );
        }

        private QaAssignment(
                String boxId,
                String profile,
                String scannedBy,
                int documentCount,
                int totalPages,
                String assignedTimeLabel,
                int reviewedPages,
                QaStatus status,
                int issueCount
        ) {
            this.boxId = Objects.requireNonNull(boxId);
            this.profile = Objects.requireNonNull(profile);
            this.scannedBy = Objects.requireNonNull(scannedBy);
            this.documentCount = documentCount;
            this.totalPages = totalPages;
            this.assignedTimeLabel = Objects.requireNonNull(assignedTimeLabel);
            this.reviewedPages = reviewedPages;
            this.status = Objects.requireNonNull(status);
            this.issueCount = issueCount;
        }
    }

    private static final class QaDocument {
        private final String name;
        private final List<QaPage> pages = new ArrayList<>();
        private boolean expanded = true;

        private QaDocument(String name) {
            this.name = name;
        }
    }

    private static final class QaPage {
        private final int pageNumber;
        private final int globalPageNumber;

        private QaPageStatus status = QaPageStatus.NOT_REVIEWED;
        private boolean pageReadable = false;
        private boolean rotationCorrect = false;
        private boolean splitCorrect = false;
        private boolean pageCountCorrect = false;
        private String comment = "";

        private QaPage(int pageNumber, int globalPageNumber) {
            this.pageNumber = pageNumber;
            this.globalPageNumber = globalPageNumber;
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
}
