package easv.gui.controller.admin;

import easv.be.MetadataReviewRecord;
import easv.bll.AdminManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.UnaryOperator;

public class MetadataReviewController {

    private static final String ALL_CLIENTS = "All Clients";
    private static final String ALL_ARCHIVES = "All Archives";
    private static final String ALL_PROFILES = "All Profiles";
    private static final String ALL_METADATA_STATUSES = "All Metadata Statuses";
    private static final String ALL_QA_STATUSES = "All QA Statuses";
    private static final String ALL_USERS = "All Users";

    private static final int DEFAULT_ROWS_PER_PAGE = 10;
    private static final List<Integer> ROWS_PER_PAGE_OPTIONS = List.of(10, 25, 50);

    private final ObservableList<MetadataReviewRow> records = FXCollections.observableArrayList();
    private final Set<String> selectedRecordIds = new HashSet<>();

    private List<MetadataReviewRow> filteredRecords = List.of();
    private List<MetadataReviewRow> pageRecords = List.of();

    private int currentPage = 1;
    private int rowsPerPage = DEFAULT_ROWS_PER_PAGE;

    private MetadataReviewRow activeReviewRecord;
    private AdminManager adminManager;

    @FXML private VBox overviewPane;
    @FXML private VBox workspacePane;

    @FXML private TextField searchField;

    @FXML private ComboBox<String> clientFilterComboBox;
    @FXML private ComboBox<String> archiveFilterComboBox;
    @FXML private ComboBox<String> profileFilterComboBox;
    @FXML private ComboBox<String> metadataStatusFilterComboBox;
    @FXML private ComboBox<String> qaStatusFilterComboBox;
    @FXML private ComboBox<String> dateRangeFilterComboBox;
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

