package easv.gui.controller.user;

import easv.gui.BackgroundExecutor;
import easv.gui.PrimeIcons;
import easv.gui.UserPortalModel;
import javafx.application.Platform;
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

import java.util.List;

public class DashboardController {
    private final UserPortalModel portalModel;
    private final UserNavigator navigator;
    private GridPane metricsSection;
    private HBox lowerSection;

    public DashboardController(UserPortalModel portalModel, UserNavigator navigator) {
        this.portalModel = portalModel;
        this.navigator = navigator == null ? UserNavigator.none() : navigator;
    }

    public Node create() {
        VBox page = new VBox(28);
        page.getStyleClass().addAll("portal-page", "dashboard-page");
        metricsSection = buildMetricsSkeleton();
        lowerSection = buildLowerSectionSkeleton();
        page.getChildren().setAll(
                buildIntro(),
                metricsSection,
                buildActions(),
                lowerSection
        );
        loadDashboardAsync(page);
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

    private GridPane buildMetrics(List<UserPortalModel.DashboardMetric> metrics) {
        if (metrics.size() < 4) {
            return buildMetricsFailureState();
        }

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

        grid.add(metricCard(
                "user-dashboard-summary-icon-teal",
                "user-dashboard-summary-icon-teal-path",
                "\ue941",
                metrics.get(0).label(),
                metrics.get(0).value(),
                "Total sessions across your scanning workspace."
        ), 0, 0);
        grid.add(metricCard(
                "user-dashboard-summary-icon-blue",
                "user-dashboard-summary-icon-blue-path",
                "\ue958",
                metrics.get(1).label(),
                metrics.get(1).value(),
                "Completed batches started during the current month."
        ), 1, 0);
        grid.add(metricCard(
                "user-dashboard-summary-icon-green",
                "user-dashboard-summary-icon-green-path",
                "\ue9e4",
                metrics.get(2).label(),
                metrics.get(2).value(),
                "Pages scanned and staged for review or export."
        ), 2, 0);
        grid.add(metricCard(
                "user-dashboard-summary-icon-amber",
                "user-dashboard-summary-icon-amber-path",
                "\ue922",
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

    private HBox buildLowerSection(List<UserPortalModel.RecentScanItem> recentScans,
                                   UserPortalModel.AccountProfile accountProfile) {
        HBox layout = new HBox(20);
        layout.setAlignment(Pos.TOP_LEFT);
        layout.getStyleClass().add("admin-dashboard-layout");

        VBox main = new VBox(24, buildRecentScansCard(recentScans));
        main.getStyleClass().add("admin-dashboard-main");
        HBox.setHgrow(main, Priority.ALWAYS);

        VBox side = new VBox(24, buildQuickAccessPanel(recentScans), buildAccountPanel(accountProfile));
        side.getStyleClass().add("admin-dashboard-side");
        side.setPrefWidth(390);
        side.setMinWidth(340);
        side.setMaxWidth(420);

        layout.getChildren().addAll(main, side);
        return layout;
    }

    private VBox buildRecentScansCard(List<UserPortalModel.RecentScanItem> recentScans) {
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
        if (recentScans.isEmpty()) {
            Label empty = new Label("No scan activity has been stored yet.");
            empty.getStyleClass().add("dashboard-section-subtitle");
            empty.setWrapText(true);
            table.getChildren().add(empty);
        } else {
            for (UserPortalModel.RecentScanItem item : recentScans) {
                table.getChildren().add(createDataRow(item));
            }
        }

        card.getChildren().addAll(header, table);
        return card;
    }

    private VBox buildQuickAccessPanel(List<UserPortalModel.RecentScanItem> recentScans) {
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

        UserPortalModel.RecentScanItem processingItem = recentScans.stream()
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

    private VBox buildAccountPanel(UserPortalModel.AccountProfile accountProfile) {
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

    private void loadDashboardAsync(VBox page) {
        BackgroundExecutor.io().execute(() -> {
            try {
                DashboardSnapshot snapshot = new DashboardSnapshot(
                        portalModel.fetchDashboardMetrics(),
                        portalModel.fetchRecentScans(),
                        portalModel.fetchAccountProfile()
                );

                Platform.runLater(() -> {
                    page.getChildren().set(1, buildMetrics(snapshot.metrics()));
                    page.getChildren().set(3, buildLowerSection(snapshot.recentScans(), snapshot.accountProfile()));
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    page.getChildren().set(1, buildMetricsFailureState());
                    page.getChildren().set(3, buildLowerSectionFailureState());
                });
            }
        });
    }

    private record DashboardSnapshot(
            List<UserPortalModel.DashboardMetric> metrics,
            List<UserPortalModel.RecentScanItem> recentScans,
            UserPortalModel.AccountProfile accountProfile
    ) {
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
                            String iconGlyph,
                            String titleText,
                            String valueText,
                            String subtitleText) {
        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add(iconBoxClass);
        iconBox.getChildren().add(createIcon(iconGlyph, iconPathClass));

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

    private GridPane buildMetricsSkeleton() {
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

        grid.add(loadingMetricCard("Assigned Profiles"), 0, 0);
        grid.add(loadingMetricCard("Batches"), 1, 0);
        grid.add(loadingMetricCard("Documents"), 2, 0);
        grid.add(loadingMetricCard("Pages"), 3, 0);
        return grid;
    }

    private HBox buildLowerSectionSkeleton() {
        HBox layout = new HBox(20);
        layout.setAlignment(Pos.TOP_LEFT);
        layout.getStyleClass().add("admin-dashboard-layout");

        VBox main = new VBox(24, buildRecentScansSkeleton());
        main.getStyleClass().add("admin-dashboard-main");
        HBox.setHgrow(main, Priority.ALWAYS);

        VBox side = new VBox(24, buildQuickAccessSkeleton(), buildAccountSkeleton());
        side.getStyleClass().add("admin-dashboard-side");
        side.setPrefWidth(390);
        side.setMinWidth(340);
        side.setMaxWidth(420);

        layout.getChildren().addAll(main, side);
        return layout;
    }

    private GridPane buildMetricsFailureState() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.getColumnConstraints().setAll(percentColumn(100));
        grid.add(sectionFailureCard("Dashboard metrics could not be loaded."), 0, 0);
        return grid;
    }

    private HBox buildLowerSectionFailureState() {
        HBox layout = new HBox(20);
        layout.setAlignment(Pos.TOP_LEFT);
        layout.getStyleClass().add("admin-dashboard-layout");

        VBox main = new VBox(24, sectionFailureCard("Recent scan history is unavailable right now."));
        main.getStyleClass().add("admin-dashboard-main");
        HBox.setHgrow(main, Priority.ALWAYS);

        VBox side = new VBox(24, buildQuickAccessSkeleton(), buildAccountSkeleton());
        side.getStyleClass().add("admin-dashboard-side");
        side.setPrefWidth(390);
        side.setMinWidth(340);
        side.setMaxWidth(420);

        layout.getChildren().addAll(main, side);
        return layout;
    }

    private VBox loadingMetricCard(String titleText) {
        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("user-dashboard-summary-icon-blue");
        Label icon = new Label("...");
        icon.getStyleClass().add("dashboard-summary-title");
        iconBox.getChildren().add(icon);

        Label value = new Label("—");
        value.getStyleClass().add("dashboard-summary-value");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(iconBox, spacer, value);
        header.setAlignment(Pos.TOP_LEFT);

        Label title = new Label(titleText);
        title.getStyleClass().add("dashboard-summary-title");

        Label subtitle = new Label("Loading...");
        subtitle.getStyleClass().add("dashboard-summary-subtitle");

        VBox text = new VBox(4, title, subtitle);
        VBox card = new VBox(12, header, text);
        card.getStyleClass().add("admin-metric-card");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private VBox buildRecentScansSkeleton() {
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
        table.getChildren().addAll(
                skeletonRow("Loading scans..."),
                skeletonRow("Loading scans..."),
                skeletonRow("Loading scans...")
        );

        card.getChildren().addAll(header, table);
        return card;
    }

    private VBox buildQuickAccessSkeleton() {
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

        Label processingTitle = new Label("Checking active work...");
        processingTitle.getStyleClass().add("dashboard-simple-title");

        Label processingDetail = new Label("Recent scan state is loading.");
        processingDetail.getStyleClass().add("dashboard-simple-detail");
        processingDetail.setWrapText(true);

        card.getChildren().addAll(title, subtitle, grid, processingTitle, processingDetail);
        return card;
    }

    private VBox buildAccountSkeleton() {
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
                detailBlock("Name", "Loading..."),
                detailBlock("Email", "Loading..."),
                detailBlock("Department", "Loading...")
        );

        return card;
    }

    private VBox sectionFailureCard(String message) {
        Label title = new Label("Unavailable");
        title.getStyleClass().add("dashboard-section-title");

        Label copy = new Label(message);
        copy.getStyleClass().add("dashboard-section-subtitle");
        copy.setWrapText(true);

        VBox card = new VBox(8, title, copy);
        card.getStyleClass().add("admin-panel-card");
        return card;
    }

    private GridPane skeletonRow(String message) {
        GridPane row = createRowSkeleton();
        row.getStyleClass().add("portal-table-row");
        Label label = new Label(message);
        label.getStyleClass().add("portal-table-cell");
        row.add(label, 0, 0);
        GridPane.setColumnSpan(label, 5);
        return row;
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
                UserPortalUi.buildIcon(iconKey, primary ? "admin-action-icon-inverse" : "admin-action-icon"),
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

    private Label createIcon(String glyph, String styleClass) {
        return PrimeIcons.create(glyph, styleClass);
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

        row.add(primaryCell(item.boxId()), 0, 0);
        row.add(dataLabel(item.profileName()), 1, 0);
        row.add(dataLabel(item.startedAt()), 2, 0);
        row.add(dataLabel(String.valueOf(item.pages())), 3, 0);
        row.add(UserPortalUi.buildStatusChip(item.status()), 4, 0);
        return row;
    }

    private Label primaryCell(String value) {
        Label label = new Label(value);
        label.getStyleClass().add("exports-table-cell-primary");
        return label;
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
