package easv.gui.controller.user;

import easv.bll.UserSession;
import easv.gui.UserPortalModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

import java.io.IOException;
import java.util.prefs.Preferences;

public class UserPortalController implements PortalNavigator {

    private static final String APP_SHELL_CLASS = "app-shell";
    private static final String DARK_MODE_CLASS = "dark";
    private static final String USER_PORTAL_SHELL_CLASS = "user-portal-shell";
    private static final String PREFERENCES_NODE = "easv.gui.portal";
    private static final String DARK_MODE_PREFERENCE_KEY = "userPortal.darkMode";
    private static final String MOON_ICON_PATH =
            "M12 3.25a8.75 8.75 0 1 0 8.75 8.75c0-.45-.04-.89-.1-1.32A6.75 6.75 0 0 1 12.32 3.4c-.1-.05-.21-.1-.32-.15zM5.25 12A6.74 6.74 0 0 1 9.83 5.6a8.75 8.75 0 0 0 8.57 8.57A6.75 6.75 0 0 1 5.25 12z";
    private static final String SUN_ICON_PATH =
            "M12 5.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13zm0 2a4.5 4.5 0 1 1 0 9 4.5 4.5 0 0 1 0-9zM11 1h2v3h-2V1zm0 19h2v3h-2v-3zM1 11h3v2H1v-2zm19 0h3v2h-3v-2zM4.22 2.81l2.12 2.12-1.41 1.41L2.81 4.22l1.41-1.41zm14.85 14.85 2.12 2.12-1.41 1.41-2.12-2.12 1.41-1.41zM19.78 2.81l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12zM4.93 17.66l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12z";

    private final UserPortalModel portalModel = new UserPortalModel();
    private final Preferences preferences = Preferences.userRoot().node(PREFERENCES_NODE);

    @FXML private BorderPane appRoot;
    @FXML private ImageView brandLogoImageView;
    @FXML private Label breadcrumbLabel;
    @FXML private Label accountNameLabel;
    @FXML private Label accountRoleLabel;
    @FXML private ToggleButton dashboardNavItem;
    @FXML private ToggleButton scanNavItem;
    @FXML private ToggleButton myScansNavItem;
    @FXML private ToggleButton assignedQANavItem;
    @FXML private ToggleButton exportsNavItem;
    @FXML private ToggleButton settingsNavItem;
    @FXML private ToggleButton darkModeToggleButton;
    @FXML private SVGPath darkModeToggleIcon;
    @FXML private StackPane contentHost;

    private NewScanController scanController;
    private AssignedQaController assignedQaController;

    private Node dashboardView;
    private Node scanView;
    private Node assignedQaView;
    private Node scansView;
    private Node exportsView;
    private Node settingsView;

    private UserPortalSection activeSection = UserPortalSection.DASHBOARD;
    private UserPortalModel.PortalSession activeSession;
    private boolean darkModeEnabled;

    @FXML
    private void initialize() {
        if (brandLogoImageView != null) {
            brandLogoImageView.setManaged(false);
            brandLogoImageView.setVisible(false);
        }

        configureShell();
        configureThemeToggle();
        configureNavigation();
        showSection(UserPortalSection.DASHBOARD);
    }

    private void configureShell() {
        if (appRoot != null) {
            if (!appRoot.getStyleClass().contains(APP_SHELL_CLASS)) {
                appRoot.getStyleClass().add(APP_SHELL_CLASS);
            }

            if (!appRoot.getStyleClass().contains(USER_PORTAL_SHELL_CLASS)) {
                appRoot.getStyleClass().add(USER_PORTAL_SHELL_CLASS);
            }

            appRoot.getStyleClass().remove(APP_SHELL_CLASS + " " + USER_PORTAL_SHELL_CLASS);
        }

        UserPortalModel.AccountProfile accountProfile = portalModel.fetchAccountProfile();
        String accountName = UserSession.hasCurrentUser()
                ? UserSession.getCurrentUser().getName()
                : accountProfile.fullName();
        String accountRole = UserSession.hasCurrentUser()
                ? UserSession.getCurrentUser().getRole()
                : "User Portal";

        if (accountNameLabel != null) {
            accountNameLabel.setText(accountName);
        }

        if (accountRoleLabel != null) {
            accountRoleLabel.setText(accountRole);
        }
    }

    private void configureThemeToggle() {
        darkModeEnabled = preferences.getBoolean(DARK_MODE_PREFERENCE_KEY, false);

        if (darkModeToggleButton != null) {
            darkModeToggleButton.setSelected(darkModeEnabled);
        }

        updateTheme(darkModeEnabled);

        if (darkModeToggleButton != null) {
            darkModeToggleButton.selectedProperty().addListener((observable, oldValue, isDark) -> updateTheme(isDark));
        }
    }

    private void updateTheme(boolean isDark) {
        if (appRoot == null) {
            return;
        }

        darkModeEnabled = isDark;
        preferences.putBoolean(DARK_MODE_PREFERENCE_KEY, isDark);

        if (darkModeToggleButton != null && darkModeToggleButton.isSelected() != isDark) {
            darkModeToggleButton.setSelected(isDark);
        }

        appRoot.getStyleClass().remove(DARK_MODE_CLASS);
        if (isDark) {
            appRoot.getStyleClass().add(DARK_MODE_CLASS);
        }

        if (darkModeToggleIcon != null) {
            darkModeToggleIcon.setContent(isDark ? MOON_ICON_PATH : SUN_ICON_PATH);
        }
    }

