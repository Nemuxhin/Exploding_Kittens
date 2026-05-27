package easv.gui.controller.admin;

import easv.be.User;
import easv.bll.AdminManager;
import easv.bll.AuthManager;
import easv.bll.UserSession;
import easv.gui.MainApp;
import easv.gui.controller.util.PrimeIcons;
import easv.gui.controller.util.Strings;
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
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.prefs.Preferences;

public class AdminController implements AdminNavigator {

    private static final String LIGHT_MODE_LOGO =
            "/images/weblager/styleguide/Main Blue/LogoBlueH.png";

    private static final String DARK_MODE_LOGO =
            "/images/weblager/styleguide/DarkmodeBlue/LogoBlue2H.png";

    private static final String ACTIVE_NAV_CLASS = "active";
    private static final String DARK_MODE_CLASS = "dark";
    private static final String THEME_PREFERENCES_NODE = "easv.gui.weblager";
    private static final String DARK_MODE_PREFERENCE_KEY = "darkMode";
    private static final String LEGACY_USER_PREFERENCES_NODE = "easv.gui.portal";
    private static final String LEGACY_USER_DARK_MODE_KEY = "userPortal.darkMode";
    private static final String ACCOUNT_SECTION = "Edit Profile";
    private static final String PRIVACY_SECTION = "Settings and Privacy";
    private static final String MOON_ICON = "\ue9c7";
    private static final String SUN_ICON = "\ue9c8";

    @FXML private StackPane appShell;
    @FXML private StackPane contentHost;

    @FXML private StackPane brandMark;
    @FXML private Label brandLogoFallbackLabel;
    @FXML private ImageView brandLogoImageView;

    @FXML private ToggleButton dashboardNavItem;
    @FXML private ToggleButton usersNavItem;
    @FXML private ToggleButton profilesNavItem;
    @FXML private ToggleButton assignmentsNavItem;
    @FXML private ToggleButton reviewNavItem;
    @FXML private ToggleButton activityNavItem;

    @FXML private Button accountMenuButton;
    @FXML private Label accountNameLabel;
    @FXML private Label accountInitialsLabel;
    @FXML private VBox accountDropdownPane;
    @FXML private Label accountDropdownNameLabel;
    @FXML private Label accountDropdownDetailLabel;
    @FXML private Button editProfileMenuButton;
    @FXML private Button settingsPrivacyMenuButton;
    @FXML private Button logoutMenuButton;
    @FXML private Button keyboardShortcutsButton;
    @FXML private ToggleButton darkModeToggleButton;
    @FXML private Label darkModeToggleIcon;

    private final AdminManager adminManager = new AdminManager();
    private final AuthManager authManager = new AuthManager();
    private final Preferences preferences = Preferences.userRoot().node(THEME_PREFERENCES_NODE);
    private MainApp mainApp;
    private AdminPage currentPage = AdminPage.DASHBOARD;
    private Scene shortcutScene;
    private boolean sceneListenerRegistered;
    private boolean userEditingTextInput;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void initialize() {
        configureBrandLogo();
        configureAccount();
        configureAccountMenu();
        configureKeyboardShortcuts();
        configureThemeToggle();
        configureNavigation();
        showPage(AdminPage.DASHBOARD);
    }

    private void configureBrandLogo() {
        updateBrandLogo(isDarkModeEnabled());
    }

    private void configureThemeToggle() {
        updateTheme(isDarkModeEnabled());

        if (darkModeToggleButton != null) {
            darkModeToggleButton.selectedProperty().addListener((observable, oldValue, isDark) ->
                    updateTheme(isDark)
            );
        }
    }

