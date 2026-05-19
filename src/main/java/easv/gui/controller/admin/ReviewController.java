package easv.gui.controller.admin;

import easv.be.ReviewRecord;
import easv.bll.AdminManager;
import easv.gui.AppDates;
import easv.gui.PrimeIcons;
import easv.gui.SearchableComboBoxes;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

    @FXML
    private void initialize() {
        configureFilters();
        configureWorkspaceControls();
        configureRowsPerPageSelector();
        loadRecords();
        configureListeners();

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
        configureDateRangePicker();

        clientFilterComboBox.getItems().setAll(
                ALL_CLIENTS,
                "Aalborg Municipality",
                "Maersk Archive",
                "Copenhagen Airport"
        );
        clientFilterComboBox.setValue(ALL_CLIENTS);

        archiveFilterComboBox.getItems().setAll(
                ALL_ARCHIVES,
                "Building Archive",
                "Technical Archive",
                "Airport Archive"
        );
        archiveFilterComboBox.setValue(ALL_ARCHIVES);

        profileFilterComboBox.getItems().setAll(
                ALL_PROFILES,
                "Building Archive",
                "Technical Drawings",
                "Standard Scan"
        );
        profileFilterComboBox.setValue(ALL_PROFILES);

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

        dateRangeFilterComboBox.getItems().setAll(
                RANGE_LAST_30_DAYS,
                RANGE_TODAY,
                RANGE_LAST_7_DAYS,
                RANGE_THIS_MONTH,
                RANGE_ALL_TIME,
                RANGE_CUSTOM
        );
        setDateRange(RANGE_LAST_30_DAYS, LocalDate.now().minusDays(30), LocalDate.now());

        scannedByFilterComboBox.getItems().setAll(
                ALL_USERS,
                "Sarah Smith",
                "John Doe",
                "Sofia Nielsen",
                "System Import"
        );
        scannedByFilterComboBox.setValue(ALL_USERS);

        SearchableComboBoxes.configure(clientFilterComboBox);
        SearchableComboBoxes.configure(archiveFilterComboBox);
        SearchableComboBoxes.configure(profileFilterComboBox);
        SearchableComboBoxes.configure(qaStatusFilterComboBox);
        SearchableComboBoxes.configure(dateRangeFilterComboBox);
        SearchableComboBoxes.configure(scannedByFilterComboBox);
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
        departmentComboBox.setValue("Technical Services");
        SearchableComboBoxes.configure(documentTypeComboBox);

        caseNumberField.setText("2026-042");
        registrationDateField.setText("Invalid date");
        buildingAddressField.setText("Skagerrakvej 16");
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

        clientFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        archiveFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        profileFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        qaStatusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        dateRangeFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingDateControls) {
                return;
            }

            applyPresetDateRange(newValue);
            applyFilters();
        });
        scannedByFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());

        dateRangePicker.setOnAction(event -> handleDateRangeSelection(dateRangePicker.getValue()));
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
        renderPagination(pageSlice, totalRecords);
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
        updateSelectedRecords(record -> record.withAssignedTo("QA Team"));
    }

    @FXML
    private void markSelectedReadyForQa() {
        updateSelectedRecords(record -> record.withReviewState("Complete", "Ready for QA", false));
    }

    @FXML
    private void exportSelected() {
        int selectedCount = selectedRecordIds.size();

        if (selectedCount == 0) {
            return;
        }

        adminManager.addAuditLog("Exports", "Exported selected review items", "Review Center", "Success",
                selectedCount + " review items were exported.");

        selectedRecordIds.clear();
        renderRows();
        paginationSummaryLabel.setText("Exported " + selectedCount + " selected records");
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

    private void renderPagination(PaginationHelper.PageSlice pageSlice, int totalRecords) {
        paginationButtonsBox.getChildren().clear();

        if (totalRecords == 0) {
            paginationSummaryLabel.setText("Showing 0 records");
            return;
        }

        paginationSummaryLabel.setText(
                "Showing " + (pageSlice.fromIndex() + 1) + "-"
                        + pageSlice.toIndex()
                        + " of "
                        + totalRecords
                        + " records"
        );

        paginationButtonsBox.getChildren().add(createPaginationButton("<<", 1, currentPage == 1));
        paginationButtonsBox.getChildren().add(createPaginationButton("<", currentPage - 1, currentPage == 1));

        for (String pageItem : PaginationHelper.buildPageItems(currentPage, pageSlice.totalPages())) {
            Node paginationItem = PaginationHelper.ELLIPSIS.equals(pageItem)
                    ? createPaginationEllipsis()
                    : createPaginationButton(pageItem, Integer.parseInt(pageItem), false);

            paginationButtonsBox.getChildren().add(paginationItem);
        }

        paginationButtonsBox.getChildren().add(createPaginationButton(
                ">",
                currentPage + 1,
                currentPage == pageSlice.totalPages()
        ));
        paginationButtonsBox.getChildren().add(createPaginationButton(
                ">>",
                pageSlice.totalPages(),
                currentPage == pageSlice.totalPages()
        ));
    }

    private Button createPaginationButton(String text, int targetPage, boolean disabled) {
        Button button = new Button(text);
        button.getStyleClass().add("pagination-button");
        button.setFocusTraversable(false);
        button.setDisable(disabled);

        if (text.equals(String.valueOf(currentPage))) {
            button.getStyleClass().add("pagination-button-active");
            return button;
        }

        if (!disabled) {
            button.setOnAction(event -> {
                currentPage = targetPage;
                renderRows();
            });
        }

        return button;
    }

    private Label createPaginationEllipsis() {
        Label ellipsis = new Label("...");
        ellipsis.getStyleClass().add("pagination-ellipsis");
        return ellipsis;
    }

    private boolean matchesSearch(ReviewRow record) {
        String searchText = normalize(searchField.getText());

        if (searchText.isBlank()) {
            return true;
        }

        return normalize(record.identity()).contains(searchText)
                || normalize(record.client()).contains(searchText)
                || normalize(record.archive()).contains(searchText)
                || normalize(record.profile()).contains(searchText)
                || normalize(record.qaStatus()).contains(searchText)
                || normalize(record.assignedTo()).contains(searchText)
                || normalize(record.scannedBy()).contains(searchText)
                || normalize(record.lastUpdated()).contains(searchText)
                || normalize(record.dateGroup()).contains(searchText)
                || String.valueOf(record.pages()).contains(searchText);
    }

    private boolean matchesFilters(ReviewRow record) {
        return matchesCombo(record.client(), clientFilterComboBox.getValue(), ALL_CLIENTS)
                && matchesCombo(record.archive(), archiveFilterComboBox.getValue(), ALL_ARCHIVES)
                && matchesCombo(record.profile(), profileFilterComboBox.getValue(), ALL_PROFILES)
                && matchesQueueFilter(record)
                && matchesCombo(record.qaStatus(), qaStatusFilterComboBox.getValue(), ALL_QA_STATUSES)
                && matchesCombo(record.scannedBy(), scannedByFilterComboBox.getValue(), ALL_USERS)
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
        if (RANGE_ALL_TIME.equals(dateRangeFilterComboBox.getValue())) {
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
        String normalizedDateText = normalize(dateText);

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

    private String documentDetailsStatusClass(String status) {
        return switch (normalize(status)) {
            case "not started" -> "review-status-neutral";
            case "incomplete" -> "review-status-warning";
            case "missing required fields", "invalid" -> "review-status-danger";
            case "complete", "approved" -> "review-status-success";
            default -> "review-status-neutral";
        };
    }

    private String qaStatusClass(String status) {
        return switch (normalize(status)) {
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

        clientFilterComboBox.setValue(ALL_CLIENTS);
        archiveFilterComboBox.setValue(ALL_ARCHIVES);
        profileFilterComboBox.setValue(ALL_PROFILES);
        qaStatusFilterComboBox.setValue(ALL_QA_STATUSES);
        setDateRange(RANGE_LAST_30_DAYS, LocalDate.now().minusDays(30), LocalDate.now());
        scannedByFilterComboBox.setValue(ALL_USERS);
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

    @FXML
    private void exportReport() {
        int exportCount = filteredRecords.isEmpty() ? records.size() : filteredRecords.size();
        adminManager.addAuditLog("Exports", "Exported review center report", "Review Center", "Success",
                "A review center report was exported.");
        paginationSummaryLabel.setText("Exported review report for " + exportCount + " records");
    }

    private void openReviewWorkspace(ReviewRow record) {
        activeReviewRecord = record;

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
                        ? "Export blocked: 2 required document fields are missing."
                        : "Document details are complete. Ready for QA assignment or approval."
        );

        configureWorkspaceControls();
        showWorkspace();
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

        for (int index = 0; index < records.size(); index++) {
            if (records.get(index).id().equals(updatedRecord.id())) {
                records.set(index, updatedRecord);
                break;
            }
        }

        selectedRecordIds.remove(updatedRecord.id());
        renderRows();
        openReviewWorkspace(updatedRecord);
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
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
}
