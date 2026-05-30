package easv.gui.controller.user;

import easv.be.User;
import easv.bll.KeyboardShortcut;
import easv.bll.ShortcutManager;
import easv.bll.UserManager;
import easv.bll.UserSession;
import easv.gui.controller.util.BackgroundExecutor;
import easv.gui.MainApp;
import easv.gui.controller.util.PrimeIcons;
import easv.gui.controller.util.SkeletonFactory;
import easv.gui.UserPortalModel;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.prefs.Preferences;

public class UserController implements UserNavigator {

    private static final String ACTIVE_NAV_CLASS = "active";
    private static final String DARK_MODE_CLASS = "dark";

    private static final String PREFERENCES_NODE = "easv.gui.portal";
    private static final String DARK_MODE_PREFERENCE_KEY = "userPortal.darkMode";

    private static final String ACCOUNT_SECTION = "Edit Profile";
    private static final String LIGHT_MODE_LOGO =
            "/images/weblager/styleguide/Main Blue/LogoBlueH.png";

    private static final String DARK_MODE_LOGO =
            "/images/weblager/styleguide/DarkmodeBlue/LogoBlue2H.png";

    private static final String MOON_ICON_PATH =
            "M12 3.25a8.75 8.75 0 1 0 8.75 8.75c0-.45-.04-.89-.1-1.32A6.75 6.75 0 0 1 12.32 3.4c-.1-.05-.21-.1-.32-.15zM5.25 12A6.74 6.74 0 0 1 9.83 5.6a8.75 8.75 0 0 0 8.57 8.57A6.75 6.75 0 0 1 5.25 12z";

    private static final String SUN_ICON_PATH =
            "M12 5.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13zm0 2a4.5 4.5 0 1 1 0 9 4.5 4.5 0 0 1 0-9zM11 1h2v3h-2V1zm0 19h2v3h-2v-3zM1 11h3v2H1v-2zm19 0h3v2h-3v-2zM4.22 2.81l2.12 2.12-1.41 1.41L2.81 4.22l1.41-1.41zm14.85 14.85 2.12 2.12-1.41 1.41-2.12-2.12 1.41-1.41zM19.78 2.81l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12zM4.93 17.66l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12z";

    private static final String EYE_ICON_PATH =
            "M12 5c5.5 0 9.5 5.3 9.5 7s-4 7-9.5 7S2.5 14.7 2.5 12 6.5 5 12 5zm0 2C8.4 7 5.5 10.4 4.7 12 5.5 13.6 8.4 17 12 17s6.5-3.4 7.3-5C18.5 10.4 15.6 7 12 7zm0 1.8A3.2 3.2 0 1 1 8.8 12 3.2 3.2 0 0 1 12 8.8z";

    private static final String EYE_OFF_ICON_PATH =
            "M3.8 4.9 19.1 20.2l-1.4 1.4-2.3-2.3A11.9 11.9 0 0 1 12 19c-5.5 0-9.5-5.3-9.5-7 0-1 1.1-2.7 3.1-4.3L2.4 6.3l1.4-1.4zm3.3 3.3C5.6 9.3 4.9 11 4.7 12c.8 1.6 3.7 5 7.3 5 1 0 2-.3 2.8-.7l-1.8-1.8A3.2 3.2 0 0 1 9.5 11l-2.4-2.4zM12 5c5.5 0 9.5 5.3 9.5 7 0 1-.9 2.4-2.4 3.8l-1.5-1.5c.8-.8 1.4-1.7 1.7-2.3-.8-1.6-3.7-5-7.3-5-1.2 0-2.3.4-3.3.9L7.1 6.3C8.5 5.5 10.1 5 12 5zm-.2 3.8c1.8 0 3.2 1.4 3.2 3.2 0 .6-.2 1.2-.5 1.8l-4.5-4.5c.5-.3 1.1-.5 1.8-.5z";

    @FXML private StackPane appShell;
    @FXML private BorderPane appRoot;
    @FXML private StackPane contentHost;

    @FXML private StackPane brandMark;
    @FXML private Label brandLogoFallbackLabel;
    @FXML private ImageView brandLogoImageView;

