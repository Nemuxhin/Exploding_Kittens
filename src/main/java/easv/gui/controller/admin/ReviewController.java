package easv.gui.controller.admin;

import easv.be.Document;
import easv.be.ReviewRecord;
import easv.be.ScanProfile;
import easv.be.User;
import easv.bll.AdminManager;
import easv.bll.QAService;
import easv.bll.TiffExportManager;
import easv.bll.TiffImageSupport;
import easv.gui.controller.util.AppDates;
import easv.gui.controller.util.BackgroundExecutor;
import easv.gui.controller.util.PaginationHelper;
import easv.gui.controller.util.PrimeIcons;
import easv.gui.controller.util.SearchableComboBoxes;
import easv.gui.controller.util.SkeletonFactory;
import easv.gui.controller.util.Strings;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.embed.swing.SwingFXUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class ReviewController {

    private static final String ALL_CLIENTS = "All Clients";
    private static final String ALL_ARCHIVES = "All Archives";
    private static final String ALL_PROFILES = "All Profiles";
    private static final String ALL_QA_STATUSES = "All QA Statuses";
    private static final String ALL_USERS = "All Users";
    private static final String RANGE_LAST_30_DAYS = "Last 30 Days";
    private static final String RANGE_TODAY = "Today";
    private static final String RANGE_LAST_7_DAYS = "Last 7 Days";
    private static final String RANGE_ALL_TIME = "All Time";
    private static final String RANGE_CUSTOM = "Custom Range";

    private static final int DEFAULT_ROWS_PER_PAGE = 10;
    private static final List<Integer> ROWS_PER_PAGE_OPTIONS = List.of(10, 25, 50);
    private static final DateTimeFormatter DATE_RANGE_FORMATTER = AppDates.FORMATTER;
    private static final double PREVIEW_PAGE_WIDTH = 500;
    private static final double PREVIEW_PAGE_HEIGHT = 560;
    private static final double MIN_PREVIEW_ZOOM = 0.50;
    private static final double MAX_PREVIEW_ZOOM = 2.50;
    private static final double PREVIEW_ZOOM_STEP = 0.10;
    private static final double PREVIEW_NUDGE_AMOUNT = 36;
    private static final double PREVIEW_SAFE_HORIZONTAL_PADDING = 132;
    private static final double PREVIEW_SAFE_VERTICAL_PADDING = 72;

    private final ObservableList<ReviewRow> records = FXCollections.observableArrayList();
    private final Set<String> selectedRecordIds = new HashSet<>();

    private List<ReviewRow> filteredRecords = List.of();
    private List<ReviewRow> pageRecords = List.of();

    private int currentPage = 1;
    private int rowsPerPage = DEFAULT_ROWS_PER_PAGE;
    private boolean updatingDateControls;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDate pendingRangeStart;

    private ContextMenu dateRangeCalendarMenu;
    private GridPane dateRangeCalendarGrid;
    private Label dateRangeCalendarMonthLabel;
    private YearMonth displayedDateRangeMonth = YearMonth.now();

    private record ParsedDateRange(LocalDate startDate, LocalDate endDate) {}

    private ReviewRow activeReviewRecord;
    private ReviewQueueFilter activeQueueFilter = ReviewQueueFilter.ALL;
    private AdminManager adminManager;
    private boolean recordsLoading;
    private long recordsLoadSequence;

    @FXML private VBox overviewPane;
    @FXML private VBox workspacePane;
    @FXML private ScrollPane reviewRootScrollPane;

    @FXML private TextField searchField;

    @FXML private ComboBox<String> clientFilterComboBox;
    @FXML private ComboBox<String> archiveFilterComboBox;
    @FXML private ComboBox<String> profileFilterComboBox;
    @FXML private ComboBox<String> qaStatusFilterComboBox;
    @FXML private ComboBox<String> dateRangeFilterComboBox;
    @FXML private HBox dateRangeBox;
    @FXML private TextField dateRangeField;
    @FXML private Button dateRangeMenuButton;
    @FXML private ComboBox<String> scannedByFilterComboBox;

    @FXML private HBox batchActionBar;
    @FXML private Label batchSelectionLabel;

    @FXML private Label missingRequiredCountLabel;
    @FXML private Label exportBlockedCountLabel;
    @FXML private Label failedValidationCountLabel;
    @FXML private Label readyForQaCountLabel;
    @FXML private Label qaRejectedCountLabel;
    @FXML private Label recentlyScannedCountLabel;

    @FXML private VBox resultsRowsContainer;
    @FXML private VBox emptyStateBox;

    @FXML private HBox paginationBar;
    @FXML private Label paginationSummaryLabel;
    @FXML private HBox paginationButtonsBox;
    @FXML private ComboBox<Integer> rowsPerPageComboBox;

    @FXML private Label workspaceTitleLabel;
    @FXML private Label workspaceSubtitleLabel;
    @FXML private Label workspaceWarningLabel;
    @FXML private BorderPane reviewWorkspaceView;
    @FXML private Label reviewReferenceInfoLabel;
    @FXML private Label reviewFilesInfoLabel;
    @FXML private Label reviewDocumentsInfoLabel;
    @FXML private Label reviewZoomLabel;
    @FXML private Label reviewStatusBadge;
    @FXML private Label reviewBoxValueLabel;
    @FXML private Label reviewProfileValueLabel;
    @FXML private Label reviewDocumentsValueLabel;
    @FXML private Label reviewSidebarSubtitleLabel;
    @FXML private VBox reviewDocumentListContainer;
    @FXML private StackPane reviewPreviewHost;
    @FXML private HBox reviewPageTrayContainer;
    @FXML private TextArea qaCommentTextArea;
    @FXML private ComboBox<String> qaActionScopeComboBox;
    @FXML private Label reviewSelectionProfileValueLabel;
    @FXML private Label reviewSelectionBoxValueLabel;
    @FXML private Label reviewSelectionDocumentValueLabel;
    @FXML private Label reviewSelectionFileValueLabel;
    @FXML private Label reviewSelectionReferenceValueLabel;
    @FXML private Label reviewSelectionFileIdValueLabel;
    @FXML private Label reviewSelectedTitleLabel;
    @FXML private TextField reviewWorkspaceSearchField;
    @FXML private VBox reviewWorkspaceTreeContainer;
    @FXML private Label reviewWorkspaceViewerPageLabel;
    @FXML private StackPane reviewWorkspacePreviewHost;
    @FXML private HBox reviewWorkspaceThumbnailStrip;
    @FXML private Button reviewZoomOutButton;
    @FXML private Button reviewZoomInButton;
    @FXML private Button reviewFitButton;
    @FXML private Button reviewPrevPageButton;
    @FXML private Button reviewNextPageButton;
    @FXML private Button reviewBarcodeButton;

    @FXML private Button boxTabButton;
    @FXML private Button caseTabButton;
    @FXML private Button documentTabButton;
    @FXML private Button pageTabButton;

    @FXML private ComboBox<String> documentTypeComboBox;
    @FXML private ComboBox<String> departmentComboBox;

    @FXML private TextField caseNumberField;
    @FXML private TextField registrationDateField;
    @FXML private TextField buildingAddressField;
    @FXML private TextArea notesTextArea;

    private QAService.QaAssignmentSnapshot activeQaAssignment;
    private List<WorkspaceQaDocument> activeQaDocuments = List.of();
    private int selectedQaDocumentIndex = 0;
    private int selectedQaPageIndex = 0;
    private double reviewPreviewZoomMultiplier = 1.0;
    private double reviewPreviewTranslateX = 0;
    private double reviewPreviewTranslateY = 0;
    private double reviewPreviewDragStartX = 0;
    private double reviewPreviewDragStartY = 0;
    private double reviewPreviewTranslateStartX = 0;
    private double reviewPreviewTranslateStartY = 0;
    private StackPane currentReviewPreviewWrapper;
    private double currentReviewPreviewBaseWidth = 1.0;
    private double currentReviewPreviewBaseHeight = 1.0;
    private boolean syncingWorkspaceQaControls;
    private final Set<Integer> collapsedReviewDocuments = new HashSet<>();

    @FXML
    private void initialize() {
        configureFilters();
        configureWorkspaceControls();
        configureWorkspaceQaControls();
        configureRowsPerPageSelector();
        loadRecords();
        configureListeners();
        configureWorkspacePreviewInteractions();

        showOverview();
        setActiveWorkspaceTab(documentTabButton);
        applyFilters();
    }

    void setAdminManager(AdminManager adminManager) {
        this.adminManager = adminManager;
        if (this.adminManager == null) {
            return;
        }
        loadRecords();
        applyFilters();
    }

    private void configureFilters() {
        configureDateRangeField();

        // Client / archive / profile / scannedBy options are data-driven —
        // refreshFilterOptions() rebuilds them from the loaded records.
        if (clientFilterComboBox != null) {
            clientFilterComboBox.getItems().setAll(ALL_CLIENTS);
            clientFilterComboBox.setValue(ALL_CLIENTS);
        }

        if (archiveFilterComboBox != null) {
            archiveFilterComboBox.getItems().setAll(ALL_ARCHIVES);
            archiveFilterComboBox.setValue(ALL_ARCHIVES);
        }

        if (profileFilterComboBox != null) {
            profileFilterComboBox.getItems().setAll(ALL_PROFILES);
            profileFilterComboBox.setValue(ALL_PROFILES);
        }

        if (qaStatusFilterComboBox != null) {
            qaStatusFilterComboBox.getItems().setAll(
                    ALL_QA_STATUSES,
                    "Not Started",
                    "Waiting for QA",
                    "QA In Progress",
                    "QA Approved",
                    "QA Rejected",
                    "Ready for QA"
            );
            qaStatusFilterComboBox.setValue(ALL_QA_STATUSES);
        }

        if (dateRangeFilterComboBox != null) {
            dateRangeFilterComboBox.getItems().setAll(
                    RANGE_TODAY,
                    RANGE_LAST_7_DAYS,
                    RANGE_LAST_30_DAYS,
                    RANGE_ALL_TIME
            );
        }
        setDateRange(RANGE_TODAY, LocalDate.now(), LocalDate.now());

        if (scannedByFilterComboBox != null) {
            scannedByFilterComboBox.getItems().setAll(ALL_USERS);
            scannedByFilterComboBox.setValue(ALL_USERS);
        }

        if (clientFilterComboBox != null) {
            SearchableComboBoxes.configure(clientFilterComboBox);
        }
        if (archiveFilterComboBox != null) {
            SearchableComboBoxes.configure(archiveFilterComboBox);
        }
        if (profileFilterComboBox != null) {
            SearchableComboBoxes.configure(profileFilterComboBox);
        }
        if (qaStatusFilterComboBox != null) {
            SearchableComboBoxes.configure(qaStatusFilterComboBox);
        }
        if (dateRangeFilterComboBox != null) {
            SearchableComboBoxes.configure(dateRangeFilterComboBox);
        }
        if (scannedByFilterComboBox != null) {
            SearchableComboBoxes.configure(scannedByFilterComboBox);
        }
    }

    private void configureWorkspaceControls() {
        configureReviewToolbarButtons();

        if (documentTypeComboBox == null || departmentComboBox == null
                || caseNumberField == null || registrationDateField == null
                || buildingAddressField == null || notesTextArea == null) {
            return;
        }

        documentTypeComboBox.getItems().setAll(
                "Building Permit",
                "Inspection Report",
                "Planning Document",
                "Technical Drawing",
                "Legal Record"
        );

        departmentComboBox.getItems().setAll(
                "Technical Services",
                "Planning",
                "Municipal Archive",
                "Legal Department"
        );
        SearchableComboBoxes.configure(documentTypeComboBox);

        caseNumberField.clear();
        registrationDateField.clear();
        buildingAddressField.clear();
        notesTextArea.clear();
    }

    private void configureRowsPerPageSelector() {
        rowsPerPageComboBox.getItems().setAll(ROWS_PER_PAGE_OPTIONS);
        rowsPerPageComboBox.setValue(DEFAULT_ROWS_PER_PAGE);

        rowsPerPageComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue <= 0) {
                return;
            }

            rowsPerPage = newValue;
            currentPage = 1;
            renderRows();
        });
    }

    private void configureListeners() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        if (reviewWorkspaceSearchField != null) {
            reviewWorkspaceSearchField.textProperty().addListener((observable, oldValue, newValue) -> renderWorkspaceQaTree());
        }

        if (clientFilterComboBox != null) {
            clientFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        }
        if (archiveFilterComboBox != null) {
            archiveFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        }
        if (profileFilterComboBox != null) {
            profileFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        }
        if (qaStatusFilterComboBox != null) {
            qaStatusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        }
        if (dateRangeFilterComboBox != null) {
            dateRangeFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (updatingDateControls) {
                    return;
                }

                applyPresetDateRange(newValue);
                applyFilters();
            });
        }
        if (scannedByFilterComboBox != null) {
            scannedByFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        }
    }

    private void configureWorkspacePreviewInteractions() {
        StackPane previewHost = activeWorkspacePreviewHost();
        if (previewHost == null) {
            return;
        }

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(previewHost.widthProperty());
        clip.heightProperty().bind(previewHost.heightProperty());
        previewHost.setClip(clip);

        previewHost.setFocusTraversable(true);
        previewHost.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleWorkspacePreviewMousePressed);
        previewHost.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleWorkspacePreviewMouseDragged);
        previewHost.addEventFilter(ScrollEvent.SCROLL, this::handleWorkspacePreviewScroll);
        previewHost.widthProperty().addListener((observable, oldValue, newValue) -> clampWorkspacePreviewTranslation());
        previewHost.heightProperty().addListener((observable, oldValue, newValue) -> clampWorkspacePreviewTranslation());
    }

    private void configureReviewToolbarButtons() {
        if (reviewZoomOutButton != null) {
            Label minusLabel = new Label("-");
            minusLabel.setStyle("-fx-text-fill: -wl-text; -fx-font-size: 20px; -fx-font-weight: 600;");
            reviewZoomOutButton.setText("");
            reviewZoomOutButton.setGraphic(minusLabel);
            reviewZoomOutButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }
        if (reviewZoomInButton != null) {
            applyToolbarGlyph(reviewZoomInButton, "M5 0 H7 V5 H12 V7 H7 V12 H5 V7 H0 V5 H5 Z", 12, 12);
        }
        if (reviewFitButton != null) {
            applyToolbarGlyph(reviewFitButton, "M6 0 L11 5 H8 V12 H4 V5 H1 Z", 12, 12);
        }
        if (reviewPrevPageButton != null) {
            applyToolbarGlyph(reviewPrevPageButton, "M0 6 L5 1 V4 H12 V8 H5 V11 Z", 12, 12);
        }
        if (reviewNextPageButton != null) {
            applyToolbarGlyph(reviewNextPageButton, "M12 6 L7 1 V4 H0 V8 H7 V11 Z", 12, 12);
        }
        if (reviewBarcodeButton != null) {
            applyToolbarGlyph(reviewBarcodeButton, "M1 6 H4 V1 H8 V6 H11 L6 12 Z", 12, 12);
        }
    }

    private void applyToolbarGlyph(Button button, String shape, double width, double height) {
        Region glyph = new Region();
        glyph.setMinSize(width, height);
        glyph.setPrefSize(width, height);
        glyph.setMaxSize(width, height);
        glyph.setStyle("-fx-background-color: -wl-text; -fx-shape: \"" + shape + "\";");
        button.setText("");
        button.setGraphic(glyph);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    private void applyFilters() {
        currentPage = 1;
        renderRows();
    }

    private void renderRows() {
        if (recordsLoading) {
            SkeletonFactory.stopShimmers(resultsRowsContainer);
            List<GridPane> skeletonRows = new ArrayList<>();
            for (int rowIndex = 0; rowIndex < rowsPerPage; rowIndex++) {
                skeletonRows.add(buildSkeletonTableRow());
            }
            resultsRowsContainer.getChildren().setAll(skeletonRows);
            resultsRowsContainer.setVisible(true);
            resultsRowsContainer.setManaged(true);
            emptyStateBox.setVisible(false);
            emptyStateBox.setManaged(false);
            paginationBar.setVisible(false);
            paginationBar.setManaged(false);
            updateBatchBar();
            return;
        }

        filteredRecords = records.stream()
                .filter(this::matchesSearch)
                .filter(this::matchesFilters)
                .toList();

        int totalRecords = filteredRecords.size();
        PaginationHelper.PageSlice pageSlice = PaginationHelper.slice(currentPage, rowsPerPage, totalRecords);

        currentPage = pageSlice.currentPage();

        pageRecords = filteredRecords.subList(pageSlice.fromIndex(), pageSlice.toIndex());

        SkeletonFactory.stopShimmers(resultsRowsContainer);
        resultsRowsContainer.getChildren().setAll(
                pageRecords.stream()
                        .map(this::buildTableRow)
                        .toList()
        );

        updateEmptyState(totalRecords);
        PaginationHelper.renderInto(paginationButtonsBox, paginationSummaryLabel, pageSlice,
                totalRecords, "records", page -> {
                    currentPage = page;
                    renderRows();
                });
        updateBatchBar();
        updateSummaryCards();
    }

    private GridPane buildSkeletonTableRow() {
        GridPane row = new GridPane();
        row.getStyleClass().add("review-table-row");
        row.getColumnConstraints().setAll(createTableColumns());
        addCell(row, SkeletonFactory.line(120, 12), 0, HPos.LEFT);
        addCell(row, SkeletonFactory.line(80, 12), 1, HPos.LEFT);
        addCell(row, SkeletonFactory.line(96, 12), 2, HPos.LEFT);
        addCell(row, SkeletonFactory.line(72, 16), 3, HPos.CENTER);
        addCell(row, SkeletonFactory.line(28, 12), 4, HPos.CENTER);
        addCell(row, SkeletonFactory.line(80, 12), 5, HPos.LEFT);
        addCell(row, SkeletonFactory.line(96, 12), 6, HPos.CENTER);
        addCell(row, SkeletonFactory.line(56, 14), 7, HPos.LEFT);
        addCell(row, SkeletonFactory.line(56, 14), 8, HPos.LEFT);
        return row;
    }

    private void updateSummaryCards() {
        missingRequiredCountLabel.setText(String.valueOf(countRecords(this::isMissingRequired)));
        exportBlockedCountLabel.setText(String.valueOf(countRecords(this::isExportBlocked)));
        failedValidationCountLabel.setText(String.valueOf(countRecords(this::isFailedValidation)));
        readyForQaCountLabel.setText(String.valueOf(countRecords(this::isReadyForQa)));
        qaRejectedCountLabel.setText(String.valueOf(countRecords(this::isQaRejected)));
        recentlyScannedCountLabel.setText(String.valueOf(countRecords(this::isRecentlyScanned)));
    }

    private long countRecords(java.util.function.Predicate<ReviewRow> predicate) {
        return records.stream()
                .filter(predicate)
                .count();
    }

    private boolean isMissingRequired(ReviewRow record) {
        return "Missing Required Fields".equalsIgnoreCase(record.documentDetailsStatus());
    }

    private boolean isExportBlocked(ReviewRow record) {
        return isMissingRequired(record)
                || "Invalid".equalsIgnoreCase(record.documentDetailsStatus())
                || "Incomplete".equalsIgnoreCase(record.documentDetailsStatus());
    }

    private boolean isFailedValidation(ReviewRow record) {
        return "Invalid".equalsIgnoreCase(record.documentDetailsStatus());
    }

    private boolean isReadyForQa(ReviewRow record) {
        return "Complete".equalsIgnoreCase(record.documentDetailsStatus())
                && "Ready for QA".equalsIgnoreCase(record.qaStatus());
    }

    private boolean isQaRejected(ReviewRow record) {
        return "QA Rejected".equalsIgnoreCase(record.qaStatus());
    }

    private boolean isRecentlyScanned(ReviewRow record) {
        return "Today".equalsIgnoreCase(record.dateGroup());
    }

    private GridPane buildTableRow(ReviewRow record) {
        GridPane row = new GridPane();
        row.getStyleClass().add("review-table-row");

        if (record.warning()) {
            row.getStyleClass().add("review-table-row-warning");
        }

        row.getColumnConstraints().setAll(createTableColumns());
        addCell(row, createWrappedLabel(record.identity(), "review-main-cell"), 0, HPos.LEFT);
        addCell(row, createWrappedLabel(record.client(), "review-cell-text"), 1, HPos.LEFT);
        addCell(row, createWrappedLabel(record.profile(), "review-cell-text"), 2, HPos.LEFT);
        addCell(row, createStatusBadge(record.qaStatus()), 3, HPos.CENTER);
        addCell(row, createWrappedLabel(String.valueOf(record.pages()), "review-cell-text"), 4, HPos.CENTER);
        addCell(row, createWrappedLabel(record.lastUpdated(), "review-cell-text"), 5, HPos.LEFT);
        addCell(row, createWrappedLabel(record.assignedTo(), "review-cell-text"), 6, HPos.CENTER);
        addCell(row, createAssignQaButton(record), 7, HPos.LEFT);
        addCell(row, createReviewButton(record), 8, HPos.LEFT);

        return row;
    }

    private List<ColumnConstraints> createTableColumns() {
        return List.of(
                createPercentColumn(17),
                createPercentColumn(13),
                createPercentColumn(13),
                createPercentColumn(12),
                createPercentColumn(5),
                createPercentColumn(11),
                createPercentColumn(13),
                createPercentColumn(8),
                createPercentColumn(8)
        );
    }

    private ColumnConstraints createPercentColumn(double percentWidth) {
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(percentWidth);
        column.setMinWidth(0);
        column.setFillWidth(true);
        column.setHgrow(Priority.ALWAYS);
        return column;
    }

    private void addCell(GridPane row, Node content, int columnIndex, HPos alignment) {
        if (content instanceof Region region) {
            region.setMinWidth(0);
            region.setMaxWidth(Double.MAX_VALUE);
        }

        StackPane wrapper = new StackPane(content);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setAlignment(toPos(alignment));

        GridPane.setHgrow(wrapper, Priority.ALWAYS);
        GridPane.setFillWidth(wrapper, true);
        GridPane.setHalignment(wrapper, alignment);
        GridPane.setValignment(wrapper, VPos.CENTER);

        row.add(wrapper, columnIndex, 0);
    }

    private Pos toPos(HPos alignment) {
        return switch (alignment) {
            case CENTER -> Pos.CENTER;
            case RIGHT -> Pos.CENTER_RIGHT;
            default -> Pos.CENTER_LEFT;
        };
    }

    private Label createWrappedLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label createStatusBadge(String status) {
        Label badge = new Label(status);
        badge.getStyleClass().add("review-status-badge");
        badge.getStyleClass().add(qaStatusClass(status));

        badge.setWrapText(true);
        badge.setMinWidth(0);
        badge.setMaxWidth(Double.MAX_VALUE);

        return badge;
    }

    private Button createReviewButton(ReviewRow record) {
        Button button = new Button("Review");
        button.getStyleClass().add("review-action-button");
        button.setFocusTraversable(false);
        button.setMinWidth(0);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setOnAction(event -> openReviewWorkspace(record));
        return button;
    }

    private Button createAssignQaButton(ReviewRow record) {
        Button button = new Button("Assign QA");
        button.getStyleClass().add("review-action-button");
        button.setFocusTraversable(false);
        button.setMinWidth(0);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setOnAction(event -> assignQaFromOverview(record));
        return button;
    }

    private void assignQaFromOverview(ReviewRow record) {
        if (record == null || adminManager == null) {
            return;
        }

        if (isQaReviewRow(record)) {
            chooseQaAssignee(record.id(), selectedAssignee -> {
                if (selectedAssignee == null) {
                    return;
                }

                ReviewRecord updatedRecord = adminManager.assignReviewRecordToQa(
                        record.id(),
                        selectedAssignee.userId()
                );

                if (updatedRecord == null) {
                    return;
                }

                replaceRecord(toReviewRow(updatedRecord));
                renderRows();
            });
            return;
        }

        ReviewRow updated = record.withAssignedTo("QA Team");
        adminManager.saveReviewRecord(toReviewRecord(updated));
        replaceRecord(updated);
        renderRows();
    }

    private void updateSelection(String recordId, boolean selected) {
        if (selected) {
            selectedRecordIds.add(recordId);
        } else {
            selectedRecordIds.remove(recordId);
        }

        updateBatchBar();
    }

    @FXML
    private void clearSelection() {
        selectedRecordIds.clear();
        renderRows();
    }

    @FXML
    private void assignSelectedToQa() {
        if (selectedRecordIds.isEmpty()) {
            return;
        }

        List<String> qaRecordIds = List.copyOf(selectedRecordIds).stream()
                .filter(recordId -> recordId != null && recordId.startsWith("qa:"))
                .toList();

        if (qaRecordIds.isEmpty()) {
            performBulkQaAssignment(null);
            return;
        }

        chooseQaAssignee(qaRecordIds.get(0), selectedAssignee -> {
            if (selectedAssignee == null) {
                return;
            }
            performBulkQaAssignment(selectedAssignee);
        });
    }

    private void performBulkQaAssignment(QaAssigneeOption selectedAssignee) {
        boolean changed = false;
        for (String recordId : List.copyOf(selectedRecordIds)) {
            if (recordId != null && recordId.startsWith("qa:")) {
                ReviewRecord updatedRecord = adminManager.assignReviewRecordToQa(
                        recordId,
                        selectedAssignee == null ? null : selectedAssignee.userId()
                );
                if (updatedRecord == null) {
                    continue;
                }
                replaceRecord(toReviewRow(updatedRecord));
                changed = true;
            } else {
                for (int index = 0; index < records.size(); index++) {
                    ReviewRow existing = records.get(index);
                    if (!existing.id().equals(recordId)) {
                        continue;
                    }
                    ReviewRow updated = existing.withAssignedTo("QA Team");
                    adminManager.saveReviewRecord(toReviewRecord(updated));
                    records.set(index, updated);
                    changed = true;
                    break;
                }
            }
        }

        if (changed) {
            selectedRecordIds.clear();
            renderRows();
        }
    }

    @FXML
    private void markSelectedReadyForQa() {
        updateSelectedRecords(record -> record.withReviewState("Complete", "Ready for QA", false));
    }

    @FXML
    private void exportSelected() {
        if (selectedRecordIds.isEmpty() || adminManager == null) {
            return;
        }

        List<ReviewRow> selectedRows = records.stream()
                .filter(row -> selectedRecordIds.contains(row.id()))
                .toList();

        Path outputDirectory = chooseExportDirectory("review-export");
        if (outputDirectory == null) {
            return;
        }

        TiffExportManager tiffExportManager = new TiffExportManager();
        int exportedRecords = 0;
        int filesWritten = 0;
        List<String> skipped = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (ReviewRow row : selectedRows) {
            List<Document> documents = adminManager.getExportableDocumentsForRecord(row.id());
            if (documents.isEmpty()) {
                skipped.add(row.identity());
                continue;
            }

            ScanProfile profile = adminManager.findProfileByName(row.profile());
            String profileCode = firstNonBlank(profile == null ? null : profile.getCode(), row.profile());
            String exportNaming = firstNonBlank(
                    profile == null ? null : profile.getExportNaming(),
                    ScanProfile.DEFAULT_EXPORT_NAMING
            );

            try {
                Path recordDirectory = outputDirectory.resolve(safeFolderSegment(row.profile(), row.identity()));
                TiffExportManager.ExportResult result = tiffExportManager.exportPlan(
                        tiffExportManager.createMultiPagePlan(
                                row.profile(),
                                profileCode,
                                exportNaming,
                                row.identity(),
                                documents
                        ),
                        recordDirectory
                );
                exportedRecords++;
                filesWritten += result.writtenFiles().size();
            } catch (IOException | RuntimeException exception) {
                failures.add(row.identity() + ": " + (exception.getMessage() == null ? "unknown error" : exception.getMessage()));
            }
        }

        String status = failures.isEmpty() && exportedRecords > 0 ? "Success" : "Failed";
        StringBuilder description = new StringBuilder()
                .append(exportedRecords).append(" of ").append(selectedRows.size()).append(" records exported (")
                .append(filesWritten).append(" TIFF ").append(filesWritten == 1 ? "file" : "files")
                .append(") to ").append(outputDirectory);

        if (!skipped.isEmpty()) {
            description.append(" • Skipped (not QA approved): ").append(String.join(", ", skipped));
        }
        if (!failures.isEmpty()) {
            description.append(" • Failed: ").append(String.join(" | ", failures));
        }

        adminManager.addAuditLog("Exports", "Exported selected review items", "Review Center", status,
                description.toString());

        Alert.AlertType alertType = "Success".equals(status) ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING;
        showExportAlert(alertType, "Export selected", description.toString());

        if (exportedRecords > 0) {
            selectedRecordIds.clear();
            renderRows();
        }

        paginationSummaryLabel.setText("Exported " + exportedRecords + " of " + selectedRows.size() + " selected records");
    }

    private Path chooseExportDirectory(String defaultSubfolder) {
        Path defaultDirectory = Path.of(
                System.getProperty("user.home"),
                "Downloads",
                "WebLager Exports",
                defaultSubfolder + "_" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
        );

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose export folder");
        Path parentForChooser = defaultDirectory.getParent();
        if (parentForChooser != null && parentForChooser.toFile().isDirectory()) {
            chooser.setInitialDirectory(parentForChooser.toFile());
        }

        Node ownerNode = overviewPane != null ? overviewPane : workspacePane;
        java.io.File chosen = chooser.showDialog(ownerNode == null || ownerNode.getScene() == null
                ? null
                : ownerNode.getScene().getWindow());

        if (chosen == null) {
            return null;
        }

        return chosen.toPath().resolve(defaultSubfolder + "_" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()));
    }

    private String safeFolderSegment(String profileName, String identity) {
        String profilePart = firstNonBlank(profileName, "profile").replaceAll("[^a-zA-Z0-9._-]", "_");
        String identityPart = firstNonBlank(identity, "record").replaceAll("[^a-zA-Z0-9._-]", "_");
        return profilePart + "_" + identityPart;
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred.trim();
    }

    private void showExportAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null || message.isBlank() ? "Operation completed." : message);
        Node ownerNode = overviewPane != null ? overviewPane : workspacePane;
        if (ownerNode != null && ownerNode.getScene() != null) {
            alert.initOwner(ownerNode.getScene().getWindow());
        }
        alert.showAndWait();
    }

    private void updateBatchBar() {
        int selectedCount = selectedRecordIds.size();
        boolean hasSelection = selectedCount > 0;

        batchActionBar.setVisible(hasSelection);
        batchActionBar.setManaged(hasSelection);

        batchSelectionLabel.setText(selectedCount == 1 ? "1 selected" : selectedCount + " selected");
    }

    private void updateSelectedRecords(UnaryOperator<ReviewRow> updater) {
        if (selectedRecordIds.isEmpty()) {
            return;
        }

        for (int index = 0; index < records.size(); index++) {
            ReviewRow record = records.get(index);

            if (selectedRecordIds.contains(record.id())) {
                ReviewRow updatedRecord = updater.apply(record);
                adminManager.saveReviewRecord(toReviewRecord(updatedRecord));
                records.set(index, updatedRecord);
            }
        }

        selectedRecordIds.clear();
        renderRows();
    }

    private void updateEmptyState(int totalRecords) {
        boolean hasRows = totalRecords > 0;

        resultsRowsContainer.setVisible(hasRows);
        resultsRowsContainer.setManaged(hasRows);

        emptyStateBox.setVisible(!hasRows);
        emptyStateBox.setManaged(!hasRows);

        paginationBar.setVisible(hasRows);
        paginationBar.setManaged(hasRows);
    }

    private boolean matchesSearch(ReviewRow record) {
        String searchText = Strings.normalize(searchField.getText());

        if (searchText.isBlank()) {
            return true;
        }

        return Strings.normalize(record.identity()).contains(searchText)
                || Strings.normalize(record.client()).contains(searchText)
                || Strings.normalize(record.archive()).contains(searchText)
                || Strings.normalize(record.profile()).contains(searchText)
                || Strings.normalize(record.qaStatus()).contains(searchText)
                || Strings.normalize(record.assignedTo()).contains(searchText)
                || Strings.normalize(record.scannedBy()).contains(searchText)
                || Strings.normalize(record.lastUpdated()).contains(searchText)
                || Strings.normalize(record.dateGroup()).contains(searchText)
                || String.valueOf(record.pages()).contains(searchText);
    }

    private boolean matchesFilters(ReviewRow record) {
        return matchesCombo(record.client(), selectedValue(clientFilterComboBox, ALL_CLIENTS), ALL_CLIENTS)
                && matchesCombo(record.archive(), selectedValue(archiveFilterComboBox, ALL_ARCHIVES), ALL_ARCHIVES)
                && matchesCombo(record.profile(), selectedValue(profileFilterComboBox, ALL_PROFILES), ALL_PROFILES)
                && matchesQueueFilter(record)
                && matchesCombo(record.qaStatus(), selectedValue(qaStatusFilterComboBox, ALL_QA_STATUSES), ALL_QA_STATUSES)
                && matchesCombo(record.scannedBy(), selectedValue(scannedByFilterComboBox, ALL_USERS), ALL_USERS)
                && matchesDateRange(record);
    }

    private boolean matchesQueueFilter(ReviewRow record) {
        return switch (activeQueueFilter) {
            case ALL -> true;
            case MISSING_REQUIRED -> isMissingRequired(record);
            case EXPORT_BLOCKED -> isExportBlocked(record);
            case FAILED_VALIDATION -> isFailedValidation(record);
            case READY_FOR_QA -> isReadyForQa(record);
            case QA_REJECTED -> isQaRejected(record);
            case RECENTLY_SCANNED -> isRecentlyScanned(record);
        };
    }

    private boolean matchesCombo(String value, String selectedValue, String allValue) {
        return selectedValue == null
                || allValue.equals(selectedValue)
                || value.equalsIgnoreCase(selectedValue);
    }

    private boolean matchesDateRange(ReviewRow record) {
        if (RANGE_ALL_TIME.equals(selectedValue(dateRangeFilterComboBox, RANGE_ALL_TIME))) {
            return true;
        }

        LocalDate recordDate = parseReviewDate(record);

        if (recordDate == null) {
            return false;
        }

        LocalDate rangeStart = fromDate;
        LocalDate rangeEnd = toDate;

        if (rangeStart == null && rangeEnd == null) {
            return true;
        }

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            LocalDate swappedDate = rangeStart;
            rangeStart = rangeEnd;
            rangeEnd = swappedDate;
        }

        boolean afterStart = rangeStart == null || !recordDate.isBefore(rangeStart);
        boolean beforeEnd = rangeEnd == null || !recordDate.isAfter(rangeEnd);

        return afterStart && beforeEnd;
    }

    private LocalDate parseReviewDate(ReviewRow record) {
        LocalDate updatedDate = parseDateText(record.lastUpdated());

        if (updatedDate != null) {
            return updatedDate;
        }

        return parseDateText(record.dateGroup());
    }

    private LocalDate parseDateText(String value) {
        String dateText = value == null ? "" : value.trim();
        String normalizedDateText = Strings.normalize(dateText);

        if (dateText.isBlank()) {
            return null;
        }

        return switch (normalizedDateText) {
            case "today", "saved just now", "updated just now" -> LocalDate.now();
            case "yesterday" -> LocalDate.now().minusDays(1);
            case "last 7 days" -> LocalDate.now().minusDays(7);
            case "last 30 days" -> LocalDate.now().minusDays(30);
            default -> parseFormattedDate(dateText);
        };
    }

    private LocalDate parseFormattedDate(String dateText) {
        List<DateTimeFormatter> dateFormatters = List.of(
                DATE_RANGE_FORMATTER,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ISO_LOCAL_DATE
        );

        for (DateTimeFormatter formatter : dateFormatters) {
            try {
                return LocalDate.parse(dateText, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        List<DateTimeFormatter> dateTimeFormatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        );

        for (DateTimeFormatter formatter : dateTimeFormatters) {
            try {
                return LocalDateTime.parse(dateText, formatter).toLocalDate();
            } catch (DateTimeParseException ignored) {
            }
        }

        return null;
    }

    private String qaStatusClass(String status) {
        return switch (Strings.normalize(status)) {
            case "not started" -> "review-status-neutral";
            case "waiting for qa", "qa in progress", "ready for qa" -> "review-status-info";
            case "qa approved" -> "review-status-success";
            case "qa rejected" -> "review-status-danger";
            default -> "review-status-neutral";
        };
    }

    @FXML
    private void clearFilters() {
        searchField.clear();

        if (clientFilterComboBox != null) {
            clientFilterComboBox.setValue(ALL_CLIENTS);
        }
        if (archiveFilterComboBox != null) {
            archiveFilterComboBox.setValue(ALL_ARCHIVES);
        }
        if (profileFilterComboBox != null) {
            profileFilterComboBox.setValue(ALL_PROFILES);
        }
        if (qaStatusFilterComboBox != null) {
            qaStatusFilterComboBox.setValue(ALL_QA_STATUSES);
        }
        setDateRange(RANGE_LAST_30_DAYS, LocalDate.now().minusDays(30), LocalDate.now());
        if (scannedByFilterComboBox != null) {
            scannedByFilterComboBox.setValue(ALL_USERS);
        }
        activeQueueFilter = ReviewQueueFilter.ALL;

        selectedRecordIds.clear();
        currentPage = 1;
        renderRows();
    }

    @FXML
    private void showMissingRequiredQueue() {
        activeQueueFilter = ReviewQueueFilter.MISSING_REQUIRED;
        qaStatusFilterComboBox.setValue(ALL_QA_STATUSES);
        setDateRange(RANGE_LAST_30_DAYS, LocalDate.now().minusDays(30), LocalDate.now());
        applyFilters();
    }

    @FXML
    private void showExportBlockedQueue() {
        activeQueueFilter = ReviewQueueFilter.EXPORT_BLOCKED;
        qaStatusFilterComboBox.setValue("Waiting for QA");
        setDateRange(RANGE_LAST_30_DAYS, LocalDate.now().minusDays(30), LocalDate.now());
        applyFilters();
    }

    @FXML
    private void showFailedValidationQueue() {
        activeQueueFilter = ReviewQueueFilter.FAILED_VALIDATION;
        qaStatusFilterComboBox.setValue(ALL_QA_STATUSES);
        setDateRange(RANGE_LAST_30_DAYS, LocalDate.now().minusDays(30), LocalDate.now());
        applyFilters();
    }

    @FXML
    private void showReadyForQaQueue() {
        activeQueueFilter = ReviewQueueFilter.READY_FOR_QA;
        qaStatusFilterComboBox.setValue("Ready for QA");
        setDateRange(RANGE_LAST_30_DAYS, LocalDate.now().minusDays(30), LocalDate.now());
        applyFilters();
    }

    @FXML
    private void showQaRejectedQueue() {
        activeQueueFilter = ReviewQueueFilter.QA_REJECTED;
        qaStatusFilterComboBox.setValue("QA Rejected");
        setDateRange(RANGE_LAST_30_DAYS, LocalDate.now().minusDays(30), LocalDate.now());
        applyFilters();
    }

    @FXML
    private void showRecentlyScannedQueue() {
        activeQueueFilter = ReviewQueueFilter.RECENTLY_SCANNED;
        qaStatusFilterComboBox.setValue(ALL_QA_STATUSES);
        LocalDate today = LocalDate.now();
        setDateRange(RANGE_TODAY, today, today);
        applyFilters();
    }

    private void applyPresetDateRange(String selectedRange) {
        LocalDate today = LocalDate.now();

        switch (selectedRange) {
            case RANGE_TODAY -> setDateRange(RANGE_TODAY, today, today);
            case RANGE_LAST_7_DAYS -> setDateRange(RANGE_LAST_7_DAYS, today.minusDays(7), today);
            case RANGE_LAST_30_DAYS -> setDateRange(RANGE_LAST_30_DAYS, today.minusDays(30), today);
            case RANGE_ALL_TIME -> setDateRange(RANGE_ALL_TIME, null, null);
            case RANGE_CUSTOM -> {
            }
            default -> setDateRange(RANGE_TODAY, today, today);
        }
    }

    private void setDateRange(String selectedRange, LocalDate fromDate, LocalDate toDate) {
        updatingDateControls = true;

        this.fromDate = fromDate;
        this.toDate = toDate;
        normalizeDateRange();
        pendingRangeStart = null;

        if (dateRangeFilterComboBox != null) {
            dateRangeFilterComboBox.setValue(selectedRange);
        }

        updateDateRangeFieldDisplay();
        updateDateRangeCalendarMonthFromSelection();
        updateDateRangeCalendarDisplay();

        updatingDateControls = false;
    }

    private void configureDateRangeField() {
        if (dateRangeField != null) {
            dateRangeField.setPromptText("MM/DD/YYYY");

            dateRangeField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (updatingDateControls) {
                    return;
                }

                applyTypedDateRangeLive(newValue);
            });

            dateRangeField.setOnAction(event ->
                    applyTypedDateRangeAndNormalize(dateRangeField.getText())
            );

            dateRangeField.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
                if (!isFocused) {
                    applyTypedDateRangeAndNormalize(dateRangeField.getText());
                }
            });
        }

        if (dateRangeBox != null && dateRangeField != null) {
            dateRangeBox.setOnMouseClicked(event -> {
                Object target = event.getTarget();

                if (target == dateRangeMenuButton) {
                    return;
                }

                dateRangeField.requestFocus();
                dateRangeField.positionCaret(dateRangeField.getText().length());
            });
        }

        if (dateRangeMenuButton != null) {
            // Canonical dropdown caret: shared Region graphic — identical shape/size/color
            // across every dropdown in the app (see .dropdown-caret-graphic in app.css).
            javafx.scene.layout.Region __rangeCaret = new javafx.scene.layout.Region();
            __rangeCaret.getStyleClass().add("dropdown-caret-graphic");
            dateRangeMenuButton.setText(null);
            dateRangeMenuButton.setGraphic(__rangeCaret);
            dateRangeMenuButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            dateRangeMenuButton.setOnAction(event -> showDateRangeCalendarMenu());
        }

        updateDateRangeFieldDisplay();
    }

    private void showDateRangeCalendarMenu() {
        if (dateRangeCalendarMenu == null) {
            dateRangeCalendarMenu = new ContextMenu();
            dateRangeCalendarMenu.getStyleClass().add("review-date-popover-menu");
        }

        Node anchor = dateRangeBox == null ? dateRangeMenuButton : dateRangeBox;

        if (anchor == null || anchor.getScene() == null) {
            return;
        }

        dateRangeCalendarMenu.getItems().setAll(new CustomMenuItem(createDateRangeCalendarPopover(), false));
        updateDateRangeCalendarMonthFromSelection();
        updateDateRangeCalendarDisplay();
        dateRangeCalendarMenu.show(anchor, Side.BOTTOM, 0, 3);
    }

    private VBox createDateRangeCalendarPopover() {
        VBox popover = new VBox(0);
        popover.getStyleClass().add("review-date-popover");

        VBox panel = new VBox(0);
        panel.getStyleClass().add("review-calendar-panel");

        HBox header = new HBox();
        header.getStyleClass().add("review-calendar-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(Double.MAX_VALUE);

        Button previousButton = new Button("<");
        previousButton.setFocusTraversable(false);
        previousButton.getStyleClass().add("review-calendar-nav-button");
        previousButton.setOnAction(event -> showPreviousDateRangeCalendarMonth());

        Button nextButton = new Button(">");
        nextButton.setFocusTraversable(false);
        nextButton.getStyleClass().add("review-calendar-nav-button");
        nextButton.setOnAction(event -> showNextDateRangeCalendarMonth());

        dateRangeCalendarMonthLabel = new Label();
        dateRangeCalendarMonthLabel.getStyleClass().add("review-calendar-month-label");
        dateRangeCalendarMonthLabel.setMaxWidth(Double.MAX_VALUE);
        dateRangeCalendarMonthLabel.setAlignment(Pos.CENTER);
        HBox.setHgrow(dateRangeCalendarMonthLabel, Priority.ALWAYS);

        header.getChildren().addAll(previousButton, dateRangeCalendarMonthLabel, nextButton);

        dateRangeCalendarGrid = new GridPane();
        dateRangeCalendarGrid.setHgap(3);
        dateRangeCalendarGrid.setVgap(6);
        dateRangeCalendarGrid.setMaxWidth(Double.MAX_VALUE);
        dateRangeCalendarGrid.getStyleClass().add("review-calendar-grid");

        panel.getChildren().addAll(header, dateRangeCalendarGrid);
        popover.getChildren().add(panel);

        updateDateRangeCalendarDisplay();
        return popover;
    }

    private void showPreviousDateRangeCalendarMonth() {
        displayedDateRangeMonth = displayedDateRangeMonth.minusMonths(1);
        updateDateRangeCalendarDisplay();
    }

    private void showNextDateRangeCalendarMonth() {
        displayedDateRangeMonth = displayedDateRangeMonth.plusMonths(1);
        updateDateRangeCalendarDisplay();
    }

    private void updateDateRangeCalendarMonthFromSelection() {
        // Always open the calendar on the current month (today) first, rather
        // than jumping back to the start of the active range (e.g. 30 days ago).
        displayedDateRangeMonth = YearMonth.now();
    }

    private void updateDateRangeCalendarDisplay() {
        if (dateRangeCalendarGrid == null || dateRangeCalendarMonthLabel == null || displayedDateRangeMonth == null) {
            return;
        }

        dateRangeCalendarGrid.getChildren().clear();
        dateRangeCalendarMonthLabel.setText(displayedDateRangeMonth.getMonth().getDisplayName(
                TextStyle.FULL,
                Locale.ENGLISH
        ) + " " + displayedDateRangeMonth.getYear());

        String[] dayNames = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};

        for (int column = 0; column < dayNames.length; column++) {
            Label label = new Label(dayNames[column]);
            label.getStyleClass().add("review-calendar-day-name");
            dateRangeCalendarGrid.add(label, column, 0);
        }

        LocalDate firstOfMonth = displayedDateRangeMonth.atDay(1);
        int leadingDays = firstOfMonth.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        LocalDate firstVisibleDate = firstOfMonth.minusDays(leadingDays);

        for (int index = 0; index < 42; index++) {
            LocalDate date = firstVisibleDate.plusDays(index);
            Button dayButton = createDateRangeCalendarDayButton(date);
            dateRangeCalendarGrid.add(dayButton, index % 7, index / 7 + 1);
        }
    }

    private Button createDateRangeCalendarDayButton(LocalDate date) {
        Button dayButton = new Button(String.valueOf(date.getDayOfMonth()));
        dayButton.getStyleClass().add("review-calendar-day-button");
        dayButton.setFocusTraversable(false);
        dayButton.setMinSize(30, 30);
        dayButton.setPrefSize(30, 30);
        dayButton.setMaxSize(30, 30);

        boolean selectedBoundary = date.equals(fromDate) || date.equals(toDate);

        if (!YearMonth.from(date).equals(displayedDateRangeMonth)) {
            dayButton.getStyleClass().add("review-calendar-day-outside");
        }

        if (date.equals(LocalDate.now()) && !selectedBoundary) {
            dayButton.getStyleClass().add("review-calendar-day-today");
        }

        if (selectedBoundary) {
            dayButton.getStyleClass().add("review-calendar-day-selected");
        } else if (isBetweenRange(date)) {
            dayButton.getStyleClass().add("review-calendar-day-in-range");
        }

        dayButton.setOnAction(event -> selectDateRangeCalendarDate(date));
        return dayButton;
    }

    private void selectDateRangeCalendarDate(LocalDate selectedDate) {
        if (selectedDate == null) {
            return;
        }

        updatingDateControls = true;

        if (pendingRangeStart == null) {
            pendingRangeStart = selectedDate;
            fromDate = selectedDate;
            toDate = selectedDate;
        } else {
            if (selectedDate.isBefore(pendingRangeStart)) {
                fromDate = selectedDate;
                toDate = pendingRangeStart;
            } else {
                fromDate = pendingRangeStart;
                toDate = selectedDate;
            }

            pendingRangeStart = null;
        }

        normalizeDateRange();

        if (dateRangeFilterComboBox != null) {
            dateRangeFilterComboBox.setValue(RANGE_CUSTOM);
        }

        updatingDateControls = false;

        displayedDateRangeMonth = YearMonth.from(selectedDate);
        updateDateRangeFieldDisplay();
        updateDateRangeCalendarDisplay();

        if (pendingRangeStart == null) {
            if (dateRangeCalendarMenu != null) {
                dateRangeCalendarMenu.hide();
            }
            applyFilters();
        }
    }

    private void applyTypedDateRangeLive(String value) {
        String cleanedValue = Strings.clean(value);

        if (isBlankOrPlaceholderDate(cleanedValue)) {
            if (fromDate != null || toDate != null || !RANGE_ALL_TIME.equals(selectedValue(dateRangeFilterComboBox, RANGE_ALL_TIME))) {
                applyParsedDateRange(null, RANGE_ALL_TIME);
                applyFilters();
            }
            return;
        }

        ParsedDateRange parsedRange = parseDateRangeFilterValue(cleanedValue);

        if (parsedRange == null || isSameDateRange(parsedRange)) {
            return;
        }

        applyParsedDateRange(parsedRange, RANGE_CUSTOM);
        applyFilters();
    }

    private void applyTypedDateRangeAndNormalize(String value) {
        if (updatingDateControls) {
            return;
        }

        String cleanedValue = Strings.clean(value);

        if (isBlankOrPlaceholderDate(cleanedValue)) {
            applyParsedDateRange(null, RANGE_ALL_TIME);
            applyFilters();
            return;
        }

        ParsedDateRange parsedRange = parseDateRangeFilterValue(cleanedValue);

        if (parsedRange == null) {
            updateDateRangeFieldDisplay();
            return;
        }

        applyParsedDateRange(parsedRange, RANGE_CUSTOM);
        applyFilters();
    }

    private boolean isBlankOrPlaceholderDate(String value) {
        String cleanedValue = Strings.clean(value);
        return cleanedValue.isBlank() || "MM/DD/YYYY".equalsIgnoreCase(cleanedValue);
    }

    private ParsedDateRange parseDateRangeFilterValue(String value) {
        String cleanedValue = Strings.clean(value)
                .replace('–', '-')
                .replace('—', '-');

        if (isBlankOrPlaceholderDate(cleanedValue)) {
            return null;
        }

        String[] parts = cleanedValue.split("\\s+-\\s+", 2);

        LocalDate startDate = parseDateRangePart(parts[0]);
        if (startDate == null) {
            return null;
        }

        LocalDate endDate = parts.length > 1 ? parseDateRangePart(parts[1]) : startDate;
        if (endDate == null) {
            return null;
        }

        return new ParsedDateRange(startDate, endDate);
    }

    private LocalDate parseDateRangePart(String value) {
        String cleanedValue = Strings.clean(value);

        if (cleanedValue.isBlank()) {
            return null;
        }

        List<DateTimeFormatter> formatters = List.of(
                DATE_RANGE_FORMATTER,
                DateTimeFormatter.ofPattern("M/d/yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH),
                DateTimeFormatter.ISO_LOCAL_DATE
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(cleanedValue, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported date format.
            }
        }

        return null;
    }

    private void applyParsedDateRange(ParsedDateRange parsedRange, String selectedRange) {
        updatingDateControls = true;

        if (parsedRange == null) {
            fromDate = null;
            toDate = null;
        } else {
            fromDate = parsedRange.startDate();
            toDate = parsedRange.endDate();
            normalizeDateRange();
        }

        pendingRangeStart = null;

        if (dateRangeFilterComboBox != null) {
            dateRangeFilterComboBox.setValue(selectedRange);
        }

        updatingDateControls = false;

        updateDateRangeFieldDisplay();
        updateDateRangeCalendarMonthFromSelection();
        updateDateRangeCalendarDisplay();
    }

    private boolean isSameDateRange(ParsedDateRange parsedRange) {
        if (parsedRange == null) {
            return fromDate == null && toDate == null;
        }

        return parsedRange.startDate().equals(fromDate)
                && parsedRange.endDate().equals(toDate);
    }

    private void normalizeDateRange() {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            LocalDate swappedDate = fromDate;
            fromDate = toDate;
            toDate = swappedDate;
        }
    }

    private boolean isBetweenRange(LocalDate date) {
        return fromDate != null
                && toDate != null
                && date.isAfter(fromDate)
                && date.isBefore(toDate);
    }

    private void updateDateRangeFieldDisplay() {
        if (dateRangeField == null) {
            return;
        }

        updatingDateControls = true;
        dateRangeField.setPromptText("MM/DD/YYYY");
        dateRangeField.setText(formatDateRange());
        updatingDateControls = false;

        if (dateRangeBox != null) {
            dateRangeBox.getStyleClass().removeAll("review-date-filter-has-value");

            if (fromDate != null || toDate != null) {
                dateRangeBox.getStyleClass().add("review-date-filter-has-value");
            }
        }
    }

    private String formatDateRange() {
        if (fromDate == null && toDate == null) {
            return "";
        }

        if (fromDate != null && toDate == null) {
            return DATE_RANGE_FORMATTER.format(fromDate);
        }

        if (fromDate == null) {
            return DATE_RANGE_FORMATTER.format(toDate);
        }

        if (fromDate.equals(toDate)) {
            return DATE_RANGE_FORMATTER.format(fromDate);
        }

        return DATE_RANGE_FORMATTER.format(fromDate) + " - " + DATE_RANGE_FORMATTER.format(toDate);
    }

    private String selectedValue(ComboBox<String> comboBox, String fallback) {
        if (comboBox == null || comboBox.getValue() == null) {
            return fallback;
        }
        return comboBox.getValue();
    }

    @FXML
    private void exportReport() {
        int exportCount = filteredRecords.isEmpty() ? records.size() : filteredRecords.size();
        adminManager.addAuditLog("Exports", "Exported review center report", "Review Center", "Success",
                "A review center report was exported.");
        paginationSummaryLabel.setText("Exported review report for " + exportCount + " records");
    }

    private void openReviewWorkspace(ReviewRow record) {
        activeReviewRecord = record;
        activeQaAssignment = null;
        activeQaDocuments = List.of();
        selectedQaDocumentIndex = 0;
        selectedQaPageIndex = 0;
        collapsedReviewDocuments.clear();
        if (reviewWorkspaceSearchField != null) {
            reviewWorkspaceSearchField.clear();
        }

        if (workspaceTitleLabel != null) {
            workspaceTitleLabel.setText(record.identity());
        }
        if (workspaceSubtitleLabel != null) {
            workspaceSubtitleLabel.setText(
                    record.profile()
                            + " - "
                            + record.client()
                            + " - "
                            + record.pages()
                            + " pages - "
                            + record.documentDetailsStatus()
            );
        }

        boolean isBlocked = record.documentDetailsStatus().equalsIgnoreCase("Missing Required Fields")
                || record.documentDetailsStatus().equalsIgnoreCase("Invalid")
                || record.documentDetailsStatus().equalsIgnoreCase("Incomplete");

        if (workspaceWarningLabel != null) {
            workspaceWarningLabel.setText("");
            workspaceWarningLabel.setVisible(false);
            workspaceWarningLabel.setManaged(false);
        }
        if (reviewBoxValueLabel != null) {
            reviewBoxValueLabel.setText(record.identity());
        }
        if (reviewProfileValueLabel != null) {
            reviewProfileValueLabel.setText(record.profile());
        }
        if (reviewDocumentsValueLabel != null) {
            reviewDocumentsValueLabel.setText("1 · " + record.pages() + " pages");
        }
        if (reviewReferenceInfoLabel != null) {
            reviewReferenceInfoLabel.setText("Ref: -");
        }
        if (reviewFilesInfoLabel != null) {
            reviewFilesInfoLabel.setText("Scanned Files: " + record.pages());
        }
        if (reviewDocumentsInfoLabel != null) {
            reviewDocumentsInfoLabel.setText("Documents: 1");
        }
        if (reviewSidebarSubtitleLabel != null) {
            reviewSidebarSubtitleLabel.setText(record.profile() + " · " + record.identity());
        }
        updateReviewZoomLabel();

        configureWorkspaceControls();
        loadWorkspaceQaData(record);
        renderWorkspaceQaView();
        showWorkspace();
    }

    private void loadWorkspaceQaData(ReviewRow record) {
        if (adminManager == null || record == null) {
            return;
        }

        List<QAService.QaDocumentSnapshot> sourceDocuments;
        if (isQaReviewRow(record)) {
            activeQaAssignment = adminManager.getQaAssignmentForReviewRecord(record.id());
            if (activeQaAssignment == null) {
                activeQaDocuments = List.of();
                return;
            }
            sourceDocuments = activeQaAssignment.documents();
        } else {
            activeQaAssignment = null;
            sourceDocuments = adminManager.getSavedProgressDocumentsForReviewRecord(record.identity(), record.profile());
        }

        List<WorkspaceQaDocument> documents = new ArrayList<>();
        for (QAService.QaDocumentSnapshot assignmentDocument : sourceDocuments) {
            List<WorkspaceQaPage> pages = new ArrayList<>();
            for (QAService.QaPageSnapshot assignmentPage : assignmentDocument.pages()) {
                pages.add(new WorkspaceQaPage(
                        assignmentPage.pageNumber(),
                        assignmentPage.globalPageNumber(),
                        assignmentPage.sourceReference(),
                        assignmentPage.displayContent(),
                        assignmentPage.rotationDegrees(),
                        assignmentPage.reviewStatus(),
                        assignmentPage.comment()
                ));
            }
            if (!pages.isEmpty()) {
                documents.add(new WorkspaceQaDocument(assignmentDocument.name(), pages));
            }
        }
        activeQaDocuments = List.copyOf(documents);
        resetWorkspacePreviewTransform();
    }

    private void renderWorkspaceQaView() {
        updateWorkspaceSummaryChips();
        renderWorkspaceQaTree();
        renderWorkspaceQaPreview();
        renderWorkspaceQaThumbnails();
        renderWorkspaceQaTools();
    }

    private void configureWorkspaceQaControls() {
        if (qaActionScopeComboBox != null) {
            qaActionScopeComboBox.getItems().setAll("Selected Page", "This Document");
            qaActionScopeComboBox.getSelectionModel().selectFirst();
        }

        if (qaCommentTextArea != null) {
            qaCommentTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
                if (syncingWorkspaceQaControls) {
                    return;
                }
                updateWorkspaceQaComment(newValue == null ? "" : newValue);
            });
        }
    }

    private void updateWorkspaceSummaryChips() {
        if (reviewFilesInfoLabel != null) {
            reviewFilesInfoLabel.setText("Scanned Files: " + getWorkspaceQaPageCount());
        }
        if (reviewDocumentsInfoLabel != null) {
            reviewDocumentsInfoLabel.setText("Documents: " + activeQaDocuments.size());
        }
        if (reviewDocumentsValueLabel != null) {
            reviewDocumentsValueLabel.setText(activeQaDocuments.size() + " · " + getWorkspaceQaPageCount() + " pages");
        }
        WorkspaceQaPage selectedPage = getSelectedWorkspaceQaPage();
        if (reviewReferenceInfoLabel != null) {
            reviewReferenceInfoLabel.setText(selectedPage == null ? "Ref: -" : "Ref: " + selectedPage.sourceReference());
        }
    }

    private void renderWorkspaceQaTree() {
        VBox treeContainer = activeWorkspaceTreeContainer();
        if (treeContainer == null) {
            return;
        }
        treeContainer.getChildren().clear();

        if (activeQaDocuments.isEmpty()) {
            VBox emptyState = new VBox(6);
            emptyState.getStyleClass().add("document-tree-empty-state");

            Label title = new Label("No documents available");
            title.getStyleClass().add("document-tree-empty-title");

            Label copy = new Label(activeReviewRecord == null
                    ? "Select a review item to see its pages."
                    : "This review item does not have any pages ready to display.");
            copy.setWrapText(true);
            copy.getStyleClass().add("document-tree-empty-copy");

            emptyState.getChildren().addAll(title, copy);
            treeContainer.getChildren().add(emptyState);
            return;
        }

        String query = reviewWorkspaceSearchField == null ? "" : Strings.normalize(reviewWorkspaceSearchField.getText());

        for (int documentIndex = 0; documentIndex < activeQaDocuments.size(); documentIndex++) {
            WorkspaceQaDocument document = activeQaDocuments.get(documentIndex);
            List<PagePointer> matchingPages = new ArrayList<>();
            for (int pageIndex = 0; pageIndex < document.pages().size(); pageIndex++) {
                WorkspaceQaPage page = document.pages().get(pageIndex);
                if (query.isBlank()
                        || Strings.normalize(document.name()).contains(query)
                        || Strings.normalize(page.sourceReference()).contains(query)
                        || Strings.normalize("page " + page.pageNumber()).contains(query)) {
                    matchingPages.add(new PagePointer(documentIndex, pageIndex));
                }
            }

            if (matchingPages.isEmpty()) {
                continue;
            }

            VBox documentBlock = new VBox(12);
            documentBlock.setAlignment(Pos.TOP_LEFT);
            documentBlock.getStyleClass().addAll("document-tree-document-block", "document-tree-list-block");

            HBox documentHeader = new HBox(9);
            documentHeader.setAlignment(Pos.CENTER_LEFT);
            documentHeader.getStyleClass().addAll("document-tree-document-header", "document-tree-document-header-framed", "document-tree-list-header");

            Region chevron = new Region();
            chevron.getStyleClass().add("document-tree-chevron-icon");
            chevron.setRotate(collapsedReviewDocuments.contains(documentIndex) ? 0 : 90);

            Label title = new Label(workspaceDocumentTitle(document, documentIndex));
            title.getStyleClass().add("document-tree-document-title");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label warning = new Label(document.pages().stream().anyMatch(page -> page.status() == QAService.QaPageReviewStatus.NEEDS_FIX) ? "!" : "");
            warning.getStyleClass().add("document-tree-warning");

            Label count = new Label(document.pages().size() + " pages");
            count.getStyleClass().add("document-tree-count");

            documentHeader.getChildren().addAll(chevron, title, spacer, warning, count);
            final int selectedDocumentIndex = documentIndex;
            documentHeader.setOnMouseClicked(event -> {
                if (collapsedReviewDocuments.contains(selectedDocumentIndex)) {
                    collapsedReviewDocuments.remove(selectedDocumentIndex);
                } else {
                    collapsedReviewDocuments.add(selectedDocumentIndex);
                }
                if (!document.pages().isEmpty()) {
                    selectedQaDocumentIndex = selectedDocumentIndex;
                    selectedQaPageIndex = 0;
                }
                resetWorkspacePreviewTransform();
                renderWorkspaceQaView();
            });
            documentBlock.getChildren().add(documentHeader);

            if (!collapsedReviewDocuments.contains(documentIndex)) {
                VBox pageStack = new VBox(0);
                pageStack.setAlignment(Pos.TOP_LEFT);
                pageStack.getStyleClass().addAll("document-tree-page-stack", "document-tree-list-page-stack");

                for (PagePointer pointer : matchingPages) {
                    WorkspaceQaPage page = activeQaDocuments.get(pointer.documentIndex()).pages().get(pointer.pageIndex());
                    pageStack.getChildren().add(createWorkspacePageRow(pointer, page));
                }
                documentBlock.getChildren().add(pageStack);
            }

            treeContainer.getChildren().add(documentBlock);
        }
    }

    private void renderWorkspaceQaPreview() {
        StackPane previewHost = activeWorkspacePreviewHost();
        Label pageLabel = activeWorkspaceViewerPageLabel();
        if (previewHost == null) {
            return;
        }
        previewHost.getChildren().clear();
        currentReviewPreviewWrapper = null;
        currentReviewPreviewBaseWidth = 1.0;
        currentReviewPreviewBaseHeight = 1.0;

        WorkspaceQaPage page = getSelectedWorkspaceQaPage();
        if (page == null) {
            previewHost.getChildren().add(createWorkspacePreviewPlaceholder("No page selected"));
            if (pageLabel != null) {
                pageLabel.setText("No page selected");
            }
            return;
        }

        if (pageLabel != null) {
            pageLabel.setText("Page " + page.pageNumber() + " of " + getWorkspaceQaPageCount());
        }
        if (reviewSelectedTitleLabel != null) {
            reviewSelectedTitleLabel.setText("Page " + page.pageNumber());
        }
        if (reviewReferenceInfoLabel != null) {
            reviewReferenceInfoLabel.setText("Ref: " + page.sourceReference());
        }

        Image image = decodeWorkspaceQaImage(page.imageContent());
        if (image == null) {
            previewHost.getChildren().add(createWorkspacePreviewPlaceholder("Preview unavailable"));
            return;
        }

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setFitWidth(PREVIEW_PAGE_WIDTH);
        imageView.setFitHeight(PREVIEW_PAGE_HEIGHT);

        StackPane imageSurface = new StackPane(imageView);
        imageSurface.setAlignment(Pos.CENTER);
        imageSurface.getStyleClass().add("mock-document-page");
        imageSurface.setMinWidth(PREVIEW_PAGE_WIDTH);
        imageSurface.setPrefWidth(PREVIEW_PAGE_WIDTH);
        imageSurface.setMaxWidth(PREVIEW_PAGE_WIDTH);
        imageSurface.setMinHeight(PREVIEW_PAGE_HEIGHT);
        imageSurface.setPrefHeight(PREVIEW_PAGE_HEIGHT);
        imageSurface.setMaxHeight(PREVIEW_PAGE_HEIGHT);
        imageSurface.setRotate(page.rotationDegrees());

        StackPane previewFrame = new StackPane(imageSurface);
        previewFrame.setAlignment(Pos.CENTER);
        previewFrame.setMinWidth(PREVIEW_PAGE_WIDTH);
        previewFrame.setPrefWidth(PREVIEW_PAGE_WIDTH);
        previewFrame.setMaxWidth(PREVIEW_PAGE_WIDTH);
        previewFrame.setMinHeight(PREVIEW_PAGE_HEIGHT);
        previewFrame.setPrefHeight(PREVIEW_PAGE_HEIGHT);
        previewFrame.setMaxHeight(PREVIEW_PAGE_HEIGHT);

        StackPane wrapper = new StackPane(previewFrame);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMinWidth(PREVIEW_PAGE_WIDTH);
        wrapper.setPrefWidth(PREVIEW_PAGE_WIDTH);
        wrapper.setMaxWidth(PREVIEW_PAGE_WIDTH);
        wrapper.setMinHeight(PREVIEW_PAGE_HEIGHT);
        wrapper.setPrefHeight(PREVIEW_PAGE_HEIGHT);
        wrapper.setMaxHeight(PREVIEW_PAGE_HEIGHT);

        currentReviewPreviewBaseWidth = PREVIEW_PAGE_WIDTH;
        currentReviewPreviewBaseHeight = PREVIEW_PAGE_HEIGHT;
        currentReviewPreviewWrapper = wrapper;
        previewHost.getChildren().add(wrapper);
        applyWorkspacePreviewTransform();
    }

    private void renderWorkspaceQaThumbnails() {
        HBox thumbnailStrip = activeWorkspaceThumbnailStrip();
        if (thumbnailStrip == null) {
            return;
        }
        thumbnailStrip.getChildren().clear();

        for (int documentIndex = 0; documentIndex < activeQaDocuments.size(); documentIndex++) {
            WorkspaceQaDocument document = activeQaDocuments.get(documentIndex);
            for (int pageIndex = 0; pageIndex < document.pages().size(); pageIndex++) {
                WorkspaceQaPage page = document.pages().get(pageIndex);
                boolean selected = documentIndex == selectedQaDocumentIndex && pageIndex == selectedQaPageIndex;
                StackPane thumbnail = new StackPane();
                thumbnail.getStyleClass().add(selected
                        ? "review-workspace-thumbnail-active"
                        : "review-workspace-thumbnail");

                Image image = decodeWorkspaceQaImage(page.imageContent());
                if (image == null) {
                    Label number = new Label(String.valueOf(page.pageNumber()));
                    number.getStyleClass().add(selected
                            ? "review-workspace-thumbnail-label-active"
                            : "review-workspace-thumbnail-label");
                    thumbnail.getChildren().add(number);
                } else {
                    ImageView imageView = new ImageView(image);
                    imageView.setPreserveRatio(true);
                    imageView.setSmooth(true);
                    imageView.setFitWidth(56);
                    imageView.setFitHeight(72);
                    imageView.setRotate(page.rotationDegrees());
                    thumbnail.getChildren().add(imageView);
                }

                final int selectedDocument = documentIndex;
                final int selectedPage = pageIndex;
                thumbnail.setOnMouseClicked(event -> {
                    selectedQaDocumentIndex = selectedDocument;
                    selectedQaPageIndex = selectedPage;
                    resetWorkspacePreviewTransform();
                    renderWorkspaceQaView();
                });
                thumbnailStrip.getChildren().add(thumbnail);
            }
        }
    }

    @FXML
    private void zoomOutWorkspacePreview() {
        updateWorkspacePreviewZoom(-0.20);
    }

    @FXML
    private void zoomInWorkspacePreview() {
        updateWorkspacePreviewZoom(0.20);
    }

    @FXML
    private void fitWorkspacePreview() {
        resetWorkspacePreviewTransform();
        applyWorkspacePreviewTransform();
    }

    @FXML
    private void showPreviousWorkspacePage() {
        moveWorkspaceSelection(-1);
    }

    @FXML
    private void showNextWorkspacePage() {
        moveWorkspaceSelection(1);
    }

    @FXML
    private void focusWorkspaceBarcodePage() {
        fitWorkspacePreview();
    }

    private void updateWorkspacePreviewZoom(double delta) {
        setWorkspacePreviewZoom(reviewPreviewZoomMultiplier + delta);
    }

    private void setWorkspacePreviewZoom(double zoom) {
        reviewPreviewZoomMultiplier = clamp(zoom, MIN_PREVIEW_ZOOM, MAX_PREVIEW_ZOOM);
        applyWorkspacePreviewTransform();
        updateReviewZoomLabel();
    }

    private void resetWorkspacePreviewTransform() {
        reviewPreviewZoomMultiplier = 1.0;
        reviewPreviewTranslateX = 0;
        reviewPreviewTranslateY = 0;
    }

    private void applyWorkspacePreviewTransform() {
        StackPane previewHost = activeWorkspacePreviewHost();
        if (currentReviewPreviewWrapper == null || previewHost == null) {
            return;
        }

        double availableWidth = Math.max(1, previewHost.getWidth() - 40);
        double availableHeight = Math.max(1, previewHost.getHeight() - 40);
        availableWidth = Math.max(1, previewHost.getWidth() - PREVIEW_SAFE_HORIZONTAL_PADDING);
        availableHeight = Math.max(1, previewHost.getHeight() - PREVIEW_SAFE_VERTICAL_PADDING);
        double autoScale = Math.min(
                availableWidth / Math.max(1.0, currentReviewPreviewBaseWidth),
                availableHeight / Math.max(1.0, currentReviewPreviewBaseHeight)
        );
        autoScale = Math.min(1.0, autoScale);
        double scale = autoScale * reviewPreviewZoomMultiplier;

        currentReviewPreviewWrapper.setScaleX(scale);
        currentReviewPreviewWrapper.setScaleY(scale);
        clampWorkspacePreviewTranslation();
    }

    private void clampWorkspacePreviewTranslation() {
        StackPane previewHost = activeWorkspacePreviewHost();
        if (currentReviewPreviewWrapper == null || previewHost == null) {
            return;
        }

        double scale = currentReviewPreviewWrapper.getScaleX();
        double scaledWidth = currentReviewPreviewBaseWidth * scale;
        double scaledHeight = currentReviewPreviewBaseHeight * scale;
        double hostWidth = Math.max(1, previewHost.getWidth());
        double hostHeight = Math.max(1, previewHost.getHeight());
        double maxX = Math.abs(hostWidth - scaledWidth) / 2;
        double maxY = Math.abs(hostHeight - scaledHeight) / 2;

        reviewPreviewTranslateX = clamp(reviewPreviewTranslateX, -maxX, maxX);
        reviewPreviewTranslateY = clamp(reviewPreviewTranslateY, -maxY, maxY);

        currentReviewPreviewWrapper.setTranslateX(reviewPreviewTranslateX);
        currentReviewPreviewWrapper.setTranslateY(reviewPreviewTranslateY);
    }

    private void handleWorkspacePreviewMousePressed(MouseEvent event) {
        if (currentReviewPreviewWrapper == null) {
            return;
        }

        reviewPreviewDragStartX = event.getSceneX();
        reviewPreviewDragStartY = event.getSceneY();
        reviewPreviewTranslateStartX = reviewPreviewTranslateX;
        reviewPreviewTranslateStartY = reviewPreviewTranslateY;
    }

    private void handleWorkspacePreviewMouseDragged(MouseEvent event) {
        if (currentReviewPreviewWrapper == null) {
            return;
        }

        reviewPreviewTranslateX = reviewPreviewTranslateStartX + event.getSceneX() - reviewPreviewDragStartX;
        reviewPreviewTranslateY = reviewPreviewTranslateStartY + event.getSceneY() - reviewPreviewDragStartY;
        clampWorkspacePreviewTranslation();
        event.consume();
    }

    private void handleWorkspacePreviewScroll(ScrollEvent event) {
        if (currentReviewPreviewWrapper == null) {
            return;
        }
        updateWorkspacePreviewZoom(event.getDeltaY() > 0 ? 0.15 : -0.15);
        event.consume();
    }

    @FXML
    private void onBackToScanningFromReview() {
        showOverview();
    }

    @FXML
    private void onApprovePage() {
        applyWorkspaceQaStatus(QAService.QaPageReviewStatus.APPROVED);
    }

    @FXML
    private void onMarkNeedsFix() {
        applyWorkspaceQaStatus(QAService.QaPageReviewStatus.NEEDS_FIX);
    }

    @FXML
    private void onClearReviewStatus() {
        applyWorkspaceQaStatus(QAService.QaPageReviewStatus.NOT_REVIEWED);
    }

    @FXML
    private void onNextUnreviewed() {
        for (int documentIndex = 0; documentIndex < activeQaDocuments.size(); documentIndex++) {
            List<WorkspaceQaPage> pages = activeQaDocuments.get(documentIndex).pages();
            for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
                if (pages.get(pageIndex).status() == QAService.QaPageReviewStatus.NOT_REVIEWED) {
                    selectedQaDocumentIndex = documentIndex;
                    selectedQaPageIndex = pageIndex;
                    resetWorkspacePreviewTransform();
                    renderWorkspaceQaView();
                    return;
                }
            }
        }
    }

    @FXML
    private void onCompleteQa() {
        if (activeReviewRecord == null) {
            return;
        }

        int totalPages = getWorkspaceQaPageCount();
        int reviewedPages = getWorkspaceReviewedPageCount();
        if (reviewedPages < totalPages) {
            showExportAlert(null, Alert.AlertType.WARNING, "QA incomplete",
                    "Review every page before completing QA.");
            return;
        }

        boolean approved = getWorkspaceQaIssueCount() == 0;
        ReviewRow updatedRecord = approved
                ? activeReviewRecord.withReviewState("Approved", "QA Approved", false)
                : activeReviewRecord.withReviewState(activeReviewRecord.documentDetailsStatus(), "QA Rejected", true);
        replaceActiveRecord(updatedRecord);

        if (approved) {
            String boxId = activeReviewRecord.identity() == null || activeReviewRecord.identity().isBlank()
                    ? "This QA review"
                    : activeReviewRecord.identity();
            String message = boxId + " completed with " + activeQaDocuments.size() + " documents.";
            showQaCompletedDialog(message, this::onBackToScanningFromReview);
            return;
        }

        showExportAlert(null, Alert.AlertType.INFORMATION, "QA rejected",
                "QA was rejected and marked as needing fixes.");
    }

    @FXML
    private void onReviewZoomIn() {
        updateWorkspacePreviewZoom(PREVIEW_ZOOM_STEP);
    }

    @FXML
    private void onReviewZoomOut() {
        updateWorkspacePreviewZoom(-PREVIEW_ZOOM_STEP);
    }

    @FXML
    private void onResetReviewPreviewView() {
        fitWorkspacePreview();
        updateReviewZoomLabel();
    }

    @FXML
    private void onNudgeReviewPreviewUp() {
        nudgeWorkspacePreview(0, -PREVIEW_NUDGE_AMOUNT);
    }

    @FXML
    private void onNudgeReviewPreviewDown() {
        nudgeWorkspacePreview(0, PREVIEW_NUDGE_AMOUNT);
    }

    @FXML
    private void onNudgeReviewPreviewLeft() {
        nudgeWorkspacePreview(-PREVIEW_NUDGE_AMOUNT, 0);
    }

    @FXML
    private void onNudgeReviewPreviewRight() {
        nudgeWorkspacePreview(PREVIEW_NUDGE_AMOUNT, 0);
    }

    private void nudgeWorkspacePreview(double deltaX, double deltaY) {
        if (currentReviewPreviewWrapper == null) {
            return;
        }

        reviewPreviewTranslateX += deltaX;
        reviewPreviewTranslateY += deltaY;
        clampWorkspacePreviewTranslation();
    }

    @FXML
    private void onSubmitForQaFromReview() {
        sendToQa();
    }

    @FXML
    private void onOpenExportTypeDialog() {
        if (activeReviewRecord == null || adminManager == null) {
            return;
        }

        Stage stage = new Stage();
        stage.setTitle("TIFF Export");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setResizable(false);

        if (reviewWorkspaceView != null && reviewWorkspaceView.getScene() != null) {
            stage.initOwner(reviewWorkspaceView.getScene().getWindow());
        }

        VBox content = buildWorkspaceExportDialogContent(stage);
        StackPane root = new StackPane(content);
        root.getStyleClass().addAll("app-shell", "exports-dialog-stage");

        Scene scene = new Scene(root);
        // This is a separate Stage/Scene, so it does NOT inherit the app's
        // stylesheets. Reuse the owning scene's full list (app.css, tokens,
        // export.css, etc.) so the export dialog is styled, and carry over dark mode.
        if (reviewWorkspaceView != null && reviewWorkspaceView.getScene() != null) {
            scene.getStylesheets().setAll(reviewWorkspaceView.getScene().getStylesheets());
            if (reviewWorkspaceView.getScene().getRoot() != null
                    && reviewWorkspaceView.getScene().getRoot().getStyleClass().contains("dark")) {
                root.getStyleClass().add("dark");
            }
        } else {
            URL stylesheetUrl = getClass().getResource("/css/app.css");
            if (stylesheetUrl != null) {
                scene.getStylesheets().add(stylesheetUrl.toExternalForm());
            }
        }

        stage.setScene(scene);
        stage.sizeToScene();
        stage.showAndWait();
    }

    private void showQaCompletedDialog(String message, Runnable onOk) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Completed QA");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Node defaultCloseButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (defaultCloseButton != null) {
            defaultCloseButton.setVisible(false);
            defaultCloseButton.setManaged(false);
        }

        dialog.getDialogPane().getStyleClass().addAll("app-shell", "profile-created-dialog-pane");

        if (reviewWorkspaceView != null && reviewWorkspaceView.getScene() != null) {
            dialog.initOwner(reviewWorkspaceView.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().setAll(reviewWorkspaceView.getScene().getStylesheets());

            if (reviewWorkspaceView.getScene().getRoot() != null
                    && reviewWorkspaceView.getScene().getRoot().getStyleClass().contains("dark")) {
                dialog.getDialogPane().getStyleClass().add("dark");
            }
        }

        dialog.getDialogPane().setPrefWidth(520);
        dialog.getDialogPane().setMaxWidth(520);
        dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        dialog.getDialogPane().setPrefHeight(Region.USE_COMPUTED_SIZE);
        dialog.getDialogPane().setMaxHeight(Region.USE_PREF_SIZE);
        dialog.getDialogPane().setGraphic(null);
        dialog.getDialogPane().setContent(createQaCompletedDialogContent(dialog, message));

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.orElse(ButtonType.CLOSE) == ButtonType.OK && onOk != null) {
            onOk.run();
        }
    }

    private VBox createQaCompletedDialogContent(Dialog<ButtonType> dialog, String message) {
        VBox root = new VBox();
        root.getStyleClass().add("profile-created-dialog-root");
        root.getChildren().addAll(
                createQaCompletedDialogHeader(dialog),
                createQaCompletedDialogBody(dialog, message)
        );
        return root;
    }

    private HBox createQaCompletedDialogHeader(Dialog<ButtonType> dialog) {
        Label brandLabel = new Label("W");
        brandLabel.getStyleClass().add("profile-created-dialog-brand-label");

        StackPane brandShell = new StackPane(brandLabel);
        brandShell.getStyleClass().add("profile-created-dialog-brand-shell");

        Label titleLabel = new Label("Completed QA");
        titleLabel.getStyleClass().add("profile-created-dialog-title");

        Button closeButton = new Button("\u00D7");
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

    private VBox createQaCompletedDialogBody(Dialog<ButtonType> dialog, String message) {
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

    @FXML
    private void onPreviousReviewPage() {
        showPreviousWorkspacePage();
    }

    @FXML
    private void onNextReviewPage() {
        showNextWorkspacePage();
    }

    private void moveWorkspaceSelection(int step) {
        List<PagePointer> pages = new ArrayList<>();
        for (int documentIndex = 0; documentIndex < activeQaDocuments.size(); documentIndex++) {
            for (int pageIndex = 0; pageIndex < activeQaDocuments.get(documentIndex).pages().size(); pageIndex++) {
                pages.add(new PagePointer(documentIndex, pageIndex));
            }
        }
        if (pages.isEmpty()) {
            return;
        }

        int currentIndex = 0;
        for (int index = 0; index < pages.size(); index++) {
            PagePointer pointer = pages.get(index);
            if (pointer.documentIndex() == selectedQaDocumentIndex && pointer.pageIndex() == selectedQaPageIndex) {
                currentIndex = index;
                break;
            }
        }

        int nextIndex = Math.max(0, Math.min(pages.size() - 1, currentIndex + step));
        PagePointer next = pages.get(nextIndex);
        selectedQaDocumentIndex = next.documentIndex();
        selectedQaPageIndex = next.pageIndex();
        resetWorkspacePreviewTransform();
        renderWorkspaceQaView();
    }

    private Node createWorkspacePreviewPlaceholder(String text) {
        StackPane paper = new StackPane();
        paper.getStyleClass().add("review-workspace-paper-preview");

        Label placeholder = new Label(text);
        placeholder.getStyleClass().add("review-workspace-paper-placeholder");
        paper.getChildren().add(placeholder);
        return paper;
    }

    private WorkspaceQaPage getSelectedWorkspaceQaPage() {
        if (selectedQaDocumentIndex < 0 || selectedQaDocumentIndex >= activeQaDocuments.size()) {
            return null;
        }
        List<WorkspaceQaPage> pages = activeQaDocuments.get(selectedQaDocumentIndex).pages();
        if (selectedQaPageIndex < 0 || selectedQaPageIndex >= pages.size()) {
            return null;
        }
        return pages.get(selectedQaPageIndex);
    }

    private List<PagePointer> getWorkspaceQaPagesForAction() {
        if (selectedQaDocumentIndex < 0 || selectedQaDocumentIndex >= activeQaDocuments.size()) {
            return List.of();
        }

        if (qaActionScopeComboBox != null && "This Document".equals(qaActionScopeComboBox.getValue())) {
            List<PagePointer> pointers = new ArrayList<>();
            List<WorkspaceQaPage> documentPages = activeQaDocuments.get(selectedQaDocumentIndex).pages();
            for (int pageIndex = 0; pageIndex < documentPages.size(); pageIndex++) {
                pointers.add(new PagePointer(selectedQaDocumentIndex, pageIndex));
            }
            return pointers;
        }

        WorkspaceQaPage selectedPage = getSelectedWorkspaceQaPage();
        if (selectedPage == null) {
            return List.of();
        }
        return List.of(new PagePointer(selectedQaDocumentIndex, selectedQaPageIndex));
    }

    private void updateWorkspaceQaComment(String comment) {
        WorkspaceQaPage selectedPage = getSelectedWorkspaceQaPage();
        if (selectedPage == null) {
            return;
        }
        replaceWorkspaceQaPage(
                new PagePointer(selectedQaDocumentIndex, selectedQaPageIndex),
                selectedPage.withComment(comment)
        );
        renderWorkspaceQaView();
    }

    private void applyWorkspaceQaStatus(QAService.QaPageReviewStatus status) {
        List<PagePointer> targets = getWorkspaceQaPagesForAction();
        if (targets.isEmpty()) {
            return;
        }

        for (PagePointer pointer : targets) {
            WorkspaceQaPage page = getWorkspaceQaPage(pointer);
            if (page == null) {
                continue;
            }
            replaceWorkspaceQaPage(pointer, page.withStatus(status));
        }
        renderWorkspaceQaView();
    }

    private WorkspaceQaPage getWorkspaceQaPage(PagePointer pointer) {
        if (pointer == null
                || pointer.documentIndex() < 0
                || pointer.documentIndex() >= activeQaDocuments.size()) {
            return null;
        }
        List<WorkspaceQaPage> pages = activeQaDocuments.get(pointer.documentIndex()).pages();
        if (pointer.pageIndex() < 0 || pointer.pageIndex() >= pages.size()) {
            return null;
        }
        return pages.get(pointer.pageIndex());
    }

    private void replaceWorkspaceQaPage(PagePointer pointer, WorkspaceQaPage replacement) {
        if (pointer == null || replacement == null) {
            return;
        }

        List<WorkspaceQaDocument> updatedDocuments = new ArrayList<>(activeQaDocuments.size());
        for (int documentIndex = 0; documentIndex < activeQaDocuments.size(); documentIndex++) {
            WorkspaceQaDocument document = activeQaDocuments.get(documentIndex);
            List<WorkspaceQaPage> updatedPages = new ArrayList<>(document.pages());
            if (documentIndex == pointer.documentIndex()
                    && pointer.pageIndex() >= 0
                    && pointer.pageIndex() < updatedPages.size()) {
                updatedPages.set(pointer.pageIndex(), replacement);
            }
            updatedDocuments.add(new WorkspaceQaDocument(document.name(), List.copyOf(updatedPages)));
        }
        activeQaDocuments = List.copyOf(updatedDocuments);
    }

    private int getWorkspaceQaPageCount() {
        int count = 0;
        for (WorkspaceQaDocument document : activeQaDocuments) {
            count += document.pages().size();
        }
        return count;
    }

    private void updateReviewZoomLabel() {
        if (reviewZoomLabel != null) {
            reviewZoomLabel.setText(Math.round(reviewPreviewZoomMultiplier * 100) + "%");
        }
    }

    private String workspaceDocumentTitle(WorkspaceQaDocument document, int documentIndex) {
        String normalizedName = document.name() == null ? "" : document.name().trim();
        if (normalizedName.isBlank()) {
            return "Document " + (documentIndex + 1);
        }
        if (normalizedName.toLowerCase(Locale.ROOT).endsWith(".tiff")
                || normalizedName.toLowerCase(Locale.ROOT).endsWith(".tif")) {
            return "Document " + (documentIndex + 1);
        }
        if (Strings.normalize(normalizedName).startsWith("document")) {
            return normalizedName;
        }
        return "Document " + (documentIndex + 1);
    }

    private Node createWorkspacePageRow(PagePointer pointer, WorkspaceQaPage page) {
        HBox row = new HBox(9);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("document-tree-page-row", "document-tree-list-page-row");
        if (pointer.documentIndex() == selectedQaDocumentIndex && pointer.pageIndex() == selectedQaPageIndex) {
            row.getStyleClass().add("document-tree-page-selected");
        }

        HBox labelRow = createWorkspacePageLabelRow(page, "Page " + page.pageNumber());
        row.getChildren().add(labelRow);
        row.setOnMouseClicked(event -> selectWorkspacePage(pointer));
        return row;
    }

    private HBox createWorkspacePageLabelRow(WorkspaceQaPage page, String baseLabel) {
        HBox labelRow = new HBox(6);
        labelRow.setAlignment(Pos.CENTER_LEFT);
        labelRow.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(labelRow, Priority.ALWAYS);

        Label pageLabel = new Label(baseLabel);
        pageLabel.getStyleClass().add("document-tree-page-title");
        labelRow.getChildren().add(pageLabel);

        if (page.status() == QAService.QaPageReviewStatus.APPROVED
                || page.status() == QAService.QaPageReviewStatus.NEEDS_FIX) {
            boolean approved = page.status() == QAService.QaPageReviewStatus.APPROVED;
            Region statusSpacer = new Region();
            HBox.setHgrow(statusSpacer, Priority.ALWAYS);
            labelRow.getChildren().add(statusSpacer);
            Label statusIcon = PrimeIcons.create(
                    approved ? Character.toString(0xE90A) : Character.toString(0xE922),
                    approved ? "qa-page-status-icon-approved" : "qa-page-status-icon-fix"
            );
            labelRow.getChildren().add(statusIcon);
        }

        return labelRow;
    }

    private void selectWorkspacePage(PagePointer pointer) {
        selectedQaDocumentIndex = pointer.documentIndex();
        selectedQaPageIndex = pointer.pageIndex();
        resetWorkspacePreviewTransform();
        renderWorkspaceQaView();
    }

    private VBox activeWorkspaceTreeContainer() {
        return reviewDocumentListContainer != null ? reviewDocumentListContainer : reviewWorkspaceTreeContainer;
    }

    private StackPane activeWorkspacePreviewHost() {
        return reviewPreviewHost != null ? reviewPreviewHost : reviewWorkspacePreviewHost;
    }

    private HBox activeWorkspaceThumbnailStrip() {
        return reviewPageTrayContainer != null ? reviewPageTrayContainer : reviewWorkspaceThumbnailStrip;
    }

    private Label activeWorkspaceViewerPageLabel() {
        return reviewWorkspaceViewerPageLabel;
    }

    private void renderWorkspaceQaTools() {
        WorkspaceQaPage page = getSelectedWorkspaceQaPage();

        syncingWorkspaceQaControls = true;

        if (qaCommentTextArea != null) {
            qaCommentTextArea.setDisable(page == null);
            qaCommentTextArea.setText(page == null ? "" : page.comment());
        }
        if (qaActionScopeComboBox != null) {
            qaActionScopeComboBox.setDisable(page == null);
        }

        syncingWorkspaceQaControls = false;
        updateWorkspaceQaStatusBadge();
    }

    private void updateWorkspaceQaStatusBadge() {
        if (reviewStatusBadge == null) {
            return;
        }

        reviewStatusBadge.getStyleClass().removeAll(
                "qa-review-status-waiting",
                "qa-review-status-review",
                "qa-review-status-issues",
                "qa-review-status-complete"
        );

        String normalizedStatus = activeReviewRecord == null || activeReviewRecord.qaStatus() == null
                ? ""
                : activeReviewRecord.qaStatus().trim().toLowerCase(Locale.ROOT);

        if (normalizedStatus.contains("approved")) {
            reviewStatusBadge.setText("QA Completed");
            reviewStatusBadge.getStyleClass().add("qa-review-status-complete");
        } else if (normalizedStatus.contains("rejected") || hasWorkspaceQaIssues()) {
            reviewStatusBadge.setText("Needs Fix");
            reviewStatusBadge.getStyleClass().add("qa-review-status-issues");
        } else if (hasWorkspaceReviewedPages()) {
            reviewStatusBadge.setText("In Review");
            reviewStatusBadge.getStyleClass().add("qa-review-status-review");
        } else {
            reviewStatusBadge.setText("Waiting for QA");
            reviewStatusBadge.getStyleClass().add("qa-review-status-waiting");
        }
    }

    private boolean hasWorkspaceReviewedPages() {
        for (WorkspaceQaDocument document : activeQaDocuments) {
            for (WorkspaceQaPage page : document.pages()) {
                if (page.status() != QAService.QaPageReviewStatus.NOT_REVIEWED) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getWorkspaceReviewedPageCount() {
        int count = 0;
        for (WorkspaceQaDocument document : activeQaDocuments) {
            for (WorkspaceQaPage page : document.pages()) {
                if (page.status() != QAService.QaPageReviewStatus.NOT_REVIEWED) {
                    count++;
                }
            }
        }
        return count;
    }

    private int getWorkspaceQaIssueCount() {
        int count = 0;
        for (WorkspaceQaDocument document : activeQaDocuments) {
            for (WorkspaceQaPage page : document.pages()) {
                if (page.status() == QAService.QaPageReviewStatus.NEEDS_FIX) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean hasWorkspaceQaIssues() {
        for (WorkspaceQaDocument document : activeQaDocuments) {
            for (WorkspaceQaPage page : document.pages()) {
                if (page.status() == QAService.QaPageReviewStatus.NEEDS_FIX) {
                    return true;
                }
            }
        }
        return false;
    }

    private Image decodeWorkspaceQaImage(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        int commaIndex = value.indexOf(',');
        if (commaIndex < 0 || commaIndex >= value.length() - 1) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(value.substring(commaIndex + 1));
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                image = TiffImageSupport.readFirstFrame(bytes, 900, 1200);
            }
            image = cropSparsePreviewImage(image);
            return image == null ? null : SwingFXUtils.toFXImage(image, null);
        } catch (Exception exception) {
            return null;
        }
    }

    private BufferedImage cropSparsePreviewImage(BufferedImage image) {
        if (image == null) {
            return null;
        }

        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < image.getHeight(); y += 2) {
            for (int x = 0; x < image.getWidth(); x += 2) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                if (red > 245 && green > 245 && blue > 245) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }

        if (maxX < minX || maxY < minY) {
            return image;
        }

        double widthRatio = (double) (maxX - minX + 1) / Math.max(1, image.getWidth());
        double heightRatio = (double) (maxY - minY + 1) / Math.max(1, image.getHeight());
        if (widthRatio >= 0.70 || heightRatio >= 0.70) {
            return image;
        }

        int padding = 28;
        int cropX = Math.max(0, minX - padding);
        int cropY = Math.max(0, minY - padding);
        int cropWidth = Math.min(image.getWidth() - cropX, (maxX - minX + 1) + padding * 2);
        int cropHeight = Math.min(image.getHeight() - cropY, (maxY - minY + 1) + padding * 2);

        if (cropWidth <= 0 || cropHeight <= 0) {
            return image;
        }

        return image.getSubimage(cropX, cropY, cropWidth, cropHeight);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String statusIconText(QAService.QaPageReviewStatus status) {
        return switch (status == null ? QAService.QaPageReviewStatus.NOT_REVIEWED : status) {
            case APPROVED -> "OK";
            case NEEDS_FIX -> "!";
            case NOT_REVIEWED -> "-";
        };
    }

    private String statusIconStyle(QAService.QaPageReviewStatus status) {
        return switch (status == null ? QAService.QaPageReviewStatus.NOT_REVIEWED : status) {
            case APPROVED -> "review-workspace-tree-status-complete";
            case NEEDS_FIX -> "review-workspace-tree-status-warning";
            case NOT_REVIEWED -> "review-workspace-tree-status-neutral";
        };
    }

    private String statusText(QAService.QaPageReviewStatus status) {
        return switch (status == null ? QAService.QaPageReviewStatus.NOT_REVIEWED : status) {
            case APPROVED -> "approved";
            case NEEDS_FIX -> "needs fix";
            case NOT_REVIEWED -> "not started";
        };
    }

    @FXML
    private void showOverview() {
        overviewPane.setVisible(true);
        overviewPane.setManaged(true);

        workspacePane.setVisible(false);
        workspacePane.setManaged(false);

        if (reviewRootScrollPane != null) {
            reviewRootScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            reviewRootScrollPane.setFitToHeight(true);
        }

        resetRootScrollPosition();
    }

    @FXML
    private void showWorkspace() {
        overviewPane.setVisible(false);
        overviewPane.setManaged(false);

        workspacePane.setVisible(true);
        workspacePane.setManaged(true);

        if (reviewRootScrollPane != null) {
            reviewRootScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            reviewRootScrollPane.setFitToHeight(true);
        }

        resetRootScrollPosition();
    }

    private void resetRootScrollPosition() {
        if (reviewRootScrollPane == null) {
            return;
        }
        Platform.runLater(() -> {
            reviewRootScrollPane.setVvalue(0);
            reviewRootScrollPane.setHvalue(0);
        });
    }

    @FXML
    private void saveReviewDetails() {
        if (activeReviewRecord == null) {
            return;
        }

        replaceActiveRecord(activeReviewRecord.withLastUpdated("Saved just now"));
        workspaceWarningLabel.setText("Review saved just now.");
    }

    @FXML
    private void markComplete() {
        if (activeReviewRecord == null) {
            return;
        }

        replaceActiveRecord(activeReviewRecord.withReviewState("Complete", activeReviewRecord.qaStatus(), false));
    }

    @FXML
    private void sendToQa() {
        if (activeReviewRecord == null) {
            return;
        }

        if (isQaReviewRow(activeReviewRecord)) {
            final ReviewRow capturedRecord = activeReviewRecord;
            chooseQaAssignee(capturedRecord.id(), selectedAssignee -> {
                if (selectedAssignee == null) {
                    return;
                }
                ReviewRecord updatedRecord = adminManager.assignReviewRecordToQa(
                        capturedRecord.id(), selectedAssignee.userId());
                if (updatedRecord != null) {
                    replaceActiveRecord(toReviewRow(updatedRecord));
                }
            });
            return;
        }

        replaceActiveRecord(activeReviewRecord.withReviewState("Complete", "Ready for QA", false));
    }

    @FXML
    private void approveReview() {
        if (activeReviewRecord == null) {
            return;
        }

        replaceActiveRecord(activeReviewRecord.withReviewState("Approved", "QA Approved", false));
    }

    @FXML
    private void rejectReview() {
        if (activeReviewRecord == null) {
            return;
        }

        replaceActiveRecord(activeReviewRecord.withReviewState(activeReviewRecord.documentDetailsStatus(), "QA Rejected", true));
    }

    private void replaceActiveRecord(ReviewRow updatedRecord) {
        adminManager.saveReviewRecord(toReviewRecord(updatedRecord));
        replaceRecord(updatedRecord);

        selectedRecordIds.remove(updatedRecord.id());
        renderRows();
        openReviewWorkspace(updatedRecord);
    }

    private void replaceRecord(ReviewRow updatedRecord) {
        for (int index = 0; index < records.size(); index++) {
            if (records.get(index).id().equals(updatedRecord.id())) {
                records.set(index, updatedRecord);
                break;
            }
        }
    }

    @FXML
    private void showBoxTab() {
        setActiveWorkspaceTab(boxTabButton);
    }

    @FXML
    private void showCaseTab() {
        setActiveWorkspaceTab(caseTabButton);
    }

    @FXML
    private void showDocumentTab() {
        setActiveWorkspaceTab(documentTabButton);
    }

    @FXML
    private void showPageTab() {
        setActiveWorkspaceTab(pageTabButton);
    }

    private void setActiveWorkspaceTab(Button activeButton) {
        List<Button> tabButtons = new ArrayList<>();
        if (boxTabButton != null) {
            tabButtons.add(boxTabButton);
        }
        if (caseTabButton != null) {
            tabButtons.add(caseTabButton);
        }
        if (documentTabButton != null) {
            tabButtons.add(documentTabButton);
        }
        if (pageTabButton != null) {
            tabButtons.add(pageTabButton);
        }

        for (Button tabButton : tabButtons) {
            tabButton.getStyleClass().removeAll("review-workspace-tab-active", "review-workspace-tab-button");

            if (tabButton == activeButton) {
                tabButton.getStyleClass().add("review-workspace-tab-active");
            } else {
                tabButton.getStyleClass().add("review-workspace-tab-button");
            }
        }
    }

    private void loadRecords() {
        if (adminManager == null) {
            recordsLoading = false;
            records.clear();
            refreshFilterOptions();
            return;
        }

        AdminManager managerSnapshot = adminManager;
        long loadId = ++recordsLoadSequence;
        recordsLoading = true;
        renderRows();

        BackgroundExecutor.io().execute(() -> {
            List<ReviewRow> loadedRows;
            try {
                loadedRows = managerSnapshot.getReviewRecords().stream()
                        .map(this::toReviewRow)
                        .toList();
            } catch (RuntimeException exception) {
                loadedRows = List.of();
            }

            List<ReviewRow> finalLoadedRows = loadedRows;
            Platform.runLater(() -> {
                if (loadId != recordsLoadSequence || adminManager != managerSnapshot) {
                    return;
                }
                recordsLoading = false;
                records.setAll(finalLoadedRows);
                refreshFilterOptions();
                renderRows();
            });
        });
    }

    private ReviewRow toReviewRow(ReviewRecord record) {
        return new ReviewRow(
                record.getId(),
                record.getIdentity(),
                record.getClient(),
                record.getArchive(),
                record.getProfile(),
                record.getDocumentDetailsTemplate(),
                record.getDocumentDetailsStatus(),
                record.getQaStatus(),
                record.getPages(),
                record.getLastUpdated(),
                record.getAssignedTo(),
                record.getScannedBy(),
                record.getDateGroup(),
                record.hasWarning()
        );
    }

    private ReviewRecord toReviewRecord(ReviewRow row) {
        return new ReviewRecord(
                row.id(),
                row.identity(),
                row.client(),
                row.archive(),
                row.profile(),
                row.documentDetailsTemplate(),
                row.documentDetailsStatus(),
                row.qaStatus(),
                row.pages(),
                row.lastUpdated(),
                row.assignedTo(),
                row.scannedBy(),
                row.dateGroup(),
                row.warning()
        );
    }

    private boolean isQaReviewRow(ReviewRow row) {
        return row != null && row.id() != null && row.id().startsWith("qa:");
    }

    private void chooseQaAssignee(String recordId, Consumer<QaAssigneeOption> onResult) {
        if (adminManager == null || recordId == null || !recordId.startsWith("qa:")) {
            onResult.accept(null);
            return;
        }

        List<QaAssigneeOption> options = adminManager.getEligibleQaAssignees(recordId).stream()
                .map(user -> new QaAssigneeOption(
                        user.getId(),
                        Strings.displayText(user.getName(), user.getUsername()),
                        Strings.displayText(user.getEmail(), "No email"),
                        user.getAssignedProfiles().isEmpty() ? "-" : String.join(", ", user.getAssignedProfiles())
                ))
                .toList();

        if (options.isEmpty()) {
            onResult.accept(null);
            return;
        }

        if (overviewPane == null || overviewPane.getScene() == null
                || !(overviewPane.getScene().getRoot() instanceof Pane sceneRoot)) {
            onResult.accept(null);
            return;
        }

        Label titleLabel = new Label("Assign QA");
        titleLabel.getStyleClass().add("qa-picker-title");

        Label subtitleLabel = new Label("Choose a reviewer for this task.");
        subtitleLabel.getStyleClass().add("qa-picker-subtitle");

        VBox headerCopy = new VBox(3, titleLabel, subtitleLabel);
        HBox.setHgrow(headerCopy, Priority.ALWAYS);

        Button closeButton = new Button("✕");
        closeButton.getStyleClass().add("qa-picker-close-button");
        closeButton.setFocusTraversable(false);

        HBox header = new HBox(8, headerCopy, closeButton);
        header.setAlignment(Pos.TOP_LEFT);
        header.getStyleClass().add("qa-picker-header");

        TextField searchField = new TextField();
        searchField.setPromptText("Search users");
        searchField.getStyleClass().add("search-field-input");
        searchField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Label searchIcon = new Label("");
        searchIcon.getStyleClass().addAll("prime-icon", "field-icon-path");
        StackPane searchIconShell = new StackPane(searchIcon);
        searchIconShell.getStyleClass().add("field-icon-shell");

        HBox searchBox = new HBox(searchIconShell, searchField);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setMaxWidth(Double.MAX_VALUE);
        searchBox.getStyleClass().add("qa-picker-search");

        HBox searchShell = new HBox(searchBox);
        HBox.setHgrow(searchBox, Priority.ALWAYS);
        searchShell.getStyleClass().add("qa-picker-search-shell");

        VBox listBox = new VBox(0);
        listBox.setFillWidth(true);
        listBox.getStyleClass().add("qa-picker-list");

        QaAssigneeOption[] selectedSlot = {null};

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("profile-secondary-button", "qa-picker-cancel-button");
        cancelBtn.setFocusTraversable(false);

        Button assignBtn = new Button("Assign QA");
        assignBtn.getStyleClass().add("qa-picker-assign-button");
        assignBtn.setFocusTraversable(false);
        assignBtn.setDisable(true);

        HBox footer = new HBox(8, cancelBtn, assignBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("qa-picker-footer");

        Runnable refresh = () -> {
            String query = Strings.normalize(searchField.getText());
            listBox.getChildren().setAll(
                    options.stream()
                            .filter(o -> query.isEmpty()
                                    || Strings.normalize(o.displayName()).contains(query)
                                    || Strings.normalize(o.email()).contains(query))
                            .map(opt -> buildQaPickerRow(opt, selectedSlot, listBox, assignBtn))
                            .toList()
            );
        };

        searchField.textProperty().addListener((obs, o, n) -> refresh.run());
        refresh.run();

        ScrollPane listScroll = new ScrollPane(listBox);
        listScroll.setFitToWidth(true);
        listScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        listScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        listScroll.getStyleClass().add("qa-picker-list-scroll");
        listScroll.setPrefHeight(340);
        listScroll.setMaxHeight(400);
        VBox.setVgrow(listScroll, Priority.ALWAYS);

        VBox pickerBox = new VBox(0, header, searchShell, listScroll, footer);
        pickerBox.getStyleClass().add("qa-picker-popup");
        pickerBox.setPrefWidth(480);
        pickerBox.setMaxWidth(480);
        pickerBox.setMaxHeight(600);

        Pane overlay = new Pane();
        overlay.setMaxWidth(Double.MAX_VALUE);
        overlay.setMaxHeight(Double.MAX_VALUE);

        Region backdrop = new Region();
        backdrop.getStyleClass().add("qa-picker-backdrop");
        backdrop.setPickOnBounds(true);
        backdrop.prefWidthProperty().bind(overlay.widthProperty());
        backdrop.prefHeightProperty().bind(overlay.heightProperty());

        overlay.prefWidthProperty().bind(sceneRoot.widthProperty());
        overlay.prefHeightProperty().bind(sceneRoot.heightProperty());

        pickerBox.layoutXProperty().bind(
                overlay.widthProperty().subtract(pickerBox.widthProperty()).divide(2));
        pickerBox.layoutYProperty().bind(
                overlay.heightProperty().subtract(pickerBox.heightProperty()).divide(2));

        overlay.getChildren().addAll(backdrop, pickerBox);
        sceneRoot.getChildren().add(overlay);

        boolean[] completed = {false};
        Runnable close = () -> {
            if (completed[0]) return;
            completed[0] = true;
            backdrop.prefWidthProperty().unbind();
            backdrop.prefHeightProperty().unbind();
            overlay.prefWidthProperty().unbind();
            overlay.prefHeightProperty().unbind();
            pickerBox.layoutXProperty().unbind();
            pickerBox.layoutYProperty().unbind();
            sceneRoot.getChildren().remove(overlay);
        };

        closeButton.setOnAction(e -> { close.run(); onResult.accept(null); });
        cancelBtn.setOnAction(e -> { close.run(); onResult.accept(null); });
        backdrop.setOnMouseClicked(e -> { close.run(); onResult.accept(null); });
        assignBtn.setOnAction(e -> {
            QaAssigneeOption sel = selectedSlot[0];
            close.run();
            onResult.accept(sel);
        });

        Platform.runLater(searchField::requestFocus);
    }

    private Button buildQaPickerRow(QaAssigneeOption option,
                                    QaAssigneeOption[] selectedSlot,
                                    VBox listBox,
                                    Button assignBtn) {
        Button row = new Button();
        row.setMaxWidth(Double.MAX_VALUE);
        row.setFocusTraversable(false);
        row.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        row.getStyleClass().add("qa-picker-row");

        boolean isSelected = selectedSlot[0] != null && selectedSlot[0].userId() == option.userId();
        if (isSelected) {
            row.getStyleClass().add("qa-picker-row-selected");
        }

        Label avatar = new Label(Strings.initials(option.displayName(), "?"));
        avatar.getStyleClass().add("qa-picker-avatar");

        Label nameLabel = new Label(option.displayName());
        nameLabel.getStyleClass().add("qa-picker-name");

        Label emailLabel = new Label(option.email());
        emailLabel.getStyleClass().add("qa-picker-email");

        VBox textBox = new VBox(3, nameLabel, emailLabel);
        textBox.setMinWidth(0);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label check = new Label("✓");
        check.getStyleClass().add("qa-picker-check");
        check.setVisible(isSelected);

        HBox content = new HBox(12, avatar, textBox, check);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMaxWidth(Double.MAX_VALUE);
        content.setFillHeight(false);
        content.getStyleClass().add("qa-picker-row-content");

        row.setGraphic(content);

        row.setOnAction(e -> {
            selectedSlot[0] = option;
            assignBtn.setDisable(false);
            for (Node child : listBox.getChildren()) {
                child.getStyleClass().remove("qa-picker-row-selected");
                if (child instanceof Button btn && btn.getGraphic() instanceof HBox hbox) {
                    for (Node hboxChild : hbox.getChildren()) {
                        if (hboxChild instanceof Label lbl
                                && lbl.getStyleClass().contains("qa-picker-check")) {
                            lbl.setVisible(false);
                        }
                    }
                }
            }
            row.getStyleClass().add("qa-picker-row-selected");
            check.setVisible(true);
        });

        return row;
    }

    private VBox buildWorkspaceExportDialogContent(Stage stage) {
        List<Document> exportDocuments = adminManager == null || activeReviewRecord == null
                ? List.of()
                : adminManager.getExportableDocumentsForRecord(activeReviewRecord.id());
        List<String> boxFiles = buildExportFiles(exportDocuments);
        ObjectProperty<TiffExportType> selectedType = new SimpleObjectProperty<>(TiffExportType.MULTI_PAGE);

        Label title = new Label("TIFF Export");
        title.getStyleClass().add("exports-dialog-title");

        VBox header = new VBox(9, title);
        header.getStyleClass().add("exports-dialog-header");

        Label boxValue = new Label(activeReviewRecord == null ? "-" : activeReviewRecord.identity());
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
        exportButton.setOnAction(event -> handleWorkspaceExport(stage, selectedType.get(), exportDocuments));

        HBox footerActions = new HBox(9, cancelButton, exportButton);
        footerActions.getStyleClass().add("exports-dialog-footer-actions");
        footerActions.setAlignment(Pos.CENTER_RIGHT);

        VBox footer = new VBox(9, outputBox, footerActions);
        footer.getStyleClass().add("exports-dialog-footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setFillWidth(true);

        VBox content = new VBox(18,
                header,
                boxCard,
                typeRow,
                filesCard,
                divider,
                footer
        );
        content.getStyleClass().add("exports-dialog-content");
        content.setFillWidth(true);
        return content;
    }

    private Button buildExportTypeCard(
            String title,
            String subtitle,
            TiffExportType type,
            ObjectProperty<TiffExportType> selectedType
    ) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("exports-dialog-option-title");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("exports-dialog-option-subtitle");

        VBox textBox = new VBox(9, titleLabel, subtitleLabel);
        textBox.getStyleClass().add("exports-dialog-option-copy");

        Label checkIcon = new Label("✓");
        checkIcon.getStyleClass().add("exports-dialog-option-check-icon");
        StackPane checkBadge = new StackPane(checkIcon);
        checkBadge.getStyleClass().add("exports-dialog-option-check-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox content = new HBox(12, textBox, spacer, checkBadge);
        content.getStyleClass().add("exports-dialog-option-content");
        content.setAlignment(Pos.TOP_LEFT);

        Button button = new Button();
        button.setGraphic(content);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.getStyleClass().add("exports-dialog-option-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setFocusTraversable(false);
        button.setOnAction(event -> selectedType.set(type));

        Runnable refreshState = () -> updateExportTypeCard(button, checkBadge, selectedType.get() == type);
        selectedType.addListener((observable, oldValue, newValue) -> refreshState.run());
        refreshState.run();

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

        if (selectedFiles == null || selectedFiles.isEmpty()) {
            Label empty = new Label("No approved documents available for this export.");
            empty.getStyleClass().add("exports-dialog-empty-state");
            fileGrid.add(empty, 0, 0, 3, 1);
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

    private List<String> buildExportFiles(List<Document> exportDocuments) {
        List<String> files = new ArrayList<>();
        if (exportDocuments == null) {
            return files;
        }

        for (Document document : exportDocuments) {
            files.add(document.getSourceItemId() + ".tiff");
        }
        return files;
    }

    private String formatSelectedDocumentCount(int count) {
        return count + " " + pluralize(count, "document");
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

    private void handleWorkspaceExport(Stage stage, TiffExportType exportType, List<Document> exportDocuments) {
        if (exportDocuments == null || exportDocuments.isEmpty()) {
            showExportAlert(stage, Alert.AlertType.ERROR, "No documents to export",
                    "There are no approved documents ready for TIFF export.");
            return;
        }

        String profileName = activeReviewRecord == null ? "" : activeReviewRecord.profile();
        String boxId = activeReviewRecord == null ? "" : activeReviewRecord.identity();
        ScanProfile profile = adminManager == null ? null : adminManager.findProfileByName(profileName);
        String profileCode = firstNonBlank(profile == null ? null : profile.getCode(), profileName);
        String exportNaming = firstNonBlank(profile == null ? null : profile.getExportNaming(), ScanProfile.DEFAULT_EXPORT_NAMING);

        try {
            Path outputDirectory = Path.of(
                    System.getProperty("user.home"),
                    "Downloads",
                    "WebLager Exports",
                    safeFolderSegment(profileName, boxId)
            );

            TiffExportManager tiffExportManager = new TiffExportManager();
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

    private List<easv.be.PageImage> flattenExportPages(List<Document> exportDocuments) {
        List<easv.be.PageImage> pages = new ArrayList<>();
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

    private String pluralize(int count, String singular) {
        return count == 1 ? singular : singular + "s";
    }

    private ColumnConstraints percentColumn(double percentWidth) {
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(percentWidth);
        column.setFillWidth(true);
        column.setHgrow(Priority.ALWAYS);
        return column;
    }

    private void refreshFilterOptions() {
        setComboOptions(clientFilterComboBox, ALL_CLIENTS, records.stream().map(ReviewRow::client).toList());
        setComboOptions(archiveFilterComboBox, ALL_ARCHIVES, records.stream().map(ReviewRow::archive).toList());
        setComboOptions(profileFilterComboBox, ALL_PROFILES, records.stream().map(ReviewRow::profile).toList());
        setComboOptions(scannedByFilterComboBox, ALL_USERS, records.stream().map(ReviewRow::scannedBy).toList());
    }

    private void setComboOptions(ComboBox<String> comboBox, String allOption, List<String> values) {
        if (comboBox == null) {
            return;
        }

        String selectedValue = comboBox.getValue();
        List<String> options = new ArrayList<>();
        options.add(allOption);

        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(options::add);

        comboBox.getItems().setAll(options);

        if (selectedValue != null && options.contains(selectedValue)) {
            comboBox.setValue(selectedValue);
        } else {
            comboBox.setValue(allOption);
        }
    }

    private enum ReviewQueueFilter {
        ALL,
        MISSING_REQUIRED,
        EXPORT_BLOCKED,
        FAILED_VALIDATION,
        READY_FOR_QA,
        QA_REJECTED,
        RECENTLY_SCANNED
    }

    record ReviewRow(
            String id,
            String identity,
            String client,
            String archive,
            String profile,
            String documentDetailsTemplate,
            String documentDetailsStatus,
            String qaStatus,
            int pages,
            String lastUpdated,
            String assignedTo,
            String scannedBy,
            String dateGroup,
            boolean warning
    ) {
        private ReviewRow withLastUpdated(String updatedAt) {
            return new ReviewRow(
                    id,
                    identity,
                    client,
                    archive,
                    profile,
                    documentDetailsTemplate,
                    documentDetailsStatus,
                    qaStatus,
                    pages,
                    updatedAt,
                    assignedTo,
                    scannedBy,
                    dateGroup,
                    warning
            );
        }

        private ReviewRow withAssignedTo(String newAssignedTo) {
            return new ReviewRow(
                    id,
                    identity,
                    client,
                    archive,
                    profile,
                    documentDetailsTemplate,
                    documentDetailsStatus,
                    qaStatus,
                    pages,
                    "Updated just now",
                    newAssignedTo,
                    scannedBy,
                    dateGroup,
                    warning
            );
        }

        private ReviewRow withReviewState(String newDocumentDetailsStatus, String newQaStatus, boolean hasWarning) {
            return new ReviewRow(
                    id,
                    identity,
                    client,
                    archive,
                    profile,
                    documentDetailsTemplate,
                    newDocumentDetailsStatus,
                    newQaStatus,
                    pages,
                    "Updated just now",
                    assignedTo,
                    scannedBy,
                    dateGroup,
                    hasWarning
            );
        }
    }

    private record WorkspaceQaDocument(String name, List<WorkspaceQaPage> pages) {
    }

    private record WorkspaceQaPage(
            int pageNumber,
            int globalPageNumber,
            String sourceReference,
            String imageContent,
            int rotationDegrees,
            QAService.QaPageReviewStatus status,
            String comment
    ) {
        private WorkspaceQaPage {
            sourceReference = sourceReference == null ? "" : sourceReference;
            imageContent = imageContent == null ? "" : imageContent;
            comment = comment == null ? "" : comment;
        }

        private WorkspaceQaPage withStatus(QAService.QaPageReviewStatus updatedStatus) {
            return new WorkspaceQaPage(
                    pageNumber,
                    globalPageNumber,
                    sourceReference,
                    imageContent,
                    rotationDegrees,
                    updatedStatus == null ? QAService.QaPageReviewStatus.NOT_REVIEWED : updatedStatus,
                    comment
            );
        }

        private WorkspaceQaPage withComment(String updatedComment) {
            return new WorkspaceQaPage(
                    pageNumber,
                    globalPageNumber,
                    sourceReference,
                    imageContent,
                    rotationDegrees,
                    status,
                    updatedComment
            );
        }
    }

    private record PagePointer(int documentIndex, int pageIndex) {
    }

    private record QaAssigneeOption(int userId, String displayName, String email, String profileSummary) {
        @Override
        public String toString() {
            return displayName;
        }
    }

    private enum TiffExportType {
        SINGLE_PAGE,
        MULTI_PAGE
    }
}
