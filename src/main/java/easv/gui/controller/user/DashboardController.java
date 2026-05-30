package easv.gui.controller.user;

import easv.gui.controller.util.BackgroundExecutor;
import easv.gui.controller.util.SkeletonFactory;
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
    private static final int MAX_RECENT_SCAN_ROWS = 5;

    private final UserPortalModel portalModel;
    private final UserNavigator navigator;

    public DashboardController(UserPortalModel portalModel, UserNavigator navigator) {
        this.portalModel = portalModel;
        this.navigator = navigator == null ? UserNavigator.none() : navigator;
    }

    public Node create() {
        VBox page = new VBox(18);
        page.getStyleClass().addAll("portal-page", "dashboard-page");
        page.getChildren().setAll(
                buildIntro(),
                buildMetricsSkeleton(),
                buildActions(),
                buildLowerSectionSkeleton()
        );
        loadDashboardAsync(page);
        return page;
    }

    private VBox buildIntro() {
        Label title = new Label("Dashboard");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Monitor scan activity, recent sessions, and queue handoffs.");
        subtitle.getStyleClass().add("page-subtitle");

        VBox intro = new VBox(6, title, subtitle);
        intro.getStyleClass().add("page-heading-copy");
        return intro;
    }

    private GridPane buildMetrics(List<UserPortalModel.DashboardMetric> metrics) {
        if (metrics.size() < 4) {
            return buildMetricsFailureState();
        }

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
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
        grid.setHgap(14);
        grid.setVgap(14);
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
                "dashboard",
                "Assigned QA",
                "Open the QA queue for work assigned to you.",
                false,
                navigator::showAssignedQa
        ), 1, 0);

        return grid;
    }

    private HBox buildLowerSection(List<UserPortalModel.HistoryItem> recentScans) {
        HBox layout = new HBox(14);
        layout.setAlignment(Pos.TOP_LEFT);
        layout.getStyleClass().add("admin-dashboard-layout");

        VBox main = new VBox(16, buildRecentScansCard(recentScans));
        main.getStyleClass().add("admin-dashboard-main");
        HBox.setHgrow(main, Priority.ALWAYS);

        layout.getChildren().add(main);
        return layout;
    }

    private VBox buildRecentScansCard(List<UserPortalModel.HistoryItem> recentScans) {
        List<UserPortalModel.HistoryItem> visibleRecentScans = recentScans.stream()
                .limit(MAX_RECENT_SCAN_ROWS)
                .toList();

        VBox card = new VBox(0);
        card.getStyleClass().add("admin-panel-card");
        card.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label("Recent Scans");
        title.getStyleClass().add("dashboard-section-title");
        Label subtitle = new Label("Latest batches in your workspace and their current state.");
        subtitle.getStyleClass().add("dashboard-section-subtitle");
        subtitle.setWrapText(true);

        HBox header = new HBox(10, new VBox(2, title, subtitle));
        header.getStyleClass().add("admin-panel-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox table = new VBox();
        table.getStyleClass().add("exports-table");
        table.getChildren().add(createHeaderRow("BOX ID", "PROFILE", "PAGES", "SIZE", "DATE", "STATUS"));
        if (visibleRecentScans.isEmpty()) {
            Label empty = new Label("No scan activity has been stored yet.");
            empty.getStyleClass().add("exports-footer-text");
            empty.setWrapText(true);
            HBox emptyRow = new HBox(empty);
            emptyRow.getStyleClass().add("exports-empty-row");
            table.getChildren().add(emptyRow);
        } else {
            for (UserPortalModel.HistoryItem item : visibleRecentScans) {
                table.getChildren().add(createDataRow(item));
            }
        }

        card.getChildren().addAll(header, table);
        return card;
    }

    private void loadDashboardAsync(VBox page) {
        BackgroundExecutor.io().execute(() -> {
            try {
                DashboardSnapshot snapshot = new DashboardSnapshot(
                        portalModel.fetchDashboardMetrics(),
                        portalModel.fetchScanHistory().stream().limit(MAX_RECENT_SCAN_ROWS).toList()
                );

                Platform.runLater(() -> {
                    SkeletonFactory.stopShimmers(page.getChildren().get(1));
                    SkeletonFactory.stopShimmers(page.getChildren().get(3));
                    page.getChildren().set(1, buildMetrics(snapshot.metrics()));
                    page.getChildren().set(3, buildLowerSection(snapshot.recentScans()));
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    SkeletonFactory.stopShimmers(page.getChildren().get(1));
                    SkeletonFactory.stopShimmers(page.getChildren().get(3));
                    page.getChildren().set(1, buildMetricsFailureState());
                    page.getChildren().set(3, buildLowerSectionFailureState());
                });
            }
        });
    }

    private record DashboardSnapshot(
            List<UserPortalModel.DashboardMetric> metrics,
            List<UserPortalModel.HistoryItem> recentScans
    ) {
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
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.getColumnConstraints().setAll(
                percentColumn(25),
                percentColumn(25),
                percentColumn(25),
                percentColumn(25)
        );

        grid.add(loadingMetricCard(), 0, 0);
        grid.add(loadingMetricCard(), 1, 0);
        grid.add(loadingMetricCard(), 2, 0);
        grid.add(loadingMetricCard(), 3, 0);
        return grid;
    }

    private HBox buildLowerSectionSkeleton() {
        HBox layout = new HBox(14);
        layout.setAlignment(Pos.TOP_LEFT);
        layout.getStyleClass().add("admin-dashboard-layout");

        VBox main = new VBox(16, buildRecentScansSkeleton());
        main.getStyleClass().add("admin-dashboard-main");
        HBox.setHgrow(main, Priority.ALWAYS);

        layout.getChildren().add(main);
        return layout;
    }

    private GridPane buildMetricsFailureState() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.getColumnConstraints().setAll(percentColumn(100));
        grid.add(sectionFailureCard("Dashboard metrics could not be loaded."), 0, 0);
        return grid;
    }

    private HBox buildLowerSectionFailureState() {
        HBox layout = new HBox(14);
        layout.setAlignment(Pos.TOP_LEFT);
        layout.getStyleClass().add("admin-dashboard-layout");

        VBox main = new VBox(16, sectionFailureCard("Recent scan history is unavailable right now."));
        main.getStyleClass().add("admin-dashboard-main");
        HBox.setHgrow(main, Priority.ALWAYS);

        layout.getChildren().add(main);
        return layout;
    }

    private VBox loadingMetricCard() {
        Region iconPlaceholder = SkeletonFactory.circle(48);
        Region valuePlaceholder = SkeletonFactory.line(64, 28);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(iconPlaceholder, spacer, valuePlaceholder);
        header.setAlignment(Pos.CENTER_LEFT);

        Region titlePlaceholder = SkeletonFactory.line(120, 12);
        Region subtitlePlaceholder = SkeletonFactory.line(180, 10, SkeletonFactory.Intensity.LIGHT);

        VBox text = new VBox(6, titlePlaceholder, subtitlePlaceholder);
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

        HBox header = new HBox(10, new VBox(2, title, subtitle));
        header.getStyleClass().add("admin-panel-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox table = new VBox();
        table.getStyleClass().add("exports-table");
        table.getChildren().add(createHeaderRow("BOX ID", "PROFILE", "PAGES", "SIZE", "DATE", "STATUS"));
        for (int rowIndex = 0; rowIndex < MAX_RECENT_SCAN_ROWS; rowIndex++) {
            table.getChildren().add(skeletonRow());
        }

        card.getChildren().addAll(header, table);
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

    private GridPane skeletonRow() {
        GridPane row = createRowSkeleton();
        row.getStyleClass().add("exports-table-row");
        row.add(skeletonCell(80, 12), 0, 0);
        row.add(skeletonCell(120, 12), 1, 0);
        row.add(skeletonCell(32, 12), 2, 0);
        row.add(skeletonCell(56, 12), 3, 0);
        row.add(skeletonCell(96, 12), 4, 0);
        row.add(skeletonCell(72, 16), 5, 0);
        return row;
    }

    private HBox skeletonCell(double width, double height) {
        HBox wrap = new HBox(SkeletonFactory.line(width, height));
        wrap.setAlignment(Pos.CENTER_LEFT);
        return wrap;
    }

    private Button createActionTile(String iconKey, String titleText, String bodyText, boolean primary, Runnable action) {
        Button tile = new Button();
        tile.getStyleClass().add("admin-action-tile");
        tile.getStyleClass().add(primary ? "admin-action-tile-primary" : "admin-action-tile-secondary");
        tile.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        tile.setMaxWidth(Double.MAX_VALUE);
        tile.setMinHeight(120);
        tile.setPrefHeight(120);
        tile.setMaxHeight(120);
        tile.setOnAction(event -> action.run());

        Label title = new Label(titleText);
        title.getStyleClass().add("admin-action-title");

        Label body = new Label(bodyText);
        body.getStyleClass().add("admin-action-text");
        body.setWrapText(true);

        VBox content = new VBox(8,
                UserPortalUi.buildIcon(iconKey, primary ? "admin-action-icon-inverse" : "admin-action-icon"),
                title,
                body
        );
        content.getStyleClass().add("admin-action-body");
        tile.setGraphic(content);
        return tile;
    }

    private Label createIcon(String glyph, String styleClass) {
        String iconKey = switch (glyph) {
            case "\ue941" -> "dashboard";
            case "\ue958" -> "exports";
            case "\ue9e4" -> "scan";
            case "\ue922" -> "settings";
            default -> "dashboard";
        };

        Label iconLabel = new Label();
        iconLabel.setGraphic(UserPortalUi.buildIcon(iconKey, styleClass));
        return iconLabel;
    }

    private GridPane createHeaderRow(String... values) {
        GridPane row = createRowSkeleton();
        row.getStyleClass().add("exports-table-header-row");
        for (int index = 0; index < values.length; index++) {
            Label label = new Label(values[index]);
            label.getStyleClass().add("exports-table-header");
            row.add(label, index, 0);
        }
        return row;
    }

    private GridPane createDataRow(UserPortalModel.HistoryItem item) {
        GridPane row = createRowSkeleton();
        row.getStyleClass().add("exports-table-row");

        row.add(primaryCell(item.boxId()), 0, 0);
        row.add(dataLabel(item.profileName()), 1, 0);
        row.add(dataLabel(String.valueOf(item.pages())), 2, 0);
        row.add(dataLabel(item.size()), 3, 0);
        row.add(dataLabel(item.startedAt()), 4, 0);
        row.add(statusCell(item.status()), 5, 0);
        return row;
    }

    private Label primaryCell(String value) {
        Label label = new Label(value);
        label.getStyleClass().add("exports-table-cell-primary");
        return label;
    }

    private Label dataLabel(String value) {
        Label label = new Label(value);
        label.getStyleClass().add("exports-table-cell");
        return label;
    }

    private HBox statusCell(String status) {
        HBox wrap = new HBox(UserPortalUi.buildStatusChip(status));
        wrap.getStyleClass().add("exports-table-cell-wrap");
        return wrap;
    }

    private GridPane createRowSkeleton() {
        GridPane row = new GridPane();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setHgap(12);
        row.getColumnConstraints().setAll(
                percentColumn(18),
                percentColumn(20),
                percentColumn(10),
                percentColumn(12),
                percentColumn(24),
                percentColumn(16)
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
