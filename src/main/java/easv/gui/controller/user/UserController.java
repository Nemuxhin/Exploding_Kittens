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
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

public class UserController implements UserNavigator {

    private static final String ACTIVE_NAV_CLASS = "active";
    private static final String DARK_MODE_CLASS = "dark";

    private static final String THEME_PREFERENCES_NODE = "easv.gui.weblager";
    private static final String DARK_MODE_PREFERENCE_KEY = "darkMode";
    private static final String LEGACY_USER_PREFERENCES_NODE = "easv.gui.portal";
    private static final String LEGACY_USER_DARK_MODE_KEY = "userPortal.darkMode";

    private static final String ACCOUNT_SECTION = "Edit Profile";

    private static final String LIGHT_MODE_LOGO =
            "/images/weblager/styleguide/Main Blue/LogoBlueH.png";

    private static final String DARK_MODE_LOGO =
            "/images/weblager/styleguide/DarkmodeBlue/LogoBlue2H.png";

    private static final String MOON_ICON = "\ue9c7";
    private static final String SUN_ICON = "\ue9c8";

    private static final String HELP_SCAN_ICON = "\ue934";
    private static final String HELP_QA_ICON = "\uea1b";
    private static final String HELP_EXPORT_ICON = "\ue956";
    private static final String HELP_SETTINGS_ICON = "\ue94a";

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
    @FXML private ToggleButton exportsNavItem;

    @FXML private Button keyboardShortcutsButton;
    @FXML private Button helpButton;
    @FXML private Button backNavigationButton;
    @FXML private Button homeNavigationButton;

    @FXML private Button accountMenuButton;
    @FXML private Label accountNameLabel;
    @FXML private Label accountInitialsLabel;
    @FXML private VBox accountDropdownPane;
    @FXML private Label accountDropdownNameLabel;
    @FXML private Label accountDropdownDetailLabel;
    @FXML private Button editProfileMenuButton;
    @FXML private Button settingsPrivacyMenuButton;
    @FXML private Button logoutMenuButton;
    @FXML private ToggleButton darkModeToggleButton;
    @FXML private Label darkModeToggleIcon;

    private final UserPortalModel portalModel = new UserPortalModel();
    private final ShortcutManager shortcutManager = new ShortcutManager();
    private final UserManager userManager = new UserManager();
    private final Preferences preferences = Preferences.userRoot().node(THEME_PREFERENCES_NODE);

    private MainApp mainApp;
    private Object activePageController;
    private final Map<UserPage, Node> pageCache = new EnumMap<>(UserPage.class);
    private final Map<UserPage, Object> controllerCache = new EnumMap<>(UserPage.class);
    private final Deque<UserPage> pageHistory = new ArrayDeque<>();
    private UserPage currentPage;
    private boolean movingThroughHistory;
    private Scene shortcutScene;
    private boolean rootShortcutFiltersRegistered;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void initialize() {
        configureAccount();
        configureAccountMenu();
        configureBrowserNavigationButtons();
        configureKeyboardShortcutsButton();
        configureHelpButton();
        configureThemeToggle();
        configureNavigation();
        configureGlobalShortcuts();
        showPage(UserPage.DASHBOARD);
    }

    private void configureBrowserNavigationButtons() {
        if (backNavigationButton != null) {
            backNavigationButton.setTooltip(new Tooltip("Go back"));
            backNavigationButton.setOnAction(event -> goBack());
        }

        if (homeNavigationButton != null) {
            homeNavigationButton.setTooltip(new Tooltip("Go to dashboard"));
            homeNavigationButton.setOnAction(event -> showPage(UserPage.DASHBOARD));
        }

        updateBrowserNavigationButtons();
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
            accountInitialsLabel.setText(Strings.initials(displayName, "U"));
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
            editProfileMenuButton.setOnAction(event -> showAccountSettingsPage());
        }

