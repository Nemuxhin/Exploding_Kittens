package easv.gui;

import easv.gui.controller.DashboardController;
import easv.gui.controller.DashboardController.PageState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class MainView {
    private final UserPortalModel portalModel;
    private final DashboardController controller;
    private final PortalViewComponents components;

    public MainView(UserPortalModel portalModel, DashboardController controller, PortalViewComponents components) {
        this.portalModel = portalModel;
        this.controller = controller;
        this.components = components;
    }

    public VBox buildDashboardBody(Consumer<PageState> onNavigate) {
        VBox body = new VBox(14);
        body.getStyleClass().add("dashboard-body");

        Label heading = new Label("Dashboard");
        heading.getStyleClass().add("dashboard-heading");
        Label subtitle = new Label("Start a new scan or review your recent activity");
        subtitle.getStyleClass().add("dashboard-subtitle");

        VBox titleBlock = new VBox(4, heading, subtitle);

        HBox actionRow = new HBox(14);
        actionRow.getStyleClass().add("dashboard-action-row");
        for (UserPortalModel.ActionCard actionCard : portalModel.actionCards()) {
            actionRow.getChildren().add(buildActionCard(actionCard, onNavigate));
        }

        HBox lowerRow = new HBox(14, buildRecentScansPanel(), buildRightColumn());
        lowerRow.getStyleClass().add("dashboard-lower-row");
        HBox.setHgrow(lowerRow.getChildren().get(0), Priority.ALWAYS);

        body.getChildren().addAll(titleBlock, actionRow, lowerRow);
        return body;
    }

    public Parent buildProfilesBody(UserPortalModel.ScanProfile selectedProfile,
                                    String currentBoxId,
                                    Consumer<UserPortalModel.ScanProfile> onSelectProfile,
                                    Consumer<String> onStartScanning) {
        VBox body = new VBox(8);
        body.getStyleClass().addAll("dashboard-body", "qa-page-body");

        Label heading = new Label("Start New Scan");
        heading.getStyleClass().add("dashboard-heading");
        Label subtitle = new Label("Select a profile and enter box information");
        subtitle.getStyleClass().add("dashboard-subtitle");
        TextField searchField = new TextField();
        searchField.setPromptText("Search profiles");
        searchField.getStyleClass().addAll("box-id-field", "compact-search-field");

        VBox formCard = new VBox(10);
        formCard.getStyleClass().add("panel-card");
        formCard.setPadding(new Insets(14));
        formCard.setMaxWidth(860);

        Label profileSectionTitle = new Label("Select Profile");
        profileSectionTitle.getStyleClass().add("form-section-title");

        VBox profilesList = new VBox(6);
        Runnable refreshProfiles = () -> {
            profilesList.getChildren().clear();
            String search = controller.normalizeSearch(searchField.getText());
            for (UserPortalModel.ScanProfile profile : portalModel.scanProfiles()) {
                if (!controller.matchesSearch(search, profile.title(), profile.description(), profile.settingsLine())) {
                    continue;
                }
                profilesList.getChildren().add(buildProfileCard(profile, profile.equals(selectedProfile), onSelectProfile));
            }
        };
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshProfiles.run());
        refreshProfiles.run();

        Label boxIdTitle = new Label("Enter Box ID");
        boxIdTitle.getStyleClass().add("form-section-title");

        TextField boxIdField = new TextField();
        boxIdField.setPromptText("e.g., BOX-2026-043");
        boxIdField.getStyleClass().add("box-id-field");
        boxIdField.setText(currentBoxId);

        Label boxIdHint = new Label("Box ID will be used in the export filename");
        boxIdHint.getStyleClass().add("form-helper-text");

        Button startButton = new Button("Start Scanning");
        startButton.getStyleClass().add("start-scanning-button");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setDisable(selectedProfile == null || currentBoxId == null || currentBoxId.isBlank());
        boxIdField.textProperty().addListener((obs, oldValue, newValue) ->
                startButton.setDisable(selectedProfile == null || newValue == null || newValue.isBlank()));
        startButton.setOnAction(event -> {
            String boxId = boxIdField.getText() == null ? "" : boxIdField.getText().trim();
            if (selectedProfile != null && !boxId.isEmpty()) {
                onStartScanning.accept(boxId);
            }
        });

        formCard.getChildren().addAll(profileSectionTitle, profilesList, boxIdTitle, boxIdField, boxIdHint, startButton);

        ScrollPane formScroll = new ScrollPane(formCard);
        formScroll.setFitToWidth(true);
        formScroll.getStyleClass().add("profile-form-scroll");
        VBox.setVgrow(formScroll, Priority.ALWAYS);

        body.getChildren().addAll(heading, subtitle, searchField, formScroll);
        return body;
    }

    public VBox buildLogsBody() {
        VBox body = new VBox(14);
        body.getStyleClass().add("dashboard-body");

        Label heading = new Label("Exports");
        heading.getStyleClass().add("dashboard-heading");
        Label subtitle = new Label("Download your exported scan files");
        subtitle.getStyleClass().add("dashboard-subtitle");
        TextField searchField = new TextField();
        searchField.setPromptText("Search exports");
        searchField.getStyleClass().addAll("box-id-field", "compact-search-field");

        VBox panel = new VBox(0);
        panel.getStyleClass().addAll("panel-card", "exports-panel");

        GridPane table = new GridPane();
        table.getStyleClass().addAll("recent-table", "exports-table");
        table.setMaxWidth(Double.MAX_VALUE);
        table.getColumnConstraints().addAll(
                components.column(310),
                components.column(170),
                components.column(170),
                components.column(150),
                components.column(110),
                components.column(145),
                components.column(130)
        );

        table.add(components.headerCell("File Name"), 0, 0);
        table.add(components.headerCell("Box ID"), 1, 0);
        table.add(components.headerCell("Profile"), 2, 0);
        table.add(components.headerCell("Date Created"), 3, 0);
        table.add(components.headerCell("Size"), 4, 0);
        table.add(components.headerCell("Status"), 5, 0);
        table.add(components.headerCell("Action"), 6, 0);

        Runnable refreshTable = () -> {
            while (table.getChildren().size() > 7) {
                table.getChildren().remove(table.getChildren().size() - 1);
            }
            String search = controller.normalizeSearch(searchField.getText());
            int row = 1;
            for (UserPortalModel.ExportRow export : portalModel.exports()) {
                if (!controller.matchesSearch(search,
                        export.fileName(),
                        export.boxId(),
                        export.profile(),
                        export.dateCreated(),
                        export.size(),
                        export.status())) {
                    continue;
                }
                table.add(components.bodyCell(export.fileName()), 0, row);
                table.add(components.bodyCell(export.boxId()), 1, row);
                table.add(components.bodyCell(export.profile()), 2, row);
                table.add(components.bodyCell(export.dateCreated()), 3, row);
                table.add(components.bodyCell(export.size()), 4, row);
                table.add(components.exportStatusCell(export.status()), 5, row);
                table.add(components.exportActionCell(export), 6, row);
                row++;
            }
        };
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshTable.run());
        refreshTable.run();

        panel.getChildren().add(table);
        body.getChildren().addAll(heading, subtitle, searchField, panel);
        return body;
    }

    public VBox buildMyScansBody() {
        VBox body = new VBox(14);
        body.getStyleClass().add("dashboard-body");

        Label heading = new Label("My Scans");
        heading.getStyleClass().add("dashboard-heading");
        Label subtitle = new Label("Only your own scan activity and export history.");
        subtitle.getStyleClass().add("dashboard-subtitle");
        TextField searchField = new TextField();
        searchField.setPromptText("Search my scans");
        searchField.getStyleClass().addAll("box-id-field", "compact-search-field");

        HBox contentRow = new HBox(14, buildMyScansTablePanel(searchField), buildMyScansSideColumn());
        HBox.setHgrow(contentRow.getChildren().get(0), Priority.ALWAYS);

        body.getChildren().addAll(heading, subtitle, searchField, contentRow);
        return body;
    }

    public VBox buildScanProgressBody(UserPortalModel.ScanProfile selectedProfile,
                                      String currentBoxId,
                                      double scanProgress,
                                      Runnable onCancelScan) {
        VBox body = new VBox();
        body.getStyleClass().addAll("dashboard-body", "scan-flow-body");

        VBox card = new VBox(14);
        card.getStyleClass().add("scan-flow-card");

        VBox hero = new VBox(8);
        hero.setAlignment(Pos.CENTER);
        Label title = new Label("Scanning in Progress");
        title.getStyleClass().add("scan-flow-title");
        Label subtitle = new Label("Please wait while we process your documents");
        subtitle.getStyleClass().add("scan-flow-subtitle");
        hero.getChildren().addAll(components.buildProgressRing(scanProgress, Math.round(scanProgress) + "%"), title, subtitle);

        VBox steps = new VBox(8,
                components.scanProgressStep("Capturing pages", controller.scanStepState(scanProgress, 0)),
                components.scanProgressStep("Applying profile settings", controller.scanStepState(scanProgress, 28)),
                components.scanProgressStep("Processing", controller.scanStepState(scanProgress, 56)),
                components.scanProgressStep("Finalizing", controller.scanStepState(scanProgress, 82))
        );

        GridPane details = new GridPane();
        details.getStyleClass().add("scan-flow-info");
        details.setHgap(18);
        details.setVgap(8);
        details.add(components.scanInfoLabel("Box ID:"), 0, 0);
        details.add(components.scanInfoValue(currentBoxId), 1, 0);
        details.add(components.scanInfoLabel("Profile:"), 0, 1);
        details.add(components.scanInfoValue(controller.scanProfileName(selectedProfile)), 1, 1);
        details.add(components.scanInfoLabel("Started by:"), 0, 2);
        details.add(components.scanInfoValue("John Doe"), 1, 2);
        details.add(components.scanInfoLabel("Time started:"), 0, 3);
        details.add(components.scanInfoValue("2:45 PM"), 1, 3);
        details.getColumnConstraints().addAll(components.column(150), components.column(300));

        Button cancelButton = new Button("x  Cancel Scan");
        cancelButton.getStyleClass().add("scan-flow-secondary-button");
        cancelButton.setMaxWidth(Double.MAX_VALUE);
        cancelButton.setOnAction(event -> onCancelScan.run());

        card.getChildren().addAll(hero, steps, details, cancelButton);
        body.getChildren().add(card);
        return body;
    }

    public VBox buildScanCompleteBody(UserPortalModel.ScanProfile selectedProfile,
                                      String currentBoxId,
                                      Runnable onReviewAndExport,
                                      Runnable onStartAnotherScan) {
        VBox body = new VBox();
        body.getStyleClass().addAll("dashboard-body", "scan-flow-body");

        VBox card = new VBox(14);
        card.getStyleClass().add("scan-flow-card");

        VBox hero = new VBox(8);
        hero.setAlignment(Pos.CENTER);
        Label title = new Label("Scan Completed Successfully!");
        title.getStyleClass().add("scan-flow-title");
        Label subtitle = new Label("Your documents have been processed and are ready for review");
        subtitle.getStyleClass().add("scan-flow-subtitle");
        hero.getChildren().addAll(components.buildSuccessBadge(), title, subtitle);

        GridPane summary = new GridPane();
        summary.getStyleClass().add("scan-flow-summary");
        summary.setHgap(40);
        summary.setVgap(8);
        Label summaryTitle = new Label("Scan Summary");
        summaryTitle.getStyleClass().add("scan-flow-summary-title");
        summary.add(summaryTitle, 0, 0, 2, 1);
        summary.add(components.infoBlock("Profile Used", controller.scanProfileName(selectedProfile)), 0, 1);
        summary.add(components.infoBlock("Box ID", currentBoxId), 1, 1);
        summary.add(components.infoBlock("Pages Scanned", "156 pages"), 0, 2);
        summary.add(components.infoBlock("Time Completed", "2:52 PM"), 1, 2);
        summary.getColumnConstraints().addAll(components.column(220), components.column(220));

        Button reviewButton = new Button("Review → QA → Export");
        reviewButton.getStyleClass().add("scan-flow-primary-button");
        reviewButton.setMaxWidth(Double.MAX_VALUE);
        reviewButton.setOnAction(event -> onReviewAndExport.run());

        Button detailsButton = new Button("View Scan Details");
        detailsButton.getStyleClass().add("scan-flow-secondary-button");
        detailsButton.setMaxWidth(Double.MAX_VALUE);
        detailsButton.setOnAction(event -> onReviewAndExport.run());

        Button anotherButton = new Button("Start Another Scan");
        anotherButton.getStyleClass().add("scan-flow-secondary-button");
        anotherButton.setMaxWidth(Double.MAX_VALUE);
        anotherButton.setOnAction(event -> onStartAnotherScan.run());

        card.getChildren().addAll(hero, summary, reviewButton, detailsButton, anotherButton);
        body.getChildren().add(card);
        return body;
    }

    private VBox buildProfileCard(UserPortalModel.ScanProfile profile,
                                  boolean selected,
                                  Consumer<UserPortalModel.ScanProfile> onSelectProfile) {
        Label title = new Label(profile.title());
        title.getStyleClass().add("profile-card-title");

        Label description = new Label(profile.description());
        description.getStyleClass().add("profile-card-description");

        VBox card = new VBox(5, title, description);
        card.getStyleClass().add(selected ? "profile-select-card-active" : "profile-select-card");
        card.setOnMouseClicked(event -> onSelectProfile.accept(profile));
        return card;
    }

    private VBox buildMyScansTablePanel(TextField searchField) {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("panel-card");
        HBox.setHgrow(panel, Priority.ALWAYS);

        Label title = new Label("My Activity");
        title.getStyleClass().add("panel-title");
        VBox header = new VBox(title);
        header.setPadding(new Insets(18, 22, 18, 22));

        GridPane table = new GridPane();
        table.getStyleClass().add("recent-table");
        table.setMaxWidth(Double.MAX_VALUE);
        table.getColumnConstraints().addAll(
                components.column(130),
                components.column(130),
                components.column(145),
                components.column(150),
                components.column(150),
                components.column(120),
                components.column(90)
        );

        table.add(components.headerCell("Action"), 0, 0);
        table.add(components.headerCell("File"), 1, 0);
        table.add(components.headerCell("Document"), 2, 0);
        table.add(components.headerCell("Profile"), 3, 0);
        table.add(components.headerCell("Box ID"), 4, 0);
        table.add(components.headerCell("Date"), 5, 0);
        table.add(components.headerCell("Time"), 6, 0);

        Runnable refreshTable = () -> {
            while (table.getChildren().size() > 7) {
                table.getChildren().remove(table.getChildren().size() - 1);
            }
            String search = controller.normalizeSearch(searchField.getText());
            int row = 1;
            for (UserPortalModel.ActivityLogEntry entry : portalModel.myScanActivity()) {
                if (!controller.matchesSearch(search,
                        entry.action(),
                        entry.file(),
                        entry.document(),
                        entry.profile(),
                        entry.boxId(),
                        entry.date(),
                        entry.time())) {
                    continue;
                }
                table.add(components.bodyCell(entry.action()), 0, row);
                table.add(components.bodyCell(entry.file()), 1, row);
                table.add(components.bodyCell(entry.document()), 2, row);
                table.add(components.bodyCell(entry.profile()), 3, row);
                table.add(components.bodyCell(entry.boxId()), 4, row);
                table.add(components.bodyCell(entry.date()), 5, row);
                table.add(components.bodyCell(entry.time()), 6, row);
                row++;
            }
        };
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshTable.run());
        refreshTable.run();

        panel.getChildren().addAll(header, table);
        return panel;
    }

    private VBox buildMyScansSideColumn() {
        List<UserPortalModel.ActivityLogEntry> myActivity = portalModel.myScanActivity();
        long exportCount = myActivity.stream().filter(entry -> "Exported".equals(entry.action())).count();
        long qaCount = myActivity.stream().filter(entry -> entry.action().contains("QA")).count();

        VBox column = new VBox(18);
        column.setPrefWidth(220);
        column.setMinWidth(220);
        column.setMaxWidth(220);

        VBox summaryPanel = new VBox(16);
        summaryPanel.getStyleClass().add("panel-card");
        summaryPanel.setPadding(new Insets(18));
        Label summaryTitle = new Label("My Summary");
        summaryTitle.getStyleClass().add("panel-title");
        summaryPanel.getChildren().addAll(
                summaryTitle,
                components.infoBlock("My Log Entries", String.valueOf(myActivity.size())),
                components.infoBlock("Exports", String.valueOf(exportCount)),
                components.infoBlock("QA Actions", String.valueOf(qaCount))
        );

        column.getChildren().add(summaryPanel);
        return column;
    }

    private VBox buildActionCard(UserPortalModel.ActionCard card, Consumer<PageState> onNavigate) {
        VBox panel = new VBox(14);
        panel.getStyleClass().add("dashboard-card");
        HBox.setHgrow(panel, Priority.ALWAYS);
        panel.setMaxWidth(Double.MAX_VALUE);
        panel.setOnMouseClicked(event -> onNavigate.accept(controller.actionCardTarget(card.title())));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane badge = components.buildActionIconBadge(card);

        Label title = new Label(card.title());
        title.getStyleClass().add("dashboard-card-title");
        Label subtitle = new Label(card.subtitle());
        subtitle.getStyleClass().add("dashboard-card-subtitle");
        subtitle.setWrapText(true);

        header.getChildren().addAll(badge, title);
        panel.getChildren().addAll(header, subtitle);
        return panel;
    }

    private VBox buildRecentScansPanel() {
        VBox panel = new VBox(0);
        panel.getStyleClass().addAll("panel-card", "recent-scans-panel");
        HBox.setHgrow(panel, Priority.ALWAYS);

        Label title = new Label("Recent Scans");
        title.getStyleClass().add("panel-title");
        VBox header = new VBox(title);
        header.setPadding(new Insets(18, 22, 18, 22));

        GridPane table = new GridPane();
        table.getStyleClass().add("recent-table");
        table.setMaxWidth(Double.MAX_VALUE);
        table.getColumnConstraints().addAll(
                components.column(190),
                components.column(180),
                components.column(160),
                components.column(150),
                components.column(90)
        );

        table.add(components.headerCell("Box ID"), 0, 0);
        table.add(components.headerCell("Profile"), 1, 0);
        table.add(components.headerCell("Status"), 2, 0);
        table.add(components.headerCell("Date"), 3, 0);
        table.add(components.headerCell("Pages"), 4, 0);

        int row = 1;
        for (UserPortalModel.ScanRow scan : portalModel.recentScans()) {
            table.add(components.bodyCell(scan.boxId()), 0, row);
            table.add(components.bodyCell(scan.profile()), 1, row);
            table.add(components.statusCell(scan.status()), 2, row);
            table.add(components.bodyCell(scan.date()), 3, row);
            table.add(components.bodyCell(String.valueOf(scan.pages())), 4, row);
            row++;
        }

        panel.getChildren().addAll(header, table);
        return panel;
    }

    private VBox buildRightColumn() {
        VBox column = new VBox(14, buildActiveProfilePanel(), buildExportNamingPanel());
        column.setPrefWidth(320);
        column.setMaxWidth(320);
        return column;
    }

    private VBox buildActiveProfilePanel() {
        UserPortalModel.ProfileSummary profile = portalModel.activeProfile();

        VBox panel = new VBox(18);
        panel.getStyleClass().addAll("panel-card", "active-profile-panel");
        panel.setPadding(new Insets(22));

        Label title = new Label("Active Profile");
        title.getStyleClass().add("panel-title");

        panel.getChildren().addAll(
                title,
                components.infoBlock("Profile Name", profile.profileName()),
                components.infoBlock("Auto-Rotate", profile.autoRotate()),
                components.infoBlock("Brightness", profile.brightness()),
                components.infoBlock("Remove Blank Pages", profile.removeBlankPages())
        );
        return panel;
    }

    private VBox buildExportNamingPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("export-naming-panel");
        panel.setPadding(new Insets(18));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("!");
        icon.getStyleClass().add("export-naming-icon");
        Label title = new Label("Export Naming");
        title.getStyleClass().add("export-naming-title");
        header.getChildren().addAll(icon, title);

        Label formatLabel = new Label("Format:");
        formatLabel.getStyleClass().add("info-panel-text");
        Label token = new Label("{profileName}_{boxId}");
        token.getStyleClass().add("info-token");

        VBox format = new VBox(8, formatLabel, token);
        format.setPadding(new Insets(0, 0, 0, 36));
        panel.getChildren().addAll(header, format);
        return panel;
    }
}