    @FXML private ToggleButton dashboardNavItem;
    @FXML private ToggleButton scanNavItem;
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
    @FXML private VBox notificationListContainer;
    @FXML private Region notificationUnreadDot;
    @FXML private Label accountDropdownNameLabel;
    @FXML private Label accountDropdownDetailLabel;
    @FXML private Button editProfileMenuButton;
    @FXML private Button logoutMenuButton;
    @FXML private ToggleButton darkModeToggleButton;
    @FXML private SVGPath darkModeToggleIcon;

    private final UserPortalModel portalModel = new UserPortalModel();
    private final ShortcutManager shortcutManager = new ShortcutManager();
    private final UserManager userManager = new UserManager();
    private final Preferences preferences = Preferences.userRoot().node(PREFERENCES_NODE);

    private MainApp mainApp;
    private UserPortalModel.RecentScanItem pendingRecentScanItem;
    private UserPortalModel.HistoryItem pendingHistoryScanItem;
    private ScanController activeScanController;
    private ContextMenu accountMenu;
    private Scene shortcutScene;
    private UserPage currentPage;
    private boolean userEditingTextInput;
    private int notificationRenderToken;
    private String loginPassword;
    private boolean forcedPasswordDialogShown;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setLoginPassword(String loginPassword) {
        this.loginPassword = loginPassword;
    }

    @FXML
    private void initialize() {
        configureAccount();
        configureAccountMenu();
        configureNotificationMenu();
        configureKeyboardShortcutsButton();
        configureGlobalShortcuts();
        configureThemeToggle();
        configureNavigation();
        showPage(UserPage.DASHBOARD);
        Platform.runLater(this::showFirstLoginPasswordDialogIfNeeded);
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
            // Shown via a ContextMenu anchored to the button (see
            // toggleAccountDropdown) so it stays connected to the button.
            accountDropdownPane.managedProperty().bind(accountDropdownPane.visibleProperty());
        }

        if (accountMenuButton != null) {
            accountMenuButton.setOnAction(event -> toggleAccountDropdown());
        }

