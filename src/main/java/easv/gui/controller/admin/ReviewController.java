package easv.gui.controller.admin;

import easv.be.Document;
import easv.be.ReviewRecord;
import easv.be.ScanProfile;
import easv.bll.AdminManager;
import easv.bll.QAService;
import easv.bll.TiffExportManager;
import easv.bll.TiffImageSupport;
import easv.gui.controller.util.AppDates;
import easv.gui.controller.util.PaginationHelper;
import easv.gui.controller.util.SearchableComboBoxes;
import easv.gui.controller.util.Strings;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;
import javafx.embed.swing.SwingFXUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
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
    private static final String RANGE_THIS_MONTH = "This Month";
    private static final String RANGE_ALL_TIME = "All Time";
    private static final String RANGE_CUSTOM = "Custom Range";

    private static final int DEFAULT_ROWS_PER_PAGE = 10;
    private static final List<Integer> ROWS_PER_PAGE_OPTIONS = List.of(10, 25, 50);
    private static final DateTimeFormatter DATE_RANGE_FORMATTER = AppDates.FORMATTER;

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

    private ReviewRow activeReviewRecord;
    private ReviewQueueFilter activeQueueFilter = ReviewQueueFilter.ALL;
    private AdminManager adminManager;

    @FXML private VBox overviewPane;
    @FXML private VBox workspacePane;

    @FXML private TextField searchField;

    @FXML private ComboBox<String> clientFilterComboBox;
    @FXML private ComboBox<String> archiveFilterComboBox;
    @FXML private ComboBox<String> profileFilterComboBox;
    @FXML private ComboBox<String> qaStatusFilterComboBox;
    @FXML private ComboBox<String> dateRangeFilterComboBox;
    @FXML private DatePicker dateRangePicker;
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

    @FXML
    private void initialize() {
        configureFilters();
        configureWorkspaceControls();
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
        if (dateRangePicker != null) {
            configureDateRangePicker();
        }

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
                    RANGE_LAST_30_DAYS,
                    RANGE_TODAY,
                    RANGE_LAST_7_DAYS,
                    RANGE_THIS_MONTH,
                    RANGE_ALL_TIME,
                    RANGE_CUSTOM
            );
        }
        setDateRange(RANGE_LAST_30_DAYS, LocalDate.now().minusDays(30), LocalDate.now());

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

        if (dateRangePicker != null) {
            dateRangePicker.setOnAction(event -> handleDateRangeSelection(dateRangePicker.getValue()));
        }
    }

    private void configureWorkspacePreviewInteractions() {
        if (reviewWorkspacePreviewHost == null) {
            return;
        }

        reviewWorkspacePreviewHost.setFocusTraversable(true);
        reviewWorkspacePreviewHost.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleWorkspacePreviewMousePressed);
        reviewWorkspacePreviewHost.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleWorkspacePreviewMouseDragged);
        reviewWorkspacePreviewHost.addEventFilter(ScrollEvent.SCROLL, this::handleWorkspacePreviewScroll);
        reviewWorkspacePreviewHost.widthProperty().addListener((observable, oldValue, newValue) -> clampWorkspacePreviewTranslation());
        reviewWorkspacePreviewHost.heightProperty().addListener((observable, oldValue, newValue) -> clampWorkspacePreviewTranslation());
    }

    private void applyFilters() {
        currentPage = 1;
        renderRows();
    }

    private void renderRows() {
        filteredRecords = records.stream()
                .filter(this::matchesSearch)
                .filter(this::matchesFilters)
                .toList();

        int totalRecords = filteredRecords.size();
        PaginationHelper.PageSlice pageSlice = PaginationHelper.slice(currentPage, rowsPerPage, totalRecords);

        currentPage = pageSlice.currentPage();

        pageRecords = filteredRecords.subList(pageSlice.fromIndex(), pageSlice.toIndex());

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

        CheckBox selectCheckBox = new CheckBox();
        selectCheckBox.getStyleClass().add("review-checkbox");
        selectCheckBox.setSelected(selectedRecordIds.contains(record.id()));
        selectCheckBox.selectedProperty().addListener((observable, oldValue, selected) ->
                updateSelection(record.id(), selected)
        );

        addCell(row, selectCheckBox, 0, HPos.CENTER);
        addCell(row, createWrappedLabel(record.identity(), "review-main-cell"), 1, HPos.LEFT);
        addCell(row, createWrappedLabel(record.client(), "review-cell-text"), 2, HPos.LEFT);
        addCell(row, createWrappedLabel(record.profile(), "review-cell-text"), 3, HPos.LEFT);
        addCell(row, createStatusBadge(record.qaStatus()), 4, HPos.LEFT);
        addCell(row, createWrappedLabel(String.valueOf(record.pages()), "review-cell-text"), 5, HPos.CENTER);
        addCell(row, createWrappedLabel(record.lastUpdated(), "review-cell-text"), 6, HPos.LEFT);
        addCell(row, createWrappedLabel(record.assignedTo(), "review-cell-text"), 7, HPos.LEFT);
        addCell(row, createReviewButton(record), 8, HPos.LEFT);

        return row;
    }

    private List<ColumnConstraints> createTableColumns() {
        return List.of(
                createPercentColumn(4),
                createPercentColumn(20),
                createPercentColumn(14),
                createPercentColumn(14),
                createPercentColumn(12),
                createPercentColumn(6),
                createPercentColumn(12),
                createPercentColumn(10),
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

        GridPane.setHgrow(content, Priority.ALWAYS);
        GridPane.setFillWidth(content, true);
        GridPane.setHalignment(content, alignment);
        GridPane.setValignment(content, VPos.CENTER);

        row.add(content, columnIndex, 0);
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
        button.setOnAction(event -> openReviewWorkspace(record));
        return button;
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
            case RANGE_THIS_MONTH -> setDateRange(RANGE_THIS_MONTH, today.withDayOfMonth(1), today);
            case RANGE_ALL_TIME -> setDateRange(RANGE_ALL_TIME, null, null);
            case RANGE_CUSTOM -> {
            }
            default -> setDateRange(RANGE_LAST_30_DAYS, today.minusDays(30), today);
        }
    }

    private void setDateRange(String selectedRange, LocalDate fromDate, LocalDate toDate) {
        updatingDateControls = true;
        this.fromDate = fromDate;
        this.toDate = toDate;
        pendingRangeStart = null;
        dateRangeFilterComboBox.setValue(selectedRange);
        dateRangePicker.setValue(toDate != null ? toDate : fromDate);
        dateRangePicker.getEditor().setText(formatDateRange());
        dateRangePicker.setDayCellFactory(dateRangePicker.getDayCellFactory());
        updatingDateControls = false;
    }

    private void handleDateRangeSelection(LocalDate selectedDate) {
        if (updatingDateControls || selectedDate == null) {
            return;
        }

        updatingDateControls = true;

        if (pendingRangeStart == null) {
            pendingRangeStart = selectedDate;
            fromDate = selectedDate;
            toDate = null;
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

        dateRangeFilterComboBox.setValue(RANGE_CUSTOM);
        dateRangePicker.setValue(toDate != null ? toDate : fromDate);
        dateRangePicker.getEditor().setText(formatDateRange());
        dateRangePicker.setDayCellFactory(dateRangePicker.getDayCellFactory());
        updatingDateControls = false;
        applyFilters();
    }

    private void configureDateRangePicker() {
        dateRangePicker.setPromptText("MM/DD/YYYY");
        dateRangePicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate value) {
                return formatDateRange();
            }

            @Override
            public LocalDate fromString(String value) {
                return null;
            }
        });

        dateRangePicker.setDayCellFactory(picker -> new DateCell() {
            {
                addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                    LocalDate selectedDate = getItem();

                    if (isSelectableDate(selectedDate)) {
                        selectDateFromCalendar(selectedDate, event);
                    }
                });
                addEventFilter(MouseEvent.MOUSE_RELEASED, MouseEvent::consume);
                addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
            }

            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                getStyleClass().removeAll(
                        "activity-log-date-range-start",
                        "activity-log-date-range-end",
                        "activity-log-date-range-between",
                        "activity-log-date-disabled"
                );

                if (empty || date == null) {
                    return;
                }

                if (date.isAfter(LocalDate.now())) {
                    setDisable(true);
                    getStyleClass().add("activity-log-date-disabled");
                    return;
                }

                if (isRangeStart(date)) {
                    getStyleClass().add("activity-log-date-range-start");
                }

                if (isRangeEnd(date)) {
                    getStyleClass().add("activity-log-date-range-end");
                }

                if (isBetweenRange(date)) {
                    getStyleClass().add("activity-log-date-range-between");
                }
            }
        });
    }

    private void selectDateFromCalendar(LocalDate selectedDate, MouseEvent event) {
        handleDateRangeSelection(selectedDate);

        if (pendingRangeStart == null) {
            dateRangePicker.hide();
        } else {
            dateRangePicker.show();
        }

        event.consume();
    }

    private boolean isSelectableDate(LocalDate date) {
        return date != null && !date.isAfter(LocalDate.now());
    }

    private boolean isRangeStart(LocalDate date) {
        return fromDate != null && date.equals(fromDate);
    }

    private boolean isRangeEnd(LocalDate date) {
        return toDate != null && date.equals(toDate);
    }

    private boolean isBetweenRange(LocalDate date) {
        return fromDate != null
                && toDate != null
                && date.isAfter(fromDate)
                && date.isBefore(toDate);
    }

    private String formatDateRange() {
        if (fromDate == null && toDate == null) {
            return RANGE_ALL_TIME;
        }

        if (fromDate != null && toDate == null) {
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
        if (reviewWorkspaceSearchField != null) {
            reviewWorkspaceSearchField.clear();
        }

        workspaceTitleLabel.setText(record.identity());
        workspaceSubtitleLabel.setText(
                record.profile()
                        + " - "
                        + record.client()
                        + " - "
                        + record.pages()
                        + " pages - "
                        + record.documentDetailsStatus()
        );

        boolean isBlocked = record.documentDetailsStatus().equalsIgnoreCase("Missing Required Fields")
                || record.documentDetailsStatus().equalsIgnoreCase("Invalid")
                || record.documentDetailsStatus().equalsIgnoreCase("Incomplete");

        workspaceWarningLabel.setText(
                isBlocked
                        ? "Export blocked: required document fields are missing."
                        : "Document details are complete. Ready for QA assignment or approval."
        );

        configureWorkspaceControls();
        loadWorkspaceQaData(record);
        renderWorkspaceQaView();
        showWorkspace();
    }

    private void loadWorkspaceQaData(ReviewRow record) {
        if (adminManager == null || !isQaReviewRow(record)) {
            return;
        }

        activeQaAssignment = adminManager.getQaAssignmentForReviewRecord(record.id());
        if (activeQaAssignment == null) {
            return;
        }

        List<WorkspaceQaDocument> documents = new ArrayList<>();
        for (QAService.QaDocumentSnapshot assignmentDocument : activeQaAssignment.documents()) {
            List<WorkspaceQaPage> pages = new ArrayList<>();
            for (QAService.QaPageSnapshot assignmentPage : assignmentDocument.pages()) {
                pages.add(new WorkspaceQaPage(
                        assignmentPage.pageNumber(),
                        assignmentPage.globalPageNumber(),
                        assignmentPage.sourceReference(),
                        assignmentPage.displayContent(),
                        assignmentPage.rotationDegrees(),
                        assignmentPage.reviewStatus()
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
        renderWorkspaceQaTree();
        renderWorkspaceQaPreview();
        renderWorkspaceQaThumbnails();
    }

    private void renderWorkspaceQaTree() {
        if (reviewWorkspaceTreeContainer == null) {
            return;
        }
        reviewWorkspaceTreeContainer.getChildren().clear();

        if (activeQaAssignment == null || activeQaDocuments.isEmpty()) {
            Label root = new Label(activeReviewRecord == null ? "No document selected" : activeReviewRecord.identity());
            root.getStyleClass().add("review-workspace-tree-root");
            reviewWorkspaceTreeContainer.getChildren().add(root);
            return;
        }

        Label root = new Label(activeQaAssignment.boxId());
        root.getStyleClass().add("review-workspace-tree-root");
        reviewWorkspaceTreeContainer.getChildren().add(root);

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

            VBox block = new VBox(6);

            Label title = new Label(document.name());
            title.getStyleClass().add("review-workspace-tree-case");
            block.getChildren().add(title);

            for (PagePointer pointer : matchingPages) {
                WorkspaceQaPage page = activeQaDocuments.get(pointer.documentIndex()).pages().get(pointer.pageIndex());
                HBox row = new HBox();
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add(pointer.documentIndex() == selectedQaDocumentIndex && pointer.pageIndex() == selectedQaPageIndex
                        ? "review-workspace-tree-row-active"
                        : "review-workspace-tree-row");

                Label statusIcon = new Label(statusIconText(page.status()));
                statusIcon.getStyleClass().add(statusIconStyle(page.status()));

                Label pageTitle = new Label("Page " + page.pageNumber());
                pageTitle.getStyleClass().add(pointer.documentIndex() == selectedQaDocumentIndex && pointer.pageIndex() == selectedQaPageIndex
                        ? "review-workspace-tree-title-active"
                        : "review-workspace-tree-title");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label pageStatus = new Label(statusText(page.status()));
                pageStatus.getStyleClass().add("review-workspace-tree-status-text");

                row.getChildren().addAll(statusIcon, pageTitle, spacer, pageStatus);
                row.setOnMouseClicked(event -> {
                    selectedQaDocumentIndex = pointer.documentIndex();
                    selectedQaPageIndex = pointer.pageIndex();
                    resetWorkspacePreviewTransform();
                    renderWorkspaceQaView();
                });
                block.getChildren().add(row);
            }

            reviewWorkspaceTreeContainer.getChildren().add(block);
        }
    }

    private void renderWorkspaceQaPreview() {
        if (reviewWorkspacePreviewHost == null) {
            return;
        }
        reviewWorkspacePreviewHost.getChildren().clear();
        currentReviewPreviewWrapper = null;
        currentReviewPreviewBaseWidth = 1.0;
        currentReviewPreviewBaseHeight = 1.0;

        WorkspaceQaPage page = getSelectedWorkspaceQaPage();
        if (page == null) {
            reviewWorkspacePreviewHost.getChildren().add(createWorkspacePreviewPlaceholder("No page selected"));
            if (reviewWorkspaceViewerPageLabel != null) {
                reviewWorkspaceViewerPageLabel.setText("No page selected");
            }
            return;
        }

        if (reviewWorkspaceViewerPageLabel != null) {
            reviewWorkspaceViewerPageLabel.setText("Page " + page.pageNumber() + " of " + getWorkspaceQaPageCount());
        }

        Image image = decodeWorkspaceQaImage(page.imageContent());
        if (image == null) {
            reviewWorkspacePreviewHost.getChildren().add(createWorkspacePreviewPlaceholder("Preview unavailable"));
            return;
        }

        StackPane paper = new StackPane();
        paper.getStyleClass().add("review-workspace-paper-preview");
        paper.setPickOnBounds(true);

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        currentReviewPreviewBaseWidth = Math.max(1, image.getWidth());
        currentReviewPreviewBaseHeight = Math.max(1, image.getHeight());
        imageView.setFitWidth(currentReviewPreviewBaseWidth);
        imageView.setFitHeight(currentReviewPreviewBaseHeight);
        imageView.setRotate(page.rotationDegrees());

        paper.getChildren().add(imageView);
        currentReviewPreviewWrapper = paper;
        reviewWorkspacePreviewHost.getChildren().add(paper);
        applyWorkspacePreviewTransform();
    }

    private void renderWorkspaceQaThumbnails() {
        if (reviewWorkspaceThumbnailStrip == null) {
            return;
        }
        reviewWorkspaceThumbnailStrip.getChildren().clear();

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
                reviewWorkspaceThumbnailStrip.getChildren().add(thumbnail);
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
        reviewPreviewZoomMultiplier = clamp(reviewPreviewZoomMultiplier + delta, 1.0, 5.0);
        applyWorkspacePreviewTransform();
    }

    private void resetWorkspacePreviewTransform() {
        reviewPreviewZoomMultiplier = 1.0;
        reviewPreviewTranslateX = 0;
        reviewPreviewTranslateY = 0;
    }

    private void applyWorkspacePreviewTransform() {
        if (currentReviewPreviewWrapper == null || reviewWorkspacePreviewHost == null) {
            return;
        }

        double availableWidth = Math.max(1, reviewWorkspacePreviewHost.getWidth() - 40);
        double availableHeight = Math.max(1, reviewWorkspacePreviewHost.getHeight() - 40);
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
        if (currentReviewPreviewWrapper == null || reviewWorkspacePreviewHost == null) {
            return;
        }

        double scale = currentReviewPreviewWrapper.getScaleX();
        double scaledWidth = currentReviewPreviewBaseWidth * scale;
        double scaledHeight = currentReviewPreviewBaseHeight * scale;
        double hostWidth = Math.max(1, reviewWorkspacePreviewHost.getWidth());
        double hostHeight = Math.max(1, reviewWorkspacePreviewHost.getHeight());
        double maxX = Math.max(0, (scaledWidth - hostWidth) / 2);
        double maxY = Math.max(0, (scaledHeight - hostHeight) / 2);

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

    private int getWorkspaceQaPageCount() {
        int count = 0;
        for (WorkspaceQaDocument document : activeQaDocuments) {
            count += document.pages().size();
        }
        return count;
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
    }

    @FXML
    private void showWorkspace() {
        overviewPane.setVisible(false);
        overviewPane.setManaged(false);

        workspacePane.setVisible(true);
        workspacePane.setManaged(true);
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
        List<Button> tabButtons = List.of(boxTabButton, caseTabButton, documentTabButton, pageTabButton);

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
            records.clear();
            refreshFilterOptions();
            return;
        }

        records.setAll(
                adminManager.getReviewRecords().stream()
                        .map(this::toReviewRow)
                        .toList()
        );
        refreshFilterOptions();
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
        searchField.getStyleClass().addAll("search-field-input", "qa-picker-search");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        HBox searchShell = new HBox(searchField);
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

        VBox textBox = new VBox(2, nameLabel, emailLabel);
        textBox.setMinWidth(0);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label check = new Label("✓");
        check.getStyleClass().add("qa-picker-check");
        check.setVisible(isSelected);

        HBox content = new HBox(12, avatar, textBox, check);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMaxWidth(Double.MAX_VALUE);
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
            QAService.QaPageReviewStatus status
    ) {
        private WorkspaceQaPage {
            sourceReference = sourceReference == null ? "" : sourceReference;
            imageContent = imageContent == null ? "" : imageContent;
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
}
