package easv.gui.controller.user;

import easv.gui.UserPortalModel;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DashboardController {
    private final UserPortalModel portalModel;
    private final UserNavigator navigator;

    public DashboardController(UserPortalModel portalModel, UserNavigator navigator) {
        this.portalModel = portalModel;
        this.navigator = navigator == null ? UserNavigator.none() : navigator;
    }

    public Node create() {
        VBox page = new VBox(28);
        page.getStyleClass().addAll("portal-page", "dashboard-page");
        page.getChildren().setAll(
                buildIntro(),
                buildActions(),
                new MyScansController(portalModel, navigator).createEmbedded()
        );
        return page;
    }

    private VBox buildIntro() {
        Label title = new Label("Dashboard");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Review scan history and reopen sessions that still need attention.");
        subtitle.getStyleClass().add("page-subtitle");

        VBox intro = new VBox(10, title, subtitle);
        intro.getStyleClass().add("page-heading-copy");
        return intro;
    }

    private HBox buildActions() {
        HBox wrapper = new HBox();
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.getStyleClass().add("user-dashboard-actions-wrap");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.getStyleClass().add("user-dashboard-actions-grid");
        grid.getColumnConstraints().setAll(percentColumn(50), percentColumn(50));

        grid.add(createActionTile(
                "scan",
                "New Scan",
                "Start a scanning session and route pages into documents.",
                true,
                navigator::showNewScan
        ), 0, 0);
        grid.add(createActionTile(
                "dashboard",
                "Assigned QA",
                "Open the QA queue for work assigned to you.",
                false,
                navigator::showAssignedQa
        ), 1, 0);

        wrapper.getChildren().add(grid);
        return wrapper;
    }

    private Button createActionTile(String iconKey, String titleText, String bodyText, boolean primary, Runnable action) {
        Button tile = new Button();
        tile.getStyleClass().add("admin-action-tile");
        tile.getStyleClass().add("user-dashboard-action-tile");
        tile.getStyleClass().add(primary ? "admin-action-tile-primary" : "admin-action-tile-secondary");
        tile.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        tile.setMinHeight(112);
        tile.setPrefHeight(112);
        tile.setMaxHeight(112);
        tile.setOnAction(event -> action.run());

        Label title = new Label(titleText);
        title.getStyleClass().add("admin-action-title");

        Label body = new Label(bodyText);
        body.getStyleClass().add("admin-action-text");
        body.setWrapText(true);

        VBox content = new VBox(12,
                UserPortalUi.buildIcon(iconKey, primary ? "admin-action-icon-inverse" : "admin-action-icon"),
                title,
                body
        );
        content.getStyleClass().add("admin-action-body");
        tile.setGraphic(content);
        return tile;
    }

    private ColumnConstraints percentColumn(double width) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setPercentWidth(width);
        constraints.setHgrow(Priority.ALWAYS);
        return constraints;
    }
}
