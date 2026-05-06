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

public class ExportsController {
    private final UserPortalModel portalModel;

    public ExportsController(UserPortalModel portalModel) {
        this.portalModel = portalModel;
    }

    public Node create() {
        VBox page = new VBox(28);
        page.getStyleClass().add("portal-page");

        Label title = new Label("Exports");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Track generated files and download exports that are ready.");
        subtitle.getStyleClass().add("page-subtitle");
        VBox intro = new VBox(10, title, subtitle);
        intro.getStyleClass().add("portal-page-intro");

        VBox card = new VBox();
        card.getStyleClass().addAll("portal-card", "portal-table-card");

        HBox header = new HBox();
        header.getStyleClass().add("portal-card-header");
        Label headerTitle = new Label("Export Queue");
        headerTitle.getStyleClass().add("portal-card-title");
        header.getChildren().add(headerTitle);

        VBox table = new VBox();
        table.getStyleClass().add("portal-table");
        table.getChildren().add(createHeaderRow("FILE NAME", "BOX ID", "PROFILE", "CREATED", "SIZE", "STATUS", "ACTION"));
        for (UserPortalModel.ExportItem item : portalModel.fetchExports()) {
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

    private GridPane createDataRow(UserPortalModel.ExportItem item) {
        GridPane row = createRowSkeleton();
        row.getStyleClass().add("portal-table-row");

        Button action = new Button("Download");
        action.getStyleClass().add("portal-row-button");
        action.setGraphic(PortalController.buildIcon("download", "portal-button-icon"));
        action.setDisable(!"Ready".equalsIgnoreCase(item.status()));

        row.add(dataLabel(item.fileName()), 0, 0);
        row.add(dataLabel(item.boxId()), 1, 0);
        row.add(dataLabel(item.profileName()), 2, 0);
        row.add(dataLabel(item.createdAt()), 3, 0);
        row.add(dataLabel(item.size()), 4, 0);
        row.add(PortalController.buildStatusChip(item.status()), 5, 0);
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
