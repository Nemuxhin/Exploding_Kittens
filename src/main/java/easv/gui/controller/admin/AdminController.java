package easv.gui.controller.admin;

import easv.be.User;
import easv.bll.AdminManager;
import easv.bll.UserSession;
import easv.gui.BackgroundExecutor;
import easv.gui.MainApp;
import easv.gui.PrimeIcons;
import easv.util.Strings;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    @FXML private ToggleButton darkModeToggleButton;
    @FXML private Label darkModeToggleIcon;

    private AdminManager adminManager;
    private final Preferences preferences = Preferences.userRoot().node(THEME_PREFERENCES_NODE);
    private MainApp mainApp;
    private AdminPage pendingPage = AdminPage.DASHBOARD;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void initialize() {
        configureBrandLogo();
        configureAccount();
        configureAccountMenu();
        configureThemeToggle();
        configureNavigation();
        setNavigationDisabled(true);
        showLoadingPage("Loading admin data...");
        loadAdminDataAsync();
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

        if (adminManager == null) {
            pendingPage = page == null ? AdminPage.DASHBOARD : page;
            showLoadingPage("Loading admin data...");
            return;
        }

        loadPage(page);
        setActiveNavItem(getNavItem(page));
    }

    private void loadAdminDataAsync() {
        CompletableFuture
                .supplyAsync(AdminManager::new, BackgroundExecutor.io())
                .whenComplete((loadedManager, throwable) -> Platform.runLater(() -> {
                    if (throwable != null) {
                        showLoadingPage("Admin data could not be loaded.");
                        return;
                    }

                    adminManager = loadedManager;
                    setNavigationDisabled(false);
                    showPage(pendingPage);
                }));
    }

    private void setNavigationDisabled(boolean disabled) {
        for (ToggleButton navItem : getNavigationItems()) {
            if (navItem != null) {
                navItem.setDisable(disabled);
            }
        }
    }

    private void showLoadingPage(String message) {
        Label titleLabel = new Label(message);
        titleLabel.getStyleClass().add("page-title");

        Label subtitleLabel = new Label("Please wait a moment.");
        subtitleLabel.getStyleClass().add("page-subtitle");

        VBox placeholder = new VBox(6, titleLabel, subtitleLabel);
        placeholder.getStyleClass().addAll("admin-page", "main-content", "admin-missing-page-placeholder");
        placeholder.setAlignment(Pos.TOP_LEFT);
        placeholder.setMaxWidth(Double.MAX_VALUE);
        placeholder.setMaxHeight(Double.MAX_VALUE);

        contentHost.getChildren().setAll(placeholder);
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
            showPage(AdminPage.DASHBOARD);
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

        if (adminManager == null) {
            showInlineMessage(saveMessage, "Please wait until admin data has loaded.", false);
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

        if (adminManager == null) {
            return sessionUser;
        }

        return adminManager.getUsers().stream()
                .filter(user -> user.getId() == sessionUser.getId())
                .findFirst()
                .orElse(sessionUser);
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
