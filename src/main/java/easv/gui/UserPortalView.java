package easv.gui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.util.List;

public class UserPortalView {
    private enum Page {
        DASHBOARD("Dashboard"),
        START_SCAN("Start Scan"),
        SCANNING("Start Scan"),
        COMPLETED("My Scans"),
        REVIEW_EXPORT("My Scans"),
        EXPORTS("Exports"),
        MY_SCANS("My Scans"),
        SETTINGS("Settings"),
        HELP("Help");

        private final String navLabel;

        Page(String navLabel) {
            this.navLabel = navLabel;
        }

        public String navLabel() {
            return navLabel;
        }
    }

    private final UserPortalModel portalModel;
    private final BorderPane root = new BorderPane();
    private final ScrollPane scrollPane = new ScrollPane();
    private final VBox content = new VBox(24);
    private SidebarNav sidebar;
    private Page activePage = Page.DASHBOARD;
    private UserPortalModel.PortalSession activeSession;

    public UserPortalView(UserPortalModel portalModel) {
        this.portalModel = portalModel;
    }

    public Parent create() {
        sidebar = new SidebarNav(activePage.navLabel(), this::onNavigate);

        content.getStyleClass().addAll("dashboard-content", "portal-main-content");
        scrollPane.getStyleClass().addAll("transparent-scroll", "portal-main-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setContent(content);

        root.getStyleClass().addAll("page", "dashboard-page", "portal-shell");
        root.setLeft(sidebar);
        root.setCenter(scrollPane);
        BorderPane.setMargin(sidebar, new Insets(10, 0, 10, 10));
        BorderPane.setMargin(scrollPane, new Insets(10, 10, 10, 0));

        renderPage();
        return root;
    }

    private void onNavigate(String item) {
        activePage = switch (item) {
            case "Start Scan" -> activeSession == null ? Page.START_SCAN : Page.SCANNING;
            case "My Scans" -> Page.MY_SCANS;
            case "Exports" -> Page.EXPORTS;
            case "Settings" -> Page.SETTINGS;
            case "Help" -> Page.HELP;
            default -> Page.DASHBOARD;
        };
        renderPage();
    }

    private void renderPage() {
        sidebar.setActiveItem(activePage.navLabel());
        content.getChildren().setAll(
                new DashboardHeader(),
                buildPage(activePage)
        );
        scrollPane.setVvalue(0);
    }

    private Node buildPage(Page page) {
        return switch (page) {
            case START_SCAN -> buildStartScanPage();
            case SCANNING -> buildScanningPage();
            case COMPLETED -> buildCompletedPage();
            case REVIEW_EXPORT -> buildReviewExportPage();
            case EXPORTS -> buildExportsPage();
            case MY_SCANS -> buildMyScansPage();
            case SETTINGS -> buildSettingsPage();
            case HELP -> buildHelpPage();
            default -> buildDashboardPage();
        };
    }

    private Node buildDashboardPage() {
        Label title = new Label("User Dashboard");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Start a new scan or continue from your recent activity.");
        subtitle.getStyleClass().add("muted");

        ActionCard startCard = new ActionCard("scan", "Start a New Scan", "Begin a new scanning session", "Start Scan", true,
                () -> {
                    activeSession = null;
                    switchPage(Page.START_SCAN);
                });
        ActionCard scansCard = new ActionCard("scans", "My Scans", "View your scanning history", "View Scans", false,
                () -> switchPage(Page.MY_SCANS));
        ActionCard exportsCard = new ActionCard("exports", "Exports", "Download your exported files", "View Exports", false,
                () -> switchPage(Page.EXPORTS));

        HBox actions = new HBox(16, startCard, scansCard, exportsCard);
        actions.getStyleClass().add("portal-action-grid");
        HBox.setHgrow(startCard, Priority.ALWAYS);
        HBox.setHgrow(scansCard, Priority.ALWAYS);
        HBox.setHgrow(exportsCard, Priority.ALWAYS);
        startCard.setMaxWidth(Double.MAX_VALUE);
        scansCard.setMaxWidth(Double.MAX_VALUE);
        exportsCard.setMaxWidth(Double.MAX_VALUE);

        RecentScansTable recentScansTable = new RecentScansTable(
                portalModel.fetchRecentScans(),
                item -> switchPage(Page.EXPORTS),
                item -> {
                    activeSession = portalModel.resumeSession(item);
                    switchPage(Page.START_SCAN);
                },
                () -> switchPage(Page.MY_SCANS)
        );

        UserPortalModel.ProfileItem profile = portalModel.getDefaultProfileForUser();
        VBox profileCard = new VBox(new ProfileSummaryCard("Active Profile", profile, portalModel.fetchProfileSettings(profile), () -> switchPage(Page.SETTINGS)));
        profileCard.setPrefWidth(330);
        recentScansTable.setPrefWidth(700);

        HBox lower = new HBox(18, recentScansTable, profileCard);
        HBox.setHgrow(recentScansTable, Priority.ALWAYS);
        lower.getStyleClass().add("portal-flex-row");

        VBox page = new VBox(24,
                pageIntro(title, subtitle),
                actions,
                lower,
                buildInfoBanner("Exports are saved using the format: {profileName}_{boxId}",
                        portalModel.formatExportName(profile.name(), "BOX-2026-001"))
        );
        page.getStyleClass().add("portal-page-section");
        return page;
    }

    private Node buildStartScanPage() {
        List<UserPortalModel.ProfileItem> profiles = portalModel.fetchProfilesForUser();
        UserPortalModel.ProfileItem defaultProfile = activeSession == null ? portalModel.getDefaultProfileForUser() : activeSession.profile();

        Label title = new Label("Start a New Scan");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Select a profile to configure how your documents will be processed during scanning.");
        subtitle.getStyleClass().add("muted");

        ComboBox<UserPortalModel.ProfileItem> profileBox = new ComboBox<>(FXCollections.observableArrayList(profiles));
        profileBox.getSelectionModel().select(defaultProfile);
        profileBox.getStyleClass().add("dashboard-combo-box");
        profileBox.setMaxWidth(Double.MAX_VALUE);

        TextField boxField = new TextField(activeSession == null ? "BOX-2026-001" : activeSession.box().id());
        boxField.getStyleClass().add("dashboard-text-field");
        boxField.setPromptText("BOX-2026-001");

        Label validation = new Label();
        validation.getStyleClass().add("dashboard-validation");
        validation.setVisible(false);
        validation.setManaged(false);

        Label selectedName = new Label(defaultProfile.name());
        selectedName.getStyleClass().add("profile-select-name");
        Label selectedDescription = new Label(defaultProfile.description());
        selectedDescription.getStyleClass().add("dashboard-card-body");
        selectedDescription.setWrapText(true);
        Label exportHint = new Label();
        exportHint.getStyleClass().add("muted");

        Runnable syncProfile = () -> {
            UserPortalModel.ProfileItem selected = profileBox.getValue();
            if (selected == null) {
                return;
            }
            selectedName.setText(selected.name());
            selectedDescription.setText(selected.description());
            exportHint.setText("Example file: " + portalModel.formatExportName(selected.name(), normalizedBoxId(boxField.getText())));
        };
        syncProfile.run();
        profileBox.valueProperty().addListener((obs, oldValue, newValue) -> syncProfile.run());
        boxField.textProperty().addListener((obs, oldValue, newValue) -> syncProfile.run());

        Button startButton = new Button("Start Scanning ->");
        startButton.getStyleClass().add("dashboard-primary-button");
        startButton.setOnAction(event -> {
            String boxId = normalizedBoxId(boxField.getText());
            if (profileBox.getValue() == null || !portalModel.isValidBoxId(boxId)) {
                validation.setText("Enter a valid Box ID in the format BOX-2026-001.");
                validation.setVisible(true);
                validation.setManaged(true);
                return;
            }
            validation.setVisible(false);
            validation.setManaged(false);
            activeSession = portalModel.startSession(profileBox.getValue(), boxId);
            switchPage(Page.SCANNING);
        });

        VBox profileColumn = new VBox(10,
                formLabel("1. Select Profile"),
                buildProfileSelectionCard(profileBox, selectedName, selectedDescription)
        );
        profileColumn.setPrefWidth(360);

        VBox boxColumn = new VBox(10,
                formLabel("2. Enter Box ID"),
                boxField,
                mutedLabel("Enter the ID of the box you are scanning")
        );
        boxColumn.setPrefWidth(230);

        VBox illustration = buildBoxIllustration();
        illustration.setPrefWidth(180);

        HBox top = new HBox(18, profileColumn, boxColumn, illustration);
        top.getStyleClass().addAll("portal-flex-row", "start-scan-row");

        VBox actionBlock = new VBox(6, startButton, mutedLabel("You can review before export"));
        actionBlock.getStyleClass().add("start-scan-action-block");
        HBox actionRow = new HBox(exportHint, spacer(), actionBlock);
        actionRow.setAlignment(Pos.BOTTOM_RIGHT);

        VBox page = new VBox(24,
                pageIntro(title, subtitle),
                top,
                actionRow,
                validationBanner(validation),
                buildInfoBanner("Exports are saved using the format: {profileName}_{boxId}",
                        portalModel.formatExportName(profileBox.getValue().name(), normalizedBoxId(boxField.getText())))
        );
        page.getStyleClass().add("portal-page-section");
        return page;
    }

    private Node buildScanningPage() {
        if (activeSession == null) {
            activeSession = portalModel.startSession(portalModel.getDefaultProfileForUser(), "BOX-2026-001");
        }
        UserPortalModel.ScanProgress progress = portalModel.fetchScanProgress(activeSession);

        Label title = new Label("Scanning in Progress");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Please do not close or refresh this page.");
        subtitle.getStyleClass().add("muted");

        VBox details = card(new VBox(12,
                sectionTitle("Scan Details"),
                kvRow("Profile", badgeValue(progress.profileName(), "default-badge")),
                kvRow("Box ID", strongLabel(progress.boxId())),
                kvRow("Started", strongLabel(progress.startedAt())),
                kvRow("Started by", strongLabel(progress.startedBy()))
        ));
        details.setPrefWidth(360);

        VBox progressCard = card(new VBox(18,
                sectionTitle("Progress"),
                buildProgressHeader(progress.progressPercent(), progress.pagesScanned(), progress.totalPages()),
                buildProgressChecklist(progress.steps())
        ));
        HBox panels = new HBox(18, details, progressCard);
        panels.getStyleClass().add("portal-flex-row");
        HBox.setHgrow(progressCard, Priority.ALWAYS);

        Button cancel = new Button("Cancel Scan");
        cancel.getStyleClass().add("dashboard-danger-button");
        cancel.setOnAction(event -> {
            activeSession = null;
            switchPage(Page.START_SCAN);
        });

        HBox actions = new HBox(12, cancel);
        VBox page = new VBox(24, pageIntro(title, subtitle), panels, actions);
        page.getStyleClass().add("portal-page-section");
        return page;
    }

    private Node buildCompletedPage() {
        UserPortalModel.ScanSummary summary = portalModel.fetchCompletedSummary(activeSession);

        Label title = new Label("Scan Completed");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Your scan has completed successfully.");
        subtitle.getStyleClass().add("muted");

        VBox summaryCard = card(new VBox(12,
                sectionTitle("Scan Summary"),
                kvRow("Profile", badgeValue(summary.profileName(), "default-badge")),
                kvRow("Box ID", strongLabel(summary.boxId())),
                kvRow("Started", strongLabel(summary.startedAt())),
                kvRow("Completed", strongLabel(summary.completedAt())),
                kvRow("Total Pages", strongLabel(String.valueOf(summary.totalPages()))),
                kvRow("Status", new StatusBadge(summary.status()))
        ));
        summaryCard.setPrefWidth(420);

        Button reviewButton = new Button("Review & Export");
        reviewButton.getStyleClass().add("dashboard-primary-button");
        reviewButton.setOnAction(event -> switchPage(Page.REVIEW_EXPORT));

        Button detailsButton = new Button("View Scan Details");
        detailsButton.getStyleClass().add("dashboard-outline-button");
        detailsButton.setOnAction(event -> switchPage(Page.MY_SCANS));

        Button startAnother = new Button("Start Another Scan");
        startAnother.getStyleClass().add("dashboard-outline-button");
        startAnother.setOnAction(event -> {
            activeSession = null;
            switchPage(Page.START_SCAN);
        });

        VBox nextCard = card(new VBox(12,
                sectionTitle("What's Next?"),
                reviewButton,
                detailsButton,
                startAnother
        ));
        nextCard.setPrefWidth(320);

        HBox panels = new HBox(18, summaryCard, nextCard);
        panels.getStyleClass().add("portal-flex-row");

        VBox page = new VBox(24,
                pageIntro(title, subtitle),
                panels,
                buildInfoBanner("Exports are saved using the format: {profileName}_{boxId}",
                        portalModel.formatExportName(summary.profileName(), summary.boxId()))
        );
        page.getStyleClass().add("portal-page-section");
        return page;
    }

    private Node buildReviewExportPage() {
        UserPortalModel.ScanSummary summary = portalModel.fetchCompletedSummary(activeSession);

        Label title = new Label("Review & Export");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Review your scan and export when ready.");
        subtitle.getStyleClass().add("muted");

        VBox preview = card(new VBox(16,
                sectionTitle("Preview"),
                buildPreviewPane(),
                buildPreviewControls()
        ));
        HBox.setHgrow(preview, Priority.ALWAYS);

        ComboBox<String> formatBox = comboBox("PDF", "TIFF");
        ComboBox<String> qualityBox = comboBox("High", "Medium", "Low");
        CheckBox ocrCheck = new CheckBox("Create OCR Text Layer");
        ocrCheck.setSelected(true);

        Button exportButton = new Button("Export File");
        exportButton.getStyleClass().add("dashboard-primary-button");
        exportButton.setMaxWidth(Double.MAX_VALUE);
        exportButton.setOnAction(event -> switchPage(Page.EXPORTS));

        VBox settings = card(new VBox(12,
                sectionTitle("Export Settings"),
                kvRow("Profile", badgeValue(summary.profileName(), "default-badge")),
                kvRow("Box ID", strongLabel(summary.boxId())),
                formField("Export Format", formatBox),
                formField("Image Quality", qualityBox),
                ocrCheck,
                exportButton
        ));
        settings.setPrefWidth(360);

        HBox panels = new HBox(18, preview, settings);
        panels.getStyleClass().add("portal-flex-row");

        VBox page = new VBox(24,
                pageIntro(title, subtitle),
                panels,
                buildInfoBanner("Exports are saved using the format: {profileName}_{boxId}",
                        portalModel.formatExportName(summary.profileName(), summary.boxId()))
        );
        page.getStyleClass().add("portal-page-section");
        return page;
    }

    private Node buildExportsPage() {
        Label title = new Label("Exports");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Download your exported files.");
        subtitle.getStyleClass().add("muted");

        TextField search = new TextField();
        search.setPromptText("Search exports by box ID or profile...");
        search.getStyleClass().add("dashboard-text-field");
        ComboBox<String> filter = comboBox("All Status", "Ready", "Failed", "Processing");

        HBox filters = new HBox(12, search, filter);
        HBox.setHgrow(search, Priority.ALWAYS);

        TableView<UserPortalModel.ExportItem> table = new TableView<>();
        table.getStyleClass().add("portal-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        FilteredList<UserPortalModel.ExportItem> exports = new FilteredList<>(FXCollections.observableArrayList(portalModel.fetchExports()), item -> true);
        table.setItems(exports);
        table.getColumns().add(exportTextColumn("File Name", UserPortalModel.ExportItem::fileName));
        table.getColumns().add(exportTextColumn("Box ID", UserPortalModel.ExportItem::boxId));
        table.getColumns().add(exportTextColumn("Profile", UserPortalModel.ExportItem::profileName));
        table.getColumns().add(exportTextColumn("Created", UserPortalModel.ExportItem::createdAt));
        table.getColumns().add(exportTextColumn("Size", UserPortalModel.ExportItem::size));
        table.getColumns().add(badgeColumn("Status", UserPortalModel.ExportItem::status));
        table.getColumns().add(iconColumn("Actions", "DL"));
        Runnable applyExportFilter = () -> {
            String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            String status = filter.getValue();
            exports.setPredicate(item -> {
                boolean matchesQuery = query.isBlank()
                        || item.fileName().toLowerCase().contains(query)
                        || item.boxId().toLowerCase().contains(query)
                        || item.profileName().toLowerCase().contains(query);
                boolean matchesStatus = status == null || "All Status".equals(status) || item.status().equalsIgnoreCase(status);
                return matchesQuery && matchesStatus;
            });
        };
        search.textProperty().addListener((obs, oldValue, newValue) -> applyExportFilter.run());
        filter.valueProperty().addListener((obs, oldValue, newValue) -> applyExportFilter.run());
        applyExportFilter.run();

        VBox page = new VBox(24, pageIntro(title, subtitle), card(new VBox(16, filters, table, buildTableFooter("Showing 1 to 4 of 4 results"))));
        page.getStyleClass().add("portal-page-section");
        return page;
    }

    private Node buildMyScansPage() {
        Label title = new Label("My Scans");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("View and manage your scanning history.");
        subtitle.getStyleClass().add("muted");

        TextField search = new TextField();
        search.setPromptText("Search scans by box ID or profile...");
        search.getStyleClass().add("dashboard-text-field");
        ComboBox<String> filter = comboBox("All Status", "Completed", "In Progress", "Failed");
        HBox filters = new HBox(12, search, filter);
        HBox.setHgrow(search, Priority.ALWAYS);

        TableView<UserPortalModel.HistoryItem> table = new TableView<>();
        table.getStyleClass().add("portal-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        FilteredList<UserPortalModel.HistoryItem> history = new FilteredList<>(FXCollections.observableArrayList(portalModel.fetchScanHistory()), item -> true);
        table.setItems(history);
        table.getColumns().add(historyTextColumn("Box ID", UserPortalModel.HistoryItem::boxId));
        table.getColumns().add(historyTextColumn("Profile", UserPortalModel.HistoryItem::profileName));
        table.getColumns().add(badgeColumn("Status", UserPortalModel.HistoryItem::status));
        table.getColumns().add(historyTextColumn("Started", UserPortalModel.HistoryItem::startedAt));
        table.getColumns().add(historyTextColumn("Completed", UserPortalModel.HistoryItem::completedAt));
        table.getColumns().add(historyTextColumn("Pages", item -> String.valueOf(item.pages())));
        table.getColumns().add(iconColumn("Export", "DL"));
        Runnable applyHistoryFilter = () -> {
            String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            String status = filter.getValue();
            history.setPredicate(item -> {
                boolean matchesQuery = query.isBlank()
                        || item.boxId().toLowerCase().contains(query)
                        || item.profileName().toLowerCase().contains(query);
                boolean matchesStatus = status == null || "All Status".equals(status) || item.status().equalsIgnoreCase(status);
                return matchesQuery && matchesStatus;
            });
        };
        search.textProperty().addListener((obs, oldValue, newValue) -> applyHistoryFilter.run());
        filter.valueProperty().addListener((obs, oldValue, newValue) -> applyHistoryFilter.run());
        applyHistoryFilter.run();
        table.setOnMouseClicked(event -> {
            UserPortalModel.HistoryItem selected = table.getSelectionModel().getSelectedItem();
            if (selected != null && "In Progress".equals(selected.status())) {
                activeSession = portalModel.resumeSession(selected);
                switchPage(Page.SCANNING);
            } else if (selected != null && "Completed".equals(selected.status())) {
                activeSession = portalModel.resumeSession(selected);
                switchPage(Page.COMPLETED);
            }
        });

        VBox page = new VBox(24, pageIntro(title, subtitle), card(new VBox(16, filters, table, buildTableFooter("Showing 1 to 5 of 5 results"))));
        page.getStyleClass().add("portal-page-section");
        return page;
    }

    private Node buildSettingsPage() {
        UserPortalModel.ProfileItem profile = portalModel.getDefaultProfileForUser();

        Label crumb = new Label("Settings  >  Profile Information");
        crumb.getStyleClass().add("dashboard-header-label");
        Label title = new Label("Profile Information");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("View details about the current active profile.");
        subtitle.getStyleClass().add("muted");

        GridPane details = new GridPane();
        details.setHgap(20);
        details.setVgap(14);
        addGridRow(details, 0, "Profile Name", badgeValue(profile.name(), "default-badge"));
        addGridRow(details, 1, "Description", wrapValue(profile.description()));
        addGridRow(details, 2, "Created By", strongLabel("Admin User"));
        addGridRow(details, 3, "Created On", strongLabel("Apr 10, 2024"));
        addGridRow(details, 4, "Last Updated", strongLabel("Apr 15, 2024"));

        VBox detailsCard = card(new VBox(16, sectionTitle("Profile Details"), details));
        HBox.setHgrow(detailsCard, Priority.ALWAYS);

        VBox settingsList = new VBox(10);
        for (UserPortalModel.ProfileSetting setting : portalModel.fetchProfileSettings(profile)) {
            settingsList.getChildren().add(kvRow(setting.label(), strongLabel(setting.value())));
        }
        settingsList.getChildren().add(kvRow("Deskew", strongLabel("On")));
        settingsList.getChildren().add(kvRow("Contrast", strongLabel("+5%")));
        settingsList.getChildren().add(kvRow("OCR", strongLabel("On")));
        Label link = new Label("View all profile settings ->");
        link.getStyleClass().add("dashboard-link");

        VBox settingsCard = card(new VBox(16, sectionTitle("Profile Settings"), settingsList, link));
        settingsCard.setPrefWidth(360);

        HBox pageRow = new HBox(18, detailsCard, settingsCard);
        pageRow.getStyleClass().add("portal-flex-row");

        VBox page = new VBox(24, crumb, pageIntro(title, subtitle), pageRow);
        page.getStyleClass().add("portal-page-section");
        return page;
    }

    private Node buildHelpPage() {
        Label title = new Label("Help & Support");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Find answers and get support.");
        subtitle.getStyleClass().add("muted");

        VBox topics = new VBox(0);
        for (String topic : portalModel.fetchHelpTopics()) {
            Button row = new Button(topic + "    >");
            row.getStyleClass().add("help-row-button");
            row.setMaxWidth(Double.MAX_VALUE);
            row.setAlignment(Pos.CENTER_LEFT);
            topics.getChildren().add(row);
        }
        VBox gettingStarted = card(new VBox(14, sectionTitle("Getting Started"), topics));
        HBox.setHgrow(gettingStarted, Priority.ALWAYS);

        VBox contact = card(new VBox(12,
                sectionTitle("Need More Help?"),
                mutedLabel("Can't find what you're looking for?"),
                pillButton("Contact Support"),
                strongLabel("support@scanningportal.com"),
                strongLabel("(555) 123-0507"),
                mutedLabel("Mon - Fri, 9:00 AM - 5:00 PM")
        ));
        contact.setPrefWidth(320);

        HBox top = new HBox(18, gettingStarted, contact);
        top.getStyleClass().add("portal-flex-row");

        VBox system = card(new VBox(14,
                sectionTitle("System Information"),
                kvRow("Version", strongLabel("1.0.0")),
                kvRow("Last Updated", strongLabel("Apr 24, 2024"))
        ));

        VBox page = new VBox(24, pageIntro(title, subtitle), top, system);
        page.getStyleClass().add("portal-page-section");
        return page;
    }

    private void switchPage(Page page) {
        if (page == Page.START_SCAN && activeSession != null && activePage == Page.SCANNING) {
            activeSession = null;
        }
        activePage = page;
        renderPage();
    }

    private VBox buildProfileSelectionCard(ComboBox<UserPortalModel.ProfileItem> profileBox, Label name, Label description) {
        Label badge = new Label("Default");
        badge.getStyleClass().add("default-badge");
        Label details = new Label("View details");
        details.getStyleClass().add("dashboard-link");
        details.setOnMouseClicked(event -> switchPage(Page.SETTINGS));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox nameRow = new HBox(8, name, badge, spacer);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10, profileBox, nameRow, description, details);
        card.getStyleClass().add("profile-select-card");
        return card;
    }

    private VBox buildBoxIllustration() {
        Rectangle box = new Rectangle(154, 112, Color.web("#f7fbfa"));
        box.setArcWidth(18);
        box.setArcHeight(18);
        box.setStroke(Color.web("#cde2dc"));
        Rectangle flap = new Rectangle(84, 44, Color.web("#ffffff"));
        flap.setArcWidth(14);
        flap.setArcHeight(14);
        flap.setRotate(-18);
        flap.setTranslateY(-32);
        flap.setStroke(Color.web("#cde2dc"));
        Text spark = new Text("*");
        spark.getStyleClass().add("illustration-spark");
        spark.setTranslateX(70);
        spark.setTranslateY(-48);
        StackPane art = new StackPane(box, flap, spark);
        art.getStyleClass().add("scan-illustration");

        VBox card = new VBox(14, art, mutedLabel("Assigned profiles control processing rules, not document categories."));
        card.getStyleClass().add("illustration-panel");
        card.setAlignment(Pos.CENTER);
        return card;
    }

    private Node buildProgressHeader(int percent, int currentPage, int totalPages) {
        StackPane ring = progressRing(percent);
        VBox text = new VBox(4,
                strongLabel("Scanning"),
                mutedLabel("Page " + currentPage + " of " + totalPages)
        );
        text.setAlignment(Pos.CENTER_LEFT);
        HBox row = new HBox(18, ring, text);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private StackPane progressRing(int percent) {
        Circle base = new Circle(52);
        base.setFill(Color.TRANSPARENT);
        base.setStroke(Color.web("#e4efec"));
        base.setStrokeWidth(10);

        Arc arc = new Arc(0, 0, 52, 52, 90, -3.6 * percent);
        arc.setType(ArcType.OPEN);
        arc.setFill(Color.TRANSPARENT);
        arc.setStroke(Color.web("#0f8d81"));
        arc.setStrokeWidth(10);
        arc.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        Label value = new Label(percent + "%");
        value.getStyleClass().add("progress-ring-value");
        return new StackPane(base, arc, value);
    }

    private VBox buildProgressChecklist(List<UserPortalModel.ProgressStep> steps) {
        VBox list = new VBox(12);
        for (UserPortalModel.ProgressStep step : steps) {
            Label bullet = new Label(step.marker());
            bullet.getStyleClass().addAll("progress-marker", step.stateStyle());
            Label text = new Label(step.label());
            text.getStyleClass().add("dashboard-card-body");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox row = new HBox(10, bullet, text, spacer);
            row.setAlignment(Pos.CENTER_LEFT);
            list.getChildren().add(row);
        }
        return list;
    }

    private Pane buildPreviewPane() {
        Rectangle page = new Rectangle(160, 220, Color.WHITE);
        page.setArcWidth(12);
        page.setArcHeight(12);
        page.setStroke(Color.web("#d8e4e0"));

        VBox lines = new VBox(8);
        lines.setPadding(new Insets(18));
        for (int i = 0; i < 12; i++) {
            Region line = new Region();
            line.getStyleClass().add(i % 3 == 0 ? "illustration-line-short" : "illustration-line");
            lines.getChildren().add(line);
        }
        StackPane preview = new StackPane(page, lines);
        preview.getStyleClass().add("preview-sheet");

        Button prev = iconCircleButton("<");
        Button next = iconCircleButton(">");
        HBox row = new HBox(20, prev, preview, next);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private Node buildPreviewControls() {
        HBox controls = new HBox(10,
                iconCircleButton("+"),
                iconCircleButton("-"),
                iconCircleButton("[]"),
                iconCircleButton("H")
        );
        controls.setAlignment(Pos.CENTER_LEFT);
        Label footer = new Label("Page 1 of 245");
        footer.getStyleClass().add("muted");
        VBox box = new VBox(10, controls, footer);
        return box;
    }

    private VBox buildInfoBanner(String title, String value) {
        Label icon = new Label("i");
        icon.getStyleClass().add("info-banner-icon");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("info-banner-title");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("info-banner-value");
        VBox copy = new VBox(6, titleLabel, valueLabel);
        HBox banner = new HBox(12, icon, copy);
        copy.setAlignment(Pos.CENTER_LEFT);
        banner.getStyleClass().add("info-banner");
        return new VBox(banner);
    }

    private TableColumn<UserPortalModel.ExportItem, String> exportTextColumn(String title, java.util.function.Function<UserPortalModel.ExportItem, String> mapper) {
        TableColumn<UserPortalModel.ExportItem, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(mapper.apply(data.getValue())));
        return column;
    }

    private TableColumn<UserPortalModel.HistoryItem, String> historyTextColumn(String title, java.util.function.Function<UserPortalModel.HistoryItem, String> mapper) {
        TableColumn<UserPortalModel.HistoryItem, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(mapper.apply(data.getValue())));
        return column;
    }

    private <T> TableColumn<T, T> badgeColumn(String title, java.util.function.Function<T, String> mapper) {
        TableColumn<T, T> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(new StatusBadge(mapper.apply(item)));
                }
                setText(null);
            }
        });
        return column;
    }

    private <T> TableColumn<T, String> iconColumn(String title, String icon) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(icon));
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label label = new Label(item);
                    label.getStyleClass().add("table-export-icon");
                    setGraphic(new StackPane(label));
                }
                setText(null);
            }
        });
        return column;
    }

    private ComboBox<String> comboBox(String... items) {
        ComboBox<String> box = new ComboBox<>(FXCollections.observableArrayList(items));
        box.getSelectionModel().selectFirst();
        box.getStyleClass().add("dashboard-combo-box");
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private VBox formField(String label, Node field) {
        return new VBox(6, mutedLabel(label), field);
    }

    private VBox pageIntro(Label title, Label subtitle) {
        VBox intro = new VBox(4, title, subtitle);
        intro.getStyleClass().add("page-intro");
        return intro;
    }

    private HBox kvRow(String label, Node value) {
        Label key = new Label(label);
        key.getStyleClass().add("profile-setting-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, key, spacer, value);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("profile-setting-row");
        return row;
    }

    private void addGridRow(GridPane grid, int row, String label, Node value) {
        Label key = new Label(label);
        key.getStyleClass().add("profile-setting-label");
        grid.add(key, 0, row);
        grid.add(value, 1, row);
    }

    private VBox card(Node child) {
        VBox box = new VBox(child);
        box.getStyleClass().add("dashboard-card");
        return box;
    }

    private VBox validationBanner(Label label) {
        VBox box = new VBox(label);
        box.setManaged(false);
        box.visibleProperty().bind(label.visibleProperty());
        box.managedProperty().bind(label.managedProperty());
        return box;
    }

    private Label formLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("dashboard-step-label");
        return label;
    }

    private Label mutedLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        label.setWrapText(true);
        return label;
    }

    private Label strongLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("profile-setting-value");
        return label;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private Node wrapValue(String text) {
        Label label = strongLabel(text);
        label.setWrapText(true);
        return label;
    }

    private HBox badgeValue(String text, String styleClass) {
        Label label = strongLabel(text);
        Label badge = new Label("Default");
        badge.getStyleClass().add(styleClass);
        HBox box = new HBox(8, label, badge);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Button iconCircleButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("icon-circle-button");
        return button;
    }

    private Button pillButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("dashboard-outline-button");
        return button;
    }

    private HBox buildTableFooter(String text) {
        Label label = mutedLabel(text);
        Button prev = iconCircleButton("<");
        Button next = iconCircleButton(">");
        prev.getStyleClass().add("table-pager-button");
        next.getStyleClass().add("table-pager-button");

        Label pageValue = new Label("1");
        pageValue.getStyleClass().add("table-page-value");
        HBox pager = new HBox(6, prev, pageValue, next);
        pager.setAlignment(Pos.CENTER_RIGHT);

        HBox footer = new HBox(label, spacer(), pager);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("table-footer-row");
        return footer;
    }

    private Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private String normalizedBoxId(String value) {
        return value == null ? "" : value.trim();
    }
}
