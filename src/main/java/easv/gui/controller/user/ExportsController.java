package easv.gui.controller.user;

import easv.gui.UserPortalModel;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExportsController {
    private static final String ALL_STATUSES = "All Statuses";
    private static final DateTimeFormatter ITEM_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FILTER_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UserPortalModel portalModel;
    private final VBox table = new VBox();
    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final DatePicker fromDateField = new DatePicker();
    private final DatePicker toDateField = new DatePicker();
    private final Label paginationSummaryLabel = new Label();
    private final HBox paginationButtonsBox = new HBox();
    private final ComboBox<Integer> rowsPerPageFilter = new ComboBox<>();
    private int currentPage = 1;

    public ExportsController(UserPortalModel portalModel) {
        this.portalModel = portalModel;
    }

    public Node create() {
        VBox page = new VBox(24);
        page.getStyleClass().addAll("portal-page", "exports-page");

        Label title = new Label("Exports");
        title.getStyleClass().add("exports-title");

        Label subtitle = new Label("Download completed exports and track files that are still being generated.");
        subtitle.getStyleClass().add("exports-subtitle");

        configureFilters();

        VBox tablePanel = new VBox();
        tablePanel.getStyleClass().add("exports-table-panel");
        table.getStyleClass().add("exports-table");
        tablePanel.getChildren().addAll(table, buildPaginationBar());

        refreshTable();

        page.getChildren().addAll(
                new VBox(6, title, subtitle),
                buildSummaryRow(),
                buildFilterPanel(),
                tablePanel
        );
        return page;
    }

    private void configureFilters() {
        searchField.setPromptText("Search by file, box, or profile");
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

    private HBox buildSummaryRow() {
        HBox row = new HBox(18,
                metricCard("Total Files", String.valueOf(portalModel.fetchExports().size())),
                metricCard("Completed Exports", String.valueOf(countReadyExports())),
                metricCard("Total Size", totalExportSizeText())
        );
        row.getStyleClass().add("exports-summary-row");
        return row;
    }

    private VBox metricCard(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("exports-metric-label");

        Label value = new Label(valueText);
        value.getStyleClass().add("exports-metric-value");

        VBox card = new VBox(6, label, value);
        card.getStyleClass().add("exports-metric-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
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
        table.getChildren().setAll(createHeaderRow("FILE NAME", "BOX ID", "PROFILE", "CREATED", "SIZE", "STATUS", "ACTION"));

        List<UserPortalModel.ExportItem> visibleItems = portalModel.fetchExports().stream()
                .filter(this::matchesFilters)
                .toList();

        int rowsPerPage = rowsPerPageFilter.getValue() == null ? 10 : rowsPerPageFilter.getValue();
        int totalItems = visibleItems.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) rowsPerPage));
        currentPage = Math.max(1, Math.min(currentPage, totalPages));

        if (visibleItems.isEmpty()) {
            table.getChildren().add(emptyRow("No matching exports"));
            updatePagination(totalItems, totalPages);
            return;
        }

        int fromIndex = Math.min((currentPage - 1) * rowsPerPage, totalItems);
        int toIndex = Math.min(fromIndex + rowsPerPage, totalItems);

        for (UserPortalModel.ExportItem item : visibleItems.subList(fromIndex, toIndex)) {
            table.getChildren().add(createDataRow(item));
        }

        updatePagination(totalItems, totalPages);
    }

    private boolean matchesFilters(UserPortalModel.ExportItem item) {
        String selectedStatus = statusFilter.getValue();
        boolean statusMatches = selectedStatus == null
                || ALL_STATUSES.equals(selectedStatus)
                || selectedStatus.equalsIgnoreCase(item.status());

        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        boolean searchMatches = query.isBlank()
                || item.fileName().toLowerCase().contains(query)
                || item.boxId().toLowerCase().contains(query)
                || item.profileName().toLowerCase().contains(query)
                || item.status().toLowerCase().contains(query);

        LocalDate itemDate = parseItemDate(item.createdAt());
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

    private GridPane createDataRow(UserPortalModel.ExportItem item) {
        GridPane row = createRowSkeleton();
        row.getStyleClass().add("exports-table-row");

        Button action = new Button("Export");
        action.getStyleClass().addAll("portal-row-button", "exports-export-button");
        action.setGraphic(UserPortalUi.buildIcon("download", "portal-button-icon-inverse"));
        action.setDisable(!"Completed".equalsIgnoreCase(item.status()));
        action.setOnAction(event -> action.requestFocus());

        Button deleteButton = new Button();
        deleteButton.getStyleClass().add("exports-delete-button");
        deleteButton.setGraphic(UserPortalUi.buildIcon("trash", "exports-delete-icon"));
        deleteButton.setDisable(!"Completed".equalsIgnoreCase(item.status()));

        HBox actionBox = new HBox(9, action, deleteButton);
        actionBox.getStyleClass().add("exports-action-box");

        row.add(primaryCell(item.fileName()), 0, 0);
        row.add(dataCell(item.boxId()), 1, 0);
        row.add(dataCell(item.profileName()), 2, 0);
        row.add(dataCell(item.createdAt()), 3, 0);
        row.add(dataCell(item.size()), 4, 0);
        row.add(statusCell(item.status()), 5, 0);
        row.add(actionBox, 6, 0);

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

    private Label primaryCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("exports-table-cell-primary");
        return label;
    }

    private Label dataCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("exports-table-cell");
        return label;
    }

    private HBox emptyRow(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("exports-footer-text");

        HBox row = new HBox(label);
        row.getStyleClass().add("exports-empty-row");
        return row;
    }

    private int countReadyExports() {
        return (int) portalModel.fetchExports().stream()
                .filter(item -> "Completed".equalsIgnoreCase(item.status()))
                .count();
    }

    private String totalExportSizeText() {
        double totalMb = 0;

        for (UserPortalModel.ExportItem item : portalModel.fetchExports()) {
            String size = item.size();
            if (size == null || !size.endsWith("MB")) {
                continue;
            }

            try {
                totalMb += Double.parseDouble(size.replace("MB", "").trim());
            } catch (NumberFormatException ignored) {
                // Demo data can keep the UI running even if one display value is not numeric.
            }
        }

        return String.format(Locale.US, "%.1f MB", totalMb);
    }

    private GridPane createRowSkeleton() {
        GridPane row = new GridPane();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.getColumnConstraints().setAll(
                percentColumn(22),
                percentColumn(12),
                percentColumn(15),
                percentColumn(18),
                percentColumn(9),
                percentColumn(11),
                percentColumn(13)
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

    private void updatePagination(int totalItems, int totalPages) {
        int rowsPerPage = rowsPerPageFilter.getValue() == null ? 10 : rowsPerPageFilter.getValue();
        int start = totalItems == 0 ? 0 : ((currentPage - 1) * rowsPerPage) + 1;
        int end = totalItems == 0 ? 0 : Math.min(currentPage * rowsPerPage, totalItems);
        paginationSummaryLabel.setText("Showing " + start + "-" + end + " of " + totalItems + " exports");
        paginationButtonsBox.getChildren().setAll(buildPaginationButtons(totalPages));
    }

    private List<Node> buildPaginationButtons(int totalPages) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(paginationButton("«", 1, currentPage == 1, false));
        nodes.add(paginationButton("‹", currentPage - 1, currentPage == 1, false));

        for (int page : visiblePages(totalPages)) {
            if (page < 0) {
                Label ellipsis = new Label("...");
                ellipsis.getStyleClass().add("pagination-ellipsis");
                nodes.add(ellipsis);
            } else {
                nodes.add(paginationButton(String.valueOf(page), page, false, page == currentPage));
            }
        }

        nodes.add(paginationButton("›", currentPage + 1, currentPage == totalPages, false));
        nodes.add(paginationButton("»", totalPages, currentPage == totalPages, false));
        return nodes;
    }

    private List<Integer> visiblePages(int totalPages) {
        List<Integer> pages = new ArrayList<>();
        if (totalPages <= 5) {
            for (int page = 1; page <= totalPages; page++) {
                pages.add(page);
            }
            return pages;
        }

        pages.add(1);
        if (currentPage > 3) {
            pages.add(-1);
        }

        int start = Math.max(2, currentPage - 1);
        int end = Math.min(totalPages - 1, currentPage + 1);
        for (int page = start; page <= end; page++) {
            pages.add(page);
        }

        if (currentPage < totalPages - 2) {
            pages.add(-1);
        }
        pages.add(totalPages);
        return pages;
    }

    private Button paginationButton(String text, int targetPage, boolean disabled, boolean active) {
        Button button = new Button(text);
        button.getStyleClass().add("pagination-button");
        if (active) {
            button.getStyleClass().add("pagination-button-active");
        }
        button.setDisable(disabled);
        button.setOnAction(event -> {
            currentPage = targetPage;
            refreshTable();
        });
        return button;
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

        try {
            return LocalDateTime.parse(value.trim(), ITEM_DATE_TIME).toLocalDate();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