        if (settingsPrivacyMenuButton != null) {
            settingsPrivacyMenuButton.setOnAction(event -> {
                hideAccountDropdown();
                showPage(UserPage.SETTINGS);
            });
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

    private void configureHelpButton() {
        if (helpButton != null) {
            helpButton.setOnAction(event -> showHelpDialog());
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
        Preferences legacyUserPreferences = Preferences.userRoot().node(LEGACY_USER_PREFERENCES_NODE);

        return preferences.getBoolean(
                DARK_MODE_PREFERENCE_KEY,
                legacyUserPreferences.getBoolean(LEGACY_USER_DARK_MODE_KEY, false)
        );
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
            darkModeToggleIcon.setText(isDark ? MOON_ICON : SUN_ICON);
            PrimeIcons.applyFont(darkModeToggleIcon);
        }
    }

    private void configureNavigation() {
        for (UserPage page : UserPage.values()) {
            ToggleButton navItem = getNavItem(page);

            if (navItem != null) {
                navItem.setOnAction(event -> showPage(page));
            }
        }
    }

    private void configureGlobalShortcuts() {
        registerRootShortcutFilters();

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

    private void registerRootShortcutFilters() {
        if (appShell == null || rootShortcutFiltersRegistered) {
            return;
        }

        // Root filters keep scan shortcuts reachable even when focus is inside child pages.
        appShell.addEventFilter(KeyEvent.KEY_PRESSED, this::handleGlobalShortcut);
        appShell.addEventFilter(KeyEvent.KEY_TYPED, this::handleGlobalTypedShortcut);
        rootShortcutFiltersRegistered = true;
    }

    private void registerSceneShortcuts(Scene scene) {
        if (scene == shortcutScene) {
            return;
        }

        if (shortcutScene != null) {
            shortcutScene.removeEventFilter(KeyEvent.KEY_PRESSED, this::handleGlobalShortcut);
            shortcutScene.removeEventFilter(KeyEvent.KEY_TYPED, this::handleGlobalTypedShortcut);
        }

        shortcutScene = scene;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleGlobalShortcut);
        scene.addEventFilter(KeyEvent.KEY_TYPED, this::handleGlobalTypedShortcut);
        scene.getAccelerators().put(KeyCombination.valueOf("F1"), this::showKeyboardShortcutsDialog);
        scene.getAccelerators().put(KeyCombination.valueOf("SHIFT+SLASH"), this::showKeyboardShortcutsDialog);
    }

    private void handleGlobalShortcut(KeyEvent event) {
        if (event.isConsumed()) {
            return;
        }

        if (isShortcutHelp(event)) {
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
                && scanController.handleGlobalShortcut(event)) {
            event.consume();
            return;
        }

        if (!(event.getTarget() instanceof TextInputControl) && isScanShortcut(event)) {
            KeyCode code = event.getCode();
            boolean shortcutDown = event.isShortcutDown();
            String typedText = event.getText();

            showPage(UserPage.SCAN);
            Platform.runLater(() -> runShortcutOnActiveScanPage(code, shortcutDown, typedText));
            event.consume();
        }
    }

    private void handleGlobalTypedShortcut(KeyEvent event) {
        if (event.isConsumed() || event.getTarget() instanceof TextInputControl) {
            return;
        }

        String character = event.getCharacter();

        if ("?".equals(character)) {
            showKeyboardShortcutsDialog();
            event.consume();
            return;
        }

        if (activePageController instanceof ScanController scanController
                && scanController.runTypedShortcut(character)) {
            event.consume();
            return;
        }

        if ("+".equals(character) || "-".equals(character)) {
            showPage(UserPage.SCAN);
            Platform.runLater(() -> runTypedShortcutOnActiveScanPage(character));
            event.consume();
        }
    }

    private boolean isShortcutHelp(KeyEvent event) {
        return event.getCode() == KeyCode.F1
                || (event.isShiftDown() && event.getCode() == KeyCode.SLASH)
                || "?".equals(event.getText());
    }

    private boolean isScanShortcut(KeyEvent event) {
        return event.getCode() == KeyCode.RIGHT
                || event.getCode() == KeyCode.LEFT
                || event.getCode() == KeyCode.R
                || event.getCode() == KeyCode.DELETE
                || event.getCode() == KeyCode.BACK_SPACE
                || event.getCode() == KeyCode.PLUS
                || event.getCode() == KeyCode.ADD
                || event.getCode() == KeyCode.EQUALS
                || event.getCode() == KeyCode.MINUS
                || event.getCode() == KeyCode.SUBTRACT
                || event.getCode() == KeyCode.ESCAPE
                || "+".equals(event.getText())
                || "-".equals(event.getText())
                || (event.isShortcutDown() && (
                event.getCode() == KeyCode.Z
                        || event.getCode() == KeyCode.S
                        || event.getCode() == KeyCode.F
        ));
    }

    private void runShortcutOnActiveScanPage(KeyCode code, boolean shortcutDown, String typedText) {
        if (activePageController instanceof ScanController scanController) {
            scanController.runShortcut(code, shortcutDown, typedText);
        }
    }

    private void runTypedShortcutOnActiveScanPage(String character) {
        if (activePageController instanceof ScanController scanController) {
            scanController.runTypedShortcut(character);
        }
    }

    @Override
    public void showPage(UserPage page) {
        hideAccountDropdown();
        rememberCurrentPageBeforeOpening(page);
        loadPage(page);
        currentPage = page;
        setActiveNavItem(getNavItem(page));
        updateBrowserNavigationButtons();
    }

    private void rememberCurrentPageBeforeOpening(UserPage nextPage) {
        if (movingThroughHistory || currentPage == null || currentPage == nextPage) {
            return;
        }

        pageHistory.push(currentPage);
    }

    private void goBack() {
        if (pageHistory.isEmpty()) {
            return;
        }

        UserPage previousPage = pageHistory.pop();
        movingThroughHistory = true;
        try {
            showPage(previousPage);
        } finally {
            movingThroughHistory = false;
        }
    }

    private void updateBrowserNavigationButtons() {
        if (backNavigationButton != null) {
            backNavigationButton.setDisable(pageHistory.isEmpty());
        }

        if (homeNavigationButton != null) {
            homeNavigationButton.setDisable(currentPage == null || currentPage == UserPage.DASHBOARD);
        }
    }

    @Override
    public void resumeRecentScan(UserPortalModel.RecentScanItem item) {
        showPage(UserPage.SCAN);
    }

    @Override
    public void resumeHistoryScan(UserPortalModel.HistoryItem item) {
        showPage(UserPage.SCAN);
    }

    private void loadPage(UserPage page) {
        if (showCachedPage(page)) {
            return;
        }

        if (!page.hasFxml()) {
            activePageController = null;
            Node programmaticPage = wrapScrollable(createProgrammaticPage(page));
            showAndRememberPage(page, programmaticPage, null);
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
            PrimeIcons.applyFont(loadedPage);

            showAndRememberPage(page, loadedPage, activePageController);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load page: " + page.fxmlPath(), exception);
        }
    }

    private boolean showCachedPage(UserPage page) {
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

        // Keep visited pages ready, so navigation feels instant after the first load.
        pageCache.put(page, pageNode);
        controllerCache.put(page, controller);
    }

    private Node createProgrammaticPage(UserPage page) {
        return switch (page) {
            case DASHBOARD -> new DashboardController(portalModel, this).create();
            case MY_SCANS -> new MyScansController(portalModel, this).create();
            case EXPORTS -> new ExportsController(portalModel).create();
            case SETTINGS -> new SettingsController(portalModel).create();
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

    private void configureLoadedController(Object controller) {
        if (controller instanceof ScanController scanController) {
            scanController.setNavigator(this);
        }
    }

    private void configureLoadedPageSize(Parent page) {
        if (!(page instanceof Region region)) {
            return;
        }

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
        removeNavCloseButton(navItem);

        if (active) {
            navItem.getStyleClass().add(ACTIVE_NAV_CLASS);
            addNavCloseButton(navItem);
        }
    }

    private void addNavCloseButton(ToggleButton navItem) {
        if (navItem == null || navItem == dashboardNavItem || !(navItem.getGraphic() instanceof HBox graphic)) {
            return;
        }

        Label closeLabel = new Label("x");
        closeLabel.getStyleClass().add("admin-top-nav-close");
        closeLabel.setOnMouseClicked(event -> {
            event.consume();
            showPage(UserPage.DASHBOARD);
        });

        graphic.getChildren().add(closeLabel);
    }

    private void removeNavCloseButton(ToggleButton navItem) {
        if (navItem != null && navItem.getGraphic() instanceof HBox graphic) {
            graphic.getChildren().removeIf(node -> node.getStyleClass().contains("admin-top-nav-close"));
        }
    }

    private List<ToggleButton> getNavigationItems() {
        return List.of(
                dashboardNavItem,
                scanNavItem,
                myScansNavItem,
                assignedQANavItem,
                exportsNavItem
        );
    }

    private ToggleButton getNavItem(UserPage page) {
        return switch (page) {
            case DASHBOARD -> dashboardNavItem;
            case SCAN -> scanNavItem;
            case MY_SCANS -> myScansNavItem;
            case ASSIGNED_QA -> assignedQANavItem;
            case EXPORTS -> exportsNavItem;
            case EDIT_PROFILE -> null;
            case SETTINGS -> null;
        };
    }

    private void toggleAccountDropdown() {
        if (accountDropdownPane == null) {
            return;
        }

        boolean shouldShow = !accountDropdownPane.isVisible();
        accountDropdownPane.setVisible(shouldShow);

        if (shouldShow) {
            accountDropdownPane.toFront();
        }
    }

    private void hideAccountDropdown() {
        if (accountDropdownPane != null) {
            accountDropdownPane.setVisible(false);
        }
    }

    private void showAccountSettingsPage() {
        hideAccountDropdown();
        setActiveNavItem(null);
        contentHost.getChildren().setAll(wrapScrollable(createAccountSettingsPage()));
    }

    private VBox createAccountSettingsPage() {
        Label titleLabel = new Label(ACCOUNT_SECTION);
        titleLabel.getStyleClass().add("page-title");

        Label subtitleLabel = new Label("Manage your account information and password.");
        subtitleLabel.getStyleClass().add("page-subtitle");

        VBox intro = new VBox(3, titleLabel, subtitleLabel);
        intro.getStyleClass().add("page-heading-copy");

        VBox detailsPanel = new VBox(20);
        detailsPanel.getStyleClass().addAll("portal-card", "settings-panel");
        detailsPanel.setMaxWidth(Double.MAX_VALUE);
        detailsPanel.getChildren().setAll(buildAccountProfileSection());

        VBox page = new VBox(18, intro, detailsPanel);
        page.getStyleClass().addAll("portal-page", "main-content");
        page.setMaxWidth(Double.MAX_VALUE);
        page.setMaxHeight(Double.MAX_VALUE);

        return page;
    }

    private Node buildAccountProfileSection() {
        User account = currentAccountUser();
        UserPortalModel.AccountProfile fallbackProfile = portalModel.fetchAccountProfile();

        Label heading = new Label("Account Information");
        heading.getStyleClass().add("settings-section-heading");

        TextField nameField = createAccountTextField(displayNameFor(account, fallbackProfile));
        TextField usernameField = createAccountTextField(account == null ? "" : Strings.clean(account.getUsername()));
        TextField emailField = createAccountTextField(account == null ? "" : Strings.clean(account.getEmail()));
        TextField roleField = createAccountTextField(account == null ? "User" : Strings.clean(account.getRole()));
        TextField statusField = createAccountTextField(account == null ? "Active" : Strings.clean(account.getStatus()));

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
        messageLabel.setText(Strings.clean(message).isBlank() ? "Something went wrong." : message);
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
        GridPane grid = new GridPane();
        grid.getStyleClass().add("weblager-shortcuts-grid");
        grid.setHgap(18);
        grid.setVgap(14);

        List<ShortcutData> shortcuts = shortcutManager.getShortcuts().stream()
                .map(this::shortcutData)
                .toList();

        for (int index = 0; index < shortcuts.size(); index++) {
            int column = index % 2;
            int row = index / 2;
            grid.add(createKeyboardShortcutRow(shortcuts.get(index)), column, row);
        }

        Label footerText = new Label("Open this dialog anytime from the keyboard button, F1, or ?.");
        footerText.getStyleClass().add("weblager-shortcuts-footer-text");

        Button closeButton = new Button("Close");
        closeButton.getStyleClass().add("weblager-shortcuts-close-button");
        closeButton.setOnAction(event -> {
            dialog.setResult(ButtonType.CLOSE);
            dialog.close();
        });

        VBox body = new VBox(18, grid, footerText, closeButton);
        body.getStyleClass().add("weblager-shortcuts-body");
        body.setAlignment(Pos.TOP_CENTER);

        return body;
    }

    private HBox createKeyboardShortcutRow(ShortcutData shortcut) {
        Label icon = new Label(shortcut.icon());
        icon.getStyleClass().addAll("prime-icon", "weblager-shortcuts-row-icon");
        PrimeIcons.applyFont(icon);

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
            case "Next page" -> "\ue933";
            case "Previous page" -> "\ue932";
            case "Rotate" -> "\ue914";
            case "Delete" -> "\ue90b";
            case "Undo" -> "\ue931";
            case "Save" -> "\ue962";
            case "Search / jump" -> "\ue908";
            case "Export" -> "\ue956";
            case "Zoom in" -> "\ue9dd";
            case "Zoom out" -> "\ue9de";
            case "Escape" -> "\ue90b";
            case "Shortcut help" -> "\ue981";
            default -> "\ue981";
        };
    }

    private record ShortcutData(String icon, String label, String key, String description) {
    }

    private void showHelpDialog() {
        hideAccountDropdown();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setHeaderText(null);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Node defaultCloseButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (defaultCloseButton != null) {
            defaultCloseButton.setVisible(false);
            defaultCloseButton.setManaged(false);
        }

        dialog.getDialogPane().getStyleClass().addAll(
                "app-shell",
                "weblager-help-dialog-pane"
        );

        if (isDarkModeEnabled()) {
            dialog.getDialogPane().getStyleClass().add(DARK_MODE_CLASS);
        }

        if (appShell != null && appShell.getScene() != null) {
            dialog.initOwner(appShell.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().setAll(appShell.getScene().getStylesheets());
        }

        dialog.getDialogPane().setContent(createHelpDialogContent(dialog));
        dialog.showAndWait();
    }

    private VBox createHelpDialogContent(Dialog<ButtonType> dialog) {
        VBox root = new VBox();
        root.getStyleClass().add("weblager-help-root");

        root.getChildren().addAll(
                createHelpDialogHeader(dialog),
                createHelpDialogBody()
        );

        return root;
    }

    private HBox createHelpDialogHeader(Dialog<ButtonType> dialog) {
        Label title = new Label("Help & Documentation");
        title.getStyleClass().add("weblager-help-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeButton = new Button("×");
        closeButton.getStyleClass().add("weblager-help-close-button");
        closeButton.setFocusTraversable(false);
        closeButton.setOnAction(event -> {
            dialog.setResult(ButtonType.CLOSE);
            dialog.close();
        });

        HBox header = new HBox(12, title, spacer, closeButton);
        header.getStyleClass().add("weblager-help-header");
        header.setAlignment(Pos.CENTER_LEFT);

        return header;
    }

    private ScrollPane createHelpDialogBody() {
        VBox content = new VBox(18);
        content.getStyleClass().add("weblager-help-content");

        VBox gettingStartedRows = new VBox(15,
                createHelpRow(
                        HELP_SCAN_ICON,
                        "Starting a Scan",
                        "Navigate to New Scan, select your scan type, choose a profile, and enter the Box ID. The system will guide you through each page for quality approval."
                ),
                createHelpRow(
                        HELP_QA_ICON,
                        "Quality Assurance",
                        "During scanning, review each page as it appears. Use Space to approve, F to flag for rescan, or Delete to remove. Flagged pages can be rescanned later."
                ),
                createHelpRow(
                        HELP_EXPORT_ICON,
                        "Exporting Files",
                        "After scanning, configure your export settings including format, quality level, and OCR options. Files are available in the Exports page."
                ),
                createHelpRow(
                        HELP_SETTINGS_ICON,
                        "Customizing Settings",
                        "Open your account menu to access Settings and Privacy, dark mode, account details, and logout."
                )
        );

        VBox commonQuestions = new VBox(6,
                createHelpSectionTitle("Common Questions"),
                createQuestion(
                        "What's the difference between single and multi scan?",
                        "Single scan is for one document with multiple pages. Multi scan allows you to scan multiple separate documents in one session."
                ),
                createQuestion(
                        "Can I edit a scan after completion?",
                        "Once completed, you can view and export scans but cannot modify them. You can flag pages for rescan or delete unwanted pages during the scanning process."
                ),
                createQuestion(
                        "How long are exports stored?",
                        "Exports are stored for 30 days. Download important files promptly to avoid data loss."
                )
        );
        commonQuestions.getStyleClass().add("weblager-help-questions");

        content.getChildren().addAll(
                createHelpSectionTitle("Getting Started"),
                gettingStartedRows,
                createHelpDivider(),
                commonQuestions,
                createHelpDivider(),
                createHelpSupportSection()
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setPrefViewportWidth(690);
        scrollPane.setPrefViewportHeight(510);
        scrollPane.getStyleClass().add("weblager-help-scroll");

        return scrollPane;
    }

    private VBox createHelpSupportSection() {
        Label title = new Label("Need More Help?");
        title.getStyleClass().add("weblager-help-section-title");

        Label copy = new Label("Contact your system administrator or IT support team for additional assistance.");
        copy.setWrapText(true);
        copy.getStyleClass().add("weblager-help-support-copy");

        VBox section = new VBox(12, title, copy);
        section.getStyleClass().add("weblager-help-support-section");

        return section;
    }

    private HBox createHelpRow(String iconGlyph, String titleText, String bodyText) {
        Label icon = PrimeIcons.create(iconGlyph, "weblager-help-icon");

        StackPane iconShell = new StackPane(icon);
        iconShell.getStyleClass().add("weblager-help-icon-shell");

        Label title = new Label(titleText);
        title.getStyleClass().add("weblager-help-row-title");

        Label body = new Label(bodyText);
        body.setWrapText(true);
        body.getStyleClass().add("weblager-help-row-copy");

        VBox textBox = new VBox(4, title, body);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox row = new HBox(15, iconShell, textBox);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("weblager-help-row");

        return row;
    }

    private TitledPane createQuestion(String questionText, String answerText) {
        Label answer = new Label(answerText);
        answer.setWrapText(true);
        answer.getStyleClass().add("weblager-help-answer");

        TitledPane question = new TitledPane(questionText, answer);
        question.setExpanded(false);
        question.setAnimated(false);
        question.setFocusTraversable(false);
        question.getStyleClass().add("weblager-help-question");

        return question;
    }

    private Label createHelpSectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("weblager-help-section-title");
        return label;
    }

    private Region createHelpDivider() {
        Region divider = new Region();
        divider.getStyleClass().add("weblager-help-divider");
        return divider;
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
            if (!Strings.clean(user.getName()).isBlank()) {
                return Strings.clean(user.getName());
            }

            if (!Strings.clean(user.getUsername()).isBlank()) {
                return Strings.clean(user.getUsername());
            }
        }

        if (fallbackProfile != null && !Strings.clean(fallbackProfile.fullName()).isBlank()) {
            return Strings.clean(fallbackProfile.fullName());
        }

        return "User";
    }

    private String accountDetailFor(User user) {
        if (user == null) {
            return "User account";
        }

        if (!Strings.clean(user.getEmail()).isBlank()) {
            return Strings.clean(user.getEmail());
        }

        if (!Strings.clean(user.getUsername()).isBlank()) {
            return Strings.clean(user.getUsername());
        }

        return Strings.clean(user.getRole()).isBlank() ? "User account" : Strings.clean(user.getRole()) + " account";
    }

}