        metadataStatusFilterComboBox.getItems().setAll(
                ALL_METADATA_STATUSES,
                "Not Started",
                "Incomplete",
                "Missing Required Fields",
                "Invalid",
                "Complete",
                "Approved"
        );
        metadataStatusFilterComboBox.setValue(ALL_METADATA_STATUSES);

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
                "Last 30 Days",
                "Today",
                "Last 7 Days",
                "This Month",
                "All Time"
        );
        dateRangeFilterComboBox.setValue("Last 30 Days");

        scannedByFilterComboBox.getItems().setAll(
                ALL_USERS,
                "Sarah Smith",
                "John Doe",
                "Sofia Nielsen",
                "System Import"
        );
        scannedByFilterComboBox.setValue(ALL_USERS);
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
        metadataStatusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        qaStatusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        dateRangeFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        scannedByFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
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

    private long countRecords(java.util.function.Predicate<MetadataReviewRow> predicate) {
        return records.stream()
                .filter(predicate)
                .count();
    }

    private boolean isMissingRequired(MetadataReviewRow record) {
        return "Missing Required Fields".equalsIgnoreCase(record.metadataStatus());
    }

    private boolean isExportBlocked(MetadataReviewRow record) {
        return isMissingRequired(record)
                || "Invalid".equalsIgnoreCase(record.metadataStatus())
                || "Incomplete".equalsIgnoreCase(record.metadataStatus());
    }

    private boolean isFailedValidation(MetadataReviewRow record) {
        return "Invalid".equalsIgnoreCase(record.metadataStatus());
    }

    private boolean isReadyForQa(MetadataReviewRow record) {
        return "Complete".equalsIgnoreCase(record.metadataStatus())
                && "Ready for QA".equalsIgnoreCase(record.qaStatus());
    }

    private boolean isQaRejected(MetadataReviewRow record) {
        return "QA Rejected".equalsIgnoreCase(record.qaStatus());
    }

    private boolean isRecentlyScanned(MetadataReviewRow record) {
        return "Today".equalsIgnoreCase(record.dateGroup());
    }

    private GridPane buildTableRow(MetadataReviewRow record) {
        GridPane row = new GridPane();
        row.getStyleClass().add("metadata-review-table-row");

        if (record.warning()) {
            row.getStyleClass().add("metadata-review-table-row-warning");
        }

        row.getColumnConstraints().setAll(createTableColumns());

        CheckBox selectCheckBox = new CheckBox();
        selectCheckBox.getStyleClass().add("metadata-review-checkbox");
        selectCheckBox.setSelected(selectedRecordIds.contains(record.id()));
        selectCheckBox.selectedProperty().addListener((observable, oldValue, selected) ->
                updateSelection(record.id(), selected)
        );

        addCell(row, selectCheckBox, 0, HPos.CENTER);
        addCell(row, createWrappedLabel(record.identity(), "metadata-review-main-cell"), 1, HPos.LEFT);
        addCell(row, createWrappedLabel(record.client(), "metadata-review-cell-text"), 2, HPos.LEFT);
        addCell(row, createWrappedLabel(record.profile(), "metadata-review-cell-text"), 3, HPos.LEFT);
        addCell(row, createWrappedLabel(record.metadataTemplate(), "metadata-review-cell-text"), 4, HPos.LEFT);
        addCell(row, createStatusBadge(record.metadataStatus(), "metadata"), 5, HPos.LEFT);
        addCell(row, createStatusBadge(record.qaStatus(), "qa"), 6, HPos.LEFT);
        addCell(row, createWrappedLabel(String.valueOf(record.pages()), "metadata-review-cell-text"), 7, HPos.CENTER);
        addCell(row, createWrappedLabel(record.lastUpdated(), "metadata-review-cell-text"), 8, HPos.LEFT);
        addCell(row, createWrappedLabel(record.assignedTo(), "metadata-review-cell-text"), 9, HPos.LEFT);
        addCell(row, createReviewButton(record), 10, HPos.LEFT);

        return row;
    }

    private List<ColumnConstraints> createTableColumns() {
        return List.of(
                createPercentColumn(4),
                createPercentColumn(17),
                createPercentColumn(12),
                createPercentColumn(10),
                createPercentColumn(12),
                createPercentColumn(11),
                createPercentColumn(10),
                createPercentColumn(4),
                createPercentColumn(8),
                createPercentColumn(7),
                createPercentColumn(5)
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

    private Label createStatusBadge(String status, String type) {
        Label badge = new Label(status);
        badge.getStyleClass().add("metadata-review-status-badge");

        if ("metadata".equals(type)) {
            badge.getStyleClass().add(metadataStatusClass(status));
        } else {
            badge.getStyleClass().add(qaStatusClass(status));
        }

        badge.setWrapText(true);
        badge.setMinWidth(0);
        badge.setMaxWidth(Double.MAX_VALUE);

        return badge;
    }

    private Button createReviewButton(MetadataReviewRow record) {
        Button button = new Button("Review");
        button.getStyleClass().add("metadata-review-action-button");
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

        adminManager.addAuditLog("Exports", "Exported metadata review records", "Metadata Review", "Success",
                selectedCount + " metadata review records were exported.");

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

    private void updateSelectedRecords(UnaryOperator<MetadataReviewRow> updater) {
        if (selectedRecordIds.isEmpty()) {
            return;
        }

        for (int index = 0; index < records.size(); index++) {
            MetadataReviewRow record = records.get(index);

            if (selectedRecordIds.contains(record.id())) {
                MetadataReviewRow updatedRecord = updater.apply(record);
                adminManager.saveMetadataReviewRecord(toMetadataReviewRecord(updatedRecord));
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

    private boolean matchesSearch(MetadataReviewRow record) {
        String searchText = normalize(searchField.getText());

        if (searchText.isBlank()) {
            return true;
        }

        return normalize(record.identity()).contains(searchText)
                || normalize(record.client()).contains(searchText)
                || normalize(record.profile()).contains(searchText)
                || normalize(record.metadataTemplate()).contains(searchText)
                || normalize(record.metadataStatus()).contains(searchText)
                || normalize(record.qaStatus()).contains(searchText)
                || normalize(record.assignedTo()).contains(searchText)
                || normalize(record.scannedBy()).contains(searchText);
    }

    private boolean matchesFilters(MetadataReviewRow record) {
        return matchesCombo(record.client(), clientFilterComboBox.getValue(), ALL_CLIENTS)
                && matchesCombo(record.archive(), archiveFilterComboBox.getValue(), ALL_ARCHIVES)
                && matchesCombo(record.profile(), profileFilterComboBox.getValue(), ALL_PROFILES)
                && matchesCombo(record.metadataStatus(), metadataStatusFilterComboBox.getValue(), ALL_METADATA_STATUSES)
                && matchesCombo(record.qaStatus(), qaStatusFilterComboBox.getValue(), ALL_QA_STATUSES)
                && matchesCombo(record.scannedBy(), scannedByFilterComboBox.getValue(), ALL_USERS)
                && matchesDateRange(record.dateGroup(), dateRangeFilterComboBox.getValue());
    }

    private boolean matchesCombo(String value, String selectedValue, String allValue) {
        return selectedValue == null
                || allValue.equals(selectedValue)
                || value.equalsIgnoreCase(selectedValue);
    }

    private boolean matchesDateRange(String recordDateGroup, String selectedDateRange) {
        if (selectedDateRange == null
                || "Last 30 Days".equals(selectedDateRange)
                || "This Month".equals(selectedDateRange)
                || "All Time".equals(selectedDateRange)) {
            return true;
        }

        if ("Last 7 Days".equals(selectedDateRange)) {
            return "Today".equalsIgnoreCase(recordDateGroup)
                    || "Last 7 Days".equalsIgnoreCase(recordDateGroup);
        }

        return recordDateGroup.equalsIgnoreCase(selectedDateRange);
    }

    private String metadataStatusClass(String status) {
        return switch (normalize(status)) {
            case "not started" -> "metadata-review-status-neutral";
            case "incomplete" -> "metadata-review-status-warning";
            case "missing required fields", "invalid" -> "metadata-review-status-danger";
            case "complete", "approved" -> "metadata-review-status-success";
            default -> "metadata-review-status-neutral";
        };
    }

    private String qaStatusClass(String status) {
        return switch (normalize(status)) {
            case "not started" -> "metadata-review-status-neutral";
            case "waiting for qa", "qa in progress", "ready for qa" -> "metadata-review-status-info";
            case "qa approved" -> "metadata-review-status-success";
            case "qa rejected" -> "metadata-review-status-danger";
            default -> "metadata-review-status-neutral";
        };
    }

    @FXML
    private void clearFilters() {
        searchField.clear();

        clientFilterComboBox.setValue(ALL_CLIENTS);
        archiveFilterComboBox.setValue(ALL_ARCHIVES);
        profileFilterComboBox.setValue(ALL_PROFILES);
        metadataStatusFilterComboBox.setValue(ALL_METADATA_STATUSES);
        qaStatusFilterComboBox.setValue(ALL_QA_STATUSES);
        dateRangeFilterComboBox.setValue("Last 30 Days");
        scannedByFilterComboBox.setValue(ALL_USERS);

        selectedRecordIds.clear();
        currentPage = 1;
        renderRows();
    }

    @FXML
    private void showMissingRequiredQueue() {
        metadataStatusFilterComboBox.setValue("Missing Required Fields");
        qaStatusFilterComboBox.setValue(ALL_QA_STATUSES);
        dateRangeFilterComboBox.setValue("Last 30 Days");
    }

    @FXML
    private void showExportBlockedQueue() {
        metadataStatusFilterComboBox.setValue("Missing Required Fields");
        qaStatusFilterComboBox.setValue("Waiting for QA");
        dateRangeFilterComboBox.setValue("Last 30 Days");
    }

    @FXML
    private void showFailedValidationQueue() {
        metadataStatusFilterComboBox.setValue("Invalid");
        qaStatusFilterComboBox.setValue(ALL_QA_STATUSES);
        dateRangeFilterComboBox.setValue("Last 30 Days");
    }

    @FXML
    private void showReadyForQaQueue() {
        metadataStatusFilterComboBox.setValue("Complete");
        qaStatusFilterComboBox.setValue("Ready for QA");
        dateRangeFilterComboBox.setValue("Last 30 Days");
    }

    @FXML
    private void showQaRejectedQueue() {
        metadataStatusFilterComboBox.setValue(ALL_METADATA_STATUSES);
        qaStatusFilterComboBox.setValue("QA Rejected");
        dateRangeFilterComboBox.setValue("Last 30 Days");
    }

    @FXML
    private void showRecentlyScannedQueue() {
        metadataStatusFilterComboBox.setValue(ALL_METADATA_STATUSES);
        qaStatusFilterComboBox.setValue(ALL_QA_STATUSES);
        dateRangeFilterComboBox.setValue("Today");
    }

    @FXML
    private void exportReport() {
        int exportCount = filteredRecords.isEmpty() ? records.size() : filteredRecords.size();
        adminManager.addAuditLog("Exports", "Exported metadata review report", "Metadata Review", "Success",
                "A metadata review report was exported.");
        paginationSummaryLabel.setText("Exported report for " + exportCount + " records");
    }

    private void openReviewWorkspace(MetadataReviewRow record) {
        activeReviewRecord = record;

        workspaceTitleLabel.setText(record.identity());
        workspaceSubtitleLabel.setText(
                record.profile()
                        + " · "
                        + record.client()
                        + " · "
                        + record.pages()
                        + " pages · "
                        + record.metadataStatus()
        );

        boolean isBlocked = record.metadataStatus().equalsIgnoreCase("Missing Required Fields")
                || record.metadataStatus().equalsIgnoreCase("Invalid")
                || record.metadataStatus().equalsIgnoreCase("Incomplete");

        workspaceWarningLabel.setText(
                isBlocked
                        ? "Export blocked: 2 required metadata fields are missing."
                        : "All required metadata is complete. Ready for QA/export."
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
    private void saveMetadata() {
        if (activeReviewRecord == null) {
            return;
        }

        replaceActiveRecord(activeReviewRecord.withLastUpdated("Saved just now"));
        workspaceWarningLabel.setText("Metadata saved just now.");
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

        replaceActiveRecord(activeReviewRecord.withReviewState(activeReviewRecord.metadataStatus(), "QA Rejected", true));
    }

    private void replaceActiveRecord(MetadataReviewRow updatedRecord) {
        adminManager.saveMetadataReviewRecord(toMetadataReviewRecord(updatedRecord));

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
            tabButton.getStyleClass().removeAll("metadata-workspace-tab-active", "metadata-workspace-tab-button");

            if (tabButton == activeButton) {
                tabButton.getStyleClass().add("metadata-workspace-tab-active");
            } else {
                tabButton.getStyleClass().add("metadata-workspace-tab-button");
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
                adminManager.getMetadataReviewRecords().stream()
                        .map(this::toMetadataReviewRow)
                        .toList()
        );
        refreshFilterOptions();
    }

    private MetadataReviewRow toMetadataReviewRow(MetadataReviewRecord record) {
        return new MetadataReviewRow(
                record.getId(),
                record.getIdentity(),
                record.getClient(),
                record.getArchive(),
                record.getProfile(),
                record.getMetadataTemplate(),
                record.getMetadataStatus(),
                record.getQaStatus(),
                record.getPages(),
                record.getLastUpdated(),
                record.getAssignedTo(),
                record.getScannedBy(),
                record.getDateGroup(),
                record.hasWarning()
        );
    }

    private MetadataReviewRecord toMetadataReviewRecord(MetadataReviewRow row) {
        return new MetadataReviewRecord(
                row.id(),
                row.identity(),
                row.client(),
                row.archive(),
                row.profile(),
                row.metadataTemplate(),
                row.metadataStatus(),
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
        setComboOptions(clientFilterComboBox, ALL_CLIENTS, records.stream().map(MetadataReviewRow::client).toList());
        setComboOptions(archiveFilterComboBox, ALL_ARCHIVES, records.stream().map(MetadataReviewRow::archive).toList());
        setComboOptions(profileFilterComboBox, ALL_PROFILES, records.stream().map(MetadataReviewRow::profile).toList());
        setComboOptions(scannedByFilterComboBox, ALL_USERS, records.stream().map(MetadataReviewRow::scannedBy).toList());
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

    record MetadataReviewRow(
            String id,
            String identity,
            String client,
            String archive,
            String profile,
            String metadataTemplate,
            String metadataStatus,
            String qaStatus,
            int pages,
            String lastUpdated,
            String assignedTo,
            String scannedBy,
            String dateGroup,
            boolean warning
    ) {
        private MetadataReviewRow withLastUpdated(String updatedAt) {
            return new MetadataReviewRow(
                    id,
                    identity,
                    client,
                    archive,
                    profile,
                    metadataTemplate,
                    metadataStatus,
                    qaStatus,
                    pages,
                    updatedAt,
                    assignedTo,
                    scannedBy,
                    dateGroup,
                    warning
            );
        }

        private MetadataReviewRow withAssignedTo(String newAssignedTo) {
            return new MetadataReviewRow(
                    id,
                    identity,
                    client,
                    archive,
                    profile,
                    metadataTemplate,
                    metadataStatus,
                    qaStatus,
                    pages,
                    "Updated just now",
                    newAssignedTo,
                    scannedBy,
                    dateGroup,
                    warning
            );
        }

        private MetadataReviewRow withReviewState(String newMetadataStatus, String newQaStatus, boolean hasWarning) {
            return new MetadataReviewRow(
                    id,
                    identity,
                    client,
                    archive,
                    profile,
                    metadataTemplate,
                    newMetadataStatus,
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
