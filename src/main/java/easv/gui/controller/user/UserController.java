package easv.gui.controller.user;

import easv.be.User;
import easv.bll.KeyboardShortcut;
import easv.bll.ShortcutManager;
import easv.bll.UserManager;
import easv.bll.UserSession;
import easv.gui.MainApp;
import easv.gui.PrimeIcons;
import easv.gui.UserPortalModel;
import easv.util.Strings;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.prefs.Preferences;

public class UserController implements UserNavigator {

    private static final String ACTIVE_NAV_CLASS = "active";
    private static final String DARK_MODE_CLASS = "dark";

    private static final String PREFERENCES_NODE = "easv.gui.portal";
    private static final String DARK_MODE_PREFERENCE_KEY = "userPortal.darkMode";

    private static final String ACCOUNT_SECTION = "Edit Profile";
    private static final String PRIVACY_SECTION = "Settings and Privacy";

    private static final String LIGHT_MODE_LOGO =
            "/images/weblager/styleguide/Main Blue/LogoBlueH.png";

    private static final String DARK_MODE_LOGO =
            "/images/weblager/styleguide/DarkmodeBlue/LogoBlue2H.png";

    private static final String MOON_ICON_PATH =
            "M12 3.25a8.75 8.75 0 1 0 8.75 8.75c0-.45-.04-.89-.1-1.32A6.75 6.75 0 0 1 12.32 3.4c-.1-.05-.21-.1-.32-.15zM5.25 12A6.74 6.74 0 0 1 9.83 5.6a8.75 8.75 0 0 0 8.57 8.57A6.75 6.75 0 0 1 5.25 12z";

    private static final String SUN_ICON_PATH =
            "M12 5.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13zm0 2a4.5 4.5 0 1 1 0 9 4.5 4.5 0 0 1 0-9zM11 1h2v3h-2V1zm0 19h2v3h-2v-3zM1 11h3v2H1v-2zm19 0h3v2h-3v-2zM4.22 2.81l2.12 2.12-1.41 1.41L2.81 4.22l1.41-1.41zm14.85 14.85 2.12 2.12-1.41 1.41-2.12-2.12 1.41-1.41zM19.78 2.81l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12zM4.93 17.66l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12z";

    @FXML private StackPane appShell;
    @FXML private BorderPane appRoot;
    @FXML private StackPane contentHost;

    @FXML private StackPane brandMark;
    @FXML private Label brandLogoFallbackLabel;
    @FXML private ImageView brandLogoImageView;

    @FXML private ToggleButton dashboardNavItem;
    @FXML private ToggleButton scanNavItem;
    @FXML private ToggleButton myScansNavItem;
    @FXML private ToggleButton assignedQANavItem;
    @FXML private Button keyboardShortcutsButton;
    @FXML private Button notificationMenuButton;
    @FXML private Button markNotificationsReadButton;
    @FXML private Button viewAllNotificationsButton;
    @FXML private Button accountMenuButton;
    @FXML private Label accountNameLabel;
    @FXML private Label accountInitialsLabel;
    @FXML private VBox accountDropdownPane;
    @FXML private VBox notificationDropdownPane;
    @FXML private Region notificationUnreadDot;
    @FXML private Label accountDropdownNameLabel;
    @FXML private Label accountDropdownDetailLabel;
    @FXML private Button editProfileMenuButton;
    @FXML private Button settingsPrivacyMenuButton;
    @FXML private Button logoutMenuButton;
    @FXML private ToggleButton darkModeToggleButton;
    @FXML private SVGPath darkModeToggleIcon;

    private final UserPortalModel portalModel = new UserPortalModel();
    private final ShortcutManager shortcutManager = new ShortcutManager();
    private final UserManager userManager = new UserManager();
    private final Preferences preferences = Preferences.userRoot().node(PREFERENCES_NODE);

    private MainApp mainApp;
    private Object activePageController;
    private final Map<UserPage, Node> pageCache = new EnumMap<>(UserPage.class);
    private final Map<UserPage, Object> controllerCache = new EnumMap<>(UserPage.class);
    private UserPage currentPage;
    private Scene shortcutScene;
    private boolean userEditingTextInput;
    private UserPortalModel.RecentScanItem pendingRecentScanItem;
    private UserPortalModel.HistoryItem pendingHistoryScanItem;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void initialize() {
        configureAccount();
        configureAccountMenu();
        configureNotificationMenu();
        configureKeyboardShortcutsButton();
        configureThemeToggle();
        configureNavigation();
        configureGlobalShortcuts();
        showPage(UserPage.DASHBOARD);
    }

