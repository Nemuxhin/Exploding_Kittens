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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
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

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void initialize() {
        configureAccount();
        configureAccountMenu();
        configureKeyboardShortcutsButton();
        configureHelpButton();
        configureThemeToggle();
        configureNavigation();
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

    @Override
    public void showPage(UserPage page) {
        hideAccountDropdown();
        loadPage(page);
        setActiveNavItem(getNavItem(page));
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
        if (!page.hasFxml()) {
            contentHost.getChildren().setAll(wrapScrollable(createProgrammaticPage(page)));
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
            configureLoadedPageSize(loadedPage);
            PrimeIcons.applyFont(loadedPage);

            contentHost.getChildren().setAll(loadedPage);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load page: " + page.fxmlPath(), exception);
        }
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

        if (active) {
            navItem.getStyleClass().add(ACTIVE_NAV_CLASS);
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
        Label title = new Label("Keyboard Shortcuts");
        title.getStyleClass().add("weblager-shortcuts-title");

        Label subtitle = new Label("Common actions for scanning and QA.");
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

        HBox header = new HBox(12, copy, spacer, closeButton);
        header.getStyleClass().add("weblager-shortcuts-header");
        header.setAlignment(Pos.CENTER_LEFT);

        return header;
    }

    private VBox createKeyboardShortcutsBody(Dialog<ButtonType> dialog) {
        HBox cards = new HBox(24,
                createShortcutCard("Keyboard", shortcutManager.getShortcuts().stream()
                        .map(this::shortcutData)
                        .toList())
        );
        cards.getStyleClass().add("weblager-shortcuts-card-row");
        cards.setAlignment(Pos.TOP_CENTER);

        Label footerText = new Label("You can open this anytime from the help button or Settings.");
        footerText.getStyleClass().add("weblager-shortcuts-footer-text");

        Button closeButton = new Button("Close");
        closeButton.getStyleClass().add("weblager-shortcuts-close-button");
        closeButton.setOnAction(event -> {
            dialog.setResult(ButtonType.CLOSE);
            dialog.close();
        });

        VBox body = new VBox(24, cards, footerText, closeButton);
        body.getStyleClass().add("weblager-shortcuts-body");
        body.setAlignment(Pos.TOP_CENTER);

        return body;
    }

    private VBox createShortcutCard(String titleText, List<ShortcutData> shortcuts) {
        Label title = new Label(titleText);
        title.getStyleClass().add("weblager-shortcuts-card-title");

        VBox rows = new VBox(12);
        rows.getStyleClass().add("weblager-shortcuts-list");

        for (ShortcutData shortcut : shortcuts) {
            rows.getChildren().add(createKeyboardShortcutRow(shortcut.label(), shortcut.key()));
        }

        VBox card = new VBox(18, title, rows);
        card.getStyleClass().add("weblager-shortcuts-card");

        return card;
    }

    private HBox createKeyboardShortcutRow(String labelText, String keyText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("weblager-shortcuts-action-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label key = new Label(keyText);
        key.getStyleClass().add("weblager-shortcuts-key");

        HBox row = new HBox(12, label, spacer, key);
        row.getStyleClass().add("weblager-shortcuts-row");
        row.setAlignment(Pos.CENTER_LEFT);

        return row;
    }

    private ShortcutData shortcutData(String label, String key) {
        return new ShortcutData(label, key);
    }

    private ShortcutData shortcutData(KeyboardShortcut shortcut) {
        return new ShortcutData(shortcut.getActionName(), shortcut.getDisplayKeys());
    }

    private record ShortcutData(String label, String key) {
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