    private void configureAccount() {
        User currentUser = UserSession.getCurrentUser();
        String displayName = displayNameFor(currentUser);
        String accountDetail = accountDetailFor(currentUser);

        if (accountNameLabel != null) {
            accountNameLabel.setText(displayName);
        }

        if (accountInitialsLabel != null) {
            accountInitialsLabel.setText(Strings.initials(displayName, "AD"));
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

    private void configureKeyboardShortcuts() {
        if (appShell != null) {
            appShell.setFocusTraversable(true);
        }

        if (keyboardShortcutsButton != null) {
            keyboardShortcutsButton.setOnAction(event -> showAdminShortcutsDialog("Keyboard Shortcuts"));
        }

        Platform.runLater(this::registerShortcutFilters);
    }

    private void registerShortcutFilters() {
        if (appShell == null) {
            return;
        }

        Scene scene = appShell.getScene();
        if (scene == null) {
            registerSceneListener();
            return;
        }

        if (scene == shortcutScene) {
            return;
        }

        if (shortcutScene != null) {
            shortcutScene.removeEventFilter(KeyEvent.KEY_PRESSED, this::handleAdminShortcut);
            shortcutScene.removeEventFilter(KeyEvent.KEY_TYPED, this::handleAdminTypedShortcut);
            shortcutScene.removeEventFilter(MouseEvent.MOUSE_PRESSED, this::rememberTextInputFocusIntent);
        }

        shortcutScene = scene;
        shortcutScene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleAdminShortcut);
        shortcutScene.addEventFilter(KeyEvent.KEY_TYPED, this::handleAdminTypedShortcut);
        shortcutScene.addEventFilter(MouseEvent.MOUSE_PRESSED, this::rememberTextInputFocusIntent);
    }

    private void registerSceneListener() {
        if (sceneListenerRegistered) {
            return;
        }

        appShell.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                registerShortcutFilters();
            }
        });
        sceneListenerRegistered = true;
    }

    private void handleAdminShortcut(KeyEvent event) {
        if (event.isConsumed() || isUserEditingTextInput(event)) {
            return;
        }

        if (event.getCode() == KeyCode.F1) {
            showAdminShortcutsDialog("Keyboard Shortcuts");
            event.consume();
            return;
        }

        if (event.isShortcutDown() && event.getCode() == KeyCode.F) {
            showAdminShortcutsDialog("Search Help");
            event.consume();
            return;
        }

        if (event.isShortcutDown() && event.getCode() == KeyCode.S) {
            reloadCurrentPage();
            event.consume();
            return;
        }

        if (event.getCode() == KeyCode.LEFT) {
            showAdjacentAdminPage(-1);
            event.consume();
            return;
        }

        if (event.getCode() == KeyCode.RIGHT) {
            showAdjacentAdminPage(1);
            event.consume();
            return;
        }

        if (event.getCode() == KeyCode.ESCAPE) {
            hideAccountDropdown();
            event.consume();
        }
    }

    private void handleAdminTypedShortcut(KeyEvent event) {
        if (event.isConsumed() || isUserEditingTextInput(event)) {
            return;
        }

        if ("?".equals(event.getCharacter())) {
            showAdminShortcutsDialog("Keyboard Shortcuts");
            event.consume();
        }
    }

    private void reloadCurrentPage() {
        showPage(currentPage == null ? AdminPage.DASHBOARD : currentPage);
    }

    private void showAdjacentAdminPage(int step) {
        AdminPage[] pages = AdminPage.values();
        int currentIndex = Math.max(0, java.util.Arrays.asList(pages).indexOf(currentPage));
        int nextIndex = Math.max(0, Math.min(pages.length - 1, currentIndex + step));
        showPage(pages[nextIndex]);
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
        for (AdminPage page : AdminPage.values()) {
            setNavigationAction(getNavItem(page), () -> showPage(page));
        }
    }

    private void setNavigationAction(ToggleButton navItem, Runnable action) {
        if (navItem == null) {
            return;
        }

        navItem.setOnAction(event -> action.run());
    }

    @Override
    public void showPage(AdminPage page) {
        hideAccountDropdown();
        loadPage(page);
        currentPage = page;
        setActiveNavItem(getNavItem(page));
        resetShortcutFocus();
    }

    private void loadPage(AdminPage page) {
        URL pageUrl = getClass().getResource(page.fxmlPath());

        if (pageUrl == null) {
            contentHost.getChildren().setAll(createMissingPagePlaceholder(page.title()));
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(pageUrl);
            Parent loadedPage = loader.load();

            configureLoadedController(loader.getController());
            configureLoadedPageSize(loadedPage);
            PrimeIcons.applyFont(loadedPage);
            contentHost.getChildren().setAll(loadedPage);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load page: " + page.fxmlPath(), exception);
        }
    }

    private void configureLoadedController(Object controller) {
        if (controller instanceof DashboardController dashboardController) {
            dashboardController.setNavigator(this);
            dashboardController.setAdminManager(adminManager);
        } else if (controller instanceof ManageUsersController manageUsersController) {
            manageUsersController.setAdminManager(adminManager);
        } else if (controller instanceof ProfilesController profilesController) {
            profilesController.setNavigator(this);
            profilesController.setAdminManager(adminManager);
        } else if (controller instanceof AssignmentsController assignmentsController) {
            assignmentsController.setAdminManager(adminManager);
        } else if (controller instanceof ReviewController reviewController) {
            reviewController.setAdminManager(adminManager);
        } else if (controller instanceof ActivityController activityController) {
            activityController.setAdminManager(adminManager);
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

        Label subtitleLabel = new Label("This page has not been created yet.");
        subtitleLabel.getStyleClass().add("page-subtitle");

        VBox placeholder = new VBox(6, titleLabel, subtitleLabel);
        placeholder.getStyleClass().addAll("admin-page", "main-content", "admin-missing-page-placeholder");
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
                usersNavItem,
                profilesNavItem,
                assignmentsNavItem,
                reviewNavItem,
                activityNavItem
        );
    }

    private ToggleButton getNavItem(AdminPage page) {
        return switch (page) {
            case DASHBOARD -> dashboardNavItem;
            case USERS -> usersNavItem;
            case PROFILES -> profilesNavItem;
            case ASSIGNMENTS -> assignmentsNavItem;
            case REVIEW -> reviewNavItem;
            case ACTIVITY -> activityNavItem;
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

    private void showAccountSettingsPage(String selectedSection) {
        hideAccountDropdown();
        setActiveNavItem(null);
        contentHost.getChildren().setAll(wrapScrollable(createAccountSettingsPage(selectedSection)));
        resetShortcutFocus();
    }

    private ScrollPane wrapScrollable(Node page) {
        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.getStyleClass().add("admin-page-scroll");
        scrollPane.setMaxWidth(Double.MAX_VALUE);
        scrollPane.setMaxHeight(Double.MAX_VALUE);
        return scrollPane;
    }

    private VBox createAccountSettingsPage(String selectedSection) {
        String safeSection = Strings.clean(selectedSection).isBlank() ? ACCOUNT_SECTION : selectedSection;

        Label titleLabel = new Label(safeSection);
        titleLabel.getStyleClass().add("page-title");

        Label subtitleLabel = new Label(accountPageSubtitle(safeSection));
        subtitleLabel.getStyleClass().add("page-subtitle");

        VBox intro = new VBox(3, titleLabel, subtitleLabel);
        intro.getStyleClass().add("page-heading-copy");

        VBox nav = buildAccountSectionNav(safeSection);
        VBox detailsPanel = buildAccountDetailsPanel(safeSection);

        HBox layout = new HBox(24, nav, detailsPanel);
        layout.getStyleClass().add("settings-layout");
        layout.setFillHeight(false);
        HBox.setHgrow(detailsPanel, Priority.ALWAYS);

        VBox page = new VBox(18, intro, layout);
        page.getStyleClass().addAll("admin-page", "admin-account-settings-page", "main-content");
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

        for (String section : List.of(ACCOUNT_SECTION, PRIVACY_SECTION)) {
            Button button = new Button(section);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            button.getStyleClass().addAll("settings-nav-button", "admin-account-section-button");

            if (section.equals(selectedSection)) {
                button.getStyleClass().add(ACTIVE_NAV_CLASS);
            }

            button.setOnAction(event -> showAccountSettingsPage(section));
            nav.getChildren().add(button);
        }

        return nav;
    }

    private VBox buildAccountDetailsPanel(String selectedSection) {
        VBox detailsPanel = new VBox(20);
        detailsPanel.getStyleClass().addAll("portal-card", "settings-panel");
        detailsPanel.setMaxWidth(Double.MAX_VALUE);

        detailsPanel.getChildren().setAll(
                switch (selectedSection) {
                    case ACCOUNT_SECTION -> buildAccountProfileSection();
                    case PRIVACY_SECTION -> buildEmptyAccountSection(PRIVACY_SECTION);
                    default -> buildEmptyAccountSection(selectedSection);
                }
        );

        return detailsPanel;
    }

    private Node buildAccountProfileSection() {
        User account = currentAccountUser();

        Label heading = new Label("Account Information");
        heading.getStyleClass().add("settings-section-heading");

        TextField nameField = createAccountTextField(displayNameFor(account));
        TextField usernameField = createAccountTextField(account == null ? "" : Strings.clean(account.getUsername()));
        TextField emailField = createAccountTextField(account == null ? "" : Strings.clean(account.getEmail()));
        TextField roleField = createAccountTextField(account == null ? "Admin" : Strings.clean(account.getRole()));
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
            User updatedUser = adminManager.updateUser(
                    account.getId(),
                    new AdminManager.UserInput(
                            nameField.getText(),
                            usernameField.getText(),
                            emailField.getText(),
                            account.getRole(),
                            account.getStatus(),
                            account.getAssignedProfiles(),
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

    private Node buildEmptyAccountSection(String title) {
        Label heading = new Label(title);
        heading.getStyleClass().add("settings-section-heading");

        VBox content = new VBox(20, heading);
        content.getStyleClass().add("settings-form");
        return content;
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

    private User currentAccountUser() {
        User sessionUser = UserSession.getCurrentUser();

        if (sessionUser == null) {
            return null;
        }

        return adminManager.getUsers().stream()
                .filter(user -> user.getId() == sessionUser.getId())
                .findFirst()
                .orElse(sessionUser);
    }

    private void showAdminShortcutsDialog(String titleText) {
        hideAccountDropdown();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle(titleText);
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Node defaultCloseButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (defaultCloseButton != null) {
            defaultCloseButton.setVisible(false);
            defaultCloseButton.setManaged(false);
        }

        dialog.getDialogPane().getStyleClass().addAll("app-shell", "weblager-shortcuts-dialog-pane");

        if (isDarkModeEnabled()) {
            dialog.getDialogPane().getStyleClass().add(DARK_MODE_CLASS);
        }

        if (appShell != null && appShell.getScene() != null) {
            dialog.initOwner(appShell.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().setAll(appShell.getScene().getStylesheets());
        }

        dialog.getDialogPane().setPrefSize(1000, 780);
        dialog.getDialogPane().setMaxSize(1000, 780);
        dialog.getDialogPane().setContent(createAdminShortcutsContent(dialog, titleText));
        PrimeIcons.applyFont(dialog.getDialogPane());

        dialog.showAndWait();
    }

    private VBox createAdminShortcutsContent(Dialog<ButtonType> dialog, String titleText) {
        VBox root = new VBox();
        root.getStyleClass().add("weblager-shortcuts-root");

        root.getChildren().addAll(
                createAdminShortcutsHeader(dialog, titleText),
                createAdminShortcutsBody(dialog)
        );

        return root;
    }

    private HBox createAdminShortcutsHeader(Dialog<ButtonType> dialog, String titleText) {
        Label keyboardIcon = new Label("\ue981");
        keyboardIcon.getStyleClass().addAll("prime-icon", "weblager-shortcuts-title-icon");
        PrimeIcons.applyFont(keyboardIcon);

        StackPane iconShell = new StackPane(keyboardIcon);
        iconShell.getStyleClass().add("weblager-shortcuts-title-icon-shell");

        Label title = new Label(titleText);
        title.getStyleClass().add("weblager-shortcuts-title");

        Label subtitle = new Label("Common actions for admin navigation.");
        subtitle.getStyleClass().add("weblager-shortcuts-subtitle");

        VBox copy = new VBox(3, title, subtitle);
        copy.getStyleClass().add("weblager-shortcuts-header-copy");

        Button closeButton = new Button("X");
        closeButton.getStyleClass().add("weblager-shortcuts-x-button");
        closeButton.setFocusTraversable(false);
        closeButton.setOnAction(event -> {
            dialog.setResult(ButtonType.CLOSE);
            dialog.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(18, iconShell, copy, spacer, closeButton);
        header.getStyleClass().add("weblager-shortcuts-header");
        header.setAlignment(Pos.CENTER_LEFT);

        return header;
    }

    private VBox createAdminShortcutsBody(Dialog<ButtonType> dialog) {
        VBox cards = new VBox(18,
                createAdminShortcutCardsRow(
                        createAdminShortcutCard("M5 12h14M13 6l6 6-6 6", "->", "Next Page", "Move to the next scanned page."),
                        createAdminShortcutCard("M19 12H5m6-6-6 6 6 6", "<-", "Previous Page", "Move to the previous scanned page.")
                ),
                createAdminShortcutCardsRow(
                        createAdminShortcutCard("M20 12a8 8 0 1 1-2.34-5.66M20 4v5h-5", "R", "Rotate", "Rotate the selected page by degree."),
                        createAdminShortcutCard("M7 7h10M10 11v6m4-6v6M9 7l1-2h4l1 2m-8 0 1 13h8l1-13", "Del", "Delete", "Delete the selected page.")
                ),
                createAdminShortcutCardsRow(
                        createAdminShortcutCard("M9 7l-5 5 5 5M4 12h11a5 5 0 0 1 0 10", "Ctrl + Z", "Undo", "Undo the last supported action."),
                        createAdminShortcutCard("M5 4h12l2 2v14H5zM8 4v6h8V4M8 15h8v5H8z", "Ctrl + S", "Save", "Save the current work.")
                ),
                createAdminShortcutCardsRow(
                        createAdminShortcutCard("M10 18a8 8 0 1 1 5.66-2.34L21 21", "Ctrl + F", "Search / Jump", "Search for a page, file, or document."),
                        createAdminShortcutCard("M12 4v12m-5-5 5 5 5-5M5 20h14", "Ctrl + E", "Export", "Open the export dialog.")
                ),
                createAdminShortcutCardsRow(
                        createAdminShortcutCard("M10 17a7 7 0 1 1 4.95-2.05L21 21M10 7v6m-3-3h6", "+", "Zoom in", "Zoom in on the current page."),
                        createAdminShortcutCard("M10 17a7 7 0 1 1 4.95-2.05L21 21M7 10h6", "-", "Zoom out", "Zoom out from the current page.")
                ),
                createAdminShortcutCardsRow(
                        createAdminShortcutCard("M7 7l10 10M17 7 7 17", "Esc", "Escape", "Close menus or dialogs."),
                        createAdminShortcutCard("M5 5h14v14H5zM8 9h8M8 12h8M8 15h5", "F1", "Shortcut Help", "Open the keyboard shortcuts help dialog.")
                )
        );

        VBox body = new VBox(cards);
        body.getStyleClass().add("weblager-shortcuts-body");

        return body;
    }

    private HBox createAdminShortcutCardsRow(Node leftCard, Node rightCard) {
        HBox row = new HBox(18, leftCard, rightCard);
        row.getStyleClass().add("weblager-shortcuts-card-row");
        HBox.setHgrow(leftCard, Priority.ALWAYS);
        HBox.setHgrow(rightCard, Priority.ALWAYS);
        return row;
    }

    private HBox createAdminShortcutCard(String iconPath, String keyText, String titleText, String descriptionText) {
        javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
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

    private VBox createShortcutSection(String titleText, String... lines) {
        Label title = new Label(titleText);
        title.getStyleClass().add("weblager-shortcuts-section-title");

        VBox rows = new VBox(4);

        for (String line : lines) {
            rows.getChildren().add(createAdminShortcutLine(line));
        }

        VBox section = new VBox(8, title, rows);
        section.getStyleClass().add("weblager-shortcuts-section");
        return section;
    }

    private HBox createAdminShortcutLine(String line) {
        String[] parts = line.split(" - ", 2);
        Label key = new Label(parts[0]);
        key.getStyleClass().add("weblager-shortcuts-key");

        Label action = new Label(parts.length > 1 ? parts[1] : parts[0]);
        action.getStyleClass().add("weblager-shortcuts-action-description");
        action.setWrapText(true);

        HBox row = new HBox(12, key, action);
        row.getStyleClass().add("weblager-shortcuts-simple-row");
        row.setAlignment(Pos.CENTER_LEFT);

        if (parts.length == 1) {
            key.setVisible(false);
            key.setManaged(false);
        }

        return row;
    }

    private void logout() {
        authManager.logout();
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

    private String displayNameFor(User user) {
        if (user == null) {
            return "Admin";
        }

        if (!Strings.clean(user.getName()).isBlank()) {
            return Strings.clean(user.getName());
        }

        if (!Strings.clean(user.getUsername()).isBlank()) {
            return Strings.clean(user.getUsername());
        }

        return "Admin";
    }

    private String accountDetailFor(User user) {
        if (user == null) {
            return "Admin account";
        }

        if (!Strings.clean(user.getEmail()).isBlank()) {
            return Strings.clean(user.getEmail());
        }

        if (!Strings.clean(user.getUsername()).isBlank()) {
            return Strings.clean(user.getUsername());
        }

        return Strings.clean(user.getRole()).isBlank() ? "Admin account" : Strings.clean(user.getRole()) + " account";
    }

}
