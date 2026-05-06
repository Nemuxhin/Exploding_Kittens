package easv.gui.controller.user;

import easv.gui.UserPortalModel;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ScansController {
    private final UserPortalModel portalModel;
    private final PortalNavigator navigator;

    public ScansController(UserPortalModel portalModel, PortalNavigator navigator) {
        this.portalModel = portalModel;
        this.navigator = navigator == null ? PortalNavigator.none() : navigator;
    }

    public Node create() {
        VBox page = new VBox(28);
        page.getStyleClass().add("portal-page");

        Label title = new Label("Scans");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Review scan history and reopen sessions that still need attention.");
        subtitle.getStyleClass().add("page-subtitle");
        VBox intro = new VBox(10, title, subtitle);
        intro.getStyleClass().add("portal-page-intro");

        VBox card = new VBox();
        card.getStyleClass().addAll("portal-card", "portal-table-card");

        HBox header = new HBox();
        header.getStyleClass().add("portal-card-header");
        Label headerTitle = new Label("Scan History");
        headerTitle.getStyleClass().add("portal-card-title");
        header.getChildren().add(headerTitle);

        VBox table = new VBox();
        table.getStyleClass().add("portal-table");
        table.getChildren().add(createHeaderRow("BOX ID", "PROFILE", "STATUS", "STARTED", "COMPLETED", "PAGES", "ACTION"));
        for (UserPortalModel.HistoryItem item : portalModel.fetchScanHistory()) {
            table.getChildren().add(createDataRow(item));
        }

        card.getChildren().addAll(header, table);
        page.getChildren().addAll(intro, card);
        return page;
    }

    private GridPane createHeaderRow(String... values) {
        GridPane row = createRowSkeleton();
        row.getStyleClass().add("portal-table-head");
        for (int index = 0; index < values.length; index++) {
            Label label = new Label(values[index]);
            label.getStyleClass().add("portal-table-cell-head");
            row.add(label, index, 0);
        }
        return row;
    }

    private GridPane createDataRow(UserPortalModel.HistoryItem item) {
        GridPane row = createRowSkeleton();
        row.getStyleClass().add("portal-table-row");

        Button box = new Button(item.boxId());
        box.getStyleClass().add("portal-table-link");
        box.setOnAction(event -> navigator.resumeHistoryScan(item));

        Button action = new Button(item.status().equalsIgnoreCase("Processing") ? "Resume" : "Reuse");
        action.getStyleClass().add("portal-row-button");
        action.setOnAction(event -> navigator.resumeHistoryScan(item));

        row.add(box, 0, 0);
        row.add(dataLabel(item.profileName()), 1, 0);
        row.add(PortalController.buildStatusChip(item.status()), 2, 0);
        row.add(dataLabel(item.startedAt()), 3, 0);
        row.add(dataLabel(item.completedAt()), 4, 0);
        row.add(dataLabel(String.valueOf(item.pages())), 5, 0);
        row.add(action, 6, 0);
        return row;
    }

    private Label dataLabel(String value) {
        Label label = new Label(value);
        label.getStyleClass().add("portal-table-cell");
        return label;
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
