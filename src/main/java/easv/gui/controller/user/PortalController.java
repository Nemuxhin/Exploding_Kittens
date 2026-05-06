package easv.gui.controller.user;

import easv.gui.UserPortalModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.util.EnumMap;
import java.util.Map;

public class PortalController implements PortalNavigator {
    private static final String DARK_MODE_CLASS = "dark";
    private static final String MOON_ICON_PATH =
            "M12 3.25a8.75 8.75 0 1 0 8.75 8.75c0-.45-.04-.89-.1-1.32A6.75 6.75 0 0 1 12.32 3.4c-.1-.05-.21-.1-.32-.15zM5.25 12A6.74 6.74 0 0 1 9.83 5.6a8.75 8.75 0 0 0 8.57 8.57A6.75 6.75 0 0 1 5.25 12z";
    private static final String SUN_ICON_PATH =
            "M12 5.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13zm0 2a4.5 4.5 0 1 1 0 9 4.5 4.5 0 0 1 0-9zM11 1h2v3h-2V1zm0 19h2v3h-2v-3zM1 11h3v2H1v-2zm19 0h3v2h-3v-2zM4.22 2.81l2.12 2.12-1.41 1.41L2.81 4.22l1.41-1.41zm14.85 14.85 2.12 2.12-1.41 1.41-2.12-2.12 1.41-1.41zM19.78 2.81l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12zM4.93 17.66l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12z";

    private final UserPortalModel portalModel;
    private final DashboardController dashboardController;
    private final NewScanController newScanController;
    private final ScansController scansController;
    private final ExportsController exportsController;
    private final SettingsController settingsController;
    private final Map<PortalPage, Button> navButtons = new EnumMap<>(PortalPage.class);
    private final VBox root = new VBox();
    private final VBox pageContent = new VBox();
    private final Label breadcrumbLabel = new Label();
    private final Label accountNameLabel = new Label();
    private final Label accountEmailLabel = new Label();
    private final ToggleButton themeToggleButton = new ToggleButton();
    private final SVGPath themeModeIcon = new SVGPath();
    private ScrollPane scrollPane;
    private PortalPage activePage = PortalPage.DASHBOARD;
    private UserPortalModel.PortalSession activeSession;

    public PortalController(UserPortalModel portalModel) {
        this.portalModel = portalModel;
        dashboardController = new DashboardController(portalModel, this);
        newScanController = new NewScanController(
                portalModel,
                session -> activeSession = session,
                this
        );
        scansController = new ScansController(portalModel, this);
        exportsController = new ExportsController(portalModel);
        settingsController = new SettingsController(portalModel);
    }

    public Parent create() {
        root.getStyleClass().add("portal-shell");
        root.getChildren().addAll(
                buildTopBar(),
                buildRecentBar(),
                buildBreadcrumbBar(),
                buildScrollPane()
        );
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        pageContent.getStyleClass().add("portal-page-content");
        configureThemeToggle();
        renderPage();
        return root;
    }

    public static SVGPath buildIcon(String key, String styleClass) {
        SVGPath icon = new SVGPath();
        icon.setContent(iconPath(key));
        icon.getStyleClass().add(styleClass);
        return icon;
    }

    public static Label buildStatusChip(String status) {
        Label label = new Label(status);
        label.getStyleClass().add("status-chip");
        String normalized = status == null ? "" : status.trim().toLowerCase().replace(' ', '-');
        label.getStyleClass().add("status-" + normalized);
        return label;
    }

