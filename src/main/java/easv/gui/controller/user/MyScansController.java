package easv.gui.controller.user;

import easv.gui.UserPortalModel;
import easv.gui.controller.utilities.PaginationHelper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class MyScansController {
    private static final String ALL_STATUSES = "All Statuses";
    private static final DateTimeFormatter ITEM_DATE_TIME = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
    private static final DateTimeFormatter LEGACY_ITEM_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserPortalModel portalModel;
    private final UserNavigator navigator;
    private final VBox table = new VBox();
    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final DatePicker fromDateField = new DatePicker();
    private final DatePicker toDateField = new DatePicker();
    private final Label paginationSummaryLabel = new Label();
    private final HBox paginationButtonsBox = new HBox();
    private final ComboBox<Integer> rowsPerPageFilter = new ComboBox<>();
    private int currentPage = 1;

    public MyScansController(UserPortalModel portalModel, UserNavigator navigator) {
        this.portalModel = portalModel;
        this.navigator = navigator == null ? UserNavigator.none() : navigator;
    }

    public Node create() {
        VBox page = new VBox(24);
        page.getStyleClass().addAll("portal-page", "my-scans-page", "exports-page");

        Label title = new Label("My Scans");
        title.getStyleClass().add("exports-title");

        Label subtitle = new Label("Review scan history and reopen sessions that still need attention.");
        subtitle.getStyleClass().add("exports-subtitle");

        configureFilters();

        VBox tablePanel = new VBox();
        tablePanel.getStyleClass().add("exports-table-panel");
        table.getStyleClass().add("exports-table");
        tablePanel.getChildren().addAll(table, buildPaginationBar());

        refreshTable();

        page.getChildren().addAll(
                new VBox(6, title, subtitle),
                buildFilterPanel(),
                tablePanel
        );
        return page;
    }

    private void configureFilters() {
        searchField.setPromptText("Search by box, profile, or status");
        searchField.getStyleClass().add("exports-filter-input");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshTable());

        statusFilter.getItems().setAll(ALL_STATUSES, "Completed", "Processing", "Failed");
        statusFilter.setValue(ALL_STATUSES);
        statusFilter.getStyleClass().add("exports-status-filter");
        statusFilter.setMinWidth(0);
        statusFilter.setPrefWidth(240);
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> refreshTable());

        UserPortalUi.configureDateFilterPicker(fromDateField);
        fromDateField.valueProperty().addListener((observable, oldValue, newValue) -> refreshTable());

        UserPortalUi.configureDateFilterPicker(toDateField);
        toDateField.valueProperty().addListener((observable, oldValue, newValue) -> refreshTable());

        rowsPerPageFilter.getItems().setAll(10, 25, 50);
        rowsPerPageFilter.setValue(10);
        rowsPerPageFilter.getStyleClass().add("exports-status-filter");
        rowsPerPageFilter.getStyleClass().add("pagination-rows-filter");
        rowsPerPageFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentPage = 1;
            refreshTable();
        });
    }

    private HBox buildFilterPanel() {
        HBox filterRow = new HBox(18,
                buildFilter("Search", searchField),
                buildFilter("Status", statusFilter),
                buildFilter("From Date", fromDateField),
                buildFilter("To Date", toDateField)
        );
        filterRow.getStyleClass().add("exports-filter-panel");
        filterRow.setAlignment(Pos.CENTER_LEFT);
        return filterRow;
    }

    private VBox buildFilter(String labelText, Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("exports-filter-label");

        VBox wrap = new VBox(6, label, control);
        wrap.getStyleClass().add("exports-filter-wrap");
        HBox.setHgrow(wrap, Priority.ALWAYS);

        if (control instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }

        return wrap;
    }

    private void refreshTable() {
        table.getChildren().setAll(createHeaderRow("BOX ID", "PROFILE", "PAGES", "SIZE", "DATE", "STATUS", "ACTION"));

        List<UserPortalModel.HistoryItem> visibleItems = portalModel.fetchScanHistory().stream()
                .filter(this::matchesFilters)
                .toList();

        int rowsPerPage = rowsPerPageFilter.getValue() == null ? 10 : rowsPerPageFilter.getValue();
        int totalItems = visibleItems.size();
        PaginationHelper.PageSlice slice = PaginationHelper.slice(currentPage, rowsPerPage, totalItems);
        currentPage = slice.currentPage();

        if (visibleItems.isEmpty()) {
            table.getChildren().add(emptyRow("No matching scans"));
        } else {
            for (UserPortalModel.HistoryItem item : visibleItems.subList(slice.fromIndex(), slice.toIndex())) {
                table.getChildren().add(createDataRow(item));
            }
        }

        PaginationHelper.renderInto(paginationButtonsBox, paginationSummaryLabel, slice,
                totalItems, "scans", page -> {
                    currentPage = page;
                    refreshTable();
                });
    }

    private boolean matchesFilters(UserPortalModel.HistoryItem item) {
        String selectedStatus = statusFilter.getValue();
        boolean statusMatches = selectedStatus == null
                || ALL_STATUSES.equals(selectedStatus)
                || selectedStatus.equalsIgnoreCase(item.status());

        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        boolean searchMatches = query.isBlank()
                || item.boxId().toLowerCase().contains(query)
                || item.profileName().toLowerCase().contains(query)
                || item.status().toLowerCase().contains(query);

        LocalDate itemDate = parseItemDate(item.startedAt());
        LocalDate fromDate = fromDateField.getValue();
        LocalDate toDate = toDateField.getValue();

        boolean fromMatches = fromDate == null || (itemDate != null && !itemDate.isBefore(fromDate));
        boolean toMatches = toDate == null || (itemDate != null && !itemDate.isAfter(toDate));

        return statusMatches && searchMatches && fromMatches && toMatches;
    }

    private GridPane createHeaderRow(String... values) {
        GridPane row = createRowSkeleton();
        row.getStyleClass().add("exports-table-header-row");

        for (int index = 0; index < values.length; index++) {
            row.add(headerCell(values[index]), index, 0);
        }

        return row;
    }

    private GridPane createDataRow(UserPortalModel.HistoryItem item) {
        GridPane row = createRowSkeleton();
        row.getStyleClass().add("exports-table-row");

        Button actionButton = new Button(item.status().equalsIgnoreCase("Processing") ? "Resume" : "Reuse");
        actionButton.getStyleClass().addAll("portal-row-button", "my-scans-action-button");
        actionButton.setOnAction(event -> navigator.resumeHistoryScan(item));

        row.add(primaryCell(item.boxId()), 0, 0);
        row.add(dataCell(item.profileName()), 1, 0);
        row.add(dataCell(String.valueOf(item.pages())), 2, 0);
        row.add(dataCell(item.size()), 3, 0);
        row.add(dataCell(item.startedAt()), 4, 0);
        row.add(statusCell(item.status()), 5, 0);
        row.add(actionButton, 6, 0);

        return row;
    }

    private HBox statusCell(String status) {
        HBox wrap = new HBox(UserPortalUi.buildStatusChip(status));
        wrap.getStyleClass().add("exports-table-cell-wrap");
        return wrap;
    }

    private Label headerCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("exports-table-header");
        return label;
    }

    private Label dataCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("exports-table-cell");
        return label;
    }

    private Label primaryCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("exports-table-cell-primary");
        return label;
    }

    private HBox emptyRow(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("exports-footer-text");

        HBox row = new HBox(label);
        row.getStyleClass().add("exports-empty-row");
        return row;
    }

    private GridPane createRowSkeleton() {
        GridPane row = new GridPane();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setHgap(12);
        row.getColumnConstraints().setAll(
                percentColumn(16),
                percentColumn(18),
                percentColumn(10),
                percentColumn(10),
                percentColumn(20),
                percentColumn(12),
                percentColumn(14)
        );
        return row;
    }

    private HBox buildPaginationBar() {
        paginationSummaryLabel.getStyleClass().add("pagination-summary-label");
        paginationButtonsBox.getStyleClass().add("pagination-buttons-box");

        Label rowsPerPageLabel = new Label("Results per page");
        rowsPerPageLabel.getStyleClass().add("rows-per-page-label");

        HBox rowsPerPageBox = new HBox(9, rowsPerPageLabel, rowsPerPageFilter);
        rowsPerPageBox.getStyleClass().add("rows-per-page-box");
        HBox.setHgrow(rowsPerPageBox, Priority.ALWAYS);

        HBox summaryBox = new HBox(paginationSummaryLabel);
        summaryBox.getStyleClass().add("pagination-summary-box");
        HBox.setHgrow(summaryBox, Priority.ALWAYS);

        HBox centerBox = new HBox(paginationButtonsBox);
        centerBox.getStyleClass().add("pagination-center-box");
        HBox.setHgrow(centerBox, Priority.ALWAYS);

        HBox bar = new HBox(18, summaryBox, centerBox, rowsPerPageBox);
        bar.getStyleClass().add("pagination-bar");
        return bar;
    }

    private ColumnConstraints percentColumn(double width) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setPercentWidth(width);
        constraints.setHgrow(Priority.ALWAYS);
        return constraints;
    }

    private LocalDate parseItemDate(String value) {
        if (value == null || value.isBlank() || "-".equals(value.trim())) {
            return null;
        }

        for (DateTimeFormatter formatter : List.of(ITEM_DATE_TIME, LEGACY_ITEM_DATE_TIME)) {
            try {
                return LocalDateTime.parse(value.trim(), formatter).toLocalDate();
            } catch (DateTimeParseException ignored) {
            }
        }

        return null;
    }
}
