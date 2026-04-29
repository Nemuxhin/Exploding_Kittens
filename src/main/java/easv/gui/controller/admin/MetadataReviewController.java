package easv.gui.controller.admin;

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
        loadSampleRecords();
        configureListeners();

        showOverview();
        setActiveWorkspaceTab(documentTabButton);
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
        int totalPages = calculateTotalPages(totalRecords);

        currentPage = clamp(currentPage, 1, totalPages);

        int fromIndex = Math.min((currentPage - 1) * rowsPerPage, totalRecords);
        int toIndex = Math.min(fromIndex + rowsPerPage, totalRecords);

        pageRecords = filteredRecords.subList(fromIndex, toIndex);

        resultsRowsContainer.getChildren().setAll(
                pageRecords.stream()
                        .map(this::buildTableRow)
                        .toList()
        );

        updateEmptyState(totalRecords);
        renderPagination(totalPages, totalRecords, fromIndex, toIndex);
        updateBatchBar();
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

    private void updateBatchBar() {
        int selectedCount = selectedRecordIds.size();
        boolean hasSelection = selectedCount > 0;

        batchActionBar.setVisible(hasSelection);
        batchActionBar.setManaged(hasSelection);

        batchSelectionLabel.setText(selectedCount == 1 ? "1 selected" : selectedCount + " selected");
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

    private void renderPagination(int totalPages, int totalRecords, int fromIndex, int toIndex) {
        paginationButtonsBox.getChildren().clear();

        if (totalRecords == 0) {
            paginationSummaryLabel.setText("Showing 0 records");
            return;
        }

        paginationSummaryLabel.setText(
                "Showing " + (fromIndex + 1) + "-" + toIndex + " of " + totalRecords + " records"
        );

        paginationButtonsBox.getChildren().add(createPaginationButton("<<", 1, currentPage == 1));
        paginationButtonsBox.getChildren().add(createPaginationButton("<", currentPage - 1, currentPage == 1));

        for (String pageItem : buildPageItems(totalPages)) {
            Node paginationItem = "...".equals(pageItem)
                    ? createPaginationEllipsis()
                    : createPaginationButton(pageItem, Integer.parseInt(pageItem), false);

            paginationButtonsBox.getChildren().add(paginationItem);
        }

        paginationButtonsBox.getChildren().add(createPaginationButton(">", currentPage + 1, currentPage == totalPages));
        paginationButtonsBox.getChildren().add(createPaginationButton(">>", totalPages, currentPage == totalPages));
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

    private List<String> buildPageItems(int totalPages) {
        List<String> items = new ArrayList<>();

        if (totalPages <= 5) {
            for (int page = 1; page <= totalPages; page++) {
                items.add(String.valueOf(page));
            }
            return items;
        }

        if (currentPage <= 3) {
            items.add("1");
            items.add("2");
            items.add("3");
            items.add("...");
            items.add(String.valueOf(totalPages));
            return items;
        }

        if (currentPage >= totalPages - 2) {
            items.add("1");
            items.add("...");
            items.add(String.valueOf(totalPages - 2));
            items.add(String.valueOf(totalPages - 1));
            items.add(String.valueOf(totalPages));
            return items;
        }

        items.add("1");
        items.add("...");
        items.add(String.valueOf(currentPage - 1));
        items.add(String.valueOf(currentPage));
        items.add(String.valueOf(currentPage + 1));
        items.add("...");
        items.add(String.valueOf(totalPages));

        return items;
    }

    private int calculateTotalPages(int totalRecords) {
        if (totalRecords == 0) {
            return 1;
        }

        return (int) Math.ceil((double) totalRecords / rowsPerPage);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
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
        System.out.println("Export Report clicked");
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
        System.out.println("Save Metadata clicked");
    }

    @FXML
    private void markComplete() {
        System.out.println("Mark Document Complete clicked");
    }

    @FXML
    private void sendToQa() {
        System.out.println("Send to QA clicked");
    }

    @FXML
    private void approveReview() {
        System.out.println("Approve Review clicked");
    }

    @FXML
    private void rejectReview() {
        System.out.println("Reject Review clicked");
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

    private void loadSampleRecords() {
        records.setAll(
                new MetadataReviewRow("record-1", "BOX-2026-004 / Case 2026-042 / Document 3", "Aalborg Municipality", "Building Archive", "Building Archive", "Building Archive Metadata", "Missing Required Fields", "Waiting for QA", 12, "Today 10:42", "Sarah Smith", "Sarah Smith", "Today", true),
                new MetadataReviewRow("record-2", "BOX-2026-011 / Case 2026-088 / Document 1", "Maersk Archive", "Technical Archive", "Technical Drawings", "Technical Drawings Metadata", "Invalid", "QA Rejected", 4, "Yesterday 15:20", "John Doe", "John Doe", "Last 7 Days", true),
                new MetadataReviewRow("record-3", "BOX-2026-018 / Case 2026-104 / Document 8", "Copenhagen Airport", "Airport Archive", "Standard Scan", "Standard Box Registration", "Complete", "Ready for QA", 7, "Today 08:15", "Unassigned", "System Import", "Today", false),
                new MetadataReviewRow("record-4", "BOX-2026-022 / Case 2026-120 / Document 2", "Aalborg Municipality", "Building Archive", "Building Archive", "Building Archive Metadata", "Not Started", "Not Started", 16, "Today 11:03", "Sofia Nielsen", "Sofia Nielsen", "Today", false),
                new MetadataReviewRow("record-5", "BOX-2026-025 / Case 2026-131 / Document 5", "Maersk Archive", "Technical Archive", "Technical Drawings", "Technical Drawings Metadata", "Incomplete", "QA In Progress", 9, "Today 12:12", "John Doe", "John Doe", "Today", true),
                new MetadataReviewRow("record-6", "BOX-2026-029 / Case 2026-144 / Document 2", "Copenhagen Airport", "Airport Archive", "Standard Scan", "Standard Box Registration", "Approved", "QA Approved", 6, "Yesterday 09:40", "Sarah Smith", "System Import", "Last 7 Days", false),
                new MetadataReviewRow("record-7", "BOX-2026-033 / Case 2026-155 / Document 9", "Aalborg Municipality", "Building Archive", "Building Archive", "Building Archive Metadata", "Complete", "Ready for QA", 11, "Today 13:05", "Sofia Nielsen", "Sofia Nielsen", "Today", false),
                new MetadataReviewRow("record-8", "BOX-2026-041 / Case 2026-170 / Document 4", "Maersk Archive", "Technical Archive", "Technical Drawings", "Technical Drawings Metadata", "Missing Required Fields", "Waiting for QA", 14, "Today 14:22", "Sarah Smith", "Sarah Smith", "Today", true),
                new MetadataReviewRow("record-9", "BOX-2026-047 / Case 2026-184 / Document 6", "Copenhagen Airport", "Airport Archive", "Standard Scan", "Standard Box Registration", "Not Started", "Not Started", 3, "Yesterday 11:10", "Unassigned", "System Import", "Last 7 Days", false),
                new MetadataReviewRow("record-10", "BOX-2026-052 / Case 2026-199 / Document 1", "Aalborg Municipality", "Building Archive", "Building Archive", "Building Archive Metadata", "Invalid", "QA Rejected", 8, "Today 15:37", "John Doe", "John Doe", "Today", true),
                new MetadataReviewRow("record-11", "BOX-2026-058 / Case 2026-208 / Document 7", "Maersk Archive", "Technical Archive", "Technical Drawings", "Technical Drawings Metadata", "Complete", "QA In Progress", 10, "Yesterday 16:02", "Sarah Smith", "Sarah Smith", "Last 7 Days", false),
                new MetadataReviewRow("record-12", "BOX-2026-063 / Case 2026-216 / Document 3", "Copenhagen Airport", "Airport Archive", "Standard Scan", "Standard Box Registration", "Approved", "QA Approved", 5, "Today 16:55", "Sofia Nielsen", "System Import", "Today", false)
        );
    }

    private record MetadataReviewRow(
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
    }
}