    private void configureAccount() {
        UserPortalModel.AccountProfile fallbackProfile = portalModel.fetchAccountProfile();
        User currentUser = UserSession.getCurrentUser();

        String displayName = displayNameFor(currentUser, fallbackProfile);
        String accountDetail = accountDetailFor(currentUser);

        if (accountNameLabel != null) {
            accountNameLabel.setText(displayName);
        }

        if (accountInitialsLabel != null) {
            accountInitialsLabel.setText(initialsFor(displayName));
        }

        if (accountDropdownNameLabel != null) {
            accountDropdownNameLabel.setText(displayName);
        }

        if (accountDropdownDetailLabel != null) {
            accountDropdownDetailLabel.setText(accountDetail);
        }
    }

    private void configureAccountMenu() {
        if (accountDropdownPane != null) {
            accountDropdownPane.setMaxHeight(Region.USE_PREF_SIZE);
            accountDropdownPane.setVisible(false);
        }

        if (accountMenuButton != null) {
            accountMenuButton.setOnAction(event -> toggleAccountDropdown());
        }

        if (editProfileMenuButton != null) {
            editProfileMenuButton.setOnAction(event -> showAccountSettingsPage(ACCOUNT_SECTION));
        }

        if (settingsPrivacyMenuButton != null) {
            settingsPrivacyMenuButton.setOnAction(event -> showAccountSettingsPage(PRIVACY_SECTION));
        }

        if (logoutMenuButton != null) {
            logoutMenuButton.setOnAction(event -> logout());
        }
    }

    private void configureKeyboardShortcutsButton() {
        if (keyboardShortcutsButton != null) {
            keyboardShortcutsButton.setOnAction(event -> showKeyboardShortcutsDialog());
        }
    }

    private void configureNotificationMenu() {
        if (notificationDropdownPane != null) {
            notificationDropdownPane.setMaxHeight(Region.USE_PREF_SIZE);
            notificationDropdownPane.setVisible(false);
            notificationDropdownPane.setManaged(false);
        }

        if (notificationMenuButton != null) {
            notificationMenuButton.setOnAction(event -> toggleNotificationDropdown());
        }

        if (markNotificationsReadButton != null) {
            markNotificationsReadButton.setOnAction(event -> markAllNotificationsRead());
        }

        if (viewAllNotificationsButton != null) {
            viewAllNotificationsButton.setOnAction(event -> expandNotificationDropdown());
        }
    }

    private void configureThemeToggle() {
        updateTheme(isDarkModeEnabled());

        if (darkModeToggleButton != null) {
            darkModeToggleButton.selectedProperty().addListener((observable, oldValue, isDark) ->
                    updateTheme(isDark)
            );
        }
    }

    private boolean isDarkModeEnabled() {
        return preferences.getBoolean(DARK_MODE_PREFERENCE_KEY, false);
    }

    private void updateTheme(boolean isDark) {
        updateDarkModeClass(isDark);
        updateBrandLogo(isDark);
        preferences.putBoolean(DARK_MODE_PREFERENCE_KEY, isDark);
        updateThemeControls(isDark);
    }

    private void updateDarkModeClass(boolean isDark) {
        if (appShell == null) {
            return;
        }

        appShell.getStyleClass().remove(DARK_MODE_CLASS);

        if (isDark) {
            appShell.getStyleClass().add(DARK_MODE_CLASS);
        }
    }

    private void updateBrandLogo(boolean isDark) {
        if (brandLogoImageView == null || brandLogoFallbackLabel == null) {
            return;
        }

        String logoPath = isDark ? DARK_MODE_LOGO : LIGHT_MODE_LOGO;
        URL logoUrl = getClass().getResource(logoPath);

        boolean logoExists = logoUrl != null;

        brandLogoImageView.setVisible(logoExists);
        brandLogoImageView.setManaged(logoExists);

        brandLogoFallbackLabel.setVisible(!logoExists);
        brandLogoFallbackLabel.setManaged(!logoExists);

        if (brandMark != null) {
            brandMark.setVisible(!logoExists);
            brandMark.setManaged(!logoExists);
        }

        if (logoExists) {
            brandLogoImageView.setImage(new Image(logoUrl.toExternalForm(), true));
        }
    }

    private void updateThemeControls(boolean isDark) {
        if (darkModeToggleButton != null) {
            if (darkModeToggleButton.isSelected() != isDark) {
                darkModeToggleButton.setSelected(isDark);
            }

            darkModeToggleButton.setText(isDark ? "Dark Mode" : "Light Mode");
        }

        if (darkModeToggleIcon != null) {
            darkModeToggleIcon.setContent(isDark ? MOON_ICON_PATH : SUN_ICON_PATH);
        }
    }

    private void configureNavigation() {
        for (UserPage page : UserPage.values()) {
            ToggleButton navItem = getNavItem(page);

            if (navItem != null) {
                navItem.setOnAction(event -> {
                    event.consume();
                    showPage(page);
                });
            }
        }
    }

    private void configureGlobalShortcuts() {
        if (appShell != null) {
            appShell.setFocusTraversable(true);
        }

        Platform.runLater(() -> {
            Scene scene = appShell == null ? null : appShell.getScene();

            if (scene != null) {
                registerSceneShortcuts(scene);
                return;
            }

            if (appShell != null) {
                appShell.sceneProperty().addListener((observable, oldScene, newScene) -> {
                    if (newScene != null) {
                        registerSceneShortcuts(newScene);
                    }
                });
            }
        });
    }

