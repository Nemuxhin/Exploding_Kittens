package easv.gui.controller.user;

import easv.gui.UserPortalModel;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class MyScansController {
    private static final String ALL_STATUSES = "All Statuses";

    private final UserPortalModel portalModel;
    private final UserNavigator navigator;
    private final VBox table = new VBox();
    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();

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
        tablePanel.getChildren().add(table);

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
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> refreshTable());
    }

    private HBox buildFilterPanel() {
        HBox filterRow = new HBox(18,
                buildFilter("Search", searchField),
                buildFilter("Status", statusFilter)
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
        table.getChildren().setAll(createHeaderRow("BOX ID", "PROFILE", "STATUS", "STARTED", "COMPLETED", "PAGES", "ACTION"));

        List<UserPortalModel.HistoryItem> visibleItems = portalModel.fetchScanHistory().stream()
                .filter(this::matchesFilters)
                .toList();

        if (visibleItems.isEmpty()) {
            table.getChildren().add(emptyRow("No matching scans"));
            return;
        }

        for (UserPortalModel.HistoryItem item : visibleItems) {
            table.getChildren().add(createDataRow(item));
        }
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

        return statusMatches && searchMatches;
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

        Button boxButton = new Button(item.boxId());
        boxButton.getStyleClass().add("portal-table-link");
        boxButton.setOnAction(event -> navigator.resumeHistoryScan(item));

        Button actionButton = new Button(item.status().equalsIgnoreCase("Processing") ? "Resume" : "Reuse");
        actionButton.getStyleClass().add("portal-row-button");
        actionButton.setOnAction(event -> navigator.resumeHistoryScan(item));

        row.add(boxButton, 0, 0);
        row.add(dataCell(item.profileName()), 1, 0);
        row.add(statusCell(item.status()), 2, 0);
        row.add(dataCell(item.startedAt()), 3, 0);
        row.add(dataCell(item.completedAt()), 4, 0);
        row.add(dataCell(String.valueOf(item.pages())), 5, 0);
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
        row.getColumnConstraints().setAll(
                percentColumn(16),
                percentColumn(18),
                percentColumn(14),
                percentColumn(18),
                percentColumn(18),
                percentColumn(8),
                percentColumn(8)
        );
        return row;
    }

    private ColumnConstraints percentColumn(double width) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setPercentWidth(width);
        constraints.setHgrow(Priority.ALWAYS);
        return constraints;
    }
}