    private void configureNavigation() {
        dashboardNavItem.setOnAction(event -> showSection(UserPortalSection.DASHBOARD));
        scanNavItem.setOnAction(event -> showSection(UserPortalSection.NEW_SCAN));
        myScansNavItem.setOnAction(event -> showSection(UserPortalSection.MY_SCANS));
        assignedQANavItem.setOnAction(event -> showSection(UserPortalSection.ASSIGNED_QA));
        exportsNavItem.setOnAction(event -> showSection(UserPortalSection.EXPORTS));
        settingsNavItem.setOnAction(event -> showSection(UserPortalSection.SETTINGS));
    }

    private void showSection(UserPortalSection section) {
        activeSection = section;
        contentHost.getChildren().setAll(loadSection(section));
        updateBreadcrumb();
        updateNavigationState();
    }

    private Node loadSection(UserPortalSection section) {
        return switch (section) {
            case DASHBOARD -> loadDashboardView();
            case NEW_SCAN -> loadScanView();
            case MY_SCANS -> loadScansView();
            case ASSIGNED_QA -> loadAssignedQaView();
            case EXPORTS -> loadExportsView();
            case SETTINGS -> loadSettingsView();
        };
    }

    private Node loadDashboardView() {
        if (dashboardView == null) {
            dashboardView = wrapScrollable(new DashboardController(portalModel, this).create());
        }
        return dashboardView;
    }

    private Node loadScanView() {
        if (scanView == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/UserViews/scan-view.fxml"));
            loader.setControllerFactory(type -> {
                if (type == NewScanController.class) {
                    return new NewScanController(portalModel, session -> { }, this);
                }
                return instantiate(type);
            });

            scanView = load(loader);
            scanController = loader.getController();
        }

        if (scanController != null) {
            scanController.activate(activeSession);
            activeSession = null;
        }

        return scanView;
    }

    private Node loadAssignedQaView() {
        if (assignedQaView == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/UserViews/assigned-qa-view.fxml"));
            assignedQaView = load(loader);
            assignedQaController = loader.getController();
            assignedQaController.setPortalModel(portalModel);
        }

        if (assignedQaController != null) {
            assignedQaController.refresh();
        }

        return assignedQaView;
    }

    private Node loadScansView() {
        if (scansView == null) {
            scansView = wrapScrollable(new ScansController(portalModel, this).create());
        }
        return scansView;
    }

    private Node loadExportsView() {
        if (exportsView == null) {
            exportsView = wrapScrollable(new ExportsController(portalModel).create());
        }
        return exportsView;
    }

    private Node loadSettingsView() {
        if (settingsView == null) {
            settingsView = wrapScrollable(new SettingsController(portalModel).create());
        }
        return settingsView;
    }

    private Parent load(FXMLLoader loader) {
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load user portal content.", exception);
        }
    }

    private Object instantiate(Class<?> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not create controller: " + type.getName(), exception);
        }
    }

    private ScrollPane wrapScrollable(Node content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMinWidth(0);
        scrollPane.setMinHeight(0);
        scrollPane.setMaxWidth(Double.MAX_VALUE);
        scrollPane.setMaxHeight(Double.MAX_VALUE);
        scrollPane.getStyleClass().add("admin-page-scroll");
        return scrollPane;
    }

    private void updateNavigationState() {
        dashboardNavItem.setSelected(activeSection == UserPortalSection.DASHBOARD);
        scanNavItem.setSelected(activeSection == UserPortalSection.NEW_SCAN);
        myScansNavItem.setSelected(activeSection == UserPortalSection.MY_SCANS);
        assignedQANavItem.setSelected(activeSection == UserPortalSection.ASSIGNED_QA);
        exportsNavItem.setSelected(activeSection == UserPortalSection.EXPORTS);
        settingsNavItem.setSelected(activeSection == UserPortalSection.SETTINGS);
    }

    private void updateBreadcrumb() {
        if (breadcrumbLabel != null) {
            breadcrumbLabel.setText("User / " + activeSection.label());
        }
    }

    @Override
    public void showPage(PortalPage page) {
        switch (page) {
            case DASHBOARD -> showSection(UserPortalSection.DASHBOARD);
            case NEW_SCAN -> showSection(UserPortalSection.NEW_SCAN);
            case SCANS -> showSection(UserPortalSection.MY_SCANS);
            case EXPORTS -> showSection(UserPortalSection.EXPORTS);
            case SETTINGS -> showSection(UserPortalSection.SETTINGS);
            default -> showSection(UserPortalSection.DASHBOARD);
        }
    }

    @Override
    public void showAssignedQa() {
        showSection(UserPortalSection.ASSIGNED_QA);
    }

    @Override
    public void resumeRecentScan(UserPortalModel.RecentScanItem item) {
        activeSession = portalModel.resumeSession(item);
        showSection(UserPortalSection.NEW_SCAN);
    }

    @Override
    public void resumeHistoryScan(UserPortalModel.HistoryItem item) {
        activeSession = portalModel.resumeSession(item);
        showSection(UserPortalSection.NEW_SCAN);
    }

    private enum UserPortalSection {
        DASHBOARD("Dashboard"),
        NEW_SCAN("New Scan"),
        MY_SCANS("My Scans"),
        ASSIGNED_QA("Assigned QA"),
        EXPORTS("Exports"),
        SETTINGS("Settings");

        private final String label;

        UserPortalSection(String label) {
            this.label = label;
        }

        private String label() {
            return label;
        }
    }
}