    private void registerSceneShortcuts(Scene scene) {
        if (scene == shortcutScene) {
            return;
        }

        if (shortcutScene != null) {
            shortcutScene.removeEventFilter(KeyEvent.KEY_PRESSED, this::handleGlobalShortcut);
            shortcutScene.removeEventFilter(KeyEvent.KEY_TYPED, this::handleGlobalTypedShortcut);
            shortcutScene.removeEventFilter(MouseEvent.MOUSE_PRESSED, this::rememberTextInputFocusIntent);
        }

        shortcutScene = scene;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleGlobalShortcut);
        scene.addEventFilter(KeyEvent.KEY_TYPED, this::handleGlobalTypedShortcut);
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, this::rememberTextInputFocusIntent);
    }

    private void handleGlobalShortcut(KeyEvent event) {
        if (event.isConsumed()) {
            return;
        }

        if (isUserEditingTextInput(event)) {
            return;
        }

        // F1 opens shortcut help from anywhere outside text fields.
        if (event.getCode() == KeyCode.F1) {
            showKeyboardShortcutsDialog();
            event.consume();
            return;
        }

        if (event.isShortcutDown() && event.getCode() == KeyCode.E) {
            showPage(UserPage.EXPORTS);
            event.consume();
            return;
        }

        if (activePageController instanceof ScanController scanController
                && scanController.handleSelectedPageArrowShortcut(event)) {
            event.consume();
            return;
        }

        if (event.getCode() == KeyCode.LEFT) {
            showAdjacentUserPage(-1);
            event.consume();
            return;
        }

        if (event.getCode() == KeyCode.RIGHT) {
            showAdjacentUserPage(1);
            event.consume();
            return;
        }

        // Forward to the active scan controller — only when the scan page is showing.
        if (activePageController instanceof ScanController scanController
                && scanController.handlePrioritizedGlobalShortcut(event)) {
            event.consume();
        }
    }

    // ? and +/- are handled via KEY_TYPED so they work on every keyboard layout.
    private void handleGlobalTypedShortcut(KeyEvent event) {
        if (event.isConsumed() || isUserEditingTextInput(event)) {
            return;
        }

        String character = event.getCharacter();

        if ("?".equals(character)) {
            showKeyboardShortcutsDialog();
            event.consume();
            return;
        }

        // Forward typed scan shortcuts only when the scan page is already active.
        if (activePageController instanceof ScanController scanController
                && scanController.runTypedShortcut(character)) {
            event.consume();
        }
    }

    @Override
    public void showPage(UserPage page) {
        hideAccountDropdown();
        hideNotificationDropdown();
        loadPage(page);
        currentPage = page;
        setActiveNavItem(getNavItem(page));
        resetShortcutFocus();
    }

    @Override
    public void resumeRecentScan(UserPortalModel.RecentScanItem item) {
        pendingRecentScanItem = item;
        pendingHistoryScanItem = null;
        showPage(UserPage.SCAN);
    }

    @Override
    public void resumeHistoryScan(UserPortalModel.HistoryItem item) {
        pendingHistoryScanItem = item;
        pendingRecentScanItem = null;
        showPage(UserPage.SCAN);
    }

    private void showAdjacentUserPage(int step) {
        List<UserPage> pages = getShortcutNavigationPages();
        int currentIndex = pages.indexOf(currentPage);

        if (currentIndex < 0) {
            currentIndex = 0;
        }

        int nextIndex = Math.max(0, Math.min(pages.size() - 1, currentIndex + step));
        showPage(pages.get(nextIndex));
    }

    private List<UserPage> getShortcutNavigationPages() {
        return List.of(
                UserPage.DASHBOARD,
                UserPage.SCAN,
                UserPage.MY_SCANS,
                UserPage.ASSIGNED_QA,
                UserPage.EXPORTS
        );
    }

    private void loadPage(UserPage page) {
        if (showCachedPage(page)) {
            return;
        }

        if (!page.hasFxml()) {
            activePageController = null;
            Node content = createProgrammaticPage(page);
            if (shouldWrapScrollable(page)) {
                content = wrapScrollable(content);
            } else if (content instanceof Region region) {
                configureRegionPageSize(region);
            }
            StackPane.setAlignment(content, Pos.TOP_CENTER);
            showAndRememberPage(page, content, null);
            return;
        }

        URL pageUrl = getClass().getResource(page.fxmlPath());

        if (pageUrl == null) {
            contentHost.getChildren().setAll(createMissingPagePlaceholder(page.title()));
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(pageUrl);
            Parent loadedPage = loader.load();

            activePageController = loader.getController();
            configureLoadedController(activePageController);
            configureLoadedPageSize(loadedPage);
            StackPane.setAlignment(loadedPage, Pos.TOP_CENTER);

            showAndRememberPage(page, loadedPage, activePageController);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load page: " + page.fxmlPath(), exception);
        }
    }

    private boolean showCachedPage(UserPage page) {
        // The scan page has form state, so we reload it to avoid showing an old page by mistake.
        if (page == UserPage.SCAN) {
            return false;
        }

        Node cachedPage = pageCache.get(page);
        if (cachedPage == null) {
            return false;
        }

        activePageController = controllerCache.get(page);
        contentHost.getChildren().setAll(cachedPage);
        return true;
    }

    private void showAndRememberPage(UserPage page, Node pageNode, Object controller) {
        activePageController = controller;
        contentHost.getChildren().setAll(pageNode);

        if (page == UserPage.SCAN) {
            pageCache.remove(page);
            controllerCache.remove(page);
            return;
        }

        // Keep simple pages ready, so navigation feels instant after the first load.
        pageCache.put(page, pageNode);
        controllerCache.put(page, controller);
    }

    private Node createProgrammaticPage(UserPage page) {
        return switch (page) {
            case DASHBOARD -> new DashboardController(portalModel, this).create();
            case MY_SCANS -> new MyScansController(portalModel, this).create();
            case EXPORTS -> new ExportsController(portalModel).create();
            case EDIT_PROFILE -> createAccountSettingsPage(ACCOUNT_SECTION);
            case SETTINGS -> createAccountSettingsPage(PRIVACY_SECTION);
            default -> createMissingPagePlaceholder(page.title());
        };
    }

    private ScrollPane wrapScrollable(Node page) {
        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.getStyleClass().add("portal-page-scroll");
        scrollPane.setMaxWidth(Double.MAX_VALUE);
        scrollPane.setMaxHeight(Double.MAX_VALUE);
        return scrollPane;
    }

    private boolean shouldWrapScrollable(UserPage page) {
        return page != UserPage.DASHBOARD;
    }

    private void configureLoadedController(Object controller) {
        if (controller instanceof ScanController scanController) {
            scanController.setNavigator(this);
            scanController.setPortalModel(portalModel);
            if (pendingHistoryScanItem != null) {
                scanController.resumeHistoryScan(pendingHistoryScanItem);
                pendingHistoryScanItem = null;
                pendingRecentScanItem = null;
            } else if (pendingRecentScanItem != null) {
                scanController.resumeRecentScan(pendingRecentScanItem);
                pendingRecentScanItem = null;
            }
        }

        if (controller instanceof AssignedQaController assignedQaController) {
            assignedQaController.setPortalModel(portalModel);
        }
    }

    private void configureLoadedPageSize(Parent page) {
        if (!(page instanceof Region region)) {
            return;
        }

        configureRegionPageSize(region);
    }

    private void configureRegionPageSize(Region region) {
        region.setMinWidth(0);
        region.setMinHeight(0);
        region.setMaxWidth(Double.MAX_VALUE);
        region.setMaxHeight(Double.MAX_VALUE);
    }

    private VBox createMissingPagePlaceholder(String pageTitle) {
        Label titleLabel = new Label(pageTitle);
        titleLabel.getStyleClass().add("page-title");

        Label subtitleLabel = new Label("No content is available for this section.");
        subtitleLabel.getStyleClass().add("page-subtitle");

        VBox placeholder = new VBox(6, titleLabel, subtitleLabel);
        placeholder.getStyleClass().add("page-content");
        placeholder.setAlignment(Pos.TOP_LEFT);
        placeholder.setMaxWidth(Double.MAX_VALUE);
        placeholder.setMaxHeight(Double.MAX_VALUE);

        return placeholder;
    }

    private void setActiveNavItem(ToggleButton activeNavItem) {
        for (ToggleButton navItem : getNavigationItems()) {
            if (navItem != null) {
                setNavItemActive(navItem, false);
            }
        }

        if (activeNavItem != null) {
            setNavItemActive(activeNavItem, true);
        }
    }

    private void setNavItemActive(ToggleButton navItem, boolean active) {
        navItem.setSelected(active);
        navItem.getStyleClass().remove(ACTIVE_NAV_CLASS);

        if (active) {
            navItem.getStyleClass().add(ACTIVE_NAV_CLASS);
        }
    }

    private List<ToggleButton> getNavigationItems() {
        return List.of(
                dashboardNavItem,
                scanNavItem,
                myScansNavItem,
                assignedQANavItem
        );
    }

    private ToggleButton getNavItem(UserPage page) {
        return switch (page) {
            case DASHBOARD -> dashboardNavItem;
            case SCAN -> scanNavItem;
            case MY_SCANS -> myScansNavItem;
            case ASSIGNED_QA -> assignedQANavItem;
            case EXPORTS -> null;
            case EDIT_PROFILE -> null;
            case SETTINGS -> null;
        };
    }

    private void toggleAccountDropdown() {
        if (accountDropdownPane == null) {
            return;
        }

        boolean shouldShow = !accountDropdownPane.isVisible();
        hideNotificationDropdown();
        accountDropdownPane.setVisible(shouldShow);

        if (shouldShow) {
            accountDropdownPane.toFront();
        }
    }

    private void toggleNotificationDropdown() {
        if (notificationDropdownPane == null) {
            return;
        }

        boolean shouldShow = !notificationDropdownPane.isVisible();
        hideAccountDropdown();
        notificationDropdownPane.setVisible(shouldShow);
        notificationDropdownPane.setManaged(shouldShow);

        if (shouldShow) {
            notificationDropdownPane.toFront();
        }
    }

    private void hideAccountDropdown() {
        if (accountDropdownPane != null) {
            accountDropdownPane.setVisible(false);
        }
    }

    private void hideNotificationDropdown() {
        if (notificationDropdownPane != null) {
            notificationDropdownPane.setVisible(false);
            notificationDropdownPane.setManaged(false);
        }
    }

    private void markAllNotificationsRead() {
        if (notificationDropdownPane != null) {
            for (Node node : notificationDropdownPane.lookupAll(".user-notification-unread-dot")) {
                node.getStyleClass().setAll("user-notification-read-spacer");
            }
        }

        if (notificationUnreadDot != null) {
            notificationUnreadDot.setVisible(false);
            notificationUnreadDot.setManaged(false);
        }

        if (markNotificationsReadButton != null) {
            markNotificationsReadButton.setText("All read");
            markNotificationsReadButton.setDisable(true);
        }
    }

    private void expandNotificationDropdown() {
        if (notificationDropdownPane == null) {
            return;
        }

        for (Node node : notificationDropdownPane.lookupAll(".user-notification-extra-item")) {
            node.setVisible(true);
            node.setManaged(true);
        }

        if (viewAllNotificationsButton != null) {
            viewAllNotificationsButton.setVisible(false);
            viewAllNotificationsButton.setManaged(false);
        }
    }

    private void showAccountSettingsPage(String selectedSection) {
        hideAccountDropdown();
        hideNotificationDropdown();
        setActiveNavItem(null);
        Node content = wrapScrollable(createAccountSettingsPage(selectedSection));
        StackPane.setAlignment(content, Pos.TOP_CENTER);
        contentHost.getChildren().setAll(content);
        resetShortcutFocus();
    }

    private void rememberTextInputFocusIntent(MouseEvent event) {
        userEditingTextInput = isTextInputTarget(event.getTarget());
    }

    private boolean isUserEditingTextInput(KeyEvent event) {
        return userEditingTextInput
                && (isTextInputTarget(event.getTarget()) || isTextInputTarget(focusedNode()));
    }

    private Node focusedNode() {
        return shortcutScene == null ? null : shortcutScene.getFocusOwner();
    }

    private boolean isTextInputTarget(Object target) {
        if (target instanceof TextInputControl) {
            return true;
        }

        if (!(target instanceof Node node)) {
            return false;
        }

        for (Node current = node; current != null; current = current.getParent()) {
            if (current instanceof TextInputControl) {
                return true;
            }
        }

        return false;
    }

    private void resetShortcutFocus() {
        userEditingTextInput = false;

        if (appShell != null) {
            Platform.runLater(appShell::requestFocus);
        }
    }

    private VBox createAccountSettingsPage(String selectedSection) {
        String safeSection = clean(selectedSection).isBlank() ? ACCOUNT_SECTION : selectedSection;

        Label titleLabel = new Label(safeSection);
        titleLabel.getStyleClass().add("page-title");

        Label subtitleLabel = new Label(accountPageSubtitle(safeSection));
        subtitleLabel.getStyleClass().add("page-subtitle");

        VBox intro = new VBox(3, titleLabel, subtitleLabel);
        intro.getStyleClass().add("page-heading-copy");

        VBox nav = buildAccountSectionNav(safeSection);
        VBox detailsPanel = new VBox(20);
        detailsPanel.getStyleClass().addAll("portal-card", "settings-panel");
        detailsPanel.setMaxWidth(Double.MAX_VALUE);
        detailsPanel.getChildren().setAll(
                switch (safeSection) {
                    case ACCOUNT_SECTION -> buildAccountProfileSection();
                    case PRIVACY_SECTION -> buildEmptyAccountSection(PRIVACY_SECTION);
                    default -> buildEmptyAccountSection(safeSection);
                }
        );

        HBox layout = new HBox(24, nav, detailsPanel);
        layout.getStyleClass().add("settings-layout");
        layout.setFillHeight(false);
        HBox.setHgrow(detailsPanel, Priority.ALWAYS);

        VBox page = new VBox(18, intro, layout);
        page.getStyleClass().addAll("portal-page", "main-content");
        page.setMaxWidth(Double.MAX_VALUE);
        page.setMaxHeight(Double.MAX_VALUE);

        return page;
    }

    private String accountPageSubtitle(String section) {
        return switch (section) {
            case ACCOUNT_SECTION -> "Manage your account information and password.";
            case PRIVACY_SECTION -> "Settings and privacy options.";
            default -> "";
        };
    }

    private VBox buildAccountSectionNav(String selectedSection) {
        VBox nav = new VBox();
        nav.getStyleClass().add("settings-nav");
        nav.setMaxHeight(Region.USE_PREF_SIZE);

        List<String> sections = List.of(ACCOUNT_SECTION, PRIVACY_SECTION);

        for (int index = 0; index < sections.size(); index++) {
            String section = sections.get(index);
            Button button = new Button();
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            button.getStyleClass().addAll("settings-nav-button", "admin-account-section-button");
            button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

            HBox row = new HBox();
            row.getStyleClass().add("settings-nav-row");
            row.setAlignment(Pos.CENTER_LEFT);

            Label label = new Label(section);
            label.getStyleClass().add("settings-nav-label");
            row.getChildren().add(label);

            button.setGraphic(row);

            if (index == 0) {
                button.getStyleClass().add("settings-nav-button-first");
            }

            if (index == sections.size() - 1) {
                button.getStyleClass().add("settings-nav-button-last");
            }

            if (section.equals(selectedSection)) {
                button.getStyleClass().add(ACTIVE_NAV_CLASS);
            }

            button.setOnAction(event -> showAccountSettingsPage(section));
            nav.getChildren().add(button);
        }

        return nav;
    }

    private Node buildAccountProfileSection() {
        User account = currentAccountUser();
        UserPortalModel.AccountProfile fallbackProfile = portalModel.fetchAccountProfile();

        Label heading = new Label("Account Information");
        heading.getStyleClass().add("settings-section-heading");

        TextField nameField = createAccountTextField(displayNameFor(account, fallbackProfile));
        TextField usernameField = createAccountTextField(account == null ? "" : clean(account.getUsername()));
        TextField emailField = createAccountTextField(account == null ? "" : clean(account.getEmail()));
        TextField roleField = createAccountTextField(account == null ? "User" : clean(account.getRole()));
        TextField statusField = createAccountTextField(account == null ? "Active" : clean(account.getStatus()));

        roleField.setEditable(false);
        statusField.setEditable(false);
        roleField.getStyleClass().add("admin-account-readonly-field");
        statusField.getStyleClass().add("admin-account-readonly-field");

        PasswordField newPasswordField = createAccountPasswordField("New password");
        PasswordField confirmPasswordField = createAccountPasswordField("Confirm new password");

        Label saveMessage = new Label();
        saveMessage.getStyleClass().add("portal-inline-message");
        saveMessage.setVisible(false);
        saveMessage.setManaged(false);

        Button saveButton = new Button("Save Changes");
        saveButton.getStyleClass().add("portal-primary-button");
        saveButton.setOnAction(event -> saveAccountProfile(
                account,
                nameField,
                usernameField,
                emailField,
                newPasswordField,
                confirmPasswordField,
                saveMessage
        ));

        VBox accountForm = new VBox(14,
                formField("Full Name", nameField),
                formField("Username", usernameField),
                formField("Email Address", emailField),
                formField("Role", roleField),
                formField("Status", statusField)
        );
        accountForm.getStyleClass().add("settings-form");

        VBox passwordForm = new VBox(14,
                sectionLabel("Password"),
                formField("New Password", newPasswordField),
                formField("Confirm Password", confirmPasswordField)
        );
        passwordForm.getStyleClass().add("settings-form");

        HBox actions = new HBox(12, saveButton, saveMessage);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(20,
                heading,
                accountForm,
                divider(),
                passwordForm,
                divider(),
                actions
        );
        content.getStyleClass().add("settings-form");

        return content;
    }

    private Node buildEmptyAccountSection(String title) {
        Label heading = new Label(title);
        heading.getStyleClass().add("settings-section-heading");

        VBox content = new VBox(20, heading);
        content.getStyleClass().add("settings-form");
        return content;
    }

    private void saveAccountProfile(User account,
                                    TextField nameField,
                                    TextField usernameField,
                                    TextField emailField,
                                    PasswordField newPasswordField,
                                    PasswordField confirmPasswordField,
                                    Label saveMessage) {
        if (account == null) {
            showInlineMessage(saveMessage, "Could not find the current account.", false);
            return;
        }

        String newPassword = newPasswordField.getText() == null ? "" : newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        if (!newPassword.isBlank() || !confirmPassword.isBlank()) {
            if (!newPassword.equals(confirmPassword)) {
                showInlineMessage(saveMessage, "Passwords do not match.", false);
                return;
            }
        }

        try {
            User updatedUser = userManager.updateCurrentAccount(
                    new UserManager.AccountInput(
                            nameField.getText(),
                            usernameField.getText(),
                            emailField.getText(),
                            newPassword
                    )
            );

            UserSession.setCurrentUser(updatedUser);
            configureAccount();

            newPasswordField.clear();
            confirmPasswordField.clear();

            showInlineMessage(saveMessage, "Changes saved.", true);
        } catch (RuntimeException exception) {
            showInlineMessage(saveMessage, exception.getMessage(), false);
        }
    }

    private User currentAccountUser() {
        return userManager.getCurrentAccount();
    }

    private VBox formField(String labelText, Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        return new VBox(8, label, field);
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("portal-section-title");
        return label;
    }

    private TextField createAccountTextField(String value) {
        TextField field = new TextField(value);
        field.getStyleClass().add("portal-text-field");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private PasswordField createAccountPasswordField(String promptText) {
        PasswordField field = new PasswordField();
        field.setPromptText(promptText);
        field.getStyleClass().add("portal-text-field");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private Region divider() {
        Region divider = new Region();
        divider.getStyleClass().add("portal-divider");
        return divider;
    }

    private void showInlineMessage(Label messageLabel, String message, boolean success) {
        messageLabel.getStyleClass().removeAll("success", "error");
        messageLabel.getStyleClass().add(success ? "success" : "error");
        messageLabel.setText(clean(message).isBlank() ? "Something went wrong." : message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showKeyboardShortcutsDialog() {
        hideAccountDropdown();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setHeaderText(null);
        dialog.setTitle("Keyboard Shortcuts");

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Node defaultCloseButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (defaultCloseButton != null) {
            defaultCloseButton.setVisible(false);
            defaultCloseButton.setManaged(false);
        }

        dialog.getDialogPane().getStyleClass().addAll(
                "app-shell",
                "weblager-shortcuts-dialog-pane"
        );

        if (isDarkModeEnabled()) {
            dialog.getDialogPane().getStyleClass().add(DARK_MODE_CLASS);
        }

        if (appShell != null && appShell.getScene() != null) {
            dialog.initOwner(appShell.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().setAll(appShell.getScene().getStylesheets());
        }

        dialog.getDialogPane().setPrefSize(560, 430);
        dialog.getDialogPane().setMaxSize(560, 430);
        dialog.getDialogPane().setContent(createKeyboardShortcutsContent(dialog));

        dialog.showAndWait();
    }

    private VBox createKeyboardShortcutsContent(Dialog<ButtonType> dialog) {
        VBox root = new VBox();
        root.getStyleClass().add("weblager-shortcuts-root");

        root.getChildren().addAll(
                createKeyboardShortcutsHeader(dialog),
                createKeyboardShortcutsBody(dialog)
        );

        return root;
    }

    private HBox createKeyboardShortcutsHeader(Dialog<ButtonType> dialog) {
        Label keyboardIcon = new Label("\ue981");
        keyboardIcon.getStyleClass().addAll("prime-icon", "weblager-shortcuts-title-icon");
        PrimeIcons.applyFont(keyboardIcon);

        StackPane iconShell = new StackPane(keyboardIcon);
        iconShell.getStyleClass().add("weblager-shortcuts-title-icon-shell");

        Label title = new Label("Keyboard Shortcuts");
        title.getStyleClass().add("weblager-shortcuts-title");

        Label subtitle = new Label("Common actions for scanning and review.");
        subtitle.getStyleClass().add("weblager-shortcuts-subtitle");

        VBox copy = new VBox(3, title, subtitle);
        copy.getStyleClass().add("weblager-shortcuts-header-copy");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeButton = new Button("×");
        closeButton.getStyleClass().add("weblager-shortcuts-x-button");
        closeButton.setFocusTraversable(false);
        closeButton.setOnAction(event -> {
            dialog.setResult(ButtonType.CLOSE);
            dialog.close();
        });

        HBox header = new HBox(18, iconShell, copy, spacer, closeButton);
        header.getStyleClass().add("weblager-shortcuts-header");
        header.setAlignment(Pos.CENTER_LEFT);

        return header;
    }

    private VBox createKeyboardShortcutsBody(Dialog<ButtonType> dialog) {
        VBox sections = new VBox(8);
        sections.getChildren().setAll(
                createShortcutSection("General shortcuts", "Save", "Undo", "Search / jump", "Escape", "Shortcut help"),
                createShortcutSection("Navigation shortcuts", "Next section / scan page", "Previous section / scan page", "Export"),
                createShortcutSection("Scanning shortcuts", "Rotate", "Delete", "Zoom in", "Zoom out")
        );

        Label footerText = new Label("Open this dialog anytime from the keyboard button, Fn + F1, or ?.");
        footerText.getStyleClass().add("weblager-shortcuts-footer-text");

        Button closeButton = new Button("Close");
        closeButton.getStyleClass().add("weblager-shortcuts-close-button");
        closeButton.setOnAction(event -> {
            dialog.setResult(ButtonType.CLOSE);
            dialog.close();
        });

        VBox body = new VBox(9, sections, footerText, closeButton);
        body.getStyleClass().add("weblager-shortcuts-body");
        body.setAlignment(Pos.TOP_CENTER);

        return body;
    }

    private VBox createShortcutSection(String titleText, String... actionNames) {
        Label title = new Label(titleText);
        title.getStyleClass().add("weblager-shortcuts-section-title");

        VBox rows = new VBox(4);
        for (String actionName : actionNames) {
            findShortcut(actionName).ifPresent(shortcut -> rows.getChildren().add(createShortcutLine(shortcut)));
        }

        VBox section = new VBox(8, title, rows);
        section.getStyleClass().add("weblager-shortcuts-section");
        return section;
    }

    private java.util.Optional<KeyboardShortcut> findShortcut(String actionName) {
        return shortcutManager.getShortcuts().stream()
                .filter(shortcut -> actionName.equals(shortcut.getActionName()))
                .findFirst();
    }

    private HBox createShortcutLine(KeyboardShortcut shortcut) {
        Label key = new Label(shortcut.getDisplayKeys());
        key.getStyleClass().add("weblager-shortcuts-key");

        Label action = new Label(shortcut.getActionName() + " - " + shortcut.getDescription());
        action.getStyleClass().add("weblager-shortcuts-action-description");
        action.setWrapText(true);

        HBox row = new HBox(12, key, action);
        row.getStyleClass().add("weblager-shortcuts-simple-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox createKeyboardShortcutRow(ShortcutData shortcut) {
        Label icon = new Label(shortcut.icon());
        icon.getStyleClass().add("weblager-shortcuts-row-icon");

        StackPane iconShell = new StackPane(icon);
        iconShell.getStyleClass().add("weblager-shortcuts-row-icon-shell");

        Label key = new Label(shortcut.key());
        key.getStyleClass().add("weblager-shortcuts-key");

        Label label = new Label(shortcut.label());
        label.getStyleClass().add("weblager-shortcuts-action-label");

        Label description = new Label(shortcut.description());
        description.getStyleClass().add("weblager-shortcuts-action-description");
        description.setWrapText(true);

        HBox titleRow = new HBox(10, key, label);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox text = new VBox(4, titleRow, description);
        text.getStyleClass().add("weblager-shortcuts-row-copy");

        HBox row = new HBox(16, iconShell, text);
        row.getStyleClass().add("weblager-shortcuts-row");
        row.setAlignment(Pos.CENTER_LEFT);

        return row;
    }

    private ShortcutData shortcutData(KeyboardShortcut shortcut) {
        return new ShortcutData(
                shortcutIcon(shortcut.getActionName()),
                shortcut.getActionName(),
                shortcut.getDisplayKeys(),
                shortcut.getDescription()
        );
    }

    private String shortcutIcon(String actionName) {
        return switch (actionName) {
            case "Next section / scan page" -> "→";
            case "Previous section / scan page" -> "←";
            case "Rotate" -> "↻";
            case "Delete" -> "⌫";
            case "Undo" -> "↶";
            case "Save" -> "💾";
            case "Search / jump" -> "⌕";
            case "Export" -> "⇩";
            case "Zoom in" -> "+";
            case "Zoom out" -> "−";
            case "Escape" -> "Esc";
            case "Shortcut help" -> "?";
            default -> "•";
        };
    }

    private record ShortcutData(String icon, String label, String key, String description) {
    }

    private HBox shortcutRow(String shortcut, String description) {
        Label shortcutLabel = new Label(shortcut);
        shortcutLabel.getStyleClass().add("portal-section-title");
        shortcutLabel.setMinWidth(72);

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("portal-muted");
        descriptionLabel.setWrapText(true);

        HBox row = new HBox(12, shortcutLabel, descriptionLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(descriptionLabel, Priority.ALWAYS);

        return row;
    }
    private void logout() {
        UserSession.clearCurrentUser();
        hideAccountDropdown();

        if (mainApp == null) {
            return;
        }

        try {
            mainApp.showLoginView();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not return to login.", exception);
        }
    }

    private String displayNameFor(User user, UserPortalModel.AccountProfile fallbackProfile) {
        if (user != null) {
            if (!clean(user.getName()).isBlank()) {
                return clean(user.getName());
            }

            if (!clean(user.getUsername()).isBlank()) {
                return clean(user.getUsername());
            }
        }

        if (fallbackProfile != null && !clean(fallbackProfile.fullName()).isBlank()) {
            return clean(fallbackProfile.fullName());
        }

        return "User";
    }

    private String accountDetailFor(User user) {
        if (user == null) {
            return "User account";
        }

        if (!clean(user.getEmail()).isBlank()) {
            return clean(user.getEmail());
        }

        if (!clean(user.getUsername()).isBlank()) {
            return clean(user.getUsername());
        }

        return clean(user.getRole()).isBlank() ? "User account" : clean(user.getRole()) + " account";
    }

    private String initialsFor(String name) {
        String cleanedName = clean(name);

        if (cleanedName.isBlank()) {
            return "U";
        }

        String[] parts = cleanedName.split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }

        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
