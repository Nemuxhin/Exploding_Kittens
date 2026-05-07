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
import java.util.Locale;

public class ExportsController {
    private static final String ALL_STATUSES = "All Statuses";

    private final UserPortalModel portalModel;
    private final VBox table = new VBox();
    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();

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
        tablePanel.getChildren().add(table);

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

        statusFilter.getItems().setAll(ALL_STATUSES, "Ready", "Processing", "Failed");
        statusFilter.setValue(ALL_STATUSES);
        statusFilter.getStyleClass().add("exports-status-filter");
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> refreshTable());
    }

    private HBox buildSummaryRow() {
        HBox row = new HBox(18,
                metricCard("Total Files", String.valueOf(portalModel.fetchExports().size())),
                metricCard("Ready to Download", String.valueOf(countReadyExports())),
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
        table.getChildren().setAll(createHeaderRow("FILE NAME", "BOX ID", "PROFILE", "CREATED", "SIZE", "STATUS", "ACTION"));

        List<UserPortalModel.ExportItem> visibleItems = portalModel.fetchExports().stream()
                .filter(this::matchesFilters)
                .toList();

        if (visibleItems.isEmpty()) {
            table.getChildren().add(emptyRow("No matching exports"));
            return;
        }

        for (UserPortalModel.ExportItem item : visibleItems) {
            table.getChildren().add(createDataRow(item));
        }
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

    private GridPane createDataRow(UserPortalModel.ExportItem item) {
        GridPane row = createRowSkeleton();
        row.getStyleClass().add("exports-table-row");

        Button action = new Button("Download");
        action.getStyleClass().add("portal-row-button");
        action.setGraphic(UserPortalUi.buildIcon("download", "portal-button-icon"));
        action.setDisable(!"Ready".equalsIgnoreCase(item.status()));

        row.add(primaryCell(item.fileName()), 0, 0);
        row.add(dataCell(item.boxId()), 1, 0);
        row.add(dataCell(item.profileName()), 2, 0);
        row.add(dataCell(item.createdAt()), 3, 0);
        row.add(dataCell(item.size()), 4, 0);
        row.add(statusCell(item.status()), 5, 0);
        row.add(action, 6, 0);

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
                .filter(item -> "Ready".equalsIgnoreCase(item.status()))
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
                percentColumn(24),
                percentColumn(12),
                percentColumn(15),
                percentColumn(18),
                percentColumn(9),
                percentColumn(12),
                percentColumn(10)
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
