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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

public class DashboardController {
    private final UserPortalModel portalModel;
    private final PortalNavigator navigator;

    public DashboardController(UserPortalModel portalModel, PortalNavigator navigator) {
        this.portalModel = portalModel;
        this.navigator = navigator == null ? PortalNavigator.none() : navigator;
    }

    public Node create() {
        VBox page = new VBox(28);
        page.getStyleClass().addAll("portal-page", "dashboard-page");
        page.getChildren().addAll(
                buildIntro(),
                buildMetrics(),
                buildActions(),
                buildLowerSection()
        );
        return page;
    }

    private VBox buildIntro() {
        Label title = new Label("Dashboard");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Monitor scan activity, recent sessions, and queue handoffs.");
        subtitle.getStyleClass().add("page-subtitle");

        VBox intro = new VBox(10, title, subtitle);
        intro.getStyleClass().add("page-heading-copy");
        return intro;
    }

    private GridPane buildMetrics() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.getColumnConstraints().setAll(
                percentColumn(25),
                percentColumn(25),
                percentColumn(25),
                percentColumn(25)
        );

        var metrics = portalModel.fetchDashboardMetrics();
        grid.add(metricCard(
                "dashboard-summary-icon-teal",
                "dashboard-summary-icon-teal-path",
                "M8 7.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5zm8 0a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5zM8 9.5c-3.04 0-5.5 1.79-5.5 4V15h11v-1.5c0-2.21-2.46-4-5.5-4zm8 0c-.52 0-1.03.05-1.5.15 1.23.89 2 2.11 2 3.55V15h5v-1.5c0-2.21-2.46-4-5.5-4z",
                metrics.get(0).label(),
                metrics.get(0).value(),
                "Total sessions across your scanning workspace."
        ), 0, 0);
        grid.add(metricCard(
                "dashboard-summary-icon-blue",
                "dashboard-summary-icon-blue-path",
                "M5 2h7l5 5v11H5z M11 2v5h5",
                metrics.get(1).label(),
                metrics.get(1).value(),
                "Completed batches started during the current month."
        ), 1, 0);
        grid.add(metricCard(
                "dashboard-summary-icon-green",
                "dashboard-summary-icon-green-path",
                "M3 17.5 9.2 11.3l3.3 3.3L20 7.1V12h2V3.5h-8.5v2H18l-5.5 5.5-3.3-3.3L1.5 15.4 3 17.5z",
                metrics.get(2).label(),
                metrics.get(2).value(),
                "Pages scanned and staged for review or export."
        ), 2, 0);
        grid.add(metricCard(
                "dashboard-summary-icon-amber",
                "dashboard-summary-icon-amber-path",
                "M12 2 1 21h22L12 2zm0 6 1 7h-2l1-7zm-1 9h2v2h-2v-2z",
                metrics.get(3).label(),
                metrics.get(3).value(),
                "Successfully finished scans from your recent work."
        ), 3, 0);
        return grid;
    }

    private GridPane buildActions() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.getColumnConstraints().setAll(percentColumn(50), percentColumn(50));

        grid.add(createActionTile(
                "scan",
                "New Scan",
                "Start a scanning session and route pages into documents.",
                true,
                navigator::showNewScan
        ), 0, 0);
        grid.add(createActionTile(
                "scans",
                "My Scans",
                "Browse scan history and reopen sessions.",
                false,
                navigator::showScans
        ), 1, 0);
        grid.add(createActionTile(
                "dashboard",
                "Assigned QA",
                "Open the QA queue for work assigned to you.",
                false,
                navigator::showAssignedQa
        ), 0, 1);
        grid.add(createActionTile(
                "exports",
                "Exports",
                "Track completed files and download ready batches.",
                false,
                navigator::showExports
        ), 1, 1);
        return grid;
    }

    private HBox buildLowerSection() {
        HBox layout = new HBox(20);
        layout.setAlignment(Pos.TOP_LEFT);
        layout.getStyleClass().add("admin-dashboard-layout");

        VBox main = new VBox(24, buildRecentScansCard());
        main.getStyleClass().add("admin-dashboard-main");
        HBox.setHgrow(main, Priority.ALWAYS);

        VBox side = new VBox(24, buildQuickAccessPanel(), buildAccountPanel());
        side.getStyleClass().add("admin-dashboard-side");
        side.setPrefWidth(390);
        side.setMinWidth(340);
        side.setMaxWidth(420);

        layout.getChildren().addAll(main, side);
        return layout;
    }

    private VBox buildRecentScansCard() {
        VBox card = new VBox(0);
        card.getStyleClass().add("admin-panel-card");
        card.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label("Recent Scans");
        title.getStyleClass().add("dashboard-section-title");
        Label subtitle = new Label("Latest batches in your workspace and their current state.");
        subtitle.getStyleClass().add("dashboard-section-subtitle");
        subtitle.setWrapText(true);

        Button link = new Button("View All ->");
        link.getStyleClass().add("admin-link-button");
        link.setOnAction(event -> navigator.showScans());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, new VBox(3, title, subtitle), spacer, link);
        header.getStyleClass().add("admin-panel-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox table = new VBox();
        table.getStyleClass().add("portal-table");
        table.getChildren().add(createHeaderRow("BOX ID", "PROFILE", "DATE", "PAGES", "STATUS"));
        for (UserPortalModel.RecentScanItem item : portalModel.fetchRecentScans()) {
            table.getChildren().add(createDataRow(item));
        }

        card.getChildren().addAll(header, table);
        return card;
    }

    private VBox buildQuickAccessPanel() {
        VBox card = new VBox(18);
        card.getStyleClass().add("admin-panel-card");

        Label title = new Label("Quick Access");
        title.getStyleClass().add("dashboard-section-title");
        Label subtitle = new Label("Fast entry points for the next step in your workflow.");
        subtitle.getStyleClass().add("dashboard-section-subtitle");
        subtitle.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.getColumnConstraints().setAll(percentColumn(50), percentColumn(50));

        grid.add(quickButton("New Scan", true, navigator::showNewScan), 0, 0);
        grid.add(quickButton("Assigned QA", false, navigator::showAssignedQa), 1, 0);
        grid.add(quickButton("Exports", false, navigator::showExports), 0, 1);
        grid.add(quickButton("Settings", false, navigator::showSettings), 1, 1);

        UserPortalModel.RecentScanItem processingItem = portalModel.fetchRecentScans().stream()
                .filter(item -> "Processing".equalsIgnoreCase(item.status()))
                .findFirst()
                .orElse(null);

        Label processingTitle = new Label(processingItem == null ? "No in-progress batch" : processingItem.boxId());
        processingTitle.getStyleClass().add("dashboard-simple-title");

        Label processingDetail = new Label(processingItem == null
                ? "All recent scans are complete or ready for export."
                : processingItem.profileName() + " is still in progress and can be resumed from My Scans.");
        processingDetail.getStyleClass().add("dashboard-simple-detail");
        processingDetail.setWrapText(true);

        card.getChildren().addAll(title, subtitle, grid, processingTitle, processingDetail);
        return card;
    }

    private VBox buildAccountPanel() {
        UserPortalModel.AccountProfile accountProfile = portalModel.fetchAccountProfile();

        VBox card = new VBox(18);
        card.getStyleClass().add("admin-panel-card");

        Label title = new Label("Account Summary");
        title.getStyleClass().add("dashboard-section-title");
        Label subtitle = new Label("Current user context for scanning and export work.");
        subtitle.getStyleClass().add("dashboard-section-subtitle");
        subtitle.setWrapText(true);

        card.getChildren().addAll(
                title,
                subtitle,
                detailBlock("Name", accountProfile.fullName()),
                detailBlock("Email", accountProfile.email()),
                detailBlock("Department", accountProfile.department())
        );

        return card;
    }

    private VBox detailBlock(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("dashboard-section-subtitle");

        Label value = new Label(valueText);
        value.getStyleClass().add("dashboard-simple-title");
        value.setWrapText(true);

        return new VBox(4, label, value);
    }

    private VBox metricCard(String iconBoxClass,
                            String iconPathClass,
                            String iconPath,
                            String titleText,
                            String valueText,
                            String subtitleText) {
        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add(iconBoxClass);
        iconBox.getChildren().add(createIcon(iconPath, iconPathClass));

        Label value = new Label(valueText);
        value.getStyleClass().add("dashboard-summary-value");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(iconBox, spacer, value);
        header.setAlignment(Pos.TOP_LEFT);

        Label title = new Label(titleText);
        title.getStyleClass().add("dashboard-summary-title");

        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("dashboard-summary-subtitle");
        subtitle.setWrapText(true);

        VBox text = new VBox(4, title, subtitle);
        VBox card = new VBox(12, header, text);
        card.getStyleClass().add("admin-metric-card");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private Button createActionTile(String iconKey, String titleText, String bodyText, boolean primary, Runnable action) {
        Button tile = new Button();
        tile.getStyleClass().add("admin-action-tile");
        tile.getStyleClass().add(primary ? "admin-action-tile-primary" : "admin-action-tile-secondary");
        tile.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        tile.setMaxWidth(Double.MAX_VALUE);
        tile.setMinHeight(144);
        tile.setPrefHeight(144);
        tile.setMaxHeight(144);
        tile.setOnAction(event -> action.run());

        Label title = new Label(titleText);
        title.getStyleClass().add("admin-action-title");

        Label body = new Label(bodyText);
        body.getStyleClass().add("admin-action-text");
        body.setWrapText(true);

        VBox content = new VBox(12,
                PortalController.buildIcon(iconKey, primary ? "admin-action-icon-inverse" : "admin-action-icon"),
                title,
                body
        );
        content.getStyleClass().add("admin-action-body");
        tile.setGraphic(content);
        return tile;
    }

    private Button quickButton(String text, boolean primary, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add(primary ? "admin-primary-button" : "admin-secondary-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> action.run());
        return button;
    }

    private SVGPath createIcon(String content, String styleClass) {
        SVGPath icon = new SVGPath();
        icon.setContent(content);
        icon.getStyleClass().add(styleClass);
        return icon;
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

    private GridPane createDataRow(UserPortalModel.RecentScanItem item) {
        GridPane row = createRowSkeleton();
        row.getStyleClass().add("portal-table-row");

        Button box = new Button(item.boxId());
        box.getStyleClass().add("portal-table-link");
        box.setOnAction(event -> navigator.resumeRecentScan(item));

        row.add(box, 0, 0);
        row.add(dataLabel(item.profileName()), 1, 0);
        row.add(dataLabel(item.startedAt()), 2, 0);
        row.add(dataLabel(String.valueOf(item.pages())), 3, 0);
        row.add(PortalController.buildStatusChip(item.status()), 4, 0);
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
                percentColumn(22),
                percentColumn(23),
                percentColumn(25),
                percentColumn(10),
                percentColumn(20)
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