        if (editProfileMenuButton != null) {
            editProfileMenuButton.setOnAction(event -> showAccountSettingsPage(ACCOUNT_SECTION));
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

    private void configureGlobalShortcuts() {
        if (appShell == null) {
            return;
        }

        appShell.setFocusTraversable(true);

        if (appShell.getScene() != null) {
            registerSceneShortcuts(appShell.getScene());
        }

        appShell.sceneProperty().addListener((observable, oldScene, newScene) -> registerSceneShortcuts(newScene));
    }

    private void registerSceneShortcuts(Scene scene) {
        if (scene == null || scene == shortcutScene) {
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

        if (currentPage == UserPage.SCAN
                && activeScanController != null
                && activeScanController.handleSelectedPageArrowShortcut(event)) {
            event.consume();
            return;
        }

        if (event.getCode() == KeyCode.LEFT) {
            showAdjacentShortcutPage(-1);
            event.consume();
            return;
        }

        if (event.getCode() == KeyCode.RIGHT) {
            showAdjacentShortcutPage(1);
            event.consume();
            return;
        }

        if (currentPage == UserPage.SCAN
                && activeScanController != null
                && activeScanController.handlePrioritizedGlobalShortcut(event)) {
            event.consume();
        }
    }

    private void handleGlobalTypedShortcut(KeyEvent event) {
        if (event.isConsumed() || isUserEditingTextInput(event)) {
            return;
        }

        if ("?".equals(event.getCharacter())) {
            showKeyboardShortcutsDialog();
            event.consume();
            return;
        }

        if (currentPage == UserPage.SCAN
                && activeScanController != null
                && activeScanController.runTypedShortcut(event.getCharacter())) {
            event.consume();
        }
    }

    private void configureNotificationMenu() {
        if (notificationDropdownPane != null) {
            notificationDropdownPane.setMaxHeight(Region.USE_PREF_SIZE);
            notificationDropdownPane.setVisible(false);
            notificationDropdownPane.setManaged(false);
        }
        renderNotifications();

        if (notificationMenuButton != null) {
            notificationMenuButton.setOnAction(event -> {
                renderNotifications();
                toggleNotificationDropdown();
            });
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
                    resetShortcutFocus();
                    showPage(page);
                });
            }
        }
    }

    @Override
    public void showPage(UserPage page) {
        resetShortcutFocus();
        hideAccountDropdown();
        hideNotificationDropdown();
        loadPage(page);
        setActiveNavItem(getNavItem(page));
        currentPage = page;
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

    private void loadPage(UserPage page) {
        if (activeScanController != null) {
            activeScanController.stopAutosave();
        }
        activeScanController = null;

        if (!page.hasFxml()) {
            Node content = createProgrammaticPage(page);
            PrimeIcons.applyFont(content);

            if (shouldWrapScrollable(page)) {
                content = wrapScrollable(content);
            } else if (content instanceof Region region) {
                configureRegionPageSize(region);
            }

            StackPane.setAlignment(content, Pos.TOP_CENTER);
            contentHost.getChildren().setAll(content);
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

            configureLoadedController(loader.getController());
            PrimeIcons.applyFont(loadedPage);
            configureLoadedPageSize(loadedPage);
            StackPane.setAlignment(loadedPage, Pos.TOP_CENTER);

            contentHost.getChildren().setAll(loadedPage);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load page: " + page.fxmlPath(), exception);
        }
    }

    private Node createProgrammaticPage(UserPage page) {
        return switch (page) {
            case DASHBOARD -> new DashboardController(portalModel, this).create();
            case EXPORTS -> new ExportsController(portalModel).create();
            case EDIT_PROFILE -> createAccountSettingsPage(ACCOUNT_SECTION);
            case SETTINGS -> createAccountSettingsPage(ACCOUNT_SECTION);
            default -> createMissingPagePlaceholder(page.title());
        };
    }

    private void showAdjacentShortcutPage(int direction) {
        List<UserPage> pages = getShortcutNavigationPages();
        int currentIndex = pages.indexOf(currentPage);

        if (currentIndex < 0) {
            currentIndex = 0;
        }

        int nextIndex = Math.floorMod(currentIndex + direction, pages.size());
        showPage(pages.get(nextIndex));
    }

    private List<UserPage> getShortcutNavigationPages() {
        return List.of(
                UserPage.DASHBOARD,
                UserPage.SCAN,
                UserPage.ASSIGNED_QA,
                UserPage.EXPORTS
        );
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
            activeScanController = scanController;
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
                setNavItemActive(navItem, navItem == activeNavItem);
            }
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
                assignedQANavItem
        );
    }

    private ToggleButton getNavItem(UserPage page) {
        return switch (page) {
            case DASHBOARD -> dashboardNavItem;
            case SCAN -> scanNavItem;
            case ASSIGNED_QA -> assignedQANavItem;
            case EXPORTS -> null;
            case EDIT_PROFILE -> null;
            case SETTINGS -> null;
        };
    }

    private void rememberTextInputFocusIntent(MouseEvent event) {
        userEditingTextInput = isTextInputTarget(event.getTarget());
    }

    private boolean isUserEditingTextInput(KeyEvent event) {
        return isTextInputTarget(event.getTarget())
                || isTextInputTarget(focusedNode())
                || (userEditingTextInput
                && (isTextInputTarget(event.getTarget()) || isTextInputTarget(focusedNode())));
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

    private void toggleAccountDropdown() {
        if (accountDropdownPane == null || accountMenuButton == null) {
            return;
        }

        if (accountMenu != null && accountMenu.isShowing()) {
            accountMenu.hide();
            return;
        }

        hideNotificationDropdown();

        if (accountMenu == null) {
            accountMenu = new ContextMenu();
            accountMenu.getStyleClass().add("admin-account-popover-menu");
            accountMenu.setAutoHide(true);

            // Reparent the existing dropdown VBox into the context menu so all
            // its wiring is preserved, but it now pops up anchored directly
            // under the account button.
            if (accountDropdownPane.getParent() instanceof Pane parent) {
                parent.getChildren().remove(accountDropdownPane);
            }
            accountDropdownPane.setVisible(true);
            CustomMenuItem item = new CustomMenuItem(accountDropdownPane, false);
            item.getStyleClass().add("admin-account-popover-item");
            accountMenu.getItems().setAll(item);
        }

        // Use the account button width as a floor — the dropdown is at least
        // that wide so it never looks orphaned, but free to grow when its
        // contents (e.g. "Light Mode" toggle) need more room. No clipping.
        double buttonWidth = accountMenuButton.getWidth();
        accountDropdownPane.setMinWidth(buttonWidth);
        accountDropdownPane.setPrefWidth(Region.USE_COMPUTED_SIZE);
        accountDropdownPane.setMaxWidth(Region.USE_COMPUTED_SIZE);
        accountMenu.show(accountMenuButton, Side.BOTTOM, 0, 4);
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
        if (accountMenu != null) {
            accountMenu.hide();
        }
    }

    private void hideNotificationDropdown() {
        if (notificationDropdownPane != null) {
            notificationDropdownPane.setVisible(false);
            notificationDropdownPane.setManaged(false);
        }
    }

    private void markAllNotificationsRead() {
        BackgroundExecutor.io().execute(() -> {
            portalModel.markAllNotificationsRead();
            Platform.runLater(this::renderNotifications);
        });
    }

    private void expandNotificationDropdown() {
        if (notificationDropdownPane == null || viewAllNotificationsButton == null) {
            return;
        }
        renderNotifications(Integer.MAX_VALUE);
        viewAllNotificationsButton.setVisible(false);
        viewAllNotificationsButton.setManaged(false);
    }

    private void renderNotifications() {
        renderNotifications(4);
    }

    private void renderNotifications(int visibleCount) {
        if (notificationListContainer == null) {
            return;
        }

        int requestToken = ++notificationRenderToken;
        SkeletonFactory.stopShimmers(notificationListContainer);
        notificationListContainer.getChildren().setAll(
                createNotificationSkeletonRow(),
                createNotificationSkeletonRow(),
                createNotificationSkeletonRow(),
                createNotificationSkeletonRow()
        );
        if (markNotificationsReadButton != null) {
            markNotificationsReadButton.setText("Mark all as read");
            markNotificationsReadButton.setDisable(true);
        }
        if (viewAllNotificationsButton != null) {
            viewAllNotificationsButton.setVisible(false);
            viewAllNotificationsButton.setManaged(false);
        }

        BackgroundExecutor.io().execute(() -> {
            List<easv.bll.QAService.NotificationSnapshot> notifications = portalModel.fetchNotifications();
            Platform.runLater(() -> applyNotifications(requestToken, visibleCount, notifications));
        });
    }

    private void applyNotifications(int requestToken,
                                    int visibleCount,
                                    List<easv.bll.QAService.NotificationSnapshot> notifications) {
        if (requestToken != notificationRenderToken || notificationListContainer == null) {
            return;
        }

        SkeletonFactory.stopShimmers(notificationListContainer);
        notificationListContainer.getChildren().clear();

        if (notifications == null || notifications.isEmpty()) {
            notificationListContainer.getChildren().add(createNotificationStateLabel("No notifications"));
        } else {
            int limit = Math.max(0, Math.min(visibleCount, notifications.size()));
            for (int index = 0; index < limit; index++) {
                notificationListContainer.getChildren().add(createNotificationRow(notifications.get(index)));
            }
        }

        boolean hasUnread = notifications != null && notifications.stream().anyMatch(easv.bll.QAService.NotificationSnapshot::unread);
        if (notificationUnreadDot != null) {
            notificationUnreadDot.setVisible(hasUnread);
            notificationUnreadDot.setManaged(hasUnread);
        }
        if (markNotificationsReadButton != null) {
            markNotificationsReadButton.setText(hasUnread ? "Mark all as read" : "All read");
            markNotificationsReadButton.setDisable(!hasUnread);
        }
        if (viewAllNotificationsButton != null) {
            boolean showViewAll = notifications != null
                    && notifications.size() > Math.max(0, visibleCount)
                    && visibleCount != Integer.MAX_VALUE;
            viewAllNotificationsButton.setVisible(showViewAll);
            viewAllNotificationsButton.setManaged(showViewAll);
        }
    }

    private Label createNotificationStateLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("exports-footer-text");
        return label;
    }

    private HBox createNotificationSkeletonRow() {
        Region marker = SkeletonFactory.circle(8);

        Region title = SkeletonFactory.line(160, 12);
        Region copy = SkeletonFactory.line(220, 10, SkeletonFactory.Intensity.LIGHT);
        VBox body = new VBox(6, title, copy);
        HBox.setHgrow(body, Priority.ALWAYS);

        Region time = SkeletonFactory.line(40, 10, SkeletonFactory.Intensity.LIGHT);

        HBox row = new HBox(12, marker, body, time);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("user-notification-item");
        return row;
    }

    private HBox createNotificationRow(easv.bll.QAService.NotificationSnapshot notification) {
        Region marker = new Region();
        marker.getStyleClass().add(notification.unread()
                ? "user-notification-unread-dot"
                : "user-notification-read-spacer");

        Label title = new Label(notification.title());
        title.getStyleClass().add("user-notification-item-title");

        Label copy = new Label(notification.message());
        copy.getStyleClass().add("user-notification-item-copy");
        copy.setWrapText(true);

        VBox body = new VBox(3, title, copy);
        HBox.setHgrow(body, Priority.ALWAYS);

        Label time = new Label(formatNotificationTime(notification.createdAt()));
        time.getStyleClass().add("user-notification-time");

        HBox row = new HBox(12, marker, body, time);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("user-notification-item");
        return row;
    }

    private String formatNotificationTime(Instant createdAt) {
        if (createdAt == null) {
            return "-";
        }
        Duration age = Duration.between(createdAt, Instant.now());
        long minutes = Math.max(0, age.toMinutes());
        if (minutes < 1) {
            return "now";
        }
        if (minutes < 60) {
            return minutes + "m ago";
        }
        long hours = age.toHours();
        if (hours < 24) {
            return hours + "h ago";
        }
        long days = age.toDays();
        return days + "d ago";
    }

    private void showAccountSettingsPage(String selectedSection) {
        hideAccountDropdown();
        hideNotificationDropdown();
        setActiveNavItem(null);
        Node content = wrapScrollable(createAccountSettingsPage(selectedSection));
        StackPane.setAlignment(content, Pos.TOP_CENTER);
        contentHost.getChildren().setAll(content);
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
            default -> "";
        };
    }

    private VBox buildAccountSectionNav(String selectedSection) {
        VBox nav = new VBox();
        nav.getStyleClass().add("settings-nav");
        nav.setMaxHeight(Region.USE_PREF_SIZE);

        List<String> sections = List.of(ACCOUNT_SECTION);

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
                            newPassword,
                            newPassword.isBlank() ? null : Boolean.FALSE
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

    private void showFirstLoginPasswordDialogIfNeeded() {
        if (forcedPasswordDialogShown || appShell == null) {
            return;
        }

        if (appShell.getScene() == null) {
            Platform.runLater(this::showFirstLoginPasswordDialogIfNeeded);
            return;
        }

        User currentUser = currentAccountUser();
        if (currentUser == null || !currentUser.isMustChangePassword()) {
            return;
        }

        forcedPasswordDialogShown = true;
        showFirstLoginPasswordDialog(currentUser);
    }

    private void showFirstLoginPasswordDialog(User account) {
        Dialog<ButtonType> dialog = new Dialog<>();
        final boolean[] passwordChangeCompleted = {false};
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setHeaderText(null);
        dialog.setTitle("Change Temporary Password");
        dialog.getDialogPane().getStyleClass().addAll("app-shell", "first-login-password-dialog-pane");

        if (appShell != null && appShell.getStyleClass().contains(DARK_MODE_CLASS)) {
            dialog.getDialogPane().getStyleClass().add(DARK_MODE_CLASS);
        }

        if (appShell != null && appShell.getScene() != null) {
            dialog.initOwner(appShell.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().setAll(appShell.getScene().getStylesheets());
        }

        String oldPassword = clean(loginPassword).isBlank() ? "Temporary password used at sign-in" : clean(loginPassword);
        TextField oldPasswordField = new TextField(oldPassword);
        oldPasswordField.setEditable(false);
        oldPasswordField.setFocusTraversable(false);
        oldPasswordField.getStyleClass().addAll("first-login-password-readonly-field", "first-login-password-short-field");
        HBox oldPasswordRow = new HBox(oldPasswordField);
        oldPasswordRow.setAlignment(Pos.CENTER_LEFT);
        oldPasswordRow.getStyleClass().add("first-login-password-input-row");

        PasswordVisibilityField newPasswordField = createPasswordVisibilityField("Enter new password");
        PasswordVisibilityField confirmPasswordField = createPasswordVisibilityField("Type the same password again");

        Label passwordHintLabel = new Label("Password must be at least 6 characters.");
        passwordHintLabel.getStyleClass().add("first-login-password-hint");

        Label validationLabel = new Label();
        validationLabel.getStyleClass().add("first-login-password-validation");
        validationLabel.setText(" ");
        validationLabel.setVisible(true);
        validationLabel.setManaged(true);

        Button changePasswordButton = new Button("Change Password");
        changePasswordButton.getStyleClass().add("portal-primary-button");
        changePasswordButton.setDefaultButton(true);
        changePasswordButton.setOnAction(event -> {
            if (!validateFirstLoginPasswordInputs(validationLabel, newPasswordField, confirmPasswordField, true)) {
                return;
            }

            String newPassword = newPasswordField.getText().trim();

            try {
                User updatedUser = userManager.updateCurrentAccount(
                        new UserManager.AccountInput(
                                account.getName(),
                                account.getUsername(),
                                account.getEmail(),
                                newPassword,
                                false
                        )
                );
                UserSession.setCurrentUser(updatedUser);
                configureAccount();
                loginPassword = null;
                passwordChangeCompleted[0] = true;
                dialog.setResult(ButtonType.OK);
                dialog.close();
            } catch (RuntimeException exception) {
                showFirstLoginValidation(validationLabel, exception.getMessage());
            }
        });

        newPasswordField.setOnAction(event -> changePasswordButton.fire());
        confirmPasswordField.setOnAction(event -> changePasswordButton.fire());

        VBox content = new VBox(16,
                createFirstLoginDialogTitle(account),
                createFirstLoginDialogField("Temporary Password", oldPasswordRow),
                createFirstLoginDialogField("New Password", newPasswordField.root()),
                passwordHintLabel,
                createFirstLoginDialogField("Confirm New Password", confirmPasswordField.root()),
                validationLabel,
                createFirstLoginDialogActions(changePasswordButton)
        );
        content.getStyleClass().add("first-login-password-dialog-root");
        content.setAlignment(Pos.TOP_CENTER);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(640);
        dialog.getDialogPane().setMinWidth(640);
        dialog.getDialogPane().setPrefHeight(Region.USE_COMPUTED_SIZE);
        dialog.getDialogPane().setMinHeight(Region.USE_COMPUTED_SIZE);

        dialog.setOnCloseRequest(event -> {
            if (!passwordChangeCompleted[0]) {
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    private VBox createFirstLoginDialogTitle(User account) {
        Label title = new Label("Change Your Temporary Password");
        title.getStyleClass().add("first-login-password-title");

        Label copy = new Label(clean(account.getName()).isBlank()
                ? "This account was created with a temporary password. Choose a new password before continuing."
                : clean(account.getName()) + ", this account was created with a temporary password. Choose a new password before continuing.");
        copy.setWrapText(true);
        copy.getStyleClass().add("first-login-password-copy");

        VBox header = new VBox(8, title, copy);
        header.getStyleClass().add("first-login-password-header");
        return header;
    }

    private VBox createFirstLoginDialogField(String labelText, Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("first-login-password-label");

        VBox wrapper = new VBox(8, label, field);
        wrapper.getStyleClass().add("first-login-password-field");
        return wrapper;
    }

    private HBox createFirstLoginDialogActions(Button changePasswordButton) {
        HBox actions = new HBox(changePasswordButton);
        actions.setAlignment(Pos.CENTER);
        actions.getStyleClass().add("first-login-password-actions");
        return actions;
    }

    private void showFirstLoginValidation(Label validationLabel, String message) {
        String cleanedMessage = clean(message);
        validationLabel.setText(cleanedMessage.isBlank() ? " " : cleanedMessage);
    }

    private boolean validateFirstLoginPasswordInputs(Label validationLabel,
                                                     PasswordVisibilityField newPasswordField,
                                                     PasswordVisibilityField confirmPasswordField,
                                                     boolean requireNewPassword) {
        String newPassword = newPasswordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        if (requireNewPassword && newPassword.isBlank()) {
            showFirstLoginValidation(validationLabel, "New password is required.");
            return false;
        }

        if (!newPassword.isBlank() && newPassword.length() < 6) {
            showFirstLoginValidation(validationLabel, "Password must be at least 6 characters.");
            return false;
        }

        if (!confirmPassword.isBlank() && !newPassword.equals(confirmPassword)) {
            showFirstLoginValidation(validationLabel, "Passwords do not match.");
            return false;
        }

        showFirstLoginValidation(validationLabel, "");
        return true;
    }

    private PasswordVisibilityField createPasswordVisibilityField(String promptText) {
        PasswordField hiddenField = createAccountPasswordField(promptText);
        TextField visibleField = createAccountTextField("");
        visibleField.setPromptText(promptText);
        visibleField.setManaged(false);
        visibleField.setVisible(false);

        hiddenField.textProperty().bindBidirectional(visibleField.textProperty());

        BooleanProperty showingPassword = new SimpleBooleanProperty(false);

        Button toggleButton = new Button();
        toggleButton.setFocusTraversable(false);
        toggleButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        toggleButton.getStyleClass().add("first-login-password-toggle-button");

        SVGPath icon = new SVGPath();
        icon.setContent(EYE_OFF_ICON_PATH);
        icon.getStyleClass().add("first-login-password-toggle-icon");
        toggleButton.setGraphic(icon);

        showingPassword.addListener((observable, oldValue, showPassword) -> {
            hiddenField.setVisible(!showPassword);
            hiddenField.setManaged(!showPassword);
            visibleField.setVisible(showPassword);
            visibleField.setManaged(showPassword);
            icon.setContent(showPassword ? EYE_ICON_PATH : EYE_OFF_ICON_PATH);
        });

        toggleButton.setOnAction(event -> {
            showingPassword.set(!showingPassword.get());
            if (showingPassword.get()) {
                visibleField.requestFocus();
                visibleField.positionCaret(visibleField.getText().length());
            } else {
                hiddenField.requestFocus();
                hiddenField.positionCaret(hiddenField.getText().length());
            }
        });

        StackPane fieldStack = new StackPane(hiddenField, visibleField);
        HBox.setHgrow(fieldStack, Priority.ALWAYS);
        fieldStack.getStyleClass().add("first-login-password-field-stack");

        HBox root = new HBox(8, fieldStack, toggleButton);
        root.setAlignment(Pos.CENTER_LEFT);
        root.getStyleClass().add("first-login-password-input-row");

        return new PasswordVisibilityField(root, hiddenField, visibleField);
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

        dialog.getDialogPane().setPrefSize(1000, 780);
        dialog.getDialogPane().setMaxSize(1000, 780);
        dialog.getDialogPane().setContent(createKeyboardShortcutsContent(dialog));
        PrimeIcons.applyFont(dialog.getDialogPane());
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
        VBox cards = new VBox(18,
                createShortcutCardsRow(
                        createShortcutCard("M5 12h14M13 6l6 6-6 6", "->", "Next Page", "Move to the next scanned page."),
                        createShortcutCard("M19 12H5m6-6-6 6 6 6", "<-", "Previous Page", "Move to the previous scanned page.")
                ),
                createShortcutCardsRow(
                        createShortcutCard("M20 12a8 8 0 1 1-2.34-5.66M20 4v5h-5", "R", "Rotate", "Rotate the selected page by degree."),
                        createShortcutCard("M7 7h10M10 11v6m4-6v6M9 7l1-2h4l1 2m-8 0 1 13h8l1-13", "Del", "Delete", "Delete the selected page.")
                ),
                createShortcutCardsRow(
                        createShortcutCard("M9 7l-5 5 5 5M4 12h11a5 5 0 0 1 0 10", "Ctrl + Z", "Undo", "Undo the last supported action."),
                        createShortcutCard("M5 4h12l2 2v14H5zM8 4v6h8V4M8 15h8v5H8z", "Ctrl + S", "Save", "Save the current work.")
                ),
                createShortcutCardsRow(
                        createShortcutCard("M10 18a8 8 0 1 1 5.66-2.34L21 21", "Ctrl + F", "Search / Jump", "Search for a page, file, or document."),
                        createShortcutCard("M12 4v12m-5-5 5 5 5-5M5 20h14", "Ctrl + E", "Export", "Open the export dialog.")
                ),
                createShortcutCardsRow(
                        createShortcutCard("M10 17a7 7 0 1 1 4.95-2.05L21 21M10 7v6m-3-3h6", "+", "Zoom in", "Zoom in on the current page."),
                        createShortcutCard("M10 17a7 7 0 1 1 4.95-2.05L21 21M7 10h6", "-", "Zoom out", "Zoom out from the current page.")
                ),
                createShortcutCardsRow(
                        createShortcutCard("M7 7l10 10M17 7 7 17", "Esc", "Escape", "Close modals or cancel the current action."),
                        createShortcutCard("M5 5h14v14H5zM8 9h8M8 12h8M8 15h5", "F1", "Shortcut Help", "Open the keyboard shortcuts help dialog.")
                )
        );

        VBox body = new VBox(cards);
        body.getStyleClass().add("weblager-shortcuts-body");

        return body;
    }

    private HBox createShortcutCardsRow(Node leftCard, Node rightCard) {
        HBox row = new HBox(18, leftCard, rightCard);
        row.getStyleClass().add("weblager-shortcuts-card-row");
        HBox.setHgrow(leftCard, Priority.ALWAYS);
        HBox.setHgrow(rightCard, Priority.ALWAYS);
        return row;
    }

    private HBox createShortcutCard(String iconPath, String keyText, String titleText, String descriptionText) {
        SVGPath icon = new SVGPath();
        icon.setContent(iconPath);
        icon.getStyleClass().add("weblager-shortcuts-card-icon");

        StackPane iconFrame = new StackPane(icon);
        iconFrame.getStyleClass().add("weblager-shortcuts-card-icon-frame");

        Label key = new Label(keyText);
        key.getStyleClass().add("weblager-shortcuts-key-pill");

        Label title = new Label(titleText);
        title.getStyleClass().add("weblager-shortcuts-card-title");

        HBox titleRow = new HBox(16, key, title);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label description = new Label(descriptionText);
        description.getStyleClass().add("weblager-shortcuts-card-description");
        description.setWrapText(true);

        VBox copy = new VBox(10, titleRow, description);
        copy.setMinWidth(0);
        HBox.setHgrow(copy, Priority.ALWAYS);

        HBox card = new HBox(16, iconFrame, copy);
        card.getStyleClass().add("weblager-shortcuts-card");
        card.setAlignment(Pos.CENTER_LEFT);
        return card;
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

    private Optional<KeyboardShortcut> findShortcut(String actionName) {
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

    private record PasswordVisibilityField(HBox root, PasswordField hiddenField, TextField visibleField) {
        private String getText() {
            return hiddenField.getText() == null ? "" : hiddenField.getText();
        }

        private void setOnAction(javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
            hiddenField.setOnAction(handler);
            visibleField.setOnAction(handler);
        }
    }
}