    private HBox buildTopBar() {
        HBox bar = new HBox(18);
        bar.getStyleClass().add("portal-top-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        HBox brand = new HBox(12);
        brand.getStyleClass().add("portal-brand");
        brand.setAlignment(Pos.CENTER_LEFT);

        StackPane brandMark = new StackPane(buildIcon("logo", "portal-action-icon-inverse"));
        brandMark.getStyleClass().add("portal-brand-mark");

        Label brandTitle = new Label("Document Scanning");
        brandTitle.getStyleClass().add("portal-brand-title");
        Label brandSubtitle = new Label("User Portal");
        brandSubtitle.getStyleClass().add("portal-brand-subtitle");
        VBox brandText = new VBox(2, brandTitle, brandSubtitle);

        brand.getChildren().addAll(brandMark, brandText);

        HBox navBar = new HBox(6);
        navBar.getStyleClass().add("portal-nav-bar");
        navBar.setAlignment(Pos.CENTER);
        for (PortalPage page : PortalPage.values()) {
            Button button = new Button();
            button.getStyleClass().add("portal-nav-button");
            button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            button.setMnemonicParsing(false);
            button.setGraphic(buildNavGraphic(page));
            button.setOnAction(event -> showPage(page));
            navButtons.put(page, button);
            navBar.getChildren().add(button);
        }

        HBox toolbar = new HBox(14);
        toolbar.getStyleClass().add("portal-toolbar");
        toolbar.getChildren().addAll(
                buildThemeToggle(),
                buildToolbarButton("help"),
                buildToolbarButton("clock")
        );

        accountNameLabel.getStyleClass().add("portal-account-name");
        accountEmailLabel.getStyleClass().add("portal-account-email");
        refreshAccountSummary();
        VBox accountText = new VBox(3, accountNameLabel, accountEmailLabel);

        Button avatar = new Button();
        avatar.getStyleClass().add("portal-avatar-button");
        avatar.setGraphic(buildIcon("user", "portal-avatar-icon"));

        HBox account = new HBox(12, toolbar, accountText, avatar);
        account.getStyleClass().add("portal-account");

        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        bar.getChildren().addAll(brand, leftSpacer, navBar, rightSpacer, account);
        return bar;
    }

    private HBox buildRecentBar() {
        HBox bar = new HBox(18);
        bar.getStyleClass().add("portal-recent-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label recentLabel = new Label("Recent:");
        recentLabel.getStyleClass().add("portal-recent-label");
        bar.getChildren().add(recentLabel);

        int visibleItems = Math.min(2, portalModel.fetchRecentScans().size());
        for (int index = 0; index < visibleItems; index++) {
            UserPortalModel.RecentScanItem item = portalModel.fetchRecentScans().get(index);
            Button button = new Button(item.boxId());
            button.getStyleClass().add("portal-recent-link");
            button.setOnAction(event -> resumeRecentScan(item));
            bar.getChildren().add(button);
        }

        Label dot = new Label(".");
        dot.getStyleClass().add("portal-recent-dot");
        Label shortcuts = new Label("Press ? for shortcuts");
        shortcuts.getStyleClass().add("portal-shortcut-hint");
        bar.getChildren().addAll(dot, shortcuts);
        return bar;
    }

    private HBox buildBreadcrumbBar() {
        HBox bar = new HBox();
        bar.getStyleClass().add("portal-breadcrumb-bar");
        breadcrumbLabel.getStyleClass().add("portal-breadcrumb");
        bar.getChildren().add(breadcrumbLabel);
        return bar;
    }

    private ScrollPane buildScrollPane() {
        scrollPane = new ScrollPane(pageContent);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("portal-page-scroll");
        scrollPane.setPadding(Insets.EMPTY);
        return scrollPane;
    }

    private HBox buildNavGraphic(PortalPage page) {
        SVGPath icon = buildIcon(page.iconKey(), "portal-nav-icon");
        Label text = new Label(page.label());
        text.getStyleClass().add("portal-nav-label");

        HBox row = new HBox(8, icon, text);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button buildToolbarButton(String iconKey) {
        Button button = new Button();
        button.getStyleClass().add("portal-toolbar-button");
        button.setGraphic(buildIcon(iconKey, "portal-toolbar-icon"));
        return button;
    }

    private ToggleButton buildThemeToggle() {
        themeToggleButton.getStyleClass().add("portal-theme-button");
        themeToggleButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        themeToggleButton.setMnemonicParsing(false);

        themeModeIcon.getStyleClass().add("portal-theme-icon");
        themeToggleButton.setGraphic(themeModeIcon);
        return themeToggleButton;
    }

    private void configureThemeToggle() {
        updateTheme(themeToggleButton.isSelected());
        themeToggleButton.selectedProperty().addListener((observable, oldValue, isDark) -> updateTheme(isDark));
    }

    private void updateTheme(boolean isDark) {
        root.getStyleClass().remove(DARK_MODE_CLASS);
        if (isDark) {
            root.getStyleClass().add(DARK_MODE_CLASS);
        }

        themeModeIcon.setContent(isDark ? MOON_ICON_PATH : SUN_ICON_PATH);
    }

    @Override
    public void showPage(PortalPage page) {
        activePage = page;
        renderPage();
    }

    @Override
    public void resumeRecentScan(UserPortalModel.RecentScanItem item) {
        activeSession = portalModel.resumeSession(item);
        activePage = PortalPage.NEW_SCAN;
        renderPage();
    }

    @Override
    public void resumeHistoryScan(UserPortalModel.HistoryItem item) {
        activeSession = portalModel.resumeSession(item);
        activePage = PortalPage.NEW_SCAN;
        renderPage();
    }

    private void renderPage() {
        refreshAccountSummary();
        navButtons.forEach((page, button) -> {
            button.getStyleClass().remove("active");
            if (page == activePage) {
                button.getStyleClass().add("active");
            }
        });

        breadcrumbLabel.setText(activePage.breadcrumb());
        pageContent.getChildren().setAll(
                switch (activePage) {
                    case NEW_SCAN -> newScanController.create(activeSession);
                    case SCANS -> scansController.create();
                    case EXPORTS -> exportsController.create();
                    case SETTINGS -> settingsController.create();
                    default -> dashboardController.create();
                }
        );
        scrollPane.setVvalue(0);
    }

    private void refreshAccountSummary() {
        UserPortalModel.AccountProfile accountProfile = portalModel.fetchAccountProfile();
        accountNameLabel.setText(accountProfile.fullName());
        accountEmailLabel.setText(accountProfile.email());
    }

    private static String iconPath(String key) {
        return switch (key) {
            case "dashboard" -> "M10 2L2 8h2v8h4v-5h4v5h4V8h2L10 2z";
            case "scan" -> "M3 3h14v14H3z M9 6h2v3h3v2h-3v3H9v-3H6V9h3z";
            case "scans" -> "M5 2h7l5 5v11H5z M11 2v5h5";
            case "exports" -> "M3 5h14v4h-2V7H5v2H3z M5 11h10v5H5z M9 2h2v5h3l-4 4-4-4h3z";
            case "settings" -> "M9 2h2l.5 2.1 1.9.8 1.8-1 1.4 1.4-1 1.8.8 1.9L18 9v2l-2.1.5-.8 1.9 1 1.8-1.4 1.4-1.8-1-.9 1.9L11 18H9l-.5-2.1-1.9-.8-1.8 1L3.4 14.7l1-1.8L3.6 11 2 10V8l2.1-.5.8-1.9-1-1.8L5.3 2.4l1.8 1 .9-1.9z M10 7a3 3 0 100 6 3 3 0 000-6z";
            case "help" -> "M10 2a8 8 0 100 16 8 8 0 000-16zm0 12h1v1H9v-1h1zm2.1-6.8c0 1.8-2.1 2.1-2.1 3.8h-1.5c0-2.4 1.9-2.5 1.9-3.8 0-.7-.6-1.2-1.4-1.2-.8 0-1.4.4-1.8 1.2L6 6.6C6.6 5.3 7.8 4.5 9.4 4.5c1.7 0 2.7 1 2.7 2.7z";
            case "clock" -> "M10 2a8 8 0 100 16 8 8 0 000-16zm1 4H9v5l4 2 1-1-3-1.5z";
            case "user", "account" -> "M10 2a3 3 0 110 6 3 3 0 010-6zm0 8c3 0 5 1.5 5 4v2H5v-2c0-2.5 2-4 5-4z";
            case "shortcuts" -> "M3 5h14v10H3z M5 7h2v2H5z M8 7h2v2H8z M11 7h2v2h-2z M14 7h1v2h-1z M5 10h8v2H5z M14 10h1v2h-1z";
            case "notifications" -> "M10 2a4 4 0 00-4 4v2.7L4.7 11v1h10.6v-1L14 8.7V6a4 4 0 00-4-4zm0 14a2 2 0 001.9-1.3H8.1A2 2 0 0010 16z";
            case "scanning" -> "M4 2h3v2H4v3H2V4c0-1.1.9-2 2-2zm9 0h3c1.1 0 2 .9 2 2v3h-2V4h-3V2zM2 13h2v3h3v2H4c-1.1 0-2-.9-2-2v-3zm14 0h2v3c0 1.1-.9 2-2 2h-3v-2h3v-3z";
            case "privacy" -> "M4 3h12v4H4z M4 9h12v4H4z M4 15h12v2H4z";
            case "save" -> "M4 2h10l2 2v12H4z M7 2v4h6V2z M7 11h6v3H7z";
            case "download" -> "M9 2h2v7h3l-4 4-4-4h3z M4 14h12v2H4z";
            case "logo" -> "M4 2h3v2H4v3H2V4c0-1.1.9-2 2-2zm9 0h3c1.1 0 2 .9 2 2v3h-2V4h-3V2zM2 13h2v3h3v2H4c-1.1 0-2-.9-2-2v-3zm14 0h2v3c0 1.1-.9 2-2 2h-3v-2h3v-3z";
            default -> "M4 4h12v12H4z";
        };
    }
}
